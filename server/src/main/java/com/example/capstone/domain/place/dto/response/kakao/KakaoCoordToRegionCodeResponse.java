package com.example.capstone.domain.place.dto.response.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record KakaoCoordToRegionCodeResponse(
        Meta meta,
        List<Document> documents
) {
    public record Meta(
            @JsonProperty("total_count") int totalCount
    ) {}

    public record Document(
            @Schema(description = "지역 구분. B=법정동, H=행정동")
            @JsonProperty("region_type") String regionType,

            @Schema(description = "행정구역 코드")
            String code,

            @Schema(description = "전체 지역명")
            @JsonProperty("address_name") String addressName,

            @Schema(description = "시도 단위")
            @JsonProperty("region_1depth_name") String region1DepthName,

            @Schema(description = "시군구 단위")
            @JsonProperty("region_2depth_name") String region2DepthName,

            @Schema(description = "읍면동 단위")
            @JsonProperty("region_3depth_name") String region3DepthName,

            @Schema(description = "리 단위")
            @JsonProperty("region_4depth_name") String region4DepthName,

            @Schema(description = "X 좌표값, 경위도인 경우 경도(longitude)")
            String x,

            @Schema(description = "Y 좌표값, 경위도인 경우 위도(latitude)")
            String y
    ) {}
}