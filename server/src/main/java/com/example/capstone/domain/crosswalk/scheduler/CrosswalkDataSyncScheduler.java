package com.example.capstone.domain.crosswalk.scheduler;

import com.example.capstone.domain.crosswalk.service.CrosswalkDataSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CrosswalkDataSyncScheduler {

    private final CrosswalkDataSyncService crosswalkDataSyncService;

    @Scheduled(cron = "0 0 3 * * *")
    public void syncDaily() {
        crosswalkDataSyncService.syncAll();
    }
}