import {
    createContext,
    useCallback,
    useContext,
    useEffect,
    useState
} from 'react';

import api from '../api/api';


// 로그인 정보를 담을 Context 생성
const AuthContext = createContext(null);


export function AuthProvider({ children }) {

    // 로그인한 사용자 정보
    const [user, setUser] =
        useState(null);

    // 로그인 상태 확인이 끝났는지
    const [loading, setLoading] =
        useState(true);


    // 현재 로그인 상태 확인
    const checkAuth =
        useCallback(async () => {

            // 액세스 토큰이 존재하는 경우에만 /auth/me 호출
            const accessToken =
                sessionStorage.getItem('accessToken');

            if (!accessToken) {
                setUser(null);
                setLoading(false);
                return;
            }

            try {

                const response =
                    await api.get('/auth/me');

                setUser(response.data);

            } catch (error) {

                setUser(null);

            } finally {

                setLoading(false);
            }

        }, []);


    // 앱 처음 실행될 때 로그인 상태 확인
    useEffect(() => {

        void checkAuth();

    }, [checkAuth]);


    // 로그인 여부
    const isLoggedIn =
        user !== null;

    console.log(
        'AuthContext 상태:',
        {
            user,
            isLoggedIn,
            loading,
            accessToken:
                sessionStorage.getItem('accessToken')
        }
    );

    return (
        <AuthContext.Provider
            value={{
                user,
                isLoggedIn,
                loading,
                checkAuth,
                setUser
            }}
        >
            {children}
        </AuthContext.Provider>
    );
}


// 다른 컴포넌트에서 편하게 사용하기 위한 함수
export function useAuth() {

    return useContext(AuthContext);
}