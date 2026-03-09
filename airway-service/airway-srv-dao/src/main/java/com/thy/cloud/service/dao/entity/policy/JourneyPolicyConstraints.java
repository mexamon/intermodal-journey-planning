package com.thy.cloud.service.dao.entity.policy;

import com.thy.cloud.data.jpa.entity.AbstractAuditionGuidKeyEntity;
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
@Table(name = "journey_policy_constraints",
       uniqueConstraints = @UniqueConstraint(name = "uk_constraints_policy_set", columnNames = "policy_set_id"))
public class JourneyPolicyConstraints extends AbstractAuditionGuidKeyEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_set_id", referencedColumnName = "id")
    @ToString.Exclude
    private JourneyPolicySet policySet;

    @Column(name = "max_legs", nullable = false)
    private Integer maxLegs;

    @Column(name = "min_flights", nullable = false)
    private Integer minFlights;

    @Column(name = "max_flights", nullable = false)
    private Integer maxFlights;

    @Column(name = "min_transfers", nullable = false)
    private Integer minTransfers;

    @Column(name = "max_transfers", nullable = false)
    private Integer maxTransfers;

    @Column(name = "max_total_duration_min")
    private Integer maxTotalDurationMin;

    @Column(name = "max_walking_total_m")
    private Integer maxWalkingTotalM;

    @Column(name = "min_connection_minutes")
    private Integer minConnectionMinutes;

    @Column(name = "max_total_co2_grams")
    private Integer maxTotalCo2Grams;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferred_modes_json", columnDefinition = "jsonb")
    private String preferredModesJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "constraints_json", columnDefinition = "jsonb")
    private String constraintsJson;
}
