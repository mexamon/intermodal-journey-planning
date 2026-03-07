package com.thy.cloud.service.dao.entity.inventory;

import com.thy.cloud.data.jpa.entity.AbstractAuditionGuidKeyEntity;
import com.thy.cloud.service.dao.enums.EnumAirportKind;
import lombok.*;

import jakarta.persistence.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Table(name = "airport_profile")
public class AirportProfile extends AbstractAuditionGuidKeyEntity {

    @Column(name = "airport_kind", nullable = false)
    @Convert(converter = com.thy.cloud.service.dao.enums.converter.EnumAirportKindConverter.class)
    private EnumAirportKind airportKind;

    @Column(name = "elevation_ft")
    private Integer elevationFt;

    @Column(name = "scheduled_service")
    private Boolean scheduledService;

    @Column(name = "home_link")
    private String homeLink;

    @Column(name = "wikipedia_link")
    private String wikipediaLink;

    @Column(name = "keywords")
    private String keywords;

    @Column(name = "terminal_count")
    private Integer terminalCount;

    @Column(name = "avg_transfer_minutes")
    private Integer avgTransferMinutes;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", referencedColumnName = "id", insertable = false, updatable = false)
    @ToString.Exclude
    private Location location;
}
