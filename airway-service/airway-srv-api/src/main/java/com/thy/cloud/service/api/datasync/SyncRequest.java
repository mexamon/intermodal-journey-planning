package com.thy.cloud.service.api.datasync;

import java.time.LocalDate;

/**
 * Parameters for a data sync operation.
 */
public record SyncRequest(
        String region,            // "TR", "EU", "*" for all
        LocalDate fromDate,
        LocalDate toDate,
        boolean fullSync          // true = replace all, false = incremental
) {
    public static SyncRequest fullSyncForRegion(String region, LocalDate from, LocalDate to) {
        return new SyncRequest(region, from, to, true);
    }

    public static SyncRequest incremental(String region, LocalDate from, LocalDate to) {
        return new SyncRequest(region, from, to, false);
    }
}
