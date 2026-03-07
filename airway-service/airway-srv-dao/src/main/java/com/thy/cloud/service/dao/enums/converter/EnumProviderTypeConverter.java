package com.thy.cloud.service.dao.enums.converter;

import com.thy.cloud.base.core.enums.AbstractEnumConverter;
import com.thy.cloud.service.dao.enums.EnumProviderType;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EnumProviderTypeConverter extends AbstractEnumConverter<EnumProviderType, String> {
}
