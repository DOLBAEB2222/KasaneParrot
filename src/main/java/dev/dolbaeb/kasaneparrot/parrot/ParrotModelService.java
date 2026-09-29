package dev.dolbaeb.kasaneparrot.parrot;

import dev.dolbaeb.kasaneparrot.KasaneParrotPlugin;
import dev.dolbaeb.kasaneparrot.module.PluginModule;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Смена «модельки» и имени попугая.
 *
 * <p>Paper не умеет произвольные модели энтити, поэтому визуальная смена
 * делается сменой {@link Parrot.Variant варианта} (RED/BLUE/GREEN/CYAN/GRAY),
 * а ressourcespack «Pesky Parrots» отрисовывает каждый вариант по-своему.
 * Дополнительно обновляется кастомное имя (MiniMessage).</p>
 */
public final class ParrotModelService implements PluginModule {

    private final KasaneParrotPlugin plugin;

    public ParrotModelService(@NotNull KasaneParrotPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return "parrot-model";
    }

    /** Применяется при надевании мешочка: курьерский вариант + курьерское имя. */
    public void applyCourierModel(@NotNull Parrot parrot, @NotNull Player owner) {
        rememberOriginalVariant(parrot);
        parrot.setVariant(plugin.cfg().parrot().courierVariant);
        applyName(parrot, plugin.cfg().parrot().nameCourierFormat, owner);
    }

    /** Применяется после первой доставки: «обычный Kasane-попугай». */
    public void applyAfterDeliveryModel(@NotNull Parrot parrot, @NotNull Player owner) {
        parrot.setVariant(plugin.cfg().parrot().afterDeliveryVariant);
        applyName(parrot, plugin.cfg().parrot().nameNormalFormat, owner);
    }

    /** Просто обновляет имя (без смены варианта). */
    public void applyNormalName(@NotNull Parrot parrot, @NotNull Player owner) {
        applyName(parrot, plugin.cfg().parrot().nameNormalFormat, owner);
    }

    /* ------------------------------------------------------------------ */

    private void rememberOriginalVariant(@NotNull Parrot parrot) {
        if (!plugin.keys().flag(parrot, plugin.keys().originalVariant)) {
            parrot.getPersistentDataContainer().set(
                    plugin.keys().originalVariant,
                    org.bukkit.persistence.PersistentDataType.STRING,
                    parrot.getVariant().name()
            );
        }
    }

    private void applyName(@NotNull Parrot parrot, @NotNull String format, @NotNull Player owner) {
        if (!plugin.cfg().parrot().nameEnabled || format.isBlank()) {
            return;
        }
        parrot.customName(MiniMessage.miniMessage().deserialize(
                format,
                Placeholder.unparsed("owner", owner.getName())
        ));
        parrot.setCustomNameVisible(true);
    }
}
