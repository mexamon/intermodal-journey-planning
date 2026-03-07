package com.thy.cloud.service.dao.entity.transport;

import com.thy.cloud.data.jpa.entity.AbstractAuditionGuidKeyEntity;
import com.thy.cloud.service.dao.enums.EnumFareClass;
import com.thy.cloud.service.dao.enums.EnumPricingType;
import lombok.*;

import jakarta.persistence.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Table(name = "fare")
public class Fare extends AbstractAuditionGuidKeyEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edge_id", nullable = false)
    @ToString.Exclude
    private TransportationEdge edge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    @ToString.Exclude
    private EdgeTrip trip;

    @Column(name = "fare_class", nullable = false)
    private EnumFareClass fareClass;

    @Column(name = "pricing_type", nullable = false)
    private EnumPricingType pricingType;

    @Column(name = "price_cents")
    private Integer priceCents;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "refundable", nullable = false)
    private Boolean refundable;

    @Column(name = "changeable", nullable = false)
    private Boolean changeable;

    @Column(name = "luggage_kg")
    private Integer luggageKg;

    @Column(name = "cabin_luggage_kg")
    private Integer cabinLuggageKg;
}
