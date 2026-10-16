package com.example.courier.application;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class InMemoryIngestionFakes {

    final InMemoryCourierTrackRepository tracks = new InMemoryCourierTrackRepository();
    final InMemoryCourierLocationRepository locations = new InMemoryCourierLocationRepository();
    final InMemoryGeofenceStateRepository geofences = new InMemoryGeofenceStateRepository();
    final InMemoryEntranceEventRepository events = new InMemoryEntranceEventRepository();

    static final class Catalog implements StoreCatalog {
        private final List<Store> stores;

        Catalog(Store... stores) {
            this.stores = List.of(stores);
        }

        @Override
        public List<Store> findAll() {
            return stores;
        }
    }

    static final class InMemoryCourierTrackRepository implements CourierTrackRepository {
        private final Map<String, CourierTrack> byId = new HashMap<>();

        @Override
        public Optional<CourierTrack> find(String courierId) {
            return Optional.ofNullable(byId.get(courierId));
        }

        @Override
        public CourierTrack lockOrCreate(String courierId) {
            return byId.computeIfAbsent(courierId, CourierTrack::empty);
        }

        @Override
        public void save(CourierTrack track) {
            byId.put(track.courierId(), track);
        }
    }

    static final class InMemoryCourierLocationRepository implements CourierLocationRepository {
        private final Map<String, Stored> byKey = new HashMap<>();
        int appendCount;

        @Override
        public Optional<Stored> findByCourierIdAndRecordedAt(String courierId, Instant recordedAt) {
            return Optional.ofNullable(byKey.get(key(courierId, recordedAt)));
        }

        @Override
        public void append(Location location, Instant receivedAt, boolean applied) {
            appendCount++;
            byKey.put(key(location.courierId(), location.recordedAt()), new Stored(location, receivedAt, applied));
        }

        private static String key(String courierId, Instant recordedAt) {
            return courierId + "|" + recordedAt;
        }
    }

    static final class InMemoryGeofenceStateRepository implements GeofenceStateRepository {
        private final Map<String, GeofenceState> byKey = new HashMap<>();

        @Override
        public GeofenceState find(String courierId, String storeId) {
            return byKey.getOrDefault(key(courierId, storeId), GeofenceState.initial());
        }

        @Override
        public void save(String courierId, String storeId, GeofenceState state) {
            byKey.put(key(courierId, storeId), state);
        }

        private static String key(String courierId, String storeId) {
            return courierId + "|" + storeId;
        }
    }

    static final class InMemoryEntranceEventRepository implements EntranceEventRepository {
        private final List<EntranceEvent> events = new ArrayList<>();

        @Override
        public void save(EntranceEvent event) {
            events.add(event);
        }

        @Override
        public List<EntranceEvent> find(String courierId, String storeId) {
            return events.stream()
                    .filter(event -> courierId == null || event.courierId().equals(courierId))
                    .filter(event -> storeId == null || event.storeId().equals(storeId))
                    .toList();
        }
    }
}
