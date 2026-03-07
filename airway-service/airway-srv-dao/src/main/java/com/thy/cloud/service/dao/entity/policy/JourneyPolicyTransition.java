package com.thy.cloud.service.dao.entity.policy;

import com.thy.cloud.data.jpa.entity.AbstractAuditionGuidKeyEntity;
import lombok.*;

import jakarta.persistence.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Table(name = "journey_policy_transition")
public class JourneyPolicyTransition extends AbstractAuditionGuidKeyEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_set_id", nullable = false)
    @ToString.Exclude
    private JourneyPolicySet policySet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_node_id", nullable = false)
    @ToString.Exclude
    private JourneyPolicyNode fromNode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_node_id", nullable = false)
    @ToString.Exclude
    private JourneyPolicyNode toNode;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "guard_json", columnDefinition = "JSONB")
    private String guardJson;

    @Column(name = "ui_json", columnDefinition = "JSONB")
    private String uiJson;
}
