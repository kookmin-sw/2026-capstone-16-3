package com.example.capstone.domain.place.dto.response.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record KakaoCoordToAddressResponse(
        Meta meta,
        List<Document> documents
) {
    public record Meta(
            @JsonProperty("total_count") int totalCount
    ) {}

    public record Document(
            Address address,
            @JsonProperty("road_address") RoadAddress roadAddress
    ) {}

    public record Address(
            @JsonProperty("address_name") String addressName,
            @JsonProperty("region_1depth_name") String region1DepthName,
            @JsonProperty("region_2depth_name") String region2DepthName,
            @JsonProperty("region_3depth_name") String region3DepthName,
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