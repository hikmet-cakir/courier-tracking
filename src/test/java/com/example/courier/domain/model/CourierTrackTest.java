package com.example.courier.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.example.courier.domain.geo.DistanceCalculator;
import com.example.courier.domain.geo.HaversineDistanceCalculator;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CourierTrackTest {

    private static final Instant T0 = Instant.parse("2024-01-01T00:00:00Z");

    @Test
    void firstLocationContributesZero() {
        DistanceCalculator distances = (lat1, lon1, lat2, lon2) -> 999.0;
        Location first = new Location("c1", T0, 41.0, 29.0);

        CourierTrack track = CourierTrack.empty("c1").apply(first, distances);

        assertThat(track.totalDistanceMeters()).isZero();
        assertThat(track.lastLatitude()).isEqualTo(41.0);
        assertThat(track.lastLongitude()).isEqualTo(29.0);
        assertThat(track.lastRecordedAt()).isEqualTo(T0);
    }

    @Test
    void totalDistanceIsSumOfConsecutiveSegmentsNotFirstMinusLast() {
        DistanceCalculator distances = (lat1, lon1, lat2, lon2) -> Math.abs(lat2 - lat1) * 1000.0;

        Location a = new Location("c1", T0, 0.0, 0.0);
        Location b = new Location("c1", T0.plusSeconds(1), 3.0, 0.0);
        Location c = new Location("c1", T0.plusSeconds(2), 1.0, 0.0);

        CourierTrack track = CourierTrack.empty("c1")
                .apply(a, distances)
                .apply(b, distances)
                .apply(c, distances);

        assertThat(track.totalDistanceMeters()).isEqualTo(5000.0);

        double firstMinusLast = distances.metersBetween(0.0, 0.0, 1.0, 0.0);
        assertThat(firstMinusLast).isEqualTo(1000.0);
        assertThat(track.totalDistanceMeters()).isNotEqualTo(firstMinusLast);
    }

    @Test
    void outAndBackSumIsNotZeroUnlikeFirstMinusLast() {
        DistanceCalculator haversine = new HaversineDistanceCalculator();
        double originLat = 41.0;
        double originLng = 29.0;
        double northLat = originLat
                + Math.toDegrees(100.0 / HaversineDistanceCalculator.EARTH_RADIUS_METERS);

        Location start = new Location("c1", T0, originLat, originLng);
        Location north = new Location("c1", T0.plusSeconds(1), northLat, originLng);
        Location back = new Location("c1", T0.plusSeconds(2), originLat, originLng);

        CourierTrack track = CourierTrack.empty("c1")
                .apply(start, haversine)
                .apply(north, haversine)
                .apply(back, haversine);

        assertThat(track.totalDistanceMeters()).isCloseTo(200.0, within(0.05));
        assertThat(haversine.metersBetween(originLat, originLng, originLat, originLng)).isZero();
    }
}
