import { Navigate } from 'react-router-dom';

import {
    useAuth
} from '../contexts/AuthContext';


function ProtectedRoute({ children }) {

    const {
        user,
        isLoggedIn,
        loading
    } = useAuth();

    console.log(
        'ProtectedRoute:',
        {
            user,
            isLoggedIn,
            loading
        }
    );

    // 로그인 상태 확인 중
    if (loading) {
        return null;
    }


    // 로그인 안 되어 있으면 로그인 페이지로 이동
    if (!isLoggedIn) {
        return (
            <Navigate
                to="/login"
                replace
                state={{
                    message:
                        '로그인이 필요한 페이지입니다.'
                }}
            />
        );
    }


    return children;
}


export default ProtectedRoute;