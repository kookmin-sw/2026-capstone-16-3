package com.example.capstone.domain.place.dto.response.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record KakaoAddressSearchResponse(
        Meta meta,
        List<Document> documents
) {
    public record Meta(
            @JsonProperty("total_count") int totalCount,
            @JsonProperty("pageable_count") int pageableCount,
            @JsonProperty("is_end") boolean isEnd
    ) {}

    public record Document(
            @JsonProperty("address_name") String addressName,
            @JsonProperty("address_type") String addressType,
            String x, // 경도
            String y, // 위도
            Address address,
            @JsonProperty("road_address") RoadAddress roadAddress
    ) {}

    public record Address(
            @JsonProperty("address_name") String addressName,
            @JsonProperty("region_1depth_name") String region1DepthName,
            @JsonProperty("region_2depth_name") String region2DepthName,
            @JsonProperty("region_3depth_name") String region3DepthName,
            @JsonProperty("region_3depth_h_name") String region3DepthHName,
            @JsonProperty("h_code") String hCode,
            @JsonProperty("b_code") String bCode,
            @JsonProperty("mountain_yn") String mountainYn,
            @JsonProperty("main_address_no") String mainAddressNo,
            @JsonProperty("sub_address_no") String subAddressNo,
            String x,
            String y
    ) {}

    public record RoadAddress(
            @JsonProperty("address_name") String addressName,
            @JsonProperty("region_1depth_name") String region1DepthName,
            @JsonProperty("region_2depth_name") String region2DepthName,
            @JsonProperty("region_3depth_name") String region3DepthName,
            @JsonProperty("road_name") String roadName,
            @JsonProperty("underground_yn") String undergroundYn,
            @JsonProperty("main_building_no") String mainBuildingNo,
            @JsonProperty("sub_building_no") String subBuildingNo,
            @JsonProperty("building_name") String buildingName,
            @JsonProperty("zone_no") String zoneNo,
            String x,
            String y
    ) {}
}