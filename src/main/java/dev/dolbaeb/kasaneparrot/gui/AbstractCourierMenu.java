package dev.dolbaeb.kasaneparrot.gui;

import dev.dolbaeb.kasaneparrot.KasaneParrotPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemMeta;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.view.builder.InventoryViewBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Базовый класс меню на ванильном Paper Menu Type API.
 *
 * <p>Окно строится через {@code MenuType.GENERIC_9xN.builder()}, открывается
 * методом {@code InventoryView#open()}. Обновление содержимого — прямой правкой
 * top-инвентаря, заголовка — методом {@code InventoryView#setTitle(Component)}
 * (живьём, без закрытия окна). Клики маршрутизируются через
 * {@code MenuListener → MenuService#handleClick}.</p>
 */
@SuppressWarnings("UnstableApiUsage") // Menu Type API помечен experimental в 1.21.8
public abstract class AbstractCourierMenu {

    protected final KasaneParrotPlugin plugin;
    protected final Player viewer;

    private InventoryView view;
    private Inventory top;
    private boolean open;

    protected AbstractCourierMenu(@NotNull KasaneParrotPlugin plugin, @NotNull Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
    }

    /** Ванильный тип окна (сундучная сетка). */
    protected abstract @NotNull MenuType.Typed<InventoryView, ? extends InventoryViewBuilder<InventoryView>> menuType();

    /** Заголовок окна; перерисовывается при каждом {@link #refresh()}. */
    protected abstract @NotNull Component renderTitle();

    /** Заполнение слотов top-инвентаря. */
    protected abstract void render();

    /** Клик по слоту top-инвентаря (событие уже отменено). */
    protected abstract void onClick(int slot, @NotNull InventoryClickEvent event);

    /* ------------------------------------------------------------------ */

    public final void open() {
        view = menuType().builder()
                .title(renderTitle())
                .build(viewer);
        top = view.getTopInventory();
        render();
        view.open();
        open = true;
        plugin.menuService().register(this);
    }

    /** Живое обновление: контент + заголовок, окно не закрывается. */
    public final void refresh() {
        if (!isOpen()) {
            return;
        }
        view.setTitle(renderTitle());
        render();
    }

    public final void handleClose() {
        open = false;
    }

    public final boolean isOpen() {
        return open && viewer.isOnline();
    }

    public final @NotNull Player viewer() {
        return viewer;
    }

    public final @NotNull Inventory top() {
        return top;
    }

    /* ------------------------------------------------------------------ */
    /* Хелперы                                                             */
    /* ------------------------------------------------------------------ */

    protected final @NotNull ItemStack filler() {
        return named(plugin.cfg().gui().filler, Component.space(), null);
    }

    /** Предмет с именем и (опц.) lore. */
    protected final @NotNull ItemStack named(@NotNull Material material,
                                             @NotNull Component name,
                                             @Nullable List<Component> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(name);
        if (lore != null && !lore.isEmpty()) {
            meta.lore(lore);
        }
        stack.setItemMeta(meta);
        return stack;
    }

    protected final void playClick() {
        play(plugin.cfg().gui().clickSound);
    }

    protected final void playSuccess() {
        play(plugin.cfg().gui().successSound);
    }

    protected final void playError() {
        play(plugin.cfg().gui().errorSound);
    }

    protected final void play(@Nullable String sound) {
        if (sound != null && !sound.isBlank()) {
            viewer.playSound(viewer.getLocation(), sound, 1.0F, 1.0F);
        }
    }

    /** Сундучный тип окна на 1..6 строк. */
    protected static @NotNull MenuType.Typed<InventoryView, ? extends InventoryViewBuilder<InventoryView>> chestType(int rows) {
        return switch (Math.max(1, Math.min(6, rows))) {
            case 1 -> MenuType.GENERIC_9X1;
            case 2 -> MenuType.GENERIC_9X2;
            case 3 -> MenuType.GENERIC_9X3;
            case 4 -> MenuType.GENERIC_9X4;
            case 5 -> MenuType.GENERIC_9X5;
            default -> MenuType.GENERIC_9X6;
        };
    }

    /** Белое имя игрока для головы. */
    protected static @NotNull Component playerName(@NotNull String name) {
        return Component.text(name, NamedTextColor.WHITE);
    }
}
