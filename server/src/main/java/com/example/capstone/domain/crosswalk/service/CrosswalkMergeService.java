package com.example.capstone.domain.crosswalk.service;

import com.example.capstone.domain.crosswalk.entity.Crosswalk;
import com.example.capstone.domain.crosswalk.enums.DataSourceType;
import com.example.capstone.domain.crosswalk.repository.CrosswalkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrosswalkMergeService {

    private static final double MATCH_DISTANCE_METERS = 30.0;

    private final CrosswalkRepository crosswalkRepository;

    @Transactional
    public void mergeCrosswalkData() {
        List<Crosswalk> seoulCrosswalks = crosswalkRepository.findByBaseSource(
                DataSourceType.SEOUL_TB_TRAFFIC_CRSNG
        );

        int mergedCount = 0;
        int deletedNationalCount = 0;
        int missingSigunguCount = 0;
        int noNationalCandidateCount = 0;
        int noDistanceMatchCount = 0;

        for (Crosswalk seoul : seoulCrosswalks) {
            if (!hasText(seoul.getSigungu())) {
                missingSigunguCount++;
                continue;
            }

            List<Crosswalk> nationalCandidates = crosswalkRepository.findByBaseSourceAndSigungu(
                    DataSourceType.NATIONAL_STANDARD_CROSSWALK,
                    seoul.getSigungu()
            );

            if (nationalCandidates.isEmpty()) {
                noNationalCandidateCount++;
                continue;
            }

            Optional<MatchedCrosswalk> matched = nationalCandidates.stream()
                    .filter(national -> isSameRegion(seoul, national))
                    .map(national -> new MatchedCrosswalk(
                            national,
                            calculateDistanceMeters(
                                    seoul.getLatitude(),
                                    seoul.getLongitude(),
                                    national.getLatitude(),
                                    national.getLongitude()
                            )
                    ))
                    .filter(match -> match.distanceMeters() <= MATCH_DISTANCE_METERS)
                    .min(Comparator.comparingDouble(MatchedCrosswalk::distanceMeters));

            if (matched.isEmpty()) {
                noDistanceMatchCount++;
                continue;
            }

            Crosswalk national = matched.get().crosswalk();

            seoul.mergeWithNationalAttributes(national, LocalDateTime.now());
            crosswalkRepository.delete(national);

            mergedCount++;
            deletedNationalCount++;

            log.debug(
                    "[CROSSWALK MERGE] seoulCode={}, nationalCode={}, distanceMeters={}",
                    seoul.getCrosswalkCode(),
                    national.getCrosswalkCode(),
                    matched.get().distanceMeters()
            );
        }

        log.info(
                "[CROSSWALK MERGE SUMMARY] seoulCount={}, mergedCount={}, deletedNationalCount={}, missingSigungu={}, noNationalCandidate={}, noDistanceMatch={}, maxDistanceMeters={}",
                seoulCrosswalks.size(),
                mergedCount,
                deletedNationalCount,
                missingSigunguCount,
                noNationalCandidateCount,
                noDistanceMatchCount,
                MATCH_DISTANCE_METERS
        );
    }

    private Optional<MatchedCrosswalk> findNearestNationalCrosswalk(Crosswalk seoul) {
        if (!hasText(seoul.getSigungu())) {
            return Optional.empty();
        }

        List<Crosswalk> nationalCandidates = crosswalkRepository.findByBaseSourceAndSigungu(
                DataSourceType.NATIONAL_STANDARD_CROSSWALK,
                seoul.getSigungu()
        );

        return nationalCandidates.stream()
                .filter(national -> isSameRegion(seoul, national))
                .map(national -> new MatchedCrosswalk(
                        national,
                        calculateDistanceMeters(
                                seoul.getLatitude(),
                                seoul.getLongitude(),
                                national.getLatitude(),
                                national.getLongitude()
                        )
                ))
                .filter(match -> match.distanceMeters() <= MATCH_DISTANCE_METERS)
                .min(Comparator.comparingDouble(MatchedCrosswalk::distanceMeters));
    }

    private boolean isSameRegion(Crosswalk seoul, Crosswalk national) {
        return Objects.equals(normalizeRegion(seoul.getSido()), normalizeRegion(national.getSido()))
                && Objects.equals(normalizeRegion(seoul.getSigungu()), normalizeRegion(national.getSigungu()));
    }

    private String normalizeRegion(String value) {
        if (!hasText(value)) {
            return null;
        }

        return value.replace(" ", "").trim();
    }

    private double calculateDistanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadius * c;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record MatchedCrosswalk(Crosswalk crosswalk, double distanceMeters) {
    }
}