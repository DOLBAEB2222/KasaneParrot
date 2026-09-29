package dev.dolbaeb.kasaneparrot.delivery;

/**
 * Итог доставки.
 */
public enum DeliveryOutcome {
    DELIVERED("посылка доставлена"),

    /** Предмет возвращён отправителю (цель вышла). */
    RETURNED_TO_SENDER("получатель вышел с сервера"),

    /** Не нашли безопасное место посадки. */
    FAILED_NO_LANDING("не нашли место для посадки"),

    /** Цель слишком долго не останавливалась. */
    FAILED_TIMEOUT("получатель не остановился"),

    /** Попугай умер/исчез во время миссии. */
    FAILED_PARROT_LOST("попугай потерян в полёте"),

    /** Отправка отменена (событием/выключением плагина). */
    CANCELLED("отправка отменена"),

    /** Ещё в полёте. */
    PENDING("в полёте");

    private final String humanLabel;

    DeliveryOutcome(String humanLabel) {
        this.humanLabel = humanLabel;
    }

    public String label() {
        return humanLabel;
    }

    public boolean isTerminal() {
        return this != PENDING;
    }
}
