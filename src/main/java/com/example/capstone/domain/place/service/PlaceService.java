package com.example.capstone.domain.place.service;

import com.example.capstone.domain.place.dto.response.*;
import com.example.capstone.domain.place.dto.response.kakao.KakaoAddressSearchResponse;
import com.example.capstone.domain.place.dto.response.kakao.KakaoCategorySearchResponse;
import com.example.capstone.domain.place.dto.response.kakao.KakaoCoordToAddressResponse;
import com.example.capstone.domain.place.exception.PlaceErrorCode;
import com.example.capstone.domain.place.exception.PlaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.concurrent.TimeoutException;

@Service
public class PlaceService {

    private final KakaoLocalClient kakaoLocalClient;
    private final PlaceCache placeCache;

    private static final int MAX_PAGE = 45;
    private static final int MAX_SIZE = 15;
    private static final Logger log = LoggerFactory.getLogger(PlaceService.class);

    public PlaceService(KakaoLocalClient kakaoLocalClient, PlaceCache placeCache) {
        this.kakaoLocalClient = kakaoLocalClient;
        this.placeCache = placeCache;
    }

    public SliceResponse<PlaceResponse> searchCategory(
            String categoryCode,
            double lat,
            double lng,
            int radiusM,
            int page,
            int size
    ) {
        validateQuery(categoryCode);
        validateLatLng(lat, lng);

        int kakaoPage = Math.min(Math.max(page, 1), MAX_PAGE);
        int kakaoSize = Math.min(Math.max(size, 1), MAX_SIZE); // Kakao limit

        try {
            KakaoCategorySearchResponse resp = kakaoLocalClient.searchCategory(
                            categoryCode.trim(), lat, lng, radiusM, kakaoSize, kakaoPage
                    )
                    .doOnSubscribe(s -> log.info("[category search] start categoryCode='{}', page={}, size={}, lat={}, lng={}, radius={}",
                            categoryCode, kakaoPage, kakaoSize, lat, lng, radiusM))
                    .doOnError(e -> log.error("[category search] fail categoryCode='{}'", categoryCode, e))
                    .block();

            if (resp == null) {
                return new SliceResponse<>(List.of(), page, kakaoSize, false, null);
            }

            boolean hasNext = resp.meta() != null && !resp.meta().isEnd();

            List<PlaceResponse> items =
                    (resp.documents() == null ? List.<KakaoCategorySearchResponse.KakaoPlaceDocument>of() : resp.documents())
                            .stream()
                            .map(doc -> toSearchPlace(doc, lat, lng))
                            .toList();

            return SliceResponse.of(items, page, kakaoSize, hasNext);
        } catch (WebClientResponseException e) {
            throw new PlaceException(
                    e.getStatusCode().is4xxClientError()
                            ? PlaceErrorCode.PLACE_EXTERNAL_API_HTTP_4XX
                            : PlaceErrorCode.PLACE_EXTERNAL_API_HTTP_5XX
            );
        } catch (WebClientRequestException e) {
            throw new PlaceException(
                    PlaceErrorCode.PLACE_EXTERNAL_API_CONNECTION_ERROR
            );
        } catch (PlaceException e) {
            throw e;
        } catch (Exception e) {
            Throwable cause = e.getCause();

            if (cause instanceof TimeoutException) {
                throw new PlaceException(
                        PlaceErrorCode.PLACE_EXTERNAL_API_TIMEOUT
                );
            }

            throw new PlaceException(
                    PlaceErrorCode.PLACE_EXTERNAL_API_ERROR
            );
        }
    }

    public SliceResponse<PlaceResponse> searchPlaces(
            String query,
            double lat,
            double lng,
            int radiusM,
            int page,
            int size
    ) {
        validateQuery(query);
        validateLatLng(lat, lng);

        int kakaoPage = Math.min(Math.max(page, 1), MAX_PAGE);
        int kakaoSize = Math.min(Math.max(size, 1), MAX_SIZE); // Kakao limit

        try {
            KakaoCategorySearchResponse resp = kakaoLocalClient.searchKeyword(
                            query.trim(), lat, lng, radiusM, kakaoSize, kakaoPage
                    )
                    .doOnSubscribe(s -> log.info("[place search] start query='{}', page={}, size={}, lat={}, lng={}, radius={}",
                            query, kakaoPage, kakaoSize, lat, lng, radiusM))
                    .doOnError(e -> log.error("[place search] fail query='{}'", query, e))
                    .block();

            if (resp == null) {
                return new SliceResponse<>(List.of(), page, kakaoSize, false, null);
            }

            boolean hasNext = resp.meta() != null && !resp.meta().isEnd();

            List<PlaceResponse> items =
                    (resp.documents() == null ? List.<KakaoCategorySearchResponse.KakaoPlaceDocument>of() : resp.documents())
                            .stream()
                            .map(doc -> toSearchPlace(doc, lat, lng))
                            .toList();
            return SliceResponse.of(items, page, kakaoSize, hasNext);
        } catch (WebClientResponseException e) {
            throw new PlaceException(
                    e.getStatusCode().is4xxClientError()
                    ? PlaceErrorCode.PLACE_EXTERNAL_API_HTTP_4XX
                    : PlaceErrorCode.PLACE_EXTERNAL_API_HTTP_5XX
            );
        } catch (WebClientRequestException e) {
            throw new PlaceException(
                    PlaceErrorCode.PLACE_EXTERNAL_API_CONNECTION_ERROR
            );
        } catch (PlaceException e) {
            throw e;
        } catch (Exception e) {
            Throwable cause = e.getCause();

            if (cause instanceof TimeoutException) {
                throw new PlaceException(
                        PlaceErrorCode.PLACE_EXTERNAL_API_TIMEOUT
                );
            }

            throw new PlaceException(
                    PlaceErrorCode.PLACE_EXTERNAL_API_ERROR
            );
        }
    }

    public PlaceDetailResponse getPlaceDetail(String placeId) {
        if (placeId == null || placeId.isBlank()) {
            throw new PlaceException(PlaceErrorCode.PLACE_BAD_REQUEST);
        }

        KakaoCategorySearchResponse.KakaoPlaceDocument doc = placeCache.get(placeId)
                .orElseThrow(() -> new PlaceException(
                        PlaceErrorCode.PLACE_CACHE_MISS
                ));

        double placeLng = Double.parseDouble(doc.x());
        double placeLat = Double.parseDouble(doc.y());
        Long dist = (doc.distance() == null || doc.distance().isBlank()) ? null : Long.parseLong(doc.distance());

        return new PlaceDetailResponse(
                placeId,
                doc.placeName(),
                doc.categoryName(),
                doc.addressName(),
                doc.roadAddressName(),
                placeLat,
                placeLng,
                dist,
                doc.phone(),
                doc.placeUrl(),
                null, // openingHours: Kakao Local REST에서 제공되지 않음
                new PlaceDetailResponse.Extra("KAKAO")
        );
    }

    private PlaceResponse toSearchPlace(
            KakaoCategorySearchResponse.KakaoPlaceDocument d,
            double originLat,
            double originLng
    ) {
        String placeId = toExternalPlaceId("KAKAO", d.id());
        placeCache.put(placeId, d);

        double placeLng = Double.parseDouble(d.x());
        double placeLat = Double.parseDouble(d.y());
        Long dist = (d.distance() == null || d.distance().isBlank()) ? null : Long.parseLong(d.distance());
        if (dist == null) {
            dist = haversineMeters(originLat, originLng, placeLat, placeLng);
        }

        Integer directionClock = toDirectionClock(originLat, originLng, placeLat, placeLng);

        return new PlaceResponse(
                placeId,
                d.placeName(),
                d.categoryName(),
                d.roadAddressName(),
                placeLat,
                placeLng,
                dist,
                directionClock
        );
    }

    public GeocodeResponse geocode(String query) {
        if (query == null || query.isBlank()) {
            throw new PlaceException(PlaceErrorCode.PLACE_BAD_REQUEST);
        }

        KakaoAddressSearchResponse resp = kakaoLocalClient.searchAddress(query.trim())
                .doOnSubscribe(s -> log.info("Kakao geocode start query='{}'", query))
                .doOnError(e -> log.error("Kakao geocode fail query='{}'", query, e))
                .block();

        if (resp == null || resp.documents() == null || resp.documents().isEmpty()) {
            throw new PlaceException(PlaceErrorCode.PLACE_NOT_FOUND);
        }

        KakaoAddressSearchResponse.Document doc = resp.documents().getFirst();

        try {
            double lng = Double.parseDouble(doc.x());
            double lat = Double.parseDouble(doc.y());

            String roadAddress = doc.roadAddress() != null ? doc.roadAddress().addressName() : null;

            return new GeocodeResponse(
                    query.trim(),
                    doc.addressName(),
                    roadAddress,
                    lat,
                    lng
            );
        } catch (Exception e) {
            throw new PlaceException(PlaceErrorCode.PLACE_EXTERNAL_API_ERROR);
        }
    }

    public ReverseGeocodeResponse reverseGeocode(double lat, double lng) {
        validateLatLng(lat, lng);

        try {
            KakaoCoordToAddressResponse resp = kakaoLocalClient.coordToAddress(lat, lng)
                    .doOnSubscribe(s -> log.info("Kakao reverse geocode start lat={}, lng={}", lat, lng))
                    .doOnError(e -> log.error("Kakao reverse geocode fail lat={}, lng={}", lat, lng, e))
                    .block();

            if (resp == null || resp.documents() == null || resp.documents().isEmpty()) {
                throw new PlaceException(PlaceErrorCode.PLACE_NOT_FOUND);
            }

            KakaoCoordToAddressResponse.Document doc = resp.documents().getFirst();

            String address = doc.address() != null ? doc.address().addressName() : null;
            String roadAddress = doc.roadAddress() != null ? doc.roadAddress().addressName() : null;

            return new ReverseGeocodeResponse(
                    address,
                    roadAddress,
                    lat,
                    lng
            );
        } catch (PlaceException e) {
            throw e;
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                throw new PlaceException(PlaceErrorCode.PLACE_EXTERNAL_API_HTTP_4XX);
            }
            if (e.getStatusCode().is5xxServerError()) {
                throw new PlaceException(PlaceErrorCode.PLACE_EXTERNAL_API_HTTP_5XX);
            }
            throw new PlaceException(PlaceErrorCode.PLACE_EXTERNAL_API_ERROR);
        } catch (WebClientRequestException e) {
            throw new PlaceException(PlaceErrorCode.PLACE_EXTERNAL_API_CONNECTION_ERROR);
        } catch (Exception e) {
            throw new PlaceException(PlaceErrorCode.PLACE_EXTERNAL_API_ERROR);
        }
    }

    private void validateQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new PlaceException(PlaceErrorCode.PLACE_BAD_REQUEST);
        }
    }

    private void validateLatLng(double lat, double lng) {
        if (lat < -90 || lat > 90) {
            throw new PlaceException(PlaceErrorCode.PLACE_INVALID_COORDINATE);
        }
        if (lng < -180 || lng > 180) {
            throw new PlaceException(PlaceErrorCode.PLACE_INVALID_COORDINATE);
        }
    }

    private static long haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (long) (R * c);
    }

    private static String toExternalPlaceId(String provider, String externalId) {
        return "ext:" + provider + ":" + externalId;
    }

    private static Integer toDirectionClock(double originLat, double originLng, double targetLat, double targetLng) {
        double bearing = bearingDegrees(originLat, originLng, targetLat, targetLng);

        // 0도=북(12시), 90도=동(3시) ... 30도 단위로 반올림
        return (int) Math.floor((bearing + 15.0) / 30.0) % 12;
    }

    private static double bearingDegrees(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dLon = Math.toRadians(lon2 - lon1);

        double y = Math.sin(dLon) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2)
                - Math.sin(phi1) * Math.cos(phi2) * Math.cos(dLon);

        double theta = Math.atan2(y, x);
        double deg = Math.toDegrees(theta);

        // 0~360 정규화
        return (deg + 360.0) % 360.0;
    }
}