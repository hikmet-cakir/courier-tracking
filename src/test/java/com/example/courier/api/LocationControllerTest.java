package com.example.courier.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.courier.application.LocationConflictException;
import com.example.courier.application.LocationIngestionResult;
import com.example.courier.application.LocationIngestionService;
import com.example.courier.domain.model.EntranceEvent;
import com.example.courier.domain.model.Location;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LocationController.class)
class LocationControllerTest {

    private static final Instant T0 = Instant.parse("2024-01-01T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LocationIngestionService ingestion;

    @Test
    void postLocationReturns201WhenCreated() throws Exception {
        when(ingestion.ingest(any(Location.class)))
                .thenReturn(new LocationIngestionResult(
                        false,
                        true,
                        12.345,
                        List.of(new EntranceEvent("c1", "Ataşehir MMM Migros", T0))));

        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"courierId":"c1","timestamp":"2024-01-01T00:00:00Z","latitude":40.99,"longitude":29.12}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applied").value(true))
                .andExpect(jsonPath("$.totalDistanceMeters").value(12.35))
                .andExpect(jsonPath("$.entranceEvents[0].storeId").value("Ataşehir MMM Migros"))
                .andExpect(jsonPath("$.entranceEvents[0].occurredAt").value("2024-01-01T00:00:00Z"));
    }

    @Test
    void postLocationReturns200OnExactDuplicate() throws Exception {
        when(ingestion.ingest(any(Location.class)))
                .thenReturn(new LocationIngestionResult(true, false, 0.0, List.of()));

        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"courierId":"c1","timestamp":"2024-01-01T00:00:00Z","latitude":40.99,"longitude":29.12}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applied").value(false))
                .andExpect(jsonPath("$.totalDistanceMeters").value(0.00))
                .andExpect(jsonPath("$.entranceEvents").isEmpty());
    }

    @Test
    void postLocationReturns400WhenCourierIdBlank() throws Exception {
        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"courierId":" ","timestamp":"2024-01-01T00:00:00Z","latitude":40.99,"longitude":29.12}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.errors[0].field").value("courierId"))
                .andExpect(jsonPath("$.errors[0].message").value("courierId must not be blank"));
    }

    @Test
    void postLocationReturns400WhenLatitudeOutOfRange() throws Exception {
        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"courierId":"c1","timestamp":"2024-01-01T00:00:00Z","latitude":90.1,"longitude":29.12}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("latitude"))
                .andExpect(jsonPath("$.errors[0].message").value("latitude must be between -90 and 90"));
    }

    @Test
    void postLocationReturns400WhenLongitudeOutOfRange() throws Exception {
        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"courierId":"c1","timestamp":"2024-01-01T00:00:00Z","latitude":40.99,"longitude":-180.1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("longitude"))
                .andExpect(jsonPath("$.errors[0].message").value("longitude must be between -180 and 180"));
    }

    @Test
    void postLocationReturns400WhenTimestampMissing() throws Exception {
        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courierId":"c1","latitude":40.99,"longitude":29.12}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("timestamp"))
                .andExpect(jsonPath("$.errors[0].message").value("timestamp must not be null"));
    }

    @Test
    void postLocationReturns400ForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Malformed JSON request"));
    }

    @Test
    void postLocationReturns400ForInvalidTimestamp() throws Exception {
        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"courierId":"c1","timestamp":"yesterday","latitude":40.99,"longitude":29.12}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("timestamp must be a valid ISO-8601 instant"));
    }

    @Test
    void postLocationReturns409OnCoordinateConflict() throws Exception {
        when(ingestion.ingest(any(Location.class))).thenThrow(new LocationConflictException("c1", T0));

        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"courierId":"c1","timestamp":"2024-01-01T00:00:00Z","latitude":41.0,"longitude":29.0}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.courierId").value("c1"))
                .andExpect(jsonPath("$.timestamp").value("2024-01-01T00:00:00Z"));
    }
}
