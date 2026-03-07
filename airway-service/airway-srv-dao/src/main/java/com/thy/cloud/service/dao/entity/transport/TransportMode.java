package com.thy.cloud.service.dao.entity.transport;

import com.thy.cloud.data.jpa.entity.AbstractAuditionGuidKeyEntity;
import com.thy.cloud.service.dao.enums.EnumModeCategory;
import com.thy.cloud.service.dao.enums.EnumCoverageType;
import com.thy.cloud.service.dao.enums.EnumEdgeResolution;
import lombok.*;

import jakarta.persistence.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Table(name = "transport_mode")
public class TransportMode extends AbstractAuditionGuidKeyEntity {

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "category", nullable = false)
    private EnumModeCategory category;

    @Column(name = "coverage_type", nullable = false)
    private EnumCoverageType coverageType;

    @Column(name = "edge_resolution", nullable = false)
    private EnumEdgeResolution edgeResolution;

    @Column(name = "requires_stop", nullable = false)
    private Boolean requiresStop;

    @Column(name = "max_walking_access_m")
    private Integer maxWalkingAccessM;

    @Column(name = "default_speed_kmh")
    private Integer defaultSpeedKmh;

    @Column(name = "api_provider")
    private String apiProvider;

    @Column(name = "icon")
    private String icon;

    @Column(name = "color_hex")
    private String colorHex;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "config_json", columnDefinition = "JSONB")
    private String configJson;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}
