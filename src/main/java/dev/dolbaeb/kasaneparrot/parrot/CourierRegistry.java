package dev.dolbaeb.kasaneparrot.parrot;

import org.bukkit.entity.Parrot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Реестр курьеров: parrot UUID → {@link CourierParrot}.
 *
 * <p>Быстрая проверка «этот попугай — курьер?» идёт в два шага:
 * сначала дешёвый PDC-флаг на сущности, затем ConcurrentHashMap.
 * Поэтому горячие слушатели (клики, урон) не аллоцируют лишнего.</p>
 */
public final class CourierRegistry {

    private final Map<UUID, CourierParrot> couriers = new ConcurrentHashMap<>();

    /** Регистрирует попугая как курьера (при надевании мешочка). */
    public void register(@NotNull CourierParrot courier) {
        couriers.put(courier.parrotId(), courier);
    }

    /** Полное снятие курьерства (смерть попугая и т.п.). */
    public void unregister(@NotNull UUID parrotId) {
        couriers.remove(parrotId);
    }

    @Nullable
    public CourierParrot get(@NotNull UUID parrotId) {
        return couriers.get(parrotId);
    }

    /**
     * Курьер по сущности. Дорогой путь (без PDC) — использовать,
     * когда флага под рукой нет (например, после рестарта).
     */
    @Nullable
    public CourierParrot byEntity(@NotNull Parrot parrot) {
        return couriers.get(parrot.getUniqueId());
    }

    /** Все зарегистрированные курьеры. */
    public Collection<CourierParrot> all() {
        return couriers.values();
    }

    /** Только курьеры в миссии. */
    public Collection<CourierParrot> onMission() {
        return couriers.values().stream()
                .filter(c -> c.state().isOnMission())
                .collect(Collectors.toList());
    }

    public void clear() {
        couriers.clear();
    }
}
