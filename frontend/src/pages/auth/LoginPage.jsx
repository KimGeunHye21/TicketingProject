import { useState } from 'react';

function LoginPage() {

    const [agreed, setAgreed] = useState(false);


    // Google 로그인 버튼 클릭
    const handleGoogleLogin = async () => {

        if (!agreed) {
            return;
        }

        // 1. codeVerifier 생성
        const codeVerifier =
            generateCodeVerifier();

        // 2. codeChallenge 생성
        const codeChallenge =
            await generateCodeChallenge(codeVerifier);


        // 3. callback에서 다시 사용해야 하므로 저장
        sessionStorage.setItem(
            'google_code_verifier',
            codeVerifier
        );

        // 4. Google OAuth URL 생성
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


        // 5. Google 로그인 페이지로 이동
        window.location.href =
            `https://accounts.google.com/o/oauth2/v2/auth?${params.toString()}`;
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

    // Base64URL 형태로 변환
    const base64UrlEncode = (array) => {

        const string = String.fromCharCode(...array);

        return btoa(string)
            .replace(/\+/g, '-')
            .replace(/\//g, '_')
            .replace(/=+$/, '');
    };

    return (
        <div>

            <h1>티켓팅 프로젝트</h1>

            <div>
                <label>
                    <input
                        type="checkbox"
                        checked={agreed}
                        onChange={(e) =>
                            setAgreed(e.target.checked)
                        }
                    />

                    [필수] 개인정보 수집·이용 동의
                </label>
            </div>

            <div>
                <button type="button">
                    개인정보 처리방침 보기
                </button>
            </div>

            <div>
                <button
                    onClick={handleGoogleLogin}
                    disabled={!agreed}
                >
                    Google로 로그인
                </button>
            </div>

        </div>
    );
}

export default LoginPage;