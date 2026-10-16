package com.example.courier.api;

import com.example.courier.api.dto.LocationRequest;
import com.example.courier.api.dto.LocationResponse;
import com.example.courier.application.LocationIngestionResult;
import com.example.courier.application.LocationIngestionService;
import com.example.courier.domain.model.Location;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class LocationController {

    private final LocationIngestionService ingestion;

    public LocationController(LocationIngestionService ingestion) {
        this.ingestion = ingestion;
    }

    @PostMapping("/locations")
    public ResponseEntity<LocationResponse> ingest(@Valid @RequestBody LocationRequest request) {
        LocationIngestionResult result = ingestion.ingest(new Location(
                request.courierId(), request.timestamp(), request.latitude(), request.longitude()));
        HttpStatus status = result.duplicate() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(LocationResponse.from(result));
    }
}
