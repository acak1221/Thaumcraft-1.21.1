package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import dev.architectury.registry.menu.MenuRegistry;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.items.ItemInkwell;
import thaumcraft.common.lib.research.TableTopology;
import thaumcraft.common.tiles.TileResearchTable;
import thaumcraft.common.tiles.TileTable;

/** Original base/main/secondary states; arcane/deconstruction are separate units. */
public final class BlockTable extends BaseEntityBlock {
    public static final MapCodec<BlockTable> CODEC = simpleCodec(BlockTable::new);
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 9);
    private static final int CONVERSION_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final VoxelShape NORTH_SELECTION = box(0, 0, -16, 16, 16, 16);
    private static final VoxelShape SOUTH_SELECTION = box(0, 0, 0, 16, 16, 32);
    private static final VoxelShape WEST_SELECTION = box(-16, 0, 0, 16, 16, 16);
    private static final VoxelShape EAST_SELECTION = box(0, 0, 0, 32, 16, 16);

    public BlockTable(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(VARIANT, 0));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(VARIANT); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TableTopology.isMain(state.getValue(VARIANT)) ? new TileResearchTable(pos, state) : new TileTable(pos, state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || state.getValue(VARIANT) < 2) return null;
        // Chunk deserialization does not call onPlace and need not contain a
        // saved scheduled tick. Only the first native BE tick requests repair.
        return TableTopology.isMain(state.getValue(VARIANT))
                ? createTickerHelper(type, ThaumcraftContent.RESEARCH_TABLE_BLOCK_ENTITY.get(), TileResearchTable::tickTopologyOnLoad)
                : createTickerHelper(type, ThaumcraftContent.TABLE_BLOCK_ENTITY.get(), TileTable::tickTopologyOnLoad);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(VARIANT, TableTopology.placementVariant(context.getHorizontalDirection()));
    }
    public BlockState state(int variant) { return defaultBlockState().setValue(VARIANT, variant); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }

    // Old selected bounding box was separate from collision/ray bounds. A narrow
    // client outline adapter uses selectionShape; native picking stays one cube.
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.block(); }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.block(); }
    @Override protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) { return Shapes.block(); }
    public static VoxelShape selectionShape(BlockState state) {
        return switch (state.getValue(VARIANT)) {
            case 2, 6 -> NORTH_SELECTION;
            case 3, 7 -> SOUTH_SELECTION;
            case 4, 8 -> WEST_SELECTION;
            case 5, 9 -> EAST_SELECTION;
            default -> Shapes.block();
        };
    }

    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(ThaumcraftContent.TABLE_ITEM.get()));
    }
    @Override public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(state.getValue(VARIANT) >= 2
                ? ThaumcraftContent.RESEARCH_TABLE_ITEM.get() : ThaumcraftContent.TABLE_ITEM.get());
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
                                                       Player player, InteractionHand hand, BlockHitResult hit) {
        // Original onItemUseFirst ran before block activation, including when
        // reconverting a secondary half. A late Item.useOn alone loses that path.
        if (held.getItem() instanceof ItemInkwell inkwell) {
            InteractionResult result = inkwell.tryCreateResearchTable(level, pos, player, hand);
            if (result == InteractionResult.FAIL) return ItemInteractionResult.FAIL;
            if (result.consumesAction()) return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                         Player player, BlockHitResult hit) {
        if (state.getValue(VARIANT) <= 1 || player.isShiftKeyDown() || level.getBlockEntity(pos) == null) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer) {
            TileResearchTable main = findOpeningMain(level, pos);
            if (main != null) MenuRegistry.openExtendedMenu(serverPlayer, main, buffer -> buffer.writeBlockPos(main.getBlockPos()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static TileResearchTable findOpeningMain(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return null;
        if (level.getBlockEntity(pos) instanceof TileResearchTable main) return main;
        // Source BlockTable:187 scans all adjacent main BEs, not only the linked
        // one. Retain this first-match quirk; there is no owner/exclusive lock.
        for (Direction direction : TableTopology.SEARCH_ORDER) {
            BlockPos candidate = pos.relative(direction);
            if (!level.hasChunkAt(candidate)) return null;
            if (level.getBlockEntity(candidate) instanceof TileResearchTable main) return main;
        }
        return null;
    }

    /** Called by the concrete inkwell early-use hook, never by generic IScribeTools. */
    public static InteractionResult tryCreateResearchTable(Level level, BlockPos pos, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!(held.getItem() instanceof ItemInkwell) || !level.hasChunkAt(pos)) return InteractionResult.PASS;
        BlockState before = level.getBlockState(pos);
        if (!(before.getBlock() instanceof BlockTable table) || !(level.getBlockEntity(pos) instanceof TileTable)
                || !TableTopology.canStartConversion(before.getValue(VARIANT))) return InteractionResult.PASS;

        int side = -1;
        BlockPos partner = null;
        BlockState partnerBefore = null;
        for (Direction direction : TableTopology.SEARCH_ORDER) {
            BlockPos candidate = pos.relative(direction);
            // Do not force-load a chunk or choose a lower-priority candidate
            // while an earlier original search direction is still unknown.
            if (!level.hasChunkAt(candidate)) return InteractionResult.PASS;
            BlockState candidateState = level.getBlockState(candidate);
            if (!candidateState.is(table) || !TableTopology.canBeConversionPartner(candidateState.getValue(VARIANT))
                    || !(level.getBlockEntity(candidate) instanceof TileTable)) continue;
            side = TableTopology.mainVariant(direction);
            partner = candidate;
            partnerBefore = candidateState;
            break;
        }
        if (side < 0) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level instanceof ServerLevel serverLevel && !serverLevel.getServer().isSameThread()) {
            throw new IllegalStateException("Research table conversion requires the server thread");
        }

        BlockState mainState = before.setValue(VARIANT, side);
        BlockState partnerState = partnerBefore.setValue(VARIANT, TableTopology.partnerVariant(side));
        if (!level.setBlock(pos, mainState, CONVERSION_FLAGS)) return InteractionResult.FAIL;
        if (!level.setBlock(partner, partnerState, CONVERSION_FLAGS)) {
            level.setBlock(pos, before, CONVERSION_FLAGS);
            return InteractionResult.FAIL;
        }
        if (!(level.getBlockEntity(pos) instanceof TileResearchTable research)) {
            level.setBlock(pos, before, CONVERSION_FLAGS);
            level.setBlock(partner, partnerBefore, CONVERSION_FLAGS);
            return InteractionResult.FAIL;
        }
        // No ink usability/damage/research/vis check: source construction accepts
        // even an exhausted inkwell and transfers the entire original stack data.
        research.setItem(0, held.copy());
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
            player.getInventory().setChanged();
        }
        level.sendBlockUpdated(pos, before, mainState, Block.UPDATE_CLIENTS);
        level.sendBlockUpdated(partner, partnerBefore, partnerState, Block.UPDATE_CLIENTS);
        return InteractionResult.CONSUME;
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        boolean changesKind = !replacement.is(this)
                || TableTopology.isMain(state.getValue(VARIANT)) != TableTopology.isMain(replacement.getValue(VARIANT));
        if (changesKind) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof TileResearchTable research) research.evacuate();
            // Both registered BE types accept TABLE, so same-block property
            // changes do not recreate the BE unless this old one is removed.
            level.removeBlockEntity(pos);
        }
        super.onRemove(state, level, pos, replacement, moving);
    }

    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (!level.isClientSide && state.getValue(VARIANT) >= 2) level.scheduleTick(pos, this, 1);
    }
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moving) {
        if (!level.isClientSide) validateTopology(level, pos);
        super.neighborChanged(state, level, pos, neighbor, neighborPos, moving);
    }
    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) { validateTopology(level, pos); }

    public void validateTopology(Level level, BlockPos pos) {
        if (level.isClientSide || !level.hasChunkAt(pos)) return;
        BlockState state = level.getBlockState(pos);
        if (!state.is(this)) return;
        int variant = state.getValue(VARIANT);
        if (!TableTopology.isMain(variant) && !TableTopology.isSecondary(variant)) return;
        BlockPos partner = pos.relative(TableTopology.direction(variant));
        boolean loaded = level.hasChunkAt(partner);
        BlockState other = loaded ? level.getBlockState(partner) : null;
        boolean otherTable = other != null && other.is(this);
        var decision = TableTopology.inspect(variant, loaded, otherTable,
                otherTable ? other.getValue(VARIANT) : -1,
                loaded && level.getBlockEntity(partner) instanceof TileResearchTable);
        if (decision == TableTopology.Decision.WAIT) {
            level.scheduleTick(pos, this, 20);
        } else if (decision == TableTopology.Decision.COLLAPSE) {
            // Forced metadata adaptation: an arbitrary modern block has no old
            // global damage value. Non-table partners are invalid. Between TABLE
            // states retain the original permissive/nonreciprocal checks.
            level.setBlock(pos, state.setValue(VARIANT, 0), Block.UPDATE_ALL);
        }
    }
}
