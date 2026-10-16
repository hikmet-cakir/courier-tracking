package com.example.courier.domain.geo;

public final class HaversineDistanceCalculator implements DistanceCalculator {

    public static final double EARTH_RADIUS_METERS = 6_371_008.8;

    @Override
    public double metersBetween(double latitude1, double longitude1, double latitude2, double longitude2) {
        double lat1 = Math.toRadians(latitude1);
        double lat2 = Math.toRadians(latitude2);
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(longitude2 - longitude1);

        double sinDLat = Math.sin(dLat / 2.0);
        double sinDLon = Math.sin(dLon / 2.0);
        double a = sinDLat * sinDLat + Math.cos(lat1) * Math.cos(lat2) * sinDLon * sinDLon;
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return EARTH_RADIUS_METERS * c;
    }
}
