package com.thy.cloud.service.dao.entity.inventory;

import com.thy.cloud.data.jpa.entity.AbstractAuditionGuidKeyEntity;
import com.thy.cloud.service.dao.enums.EnumLocationType;
import com.thy.cloud.service.dao.enums.EnumLocationSource;
import com.thy.cloud.service.dao.entity.reference.RefCountry;
import com.thy.cloud.service.dao.entity.reference.RefRegion;
import lombok.*;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Table(name = "location")
public class Location extends AbstractAuditionGuidKeyEntity {

    @Column(name = "type", nullable = false)
    private EnumLocationType type;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "country_iso_code", nullable = false, length = 2)
    private String countryIsoCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_iso_code", referencedColumnName = "iso_code", insertable = false, updatable = false)
    @ToString.Exclude
    private RefCountry country;

    @Column(name = "region_code")
    private String regionCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_code", referencedColumnName = "code", insertable = false, updatable = false)
    @ToString.Exclude
    private RefRegion region;

    @Column(name = "city")
    private String city;

    @Column(name = "timezone")
    private String timezone;

    @Column(name = "lat", precision = 9, scale = 6)
    private BigDecimal lat;

    @Column(name = "lon", precision = 9, scale = 6)
    private BigDecimal lon;

    @Column(name = "iata_code", length = 3)
    private String iataCode;

    @Column(name = "icao_code", length = 4)
    private String icaoCode;

    @Column(name = "gps_code")
    private String gpsCode;

    @Column(name = "local_code")
    private String localCode;

    @Column(name = "is_searchable", nullable = false)
    private Boolean isSearchable;

    @Column(name = "search_priority", nullable = false)
    private Integer searchPriority;

    @Column(name = "source", nullable = false)
    private EnumLocationSource source;

    @Column(name = "source_pk")
    private String sourcePk;
}
