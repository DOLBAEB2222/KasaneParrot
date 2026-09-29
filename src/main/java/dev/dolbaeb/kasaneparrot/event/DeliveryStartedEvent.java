package dev.dolbaeb.kasaneparrot.event;

import dev.dolbaeb.kasaneparrot.delivery.DeliverySession;
import dev.dolbaeb.kasaneparrot.parrot.CourierParrot;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Вызывается при подтверждении отправки, до взятия предмета из руки.
 * Отменяемое: доставка не начнётся.
 */
public final class DeliveryStartedEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final DeliverySession session;
    private final Player sender;
    private final CourierParrot courier;
    private boolean cancelled;

    public DeliveryStartedEvent(@NotNull DeliverySession session,
                                @NotNull Player sender,
                                @NotNull CourierParrot courier) {
        this.session = session;
        this.sender = sender;
        this.courier = courier;
    }

    public @NotNull DeliverySession session() {
        return session;
    }

    public @NotNull Player sender() {
        return sender;
    }

    public @NotNull CourierParrot courier() {
        return courier;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
