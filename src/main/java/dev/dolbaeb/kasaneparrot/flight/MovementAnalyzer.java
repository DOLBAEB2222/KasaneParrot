package dev.dolbaeb.kasaneparrot.flight;

import dev.dolbaeb.kasaneparrot.KasaneParrotPlugin;
import dev.dolbaeb.kasaneparrot.module.PluginModule;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Анализатор активности игрока: «что делает цель».
 *
 * <p>Умная посадка (этап 2) спрашивает у анализатора, чем занят получатель:
 * стоит / идёт / бежит / элитры и т.д. Для устойчивости к микродвижениям
 * скорость сглаживается скользящим окном (10 семплов по 2 тика ≈ 1 секунда).</p>
 */
public final class MovementAnalyzer implements PluginModule {

    /** Сколько семплов в окне сглаживания. */
    private static final int WINDOW_SIZE = 10;
    /** Период семплирования (тики). */
    private static final long SAMPLE_PERIOD_TICKS = 2L;

    /** Пороги скорости (блоков/тик, по сглаженному окну). */
    private static final double SPRINT_SPEED = 0.22D;
    private static final double WALK_SPEED = 0.08D;

    private final KasaneParrotPlugin plugin;
    private final Map<UUID, SpeedWindow> windows = new ConcurrentHashMap<>();
    private BukkitTask task;

    public MovementAnalyzer(@NotNull KasaneParrotPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return "movement-analyzer";
    }

    @Override
    public void onEnable() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::sampleAll, 40L, SAMPLE_PERIOD_TICKS);
    }

    @Override
    public void onDisable() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        windows.clear();
    }

    /* ------------------------------------------------------------------ */

    /** Сглаженная скорость игрока (блоков/тик). */
    public double averageSpeed(@NotNull Player player) {
        SpeedWindow window = windows.get(player.getUniqueId());
        return window == null ? 0.0D : window.average();
    }

    /** Полный анализ: чем занят игрок прямо сейчас. */
    @NotNull
    public TargetActivity analyze(@NotNull Player player) {
        if (player.isDead()) {
            return TargetActivity.DEAD;
        }
        if (player.isSleeping()) {
            return TargetActivity.SLEEPING;
        }
        if (player.isInsideVehicle()) {
            return TargetActivity.IN_VEHICLE;
        }
        if (player.isGliding()) {
            return TargetActivity.GLIDING;
        }
        if (player.getGameMode() == GameMode.SPECTATOR || player.isFlying()) {
            return TargetActivity.FLYING;
        }
        if (player.isSwimming() || player.isInWater()) {
            return TargetActivity.SWIMMING;
        }

        double speed = averageSpeed(player);
        if (speed >= SPRINT_SPEED) {
            return TargetActivity.SPRINTING;
        }
        if (speed >= WALK_SPEED) {
            return TargetActivity.WALKING;
        }
        if (player.isSneaking()) {
            return TargetActivity.SNEAKING;
        }
        return TargetActivity.IDLE;
    }

    /* ------------------------------------------------------------------ */

    private void sampleAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            windows.computeIfAbsent(player.getUniqueId(), uuid -> new SpeedWindow())
                    .push(player.getVelocity().length());
        }
    }

    /** Кольцевой буфер скоростей. */
    private static final class SpeedWindow {
        private final double[] samples = new double[WINDOW_SIZE];
        private int index;
        private int filled;

        void push(double speed) {
            samples[index] = speed;
            index = (index + 1) % samples.length;
            if (filled < samples.length) {
                filled++;
            }
        }

        double average() {
            if (filled == 0) {
                return 0.0D;
            }
            double sum = 0.0D;
            for (int i = 0; i < filled; i++) {
                sum += samples[i];
            }
            return sum / filled;
        }
    }
}
