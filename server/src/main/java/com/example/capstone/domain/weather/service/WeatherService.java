package com.example.capstone.domain.weather.service;

import com.example.capstone.domain.weather.dto.response.OpenWeatherResponse;
import com.example.capstone.domain.weather.dto.response.WeatherResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class WeatherService {

    private final WebClient weatherWebClient;
    private final String appKey;

    public WeatherService(
            @Qualifier("weatherWebClient") WebClient weatherWebClient,
            @Value("${weather.app-key}") String appKey
    ) {
        this.weatherWebClient = weatherWebClient;
        this.appKey = appKey;
    }

    public WeatherResponse getWeather(double lat, double lon) {
        OpenWeatherResponse response = weatherWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/data/2.5/weather")
                        .queryParam("lat", lat)
                        .queryParam("lon", lon)
                        .queryParam("appid", appKey)
                        .queryParam("units", "metric")
                        .build())
                .retrieve()
                .bodyToMono(OpenWeatherResponse.class)
                .block();

        if (response == null || response.weather() == null || response.weather().isEmpty()) {
            throw new RuntimeException("날씨 데이터를 불러올 수 없습니다.");
        }

        return new WeatherResponse(
                response.weather().get(0).main(),
                response.weather().get(0).description(),
                response.main().temp(),
                response.wind().speed()
        );
    }
}
