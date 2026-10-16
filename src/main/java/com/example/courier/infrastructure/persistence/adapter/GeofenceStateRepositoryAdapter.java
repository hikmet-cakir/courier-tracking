package com.example.courier.infrastructure.persistence.adapter;

import com.example.courier.domain.geofence.GeofenceState;
import com.example.courier.domain.port.GeofenceStateRepository;
import com.example.courier.infrastructure.persistence.CourierPersistenceMapper;
import com.example.courier.infrastructure.persistence.GeofenceStateJpaRepository;
import com.example.courier.infrastructure.persistence.entity.GeofenceStateId;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class GeofenceStateRepositoryAdapter implements GeofenceStateRepository {

    private final GeofenceStateJpaRepository jpa;

    public GeofenceStateRepositoryAdapter(GeofenceStateJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(readOnly = true)
    public GeofenceState find(String courierId, String storeId) {
        return jpa.findById(new GeofenceStateId(courierId, storeId))
                .map(CourierPersistenceMapper::toDomain)
                .orElseGet(GeofenceState::initial);
    }

    @Override
    @Transactional
    public void save(String courierId, String storeId, GeofenceState state) {
        jpa.save(CourierPersistenceMapper.toEntity(courierId, storeId, state));
    }
}
