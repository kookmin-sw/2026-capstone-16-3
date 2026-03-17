package com.example.capstone.domain.place.entity;

import com.example.capstone.domain.user.entity.User;
import com.example.capstone.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "place_recent_search",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_place_recent_user_place",
                        columnNames = {"user_id", "place_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_place_recent_user_searched",
                        columnList = "user_id,searched_at"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PlaceRecentSearch extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "place_id", nullable = false, length = 128)
    private String placeId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "searched_at", nullable = false)
    private Instant searchedAt;

    public void update(
            String name,
            String address,
            Double latitude,
            Double longitude,
            Instant searchedAt
    ) {
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.searchedAt = searchedAt;
    }
}