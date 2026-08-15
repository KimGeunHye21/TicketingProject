import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

function OAuthCallbackPage() {

    const navigate = useNavigate();


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


                // {
                //   accessToken: "...",
                //   refreshToken: "..."
                // }


                // 5. JWT 저장
                sessionStorage.setItem(
                    'accessToken',
                    data.accessToken
                );

                sessionStorage.setItem(
                    'refreshToken',
                    data.refreshToken
                );


                // 이제 codeVerifier는 필요 없음
                sessionStorage.removeItem(
                    'google_code_verifier'
                );


                // 6. 메인 페이지 이동
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
        <div>
            로그인 처리 중...
        </div>
    );
}

export default OAuthCallbackPage;