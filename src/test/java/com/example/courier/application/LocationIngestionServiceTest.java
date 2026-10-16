package com.example.courier.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.courier.domain.geo.HaversineDistanceCalculator;
import com.example.courier.domain.geofence.GeofenceEvaluator;
import com.example.courier.domain.model.Location;
import com.example.courier.domain.model.Store;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LocationIngestionServiceTest {

    private static final Instant T0 = Instant.parse("2024-01-01T00:00:00Z");
    private static final Store STORE_A = new Store("store-a", "Store A", 41.0, 29.0);
    private static final Store STORE_B = new Store("store-b", "Store B", 41.5, 29.5);

    private InMemoryIngestionFakes fakes;
    private LocationIngestionService service;

    @BeforeEach
    void setUp() {
        fakes = new InMemoryIngestionFakes();
        service = new LocationIngestionService(
                fakes.tracks,
                fakes.locations,
                fakes.geofences,
                fakes.events,
                new InMemoryIngestionFakes.Catalog(STORE_A, STORE_B),
                new HaversineDistanceCalculator(),
                new GeofenceEvaluator());
    }

    @Test
    void firstLocationInsideStoreIsAppliedAndEmitsEntrance() {
        LocationIngestionResult result = service.ingest(new Location("c1", T0, 41.0, 29.0));

        assertThat(result.duplicate()).isFalse();
        assertThat(result.applied()).isTrue();
        assertThat(result.totalDistanceMeters()).isZero();
        assertThat(result.entranceEvents()).hasSize(1);
        assertThat(result.entranceEvents().getFirst().storeId()).isEqualTo("store-a");
        assertThat(fakes.geofences.find("c1", "store-a").inside()).isTrue();
        assertThat(fakes.geofences.find("c1", "store-b").inside()).isFalse();
    }

    @Test
    void consecutiveAppliedLocationsAccumulateSegmentDistances() {
        service.ingest(new Location("c1", T0, 41.0, 29.0));
        LocationIngestionResult second = service.ingest(new Location("c1", T0.plusSeconds(1), 41.002, 29.0));

        assertThat(second.applied()).isTrue();
        assertThat(second.totalDistanceMeters()).isGreaterThan(0.0);
        assertThat(fakes.tracks.find("c1").orElseThrow().totalDistanceMeters())
                .isEqualTo(second.totalDistanceMeters());
    }

    @Test
    void stayingInsideDoesNotEmitAnotherEntrance() {
        service.ingest(new Location("c1", T0, 41.0, 29.0));
        LocationIngestionResult stay = service.ingest(new Location("c1", T0.plusSeconds(10), 41.0001, 29.0));

        assertThat(stay.entranceEvents()).isEmpty();
        assertThat(fakes.events.find("c1", "store-a")).hasSize(1);
    }

    @Test
    void outOfOrderLocationIsStoredButNotApplied() {
        service.ingest(new Location("c1", T0.plusSeconds(60), 41.0, 29.0));
        double totalAfterFirst = fakes.tracks.find("c1").orElseThrow().totalDistanceMeters();

        LocationIngestionResult late = service.ingest(new Location("c1", T0, 41.5, 29.5));

        assertThat(late.duplicate()).isFalse();
        assertThat(late.applied()).isFalse();
        assertThat(late.totalDistanceMeters()).isEqualTo(totalAfterFirst);
        assertThat(late.entranceEvents()).isEmpty();
        assertThat(fakes.locations.findByCourierIdAndRecordedAt("c1", T0).orElseThrow().applied())
                .isFalse();
        assertThat(fakes.tracks.find("c1").orElseThrow().lastRecordedAt()).isEqualTo(T0.plusSeconds(60));
        assertThat(fakes.geofences.find("c1", "store-a").inside()).isTrue();
        assertThat(fakes.geofences.find("c1", "store-b").inside()).isFalse();
        assertThat(fakes.events.find(null, null)).hasSize(1);
    }

    @Test
    void exactDuplicateIsIdempotentAndHasNoSideEffects() {
        Location location = new Location("c1", T0, 41.0, 29.0);
        LocationIngestionResult original = service.ingest(location);
        int appends = fakes.locations.appendCount;
        int eventCount = fakes.events.find(null, null).size();

        LocationIngestionResult retry = service.ingest(location);

        assertThat(retry.duplicate()).isTrue();
        assertThat(retry.applied()).isEqualTo(original.applied());
        assertThat(retry.totalDistanceMeters()).isEqualTo(original.totalDistanceMeters());
        assertThat(retry.entranceEvents()).isEqualTo(original.entranceEvents());
        assertThat(fakes.locations.appendCount).isEqualTo(appends);
        assertThat(fakes.events.find(null, null)).hasSize(eventCount);
    }

    @Test
    void sameTimestampWithDifferentCoordinatesConflicts() {
        service.ingest(new Location("c1", T0, 41.0, 29.0));

        assertThatThrownBy(() -> service.ingest(new Location("c1", T0, 41.1, 29.0)))
                .isInstanceOfSatisfying(
                        LocationConflictException.class, ex -> assertThat(ex.courierId()).isEqualTo("c1"));
        assertThat(fakes.locations.appendCount).isEqualTo(1);
    }

    @Test
    void reentryInsideCooldownDoesNotWriteEventOrAdvanceLastEntrance() {
        service.ingest(new Location("c1", T0, 41.0, 29.0));
        service.ingest(new Location("c1", T0.plusSeconds(10), 41.5, 29.5));
        LocationIngestionResult reentry = service.ingest(new Location("c1", T0.plusSeconds(30), 41.0, 29.0));

        assertThat(reentry.applied()).isTrue();
        assertThat(reentry.entranceEvents()).isEmpty();
        assertThat(fakes.events.find("c1", "store-a")).hasSize(1);
        assertThat(fakes.geofences.find("c1", "store-a").inside()).isTrue();
        assertThat(fakes.geofences.find("c1", "store-a").lastEntranceAt()).isEqualTo(T0);
    }
}
