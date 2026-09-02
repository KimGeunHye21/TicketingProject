package com.example.ticketing.queue.redis;

import com.example.ticketing.exception.queue.QueueUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
public class QueueAdmissionRedisStore {
    private static final Duration TERMINAL_RETENTION = Duration.ofMinutes(10);
    // 잘못 남은 waiting ZSET member가 지나치게 많은 경우를 대비하여 한번에 실행되는 대기열처리 수 제한
    private static final int MAX_WAITING_INSPECTIONS = 10_000;

    private final StringRedisTemplate redisTemplate;

    private static final DefaultRedisScript<Long>
            ADMIT_NEXT_SCRIPT =
            new DefaultRedisScript<>("""
                -- KEYS[1]: waiting ZSET
                -- KEYS[2]: selecting ZSET
                -- KEYS[3]: waiting heartbeat ZSET
                --
                -- ARGV[1]: QueueTicket key prefix
                -- ARGV[2]: maxSelectingUsers
                -- ARGV[3]: selectingTtlMillis
                -- ARGV[4]: maxWaitingInspections

                local maxSelectingUsers = tonumber(ARGV[2])
                local selectingTtlMillis = tonumber(ARGV[3])
                local maxWaitingInspections = tonumber(ARGV[4])


                -- =========================================
                -- Epoch millis를 Instant.parse() 가능한
                -- UTC ISO-8601 문자열로 변환하는 함수
                -- =========================================

                local function epochMillisToIso8601(epochMillis)
                    local totalSeconds = math.floor(epochMillis / 1000)
                    local milliseconds = epochMillis - totalSeconds * 1000

                    local days = math.floor(totalSeconds / 86400)
                    local secondsOfDay = totalSeconds - days * 86400

                    local hour = math.floor(secondsOfDay / 3600)
                    local minute = math.floor((secondsOfDay % 3600) / 60)
                    local second = secondsOfDay % 60

                    -- Unix epoch day를 Gregorian 날짜로 변환
                    local z = days + 719468
                    local era = math.floor(z / 146097)
                    local dayOfEra = z - era * 146097

                    local yearOfEra =
                        math.floor(
                            (
                                dayOfEra
                                - math.floor(dayOfEra / 1460)
                                + math.floor(dayOfEra / 36524)
                                - math.floor(dayOfEra / 146096)
                            ) / 365
                        )

                    local year = yearOfEra + era * 400
                    local dayOfYear =
                        dayOfEra
                        - (
                            365 * yearOfEra
                            + math.floor(yearOfEra / 4)
                            - math.floor(yearOfEra / 100)
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
                -- 1. Redis 서버 기준 시간 계산
                -- =========================================

                local redisTime = redis.call('TIME')
                local nowMillis =
                    tonumber(redisTime[1]) * 1000
                    + math.floor(tonumber(redisTime[2]) / 1000)

                local selectingExpiresAtMillis = nowMillis + selectingTtlMillis
                local selectingStartedAt = epochMillisToIso8601(nowMillis)
                local selectingExpiresAt = epochMillisToIso8601(selectingExpiresAtMillis)


                -- =========================================
                -- 2. 현재 SELECTING 인원과 여유 슬롯 계산
                -- =========================================

                local currentSelectingCount =
                    redis.call(
                        'ZCARD',
                        KEYS[2]
                    )

                local availableSlots = maxSelectingUsers - currentSelectingCount
                if availableSlots <= 0 then
                    return 0
                end

                -- =========================================
                -- 3. WAITING 선두 사용자 선정
                -- =========================================

                local admittedCount = 0
                local inspectedCount = 0

                while admittedCount < availableSlots
                    and inspectedCount < maxWaitingInspections do

                    -- waitingNumber가 가장 작은 티켓 1개를 조회와 동시에 ZSET에서 제거
                    local popped =
                        redis.call(
                            'ZPOPMIN',
                            KEYS[1],
                            1
                        )

                    if #popped == 0 then
                        break
                    end

                    local queueTicketId = popped[1]
                    inspectedCount = inspectedCount + 1

                    local ticketKey = ARGV[1] .. queueTicketId
                    local currentStatus =
                        redis.call(
                            'HGET',
                            ticketKey,
                            'status'
                        )

                    -- 티켓 Hash가 없거나 WAITING이 아니라면
                    -- 잘못 남은 ZSET member이므로 버리고 다음 확인
                    if currentStatus == 'WAITING' then

                        -- =================================
                        -- 4. WAITING → SELECTING
                        -- =================================

                        redis.call(
                            'HSET',
                            ticketKey,
                            'status', 'SELECTING',
                            'selectingStartedAt',
                                selectingStartedAt,
                            'selectingExpiresAt',
                                selectingExpiresAt,
                            'selectingExpiresAtEpochMilli',
                                selectingExpiresAtMillis
                        )

                        -- ZPOPMIN으로 waiting에서는 이미 제거됨

                        -- WAITING heartbeat 제거
                        redis.call(
                            'ZREM',
                            KEYS[3],
                            queueTicketId
                        )

                        -- SELECTING ZSET에 추가
                        redis.call(
                            'ZADD',
                            KEYS[2],
                            selectingExpiresAtMillis,
                            queueTicketId
                        )

                        admittedCount = admittedCount + 1

                    else
                        -- WAITING이 아닌 티켓의 heartbeat가
                        -- 남아 있다면 같이 정리
                        redis.call(
                            'ZREM',
                            KEYS[3],
                            queueTicketId
                        )
                    end
                end

                -- selecting ZSET 자체가 선택 만료시간보다
                -- 먼저 사라지지 않게 TTL 설정
                if admittedCount > 0 then
                    local selectingQueueTtl =
                        redis.call(
                            'PTTL',
                            KEYS[2]
                        )

                    if selectingQueueTtl < selectingTtlMillis then
                        redis.call(
                            'PEXPIRE',
                            KEYS[2],
                            selectingTtlMillis
                        )
                    end
                end

                return admittedCount
                """, Long.class);


    public int admitNext(
            Long sessionId,
            int maxSelectingUsers, // 좌석 선택 상태를 허용할 최대 인원
            Duration selectingTtl
    ) {
        long selectingTtlMillis =
                validateAndGetTtlMillis(
                        sessionId,
                        maxSelectingUsers,
                        selectingTtl
                );

        try {
            Long admittedCount =
                    redisTemplate.execute(
                            ADMIT_NEXT_SCRIPT,
                            List.of(
                                    QueueRedisKey.waitingQueue(sessionId), // KEYS[1]: WAITING ZSET
                                    QueueRedisKey.selectingQueue(sessionId), // KEYS[2]: SELECTING ZSET
                                    QueueRedisKey.waitingHeartbeat(sessionId) // KEYS[3]: WAITING heartbeat ZSET
                            ),

                            QueueRedisKey.ticketPrefix(sessionId), // ARGV[1]: QueueTicket Key prefix
                            Integer.toString(maxSelectingUsers), // ARGV[2]: 최대 SELECTING 인원
                            Long.toString(selectingTtlMillis), // ARGV[3]: SELECTING 제한시간(ms)
                            Integer.toString(MAX_WAITING_INSPECTIONS) // ARGV[4]: 최대 WAITING 검사 수
                    );

            if (admittedCount == null) {
                throw new IllegalArgumentException(
                        "Redis 입장 처리 결과가 없습니다."
                );
            }

            if (admittedCount < 0L || admittedCount > maxSelectingUsers) {
                throw new IllegalArgumentException(
                        "Redis 입장 처리 결과가 올바르지 않습니다.\n입장자 수: " + admittedCount
                );
            }

            return admittedCount.intValue();

        } catch (DataAccessException |
                 IllegalArgumentException exception) {

            throw new QueueUnavailableException(exception);
        }
    }

    // Redis 호출 전에 입력값과 설정값을 검사합니다.
    private long validateAndGetTtlMillis(
            Long sessionId,
            int maxSelectingUsers,
            Duration selectingTtl
    ) {
        if (sessionId == null || sessionId <= 0L) {
            throw new IllegalArgumentException(
                    "sessionId는 1 이상이어야 합니다."
            );
        }

        if (maxSelectingUsers < 1) {
            throw new IllegalArgumentException(
                    "maxSelectingUsers는 1 이상이어야 합니다."
            );
        }

        if (selectingTtl == null
                || selectingTtl.isZero()
                || selectingTtl.isNegative()) {

            throw new IllegalArgumentException(
                    "selectingTtl은 양수여야 합니다."
            );
        }

        final long selectingTtlMillis;

        try {
            selectingTtlMillis = selectingTtl.toMillis();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "selectingTtl이 너무 큽니다.",
                    exception
            );
        }

        if (selectingTtlMillis < 1L) {
            throw new IllegalArgumentException(
                    "selectingTtl은 최소 1ms 이상이어야 합니다."
            );
        }

        return selectingTtlMillis;
    }



}
