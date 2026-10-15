package com.example.courier;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.courier.domain.model.Store;
import com.example.courier.domain.port.StoreCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ApplicationContextIT extends AbstractPostgresIT {

    @Autowired
    private StoreCatalog storeCatalog;

    @Test
    void contextLoadsWithFiveStoresFromJson() {
        assertThat(storeCatalog.findAll())
                .extracting(Store::id)
                .containsExactly(
                        "Ataşehir MMM Migros",
                        "Novada MMM Migros",
                        "Beylikdüzü 5M Migros",
                        "Ortaköy MMM Migros",
                        "Caddebostan MMM Migros");
    }
}
