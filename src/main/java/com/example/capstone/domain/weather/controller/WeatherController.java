package com.example.capstone.domain.weather.controller;

import com.example.capstone.domain.weather.dto.response.WeatherResponse;
import com.example.capstone.domain.weather.service.WeatherService;
import com.example.capstone.global.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping
    public ApiResponse<WeatherResponse> getWeather(
            @RequestParam double lat,
            @RequestParam double lon
    ) {
        return ApiResponse.ok(weatherService.getWeather(lat, lon));
    }
}
