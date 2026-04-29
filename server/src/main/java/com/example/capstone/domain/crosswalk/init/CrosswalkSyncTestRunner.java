package com.example.capstone.domain.crosswalk.init;

import com.example.capstone.domain.crosswalk.service.CrosswalkDataSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CrosswalkSyncTestRunner implements CommandLineRunner {

    private final CrosswalkDataSyncService crosswalkDataSyncService;

    @Override
    public void run(String... args) {
        crosswalkDataSyncService.syncAll();
    }
}