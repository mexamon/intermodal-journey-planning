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
@Schema(description = "Service area type")
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
