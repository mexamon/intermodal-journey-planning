package com.thy.cloud.service.api.datasync.gtfs.model;

/**
 * GTFS routes.txt record — a transit route (M1 line, bus 500, etc.).
 */
public record GtfsRoute(
        String routeId,
        String agencyId,
        String routeShortName,   // "M1"
        String routeLongName,    // "Yenikapı - Atatürk Havalimanı"
        int routeType            // 0=tram, 1=subway, 2=rail, 3=bus, 4=ferry
) {
    /**
     * Map GTFS route_type to our transport_mode code.
     */
    public String toTransportModeCode() {
        return switch (routeType) {
            case 0 -> "SUBWAY";    // Tram / Light rail → mapped to SUBWAY
            case 1 -> "SUBWAY";    // Subway/Metro
            case 2 -> "TRAIN";     // Rail
            case 3 -> "BUS";       // Bus
            case 4 -> "FERRY";     // Ferry
            default -> "BUS";
        };
    }
}
