package com.example.courier.domain.port;

import com.example.courier.domain.geofence.GeofenceState;

public interface GeofenceStateRepository {

    GeofenceState find(String courierId, String storeId);

    void save(String courierId, String storeId, GeofenceState state);
}
