package com.example.courier.application;

import com.example.courier.domain.geo.DistanceCalculator;
import com.example.courier.domain.geofence.GeofenceDecision;
import com.example.courier.domain.geofence.GeofenceEvaluator;
import com.example.courier.domain.geofence.GeofenceState;
import com.example.courier.domain.model.CourierTrack;
import com.example.courier.domain.model.EntranceEvent;
import com.example.courier.domain.model.Location;
import com.example.courier.domain.model.Store;
import com.example.courier.domain.port.CourierLocationRepository;
import com.example.courier.domain.port.CourierTrackRepository;
import com.example.courier.domain.port.EntranceEventRepository;
import com.example.courier.domain.port.GeofenceStateRepository;
import com.example.courier.domain.port.StoreCatalog;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocationIngestionService {

    private final CourierTrackRepository courierTracks;
    private final CourierLocationRepository courierLocations;
    private final GeofenceStateRepository geofenceStates;
    private final EntranceEventRepository entranceEvents;
    private final StoreCatalog storeCatalog;
    private final DistanceCalculator distances;
    private final GeofenceEvaluator geofenceEvaluator;

    public LocationIngestionService(
            CourierTrackRepository courierTracks,
            CourierLocationRepository courierLocations,
            GeofenceStateRepository geofenceStates,
            EntranceEventRepository entranceEvents,
            StoreCatalog storeCatalog,
            DistanceCalculator distances,
            GeofenceEvaluator geofenceEvaluator) {
        this.courierTracks = courierTracks;
        this.courierLocations = courierLocations;
        this.geofenceStates = geofenceStates;
        this.entranceEvents = entranceEvents;
        this.storeCatalog = storeCatalog;
        this.distances = distances;
        this.geofenceEvaluator = geofenceEvaluator;
    }

    @Transactional
    public LocationIngestionResult ingest(Location location) {
        Objects.requireNonNull(location, "location");
        Instant receivedAt = Instant.now();
        CourierTrack track = courierTracks.lockOrCreate(location.courierId());

        var existing = courierLocations.findByCourierIdAndRecordedAt(location.courierId(), location.recordedAt());
        if (existing.isPresent()) {
            return duplicateOrConflict(location, track, existing.get());
        }

        if (track.hasAppliedLocation() && !location.recordedAt().isAfter(track.lastRecordedAt())) {
            courierLocations.append(location, receivedAt, false);
            return new LocationIngestionResult(false, false, track.totalDistanceMeters(), List.of());
        }

        courierLocations.append(location, receivedAt, true);
        CourierTrack updated = track.apply(location, distances);
        courierTracks.save(updated);

        List<EntranceEvent> created = new ArrayList<>();
        for (Store store : storeCatalog.findAll()) {
            double distanceToStore = distances.metersBetween(
                    location.latitude(), location.longitude(), store.latitude(), store.longitude());
            GeofenceState current = geofenceStates.find(location.courierId(), store.id());
            GeofenceDecision decision =
                    geofenceEvaluator.evaluate(current, distanceToStore, location.recordedAt());
            if (!decision.state().equals(current)) {
                geofenceStates.save(location.courierId(), store.id(), decision.state());
            }
            if (decision.entranceEmitted()) {
                EntranceEvent event =
                        new EntranceEvent(location.courierId(), store.id(), location.recordedAt());
                entranceEvents.save(event);
                created.add(event);
            }
        }
        return new LocationIngestionResult(false, true, updated.totalDistanceMeters(), created);
    }

    private LocationIngestionResult duplicateOrConflict(
            Location location, CourierTrack track, CourierLocationRepository.Stored stored) {
        if (!sameCoordinates(stored.location(), location)) {
            throw new LocationConflictException(location.courierId(), location.recordedAt());
        }
        List<EntranceEvent> events = stored.applied() ? eventsAt(location) : List.of();
        return new LocationIngestionResult(true, stored.applied(), track.totalDistanceMeters(), events);
    }

    private List<EntranceEvent> eventsAt(Location location) {
        return entranceEvents.find(location.courierId(), null).stream()
                .filter(event -> event.occurredAt().equals(location.recordedAt()))
                .toList();
    }

    private static boolean sameCoordinates(Location left, Location right) {
        return Double.compare(left.latitude(), right.latitude()) == 0
                && Double.compare(left.longitude(), right.longitude()) == 0;
    }
}
