package com.example.capstone.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Bean
    @Qualifier("defaultWebClient")
    public WebClient defaultWebClient() {
        return WebClient.builder().build();
    }

    @Bean
    @Qualifier("weatherWebClient")
    public WebClient weatherWebClient(
            @Value("${weather.base-url}") String baseUrl,
            @Value("${weather.connect-timeout-millis}") int connectTimeoutMillis,
            @Value("${weather.read-timeout-seconds}") int readTimeoutSeconds
    ) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis)
                .responseTimeout(Duration.ofSeconds(readTimeoutSeconds))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(readTimeoutSeconds, TimeUnit.SECONDS)));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
  
    @Bean
    @Qualifier("tmapWebClient")
    public WebClient tmapWebClient(
            @Value("${sk.tmap.base-url}") String baseUrl,
            @Value("${sk.tmap.connect-timeout-millis}") int connectTimeoutMillis,
            @Value("${sk.tmap.read-timeout-seconds}") int readTimeoutSeconds
    ) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis)
                .responseTimeout(Duration.ofSeconds(readTimeoutSeconds))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(readTimeoutSeconds, TimeUnit.SECONDS)));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
  
    @Bean
    @Qualifier("fastApiWebClient")
    public WebClient fastApiWebClient(
            @Value("${ai.fastapi.base-url}") String baseUrl,
            @Value("${ai.fastapi.connect-timeout-millis}") int connectTimeoutMillis,
            @Value("${ai.fastapi.read-timeout-seconds}") int readTimeoutSeconds
    ) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis)
                .responseTimeout(Duration.ofSeconds(readTimeoutSeconds))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(readTimeoutSeconds, TimeUnit.SECONDS)));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
  
    @Bean
    @Qualifier("kakaoWebClient")
    public WebClient kakaoWebClient(
            @Value("${kakao.local.base-url:https://dapi.kakao.com}") String baseUrl,
            @Value("${kakao.local.rest-api-key}") String restApiKey
    ) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + restApiKey)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    @Qualifier("naverWebClient")
    public WebClient naverWebClient(
            @Value("${naver.map.base-url}") String baseUrl,
            @Value("${naver.map.connect-timeout-millis}") int connectTimeoutMillis,
            @Value("${naver.map.read-timeout-seconds}") int readTimeoutSeconds
    ) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis)
                .responseTimeout(Duration.ofSeconds(readTimeoutSeconds))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(readTimeoutSeconds, TimeUnit.SECONDS)));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
