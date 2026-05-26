package com.example.capstone.domain.crosswalk.service.sync;

import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CrosswalkSyncStats {

    private final String sourceName;
    private int totalCount;
    private int successCount;
    private int failureCount;
    private final Map<FailureReason, Integer> failureReasons =
            new EnumMap<>(FailureReason.class);

    public CrosswalkSyncStats(String sourceName) {
        this.sourceName = sourceName;
    }

    public void increaseTotal() {
        totalCount++;
    }

    public void increaseTotal(int count) {
        totalCount += count;
    }

    public void increaseSuccess() {
        successCount++;
    }

    public void increaseFailure(FailureReason reason) {
        failureCount++;
        failureReasons.merge(reason, 1, Integer::sum);
    }

    public String sourceName() {
        return sourceName;
    }

    public int totalCount() {
        return totalCount;
    }

    public int successCount() {
        return successCount;
    }

    public int failureCount() {
        return failureCount;
    }

    public Map<FailureReason, Integer> failureReasons() {
        return Map.copyOf(failureReasons);
    }

    public String failureSummary() {
        if (failureReasons.isEmpty()) {
            return "{}";
        }

        return failureReasons.entrySet().stream()
                .map(entry -> entry.getKey().name() + "=" + entry.getValue())
                .collect(Collectors.joining(", ", "{", "}"));
    }
}