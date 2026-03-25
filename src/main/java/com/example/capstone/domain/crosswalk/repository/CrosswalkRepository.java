package com.example.capstone.domain.crosswalk.repository;

import com.example.capstone.domain.crosswalk.entity.Crosswalk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CrosswalkRepository extends JpaRepository<Crosswalk, Long> {
    Optional<Crosswalk> findByCrslkManageNo(String crslkManageNo);
}