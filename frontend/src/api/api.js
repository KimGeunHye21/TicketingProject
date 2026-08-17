import axios from "axios";

const api = axios.create({
    baseURL: "http://localhost:8080",
});

// 요청 인터셉터
// 요청 보내기 직전에 Access Token 자동으로 붙이기
api.interceptors.request.use((config) => {

    const accessToken = localStorage.getItem("accessToken");

    if (accessToken) {
        config.headers.Authorization = `Bearer ${accessToken}`;
    }

    return config;
});

// 응답 인터셉터
// 액세스 토큰 만료시 액세스 토큰 재발급 요청
api.interceptors.response.use(
    (response) => response,

    async (error) => {

        const originalRequest = error.config;

        // Access Token 만료로 401 발생
        if (
            error.response?.status === 401 &&
            !originalRequest._retry
        ) {
            originalRequest._retry = true;

            try {
                const refreshToken =
                    localStorage.getItem("refreshToken");

                const response = await axios.post(
                    "http://localhost:8080/auth/refresh",
                    {
                        refreshToken: refreshToken
                    }
                );

                const newAccessToken =
                    response.data.accessToken;

                localStorage.setItem(
                    "accessToken",
                    newAccessToken
                );

                // 기존 실패했던 요청에 새 토큰 설정
                originalRequest.headers.Authorization =
                    `Bearer ${newAccessToken}`;

                // 기존 요청 재시도
                return api(originalRequest);

            } catch (refreshError) {

                // Refresh Token도 만료됐거나 잘못됨
                localStorage.removeItem("accessToken");
                localStorage.removeItem("refreshToken");

                window.location.href = "/login";

                return Promise.reject(refreshError);
            }
        }

        return Promise.reject(error);
    }
);