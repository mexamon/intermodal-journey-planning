package com.thy.cloud.service.dao.repository.policy;

import com.thy.cloud.data.jpa.repository.GenericRepository;
import com.thy.cloud.service.dao.entity.policy.JourneyPolicySet;
import com.thy.cloud.service.dao.enums.EnumPolicyStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JourneyPolicySetRepository extends GenericRepository<JourneyPolicySet, UUID> {

    Optional<JourneyPolicySet> findByCode(String code);

    List<JourneyPolicySet> findByStatus(EnumPolicyStatus status);

    List<JourneyPolicySet> findByScopeTypeAndScopeKeyAndStatus(
            com.thy.cloud.service.dao.enums.EnumPolicyScopeType scopeType,
            String scopeKey,
            EnumPolicyStatus status);
}
