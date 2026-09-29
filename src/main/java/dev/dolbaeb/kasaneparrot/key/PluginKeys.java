package dev.dolbaeb.kasaneparrot.key;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Все {@link NamespacedKey} плагина и хелперы для PersistentDataContainer.
 *
 * <p>Данные на попугае (PDC) позволяют дешёвым тегом отвечать на вопрос
 * «это Kasane-курьер?» без обращения к реестру, а также переживать
 * рестарты сервера.</p>
 */
public final class PluginKeys {

    /** Признак «попугай является Kasane-курьером» (byte 1). */
    public final NamespacedKey courier;
    /** Признак «мешочек надет» (byte 1). */
    public final NamespacedKey pouched;
    /** Исходный вариант попугая (string) — чтобы вернуть «до мешочка», если понадобится. */
    public final NamespacedKey originalVariant;
    /** UUID хозяина (string) — быстрая проверка без resolve AnimalTamer. */
    public final NamespacedKey owner;
    /** Признак «это рамка доставки KasaneParrot» (byte 1), вешается на ItemFrame. */
    public final NamespacedKey deliveryFrame;
    /** UUID сессии доставки, к которой относится рамка (string). */
    public final NamespacedKey frameSession;

    private PluginKeys(@NotNull Plugin plugin) {
        this.courier = new NamespacedKey(plugin, "courier");
        this.pouched = new NamespacedKey(plugin, "pouched");
        this.originalVariant = new NamespacedKey(plugin, "original_variant");
        this.owner = new NamespacedKey(plugin, "owner");
        this.deliveryFrame = new NamespacedKey(plugin, "delivery_frame");
        this.frameSession = new NamespacedKey(plugin, "frame_session");
    }

    public static PluginKeys create(@NotNull Plugin plugin) {
        return new PluginKeys(plugin);
    }

    /* ------------------------------------------------------------------ */
    /* Хелперы PDC                                                        */
    /* ------------------------------------------------------------------ */

    /** true, если флаг установлен. */
    public boolean flag(@Nullable PersistentDataHolder holder, @NotNull NamespacedKey key) {
        return holder != null
                && holder.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    /** Устанавливает флаг (byte 1). */
    public void setFlag(@NotNull PersistentDataHolder holder, @NotNull NamespacedKey key) {
        holder.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
    }

    /** Снимает флаг. */
    public void clearFlag(@NotNull PersistentDataHolder holder, @NotNull NamespacedKey key) {
        holder.getPersistentDataContainer().remove(key);
    }

    /** Сохраняет UUID как строку. */
    public void setUuid(@NotNull PersistentDataHolder holder, @NotNull NamespacedKey key, @Nullable UUID value) {
        if (value == null) {
            holder.getPersistentDataContainer().remove(key);
        } else {
            holder.getPersistentDataContainer().set(key, PersistentDataType.STRING, value.toString());
        }
    }

    /** Читает UUID, сохранённый {@link #setUuid}. */
    @Nullable
    public UUID getUuid(@Nullable PersistentDataHolder holder, @NotNull NamespacedKey key) {
        if (holder == null) {
            return null;
        }
        String raw = holder.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
