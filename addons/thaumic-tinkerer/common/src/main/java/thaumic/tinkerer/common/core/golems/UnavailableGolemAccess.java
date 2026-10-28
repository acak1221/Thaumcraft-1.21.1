package thaumic.tinkerer.common.core.golems;

import java.util.List;
import net.minecraft.core.BlockPos;

/** The stand-in for a golem that cannot be read: the answers of the 1.7.10 connector that has no golem bound. */
public final class UnavailableGolemAccess implements GolemAccess {
    public static final UnavailableGolemAccess INSTANCE = new UnavailableGolemAccess();

    private UnavailableGolemAccess() {}

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public String typeName() {
        return null;
    }

    @Override
    public int core() {
        return -1;
    }

    @Override
    public String decoration() {
        return "";
    }

    @Override
    public float health() {
        return 0;
    }

    @Override
    public double x() {
        return 0;
    }

    @Override
    public double y() {
        return 0;
    }

    @Override
    public double z() {
        return 0;
    }

    @Override
    public BlockPos home() {
        return BlockPos.ZERO;
    }

    @Override
    public int homeFacing() {
        return 0;
    }

    @Override
    public List<Marker> markers() {
        return List.of();
    }

    @Override
    public void setHome(BlockPos home, int facing) {}

    @Override
    public void setMarkers(List<Marker> markers) {}
}
