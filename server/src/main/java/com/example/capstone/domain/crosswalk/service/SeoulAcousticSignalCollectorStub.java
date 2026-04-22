package com.example.capstone.domain.crosswalk.service;

import com.example.capstone.domain.crosswalk.service.sync.SeoulAcousticSignalCollector;
import com.example.capstone.domain.crosswalk.service.sync.SeoulCrosswalkCollector;
import org.springframework.stereotype.Service;

@Service
public class SeoulAcousticSignalCollectorStub implements SeoulAcousticSignalCollector {
    @Override
    public void collectAndSave() {
        // TODO: 구현 전 임시
    }
}