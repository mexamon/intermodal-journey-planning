package com.thy.cloud.service.dao.enums.converter;

import com.thy.cloud.base.core.enums.AbstractEnumConverter;
import com.thy.cloud.service.dao.enums.EnumPolicySegment;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EnumPolicySegmentConverter extends AbstractEnumConverter<EnumPolicySegment, String> {
}
