package dev.dolbaeb.kasaneparrot.flight;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * Кубическая кривая Безье в 3D — основа «плавных движений» попугая.
 *
 * <p>Полёт (этап 2, SplineFlightController) двигает попугая вдоль
 * такой кривой с easing-параметризацией: точки {@link #point(double t)}
 * задают позицию, {@link #derivative(double t)} — направление полёта
 * (для yaw/pitch).</p>
 */
public final class FlightPath {

    private final Vector p0;
    private final Vector p1;
    private final Vector p2;
    private final Vector p3;

    private FlightPath(@NotNull Vector p0, @NotNull Vector p1,
                       @NotNull Vector p2, @NotNull Vector p3) {
        this.p0 = p0;
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
    }

    /** Прямая траектория: контрольные точки на 1/3 и 2/3 пути. */
    @NotNull
    public static FlightPath straight(@NotNull Location from, @NotNull Location to) {
        Vector a = from.toVector();
        Vector b = to.toVector();
        Vector third = b.clone().subtract(a).multiply(1.0D / 3.0D);
        return new FlightPath(
                a.clone(),
                a.clone().add(third),
                a.clone().add(third.clone().multiply(2.0D)),
                b.clone()
        );
    }

    /** Дугообразная траектория: середины приподняты на {@code bulge} блоков. */
    @NotNull
    public static FlightPath arc(@NotNull Location from, @NotNull Location to, double bulge) {
        Vector a = from.toVector();
        Vector b = to.toVector();
        Vector third = b.clone().subtract(a).multiply(1.0D / 3.0D);
        Vector up = new Vector(0, bulge, 0);
        return new FlightPath(
                a.clone(),
                a.clone().add(third).add(up),
                a.clone().add(third.clone().multiply(2.0D)).add(up),
                b.clone()
        );
    }

    /** Точка на кривой, t ∈ [0..1]. */
    @NotNull
    public Vector point(double t) {
        double u = 1.0D - t;
        double b0 = u * u * u;
        double b1 = 3.0D * u * u * t;
        double b2 = 3.0D * u * t * t;
        double b3 = t * t * t;
        return new Vector(
                p0.getX() * b0 + p1.getX() * b1 + p2.getX() * b2 + p3.getX() * b3,
                p0.getY() * b0 + p1.getY() * b1 + p2.getY() * b2 + p3.getY() * b3,
                p0.getZ() * b0 + p1.getZ() * b1 + p2.getZ() * b2 + p3.getZ() * b3
        );
    }

    /** Касательная (направление движения) в точке t. */
    @NotNull
    public Vector derivative(double t) {
        double u = 1.0D - t;
        double d0 = -3.0D * u * u;
        double d1 = 3.0D * u * u - 6.0D * u * t;
        double d2 = 6.0D * u * t - 3.0D * t * t;
        double d3 = 3.0D * t * t;
        Vector dir = new Vector(
                p1.getX() * d0 + p2.getX() * d1 + p3.getX() * d2 + (p3.getX() - p2.getX()) * d3,
                p1.getY() * d0 + p2.getY() * d1 + p3.getY() * d2 + (p3.getY() - p2.getY()) * d3,
                p1.getZ() * d0 + p2.getZ() * d1 + p3.getZ() * d2 + (p3.getZ() - p2.getZ()) * d3
        );
        return dir.lengthSquared() < 1.0E-8D ? new Vector(0, 0, 0) : dir.normalize();
    }

    /** Приблизительная длина кривой (для расчёта длительности полёта). */
    public double approximateLength(int segments) {
        double length = 0.0D;
        Vector prev = point(0.0D);
        for (int i = 1; i <= segments; i++) {
            Vector cur = point((double) i / segments);
            length += cur.distance(prev);
            prev = cur;
        }
        return length;
    }
}
