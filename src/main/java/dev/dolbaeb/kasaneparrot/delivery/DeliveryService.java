package dev.dolbaeb.kasaneparrot.delivery;

import dev.dolbaeb.kasaneparrot.KasaneParrotPlugin;
import dev.dolbaeb.kasaneparrot.event.DeliveryCompletedEvent;
import dev.dolbaeb.kasaneparrot.event.DeliveryFailedEvent;
import dev.dolbaeb.kasaneparrot.event.DeliveryStartedEvent;
import dev.dolbaeb.kasaneparrot.flight.LandingSpot;
import dev.dolbaeb.kasaneparrot.flight.LandingSpotFinder;
import dev.dolbaeb.kasaneparrot.module.PluginModule;
import dev.dolbaeb.kasaneparrot.parrot.CourierParrot;
import dev.dolbaeb.kasaneparrot.parrot.CourierRegistry;
import dev.dolbaeb.kasaneparrot.parrot.CourierState;
import dev.dolbaeb.kasaneparrot.text.Msg;
import dev.dolbaeb.kasaneparrot.util.TimeFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Оркестратор доставки: глобальный цикл + машина состояний.
 *
 * <p>Скелет (этап 1) переводит фазы по времени: длительности взлёта/спуска
 * вычисляются из скорости и высоты конфига. Плавные траектории
 * (спираль взлёта, сплайн снижения, hover-follow за бегущей целью)
 * подключаются на этапе 2 через {@code FlightController} без изменения
 * этого класса — точки входа фаз помечены TODO.</p>
 */
public final class DeliveryService implements PluginModule {

    /** Пауза «попугай рядом с посылкой» перед отлётом (мс). */
    private static final long HANDOFF_PAUSE_MS = 2_000L;
    /** Длительность подъёма после выгрузки (мс). */
    private static final long DEPART_MS = 2_500L;

    private final KasaneParrotPlugin plugin;
    private final Map<UUID, DeliverySession> sessions = new ConcurrentHashMap<>();
    private final LandingSpotFinder landingSpotFinder;
    private final Random random = new Random();
    private BukkitTask tickTask;

    public DeliveryService(@NotNull KasaneParrotPlugin plugin) {
        this.plugin = plugin;
        this.landingSpotFinder = new LandingSpotFinder(plugin);
    }

    @Override
    public String id() {
        return "delivery";
    }

    @Override
    public void onEnable() {
        long period = plugin.cfg().flight().tickPeriod;
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, period);
    }

    @Override
    public void onDisable() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        for (DeliverySession session : sessions.values()) {
            CourierParrot courier = plugin.registry().get(session.parrotId());
            if (courier != null) {
                abort(courier, session);
            }
        }
        sessions.clear();
    }

    /* ------------------------------------------------------------------ */
    /* Публичное API                                                      */
    /* ------------------------------------------------------------------ */

    /**
     * Запускает доставку (вызывается подтверждением в меню).
     *
     * @return false, если доставка не стартовала (валидация/событие)
     */
    public boolean startDelivery(@NotNull Player sender,
                                 @NotNull CourierParrot courier,
                                 @NotNull OfflinePlayer target,
                                 @NotNull DeliveryDestination destination,
                                 @NotNull ItemStack payload) {
        if (courier.isBusy()) {
            return false;
        }
        var cfg = plugin.cfg().delivery();

        if (!cfg.allowSelf && sender.getUniqueId().equals(target.getUniqueId())) {
            plugin.messages().send(sender, Msg.DELIVERY_TARGET_IS_SELF);
            return false;
        }
        if (cfg.payloadBlacklist.contains(payload.getType())) {
            plugin.messages().send(sender, Msg.DELIVERY_PAYLOAD_BLACKLISTED,
                    Placeholder.unparsed("material", payload.getType().name()));
            return false;
        }
        Location targetPoint = resolveTargetPoint(target, destination);
        if (targetPoint == null) {
            plugin.messages().send(sender, Msg.GUI_DESTINATION_SPAWN_UNAVAILABLE);
            return false;
        }

        String targetName = target.getName() != null ? target.getName() : "?";
        DeliveryRequest request = new DeliveryRequest(
                sender.getUniqueId(), sender.getName(),
                target.getUniqueId(), targetName,
                payload.clone(), destination,
                System.currentTimeMillis()
        );
        long duration = plugin.timeCalculator().deliverySeconds(sender.getLocation(), targetPoint);
        DeliverySession session = DeliverySession.create(courier, request, duration);

        // Событие позволяет другим плагинам запретить отправку.
        DeliveryStartedEvent started = new DeliveryStartedEvent(session, sender, courier);
        Bukkit.getPluginManager().callEvent(started);
        if (started.isCancelled()) {
            plugin.messages().send(sender, Msg.DELIVERY_FAILED,
                    Placeholder.component("reason", plugin.messages().get(Msg.DELIVERY_REASON_CANCELLED)));
            return false;
        }

        // Забираем предмет из руки отправителя.
        ItemStack hand = sender.getInventory().getItemInMainHand();
        int amount = cfg.takeWholeStack ? payload.getAmount() : 1;
        hand.setAmount(Math.max(0, hand.getAmount() - amount));

        courier.payload(payload.clone());
        courier.session(session);
        sessions.put(courier.parrotId(), session);
        beginAscend(courier, session);

        plugin.messages().send(sender, Msg.DELIVERY_STARTED,
                Placeholder.unparsed("target", targetName),
                Placeholder.unparsed("time", TimeFormat.format(duration)));
        return true;
    }

    /** Все активные сессии (для /kasane status). */
    public Collection<DeliverySession> sessions() {
        return sessions.values();
    }

    /** Получатель вышел: запоминаем и уведомляем отправителя. */
    public void handleTargetQuit(@NotNull Player target) {
        for (DeliverySession session : sessions.values()) {
            if (!session.request().targetId().equals(target.getUniqueId())) {
                continue;
            }
            if (session.state() != CourierState.VANISHED && session.state() != CourierState.DESCENDING) {
                continue;
            }
            Player sender = Bukkit.getPlayer(session.request().senderId());
            if (sender != null && !session.notifiedTargetGone()) {
                session.notifiedTargetGone(true);
                plugin.messages().send(sender, Msg.DELIVERY_TARGET_GONE,
                        Placeholder.component("reason", offlineActionMessage()));
            }
        }
    }

    /** Получатель вернулся: попугай «долетает». */
    public void handleTargetJoin(@NotNull Player target) {
        long now = System.currentTimeMillis();
        for (DeliverySession session : sessions.values()) {
            if (session.request().targetId().equals(target.getUniqueId())
                    && session.state() == CourierState.VANISHED) {
                session.arriveAtMillis(now + 2_000L);
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /* Глобальный цикл                                                    */
    /* ------------------------------------------------------------------ */

    private void tick() {
        for (DeliverySession session : sessions.values()) {
            CourierParrot courier = plugin.registry().get(session.parrotId());
            if (courier == null) {
                sessions.remove(session.parrotId());
                continue;
            }
            try {
                tickSession(courier, session);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Ошибка тика доставки " + session.id(), e);
            }
        }
    }

    private void tickSession(@NotNull CourierParrot courier, @NotNull DeliverySession session) {
        switch (session.state()) {
            case ASCENDING -> {
                if (courier.stateDurationMs() >= ascendMs()) {
                    beginTransit(courier, session);
                }
            }
            case VANISHED -> {
                if (System.currentTimeMillis() >= session.arriveAtMillis()) {
                    beginApproach(courier, session);
                }
            }
            case DESCENDING -> {
                if (courier.stateDurationMs() >= descendMs()) {
                    beginHandoff(courier, session);
                }
            }
            case HANDOFF -> {
                if (courier.stateDurationMs() >= HANDOFF_PAUSE_MS) {
                    beginDepart(courier, session);
                }
            }
            case DEPARTING -> {
                if (courier.stateDurationMs() >= DEPART_MS) {
                    teleportNearOwner(courier, session, CourierState.RETURNING);
                }
            }
            case RETURNING -> {
                if (session.nearOwner() && courier.stateDurationMs() >= returnMs()) {
                    completeDelivery(courier, session);
                }
            }
            case RETURNING_FAILED -> {
                if (!session.nearOwner()) {
                    if (courier.stateDurationMs() >= DEPART_MS) {
                        teleportNearOwner(courier, session, CourierState.RETURNING_FAILED);
                    }
                } else if (courier.stateDurationMs() >= returnMs()) {
                    completeFailure(courier, session);
                }
            }
            default -> {
                // POUCHED/AWAITING_PAYLOAD/etc — вне миссии.
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /* Фазы                                                                */
    /* ------------------------------------------------------------------ */

    private void beginAscend(@NotNull CourierParrot courier, @NotNull DeliverySession session) {
        Parrot parrot = courier.entity();
        if (!courier.isAlive()) {
            fail(courier, session, DeliveryOutcome.FAILED_PARROT_LOST,
                    plugin.messages().get(Msg.DELIVERY_REASON_PARROT_LOST));
            return;
        }
        parrot.setSitting(false);
        session.vanishPoint(parrot.getLocation().clone());
        session.state(CourierState.ASCENDING);
        courier.state(CourierState.ASCENDING);

        // TODO(этап 2): SplineFlightController — спираль радиусом
        //   cfg.flight.ascend.spiralRadius с easing cfg.flight.ascend.easingName,
        //   частицы перьев по ходу движения.
        parrot.getWorld().spawnParticle(Particle.CLOUD, parrot.getLocation().add(0, 0.5, 0),
                12, 0.3, 0.1, 0.3, 0.01);
        debug("Взлёт: " + courier.parrotId());
    }

    private void beginTransit(@NotNull CourierParrot courier, @NotNull DeliverySession session) {
        Parrot parrot = courier.entity();
        if (!courier.isAlive()) {
            fail(courier, session, DeliveryOutcome.FAILED_PARROT_LOST,
                    plugin.messages().get(Msg.DELIVERY_REASON_PARROT_LOST));
            return;
        }
        // «Улетел на 100 блоков и пропал»: поднимаем и скрываем.
        double height = plugin.cfg().flight().ascend().height;
        parrot.teleport(session.vanishPoint().clone().add(0, height, 0));
        hideParrot(parrot);

        session.state(CourierState.VANISHED);
        courier.state(CourierState.VANISHED);
        session.arriveAtMillis(System.currentTimeMillis() + session.durationSeconds() * 1000L);
        debug("В пути: " + courier.parrotId() + ", прибытие через " + session.durationSeconds() + " с");
    }

    private void beginApproach(@NotNull CourierParrot courier, @NotNull DeliverySession session) {
        Parrot parrot = courier.entity();
        if (!courier.isAlive()) {
            fail(courier, session, DeliveryOutcome.FAILED_PARROT_LOST,
                    plugin.messages().get(Msg.DELIVERY_REASON_PARROT_LOST));
            return;
        }

        Player target = Bukkit.getPlayer(session.request().targetId());
        Location point;
        boolean targetOnline = target != null;

        if (targetOnline && session.request().destination() == DeliveryDestination.PLAYER) {
            point = target.getLocation();
        } else {
            // Офлайн-получатель или доставка к точке спавна.
            point = respawnPoint(Bukkit.getOfflinePlayer(session.request().targetId()));
            if (point == null) {
                // Политика WAIT: ждём повторного входа, пока не истёк таймаут.
                long waited = System.currentTimeMillis() - session.arriveAtMillis();
                if (waited < plugin.cfg().delivery().waitTimeoutSeconds * 1000L) {
                    return;
                }
                fail(courier, session, DeliveryOutcome.RETURNED_TO_SENDER, offlineActionMessage());
                return;
            }
        }

        Location approach = point.clone().add(
                (random.nextDouble() - 0.5) * 8.0D,
                plugin.cfg().flight().approach().heightAboveTarget,
                (random.nextDouble() - 0.5) * 8.0D
        );
        session.approachPoint(approach);
        parrot.teleportAsync(approach).thenRun(() -> showParrot(parrot));

        session.state(CourierState.DESCENDING);
        courier.state(CourierState.DESCENDING);

        if (plugin.cfg().delivery().announceArrival && targetOnline) {
            plugin.messages().send(target, Msg.DELIVERY_ARRIVING,
                    Placeholder.unparsed("sender", session.request().senderName()));
        }
        if (approach.getWorld() != null) {
            approach.getWorld().playSound(approach, "minecraft:entity.parrot.ambient", 2.0F, 1.2F);
        }
        debug("Подход к цели: " + courier.parrotId());
    }

    private void beginHandoff(@NotNull CourierParrot courier, @NotNull DeliverySession session) {
        Parrot parrot = courier.entity();
        if (!courier.isAlive()) {
            fail(courier, session, DeliveryOutcome.FAILED_PARROT_LOST,
                    plugin.messages().get(Msg.DELIVERY_REASON_PARROT_LOST));
            return;
        }

        Player target = Bukkit.getPlayer(session.request().targetId());
        LandingSpot spot = target == null ? null : landingSpotFinder.find(target).orElse(null);

        if (spot == null) {
            // Цель движется или рядом нет места.
            // TODO(этап 2): hover-follow — лететь рядом с целью (упреждение
            //   из MovementAnalyzer + FlightPath) и садиться при остановке.
            long maxWaitMs = plugin.cfg().flight().landing().maxWaitSeconds * 1000L;
            if (courier.stateDurationMs() < maxWaitMs) {
                return; // ещё ждём удобного момента
            }
            String timeoutAction = plugin.cfg().flight().landing().timeoutAction;
            if ("DROP_IN_AIR".equalsIgnoreCase(timeoutAction) && target != null) {
                // Выпростить посылку рядом с бегущей целью.
                session.handoffPoint(target.getLocation().clone());
                target.getWorld().dropItemNaturally(
                        target.getLocation().add(0, 1, 0), session.request().payload());
                finishHandoff(courier, session, target);
                return;
            }
            fail(courier, session, DeliveryOutcome.FAILED_NO_LANDING,
                    plugin.messages().get(Msg.DELIVERY_REASON_NO_LANDING));
            return;
        }

        session.handoffPoint(spot.location().clone());
        parrot.teleport(spot.location().clone().add(0, 1.0, 0));
        parrot.setSitting(true);

        UUID frameId = plugin.frameService().spawnDeliveryFrame(
                spot.location(), session.request().payload(), session);
        session.frameId(frameId);
        finishHandoff(courier, session, target);
    }

    /** Общие действия после выгрузки посылки (рамкой или дропом). */
    private void finishHandoff(@NotNull CourierParrot courier,
                               @NotNull DeliverySession session,
                               @Nullable Player target) {
        courier.payload(null);
        session.outcome(DeliveryOutcome.DELIVERED);
        Bukkit.getPluginManager().callEvent(new DeliveryCompletedEvent(session, target));

        Player sender = Bukkit.getPlayer(session.request().senderId());
        if (sender != null) {
            plugin.messages().send(sender, Msg.DELIVERY_DELIVERED,
                    Placeholder.unparsed("target", session.request().targetName()));
        }
        if (target != null) {
            target.playSound(target.getLocation(), "minecraft:entity.parrot.ambient", 1.0F, 1.5F);
        }

        session.state(CourierState.HANDOFF);
        courier.state(CourierState.HANDOFF);
        debug("Посылка выгружена: " + courier.parrotId());
    }

    private void beginDepart(@NotNull CourierParrot courier, @NotNull DeliverySession session) {
        Parrot parrot = courier.entity();
        if (courier.isAlive()) {
            parrot.setSitting(false);
            // TODO(этап 2): плавный набор высоты по дуге (FlightPath.arc).
            if (parrot.getWorld() != null) {
                parrot.getWorld().spawnParticle(Particle.CLOUD, parrot.getLocation(),
                        10, 0.3, 0.3, 0.3, 0.01);
            }
        }
        session.state(CourierState.DEPARTING);
        courier.state(CourierState.DEPARTING);
    }

    /** Телепорт над хозяином и переход в снижение (общий для успеха/провала). */
    private void teleportNearOwner(@NotNull CourierParrot courier,
                                   @NotNull DeliverySession session,
                                   @NotNull CourierState next) {
        Player owner = Bukkit.getPlayer(courier.ownerId());
        if (owner == null) {
            return; // ждём возвращения хозяина, тик повторит
        }
        Parrot parrot = courier.entity();
        if (parrot != null) {
            Location above = owner.getLocation().clone()
                    .add(0, plugin.cfg().flight().approach().heightAboveTarget / 2.0D, 0);
            parrot.teleportAsync(above).thenRun(() -> showParrot(parrot));
        }
        session.nearOwner(true);
        session.state(next);
        courier.state(next);
        debug("Возврат к хозяину: " + courier.parrotId());
    }

    private void completeDelivery(@NotNull CourierParrot courier, @NotNull DeliverySession session) {
        Player owner = Bukkit.getPlayer(courier.ownerId());
        if (owner == null) {
            return;
        }
        Parrot parrot = courier.entity();
        if (courier.isAlive()) {
            // TODO(этап 2): спуск к хозяину по дуге, а не мгновенный телепорт.
            parrot.teleport(owner.getLocation());
            plugin.modelService().applyAfterDeliveryModel(parrot, owner);
            if (plugin.cfg().flight().return().perchOnShoulder) {
                plugin.shoulderService().perch(owner, parrot);
            } else {
                parrot.setSitting(true);
            }
        }

        courier.session(null);
        courier.payload(null);
        courier.state(CourierState.POUCHED);
        sessions.remove(session.parrotId());
        debug("Доставка завершена: " + session.id());
    }

    private void completeFailure(@NotNull CourierParrot courier, @NotNull DeliverySession session) {
        Player owner = Bukkit.getPlayer(courier.ownerId());
        if (owner == null) {
            return;
        }
        if (courier.payload() != null) {
            returnPayload(owner, courier.payload());
        }
        Parrot parrot = courier.entity();
        if (courier.isAlive()) {
            parrot.teleport(owner.getLocation());
            if (plugin.cfg().flight().return().perchOnShoulder) {
                plugin.shoulderService().perch(owner, parrot);
            } else {
                parrot.setSitting(true);
            }
        }
        plugin.messages().send(owner, Msg.DELIVERY_RETURNED);

        courier.session(null);
        courier.payload(null);
        courier.state(CourierState.POUCHED);
        sessions.remove(session.parrotId());
    }

    /* ------------------------------------------------------------------ */
    /* Сбои                                                                */
    /* ------------------------------------------------------------------ */

    /** Доставка сорвалась: уведомления, возврат груза, попугай — домой. */
    public void fail(@NotNull CourierParrot courier,
                     @NotNull DeliverySession session,
                     @NotNull DeliveryOutcome outcome,
                     @NotNull Component reason) {
        session.outcome(outcome);
        session.failureReason(reason.toString());
        Bukkit.getPluginManager().callEvent(new DeliveryFailedEvent(session, outcome));

        Player sender = Bukkit.getPlayer(session.request().senderId());
        if (sender != null) {
            plugin.messages().send(sender, Msg.DELIVERY_FAILED,
                    Placeholder.component("reason", reason));
        }

        Parrot parrot = courier.entity();
        if (courier.isAlive() && parrot != null) {
            showParrot(parrot);
        }
        session.nearOwner(false);
        session.state(CourierState.RETURNING_FAILED);
        courier.state(CourierState.RETURNING_FAILED);
    }

    /** Мягкая отмена (выключение плагина): без событий, груз — отправителю. */
    private void abort(@NotNull CourierParrot courier, @NotNull DeliverySession session) {
        Parrot parrot = courier.entity();
        if (parrot != null && parrot.isValid()) {
            showParrot(parrot);
        }
        Player sender = Bukkit.getPlayer(session.request().senderId());
        if (sender != null && courier.payload() != null) {
            returnPayload(sender, courier.payload());
        }
        courier.session(null);
        courier.payload(null);
        courier.state(CourierState.POUCHED);
    }

    private void returnPayload(@NotNull Player to, @NotNull ItemStack payload) {
        var leftover = to.getInventory().addItem(payload);
        for (ItemStack rest : leftover.values()) {
            to.getWorld().dropItemNaturally(to.getLocation().add(0, 1, 0), rest);
        }
    }

    /* ------------------------------------------------------------------ */
    /* Хелперы                                                             */
    /* ------------------------------------------------------------------ */

    @Nullable
    private Location resolveTargetPoint(@NotNull OfflinePlayer target,
                                        @NotNull DeliveryDestination destination) {
        if (destination == DeliveryDestination.PLAYER) {
            Player online = target.getPlayer();
            if (online != null) {
                return online.getLocation();
            }
            return respawnPoint(target);
        }
        return respawnPoint(target);
    }

    @Nullable
    private Location respawnPoint(@NotNull OfflinePlayer target) {
        Location loc = target.getRespawnLocation();
        return loc == null ? null : loc.clone();
    }

    private Component offlineActionMessage() {
        return switch (plugin.cfg().delivery().offlinePolicy()) {
            case DELIVER_TO_RESPAWN -> plugin.messages().get(Msg.DELIVERY_ACTION_RESPAWN);
            case RETURN_TO_SENDER -> plugin.messages().get(Msg.DELIVERY_ACTION_RETURN);
            case WAIT -> plugin.messages().get(Msg.DELIVERY_ACTION_WAIT);
        };
    }

    private void hideParrot(@NotNull Parrot parrot) {
        parrot.setVisibleByDefault(false);
        parrot.setInvulnerable(true);
        parrot.setSilent(true);
        parrot.setAI(false);
        parrot.setCollidable(false);
    }

    private void showParrot(@NotNull Parrot parrot) {
        parrot.setVisibleByDefault(true);
        parrot.setInvulnerable(plugin.cfg().parrot().invulnerableWhileDelivering);
        parrot.setSilent(false);
        parrot.setAI(true);
        parrot.setCollidable(true);
    }

    private long ascendMs() {
        var a = plugin.cfg().flight().ascend();
        return (long) (a.height / a.speedBlocksPerTick * 50.0D);
    }

    private long descendMs() {
        var a = plugin.cfg().flight().approach();
        return (long) (a.heightAboveTarget / a.descentSpeed * 50.0D);
    }

    private long returnMs() {
        return descendMs();
    }

    private void debug(@NotNull String message) {
        if (plugin.cfg().debug) {
            plugin.getLogger().info("[delivery] " + message);
        }
    }
}
