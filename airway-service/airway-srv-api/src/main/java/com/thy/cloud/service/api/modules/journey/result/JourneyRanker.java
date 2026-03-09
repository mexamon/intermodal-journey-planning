package com.thy.cloud.service.api.modules.journey.result;

import com.thy.cloud.service.api.modules.journey.model.JourneyResult;
import com.thy.cloud.service.api.modules.journey.model.JourneySegment;
import com.thy.cloud.service.api.modules.transport.service.TransportService;
import com.thy.cloud.service.dao.enums.EnumModeCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Ranks, deduplicates, and labels journey results.
 * Ensures modal diversity so transit routes aren't pushed out by TAXI/UBER-only routes.
 * Diverse modes (GROUND_FIXED category) are loaded from DB and cached in Redis.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JourneyRanker {

    private static final int MAX_RESULTS = 10;

    private final TransportService transportService;

    /** Self-injection for AOP proxy — required so @Cacheable works on internal calls */
    @Autowired
    @Lazy
    private JourneyRanker self;

    /**
     * Load diverse mode codes from DB (category = GROUND_FIXED).
     * Cached in Redis — invalidated when transport modes change.
     */
    @Cacheable(value = "diverse_modes", key = "'GROUND_FIXED'")
    public Set<String> getDiverseModes() {
        Set<String> modes = transportService.listActiveModes().stream()
                .filter(tm -> tm.getCategory() == EnumModeCategory.GROUND_FIXED)
                .map(tm -> tm.getCode())
                .collect(Collectors.toSet());
        log.info("Loaded diverse modes (GROUND_FIXED): {}", modes);
        return modes;
    }

    /**
     * Sort, deduplicate, apply diversity filter, and assign labels.
     */
    public List<JourneyResult> rankAndLabel(List<JourneyResult> results, String sortBy) {
        if (results.isEmpty()) return results;

        Comparator<JourneyResult> comparator = switch (sortBy != null ? sortBy : "FASTEST") {
            case "CHEAPEST" -> Comparator.comparingInt(JourneyResult::getTotalCostCents);
            case "GREENEST" -> Comparator.comparingInt(JourneyResult::getCo2Grams);
            case "FEWEST_TRANSFERS" -> Comparator.comparingInt(JourneyResult::getTransfers);
            default -> Comparator.comparingInt(JourneyResult::getTotalDurationMin);
        };

        results.sort(comparator);

        // Deduplicate
        List<JourneyResult> unique = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (JourneyResult r : results) {
            String key = r.getSegments().stream()
                    .map(s -> (s.getEdgeId() != null ? s.getEdgeId() : s.getMode()) + ":" + s.getDepartureTime())
                    .collect(Collectors.joining("|"));
            if (seen.add(key)) unique.add(r);
        }

        log.info("Ranking: {} total unique paths", unique.size());

        // Modal diversity: reserve slots for routes with unique mode combinations
        Set<String> diverseModes = self.getDiverseModes();
        List<JourneyResult> top = new ArrayList<>();
        Map<String, JourneyResult> bestByModeSet = new LinkedHashMap<>();

        for (JourneyResult r : unique) {
            String modeSetKey = r.getSegments().stream()
                    .map(JourneySegment::getMode)
                    .collect(Collectors.joining("+"));
            boolean hasDiverseMode = r.getSegments().stream()
                    .anyMatch(s -> diverseModes.contains(s.getMode()));
            if (hasDiverseMode) {
                bestByModeSet.putIfAbsent(modeSetKey, r);
            } else {
                if (top.size() < MAX_RESULTS) top.add(r);
            }
        }

        int diverseSlots = 0;
        for (JourneyResult dr : bestByModeSet.values()) {
            if (diverseSlots >= 5) break;
            if (top.size() < MAX_RESULTS) {
                top.add(dr);
            } else {
                top.set(MAX_RESULTS - 1 - diverseSlots, dr);
            }
            diverseSlots++;
        }

        top.sort(comparator);
        log.info("Final top: {} results ({} diverse mode-sets from {} unique combos)",
                top.size(), diverseSlots, bestByModeSet.size());

        if (!top.isEmpty()) assignLabels(top);
        return top;
    }

    private void assignLabels(List<JourneyResult> results) {
        JourneyResult fastest = results.stream().min(Comparator.comparingInt(JourneyResult::getTotalDurationMin)).orElse(null);
        JourneyResult cheapest = results.stream().min(Comparator.comparingInt(JourneyResult::getTotalCostCents)).orElse(null);
        JourneyResult greenest = results.stream().min(Comparator.comparingInt(JourneyResult::getCo2Grams)).orElse(null);
        JourneyResult fewest = results.stream().min(Comparator.comparingInt(JourneyResult::getTransfers)).orElse(null);

        for (JourneyResult r : results) {
            List<String> tags = new ArrayList<>();
            if (r == fastest) { r.setLabel("En Hızlı"); tags.add("fastest"); }
            if (r == cheapest) { if (r.getLabel() == null) r.setLabel("En Ucuz"); tags.add("cheapest"); }
            if (r == greenest) { if (r.getLabel() == null) r.setLabel("En Yeşil"); tags.add("greenest"); }
            if (r == fewest) { if (r.getLabel() == null) r.setLabel("En Az Aktarma"); tags.add("fewest_transfers"); }
            if (r.getLabel() == null) r.setLabel("Alternatif " + (results.indexOf(r) + 1));
            r.setTags(tags);
        }

        if (!results.isEmpty() && results.get(0).getTags() != null) {
            results.get(0).getTags().add("recommended");
        }
    }
}
