package com.example.ticketing.queue;

import com.example.ticketing.exception.QueueUnavailableException;
import com.example.ticketing.queue.domain.QueueStatus;
import com.example.ticketing.queue.domain.QueueTicket;
import com.example.ticketing.queue.dto.AdmissionToken;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AdmissionTokenRedisStore {

    private static final String NOT_FOUND = "__NOT_FOUND__";
    private static final String NOT_SELECTING = "__NOT_SELECTING__";
    private static final String INCONSISTENT = "__INCONSISTENT__";
    private static final String TOKEN_HASH_COLLISION = "__TOKEN_HASH_COLLISION__";
    private static final String EXISTING = "EXISTING";
    private static final String CREATED = "CREATED";

    private final StringRedisTemplate redisTemplate;


    /**
     * 동일 티켓에 이미 토큰이 있다면 기존 토큰을 유지
     * 없다면 새 토큰 해시와 양방향 매핑을 저장
     */
    private static final DefaultRedisScript<String>
            SAVE_IF_ABSENT_SCRIPT =
            new DefaultRedisScript<>("""

                -- =========================================
                -- 이 대기열 티켓이 토큰을 발급 받을 수 있는 상태인지 검사
                -- =========================================
                
                local ticketStatus =
                    redis.call(
                        'HGET',
                        KEYS[3],
                        'status'
                    )

                if ticketStatus ~= 'SELECTING' then
                    return '__NOT_SELECTING__'
                end


                local currentSelectingExpiresAt =
                    redis.call(
                        'HGET',
                        KEYS[3],
                        'selectingExpiresAt'
                    )

                if not currentSelectingExpiresAt then
                    return '__INCONSISTENT__'
                end

                -- 상태 조회 이후 SELECTING 시간이 바뀐 경우 발급하지 않음
                if currentSelectingExpiresAt ~= ARGV[8] then
                    return '__NOT_SELECTING__'
                end


                -- Redis 서버 기준 현재시간 조회
                local redisTime = redis.call('TIME')

                local nowMillis =
                    tonumber(redisTime[1]) * 1000
                    + math.floor(
                        tonumber(redisTime[2]) / 1000
                    )


                local tokenExpiresAtMillis = tonumber(ARGV[6])
                local selectingExpiresAtMillis = tonumber(ARGV[7])

                if not tokenExpiresAtMillis
                    or not selectingExpiresAtMillis then
                    return '__INCONSISTENT__'
                end


                -- 입장 토큰은 SELECTING 만료시간을 넘을 수 없음
                if tokenExpiresAtMillis
                    > selectingExpiresAtMillis then
                    return '__INCONSISTENT__'
                end

                -- 토큰 만료시간이 현재시각 전일 때
                if tokenExpiresAtMillis <= nowMillis then
                    return '__NOT_SELECTING__'
                end


                -- =========================================
                -- 동일 티켓에 발급된 기존 토큰이 존재한다면 토큰 검사
                -- =========================================

                if redis.call('EXISTS', KEYS[1]) == 1 then

                    local existingHash =
                        redis.call(
                            'HGET',
                            KEYS[1],
                            'tokenHash'
                        )
                    local existingExpiresAt =
                        redis.call(
                            'HGET',
                            KEYS[1],
                            'expiresAt'
                        )
                    local existingExpiresAtMillis =
                        redis.call(
                            'HGET',
                            KEYS[1],
                            'expiresAtEpochMilli'
                        )
                    local existingSelectingExpiresAt =
                        redis.call(
                            'HGET',
                            KEYS[1],
                            'selectingExpiresAt'
                        )

                    if not existingHash
                        or not existingExpiresAt
                        or not existingExpiresAtMillis
                        or not existingSelectingExpiresAt then
                        return '__INCONSISTENT__'
                    end


                    -- 역방향 조회용 Key 생성
                    local existingReverseKey = ARGV[9] .. existingHash

                    local reverseTicketId =
                        redis.call(
                            'HGET',
                            existingReverseKey,
                            'queueTicketId'
                        )

                    -- 역방향 해시에서도 같은 대기열 티켓을 가리키고 있는지 검사
                    if reverseTicketId ~= ARGV[2] then
                        return '__INCONSISTENT__'
                    end


                    -- 기존 입장 토큰의 만료시간이 현재 시간보다 같거나 과거라면
                    -- 같거나 과거라면 이미 만료된 토큰이므로 정방향 Key와 역방향 Key를 삭제
                    if tonumber(existingExpiresAtMillis) <= nowMillis then

                        redis.call(
                            'DEL',
                            KEYS[1],
                            existingReverseKey
                        )

                    else

                        -- 기존 토큰 만료시간이 현재 SELECTING
                        -- 만료시간보다 늦으면 잘못된 데이터
                        if tonumber(existingExpiresAtMillis)
                            > selectingExpiresAtMillis then
                            return '__INCONSISTENT__'
                        end

                        -- 기존 토큰 발급 당시 SELECTING 시간과
                        -- 현재 SELECTING 시간이 다르면 잘못된 데이터
                        if existingSelectingExpiresAt
                            ~= currentSelectingExpiresAt then
                            return '__INCONSISTENT__'
                        end

                        -- 기존 토큰이 아직 유효하므로 새 토큰을 저장하지 않음
                        return 'EXISTING'
                    end
                end


                -- =========================================
                -- 기존 대기열 토큰이 존재하지 않는다면 새 토큰 발급
                -- =========================================

                -- 다른 토큰과 해시가 충돌한 경우
                if redis.call('EXISTS', KEYS[2]) == 1 then
                    return '__TOKEN_HASH_COLLISION__'
                end

                -- 티켓 ID → 토큰 Hash 매핑 (순방향 해시)
                -- queue:{sessionId}:admission:ticket:{queueTicketId}
                redis.call(
                    'HSET',
                    KEYS[1],
                    'tokenHash', ARGV[1],
                    'expiresAt', ARGV[5],
                    'expiresAtEpochMilli', ARGV[6],
                    'selectingExpiresAt', ARGV[8]
                )

                -- 토큰 해시 → 입장 정보 (역방향 해시)
                -- queue:{sessionId}:admission:token:{tokenHash}
                redis.call(
                    'HSET',
                    KEYS[2],
                    'queueTicketId', ARGV[2],
                    'userId', ARGV[3],
                    'eventId', ARGV[4],
                    'sessionId', ARGV[10],
                    'expiresAt', ARGV[5],
                    'expiresAtEpochMilli', ARGV[6],
                    'selectingExpiresAt', ARGV[8]
                )


                -- 두 방향 매핑을 같은 절대 시각에 만료시킴
                local ticketExpireResult =
                    redis.call(
                        'PEXPIREAT',
                        KEYS[1],
                        tokenExpiresAtMillis
                    )
                local tokenExpireResult =
                    redis.call(
                        'PEXPIREAT',
                        KEYS[2],
                        tokenExpiresAtMillis
                    )


                -- TTL 설정이 정상적으로 안 됐으면
                if ticketExpireResult ~= 1
                    or tokenExpireResult ~= 1 then

                    redis.call(
                        'DEL',
                        KEYS[1],
                        KEYS[2]
                    )

                    return '__INCONSISTENT__'
                end


                return 'CREATED'
                """, String.class);


    /**
     * 기존 토큰이 없으면 새 토큰을 저장한다.
     * Optional.of(candidate) -> 새 토큰이 실제로 생성됨
     * Optional.empty()-> 이미 유효한 토큰이 존재함
     */
    public Optional<AdmissionToken> saveIfAbsent(
            QueueTicket ticket,
            AdmissionToken candidate
    ) {
        validateIssuance(ticket, candidate);

        // 원문 토큰을 SHA-256으로 해시 (Redis에는 이 hash만 저장)
        String tokenHash = hashToken(candidate.value());
        Instant selectingExpiresAt = ticket.selectingExpiresAt();

        List<String> keys = List.of(

                // 티켓 ID → 토큰 Hash 매핑 (순방향)
                QueueRedisKey.admissionTicket(  // KEYS[1]
                        ticket.sessionId(),
                        ticket.queueTicketId()
                ),
                // 새 토큰 Hash → 입장 정보 (역방향)
                QueueRedisKey.admissionToken(   // KEYS[2]:
                        ticket.sessionId(),
                        tokenHash
                ),
                // QueueTicket Hash
                QueueRedisKey.ticket(           // KEYS[3]
                        ticket.sessionId(),
                        ticket.queueTicketId()
                )
        );


        try {
            String result = redisTemplate.execute(
                    SAVE_IF_ABSENT_SCRIPT,
                    keys,

                    tokenHash,                                              // ARGV[1]: 새 토큰 Hash
                    ticket.queueTicketId(),                                 // ARGV[2]: queueTicketId
                    ticket.userId().toString(),                             // ARGV[3]: userId
                    ticket.eventId().toString(),                            // ARGV[4]: eventId
                    candidate.expiresAt().toString(),                       // ARGV[5]: 토큰 만료시간
                    Long.toString(candidate.expiresAt().toEpochMilli()),    // ARGV[6]: 토큰 절대 만료시간
                    Long.toString(selectingExpiresAt.toEpochMilli()),       // ARGV[7]: SELECTING 절대 만료시간
                    selectingExpiresAt.toString(),                          // ARGV[8]: SELECTING 만료시간
                    QueueRedisKey.admissionTokenPrefix(ticket.sessionId()), // ARGV[9]: 토큰 Hash Key prefix
                    ticket.sessionId().toString()                           // ARGV[10]: sessionId
            );

            return parseSaveResult(
                    result,
                    candidate
            );

        } catch (DataAccessException |
                 IllegalArgumentException exception) {

            throw new QueueUnavailableException(exception);
        }
    }


    // Java에서 토큰 발급 가능 여부 미리 검사
    private void validateIssuance(
            QueueTicket ticket,
            AdmissionToken candidate
    ) {
        Objects.requireNonNull(
                ticket,
                "ticket은 필수입니다."
        );
        Objects.requireNonNull(
                candidate,
                "candidate는 필수입니다."
        );

        if (ticket.status() != QueueStatus.SELECTING) {
            throw new IllegalArgumentException(
                    "SELECTING 티켓에만 입장 토큰을 발급할 수 있습니다."
            );
        }

        Instant selectingExpiresAt =
                Objects.requireNonNull(
                        ticket.selectingExpiresAt(),
                        "selectingExpiresAt은 필수입니다."
                );

        if (candidate.expiresAt().isAfter(selectingExpiresAt)) {
            throw new IllegalArgumentException(
                    "입장 토큰 만료시간은 SELECTING 만료시간을 "
                            + "초과할 수 없습니다."
            );
        }
    }


    private Optional<AdmissionToken> parseSaveResult(
            String result,
            AdmissionToken candidate
    ) {
        if (result == null) {
            throw new IllegalArgumentException(
                    "Redis 입장 토큰 저장 결과가 없습니다."
            );
        }
        // 이미 유효한 토큰이 존재
        return switch (result) {
            case NOT_SELECTING -> throw new IllegalArgumentException(
                    "현재 티켓이 SELECTING 상태가 아닙니다."
            );
            case INCONSISTENT -> throw new IllegalArgumentException(
                    "Redis 입장 토큰 데이터가 일관되지 않습니다."
            );
            case TOKEN_HASH_COLLISION -> throw new IllegalArgumentException(
                    "입장 토큰 해시가 충돌했습니다."
            );
            case EXISTING -> Optional.empty();


            // 이번 요청에서 새 토큰이 실제 저장됐으므로
            // Java 메모리에 있는 candidate 원문을 클라이언트에 전달 가능
            case CREATED -> Optional.of(candidate);
            default -> throw new IllegalArgumentException(
                    "Redis 입장 토큰 저장 결과 형식이 올바르지 않습니다."
            );
        };
    }


    /**
     * 구역·좌석 API에서 사용할 입장 토큰 검증 스크립트.
     */
    private static final DefaultRedisScript<String>
            VERIFY_SCRIPT =
            new DefaultRedisScript<>("""

                if redis.call('EXISTS', KEYS[1]) == 0 then
                    return '__NOT_FOUND__'
                end


                local values =
                    redis.call(
                        'HMGET',
                        KEYS[1],
                        'queueTicketId',
                        'userId',
                        'eventId',
                        'sessionId',
                        'expiresAt',
                        'expiresAtEpochMilli',
                        'selectingExpiresAt'
                    )


                for index = 1, 7 do
                    if not values[index] then
                        return '__INCONSISTENT__'
                    end
                end


                -- 요청 사용자·공연·회차가 다르면 존재 여부를 숨김
                if values[2] ~= ARGV[1]
                    or values[3] ~= ARGV[2]
                    or values[4] ~= ARGV[3] then

                    return '__NOT_FOUND__'
                end


                local redisTime = redis.call('TIME')

                local nowMillis =
                    tonumber(redisTime[1]) * 1000
                    + math.floor(tonumber(redisTime[2]) / 1000)

                if tonumber(values[6]) <= nowMillis then
                    return '__NOT_FOUND__'
                end


                local queueTicketKey = ARGV[4] .. values[1]
                local currentStatus =
                    redis.call(
                        'HGET',
                        queueTicketKey,
                        'status'
                    )


                -- SELECTING 상태에서만 구역·좌석 API 접근 허용
                if currentStatus ~= 'SELECTING' then
                    return '__NOT_FOUND__'
                end


                local currentSelectingExpiresAt =
                    redis.call(
                        'HGET',
                        queueTicketKey,
                        'selectingExpiresAt'
                    )


                if currentSelectingExpiresAt ~= values[7] then
                    return '__NOT_FOUND__'
                end


                -- 티켓 → 토큰 매핑도 같은 토큰을 가리키는지 검사
                local ticketAdmissionKey = ARGV[5] .. values[1]
                local mappedTokenHash =
                    redis.call(
                        'HGET',
                        ticketAdmissionKey,
                        'tokenHash'
                    )


                if mappedTokenHash ~= ARGV[6] then
                    return '__NOT_FOUND__'
                end


                return values[1]
                    .. '|'
                    .. values[2]
                    .. '|'
                    .. values[3]
                    .. '|'
                    .. values[4]
                    .. '|'
                    .. values[5]
                """, String.class);


    /**
     * 구역·좌석 API에서 호출할 검증 메서드
     * 실패 사유를 구분하지 않고 Optional.empty()를 반환
     */
    public Optional<VerifiedAdmission> verify(
            String rawToken,
            Long expectedUserId,
            Long expectedEventId,
            Long expectedSessionId
    ) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }

        Objects.requireNonNull(
                expectedUserId,
                "expectedUserId는 필수입니다."
        );

        Objects.requireNonNull(
                expectedEventId,
                "expectedEventId는 필수입니다."
        );

        Objects.requireNonNull(
                expectedSessionId,
                "expectedSessionId는 필수입니다."
        );

        // 클라이언트가 가지고 있는 원문 토큰을
        // 서버에서 동일하게 SHA-256 해시
        String tokenHash = hashToken(rawToken);


        try {
            String result = redisTemplate.execute(
                    VERIFY_SCRIPT,

                    List.of(
                            // tokenHash로 역방향 조회
                            QueueRedisKey.admissionToken(
                                    expectedSessionId,
                                    tokenHash
                            )
                    ),

                    expectedUserId.toString(),                              // ARGV[1]: 요청 사용자 ID
                    expectedEventId.toString(),                             // ARGV[2]: 요청 공연 ID
                    expectedSessionId.toString(),                           // ARGV[3]: 요청 회차 ID
                    QueueRedisKey.ticketPrefix(expectedSessionId),          // ARGV[4]: QueueTicket Key prefix
                    QueueRedisKey.admissionTicketPrefix(expectedSessionId), // ARGV[5]: 티켓 → 토큰 매핑 Key prefix
                    tokenHash                                               // ARGV[6]: 검증할 토큰 Hash
            );


            return parseVerifyResult(result);

        } catch (DataAccessException |
                 IllegalArgumentException exception) {

            throw new QueueUnavailableException(exception);
        }
    }


    private Optional<VerifiedAdmission> parseVerifyResult(
            String result
    ) {
        if (result == null) {
            throw new IllegalArgumentException(
                    "Redis 입장 토큰 검증 결과가 없습니다."
            );
        }
        if (NOT_FOUND.equals(result)) {
            return Optional.empty();
        }
        if (INCONSISTENT.equals(result)) {
            throw new IllegalArgumentException(
                    "Redis 입장 정보가 일관되지 않습니다."
            );
        }


        String[] fields = result.split("\\|", -1);

        if (fields.length != 5) {
            throw new IllegalArgumentException(
                    "Redis 입장 토큰 검증 결과 형식이 올바르지 않습니다."
            );
        }


        return Optional.of(
                new VerifiedAdmission(
                        fields[0],
                        Long.valueOf(fields[1]),
                        Long.valueOf(fields[2]),
                        Long.valueOf(fields[3]),
                        Instant.parse(fields[4])
                )
        );
    }


    // 원문 토큰을 SHA-256으로 해시
    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hashed =
                    digest.digest(
                            rawToken.getBytes(StandardCharsets.UTF_8)
                    );

            return HexFormat.of().formatHex(hashed);

        } catch (NoSuchAlgorithmException exception) {

            throw new IllegalStateException(
                    "SHA-256 알고리즘을 사용할 수 없습니다.",
                    exception
            );
        }
    }


    // 검증 완료 후 좌석 서비스에서 사용할 내부 정보
    public record VerifiedAdmission(
            String queueTicketId,
            Long userId,
            Long eventId,
            Long sessionId,
            Instant expiresAt
    ) {
    }
}