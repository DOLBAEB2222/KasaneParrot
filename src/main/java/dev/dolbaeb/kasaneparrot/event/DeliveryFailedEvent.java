package dev.dolbaeb.kasaneparrot.event;

import dev.dolbaeb.kasaneparrot.delivery.DeliveryOutcome;
import dev.dolbaeb.kasaneparrot.delivery.DeliverySession;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Вызывается при срыве доставки (цель потеряна, нет места посадки и т.д.).
 */
public final class DeliveryFailedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final DeliverySession session;
    private final DeliveryOutcome outcome;

    public DeliveryFailedEvent(@NotNull DeliverySession session, @NotNull DeliveryOutcome outcome) {
        this.session = session;
        this.outcome = outcome;
    }

    public @NotNull DeliverySession session() {
        return session;
    }

    public @NotNull DeliveryOutcome outcome() {
        return outcome;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
