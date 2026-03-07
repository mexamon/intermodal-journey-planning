package com.thy.cloud.service.dao.entity.inventory;

import com.thy.cloud.data.jpa.entity.AbstractAuditionGuidKeyEntity;
import com.thy.cloud.service.dao.enums.EnumProviderType;
import com.thy.cloud.service.dao.entity.reference.RefCountry;
import lombok.*;

import jakarta.persistence.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Table(name = "provider")
public class Provider extends AbstractAuditionGuidKeyEntity {

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "type", nullable = false)
    @Convert(converter = com.thy.cloud.service.dao.enums.converter.EnumProviderTypeConverter.class)
    private EnumProviderType type;

    @Column(name = "country_iso_code", length = 2)
    private String countryIsoCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_iso_code", referencedColumnName = "iso_code", insertable = false, updatable = false)
    @ToString.Exclude
    private RefCountry country;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "config_json", columnDefinition = "JSONB")
    private String configJson;
}
