package com.example.capstone.domain.crosswalk.service;

import com.example.capstone.domain.crosswalk.service.sync.NationalCrosswalkCollector;
import org.springframework.stereotype.Service;

@Service
public class NationalCrosswalkCollectorStub implements NationalCrosswalkCollector {
    @Override
    public void collectAndSave() {
        // TODO: 구현 전 임시
    }
}