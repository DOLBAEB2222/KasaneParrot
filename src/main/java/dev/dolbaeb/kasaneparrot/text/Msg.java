package dev.dolbaeb.kasaneparrot.text;

/**
 * Ключи сообщений. Структура повторяет messages_ru.yml.
 */
public enum Msg {
    PREFIX("prefix"),

    GENERIC_NO_PERMISSION("generic.no-permission"),
    GENERIC_PLAYERS_ONLY("generic.players-only"),
    GENERIC_UNKNOWN_PLAYER("generic.unknown-player"),
    GENERIC_RELOADED("generic.reloaded"),

    PARROT_NOT_A_PARROT("parrot.not-a-parrot"),
    PARROT_NOT_TAMED("parrot.not-tamed"),
    PARROT_NOT_YOUR_PARROT("parrot.not-your-parrot"),
    PARROT_ALREADY_COURIER("parrot.already-courier"),
    PARROT_POUCHED("parrot.pouched"),
    PARROT_COURIER_NAME("parrot.courier-name"),

    POUCH_WRONG_ITEM("pouch.wrong-item"),
    POUCH_CANT_AFFORD("pouch.cant-afford"),

    DELIVERY_PAYLOAD_REQUIRED("delivery.payload-required"),
    DELIVERY_PAYLOAD_BLACKLISTED("delivery.payload-blacklisted"),
    DELIVERY_STARTED("delivery.started"),
    DELIVERY_TARGET_IS_SELF("delivery.target-is-self"),
    DELIVERY_TARGET_GONE("delivery.target-gone"),
    DELIVERY_ACTION_RESPAWN("delivery.action-respawn"),
    DELIVERY_ACTION_RETURN("delivery.action-return"),
    DELIVERY_ACTION_WAIT("delivery.action-wait"),
    DELIVERY_ARRIVING("delivery.arriving"),
    DELIVERY_DELIVERED("delivery.delivered"),
    DELIVERY_FAILED("delivery.failed"),
    DELIVERY_RETURNED("delivery.returned"),
    DELIVERY_REASON_NO_LANDING("delivery.reason-no-landing"),
    DELIVERY_REASON_TIMEOUT("delivery.reason-timeout"),
    DELIVERY_REASON_PARROT_LOST("delivery.reason-parrot-lost"),
    DELIVERY_REASON_CANCELLED("delivery.reason-cancelled"),

    GUI_SELECT_INFO_NAME("gui.select-info-name"),
    GUI_SELECT_INFO_LORE("gui.select-info-lore"),
    GUI_PREV_PAGE("gui.prev-page"),
    GUI_NEXT_PAGE("gui.next-page"),
    GUI_DESTINATION_PLAYER_NAME("gui.destination-player-name"),
    GUI_DESTINATION_PLAYER_LORE("gui.destination-player-lore"),
    GUI_DESTINATION_SPAWN_NAME("gui.destination-spawn-name"),
    GUI_DESTINATION_SPAWN_LORE("gui.destination-spawn-lore"),
    GUI_DESTINATION_SPAWN_UNAVAILABLE("gui.destination-spawn-unavailable"),
    GUI_CONFIRM_NAME("gui.confirm-name"),
    GUI_CONFIRM_LORE("gui.confirm-lore"),
    GUI_CANCEL_NAME("gui.cancel-name"),
    GUI_CANCEL_LORE("gui.cancel-lore"),
    GUI_PAYLOAD_NAME("gui.payload-name"),
    GUI_PAYLOAD_LORE("gui.payload-lore"),
    GUI_TARGET_NONE("gui.target-none");

    private final String path;

    Msg(String path) {
        this.path = path;
    }

    /** Путь в YAML-файле сообщений. */
    public String path() {
        return path;
    }
}
