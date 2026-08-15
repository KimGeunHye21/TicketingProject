import { Link } from 'react-router-dom';
import '../../styles/auth.css';

function TermsPage() {
    return (
        <main className="auth-page">
            <div className="auth-container">

                <h1 className="auth-title">
                    이용약관
                </h1>

                <section className="auth-section">
                    <h2>제1조 목적</h2>

                    <p>
                        본 약관은 티켓팅 프로젝트(이하 "서비스")가 제공하는
                        티켓 예매 관련 기능의 이용 조건 및 절차를 정하는 것을 목적으로 합니다.
                    </p>
                </section>

                <section className="auth-section">
                    <h2>제2조 서비스의 내용</h2>

                    <p>
                        서비스는 다음 기능을 제공합니다.
                    </p>

                    <ul>
                        <li>Google 계정을 이용한 로그인</li>
                        <li>공연 및 이벤트 정보 조회</li>
                        <li>좌석 조회 및 좌석 선점</li>
                        <li>티켓 예매 및 예매 내역 확인</li>
                    </ul>

                    <p>
                        현재 본 서비스는 개인 프로젝트 및 테스트 목적으로 운영되며,
                        실제 결제 기능은 제공하지 않습니다.
                    </p>
                </section>

                <section className="auth-section">
                    <h2>제3조 회원 이용</h2>

                    <p>
                        회원은 Google 계정을 통해 서비스에 로그인할 수 있습니다.
                    </p>

                    <p>
                        회원은 본인의 계정을 타인에게 부정하게 사용하게 해서는 안 되며,
                        자동화 프로그램이나 기타 비정상적인 방법으로 서비스를
                        이용해서는 안 됩니다.
                    </p>
                </section>

                <section className="auth-section">
                    <h2>제4조 좌석 선점 및 예매</h2>

                    <p>
                        회원이 좌석을 선택한 경우 해당 좌석은 일정 시간 동안
                        임시로 선점될 수 있습니다.
                    </p>

                    <p>
                        좌석 선점은 약 5분 동안 유지되며, 해당 시간 내에 예매가
                        완료되지 않은 경우 자동으로 해제될 수 있습니다.
                    </p>

                    <p>
                        동일한 좌석에 여러 사용자가 동시에 접근하는 경우 먼저
                        정상적으로 선점한 사용자가 우선권을 가질 수 있습니다.
                    </p>

                    <p>
                        공연 또는 이벤트별로 회원당 최대 예매 가능 매수가
                        제한될 수 있습니다.
                    </p>
                </section>

                <section className="auth-section">
                    <h2>제5조 서비스 이용 제한</h2>

                    <p>
                        다음과 같은 행위가 확인되는 경우 서비스 이용이 제한될 수 있습니다.
                    </p>

                    <ul>
                        <li>서비스에 과도한 요청을 반복적으로 보내는 행위</li>
                        <li>자동화 프로그램을 이용하여 좌석을 선점하거나 예매하는 행위</li>
                        <li>다른 사용자의 서비스 이용을 방해하는 행위</li>
                        <li>서비스의 정상적인 운영을 방해하는 기타 행위</li>
                    </ul>
                </section>

                <section className="auth-section">
                    <h2>제6조 서비스 변경 및 중단</h2>

                    <p>
                        본 서비스는 개인 프로젝트의 개발 및 테스트 과정에서
                        기능이 변경되거나 일시적으로 중단될 수 있습니다.
                    </p>

                    <p>
                        개발 또는 운영상의 필요에 따라 일부 기능이 예고 없이
                        변경되거나 삭제될 수 있습니다.
                    </p>
                </section>

                <section className="auth-section">
                    <h2>제7조 책임의 제한</h2>

                    <p>
                        본 서비스는 학습 및 포트폴리오 목적의 프로젝트로 제공됩니다.
                    </p>

                    <p>
                        서비스에 표시되는 공연, 좌석, 가격 등의 정보는 실제 티켓
                        판매를 의미하지 않으며, 서비스 이용 과정에서 발생하는
                        실제 금전 거래를 보장하지 않습니다.
                    </p>
                </section>

                <section className="auth-section">
                    <h2>제8조 약관의 변경</h2>

                    <p>
                        서비스 운영 과정에서 본 약관은 변경될 수 있습니다.
                    </p>

                    <p>
                        중요한 변경 사항이 있는 경우 서비스 내에서
                        변경 내용을 안내할 수 있습니다.
                    </p>
                </section>

                <section className="auth-section">
                    <h2>부칙</h2>

                    <p>
                        본 약관은 서비스 공개일부터 적용합니다.
                    </p>
                </section>

                <Link
                    to="/login"
                    className="auth-back-link"
                >
                    ← 로그인 화면으로 돌아가기
                </Link>

            </div>
        </main>
    );
}

export default TermsPage;