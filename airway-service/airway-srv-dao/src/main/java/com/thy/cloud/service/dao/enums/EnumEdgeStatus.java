package com.thy.cloud.service.dao.enums;

import com.thy.cloud.base.core.enums.IEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Schema(description = "Edge status")
public enum EnumEdgeStatus implements IEnum<String> {

    ACTIVE("ACTIVE", "Active edge"),
    INACTIVE("INACTIVE", "Inactive edge");

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
