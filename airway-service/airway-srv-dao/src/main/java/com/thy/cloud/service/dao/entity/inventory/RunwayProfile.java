package com.thy.cloud.service.dao.entity.inventory;

import com.thy.cloud.data.jpa.entity.AbstractAuditionGuidKeyEntity;
import lombok.*;

import jakarta.persistence.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Table(name = "runway_profile")
public class RunwayProfile extends AbstractAuditionGuidKeyEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    @ToString.Exclude
    private Location location;

    @Column(name = "ident")
    private String ident;

    @Column(name = "length_ft")
    private Integer lengthFt;

    @Column(name = "width_ft")
    private Integer widthFt;

    @Column(name = "surface")
    private String surface;

    @Column(name = "is_lighted", nullable = false)
    private Boolean isLighted;

    @Column(name = "is_closed", nullable = false)
    private Boolean isClosed;

    @Column(name = "source_pk")
    private String sourcePk;
}
