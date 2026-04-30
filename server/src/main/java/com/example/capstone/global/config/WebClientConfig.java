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
import org.springframework.web.reactive.function.client.ExchangeStrategies;
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

    @Bean(name="opendataWebClient")
    public WebClient opendataWebClient(
            @Value("${webclient.opendata.connect-timeout-millis:5000}") int connectTimeoutMillis,
            @Value("${webclient.opendata.read-timeout-seconds:20}") int readTimeoutSeconds,
            @Value("${webclient.opendata.max-in-memory-size:4194304}") int maxInMemorySize
    ) {
        HttpClient httpClient = HttpClient.newConnection()
                .keepAlive(false)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis)
                .responseTimeout(Duration.ofSeconds(readTimeoutSeconds))
                .headers(headers -> {
                    headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
                    headers.add(HttpHeaders.USER_AGENT, "Mozilla/5.0");
                    headers.add(HttpHeaders.CONNECTION, "close");
                })
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(readTimeoutSeconds, TimeUnit.SECONDS)));

        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer ->
                        configurer.defaultCodecs().maxInMemorySize(maxInMemorySize))
                .build();

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(exchangeStrategies)
                .build();
    }
}
