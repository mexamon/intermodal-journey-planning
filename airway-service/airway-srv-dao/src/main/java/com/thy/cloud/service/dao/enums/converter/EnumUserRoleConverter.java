package com.thy.cloud.service.dao.enums.converter;

import com.thy.cloud.base.core.enums.AbstractEnumConverter;
import com.thy.cloud.service.dao.enums.EnumUserRole;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EnumUserRoleConverter extends AbstractEnumConverter<EnumUserRole, String> {
}
