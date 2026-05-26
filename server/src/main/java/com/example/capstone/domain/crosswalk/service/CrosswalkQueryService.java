package com.example.capstone.domain.crosswalk.service;

import com.example.capstone.domain.crosswalk.dto.response.CrosswalkDetailResponse;
import com.example.capstone.domain.crosswalk.dto.response.CrosswalkNearbyResponse;
import com.example.capstone.domain.crosswalk.entity.Crosswalk;
import com.example.capstone.domain.crosswalk.entity.CrosswalkAcousticSignalMapping;
import com.example.capstone.domain.crosswalk.mapper.CrosswalkResponseMapper;
import com.example.capstone.domain.crosswalk.repository.CrosswalkAcousticSignalMappingRepository;
import com.example.capstone.domain.crosswalk.repository.CrosswalkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrosswalkQueryService {

    private final CrosswalkRepository crosswalkRepository;
    private final CrosswalkAcousticSignalMappingRepository mappingRepository;
    private final CrosswalkResponseMapper mapper;

    public List<CrosswalkNearbyResponse> getNearbyCrosswalks(double latitude, double longitude, double radiusMeters) {
        return crosswalkRepository.findAll().stream()
                .map(crosswalk -> {
                    double distance = calculateDistanceMeters(latitude, longitude, crosswalk.getLatitude(), crosswalk.getLongitude());
                    return new CrosswalkDistance(crosswalk, distance);
                })
                .filter(it -> it.distanceMeters() <= radiusMeters)
                .sorted(Comparator.comparingDouble(CrosswalkDistance::distanceMeters))
                .map(it -> {
                    List<CrosswalkAcousticSignalMapping> mappings = mappingRepository.findByCrosswalk(it.crosswalk());
                    return mapper.toNearbyResponse(it.crosswalk(), it.distanceMeters(), mappings);
                })
                .toList();
    }

    public CrosswalkDetailResponse getCrosswalkDetail(String crosswalkCode) {
        Crosswalk crosswalk = crosswalkRepository.findByCrosswalkCode(crosswalkCode)
                .orElseThrow(() -> new IllegalArgumentException("횡단보도를 찾을 수 없습니다. code=" + crosswalkCode));

        List<CrosswalkAcousticSignalMapping> mappings = mappingRepository.findByCrosswalk(crosswalk);

        return mapper.toDetailResponse(crosswalk, mappings);
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

    private record CrosswalkDistance(Crosswalk crosswalk, double distanceMeters) {
    }
}