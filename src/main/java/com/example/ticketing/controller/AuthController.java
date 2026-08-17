package com.example.ticketing.controller;


import com.example.ticketing.domain.Provider;
import com.example.ticketing.dto.auth.*;
import com.example.ticketing.dto.user.UserResponse;
import com.example.ticketing.security.CustomUserDetails;
import com.example.ticketing.service.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.time.Duration;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // 구글 로그인
    @PostMapping("/login/{provider}")
    public ResponseEntity<LoginResponse> login(
            @PathVariable Provider provider,
            @RequestBody LoginRequest request
    ) {
        LoginResult result = authService.login(provider, request);

        // Refresh Token을 브라우저에 저장할 HttpOnly 쿠키 생성
        ResponseCookie refreshCookie =
                ResponseCookie
                        .from(
                                "refreshToken",
                                result.refreshToken()
                        )
                        .httpOnly(true)
                        .secure(false) // 로컬 개발(http)
                        .path("/")
                        .maxAge(Duration.ofDays(14))
                        .sameSite("Lax") //다른 사이트에서 내 사이트로 요청을 보낼 때 쿠키가 함부로 전송되는 걸 제한하는 보안 설정
                        .build();

        LoginResponse response = new LoginResponse(result.accessToken());

        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie.toString()
                )
                .body(response);
    }

    // Access Token 재발급
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshAccessToken(
            @CookieValue(
                    value = "refreshToken",
                    required = false // 쿠키가 없으면 컨트롤러에서 처리하기 위해 일단 허용
            )
            String refreshToken
    ) {

        if (refreshToken == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED) // 401반환
                    .build();
        }

        TokenResponse response = authService.refreshAccessToken(refreshToken);

        return ResponseEntity.ok(response);
    }

    // 현재 로그인한 사용자 정보 조회
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();

        UserResponse response = authService.getCurrentUser(userId);

        return ResponseEntity.ok(response);
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();

        // Redis의 Refresh Token 삭제
        authService.logout(userId);

        // 브라우저의 Refresh Token Cookie 삭제
        ResponseCookie deleteCookie =
                ResponseCookie
                        .from("refreshToken", "")
                        .httpOnly(true)
                        .secure(false)   // 로컬 개발
                        .path("/")
                        .maxAge(0)
                        .sameSite("Lax")
                        .build();

        return ResponseEntity
                .noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        deleteCookie.toString()
                )
                .build();
    }


    // 회원 탈퇴
    @DeleteMapping("/withdraw")
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();

        authService.withdraw(userId);

        return ResponseEntity.noContent().build();
    }


}

