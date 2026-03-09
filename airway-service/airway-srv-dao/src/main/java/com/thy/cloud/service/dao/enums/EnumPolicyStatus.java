package com.thy.cloud.service.dao.enums;

import com.thy.cloud.base.core.enums.IEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Schema(description = "Journey policy status")
public enum EnumPolicyStatus implements IEnum<String> {

    DRAFT("DRAFT", "Draft policy"),
    ACTIVE("ACTIVE", "Active policy"),
    DEPRECATED("DEPRECATED", "Deprecated policy");

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
