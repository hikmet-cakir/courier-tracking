package com.example.courier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.example.courier.domain.model.Store;
import com.example.courier.domain.port.StoreCatalog;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ApplicationContextIT extends AbstractPostgresIT {

    @Autowired
    private StoreCatalog storeCatalog;

    @Test
    void contextLoadsWithFiveStoresFromJson() {
        assertThat(storeCatalog.findAll())
                .extracting(Store::id, Store::name)
                .containsExactly(
                        tuple(idFromName("Ataşehir MMM Migros"), "Ataşehir MMM Migros"),
                        tuple(idFromName("Novada MMM Migros"), "Novada MMM Migros"),
                        tuple(idFromName("Beylikdüzü 5M Migros"), "Beylikdüzü 5M Migros"),
                        tuple(idFromName("Ortaköy MMM Migros"), "Ortaköy MMM Migros"),
                        tuple(idFromName("Caddebostan MMM Migros"), "Caddebostan MMM Migros"));
    }

    private static String idFromName(String name) {
        return UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
