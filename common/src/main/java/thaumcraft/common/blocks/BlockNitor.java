package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftResources;
import thaumcraft.common.tiles.TileNitor;

/** The metadata-1 slice of TC4 BlockAiry, not a torch or a vanilla fire. */
public final class BlockNitor extends BaseEntityBlock {
    public static final MapCodec<BlockNitor> CODEC = simpleCodec(BlockNitor::new);
    private static final VoxelShape SHAPE = box(4.8, 4.8, 4.8, 11.2, 11.2, 11.2);
    private static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "blocks/nitor"));

    public BlockNitor(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileNitor(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? createTickerHelper(type, ThaumcraftResources.NITOR_BLOCK_ENTITY.get(), TileNitor::clientTick) : null;
    }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        TileNitor.animate(level, pos, random);
    }

    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        // There is deliberately no second BlockItem and no AIR/asItem fallback.
        // The native table names the existing MATERIALS nitor and applies the
        // native explosion-survival condition; never also pop an item here.
        var params = builder.withParameter(LootContextParams.BLOCK_STATE, state).create(LootContextParamSets.BLOCK);
        return params.getLevel().getServer().reloadableRegistries().getLootTable(LOOT).getRandomItems(params);
    }
    @Override public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(ThaumcraftContent.MATERIALS.get("nitor").get());
    }
}
