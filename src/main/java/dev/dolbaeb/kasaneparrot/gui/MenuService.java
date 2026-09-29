package dev.dolbaeb.kasaneparrot.gui;

import dev.dolbaeb.kasaneparrot.KasaneParrotPlugin;
import dev.dolbaeb.kasaneparrot.delivery.DeliveryDestination;
import dev.dolbaeb.kasaneparrot.module.PluginModule;
import dev.dolbaeb.kasaneparrot.parrot.CourierParrot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис меню: реестр открытых окон, маршрутизация кликов и
 * <b>оптимизированное</b> живое обновление списка игроков.
 *
 * <p>Оптимизация join/quit: событие не перерисовывает меню сразу, а
 * планирует единственную отложенную задачу (refresh-delay-ticks).
 * Пачка событий (выход+вход, несколько игроков) схлопывается в один
 * рефреш; перерисовываются только открытые меню со списком игроков,
 * у которых живой заголовок и контент обновляются без переоткрытия окна.</p>
 */
public final class MenuService implements PluginModule {

    private final KasaneParrotPlugin plugin;
    private final HeadCache headCache;
    private final Map<UUID, AbstractCourierMenu> openMenus = new ConcurrentHashMap<>();
    private BukkitTask refreshTask;

    public MenuService(@NotNull KasaneParrotPlugin plugin, @NotNull HeadCache headCache) {
        this.plugin = plugin;
        this.headCache = headCache;
    }

    @Override
    public String id() {
        return "menus";
    }

    @Override
    public void onDisable() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        for (AbstractCourierMenu menu : openMenus.values()) {
            try {
                menu.viewer().closeInventory();
            } catch (Exception ignored) {
                // Выключение сервера — окна закрываются сами.
            }
        }
        openMenus.clear();
    }

    /* ------------------------------------------------------------------ */
    /* Открытие меню                                                       */
    /* ------------------------------------------------------------------ */

    public void openPlayerSelect(@NotNull Player viewer, @NotNull CourierParrot courier) {
        new PlayerSelectMenu(plugin, viewer, courier, 0).open();
    }

    public void openDestinationMenu(@NotNull Player viewer,
                                    @NotNull CourierParrot courier,
                                    @NotNull UUID targetId,
                                    @NotNull String targetName) {
        new DestinationMenu(plugin, viewer, courier, targetId, targetName).open();
    }

    public void openConfirmMenu(@NotNull Player viewer,
                                @NotNull CourierParrot courier,
                                @NotNull UUID targetId,
                                @NotNull String targetName,
                                @NotNull DeliveryDestination destination) {
        new ConfirmMenu(plugin, viewer, courier, targetId, targetName, destination).open();
    }

    /* ------------------------------------------------------------------ */
    /* Маршрутизация событий инвентаря                                     */
    /* ------------------------------------------------------------------ */

    public void handleClick(@NotNull InventoryClickEvent event) {
        AbstractCourierMenu menu = menuOf(event);
        if (menu == null) {
            return;
        }
        // Меню — чистый UI: никаких перемещений предметов.
        event.setCancelled(true);
        if (event.getClickedInventory() == menu.top()) {
            menu.onClick(event.getSlot(), event);
        }
    }

    public void handleDrag(@NotNull InventoryDragEvent event) {
        AbstractCourierMenu menu = menuOf(event);
        if (menu == null) {
            return;
        }
        event.setCancelled(true);
    }

    public void handleClose(@NotNull InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        AbstractCourierMenu menu = openMenus.get(player.getUniqueId());
        if (menu != null && event.getInventory() == menu.top()) {
            menu.handleClose();
            openMenus.remove(player.getUniqueId(), menu);
        }
    }

    /* ------------------------------------------------------------------ */
    /* Живое обновление                                                    */
    /* ------------------------------------------------------------------ */

    /** Планирует единый рефреш списков игроков (схлопывает пачки join/quit). */
    public void scheduleOnlineRefresh() {
        if (!plugin.cfg().gui().select().liveUpdate) {
            return;
        }
        if (refreshTask != null) {
            return; // уже запланировано
        }
        long delay = plugin.cfg().gui().select().refreshDelayTicks;
        refreshTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            refreshTask = null;
            for (AbstractCourierMenu menu : openMenus.values()) {
                if (menu instanceof PlayerSelectMenu && menu.isOpen()) {
                    menu.refresh();
                }
            }
        }, delay);
    }

    /* ------------------------------------------------------------------ */

    void register(@NotNull AbstractCourierMenu menu) {
        openMenus.put(menu.viewer().getUniqueId(), menu);
    }

    void unregister(@NotNull AbstractCourierMenu menu) {
        openMenus.remove(menu.viewer().getUniqueId(), menu);
    }

    public HeadCache headCache() {
        return headCache;
    }

    @org.jetbrains.annotations.Nullable
    private AbstractCourierMenu menuOf(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return null;
        }
        AbstractCourierMenu menu = openMenus.get(player.getUniqueId());
        if (menu == null) {
            return null;
        }
        // Защита: это именно наше окно (не игрокский инвентарь/сундук).
        if (event.getView().getTopInventory() != menu.top()) {
            return null;
        }
        return menu;
    }

    private AbstractCourierMenu menuOf(@NotNull InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return null;
        }
        AbstractCourierMenu menu = openMenus.get(player.getUniqueId());
        if (menu == null) {
            return null;
        }
        if (event.getView().getTopInventory() != menu.top()) {
            return null;
        }
        return menu;
    }
}
