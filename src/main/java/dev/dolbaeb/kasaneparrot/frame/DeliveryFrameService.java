package dev.dolbaeb.kasaneparrot.frame;

import dev.dolbaeb.kasaneparrot.KasaneParrotPlugin;
import dev.dolbaeb.kasaneparrot.delivery.DeliverySession;
import dev.dolbaeb.kasaneparrot.module.PluginModule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Рамки доставки: невидимый ItemFrame на земле, внутри — предмет.
 *
 * <p>Поведение по ТЗ: ЛКМ по рамке — предмет выпадает на землю, рамка
 * исчезает. Правый клик (взять/вертеть) блокируется: предмет можно
 * забрать только «сбив» рамку. Взрывы/физика не уничтожают груз —
 * предмет выпадает на землю.</p>
 */
public final class DeliveryFrameService implements PluginModule {

    /** Период проверки времени жизни рамок (тики). */
    private static final long EXPIRE_PERIOD_TICKS = 200L;

    private final KasaneParrotPlugin plugin;
    private final Map<UUID, Entry> frames = new ConcurrentHashMap<>();
    private BukkitTask expireTask;

    /** Рамка + время рождения. */
    private record Entry(@NotNull ItemFrame frame, long spawnedAt) {
    }

    public DeliveryFrameService(@NotNull KasaneParrotPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return "delivery-frames";
    }

    @Override
    public void onEnable() {
        expireTask = Bukkit.getScheduler().runTaskTimer(
                plugin, this::expireFrames, EXPIRE_PERIOD_TICKS, EXPIRE_PERIOD_TICKS);
    }

    @Override
    public void onDisable() {
        if (expireTask != null) {
            expireTask.cancel();
            expireTask = null;
        }
        for (Entry entry : frames.values()) {
            dropAndRemove(entry.frame());
        }
        frames.clear();
    }

    /* ------------------------------------------------------------------ */

    /**
     * Ставит невидимую рамку с посылкой на найденное место посадки.
     *
     * @return UUID рамки или null, если поставить не удалось
     */
    @Nullable
    public UUID spawnDeliveryFrame(@NotNull Location ground,
                                   @NotNull ItemStack payload,
                                   @NotNull DeliverySession session) {
        World world = ground.getWorld();
        if (world == null) {
            return null;
        }
        var cfg = plugin.cfg().delivery();

        // Рамка «лежит» на земле: позиция — блок воздуха над поверхностью,
        // крепление к верхней грани блока-земли.
        Location frameLoc = ground.clone();
        ItemFrame frame = world.spawn(frameLoc, ItemFrame.class, f -> {
            f.setFacingDirection(BlockFace.UP, true);
            f.setVisible(!cfg.frameInvisible);
            f.setGlowing(cfg.frameGlowing);
            f.setFixed(false);
            // Дропом управляем сами (ЛКМ), ванильный шанс отключаем.
            f.setItemDropChance(0.0F);
            f.setItem(payload.clone());
            f.setPersistent(true);
            plugin.keys().setFlag(f, plugin.keys().deliveryFrame);
            plugin.keys().setUuid(f, plugin.keys().frameSession, session.id());
        });

        if (frame == null) {
            return null;
        }
        frames.put(frame.getUniqueId(), new Entry(frame, System.currentTimeMillis()));
        world.playSound(frame.getLocation(), "minecraft:entity.item_frame.add_item", 1.0F, 1.0F);
        return frame.getUniqueId();
    }

    /** Быстрая проверка «наша ли это рамка». */
    public boolean isDeliveryFrame(@Nullable ItemFrame frame) {
        return frame != null && plugin.keys().flag(frame, plugin.keys().deliveryFrame);
    }

    /**
     * ЛКМ игрока по рамке: отменяем ваниль, дропаем предмет, удаляем рамку.
     */
    public void handleLeftClick(@NotNull Player player, @NotNull ItemFrame frame) {
        if (!isDeliveryFrame(frame)) {
            return;
        }
        player.playSound(player.getLocation(), "minecraft:entity.item_frame.remove_item", 1.0F, 1.0F);
        dropAndRemove(frame);
    }

    /** Рамку сломало окружение (взрыв и т.п.): груз не теряем. */
    public void handleForcedBreak(@NotNull ItemFrame frame) {
        if (isDeliveryFrame(frame)) {
            dropAndRemove(frame);
        }
    }

    /* ------------------------------------------------------------------ */

    private void dropAndRemove(@NotNull ItemFrame frame) {
        frames.remove(frame.getUniqueId());
        ItemStack payload = frame.getItem();
        if (payload != null && !payload.getType().isAir()) {
            World world = frame.getWorld();
            world.dropItemNaturally(frame.getLocation(), payload);
            frame.setItem(null);
        }
        frame.remove();
    }

    private void expireFrames() {
        long lifetimeMs = plugin.cfg().delivery().frameLifetimeSeconds * 1000L;
        if (lifetimeMs <= 0L) {
            return; // рамки вечные
        }
        long now = System.currentTimeMillis();
        for (Entry entry : frames.values()) {
            if (now - entry.spawnedAt() >= lifetimeMs) {
                dropAndRemove(entry.frame());
            }
        }
    }
}
