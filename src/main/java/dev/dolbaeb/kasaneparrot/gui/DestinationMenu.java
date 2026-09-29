package dev.dolbaeb.kasaneparrot.gui;

import dev.dolbaeb.kasaneparrot.KasaneParrotPlugin;
import dev.dolbaeb.kasaneparrot.delivery.DeliveryDestination;
import dev.dolbaeb.kasaneparrot.parrot.CourierParrot;
import dev.dolbaeb.kasaneparrot.text.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.view.builder.InventoryViewBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Меню выбора адреса: «к игроку» или «к точке спавна».
 * Пункт спавна показывается только при наличии точки возрождения (как в ТЗ).
 */
public final class DestinationMenu extends AbstractCourierMenu {

    private static final int SLOT_TO_PLAYER = 11;
    private static final int SLOT_TO_SPAWN = 15;

    private final CourierParrot courier;
    private final UUID targetId;
    private final String targetName;

    public DestinationMenu(@NotNull KasaneParrotPlugin plugin,
                           @NotNull Player viewer,
                           @NotNull CourierParrot courier,
                           @NotNull UUID targetId,
                           @NotNull String targetName) {
        super(plugin, viewer);
        this.courier = courier;
        this.targetId = targetId;
        this.targetName = targetName;
    }

    @Override
    protected @NotNull MenuType.Typed<InventoryView, ? extends InventoryViewBuilder<InventoryView>> menuType() {
        return chestType(plugin.cfg().gui().destination.rows);
    }

    @Override
    protected @NotNull Component renderTitle() {
        return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                .deserialize(plugin.cfg().gui().destination.title);
    }

    @Override
    protected void render() {
        var top = top();
        top.clear();
        for (int i = 0; i < top.getSize(); i++) {
            top.setItem(i, filler());
        }

        var icons = plugin.cfg().gui().destination;
        top.setItem(SLOT_TO_PLAYER, named(icons.toPlayerIcon,
                plugin.messages().get(Msg.GUI_DESTINATION_PLAYER_NAME),
                plugin.messages().getList(Msg.GUI_DESTINATION_PLAYER_LORE)));

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);
        if (target.getRespawnLocation() != null) {
            top.setItem(SLOT_TO_SPAWN, named(icons.toSpawnIcon,
                    plugin.messages().get(Msg.GUI_DESTINATION_SPAWN_NAME),
                    plugin.messages().getList(Msg.GUI_DESTINATION_SPAWN_LORE)));
        }
    }

    @Override
    protected void onClick(int slot, @NotNull InventoryClickEvent event) {
        if (slot == SLOT_TO_PLAYER) {
            playClick();
            plugin.menuService().openConfirmMenu(viewer, courier, targetId, targetName,
                    DeliveryDestination.PLAYER);
        } else if (slot == SLOT_TO_SPAWN) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);
            if (target.getRespawnLocation() == null) {
                // Точка спавна пропала между открытием меню и кликом.
                playError();
                plugin.messages().send(viewer, Msg.GUI_DESTINATION_SPAWN_UNAVAILABLE);
                return;
            }
            playClick();
            plugin.menuService().openConfirmMenu(viewer, courier, targetId, targetName,
                    DeliveryDestination.SPAWN_POINT);
        }
    }
}
