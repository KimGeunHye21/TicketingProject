package com.example.ticketing.queue.scheduler;

import com.example.ticketing.exception.queue.QueueUnavailableException;
import com.example.ticketing.queue.service.QueueAdmissionService;
import com.example.ticketing.repository.EventSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueueAdmissionScheduler {

    private final EventSessionRepository eventSessionRepository;
    private final QueueAdmissionService queueAdmissionService;

    // fixedDelay 정책: 이전 실행이 끝난 뒤 설정된 시간만큼 기다렸다가 다음 실행을 시작
    @Scheduled(
            fixedDelayString = "${queue.admission.scheduler.fixed-delay-ms:1000}",
            initialDelayString = "${queue.admission.scheduler.initial-delay-ms:1000}"
    )
    public void admitWaitingUsers() {
        LocalDateTime startedAt = LocalDateTime.now();
        List<Long> activeSessionIds;

        try {
            activeSessionIds = eventSessionRepository.findActiveQueueSessionIds(startedAt);

        } catch (RuntimeException exception) {
            log.error(
                    "대기열 활성 회차 조회 실패",
                    exception
            );

            return;
        }
        if (activeSessionIds.isEmpty()) {
            log.debug(
                    "대기열 입장 처리 대상 회차 없음"
            );

            return;
        }

        long totalAdmittedCount = 0L;
        int successfulSessionCount = 0;
        int failedSessionCount = 0;

        for (Long sessionId : activeSessionIds) {
            try {
                long admittedCount = queueAdmissionService.admitNext(sessionId);

                totalAdmittedCount += admittedCount;
                successfulSessionCount++;

            } catch (QueueUnavailableException exception) {
                failedSessionCount++;

                log.warn(
                        "회차 대기열 Redis 입장 처리 실패: "
                                + "sessionId={}",
                        sessionId
                );

            } catch (RuntimeException exception) {
                failedSessionCount++;

                log.error(
                        "회차 대기열 입장 처리 실패: "
                                + "sessionId={}",
                        sessionId,
                        exception
                );
            }
        }

        /*
         * 1초마다 실행되므로 아무도 입장하지 않은 정상 실행은
         * DEBUG로 기록해 INFO 로그 증가를 방지합
         */
        if (totalAdmittedCount > 0L || failedSessionCount > 0) {

            log.info(
                    "대기열 입장 스케줄 완료: "
                            + "activeSessions={}, "
                            + "successfulSessions={}, "
                            + "failedSessions={}, "
                            + "admittedUsers={}",
                    activeSessionIds.size(),
                    successfulSessionCount,
                    failedSessionCount,
                    totalAdmittedCount
            );

        } else {
            log.debug(
                    "대기열 입장 스케줄 완료: "
                            + "activeSessions={}, "
                            + "admittedUsers=0",
                    activeSessionIds.size()
            );
        }
    }
}
