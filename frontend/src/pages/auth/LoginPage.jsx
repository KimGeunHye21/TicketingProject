import { Link } from 'react-router-dom';
import '../../styles/auth.css';

function LoginPage() {

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

        // 3. Callback에서 사용하기 위해 저장
        sessionStorage.setItem(
            'google_code_verifier',
            codeVerifier
        );

        // 4. Google OAuth 요청 정보
        const params = new URLSearchParams({

            client_id:
            import.meta.env.VITE_GOOGLE_CLIENT_ID,

            redirect_uri:
            import.meta.env.VITE_GOOGLE_REDIRECT_URI,

            response_type: 'code',

            scope: 'openid email profile',

            code_challenge: codeChallenge,

            code_challenge_method: 'S256'
        });


        // 5. Google 로그인 페이지 이동
        window.location.href =
            `https://accounts.google.com/o/oauth2/v2/auth?${params.toString()}`;
    };


    return (
        <main className="auth-page">

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