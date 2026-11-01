package thaumcraft.api.golems;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * An immutable snapshot of a Thaumcraft golem, as {@link GolemApi#view} reads it: what the 1.7.10 Golem Connector of
 * Thaumic Tinkerer read from the internal {@code EntityGolemBase} (type, core, accessories, health, position, home and
 * markers), plus the golem's id and dimension.
 *
 * <p>A snapshot, not a live view: it does not follow the golem afterwards; ask {@link GolemApi#view} again. Building one
 * by hand is allowed (tests do); the constructor only checks that nothing is null and that {@code dimension} is a
 * dimension key, and copies the home and the marker list.</p>
 *
 * @param id          the golem's entity UUID ({@code Entity.getUUID()}), the id the Golem Connector binds to
 * @param dimension   the dimension the golem is in
 * @param materialId  the original material id, 0 (straw) to 7 (thaumium); see {@link #material()}. A golem loaded from
 *                    data this version does not know may hold another number
 * @param core        the animation core: {@link #NO_CORE} (-1) for a golem without a core, otherwise the original
 *                    {@code ItemGolemCore} damage value: 0 fill, 1 empty, 2 gather, 3 harvest, 4 guard, 5 liquid
 *                    (decanting), 6 alchemy (essentia), 7 lumber (chop), 8 use, 9 butcher, 10 sorting, 11 fishing
 * @param decoration  the accessories, one letter each in the order they were put on, exactly as the original
 *                    {@code decoration} field and the entity's {@code Decoration} tag hold them: {@code H} top hat,
 *                    {@code G} spectacles, {@code B} bowtie, {@code F} fez, {@code R} dart launcher, {@code V} visor,
 *                    {@code P} iron plating, {@code M} mace arm. Empty for a golem without accessories, never null
 * @param health      the current health ({@code LivingEntity.getHealth()})
 * @param position    the entity position (the original read {@code posX/posY/posZ})
 * @param home        the home block (original {@code getHomePosition()})
 * @param homeFacing  the side of the home block the golem works from (original {@code homeFacing}, a 3D data value
 *                    of the same order as {@link Direction#get3DDataValue()})
 * @param markers     the markers in the golem's own order (the order it routes by), unmodifiable. Only the server knows
 *                    them: a snapshot of a golem on a client level always holds an empty list
 */
public record GolemView(UUID id, ResourceKey<Level> dimension, int materialId, int core, String decoration, float health,
                        Vec3 position, BlockPos home, Direction homeFacing, List<Marker> markers) {
    /** The {@link #core()} of a golem without an animation core (original {@code getCore() == -1}). */
    public static final int NO_CORE = -1;

    /**
     * Checks that no component is null and that {@code dimension} is a key of the dimension registry, and copies
     * {@code markers} into an unmodifiable list.
     *
     * @throws NullPointerException     for a null component or a null marker
     * @throws IllegalArgumentException for a {@code dimension} key of another registry
     */
    public GolemView {
        Objects.requireNonNull(id, "id");
        requireDimension(dimension);
        Objects.requireNonNull(decoration, "decoration");
        Objects.requireNonNull(position, "position");
        home = Objects.requireNonNull(home, "home").immutable();
        Objects.requireNonNull(homeFacing, "homeFacing");
        markers = List.copyOf(Objects.requireNonNull(markers, "markers"));
    }

    /** The material, or {@code null} when {@link #materialId()} is no TC4 material (the original connector raised "Unknown Golem Type"). */
    public GolemMaterial material() {
        return GolemMaterial.byId(materialId);
    }

    /** True when the golem has an animation core ({@link #core()} is not {@link #NO_CORE}). */
    public boolean hasCore() {
        return core != NO_CORE;
    }

    static ResourceKey<Level> requireDimension(ResourceKey<Level> dimension) {
        Objects.requireNonNull(dimension, "dimension");
        if (!dimension.isFor(Registries.DIMENSION)) throw new IllegalArgumentException("Not a dimension key: " + dimension);
        return dimension;
    }

    /**
     * One golem marker: a block face in a dimension, with an optional Order colour. The read view of the original
     * {@code thaumcraft.common.entities.golems.Marker} ({@code x, y, z, dim, side, color}) and the value
     * {@link GolemApi#setMarkers} takes.
     *
     * <p>Markers of another dimension than the golem's are kept and saved, as the original kept them; the golem never
     * works at them (the bell and the golem only act in the golem's own dimension).</p>
     *
     * @param pos       the marked block
     * @param dimension the dimension of the marked block (the original stored a numeric dimension id)
     * @param side      the marked face of the block (the original {@code side}, a {@code ForgeDirection} ordinal: the same
     *                  order as {@link Direction#get3DDataValue()})
     * @param color     the Order colour: {@link #ANY_COLOR} (-1) for an ordinary marker, 0 to 15 for a dye colour
     *                  ({@code DyeColor.getId()} order)
     */
    public record Marker(BlockPos pos, ResourceKey<Level> dimension, Direction side, int color) {
        /** The colour of an ordinary, uncoloured marker. */
        public static final int ANY_COLOR = -1;
        /** The highest dye colour a marker can have. */
        public static final int LAST_COLOR = 15;

        /**
         * Checks the components and makes {@code pos} immutable.
         *
         * @throws NullPointerException     for a null component
         * @throws IllegalArgumentException for a colour outside -1..15 or a {@code dimension} key of another registry
         */
        public Marker {
            pos = Objects.requireNonNull(pos, "pos").immutable();
            requireDimension(dimension);
            Objects.requireNonNull(side, "side");
            if (color < ANY_COLOR || color > LAST_COLOR) throw new IllegalArgumentException("Invalid golem marker colour: " + color);
        }

        /** An ordinary marker without a colour. */
        public Marker(BlockPos pos, ResourceKey<Level> dimension, Direction side) {
            this(pos, dimension, side, ANY_COLOR);
        }

        /** True for a marker with an Order colour (0..15). */
        public boolean hasColor() {
            return color != ANY_COLOR;
        }
    }
}
