package com.example.capstone.domain.place.repository;

import com.example.capstone.domain.place.entity.RecentPlace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecentPlaceRepository extends JpaRepository<RecentPlace, Long> {

    Page<RecentPlace> findByUserIdOrderBySearchedAtDesc(Long userId, Pageable pageable);

    Optional<RecentPlace> findByUserIdAndPlaceId(Long userId, String placeId);

    long countByUserId(Long userId);

    int deleteByIdAndUserId(Long id, Long userId);

    int deleteByUserId(Long userId);
}