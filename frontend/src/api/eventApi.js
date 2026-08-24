import api from './api';

export const getEvents = async (page = 0) => {
    const response = await api.get('/events', {
        params: {
            page
        }
    });

    return response.data;
};