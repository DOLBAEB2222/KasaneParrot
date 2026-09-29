package dev.dolbaeb.kasaneparrot.command;

import dev.dolbaeb.kasaneparrot.KasaneParrotPlugin;
import dev.dolbaeb.kasaneparrot.delivery.DeliverySession;
import dev.dolbaeb.kasaneparrot.text.Msg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

/**
 * /kasane — управление плагином: reload | status | pack | help.
 */
public final class KasaneCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUB_COMMANDS = List.of("help", "reload", "status", "pack");

    private final KasaneParrotPlugin plugin;

    public KasaneCommand(@NotNull KasaneParrotPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> reload(sender);
            case "status" -> status(sender);
            case "pack" -> pack(sender);
            default -> help(sender);
        }
        return true;
    }

    private void reload(@NotNull CommandSender sender) {
        if (!sender.hasPermission("kasaneparrot.admin")) {
            plugin.messages().send(sender, Msg.GENERIC_NO_PERMISSION);
            return;
        }
        long start = System.currentTimeMillis();
        plugin.reloadAll();
        plugin.messages().send(sender, Msg.GENERIC_RELOADED,
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed(
                        "ms", String.valueOf(System.currentTimeMillis() - start)));
    }

    private void status(@NotNull CommandSender sender) {
        if (!sender.hasPermission("kasaneparrot.admin")) {
            plugin.messages().send(sender, Msg.GENERIC_NO_PERMISSION);
            return;
        }
        sender.sendMessage(Component.text("=== KasaneParrot ===", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text(
                "Курьеров зарегистрировано: " + plugin.registry().all().size(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text(
                "Активных доставок: " + plugin.deliveryService().sessions().size(), NamedTextColor.GRAY));
        for (DeliverySession session : plugin.deliveryService().sessions()) {
            sender.sendMessage(Component.text(String.format(
                    " • %s → %s [%s] %s",
                    session.request().senderName(),
                    session.request().targetName(),
                    session.state(),
                    session.outcome().label()
            ), NamedTextColor.DARK_GRAY));
        }
    }

    private void pack(@NotNull CommandSender sender) {
        if (!sender.hasPermission("kasaneparrot.admin")) {
            plugin.messages().send(sender, Msg.GENERIC_NO_PERMISSION);
            return;
        }
        var pack = plugin.cfg().resourcePack();
        sender.sendMessage(Component.text("=== Ресурспак ===", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("Режим: " + pack.mode, NamedTextColor.GRAY));
        sender.sendMessage(Component.text("URL: " + (pack.url.isBlank() ? "<не задан>" : pack.url),
                NamedTextColor.GRAY));
        sender.sendMessage(Component.text(
                "Файл «Pesky Parrots.zip» в корне репозитория нужно разместить по URL и "
                        + "прописать его в resource-pack.url (pack_format требует обновления до 1.21.8).",
                NamedTextColor.DARK_GRAY));
    }

    private void help(@NotNull CommandSender sender) {
        sender.sendMessage(Component.text("=== KasaneParrot ===", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("/kasane help — эта справка", NamedTextColor.GRAY));
        if (sender.hasPermission("kasaneparrot.admin")) {
            sender.sendMessage(Component.text("/kasane reload — перезагрузить конфиг", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("/kasane status — статистика доставок", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("/kasane pack — информация о ресурспаке", NamedTextColor.GRAY));
        }
        sender.sendMessage(Component.text(
                "Приручите попугая семечками, Shift+ПКМ мешочком (Bundle) — и отправляйте посылки.",
                NamedTextColor.DARK_GRAY));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      @NotNull String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return SUB_COMMANDS.stream()
                    .filter(s -> s.startsWith(prefix))
                    .toList();
        }
        return List.of();
    }
}
