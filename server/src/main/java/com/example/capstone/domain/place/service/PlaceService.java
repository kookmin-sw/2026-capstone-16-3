package com.example.capstone.domain.place.service;

import com.example.capstone.domain.place.dto.response.GeocodeResponse;
import com.example.capstone.domain.place.dto.response.PlaceDetailResponse;
import com.example.capstone.domain.place.dto.response.PlaceResponse;
import com.example.capstone.domain.place.dto.response.ReverseGeocodeResponse;
import com.example.capstone.domain.place.dto.response.SliceResponse;
import com.example.capstone.domain.place.dto.response.kakao.KakaoAddressSearchResponse;
import com.example.capstone.domain.place.dto.response.kakao.KakaoCategorySearchResponse;
import com.example.capstone.domain.place.exception.PlaceErrorCode;
import com.example.capstone.domain.place.exception.PlaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlaceService {

    private static final Logger log = LoggerFactory.getLogger(PlaceService.class);

    private final KakaoLocalClient kakaoLocalClient;
    private final PlaceAddressResolver placeAddressResolver;
    private final PlaceCache placeCache;
    private final PlaceSearchMapper placeSearchMapper;
    private final PlaceRequestValidator placeRequestValidator;
    private final PlaceReverseGeocodeResolver placeReverseGeocodeResolver;
    private final PlaceGeoCalculator placeGeoCalculator;

    public PlaceService(
            KakaoLocalClient kakaoLocalClient,
            PlaceAddressResolver placeAddressResolver,
            PlaceCache placeCache,
            PlaceSearchMapper placeSearchMapper,
            PlaceRequestValidator placeRequestValidator,
            PlaceReverseGeocodeResolver placeReverseGeocodeResolver,
            PlaceGeoCalculator placeGeoCalculator
    ) {
        this.kakaoLocalClient = kakaoLocalClient;
        this.placeAddressResolver = placeAddressResolver;
        this.placeCache = placeCache;
        this.placeSearchMapper = placeSearchMapper;
        this.placeRequestValidator = placeRequestValidator;
        this.placeReverseGeocodeResolver = placeReverseGeocodeResolver;
        this.placeGeoCalculator = placeGeoCalculator;
    }

    public SliceResponse<PlaceResponse> searchNearbyByCategory(
            String categoryCode,
            double lat,
            double lng,
            int radiusM,
            int page,
            int size
    ) {
        String normalizedCode = placeRequestValidator.requireSingleCategoryCode(categoryCode);
        placeRequestValidator.validateLatLng(lat, lng);

        int kakaoPage = placeRequestValidator.normalizeKakaoPage(page);
        int kakaoSize = placeRequestValidator.normalizeKakaoSize(size);

        KakaoCategorySearchResponse response = kakaoLocalClient.searchCategory(
                        normalizedCode,
                        lat,
                        lng,
                        radiusM,
                        kakaoSize,
                        kakaoPage
                )
                .doOnSubscribe(s -> log.info(
                        "[category search] start categoryCode='{}', page={}, size={}, lat={}, lng={}, radius={}",
                        normalizedCode,
                        kakaoPage,
                        kakaoSize,
                        lat,
                        lng,
                        radiusM
                ))
                .doOnError(e -> log.error("[category search] fail categoryCode='{}'", normalizedCode, e))
                .block();

        return toPlaceSliceResponse(response, page, kakaoSize, lat, lng);
    }

    public SliceResponse<PlaceResponse> searchPlaces(
            String query,
            double lat,
            double lng,
            int radiusM,
            int page,
            int size
    ) {
        String normalizedQuery = placeRequestValidator.requireQuery(query);
        placeRequestValidator.validateLatLng(lat, lng);

        int kakaoPage = placeRequestValidator.normalizeKakaoPage(page);
        int kakaoSize = placeRequestValidator.normalizeKakaoSize(size);

        KakaoCategorySearchResponse response = kakaoLocalClient.searchKeywordByAccuracy(
                        normalizedQuery,
                        lat,
                        lng,
                        radiusM,
                        kakaoSize,
                        kakaoPage
                )
                .doOnSubscribe(s -> log.info(
                        "[place search] start query='{}', page={}, size={}, lat={}, lng={}, radius={}",
                        normalizedQuery,
                        kakaoPage,
                        kakaoSize,
                        lat,
                        lng,
                        radiusM
                ))
                .doOnError(e -> log.error("[place search] fail query='{}'", normalizedQuery, e))
                .block();

        return toPlaceSliceResponse(response, page, kakaoSize, lat, lng);
    }

    public PlaceDetailResponse getPlaceDetail(String placeId) {
        if (placeId == null || placeId.isBlank()) {
            throw new PlaceException(PlaceErrorCode.PLACE_BAD_REQUEST);
        }

        KakaoCategorySearchResponse.KakaoPlaceDocument document = placeCache.get(placeId)
                .orElseThrow(() -> new PlaceException(PlaceErrorCode.PLACE_CACHE_MISS));

        double placeLng = parseRequiredDouble(document.x());
        double placeLat = parseRequiredDouble(document.y());
        Long distanceM = placeGeoCalculator.parseDistance(document.distance());

        String resolvedAddress = placeAddressResolver.normalizeAddress(document.addressName());

        String resolvedRoadAddress = placeAddressResolver.resolveRoadAddress(
                document.addressName(),
                document.roadAddressName(),
                placeLat,
                placeLng
        );

        return new PlaceDetailResponse(
                placeId,
                document.placeName(),
                document.categoryName(),
                resolvedAddress,
                resolvedRoadAddress,
                placeLat,
                placeLng,
                distanceM,
                document.phone(),
                document.placeUrl(),
                null,
                new PlaceDetailResponse.Extra("KAKAO")
        );
    }

    public GeocodeResponse geocode(String query) {
        String normalizedQuery = placeRequestValidator.requireQuery(query);

        KakaoAddressSearchResponse response = kakaoLocalClient.searchAddress(normalizedQuery)
                .doOnSubscribe(s -> log.info("Kakao geocode start query='{}'", normalizedQuery))
                .doOnError(e -> log.error("Kakao geocode fail query='{}'", normalizedQuery, e))
                .block();

        if (response == null || response.documents() == null || response.documents().isEmpty()) {
            throw new PlaceException(PlaceErrorCode.PLACE_NOT_FOUND);
        }

        KakaoAddressSearchResponse.Document document = response.documents().getFirst();

        double lng = parseRequiredDouble(document.x());
        double lat = parseRequiredDouble(document.y());

        String address = placeAddressResolver.normalizeAddress(document.addressName());
        String roadAddress = document.roadAddress() != null
                ? document.roadAddress().addressName()
                : null;

        String resolvedRoadAddress = placeAddressResolver.resolveRoadAddress(
                document.addressName(),
                roadAddress,
                lat,
                lng
        );

        return new GeocodeResponse(
                normalizedQuery,
                address,
                resolvedRoadAddress,
                lat,
                lng
        );
    }

    public ReverseGeocodeResponse reverseGeocode(double lat, double lng) {
        placeRequestValidator.validateLatLng(lat, lng);
        return placeReverseGeocodeResolver.resolve(lat, lng);
    }

    private SliceResponse<PlaceResponse> toPlaceSliceResponse(
            KakaoCategorySearchResponse response,
            int requestedPage,
            int kakaoSize,
            double originLat,
            double originLng
    ) {
        if (response == null) {
            return SliceResponse.of(List.of(), requestedPage, kakaoSize, false);
        }

        boolean hasNext = response.meta() != null && !response.meta().isEnd();

        List<PlaceResponse> items = safeDocuments(response).stream()
                .map(document -> placeSearchMapper.toPlaceResponse(document, originLat, originLng))
                .toList();

        return SliceResponse.of(items, requestedPage, kakaoSize, hasNext);
    }

    private List<KakaoCategorySearchResponse.KakaoPlaceDocument> safeDocuments(
            KakaoCategorySearchResponse response
    ) {
        if (response == null || response.documents() == null) {
            return List.of();
        }

        return response.documents();
    }

    private double parseRequiredDouble(String value) {
        Double parsed = placeGeoCalculator.parseDoubleOrNull(value);
        if (parsed == null) {
            throw new PlaceException(PlaceErrorCode.PLACE_EXTERNAL_API_ERROR);
        }

        return parsed;
    }
}