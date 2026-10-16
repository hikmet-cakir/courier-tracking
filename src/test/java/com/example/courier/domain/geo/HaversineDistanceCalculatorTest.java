package com.example.courier.domain.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class HaversineDistanceCalculatorTest {

    private static final double METERS = 1.0;
    private static final double CITY_TOLERANCE_METERS = 2_000.0;

    private final DistanceCalculator calculator = new HaversineDistanceCalculator();

    @Test
    void parisToLondonMatchesKnownCityDistance() {
        double meters = calculator.metersBetween(48.8566, 2.3522, 51.5074, -0.1278);
        assertThat(meters).isCloseTo(344_000.0, within(CITY_TOLERANCE_METERS));
    }

    @Test
    void istanbulToAnkaraMatchesKnownCityDistance() {
        double meters = calculator.metersBetween(41.0082, 28.9784, 39.9334, 32.8597);
        assertThat(meters).isCloseTo(349_000.0, within(CITY_TOLERANCE_METERS));
    }

    @Test
    void samePointIsZero() {
        assertThat(calculator.metersBetween(40.9923307, 29.1244229, 40.9923307, 29.1244229))
                .isCloseTo(0.0, within(1e-9));
    }

    @Test
    void distanceIsSymmetric() {
        double forward = calculator.metersBetween(40.9923307, 29.1244229, 41.055783, 29.0210292);
        double reverse = calculator.metersBetween(41.055783, 29.0210292, 40.9923307, 29.1244229);
        assertThat(forward).isCloseTo(reverse, within(METERS));
    }

    @Test
    void antimeridianUsesShortestArc() {
        double meters = calculator.metersBetween(0.0, 179.9, 0.0, -179.9);
        double expected = HaversineDistanceCalculator.EARTH_RADIUS_METERS * Math.toRadians(0.2);
        assertThat(meters).isCloseTo(expected, within(METERS));
        assertThat(meters).isLessThan(50_000.0);
    }

    @Test
    void polesWithDifferentLongitudesAreNearlyCoincident() {
        assertThat(calculator.metersBetween(90.0, 0.0, 90.0, 179.0)).isCloseTo(0.0, within(METERS));
    }

    @Test
    void northPoleToSouthPoleIsHalfCircumference() {
        double expected = Math.PI * HaversineDistanceCalculator.EARTH_RADIUS_METERS;
        assertThat(calculator.metersBetween(90.0, 0.0, -90.0, 0.0)).isCloseTo(expected, within(METERS));
    }
}
