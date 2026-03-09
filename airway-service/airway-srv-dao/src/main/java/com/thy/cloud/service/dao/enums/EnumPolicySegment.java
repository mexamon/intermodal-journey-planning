package com.thy.cloud.service.dao.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.thy.cloud.base.core.enums.IEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Schema(description = "Customer segment for journey policies")
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum EnumPolicySegment implements IEnum<String> {

    DEFAULT("DEFAULT", "Default segment"),
    CORPORATE("CORPORATE", "Corporate segment"),
    ELITE("ELITE", "Elite segment"),
    IRROPS("IRROPS", "Irregular operations");

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

    @JsonCreator
    public static EnumPolicySegment fromJson(@JsonProperty("value") String value) {
        for (EnumPolicySegment t : values()) {
            if (t.value.equals(value)) return t;
        }
        return null;
    }
}
