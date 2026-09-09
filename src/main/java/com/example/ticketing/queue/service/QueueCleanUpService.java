package com.example.ticketing.queue.service;

import com.example.ticketing.queue.redis.QueueHeartbeatRedisStore;
import com.example.ticketing.queue.redis.QueueRedisStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueCleanUpService {

    // 마지막 heartbeat 이후 1분이 지난 WAITING 티켓을 취소
    private static final Duration STALE_WAITING_TIMEOUT = Duration.ofMinutes(1);
    // Lua 실행이 Redis를 너무 오래 점유하지 않도록
    private static final int STALE_WAITING_BATCH_SIZE = 500;
    // ZSCAN의 COUNT: 대강 Redis가 한 번에 검사할 개수
    private static final int DANGLING_WAITING_SCAN_COUNT = 1_000;

    private final QueueHeartbeatRedisStore queueHeartbeatRedisStore;
    private final QueueRedisStore queueRedisStore;


    // 하트비트가 끊긴 대기열티켓 취소
    public int cancelStaleWaiting(Long sessionId) {
        int cancelledCount =
                queueHeartbeatRedisStore.cancelStaleWaiting(
                        sessionId,
                        STALE_WAITING_TIMEOUT,
                        STALE_WAITING_BATCH_SIZE
                );

        if (cancelledCount > 0) {
            log.info(
                    "heartbeat 만료 WAITING 정리 완료: "
                            + "sessionId={}, cancelledCount={}",
                    sessionId,
                    cancelledCount
            );
        }

        // @return: 실제 WAITING → CANCELLED 처리된 티켓 수
        return cancelledCount;
    }

    // 정합성에 맞지 않는 대기열 티켓 정리
    public int cleanupDanglingWaitingMembers(Long sessionId) {
        int removedCount =
                queueRedisStore.cleanupDanglingWaitingMembers(
                        sessionId,
                        DANGLING_WAITING_SCAN_COUNT
                );

        if (removedCount > 0) {
            log.info(
                    "잘못 남은 WAITING member 정리 완료: "
                            + "sessionId={}, removedCount={}",
                    sessionId,
                    removedCount
            );
        }

        // @return 정합성 불일치로 제거한 member 수
        return removedCount;
    }
}
