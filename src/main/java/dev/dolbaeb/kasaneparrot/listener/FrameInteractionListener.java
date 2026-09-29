package dev.dolbaeb.kasaneparrot.listener;

import dev.dolbaeb.kasaneparrot.KasaneParrotPlugin;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerItemFrameChangeEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Защита рамок доставки.
 *
 * <p>По ТЗ: ЛКМ по рамке — предмет выпадает, рамка исчезает. Правый клик
 * (взять/поворот) и урон блокируются: забрать посылку можно только «сбив»
 * рамку. Взрывы и физика не съедают груз — предмет выпадает на землю.</p>
 */
public final class FrameInteractionListener implements Listener {

    private final KasaneParrotPlugin plugin;

    public FrameInteractionListener(@NotNull KasaneParrotPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemFrameChange(@NotNull PlayerItemFrameChangeEvent event) {
        if (!(event.getItemFrame() instanceof ItemFrame frame)) {
            return;
        }
        if (!plugin.frameService().isDeliveryFrame(frame)) {
            return;
        }
        event.setCancelled(true);
        if (event.getAction() == PlayerItemFrameChangeEvent.Action.LEFT_CLICK) {
            // Наша семантика: предмет выпадает, рамка исчезает.
            plugin.frameService().handleLeftClick(event.getPlayer(), frame);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHangingBreak(@NotNull HangingBreakEvent event) {
        if (!(event.getEntity() instanceof ItemFrame frame)) {
            return;
        }
        if (!plugin.frameService().isDeliveryFrame(frame)) {
            return;
        }
        if (event instanceof HangingBreakByEntityEvent byEntity
                && byEntity.getRemover() instanceof Player) {
            // Атаку игрока обрабатывает PlayerItemFrameChangeEvent.
            return;
        }
        // Взрыв/физика: груз не теряем — дропаем предмет.
        event.setCancelled(true);
        plugin.frameService().handleForcedBreak(frame);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFrameDamage(@NotNull EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ItemFrame frame)) {
            return;
        }
        if (plugin.frameService().isDeliveryFrame(frame)) {
            event.setCancelled(true);
        }
    }
}
