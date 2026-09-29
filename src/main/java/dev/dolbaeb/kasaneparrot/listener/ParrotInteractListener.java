package dev.dolbaeb.kasaneparrot.listener;

import dev.dolbaeb.kasaneparrot.KasaneParrotPlugin;
import dev.dolbaeb.kasaneparrot.event.ParrotPouchedEvent;
import dev.dolbaeb.kasaneparrot.parrot.CourierParrot;
import dev.dolbaeb.kasaneparrot.parrot.CourierState;
import dev.dolbaeb.kasaneparrot.text.Msg;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Взаимодействие игрока с попугаем:
 *
 * <ul>
 *   <li><b>Shift+ПКМ мешочком (Bundle)</b> по своему приручённому попугаю —
 *       надеть мешочек (один раз и навсегда, модель меняется), затем меню;</li>
 *   <li><b>Shift+ПКМ</b> по попугаю-курьеру — меню отправки;</li>
 *   <li><b>ПКМ с предметом в руке</b> по курьеру — меню отправки (по ТЗ);</li>
 *   <li>остальные клики — ванильное поведение (сесть/встать, корм).</li>
 * </ul>
 */
public final class ParrotInteractListener implements Listener {

    private final KasaneParrotPlugin plugin;

    public ParrotInteractListener(@NotNull KasaneParrotPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Parrot parrot)) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return; // обрабатываем только главную руку (событие приходит дважды)
        }
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (player.isSneaking()) {
            // 1) Надевание мешочка.
            if (hand.getType() == plugin.cfg().pouch().material && !isCourier(parrot)) {
                event.setCancelled(true);
                attachPouch(player, parrot);
                return;
            }
            // 2) Меню отправки для курьера.
            CourierParrot courier = resolveCourier(parrot);
            if (courier != null && courier.state() == CourierState.POUCHED) {
                event.setCancelled(true);
                // Если попугай сидит на плече — снимаем его в мир.
                releaseIfPerched(player, courier);
                plugin.menuService().openPlayerSelect(player, courier);
                return;
            }
        } else {
            // 3) ПКМ с предметом по курьеру → меню (по ТЗ).
            CourierParrot courier = resolveCourier(parrot);
            if (courier != null && courier.state() == CourierState.POUCHED
                    && !hand.getType().isAir()) {
                event.setCancelled(true);
                plugin.menuService().openPlayerSelect(player, courier);
                return;
            }
        }
        // Иначе — ваниль (присесть/встать, покормить).
    }

    /* ------------------------------------------------------------------ */

    private void attachPouch(@NotNull Player player, @NotNull Parrot parrot) {
        if (!player.hasPermission("kasaneparrot.use")) {
            plugin.messages().send(player, Msg.GENERIC_NO_PERMISSION);
            return;
        }
        if (!parrot.isTamed()) {
            plugin.messages().send(player, Msg.PARROT_NOT_TAMED);
            return;
        }
        UUID owner = parrot.getOwnerUniqueId();
        if (plugin.cfg().pouch().requireOwner && (owner == null || !owner.equals(player.getUniqueId()))) {
            plugin.messages().send(player, Msg.PARROT_NOT_YOUR_PARROT);
            return;
        }
        if (plugin.keys().flag(parrot, plugin.keys().pouched)) {
            plugin.messages().send(player, Msg.PARROT_ALREADY_COURIER);
            return;
        }

        ParrotPouchedEvent pouchEvent = new ParrotPouchedEvent(player, parrot);
        plugin.getServer().getPluginManager().callEvent(pouchEvent);
        if (pouchEvent.isCancelled()) {
            return;
        }

        // Расходуем мешочек.
        if (plugin.cfg().pouch().consumeOnAttach) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            hand.setAmount(hand.getAmount() - 1);
        }

        UUID ownerId = owner != null ? owner : player.getUniqueId();
        plugin.keys().setFlag(parrot, plugin.keys().pouched);
        plugin.keys().setUuid(parrot, plugin.keys().owner, ownerId);

        CourierParrot courier = new CourierParrot(parrot, ownerId);
        courier.state(CourierState.POUCHED);
        plugin.registry().register(courier);
        plugin.modelService().applyCourierModel(parrot, player);

        plugin.messages().send(player, Msg.PARROT_POUCHED,
                Placeholder.unparsed("name", parrot.customName() != null
                        ? net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                                .plainText().serialize(parrot.customName())
                        : "попугай"));

        if (plugin.cfg().pouch().openMenuAfterAttach) {
            plugin.menuService().openPlayerSelect(player, courier);
        }
    }

    /* ------------------------------------------------------------------ */

    private boolean isCourier(@NotNull Parrot parrot) {
        return plugin.registry().byEntity(parrot) != null
                || plugin.keys().flag(parrot, plugin.keys().pouched);
    }

    /**
     * Курьер по сущности с ленивым восстановлением после рестарта:
     * PDC-флаг есть, а в реестре записи нет — пересоздаём.
     */
    @Nullable
    private CourierParrot resolveCourier(@NotNull Parrot parrot) {
        CourierParrot courier = plugin.registry().byEntity(parrot);
        if (courier != null) {
            courier.entity(parrot);
            return courier;
        }
        if (!plugin.keys().flag(parrot, plugin.keys().pouched)) {
            return null;
        }
        UUID owner = plugin.keys().getUuid(parrot, plugin.keys().owner);
        if (owner == null) {
            return null;
        }
        courier = new CourierParrot(parrot, owner);
        courier.state(CourierState.POUCHED);
        plugin.registry().register(courier);
        return courier;
    }

    private void releaseIfPerched(@NotNull Player player, @NotNull CourierParrot courier) {
        if (!courier.isAlive() && plugin.shoulderService().isPerched(player, courier.parrotId())) {
            Parrot released = plugin.shoulderService().release(player, courier.parrotId());
            if (released != null) {
                courier.entity(released);
            }
        }
    }
}
