package com.example.capstone.domain.crosswalk.service;

import com.example.capstone.domain.crosswalk.dto.external.CrosswalkApiItem;
import com.example.capstone.domain.crosswalk.dto.request.CrosswalkCandidateRequest;
import com.example.capstone.domain.crosswalk.dto.response.CrosswalkCandidateItem;
import com.example.capstone.domain.crosswalk.dto.response.CrosswalkCandidateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class CrosswalkCandidateService {

    private static final String SOURCE_NAME = "public-crosswalk-api";

    private final CrosswalkPublicApiService crosswalkPublicApiService;
    private final CrosswalkRegionResolverService crosswalkRegionResolverService;

    public CrosswalkCandidateResponse findCandidates(CrosswalkCandidateRequest request) {
        log.info("userLocation=({}, {}), radius={}",
                request.latitude(), request.longitude(), request.radiusMeters());

        CrosswalkRegionResolverService.RegionInfo regionInfo =
                crosswalkRegionResolverService.resolve(request.latitude(), request.longitude());

        log.info("resolvedRegion ctprvnNm={}, signguNm={}",
                regionInfo.ctprvnNm(), regionInfo.signguNm());

        List<CrosswalkApiItem> rawItems = crosswalkPublicApiService.fetchByRegion(
                        regionInfo.ctprvnNm(),
                        regionInfo.signguNm()
                );

        log.info("raw item count={}", rawItems.size());

        List<CrosswalkApiItem> coordReadyItems = rawItems.stream()
                .filter(item -> item.getLatitude() != null && !item.getLatitude().isBlank())
                .filter(item -> item.getLongitude() != null && !item.getLongitude().isBlank())
                .toList();

        log.info("coordReady item count={}", coordReadyItems.size());

        List<CrosswalkCandidateItem> mappedItems = coordReadyItems.stream()
                .map(item -> toCandidateItem(item, request.latitude(), request.longitude()))
                .toList();

        mappedItems.forEach(item ->
                log.info("candidate distance check id={}, lat={}, lon={}, distance={}",
                        item.candidateId(),
                        item.latitude(),
                        item.longitude(),
                        item.distanceMeters())
        );

        log.info("mapped candidate count before distance filter={}", mappedItems.size());

        List<CrosswalkCandidateItem> filteredByDistance = mappedItems.stream()
                .filter(item -> item.distanceMeters() <= request.radiusMeters())
                .toList();

        log.info("candidate count after distance filter={}", filteredByDistance.size());

        List<CrosswalkCandidateItem> candidates = filteredByDistance.stream()
                .sorted(Comparator.comparing(CrosswalkCandidateItem::distanceMeters))
                .limit(request.maxCandidates())
                .toList();

        log.info("final candidate count after sort/limit={}", candidates.size());

        return new CrosswalkCandidateResponse(
                UUID.randomUUID().toString(),
                new CrosswalkCandidateResponse.UserLocation(request.latitude(), request.longitude()),
                request.radiusMeters(),
                candidates
        );
    }

    private CrosswalkCandidateItem toCandidateItem(CrosswalkApiItem item, double userLat, double userLon) {
        double lat = parseDouble(item.getLatitude(), "latitude");
        double lng = parseDouble(item.getLongitude(), "longitude");

        double distance = distanceMeters(userLat, userLon, lat, lng);

        return new CrosswalkCandidateItem(
                item.getCrslkManageNo(),
                distance,
                lat,
                lng,
                firstNonBlank(item.getRoadNm(), item.getRdnmadr(), item.getLnmadr()),
                isYes(item.getTfclghtYn()),
                isYes(item.getSoundSgngnrYn()),
                isYes(item.getBrllBlckYn()),
                isYes(item.getFtpthLowerYn()),
                SOURCE_NAME
        );
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private double parseDouble(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " 값이 비어 있습니다.");
        }

        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " 숫자 변환 실패: " + value, e);
        }
    }

    private int parseInt(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " 값이 비어 있습니다.");
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " 정수 변환 실패: " + value, e);
        }
    }

    private boolean isYes(String value) {
        return "Y".equalsIgnoreCase(value);
    }

    private double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6371000.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }
}
