package dev.dolbaeb.kasaneparrot.gui;

import dev.dolbaeb.kasaneparrot.KasaneParrotPlugin;
import dev.dolbaeb.kasaneparrot.parrot.CourierParrot;
import dev.dolbaeb.kasaneparrot.text.Msg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemMeta;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.view.builder.InventoryViewBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Меню выбора получателя: головы онлайн-игроков, пагинация, живое
 * обновление при входе/выходе игроков (через {@code MenuService}).
 */
public final class PlayerSelectMenu extends AbstractCourierMenu {

    private final CourierParrot courier;
    private final Map<Integer, UUID> slotToTarget = new HashMap<>();
    private int page;

    public PlayerSelectMenu(@NotNull KasaneParrotPlugin plugin,
                            @NotNull Player viewer,
                            @NotNull CourierParrot courier,
                            int page) {
        super(plugin, viewer);
        this.courier = courier;
        this.page = page;
    }

    public @NotNull CourierParrot courier() {
        return courier;
    }

    @Override
    protected @NotNull MenuType.Typed<InventoryView, ? extends InventoryViewBuilder<InventoryView>> menuType() {
        return chestType(plugin.cfg().gui().select().rows);
    }

    @Override
    protected @NotNull Component renderTitle() {
        int online = targets().size();
        return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                plugin.cfg().gui().select().title + " <gray>(онлайн: " + online + ")</gray>"
        );
    }

    @Override
    protected void render() {
        Inventory top = top();
        top.clear();
        slotToTarget.clear();

        List<Player> players = targets();
        int contentSlots = contentSlots();
        int pages = Math.max(1, ceilDiv(players.size(), contentSlots));
        page = Math.max(0, Math.min(page, pages - 1));
        int start = page * contentSlots;

        for (int i = 0; i < contentSlots && start + i < players.size(); i++) {
            Player target = players.get(start + i);
            top.setItem(i, headItem(target));
            slotToTarget.put(i, target.getUniqueId());
        }

        // Нижний ряд: пагинация и инфо (если строк больше одной).
        if (top.getSize() >= 18) {
            int base = top.getSize() - 9;
            for (int i = base; i < top.getSize(); i++) {
                top.setItem(i, filler());
            }
            var icons = plugin.cfg().gui().select();
            if (page > 0) {
                top.setItem(base, named(icons.prevPageIcon,
                        plugin.messages().get(Msg.GUI_PREV_PAGE), null));
            }
            top.setItem(base + 4, named(icons.infoIcon,
                    plugin.messages().get(Msg.GUI_SELECT_INFO_NAME,
                            Placeholder.unparsed("online", String.valueOf(players.size()))),
                    plugin.messages().getList(Msg.GUI_SELECT_INFO_LORE,
                            Placeholder.unparsed("page", String.valueOf(page + 1)),
                            Placeholder.unparsed("pages", String.valueOf(pages)))));
            if (page < pages - 1) {
                top.setItem(base + 8, named(icons.nextPageIcon,
                        plugin.messages().get(Msg.GUI_NEXT_PAGE), null));
            }
        }
    }

    @Override
    protected void onClick(int slot, @NotNull InventoryClickEvent event) {
        UUID targetId = slotToTarget.get(slot);
        if (targetId != null) {
            playClick();
            String name = String.valueOf(Bukkit.getOfflinePlayer(targetId).getName());
            plugin.menuService().openDestinationMenu(viewer, courier, targetId, name);
            return;
        }
        if (top().getSize() < 18) {
            return;
        }
        int base = top().getSize() - 9;
        if (slot == base && page > 0) {
            page--;
            playClick();
            refresh();
        } else if (slot == base + 8) {
            page++;
            playClick();
            refresh();
        }
    }

    /* ------------------------------------------------------------------ */

    private @NotNull List<Player> targets() {
        boolean showSelf = plugin.cfg().gui().select().showSelf;
        return Bukkit.getOnlinePlayers().stream()
                .filter(p -> showSelf || !p.equals(viewer))
                .sorted(Comparator.comparing(Player::getName))
                .toList();
    }

    private int contentSlots() {
        int size = top().getSize();
        // Одна строка: без ряда пагинации; иначе последняя строка — служебная.
        return size < 18 ? size : size - 9;
    }

    private @NotNull ItemStack headItem(@NotNull Player target) {
        ItemStack head = plugin.headCache().head(target);
        ItemMeta meta = head.getItemMeta();
        meta.displayName(playerName(target.getName()));
        meta.lore(List.of(
                Component.text("онлайн", NamedTextColor.GREEN),
                Component.text("нажмите, чтобы выбрать", NamedTextColor.DARK_GRAY)
        ));
        head.setItemMeta(meta);
        return head;
    }

    private static int ceilDiv(int value, int div) {
        return (value + div - 1) / div;
    }
}
