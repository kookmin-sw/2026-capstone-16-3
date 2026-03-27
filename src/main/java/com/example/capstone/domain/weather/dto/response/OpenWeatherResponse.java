package com.example.capstone.domain.weather.dto.response;

import java.util.List;

public record OpenWeatherResponse(
        List<Weather> weather,
        Main main,
        Wind wind
) {
    public record Weather(
            String main,
            String description
    ) {}

    public record Main(
            double temp
    ) {}

    public record Wind(
            double speed
    ) {}
}
