package com.example.ticketing.service;

import com.example.ticketing.domain.Event;
import com.example.ticketing.domain.EventSession;
import com.example.ticketing.domain.SeatInstance;
import com.example.ticketing.dto.hold.SeatHoldResponse;
import com.example.ticketing.dto.hold.SeatStatusResponse;
import com.example.ticketing.exception.BookingNotOpenException;
import com.example.ticketing.exception.EventNotFoundException;
import com.example.ticketing.exception.EventSessionNotFoundException;
import com.example.ticketing.queue.service.AdmissionTokenService;
import com.example.ticketing.repository.EventRepository;
import com.example.ticketing.repository.EventSessionRepository;
import com.example.ticketing.repository.SeatInstanceRepository;
import com.example.ticketing.repository.SectionRepository;
import com.example.ticketing.hold.redis.SeatHoldRedisStore;
import com.example.ticketing.security.AdmissionCookieFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HoldService {
    private final EventRepository eventRepository;
    private final EventSessionRepository eventSessionRepository;
    private final SeatInstanceRepository seatInstanceRepository;
    private final SectionRepository sectionRepository;
    private final SeatHoldRedisStore seatHoldRedisStore;
    private final AdmissionTokenService admissionTokenService;
    private final AdmissionCookieFactory admissionCookieFactory;

    // 좌석 선점
    public SeatHoldResponse holdSeat(Long userId, Long seatInstanceId, Map<String, String> cookies) {
        SeatInstance seatInstance = seatInstanceRepository.findWithSessionById(seatInstanceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "좌석을 찾을 수 없습니다."));
        EventSession session = seatInstance.getSession();
        Event event = session.getEvent();
        validateBookingTime(event, session);
        verifyAdmission(cookies, userId, event.getId(), session.getId());

        SeatHoldRedisStore.HoldResult result;
        try {
            result = seatHoldRedisStore.holdOrGet(
                    userId, session.getId(), seatInstance.getId(), event.getMaxTicketPerUser()
            );
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "좌석 선점 처리에 실패했습니다. 잠시 후 다시 시도해 주세요.",
                    exception
            );
        }

        return switch (result.code()) {
            case SUCCESS -> SeatHoldResponse.from(result);
            case SEAT_UNAVAILABLE -> throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "이미 선택되었거나 예약된 좌석입니다."
            );
            case LIMIT_EXCEEDED -> throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "최대 예매 가능 매수를 초과했습니다."
            );
            case NOT_READY -> throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "좌석 정보를 준비 중입니다. 잠시 후 다시 시도해 주세요."
            );
            case INVALID_ARGUMENT -> throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "좌석 선점 처리 값이 올바르지 않습니다."
            );
        };
    }

    // 구역 전체 좌석 상태 조회
    public List<SeatStatusResponse> getSeatStatuses(Long eventId, Long sessionId, Long sectionId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        EventSession session = eventSessionRepository.findByIdAndEvent_Id(sessionId, eventId)
                .orElseThrow(() -> new EventSessionNotFoundException(eventId, sessionId));
        validateBookingTime(event, session);

        if (sectionId != null && !sectionRepository.existsByIdAndEvent_Id(sectionId, eventId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "공연에 속한 구역을 찾을 수 없습니다.");
        }


        List<Long> seatInstanceIds = seatInstanceRepository.findIdsBySessionAndSection(sessionId, sectionId);
        SeatHoldRedisStore.SeatStatusResult result;
        try {
            result = seatHoldRedisStore.getSeatStatuses(sessionId, seatInstanceIds);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "좌석 상태 조회에 실패했습니다. 잠시 후 다시 시도해 주세요.",
                    exception
            );
        }

        if (result.code() == SeatHoldRedisStore.ResultCode.NOT_READY) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "좌석 정보를 준비 중입니다. 잠시 후 다시 시도해 주세요."
            );
        }
        if (!result.success()) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "좌석 상태 조회 결과가 올바르지 않습니다."
            );
        }
        return result.seats().stream().map(SeatStatusResponse::from).toList();
    }

    private void validateBookingTime(Event event, EventSession session) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(event.getBookingOpenAt())) {
            throw BookingNotOpenException.beforeOpen(event.getBookingOpenAt());
        }
        if (!now.isBefore(session.getStartAt())) {
            throw BookingNotOpenException.sessionStarted(session.getStartAt());
        }
    }

    private void verifyAdmission(Map<String, String> cookies, Long userId, Long eventId, Long sessionId) {
        String rawToken = cookies.get(admissionCookieFactory.cookieName(sessionId));
        admissionTokenService.verifyOrThrow(rawToken, userId, eventId, sessionId);
    }

}
