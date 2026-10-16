package com.example.courier.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.courier.domain.model.EntranceEvent;
import com.example.courier.domain.port.EntranceEventRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EntranceEventController.class)
class EntranceEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EntranceEventRepository entranceEvents;

    @Test
    void listReturnsEventsInRepositoryOrder() throws Exception {
        Instant t0 = Instant.parse("2024-01-01T00:00:00Z");
        Instant t1 = Instant.parse("2024-01-01T00:01:00Z");
        when(entranceEvents.find(null, null))
                .thenReturn(List.of(
                        new EntranceEvent("c1", "store-a", t0), new EntranceEvent("c2", "store-b", t1)));

        mockMvc.perform(get("/api/v1/entrance-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courierId").value("c1"))
                .andExpect(jsonPath("$[0].occurredAt").value("2024-01-01T00:00:00Z"))
                .andExpect(jsonPath("$[1].courierId").value("c2"));
    }

    @Test
    void listPassesOptionalFilters() throws Exception {
        when(entranceEvents.find("c1", "store-a")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/entrance-events").param("courierId", "c1").param("storeId", "store-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(entranceEvents).find("c1", "store-a");
    }
}
