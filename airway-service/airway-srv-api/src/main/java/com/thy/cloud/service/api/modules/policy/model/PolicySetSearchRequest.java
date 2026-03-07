package com.thy.cloud.service.api.modules.policy.model;

import com.thy.cloud.service.dao.enums.EnumPolicyScopeType;
import com.thy.cloud.service.dao.enums.EnumPolicySegment;
import com.thy.cloud.service.dao.enums.EnumPolicyStatus;
import lombok.Data;

@Data
public class PolicySetSearchRequest {

    private String code;
    private EnumPolicyScopeType scopeType;
    private String scopeKey;
    private EnumPolicySegment segment;
    private EnumPolicyStatus status;
}
