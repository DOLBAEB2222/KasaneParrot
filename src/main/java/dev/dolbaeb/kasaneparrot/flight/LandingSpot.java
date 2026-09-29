package dev.dolbaeb.kasaneparrot.flight;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Найденное место посадки: точка, качество и способ, которым туда попадём.
 */
public record LandingSpot(@NotNull Location location, double score, @NotNull Strategy strategy) {

    /** Как именно попугай окажется на месте. */
    public enum Strategy {
        /** Прямо перед игроком (по направлению взгляда). */
        IN_FRONT,
        /** Ближайшая безопасная площадка рядом с игроком. */
        NEARBY_GROUND,
        /** Лететь рядом с движущейся целью до её остановки. */
        HOVER_FOLLOW
    }
}
