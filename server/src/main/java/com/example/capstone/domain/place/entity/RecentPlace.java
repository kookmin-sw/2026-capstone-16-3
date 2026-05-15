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
public class RecentPlace extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "place_id", nullable = false, length = 128)
    private String placeId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * 최근 검색 목록에 표시할 주소.
     * 도로명 주소를 우선 저장하고, 도로명 변환이 불가능할 때만 지번 주소를 저장한다.
     */
    @Column(name = "road_address", length = 500)
    private String roadAddress;

    /**
     * 카카오 원본 지번 주소.
     * 기존 address 값이 지번으로 저장된 데이터도 조회 시 도로명 주소로 보정하기 위해 보관한다.
     */
    @Column(name = "jibun_address", length = 500)
    private String jibunAddress;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "lng", nullable = false)
    private Double longitude;

    @Column(name = "searched_at", nullable = false)
    private Instant searchedAt;

    public void update(
            String name,
            String roadAddress,
            String jibunAddress,
            Double latitude,
            Double longitude,
            Instant searchedAt
    ) {
        this.name = name;
        this.roadAddress = roadAddress;
        this.jibunAddress = jibunAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.searchedAt = searchedAt;
    }
}