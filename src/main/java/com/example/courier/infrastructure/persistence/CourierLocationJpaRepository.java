package com.example.courier.infrastructure.persistence;

import com.example.courier.infrastructure.persistence.entity.CourierLocationEntity;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourierLocationJpaRepository extends JpaRepository<CourierLocationEntity, Long> {

    Optional<CourierLocationEntity> findByCourierIdAndRecordedAt(String courierId, Instant recordedAt);
}
