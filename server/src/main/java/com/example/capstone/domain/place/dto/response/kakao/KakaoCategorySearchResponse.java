package com.example.capstone.domain.place.dto.response.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record KakaoCategorySearchResponse(
        KakaoMeta meta,
        List<KakaoPlaceDocument> documents
) {
    public record KakaoMeta(
            @JsonProperty("total_count") int totalCount,
            @JsonProperty("pageable_count") int pageableCount,
            @JsonProperty("is_end") boolean isEnd
    ) {}

    public record KakaoPlaceDocument(
            String id,
            @JsonProperty("place_name") String placeName,
            @JsonProperty("category_name") String categoryName,
            @JsonProperty("category_group_code") String categoryGroupCode,
            @JsonProperty("category_group_name") String categoryGroupName,
            String phone,
            @JsonProperty("address_name") String addressName,
            @JsonProperty("road_address_name") String roadAddressName,
            String x, // 경도(lng)
            String y, // 위도(lat)
            @JsonProperty("place_url") String placeUrl,
            String distance // sort=distance일 때 미터 문자열
    ) {}
}