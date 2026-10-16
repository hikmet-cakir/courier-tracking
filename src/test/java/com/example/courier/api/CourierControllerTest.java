package com.example.courier.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.courier.domain.model.CourierTrack;
import com.example.courier.domain.port.CourierTrackRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CourierController.class)
class CourierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourierTrackRepository courierTracks;

    @Test
    void getDistanceReturns200WithRoundedMeters() throws Exception {
        when(courierTracks.find("c1"))
                .thenReturn(Optional.of(new CourierTrack("c1", 41.0, 29.0, java.time.Instant.parse("2024-01-01T00:00:00Z"), 10.004)));

        mockMvc.perform(get("/api/v1/couriers/c1/distance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courierId").value("c1"))
                .andExpect(jsonPath("$.totalDistanceMeters").value(10.00));
    }

    @Test
    void getDistanceReturns404ForUnknownCourier() throws Exception {
        when(courierTracks.find("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/couriers/missing/distance"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Unknown courier: missing"))
                .andExpect(jsonPath("$.courierId").value("missing"));
    }
}
