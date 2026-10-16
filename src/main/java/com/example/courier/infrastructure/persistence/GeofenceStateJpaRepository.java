package com.example.courier.infrastructure.persistence;

import com.example.courier.infrastructure.persistence.entity.GeofenceStateEntity;
import com.example.courier.infrastructure.persistence.entity.GeofenceStateId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeofenceStateJpaRepository extends JpaRepository<GeofenceStateEntity, GeofenceStateId> {}
