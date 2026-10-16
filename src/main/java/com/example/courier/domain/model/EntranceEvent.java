package com.example.courier.domain.model;

import java.time.Instant;
import java.util.Objects;

public record EntranceEvent(String courierId, String storeId, Instant occurredAt) {

    public EntranceEvent {
        Objects.requireNonNull(courierId, "courierId");
        Objects.requireNonNull(storeId, "storeId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        if (courierId.isBlank()) {
            throw new IllegalArgumentException("courierId must not be blank");
        }
        if (storeId.isBlank()) {
            throw new IllegalArgumentException("storeId must not be blank");
        }
    }
}
