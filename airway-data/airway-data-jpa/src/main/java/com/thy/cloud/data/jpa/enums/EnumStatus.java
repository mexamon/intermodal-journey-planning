package com.thy.cloud.data.jpa.enums;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.thy.cloud.base.core.enums.IEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@Tag(name = "EnumStatus", description = "EnumStatus Enumeration")
public enum EnumStatus implements IEnum<Integer> {

    PASSIVE(0, "PASSIVE"),
    ACTIVE(1, "ACTIVE");

    /**
     * Return code
     */
    private Integer value;

    /**
     * Return description
     */
    private String desc;

    @Override
    public Integer getValue() {
        return this.value;
    }

    @Override
    public String getDesc() {
        return this.desc;
    }

    @Override
    @Schema(description = "Codes", allowableValues = "ACTIVE,PASSIVE", example = "ACTIVE")
    public String getCode() {
        return toString();
    }

}
