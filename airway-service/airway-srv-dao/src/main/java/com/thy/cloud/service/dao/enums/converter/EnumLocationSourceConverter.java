package com.thy.cloud.service.dao.enums.converter;

import com.thy.cloud.base.core.enums.AbstractEnumConverter;
import com.thy.cloud.service.dao.enums.EnumLocationSource;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EnumLocationSourceConverter extends AbstractEnumConverter<EnumLocationSource, String> {
}
