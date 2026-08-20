import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import api from '../../api/api';
import '../../styles/auth.css';

function LogoutPage() {

    const navigate = useNavigate();

    const [loading, setLoading] =
        useState(false);


    const handleLogout = async () => {

        try {

            setLoading(true);

            // 서버 로그아웃
            await api.post('/auth/logout');

            // Access Token 삭제
            sessionStorage.removeItem(
                'accessToken'
            );

            // 메인으로 이동 + React 상태 초기화
            window.location.replace('/');

        } catch (error) {

            setLoading(false);

            console.error(
                '로그아웃 실패:',
                error
            );

            alert(
                '로그아웃에 실패했습니다.'
            );
        }
    };


    const handleCancel = () => {
        navigate('/');
    };


    return (
        <main className="auth-page">

            <div className="logout-container">

                <h1 className="logout-title">
                    로그아웃
                </h1>

                <p className="logout-message">
                    정말 로그아웃하시겠습니까?
                </p>

                <div className="logout-buttons">

                    <button
                        type="button"
                        className="logout-cancel-button"
                        onClick={handleCancel}
                        disabled={loading}
                    >
                        취소
                    </button>

                    <button
                        type="button"
                        className="logout-confirm-button"
                        onClick={handleLogout}
                        disabled={loading}
                    >
                        {loading
                            ? '로그아웃 중...'
                            : '로그아웃'}
                    </button>

                </div>

            </div>

        </main>
    );
}

export default LogoutPage;