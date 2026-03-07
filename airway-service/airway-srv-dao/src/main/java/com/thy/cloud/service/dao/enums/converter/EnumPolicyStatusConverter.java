package com.thy.cloud.service.dao.enums.converter;

import com.thy.cloud.base.core.enums.AbstractEnumConverter;
import com.thy.cloud.service.dao.enums.EnumPolicyStatus;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EnumPolicyStatusConverter extends AbstractEnumConverter<EnumPolicyStatus, String> {
}
