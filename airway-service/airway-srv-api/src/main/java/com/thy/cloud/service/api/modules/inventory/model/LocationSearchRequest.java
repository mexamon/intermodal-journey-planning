package com.thy.cloud.service.api.modules.inventory.model;

import com.thy.cloud.service.dao.enums.EnumLocationType;
import lombok.Data;

@Data
public class LocationSearchRequest {

    private String name;
    private String iataCode;
    private String icaoCode;
    private String countryIsoCode;
    private String city;
    private EnumLocationType type;
}
