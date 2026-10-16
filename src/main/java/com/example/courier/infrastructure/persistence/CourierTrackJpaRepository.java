package com.example.courier.infrastructure.persistence;

import com.example.courier.infrastructure.persistence.entity.CourierTrackEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourierTrackJpaRepository extends JpaRepository<CourierTrackEntity, String> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            value =
                    """
                    INSERT INTO courier_track (courier_id, total_distance_meters)
                    VALUES (:courierId, 0)
                    ON CONFLICT (courier_id) DO NOTHING
                    """,
            nativeQuery = true)
    void insertIfAbsent(@Param("courierId") String courierId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM CourierTrackEntity t WHERE t.courierId = :courierId")
    Optional<CourierTrackEntity> findByCourierIdForUpdate(@Param("courierId") String courierId);
}
