package dev.dolbaeb.kasaneparrot;

import dev.dolbaeb.kasaneparrot.command.KasaneCommand;
import dev.dolbaeb.kasaneparrot.config.KasaneConfig;
import dev.dolbaeb.kasaneparrot.delivery.DeliveryService;
import dev.dolbaeb.kasaneparrot.delivery.DeliveryTimeCalculator;
import dev.dolbaeb.kasaneparrot.frame.DeliveryFrameService;
import dev.dolbaeb.kasaneparrot.gui.HeadCache;
import dev.dolbaeb.kasaneparrot.gui.MenuService;
import dev.dolbaeb.kasaneparrot.key.PluginKeys;
import dev.dolbaeb.kasaneparrot.listener.CourierProtectionListener;
import dev.dolbaeb.kasaneparrot.listener.FrameInteractionListener;
import dev.dolbaeb.kasaneparrot.listener.MenuListener;
import dev.dolbaeb.kasaneparrot.listener.ParrotInteractListener;
import dev.dolbaeb.kasaneparrot.listener.PlayerConnectionListener;
import dev.dolbaeb.kasaneparrot.module.ModuleManager;
import dev.dolbaeb.kasaneparrot.parrot.CourierRegistry;
import dev.dolbaeb.kasaneparrot.parrot.ParrotModelService;
import dev.dolbaeb.kasaneparrot.shoulder.ShoulderService;
import dev.dolbaeb.kasaneparrot.text.MessageService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * KasaneParrot — почтовая служба Kasane-попугаев (Paper 1.21.8).
 *
 * <p>Точка сборки всех модулей. Порядок важен: конфиг → сообщения → ключи →
 * сервисы (без задач) → ModuleManager.enableAll() (задачи/циклы) →
 * слушатели и команды.</p>
 */
public final class KasaneParrotPlugin extends JavaPlugin {

    private KasaneConfig kasaneConfig;
    private MessageService messageService;
    private PluginKeys pluginKeys;
    private ModuleManager moduleManager;

    private CourierRegistry courierRegistry;
    private ParrotModelService parrotModelService;
    private HeadCache headCache;
    private MenuService menuService;
    private DeliveryFrameService frameService;
    private ShoulderService shoulderService;
    private DeliveryTimeCalculator timeCalculator;
    private DeliveryService deliveryService;
    private dev.dolbaeb.kasaneparrot.flight.MovementAnalyzer movementAnalyzer;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.kasaneConfig = KasaneConfig.load(this);
        this.messageService = new MessageService(this, kasaneConfig.language);
        this.pluginKeys = PluginKeys.create(this);

        // Сервисы (конструируются без побочных эффектов).
        this.courierRegistry = new CourierRegistry();
        this.parrotModelService = new ParrotModelService(this);
        this.movementAnalyzer = new dev.dolbaeb.kasaneparrot.flight.MovementAnalyzer(this);
        this.headCache = new HeadCache(this);
        this.shoulderService = new ShoulderService(this);
        this.frameService = new DeliveryFrameService(this);
        this.timeCalculator = new DeliveryTimeCalculator(this);
        this.menuService = new MenuService(this, headCache);
        this.deliveryService = new DeliveryService(this);

        // Модули: включение запускает их задачи.
        this.moduleManager = new ModuleManager(this);
        moduleManager.register(movementAnalyzer);
        moduleManager.register(headCache);
        moduleManager.register(parrotModelService);
        moduleManager.register(frameService);
        moduleManager.register(menuService);
        moduleManager.register(deliveryService);
        moduleManager.enableAll();

        registerListeners();
        registerCommands();

        getLogger().info("KasaneParrot запущен: почта для попугаев готова к работе.");
    }

    @Override
    public void onDisable() {
        if (moduleManager != null) {
            moduleManager.disableAll();
        }
    }

    /** Полная перезагрузка конфига и сообщений (сервисы читают их на лету). */
    public void reloadAll() {
        this.kasaneConfig = KasaneConfig.load(this);
        this.messageService.reload();
        moduleManager.reloadAll();
    }

    /* ------------------------------------------------------------------ */

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new ParrotInteractListener(this), this);
        pm.registerEvents(new FrameInteractionListener(this), this);
        pm.registerEvents(new PlayerConnectionListener(this), this);
        pm.registerEvents(new MenuListener(this), this);
        pm.registerEvents(new CourierProtectionListener(this), this);
    }

    private void registerCommands() {
        PluginCommand command = getCommand("kasane");
        if (command != null) {
            KasaneCommand executor = new KasaneCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
    }

    /* ------------------------------------------------------------------ */
    /* Доступ сервисов (всегда актуальные экземпляры)                      */
    /* ------------------------------------------------------------------ */

    public @NotNull KasaneConfig cfg() {
        return kasaneConfig;
    }

    public @NotNull MessageService messages() {
        return messageService;
    }

    public @NotNull PluginKeys keys() {
        return pluginKeys;
    }

    public @NotNull CourierRegistry registry() {
        return courierRegistry;
    }

    public @NotNull ParrotModelService modelService() {
        return parrotModelService;
    }

    public @NotNull dev.dolbaeb.kasaneparrot.flight.MovementAnalyzer movementAnalyzer() {
        return movementAnalyzer;
    }

    public @NotNull HeadCache headCache() {
        return headCache;
    }

    public @NotNull MenuService menuService() {
        return menuService;
    }

    public @NotNull DeliveryFrameService frameService() {
        return frameService;
    }

    public @NotNull ShoulderService shoulderService() {
        return shoulderService;
    }

    public @NotNull DeliveryTimeCalculator timeCalculator() {
        return timeCalculator;
    }

    public @NotNull DeliveryService deliveryService() {
        return deliveryService;
    }
}
