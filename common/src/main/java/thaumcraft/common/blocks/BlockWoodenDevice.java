package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IAspectSource;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.tiles.TileBanner;
import thaumcraft.common.tiles.TileOwned;

/**
 * Original BlockWoodenDevice: the wooden device family. This port owns the two
 * arcane pressure plate variants, the greatwood and silverwood blocks and the
 * banner; every other metadata of the original family already exists as its
 * own native block.
 */
public final class BlockWoodenDevice extends BaseEntityBlock implements StateDependentBlockEntity {
    public static final MapCodec<BlockWoodenDevice> CODEC = simpleCodec(BlockWoodenDevice::new);
    /** Original BlockWoodenDevice metadata 0..14. */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 15);
    /** The banner hangs on a wall. */
    private static final VoxelShape BANNER = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 1.0D);

    public BlockWoodenDevice(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(VARIANT, 6));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(VARIANT); }

    /**
     * The original BlockWoodenDevice is drawn by the renderer of the family: the 1.7.10 block
     * answers the render id of the family (OpenTC4 BlockWoodenDevice:200-201) and the client
     * registers {@code BlockWoodenDeviceRenderer} for it (OpenTC4
     * {@code client/renderers/block/BlockWoodenDeviceRenderer.java:145}), so the wooden devices
     * are visible.
     * {@code BaseEntityBlock} answers {@code RenderShape.INVISIBLE} for every block entity
     * block (Minecraft 1.21.1 BaseEntityBlock:22-25), so the port has to say it again.
     */
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    public static int variant(BlockState state) { return state.getValue(VARIANT); }

    /**
     * BlockWoodenDevice:473-495 - the block entity of the owned wooden variants. The arcane
     * pressure plate, the original metadata two and three, is a block of its own now, because
     * the original gave it a hardness and a blast resistance of its own.
     */
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        int kind = EldritchBlockRules.woodenDeviceKind(state.getValue(VARIANT));
        if (kind == EldritchBlockRules.DEVICE_KIND_BANNER) {
            return new TileBanner(pos, state);
        }
        return null;
    }

    /** BlockWoodenDevice - the banner is a thin wall plate. */
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int kind = EldritchBlockRules.woodenDeviceKind(state.getValue(VARIANT));
        if (kind == EldritchBlockRules.DEVICE_KIND_BANNER) return BANNER;
        return Shapes.block();
    }

    /** BlockWoodenDevice:363-401 - the owner cycles the plate sensitivity and the banner takes an aspect. */
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        int variant = state.getValue(VARIANT);
        if (variant != EldritchBlockRules.DEVICE_WOOD_BANNER) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof TileBanner banner)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(player.getUsedItemHand());
        if (player.isShiftKeyDown()) {
            banner.setAspect(null);
        } else if (held.getItem() instanceof IAspectSource source && source.getAspects().getAspects().length > 0) {
            Aspect aspect = source.getAspects().getAspects()[0];
            banner.setAspect(aspect);
            if (!player.getAbilities().instabuild) held.shrink(1);
        } else {
            return InteractionResult.PASS;
        }
        banner.setChanged();
        level.sendBlockUpdated(pos, state, state, 3);
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.WOOL_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.CONSUME;
    }

    /** BlockWoodenDevice:443-450 - the owner of the banner is the placer. */
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (state.getValue(VARIANT) != EldritchBlockRules.DEVICE_WOOD_BANNER) return;
        if (!(level.getBlockEntity(pos) instanceof TileBanner banner)) return;
        banner.setFacing(Direction.from2DDataValue(placer.getDirection().get2DDataValue()).getOpposite());
        if (stack.getItem() instanceof DyeItem dye) banner.setColor((byte) dye.getDyeColor().getId());
        // Original BlockWoodenDevice:121-124: the banner item carries its colour and its aspect.
        BannerData data = stack.getOrDefault(ThaumcraftEldritchBlocks.BANNER_DATA.get(), BannerData.NONE);
        if (data.hasColor()) banner.setColor((byte) data.color());
        if (data.aspect() != null) banner.setAspect(data.aspect());
        banner.setChanged();
        level.sendBlockUpdated(pos, state, state, 3);
    }

    /**
     * Original BlockWoodenDevice:421-428 - a broken banner drops the banner item with its colour and
     * its aspect, not the plain wooden device.
     */
    @Override protected List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        if (EldritchBlockRules.woodenDeviceKind(state.getValue(VARIANT)) == EldritchBlockRules.DEVICE_KIND_BANNER) {
            ItemStack drop = new ItemStack(ThaumcraftEldritchBlocks.BANNER_ITEM.get());
            BlockEntity entity = builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
            if (entity instanceof TileBanner banner) {
                int colour = Math.max(EldritchBlockRules.BANNER_COLOUR_NONE, Math.min(15, banner.getColor()));
                String aspect = banner.getAspect() == null ? "" : banner.getAspect().getTag();
                BannerData data = new BannerData(colour, aspect);
                if (data.hasColor() || !data.aspectTag().isEmpty()) drop.set(ThaumcraftEldritchBlocks.BANNER_DATA.get(), data);
            }
            return List.of(drop);
        }
        return super.getDrops(state, builder);
    }
}
