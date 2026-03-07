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
@Schema(description = "Location type classification")
public enum EnumLocationType implements IEnum<String> {

    AIRPORT("AIRPORT", "Airport location"),
    CITY("CITY", "City location"),
    STATION("STATION", "Station location"),
    POI("POI", "Point of interest");

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
