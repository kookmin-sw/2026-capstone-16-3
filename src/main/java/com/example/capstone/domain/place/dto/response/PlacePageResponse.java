package com.example.capstone.domain.place.dto.response;

import java.util.List;

public record PlacePageResponse(
        List<PlaceResponse> items,
        int page,
        int size,
        int total,
        boolean hasNext
) {}