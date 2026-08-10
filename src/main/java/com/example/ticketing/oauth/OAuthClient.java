package com.example.ticketing.oauth;


import com.example.ticketing.domain.Provider;

public interface OAuthClient {

    Provider getProvider();

    OAuthUserInfo getUserInfo(String authorizationCode);
}
