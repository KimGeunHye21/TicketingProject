import api from './api';

// 공연 리스트
export const getEvents = async (page = 0) => {
    const response = await api.get('/events', {
        params: {
            page
        }
    });

    return response.data;
};

// 공연 상세 조회
export const getEventDetail = async (eventId) => {

    const response = await api.get(
        `/events/${eventId}`
    );

    return response.data;
};