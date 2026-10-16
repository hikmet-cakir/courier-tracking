package com.example.courier.infrastructure.persistence.adapter;

import com.example.courier.domain.model.CourierTrack;
import com.example.courier.domain.port.CourierTrackRepository;
import com.example.courier.infrastructure.persistence.CourierPersistenceMapper;
import com.example.courier.infrastructure.persistence.CourierTrackJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class CourierTrackRepositoryAdapter implements CourierTrackRepository {

    private final CourierTrackJpaRepository jpa;

    public CourierTrackRepositoryAdapter(CourierTrackJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CourierTrack> find(String courierId) {
        return jpa.findById(courierId).map(CourierPersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public CourierTrack lockOrCreate(String courierId) {
        jpa.insertIfAbsent(courierId);
        return jpa.findByCourierIdForUpdate(courierId)
                .map(CourierPersistenceMapper::toDomain)
                .orElseThrow(() -> new IllegalStateException("courier_track missing after insert: " + courierId));
    }

    @Override
    @Transactional
    public void save(CourierTrack track) {
        jpa.save(CourierPersistenceMapper.toEntity(track));
    }
}
