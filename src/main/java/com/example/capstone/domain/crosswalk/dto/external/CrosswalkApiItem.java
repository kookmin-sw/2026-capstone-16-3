package com.example.capstone.domain.crosswalk.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrosswalkApiItem {
    private String ctprvnNm;
    private String signguNm;
    private String roadNm;
    private String rdnmadr;
    private String lnmadr;
    private String crslkManageNo;
    private String crslkKnd;
    private String bcyclCrslkCmbnatYn;
    private String highlandYn;
    private String latitude;
    private String longitude;
    private String cartrkCo;
    private String bt;
    private String et;
    private String tfclghtYn;
    private String fnctngSgngnrYn;
    private String sondSgngnrYn;
    private String greenSgngnrTime;
    private String redSgngnrTime;
    private String tfcilndYn;
    private String ftpthLowerYn;
    private String brllBlckYn;
    private String cnctrLghtFcltyYn;
    private String institutionNm;
    private String phoneNumber;
    private String referenceDate;
}