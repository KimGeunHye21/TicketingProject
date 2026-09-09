package com.example.ticketing.queue.scheduler;

import com.example.ticketing.exception.queue.QueueUnavailableException;
import com.example.ticketing.queue.service.QueueCleanUpService;
import com.example.ticketing.repository.EventSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.ToIntFunction;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueueCleanUpScheduler {
    private final EventSessionRepository eventSessionRepository;
    private final QueueCleanUpService queueCleanupService;

    /**
     * heartbeat 만료 WAITING 정리
     */
    @Scheduled(
            fixedDelayString = "${queue.cleanup.heartbeat.fixed-delay-ms:60000}",
            initialDelayString = "${queue.cleanup.heartbeat.initial-delay-ms:60000}"
    )
    public void cancelStaleWaiting() {
        runForActiveSessions(
                "heartbeat 만료 WAITING 정리",
                queueCleanupService::cancelStaleWaiting
        );
    }

    /**
     * 잘못 남은 waiting ZSET member 정리
     */
    @Scheduled(
            fixedDelayString = "${queue.cleanup.dangling.fixed-delay-ms:300000}",
            initialDelayString = "${queue.cleanup.dangling.initial-delay-ms:300000}"
    )
    public void cleanupDanglingWaitingMembers() {
        runForActiveSessions(
                "dangling WAITING member 정리",
                queueCleanupService::cleanupDanglingWaitingMembers
        );
    }

    private void runForActiveSessions(
            String cleanupName,
            ToIntFunction<Long> cleanupOperation
    ) {
        List<Long> activeSessionIds;

        try {
            activeSessionIds =
                    eventSessionRepository.findActiveQueueSessionIds(
                            LocalDateTime.now()
                    );
        } catch (RuntimeException exception) {
            log.error(
                    "{}: 활성 회차 조회 실패",
                    cleanupName,
                    exception
            );

            return;
        }

        if (activeSessionIds.isEmpty()) {
            log.debug(
                    "{}: 정리 대상 활성 회차 없음",
                    cleanupName
            );

            return;
        }


        long totalCleanedCount = 0L;
        int failedSessionCount = 0;

        for (Long sessionId : activeSessionIds) {
            try {
                totalCleanedCount += cleanupOperation.applyAsInt(sessionId);

            } catch (QueueUnavailableException exception) {
                failedSessionCount++;

                log.warn(
                        "{} Redis 처리 실패: sessionId={}",
                        cleanupName,
                        sessionId
                );

            } catch (RuntimeException exception) {
                failedSessionCount++;

                log.error(
                        "{} 처리 실패: sessionId={}",
                        cleanupName,
                        sessionId,
                        exception
                );
            }
        }

        if (totalCleanedCount > 0L || failedSessionCount > 0) {
            log.info(
                    "{} 완료: activeSessions={}, "
                            + "failedSessions={}, cleanedCount={}",
                    cleanupName,
                    activeSessionIds.size(),
                    failedSessionCount,
                    totalCleanedCount
            );
        } else {
            log.debug(
                    "{} 완료: activeSessions={}, cleanedCount=0",
                    cleanupName,
                    activeSessionIds.size()
            );
        }
    }
}
