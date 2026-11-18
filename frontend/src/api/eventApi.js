import api from './api';

// 공연 리스트
export const getEvents = async (page = 0) => {
    const response = await api.get('/events', { params: { page } });

    return response.data;
};

// 공연 상세 조회
export const getEventDetail = async (eventId) => {
    const response = await api.get(`/events/${eventId}`);

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
