package com.example.courier.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import com.example.courier.AbstractPostgresIT;
import com.example.courier.domain.model.EntranceEvent;
import com.example.courier.domain.model.Location;
import com.example.courier.domain.port.EntranceEventRepository;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class LocationIngestionRollbackIT extends AbstractPostgresIT {

    private static final Instant T0 = Instant.parse("2024-06-01T12:00:00Z");

    @Autowired
    private LocationIngestionService ingestion;

    @MockitoSpyBean
    private EntranceEventRepository entranceEvents;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void truncate() {
        jdbc.execute("TRUNCATE TABLE courier_location, courier_track, geofence_state, entrance_event");
    }

    @AfterEach
    void resetSpy() {
        reset(entranceEvents);
    }

    @Test
    void midTransactionFailureRollsBackLocationTrackStateAndEvents() {
        doThrow(new IllegalStateException("forced failure")).when(entranceEvents).save(any(EntranceEvent.class));

        assertThatThrownBy(() -> ingestion.ingest(new Location("c1", T0, 40.9923307, 29.1244229)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("forced failure");

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM courier_location", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM courier_track", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM geofence_state", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM entrance_event", Long.class)).isZero();
    }
}
