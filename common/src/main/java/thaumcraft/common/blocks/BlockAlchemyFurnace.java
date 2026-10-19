package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.common.config.ThaumcraftDistillation;
import thaumcraft.common.tiles.TileAlchemyFurnace;

/** Flattened original BlockStoneDevice metadata0: full cube, four fronts, light12. */
public final class BlockAlchemyFurnace extends BaseEntityBlock {
    public static final MapCodec<BlockAlchemyFurnace> CODEC = simpleCodec(BlockAlchemyFurnace::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty FILLED = BooleanProperty.create("filled");

    public BlockAlchemyFurnace(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false).setValue(FILLED, false));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(LIT, FILLED); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileAlchemyFurnace(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ThaumcraftDistillation.ALCHEMY_FURNACE_BLOCK_ENTITY.get(), TileAlchemyFurnace::tick);
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
                                                       Player player, InteractionHand hand, BlockHitResult hit) {
        return open(level, pos, player).consumesAction() ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return open(level, pos, player);
    }
    private InteractionResult open(Level level, BlockPos pos, Player player) {
        if (player.isShiftKeyDown() || player.isSpectator() || !(level.getBlockEntity(pos) instanceof TileAlchemyFurnace tile)) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer && tile.stillValid(player)) {
            MenuRegistry.openExtendedMenu(serverPlayer, tile, data -> data.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (stack.has(DataComponents.CUSTOM_NAME) && level.getBlockEntity(pos) instanceof TileAlchemyFurnace tile) {
            tile.setCustomName(stack.get(DataComponents.CUSTOM_NAME));
        }
    }

    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moving) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TileAlchemyFurnace tile) tile.getBellows();
    }

    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TileAlchemyFurnace tile ? AbstractContainerMenu.getRedstoneSignalFromContainer(tile) : 0;
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!replacement.is(this) && level.getBlockEntity(pos) instanceof TileAlchemyFurnace tile) tile.evacuate();
        // No spawns, aspect transfers, comparator callbacks or adjacent BE edits
        // inside LevelChunk's removal. DeferredItemDrops delivers level-post.
        super.onRemove(state, level, pos, replacement, moving);
    }

    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        // BlockStoneDevice:217-232 uses two world RNG draws and all four mouths.
        float x = pos.getX() + .5F, y = pos.getY() + .2F + random.nextFloat() * 5 / 16, z = pos.getZ() + .5F;
        float offset = random.nextFloat() * .5F - .25F;
        mouth(level, x - .52F, y, z + offset);
        mouth(level, x + .52F, y, z + offset);
        mouth(level, x + offset, y, z - .52F);
        mouth(level, x + offset, y, z + .52F);
    }

    private static void mouth(Level level, float x, float y, float z) {
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0, 0);
        level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0, 0);
    }
}
