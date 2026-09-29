package dev.dolbaeb.kasaneparrot.flight;

import dev.dolbaeb.kasaneparrot.KasaneParrotPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Поиск безопасного места посадки рядом с получателем.
 *
 * <p>Алгоритм: генерируем кандидатов (дуга перед взглядом + кольца вокруг
 * игрока), для каждого ищем землю сканированием столба вниз, проверяем
 * безопасность (2 блока воздуха, отсутствие опасных поверхностей),
 * скорим по близости к игроку и направлению взгляда.</p>
 */
public final class LandingSpotFinder {

    /** Максимальная глубина поиска земли вниз от уровня игрока. */
    private static final int SEARCH_DOWN = 10;
    /** Максимальная высота поиска земли вверх от уровня игрока. */
    private static final int SEARCH_UP = 5;

    private final KasaneParrotPlugin plugin;

    public LandingSpotFinder(@NotNull KasaneParrotPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Ищет лучшее место для «выдачи» посылки рядом с игроком.
     *
     * @return пусто, если безопасной площадки нет (попугай будет летать рядом)
     */
    public Optional<LandingSpot> find(@NotNull Player target) {
        Location eyes = target.getEyeLocation();
        var scan = plugin.cfg().flight().landing;

        LandingSpot best = null;

        // 1. Дуга перед взглядом: 8 направлений вокруг yaw игрока.
        for (int i = -2; i <= 2; i++) {
            double yawRad = Math.toRadians(eyes.getYaw() + i * 30.0D);
            double dx = -Math.sin(yawRad);
            double dz = Math.cos(yawRad);
            double dist = 2.5D;
            Location candidate = eyes.clone().add(dx * dist, 0, dz * dist);
            best = better(best, groundSpot(candidate, target, LandingSpot.Strategy.IN_FRONT, 1.15D));
        }

        // 2. Кольца вокруг игрока: радиусы от min-distance до scan-radius.
        for (double radius = scan.minDistance; radius <= scan.scanRadius + 0.01D; radius += 2.0D) {
            int points = Math.max(6, (int) (radius * 4));
            for (int i = 0; i < points; i++) {
                double angle = (Math.PI * 2.0D * i) / points;
                Location candidate = eyes.clone().add(
                        Math.cos(angle) * radius, 0, Math.sin(angle) * radius
                );
                best = better(best, groundSpot(candidate, target, LandingSpot.Strategy.NEARBY_GROUND, 1.0D));
            }
        }

        return Optional.ofNullable(best);
    }

    /* ------------------------------------------------------------------ */

    @Nullable
    private LandingSpot groundSpot(@NotNull Location candidate,
                                   @NotNull Player target,
                                   @NotNull LandingSpot.Strategy strategy,
                                   double bias) {
        Location ground = groundBelow(candidate);
        if (ground == null) {
            return null;
        }

        // Скоринг: ближе к игроку — лучше; небольшой штраф за перепад высоты.
        double distance = ground.distance(target.getLocation());
        if (distance < plugin.cfg().flight().landing.minDistance) {
            return null;
        }
        double score = bias * (1.0D / (1.0D + distance))
                - Math.abs(ground.getY() - target.getLocation().getY()) * 0.01D;
        return new LandingSpot(ground, score, strategy);
    }

    /**
     * Ищет безопасную точку земли в столбе под кандидатом:
     * сверху вниз от {@code y + SEARCH_UP} до {@code y - SEARCH_DOWN}.
     */
    @Nullable
    private Location groundBelow(@NotNull Location candidate) {
        var scan = plugin.cfg().flight().landing;
        int x = candidate.getBlockX();
        int z = candidate.getBlockZ();
        var world = candidate.getWorld();
        if (world == null) {
            return null;
        }

        for (int y = candidate.getBlockY() + SEARCH_UP; y >= candidate.getBlockY() - SEARCH_DOWN; y--) {
            Block surface = world.getBlockAt(x, y, z);
            Block above1 = surface.getRelative(org.bukkit.block.BlockFace.UP);
            Block above2 = above1.getRelative(org.bukkit.block.BlockFace.UP);
            Block below = surface.getRelative(org.bukkit.block.BlockFace.DOWN);

            // Поверхность должна быть непустой, над ней — два воздуха.
            if (!isSafeGround(surface) || !above1.getType().isAir() || !above2.getType().isAir()) {
                continue;
            }
            // Опора под поверхностью — твёрдая (не сыпучая обманка).
            if (!below.getType().isSolid()) {
                continue;
            }
            if (!isSafeSurface(surface, below)) {
                continue;
            }
            return new Location(world, x + 0.5D, y + 1.0D, z + 0.5D);
        }
        return null;
    }

    private boolean isSafeGround(@NotNull Block block) {
        Material type = block.getType();
        if (!type.isSolid()) {
            return false;
        }
        return !plugin.cfg().flight().landing.forbiddenMaterials.contains(type);
    }

    /** Дополнительные проверки поверхности: вода, опасные соседи. */
    private boolean isSafeSurface(@NotNull Block surface, @NotNull Block below) {
        var scan = plugin.cfg().flight().landing;
        Material surfaceType = surface.getType();
        Material belowType = below.getType();

        if (scan.forbiddenMaterials.contains(surfaceType)
                || scan.forbiddenMaterials.contains(belowType)) {
            return false;
        }
        if (scan.avoidWater && (surfaceType == Material.WATER || belowType == Material.WATER)) {
            return false;
        }
        // Не садимся на верхнюю грань блока под опасным блоком сверху.
        return !surface.getRelative(org.bukkit.block.BlockFace.UP, 2).getType().isSolid();
    }

    @Nullable
    private static LandingSpot better(@Nullable LandingSpot current, @Nullable LandingSpot candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null || candidate.score() > current.score()) {
            return candidate;
        }
        return current;
    }
}
