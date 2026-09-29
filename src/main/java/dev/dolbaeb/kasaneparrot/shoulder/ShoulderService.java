package dev.dolbaeb.kasaneparrot.shoulder;

import dev.dolbaeb.kasaneparrot.KasaneParrotPlugin;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Посадка попугая на плечо и снятие с плеча.
 *
 * <p>Замечание по API: {@code HumanEntity#setShoulderEntityLeft/Right}
 * помечены Bukkit как deprecated («нет чёткой семантики сериализации»),
 * но это единственный чистый способ усадить попугая на плечо, а клиент
 * Minecraft рендерит на плечах <b>только</b> попугаев — то есть ровно
 * наш случай. Метод корректно убирает сущность из мира.</p>
 */
@SuppressWarnings("deprecation")
public final class ShoulderService {

    private final KasaneParrotPlugin plugin;

    public ShoulderService(@NotNull KasaneParrotPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Сажает попугая на плечо (SIDE из конфига: LEFT / RIGHT / AUTO).
     *
     * @return true, если посадка удалась
     */
    public boolean perch(@NotNull Player owner, @NotNull Parrot parrot) {
        String side = plugin.cfg().flight().returning.shoulderSide.toUpperCase(java.util.Locale.ROOT);
        if ("LEFT".equals(side)) {
            owner.setShoulderEntityLeft(parrot);
            return true;
        }
        if ("RIGHT".equals(side)) {
            owner.setShoulderEntityRight(parrot);
            return true;
        }
        // AUTO: менее занятое плечо.
        if (owner.getShoulderEntityLeft() == null) {
            owner.setShoulderEntityLeft(parrot);
        } else {
            owner.setShoulderEntityRight(parrot);
        }
        return true;
    }

    /**
     * Снимает конкретного попугая с плеч игрока (например, перед новой
     * доставкой) и возвращает сущность в мир.
     *
     * @return освобождённый попугай или null, если его не было на плече
     */
    @Nullable
    public Parrot release(@NotNull Player owner, @NotNull UUID parrotId) {
        if (isPerched(owner.getShoulderEntityLeft(), parrotId)) {
            org.bukkit.entity.Entity released = owner.releaseLeftShoulderEntity();
            return released instanceof Parrot parrot ? parrot : null;
        }
        if (isPerched(owner.getShoulderEntityRight(), parrotId)) {
            org.bukkit.entity.Entity released = owner.releaseRightShoulderEntity();
            return released instanceof Parrot parrot ? parrot : null;
        }
        return null;
    }

    /** true, если попугай с данным UUID сейчас на плече игрока. */
    public boolean isPerched(@NotNull Player owner, @NotNull UUID parrotId) {
        return isPerched(owner.getShoulderEntityLeft(), parrotId)
                || isPerched(owner.getShoulderEntityRight(), parrotId);
    }

    private static boolean isPerched(@Nullable org.bukkit.entity.Entity entity, @NotNull UUID parrotId) {
        return entity != null && entity.getUniqueId().equals(parrotId);
    }
}
