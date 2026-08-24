import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { getEvents } from '../../api/eventApi';

import eventPlaceholder from '../../assets/evnet-placeholder.svg';
import '../../styles/event.css';

function EventListPage() {

    const navigate = useNavigate();

    const [events, setEvents] = useState([]);

    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);


    useEffect(() => {

        const fetchEvents = async () => {

            try {

                setLoading(true);
                setError(null);

                const data = await getEvents(page);

                setEvents(data.content);
                setTotalPages(data.totalPages);

            } catch (error) {

                console.error(
                    '공연 목록 조회 실패:',
                    error
                );

                setError(
                    '공연 목록을 불러오지 못했습니다.'
                );

            } finally {

                setLoading(false);
            }
        };

        fetchEvents();

    }, [page]);


    // 공연 상세 페이지 이동
    const handleEventClick = (eventId) => {

        navigate(`/events/${eventId}`);
    };


    // 날짜 표시
    const formatDateTime = (dateTime) => {

        if (!dateTime) {
            return '';
        }

        const date = new Date(dateTime);

        return date.toLocaleString('ko-KR', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });
    };


    if (loading) {
        return (
            <main className="page-container event-page">
                <p className="event-message">
                    공연 목록을 불러오는 중입니다.
                </p>
            </main>
        );
    }


    if (error) {
        return (
            <main className="page-container event-page">
                <p className="event-message event-error">
                    {error}
                </p>
            </main>
        );
    }


    return (
        <main className="page-container event-page">

            <div className="event-page-header">
                <h1 className="event-page-title">
                    공연
                </h1>

                <p className="event-page-description">
                    예매 가능한 공연을 확인해보세요.
                </p>
            </div>


            {events.length === 0 ? (

                <p className="event-message">
                    등록된 공연이 없습니다.
                </p>

            ) : (

                <div className="event-grid">

                    {events.map((event) => (

                        <article
                            key={event.eventId}
                            className="event-card"
                            onClick={() =>
                                handleEventClick(
                                    event.eventId
                                )
                            }
                        >

                            <div className="event-image-wrapper">

                                <img
                                    src={eventPlaceholder}
                                    alt={`${event.title} 공연 이미지`}
                                    className="event-image"
                                />

                            </div>


                            <div className="event-info">

                                <h2 className="event-title">
                                    {event.title}
                                </h2>


                                <p className="event-cast">
                                    {event.cast ||
                                        '출연진 정보 없음'}
                                </p>


                                <div className="event-date-info">

                                    <div className="event-date-row">

                                        <span className="event-info-label">
                                            공연일
                                        </span>

                                        <span>
                                            {event.endDate
                                                ? `${event.startDate} ~ ${event.endDate}`
                                                : event.startDate}
                                        </span>

                                    </div>


                                    <div className="event-date-row">

                                        <span className="event-info-label">
                                            예매 오픈
                                        </span>

                                        <span>
                                            {formatDateTime(
                                                event.bookingOpenAt
                                            )}
                                        </span>

                                    </div>

                                </div>

                            </div>

                        </article>

                    ))}

                </div>
            )}


            {totalPages > 1 && (

                <div className="event-pagination">

                    <button
                        type="button"
                        className="pagination-button"
                        disabled={page === 0}
                        onClick={() =>
                            setPage(
                                (prev) => prev - 1
                            )
                        }
                    >
                        이전
                    </button>


                    <div className="pagination-pages">

                        {Array.from(
                            {
                                length: totalPages
                            },
                            (_, index) => (
                                <button
                                    key={index}
                                    type="button"
                                    className={
                                        page === index
                                            ? 'pagination-number active'
                                            : 'pagination-number'
                                    }
                                    onClick={() =>
                                        setPage(index)
                                    }
                                >
                                    {index + 1}
                                </button>
                            )
                        )}

                    </div>


                    <button
                        type="button"
                        className="pagination-button"
                        disabled={
                            page === totalPages - 1
                        }
                        onClick={() =>
                            setPage(
                                (prev) => prev + 1
                            )
                        }
                    >
                        다음
                    </button>

                </div>
            )}

        </main>
    );
}

export default EventListPage;