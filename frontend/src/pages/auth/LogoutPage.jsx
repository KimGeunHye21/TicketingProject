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

            // 백엔드 로그아웃 요청
            await api.post('/auth/logout');

            // 프론트 Access Token 삭제
            sessionStorage.removeItem(
                'accessToken'
            );

            // 로그인 페이지로 이동
            navigate('/login');

        } catch (error) {

            console.error(
                '로그아웃 실패',
                error
            );

            alert(
                '로그아웃에 실패했습니다.'
            );

        } finally {
            setLoading(false);
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