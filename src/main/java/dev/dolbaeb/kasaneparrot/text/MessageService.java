package dev.dolbaeb.kasaneparrot.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Сервис сообщений: MiniMessage-шаблоны из messages_&lt;lang&gt;.yml.
 *
 * <p>Все строки кэшируются при загрузке — обращение к сообщению не парсит YAML.
 * Плейсхолдеры передаются через {@link TagResolver} (см. {@code Placeholder.unparsed(...)}).</p>
 */
public final class MessageService {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final JavaPlugin plugin;
    private final String language;
    private final Map<Msg, String> cache = new EnumMap<>(Msg.class);
    private final Map<Msg, List<String>> listCache = new EnumMap<>(Msg.class);

    public MessageService(@NotNull JavaPlugin plugin, @NotNull String language) {
        this.plugin = plugin;
        this.language = language;
        reload();
    }

    /** Перечитывает файл сообщений с диска. */
    public void reload() {
        cache.clear();
        listCache.clear();

        String fileName = "messages_" + language + ".yml";
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        for (Msg msg : Msg.values()) {
            String value = yaml.getString(msg.path());
            if (value != null) {
                cache.put(msg, value);
            }
            List<String> list = yaml.getStringList(msg.path());
            if (!list.isEmpty()) {
                listCache.put(msg, list);
            }
        }
        plugin.getLogger().info("Загружено сообщений: " + cache.size() + " (" + fileName + ")");
    }

    /** Сырая строка шаблона (без парсинга). */
    @NotNull
    public String raw(@NotNull Msg key) {
        return cache.getOrDefault(key, "<red>missing: " + key.path() + "</red>");
    }

    /** Компонент из шаблона. */
    @NotNull
    public Component get(@NotNull Msg key, @NotNull TagResolver... resolvers) {
        return MINI.deserialize(raw(key), resolvers);
    }

    /** Список компонентов (lore и т.п.). */
    @NotNull
    public List<Component> getList(@NotNull Msg key, @NotNull TagResolver... resolvers) {
        List<String> raw = listCache.get(key);
        List<Component> out = new ArrayList<>(raw == null ? 1 : raw.size());
        if (raw != null) {
            for (String line : raw) {
                out.add(MINI.deserialize(line, resolvers));
            }
        }
        return out;
    }

    /** Компонент с префиксом плагина. */
    @NotNull
    public Component prefixed(@NotNull Msg key, @NotNull TagResolver... resolvers) {
        return Component.empty()
                .append(MINI.deserialize(raw(Msg.PREFIX)))
                .append(get(key, resolvers));
    }

    /** Отправляет сообщение с префиксом. */
    public void send(@NotNull CommandSender to, @NotNull Msg key, @NotNull TagResolver... resolvers) {
        to.sendMessage(prefixed(key, resolvers));
    }
}
