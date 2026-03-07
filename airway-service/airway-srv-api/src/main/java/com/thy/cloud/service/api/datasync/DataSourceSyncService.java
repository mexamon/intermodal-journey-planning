package com.thy.cloud.service.api.datasync;

/**
 * Syncs external data sources into the transportation_edge + edge_trip tables.
 * <p>
 * Used by import-time sources (Amadeus, GTFS) — NOT by query-time sources (Google, Computed).
 * <p>
 * Implementations:
 * <ul>
 *     <li>{@code AmadeusSyncService} — syncs flight schedules from Amadeus API (or mock)</li>
 *     <li>{@code GtfsSyncService} — imports GTFS transit feeds (bus, metro, train, ferry)</li>
 * </ul>
 */
public interface DataSourceSyncService {

    /**
     * The data source type this syncer handles.
     *
     * @return "AMADEUS" or "GTFS"
     */
    String getSourceType();

    /**
     * Execute a full or incremental sync.
     *
     * @param request sync parameters (region, date range, full/incremental)
     * @return summary of what was synced
     */
    SyncResult sync(SyncRequest request);
}
