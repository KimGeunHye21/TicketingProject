import { useEffect, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import { getSectionSeats } from '../../api/eventApi';
import '../../styles/bookingMap.css';

function SeatSelectPage() {
    const { eventId, sessionId, sectionId } = useParams();
    const location = useLocation();
    const section = location.state?.section;
    const [seats, setSeats] = useState([]);
    const [loading, setLoading] = useState(true);
    const [errorMessage, setErrorMessage] = useState('');
    const [admissionRequired, setAdmissionRequired] = useState(false);
    const [retryCount, setRetryCount] = useState(0);
    const sectionsPath = `/booking/events/${eventId}/sessions/${sessionId}/sections`;

    useEffect(() => {
        const controller = new AbortController();
        const loadSeats = async () => {
            setLoading(true);
            setErrorMessage('');
            setAdmissionRequired(false);
            setSeats([]);
            try {
                const data = await getSectionSeats(eventId, sessionId, sectionId, controller.signal);
                if (!controller.signal.aborted) {
                    if (!Array.isArray(data)) throw new Error('좌석 목록 형식이 올바르지 않습니다.');
                    setSeats(data.filter((seat) => String(seat.sectionId) === sectionId));
                }
            } catch (error) {
                if (!controller.signal.aborted) {
                    const status = error.response?.status;
                    setAdmissionRequired(status === 403);
                    setErrorMessage(status === 501
                        ? '좌석 정보가 준비 중입니다. 잠시 후 다시 확인해 주세요.'
                        : status === 403
                            ? '입장 권한이 없거나 좌석 선택 시간이 만료되었습니다. 대기열에서 입장 상태를 확인해 주세요.'
                            : '좌석 정보를 불러오지 못했습니다. 다시 시도해 주세요.');
                }
            } finally {
                if (!controller.signal.aborted) setLoading(false);
            }
        };
        void loadSeats();
        return () => controller.abort();
    }, [eventId, sessionId, sectionId, retryCount]);

    const plottedSeats = seats.filter((seat) => Number.isFinite(seat.x) && Number.isFinite(seat.y));
    const xs = plottedSeats.map((seat) => seat.x);
    const ys = plottedSeats.map((seat) => seat.y);
    const minX = plottedSeats.length ? Math.min(...xs) : 0;
    const minY = plottedSeats.length ? Math.min(...ys) : 0;
    const width = plottedSeats.length ? Math.max(40, Math.max(...xs) - minX) + 40 : 100;
    const height = plottedSeats.length ? Math.max(40, Math.max(...ys) - minY) + 40 : 100;

    return (
        <main className="booking-map-page">
            <div className="booking-map-container">
                <header className="booking-map-header">
                    <div>
                        <span className="booking-map-step">02 좌석 조회</span>
                        <h1>{section?.name ? `${section.name} 좌석 배치도` : '좌석 배치도'}</h1>
                        <p>선택한 구역의 좌석 위치와 예매 가능 여부를 확인하세요.</p>
                    </div>
                    <Link className="booking-map-back" to={sectionsPath} state={{ selectingExpiresAt: location.state?.selectingExpiresAt }}>다른 구역 선택</Link>
                </header>

                {loading && <div className="booking-map-message" role="status">좌석 정보를 불러오는 중입니다.</div>}
                {!loading && errorMessage && (
                    <div className="booking-map-message" role="alert">
                        <p>{errorMessage}</p>
                        {admissionRequired
                            ? <Link className="booking-map-action" to={`/queue/events/${eventId}/sessions/${sessionId}`}>대기열 확인</Link>
                            : <button type="button" onClick={() => setRetryCount((count) => count + 1)}>다시 시도</button>}
                    </div>
                )}
                {!loading && !errorMessage && seats.length === 0 && (
                    <div className="booking-map-message" role="status">등록된 좌석이 없습니다.</div>
                )}
                {!loading && !errorMessage && seats.length > 0 && (
                    <section className="booking-map-card">
                        <div className="booking-seat-toolbar">
                            <div className="booking-seat-legend">
                                <span><i className="booking-seat-dot" />예매 가능</span>
                                <span><i className="booking-seat-dot unavailable" />예매 불가</span>
                            </div>
                            <button type="button" className="booking-map-back" onClick={() => setRetryCount((count) => count + 1)}>새로고침</button>
                        </div>
                        {plottedSeats.length > 0 ? (
                            <div className="booking-seat-scroll">
                                <svg className="booking-seat-map" viewBox={`${minX - 20} ${minY - 20} ${width} ${height}`} role="img" aria-label="구역 내 좌석 위치와 예매 가능 상태">
                                    {plottedSeats.map((seat) => (
                                        <g key={seat.seatInstanceId}>
                                            <title>{seat.seatRow}열 {seat.seatNo}번 · {seat.reservable ? '예매 가능' : '예매 불가'}</title>
                                            <circle cx={seat.x} cy={seat.y} r="7" className={`booking-seat${seat.reservable ? '' : ' unavailable'}`} />
                                            <text x={seat.x} y={seat.y} textAnchor="middle" dominantBaseline="middle" className="booking-seat-number">{seat.seatNo}</text>
                                        </g>
                                    ))}
                                </svg>
                            </div>
                        ) : <p className="booking-map-note">좌석의 위치 정보가 없습니다. 아래 목록을 확인해 주세요.</p>}
                        <details className="booking-seat-list">
                            <summary>좌석 목록으로 확인</summary>
                            <ul>
                                {seats.map((seat) => (
                                    <li key={seat.seatInstanceId}>{seat.seatRow}열 {seat.seatNo}번 · {seat.reservable ? '예매 가능' : '예매 불가'}</li>
                                ))}
                            </ul>
                        </details>
                    </section>
                )}
            </div>
        </main>
    );
}

export default SeatSelectPage;
