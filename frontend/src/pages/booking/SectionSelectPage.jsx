import { useParams } from 'react-router-dom';


function SectionSelectPage() {

    const { eventId, sessionId } = useParams();


    return (

        <main style={{
            minHeight: '80vh',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '16px'
        }}>

            <h1>구역 선택</h1>

            <p>
                예매 대기열 입장이 완료되었습니다.
            </p>

            <p>
                공연 ID: {eventId}
                <br />
                회차 ID: {sessionId}
            </p>

            <p>
                구역 선택 기능은 구현 예정입니다.
            </p>

        </main>
    );
}

export default SectionSelectPage;