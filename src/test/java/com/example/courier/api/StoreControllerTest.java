package com.example.courier.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.courier.domain.model.Store;
import com.example.courier.domain.port.StoreCatalog;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StoreController.class)
class StoreControllerTest {

    private static final String ATASEHIR_ID =
            UUID.nameUUIDFromBytes("Ataşehir MMM Migros".getBytes(StandardCharsets.UTF_8)).toString();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StoreCatalog storeCatalog;

    @Test
    void listReturnsLoadedStores() throws Exception {
        when(storeCatalog.findAll())
                .thenReturn(List.of(new Store(ATASEHIR_ID, "Ataşehir MMM Migros", 40.9923307, 29.1244229)));

        mockMvc.perform(get("/api/v1/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ATASEHIR_ID))
                .andExpect(jsonPath("$[0].name").value("Ataşehir MMM Migros"))
                .andExpect(jsonPath("$[0].latitude").value(40.9923307))
                .andExpect(jsonPath("$[0].longitude").value(29.1244229));
    }
}
