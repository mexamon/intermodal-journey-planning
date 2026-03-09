package com.thy.cloud.service.api.modules.journey.result;

import com.thy.cloud.service.api.modules.journey.model.JourneyResult;
import com.thy.cloud.service.api.modules.journey.model.JourneySegment;
import com.thy.cloud.service.api.resolver.model.ResolvedEdge;
import com.thy.cloud.service.api.util.CurrencyConverter;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Converts resolved edge paths into JourneyResult DTOs.
 */
@Component
public class ResultMapper {

    /**
     * Convert a list of paths into JourneyResult list.
     */
    public List<JourneyResult> mapAll(List<List<ResolvedEdge>> paths, LocalDate date, String targetCurrency) {
        return paths.stream()
                .map(path -> buildResult(path, date, targetCurrency))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private JourneyResult buildResult(List<ResolvedEdge> path, LocalDate date, String targetCurrency) {
        if (path.isEmpty()) return null;

        List<JourneySegment> segments = new ArrayList<>();
        int totalCost = 0;
        int totalCO2 = 0;

        for (ResolvedEdge edge : path) {
            int originalCostCents = edge.costCents() != null ? edge.costCents() : 0;
            String segCurrency = edge.currency() != null ? edge.currency() : "EUR";
            int convertedCost = CurrencyConverter.convert(originalCostCents, segCurrency, targetCurrency);

            segments.add(JourneySegment.builder()
                    .mode(edge.transportModeCode())
                    .originCode(edge.origin().iataCode() != null ? edge.origin().iataCode() : shortCode(edge.origin().name()))
                    .originName(edge.origin().name())
                    .destinationCode(edge.destination().iataCode() != null ? edge.destination().iataCode() : shortCode(edge.destination().name()))
                    .destinationName(edge.destination().name())
                    .departureTime(edge.departureTime() != null ? edge.departureTime().toString().substring(0, 5) : "")
                    .arrivalTime(edge.arrivalTime() != null ? edge.arrivalTime().toString().substring(0, 5) : "")
                    .durationMin(edge.durationMin())
                    .serviceCode(edge.serviceCode())
                    .provider(edge.providerCode())
                    .costCents(convertedCost)
                    .currency(targetCurrency)
                    .edgeId(edge.id() != null ? edge.id().toString() : null)
                    .originTimezone(edge.origin().timezone())
                    .destinationTimezone(edge.destination().timezone())
                    .build());

            totalCost += convertedCost;
            if (edge.co2Grams() != null) totalCO2 += edge.co2Grams();
        }

        // Flight-only duration + extract first/last flight times
        int flightDuration = 0;
        String firstFlightDep = null;
        String lastFlightArr = null;
        String depTimezone = null;
        String arrTimezone = null;
        String depCode = null;
        String arrCode = null;

        for (ResolvedEdge edge : path) {
            if ("FLIGHT".equals(edge.transportModeCode())) {
                flightDuration += edge.durationMin();
                if (firstFlightDep == null && edge.departureTime() != null) {
                    firstFlightDep = edge.departureTime().toString().substring(0, 5);
                    depTimezone = edge.origin().timezone();
                    depCode = edge.origin().iataCode() != null ? edge.origin().iataCode() : shortCode(edge.origin().name());
                }
                if (edge.arrivalTime() != null) {
                    lastFlightArr = edge.arrivalTime().toString().substring(0, 5);
                    arrTimezone = edge.destination().timezone();
                    arrCode = edge.destination().iataCode() != null ? edge.destination().iataCode() : shortCode(edge.destination().name());
                }
            }
        }

        if (flightDuration == 0) {
            for (ResolvedEdge edge : path) {
                flightDuration += edge.durationMin();
            }
        }

        return JourneyResult.builder()
                .id(UUID.randomUUID().toString())
                .segments(segments)
                .totalDurationMin(flightDuration)
                .totalCostCents(totalCost)
                .currency(targetCurrency)
                .co2Grams(totalCO2)
                .transfers(segments.size() - 1)
                .tags(new ArrayList<>())
                .departureTime(firstFlightDep)
                .arrivalTime(lastFlightArr)
                .departureTimezone(depTimezone)
                .arrivalTimezone(arrTimezone)
                .departureCode(depCode)
                .arrivalCode(arrCode)
                .build();
    }

    private String shortCode(String name) {
        if (name == null) return "???";
        return name.length() > 3 ? name.substring(0, 3).toUpperCase() : name.toUpperCase();
    }
}
