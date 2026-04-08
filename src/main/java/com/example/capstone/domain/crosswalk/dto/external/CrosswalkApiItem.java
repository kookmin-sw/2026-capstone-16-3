package com.example.capstone.domain.crosswalk.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrosswalkApiItem {
    private String ctprvnNm; // 시도명
    private String signguNm; // 시군구명
    private String roadNm; // 도로명
    private String rdnmadr; // 도로명주소
    private String lnmadr; // 지번주소
    private String crslkManageNo; // 횡단보도관리번호
    private String crslkKnd; // 횡단보도종류
    private String bcyclCrslkCmbnatYn; // 자전거횡단도겸용여부(Y, N)
    private String highlandYn; // 고원식적용여부(Y, N)
    private String latitude; // 위도
    private String longitude; // 경도
    private String cartrkCo; // 차로수
    private String bt; // 횡단보도폭
    private String et; // 횡단보도연장
    private String tfclghtYn; // 보행자신호등유무(Y, N)
    private String fnctngSgngnrYn; // 보행자작동신호기유무(Y, N)
    private String soundSgngnrYn; // 음향신호기설치여부(Y, N)
    private String greenSgngnrTime; // 녹색신호시간
    private String redSgngnrTime; // 적색신호시간
    private String tfcilndYn; // 교통섬유무(Y, N)
    private String ftpthLowerYn; // 보도턱낮춤여부(Y, N)
    private String brllBlckYn; // 점자블록유무(Y, N)
    private String cnctrLghtFcltyYn; // 집중조명시설유무(Y, N)
    private String institutionNm; // 관리기관명
    private String phoneNumber; // 관리기관전화번호
    private String referenceDate; // 데이터기준일자(mm/dd/yyyy)
    private String instt_code; // 제공기관코드
}