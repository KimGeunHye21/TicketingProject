import { useEffect, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import { getSectionMap } from '../../api/eventApi';
import '../../styles/bookingMap.css';

const formatPrice = (price) => `${Number(price).toLocaleString('ko-KR')}원`;

function SectionSelectPage() {
    const { eventId, sessionId } = useParams();
    const location = useLocation();
    const [sections, setSections] = useState([]);
    const [loading, setLoading] = useState(true);
    const [errorMessage, setErrorMessage] = useState('');
    const [retryCount, setRetryCount] = useState(0);

    useEffect(() => {
        const controller = new AbortController();
        const loadSections = async () => {
            setLoading(true);
            setErrorMessage('');
            setSections([]);
            try {
                const data = await getSectionMap(eventId, sessionId, controller.signal);
                if (!controller.signal.aborted) {
                    if (!Array.isArray(data.sections)) throw new Error('구역 목록 형식이 올바르지 않습니다.');
                    setSections(data.sections);
                }
            } catch (error) {
                if (!controller.signal.aborted) {
                    setErrorMessage(error.response?.status === 404
                        ? '공연 또는 회차 정보를 찾을 수 없습니다.'
                        : '구역 배치도를 불러오지 못했습니다. 예매 가능 시간을 확인하거나 다시 시도해 주세요.');
                }
            } finally {
                if (!controller.signal.aborted) setLoading(false);
            }
        };
        void loadSections();
        return () => controller.abort();
    }, [eventId, sessionId, retryCount]);

    const seatPath = (sectionId) =>
        `/booking/events/${eventId}/sessions/${sessionId}/sections/${sectionId}/seats`;
    const linkState = (section) => ({ section, selectingExpiresAt: location.state?.selectingExpiresAt });

    return (
        <main className="booking-map-page">
            <div className="booking-map-container">
                <header className="booking-map-header">
                    <div>
                        <span className="booking-map-step">01 구역 선택</span>
                        <h1>어느 구역에서 관람할까요?</h1>
                        <p>배치도의 구역을 누르면 해당 구역의 좌석을 확인할 수 있습니다.</p>
                    </div>
                    <Link className="booking-map-back" to={`/events/${eventId}`}>공연 상세로</Link>
                </header>

                {loading && <div className="booking-map-message" role="status">구역 배치도를 불러오는 중입니다.</div>}
                {!loading && errorMessage && (
                    <div className="booking-map-message" role="alert">
                        <p>{errorMessage}</p>
                        <button type="button" onClick={() => setRetryCount((count) => count + 1)}>다시 시도</button>
                    </div>
                )}
                {!loading && !errorMessage && sections.length === 0 && (
                    <div className="booking-map-message" role="status">등록된 구역이 없습니다.</div>
                )}
                {!loading && !errorMessage && sections.length > 0 && (
                    <div className="booking-map-layout">
                        <section className="booking-map-card" aria-labelledby="section-map-heading">
                            <h2 id="section-map-heading">구역 배치도</h2>
                            <svg className="booking-section-map" viewBox="0 0 100 100" role="group" aria-label="클릭하여 구역을 선택하는 배치도">
                                {sections.map((section) => {
                                    const points = section.points;
                                    if (!Array.isArray(points) || points.length !== 4
                                            || points.some((point) => !Number.isFinite(point?.x) || !Number.isFinite(point?.y))) {
                                        return null;
                                    }
                                    const centerX = points.reduce((sum, point) => sum + point.x, 0) / points.length;
                                    const centerY = points.reduce((sum, point) => sum + point.y, 0) / points.length;
                                    return (
                                        <Link
                                            key={section.sectionId}
                                            className="booking-section-link"
                                            to={seatPath(section.sectionId)}
                                            state={linkState(section)}
                                            aria-label={`${section.name} 좌석 조회, ${formatPrice(section.price)}, 잔여 ${section.availableSeatCount}석`}
                                        >
                                            <polygon
                                                points={points.map((point) => `${point.x},${point.y}`).join(' ')}
                                                fill={section.color || '#4964a1'}
                                                stroke={section.color || '#4964a1'}
                                            />
                                            <text x={centerX} y={centerY} textAnchor="middle" aria-hidden="true">
                                                <tspan x={centerX} dy="-1.5" className="booking-section-name">{section.name}</tspan>
                                                <tspan x={centerX} dy="4.5" className="booking-section-price">{formatPrice(section.price)}</tspan>
                                            </text>
                                        </Link>
                                    );
                                })}
                            </svg>
                            <p className="booking-map-note">구역의 대략적인 위치를 표시한 배치도입니다. 실제 형태와 비율은 다를 수 있습니다.</p>
                        </section>
                        <aside className="booking-map-card booking-section-list" aria-labelledby="section-list-heading">
                            <h2 id="section-list-heading">구역 안내</h2>
                            <p className="booking-map-note">목록에서도 구역을 선택할 수 있습니다.</p>
                            {sections.map((section) => (
                                <Link key={section.sectionId} to={seatPath(section.sectionId)} state={linkState(section)} className="booking-section-item">
                                    <span className="booking-section-dot" style={{ backgroundColor: section.color || '#4964a1' }} aria-hidden="true" />
                                    <span className="booking-section-info">
                                        <strong>{section.name}</strong>
                                        <span>{formatPrice(section.price)} · 잔여 {section.availableSeatCount}석</span>
                                    </span>
                                    <span aria-hidden="true">→</span>
                                </Link>
                            ))}
                        </aside>
                    </div>
                )}
            </div>
        </main>
    );
}

export default SectionSelectPage;
