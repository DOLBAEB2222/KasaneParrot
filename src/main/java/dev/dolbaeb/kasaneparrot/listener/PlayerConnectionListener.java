package dev.dolbaeb.kasaneparrot.listener;

import dev.dolbaeb.kasaneparrot.KasaneParrotPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Вход/выход игроков: живое обновление меню и реакция доставок
 * на исчезновение/появление получателя.
 */
public final class PlayerConnectionListener implements Listener {

    private final KasaneParrotPlugin plugin;

    public PlayerConnectionListener(@NotNull KasaneParrotPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        // Один схлопнутый рефреш на пачку событий join.
        plugin.menuService().scheduleOnlineRefresh();
        // Попугай «долетает» до вернувшегося получателя.
        plugin.deliveryService().handleTargetJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        plugin.menuService().scheduleOnlineRefresh();
        // Уведомляем отправителей, чьи получатели вышли.
        plugin.deliveryService().handleTargetQuit(event.getPlayer());
    }
}
