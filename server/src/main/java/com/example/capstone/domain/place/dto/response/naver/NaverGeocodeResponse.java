package com.example.capstone.domain.place.dto.response.naver;

import java.util.List;

public record NaverGeocodeResponse(
        String status,
        Meta meta,
        List<Address> addresses,
        String errorMessage
) {
    public record Meta(
            int totalCount,
            int page,
            int count
    ) {}

    public record Address(
            String roadAddress,
            String jibunAddress,
            String englishAddress,
            String x,
            String y
    ) {}
}