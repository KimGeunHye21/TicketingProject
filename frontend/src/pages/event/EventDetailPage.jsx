import { useEffect, useState } from 'react';

import {
    useNavigate,
    useParams
} from 'react-router-dom';

import {
    getEventDetail
} from '../../api/eventApi';

import {
    useAuth
} from '../../contexts/AuthContext';

import eventPlaceholder
    from '../../assets/evnet-placeholder.svg';

import '../../styles/eventDetail.css';


function EventDetailPage() {

    const { eventId } = useParams();

    const navigate = useNavigate();


    /* =========================
       로그인 상태
    ========================= */

    const {
        isLoggedIn,
        loading: authLoading
    } = useAuth();


    /* =========================
       공연 정보
    ========================= */

    const [event, setEvent] =
        useState(null);

    const [loading, setLoading] =
        useState(true);

    const [error, setError] =
        useState(null);


    // 공연일 선택창 표시 여부
    const [
        showSessionSelect,
        setShowSessionSelect
    ] = useState(false);


    // 선택한 공연 회차
    const [
        selectedSessionId,
        setSelectedSessionId
    ] = useState(null);


    /* =========================
       공연 상세 조회
    ========================= */

    useEffect(() => {

        const fetchEvent = async () => {

            try {

                setLoading(true);

                const data =
                    await getEventDetail(eventId);

                setEvent(data);

            } catch (error) {

                console.error(
                    '공연 상세 조회 실패:',
                    error
                );

                setError(
                    '공연 정보를 불러오지 못했습니다.'
                );

            } finally {

                setLoading(false);
            }
        };


        fetchEvent();

    }, [eventId]);


    /* =========================
       날짜 + 시간 출력
    ========================= */

    const formatDateTime = (dateTime) => {

        if (!dateTime) {
            return '';
        }


        const date =
            new Date(dateTime);


        return date.toLocaleString(
            'ko-KR',
            {
                year: 'numeric',
                month: 'long',
                day: 'numeric',
                weekday: 'short',
                hour: '2-digit',
                minute: '2-digit'
            }
        );
    };


    /* =========================
       예매 시작 여부
    ========================= */

    const isBookingOpen = () => {

        if (!event) {
            return false;
        }


        const now =
            new Date();

        const bookingOpenAt =
            new Date(
                event.bookingOpenAt
            );


        return now >= bookingOpenAt;
    };


    /* =========================
       예매 D-Day 계산
    ========================= */

    const getBookingDDay = () => {

        const now =
            new Date();

        const openAt =
            new Date(
                event.bookingOpenAt
            );


        const today =
            new Date(
                now.getFullYear(),
                now.getMonth(),
                now.getDate()
            );


        const openDate =
            new Date(
                openAt.getFullYear(),
                openAt.getMonth(),
                openAt.getDate()
            );


        const difference =
            openDate.getTime() -
            today.getTime();


        const day =
            Math.ceil(
                difference /
                (1000 * 60 * 60 * 24)
            );


        if (day <= 0) {
            return 'D-DAY';
        }


        return `D-${day}`;
    };


    /* =========================
       예매하기 클릭
    ========================= */

    const handleBookingClick = () => {

        // 로그인 상태를 아직 확인 중
        if (authLoading) {
            return;
        }


        // 로그인 안 되어 있음
        if (!isLoggedIn) {

            // 로그인 후 돌아올 현재 공연 상세페이지 저장
            sessionStorage.setItem(
                'login_return_to',
                `/events/${eventId}`
            );

            navigate(
                '/login',
                {
                    state: {
                        message:
                            '로그인이 필요한 페이지입니다.'
                    }
                }
            );

            return;
        }


        // 로그인되어 있음
        // URL 이동 없이 공연일 선택창 표시
        setShowSessionSelect(true);
    };


    /* =========================
       좌석 선택 페이지 이동
    ========================= */

    const handleSeatSelection = () => {

        if (!selectedSessionId) {
            return;
        }


        navigate(
            `/events/${eventId}/sessions/${selectedSessionId}/seats`
        );
    };


    /* =========================
       로딩
    ========================= */

    if (loading) {

        return (
            <main className="page-container event-detail-page">

                <p className="event-message">
                    공연 정보를 불러오는 중입니다.
                </p>

            </main>
        );
    }


    /* =========================
       오류
    ========================= */

    if (error || !event) {

        return (
            <main className="page-container event-detail-page">

                <p className="event-message event-error">
                    {error}
                </p>

            </main>
        );
    }


    return (

        <main className="page-container event-detail-page">


            {/* =========================
                상단 공연 정보
            ========================= */}

            <section className="event-detail-main">


                {/* 포스터 */}

                <div className="event-detail-poster">

                    <img
                        src={eventPlaceholder}
                        alt={`${event.title} 공연 이미지`}
                    />

                </div>


                {/* 상세 정보 */}

                <div className="event-detail-content">


                    <h1 className="event-detail-title">
                        {event.title}
                    </h1>


                    <div className="event-detail-info">


                        <div className="event-detail-row">

                            <span className="event-detail-label">
                                공연장
                            </span>

                            <span>
                                {event.placeName}
                            </span>

                        </div>


                        <div className="event-detail-row">

                            <span className="event-detail-label">
                                주소
                            </span>

                            <span>
                                {event.address}
                            </span>

                        </div>


                        <div className="event-detail-row">

                            <span className="event-detail-label">
                                공연 시간
                            </span>

                            <span>
                                {event.runningTime}분
                            </span>

                        </div>


                        <div className="event-detail-row">

                            <span className="event-detail-label">
                                출연진
                            </span>

                            <span>
                                {event.cast ||
                                    '출연진 정보 없음'}
                            </span>

                        </div>


                        <div className="event-detail-row">

                            <span className="event-detail-label">
                                최대 예매
                            </span>

                            <span>
                                1인 {event.maxTicketPerUser}매
                            </span>

                        </div>


                        <div className="event-detail-row">

                            <span className="event-detail-label">
                                예매 오픈
                            </span>

                            <span>
                                {formatDateTime(
                                    event.bookingOpenAt
                                )}
                            </span>

                        </div>

                    </div>


                    {/* =========================
                        예매 상태
                    ========================= */}

                    <div className="event-booking-area">

                        {!isBookingOpen() ? (

                            <div className="booking-dday">

                                <span>
                                    예매 오픈까지
                                </span>

                                <strong>
                                    {getBookingDDay()}
                                </strong>

                            </div>

                        ) : (

                            <button
                                type="button"
                                className="booking-button"
                                onClick={handleBookingClick}
                                disabled={authLoading}
                            >
                                예매하기
                            </button>

                        )}

                    </div>

                </div>

            </section>


            {/* =========================
                공연 일자 선택창
            ========================= */}

            {showSessionSelect && (

                <section className="session-select-area">

                    <div className="session-select-header">

                        <h2>
                            공연 일자 선택
                        </h2>


                        <button
                            type="button"
                            className="session-close-button"
                            onClick={() => {

                                setShowSessionSelect(
                                    false
                                );

                                setSelectedSessionId(
                                    null
                                );
                            }}
                        >
                            ✕
                        </button>

                    </div>


                    <div className="session-list">

                        {event.sessions.length === 0 ? (

                            <p className="session-empty">
                                선택 가능한 공연이 없습니다.
                            </p>

                        ) : (

                            event.sessions.map(
                                (session) => (

                                    <button
                                        key={session.sessionId}
                                        type="button"
                                        className={
                                            selectedSessionId ===
                                            session.sessionId
                                                ? 'session-item selected'
                                                : 'session-item'
                                        }
                                        onClick={() =>
                                            setSelectedSessionId(
                                                session.sessionId
                                            )
                                        }
                                    >

                                        {formatDateTime(
                                            session.startAt
                                        )}

                                    </button>

                                )
                            )

                        )}

                    </div>


                    <button
                        type="button"
                        className="seat-select-button"
                        disabled={
                            !selectedSessionId
                        }
                        onClick={
                            handleSeatSelection
                        }
                    >
                        좌석 선택하기
                    </button>

                </section>

            )}

        </main>
    );
}


export default EventDetailPage;