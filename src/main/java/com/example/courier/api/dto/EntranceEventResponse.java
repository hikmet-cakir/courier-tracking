package com.example.courier.api.dto;

import com.example.courier.domain.model.EntranceEvent;
import java.time.Instant;

public record EntranceEventResponse(String courierId, String storeId, Instant occurredAt) {

    public static EntranceEventResponse from(EntranceEvent event) {
        return new EntranceEventResponse(event.courierId(), event.storeId(), event.occurredAt());
    }
}
