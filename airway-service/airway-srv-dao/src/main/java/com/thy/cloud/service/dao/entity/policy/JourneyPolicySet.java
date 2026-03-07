package com.thy.cloud.service.dao.entity.policy;

import com.thy.cloud.data.jpa.entity.AbstractAuditionGuidKeyEntity;
import com.thy.cloud.service.dao.enums.EnumPolicyScopeType;
import com.thy.cloud.service.dao.enums.EnumPolicySegment;
import com.thy.cloud.service.dao.enums.EnumPolicyStatus;
import lombok.*;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Table(name = "journey_policy_set")
public class JourneyPolicySet extends AbstractAuditionGuidKeyEntity {

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "scope_type", nullable = false)
    @Convert(converter = com.thy.cloud.service.dao.enums.converter.EnumPolicyScopeTypeConverter.class)
    private EnumPolicyScopeType scopeType;

    @Column(name = "scope_key", nullable = false)
    private String scopeKey;

    @Column(name = "segment", nullable = false)
    @Convert(converter = com.thy.cloud.service.dao.enums.converter.EnumPolicySegmentConverter.class)
    private EnumPolicySegment segment;

    @Column(name = "status", nullable = false)
    @Convert(converter = com.thy.cloud.service.dao.enums.converter.EnumPolicyStatusConverter.class)
    private EnumPolicyStatus status;

    @Column(name = "version", nullable = false, insertable = false, updatable = false)
    private Integer policyVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_policy_set_id")
    @ToString.Exclude
    private JourneyPolicySet parentPolicySet;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "effective_from")
    private Date effectiveFrom;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "effective_to")
    private Date effectiveTo;

    @Column(name = "description")
    private String description;

    @Column(name = "created_by")
    private String createdBy;
}
