package com.example.capstone.domain.place.dto.response;

public record ReverseGeocodeResponse(
        String address,
        String roadAddress,
        double lat,
        double lng
) {
}