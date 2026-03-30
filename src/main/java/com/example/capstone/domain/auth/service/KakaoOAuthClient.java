package com.example.capstone.domain.auth.service;

import com.example.capstone.domain.auth.dto.response.KakaoTokenResponse;
import com.example.capstone.domain.auth.dto.response.KakaoUserResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class KakaoOAuthClient {

    private final WebClient webClient;

    public KakaoOAuthClient(@Qualifier("defaultWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @Value("${kakao.client-id}") private String clientId;
    @Value("${kakao.client-secret}") private String clientSecret;
    @Value("${kakao.redirect-uri}") private String redirectUri;

    public KakaoTokenResponse getToken(String code) {
        return webClient.post()
                .uri("https://kauth.kakao.com/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters
                        .fromFormData("grant_type", "authorization_code")
                        .with("client_id", clientId)
                        .with("redirect_uri", redirectUri)
                        .with("code", code)
                        .with("client_secret", clientSecret)
                )
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class).map(body -> new RuntimeException("Kakao token error: " + body))
                )
                .bodyToMono(KakaoTokenResponse.class)
                .block();
    }

    public KakaoUserResponse getUserInfo(String accessToken) {
        return webClient.get()
                .uri("https://kapi.kakao.com/v2/user/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(KakaoUserResponse.class)
                .block();
    }
}
