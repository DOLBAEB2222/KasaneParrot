package dev.dolbaeb.kasaneparrot.delivery;

/**
 * Куда доставлять посылку.
 *
 * <p>В меню выбора адреса пункт «к точке спавна» показывается только если
 * у получателя есть точка возрождения (кровать/якорь) — как в ТЗ.</p>
 */
public enum DeliveryDestination {
    /** Прямо к игроку, где бы он ни находился. */
    PLAYER,

    /** К точке возрождения игрока (bed/respawn anchor). */
    SPAWN_POINT
}
