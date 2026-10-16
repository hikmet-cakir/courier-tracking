package com.example.courier.infrastructure.persistence.adapter;

import com.example.courier.domain.model.EntranceEvent;
import com.example.courier.domain.port.EntranceEventRepository;
import com.example.courier.infrastructure.persistence.CourierPersistenceMapper;
import com.example.courier.infrastructure.persistence.EntranceEventJpaRepository;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class EntranceEventRepositoryAdapter implements EntranceEventRepository {

    private final EntranceEventJpaRepository jpa;

    public EntranceEventRepositoryAdapter(EntranceEventJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public void save(EntranceEvent event) {
        jpa.saveAndFlush(CourierPersistenceMapper.toEntity(event));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntranceEvent> find(String courierId, String storeId) {
        if (courierId != null && storeId != null) {
            return jpa.findByCourierIdAndStoreIdOrderByOccurredAtAsc(courierId, storeId).stream()
                    .map(CourierPersistenceMapper::toDomain)
                    .toList();
        }
        if (courierId != null) {
            return jpa.findByCourierIdOrderByOccurredAtAsc(courierId).stream()
                    .map(CourierPersistenceMapper::toDomain)
                    .toList();
        }
        if (storeId != null) {
            return jpa.findByStoreIdOrderByOccurredAtAsc(storeId).stream()
                    .map(CourierPersistenceMapper::toDomain)
                    .toList();
        }
        return jpa.findAllByOrderByOccurredAtAsc().stream()
                .map(CourierPersistenceMapper::toDomain)
                .toList();
    }
}
