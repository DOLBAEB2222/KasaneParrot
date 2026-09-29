package dev.dolbaeb.kasaneparrot.delivery;

import dev.dolbaeb.kasaneparrot.parrot.CourierParrot;
import dev.dolbaeb.kasaneparrot.parrot.CourierState;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Полное состояние одной доставки: от подтверждения до возвращения
 * попугая на плечо хозяина. Хранится в памяти, между рестартами —
 * в storage (этап 3).
 */
public final class DeliverySession {

    private final UUID id;
    private final DeliveryRequest request;
    private final UUID parrotId;
    private final long startedAtMillis;
    private final long durationSeconds;

    private CourierState state;
    /** Момент, когда «скрытый» попугай должен появиться у цели. */
    private long arriveAtMillis;
    /** Точка взлёта (для восстановления при сбоях). */
    private Location vanishPoint;
    /** Точка появления у цели. */
    private Location approachPoint;
    /** Точка выгрузки посылки. */
    private Location handoffPoint;
    /** UUID рамки доставки (если уже создана). */
    private UUID frameId;
    /** Итог. */
    private DeliveryOutcome outcome = DeliveryOutcome.PENDING;
    private String failureReason = "";

    private DeliverySession(@NotNull UUID id,
                            @NotNull DeliveryRequest request,
                            @NotNull UUID parrotId,
                            long startedAtMillis,
                            long durationSeconds) {
        this.id = id;
        this.request = request;
        this.parrotId = parrotId;
        this.startedAtMillis = startedAtMillis;
        this.durationSeconds = durationSeconds;
        this.state = CourierState.ASCENDING;
    }

    public static DeliverySession create(@NotNull CourierParrot parrot,
                                         @NotNull DeliveryRequest request,
                                         long durationSeconds) {
        return new DeliverySession(
                UUID.randomUUID(),
                request,
                parrot.parrotId(),
                System.currentTimeMillis(),
                durationSeconds
        );
    }

    /* ------------------------------------------------------------------ */

    @NotNull
    public UUID id() {
        return id;
    }

    @NotNull
    public DeliveryRequest request() {
        return request;
    }

    @NotNull
    public UUID parrotId() {
        return parrotId;
    }

    public long startedAtMillis() {
        return startedAtMillis;
    }

    public long durationSeconds() {
        return durationSeconds;
    }

    @NotNull
    public CourierState state() {
        return state;
    }

    public void state(@NotNull CourierState state) {
        this.state = state;
    }

    public long arriveAtMillis() {
        return arriveAtMillis;
    }

    public void arriveAtMillis(long arriveAtMillis) {
        this.arriveAtMillis = arriveAtMillis;
    }

    @Nullable
    public Location vanishPoint() {
        return vanishPoint;
    }

    public void vanishPoint(@Nullable Location vanishPoint) {
        this.vanishPoint = vanishPoint;
    }

    @Nullable
    public Location approachPoint() {
        return approachPoint;
    }

    public void approachPoint(@Nullable Location approachPoint) {
        this.approachPoint = approachPoint;
    }

    @Nullable
    public Location handoffPoint() {
        return handoffPoint;
    }

    public void handoffPoint(@Nullable Location handoffPoint) {
        this.handoffPoint = handoffPoint;
    }

    @Nullable
    public UUID frameId() {
        return frameId;
    }

    public void frameId(@Nullable UUID frameId) {
        this.frameId = frameId;
    }

    @NotNull
    public DeliveryOutcome outcome() {
        return outcome;
    }

    public void outcome(@NotNull DeliveryOutcome outcome) {
        this.outcome = outcome;
    }

    @NotNull
    public String failureReason() {
        return failureReason;
    }

    public void failureReason(@NotNull String failureReason) {
        this.failureReason = failureReason;
    }

    public boolean nearOwner() {
        return nearOwner;
    }

    public void nearOwner(boolean nearOwner) {
        this.nearOwner = nearOwner;
    }

    public boolean notifiedTargetGone() {
        return notifiedTargetGone;
    }

    public void notifiedTargetGone(boolean notifiedTargetGone) {
        this.notifiedTargetGone = notifiedTargetGone;
    }
}
