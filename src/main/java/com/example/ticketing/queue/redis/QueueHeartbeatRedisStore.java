package com.example.ticketing.queue.redis;

import com.example.ticketing.exception.queue.QueueUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
@Component
public class QueueHeartbeatRedisStore {
    // heartbeat는 polling마다 쓰지 않고 30초마다 갱신
    private static final Duration HEARTBEAT_WRITE_INTERVAL = Duration.ofSeconds(30);
    // CANCELLED 상태를 클라이언트가 확인할 수 있도록 10분 보존
    private static final Duration TERMINAL_RETENTION = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;


    // Lua: heartbeat 갱신 처리
    private static final DefaultRedisScript<Long>
            TOUCH_HEARTBEAT_SCRIPT =
            new DefaultRedisScript<>("""
                local status =
                    redis.call(
                        'HGET',
                        KEYS[1],
                        'status'
                    )

                -- WAITING이 아니면 heartbeat를 갱신하지 않음
                if status ~= 'WAITING' then
                    return 0
                end

                -- 마지막 heartbeat시각을 조회
                local lastHeartbeat =
                    redis.call(
                        'ZSCORE',
                        KEYS[2],
                        ARGV[1]
                    )

                local nowMillis =
                    tonumber(ARGV[2])

                local intervalMillis =
                    tonumber(ARGV[3])

                -- 마지막 갱신 후 30초가 지나지 않았다면 쓰지 않음
                if lastHeartbeat
                    and nowMillis - tonumber(lastHeartbeat)
                        < intervalMillis then
                    return 0
                end
                -- Sorted Set에 heartbeat 시간을 갱신
                -- queue:{sessionId}:heartbeat
                redis.call(
                    'ZADD',
                    KEYS[2],
                    nowMillis,
                    ARGV[1]
                )

                -- heartbeat ZSET에 아직 TTL이 없다면
                local heartbeatTtl =
                  redis.call(
                      'PTTL',
                      KEYS[2]
                  )
            
                if heartbeatTtl == -1 then
                  --이미 공연 시작 + grace까지 TTL이 설정되어있는 QueueTicket의 member를 참고
                  local ticketTtl =
                      redis.call(
                          'PTTL',
                          KEYS[1]
                      )
           
                  if ticketTtl > 0 then
                      redis.call(
                          'PEXPIRE',
                          KEYS[2],
                          ticketTtl
                      )
                  end
                end

                return 1
                """, Long.class);


    // 마지막 쓰기 후 30초가 지난 경우에만 heartbeat 갱신
    public boolean touchIfNecessary(
            Long sessionId,
            String queueTicketId,
            Instant now
    ) {
        try {
            Long updated = redisTemplate.execute(
                    TOUCH_HEARTBEAT_SCRIPT,
                    List.of(
                            QueueRedisKey.ticket( // KEYS[1]: QueueTicket Hash
                                    sessionId,
                                    queueTicketId
                            ),
                            QueueRedisKey.waitingHeartbeat( //KEYS[2]: WAITING heartbeat ZSET
                                    sessionId
                            )
                    ),
                    queueTicketId,                     // ARGV[1]: queueTicketId
                    Long.toString(now.toEpochMilli()), // ARGV[2]: 현재 시간
                    Long.toString(
                            HEARTBEAT_WRITE_INTERVAL.toMillis() // ARGV[3]: heartbeat를 최소 몇 ms 간격으로 갱신할지
                    )
            );

            return updated != null && updated == 1L;

        } catch (DataAccessException | IllegalArgumentException exception) {
            throw new QueueUnavailableException(exception);
        }
    }

    // Lua: heartbeat가 장시간 끊긴 WAITING 티켓을 CANCELLED 상태로 전환
    private static final DefaultRedisScript<Long>
            CANCEL_STALE_WAITING_SCRIPT =
            new DefaultRedisScript<>("""
            local heartbeatTimeoutMillis = tonumber(ARGV[3])
            local terminalRetentionMillis = tonumber(ARGV[4])
            local batchSize = tonumber(ARGV[5])

            if not heartbeatTimeoutMillis
                or heartbeatTimeoutMillis < 1
                or not terminalRetentionMillis
                or terminalRetentionMillis < 1
                or not batchSize
                or batchSize < 1 then

                return -1
            end

            -- =========================================
            -- Epoch millis를 ISO-8601 문자열로 변환
            -- =========================================

            local function epochMillisToIso8601(
                epochMillis
            )
                local totalSeconds = math.floor(epochMillis / 1000)
                local milliseconds = epochMillis - totalSeconds * 1000
                local days = math.floor(totalSeconds / 86400)
                local secondsOfDay = totalSeconds - days * 86400
                local hour = math.floor(secondsOfDay / 3600)
                local minute = math.floor((secondsOfDay % 3600) / 60)
                local second = secondsOfDay % 60
                local z = days + 719468
                local era = math.floor(z / 146097)
                local dayOfEra = z - era * 146097
                local yearOfEra =
                    math.floor(
                        (
                            dayOfEra
                            - math.floor(
                                dayOfEra / 1460
                            )
                            + math.floor(
                                dayOfEra / 36524
                            )
                            - math.floor(
                                dayOfEra / 146096
                            )
                        ) / 365
                    )
                local year = yearOfEra + era * 400
                local dayOfYear =
                    dayOfEra
                    - (
                        365 * yearOfEra
                        + math.floor(
                            yearOfEra / 4
                        )
                        - math.floor(
                            yearOfEra / 100
                        )
                    )
                local monthPrime =
                    math.floor(
                        (5 * dayOfYear + 2) / 153
                    )
                local day =
                    dayOfYear
                    - math.floor(
                        (153 * monthPrime + 2) / 5
                    )
                    + 1
            
                local month

                if monthPrime < 10 then
                    month = monthPrime + 3
                else
                    month = monthPrime - 9
                end

                if month <= 2 then
                    year = year + 1
                end

                return string.format(
                    '%04d-%02d-%02dT%02d:%02d:%02d.%03dZ',
                    year,
                    month,
                    day,
                    hour,
                    minute,
                    second,
                    milliseconds
                )
            end

            -- =========================================
            -- 1. Redis 서버 기준 현재시간 조회
            -- =========================================

            local redisTime = redis.call('TIME')
            local nowMillis =
                tonumber(redisTime[1]) * 1000
                + math.floor(tonumber(redisTime[2]) / 1000)

            local heartbeatCutoffMillis = nowMillis - heartbeatTimeoutMillis
            local terminalAt = epochMillisToIso8601(nowMillis)

            -- =========================================
            -- 2. heartbeat 만료 후보 조회
            -- =========================================

            local staleTicketIds =
                redis.call(
                    'ZRANGEBYSCORE',
                    KEYS[1],
                    '-inf',
                    heartbeatCutoffMillis,
                    'LIMIT',
                    0,
                    batchSize
                )

            local cancelledCount = 0

            for _, queueTicketId
                in ipairs(staleTicketIds) do

                    local ticketKey = ARGV[1] .. queueTicketId

                    local currentStatus =
                        redis.call(
                            'HGET',
                            ticketKey,
                            'status'
                        )

                    if currentStatus == 'WAITING' then
                        local userId =
                            redis.call(
                                'HGET',
                                ticketKey,
                                'userId'
                            )

                        -- =================================
                        -- 3. WAITING → CANCELLED
                        -- =================================

                        redis.call(
                            'HSET',
                            ticketKey,
                            'status', 'CANCELLED',
                            'terminalAt', terminalAt
                        )

                        -- 대기열에서 제거
                        redis.call(
                            'ZREM',
                            KEYS[2],
                            queueTicketId
                        )

                        -- heartbeat에서도 제거
                        redis.call(
                            'ZREM',
                            KEYS[1],
                            queueTicketId
                        )

                        -- 종료 상태를 10분 동안 보존
                        redis.call(
                            'PEXPIRE',
                            ticketKey,
                            terminalRetentionMillis
                        )

                        -- 사용자 → 티켓 매핑도 같은 시간 보존
                        if userId then
                            local userTicketKey = ARGV[2] .. userId

                            local mappedTicketId =
                                redis.call(
                                    'GET',
                                    userTicketKey
                                )

                            if mappedTicketId == queueTicketId then

                                redis.call(
                                    'PEXPIRE',
                                    userTicketKey,
                                    terminalRetentionMillis
                                )
                            end
                        end

                        cancelledCount = cancelledCount + 1

                    else
                        -- 잘못 남은 waiting/heartbeat member만 제거합니다.
                        redis.call(
                            'ZREM',
                            KEYS[2],
                            queueTicketId
                        )

                        redis.call(
                            'ZREM',
                            KEYS[1],
                            queueTicketId
                        )
                    end
                end
            end

            return cancelledCount
            """, Long.class);

    /**
     * heartbeat가 설정 시간 이상 끊긴 WAITING 티켓을 CANCELLED로 변경
     * @return 실제 WAITING → CANCELLED 처리된 티켓 수
     */
    public int cancelStaleWaiting(
            Long sessionId,
            Duration heartbeatTimeout,
            int batchSize
    ) {
        long heartbeatTimeoutMillis =
                validateCleanupArguments(
                        sessionId,
                        heartbeatTimeout,
                        batchSize
                );

        try {
            Long cancelledCount =
                    redisTemplate.execute(
                            CANCEL_STALE_WAITING_SCRIPT,
                            List.of(
                                    QueueRedisKey.waitingHeartbeat(sessionId), // KEYS[1]: WAITING heartbeat ZSET
                                    QueueRedisKey.waitingQueue(sessionId)      // KEYS[2]: WAITING ZSET
                            ),
                            QueueRedisKey.ticketPrefix(sessionId),             // ARGV[1]: QueueTicket key prefix
                            QueueRedisKey.userTicketPrefix(sessionId),         // ARGV[2]: userTicket key prefix
                            Long.toString(heartbeatTimeoutMillis),             // ARGV[3]: heartbeat timeout(ms)
                            Long.toString(TERMINAL_RETENTION.toMillis()),      // ARGV[4]: 종료 상태 보존시간(ms)
                            Integer.toString(batchSize)                        // ARGV[5]: 한 번에 처리할 최대 개수
                    );

            if (cancelledCount == null) {
                throw new IllegalArgumentException(
                        "Redis heartbeat 정리 결과가 없습니다."
                );
            }

            if (cancelledCount < 0L || cancelledCount > batchSize) {
                throw new IllegalArgumentException(
                        "Redis heartbeat 정리 결과가 "
                                + "올바르지 않습니다: "
                                + cancelledCount
                );
            }

            return cancelledCount.intValue();

        } catch (DataAccessException | IllegalArgumentException exception) {

            throw new QueueUnavailableException(exception);
        }
    }

    private long validateCleanupArguments(
            Long sessionId,
            Duration heartbeatTimeout,
            int batchSize
    ) {
        if (sessionId == null || sessionId <= 0L) {
            throw new IllegalArgumentException(
                    "sessionId는 1 이상이어야 합니다."
            );
        }

        if (heartbeatTimeout == null
                || heartbeatTimeout.isZero()
                || heartbeatTimeout.isNegative()) {

            throw new IllegalArgumentException(
                    "heartbeatTimeout은 양수여야 합니다."
            );
        }

        if (batchSize < 1) {
            throw new IllegalArgumentException(
                    "batchSize는 1 이상이어야 합니다."
            );
        }

        final long timeoutMillis;

        try {
            timeoutMillis = heartbeatTimeout.toMillis();

        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "heartbeatTimeout이 너무 큽니다.",
                    exception
            );
        }

        if (timeoutMillis < 1L) {
            throw new IllegalArgumentException(
                    "heartbeatTimeout은 최소 1ms 이상이어야 합니다."
            );
        }

        return timeoutMillis;
    }
}
