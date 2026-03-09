package com.thy.cloud.service.dao.entity.policy;

import com.thy.cloud.data.jpa.entity.AbstractAuditionGuidKeyEntity;
import com.thy.cloud.service.dao.enums.EnumNodeKey;
import lombok.*;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Table(name = "journey_policy_node")
public class JourneyPolicyNode extends AbstractAuditionGuidKeyEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_set_id", nullable = false)
    @ToString.Exclude
    private JourneyPolicySet policySet;

    @Column(name = "node_key", nullable = false)
    @Convert(converter = com.thy.cloud.service.dao.enums.converter.EnumNodeKeyConverter.class)
    private EnumNodeKey nodeKey;

    @Column(name = "min_visits", nullable = false)
    private Integer minVisits;

    @Column(name = "max_visits", nullable = false)
    private Integer maxVisits;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "props_json", columnDefinition = "jsonb")
    private String propsJson;

    @Column(name = "ui_x")
    private Integer uiX;

    @Column(name = "ui_y")
    private Integer uiY;
}
