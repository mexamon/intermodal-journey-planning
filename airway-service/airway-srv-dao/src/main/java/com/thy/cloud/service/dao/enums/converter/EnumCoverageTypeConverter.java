package com.thy.cloud.service.dao.enums.converter;

import com.thy.cloud.base.core.enums.AbstractEnumConverter;
import com.thy.cloud.service.dao.enums.EnumCoverageType;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EnumCoverageTypeConverter extends AbstractEnumConverter<EnumCoverageType, String> {
}
