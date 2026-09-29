package dev.dolbaeb.kasaneparrot.module;

/**
 * Модуль плагина — единица жизненного цикла.
 *
 * <p>Все крупные подсистемы KasaneParrot ({@code ParrotModelService},
 * {@code DeliveryService}, {@code MenuService}, ...) реализуют этот интерфейс
 * и регистрируются в {@link ModuleManager}. Это даёт единый порядок
 * включения/выключения и точечную перезагрузку по {@code /kasane reload}.</p>
 */
public interface PluginModule {

    /** Короткий идентификатор модуля (для логов и отладки). */
    String id();

    /** Вызывается один раз при запуске плагина (после загрузки конфига). */
    default void onEnable() {
    }

    /** Вызывается при выключении плагина; обязан освободить ресурсы и задачи. */
    default void onDisable() {
    }

    /** Вызывается при {@code /kasane reload}; модуль обязан перечитать настройки. */
    default void onReload() {
    }
}
