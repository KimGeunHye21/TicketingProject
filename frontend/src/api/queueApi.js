import api from './api';

// 진행 중인 중복 등록 요청 방지를 위한 map
const pendingJoins = new Map();

// 대기열 등록
export function joinQueue(eventId, sessionId) {
    const key = `${eventId}:${sessionId}`;

    // 이미 같은 요청이 진행 중이면 기존 Promise 재사용
    if (pendingJoins.has(key)) {
        return pendingJoins.get(key);
    }

    const request = api.post(`/queue/events/${eventId}/sessions/${sessionId}`).then(response => response.data).finally(() => { pendingJoins.delete(key); });

    pendingJoins.set(key, request);

    return request;
}

// 대기열 상태 조회
export async function getQueueStatus(eventId, sessionId) {
    const response = await api.get(`/queue/events/${eventId}/sessions/${sessionId}`);
    return response.data;
}