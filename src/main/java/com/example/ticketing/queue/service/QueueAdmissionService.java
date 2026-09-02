package com.example.ticketing.queue.service;

import com.example.ticketing.queue.redis.QueueAdmissionRedisStore;
import com.example.ticketing.repository.SeatInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
@Slf4j
@Service
@RequiredArgsConstructor
public class QueueAdmissionService {

    private static final Duration SELECTING_TTL = Duration.ofMinutes(5); // 좌석 선택 제한 시간 5분
    private final QueueAdmissionRedisStore queueAdmissionRedisStore;
    private final SeatInstanceRepository seatInstanceRepository;


    public long admitNext(Long sessionId) {
        validateSessionId(sessionId);
        long seatCapacity = seatInstanceRepository.countBySession_Id(sessionId);


        if (seatCapacity <= 0L) { // 좌석 인스턴스가 없는 경우
            log.debug(
                    "대기열 입장 처리 대상 없음: " + "sessionId={}, seatCapacity=0",
                    sessionId
            );
            return 0L;
        }

        int maxSelectingUsers = (int) seatCapacity;

        // 상태변경: redis에게 위임
        // return 실제로 WAITING에서 SELECTING으로 변경된 사용자 수
        int admittedCount =
                queueAdmissionRedisStore.admitNext(
                        sessionId,
                        maxSelectingUsers,
                        SELECTING_TTL
                );

        if (admittedCount > 0) {
            log.info(
                    "대기열 입장 처리 완료: "
                            + "sessionId={}, "
                            + "capacity={}, "
                            + "selectingTtlSeconds={}, "
                            + "admittedCount={}",
                    sessionId,
                    maxSelectingUsers,
                    SELECTING_TTL.toSeconds(),
                    admittedCount
            );
        } else {
            log.debug(
                    "대기열 입장 처리 대상 없음: "
                            + "sessionId={}, "
                            + "capacity={}",
                    sessionId,
                    maxSelectingUsers
            );
        }

        return admittedCount;
    }

    private void validateSessionId(Long sessionId) {
        if (sessionId == null || sessionId <= 0L) {
            throw new IllegalArgumentException(
                    "sessionId는 1 이상이어야 합니다."
            );
        }
    }
}