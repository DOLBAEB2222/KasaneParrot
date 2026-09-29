package dev.dolbaeb.kasaneparrot.listener;

import dev.dolbaeb.kasaneparrot.KasaneParrotPlugin;
import dev.dolbaeb.kasaneparrot.delivery.DeliveryOutcome;
import dev.dolbaeb.kasaneparrot.parrot.CourierParrot;
import dev.dolbaeb.kasaneparrot.text.Msg;
import org.bukkit.entity.Parrot;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Защита курьеров: урон в миссии, смерть (груз возвращается),
 * порталы и деспавн.
 */
public final class CourierProtectionListener implements Listener {

    private final KasaneParrotPlugin plugin;

    public CourierProtectionListener(@NotNull KasaneParrotPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(@NotNull EntityDamageEvent event) {
        CourierParrot courier = courierOf(event.getEntity());
        if (courier == null) {
            return;
        }
        if (courier.state().isOnMission() && plugin.cfg().parrot().invulnerableWhileDelivering) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(@NotNull EntityDeathEvent event) {
        CourierParrot courier = courierOf(event.getEntity());
        if (courier == null) {
            return;
        }
        if (courier.session() != null) {
            plugin.deliveryService().fail(
                    courier,
                    courier.session(),
                    DeliveryOutcome.FAILED_PARROT_LOST,
                    plugin.messages().get(Msg.DELIVERY_REASON_PARROT_LOST)
            );
        }
        plugin.registry().unregister(courier.parrotId());
        plugin.keys().clearFlag(event.getEntity(), plugin.keys().pouched);
        plugin.keys().setUuid(event.getEntity(), plugin.keys().owner, null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPortal(@NotNull EntityPortalEvent event) {
        CourierParrot courier = courierOf(event.getEntity());
        if (courier != null && plugin.cfg().parrot().blockPortals) {
            event.setCancelled(true);
        }
    }

    // Примечание: io.papermc.paper.event.entity.EntityRemoveEvent отсутствует
    // в paper-api 1.21.8 — защита от деспавна обеспечивается тем, что
    // приручённые попугаи persistent по умолчанию (+ setAI(false) в полёте).

    @Nullable
    private CourierParrot courierOf(@NotNull org.bukkit.entity.Entity entity) {
        return entity instanceof Parrot ? plugin.registry().byEntity((Parrot) entity) : null;
    }
}
