package com.example.courier.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record LocationRequest(
        @NotBlank(message = "courierId must not be blank") String courierId,
        @NotNull(message = "timestamp must not be null") Instant timestamp,
        @NotNull(message = "latitude must not be null")
                @DecimalMin(value = "-90.0", message = "latitude must be between -90 and 90")
                @DecimalMax(value = "90.0", message = "latitude must be between -90 and 90")
                Double latitude,
        @NotNull(message = "longitude must not be null")
                @DecimalMin(value = "-180.0", message = "longitude must be between -180 and 180")
                @DecimalMax(value = "180.0", message = "longitude must be between -180 and 180")
                Double longitude) {}
