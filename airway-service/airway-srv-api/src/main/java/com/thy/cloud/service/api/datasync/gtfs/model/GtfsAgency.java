package com.thy.cloud.service.api.datasync.gtfs.model;

/**
 * GTFS agency.txt record — transit operator.
 */
public record GtfsAgency(
        String agencyId,
        String agencyName,
        String agencyUrl,
        String agencyTimezone,
        String agencyLang
) {}
