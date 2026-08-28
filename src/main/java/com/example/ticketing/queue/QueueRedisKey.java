package com.example.ticketing.queue;

public final class QueueRedisKey {

    private QueueRedisKey() {
    }

    /**
     * 동일 사용자·동일 회차의 티켓 ID
     *
     * Value: queueTicketId
     */
    public static String userTicket(
            Long sessionId,
            Long userId
    ) {
        return prefix(sessionId)
                + "user:"
                + userId;
    }

    /**
     * 회차별 대기 순번 카운터
     *
     * Value: Long
     */
    public static String sequence(Long sessionId) {
        return prefix(sessionId) + "sequence";
    }

    /**
     * 회차별 대기열 Sorted Set
     *
     * Member: queueTicketId
     * Score: waitingNumber
     */
    public static String waitingQueue(Long sessionId) {
        return prefix(sessionId) + "waiting";
    }

    /**
     * 개별 대기열 티켓 Hash
     */
    public static String ticket(
            Long sessionId,
            String queueTicketId
    ) {
        return ticketPrefix(sessionId)
                + queueTicketId;
    }

    /**
     * Lua 스크립트에서 티켓 키를 만들 때 사용하는 접두사
     */
    public static String ticketPrefix(Long sessionId) {
        return prefix(sessionId) + "ticket:";
    }

    private static String prefix(Long sessionId) {
        return "queue:{"
                + sessionId
                + "}:";
    }

    /**
     * WAITING 사용자의 마지막 heartbeat 시간 저장
     */
    public static String waitingHeartbeat(Long sessionId) {
        return prefix(sessionId) + "heartbeat";
    }
}