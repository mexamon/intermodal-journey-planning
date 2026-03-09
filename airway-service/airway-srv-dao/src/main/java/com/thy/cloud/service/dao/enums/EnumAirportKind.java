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
@Schema(description = "Airport type classification from OurAirports")
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum EnumAirportKind implements IEnum<String> {

    LARGE_AIRPORT("large_airport", "Large airport"),
    MEDIUM_AIRPORT("medium_airport", "Medium airport"),
    SMALL_AIRPORT("small_airport", "Small airport"),
    HELIPORT("heliport", "Heliport"),
    SEAPLANE_BASE("seaplane_base", "Seaplane base"),
    BALLOONPORT("balloonport", "Balloonport"),
    CLOSED("closed", "Closed airport");

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
