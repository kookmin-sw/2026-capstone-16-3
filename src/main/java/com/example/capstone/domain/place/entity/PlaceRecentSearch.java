package com.example.capstone.domain.place.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "place_recent_search",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_place_recent_user_place",
                        columnNames = {"user_key", "place_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_place_recent_user_searched",
                        columnList = "user_key,searched_at"
                )
        }
)
public class PlaceRecentSearch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_key", nullable = false, length = 128)
    private String userKey;

    @Column(name = "place_id", nullable = false, length = 128)
    private String placeId; // ext:KAKAO:...

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "category", length = 255)
    private String category;

    @Column(name = "distance_m")
    private Long distanceM;

    @Column(name = "direction_clock")
    private Integer directionClock;

    @Column(name = "road_address", length = 255)
    private String roadAddress;

    @Column(name = "jibun_address", length = 255)
    private String jibunAddress; // ✅ 추가

    @Column(name = "searched_at", nullable = false)
    private Instant searchedAt;

    protected PlaceRecentSearch() {}

    public PlaceRecentSearch(
            String userKey,
            String placeId,
            String name,
            String category,
            Long distanceM,
            Integer directionClock,
            String roadAddress,
            String jibunAddress,
            Instant searchedAt
    ) {
        this.userKey = userKey;
        this.placeId = placeId;
        this.name = name;
        this.category = category;
        this.distanceM = distanceM;
        this.directionClock = directionClock;
        this.roadAddress = roadAddress;
        this.jibunAddress = jibunAddress;
        this.searchedAt = searchedAt;
    }

    public void touch(Long distanceM, Integer directionClock, String roadAddress, String jibunAddress, Instant searchedAt) {
        this.distanceM = distanceM;
        this.directionClock = directionClock;
        this.roadAddress = roadAddress;
        this.jibunAddress = jibunAddress;
        this.searchedAt = searchedAt;
    }

    public Long getId() { return id; }
    public String getUserKey() { return userKey; }
    public String getPlaceId() { return placeId; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public Long getDistanceM() { return distanceM; }
    public Integer getDirectionClock() { return directionClock; }
    public String getRoadAddress() { return roadAddress; }
    public String getJibunAddress() { return jibunAddress; }
    public Instant getSearchedAt() { return searchedAt; }
}