import axios from "axios";

const api = axios.create({
    baseURL: "http://localhost:8080",
    withCredentials: true
});

// 요청 인터셉터
// 요청 보내기 직전에 Access Token 자동으로 붙이기
api.interceptors.request.use((config) => {

    const accessToken =
        sessionStorage.getItem("accessToken");

    if (accessToken) {
        config.headers.Authorization =
            `Bearer ${accessToken}`;
    }

    return config;
});

// 응답 인터셉터
// Access Token 만료 시 Access Token 재발급 요청
api.interceptors.response.use(
    (response) => response,

    async (error) => {

        const originalRequest = error.config;
        const accessToken =
            sessionStorage.getItem("accessToken");

        // Access Token 만료로 401 발생
        if (
            error.response?.status === 401 &&
            accessToken &&
            originalRequest &&
            !originalRequest._retry
        ) {
            originalRequest._retry = true;

            try {
                const response = await fetch(
                    `${import.meta.env.VITE_API_BASE_URL}/auth/refresh`,
                    {
                        method: "POST",
                        credentials: "include"
                    }
                );

                if (!response.ok) {
                    throw new Error(
                        "Access Token 재발급 실패"
                    );
                }

                const data =
                    await response.json();

                const newAccessToken =
                    data.accessToken;

                sessionStorage.setItem(
                    "accessToken",
                    newAccessToken
                );

                // 기존 실패했던 요청에 새 토큰 설정
                originalRequest.headers.Authorization =
                    `Bearer ${newAccessToken}`;

                // 기존 요청 재시도
                return api(originalRequest);

            } catch (refreshError) {

                sessionStorage.removeItem(
                    "accessToken"
                );

                window.location.href =
                    "/login";

                return Promise.reject(
                    refreshError
                );
            }
        }

        return Promise.reject(error);
    }
);

export default api;