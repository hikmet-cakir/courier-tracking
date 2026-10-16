package com.example.courier.domain.geofence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class GeofenceEvaluatorTest {

    private static final Instant T0 = Instant.parse("2024-01-01T00:00:00Z");

    private final GeofenceEvaluator evaluator = new GeofenceEvaluator();

    @Test
    void ninetyNinePointNineMetersIsInside() {
        GeofenceDecision decision = evaluator.evaluate(GeofenceState.initial(), 99.9, T0);
        assertThat(decision.state().inside()).isTrue();
        assertThat(decision.entranceEmitted()).isTrue();
    }

    @Test
    void exactlyOneHundredMetersIsInsideInclusive() {
        GeofenceDecision decision = evaluator.evaluate(GeofenceState.initial(), 100.0, T0);
        assertThat(decision.state().inside()).isTrue();
        assertThat(decision.entranceEmitted()).isTrue();
    }

    @Test
    void oneHundredPointOneMetersIsOutside() {
        GeofenceDecision decision = evaluator.evaluate(GeofenceState.initial(), 100.1, T0);
        assertThat(decision.state().inside()).isFalse();
        assertThat(decision.entranceEmitted()).isFalse();
    }

    @Test
    void outsideToInsideEmitsEntrance() {
        GeofenceDecision decision = evaluator.evaluate(GeofenceState.initial(), 40.0, T0);
        assertThat(decision.entranceEmitted()).isTrue();
        assertThat(decision.state()).isInstanceOf(GeofenceState.Inside.class);
        assertThat(decision.state().lastEntranceAt()).isEqualTo(T0);
    }

    @Test
    void remainingInsideDoesNotEmitAgain() {
        GeofenceState inside = evaluator.evaluate(GeofenceState.initial(), 40.0, T0).state();
        GeofenceDecision stay = evaluator.evaluate(inside, 10.0, T0.plusSeconds(10));
        assertThat(stay.entranceEmitted()).isFalse();
        assertThat(stay.state().inside()).isTrue();
        assertThat(stay.state().lastEntranceAt()).isEqualTo(T0);
    }

    @Test
    void reentryInsideCooldownDoesNotEmitAndDoesNotAdvanceLastEntrance() {
        GeofenceState afterFirst = evaluator.evaluate(GeofenceState.initial(), 40.0, T0).state();
        GeofenceState outside = evaluator.evaluate(afterFirst, 150.0, T0.plusSeconds(10)).state();
        GeofenceDecision reentry = evaluator.evaluate(outside, 40.0, T0.plusSeconds(59));

        assertThat(reentry.entranceEmitted()).isFalse();
        assertThat(reentry.state().inside()).isTrue();
        assertThat(reentry.state().lastEntranceAt()).isEqualTo(T0);
    }

    @Test
    void reentryAtExactlySixtySecondsEmitsAndAdvancesLastEntrance() {
        GeofenceState afterFirst = evaluator.evaluate(GeofenceState.initial(), 40.0, T0).state();
        GeofenceState outside = evaluator.evaluate(afterFirst, 150.0, T0.plusSeconds(10)).state();
        Instant t60 = T0.plusSeconds(60);
        GeofenceDecision reentry = evaluator.evaluate(outside, 40.0, t60);

        assertThat(reentry.entranceEmitted()).isTrue();
        assertThat(reentry.state().inside()).isTrue();
        assertThat(reentry.state().lastEntranceAt()).isEqualTo(t60);
    }

    @Test
    void reentryAtFiftyNineSecondsDoesNotEmit() {
        GeofenceState afterFirst = evaluator.evaluate(GeofenceState.initial(), 40.0, T0).state();
        GeofenceState outside = evaluator.evaluate(afterFirst, 150.0, T0.plusSeconds(5)).state();
        GeofenceDecision reentry = evaluator.evaluate(outside, 40.0, T0.plusSeconds(59));

        assertThat(reentry.entranceEmitted()).isFalse();
        assertThat(reentry.state().lastEntranceAt()).isEqualTo(T0);
    }

    @Test
    void firstLocationAlreadyInsideEmitsEntrance() {
        GeofenceDecision decision = evaluator.evaluate(GeofenceState.initial(), 0.0, T0);
        assertThat(decision.entranceEmitted()).isTrue();
        assertThat(decision.state().inside()).isTrue();
        assertThat(decision.state().lastEntranceAt()).isEqualTo(T0);
    }

    @Test
    void oneCourierNearTwoStoresKeepsIndependentState() {
        GeofenceState storeA = GeofenceState.initial();
        GeofenceState storeB = GeofenceState.initial();

        GeofenceDecision a = evaluator.evaluate(storeA, 20.0, T0);
        GeofenceDecision b = evaluator.evaluate(storeB, 150.0, T0);

        assertThat(a.entranceEmitted()).isTrue();
        assertThat(b.entranceEmitted()).isFalse();
        assertThat(a.state().inside()).isTrue();
        assertThat(b.state().inside()).isFalse();
    }

    @Test
    void twoCouriersAtSameStoreKeepIndependentCooldown() {
        GeofenceState courier1 = GeofenceState.initial();
        GeofenceState courier2 = GeofenceState.initial();

        GeofenceState c1Inside = evaluator.evaluate(courier1, 10.0, T0).state();
        GeofenceState c1Outside = evaluator.evaluate(c1Inside, 200.0, T0.plusSeconds(5)).state();
        GeofenceDecision c1Reentry = evaluator.evaluate(c1Outside, 10.0, T0.plusSeconds(30));
        GeofenceDecision c2Entry = evaluator.evaluate(courier2, 10.0, T0.plusSeconds(30));

        assertThat(c1Reentry.entranceEmitted()).isFalse();
        assertThat(c2Entry.entranceEmitted()).isTrue();
        assertThat(c1Reentry.state().lastEntranceAt()).isEqualTo(T0);
        assertThat(c2Entry.state().lastEntranceAt()).isEqualTo(T0.plusSeconds(30));
    }
}
