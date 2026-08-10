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

import com.example.ticketing.repository.UserRepository;
import com.example.ticketing.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final GoogleOAuthClient googleOAuthClient;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    // 로그인
    public LoginResponse login(Provider provider, LoginRequest request) {

        // TODO: Authorization Code로 Google Access Token 받기
        // TODO: Google Access Token으로 사용자 정보 조회
        OAuthClient oauthClient = switch (provider) {
            case GOOGLE -> googleOAuthClient;
            default -> throw new IllegalArgumentException(
                    "지원하지 않는 로그인 방식입니다."
            );
        };
        OAuthUserInfo userInfo =
                oauthClient.getUserInfo(request.authorizationCode(), request.codeVerifier());

        // TODO: providerId로 기존 회원 조회
        // TODO: 없으면 Google 정보로 User 생성 및 저장
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

        // TODO: Access Token 생성
        // TODO: Refresh Token 생성
        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user);

        // TODO: Refresh Token Redis 저장
        // TODO: Access Token + Refresh Token 응답

        return null;
    }


    // Access Token 재발급
    public TokenResponse refreshAccessToken(RefreshRequest request) {

        // TODO: Refresh Token 추출
        // TODO: Redis에 저장된 Refresh Token과 비교
        // TODO: Refresh Token 유효성 검증
        // TODO: userId 추출
        // TODO: 새로운 Access Token 생성
        // TODO: TokenResponse 생성

        return null;
    }

    // 현재 로그인한 사용자 정보 조회
    public UserResponse getCurrentUser(Long userId) {

        // TODO: userId로 DB에서 User 조회
        // TODO: User -> UserResponse 변환

        return null;
    }

    // 로그아웃
    public void logout(Long userId) {

        // TODO: Redis에서 해당 사용자의 Refresh Token 삭제
    }


    // 회원 탈퇴
    public void withdraw(Long userId) {

        // TODO: Redis에서 Refresh Token 삭제
        // TODO: DB에서 사용자 삭제 또는 탈퇴 처리
    }
}
