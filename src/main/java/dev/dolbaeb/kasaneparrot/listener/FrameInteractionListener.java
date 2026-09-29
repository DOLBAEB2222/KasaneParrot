package dev.dolbaeb.kasaneparrot.listener;

import dev.dolbaeb.kasaneparrot.KasaneParrotPlugin;
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Защита рамок доставки.
 *
 * <p>По ТЗ: ЛКМ по рамке — предмет выпадает, рамка исчезает. Правый клик
 * (поворот) и выкладывание предметов блокируются.</p>
 *
 * <p>Механика 1.21.8: ЛКМ по заполненной рамке выбивает предмет — это
 * действие {@code REMOVE} события {@link PlayerItemFrameChangeEvent}
 * (константы LEFT_CLICK в этом enum нет). Мы отменяем ванильный выброс и
 * выполняем свою семантику: дроп предмета + удаление рамки.</p>
 */
public final class FrameInteractionListener implements Listener {

    private final KasaneParrotPlugin plugin;

    public FrameInteractionListener(@NotNull KasaneParrotPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemFrameChange(@NotNull PlayerItemFrameChangeEvent event) {
        ItemFrame frame = event.getItemFrame();
        if (!plugin.frameService().isDeliveryFrame(frame)) {
            return;
        }
        event.setCancelled(true);
        if (event.getAction() == PlayerItemFrameChangeEvent.ItemFrameChangeAction.REMOVE) {
            // ЛКМ игрока: наша семантика — предмет выпадает, рамка исчезает.
            plugin.frameService().handleLeftClick(event.getPlayer(), frame);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFrameDamage(@NotNull EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ItemFrame frame)) {
            return;
        }
        if (!plugin.frameService().isDeliveryFrame(frame)) {
            return;
        }
        if (!(event.getDamager() instanceof Player)) {
            // Мобы/снаряды/взрывы рамку не трогают.
            event.setCancelled(true);
        }
        // Урон от игрока не отменяем: он триггерит REMOVE,
        // который мы перехватываем выше.
    }

    @EventHandler(ignoreCancelled = true)
    public void onHangingBreak(@NotNull HangingBreakEvent event) {
        if (!(event.getEntity() instanceof ItemFrame frame)) {
            return;
        }
        if (!plugin.frameService().isDeliveryFrame(frame)) {
            return;
        }
        // Взрыв/физика/атака пустой рамки: груз не теряем — дропаем предмет.
        event.setCancelled(true);
        plugin.frameService().handleForcedBreak(frame);
    }
}
