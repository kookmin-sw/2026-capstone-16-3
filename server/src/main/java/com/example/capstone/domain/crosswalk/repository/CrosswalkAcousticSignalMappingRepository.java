package com.example.capstone.domain.crosswalk.repository;

import com.example.capstone.domain.crosswalk.entity.Crosswalk;
import com.example.capstone.domain.crosswalk.entity.CrosswalkAcousticSignalMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrosswalkAcousticSignalMappingRepository extends JpaRepository<CrosswalkAcousticSignalMapping, Long> {

    List<CrosswalkAcousticSignalMapping> findByCrosswalk(Crosswalk crosswalk);
}