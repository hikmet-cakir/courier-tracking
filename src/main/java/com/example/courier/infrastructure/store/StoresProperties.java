package com.example.courier.infrastructure.store;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "courier.stores")
public record StoresProperties(String location) {
}
