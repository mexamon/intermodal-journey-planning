package com.thy.cloud.service.dao.enums;

import com.thy.cloud.base.core.enums.IEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Schema(description = "Customer segment for journey policies")
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
}
