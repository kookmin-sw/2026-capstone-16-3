package com.example.capstone.domain.crosswalk.entity;

import com.example.capstone.domain.crosswalk.enums.DataSourceType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "crosswalk")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Crosswalk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String crosswalkCode;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    private String roadAddress;
    private String sido;
    private String sigungu;
    private String emd;

    private String kind;

    private Double width;
    private Double length;

    private Boolean pedestrianSignal;
    private Boolean actuatedSignal;

    private Integer greenTime;
    private Integer redTime;

    private Boolean brailleBlock;
    private Boolean curbLowered;
    private Boolean trafficIsland;
    private Boolean safetyLighting;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DataSourceType baseSource;

    @Column(nullable = false)
    private LocalDate referenceDate;

    @Column(nullable = false)
    private LocalDateTime lastSyncedAt;

    @Builder
    public Crosswalk(
            String crosswalkCode,
            Double latitude,
            Double longitude,
            String roadAddress,
            String sido,
            String sigungu,
            String emd,
            String kind,
            Double width,
            Double length,
            Boolean pedestrianSignal,
            Boolean actuatedSignal,
            Integer greenTime,
            Integer redTime,
            Boolean brailleBlock,
            Boolean curbLowered,
            Boolean trafficIsland,
            Boolean safetyLighting,
            DataSourceType baseSource,
            LocalDate referenceDate,
            LocalDateTime lastSyncedAt
    ) {
        this.crosswalkCode = crosswalkCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.roadAddress = roadAddress;
        this.sido = sido;
        this.sigungu = sigungu;
        this.emd = emd;
        this.kind = kind;
        this.width = width;
        this.length = length;
        this.pedestrianSignal = pedestrianSignal;
        this.actuatedSignal = actuatedSignal;
        this.greenTime = greenTime;
        this.redTime = redTime;
        this.brailleBlock = brailleBlock;
        this.curbLowered = curbLowered;
        this.trafficIsland = trafficIsland;
        this.safetyLighting = safetyLighting;
        this.baseSource = baseSource;
        this.referenceDate = referenceDate;
        this.lastSyncedAt = lastSyncedAt;
    }

    public void updateFrom(
            Double latitude,
            Double longitude,
            String roadAddress,
            String sido,
            String sigungu,
            String emd,
            String kind,
            Double width,
            Double length,
            Boolean pedestrianSignal,
            Boolean actuatedSignal,
            Integer greenTime,
            Integer redTime,
            Boolean brailleBlock,
            Boolean curbLowered,
            Boolean trafficIsland,
            Boolean safetyLighting,
            DataSourceType baseSource,
            LocalDate referenceDate,
            LocalDateTime lastSyncedAt
    ) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.roadAddress = roadAddress;
        this.sido = sido;
        this.sigungu = sigungu;
        this.emd = emd;
        this.kind = kind;
        this.width = width;
        this.length = length;
        this.pedestrianSignal = pedestrianSignal;
        this.actuatedSignal = actuatedSignal;
        this.greenTime = greenTime;
        this.redTime = redTime;
        this.brailleBlock = brailleBlock;
        this.curbLowered = curbLowered;
        this.trafficIsland = trafficIsland;
        this.safetyLighting = safetyLighting;
        this.baseSource = baseSource;
        this.referenceDate = referenceDate;
        this.lastSyncedAt = lastSyncedAt;
    }

    public void mergeWithNationalAttributes(Crosswalk national, LocalDateTime mergedAt) {
        this.roadAddress = firstNonBlank(this.roadAddress, national.getRoadAddress());
        this.sido = firstNonBlank(this.sido, national.getSido());
        this.sigungu = firstNonBlank(this.sigungu, national.getSigungu());
        this.emd = firstNonBlank(this.emd, national.getEmd());
        this.kind = firstNonBlank(this.kind, national.getKind());

        this.width = firstNonNull(this.width, national.getWidth());
        this.length = firstNonNull(this.length, national.getLength());

        this.pedestrianSignal = firstNonNull(this.pedestrianSignal, national.getPedestrianSignal());
        this.actuatedSignal = firstNonNull(this.actuatedSignal, national.getActuatedSignal());

        this.greenTime = firstNonNull(this.greenTime, national.getGreenTime());
        this.redTime = firstNonNull(this.redTime, national.getRedTime());

        this.brailleBlock = firstNonNull(this.brailleBlock, national.getBrailleBlock());
        this.curbLowered = firstNonNull(this.curbLowered, national.getCurbLowered());
        this.trafficIsland = firstNonNull(this.trafficIsland, national.getTrafficIsland());
        this.safetyLighting = firstNonNull(this.safetyLighting, national.getSafetyLighting());

        this.referenceDate = firstNonNull(this.referenceDate, national.getReferenceDate());
        this.lastSyncedAt = mergedAt;
    }

    private String firstNonBlank(String current, String incoming) {
        return current != null && !current.isBlank() ? current : incoming;
    }

    private <T> T firstNonNull(T current, T incoming) {
        return current != null ? current : incoming;
    }
}