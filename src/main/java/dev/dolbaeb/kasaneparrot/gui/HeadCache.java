package dev.dolbaeb.kasaneparrot.gui;

import com.destroystokyo.paper.profile.PlayerProfile;
import dev.dolbaeb.kasaneparrot.KasaneParrotPlugin;
import dev.dolbaeb.kasaneparrot.module.PluginModule;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Кэш голов игроков для меню выбора получателя.
 *
 * <p>Профиль создаётся через Paper {@code Bukkit#createProfile(uuid, name)}
 * и вешается на {@code SkullMeta#setPlayerProfile}. Кэш ограничен (LRU),
 * головы раздаются клонами — один прототип на игрока.</p>
 */
public final class HeadCache implements PluginModule {

    private static final int LIMIT = 256;

    private final KasaneParrotPlugin plugin;
    private final Map<UUID, ItemStack> cache;

    public HeadCache(@NotNull KasaneParrotPlugin plugin) {
        this.plugin = plugin;
        this.cache = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<UUID, ItemStack> eldest) {
                return size() > LIMIT;
            }
        });
    }

    @Override
    public String id() {
        return "head-cache";
    }

    @Override
    public void onDisable() {
        cache.clear();
    }

    /** Голова игрока (клон — безопасно класть в любое меню). */
    public @NotNull ItemStack head(@NotNull Player player) {
        ItemStack proto;
        synchronized (cache) {
            proto = cache.computeIfAbsent(player.getUniqueId(), this::createHead);
        }
        return proto.clone();
    }

    private @NotNull ItemStack createHead(@NotNull UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        String name = online != null ? online.getName() : null;

        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) stack.getItemMeta();
        PlayerProfile profile = Bukkit.createProfile(uuid, name);
        meta.setPlayerProfile(profile);
        stack.setItemMeta(meta);
        return stack;
    }
}
