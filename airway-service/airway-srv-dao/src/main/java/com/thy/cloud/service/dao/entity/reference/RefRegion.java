package com.thy.cloud.service.dao.entity.reference;

import com.thy.cloud.data.jpa.entity.AbstractAuditionGuidKeyEntity;
import lombok.*;

import jakarta.persistence.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Table(name = "ref_region")
public class RefRegion extends AbstractAuditionGuidKeyEntity {

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "local_code")
    private String localCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "continent", length = 2)
    private String continent;

    @Column(name = "country_iso_code", nullable = false, length = 2)
    private String countryIsoCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_iso_code", referencedColumnName = "iso_code", insertable = false, updatable = false)
    @ToString.Exclude
    private RefCountry country;

    @Column(name = "wikipedia_link")
    private String wikipediaLink;

    @Column(name = "keywords")
    private String keywords;
}
