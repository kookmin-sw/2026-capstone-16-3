package com.example.capstone.domain.crosswalk.service;

import com.example.capstone.domain.crosswalk.entity.AcousticSignal;
import com.example.capstone.domain.crosswalk.entity.Crosswalk;
import com.example.capstone.domain.crosswalk.entity.CrosswalkAcousticSignalMapping;
import com.example.capstone.domain.crosswalk.enums.MatchMethod;
import com.example.capstone.domain.crosswalk.repository.AcousticSignalRepository;
import com.example.capstone.domain.crosswalk.repository.CrosswalkAcousticSignalMappingRepository;
import com.example.capstone.domain.crosswalk.repository.CrosswalkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrosswalkAcousticSignalMatchService {

    private static final double MATCH_DISTANCE_METERS = 20.0;
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private final CrosswalkRepository crosswalkRepository;
    private final AcousticSignalRepository acousticSignalRepository;
    private final CrosswalkAcousticSignalMappingRepository mappingRepository;

    @Transactional
    public void matchCrosswalkAndSignals() {
        List<AcousticSignal> signals = acousticSignalRepository.findAll();

        mappingRepository.deleteAllInBatch();

        int matchedCount = 0;
        int skippedCount = 0;
        LocalDateTime matchedAt = LocalDateTime.now();

        for (AcousticSignal signal : signals) {
            Optional<MatchedCrosswalk> matched = findNearestCrosswalk(signal);

            if (matched.isEmpty()) {
                skippedCount++;
                continue;
            }

            MatchedCrosswalk nearest = matched.get();

            mappingRepository.save(
                    CrosswalkAcousticSignalMapping.builder()
                            .crosswalk(nearest.crosswalk())
                            .acousticSignal(signal)
                            .matchMethod(MatchMethod.COORDINATE_NEAREST)
                            .distanceMeters(nearest.distanceMeters())
                            .confidence(calculateConfidence(nearest.distanceMeters()))
                            .matchedAt(matchedAt)
                            .build()
            );

            matchedCount++;
        }

        log.info(
                "[CROSSWALK ACOUSTIC SIGNAL MATCH SUMMARY] signalCount={}, crosswalkCount={}, matchedCount={}, skippedCount={}, failureReasons={{NO_NEARBY_CROSSWALK={}}}, maxDistanceMeters={}",
                signals.size(),
                crosswalkRepository.count(),
                matchedCount,
                skippedCount,
                skippedCount,
                MATCH_DISTANCE_METERS
        );
    }

    private Optional<MatchedCrosswalk> findNearestCrosswalk(AcousticSignal signal) {
        BoundingBox box = createBoundingBox(
                signal.getLatitude(),
                signal.getLongitude()
        );

        List<Crosswalk> candidates = crosswalkRepository.findCandidatesByBoundingBox(
                box.minLatitude(),
                box.maxLatitude(),
                box.minLongitude(),
                box.maxLongitude()
        );

        return candidates.stream()
                .map(crosswalk -> new MatchedCrosswalk(
                        crosswalk,
                        calculateDistanceMeters(
                                signal.getLatitude(),
                                signal.getLongitude(),
                                crosswalk.getLatitude(),
                                crosswalk.getLongitude()
                        )
                ))
                .filter(match -> match.distanceMeters() <= MATCH_DISTANCE_METERS)
                .min(Comparator.comparingDouble(MatchedCrosswalk::distanceMeters));
    }

    private BoundingBox createBoundingBox(double lat, double lng) {
        double latDelta = Math.toDegrees(MATCH_DISTANCE_METERS / EARTH_RADIUS_METERS);
        double lngDelta = Math.toDegrees(
                MATCH_DISTANCE_METERS / (EARTH_RADIUS_METERS * Math.cos(Math.toRadians(lat)))
        );

        return new BoundingBox(
                lat - latDelta,
                lat + latDelta,
                lng - lngDelta,
                lng + lngDelta
        );
    }

    private double calculateDistanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }

    private double calculateConfidence(double distanceMeters) {
        double confidence = 1.0 - (distanceMeters / MATCH_DISTANCE_METERS) * 0.5;
        return Math.max(0.5, Math.min(1.0, confidence));
    }

    private record MatchedCrosswalk(
            Crosswalk crosswalk,
            double distanceMeters
    ) {
    }

    private record BoundingBox(
            double minLatitude,
            double maxLatitude,
            double minLongitude,
            double maxLongitude
    ) {
    }
}