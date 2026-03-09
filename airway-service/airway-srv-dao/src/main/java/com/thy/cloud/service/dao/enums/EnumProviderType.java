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
@Schema(description = "Transport service provider type")
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum EnumProviderType implements IEnum<String> {

    AIRLINE("AIRLINE", "Airline operator"),
    BUS_COMPANY("BUS_COMPANY", "Bus transport company"),
    TRAIN_OPERATOR("TRAIN_OPERATOR", "Train operator"),
    METRO_OPERATOR("METRO_OPERATOR", "Metro/subway operator"),
    RIDE_SHARE("RIDE_SHARE", "Ride-sharing service"),
    FERRY_OPERATOR("FERRY_OPERATOR", "Ferry operator"),
    OTHER("OTHER", "Other transport provider");

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
