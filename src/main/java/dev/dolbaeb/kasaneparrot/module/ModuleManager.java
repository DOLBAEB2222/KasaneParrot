package dev.dolbaeb.kasaneparrot.module;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Реестр модулей: единая точка управления жизненным циклом подсистем плагина.
 *
 * <p>Порядок регистрации = порядок включения и обратный порядок выключения.</p>
 */
public final class ModuleManager {

    private final Plugin plugin;
    private final Map<String, PluginModule> modules = new LinkedHashMap<>();

    public ModuleManager(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    /** Регистрирует модуль (до {@link #enableAll()}). */
    public void register(@NotNull PluginModule module) {
        modules.put(module.id(), module);
    }

    public Collection<PluginModule> modules() {
        return Collections.unmodifiableCollection(modules.values());
    }

    public void enableAll() {
        for (PluginModule module : modules.values()) {
            try {
                module.onEnable();
                plugin.getLogger().info("Модуль включён: " + module.id());
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Не удалось включить модуль " + module.id(), e);
            }
        }
    }

    public void disableAll() {
        // Выключаем в обратном порядке: зависимые модули раньше зависимостей.
        var it = modules.values().toArray(new PluginModule[0]);
        for (int i = it.length - 1; i >= 0; i--) {
            PluginModule module = it[i];
            try {
                module.onDisable();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Ошибка выключения модуля " + module.id(), e);
            }
        }
    }

    public void reloadAll() {
        for (PluginModule module : modules.values()) {
            try {
                module.onReload();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Ошибка перезагрузки модуля " + module.id(), e);
            }
        }
    }
}
