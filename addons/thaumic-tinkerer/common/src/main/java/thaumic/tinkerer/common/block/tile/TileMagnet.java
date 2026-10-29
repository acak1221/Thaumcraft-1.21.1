/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [12 Sep 2013, 17:10:55 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block.tile;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.client.ThaumcraftClientFX;
import thaumic.tinkerer.common.block.BlockMagnet;
import thaumic.tinkerer.common.content.Machines;

/**
 * The Kinetic Attractor. It runs on both sides, as in 1.7.10: the server moves the entities (and sends the motion),
 * the client repeats the motion and shows a sparkle on every entity that is moved.
 *
 * <p>The polarity is the block state {@link BlockMagnet#PULLING} (bit 0 of the 1.7.10 metadata: 0 pulls, "blue"; 1
 * pushes, "red"); the Corporeal Attractor ({@link TileMobMagnet}) is a block of its own (bit 1 of the metadata).</p>
 */
public class TileMagnet extends BlockEntity {

    public TileMagnet(BlockPos pos, BlockState state) {
        this(Machines.TILE_MAGNET.get(), pos, state);
    }

    protected TileMagnet(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileMagnet magnet) {
        magnet.update(level, pos, state);
    }

    /** The redstone level the magnet is powered with: the strongest signal of its six neighbours. */
    public int getRedstone() {
        return level == null ? 0 : level.getBestNeighborSignal(worldPosition);
    }

    private void update(Level level, BlockPos pos, BlockState state) {
        int redstone = level.getBestNeighborSignal(pos);

        if (redstone > 0) {
            double x1 = pos.getX() + 0.5;
            double y1 = pos.getY() + 0.5;
            double z1 = pos.getZ() + 0.5;

            boolean blue = isPulling(state);
            int speedMod = blue ? 1 : -1;
            double range = rangeFor(redstone);

            AABB boundingBox = new AABB(x1 - range, pos.getY(), z1 - range, x1 + range, y1 + range, z1 + range);
            List<Entity> entities = level.getEntitiesOfClass(Entity.class, boundingBox, this::isSelected);

            for (Entity entity : entities) {
                double x2 = entity.getX();
                double y2 = entity.getY();
                double z2 = entity.getZ();
                // 1.7.10 dropped items were positioned at their centre, everything else at its feet.
                if (entity instanceof ItemEntity) y2 += entity.getBbHeight() / 2;

                float distanceSqrd = blue ? (float) ((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2) + (z1 - z2) * (z1 - z2))
                        : 1.1F;

                if (distanceSqrd > 1) {
                    setEntityMotionFromVector(entity, new Vec3(x1, y1, z1), speedMod * 0.25F);
                    if (level.isClientSide) {
                        ThaumcraftClientFX.sparkle(level, x2, y2, z2, blue ? 2 : 4);
                    }
                }
            }
        }
    }

    /** The entities the magnet moves: dropped items. */
    public boolean isSelected(Entity entity) {
        return entity instanceof ItemEntity;
    }

    /** True when the magnet pulls ("blue"), false when it pushes ("red"). */
    public boolean isPulling() {
        return isPulling(getBlockState());
    }

    private static boolean isPulling(BlockState state) {
        return !state.hasProperty(BlockMagnet.PULLING) || state.getValue(BlockMagnet.PULLING);
    }

    /** {@code setPulling} of the peripheral: changes the polarity the way a click does. */
    public void setPulling(boolean pulling) {
        if (level == null || level.isClientSide) return;
        BlockState state = getBlockState();
        if (state.hasProperty(BlockMagnet.PULLING) && state.getValue(BlockMagnet.PULLING) != pulling) {
            level.setBlock(worldPosition, state.setValue(BlockMagnet.PULLING, pulling), 3);
        }
    }

    /** The half width of the box the magnet searches at a redstone level: an integer division, as in 1.7.10. */
    public static int rangeFor(int redstone) {
        return redstone / 2;
    }

    /**
     * {@code MiscHelper.setEntityMotionFromVector} of 1.7.10: the motion is the vector from the centre of the entity
     * to the point, normalised when longer than one block, times the modifier.
     */
    static void setEntityMotionFromVector(Entity entity, Vec3 point, float modifier) {
        Vec3 entityCenter = new Vec3(entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ());
        entity.setDeltaMovement(motionTowards(entityCenter, point, modifier));
    }

    /** The motion of an entity with the given centre that a magnet at {@code point} causes. */
    public static Vec3 motionTowards(Vec3 entityCenter, Vec3 point, float modifier) {
        Vec3 finalVector = point.subtract(entityCenter);

        if (finalVector.length() > 1) finalVector = finalVector.normalize();

        return new Vec3(finalVector.x * modifier, finalVector.y * modifier, finalVector.z * modifier);
    }
}
