package com.example.courier.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.courier.AbstractPostgresIT;
import com.example.courier.domain.model.Location;
import com.example.courier.domain.port.CourierLocationRepository;
import com.example.courier.domain.port.CourierTrackRepository;
import com.example.courier.domain.port.EntranceEventRepository;
import com.example.courier.domain.port.GeofenceStateRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class LocationIngestionIT extends AbstractPostgresIT {

    private static final Instant T0 = Instant.parse("2024-06-01T10:00:00Z");
    private static final Instant T1 = Instant.parse("2024-06-01T10:01:00Z");
    private static final String ATASEHIR = "Ataşehir MMM Migros";
    private static final double ATASEHIR_LAT = 40.9923307;
    private static final double ATASEHIR_LNG = 29.1244229;

    @Autowired
    private LocationIngestionService ingestion;

    @Autowired
    private CourierLocationRepository courierLocations;

    @Autowired
    private CourierTrackRepository courierTracks;

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
    void duplicateWithSameCoordinatesIsIdempotent() {
        Location location = new Location("c1", T0, ATASEHIR_LAT, ATASEHIR_LNG);
        LocationIngestionResult original = ingestion.ingest(location);
        LocationIngestionResult retry = ingestion.ingest(location);

        assertThat(original.duplicate()).isFalse();
        assertThat(retry.duplicate()).isTrue();
        assertThat(retry.applied()).isTrue();
        assertThat(retry.totalDistanceMeters()).isEqualTo(original.totalDistanceMeters());
        assertThat(retry.entranceEvents()).isEqualTo(original.entranceEvents());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM courier_location", Long.class)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM entrance_event", Long.class)).isEqualTo(1L);
    }

    @Test
    void duplicateWithDifferentCoordinatesConflictsAndLeavesStateUnchanged() {
        ingestion.ingest(new Location("c1", T0, ATASEHIR_LAT, ATASEHIR_LNG));

        assertThatThrownBy(() -> ingestion.ingest(new Location("c1", T0, 41.0, 29.0)))
                .isInstanceOf(LocationConflictException.class);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM courier_location", Long.class)).isEqualTo(1L);
        assertThat(courierLocations.findByCourierIdAndRecordedAt("c1", T0).orElseThrow().location().latitude())
                .isEqualTo(ATASEHIR_LAT);
        assertThat(entranceEvents.find("c1", ATASEHIR)).hasSize(1);
    }

    @Test
    void outOfOrderLocationIsStoredButNotApplied() {
        LocationIngestionResult first = ingestion.ingest(new Location("c1", T1, ATASEHIR_LAT, ATASEHIR_LNG));
        LocationIngestionResult late = ingestion.ingest(new Location("c1", T0, 41.0, 29.0));

        assertThat(first.applied()).isTrue();
        assertThat(late.duplicate()).isFalse();
        assertThat(late.applied()).isFalse();
        assertThat(late.totalDistanceMeters()).isEqualTo(first.totalDistanceMeters());
        assertThat(late.entranceEvents()).isEmpty();
        assertThat(courierLocations.findByCourierIdAndRecordedAt("c1", T0).orElseThrow().applied()).isFalse();
        assertThat(courierTracks.find("c1").orElseThrow().lastRecordedAt()).isEqualTo(T1);
        assertThat(geofenceStates.find("c1", ATASEHIR).inside()).isTrue();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM courier_location WHERE applied = TRUE", Long.class))
                .isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM entrance_event", Long.class)).isEqualTo(1L);
    }
}
