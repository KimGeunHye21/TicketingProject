package com.example.ticketing.oauth;

import com.example.ticketing.domain.Provider;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class GoogleOAuthClient implements OAuthClient {

    private static final String GOOGLE_TOKEN_URL =
            "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USER_INFO_URL =
            "https://openidconnect.googleapis.com/v1/userinfo";
    private final RestClient restClient = RestClient.create();

    @Value("${oauth.google.client-id}")
    private String clientId;
    @Value("${oauth.google.client-secret}")
    private String clientSecret;
    @Value("${oauth.google.redirect-uri}")
    private String redirectUri;

    @Override
    public Provider getProvider() {
        return Provider.GOOGLE;
    }

    @Override
    public OAuthUserInfo getUserInfo(String authorizationCode, String codeVerifier) {

        // TODO authorizationCode로 Google Access Token 요청
        String accessToken =
                getGoogleAccessToken(
                        authorizationCode,
                        codeVerifier
                );

        // TODO Google 사용자 정보 조회
        GoogleUserResponse googleUser =
                getGoogleUserInfo(accessToken);

        // TODO OAuthUserInfo로 변환
        return new OAuthUserInfo(
                Provider.GOOGLE,
                googleUser.sub(),
                googleUser.email(),
                googleUser.name()
        );
    }

    // 1. Authorization Code -> Google Access Token
    private String getGoogleAccessToken(
            String authorizationCode,
            String codeVerifier
    ) {

        MultiValueMap<String, String> request = new LinkedMultiValueMap<>();

        request.add("code", authorizationCode);
        request.add("client_id", clientId);
        request.add("client_secret", clientSecret);
        request.add("redirect_uri", redirectUri);
        request.add("grant_type", "authorization_code");
        request.add("code_verifier", codeVerifier);

        GoogleTokenResponse response = restClient.post()
                .uri(GOOGLE_TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(request)
                .retrieve()
                .body(GoogleTokenResponse.class);

        if (response == null || response.accessToken() == null) {
            throw new IllegalStateException(
                    "Google Access Token 발급에 실패했습니다."
            );
        }

        return response.accessToken();
    }
    private record GoogleTokenResponse(
            @JsonProperty("access_token")
            String accessToken
    ) {
    }

    // 2. Google Access Token -> Google 사용자 정보
    private GoogleUserResponse getGoogleUserInfo(
            String accessToken
    ) {

        GoogleUserResponse response = restClient.get()
                .uri(GOOGLE_USER_INFO_URL)
                .headers(headers ->
                        headers.setBearerAuth(accessToken)
                )
                .retrieve()
                .body(GoogleUserResponse.class);

        if (response == null || response.sub() == null) {
            throw new IllegalStateException(
                    "Google 사용자 정보 조회에 실패했습니다."
            );
        }

        return response;
    }
    private record GoogleUserResponse(
            String sub,
            String email,
            String name
    ) {
    }



}
