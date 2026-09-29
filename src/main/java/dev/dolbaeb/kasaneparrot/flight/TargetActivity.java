package dev.dolbaeb.kasaneparrot.flight;

/**
 * Чем сейчас занят игрок-получатель — определяет стратегию посадки.
 */
public enum TargetActivity {
    /** Стоит на месте. */
    IDLE,
    /** Спокойно идёт. */
    WALKING,
    /** Крадётся. */
    SNEAKING,
    /** Бежит (спринт). */
    SPRINTING,
    /** Плавает. */
    SWIMMING,
    /** Летит на элитрах. */
    GLIDING,
    /** В полёте (креатив/наблюдатель). */
    FLYING,
    /** Едет/плывёт в транспорте или на entity. */
    IN_VEHICLE,
    /** Спит. */
    SLEEPING,
    /** Мёртв. */
    DEAD,
    /** Не удалось определить. */
    UNKNOWN;

    /** true, если цель перемещается. */
    public boolean isMoving() {
        return this == WALKING || this == SNEAKING || this == SPRINTING
                || this == SWIMMING || this == GLIDING || this == FLYING
                || this == IN_VEHICLE;
    }

    /**
     * true, если посадка на землю рядом с целью невозможна/бессмысленна —
     * попугаю следует лететь рядом и ждать остановки.
     */
    public boolean needsHoverFollow() {
        return this == GLIDING || this == FLYING || this == IN_VEHICLE || this == SWIMMING;
    }

    /** Человекочитаемая метка (для отладки и сообщений). */
    public String label() {
        return switch (this) {
            case IDLE -> "стоит";
            case WALKING -> "идёт";
            case SNEAKING -> "крадётся";
            case SPRINTING -> "бежит";
            case SWIMMING -> "плывёт";
            case GLIDING -> "летит на элитрах";
            case FLYING -> "летит";
            case IN_VEHICLE -> "в транспорте";
            case SLEEPING -> "спит";
            case DEAD -> "мёртв";
            case UNKNOWN -> "неизвестно";
        };
    }
}
