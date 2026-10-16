package com.example.courier.infrastructure.persistence.adapter;

import com.example.courier.domain.model.Location;
import com.example.courier.domain.port.CourierLocationRepository;
import com.example.courier.infrastructure.persistence.CourierLocationJpaRepository;
import com.example.courier.infrastructure.persistence.CourierPersistenceMapper;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class CourierLocationRepositoryAdapter implements CourierLocationRepository {

    private final CourierLocationJpaRepository jpa;

    public CourierLocationRepositoryAdapter(CourierLocationJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Stored> findByCourierIdAndRecordedAt(String courierId, Instant recordedAt) {
        return jpa.findByCourierIdAndRecordedAt(courierId, recordedAt)
                .map(entity -> new Stored(
                        CourierPersistenceMapper.toLocation(entity), entity.getReceivedAt(), entity.isApplied()));
    }

    @Override
    @Transactional
    public void append(Location location, Instant receivedAt, boolean applied) {
        jpa.saveAndFlush(CourierPersistenceMapper.toEntity(location, receivedAt, applied));
    }
}
