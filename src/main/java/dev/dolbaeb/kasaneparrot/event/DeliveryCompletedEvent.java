package dev.dolbaeb.kasaneparrot.event;

import dev.dolbaeb.kasaneparrot.delivery.DeliverySession;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Вызывается, когда посылка выгружена (рамка поставлена / предмет выпал).
 */
public final class DeliveryCompletedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final DeliverySession session;
    private final Player target;

    public DeliveryCompletedEvent(@NotNull DeliverySession session, @Nullable Player target) {
        this.session = session;
        this.target = target;
    }

    public @NotNull DeliverySession session() {
        return session;
    }

    /** Получатель, если был онлайн в момент выгрузки. */
    public @Nullable Player target() {
        return target;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
