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
@Schema(description = "Transport mode category")
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum EnumModeCategory implements IEnum<String> {

    AIR("AIR", "Air transport"),
    GROUND_FIXED("GROUND_FIXED", "Fixed-route ground transport"),
    GROUND_FLEX("GROUND_FLEX", "Flexible ground transport"),
    PEDESTRIAN("PEDESTRIAN", "Walking");

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
