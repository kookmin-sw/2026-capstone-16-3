package com.example.capstone.domain.place.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ReverseGeocodeResponse(
        @Schema(description = "지번 주소")
        String jibunAddress,
        @Schema(description = "도로명 주소")
        String roadAddress,
        @Schema(description = "행정동 주소")
        String regionAddress,
        String placeName,
        @Schema(description = "현재 좌표와 근처 장소 사이 거리(m)")
        Long distanceM,
        @Schema(description = "역지오코딩 결과 출처")
        String source,
        @Schema(description = "요청 위도")
        double lat,
        @Schema(description = "요청 경도")
        double lng
) {
    public static ReverseGeocodeResponse ofAddress(
            String jibunAddress,
            String roadAddress,
            double lat,
            double lng
    ) {
        return new ReverseGeocodeResponse(
                jibunAddress,
                roadAddress,
                null,
                null,
                null,
                "KAKAO_ADDRESS",
                lat,
                lng
        );
    }

    public static ReverseGeocodeResponse ofRegion(
            String regionAddress,
            double lat,
            double lng
    ) {
        return new ReverseGeocodeResponse(
                null,
                null,
                regionAddress,
                null,
                null,
                "KAKAO_REGION_FALLBACK",
                lat,
                lng
        );
    }

    public static ReverseGeocodeResponse ofNearestPlace(
            String jibunAddress,
            String roadAddress,
            String regionAddress,
            String placeName,
            Long distanceM,
            double lat,
            double lng
    ) {
        return new ReverseGeocodeResponse(
                jibunAddress,
                roadAddress,
                regionAddress,
                placeName,
                distanceM,
                "KAKAO_NEAREST_PLACE_FALLBACK",
                lat,
                lng
        );
    }
}