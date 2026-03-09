package com.thy.cloud.service.dao.enums;

import com.thy.cloud.base.core.enums.IEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Schema(description = "Edge schedule type — how the edge resolves its timetable")
public enum EnumScheduleType implements IEnum<String> {

    FIXED("FIXED", "Named departures (flights, ICE trains) — see edge_trip"),
    FREQUENCY("FREQUENCY", "Interval-based (metro every 5min, bus every 15min)"),
    ON_DEMAND("ON_DEMAND", "No fixed schedule (Uber, walking, taxi)");

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
