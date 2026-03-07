package com.thy.cloud.service.api.modules.journey.service;

import com.thy.cloud.service.api.modules.journey.model.JourneyResult;
import com.thy.cloud.service.api.modules.journey.model.JourneySearchRequest;

import java.util.List;

public interface JourneySearchService {

    List<JourneyResult> search(JourneySearchRequest request);
}
