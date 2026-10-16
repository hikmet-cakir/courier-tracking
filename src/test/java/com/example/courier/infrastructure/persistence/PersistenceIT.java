package com.example.courier.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.example.courier.AbstractPostgresIT;
import com.example.courier.domain.geo.HaversineDistanceCalculator;
import com.example.courier.domain.geofence.GeofenceState;
import com.example.courier.domain.model.CourierTrack;
import com.example.courier.domain.model.EntranceEvent;
import com.example.courier.domain.model.Location;
import com.example.courier.domain.port.CourierLocationRepository;
import com.example.courier.domain.port.CourierTrackRepository;
import com.example.courier.domain.port.EntranceEventRepository;
import com.example.courier.domain.port.GeofenceStateRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class PersistenceIT extends AbstractPostgresIT {

    private static final Instant T0 = Instant.parse("2024-06-01T10:00:00Z");
    private static final String STORE_A = "Ataşehir MMM Migros";
    private static final String STORE_B = "Novada MMM Migros";

    @Autowired
    private CourierTrackRepository courierTracks;

    @Autowired
    private CourierLocationRepository courierLocations;

    @Autowired
    private GeofenceStateRepository geofenceStates;

    @Autowired
    private EntranceEventRepository entranceEvents;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void truncate() {
        jdbc.execute("TRUNCATE TABLE courier_location, courier_track, geofence_state, entrance_event");
    }

    @Test
    void duplicateCourierLocationNaturalKeyIsRejected() {
        Location location = new Location("c1", T0, 41.0, 29.0);
        courierLocations.append(location, T0, true);

        assertThatThrownBy(() -> courierLocations.append(location, T0.plusSeconds(1), false))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicateEntranceEventNaturalKeyIsRejected() {
        EntranceEvent event = new EntranceEvent("c1", STORE_A, T0);
        entranceEvents.save(event);

        assertThatThrownBy(() -> entranceEvents.save(event)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void lockOrCreateInsertsOnceAndReturnsExistingRow() {
        CourierTrack created = courierTracks.lockOrCreate("c1");
        assertThat(created.courierId()).isEqualTo("c1");
        assertThat(created.hasAppliedLocation()).isFalse();
        assertThat(created.totalDistanceMeters()).isZero();

        CourierTrack updated = created.apply(new Location("c1", T0, 41.0, 29.0), new HaversineDistanceCalculator());
        courierTracks.save(updated);

        CourierTrack lockedAgain = courierTracks.lockOrCreate("c1");
        assertThat(lockedAgain.lastRecordedAt()).isEqualTo(T0);
        assertThat(lockedAgain.lastLatitude()).isEqualTo(41.0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM courier_track", Long.class)).isEqualTo(1L);
    }

    @Test
    void missingGeofenceRowMeansOutsideWithNoEntrance() {
        GeofenceState missing = geofenceStates.find("c1", STORE_A);
        assertThat(missing.inside()).isFalse();
        assertThat(missing.lastEntranceAt()).isNull();
        assertThat(missing).isInstanceOf(GeofenceState.Outside.class);
    }

    @Test
    void geofenceStateRoundTripPreservesInsideAndLastEntrance() {
        Instant enteredAt = T0.plusSeconds(15);
        geofenceStates.save("c1", STORE_A, new GeofenceState.Inside(enteredAt));
        geofenceStates.save("c1", STORE_B, new GeofenceState.Outside(T0));

        GeofenceState inside = geofenceStates.find("c1", STORE_A);
        GeofenceState outside = geofenceStates.find("c1", STORE_B);

        assertThat(inside).isInstanceOf(GeofenceState.Inside.class);
        assertThat(inside.lastEntranceAt()).isEqualTo(enteredAt);
        assertThat(outside).isInstanceOf(GeofenceState.Outside.class);
        assertThat(outside.lastEntranceAt()).isEqualTo(T0);
    }

    @Test
    void entranceEventRoundTripIsOrderedByOccurredAt() {
        entranceEvents.save(new EntranceEvent("c2", STORE_A, T0.plusSeconds(30)));
        entranceEvents.save(new EntranceEvent("c1", STORE_A, T0));
        entranceEvents.save(new EntranceEvent("c1", STORE_B, T0.plusSeconds(10)));

        assertThat(entranceEvents.find(null, null))
                .extracting(EntranceEvent::occurredAt)
                .containsExactly(T0, T0.plusSeconds(10), T0.plusSeconds(30));
        assertThat(entranceEvents.find("c1", STORE_A)).containsExactly(new EntranceEvent("c1", STORE_A, T0));
    }

    @Test
    void storedTotalDistanceEqualsSumOfAppliedLocationSegments() {
        HaversineDistanceCalculator haversine = new HaversineDistanceCalculator();
        Location first = new Location("c1", T0, 41.0, 29.0);
        Location second = new Location("c1", T0.plusSeconds(1), 41.002, 29.0);
        Location third = new Location("c1", T0.plusSeconds(2), 41.002, 29.003);
        Location notApplied = new Location("c1", T0.minusSeconds(5), 40.0, 28.0);

        CourierTrack track = courierTracks.lockOrCreate("c1");
        courierLocations.append(notApplied, T0, false);
        courierLocations.append(first, T0, true);
        track = track.apply(first, haversine);
        courierLocations.append(second, T0, true);
        track = track.apply(second, haversine);
        courierLocations.append(third, T0, true);
        track = track.apply(third, haversine);
        courierTracks.save(track);

        CourierTrack stored = courierTracks.find("c1").orElseThrow();
        List<double[]> applied = jdbc.query(
                """
                SELECT latitude, longitude
                FROM courier_location
                WHERE courier_id = ? AND applied = TRUE
                ORDER BY recorded_at
                """,
                (rs, rowNum) -> new double[] {rs.getDouble("latitude"), rs.getDouble("longitude")},
                "c1");

        double recomputed = 0.0;
        for (int i = 1; i < applied.size(); i++) {
            recomputed += haversine.metersBetween(
                    applied.get(i - 1)[0], applied.get(i - 1)[1], applied.get(i)[0], applied.get(i)[1]);
        }

        assertThat(applied).hasSize(3);
        assertThat(stored.totalDistanceMeters()).isCloseTo(recomputed, within(1e-9));
        assertThat(stored.totalDistanceMeters()).isGreaterThan(0.0);
    }

    @Test
    void locationRoundTripPreservesCoordinatesAndAppliedFlag() {
        Location location = new Location("c1", T0, 40.9923307, 29.1244229);
        courierLocations.append(location, T0.plusSeconds(3), true);

        CourierLocationRepository.Stored stored =
                courierLocations.findByCourierIdAndRecordedAt("c1", T0).orElseThrow();
        assertThat(stored.location()).isEqualTo(location);
        assertThat(stored.applied()).isTrue();
        assertThat(stored.receivedAt()).isEqualTo(T0.plusSeconds(3));
    }
}
