package com.example.courier.application;

import java.time.Instant;

public class LocationConflictException extends RuntimeException {

    private final String courierId;
    private final Instant recordedAt;

    public LocationConflictException(String courierId, Instant recordedAt) {
        super("A different location already exists for courier '%s' at %s".formatted(courierId, recordedAt));
        this.courierId = courierId;
        this.recordedAt = recordedAt;
    }

    public String courierId() {
        return courierId;
    }

    public Instant recordedAt() {
        return recordedAt;
    }
}
