package com.thy.cloud.service.dao.entity.transport;

import com.thy.cloud.data.jpa.entity.AbstractAuditionGuidKeyEntity;
import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Table(name = "edge_trip")
public class EdgeTrip extends AbstractAuditionGuidKeyEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edge_id", nullable = false)
    @ToString.Exclude
    private TransportationEdge edge;

    @Column(name = "service_code")
    private String serviceCode;

    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;

    @Column(name = "arrival_time", nullable = false)
    private LocalTime arrivalTime;

    @Column(name = "operating_days_mask", nullable = false)
    private Short operatingDaysMask;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "estimated_cost_cents")
    private Integer estimatedCostCents;

    @Column(name = "attrs_json", columnDefinition = "JSONB")
    private String attrsJson;
}
