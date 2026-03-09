package com.thy.cloud.service.dao.enums;

import com.thy.cloud.base.core.enums.IEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Schema(description = "State machine node key")
public enum EnumNodeKey implements IEnum<String> {

    START("START", "Journey start"),
    BEFORE("BEFORE", "Pre-flight transfer"),
    FIRST_MILE("FIRST_MILE", "First mile transfer"),
    FLIGHT("FLIGHT", "Flight segment"),
    MAIN_HAUL("MAIN_HAUL", "Main haul flight"),
    INTERCHANGE("INTERCHANGE", "Inter-airport transfer"),
    AFTER("AFTER", "Post-flight transfer"),
    LAST_MILE("LAST_MILE", "Last mile transfer"),
    END("END", "Journey end"),
    WALK_ACCESS("WALK_ACCESS", "Walking access");

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
