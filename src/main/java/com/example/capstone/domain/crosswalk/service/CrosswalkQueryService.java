package com.example.capstone.domain.crosswalk.service;

import com.example.capstone.domain.crosswalk.dto.response.CrosswalkNearbyItem;
import com.example.capstone.domain.crosswalk.dto.response.CrosswalkNearbyResponse;
import com.example.capstone.domain.crosswalk.entity.Crosswalk;
import com.example.capstone.domain.crosswalk.repository.CrosswalkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CrosswalkQueryService {

    private final CrosswalkRepository crosswalkRepository;
    private final CrosswalkGuidanceService crosswalkGuidanceService;

    public CrosswalkNearbyResponse findNearby(double latitude, double longitude, double radiusMeters, int limit) {
        List<CrosswalkNearbyItem> items = crosswalkRepository.findAll().stream()
                .filter(c -> c.getLatitude() != null && c.getLongitude() != null)
                .map(c -> toItem(c, latitude, longitude))
                .filter(item -> item.distanceMeters() <= radiusMeters)
                .sorted(Comparator.comparing(CrosswalkNearbyItem::distanceMeters))
                .limit(limit)
                .toList();

        return new CrosswalkNearbyResponse(items, items.size());
    }

    private CrosswalkNearbyItem toItem(Crosswalk crosswalk, double userLat, double userLon) {
        double distance = distanceMeters(userLat, userLon, crosswalk.getLatitude(), crosswalk.getLongitude());

        return new CrosswalkNearbyItem(
                crosswalk.getCrslkManageNo(),
                crosswalk.getRoadNm(),
                crosswalk.getRdnmadr() != null ? crosswalk.getRdnmadr() : crosswalk.getLnmadr(),
                crosswalk.getLatitude(),
                crosswalk.getLongitude(),
                distance,
                calculateRiskScore(crosswalk),
                crosswalkGuidanceService.generateStaticGuidance(crosswalk)
        );
    }

    private int calculateRiskScore(Crosswalk c) {
        int score = 0;

        if (c.getCartrkCo() != null) score += c.getCartrkCo() * 10;
        if (c.getEt() != null) score += Math.min(c.getEt().intValue(), 30);
        if (!"Y".equalsIgnoreCase(c.getTfclghtYn())) score += 20;
        if (!"Y".equalsIgnoreCase(c.getSondSgngnrYn())) score += 10;
        if (!"Y".equalsIgnoreCase(c.getBrllBlckYn())) score += 10;
        if (!"Y".equalsIgnoreCase(c.getFtpthLowerYn())) score += 5;

        return score;
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