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

}
