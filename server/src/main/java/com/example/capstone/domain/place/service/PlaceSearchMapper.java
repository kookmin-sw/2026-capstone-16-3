package com.example.capstone.domain.place.service;

import com.example.capstone.domain.place.dto.response.PlaceResponse;
import com.example.capstone.domain.place.dto.response.kakao.KakaoCategorySearchResponse;
import com.example.capstone.domain.place.exception.PlaceErrorCode;
import com.example.capstone.domain.place.exception.PlaceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlaceSearchMapper {

    private static final String PROVIDER_KAKAO = "KAKAO";

    private final PlaceCache placeCache;
    private final PlaceAddressResolver placeAddressResolver;
    private final PlaceGeoCalculator placeGeoCalculator;

    public PlaceResponse toPlaceResponse(
            KakaoCategorySearchResponse.KakaoPlaceDocument document,
            double originLat,
            double originLng
    ) {
        if (document == null) {
            throw new PlaceException(PlaceErrorCode.PLACE_EXTERNAL_API_ERROR);
        }

        String placeId = toExternalPlaceId(PROVIDER_KAKAO, document.id());
        placeCache.put(placeId, document);

        double placeLat = parseRequiredDouble(document.y());
        double placeLng = parseRequiredDouble(document.x());

        Long distanceM = placeGeoCalculator.parseDistance(document.distance());
        if (distanceM == null) {
            distanceM = placeGeoCalculator.haversineMeters(
                    originLat,
                    originLng,
                    placeLat,
                    placeLng
            );
        }

        Integer directionClock = placeGeoCalculator.toDirectionClock(
                originLat,
                originLng,
                placeLat,
                placeLng
        );

        String displayAddress = resolveDisplayAddress(
                document.addressName(),
                document.roadAddressName()
        );

        return new PlaceResponse(
                placeId,
                document.placeName(),
                document.categoryName(),
                displayAddress,
                placeLat,
                placeLng,
                distanceM,
                directionClock
        );
    }

    private double parseRequiredDouble(String value) {
        Double parsed = placeGeoCalculator.parseDoubleOrNull(value);
        if (parsed == null) {
            throw new PlaceException(PlaceErrorCode.PLACE_EXTERNAL_API_ERROR);
        }
        return parsed;
    }

    private String resolveDisplayAddress(String jibunAddress, String roadAddress) {
        if (hasText(roadAddress)) {
            return placeAddressResolver.normalizeAddress(roadAddress);
        }

        return placeAddressResolver.normalizeAddress(jibunAddress);
    }

    private String toExternalPlaceId(String provider, String externalId) {
        return "ext:" + provider + ":" + externalId;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}