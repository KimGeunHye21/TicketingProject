package com.example.ticketing.oauth;

import com.example.ticketing.domain.Provider;

public class GoogleOAuthClient implements OAuthClient {

    @Override
    public Provider getProvider() {
        return Provider.GOOGLE;
    }

    @Override
    public OAuthUserInfo getUserInfo(String authorizationCode) {

        // TODO authorizationCode로 Google Access Token 요청
        // TODO Google 사용자 정보 조회
        // TODO OAuthUserInfo로 변환

        return null;
    }
}
