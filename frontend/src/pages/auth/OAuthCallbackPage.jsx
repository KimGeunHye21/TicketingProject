import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import '../../styles/auth.css';
import {
    useAuth
} from '../../contexts/AuthContext';

function OAuthCallbackPage() {

    const navigate = useNavigate();
    const { checkAuth } = useAuth();

    useEffect(() => {

        const login = async () => {

            try {
                // 1. URL에서 authorizationCode 꺼내기
                const params =
                    new URLSearchParams(
                        window.location.search
                    );

                const authorizationCode =
                    params.get('code');

                if (!authorizationCode) {
                    throw new Error(
                        'Authorization Code가 없습니다.'
                    );
                }


                // 2. LoginPage에서 저장했던 codeVerifier 가져오기
                const codeVerifier =
                    sessionStorage.getItem(
                        'google_code_verifier'
                    );

                if (!codeVerifier) {
                    throw new Error(
                        'Code Verifier가 없습니다.'
                    );
                }


                // 3. Spring Boot 로그인 API 호출
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


                // 4. JWT 받기
                const data =
                    await response.json();


                // 5. JWT 저장
                sessionStorage.setItem(
                    'accessToken',
                    data.accessToken
                );


                // 6. 사용 완료한 codeVerifier 삭제
                sessionStorage.removeItem(
                    'google_code_verifier'
                );

                // 7. AuthContext 로그인 상태 갱신
                await checkAuth();

                // 8. 메인 페이지 이동
                navigate('/');

            } catch (error) {

                console.error(error);

                alert('로그인에 실패했습니다.');

                navigate('/login');
            }
        };


        login();

    }, [navigate]);


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