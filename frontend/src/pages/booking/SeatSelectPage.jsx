import { useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import { getEventDetail, getSectionSeats, getSeatStatuses } from '../../api/eventApi';
import '../../styles/bookingMap.css';

const STATE_LABEL = { AVAILABLE: '예매 가능', HELD: '선점 중', RESERVED: '예약 완료', UNKNOWN: '확인 중' };
const GRID_STEP = 24;
const PADDING = 18;
const SEAT_SIZE = 20;

function getSeatGrid(seats) {
    const plotted = seats.filter((seat) => Number.isFinite(seat.x) && Number.isFinite(seat.y));
    if (!plotted.length) return null;
    const xs = plotted.map((seat) => seat.x);
    const ys = plotted.map((seat) => seat.y);
    const minX = Math.min(...xs);
    const maxY = Math.max(...ys);
    return {
        width: (Math.max(...xs) - minX) * GRID_STEP + 2 * PADDING,
        height: (maxY - Math.min(...ys)) * GRID_STEP + 2 * PADDING,
        seats: plotted.map((seat) => ({
            ...seat, cx: PADDING + (seat.x - minX) * GRID_STEP, cy: PADDING + (maxY - seat.y) * GRID_STEP
        }))
    };
}

function getErrorMessage(status) {
    switch (status) {
        case 401: return '로그인이 필요합니다.';
        case 403: return '입장 권한이 없거나 좌석 선택 시간이 만료되었습니다.';
        case 404: return '해당 공연의 회차 또는 구역을 찾을 수 없습니다.';
        case 409: return '현재 예매할 수 없는 회차입니다.';
        default: return '좌석 정보를 불러오지 못했습니다. 다시 시도해 주세요.';
    }
}

function SectionSeats({ eventId, sessionId, sectionId, onRetry }) {
    const [seats, setSeats] = useState(null);
    const [statuses, setStatuses] = useState(new Map());
    const [maxTicketPerUser, setMaxTicketPerUser] = useState(null);
    const [failure, setFailure] = useState(null);
    const [retrying, setRetrying] = useState(false);
    const [hoveredSeat, setHoveredSeat] = useState(null);
    const [selectedIds, setSelectedIds] = useState(() => new Set());
    const grid = useMemo(() => getSeatGrid(seats ?? []), [seats]);

    useEffect(() => {
        const controller = new AbortController();
        const { signal } = controller;
        let timer;

        async function pollStatuses() {
            try {
                const data = await getSeatStatuses(eventId, sessionId, sectionId, signal);
                if (signal.aborted) return;
                const next = new Map(data.map(({ seatInstanceId, status }) => [seatInstanceId, status]));
                setStatuses(next);
                setSelectedIds((current) => {
                    const available = [...current].filter((id) => next.get(id) === 'AVAILABLE');
                    return available.length === current.size ? current : new Set(available);
                });
                setRetrying(false);
            } catch (error) {
                if (signal.aborted) return;
                const status = error.response?.status;
                if (status >= 400 && status < 500 && status !== 408 && status !== 429) {
                    setFailure({ status });
                    return;
                }
                setRetrying(true);
            }
            timer = window.setTimeout(pollStatuses, 1000);
        }

        async function loadSeats() {
            try {
                const [data, event] = await Promise.all([
                    getSectionSeats(eventId, sessionId, sectionId, signal),
                    getEventDetail(eventId, signal)
                ]);
                if (signal.aborted) return;
                const limit = event?.maxTicketPerUser;
                if (!Number.isInteger(limit) || limit < 1) {
                    setFailure({ message: '이 공연의 최대 예매 매수를 확인할 수 없습니다.' });
                    return;
                }
                const sectionSeats = data.filter((seat) => String(seat.sectionId) === sectionId);
                setMaxTicketPerUser(limit);
                setSeats(sectionSeats);
                if (sectionSeats.length) void pollStatuses();
            } catch (error) {
                if (!signal.aborted) setFailure({ status: error.response?.status });
            }
        }
        void loadSeats();
        return () => {
            controller.abort();
            window.clearTimeout(timer);
        };
    }, [eventId, sessionId, sectionId]);

    if (failure) {
        return (
            <div className="booking-map-message" role="alert">
                <p>{failure.message ?? getErrorMessage(failure.status)}</p>
                {failure.status === 403 ? (
                    <Link className="booking-map-action" to={`/queue/events/${eventId}/sessions/${sessionId}`}>대기열 확인</Link>
                ) : failure.status === 401 ? (
                    <Link className="booking-map-action" to="/login">로그인</Link>
                ) : <button type="button" onClick={onRetry}>다시 시도</button>}
            </div>
        );
    }
    if (!seats?.length) {
        return <div className="booking-map-message" role="status">{seats ? '등록된 좌석이 없습니다.' : '좌석 정보를 불러오는 중입니다.'}</div>;
    }

    function getState(id) {
        const state = statuses.get(id);
        return Object.hasOwn(STATE_LABEL, state) ? state : 'UNKNOWN';
    }

    function toggleSeat(id) {
        if (getState(id) !== 'AVAILABLE') return;
        setSelectedIds((current) => {
            if (!current.has(id) && current.size >= maxTicketPerUser) return current;
            const next = new Set(current);
            if (next.has(id)) next.delete(id);
            else next.add(id);
            return next;
        });
    }

    const limitReached = selectedIds.size >= maxTicketPerUser;
    function canSelectSeat(id) {
        return getState(id) === 'AVAILABLE' && (!limitReached || selectedIds.has(id));
    }

    const selectedSeats = seats.filter((seat) => selectedIds.has(seat.seatInstanceId));

    return (
        <section className="booking-map-card">
            <div className="booking-seat-legend">
                {Object.entries(STATE_LABEL).map(([state, label]) => (
                    <span key={state}><i className="booking-seat-dot" data-state={state} />{label}</span>
                ))}
            </div>
            <p className="booking-map-note" role="status">
                최대 {maxTicketPerUser}매까지 선택할 수 있어요.
                {limitReached && ' 다른 좌석을 선택하려면 선택한 좌석을 먼저 해제해 주세요.'}
            </p>
            {retrying && <p className="booking-map-note" role="status">상태 갱신을 재시도하고 있습니다. 표시된 상태는 최신 정보와 다를 수 있습니다.</p>}
            {grid ? (
                <div className="booking-seat-scroll">
                    <div className="booking-seat-canvas">
                        <svg className="booking-seat-map" viewBox={`0 0 ${grid.width} ${grid.height}`} width={grid.width} height={grid.height} role="group" aria-label="구역 내 좌석 배치도">
                            {grid.seats.map((seat) => {
                                const id = seat.seatInstanceId;
                                const status = getState(id);
                                const selectable = canSelectSeat(id);
                                return (
                                    <rect key={id} className="booking-seat" data-state={status}
                                          x={seat.cx - SEAT_SIZE / 2} y={seat.cy - SEAT_SIZE / 2} width={SEAT_SIZE} height={SEAT_SIZE}
                                          role="button" tabIndex={selectable ? 0 : -1} aria-disabled={!selectable}
                                          aria-pressed={selectedIds.has(id)} aria-label={`${seat.seatRow}열 ${seat.seatNo}번 · ${STATE_LABEL[status]}`}
                                          onClick={() => toggleSeat(id)} onKeyDown={(event) => {
                                        if (event.key !== 'Enter' && event.key !== ' ') return;
                                        event.preventDefault();
                                        if (!event.repeat) toggleSeat(id);
                                    }}
                                          onMouseEnter={() => setHoveredSeat(seat)} onMouseLeave={() => setHoveredSeat(null)}
                                          onFocus={() => setHoveredSeat(seat)} onBlur={() => setHoveredSeat(null)} />
                                );
                            })}
                        </svg>
                        {hoveredSeat && (
                            <div className="booking-seat-tooltip" aria-hidden="true" style={{
                                '--seat-offset-x': `${hoveredSeat.cx - grid.width / 2}px`,
                                '--seat-top': `${hoveredSeat.cy - SEAT_SIZE / 2}px`
                            }}>
                                {hoveredSeat.seatRow}열 {hoveredSeat.seatNo}번
                            </div>
                        )}
                    </div>
                </div>
            ) : <p className="booking-map-note">좌석 위치 정보가 없습니다. 아래 목록을 확인해 주세요.</p>}
            {selectedSeats.length > 0 && (
                <div className="booking-seat-selection">
                    <div aria-live="polite">
                        <strong>선택 좌석 {selectedSeats.length}석</strong>
                        <p>{selectedSeats.map((seat) => `${seat.seatRow}열 ${seat.seatNo}번`).join(', ')}</p>
                    </div>
                    <button type="button" className="booking-map-action">예매하기</button>
                </div>
            )}
        </section>
    );
}

export default function SeatSelectPage() {
    const { eventId, sessionId, sectionId } = useParams();
    const { state } = useLocation();
    const [attempt, setAttempt] = useState(0);
    const sectionsPath = `/booking/events/${eventId}/sessions/${sessionId}/sections`;

    return (
        <main className="booking-map-page">
            <div className="booking-map-container">
                <header className="booking-map-header">
                    <div>
                        <span className="booking-map-step">02 좌석 조회</span>
                        <h1>{state?.section?.name ? `${state.section.name} 좌석 배치도` : '좌석 배치도'}</h1>
                        <p>선택한 구역의 좌석 위치와 예매 가능 여부를 확인하세요.</p>
                    </div>
                    <Link className="booking-map-back" to={sectionsPath} state={{ selectingExpiresAt: state?.selectingExpiresAt }}>다른 구역 선택</Link>
                </header>
                <SectionSeats key={`${eventId}/${sessionId}/${sectionId}/${attempt}`} eventId={eventId} sessionId={sessionId}
                              sectionId={sectionId} onRetry={() => setAttempt((count) => count + 1)} />
            </div>
        </main>
    );
}
