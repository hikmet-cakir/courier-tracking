package com.example.courier.api;

import com.example.courier.api.dto.StoreResponse;
import com.example.courier.domain.port.StoreCatalog;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class StoreController {

    private final StoreCatalog storeCatalog;

    public StoreController(StoreCatalog storeCatalog) {
        this.storeCatalog = storeCatalog;
    }

    @GetMapping("/stores")
    public List<StoreResponse> list() {
        return storeCatalog.findAll().stream().map(StoreResponse::from).toList();
    }
}
