package dev.dolbaeb.kasaneparrot.util;

import org.jetbrains.annotations.NotNull;

/**
 * Функции плавности (easing) для полёта попугая.
 *
 * <p>Полёт строится по параметру {@code t ∈ [0..1]} вдоль траектории;
 * easing задаёт закон разгона и торможения, чтобы движения выглядели
 * «плавными», а не линейно-роботизированными.</p>
 */
@FunctionalInterface
public interface Easing {

    /** @param t параметр [0..1] (значения вне диапазона клампятся). */
    double apply(double t);

    static double clamp01(double t) {
        return t < 0.0D ? 0.0D : Math.min(t, 1.0D);
    }

    static Easing linear() {
        return t -> clamp01(t);
    }

    static Easing easeInQuad() {
        return t -> {
            double x = clamp01(t);
            return x * x;
        };
    }

    static Easing easeOutQuad() {
        return t -> {
            double x = clamp01(t);
            return 1.0D - (1.0D - x) * (1.0D - x);
        };
    }

    /** Медленный разгон, плавное торможение — базовый режим полёта. */
    static Easing easeInOutCubic() {
        return t -> {
            double x = clamp01(t);
            return x < 0.5D
                    ? 4.0D * x * x * x
                    : 1.0D - Math.pow(-2.0D * x + 2.0D, 3.0D) / 2.0D;
        };
    }

    static Easing smoothstep() {
        return t -> {
            double x = clamp01(t);
            return x * x * (3.0D - 2.0D * x);
        };
    }

    /** Разбирает имя из конфига, неизвестное имя → {@link #easeInOutCubic()}. */
    @NotNull
    static Easing fromName(@NotNull String name) {
        return switch (name.toUpperCase(java.util.Locale.ROOT)) {
            case "LINEAR" -> linear();
            case "EASE_IN_QUAD" -> easeInQuad();
            case "EASE_OUT_QUAD" -> easeOutQuad();
            case "SMOOTHSTEP" -> smoothstep();
            default -> easeInOutCubic();
        };
    }
}
