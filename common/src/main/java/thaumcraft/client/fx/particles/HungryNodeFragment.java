package thaumcraft.client.fx.particles;

import java.lang.ref.WeakReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.tiles.TileNode;

/** Original FXBoreParticles inward acceleration/friction/scale with native block atlas sprites. */
public final class HungryNodeFragment extends TerrainParticle {
    private final WeakReference<TileNode> node;
    private final BlockPos source;
    private final Vec3 destination;
    public HungryNodeFragment(ClientLevel level,TileNode node,BlockPos source,BlockState state) {
        super(level,source.getX()+level.random.nextFloat(),source.getY()+level.random.nextFloat(),source.getZ()+level.random.nextFloat(),0,0,0,state,source);
        this.node = new WeakReference<>(node); this.source = source.immutable(); destination = Vec3.atCenterOf(node.getBlockPos());
        gravity = 0; hasPhysics = false; // Native visual-only adaptation: no old solid-block push-out routine.
        quadSize = .1F * (random.nextFloat() * .3F + .4F);
        int base = Math.max(1,(int)(destination.distanceTo(new Vec3(x,y,z))*3)); lifetime = base/2+random.nextInt(base);
        xd = level.random.nextGaussian() * .01F; yd = level.random.nextGaussian() * .01F; zd = level.random.nextGaussian() * .01F;
    }
    @Override public void tick() {
        xo=x; yo=y; zo=z;
        TileNode target=node.get();
        if (Minecraft.getInstance().level != level || target==null || target.isRemoved() || target.getLevel()!=level
                || !level.hasChunkAt(target.getBlockPos()) || level.getBlockEntity(target.getBlockPos())!=target
                || !level.hasChunkAt(source) || !level.hasChunkAt(BlockPos.containing(x,y,z)) || age++>=lifetime || BlockPos.containing(x,y,z).equals(target.getBlockPos())) { remove(); return; }
        move(xd,yd,zd); xd*=.985D; yd*=.985D; zd*=.985D;
        Vec3 difference=destination.subtract(x,y,z); double distance=difference.length();
        if (!Double.isFinite(distance) || distance<1e-12) { remove(); return; }
        double acceleration=.3D;
        if (distance<4) { quadSize*=.9F; acceleration=.6D; }
        xd=Mth.clamp((float)(xd+difference.x/distance*acceleration),-.35F,.35F);
        yd=Mth.clamp((float)(yd+difference.y/distance*acceleration),-.35F,.35F);
        zd=Mth.clamp((float)(zd+difference.z/distance*acceleration),-.35F,.35F);
    }
}
