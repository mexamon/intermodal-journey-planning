package com.thy.cloud.service.dao.enums.converter;

import com.thy.cloud.base.core.enums.AbstractEnumConverter;
import com.thy.cloud.service.dao.enums.EnumAirportKind;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EnumAirportKindConverter extends AbstractEnumConverter<EnumAirportKind, String> {
}
