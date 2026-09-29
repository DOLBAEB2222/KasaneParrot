package dev.dolbaeb.kasaneparrot.parrot;

import dev.dolbaeb.kasaneparrot.delivery.DeliverySession;
import org.bukkit.entity.Parrot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Обёртка над попугаем-курьером: состояние, хозяин, груз и ссылка на сессию.
 *
 * <p>Сущность может отсутствовать (плечо/выгрузка), поэтому везде, где
 * нужен {@link Parrot}, используется {@link #entity()} с null-проверкой
 * через {@link #isAlive()}.</p>
 */
public final class CourierParrot {

    private final UUID parrotId;
    private final UUID ownerId;

    private Parrot entity;
    private CourierState state = CourierState.NORMAL;
    private ItemStack payload;
    private DeliverySession session;
    private long stateChangedAt = System.currentTimeMillis();

    public CourierParrot(@NotNull Parrot entity, @NotNull UUID ownerId) {
        this.entity = entity;
        this.parrotId = entity.getUniqueId();
        this.ownerId = ownerId;
    }

    /* ------------------------------------------------------------------ */

    @NotNull
    public UUID parrotId() {
        return parrotId;
    }

    @NotNull
    public UUID ownerId() {
        return ownerId;
    }

    @Nullable
    public Parrot entity() {
        return entity;
    }

    /** Обновляет ссылку на сущность (после ре-спавна/телепорта). */
    public void entity(@Nullable Parrot entity) {
        this.entity = entity;
    }

    /** true, если сущность ещё жива и в мире. */
    public boolean isAlive() {
        return entity != null && entity.isValid() && !entity.isDead();
    }

    @NotNull
    public CourierState state() {
        return state;
    }

    public void state(@NotNull CourierState state) {
        this.state = state;
        this.stateChangedAt = System.currentTimeMillis();
    }

    public long stateChangedAt() {
        return stateChangedAt;
    }

    /** Мс в текущем состоянии. */
    public long stateDurationMs() {
        return System.currentTimeMillis() - stateChangedAt;
    }

    @Nullable
    public ItemStack payload() {
        return payload;
    }

    public void payload(@Nullable ItemStack payload) {
        this.payload = payload;
    }

    @Nullable
    public DeliverySession session() {
        return session;
    }

    public void session(@Nullable DeliverySession session) {
        this.session = session;
    }

    /** true, если попугай занят доставкой. */
    public boolean isBusy() {
        return state.isOnMission() || state == CourierState.AWAITING_PAYLOAD;
    }
}
