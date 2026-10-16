package com.example.courier.infrastructure.persistence;

import com.example.courier.infrastructure.persistence.entity.EntranceEventEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntranceEventJpaRepository extends JpaRepository<EntranceEventEntity, Long> {

    List<EntranceEventEntity> findAllByOrderByOccurredAtAsc();

    List<EntranceEventEntity> findByCourierIdOrderByOccurredAtAsc(String courierId);

    List<EntranceEventEntity> findByStoreIdOrderByOccurredAtAsc(String storeId);

    List<EntranceEventEntity> findByCourierIdAndStoreIdOrderByOccurredAtAsc(String courierId, String storeId);
}
