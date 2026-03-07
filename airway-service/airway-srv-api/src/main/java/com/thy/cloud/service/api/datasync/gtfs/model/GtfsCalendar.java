package com.thy.cloud.service.api.datasync.gtfs.model;

/**
 * GTFS calendar.txt record — service schedule.
 */
public record GtfsCalendar(
        String serviceId,
        boolean monday,
        boolean tuesday,
        boolean wednesday,
        boolean thursday,
        boolean friday,
        boolean saturday,
        boolean sunday,
        String startDate,  // YYYYMMDD
        String endDate     // YYYYMMDD
) {
    /**
     * Convert to 7-bit bitmask (Mon=bit0 … Sun=bit6).
     */
    public short toBitmask() {
        short mask = 0;
        if (monday)    mask |= 1;
        if (tuesday)   mask |= 2;
        if (wednesday) mask |= 4;
        if (thursday)  mask |= 8;
        if (friday)    mask |= 16;
        if (saturday)  mask |= 32;
        if (sunday)    mask |= 64;
        return mask;
    }
}
