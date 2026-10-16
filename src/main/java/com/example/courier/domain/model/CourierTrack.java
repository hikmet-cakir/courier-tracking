package com.example.courier.domain.model;

import com.example.courier.domain.geo.DistanceCalculator;
import java.time.Instant;
import java.util.Objects;

public record CourierTrack(
        String courierId,
        Double lastLatitude,
        Double lastLongitude,
        Instant lastRecordedAt,
        double totalDistanceMeters) {

    public CourierTrack {
        Objects.requireNonNull(courierId, "courierId");
        if (courierId.isBlank()) {
            throw new IllegalArgumentException("courierId must not be blank");
        }
        if (totalDistanceMeters < 0.0) {
            throw new IllegalArgumentException("totalDistanceMeters must not be negative");
        }
        if (lastRecordedAt == null) {
            if (lastLatitude != null || lastLongitude != null) {
                throw new IllegalArgumentException("last coordinates require lastRecordedAt");
            }
        } else if (lastLatitude == null || lastLongitude == null) {
            throw new IllegalArgumentException("lastRecordedAt requires last coordinates");
        }
    }

    public static CourierTrack empty(String courierId) {
        return new CourierTrack(courierId, null, null, null, 0.0);
    }

    public boolean hasAppliedLocation() {
        return lastRecordedAt != null;
    }

    public CourierTrack apply(Location location, DistanceCalculator distances) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(distances, "distances");
        if (!courierId.equals(location.courierId())) {
            throw new IllegalArgumentException("location courierId does not match track");
        }
        double nextTotal = totalDistanceMeters;
        if (hasAppliedLocation()) {
            nextTotal += distances.metersBetween(
                    lastLatitude, lastLongitude, location.latitude(), location.longitude());
        }
        return new CourierTrack(
                courierId, location.latitude(), location.longitude(), location.recordedAt(), nextTotal);
    }
}
