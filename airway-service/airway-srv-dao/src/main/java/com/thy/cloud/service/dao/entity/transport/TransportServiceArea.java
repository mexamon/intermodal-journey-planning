package com.thy.cloud.service.dao.entity.transport;

import com.thy.cloud.data.jpa.entity.AbstractAuditionGuidKeyEntity;
import com.thy.cloud.service.dao.enums.EnumAreaType;
import com.thy.cloud.service.dao.entity.inventory.Provider;
import com.thy.cloud.service.dao.entity.reference.RefCountry;
import lombok.*;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Table(name = "transport_service_area")
public class TransportServiceArea extends AbstractAuditionGuidKeyEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transport_mode_id")
    @ToString.Exclude
    private TransportMode transportMode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    @ToString.Exclude
    private Provider provider;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "area_type", nullable = false)
    @Convert(converter = com.thy.cloud.service.dao.enums.converter.EnumAreaTypeConverter.class)
    private EnumAreaType areaType;

    @Column(name = "center_lat", precision = 9, scale = 6)
    private BigDecimal centerLat;

    @Column(name = "center_lon", precision = 9, scale = 6)
    private BigDecimal centerLon;

    @Column(name = "radius_m")
    private Integer radiusM;

    @Column(name = "boundary_geojson", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String boundaryGeojson;

    @Column(name = "country_iso_code", length = 2)
    private String countryIsoCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_iso_code", referencedColumnName = "iso_code", insertable = false, updatable = false)
    @ToString.Exclude
    private RefCountry country;

    @Column(name = "city")
    private String city;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Temporal(TemporalType.DATE)
    @Column(name = "valid_from")
    private Date validFrom;

    @Temporal(TemporalType.DATE)
    @Column(name = "valid_to")
    private Date validTo;

    @Column(name = "config_json", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String configJson;
}
