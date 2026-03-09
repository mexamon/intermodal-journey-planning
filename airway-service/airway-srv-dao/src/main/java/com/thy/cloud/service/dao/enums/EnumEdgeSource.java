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
@Schema(description = "Edge data source")
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum EnumEdgeSource implements IEnum<String> {

    MANUAL("MANUAL", "Admin entered"),
    GOOGLE_API("GOOGLE_API", "Google Directions API"),
    GTFS("GTFS", "General Transit Feed"),
    AMADEUS("AMADEUS", "Amadeus Flight API"),
    COMPUTED("COMPUTED", "Distance calculation");

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
