package com.example.courier.infrastructure.persistence.entity;

import java.io.Serializable;
import java.util.Objects;

public final class GeofenceStateId implements Serializable {

    private String courierId;
    private String storeId;

    public GeofenceStateId() {}

    public GeofenceStateId(String courierId, String storeId) {
        this.courierId = courierId;
        this.storeId = storeId;
    }

    public String getCourierId() {
        return courierId;
    }

    public void setCourierId(String courierId) {
        this.courierId = courierId;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GeofenceStateId that)) {
            return false;
        }
        return Objects.equals(courierId, that.courierId) && Objects.equals(storeId, that.storeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(courierId, storeId);
    }
}
