package com.example.capstone.domain.place.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ReverseGeocodeResponse(
        @Schema(description = "현재 위치 표시 주소. 도로명 주소 -> 지번 주소 -> 행정동 주소 순서")
        String address,

        @Schema(description = "현재 좌표 근처의 장소명")
        String placeName,

        @Schema(description = "현재 좌표와 근처 장소 사이 거리(m)")
        Long distanceM,

        @Schema(description = "응답 생성 기준")
        String source,

        @Schema(description = "요청 위도")
        double lat,

        @Schema(description = "요청 경도")
        double lng
) {
    public static ReverseGeocodeResponse ofAddress(
            String roadAddress,
            String jibunAddress,
            double lat,
            double lng
    ) {
        String address = firstNonBlank(roadAddress, jibunAddress);

        return new ReverseGeocodeResponse(
                address,
                null,
                null,
                hasText(roadAddress) ? "KAKAO_ROAD_ADDRESS" : "KAKAO_JIBUN_ADDRESS",
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
                regionAddress,
                null,
                null,
                "KAKAO_REGION_ADDRESS",
                lat,
                lng
        );
    }

    public static ReverseGeocodeResponse ofNearestPlace(
            String roadAddress,
            String jibunAddress,
            String regionAddress,
            String placeName,
            Long distanceM,
            double lat,
            double lng
    ) {
        String address = firstNonBlank(roadAddress, jibunAddress, regionAddress);

        return new ReverseGeocodeResponse(
                address,
                placeName,
                distanceM,
                selectNearestSource(roadAddress, jibunAddress, regionAddress),
                lat,
                lng
        );
    }

    private static String selectNearestSource(
            String roadAddress,
            String jibunAddress,
            String regionAddress
    ) {
        if (hasText(roadAddress) || hasText(jibunAddress)) {
            return "KAKAO_NEAREST_PLACE";
        }
        if (hasText(regionAddress)) {
            return "KAKAO_REGION_ADDRESS";
        }
        return "KAKAO_ADDRESS_NOT_FOUND";
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    public static ReverseGeocodeResponse ofResolvedAddress(
            String address,
            String source,
            double lat,
            double lng
    ) {
        return new ReverseGeocodeResponse(
                address,
                null,
                null,
                source,
                lat,
                lng
        );
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}