package dev.dolbaeb.kasaneparrot.gui;

import dev.dolbaeb.kasaneparrot.KasaneParrotPlugin;
import dev.dolbaeb.kasaneparrot.delivery.DeliveryDestination;
import dev.dolbaeb.kasaneparrot.parrot.CourierParrot;
import dev.dolbaeb.kasaneparrot.text.Msg;
import dev.dolbaeb.kasaneparrot.util.TimeFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemMeta;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.view.builder.InventoryViewBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Финальное подтверждение: что везём, кому и сколько это займёт.
 * Предмет берётся из главной руки в момент подтверждения.
 */
public final class ConfirmMenu extends AbstractCourierMenu {

    private static final int SLOT_CONFIRM = 11;
    private static final int SLOT_PAYLOAD = 13;
    private static final int SLOT_CANCEL = 15;

    private final CourierParrot courier;
    private final UUID targetId;
    private final String targetName;
    private final DeliveryDestination destination;
    private final ItemStack payloadSnapshot;

    public ConfirmMenu(@NotNull KasaneParrotPlugin plugin,
                       @NotNull Player viewer,
                       @NotNull CourierParrot courier,
                       @NotNull UUID targetId,
                       @NotNull String targetName,
                       @NotNull DeliveryDestination destination) {
        super(plugin, viewer);
        this.courier = courier;
        this.targetId = targetId;
        this.targetName = targetName;
        this.destination = destination;
        this.payloadSnapshot = viewer.getInventory().getItemInMainHand().clone();
    }

    @Override
    protected @NotNull MenuType.Typed<InventoryView, ? extends InventoryViewBuilder<InventoryView>> menuType() {
        return chestType(plugin.cfg().gui().confirm().rows);
    }

    @Override
    protected @NotNull Component renderTitle() {
        return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                .deserialize(plugin.cfg().gui().confirm().title);
    }

    @Override
    protected void render() {
        var top = top();
        top.clear();
        for (int i = 0; i < top.getSize(); i++) {
            top.setItem(i, filler());
        }

        long seconds = estimateSeconds();
        var icons = plugin.cfg().gui().confirm();

        // Посылка (снимок из руки).
        ItemStack payload = payloadSnapshot.clone();
        ItemMeta meta = payload.getItemMeta();
        meta.displayName(plugin.messages().get(Msg.GUI_PAYLOAD_NAME,
                Placeholder.component("item", payload.displayName() != null
                        ? payload.displayName()
                        : Component.text(payload.getType().name()))));
        List<Component> lore = new ArrayList<>(plugin.messages().getList(Msg.GUI_PAYLOAD_LORE));
        lore.add(Component.text("количество: " + payload.getAmount(),
                net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY));
        meta.lore(lore);
        payload.setItemMeta(meta);
        top.setItem(SLOT_PAYLOAD, payload);

        top.setItem(SLOT_CONFIRM, named(icons.confirmIcon,
                plugin.messages().get(Msg.GUI_CONFIRM_NAME),
                plugin.messages().getList(Msg.GUI_CONFIRM_LORE,
                        Placeholder.unparsed("target", targetName),
                        Placeholder.unparsed("time", TimeFormat.format(seconds)))));
        top.setItem(SLOT_CANCEL, named(icons.cancelIcon,
                plugin.messages().get(Msg.GUI_CANCEL_NAME),
                plugin.messages().getList(Msg.GUI_CANCEL_LORE)));
    }

    @Override
    protected void onClick(int slot, @NotNull InventoryClickEvent event) {
        if (slot == SLOT_CONFIRM) {
            confirm();
        } else if (slot == SLOT_CANCEL) {
            playClick();
            viewer.closeInventory();
        }
    }

    /* ------------------------------------------------------------------ */

    private void confirm() {
        ItemStack hand = viewer.getInventory().getItemInMainHand();
        if (hand.getType().isAir() || hand.getAmount() <= 0) {
            playError();
            plugin.messages().send(viewer, Msg.DELIVERY_PAYLOAD_REQUIRED);
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);
        boolean started = plugin.deliveryService()
                .startDelivery(viewer, courier, target, destination, hand);
        if (started) {
            playSuccess();
            viewer.closeInventory();
        } else {
            playError();
        }
    }

    private long estimateSeconds() {
        Location targetPoint = null;
        if (destination == DeliveryDestination.PLAYER) {
            Player online = Bukkit.getPlayer(targetId);
            targetPoint = online != null ? online.getLocation() : null;
        }
        if (targetPoint == null) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(targetId);
            targetPoint = offline.getRespawnLocation();
        }
        if (targetPoint == null) {
            return plugin.cfg().delivery().crossWorldSeconds;
        }
        return plugin.timeCalculator().deliverySeconds(viewer.getLocation(), targetPoint);
    }
}
