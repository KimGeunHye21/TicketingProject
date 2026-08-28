package com.example.ticketing.queue;

import com.example.ticketing.exception.QueueUnavailableException;
import com.example.ticketing.queue.domain.QueueStatus;
import com.example.ticketing.queue.domain.QueueTicket;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import com.example.ticketing.queue.dto.QueueStatusSnapshot;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Component
@RequiredArgsConstructor
public class QueueRedisStore {
    // heartbeat는 polling마다 쓰지 않고 30초마다 갱신
    private static final Duration HEARTBEAT_WRITE_INTERVAL = Duration.ofSeconds(30);
    // EXPIRED, CANCELLED 상태 보존 기간
    private static final Duration TERMINAL_RETENTION = Duration.ofMinutes(10);

    // Redis 명령 실행용 객체
    private final StringRedisTemplate redisTemplate;

    // Lua: 대기열 추가 스크립트
    private static final DefaultRedisScript<String> JOIN_SCRIPT =
            new DefaultRedisScript<>("""
                    
                    
                    -- =====================================================
                    -- 1. 동일 사용자에게 기존 대기열 티켓이 있는지 확인
                    -- =====================================================
                   
                    local existingTicketId = redis.call('GET', KEYS[1])

                    -- 기존 티켓 아이디가 존재한다면
                    if existingTicketId then
                        -- ARGV[1] = queue:{sessionId}:ticket:{existingTicketId}
                        local existingTicketKey = ARGV[1] .. existingTicketId

                        -- 기존 티켓의 상태 조회
                        local existingStatus =
                            redis.call(
                                'HGET',
                                existingTicketKey,
                                'status'
                            )

                        -- WAITING, SELECTING, CHECKOUT은 아직 처리 중인 활성 티켓
                        if existingStatus == 'WAITING'
                            or existingStatus == 'SELECTING'
                            or existingStatus == 'CHECKOUT' then

                            -- 기존 대기 순번 조회
                            local existingWaitingNumber =
                                redis.call(
                                    'HGET',
                                    existingTicketKey,
                                    'waitingNumber'
                                )

                            -- 기존 티켓 생성 시간 조회
                            local existingCreatedAt =
                                redis.call(
                                    'HGET',
                                    existingTicketKey,
                                    'createdAt'
                                )
                    
                            -- SELECTING 시작 시각
                            local existingSelectingStartedAt =
                                redis.call(
                                    'HGET',
                                    existingTicketKey,
                                    'selectingStartedAt'
                                )
                    
                            -- SELECTING 만료 시각
                            local existingSelectingExpiresAt =
                                redis.call(
                                    'HGET',
                                    existingTicketKey,
                                    'selectingExpiresAt'
                                )
                    
                    
                            -- 필요한 데이터가 정상적으로 존재한다면 기존 티켓 정보를 반환
                            if existingWaitingNumber
                                and existingCreatedAt then

                                return existingTicketId
                                    .. '|'
                                    .. existingWaitingNumber
                                    .. '|'
                                    .. existingStatus
                                    .. '|'
                                    .. existingCreatedAt
                                    .. '|'
                                    .. (existingSelectingStartedAt or '')
                                    .. '|'
                                    .. (existingSelectingExpiresAt or '')
                            end
                        end

                        -- 사용자 -> 티켓 매핑은 존재하지만 실제 티켓이 없거나 활성 상태가 아니라면
                        -- 잘못 남아 있는 매핑을 삭제
                        redis.call('DEL', KEYS[1])
                    end
                    
                    
                    -- 기존 티켓아이디가 존재하지 않으면
                    -- =====================================================
                    -- 2. 새로운 대기 순번 발급 (회차별)
                    -- =====================================================
                    
                    -- key: queue:{sessionId}:sequence
                    -- value: INCR이 redis에서 원자적으로 매겨줌
                    local waitingNumber = redis.call('INCR', KEYS[2])


                    -- =====================================================
                    -- 3. 새로운 QueueTicket의 Redis Key 생성
                    -- =====================================================
                    
                    -- ARGV[1] = 티켓 Key prefix
                    -- ARGV[2] = 새 queueTicketId
                    local ticketKey = ARGV[1] .. ARGV[2]


                    -- =====================================================
                    -- 4. 티켓 정보를 Redis Hash에 저장
                    -- =====================================================
                    
                    redis.call(
                        'HSET',
                        ticketKey,
                        'queueTicketId', ARGV[2],
                        'userId', ARGV[3],
                        'eventId', ARGV[4],
                        'sessionId', ARGV[5],
                        'waitingNumber', waitingNumber,
                        'status', 'WAITING',
                        'createdAt', ARGV[6]
                        )


                    -- =====================================================
                    -- 5. 실제 대기열에 티켓 추가
                    -- =====================================================
                    
                    -- 회차별 대기열 Sorted Set
                    -- KEY: queue:{sessionId}:waiting
                    -- VALUE: Score=waitingNumber, Member=queueTicketId
                    redis.call(
                        'ZADD',
                        KEYS[3],
                        waitingNumber,
                        ARGV[2]
                    )
                    
                    
                    -- =====================================================
                    -- 6. 사용자 -> 티켓 ID 매핑 저장
                    -- =====================================================
                    
                    -- 동일 사용자에게 이미 티켓이 있는지 빠르게 찾기 위한 Key
                    -- KEY: queue:{sessionId}:user:{userId}
                    -- VALUE: queueTicketId
                    redis.call(
                        'SET',
                        KEYS[1],
                        ARGV[2],
                        'EX',
                        ARGV[7]
                    )
                    
                    
                    -- =====================================================
                    -- 7. 관련 Key에 만료 시간 설정
                    -- =====================================================

                    redis.call('EXPIRE', KEYS[2], ARGV[7])
                    redis.call('EXPIRE', KEYS[3], ARGV[7])
                    redis.call('EXPIRE', ticketKey, ARGV[7])


                    -- =====================================================
                    -- 8. 생성한 티켓 정보 반환
                    -- =====================================================
                    
                    -- ticketId|waitingNumber|WAITING|createdAt|lastSeenAt|selectingStartedAt|selectingExpiresAt
                    return ARGV[2]
                        .. '|'
                        .. waitingNumber
                        .. '|WAITING|'
                        .. ARGV[6]
                        .. '||'
                    """, String.class);

    /**
     * 동일 사용자·동일 회차의 티켓을 조회한다.
     */
    // 등록 시 활성 티켓만 반환
    public Optional<QueueTicket> findActiveTicket(
            Long userId,
            Long sessionId
    ) {
        return findTicketByUser(userId, sessionId)
                .filter(ticket -> ticket.status().isActive());
    }

    // 종료 상태를 포함한 모든 티켓 조회
    public Optional<QueueTicket> findTicketByUser(
            Long userId,
            Long sessionId
    ) {
        try {
            String queueTicketId =
                    redisTemplate.opsForValue().get(
                            QueueRedisKey.userTicket(
                                    sessionId,
                                    userId
                            )
                    );

            if (queueTicketId == null) {
                return Optional.empty();
            }

            Map<Object, Object> values =
                    redisTemplate.opsForHash().entries(
                            QueueRedisKey.ticket(
                                    sessionId,
                                    queueTicketId
                            )
                    );

            if (values.isEmpty()) {
                return Optional.empty();
            }

            // 상태 필터링을 하지 않으므로
            // WAITING, SELECTING, CHECKOUT,
            // EXPIRED, CANCELLED 모두 반환
            return Optional.of(toQueueTicket(values));

        } catch (DataAccessException | IllegalArgumentException exception) {
            throw new QueueUnavailableException(exception);
        }
    }

    /**
     * 기존 활성 티켓이 있으면 반환하고,
     * 없으면 새로운 티켓을 원자적으로 등록
     */
    public QueueTicket joinOrGet(
            QueueTicket candidate,
            Duration expiration
    ) {
        long expirationSeconds =
                Math.max(1L, expiration.toSeconds());

        Long sessionId = candidate.sessionId();


        List<String> keys = List.of(
                // KEYS[1]: 사용자 -> 티켓 ID 매핑
                QueueRedisKey.userTicket(
                        sessionId,
                        candidate.userId()
                ),
                // KEYS[2]: 회차별 순번 카운터
                QueueRedisKey.sequence(sessionId),
                // KEYS[3]: 실제 대기열 Sorted Set
                QueueRedisKey.waitingQueue(sessionId)
        );

        try {
            // Redis에서 Lua Script 실행
            // Redis가 이 스크립트 전체를 하나의 원자적인 작업처럼 처리
            String result = redisTemplate.execute(
                    JOIN_SCRIPT,
                    keys,

                    // ARGV[1]
                    QueueRedisKey.ticketPrefix(sessionId),

                    // ARGV[2]
                    candidate.queueTicketId(),

                    // ARGV[3]
                    candidate.userId().toString(),

                    // ARGV[4]
                    candidate.eventId().toString(),

                    // ARGV[5]
                    sessionId.toString(),

                    // ARGV[6]
                    candidate.createdAt().toString(),

                    // ARGV[7]
                    Long.toString(expirationSeconds)
            );

            // Lua Script의 결과 String을 QueueTicket 객체로 변환
            return parseJoinResult(result, candidate);

        } catch (DataAccessException | IllegalArgumentException exception) {
            throw new QueueUnavailableException(exception);
        }
    }

    // Lua Script에서 반환한 문자열을 QueueTicket으로 변환
    private QueueTicket parseJoinResult(
            String result,
            QueueTicket candidate
    ) {
        if (result == null) {
            throw new IllegalArgumentException(
                    "Redis 대기열 등록 결과가 없습니다."
            );
        }

        String[] fields = result.split("\\|", -1);

        if (fields.length != 6) {
            throw new IllegalArgumentException(
                    "Redis 대기열 등록 결과 형식이 올바르지 않습니다."
            );
        }

        return new QueueTicket(
                fields[0],
                candidate.userId(),
                candidate.eventId(),
                candidate.sessionId(),
                Long.parseLong(fields[1]),
                QueueStatus.valueOf(fields[2]),
                Instant.parse(fields[3]),
                optionalInstant(fields[4]),
                optionalInstant(fields[5])
        );
    }

    // Redis Hash 데이터를 QueueTicket Java 객체로 변환
    private QueueTicket toQueueTicket(
            Map<Object, Object> values
    ) {
        return new QueueTicket(
                value(values, "queueTicketId"),
                Long.valueOf(value(values, "userId")),
                Long.valueOf(value(values, "eventId")),
                Long.valueOf(value(values, "sessionId")),
                Long.parseLong(value(values, "waitingNumber")),
                QueueStatus.valueOf(value(values, "status")),
                Instant.parse(value(values, "createdAt")),
                optionalInstant(values, "selectingStartedAt"),
                optionalInstant(values, "selectingExpiresAt")
        );
    }

    private String value(
            Map<Object, Object> values,
            String key
    ) {
        Object value = values.get(key);

        if (value == null) {
            throw new IllegalArgumentException(
                    "Redis 대기열 티켓 필드가 없습니다: " + key
            );
        }

        return value.toString();
    }

    private Instant optionalInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return Instant.parse(value);
    }

    private Instant optionalInstant(
            Map<Object, Object> values,
            String key
    ) {
        Object value = values.get(key);

        if (value == null || value.toString().isBlank()) {
            return null;
        }

        return Instant.parse(value.toString());
    }


    // Lua: 대기열 상태와 대기열 순번 조회
    private static final DefaultRedisScript<String>
            STATUS_SNAPSHOT_SCRIPT =
            new DefaultRedisScript<>("""
                local status =
                    redis.call(
                        'HGET',
                        KEYS[1],
                        'status'
                    )

                if not status then
                    return '__TICKET_MISSING__'
                end

                if status == 'WAITING' then
                    local rank =
                        redis.call(
                            'ZRANK',
                            KEYS[2],
                            ARGV[1]
                        )

                    if not rank then
                        return '__WAITING_RANK_MISSING__'
                    end

                    return status
                        .. '|'
                        .. rank
                        .. '|'
                end

                if status == 'SELECTING' then
                    local selectingExpiresAt =
                        redis.call(
                            'HGET',
                            KEYS[1],
                            'selectingExpiresAt'
                        )

                    return status
                        .. '||'
                        .. (selectingExpiresAt or '')
                end

                return status .. '||'
                """, String.class);

    /**
     * 현재 상태, 대기열 순번, SELECTING 만료시간 조회
     */
    public QueueStatusSnapshot getStatusSnapshot(
            Long sessionId,
            String queueTicketId
    ) {
        try {
            String result = redisTemplate.execute(
                    STATUS_SNAPSHOT_SCRIPT,
                    List.of(
                            QueueRedisKey.ticket( // KEYS[1]
                                    sessionId,
                                    queueTicketId
                            ),
                            QueueRedisKey.waitingQueue(sessionId) // KEYS[2]
                    ),
                    queueTicketId // ARGV[1]
            );

            return parseStatusSnapshot(result);

        } catch (DataAccessException | IllegalArgumentException exception) {
            throw new QueueUnavailableException(exception);
        }
    }

    private QueueStatusSnapshot parseStatusSnapshot(
            String result
    ) {
        if (result == null) {
            throw new IllegalArgumentException(
                    "Redis 상태 조회 결과가 없습니다."
            );
        }

        if ("__TICKET_MISSING__".equals(result)) {
            throw new IllegalArgumentException(
                    "Redis 티켓 Hash가 없습니다."
            );
        }

        if ("__WAITING_RANK_MISSING__".equals(result)) {
            throw new IllegalArgumentException(
                    "WAITING 티켓이 대기열 ZSET에 없습니다."
            );
        }

        String[] fields = result.split("\\|", -1);

        if (fields.length != 3) {
            throw new IllegalArgumentException(
                    "Redis 상태 조회 결과 형식이 올바르지 않습니다."
            );
        }

        QueueStatus status = QueueStatus.valueOf(fields[0]);
        Long aheadCount = fields[1].isBlank() ? null : Long.valueOf(fields[1]);
        Instant selectingExpiresAt = fields[2].isBlank() ? null : Instant.parse(fields[2]);

        return new QueueStatusSnapshot(
                status,
                aheadCount,
                selectingExpiresAt
        );
    }


    // Lua: WAITING 상태 확인과 heartbeat 갱신 처리
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


                --heartbeat 데이터가 티켓보다 먼저 Redis에서 없어지지 않게 TTL설정
                -- heartbeat ZSET도 티켓과 비슷한 시점에 만료
                local ticketTtl =
                    redis.call('PTTL', KEYS[1])

                -- 해당 티켓에 정상적인 만료 시간이 설정되어있으면
                if ticketTtl > 0 then
                    local heartbeatTtl =
                        redis.call('PTTL', KEYS[2])

                    -- heartbeat가 티켓보다 먼저 만료될 예정이라면
                    if heartbeatTtl < ticketTtl then
                        -- heartbeat ZSET의 TTL을 티켓의 남은 TTL만큼 연장
                        redis.call(
                            'PEXPIRE',
                            KEYS[2],
                            ticketTtl
                        )
                    end
                end

                return 1
                """, Long.class);


    /**
     * 마지막 쓰기 후 30초가 지난 경우에만 heartbeat 갱신
     */
    public boolean touchWaitingHeartbeatIfNecessary(
            Long sessionId,
            String queueTicketId,
            Instant now
    ) {
        try {
            Long updated = redisTemplate.execute(
                    TOUCH_HEARTBEAT_SCRIPT,
                    List.of(
                            QueueRedisKey.ticket(
                                    sessionId,
                                    queueTicketId
                            ),
                            QueueRedisKey.waitingHeartbeat(
                                    sessionId
                            )
                    ),
                    queueTicketId,
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


    // Lua: 종료상태 (EXPIRED, CANCELLED) 전환 및 보존 TTL 설정
    private static final DefaultRedisScript<Long>
            TERMINAL_TRANSITION_SCRIPT =
            new DefaultRedisScript<>("""
                local currentStatus =
                    redis.call(
                        'HGET',
                        KEYS[1],
                        'status'
                    )

                if not currentStatus then
                    return 0
                end

                local targetStatus = ARGV[2]

                -- EXPIRED는 SELECTING 상태에서만 전환
                if targetStatus == 'EXPIRED' then
                    if currentStatus ~= 'SELECTING' then
                        return 0
                    end

                -- CANCELLED는 활성 상태에서만 전환
                elseif targetStatus == 'CANCELLED' then
                    if currentStatus ~= 'WAITING'
                        and currentStatus ~= 'SELECTING'
                        and currentStatus ~= 'CHECKOUT' then
                        return 0
                    end
                else
                    return -1
                end


                -- queue:{sessionId}:ticket:{queueTicketId}에서
                -- status 필드 내용 변경, terminalAt 필드 추가
                redis.call(
                    'HSET',
                    KEYS[1],
                    'status', targetStatus,
                    'terminalAt', ARGV[3]
                )

                -- queue:{sessionId}:ticket:{queueTicketId}에서 selectingExpiresAt 필드를 삭제
                redis.call(
                    'HDEL',
                    KEYS[1],
                    'selectingExpiresAt'
                )


                -- WAITING 상태였을 가능성에 대비해 대기열에서 티켓 제거
                -- KEYS[3] = waitingQueue
                redis.call(
                    'ZREM',
                    KEYS[3],
                    ARGV[1]
                )
                -- KEYS[4] = waitingHeartbeat
                redis.call(
                    'ZREM',
                    KEYS[4],
                    ARGV[1]
                )
                

                local retentionSeconds =
                    tonumber(ARGV[4])

                -- 종료 티켓 Hash 보존
                redis.call(
                    'EXPIRE',
                    KEYS[1],
                    retentionSeconds
                )

                -- 사용자 매핑이 현재 티켓을 가리킬 때만 보존 TTL 설정
                local mappedTicketId =
                    redis.call('GET', KEYS[2])

                if mappedTicketId == ARGV[1] then
                    redis.call(
                        'EXPIRE',
                        KEYS[2],
                        retentionSeconds
                    )
                end

                return 1
                """, Long.class);

    /**
     * 상태를 종료상태로 변경 (만료or취소)
     */
    private boolean transitionToTerminal(
            Long userId,
            Long sessionId,
            String queueTicketId,
            QueueStatus targetStatus,
            Instant now
    ) {
        try {
            Long transitioned = redisTemplate.execute(
                    TERMINAL_TRANSITION_SCRIPT,
                    List.of(
                            QueueRedisKey.ticket(
                                    sessionId,
                                    queueTicketId
                            ),
                            QueueRedisKey.userTicket(
                                    sessionId,
                                    userId
                            ),
                            QueueRedisKey.waitingQueue(
                                    sessionId
                            ),
                            QueueRedisKey.waitingHeartbeat(
                                    sessionId
                            )
                    ),
                    queueTicketId,
                    targetStatus.name(),
                    now.toString(),
                    Long.toString(
                            TERMINAL_RETENTION.toSeconds()
                    )
            );

            if (transitioned == null) {
                throw new IllegalArgumentException(
                        "Redis 종료 상태 전환 결과가 없습니다."
                );
            }

            if (transitioned == -1L) {
                throw new IllegalArgumentException(
                        "지원하지 않는 종료 상태입니다: "
                                + targetStatus
                );
            }

            return transitioned == 1L;

        } catch (DataAccessException | IllegalArgumentException exception) {
            throw new QueueUnavailableException(exception);
        }
    }

    // SELECTING이 끝난 경우 EXPIRED상태변경
    public boolean expireSelectingIfCurrent(
            Long userId,
            Long sessionId,
            String queueTicketId,
            Instant now
    ) {
        return transitionToTerminal(
                userId,
                sessionId,
                queueTicketId,
                QueueStatus.EXPIRED,
                now
        );
    }

    // 활성 티켓 취소
    public boolean cancelIfActive(
            Long userId,
            Long sessionId,
            String queueTicketId,
            Instant now
    ) {
        return transitionToTerminal(
                userId,
                sessionId,
                queueTicketId,
                QueueStatus.CANCELLED,
                now
        );
    }
}