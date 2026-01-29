package com.example.capstone.domain.place.entity;

import com.example.capstone.domain.user.entity.User;
import com.example.capstone.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "recent_search_places",
        indexes = {
                @Index(name = "idx_recent_search_user_time", columnList = "user_id, searched_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RecentSearchPlace extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "place_name", length = 255)
    private String placeName;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "searched_at", nullable = false)
    private Instant searchedAt;
}
