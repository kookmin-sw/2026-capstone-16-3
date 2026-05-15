package com.example.capstone.domain.crosswalk.entity;

import com.example.capstone.domain.crosswalk.enums.MatchMethod;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "crosswalk_acoustic_signal_mapping",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_crosswalk_signal_mapping",
                        columnNames = {"crosswalk_id", "acoustic_signal_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrosswalkAcousticSignalMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crosswalk_id")
    private Crosswalk crosswalk;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "acoustic_signal_id")
    private AcousticSignal acousticSignal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchMethod matchMethod;

    @Column(nullable = false)
    private Double distanceMeters;

    @Column(nullable = false)
    private Double confidence;

    @Column(nullable = false)
    private LocalDateTime matchedAt;

    @Builder
    public CrosswalkAcousticSignalMapping(
            Crosswalk crosswalk,
            AcousticSignal acousticSignal,
            MatchMethod matchMethod,
            Double distanceMeters,
            Double confidence,
            LocalDateTime matchedAt
    ) {
        this.crosswalk = crosswalk;
        this.acousticSignal = acousticSignal;
        this.matchMethod = matchMethod;
        this.distanceMeters = distanceMeters;
        this.confidence = confidence;
        this.matchedAt = matchedAt;
    }

    public void updateMatch(MatchMethod matchMethod, Double distanceMeters, Double confidence, LocalDateTime matchedAt) {
        this.matchMethod = matchMethod;
        this.distanceMeters = distanceMeters;
        this.confidence = confidence;
        this.matchedAt = matchedAt;
    }
}