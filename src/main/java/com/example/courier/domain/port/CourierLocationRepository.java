package com.example.courier.domain.port;

import com.example.courier.domain.model.Location;
import java.time.Instant;
import java.util.Optional;

public interface CourierLocationRepository {

    Optional<Stored> findByCourierIdAndRecordedAt(String courierId, Instant recordedAt);

    void append(Location location, Instant receivedAt, boolean applied);

    record Stored(Location location, Instant receivedAt, boolean applied) {}
}
