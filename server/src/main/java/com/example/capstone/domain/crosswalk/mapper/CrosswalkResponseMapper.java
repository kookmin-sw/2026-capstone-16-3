package com.example.capstone.domain.crosswalk.mapper;

import com.example.capstone.domain.crosswalk.dto.response.*;
import com.example.capstone.domain.crosswalk.entity.AcousticSignal;
import com.example.capstone.domain.crosswalk.entity.Crosswalk;
import com.example.capstone.domain.crosswalk.entity.CrosswalkAcousticSignalMapping;
import com.example.capstone.domain.crosswalk.enums.AcousticSignalAggregateStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CrosswalkResponseMapper {

    public CrosswalkNearbyResponse toNearbyResponse(Crosswalk crosswalk, double distanceMeters, List<CrosswalkAcousticSignalMapping> mappings) {
        boolean installed = !mappings.isEmpty();

        return new CrosswalkNearbyResponse(
                crosswalk.getCrosswalkCode(),
                distanceMeters,
                toLocationDto(crosswalk),
                new CrosswalkInfoSummaryDto(
                        crosswalk.getPedestrianSignal(),
                        crosswalk.getBrailleBlock(),
                        crosswalk.getCurbLowered()
                ),
                new AcousticSignalSummaryDto(installed),
                new CrosswalkGuidanceSummaryDto(buildGuidanceSummary(crosswalk, installed))
        );
    }

    public CrosswalkDetailResponse toDetailResponse(Crosswalk crosswalk, List<CrosswalkAcousticSignalMapping> mappings) {
        List<AcousticSignalDeviceDto> devices = mappings.stream()
                .map(CrosswalkAcousticSignalMapping::getAcousticSignal)
                .map(this::toAcousticSignalDeviceDto)
                .toList();

        boolean installed = !devices.isEmpty();

        return new CrosswalkDetailResponse(
                crosswalk.getCrosswalkCode(),
                toLocationDto(crosswalk),
                new CrosswalkInfoDto(
                        crosswalk.getKind(),
                        crosswalk.getWidth(),
                        crosswalk.getLength(),
                        crosswalk.getPedestrianSignal(),
                        crosswalk.getActuatedSignal(),
                        crosswalk.getGreenTime(),
                        crosswalk.getRedTime(),
                        crosswalk.getBrailleBlock(),
                        crosswalk.getCurbLowered(),
                        crosswalk.getTrafficIsland(),
                        crosswalk.getSafetyLighting()
                ),
                new AcousticSignalDto(
                        installed,
                        devices.size(),
                        calculateAggregateStatus(installed, devices.size()).name(),
                        devices
                ),
                new CrosswalkGuidanceSummaryDto(buildGuidanceSummary(crosswalk, installed)),
                new CrosswalkSourceDto(
                        crosswalk.getBaseSource().name(),
                        "NATIONAL_STANDARD_CROSSWALK",
                        installed ? "SEOUL_ACOUSTIC_SIGNAL" : null
                )
        );
    }

    private CrosswalkLocationDto toLocationDto(Crosswalk crosswalk) {
        return new CrosswalkLocationDto(
                crosswalk.getLatitude(),
                crosswalk.getLongitude(),
                crosswalk.getRoadAddress(),
                crosswalk.getSido(),
                crosswalk.getSigungu(),
                crosswalk.getEmd()
        );
    }

    private AcousticSignalDeviceDto toAcousticSignalDeviceDto(AcousticSignal signal) {
        return new AcousticSignalDeviceDto(
                signal.getAcousticSignalCode(),
                signal.getDirection(),
                signal.getStatus()
        );
    }

    private AcousticSignalAggregateStatus calculateAggregateStatus(boolean installed, int count) {
        if (!installed) {
            return AcousticSignalAggregateStatus.NONE;
        }
        if (count >= 2) {
            return AcousticSignalAggregateStatus.NORMAL;
        }
        if (count == 1) {
            return AcousticSignalAggregateStatus.PARTIAL;
        }
        return AcousticSignalAggregateStatus.UNKNOWN;
    }

    private String buildGuidanceSummary(Crosswalk crosswalk, boolean acousticInstalled) {
        if (Boolean.TRUE.equals(acousticInstalled) && Boolean.TRUE.equals(crosswalk.getBrailleBlock())) {
            return "음향신호기와 점자블록이 있는 횡단보도";
        }
        if (Boolean.TRUE.equals(acousticInstalled)) {
            return "음향신호기가 있는 횡단보도";
        }
        if (Boolean.TRUE.equals(crosswalk.getPedestrianSignal())) {
            return "보행자 신호는 있으나 음향신호기 정보는 확인되지 않음";
        }
        return "횡단보도 정보가 확인됨";
    }
}