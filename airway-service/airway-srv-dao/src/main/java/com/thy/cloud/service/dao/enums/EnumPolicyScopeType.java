package com.thy.cloud.service.dao.enums;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.thy.cloud.base.core.enums.IEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@Schema(description = "Journey policy scope type")
public enum EnumPolicyScopeType implements IEnum<String> {

    GLOBAL("GLOBAL", "Global scope"),
    COUNTRY("COUNTRY", "Country scope"),
    REGION("REGION", "Region scope"),
    AIRPORT("AIRPORT", "Airport scope"),
    AIRPORT_PAIR("AIRPORT_PAIR", "Airport pair scope");

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
