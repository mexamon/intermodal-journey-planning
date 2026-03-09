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
@Schema(description = "Service area type")
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum EnumAreaType implements IEnum<String> {

    RADIUS("RADIUS", "Circle-based coverage"),
    POLYGON("POLYGON", "GeoJSON boundary"),
    CITY("CITY", "City-scoped"),
    COUNTRY("COUNTRY", "Country-scoped"),
    GLOBAL("GLOBAL", "Global coverage");

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
