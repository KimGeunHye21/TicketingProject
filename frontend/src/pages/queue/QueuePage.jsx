import { useEffect, useState } from 'react';

import {
    useNavigate,
    useParams
} from 'react-router-dom';

import {
    joinQueue,
    getQueueStatus
} from '../../api/queueApi';

import '../../styles/queuePage.css';


const POLLING_INTERVAL = 3000;


function QueuePage() {

    const { eventId, sessionId } = useParams();

    const navigate = useNavigate();


    // CONNECTING, WAITING, ENTERING, ERROR, EXPIRED, CANCELLED
    const [phase, setPhase] = useState('CONNECTING');

    // 내 앞의 대기 인원
    const [peopleAhead, setPeopleAhead] = useState(null);

    // 오류 메시지
    const [errorMessage, setErrorMessage] = useState('');

    // 재시도용
    const [retryCount, setRetryCount] = useState(0);


    useEffect(() => {

        let isActive = true;
        let pollingTimer = null;

        // 대기열 상태 조회
        const checkQueueStatus = async () => {
            try {

                const result = await getQueueStatus(eventId, sessionId); // 대기열 get요청 결과
                if (!isActive) return;

                // 1. 대기 중
                if (result.status === 'WAITING') {
                    setPeopleAhead(result.aheadCount);
                    setPhase('WAITING');

                    // 폴링 예약 (이전 요청이 끝난 후 3초 뒤 재조회)
                    pollingTimer = setTimeout(checkQueueStatus, POLLING_INTERVAL);

                    return;
                }


                // 2. 입장 가능
                if (result.status === 'SELECTING') {
                    setPhase('ENTERING');

                    // 구역 선택 페이지로 자동 이동
                    navigate(
                        `/booking/events/${eventId}/sessions/${sessionId}/sections`,
                        {
                            replace: true,
                            state: {
                                selectingExpiresAt: result.selectingExpiresAt
                            }
                        }
                    );

                    return;
                }

                // 3. 만료
                if (result.status === 'EXPIRED') {
                    setPhase('EXPIRED');
                    return;
                }

                // 4. 취소
                if (result.status === 'CANCELLED') {
                    setPhase('CANCELLED');
                    return;
                }

                throw new Error(
                    '알 수 없는 대기열 상태입니다.'
                );

            } catch (error) {
                if (!isActive) return;
                console.error('대기열 상태 조회 실패:', error);
                setErrorMessage('대기열 상태를 확인하지 못했습니다.');

                setPhase('ERROR');
            }
        };

        // 최초 대기열 등록
        const registerQueue = async () => {

            setPhase('CONNECTING');

            setPeopleAhead(null);
            setErrorMessage('');

            try {
                if (!eventId || !sessionId) {
                    throw new Error( '공연 또는 회차 정보가 없습니다.' );
                }

                // 대기열 등록 POST
                const result = await joinQueue(eventId, sessionId);
                if (!isActive) return;

                if (!result) {
                    throw new Error('대기열 등록 결과를 받지 못했습니다.');
                }

                // 등록 성공 -> 상태 조회 폴링 시작
                await checkQueueStatus();

            } catch (error) {

                if (!isActive) return;
                console.error('대기열 등록 실패:', error);
                setErrorMessage('대기열에 접속하지 못했습니다.');

                setPhase('ERROR');
            }
        };
        registerQueue();


        // 페이지 이탈 시 정리
        return () => {

            isActive = false;

            if (pollingTimer !== null) { clearTimeout(pollingTimer); }
        };

    }, [eventId, sessionId, navigate, retryCount]);


    // 다시 시도
    const handleRetry = () => {

        setRetryCount(prev => prev + 1);
    };


    // 공연 상세로 이동
    const handleGoBack = () => {

        navigate(`/events/${eventId}`);
    };


    return (

        <main className="queue-page">

            <div className="queue-card">

                <div className="queue-header">

                    <span className="queue-label">
                        TICKETING
                    </span>

                    <h1>예매 대기</h1>

                    <p>
                        원활한 예매를 위해
                        순서대로 입장하고 있습니다.
                    </p>

                </div>


                <div
                    className="queue-content"
                    aria-live="polite"
                >

                    {/* 대기열 등록 중 */}
                    {phase === 'CONNECTING' && (

                        <div className="queue-status">

                            <div className="queue-spinner" />

                            <h2>
                                대기 순서를 발급받는 중입니다...
                            </h2>

                            <p>
                                서버에 접속하고 있습니다.
                                <br />
                                잠시만 기다려 주세요.
                            </p>

                        </div>
                    )}


                    {/* 대기 중 */}
                    {phase === 'WAITING' && (

                        <div className="queue-status">

                            <span className="queue-status-label">
                                현재 내 앞의 대기 인원
                            </span>

                            <div className="queue-count">

                                <strong>
                                    {peopleAhead ?? '-'}
                                </strong>

                                <span>명</span>

                            </div>

                            <h2>
                                예매 대기 중입니다
                            </h2>

                            <p>
                                순서가 되면 자동으로
                                예매 화면으로 이동합니다.
                            </p>

                            <div className="queue-refresh">

                                <span className="queue-refresh-dot" />

                                대기 순서 자동 갱신 중

                            </div>

                        </div>
                    )}


                    {/* 입장 중 */}
                    {phase === 'ENTERING' && (

                        <div className="queue-status">

                            <div className="queue-spinner" />

                            <h2>
                                입장 중입니다...
                            </h2>

                            <p>
                                구역 선택 화면으로 이동합니다.
                            </p>

                        </div>
                    )}


                    {/* 오류 */}
                    {phase === 'ERROR' && (

                        <div className="queue-status">

                            <h2>접속 오류</h2>

                            <p>{errorMessage}</p>

                            <button
                                className="queue-button"
                                onClick={handleRetry}
                            >
                                다시 시도
                            </button>

                        </div>
                    )}


                    {/* 대기열 만료 또는 취소 */}
                    {(phase === 'EXPIRED' ||
                        phase === 'CANCELLED') && (

                        <div className="queue-status">

                            <h2>
                                대기열 이용이 종료되었습니다
                            </h2>

                            <p>
                                다시 예매를 진행해 주세요.
                            </p>

                            <button
                                className="queue-button"
                                onClick={handleGoBack}
                            >
                                공연 상세로 돌아가기
                            </button>

                        </div>
                    )}

                </div>


                <div className="queue-footer">

                    <p>
                        대기 중에는 페이지를
                        새로고침하지 않는 것을 권장합니다.
                    </p>

                </div>

            </div>

        </main>
    );
}

export default QueuePage;