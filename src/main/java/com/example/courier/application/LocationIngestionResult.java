package com.example.courier.application;

import com.example.courier.domain.model.EntranceEvent;
import java.util.List;

public record LocationIngestionResult(
        boolean duplicate,
        boolean applied,
        double totalDistanceMeters,
        List<EntranceEvent> entranceEvents) {

    public LocationIngestionResult {
        entranceEvents = List.copyOf(entranceEvents);
    }
}
