package com.example.capstone.domain.crosswalk.service;

import com.example.capstone.domain.crosswalk.service.sync.PublicDataCollector;
import org.springframework.stereotype.Service;

@Service
public class SeoulCrosswalkCollectorStub implements PublicDataCollector {
    @Override
    public void collectAndSave() {
        // TODO: 구현 전 임시
    }
}