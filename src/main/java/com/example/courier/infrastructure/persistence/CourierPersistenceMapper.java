package com.example.courier.infrastructure.persistence;

import com.example.courier.domain.geofence.GeofenceState;
import com.example.courier.domain.model.CourierTrack;
import com.example.courier.domain.model.EntranceEvent;
import com.example.courier.domain.model.Location;
import com.example.courier.infrastructure.persistence.entity.CourierLocationEntity;
import com.example.courier.infrastructure.persistence.entity.CourierTrackEntity;
import com.example.courier.infrastructure.persistence.entity.EntranceEventEntity;
import com.example.courier.infrastructure.persistence.entity.GeofenceStateEntity;
import java.time.Instant;

public final class CourierPersistenceMapper {

    private CourierPersistenceMapper() {}

    public static CourierTrack toDomain(CourierTrackEntity entity) {
        return new CourierTrack(
                entity.getCourierId(),
                entity.getLastLatitude(),
                entity.getLastLongitude(),
                entity.getLastRecordedAt(),
                entity.getTotalDistanceMeters());
    }

    public static CourierTrackEntity toEntity(CourierTrack track) {
        CourierTrackEntity entity = new CourierTrackEntity();
        entity.setCourierId(track.courierId());
        entity.setLastLatitude(track.lastLatitude());
        entity.setLastLongitude(track.lastLongitude());
        entity.setLastRecordedAt(track.lastRecordedAt());
        entity.setTotalDistanceMeters(track.totalDistanceMeters());
        return entity;
    }

    public static Location toLocation(CourierLocationEntity entity) {
        return new Location(
                entity.getCourierId(), entity.getRecordedAt(), entity.getLatitude(), entity.getLongitude());
    }

    public static CourierLocationEntity toEntity(Location location, Instant receivedAt, boolean applied) {
        CourierLocationEntity entity = new CourierLocationEntity();
        entity.setCourierId(location.courierId());
        entity.setRecordedAt(location.recordedAt());
        entity.setLatitude(location.latitude());
        entity.setLongitude(location.longitude());
        entity.setReceivedAt(receivedAt);
        entity.setApplied(applied);
        return entity;
    }

    public static GeofenceState toDomain(GeofenceStateEntity entity) {
        if (entity.isInside()) {
            return new GeofenceState.Inside(entity.getLastEntranceAt());
        }
        return new GeofenceState.Outside(entity.getLastEntranceAt());
    }

    public static GeofenceStateEntity toEntity(String courierId, String storeId, GeofenceState state) {
        GeofenceStateEntity entity = new GeofenceStateEntity();
        entity.setCourierId(courierId);
        entity.setStoreId(storeId);
        entity.setInside(state.inside());
        entity.setLastEntranceAt(state.lastEntranceAt());
        return entity;
    }

    public static EntranceEvent toDomain(EntranceEventEntity entity) {
        return new EntranceEvent(entity.getCourierId(), entity.getStoreId(), entity.getOccurredAt());
    }

    public static EntranceEventEntity toEntity(EntranceEvent event) {
        EntranceEventEntity entity = new EntranceEventEntity();
        entity.setCourierId(event.courierId());
        entity.setStoreId(event.storeId());
        entity.setOccurredAt(event.occurredAt());
        return entity;
    }
}
