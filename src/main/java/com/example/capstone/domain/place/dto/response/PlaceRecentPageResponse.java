package com.example.capstone.domain.place.dto.response;

import java.time.Instant;
import java.util.List;

public record PlaceRecentPageResponse(
        List<Item> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public record Item(
            Long id,                 // DB PK (단건 삭제용)
            String placeId,           // ext:KAKAO:26338954
            String name,
            String category,
            Long distanceM,
            Integer directionClock,   // 0~11 (12시=0, 3시=3 ...)
            String roadAddress,
            Instant searchedAt
    ) {}
}