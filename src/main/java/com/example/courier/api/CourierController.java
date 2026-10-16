package com.example.courier.api;

import com.example.courier.api.dto.DistanceResponse;
import com.example.courier.domain.port.CourierTrackRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CourierController {

    private final CourierTrackRepository courierTracks;

    public CourierController(CourierTrackRepository courierTracks) {
        this.courierTracks = courierTracks;
    }

    @GetMapping("/couriers/{courierId}/distance")
    public DistanceResponse distance(@PathVariable String courierId) {
        return courierTracks
                .find(courierId)
                .map(DistanceResponse::from)
                .orElseThrow(() -> new CourierNotFoundException(courierId));
    }
}
