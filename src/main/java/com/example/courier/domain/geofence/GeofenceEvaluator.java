package com.example.courier.domain.geofence;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class GeofenceEvaluator {

    public static final double RADIUS_METERS = 100.0;
    public static final Duration REENTRY_COOLDOWN = Duration.ofSeconds(60);

    public GeofenceDecision evaluate(GeofenceState state, double distanceMeters, Instant recordedAt) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(recordedAt, "recordedAt");
        boolean nowInside = distanceMeters <= RADIUS_METERS;
        return state.transition(nowInside, recordedAt);
    }
}
