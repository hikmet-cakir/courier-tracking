package com.example.courier.domain.port;

import com.example.courier.domain.model.CourierTrack;
import java.util.Optional;

public interface CourierTrackRepository {

    Optional<CourierTrack> find(String courierId);

    CourierTrack lockOrCreate(String courierId);

    void save(CourierTrack track);
}
