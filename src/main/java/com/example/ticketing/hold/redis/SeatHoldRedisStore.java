package com.example.ticketing.hold.redis;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Component
public class SeatHoldRedisStore {
    private static final long HOLD_TTL_MILLIS = Duration.ofMinutes(5).toMillis();
    private final StringRedisTemplate redisTemplate;
    private final JsonMapper jsonMapper;

    public SeatHoldRedisStore(StringRedisTemplate redisTemplate, JsonMapper jsonMapper) {
        this.redisTemplate = redisTemplate;
        this.jsonMapper = jsonMapper;
    }

    private static final DefaultRedisScript<String> HOLD_SCRIPT =
            new DefaultRedisScript<>("""
                    -- KEYS[1]: 회차별 reservations Hash
                    -- KEYS[2]: 해당 좌석의 hold String
                    -- KEYS[3]: 해당 사용자의 holds Sorted Set
                    -- ARGV[1]: userId
                    -- ARGV[2]: seatInstanceId
                    -- ARGV[3]: 새 holdId
                    -- ARGV[4]: 최대 예매 매수
                    -- ARGV[5]: 선점 시간(ms)
                    
                    local function result(code, hold)
                        if not hold then
                            return cjson.encode({code = code})
                        end
                        return cjson.encode({
                            code = code,
                            holdId = hold.holdId,
                            seatInstanceId = hold.seatInstanceId,
                            expiresAt = hold.expiresAt
                        })
                    end
                    
                    if #KEYS ~= 3 or #ARGV ~= 5 then
                        return result('INVALID_ARGUMENT')
                    end
                    
                    local userId = ARGV[1]
                    local seatInstanceId = ARGV[2]
                    local holdId = ARGV[3]
                    local maxTickets = tonumber(ARGV[4])
                    local ttlMillis = tonumber(ARGV[5])
                    
                    if userId == '' or seatInstanceId == '' or holdId == ''
                        or not maxTickets or maxTickets < 1 or maxTickets % 1 ~= 0
                        or not ttlMillis or ttlMillis < 1 or ttlMillis > 300000
                        or ttlMillis % 1 ~= 0 then
                        return result('INVALID_ARGUMENT')
                    end


                    -- 예약 정보가 준비되지 않았다면 선점을 허용X
                    if redis.call('HGET', KEYS[1], 'ready') ~= '1' then
                        return result('NOT_READY')
                    end
                    -- 이미 예약 완료된 좌석인지
                    if redis.call('HEXISTS', KEYS[1], 'seat:' .. seatInstanceId) == 1 then
                        return result('SEAT_UNAVAILABLE')
                    end
                    
                    local serverTime = redis.call('TIME')
                    local now = tonumber(serverTime[1]) * 1000
                        + math.floor(tonumber(serverTime[2]) / 1000)
                    
                    -- 해당 좌석에 기존 선점이 있는지?
                    local existingValue = redis.call('GET', KEYS[2])
                    if existingValue then
                        local ok, existingHold = pcall(cjson.decode, existingValue)
                        if not ok or type(existingHold) ~= 'table'
                            or existingHold.seatInstanceId ~= seatInstanceId
                            or type(existingHold.userId) ~= 'string'
                            or type(existingHold.holdId) ~= 'string'
                            or type(existingHold.expiresAt) ~= 'number' then
                            return result('NOT_READY')
                        end
                    
                        -- 이 좌석의 선점이 아직 hold상태라면
                        if existingHold.expiresAt > now then
                    
                            -- 다른 사용자가 선점 시도 -> 좌석 사용x 반환
                            if existingHold.userId ~= userId then
                                return result('SEAT_UNAVAILABLE')
                            end
                            -- 기존 사용자가 선점 시도 -> 선점의 만료 시각과 TTL을 유지
                            return result('SUCCESS', existingHold)
                        end
                    end
                    

                    -- 사용자가 예약 한 티켓 매수 확인
                    local reservedCountValue = redis.call(
                        'HGET', KEYS[1], 'user:' .. userId .. ':count'
                    )
                    local reservedCount = tonumber(reservedCountValue or '0')
                    if not reservedCount or reservedCount < 0 or reservedCount % 1 ~= 0 then
                        return result('NOT_READY')
                    end
                    
                    -- 만료된 항목을 제거한 뒤 유효한 선점만 계산
                    redis.call('ZREMRANGEBYSCORE', KEYS[3], '-inf', now)
                    local heldCount = redis.call('ZCARD', KEYS[3])
                    if reservedCount + heldCount + 1 > maxTickets then
                        return result('LIMIT_EXCEEDED')
                    end

                    -- 모든 검사 통과됨 -> 실제 hold생성
                    local expiresAt = now + ttlMillis
                    local hold = {
                        holdId = holdId,
                        seatInstanceId = seatInstanceId,
                        userId = userId,
                        expiresAt = expiresAt
                    }
                    local encodedHold = cjson.encode(hold)
                    redis.call('ZADD', KEYS[3], expiresAt, seatInstanceId)
                    
                    -- 사용자 목록은 가장 늦게 만료되는 선점까지 유지
                    local latestHold = redis.call('ZRANGE', KEYS[3], -1, -1, 'WITHSCORES')
                    redis.call('PEXPIREAT', KEYS[3], latestHold[2])
                    redis.call('SET', KEYS[2], encodedHold, 'PXAT', expiresAt)
                    
                    return result('SUCCESS', hold)
                    """, String.class);

    // 좌석을 선점하거나 동일 사용자의 기존 선점을 반환한다.
    public HoldResult holdOrGet(
            Long userId,
            Long sessionId,
            Long seatInstanceId,
            int maxTickets
    ) {
        validateArguments(userId, sessionId, seatInstanceId, maxTickets);
        List<String> keys = List.of(
                SeatHoldRedisKey.reservations(sessionId),
                SeatHoldRedisKey.hold(sessionId, seatInstanceId),
                SeatHoldRedisKey.userHolds(sessionId, userId)
        );
        String holdId = UUID.randomUUID().toString();

        try {
            String result = redisTemplate.execute(
                    HOLD_SCRIPT,
                    keys,
                    userId.toString(),
                    seatInstanceId.toString(),
                    holdId,
                    Integer.toString(maxTickets),
                    Long.toString(HOLD_TTL_MILLIS)
            );
            return parseResult(result, seatInstanceId);
        } catch (DataAccessException | JacksonException exception) {
            throw new IllegalStateException("Redis 좌석 선점 처리에 실패했습니다.", exception);
        }
    }

    private void validateArguments(
            Long userId,
            Long sessionId,
            Long seatInstanceId,
            int maxTickets
    ) {
        if (userId == null || userId <= 0
                || sessionId == null || sessionId <= 0
                || seatInstanceId == null || seatInstanceId <= 0
                || maxTickets <= 0) {
            throw new IllegalArgumentException("좌석 선점 요청 값이 올바르지 않습니다.");
        }
    }

    private HoldResult parseResult(String value, Long requestedSeatInstanceId) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Redis 좌석 선점 결과가 없습니다.");
        }
        HoldResult result = jsonMapper.readValue(value, HoldResult.class);
        if (result == null || result.code() == null) {
            throw new IllegalStateException("Redis 좌석 선점 결과 코드가 없습니다.");
        }
        if (result.success() && (result.holdId() == null || result.holdId().isBlank()
                || !requestedSeatInstanceId.equals(result.seatInstanceId())
                || result.expiresAt() == null || result.expiresAt() <= 0)) {
            throw new IllegalStateException("Redis 좌석 선점 성공 결과가 올바르지 않습니다.");
        }
        return result;
    }

    public record HoldResult(
            ResultCode code,
            String holdId,
            Long seatInstanceId,
            Long expiresAt
    ) {
        public boolean success() {
            return code == ResultCode.SUCCESS;
        }
    }



    private static final DefaultRedisScript<String> SEAT_STATUS_SCRIPT =
        new DefaultRedisScript<>("""
            -- KEYS[1]: 회차별 예약 Hash
            -- KEYS[2..N]: 좌석별 선점 키
            -- ARGV[1..N-1]: 각 선점 키에 대응하는 좌석 ID
            local function failure(code)
                return cjson.encode({code = code})
            end

            if #KEYS ~= #ARGV + 1 then
                return failure('INVALID_ARGUMENT')
            end
            if redis.call('HGET', KEYS[1], 'ready') ~= '1' then
                return failure('NOT_READY')
            end
            if #ARGV == 0 then
                return '{"code":"SUCCESS","seats":[]}'
            end

            local serverTime = redis.call('TIME')
            local now = tonumber(serverTime[1]) * 1000
                + math.floor(tonumber(serverTime[2]) / 1000)
            
            local seats = {}
            for i = 2, #KEYS do
                local seatInstanceId = ARGV[i - 1]
                local status = 'AVAILABLE'
            
                ---- 예약 완료된 좌석이면 RESERVED
                if redis.call('HEXISTS', KEYS[1], 'seat:' .. seatInstanceId) == 1 then
                    status = 'RESERVED'
                else
                    -- 예약되지 않았다면 현재 선점 여부 확인
                    local holdValue = redis.call('GET', KEYS[i])
                    if holdValue then
            
                        local ok, hold = pcall(cjson.decode, holdValue)
                        -- 선점 데이터가 깨져 있으면 상태를 신뢰할 수 없음
                        if not ok or type(hold) ~= 'table'
                            or hold.seatInstanceId ~= seatInstanceId
                            or type(hold.expiresAt) ~= 'number' then
                            return failure('NOT_READY')
                        end
            
                        ---- 아직 만료되지 않은 선점이면 HELD
                        if hold.expiresAt > now then
                            status = 'HELD'
                        end
                    end
                end

                seats[#seats + 1] = {seatInstanceId = seatInstanceId, status = status}
            end
            return cjson.encode({code = 'SUCCESS', seats = seats})
            """, String.class);

    // 전달받은 좌석 목록의 상태를 한 번에 조회
    public SeatStatusResult getSeatStatuses(Long sessionId, List<Long> seatInstanceIds) {
        if (sessionId == null || sessionId <= 0 || seatInstanceIds == null
                || seatInstanceIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("좌석 상태 조회 요청 값이 올바르지 않습니다.");
        }

        List<Long> ids = seatInstanceIds.stream().distinct().toList();
        List<String> keys = new ArrayList<>(ids.size() + 1);
        List<String> arguments = new ArrayList<>(ids.size());
        keys.add(SeatHoldRedisKey.reservations(sessionId));

        for (Long seatInstanceId : ids) {
            keys.add(SeatHoldRedisKey.hold(sessionId, seatInstanceId));
            arguments.add(seatInstanceId.toString());
        }

        try {
            String value = redisTemplate.execute(SEAT_STATUS_SCRIPT, keys, arguments.toArray());
            return parseSeatStatusResult(value, ids);
        } catch (DataAccessException | JacksonException exception) {
            throw new IllegalStateException("Redis 좌석 상태 조회에 실패했습니다.", exception);
        }
    }

    private SeatStatusResult parseSeatStatusResult(String value, List<Long> requestedIds) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Redis 좌석 상태 조회 결과가 없습니다.");
        }
        SeatStatusResult result = jsonMapper.readValue(value, SeatStatusResult.class);
        if (result == null || result.code() == null) {
            throw new IllegalStateException("Redis 좌석 상태 조회 결과 코드가 없습니다.");
        }
        if (result.code() != ResultCode.SUCCESS) {
            return new SeatStatusResult(result.code(), List.of());
        }
        if (result.seats() == null || result.seats().size() != requestedIds.size()) {
            throw new IllegalStateException("Redis 좌석 상태 조회 결과 개수가 올바르지 않습니다.");
        }
        for (int i = 0; i < requestedIds.size(); i++) {
            SeatStatusInfo seat = result.seats().get(i);
            if (seat == null || !requestedIds.get(i).equals(seat.seatInstanceId())
                    || seat.status() == null) {
                throw new IllegalStateException("Redis 좌석 상태 조회 결과가 올바르지 않습니다.");
            }
        }
        return result;
    }

    public record SeatStatusInfo(Long seatInstanceId, SeatState status) {
    }

    public record SeatStatusResult(ResultCode code, List<SeatStatusInfo> seats) {
        public boolean success() {
            return code == ResultCode.SUCCESS;
        }
    }

    public enum SeatState {
        AVAILABLE,
        HELD,
        RESERVED
    }

    // 조회의 결과를 나타내기 위한 code
    public enum ResultCode {
        SUCCESS,
        SEAT_UNAVAILABLE,
        LIMIT_EXCEEDED,
        NOT_READY,
        INVALID_ARGUMENT
    }


}
