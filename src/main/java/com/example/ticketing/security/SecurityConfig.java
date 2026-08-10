package com.example.ticketing.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        JwtAuthenticationFilter jwtAuthenticationFilter =
                new JwtAuthenticationFilter(jwtTokenProvider);

        http
                .csrf(AbstractHttpConfigurer::disable) // CSRF 보호 기능 끔

                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS // 서버가 로그인 상태를 세션에 저장하지 않음
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // 로그인 전에도 호출 가능
                        .requestMatchers(
                                "/auth/login/**",
                                "/auth/refresh"
                        ).permitAll()

                        // 로그인 필요
                        .requestMatchers(
                                "/auth/me",
                                "/auth/logout",
                                "/auth/withdraw"
                        ).authenticated()

                        // 아직 다른 API 정책은 안 정했으므로 허용
                        .anyRequest().permitAll()
                )

                //JWT 검사 필터를 권한 검사보다 먼저 실행
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        AuthorizationFilter.class
                );

        return http.build();
    }
}
