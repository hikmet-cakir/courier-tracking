package com.example.courier.api.dto;

import com.example.courier.api.Distances;
import com.example.courier.application.LocationIngestionResult;
import java.math.BigDecimal;
import java.util.List;

public record LocationResponse(
        boolean applied, BigDecimal totalDistanceMeters, List<EntranceEventResponse> entranceEvents) {

    public static LocationResponse from(LocationIngestionResult result) {
        return new LocationResponse(
                result.applied(),
                Distances.roundMeters(result.totalDistanceMeters()),
                result.entranceEvents().stream().map(EntranceEventResponse::from).toList());
    }
}
