package thaumic.tinkerer.common.core.golems;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;

/**
 * What the Golem Connector needs to know about, and change in, a Thaumcraft golem (type, core, accessories, health,
 * position, home and markers).
 *
 * <p>Thaumcraft's golems have no public API ({@code docs/api/PORTING.md}: "Golems had no API in 1.7.10 and have none
 * now"), and the add-on may not reach into the mod. This interface is the shape the API is asked to provide
 * ({@code thaumcraft.api.golems}, request A11 in docs/api-requests/TileBlocks.md); until then {@link GolemAccessProvider}
 * answers with {@link UnavailableGolemAccess}, which behaves like the 1.7.10 connector without a golem.</p>
 */
public interface GolemAccess {

    /** A marker of a golem: a block position, its dimension, the side and the colour (-1 for any). */
    record Marker(int x, int y, int z, String dimension, byte side, byte color) {}

    /** True when the golem is there and readable. */
    boolean exists();

    /** "Clay", "Flesh", "Iron", "Stone", "Straw", "Tallow", "Thaumium" or "Wood"; null when the type has no such name. */
    @Nullable
    String typeName();

    /** The animation core: 0 to 11 as in {@link EnumGolemCores}, or -1 for a golem without a core. */
    int core();

    /** The accessories, one character each (see {@link EnumGolemDecorations}). */
    String decoration();

    float health();

    double x();

    double y();

    double z();

    BlockPos home();

    int homeFacing();

    List<Marker> markers();

    /** 1.7.10 {@code setHomeArea(x, y, z, 35)} and {@code homeFacing}. */
    void setHome(BlockPos home, int facing);

    void setMarkers(List<Marker> markers);
}
