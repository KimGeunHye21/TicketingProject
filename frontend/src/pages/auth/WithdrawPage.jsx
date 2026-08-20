import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import api from '../../api/api';

import '../../styles/auth.css';


function WithdrawPage() {

    const navigate = useNavigate();


    const [loading, setLoading] =
        useState(false);


    const handleWithdraw = async () => {

        try {

            setLoading(true);

            // 백엔드 회원 탈퇴 요청
            await api.delete('/auth/withdraw');

            // 프론트 Access Token 삭제
            sessionStorage.removeItem(
                'accessToken'
            );

            window.location.replace('/');

        } catch (error) {

            console.error(
                '회원 탈퇴 실패',
                error
            );

            alert(
                '회원 탈퇴에 실패했습니다.'
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
                    회원 탈퇴
                </h1>

                <p className="logout-message">
                    정말 회원 탈퇴하시겠습니까?
                    <br />
                    탈퇴한 회원 정보는 복구할 수 없습니다.
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
                        onClick={handleWithdraw}
                        disabled={loading}
                    >
                        {loading
                            ? '탈퇴 처리 중...'
                            : '회원 탈퇴'}
                    </button>

                </div>

            </div>

        </main>
    );
}


export default WithdrawPage;