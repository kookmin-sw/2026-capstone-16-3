package com.example.capstone.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }

    @Bean(name = "kakaoLocalWebClient")
    public WebClient kakaoLocalWebClient(WebClient.Builder builder, @Value("${kakao.local.rest-api-key}") String restApiKey) {
        return builder
                .baseUrl(restApiKey)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK" + restApiKey)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
