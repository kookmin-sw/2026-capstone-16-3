package com.example.capstone.domain.place.service;

import com.example.capstone.domain.place.exception.PlaceErrorCode;
import com.example.capstone.domain.place.exception.PlaceException;
import org.springframework.stereotype.Component;

@Component
public class PlaceRequestValidator {

    private static final int MAX_KAKAO_PAGE = 45;
    private static final int MAX_KAKAO_SIZE = 15;

    private static final double MIN_LAT = -90.0;
    private static final double MAX_LAT = 90.0;
    private static final double MIN_LNG = -180.0;
    private static final double MAX_LNG = 180.0;

    private static final double KOREA_MIN_LAT = 33.0;
    private static final double KOREA_MAX_LAT = 39.5;
    private static final double KOREA_MIN_LNG = 124.0;
    private static final double KOREA_MAX_LNG = 132.0;

    public String requireQuery(String query) {
        if (!hasText(query)) {
            throw new PlaceException(PlaceErrorCode.PLACE_BAD_REQUEST);
        }

        return query.trim();
    }

    public String requireSingleCategoryCode(String categoryCode) {
        String normalizedCode = requireQuery(categoryCode).toUpperCase();

        if (normalizedCode.contains(",")) {
            throw new PlaceException(PlaceErrorCode.PLACE_BAD_REQUEST);
        }

        return normalizedCode;
    }

    public void validateLatLng(double lat, double lng) {
        if (lat < MIN_LAT || lat > MAX_LAT || lng < MIN_LNG || lng > MAX_LNG) {
            throw new PlaceException(PlaceErrorCode.PLACE_INVALID_COORDINATE);
        }

        if (lat < KOREA_MIN_LAT || lat > KOREA_MAX_LAT
                || lng < KOREA_MIN_LNG || lng > KOREA_MAX_LNG) {
            throw new PlaceException(PlaceErrorCode.PLACE_INVALID_COORDINATE);
        }
    }

    public int normalizeKakaoPage(int page) {
        return clamp(page, 1, MAX_KAKAO_PAGE);
    }

    public int normalizeKakaoSize(int size) {
        return clamp(size, 1, MAX_KAKAO_SIZE);
    }

    private int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}