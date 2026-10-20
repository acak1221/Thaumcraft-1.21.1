package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.config.ThaumcraftShockOrbs;

/** Original BlockAiry10. Native isAir must be false so LevelChunkSection counts this randomly ticking block. */
public final class BlockShockTrap extends Block {
    private static java.util.function.BiConsumer<Level,BlockPos> clientEffect;
    public BlockShockTrap(Properties properties) { super(properties); }
    public static void setClientEffect(java.util.function.BiConsumer<Level,BlockPos> effect) { clientEffect=java.util.Objects.requireNonNull(effect); }
    public static boolean semanticAir(BlockState state) { return state.isAir()||state.is(ThaumcraftShockOrbs.SHOCK_TRAP.get()); }
    public static boolean originalAiry(BlockState state) {
        // Original Airy3 arcane light and Airy10 are both semantic-air states but must never be replaced by another field.
        return state.is(ThaumcraftShockOrbs.SHOCK_TRAP.get())||state.getBlock() instanceof BlockArcaneLight;
    }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return Shapes.empty(); }
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return Shapes.empty(); }
    @Override protected VoxelShape getOcclusionShape(BlockState state,BlockGetter level,BlockPos pos) { return Shapes.empty(); }
    @Override protected void entityInside(BlockState state,Level level,BlockPos pos,Entity entity) {
        entity.hurt(level.damageSources().magic(),1+level.random.nextInt(2));
        Vec3 motion=entity.getDeltaMovement();entity.setDeltaMovement(motion.x*.8,motion.y,motion.z*.8);
        if(level instanceof ServerLevel server&&server.random.nextInt(100)==0)server.removeBlock(pos,false);
    }
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) { level.removeBlock(pos,false); }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random) { if(clientEffect!=null)clientEffect.accept(level,pos); }
}
