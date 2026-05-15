package com.example.capstone.domain.place.dto.response;

public record GeocodeResponse(
        String query,
        String address,
        String roadAddress,
        double lat,
        double lng
) {
}