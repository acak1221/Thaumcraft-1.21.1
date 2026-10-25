package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.lib.loot.LootTables;
import thaumcraft.common.lib.world.EldritchBlockRules;

/**
 * Original BlockLoot: the loot urn and the loot crate. The original block
 * metadata zero to two is the rarity of the generated loot.
 */
public final class BlockLoot extends Block {
    public static final MapCodec<BlockLoot> CODEC = simpleCodec(BlockLoot::new);
    /** Original BlockLoot metadata zero to two, the rarity of the container. */
    public static final IntegerProperty RARITY = IntegerProperty.create("rarity", 0, 2);

    public BlockLoot(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(RARITY, 0));
    }

    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(RARITY); }

    public static int rarityOf(BlockState state) { return state.getValue(RARITY); }

    /** Original ItemBlock metadata becomes the placed container's loot tier. */
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Integer rarity = BlockLootItem.rarity(context.getItemInHand());
        return defaultBlockState().setValue(RARITY, EldritchBlockRules.lootRarity(rarity == null ? 0 : rarity));
    }

    /** Original BlockLoot.damageDropped preserves metadata for creative pick-block. */
    @Override public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack result = new ItemStack(this);
        BlockLootItem.setRarity(result, state.getValue(RARITY));
        return result;
    }

    /** Original BlockLoot:101-105 exposes all three metadata tiers in the creative tab. */
    public static List<ItemStack> creativeVariants(net.minecraft.world.item.Item item) {
        List<ItemStack> variants = new ArrayList<>();
        for (int rarity = 0; rarity <= 2; rarity++) {
            ItemStack stack = new ItemStack(item);
            BlockLootItem.setRarity(stack, rarity);
            variants.add(stack);
        }
        return variants;
    }

    /** BlockLoot:78-84 - the container drops one to three plus rarity items. */
    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        int rarity = EldritchBlockRules.lootRarity(state.getValue(RARITY));
        if (!(params.getLevel() instanceof ServerLevel level)) return List.of();
        RandomSource random = level.getRandom();
        int count = EldritchBlockRules.lootDropCount(rarity, random.nextInt(3));
        List<ItemStack> drops = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ItemStack stack = LootTables.generateLoot(level, rarity, random);
            if (stack == null || stack.isEmpty()) continue;
            drops.add(stack);
        }
        return drops;
    }
}
