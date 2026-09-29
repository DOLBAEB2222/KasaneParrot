package dev.dolbaeb.kasaneparrot.event;

import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Вызывается, когда игрок надевает мешочек на приручённого попугая.
 * Отменяемое: отмена оставит попугая ванильным.
 */
public final class ParrotPouchedEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Parrot parrot;
    private boolean cancelled;

    public ParrotPouchedEvent(@NotNull Player player, @NotNull Parrot parrot) {
        this.player = player;
        this.parrot = parrot;
    }

    public @NotNull Player player() {
        return player;
    }

    public @NotNull Parrot parrot() {
        return parrot;
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
