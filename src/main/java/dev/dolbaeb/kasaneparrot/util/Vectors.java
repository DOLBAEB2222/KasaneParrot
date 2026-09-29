package dev.dolbaeb.kasaneparrot.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * Векторная математика для полётного контроллера.
 */
public final class Vectors {

    private Vectors() {
    }

    /** Расстояние между точками; для разных миров — {@link Double#POSITIVE_INFINITY}. */
    public static double distance(@NotNull Location a, @NotNull Location b) {
        if (a.getWorld() != null && b.getWorld() != null && !a.getWorld().equals(b.getWorld())) {
            return Double.POSITIVE_INFINITY;
        }
        return a.distance(b);
    }

    /** Линейная интерполяция векторов. */
    @NotNull
    public static Vector lerp(@NotNull Vector from, @NotNull Vector to, double t) {
        return from.clone().multiply(1.0D - t).add(to.clone().multiply(t));
    }

    /** Линейная интерполяция точек (мир берётся из {@code from}). */
    @NotNull
    public static Location lerp(@NotNull Location from, @NotNull Location to, double t) {
        World world = from.getWorld() != null ? from.getWorld() : to.getWorld();
        Vector pos = lerp(from.toVector(), to.toVector(), t);
        return pos.toLocation(world);
    }

    /**
     * Упреждение: точка, где окажется движущаяся цель через {@code seconds},
     * если сохранит текущую скорость (в блоках/тик).
     */
    @NotNull
    public static Vector lead(@NotNull Vector position, @NotNull Vector velocityPerTick, double seconds) {
        return position.clone().add(velocityPerTick.clone().multiply(seconds * 20.0D));
    }

    /** Направление из точки {@code from} в {@code to} (нормированное). */
    @NotNull
    public static Vector direction(@NotNull Vector from, @NotNull Vector to) {
        Vector dir = to.clone().subtract(from);
        return dir.lengthSquared() < 1.0E-6D ? new Vector(0, 0, 0) : dir.normalize();
    }

    /** Скорость (блоков/тик) по вектору скорости. */
    public static double speed(@NotNull Vector velocity) {
        return velocity.length();
    }
}
