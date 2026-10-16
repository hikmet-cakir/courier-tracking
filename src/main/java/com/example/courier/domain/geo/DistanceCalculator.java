package com.example.courier.domain.geo;

public interface DistanceCalculator {

    double metersBetween(double latitude1, double longitude1, double latitude2, double longitude2);
}
