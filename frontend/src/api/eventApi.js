import api from './api';

// 공연 리스트
export const getEvents = async (page = 0) => {
    const response = await api.get('/events', { params: { page } });

    return response.data;
};

// 공연 상세 조회
export const getEventDetail = async (eventId, signal) => {
    const response = await api.get(`/events/${eventId}`, { signal });

    return response.data;
};

// 공연 구역 배치도 조회
export const getSectionMap = async (eventId, sessionId, signal) => {
    const response = await api.get(`/events/${eventId}/sessions/${sessionId}/sections`, { signal });
    return response.data;
};

// 선택한 구역의 좌석 조회
export const getSectionSeats = async (eventId, sessionId, sectionId, signal) => {
    const response = await api.get(`/events/${eventId}/sessions/${sessionId}/seats`, {
        params: { sectionId },
        signal
    });
    return response.data;
};

// 선택한 구역의 좌석 상태 조회
export const getSeatStatuses = async (eventId, sessionId, sectionId, signal) => {
    const response = await api.get(`/seat-holds/status/event/${eventId}/sessions/${sessionId}`, {
        params: { sectionId },
        signal,
        timeout: 10000
    });
    return response.data;
};
