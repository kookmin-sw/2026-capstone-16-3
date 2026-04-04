package com.example.capstone.domain.crosswalk.service;

import com.example.capstone.domain.crosswalk.dto.external.CrosswalkApiItem;
import com.example.capstone.domain.crosswalk.dto.request.CrosswalkCandidateRequest;
import com.example.capstone.domain.crosswalk.dto.response.CrosswalkCandidateItem;
import com.example.capstone.domain.crosswalk.dto.response.CrosswalkCandidateResponse;
import com.example.capstone.domain.crosswalk.dto.response.CrosswalkNearbyItem;
import com.example.capstone.domain.crosswalk.dto.response.CrosswalkNearbyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrosswalkCandidateService {

    private static final String SOURCE_NAME = "public-crosswalk-api";

    private final CrosswalkPublicApiService crosswalkPublicApiService;
    private final CrosswalkRegionResolverService crosswalkRegionResolverService;

    public CrosswalkCandidateResponse findCandidates(CrosswalkCandidateRequest request) {
        CrosswalkRegionResolverService.RegionInfo regionInfo =
                crosswalkRegionResolverService.resolve(request.latitude(), request.longitude());

        List<CrosswalkCandidateItem> candidates = crosswalkPublicApiService.fetchByRegion(
                        regionInfo.ctprvnNm(),
                        regionInfo.signguNm()
                ).stream()
                .filter(item -> item.getLatitude() != null && item.getLongitude() != null)
                .map(item -> toCandidateItem(item, request.latitude(), request.longitude()))
                .filter(item -> item.distanceMeters() <= request.radiusMeters())
                .sorted(Comparator.comparing(CrosswalkCandidateItem::distanceMeters))
                .limit(request.maxCandidates())
                .toList();

        return new CrosswalkCandidateResponse(
                UUID.randomUUID().toString(),
                new CrosswalkCandidateResponse.UserLocation(request.latitude(), request.longitude()),
                request.radiusMeters(),
                candidates
        );
    }

    public CrosswalkNearbyResponse findNearbyResponse(CrosswalkCandidateRequest request) {
        List<CrosswalkNearbyItem> items = findCandidates(request).candidates().stream()
                .map(candidate -> new CrosswalkNearbyItem(
                        candidate.candidateId(),
                        candidate.roadName(),
                        null,
                        candidate.latitude(),
                        candidate.longitude(),
                        candidate.distanceMeters(),
                        candidate.pedestrianSignal(),
                        candidate.audioSignal(),
                        candidate.brailleBlock(),
                        candidate.curbLowered(),
                        candidate.source()
                ))
                .toList();

        return new CrosswalkNearbyResponse(items, items.size());
    }

    private CrosswalkCandidateItem toCandidateItem(CrosswalkApiItem item, double userLat, double userLon) {
        double distance = distanceMeters(userLat, userLon, item.getLatitude(), item.getLongitude());

        return new CrosswalkCandidateItem(
                item.getCrslkManageNo(),
                distance,
                item.getLatitude(),
                item.getLongitude(),
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
