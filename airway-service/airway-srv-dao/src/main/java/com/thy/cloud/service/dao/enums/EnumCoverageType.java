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
@Schema(description = "Coverage type defining how a transport mode operates geographically")
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum EnumCoverageType implements IEnum<String> {

    POINT_TO_POINT("POINT_TO_POINT", "Anywhere coverage (e.g. Uber)"),
    FIXED_STOP("FIXED_STOP", "Fixed stations (e.g. Bus/Train)"),
    NETWORK("NETWORK", "Network graph (e.g. Subway)"),
    COMPUTED("COMPUTED", "Distance-based (e.g. Walking)");

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
