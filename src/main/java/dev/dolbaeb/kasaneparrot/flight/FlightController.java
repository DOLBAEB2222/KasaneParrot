package dev.dolbaeb.kasaneparrot.flight;

import org.bukkit.entity.Parrot;

/**
 * Контроллер полёта отдельного попугая.
 *
 * <p>Контракт: {@link DeliveryService} (этап 2) создаёт контроллер на фазе
 * ({@code ASCENDING}/{@code DESCENDING}/{@code DEPARTING}) и дергает
 * {@link #tick()} каждый тик из своего глобального цикла.</p>
 *
 * <p>Планируемая реализация — {@code SplineFlightController}:
 * <ul>
 *   <li>траектория — {@link FlightPath} (Безье) либо спираль для взлёта;</li>
 *   <li>параметр t растёт по {@code speed / length} и сглаживается
 *       {@link dev.dolbaeb.kasaneparrot.util.Easing};</li>
 *   <li>позиция применяется через {@code parrot.teleport(...)} (Paper
 *       интерполирует перемещение для клиентов на 3 тика);</li>
 *   <li>yaw/pitch — из {@link FlightPath#derivative(double)}.</li>
 * </ul></p>
 */
public interface FlightController {

    /** Запуск (первый кадр полёта, звук/частицы). */
    void start();

    /**
     * Один кадр полёта.
     *
     * @return true, пока полёт продолжается; false — фаза завершена
     */
    boolean tick();

    /** Аварийная остановка (например, смерть попугая или /reload). */
    void cancel();

    /** Человекочитаемое описание фазы (для /kasane status и debug-лога). */
    String describe();

    /** Попугай, которым управляет контроллер. */
    Parrot parrot();
}
