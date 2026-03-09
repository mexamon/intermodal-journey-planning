package com.thy.cloud.service.api.modules.journey.service;

import com.thy.cloud.service.api.modules.journey.filter.PolicyFilter;
import com.thy.cloud.service.api.modules.journey.graph.GraphBuilder;
import com.thy.cloud.service.api.modules.journey.graph.HubDiscoveryService;
import com.thy.cloud.service.api.modules.journey.model.JourneyResult;
import com.thy.cloud.service.api.modules.journey.model.JourneySearchRequest;
import com.thy.cloud.service.api.modules.journey.result.JourneyRanker;
import com.thy.cloud.service.api.modules.journey.result.ResultMapper;
import com.thy.cloud.service.api.modules.journey.search.BfsPathFinder;
import com.thy.cloud.service.api.modules.policy.service.PolicyResolver;
import com.thy.cloud.service.api.resolver.model.ResolvedEdge;
import com.thy.cloud.service.api.resolver.model.ResolvedLocation;
import com.thy.cloud.service.api.modules.transport.service.TransportService;
import com.thy.cloud.service.dao.entity.inventory.Location;
import com.thy.cloud.service.dao.entity.policy.JourneyPolicyConstraints;
import com.thy.cloud.service.dao.entity.transport.TransportMode;
import com.thy.cloud.service.dao.enums.EnumModeCategory;
import com.thy.cloud.service.dao.repository.inventory.LocationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JourneySearchServiceImpl}.
 * All dependencies are mocked — no Spring context needed.
 */
@ExtendWith(MockitoExtension.class)
class JourneySearchServiceImplTest {

    @Mock LocationRepository locationRepository;
    @Mock TransportService transportService;
    @Mock PolicyResolver policyResolver;
    @Mock HubDiscoveryService hubDiscovery;
    @Mock GraphBuilder graphBuilder;
    @Mock BfsPathFinder bfsPathFinder;
    @Mock PolicyFilter policyFilter;
    @Mock ResultMapper resultMapper;
    @Mock JourneyRanker journeyRanker;

    @InjectMocks
    JourneySearchServiceImpl service;

    // ── Test fixtures ─────────────────────────────────────────

    private static final UUID IST_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID LHR_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private JourneySearchRequest buildRequest(String originIata, String destIata) {
        JourneySearchRequest req = new JourneySearchRequest();
        req.setOriginIataCode(originIata);
        req.setDestinationIataCode(destIata);
        req.setDepartureDate(LocalDate.of(2026, 3, 15));
        req.setEarliestDeparture(LocalTime.of(6, 0));
        req.setMaxTransfers(3);
        req.setMaxDurationMinutes(1440);
        req.setSortBy("FASTEST");
        req.setTargetCurrency("EUR");
        return req;
    }

    private Location buildLocation(UUID id, String name, String iata,
                                   double lat, double lon, String country, String tz) {
        Location loc = new Location();
        loc.setId(id);
        loc.setName(name);
        loc.setIataCode(iata);
        loc.setLat(BigDecimal.valueOf(lat));
        loc.setLon(BigDecimal.valueOf(lon));
        loc.setCountryIsoCode(country);
        loc.setTimezone(tz);
        return loc;
    }

    // ── Tests ─────────────────────────────────────────────────

    @Test
    void search_sameOriginAndDestination_returnsEmptyList() {
        Location istLoc = buildLocation(IST_ID, "Istanbul Airport", "IST",
                41.275, 28.751, "TR", "Europe/Istanbul");

        when(locationRepository.findByIataCode("IST")).thenReturn(Optional.of(istLoc));

        JourneySearchRequest req = buildRequest("IST", "IST");
        List<JourneyResult> results = service.search(req);

        assertTrue(results.isEmpty());
        verifyNoInteractions(hubDiscovery, graphBuilder, bfsPathFinder);
    }

    @Test
    void search_normalFlow_callsAllComponents() {
        Location istLoc = buildLocation(IST_ID, "Istanbul Airport", "IST",
                41.275, 28.751, "TR", "Europe/Istanbul");
        Location lhrLoc = buildLocation(LHR_ID, "Heathrow Airport", "LHR",
                51.470, -0.454, "GB", "Europe/London");

        when(locationRepository.findByIataCode("IST")).thenReturn(Optional.of(istLoc));
        when(locationRepository.findByIataCode("LHR")).thenReturn(Optional.of(lhrLoc));
        when(hubDiscovery.discoverHubs(any())).thenReturn(List.of());
        when(policyResolver.resolveForRoute("IST", "LHR")).thenReturn(null);

        TransportMode flight = new TransportMode();
        flight.setCode("FLIGHT");
        flight.setCategory(EnumModeCategory.AIR);
        flight.setConfigJson("{\"transfer_time_min\": 60}");
        when(transportService.listActiveModes()).thenReturn(List.of(flight));

        when(policyFilter.apply(anyList(), isNull())).thenAnswer(inv -> inv.getArgument(0));
        when(resultMapper.mapAll(anyList(), any(), anyString())).thenReturn(new ArrayList<>());
        when(journeyRanker.rankAndLabel(anyList(), anyString())).thenAnswer(inv -> inv.getArgument(0));

        JourneySearchRequest req = buildRequest("IST", "LHR");
        List<JourneyResult> results = service.search(req);

        assertNotNull(results);
        verify(hubDiscovery, times(2)).discoverHubs(any());
        verify(graphBuilder).buildFirstMile(any(), anyList(), any(), anyMap(), anyMap());
        verify(graphBuilder).buildTrunk(anyList(), anyBoolean(), any(), any(), anyInt(), anyMap(), anyMap());
        verify(graphBuilder).buildLastMile(anyList(), any(), any(), anyMap(), anyMap());
        verify(bfsPathFinder).findPaths(anyMap(), anyString(), anySet(),
                any(LocalTime.class), anyInt(), anyInt(), anyMap(), anyList());
        verify(policyFilter).apply(anyList(), isNull());
        verify(resultMapper).mapAll(anyList(), any(LocalDate.class), eq("EUR"));
        verify(journeyRanker).rankAndLabel(anyList(), eq("FASTEST"));
    }

    @Test
    void search_withPolicyConstraints_usesConstraintValues() {
        Location istLoc = buildLocation(IST_ID, "Istanbul Airport", "IST",
                41.275, 28.751, "TR", "Europe/Istanbul");
        Location lhrLoc = buildLocation(LHR_ID, "Heathrow Airport", "LHR",
                51.470, -0.454, "GB", "Europe/London");

        when(locationRepository.findByIataCode("IST")).thenReturn(Optional.of(istLoc));
        when(locationRepository.findByIataCode("LHR")).thenReturn(Optional.of(lhrLoc));
        when(hubDiscovery.discoverHubs(any())).thenReturn(List.of());

        JourneyPolicyConstraints constraints = new JourneyPolicyConstraints();
        constraints.setMaxTransfers(2);
        constraints.setMaxLegs(3);
        constraints.setMaxFlights(1);
        constraints.setMaxTotalDurationMin(720);
        when(policyResolver.resolveForRoute("IST", "LHR")).thenReturn(constraints);

        TransportMode flight = new TransportMode();
        flight.setCode("FLIGHT");
        flight.setCategory(EnumModeCategory.AIR);
        when(transportService.listActiveModes()).thenReturn(List.of(flight));

        when(policyFilter.apply(anyList(), eq(constraints))).thenReturn(new ArrayList<>());
        when(resultMapper.mapAll(anyList(), any(), anyString())).thenReturn(new ArrayList<>());
        when(journeyRanker.rankAndLabel(anyList(), anyString())).thenReturn(new ArrayList<>());

        JourneySearchRequest req = buildRequest("IST", "LHR");
        service.search(req);

        verify(bfsPathFinder).findPaths(anyMap(), anyString(), anySet(),
                any(LocalTime.class), eq(2), eq(720), anyMap(), anyList());
        verify(policyFilter).apply(anyList(), eq(constraints));
    }

    @Test
    void search_withCoordinates_createsVirtualLocation() {
        JourneySearchRequest req = new JourneySearchRequest();
        req.setOriginLat(41.005);
        req.setOriginLon(28.978);
        req.setOriginQuery("Taksim");
        req.setOriginType("place");
        req.setDestinationIataCode("LHR");
        req.setDepartureDate(LocalDate.of(2026, 3, 15));

        Location lhrLoc = buildLocation(LHR_ID, "Heathrow Airport", "LHR",
                51.470, -0.454, "GB", "Europe/London");

        when(locationRepository.findByIataCode("LHR")).thenReturn(Optional.of(lhrLoc));
        when(hubDiscovery.discoverHubs(any())).thenReturn(List.of());
        when(policyResolver.resolveForRoute(isNull(), eq("LHR"))).thenReturn(null);

        TransportMode walking = new TransportMode();
        walking.setCode("WALKING");
        walking.setCategory(EnumModeCategory.PEDESTRIAN);
        when(transportService.listActiveModes()).thenReturn(List.of(walking));

        when(policyFilter.apply(anyList(), isNull())).thenReturn(new ArrayList<>());
        when(resultMapper.mapAll(anyList(), any(), anyString())).thenReturn(new ArrayList<>());
        when(journeyRanker.rankAndLabel(anyList(), anyString())).thenReturn(new ArrayList<>());

        List<JourneyResult> results = service.search(req);

        assertNotNull(results);
        verify(hubDiscovery, times(2)).discoverHubs(argThat(loc -> loc.name() != null));
    }

    @Test
    void search_missingLocationData_throwsException() {
        JourneySearchRequest req = new JourneySearchRequest();
        req.setOriginQuery("Unknown Place");
        req.setDestinationIataCode("LHR");

        assertThrows(IllegalArgumentException.class, () -> service.search(req));
    }
}
