package com.thy.cloud.service.api.modules.journey;

import com.thy.cloud.base.core.api.Result;
import com.thy.cloud.service.api.modules.journey.model.JourneyResult;
import com.thy.cloud.service.api.modules.journey.model.JourneySearchRequest;
import com.thy.cloud.service.api.modules.journey.service.JourneySearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/journey")
@RequiredArgsConstructor
public class JourneyController {

    private final JourneySearchService journeySearchService;

    @PostMapping(value = "/search", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<List<JourneyResult>> search(@RequestBody @Valid JourneySearchRequest request) {
        return Result.success(journeySearchService.search(request));
    }
}
