package com.example.courier.api.dto;

import com.example.courier.domain.model.Store;

public record StoreResponse(String id, String name, double latitude, double longitude) {

    public static StoreResponse from(Store store) {
        return new StoreResponse(store.id(), store.name(), store.latitude(), store.longitude());
    }
}
