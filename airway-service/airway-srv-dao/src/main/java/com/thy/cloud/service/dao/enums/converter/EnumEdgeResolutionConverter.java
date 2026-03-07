package com.thy.cloud.service.dao.enums.converter;

import com.thy.cloud.base.core.enums.AbstractEnumConverter;
import com.thy.cloud.service.dao.enums.EnumEdgeResolution;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EnumEdgeResolutionConverter extends AbstractEnumConverter<EnumEdgeResolution, String> {
}
