import {
    Link,
    useLocation,
    useNavigate
} from 'react-router-dom';

import {
    useState
} from 'react';

import '../../styles/auth.css';

function LoginPage() {

    // 로그인이 필요한 페이지의 안내 메시지
    const location = useLocation();
    const navigate = useNavigate();

    const [showAlert, setShowAlert] =
        useState(
            Boolean(location.state?.message)
        );

    const handleAlertConfirm = () => {

        setShowAlert(false);

        // 현재 history에 남아 있는 message도 제거
        navigate(
            '/login',
            {
                replace: true,
                state: null
            }
        );
    };


    // Base64URL 형태로 변환
    const base64UrlEncode = (array) => {

        const string = String.fromCharCode(...array);

        return btoa(string)
            .replace(/\+/g, '-')
            .replace(/\//g, '_')
            .replace(/=+$/, '');
    };


    // PKCE용 codeVerifier 생성
    const generateCodeVerifier = () => {

        const array = new Uint8Array(32);

        crypto.getRandomValues(array);

        return base64UrlEncode(array);
    };


    // codeVerifier -> codeChallenge
    const generateCodeChallenge = async (codeVerifier) => {

        const encoder = new TextEncoder();

        const data = encoder.encode(codeVerifier);

        const digest = await crypto.subtle.digest(
            'SHA-256',
            data
        );

        return base64UrlEncode(
            new Uint8Array(digest)
        );
    };


    // Google 로그인
    const handleGoogleLogin = async () => {

        // 1. codeVerifier 생성
        const codeVerifier =
            generateCodeVerifier();

        // 2. codeChallenge 생성
        const codeChallenge =
            await generateCodeChallenge(codeVerifier);

        // 3. OAuth 요청 위조 방지용 state 생성
        const state =
            crypto.randomUUID();


        // 4. Callback에서 사용하기 위해 저장
        sessionStorage.setItem(
            'google_code_verifier',
            codeVerifier
        );
        sessionStorage.setItem(
            'google_oauth_state',
            state
        );


        // 5. Google OAuth 요청 정보
        const params = new URLSearchParams({

            client_id:
            import.meta.env.VITE_GOOGLE_CLIENT_ID,

            redirect_uri:
            import.meta.env.VITE_GOOGLE_REDIRECT_URI,

            response_type: 'code',

            scope: 'openid email profile',

            code_challenge: codeChallenge,

            code_challenge_method: 'S256',

            state
        });


        // 6. Google 로그인 페이지 이동
        window.location.href =
            `https://accounts.google.com/o/oauth2/v2/auth?${params.toString()}`;
    };


    return (
        <main className="auth-page">

            {showAlert && (
                <div className="login-alert">

                    <p className="login-alert-message">
                        {location.state?.message}
                    </p>

                    <button
                        type="button"
                        className="login-alert-button"
                        onClick={handleAlertConfirm}
                    >
                        확인
                    </button>

                </div>
            )}


            <div className="login-container">

                <h1 className="login-title">
                    티켓팅 프로젝트
                </h1>

                <button
                    type="button"
                    className="google-login-button"
                    onClick={handleGoogleLogin}
                >
                    Google 계정으로 계속하기
                </button>

                <p className="login-policy">
                    계속하면{' '}

                    <Link to="/auth/terms">
                        이용약관
                    </Link>

                    {' '}및{' '}

                    <Link to="/auth/privacy">
                        개인정보처리방침
                    </Link>

                    에 동의하는 것으로 간주합니다.
                </p>

            </div>

        </main>
    );
}

export default LoginPage;