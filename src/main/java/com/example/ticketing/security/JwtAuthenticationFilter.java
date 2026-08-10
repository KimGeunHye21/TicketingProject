package com.example.ticketing.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null
                && jwtTokenProvider.validateToken(token)
                && jwtTokenProvider.isAccessToken(token)) {

            Long userId = jwtTokenProvider.getUserId(token);

            CustomUserDetails userDetails = new CustomUserDetails(userId);

            // 로그인 사용자 정보를 담은 인증 객체 생성
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            Collections.emptyList()
                    );

            // "이 요청은 이 사용자로 로그인되어 있다"고 Spring Security에 등록
            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);
        }

        // 다음 필터 / 최종적으로 Controller까지 요청 계속 진행
        filterChain.doFilter(request, response);
    }

    // Authorization: Bearer {token} 에서 token 부분만 추출
    private String resolveToken(
            HttpServletRequest request
    ) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }

        return authorization.substring(7);
    }

}
