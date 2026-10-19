package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.config.ThaumcraftAlchemy;
import thaumcraft.common.entities.EntitySpecialItem;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.tiles.TileCrucible;

/** Flattened original blockMetalDevice:0, with no GUI. */
public final class BlockCrucible extends BaseEntityBlock {
    public static final MapCodec<BlockCrucible> CODEC = simpleCodec(BlockCrucible::new);
    // BlockMetalDevice:313-326: 5/16 floor, 1/8 walls, walls stop at .85.
    private static final VoxelShape COLLISION = Shapes.or(box(0, 0, 0, 16, 5, 16),
            box(0, 0, 0, 2, 13.6, 16), box(14, 0, 0, 16, 13.6, 16),
            box(0, 0, 0, 16, 13.6, 2), box(0, 0, 14, 16, 13.6, 16));
    private int contactDelay;

    public BlockCrucible(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileCrucible(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return COLLISION; }
    // Original selection bounds remain the full cube, unlike physical collision.
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.block(); }
    @Override protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) { return Shapes.empty(); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ThaumcraftAlchemy.CRUCIBLE_BLOCK_ENTITY.get(), TileCrucible::tick);
    }

    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Original BlockMetalDevice:380-385 plays lava-pop during boiling, not
        // an invented continuous furnace loop or a server tick sound packet.
        if (random.nextInt(10) == 0 && level.getBlockEntity(pos) instanceof TileCrucible tile
                && tile.getWaterAmount() > 0 && tile.getHeat() > 150) {
            level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.LAVA_POP, SoundSource.BLOCKS,
                    .1F + random.nextFloat() * .1F, 1.2F + random.nextFloat() * .2F, false);
        }
    }

    @Override protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof TileCrucible tile)) return;
        if (entity instanceof ItemEntity item && !(entity instanceof EntitySpecialItem)
                && tile.getHeat() > 150 && tile.getWaterAmount() > 0) {
            tile.attemptSmelt(item);
        } else {
            // The original delay belongs to the block singleton, not each mob.
            if (++contactDelay < 10) return;
            contactDelay = 0;
            if (entity instanceof LivingEntity && tile.getHeat() > 150 && tile.getWaterAmount() > 0) {
                entity.hurt(level.damageSources().inFire(), 1);
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, .4F, 2 + level.random.nextFloat() * .4F);
            }
        }
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
                                                       Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TileCrucible tile)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (held.getItem() instanceof ItemWandCasting) {
            if (!level.isClientSide && player.isShiftKeyDown()) tile.spillRemnants();
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        int amount = waterIn(held);
        if (amount == 0 || player.isSpectator()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), held)) return ItemInteractionResult.FAIL;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (tile.fillWater(amount, false) == 0) return ItemInteractionResult.CONSUME;
        ItemStack empty = new ItemStack(held.is(Items.WATER_BUCKET) ? Items.BUCKET : Items.GLASS_BOTTLE);
        tile.fillWater(amount, true);
        // Source BlockMetalDevice:546-566 consumes even in creative, and wastes
        // any excess water when a whole registered container only partly fits.
        held.shrink(1);
        if (held.isEmpty()) player.setItemInHand(hand, ItemStack.EMPTY);
        if (!player.getInventory().add(empty) && level instanceof ServerLevel server) {
            // Inventory-full remainder must survive a cancelled item ADD too.
            DeferredItemDrops.enqueue(server, player.blockPosition(), java.util.List.of(empty));
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        level.playSound(null, pos, SoundEvents.GENERIC_SWIM, SoundSource.BLOCKS, .33F,
                1 + (level.random.nextFloat() - level.random.nextFloat()) * .3F);
        return ItemInteractionResult.CONSUME;
    }

    private static int waterIn(ItemStack held) {
        if (held.is(Items.WATER_BUCKET)) return 1000;
        PotionContents potion = held.get(DataComponents.POTION_CONTENTS);
        // Native equivalent of the original registered quarter-bucket water
        // bottle. Effects/splash/lingering potions are never accepted as water.
        return held.is(Items.POTION) && potion != null && potion.is(Potions.WATER) && !potion.hasEffects() ? 250 : 0;
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!replacement.is(this) && level.getBlockEntity(pos) instanceof TileCrucible tile) tile.onReplaced();
        // Never place flux or spawn output from inside LevelChunk's BE removal.
        super.onRemove(state, level, pos, replacement, moving);
    }

    /** Called by the wand BEFORE its other triggers, including while sneaking. */
    public static InteractionResult tryCreate(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack held = context.getItemInHand();
        if (player == null || player.isSpectator() || !(held.getItem() instanceof ItemWandCasting) || !level.hasChunkAt(pos)) return InteractionResult.PASS;
        BlockState before = level.getBlockState(pos);
        boolean crucible = before.is(ThaumcraftAlchemy.CRUCIBLE.get());
        // Original cauldron metadata was unrestricted. Both modern empty and
        // water cauldrons map to it; their previous water is NOT transferred.
        if (!crucible && !before.is(Blocks.CAULDRON) && !before.is(Blocks.WATER_CAULDRON)) return InteractionResult.PASS;
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), held)) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread()) throw new IllegalStateException("Crucible conversion requires the server thread");
        if (crucible) {
            if (player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof TileCrucible tile) tile.spillRemnants();
            return InteractionResult.CONSUME;
        }
        // WandManager:121-133 and performTrigger:713-714: free, no CRUCIBLE
        // research check, no vis debit, and the actual wand stays in the hand.
        if (!level.setBlock(pos, ThaumcraftAlchemy.CRUCIBLE.get().defaultBlockState(), Block.UPDATE_ALL)) return InteractionResult.FAIL;
        if (!(level.getBlockEntity(pos) instanceof TileCrucible)) {
            level.setBlock(pos, before, Block.UPDATE_ALL);
            return InteractionResult.FAIL;
        }
        level.playSound(null, pos, SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "wand")), SoundSource.BLOCKS, 1, 1);
        level.blockEvent(pos, ThaumcraftAlchemy.CRUCIBLE.get(), 1, 1);
        return InteractionResult.CONSUME;
    }
}
