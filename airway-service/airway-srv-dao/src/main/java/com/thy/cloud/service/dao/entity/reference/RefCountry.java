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
@Table(name = "ref_country")
public class RefCountry extends AbstractAuditionGuidKeyEntity {

    @Column(name = "iso_code", nullable = false, length = 2)
    private String isoCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "continent", length = 2)
    private String continent;

    @Column(name = "wikipedia_link")
    private String wikipediaLink;

    @Column(name = "keywords")
    private String keywords;
}
