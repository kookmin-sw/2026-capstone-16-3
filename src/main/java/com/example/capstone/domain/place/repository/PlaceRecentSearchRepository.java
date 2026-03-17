package com.example.capstone.domain.place.repository;

import com.example.capstone.domain.place.entity.PlaceRecentSearch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlaceRecentSearchRepository extends JpaRepository<PlaceRecentSearch, Long> {

    Page<PlaceRecentSearch> findByUserIdOrderBySearchedAtDesc(Long userId, Pageable pageable);

    Optional<PlaceRecentSearch> findByUserIdAndPlaceId(Long userId, String placeId);

    long countByUserId(Long userId);

    int deleteByIdAndUserId(Long id, Long userId);

    int deleteByUserId(Long userId);
}