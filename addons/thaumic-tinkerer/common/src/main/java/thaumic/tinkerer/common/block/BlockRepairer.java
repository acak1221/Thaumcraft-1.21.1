/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.api.ItemApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.block.tile.TileRepairer;
import thaumic.tinkerer.common.content.Machines;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.ThaumicTinkererInfusionRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.research.IRegisterableResearch;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

public class BlockRepairer extends BaseEntityBlock implements ITTinkererRegisterable {

    /** Metadata 2..5 of 1.7.10: the side of the tube connection, the way the player looked at it when placing. */
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public static final MapCodec<BlockRepairer> CODEC = simpleCodec(BlockRepairer::new);

    public BlockRepairer(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // l1 = floor(yaw * 4 / 360 + 0.5) & 3: 0 -> metadata 2, 1 -> 5, 2 -> 3, 3 -> 4 is the opposite of the way the
        // player looks.
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        return activate(level, pos, player, hand) ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        return activate(level, pos, player, InteractionHand.MAIN_HAND) ? InteractionResult.sidedSuccess(level.isClientSide)
                : InteractionResult.PASS;
    }

    /** {@code onBlockActivated}: puts a damaged tool in, or gives back whatever is in the slot. */
    private static boolean activate(Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!(level.getBlockEntity(pos) instanceof TileRepairer repairer)) return false;
        ItemStack stack = repairer.getItem(0);

        if (stack.isEmpty()) {
            ItemStack playerStack = player.getItemInHand(hand);
            if (TileRepairer.isRepairable(playerStack)) {
                if (!level.isClientSide) repairer.setItem(0, playerStack.split(1));
                return true;
            }
        } else {
            if (!level.isClientSide) {
                ItemStack given = stack.copy();
                repairer.setItem(0, ItemStack.EMPTY);
                if (!player.getInventory().add(given)) player.drop(given, false);
            }
            return true;
        }

        return false;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof TileRepairer repairer) {
                MachineDrops.scatter(level, pos, repairer);
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileRepairer(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return createTickerHelper(type, Machines.TILE_REPAIRER.get(), TileRepairer::tick);
    }

    /** {@code canProvidePower}: the repairer is a redstone source while it holds a damaged tool. */
    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    /** {@code isProvidingStrongPower}: 15 while the tool in the slot is damaged. */
    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (level.getBlockEntity(pos) instanceof TileRepairer repairer) {
            ItemStack stack = repairer.getItem(0);
            if (!stack.isEmpty() && stack.getDamageValue() != 0) return 15;
        }
        return 0;
    }

    @Override
    public IRegisterableResearch getResearchItem() {
        return (IRegisterableResearch) new TTResearchItem(LibResearch.KEY_REPAIRER,
                new AspectList().add(Aspect.TOOL, 2).add(Aspect.CRAFT, 1).add(Aspect.ORDER, 1).add(Aspect.MAGIC, 1), -1,
                -9, 3, new ItemStack(this)).setConcealed().setParents(LibResearch.KEY_FUNNEL)
                        .setParentsHidden("THAUMIUM", "ENCHFABRIC")
                        .setPages(new ResearchPage("0"), ResearchHelper.infusionPage(LibResearch.KEY_REPAIRER));
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererInfusionRecipe(LibResearch.KEY_REPAIRER, new ItemStack(this), 8,
                new AspectList().add(Aspect.TOOL, 15).add(Aspect.CRAFT, 20).add(Aspect.ORDER, 10).add(Aspect.MAGIC, 15),
                ItemApi.getBlock("blockCosmeticSolid", 4), new ItemStack(Items.IRON_INGOT),
                new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.DIAMOND), new ItemStack(Items.COBBLESTONE),
                new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.LEATHER), ItemApi.getItem("itemResource", 7),
                ItemApi.getItem("itemResource", 2));
    }
}
