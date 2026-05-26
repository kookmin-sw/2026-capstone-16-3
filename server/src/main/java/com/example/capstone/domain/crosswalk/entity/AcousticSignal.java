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
@Table(name = "acoustic_signal")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AcousticSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String acousticSignalCode;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    private String direction;
    private String status;
    private String positionInfo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DataSourceType source;

    @Column(nullable = false)
    private LocalDate referenceDate;

    @Column(nullable = false)
    private LocalDateTime lastSyncedAt;

    @Builder
    public AcousticSignal(
            String acousticSignalCode,
            Double latitude,
            Double longitude,
            String direction,
            String status,
            String positionInfo,
            DataSourceType source,
            LocalDate referenceDate,
            LocalDateTime lastSyncedAt
    ) {
        this.acousticSignalCode = acousticSignalCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.direction = direction;
        this.status = status;
        this.positionInfo = positionInfo;
        this.source = source;
        this.referenceDate = referenceDate;
        this.lastSyncedAt = lastSyncedAt;
    }

    public void updateFrom(
            Double latitude,
            Double longitude,
            String direction,
            String status,
            String positionInfo,
            DataSourceType source,
            LocalDate referenceDate,
            LocalDateTime lastSyncedAt
    ) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.direction = direction;
        this.status = status;
        this.positionInfo = positionInfo;
        this.source = source;
        this.referenceDate = referenceDate;
        this.lastSyncedAt = lastSyncedAt;
    }
}