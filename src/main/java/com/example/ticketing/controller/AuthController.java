package com.example.ticketing.controller;


import com.example.ticketing.domain.Provider;
import com.example.ticketing.dto.auth.LoginRequest;
import com.example.ticketing.dto.auth.LoginResponse;
import com.example.ticketing.dto.auth.RefreshRequest;
import com.example.ticketing.dto.auth.TokenResponse;
import com.example.ticketing.dto.user.UserResponse;
import com.example.ticketing.security.CustomUserDetails;
import com.example.ticketing.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

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
        LoginResponse response = authService.login(provider, request);

        return ResponseEntity.ok(response);
    }

    // Access Token 재발급
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshAccessToken(
            @RequestBody RefreshRequest request
    ) {
        TokenResponse response = authService.refreshAccessToken(request);

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

        authService.logout(userId);

        return ResponseEntity.noContent().build();
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

