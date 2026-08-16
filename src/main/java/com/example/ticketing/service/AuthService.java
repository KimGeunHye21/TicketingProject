package com.example.ticketing.service;

import com.example.ticketing.domain.Provider;
import com.example.ticketing.domain.Role;
import com.example.ticketing.domain.User;
import com.example.ticketing.dto.auth.LoginRequest;
import com.example.ticketing.dto.auth.LoginResponse;
import com.example.ticketing.dto.auth.RefreshRequest;
import com.example.ticketing.dto.auth.TokenResponse;
import com.example.ticketing.dto.user.UserResponse;
import com.example.ticketing.oauth.GoogleOAuthClient;
import com.example.ticketing.oauth.OAuthClient;
import com.example.ticketing.oauth.OAuthUserInfo;

import com.example.ticketing.redis.RefreshTokenStore;
import com.example.ticketing.repository.UserRepository;
import com.example.ticketing.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final GoogleOAuthClient googleOAuthClient;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;

    // 로그인
    public LoginResponse login(Provider provider, LoginRequest request) {

        // Authorization Code로 Google Access Token 받기
        // Google Access Token으로 사용자 정보 조회
        OAuthClient oauthClient = switch (provider) {
            case GOOGLE -> googleOAuthClient;
            default -> throw new IllegalArgumentException(
                    "지원하지 않는 로그인 방식입니다."
            );
        };
        OAuthUserInfo userInfo =
                oauthClient.getUserInfo(request.authorizationCode(), request.codeVerifier());

        // providerId로 기존 회원 조회
        // 없으면 Google 정보로 User 생성 및 저장
        User user = userRepository
                .findByProviderAndProviderId(
                        provider,
                        userInfo.providerId()
                )
                .orElseGet(() -> userRepository.save(
                        new User(
                                provider,
                                userInfo.providerId(),
                                userInfo.email(),
                                userInfo.name(),
                                Role.USER
                        )
                ));

        // Access Token 생성
        // Refresh Token 생성
        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user);

        // Refresh Token Redis 저장
        refreshTokenStore.save(
                user.getId(),
                refreshToken,
                Duration.ofDays(14)
        );

        // Access Token + Refresh Token 응답
        return new LoginResponse(
                accessToken,
                refreshToken
        );
    }


    // Access Token 재발급
    public TokenResponse refreshAccessToken(RefreshRequest request) {

        // Refresh Token 추출
        String refreshToken = request.refreshToken();

        // Refresh Token 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException(
                    "유효하지 않은 Refresh Token입니다."
            );
        }
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException(
                    "Refresh Token이 아닙니다."
            );
        }

        // userId 추출
        Long userId = jwtTokenProvider.getUserId(refreshToken);

        // Redis에 저장된 Refresh Token과 비교
        String savedRefreshToken = refreshTokenStore.find(userId);
        if (savedRefreshToken == null
                || !savedRefreshToken.equals(refreshToken)) {

            throw new IllegalArgumentException(
                    "Refresh Token이 일치하지 않습니다."
            );
        }

        // 새로운 Access Token 생성
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 사용자입니다."
                        )
                );
        String newAccessToken = jwtTokenProvider.createAccessToken(user);

        // TokenResponse 생성
        return new TokenResponse(
                newAccessToken
        );
    }

    // 현재 로그인한 사용자 정보 조회
    public UserResponse getCurrentUser(Long userId) {

        // userId로 DB에서 User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 사용자입니다."
                        )
                );

        // User -> UserResponse 변환
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole()
        );
    }

    // 로그아웃
    public void logout(Long userId) {

        // Redis에서 해당 사용자의 Refresh Token 삭제
        refreshTokenStore.delete(userId);
    }


    // 회원 탈퇴
    public void withdraw(Long userId) {

        // Redis에서 Refresh Token 삭제
        refreshTokenStore.delete(userId);

        // DB에서 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 사용자입니다."
                        )
                );

        // 사용자 삭제
        userRepository.delete(user);
    }
}
