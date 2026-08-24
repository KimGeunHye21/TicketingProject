import {
    useEffect,
    useRef
} from 'react';
import { useNavigate } from 'react-router-dom';
import '../../styles/auth.css';
import {
    useAuth
} from '../../contexts/AuthContext';



function OAuthCallbackPage() {

    const navigate = useNavigate();
    const { checkAuth } = useAuth();
    const loginStarted = useRef(false);

    useEffect(() => {
        // 이미 로그인 처리를 시작했다면 다시 실행하지 않음
        if (loginStarted.current) { return; }
        loginStarted.current = true;

        const login = async () => {

            try {
                // 1. Callback URL의 파라미터 가져오기
                const params =
                    new URLSearchParams(
                        window.location.search
                    );


                // 2. OAuth state 검증
                const returnedState =
                    params.get('state');

                const savedState =
                    sessionStorage.getItem(
                        'google_oauth_state'
                    );

                if (
                    !returnedState ||
                    !savedState ||
                    returnedState !== savedState
                ) {
                    throw new Error(
                        'OAuth state가 일치하지 않습니다.'
                    );
                }


                // 3. Authorization Code 가져오기
                const authorizationCode =
                    params.get('code');

                if (!authorizationCode) {
                    throw new Error(
                        'Authorization Code가 없습니다.'
                    );
                }


                // 4. codeVerifier 가져오기
                const codeVerifier =
                    sessionStorage.getItem(
                        'google_code_verifier'
                    );

                if (!codeVerifier) {
                    throw new Error(
                        'Code Verifier가 없습니다.'
                    );
                }


                // 5. 백엔드 로그인 API 호출
                const response = await fetch(
                    `${import.meta.env.VITE_API_BASE_URL}/auth/login/GOOGLE`,
                    {
                        method: 'POST',
                        credentials: 'include',

                        headers: {
                            'Content-Type': 'application/json'
                        },

                        body: JSON.stringify({
                            authorizationCode,
                            codeVerifier
                        })
                    }
                );

                if (!response.ok) {
                    throw new Error(
                        '로그인에 실패했습니다.'
                    );
                }


                // 6. Access Token 저장
                const data =
                    await response.json();

                sessionStorage.setItem(
                    'accessToken',
                    data.accessToken
                );


                // 7. 전역 로그인 상태 갱신
                await checkAuth();


                // 8. 로그인 전 페이지로 이동
                const returnTo =
                    sessionStorage.getItem(
                        'login_return_to'
                    );

                sessionStorage.removeItem(
                    'login_return_to'
                );

                navigate(
                    returnTo || '/',
                    {
                        replace: true
                    }
                );

            } catch (error) {

                console.error(error);

                alert('로그인에 실패했습니다.');

                navigate('/login');

            } finally {

                // 9. OAuth 일회용 값 삭제
                sessionStorage.removeItem(
                    'google_code_verifier'
                );

                sessionStorage.removeItem(
                    'google_oauth_state'
                );
            }
        };


        login();

    }, [navigate, checkAuth]);


    return (
        <main className="auth-page">

            <div className="callback-container">

                <div className="callback-spinner" />

                <p className="callback-message">
                    로그인 처리 중...
                </p>

            </div>

        </main>
    );
}

export default OAuthCallbackPage;