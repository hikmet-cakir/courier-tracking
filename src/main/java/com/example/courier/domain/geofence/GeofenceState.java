package com.example.courier.domain.geofence;

import java.time.Instant;
import java.util.Objects;

public sealed interface GeofenceState permits GeofenceState.Outside, GeofenceState.Inside {

    boolean inside();

    Instant lastEntranceAt();

    GeofenceDecision transition(boolean nowInside, Instant recordedAt);

    static GeofenceState initial() {
        return new Outside(null);
    }

    record Outside(Instant lastEntranceAt) implements GeofenceState {

        @Override
        public boolean inside() {
            return false;
        }

        @Override
        public GeofenceDecision transition(boolean nowInside, Instant recordedAt) {
            Objects.requireNonNull(recordedAt, "recordedAt");
            if (!nowInside) {
                return new GeofenceDecision(this, false);
            }
            if (canRecordEntrance(lastEntranceAt, recordedAt)) {
                return new GeofenceDecision(new Inside(recordedAt), true);
            }
            return new GeofenceDecision(new Inside(lastEntranceAt), false);
        }
    }

    record Inside(Instant lastEntranceAt) implements GeofenceState {

        @Override
        public boolean inside() {
            return true;
        }

        @Override
        public GeofenceDecision transition(boolean nowInside, Instant recordedAt) {
            Objects.requireNonNull(recordedAt, "recordedAt");
            if (nowInside) {
                return new GeofenceDecision(this, false);
            }
            return new GeofenceDecision(new Outside(lastEntranceAt), false);
        }
    }

    private static boolean canRecordEntrance(Instant lastEntranceAt, Instant recordedAt) {
        if (lastEntranceAt == null) {
            return true;
        }
        return !recordedAt.isBefore(lastEntranceAt.plus(GeofenceEvaluator.REENTRY_COOLDOWN));
    }
}
