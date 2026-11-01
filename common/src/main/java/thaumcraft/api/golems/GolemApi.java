package thaumcraft.api.golems;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.items.golems.GolemMarker;

/**
 * Read and edit a Thaumcraft golem: the native counterpart of what the 1.7.10 Golem Connector of Thaumic Tinkerer did
 * with the internal {@code EntityGolemBase} ({@code getGolemType}, {@code getCore}, {@code decoration},
 * {@code getHealth}, {@code getHomePosition}, {@code homeFacing}, {@code getMarkers}, {@code setHomeArea},
 * {@code setMarkers}). Not a 1.7.10 class: golems had no API.
 *
 * <p><b>Sides and threads.</b> Reads work on both sides; writes only on the server. A golem of a server level belongs to
 * the server thread: every method here throws {@link IllegalStateException} when it is called with such a golem from
 * another thread (a ComputerCraft computer thread, for example; run the call on the main thread). On a client, call it
 * on the client thread.</p>
 *
 * <p><b>Null and other entities.</b> {@code null} and an entity that is no Thaumcraft golem answer "no golem"
 * ({@link Optional#empty()}, {@code false}); they never throw.</p>
 */
public final class GolemApi {
    /** The most markers a golem keeps (the bell's limit); {@link #setMarkers} refuses a longer list. */
    public static final int MAX_MARKERS = 64;

    private GolemApi() {}

    /**
     * Whether the entity is a Thaumcraft golem (the {@code thaumcraft:golem} entity type; the traveling trunk is not a
     * golem). Same answer as {@code ThaumcraftEntities.is(entity, ThaumcraftEntities.GOLEM)}. Any side, any thread.
     *
     * @param entity the entity to ask, or {@code null}
     * @return true for a golem, also a removed one
     */
    public static boolean isGolem(Entity entity) {
        return entity instanceof EntityGolemBase;
    }

    /**
     * A snapshot of the golem: material, core, accessories, health, position, home, home side and markers.
     *
     * <p>On the server every value is the golem's own. On a client the values are the copies the server syncs to it,
     * and the markers, which are never sent to clients, read as an empty list. Read-only: the call changes nothing and
     * loads no chunk. Cheap (it copies at most {@value #MAX_MARKERS} markers).</p>
     *
     * @param entity the entity to read, or {@code null}
     * @return the snapshot, or {@link Optional#empty()} for {@code null}, an entity that is no golem and a golem that is
     *         removed from its level (dead and gone, unloaded, or changed dimension)
     * @throws IllegalStateException when the golem is on a server level and this is not the server thread
     */
    public static Optional<GolemView> view(Entity entity) {
        if (!(entity instanceof EntityGolemBase golem)) return Optional.empty();
        requireOwnerThread(golem);
        if (golem.isRemoved()) return Optional.empty();
        List<GolemView.Marker> markers = new ArrayList<>();
        for (GolemMarker marker : golem.getMarkers()) {
            markers.add(new GolemView.Marker(marker.pos(), ResourceKey.create(Registries.DIMENSION, marker.dimension()),
                    marker.face(), marker.color()));
        }
        return Optional.of(new GolemView(golem.getUUID(), golem.level().dimension(), golem.getGolemMaterial(),
                golem.getGolemCore(), golem.getDecoration(), golem.getHealth(), golem.position(), golem.getHomePosition(),
                golem.getHomeFacing(), markers));
    }

    /**
     * Gives the golem a new home and home side, as the golem placer does when it puts a golem down: the golem's own
     * {@code setHome}. The original connector called {@code setHomeArea(x, y, z, 35)} and set {@code homeFacing}.
     *
     * <p>The effects are the golem's own: the work in progress that depends on the home (a use, alchemy, liquid, harvest,
     * lumber, guard or butcher target, a fishing cast) is dropped, the golem stops its current path, the home and the
     * side are synced to the clients that see the golem, and both are saved with the golem (keys {@code HomeX/Y/Z},
     * {@code HomeFacing}). The home restriction radius is 32, the radius the original gave a golem on every load (the
     * connector's 35 lasted until the next load in 1.7.10; golems in this port never read the radius).</p>
     *
     * <p>The home is a block of the golem's own dimension. Server side only, on the server thread.</p>
     *
     * @param entity the golem, or {@code null}
     * @param home   the new home block; it must lie inside the world (build height and world limit,
     *               {@code Level.isInWorldBounds})
     * @param facing the side of the home block the golem works from (the original {@code homeFacing})
     * @return true when the golem now has exactly this home and side. False, and nothing changed, for {@code null}
     *         arguments, an entity that is no golem, a golem on a client or removed from its level, a home outside the
     *         world, and a golem that refuses (one that is retiring, holds data this version does not support, or is in
     *         the middle of moving cargo; the golem's own guards)
     * @throws IllegalStateException when the golem is on a server level and this is not the server thread
     */
    public static boolean setHome(Entity entity, BlockPos home, Direction facing) {
        if (!(entity instanceof EntityGolemBase golem)) return false;
        requireOwnerThread(golem);
        if (home == null || facing == null || !(golem.level() instanceof ServerLevel level) || golem.isRemoved()
                || !level.isInWorldBounds(home)) return false;
        return golem.setHome(home.immutable(), facing);
    }

    /**
     * Replaces the golem's markers, as the golem bell does: the golem's own {@code setMarkers}, with its rules.
     *
     * <p>The list replaces all markers, in its order (the order the golem routes by). Markers of other dimensions are
     * kept and saved; the golem works only at the markers of its own dimension. A marker with a colour needs the Order
     * upgrade: a golem without it refuses a list that holds a coloured marker, as it refuses that edit from the bell. The
     * effects are the golem's own: work that depends on a marker (use, alchemy and liquid targets) is dropped, the
     * inventory cores re-plan, and the markers are saved with the golem (key {@code Markers}). Markers are server data
     * and are not sent to clients; a golem bell that selected this golem shows the new markers after its next use.</p>
     *
     * <p>Server side only, on the server thread.</p>
     *
     * @param entity  the golem, or {@code null}
     * @param markers the new markers, at most {@value #MAX_MARKERS}; an empty list removes every marker. The list is
     *                copied, not kept
     * @return true when the golem now has exactly these markers. False, and nothing changed, for {@code null}, a list
     *         with a {@code null} entry or more than {@value #MAX_MARKERS} markers, an entity that is no golem, a golem on
     *         a client or removed from its level, a coloured marker on a golem without the Order upgrade, and a golem
     *         that refuses (retiring, unsupported data, cargo in motion)
     * @throws IllegalStateException when the golem is on a server level and this is not the server thread
     */
    public static boolean setMarkers(Entity entity, List<GolemView.Marker> markers) {
        if (!(entity instanceof EntityGolemBase golem)) return false;
        requireOwnerThread(golem);
        if (markers == null || markers.size() > MAX_MARKERS || !(golem.level() instanceof ServerLevel) || golem.isRemoved()) {
            return false;
        }
        List<GolemMarker> converted = new ArrayList<>(markers.size());
        for (GolemView.Marker marker : markers) {
            if (marker == null) return false;
            converted.add(new GolemMarker(marker.dimension().location(), marker.pos(), marker.side(), marker.color()));
        }
        return golem.setMarkers(converted);
    }

    private static void requireOwnerThread(EntityGolemBase golem) {
        if (golem.level() instanceof ServerLevel server && !server.getServer().isSameThread()) {
            throw new IllegalStateException("A golem of a server level is read and changed on the server thread only");
        }
    }
}
