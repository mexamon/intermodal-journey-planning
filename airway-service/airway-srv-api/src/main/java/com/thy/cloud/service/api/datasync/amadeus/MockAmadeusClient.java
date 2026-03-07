package com.thy.cloud.service.api.datasync.amadeus;

import com.thy.cloud.service.api.datasync.amadeus.model.AmadeusFlightOffer;
import com.thy.cloud.service.api.datasync.amadeus.model.AmadeusFlightOffer.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Mock Amadeus client returning hardcoded flight schedule data.
 * <p>
 * Contains 50+ flights across 13+ routes covering:
 * <ul>
 *     <li>Turkish Airlines (TK) — out of IST and SAW</li>
 *     <li>Pegasus Airlines (PC) — out of SAW</li>
 *     <li>Domestic routes (SAW↔ESB, SAW↔ADB)</li>
 *     <li>International routes (SAW/IST → LHR/LGW/STN/LTN/CDG/MUC/AMS/FCO)</li>
 * </ul>
 * <p>
 * When real Amadeus API keys are obtained, swap this implementation with
 * {@code RealAmadeusClient} — zero changes needed in sync logic.
 */
@Component
@Slf4j
public class MockAmadeusClient implements AmadeusClient {

    private final List<AmadeusFlightOffer> allFlights;

    public MockAmadeusClient() {
        this.allFlights = buildAllFlights();
        log.info("MockAmadeusClient initialized with {} flight offers", allFlights.size());
    }

    @Override
    public List<AmadeusFlightOffer> searchFlights(String originIata, String destinationIata,
                                                   LocalDate departureDate, int maxOffers) {
        return allFlights.stream()
                .filter(f -> {
                    var seg = f.itineraries().get(0).segments().get(0);
                    return seg.departure().iataCode().equals(originIata)
                            && seg.arrival().iataCode().equals(destinationIata);
                })
                .limit(maxOffers)
                .toList();
    }

    @Override
    public List<AmadeusFlightOffer> getAllScheduledFlights() {
        return Collections.unmodifiableList(allFlights);
    }

    // ═══════════════════════════════════════════════════════════
    //  DUMMY FLIGHT DATA BUILDER
    // ═══════════════════════════════════════════════════════════

    private List<AmadeusFlightOffer> buildAllFlights() {
        List<AmadeusFlightOffer> flights = new ArrayList<>();
        int id = 1;

        // ── SAW → LHR (Turkish Airlines) ──
        flights.add(flight(id++, "SAW", "LHR", "TK", "1987", "06:30", "09:20", 230, 18500, "77W", allDays()));
        flights.add(flight(id++, "SAW", "LHR", "TK", "1971", "10:15", "13:05", 230, 22000, "333", allDays()));
        flights.add(flight(id++, "SAW", "LHR", "TK", "1975", "14:40", "17:30", 230, 19500, "77W", allDays()));
        flights.add(flight(id++, "SAW", "LHR", "TK", "1979", "19:00", "21:50", 230, 17500, "321", weekdays()));
        flights.add(flight(id++, "SAW", "LHR", "TK", "1973", "22:30", "01:20", 230, 16000, "321", allDays()));

        // ── SAW → LGW (Pegasus) ──
        flights.add(flight(id++, "SAW", "LGW", "PC", "1171", "05:45", "08:25", 240, 9500, "320", allDays()));
        flights.add(flight(id++, "SAW", "LGW", "PC", "1173", "11:00", "13:40", 240, 11000, "320", allDays()));
        flights.add(flight(id++, "SAW", "LGW", "PC", "1175", "17:30", "20:10", 240, 10500, "321", weekdays()));
        flights.add(flight(id++, "SAW", "LGW", "PC", "1177", "23:00", "01:40", 240, 8500, "320", allDays()));

        // ── SAW → STN (Pegasus) ──
        flights.add(flight(id++, "SAW", "STN", "PC", "1191", "06:00", "08:45", 245, 8500, "320", allDays()));
        flights.add(flight(id++, "SAW", "STN", "PC", "1193", "13:30", "16:15", 245, 9500, "320", allDays()));
        flights.add(flight(id++, "SAW", "STN", "PC", "1195", "20:00", "22:45", 245, 7500, "320", weekdays()));

        // ── SAW → LTN (Turkish Airlines) ──
        flights.add(flight(id++, "SAW", "LTN", "TK", "1991", "07:00", "09:45", 225, 17000, "321", allDays()));
        flights.add(flight(id++, "SAW", "LTN", "TK", "1993", "15:00", "17:45", 225, 18500, "321", allDays()));

        // ── IST → LHR (Turkish Airlines) ──
        flights.add(flight(id++, "IST", "LHR", "TK", "1981", "06:00", "08:50", 230, 21000, "77W", allDays()));
        flights.add(flight(id++, "IST", "LHR", "TK", "1983", "09:30", "12:20", 230, 24000, "333", allDays()));
        flights.add(flight(id++, "IST", "LHR", "TK", "1985", "13:00", "15:50", 230, 22500, "77W", allDays()));
        flights.add(flight(id++, "IST", "LHR", "TK", "1989", "17:00", "19:50", 230, 23000, "77W", allDays()));
        flights.add(flight(id++, "IST", "LHR", "TK", "1995", "21:30", "00:20", 230, 19000, "321", allDays()));

        // ── IST → LGW (Turkish Airlines) ──
        flights.add(flight(id++, "IST", "LGW", "TK", "1991", "08:00", "10:40", 235, 20000, "321", allDays()));
        flights.add(flight(id++, "IST", "LGW", "TK", "1997", "16:30", "19:10", 235, 21500, "321", weekdays()));

        // ── LHR → SAW (Return — Turkish Airlines) ──
        flights.add(flight(id++, "LHR", "SAW", "TK", "1988", "10:30", "16:20", 220, 19000, "77W", allDays()));
        flights.add(flight(id++, "LHR", "SAW", "TK", "1972", "14:00", "19:50", 220, 22500, "333", allDays()));
        flights.add(flight(id++, "LHR", "SAW", "TK", "1976", "18:30", "00:20", 220, 18000, "77W", allDays()));
        flights.add(flight(id++, "LHR", "SAW", "TK", "1974", "23:00", "04:50", 220, 16500, "321", weekdays()));

        // ── LGW → SAW (Return — Pegasus) ──
        flights.add(flight(id++, "LGW", "SAW", "PC", "1172", "09:30", "15:10", 230, 9500, "320", allDays()));
        flights.add(flight(id++, "LGW", "SAW", "PC", "1174", "15:00", "20:40", 230, 10500, "320", allDays()));
        flights.add(flight(id++, "LGW", "SAW", "PC", "1176", "21:30", "03:10", 230, 8000, "320", weekdays()));

        // ── LHR → IST (Return — Turkish Airlines) ──
        flights.add(flight(id++, "LHR", "IST", "TK", "1982", "11:00", "16:50", 225, 21500, "77W", allDays()));
        flights.add(flight(id++, "LHR", "IST", "TK", "1984", "15:00", "20:50", 225, 23000, "333", allDays()));

        // ── IST → CDG Paris (Turkish Airlines) ──
        flights.add(flight(id++, "IST", "CDG", "TK", "1827", "07:00", "10:00", 240, 23000, "333", allDays()));
        flights.add(flight(id++, "IST", "CDG", "TK", "1829", "12:30", "15:30", 240, 25000, "77W", allDays()));
        flights.add(flight(id++, "IST", "CDG", "TK", "1831", "18:00", "21:00", 240, 21000, "321", allDays()));

        // ── IST → MUC Munich (Turkish Airlines) ──
        flights.add(flight(id++, "IST", "MUC", "TK", "1631", "06:30", "08:45", 195, 19000, "321", allDays()));
        flights.add(flight(id++, "IST", "MUC", "TK", "1633", "14:00", "16:15", 195, 21000, "321", allDays()));
        flights.add(flight(id++, "IST", "MUC", "TK", "1635", "20:00", "22:15", 195, 17500, "320", weekdays()));

        // ── IST → AMS Amsterdam (Turkish Airlines) ──
        flights.add(flight(id++, "IST", "AMS", "TK", "1951", "07:30", "10:15", 225, 22000, "333", allDays()));
        flights.add(flight(id++, "IST", "AMS", "TK", "1953", "16:00", "18:45", 225, 20000, "321", allDays()));

        // ── IST → FCO Rome (Turkish Airlines) ──
        flights.add(flight(id++, "IST", "FCO", "TK", "1861", "08:00", "10:00", 180, 18000, "321", allDays()));
        flights.add(flight(id++, "IST", "FCO", "TK", "1863", "15:30", "17:30", 180, 19500, "321", allDays()));

        // ── Domestic: SAW → ESB Ankara (Turkish Airlines) ──
        flights.add(flight(id++, "SAW", "ESB", "TK", "2120", "06:00", "07:15", 75, 6500, "321", allDays()));
        flights.add(flight(id++, "SAW", "ESB", "TK", "2122", "09:00", "10:15", 75, 7500, "321", allDays()));
        flights.add(flight(id++, "SAW", "ESB", "TK", "2124", "12:00", "13:15", 75, 7000, "320", allDays()));
        flights.add(flight(id++, "SAW", "ESB", "TK", "2126", "16:00", "17:15", 75, 7500, "321", allDays()));
        flights.add(flight(id++, "SAW", "ESB", "TK", "2128", "20:00", "21:15", 75, 6000, "320", weekdays()));

        // ── Domestic: SAW → ADB Izmir (Pegasus) ──
        flights.add(flight(id++, "SAW", "ADB", "PC", "2230", "06:30", "07:40", 70, 5500, "320", allDays()));
        flights.add(flight(id++, "SAW", "ADB", "PC", "2232", "10:00", "11:10", 70, 6500, "320", allDays()));
        flights.add(flight(id++, "SAW", "ADB", "PC", "2234", "14:30", "15:40", 70, 6000, "320", allDays()));
        flights.add(flight(id++, "SAW", "ADB", "PC", "2236", "19:00", "20:10", 70, 5000, "320", weekdays()));

        // ── Domestic: ESB → LHR (Turkish Airlines, connecting) ──
        flights.add(flight(id++, "ESB", "LHR", "TK", "2190", "08:00", "11:50", 240, 24000, "77W", allDays()));
        flights.add(flight(id++, "ESB", "LHR", "TK", "2192", "16:00", "19:50", 240, 22000, "321", weekdays()));

        // ── Return domestic: ESB → SAW ──
        flights.add(flight(id++, "ESB", "SAW", "TK", "2121", "07:30", "08:45", 75, 6500, "321", allDays()));
        flights.add(flight(id++, "ESB", "SAW", "TK", "2123", "11:00", "12:15", 75, 7500, "321", allDays()));
        flights.add(flight(id++, "ESB", "SAW", "TK", "2125", "15:00", "16:15", 75, 7000, "320", allDays()));
        flights.add(flight(id++, "ESB", "SAW", "TK", "2127", "19:00", "20:15", 75, 6000, "321", weekdays()));

        // ── Return domestic: ADB → SAW ──
        flights.add(flight(id++, "ADB", "SAW", "PC", "2231", "08:00", "09:10", 70, 5500, "320", allDays()));
        flights.add(flight(id++, "ADB", "SAW", "PC", "2233", "12:00", "13:10", 70, 6500, "320", allDays()));
        flights.add(flight(id++, "ADB", "SAW", "PC", "2235", "17:00", "18:10", 70, 5000, "320", weekdays()));

        log.info("Built {} mock Amadeus flight offers", flights.size());
        return flights;
    }

    // ═══════════════════════════════════════════════════════════
    //  HELPER BUILDERS
    // ═══════════════════════════════════════════════════════════

    private AmadeusFlightOffer flight(int id, String from, String to,
                                      String carrier, String number,
                                      String depTime, String arrTime,
                                      int durationMin, int costCents,
                                      String aircraftCode,
                                      List<Integer> operatingDays) {
        Segment segment = new Segment(
                new Endpoint(from, null, LocalTime.parse(depTime)),
                new Endpoint(to, null, LocalTime.parse(arrTime)),
                carrier,
                number,
                new Aircraft(aircraftCode),
                "PT" + (durationMin / 60) + "H" + (durationMin % 60) + "M",
                0,
                operatingDays
        );

        Itinerary itinerary = new Itinerary(
                segment.duration(),
                List.of(segment)
        );

        String cabin = costCents > 20000 ? "BUSINESS" : "ECONOMY";

        return new AmadeusFlightOffer(
                String.valueOf(id),
                "MOCK",
                List.of(itinerary),
                new Price("EUR", costCents),
                List.of(new TravelerPricing("1",
                        List.of(new FareDetail("1", cabin, "YOWTR", "ECOFLY", "Y"))))
        );
    }

    private List<Integer> allDays() {
        return List.of(1, 2, 3, 4, 5, 6, 7); // Mon-Sun
    }

    private List<Integer> weekdays() {
        return List.of(1, 2, 3, 4, 5); // Mon-Fri
    }
}
