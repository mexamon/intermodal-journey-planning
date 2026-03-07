package com.thy.cloud.service.dao.entity.transport;

import com.thy.cloud.data.jpa.entity.AbstractAuditionGuidKeyEntity;
import com.thy.cloud.service.dao.enums.EnumEdgeStatus;
import com.thy.cloud.service.dao.enums.EnumEdgeSource;
import com.thy.cloud.service.dao.enums.EnumScheduleType;
import com.thy.cloud.service.dao.entity.inventory.Location;
import com.thy.cloud.service.dao.entity.inventory.Provider;
import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Table(name = "transportation_edge")
public class TransportationEdge extends AbstractAuditionGuidKeyEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_location_id", nullable = false)
    @ToString.Exclude
    private Location originLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_location_id", nullable = false)
    @ToString.Exclude
    private Location destinationLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transport_mode_id", nullable = false)
    @ToString.Exclude
    private TransportMode transportMode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    @ToString.Exclude
    private Provider provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_area_id")
    @ToString.Exclude
    private TransportServiceArea serviceArea;

    @Column(name = "service_code")
    @Deprecated // V013: Use EdgeTrip.serviceCode for FIXED-schedule trips
    private String serviceCode;

    // ── Schedule Type (V013) ──

    @Column(name = "schedule_type", nullable = false)
    private EnumScheduleType scheduleType;

    // ── Trips (V013 — FIXED schedule departures) ──

    @OneToMany(mappedBy = "edge", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<EdgeTrip> trips = new ArrayList<>();

    // ── Schedule ──

    @Column(name = "operating_days_mask", nullable = false)
    private Short operatingDaysMask;

    @Column(name = "operating_start_time")
    private LocalTime operatingStartTime;

    @Column(name = "operating_end_time")
    private LocalTime operatingEndTime;

    @Column(name = "valid_from")
    @Deprecated // V013: Use EdgeTrip.validFrom for per-trip validity
    private LocalDate validFrom;

    @Column(name = "valid_to")
    @Deprecated // V013: Use EdgeTrip.validTo for per-trip validity
    private LocalDate validTo;

    // ── Timetable (DEPRECATED V013 — use EdgeTrip) ──

    @Column(name = "departure_time")
    @Deprecated // V013: Use EdgeTrip.departureTime
    private LocalTime departureTime;

    @Column(name = "arrival_time")
    @Deprecated // V013: Use EdgeTrip.arrivalTime
    private LocalTime arrivalTime;

    // ── Frequency (high-frequency modes) ──

    @Column(name = "frequency_minutes")
    private Integer frequencyMinutes;

    // ── Status & Source ──

    @Column(name = "status", nullable = false)
    private EnumEdgeStatus status;

    @Column(name = "source", nullable = false)
    private EnumEdgeSource source;

    // ── Estimates ──

    @Column(name = "estimated_duration_min")
    private Integer estimatedDurationMin;

    @Column(name = "estimated_cost_cents")
    @Deprecated // V013: Use EdgeTrip.estimatedCostCents or Fare table
    private Integer estimatedCostCents;

    @Column(name = "distance_m")
    private Integer distanceM;

    // ── Environmental ──

    @Column(name = "co2_grams")
    private Integer co2Grams;

    // ── Walking access ──

    @Column(name = "walking_access_origin_m")
    private Integer walkingAccessOriginM;

    @Column(name = "walking_access_dest_m")
    private Integer walkingAccessDestM;

    @Column(name = "attrs_json", columnDefinition = "JSONB")
    private String attrsJson;
}
