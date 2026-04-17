package com.example.capstone.domain.place.dto.response.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record KakaoCoordToAddressResponse(
        Meta meta,
        List<Document> documents
) {
    public record Meta(
            @JsonProperty("total_count") int totalCount
    ) {}

    public record Document(
            @Schema(description = "지번 주소 상세 정보")
            Address address,
            @Schema(description = "도로명 주소 상세 정보")
            @JsonProperty("road_address") RoadAddress roadAddress
    ) {}

    public record Address(
            @Schema(description = "전체 지번 주소")
            @JsonProperty("address_name") String addressName,
            @Schema(description = "시도 단위")
            @JsonProperty("region_1depth_name") String region1DepthName,
            @Schema(description = "구 단위")
            @JsonProperty("region_2depth_name") String region2DepthName,
            @Schema(description = "동 단위")
            @JsonProperty("region_3depth_name") String region3DepthName,
            @Schema(description = "산 여부, Y/N")
            @JsonProperty("mountain_yn") String mountainYn,
            @Schema(description = "지번 주번지")
            @JsonProperty("main_address_no") String mainAddressNo,
            @Schema(description = "지번 부번지, 없을 경우 빈 문자열(\"\") 반환")
            @JsonProperty("sub_address_no") String subAddressNo,
            @Schema(description = "X 좌표값, 경위도인 경우 경도(longitude)")
            String x,
            @Schema(description = "Y 좌표값, 경위도인 경우 위도(latitude)")
            String y
    ) {}

    public record RoadAddress(
            @Schema(description = "전체 도로명 주소")
            @JsonProperty("address_name") String addressName,
            @Schema(description = "지역명1")
            @JsonProperty("region_1depth_name") String region1DepthName,
            @Schema(description = "지역명2")
            @JsonProperty("region_2depth_name") String region2DepthName,
            @Schema(description = "지역명3")
            @JsonProperty("region_3depth_name") String region3DepthName,
            @Schema(description = "도로명")
            @JsonProperty("road_name") String roadName,
            @Schema(description = "지하 여부, Y/N")
            @JsonProperty("underground_yn") String undergroundYn,
            @Schema(description = "건물 본번")
            @JsonProperty("main_building_no") String mainBuildingNo,
            @Schema(description = "건물 부번, 없을 경우 빈 문자열(\"\") 반환")
            @JsonProperty("sub_building_no") String subBuildingNo,
            @Schema(description = "건물 이름")
            @JsonProperty("building_name") String buildingName,
            @Schema(description = "우편번호(5자리)")
            @JsonProperty("zone_no") String zoneNo,
            @Schema(description = "X 좌표값, 경위도인 경우 경도(longitude)")
            String x,
            @Schema(description = "Y 좌표값, 경위도인 경우 위도(latitude)")
            String y
    ) {}
}