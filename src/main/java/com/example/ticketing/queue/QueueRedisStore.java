package com.example.ticketing.queue;

import com.example.ticketing.exception.QueueUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class QueueRedisStore {

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

                        -- WAITING 또는 READY 상태이면 아직 사용할 수 있는 활성 티켓
                        if existingStatus == 'WAITING'
                            or existingStatus == 'READY' then

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
                    
                    -- ticketId|waitingNumber|WAITING|createdAt
                    return ARGV[2]
                        .. '|'
                        .. waitingNumber
                        .. '|WAITING|'
                        .. ARGV[6]
                    """, String.class);

    // Redis 명령 실행용 객체
    private final StringRedisTemplate redisTemplate;

    /**
     * 동일 사용자·동일 회차의 활성 티켓을 조회한다.
     */
    public Optional<QueueTicket> findActiveTicket(
            Long userId,
            Long sessionId
    ) {
        try {
            // 세션id+사용자id -> 대기열 티켓을 가지고 있는지 매핑
            String queueTicketId =
                    redisTemplate.opsForValue()
                            .get(
                                    QueueRedisKey.userTicket(
                                            sessionId,
                                            userId
                                    )
                            );

            if (queueTicketId == null) {
                return Optional.empty();
            }

            // queueTicketId를 이용해서 QueueTicket Hash를 조회
            Map<Object, Object> values =
                    redisTemplate.opsForHash()
                            .entries(
                                    QueueRedisKey.ticket(
                                            sessionId,
                                            queueTicketId
                                    )
                            );

            // 티켓 Hash가 없는 유효하지 않은 대기열 티켓인 경우 empty
            if (values.isEmpty()) {
                return Optional.empty();
            }

            // Redis Hash 데이터를 Java QueueTicket 객체로 변환
            QueueTicket ticket = toQueueTicket(values);

            // active상태가 아닌 경우 empty
            if (!ticket.status().isActive()) {
                return Optional.empty();
            }

            // 대기열 티켓 반환
            return Optional.of(ticket);

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

        if (fields.length != 4) {
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
                LocalDateTime.parse(fields[3])
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
                LocalDateTime.parse(value(values, "createdAt"))
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
}