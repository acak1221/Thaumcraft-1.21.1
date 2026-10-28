/**
 * Created by Katrina on 14/04/2015.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block.tile;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.blocks.BlocksTC;
import thaumic.tinkerer.common.content.SimpleBlocks;

/**
 * The block entity of the Paving Slab of Warding: every five ticks it pushes the mobs that hang above it away, and
 * every hundred ticks it puts Thaumcraft's warding aura into the two blocks above, both only without a redstone signal
 * (the aura is kept even with one, as before).
 */
public class TileWardedSlab extends BlockEntity {

    int count = 0;

    public TileWardedSlab(BlockPos pos, BlockState state) {
        super(SimpleBlocks.TILE_WARDED_SLAB.get(), pos, state);
    }

    public boolean gettingPower() {
        return level != null && level.hasNeighborSignal(worldPosition);
    }

    /** 1.7.10 {@code updateEntity} (server side only). */
    public static void serverTick(Level level, BlockPos pos, BlockState state, TileWardedSlab tile) {
        if (tile.count == 0) {
            tile.count = level.random.nextInt(100);
        }

        if (tile.count % 5 == 0 && !tile.gettingPower()) {
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(pos.getX(), pos.getY() - 1, pos.getZ(), pos.getX() + 1, pos.getY() + 3, pos.getZ() + 1)
                            .inflate(0.1D, 0.1D, 0.1D));
            for (LivingEntity e : targets) {
                if (!e.onGround() && !(e instanceof Player)) {
                    e.push(-Mth.sin((e.getYRot() + 180.0F) * 3.1415927F / 180.0F) * 0.2F, -0.1D,
                            Mth.cos((e.getYRot() + 180.0F) * 3.1415927F / 180.0F) * 0.2F);
                }
            }
        }

        if (++tile.count % 100 == 0) {
            seal(level, pos.above());
            seal(level, pos.above(2));
        }
    }

    /**
     * Puts {@code blockAiry:4} (the warding aura) into the cell unless it holds {@code blockAiry:3} already or cannot
     * be replaced. A cell that holds the aura already is left alone (1.7.10 replaced it with itself).
     */
    private static void seal(Level level, BlockPos target) {
        BlockState aura = BlocksTC.state("blockAiry", 4);
        BlockState glimmer = BlocksTC.state("blockAiry", 3);
        if (aura == null) return;
        BlockState there = level.getBlockState(target);
        if (there == aura) return;
        if (glimmer != null && there == glimmer) return;
        if (there.canBeReplaced()) {
            level.setBlock(target, aura, 3);
        }
    }
}
