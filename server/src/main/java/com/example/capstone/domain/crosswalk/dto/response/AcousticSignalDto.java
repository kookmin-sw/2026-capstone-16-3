package com.example.capstone.domain.crosswalk.dto.response;

import java.util.List;

public record AcousticSignalDto(
        Boolean installed,
        Integer count,
        String status,
        List<AcousticSignalDeviceDto> devices
) {
}