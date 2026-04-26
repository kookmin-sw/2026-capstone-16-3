package com.example.capstone.domain.place.dto.response;

public record ReverseGeocodeResponse(
        String address,
        String roadAddress,
        String regionAddress,
        String placeName,
        Long distanceMeters,
        String source,
        double lat,
        double lng
) {
    public static ReverseGeocodeResponse ofAddress(
            String address,
            String roadAddress,
            double lat,
            double lng
    ) {
        return new ReverseGeocodeResponse(
                address,
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
            String address,
            String roadAddress,
            String regionAddress,
            String placeName,
            Long distanceMeters,
            double lat,
            double lng
    ) {
        return new ReverseGeocodeResponse(
                address,
                roadAddress,
                regionAddress,
                placeName,
                distanceMeters,
                "KAKAO_NEAREST_PLACE_FALLBACK",
                lat,
                lng
        );
    }
}