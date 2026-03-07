package com.thy.cloud.service.dao.repository.policy;

import com.thy.cloud.data.jpa.repository.GenericRepository;
import com.thy.cloud.service.dao.entity.policy.JourneyPolicyConstraints;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JourneyPolicyConstraintsRepository extends GenericRepository<JourneyPolicyConstraints, UUID> {
}
