package com.example.courier;

import com.example.courier.infrastructure.store.StoresProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(StoresProperties.class)
public class CourierTrackingApplication {

    public static void main(String[] args) {
        SpringApplication.run(CourierTrackingApplication.class, args);
    }
}
