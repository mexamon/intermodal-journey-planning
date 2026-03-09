package com.thy.cloud.service.dao.enums;

import com.thy.cloud.base.core.enums.IEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Schema(description = "User role for access control")
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum EnumUserRole implements IEnum<String> {

    ADMIN("ADMIN", "Full access"),
    AGENCY("AGENCY", "Route listing only");

    private String value;
    private String desc;

    @Override
    public String getValue() {
        return this.value;
    }

    @Override
    public String getDesc() {
        return this.desc;
    }
}
