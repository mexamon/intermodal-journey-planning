package com.thy.cloud.service.dao.repository.policy;

import com.thy.cloud.data.jpa.repository.GenericRepository;
import com.thy.cloud.service.dao.entity.policy.JourneyPolicyTransition;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JourneyPolicyTransitionRepository extends GenericRepository<JourneyPolicyTransition, UUID> {

    List<JourneyPolicyTransition> findByPolicySetId(UUID policySetId);

    List<JourneyPolicyTransition> findByFromNodeId(UUID fromNodeId);
}
