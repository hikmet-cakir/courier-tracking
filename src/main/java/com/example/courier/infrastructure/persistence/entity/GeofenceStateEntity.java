package com.example.courier.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "geofence_state")
@IdClass(GeofenceStateId.class)
public class GeofenceStateEntity {

    @Id
    @Column(name = "courier_id")
    private String courierId;

    @Id
    @Column(name = "store_id")
    private String storeId;

    @Column(nullable = false)
    private boolean inside;

    @Column(name = "last_entrance_at")
    private Instant lastEntranceAt;

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

    public boolean isInside() {
        return inside;
    }

    public void setInside(boolean inside) {
        this.inside = inside;
    }

    public Instant getLastEntranceAt() {
        return lastEntranceAt;
    }

    public void setLastEntranceAt(Instant lastEntranceAt) {
        this.lastEntranceAt = lastEntranceAt;
    }
}
