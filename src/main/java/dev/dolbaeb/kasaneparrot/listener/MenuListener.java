package dev.dolbaeb.kasaneparrot.listener;

import dev.dolbaeb.kasaneparrot.KasaneParrotPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Мост событий инвентаря → {@code MenuService}. Единственная точка,
 * где меню получают клики/драги/закрытия.
 */
public final class MenuListener implements Listener {

    private final KasaneParrotPlugin plugin;

    public MenuListener(@NotNull KasaneParrotPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(@NotNull InventoryClickEvent event) {
        plugin.menuService().handleClick(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(@NotNull InventoryDragEvent event) {
        plugin.menuService().handleDrag(event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(@NotNull InventoryCloseEvent event) {
        plugin.menuService().handleClose(event);
    }
}
