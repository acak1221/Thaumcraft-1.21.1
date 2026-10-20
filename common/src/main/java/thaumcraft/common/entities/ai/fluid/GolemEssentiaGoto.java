package thaumcraft.common.entities.ai.fluid;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.items.golems.GolemMarker;
import thaumcraft.common.lib.golems.GolemAiRules;
import thaumcraft.common.tiles.TileJar;

/**
 * Original {@code AIEssentiaGoto} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/fluid/AIEssentiaGoto.java}).
 *
 * <p>A golem that carries essentia walks to the nearest marked jar that still has room for that aspect. When the golem
 * does not move for two hundred ticks it takes a small detour, exactly as the original did, and gives up after the detour
 * fails.</p>
 *
 * <p>The original registered this goal with priority 3.</p>
 */
public final class GolemEssentiaGoto extends Goal {
    /** The original 1.7 goal priority of {@code AIEssentiaGoto}. */
    public static final int PRIORITY = 3;

    private final EntityGolemBase golem;
    private final GolemAiRules.Cargo cargo;
    private BlockPos destination;
    private int count;
    private int previousX, previousY, previousZ;

    public GolemEssentiaGoto(EntityGolemBase golem) {
        this(golem, GolemAiRules.cargoOf(golem));
    }

    public GolemEssentiaGoto(EntityGolemBase golem, GolemAiRules.Cargo cargo) {
        this.golem = golem;
        this.cargo = cargo;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public boolean canUse() {
        if (!GolemAiRules.searchTick(golem.tickCount)) return false;
        Aspect aspect = cargo.aspect();
        if (aspect == null || cargo.essentiaAmount() == 0) return false;
        destination = findJarWithRoom(aspect);
        return destination != null;
    }

    @Override public boolean canContinueToUse() { return count > 0 && !golem.getNavigation().isDone(); }

    @Override public void start() {
        count = GolemAiRules.pathBudget();
        previousX = golem.blockPosition().getX();
        previousY = golem.blockPosition().getY();
        previousZ = golem.blockPosition().getZ();
        golem.getNavigation().moveTo(destination.getX() + 0.5, destination.getY() + 0.5, destination.getZ() + 0.5,
                golem.getAttributeValue(Attributes.MOVEMENT_SPEED));
    }

    @Override public void stop() { count = 0; }

    @Override public void tick() {
        if (--count != 0) return;
        BlockPos here = golem.blockPosition();
        if (previousX != here.getX() || previousY != here.getY() || previousZ != here.getZ()) return;
        Vec3 detour = DefaultRandomPos.getPos(golem, 2, 1);
        if (detour == null) return;
        count = GolemAiRules.detourBudget();
        golem.getNavigation().moveTo(detour.x, detour.y, detour.z,
                golem.getAttributeValue(Attributes.MOVEMENT_SPEED));
    }

    /**
     * Original {@code GolemHelper.findJarWithRoom}: the nearest marked essentia container inside the golem's range that
     * still accepts the carried aspect. The original also walks into the jars that are connected to a marked one; the
     * port does the same, one block at a time.
     */
    private BlockPos findJarWithRoom(Aspect aspect) {
        if (!(golem.level() instanceof ServerLevel server)) return null;
        BlockPos home = golem.getHomePosition();
        float range = golem.getRange();
        double rangeSqr = range * range;
        BlockPos best = null;
        double nearest = Double.MAX_VALUE;
        for (GolemMarker marker : golem.getMarkers()) {
            if (!marker.dimension().equals(server.dimension().location())) continue;
            if (!accepts(server, marker, aspect)) continue;
            double distance = Math.sqrt(home.distSqr(marker.pos()));
            if (distance > range || distance >= nearest) continue;
            nearest = distance;
            best = marker.pos();
        }
        return best;
    }

    private boolean accepts(ServerLevel server, GolemMarker marker, Aspect aspect) {
        BlockEntity tile = server.getBlockEntity(marker.pos());
        if (!(tile instanceof IEssentiaTransport transport)) return false;
        if (!transport.canInputFrom(marker.face())) return false;
        if (transport.getSuctionAmount(marker.face()) <= 0) return false;
        Aspect suction = transport.getSuctionType(marker.face());
        if (suction != null && suction != aspect) return false;
        if (tile instanceof TileJar jar) {
            if (jar.getAmount() >= jar.getMaxAmount()) return false;
            return jar.getAmount() == 0 || jar.getAspect() == aspect;
        }
        return true;
    }
}
