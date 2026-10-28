/**
 * Created by pixlepix on 4/19/14.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import thaumic.tinkerer.common.block.tile.TileForcefield;
import thaumic.tinkerer.common.content.SimpleBlocks;

/**
 * The wall of the Earth Imbued potion: an invisible block that is solid for about three seconds and then vanishes
 * (its block entity counts the ticks). 1.7.10 made it a {@code Material.air} block with the default full collision
 * box: replaceable, invisible, not opaque, no drops.
 */
public class BlockForcefield extends BaseEntityBlock {
    public static final MapCodec<BlockForcefield> CODEC = simpleCodec(BlockForcefield::new);

    public BlockForcefield(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public static BlockBehaviour.Properties defaultProperties() {
        return BlockBehaviour.Properties.of().noOcclusion().replaceable().instabreak().noLootTable()
                .pushReaction(PushReaction.DESTROY).noTerrainParticles();
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /** 1.7.10 {@code getDrops}: an empty list. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileForcefield(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != SimpleBlocks.TILE_FORCEFIELD.get()) return null;
        return (lvl, pos, st, be) -> TileForcefield.tick(lvl, pos, st, (TileForcefield) be);
    }
}
