package com.example.courier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.example.courier.api.dto.LocationRequest;
import com.example.courier.domain.geo.HaversineDistanceCalculator;
import com.example.courier.domain.model.EntranceEvent;
import com.example.courier.domain.port.CourierTrackRepository;
import com.example.courier.domain.port.EntranceEventRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "spring.datasource.hikari.maximum-pool-size=40")
class ConcurrencyIT extends AbstractPostgresIT {

    private static final Instant T0 = Instant.parse("2024-07-01T08:00:00Z");
    private static final double ATASEHIR_LAT = 40.9923307;
    private static final double ATASEHIR_LNG = 29.1244229;
    private static final double FIVE_METERS_NORTH =
            Math.toDegrees(5.0 / HaversineDistanceCalculator.EARTH_RADIUS_METERS);
    private static final double HUNDRED_METERS_NORTH =
            Math.toDegrees(100.0 / HaversineDistanceCalculator.EARTH_RADIUS_METERS);

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CourierTrackRepository courierTracks;

    @Autowired
    private EntranceEventRepository entranceEvents;

    private final HaversineDistanceCalculator haversine = new HaversineDistanceCalculator();

    @BeforeEach
    void truncate() {
        jdbc.execute("TRUNCATE TABLE courier_location, courier_track, geofence_state, entrance_event");
    }

    @Test
    void concurrentPostsForSameCourierAreSerializedWithoutLostUpdates() throws Exception {
        int n = 20;
        String courierId = "same-courier";
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch start = new CountDownLatch(1);
        List<Throwable> failures = new CopyOnWriteArrayList<>();
        List<Future<?>> jobs = new ArrayList<>();

        try {
            for (int i = 0; i < n; i++) {
                int index = i;
                jobs.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    Instant timestamp = T0.plusSeconds(index);
                    double latitude = ATASEHIR_LAT + index * FIVE_METERS_NORTH;
                    postLocation(courierId, timestamp, latitude, ATASEHIR_LNG);
                    return null;
                }));
            }

            assertThat(ready.await(15, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<?> job : jobs) {
                job.get(45, TimeUnit.SECONDS);
            }
        } catch (Exception ex) {
            failures.add(ex);
            throw ex;
        } finally {
            pool.shutdownNow();
        }

        assertThat(failures).isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM courier_location WHERE courier_id = ?", Long.class, courierId))
                .isEqualTo((long) n);

        double expected = sumAppliedSegments(courierId);
        double stored = courierTracks.find(courierId).orElseThrow().totalDistanceMeters();
        assertThat(stored).isCloseTo(expected, within(1e-6));

        assertAtMostOneEntrancePerCooldown(courierId);
    }

    @Test
    void concurrentPostsForDifferentCouriersDoNotInterfere() throws Exception {
        int couriers = 10;
        ExecutorService pool = Executors.newFixedThreadPool(couriers);
        CountDownLatch ready = new CountDownLatch(couriers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> jobs = new ArrayList<>();

        try {
            for (int i = 0; i < couriers; i++) {
                int index = i;
                jobs.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    String courierId = "courier-" + index;
                    double originLat = 40.0 + index * 0.02;
                    postLocation(courierId, T0, originLat, 29.0);
                    postLocation(courierId, T0.plusSeconds(1), originLat + HUNDRED_METERS_NORTH, 29.0);
                    return null;
                }));
            }

            assertThat(ready.await(15, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<?> job : jobs) {
                job.get(45, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        for (int i = 0; i < couriers; i++) {
            String courierId = "courier-" + i;
            double expected = sumAppliedSegments(courierId);
            double stored = courierTracks.find(courierId).orElseThrow().totalDistanceMeters();
            assertThat(stored).isCloseTo(expected, within(1e-6));
            assertThat(stored).isCloseTo(100.0, within(0.5));
            assertThat(jdbc.queryForObject(
                            "SELECT COUNT(*) FROM courier_location WHERE courier_id = ? AND applied = TRUE",
                            Long.class,
                            courierId))
                    .isEqualTo(2L);
            assertAtMostOneEntrancePerCooldown(courierId);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM courier_track", Long.class)).isEqualTo((long) couriers);
    }

    private void postLocation(String courierId, Instant timestamp, double latitude, double longitude) {
        LocationRequest body = new LocationRequest(courierId, timestamp, latitude, longitude);
        ResponseEntity<String> response = rest.postForEntity("/api/v1/locations", body, String.class);
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("POST /locations for %s at %s", courierId, timestamp)
                .isTrue();
    }

    private double sumAppliedSegments(String courierId) {
        List<double[]> points = jdbc.query(
                """
                SELECT latitude, longitude
                FROM courier_location
                WHERE courier_id = ? AND applied = TRUE
                ORDER BY recorded_at
                """,
                (rs, rowNum) -> new double[] {rs.getDouble("latitude"), rs.getDouble("longitude")},
                courierId);
        double total = 0.0;
        for (int i = 1; i < points.size(); i++) {
            total += haversine.metersBetween(
                    points.get(i - 1)[0], points.get(i - 1)[1], points.get(i)[0], points.get(i)[1]);
        }
        return total;
    }

    private void assertAtMostOneEntrancePerCooldown(String courierId) {
        List<EntranceEvent> events = new ArrayList<>(entranceEvents.find(courierId, null));
        events.sort(Comparator.comparing(EntranceEvent::storeId).thenComparing(EntranceEvent::occurredAt));
        EntranceEvent previous = null;
        for (EntranceEvent event : events) {
            if (previous != null && previous.storeId().equals(event.storeId())) {
                assertThat(Duration.between(previous.occurredAt(), event.occurredAt()))
                        .isGreaterThanOrEqualTo(Duration.ofSeconds(60));
            }
            previous = event;
        }
    }
}
