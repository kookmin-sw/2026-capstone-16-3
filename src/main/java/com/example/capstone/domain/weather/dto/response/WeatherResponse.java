package com.example.capstone.domain.weather.dto.response;

public record WeatherResponse(
        String weather,
        String description,
        double temperature,
        double windSpeed
) {
}
