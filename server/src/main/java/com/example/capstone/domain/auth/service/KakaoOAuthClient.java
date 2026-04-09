package com.example.capstone.domain.auth.service;

import com.example.capstone.domain.auth.dto.response.KakaoUserResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class KakaoOAuthClient {

    private final WebClient webClient;

    public KakaoOAuthClient(@Qualifier("defaultWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public KakaoUserResponse getUserInfo(String kakaoAccessToken) {
        return webClient.get()
                .uri("https://kapi.kakao.com/v2/user/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .map(body -> new RuntimeException("Kakao user info error: " + body))
                )
                .bodyToMono(KakaoUserResponse.class)
                .block();
    }
}
