package com.example.capstone.domain.crosswalk.entity;

import com.example.capstone.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "crosswalks",
        indexes = {
                @Index(name = "idx_crosswalk_manage_no", columnList = "crslk_manage_no", unique = true),
                @Index(name = "idx_crosswalk_region", columnList = "ctprvn_nm, signgu_nm"),
                @Index(name = "idx_crosswalk_lat_lon", columnList = "latitude, longitude")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Crosswalk extends BaseEntity {

    @Column(name = "ctprvn_nm", length = 100)
    private String ctprvnNm;

    @Column(name = "signgu_nm", length = 100)
    private String signguNm;

    @Column(name = "road_nm", length = 255)
    private String roadNm;

    @Column(name = "rdnmadr", length = 500)
    private String rdnmadr;

    @Column(name = "lnmadr", length = 500)
    private String lnmadr;

    @Column(name = "crslk_manage_no", nullable = false, unique = true, length = 100)
    private String crslkManageNo;

    @Column(name = "crslk_knd", length = 50)
    private String crslkKnd;

    @Column(name = "bcycl_crslk_cmbnat_yn", length = 1)
    private String bcyclCrslkCmbnatYn;

    @Column(name = "highland_yn", length = 1)
    private String highlandYn;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "cartrk_co")
    private Integer cartrkCo;

    @Column(name = "bt")
    private Double bt;

    @Column(name = "et")
    private Double et;

    @Column(name = "tfclght_yn", length = 1)
    private String tfclghtYn;

    @Column(name = "fnctng_sgngnr_yn", length = 1)
    private String fnctngSgngnrYn;

    @Column(name = "sond_sgngnr_yn", length = 1)
    private String sondSgngnrYn;

    @Column(name = "green_sgngnr_time", length = 50)
    private String greenSgngnrTime;

    @Column(name = "red_sgngnr_time", length = 50)
    private String redSgngnrTime;

    @Column(name = "tfcilnd_yn", length = 1)
    private String tfcilndYn;

    @Column(name = "ftpth_lower_yn", length = 1)
    private String ftpthLowerYn;

    @Column(name = "brll_blck_yn", length = 1)
    private String brllBlckYn;

    @Column(name = "cnctr_lght_fclty_yn", length = 1)
    private String cnctrLghtFcltyYn;

    @Column(name = "institution_nm", length = 255)
    private String institutionNm;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @Column(name = "reference_date")
    private LocalDate referenceDate;

    public void updateFromApi(
            String ctprvnNm,
            String signguNm,
            String roadNm,
            String rdnmadr,
            String lnmadr,
            String crslkKnd,
            String bcyclCrslkCmbnatYn,
            String highlandYn,
            Double latitude,
            Double longitude,
            Integer cartrkCo,
            Double bt,
            Double et,
            String tfclghtYn,
            String fnctngSgngnrYn,
            String sondSgngnrYn,
            String greenSgngnrTime,
            String redSgngnrTime,
            String tfcilndYn,
            String ftpthLowerYn,
            String brllBlckYn,
            String cnctrLghtFcltyYn,
            String institutionNm,
            String phoneNumber,
            LocalDate referenceDate
    ) {
        this.ctprvnNm = ctprvnNm;
        this.signguNm = signguNm;
        this.roadNm = roadNm;
        this.rdnmadr = rdnmadr;
        this.lnmadr = lnmadr;
        this.crslkKnd = crslkKnd;
        this.bcyclCrslkCmbnatYn = bcyclCrslkCmbnatYn;
        this.highlandYn = highlandYn;
        this.latitude = latitude;
        this.longitude = longitude;
        this.cartrkCo = cartrkCo;
        this.bt = bt;
        this.et = et;
        this.tfclghtYn = tfclghtYn;
        this.fnctngSgngnrYn = fnctngSgngnrYn;
        this.sondSgngnrYn = sondSgngnrYn;
        this.greenSgngnrTime = greenSgngnrTime;
        this.redSgngnrTime = redSgngnrTime;
        this.tfcilndYn = tfcilndYn;
        this.ftpthLowerYn = ftpthLowerYn;
        this.brllBlckYn = brllBlckYn;
        this.cnctrLghtFcltyYn = cnctrLghtFcltyYn;
        this.institutionNm = institutionNm;
        this.phoneNumber = phoneNumber;
        this.referenceDate = referenceDate;
    }

    public static Crosswalk create(String crslkManageNo) {
        return Crosswalk.builder()
                .crslkManageNo(crslkManageNo)
                .build();
    }
}