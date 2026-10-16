package com.example.courier.application;

import com.example.courier.domain.geo.DistanceCalculator;
import com.example.courier.domain.geo.HaversineDistanceCalculator;
import com.example.courier.domain.geofence.GeofenceEvaluator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class DomainBeans {

    @Bean
    DistanceCalculator distanceCalculator() {
        return new HaversineDistanceCalculator();
    }

    @Bean
    GeofenceEvaluator geofenceEvaluator() {
        return new GeofenceEvaluator();
    }
}
