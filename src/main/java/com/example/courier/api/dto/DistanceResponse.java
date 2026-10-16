package com.example.courier.api.dto;

import com.example.courier.api.Distances;
import com.example.courier.domain.model.CourierTrack;
import java.math.BigDecimal;

public record DistanceResponse(String courierId, BigDecimal totalDistanceMeters) {

    public static DistanceResponse from(CourierTrack track) {
        return new DistanceResponse(track.courierId(), Distances.roundMeters(track.totalDistanceMeters()));
    }
}
