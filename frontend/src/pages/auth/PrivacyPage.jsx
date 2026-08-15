import { Link } from 'react-router-dom';
import '../../styles/auth.css';

function PrivacyPage() {
    return (
        <main className="auth-page">
            <div className="auth-container">

                <h1 className="auth-title">
                    개인정보처리방침
                </h1>

                <p className="auth-intro">
                    티켓팅 프로젝트(이하 "서비스")는 이용자의 개인정보를
                    중요하게 생각하며, 서비스 제공에 필요한 최소한의
                    개인정보만 처리합니다.
                </p>

                <section className="auth-section">
                    <h2>1. 처리하는 개인정보 항목</h2>

                    <p>
                        서비스는 Google 로그인 과정에서 다음 정보를 처리할 수 있습니다.
                    </p>

                    <ul>
                        <li>Google 계정 고유 식별정보</li>
                        <li>이메일 주소</li>
                        <li>이름</li>
                    </ul>

                    <p>
                        서비스 이용 과정에서 다음 정보가 추가로 생성될 수 있습니다.
                    </p>

                    <ul>
                        <li>예매 내역</li>
                        <li>좌석 선택 및 예매 정보</li>
                        <li>서비스 이용 기록</li>
                    </ul>
                </section>

                <section className="auth-section">
                    <h2>2. 개인정보 처리 목적</h2>

                    <p>
                        서비스는 개인정보를 다음 목적으로 이용합니다.
                    </p>

                    <ul>
                        <li>회원 식별 및 로그인</li>
                        <li>회원 계정 관리</li>
                        <li>티켓 예매 처리</li>
                        <li>예매 내역 관리</li>
                        <li>서비스의 안정적인 운영</li>
                        <li>부정 이용 방지</li>
                    </ul>
                </section>

                <section className="auth-section">
                    <h2>3. 개인정보 수집 방법</h2>

                    <p>
                        개인정보는 다음 방법으로 처리됩니다.
                    </p>

                    <ul>
                        <li>Google OAuth 로그인</li>
                        <li>
                            서비스 이용 과정에서 이용자가 직접 수행한
                            예매 및 좌석 선택
                        </li>
                    </ul>

                    <p>
                        서비스는 Google 계정의 비밀번호를 직접 수집하거나
                        저장하지 않습니다.
                    </p>
                </section>

                <section className="auth-section">
                    <h2>4. 개인정보 보유 및 이용 기간</h2>

                    <p>
                        개인정보는 원칙적으로 회원이 서비스를 이용하는 동안
                        보관합니다.
                    </p>

                    <p>
                        회원 탈퇴 시 회원 식별정보 및 개인정보는
                        지체 없이 삭제합니다.
                    </p>

                    <p>
                        다만 서비스 운영 또는 관련 법령에 따라 일정 기간 보관이
                        필요한 정보가 있는 경우 해당 기간 동안 보관될 수 있습니다.
                    </p>
                </section>

                <section className="auth-section">
                    <h2>5. 로그인 토큰 처리</h2>

                    <p>
                        서비스는 로그인 상태 유지 및 인증을 위해
                        Access Token과 Refresh Token을 사용할 수 있습니다.
                    </p>

                    <p>
                        Refresh Token은 로그인 유지 및 새로운 Access Token 발급을
                        위해 Redis에 일정 기간 저장될 수 있으며,
                        만료 또는 로그아웃 시 삭제됩니다.
                    </p>

                    <p>
                        Refresh Token은 서비스 내부 인증 목적으로만 사용됩니다.
                    </p>
                </section>

                <section className="auth-section">
                    <h2>6. 개인정보의 제3자 제공</h2>

                    <p>
                        서비스는 이용자의 개인정보를 원칙적으로
                        제3자에게 제공하지 않습니다.
                    </p>

                    <p>
                        다만 법령에 특별한 규정이 있거나 법적 의무에 따라
                        필요한 경우에는 예외가 적용될 수 있습니다.
                    </p>
                </section>

                <section className="auth-section">
                    <h2>7. 개인정보 처리 위탁</h2>

                    <p>
                        현재 서비스는 별도의 개인정보 처리 업무를
                        외부 업체에 위탁하지 않습니다.
                    </p>

                    <p>
                        향후 외부 서비스를 이용하게 되는 경우 관련 내용을
                        본 개인정보처리방침에 반영할 수 있습니다.
                    </p>
                </section>

                <section className="auth-section">
                    <h2>8. 개인정보 삭제</h2>

                    <p>
                        회원이 탈퇴하는 경우 서비스에 저장된 회원 개인정보는
                        삭제됩니다.
                    </p>

                    <p>
                        다만 서비스의 기술적 구조 또는 법적 보관 의무에 따라
                        일부 정보가 일정 기간 보관될 수 있습니다.
                    </p>
                </section>

                <section className="auth-section">
                    <h2>9. 이용자의 권리</h2>

                    <p>
                        이용자는 자신의 개인정보에 대해 조회, 수정 또는
                        삭제를 요청할 수 있습니다.
                    </p>

                    <p>
                        회원 탈퇴 기능을 통해 개인정보 삭제를
                        요청할 수도 있습니다.
                    </p>
                </section>

                <section className="auth-section">
                    <h2>10. 개인정보 보호 관련 문의</h2>

                    <p>
                        개인정보 처리와 관련한 문의는
                        서비스 운영자에게 요청할 수 있습니다.
                    </p>

                    <p>
                        운영자 연락처는 서비스 공개 시
                        별도로 안내할 수 있습니다.
                    </p>
                </section>

                <section className="auth-section">
                    <h2>11. 개인정보처리방침의 변경</h2>

                    <p>
                        서비스 기능 또는 개인정보 처리 방식이 변경되는 경우
                        본 개인정보처리방침도 변경될 수 있습니다.
                    </p>

                    <p>
                        중요한 변경 사항이 있는 경우
                        서비스 내에서 안내할 수 있습니다.
                    </p>
                </section>

                <section className="auth-section">
                    <h2>부칙</h2>

                    <p>
                        본 개인정보처리방침은 서비스 공개일부터 적용합니다.
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

export default PrivacyPage;