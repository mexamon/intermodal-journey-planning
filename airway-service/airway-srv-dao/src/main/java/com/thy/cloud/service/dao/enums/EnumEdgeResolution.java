package com.thy.cloud.service.dao.enums;

import com.thy.cloud.base.core.enums.IEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Schema(description = "Edge resolution strategy for transport modes")
public enum EnumEdgeResolution implements IEnum<String> {

    STATIC("STATIC", "Pre-defined edges"),
    API_DYNAMIC("API_DYNAMIC", "Resolved at query time"),
    COMPUTED("COMPUTED", "Distance calculation"),
    HYBRID("HYBRID", "Static + API enrichment");

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
