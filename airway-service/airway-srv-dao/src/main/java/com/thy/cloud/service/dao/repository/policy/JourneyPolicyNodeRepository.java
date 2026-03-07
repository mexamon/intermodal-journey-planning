package com.thy.cloud.service.dao.repository.policy;

import com.thy.cloud.data.jpa.repository.GenericRepository;
import com.thy.cloud.service.dao.entity.policy.JourneyPolicyNode;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JourneyPolicyNodeRepository extends GenericRepository<JourneyPolicyNode, UUID> {

    List<JourneyPolicyNode> findByPolicySetId(UUID policySetId);
}
