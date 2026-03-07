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
@Schema(description = "Data source for location records")
public enum EnumLocationSource implements IEnum<String> {

    INTERNAL("INTERNAL", "Admin entered"),
    OURAIRPORTS("OURAIRPORTS", "CSV seed from OurAirports"),
    GOOGLE_PLACES("GOOGLE_PLACES", "Google Places API"),
    GTFS("GTFS", "General Transit Feed Specification"),
    API("API", "External API source");

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
