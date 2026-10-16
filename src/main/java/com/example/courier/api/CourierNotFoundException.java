package com.example.courier.api;

public class CourierNotFoundException extends RuntimeException {

    private final String courierId;

    public CourierNotFoundException(String courierId) {
        super("Unknown courier: " + courierId);
        this.courierId = courierId;
    }

    public String courierId() {
        return courierId;
    }
}
