package dev.dolbaeb.kasaneparrot.parrot;

/**
 * Состояния курьера в жизненном цикле доставки.
 *
 * <pre>
 * NORMAL ──(мешочек)──► POUCHED ──(подтверждение)──► ASCENDING
 *      ▲                                              │
 *      │                                        (высота 100, исчез)
 *      │                                              ▼
 * REUNITED ◄── RETURNING ◄── DEPARTING ◄── HANDOFF ◄── VANISHED
 *    │            ▲            (рамка)      (спуск)      │
 *    │            │                                       │ (таймер доставки)
 *    │            └──────────── RETURNING_FAILED ◄────────┤
 *    └──────────────────────────── (ошибка) ◄─────────────┘
 * </pre>
 */
public enum CourierState {
    /** Приручённый попугай без мешочка (ванильное поведение). */
    NORMAL,

    /** Мешочек надет; попугай ждёт отправки. Shift+ПКМ открывает меню. */
    POUCHED,

    /** Меню открыто, ждём подтверждения (транзитное состояние). */
    AWAITING_PAYLOAD,

    /** Взлёт на configured высоту (100 блоков по ТЗ). */
    ASCENDING,

    /** Попугай скрыт (setVisibleByDefault(false)), «в пути». */
    VANISHED,

    /** Появился над целью, плавно снижается. */
    DESCENDING,

    /** Предмет выложен в невидимую рамку, попугай рядом. */
    HANDOFF,

    /** Улетает вверх после выгрузки предмета. */
    DEPARTING,

    /** Телепорт к хозяину, спуск и посадка на плечо. */
    RETURNING,

    /** Доставка завершена, попугай на плече — обычный попугай с кастомной текстурой. */
    REUNITED,

    /** Ошибка доставки: возврат с предметом к хозяину. */
    RETURNING_FAILED;

    /** true, если попугай сейчас «в командировке» (занят доставкой). */
    public boolean isOnMission() {
        return this == ASCENDING || this == VANISHED || this == DESCENDING
                || this == HANDOFF || this == DEPARTING || this == RETURNING
                || this == RETURNING_FAILED;
    }
}
