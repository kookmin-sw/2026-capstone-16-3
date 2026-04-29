package com.example.capstone.domain.place.repository;

import com.example.capstone.domain.place.entity.RecentPlace;
import com.example.capstone.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecentPlaceRepository extends JpaRepository<RecentPlace, Long> {

    Slice<RecentPlace> findByUserIdOrderBySearchedAtDesc(Long userId, Pageable pageable);

    List<RecentPlace> findAllByUserIdOrderBySearchedAtDesc(Long userId);

    Optional<RecentPlace> findByUserIdAndPlaceId(Long userId, String placeId);

    long countByUserId(Long userId);

    int deleteByIdAndUserId(Long id, Long userId);

    int deleteByUserId(Long userId);

    void deleteByUser(User user);
}