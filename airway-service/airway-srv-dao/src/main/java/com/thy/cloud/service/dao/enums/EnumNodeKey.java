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
@Schema(description = "State machine node key")
public enum EnumNodeKey implements IEnum<String> {

    START("START", "Journey start"),
    BEFORE("BEFORE", "Pre-flight transfer"),
    FLIGHT("FLIGHT", "Flight segment"),
    AFTER("AFTER", "Post-flight transfer"),
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
