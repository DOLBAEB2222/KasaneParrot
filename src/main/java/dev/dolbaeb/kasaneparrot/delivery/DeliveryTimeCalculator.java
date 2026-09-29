package dev.dolbaeb.kasaneparrot.delivery;

import dev.dolbaeb.kasaneparrot.KasaneParrotPlugin;
import dev.dolbaeb.kasaneparrot.util.Vectors;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Расчёт времени доставки по расстоянию (по ТЗ: от 30 секунд до 5 минут).
 *
 * <p>Формула: расстояние между игроками клампится в
 * {@code [min-distance .. max-distance]} и линейно отображается на
 * {@code [min-seconds .. max-seconds]}. Междумирье — фиксированное время.</p>
 */
public final class DeliveryTimeCalculator {

    private final KasaneParrotPlugin plugin;

    public DeliveryTimeCalculator(@NotNull KasaneParrotPlugin plugin) {
        this.plugin = plugin;
    }

    /** Секунды полёта между двумя точками. */
    public long deliverySeconds(@NotNull Location from, @NotNull Location to) {
        var time = plugin.cfg().delivery();

        if (from.getWorld() == null || to.getWorld() == null
                || !from.getWorld().equals(to.getWorld())) {
            return time.crossWorldSeconds;
        }

        double distance = Vectors.distance(from, to);
        return deliverySeconds(distance);
    }

    /** Секунды полёта по известному расстоянию (блоков). */
    public long deliverySeconds(double distance) {
        var time = plugin.cfg().delivery();

        double span = time.maxDistance - time.minDistance;
        double ratio = span <= 0.0D
                ? 1.0D
                : (clamp(distance, time.minDistance, time.maxDistance) - time.minDistance) / span;

        double seconds = time.minSeconds + (time.maxSeconds - time.minSeconds) * ratio;
        return Math.round(seconds);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
