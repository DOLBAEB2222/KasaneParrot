package dev.dolbaeb.kasaneparrot.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Parrot;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Типизированная обёртка над config.yml.
 *
 * <p>Загружается один раз (и перезагружается по {@code /kasane reload});
 * все поля — immutable, вложенные секции — статические вложенные классы.
 * Никакой другой класс плагина не читает {@link FileConfiguration} напрямую.</p>
 */
public final class KasaneConfig {

    private final boolean debug;
    public final String language;

    private final ParrotCfg parrot;
    private final PouchCfg pouch;
    private final DeliveryCfg delivery;
    private final FlightCfg flight;
    private final GuiCfg gui;
    private final ResourcePackCfg resourcePack;
    private final StorageCfg storage;

    private KasaneConfig(@NotNull FileConfiguration c) {
        this.debug = c.getBoolean("debug", false);
        this.language = c.getString("language", "ru");
        this.parrot = new ParrotCfg(c);
        this.pouch = new PouchCfg(c);
        this.delivery = new DeliveryCfg(c);
        this.flight = new FlightCfg(c);
        this.gui = new GuiCfg(c);
        this.resourcePack = new ResourcePackCfg(c);
        this.storage = new StorageCfg(c);
    }

    public static KasaneConfig load(@NotNull JavaPlugin plugin) {
        plugin.reloadConfig();
        return new KasaneConfig(plugin.getConfig());
    }

    public boolean debug() {
        return debug;
    }

    public ParrotCfg parrot() {
        return parrot;
    }

    public PouchCfg pouch() {
        return pouch;
    }

    public DeliveryCfg delivery() {
        return delivery;
    }

    public FlightCfg flight() {
        return flight;
    }

    public GuiCfg gui() {
        return gui;
    }

    public ResourcePackCfg resourcePack() {
        return resourcePack;
    }

    public StorageCfg storage() {
        return storage;
    }

    /* ------------------------------------------------------------------ */

    /** Секция {@code parrot}. */
    public static final class ParrotCfg {
        public final Parrot.Variant courierVariant;
        public final Parrot.Variant afterDeliveryVariant;
        public final boolean nameEnabled;
        public final String nameNormalFormat;
        public final String nameCourierFormat;
        public final boolean invulnerableWhileDelivering;
        public final boolean blockPortals;

        ParrotCfg(@NotNull FileConfiguration c) {
            this.courierVariant = variant(c.getString("parrot.model.courier-variant", "CYAN"));
            this.afterDeliveryVariant = variant(c.getString("parrot.model.after-delivery-variant", "GRAY"));
            this.nameEnabled = c.getBoolean("parrot.name.enabled", true);
            this.nameNormalFormat = c.getString("parrot.name.normal-format", "<italic><#31d8e0>Kasane {owner}");
            this.nameCourierFormat = c.getString("parrot.name.courier-format", "<italic><bold><#e6b800>✉ Kasane {owner}");
            this.invulnerableWhileDelivering = c.getBoolean("parrot.protection.invulnerable-while-delivering", true);
            this.blockPortals = c.getBoolean("parrot.protection.block-portals", true);
        }

        private static Parrot.Variant variant(String name) {
            try {
                return Parrot.Variant.valueOf(name.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return Parrot.Variant.CYAN;
            }
        }
    }

    /** Секция {@code pouch}. */
    public static final class PouchCfg {
        public final Material material;
        public final boolean consumeOnAttach;
        public final boolean requireOwner;
        public final boolean openMenuAfterAttach;

        PouchCfg(@NotNull FileConfiguration c) {
            Material m = Material.matchMaterial(c.getString("pouch.material", "BUNDLE"));
            this.material = m != null ? m : Material.BUNDLE;
            this.consumeOnAttach = c.getBoolean("pouch.consume-on-attach", true);
            this.requireOwner = c.getBoolean("pouch.require-owner", true);
            this.openMenuAfterAttach = c.getBoolean("pouch.open-menu-after-attach", true);
        }
    }

    /** Секция {@code delivery}. */
    public static final class DeliveryCfg {
        public final long minSeconds;
        public final long maxSeconds;
        public final double minDistance;
        public final double maxDistance;
        public final long crossWorldSeconds;
        public final boolean allowSelf;
        public final OfflinePolicy offlinePolicy;
        public final long waitTimeoutSeconds;
        public final boolean announceArrival;
        public final boolean takeWholeStack;
        public final Set<Material> payloadBlacklist;
        public final boolean frameInvisible;
        public final boolean frameGlowing;
        public final boolean frameDropOnLeftClick;
        public final long frameLifetimeSeconds;

        /** Политика поведения при выходе получателя во время доставки. */
        public enum OfflinePolicy {
            DELIVER_TO_RESPAWN, RETURN_TO_SENDER, WAIT
        }

        DeliveryCfg(@NotNull FileConfiguration c) {
            this.minSeconds = Math.max(1L, c.getLong("delivery.time.min-seconds", 30L));
            this.maxSeconds = Math.max(minSeconds, c.getLong("delivery.time.max-seconds", 300L));
            this.minDistance = Math.max(1.0D, c.getDouble("delivery.time.min-distance", 50.0D));
            this.maxDistance = Math.max(minDistance, c.getDouble("delivery.time.max-distance", 3000.0D));
            this.crossWorldSeconds = Math.max(1L, c.getLong("delivery.time.cross-world-seconds", 300L));
            this.allowSelf = c.getBoolean("delivery.target.allow-self", false);
            this.offlinePolicy = parsePolicy(c.getString("delivery.target.offline-policy", "WAIT"));
            this.waitTimeoutSeconds = Math.max(10L, c.getLong("delivery.target.wait-timeout-seconds", 120L));
            this.announceArrival = c.getBoolean("delivery.target.announce-arrival", true);
            this.takeWholeStack = c.getBoolean("delivery.payload.take-whole-stack", true);
            this.payloadBlacklist = parseMaterials(c.getStringList("delivery.payload.blacklisted-materials"));
            this.frameInvisible = c.getBoolean("delivery.frame.invisible", true);
            this.frameGlowing = c.getBoolean("delivery.frame.glowing", false);
            this.frameDropOnLeftClick = c.getBoolean("delivery.frame.drop-on-left-click", true);
            this.frameLifetimeSeconds = Math.max(0L, c.getLong("delivery.frame.lifetime-seconds", 300L));
        }

        private static OfflinePolicy parsePolicy(String raw) {
            try {
                return OfflinePolicy.valueOf(raw.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return OfflinePolicy.WAIT;
            }
        }

        private static Set<Material> parseMaterials(java.util.List<String> raw) {
            Set<Material> out = new HashSet<>();
            for (String name : raw) {
                Material m = Material.matchMaterial(name);
                if (m != null) {
                    out.add(m);
                }
            }
            return out;
        }
    }

    /** Секция {@code flight}. */
    public static final class FlightCfg {
        public final long tickPeriod;
        public final AscendCfg ascend;
        public final ApproachCfg approach;
        public final LandingCfg landing;
        public final ReturnCfg returning;

        public static final class AscendCfg {
            public final double height;
            public final double speedBlocksPerTick;
            public final double spiralRadius;
            public final String easingName;

            AscendCfg(@NotNull ConfigurationSection c) {
                this.height = c.getDouble("height", 100.0D);
                this.speedBlocksPerTick = Math.max(0.05D, c.getDouble("speed", 0.9D));
                this.spiralRadius = Math.max(0.0D, c.getDouble("spiral-radius", 6.0D));
                this.easingName = c.getString("easing", "EASE_IN_OUT_CUBIC");
            }
        }

        public static final class ApproachCfg {
            public final double heightAboveTarget;
            public final double descentSpeed;
            public final double hoverDistance;

            ApproachCfg(@NotNull ConfigurationSection c) {
                this.heightAboveTarget = Math.max(1.0D, c.getDouble("height-above-target", 50.0D));
                this.descentSpeed = Math.max(0.05D, c.getDouble("descent-speed", 0.65D));
                this.hoverDistance = Math.max(1.0D, c.getDouble("hover-distance", 2.5D));
            }
        }

        public static final class LandingCfg {
            public final long maxWaitSeconds;
            public final long reEvaluateTicks;
            public final String timeoutAction;
            public final double scanRadius;
            public final double minDistance;
            public final boolean avoidWater;
            public final Set<Material> forbiddenMaterials;

            LandingCfg(@NotNull ConfigurationSection c) {
                this.maxWaitSeconds = Math.max(5L, c.getLong("max-wait-seconds", 45L));
                this.reEvaluateTicks = Math.max(1L, c.getLong("re-evaluate-ticks", 20L));
                this.timeoutAction = c.getString("timeout-action", "RETURN");
                this.scanRadius = Math.max(1.0D, c.getDouble("scan.radius", 8.0D));
                this.minDistance = Math.max(0.0D, c.getDouble("scan.min-distance", 2.0D));
                this.avoidWater = c.getBoolean("scan.avoid-water", true);
                this.forbiddenMaterials = DeliveryCfg.parseMaterials(c.getStringList("scan.forbidden-materials"));
            }
        }

        public static final class ReturnCfg {
            public final double height;
            public final boolean perchOnShoulder;
            public final String shoulderSide;

            ReturnCfg(@NotNull ConfigurationSection c) {
                this.height = Math.max(1.0D, c.getDouble("height", 100.0D));
                this.perchOnShoulder = c.getBoolean("perch-on-shoulder", true);
                this.shoulderSide = c.getString("shoulder-side", "AUTO");
            }
        }

        FlightCfg(@NotNull FileConfiguration c) {
            this.tickPeriod = Math.max(1L, c.getLong("flight.tick-period", 1L));
            this.ascend = new AscendCfg(section(c, "flight.ascend"));
            this.approach = new ApproachCfg(section(c, "flight.approach"));
            this.landing = new LandingCfg(section(c, "flight.landing"));
            this.returning = new ReturnCfg(section(c, "flight.return"));
        }

        private static ConfigurationSection section(FileConfiguration c, String path) {
            ConfigurationSection s = c.getConfigurationSection(path);
            if (s == null) {
                throw new IllegalStateException("В config.yml отсутствует секция " + path);
            }
            return s;
        }
    }

    /** Секция {@code gui}. */
    public static final class GuiCfg {
        public final Material filler;
        public final String clickSound;
        public final String successSound;
        public final String errorSound;
        public final SelectCfg select;
        public final DestinationCfg destination;
        public final ConfirmCfg confirm;

        public static final class SelectCfg {
            public final String title;
            public final int rows;
            public final boolean showSelf;
            public final boolean liveUpdate;
            public final long refreshDelayTicks;
            public final Material prevPageIcon;
            public final Material nextPageIcon;
            public final Material infoIcon;

            SelectCfg(@NotNull FileConfiguration c) {
                this.title = c.getString("gui.select-menu.title", "<dark_red>Почта Kasane");
                this.rows = clamp(c.getInt("gui.select-menu.rows", 6), 1, 6);
                this.showSelf = c.getBoolean("gui.select-menu.show-self", false);
                this.liveUpdate = c.getBoolean("gui.select-menu.live-update", true);
                this.refreshDelayTicks = Math.max(1L, c.getLong("gui.select-menu.refresh-delay-ticks", 1L));
                this.prevPageIcon = material(c, "gui.select-menu.icons.prev-page", Material.SPECTRAL_ARROW);
                this.nextPageIcon = material(c, "gui.select-menu.icons.next-page", Material.ARROW);
                this.infoIcon = material(c, "gui.select-menu.icons.info", Material.BOOK);
            }
        }

        public static final class DestinationCfg {
            public final String title;
            public final int rows;
            public final Material toPlayerIcon;
            public final Material toSpawnIcon;

            DestinationCfg(@NotNull FileConfiguration c) {
                this.title = c.getString("gui.destination-menu.title", "<dark_red>Куда доставить?");
                this.rows = clamp(c.getInt("gui.destination-menu.rows", 3), 2, 6);
                this.toPlayerIcon = material(c, "gui.destination-menu.icons.to-player", Material.COMPASS);
                this.toSpawnIcon = material(c, "gui.destination-menu.icons.to-spawn", Material.RED_BED);
            }
        }

        public static final class ConfirmCfg {
            public final String title;
            public final int rows;
            public final Material confirmIcon;
            public final Material cancelIcon;

            ConfirmCfg(@NotNull FileConfiguration c) {
                this.title = c.getString("gui.confirm-menu.title", "<dark_red>Подтверждение");
                this.rows = clamp(c.getInt("gui.confirm-menu.rows", 3), 2, 6);
                this.confirmIcon = material(c, "gui.confirm-menu.icons.confirm", Material.LIME_DYE);
                this.cancelIcon = material(c, "gui.confirm-menu.icons.cancel", Material.BARRIER);
            }
        }

        GuiCfg(@NotNull FileConfiguration c) {
            this.filler = material(c, "gui.filler", Material.GRAY_STAINED_GLASS_PANE);
            this.clickSound = c.getString("gui.sounds.click", "minecraft:ui.loom.select_pattern");
            this.successSound = c.getString("gui.sounds.success", "minecraft:entity.player.levelup");
            this.errorSound = c.getString("gui.sounds.error", "minecraft:entity.villager.no");
            this.select = new SelectCfg(c);
            this.destination = new DestinationCfg(c);
            this.confirm = new ConfirmCfg(c);
        }

        private static int clamp(int v, int min, int max) {
            return Math.max(min, Math.min(max, v));
        }

        private static Material material(FileConfiguration c, String path, Material def) {
            Material m = Material.matchMaterial(c.getString(path, def.name()));
            return m != null ? m : def;
        }
    }

    /** Секция {@code resource-pack}. */
    public static final class ResourcePackCfg {
        public enum Mode { NONE, PROMPT, FORCE }

        public final Mode mode;
        public final String url;
        public final String sha1;
        public final String prompt;
        public final boolean kickOnDecline;

        ResourcePackCfg(@NotNull FileConfiguration c) {
            this.mode = parseMode(c.getString("resource-pack.mode", "NONE"));
            this.url = c.getString("resource-pack.url", "");
            this.sha1 = c.getString("resource-pack.sha1", "");
            this.prompt = c.getString("resource-pack.prompt", "");
            this.kickOnDecline = c.getBoolean("resource-pack.kick-on-decline", false);
        }

        private static Mode parseMode(String raw) {
            try {
                return Mode.valueOf(raw.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return Mode.NONE;
            }
        }
    }

    /** Секция {@code storage}. */
    public static final class StorageCfg {
        public final String type;
        public final long saveIntervalSeconds;
        public final int historySize;

        StorageCfg(@NotNull FileConfiguration c) {
            this.type = c.getString("storage.type", "YAML");
            this.saveIntervalSeconds = Math.max(10L, c.getLong("storage.save-interval-seconds", 60L));
            this.historySize = Math.max(0, c.getInt("storage.history-size", 50));
        }
    }
}
