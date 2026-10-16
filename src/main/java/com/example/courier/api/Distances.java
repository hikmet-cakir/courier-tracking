package com.example.courier.api;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Distances {

    private Distances() {}

    public static BigDecimal roundMeters(double meters) {
        return BigDecimal.valueOf(meters).setScale(2, RoundingMode.HALF_UP);
    }
}
