package com.example.courier.domain.model;

import java.time.Instant;
import java.util.Objects;

public record Location(String courierId, Instant recordedAt, double latitude, double longitude) {

    public Location {
        Objects.requireNonNull(courierId, "courierId");
        Objects.requireNonNull(recordedAt, "recordedAt");
        if (courierId.isBlank()) {
            throw new IllegalArgumentException("courierId must not be blank");
        }
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("latitude out of range: " + latitude);
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("longitude out of range: " + longitude);
        }
    }
}
