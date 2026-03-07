package com.thy.cloud.data.jpa.enums;

import com.thy.cloud.base.core.enums.AbstractEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EnumStatusConverter extends AbstractEnumConverter<EnumStatus, Integer> {

}