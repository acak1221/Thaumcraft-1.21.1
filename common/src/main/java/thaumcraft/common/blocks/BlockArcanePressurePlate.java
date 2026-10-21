package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
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
import thaumcraft.common.config.ThaumcraftSettings;
import thaumcraft.common.lib.world.BlockFamilyShapes;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.tiles.TileArcanePressurePlate;

/**
 * Original BlockWoodenDevice metadata two and three: the arcane pressure plate.
 *
 * <p>The original overrode the two values that Minecraft 1.7.10 binds to the block
 * and not to the block state: the hardness of the plate is two, or none at all
 * while the warded stone option is on (oracle {@code BlockWoodenDevice:196-203}),
 * and its blast resistance is the absolute value 999 (oracle
 * {@code BlockWoodenDevice:204-212}), which is not the 0.6 factor of a setter
 * value. Minecraft 1.21.1 binds both to the block, so the plate became a block
 * of its own.</p>
 */
public final class BlockArcanePressurePlate extends BaseEntityBlock {
    public static final MapCodec<BlockArcanePressurePlate> CODEC = simpleCodec(BlockArcanePressurePlate::new);
    /**
     * Original BlockWoodenDevice metadata two (the plate) and three (the pressed plate).
     * The values are the original metadata values, so the rules of the family keep working.
     */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 2, 3);
    /**
     * BlockWoodenDevice:249-256 - the selection box of the plate is the thin square that is
     * inset by a sixteenth on every side. The unpressed plate is a sixteenth high, the pressed
     * one only half of that.
     */
    private static final VoxelShape PLATE = BlockFamilyShapes.arcanePressurePlate(EldritchBlockRules.PLATE_BOX_HEIGHT);
    /** BlockWoodenDevice:254-256 - the pressed plate is half as high. */
    private static final VoxelShape PLATE_PRESSED = BlockFamilyShapes.arcanePressurePlate(EldritchBlockRules.PLATE_BOX_HEIGHT_PRESSED);

    public BlockArcanePressurePlate(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(VARIANT, EldritchBlockRules.DEVICE_WOOD_PRESSURE_PLATE));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(VARIANT); }

    /**
     * The original arcane pressure plate is the metadata two and three of the wooden device
     * family, and the family is drawn by the renderer of the family (OpenTC4
     * BlockWoodenDevice:200-201 and {@code client/renderers/block/BlockWoodenDeviceRenderer.java:145}),
     * so the plate is visible.
     * {@code BaseEntityBlock} answers {@code RenderShape.INVISIBLE} for every block entity
     * block (Minecraft 1.21.1 BaseEntityBlock:22-25), so the port has to say it again.
     */
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    public static int variant(BlockState state) { return state.getValue(VARIANT); }

    /** BlockWoodenDevice:403 - the plate carries the owned block entity. */
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileArcanePressurePlate(pos, state); }

    /**
     * BlockWoodenDevice:249-256 - the plate is a thin square that is inset by a sixteenth on
     * every side; the pressed plate is only half as high.
     */
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(VARIANT) == EldritchBlockRules.DEVICE_WOOD_PRESSURE_PLATE_PRESSED ? PLATE_PRESSED : PLATE;
    }

    /**
     * BlockWoodenDevice:305-306 - the original adds no collision box at all for the metadata
     * two and three, so nothing ever stands on the plate.
     */
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /**
     * Original BlockWoodenDevice:196-203 - the hardness of the plate is two, and none at all
     * while the warded stone option is on. Minecraft 1.21.1 binds the destroy speed to the block
     * state, so the unbreakable state of the original is a mining progress of zero.
     */
    @Override protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (ThaumcraftSettings.current().wardedStone()) return 0.0F;
        return super.getDestroyProgress(state, player, level, pos);
    }

    /** BlockWoodenDevice:552-604 - the plate is checked every tick. */
    @Override protected void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        super.tick(state, level, pos, random);
        int variant = state.getValue(VARIANT);
        boolean pressed = detectPlate(level, pos, state);
        if (pressed && variant != EldritchBlockRules.DEVICE_WOOD_PRESSURE_PLATE_PRESSED) {
            level.setBlock(pos, state.setValue(VARIANT, EldritchBlockRules.DEVICE_WOOD_PRESSURE_PLATE_PRESSED), Block.UPDATE_ALL);
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), net.minecraft.sounds.SoundSource.BLOCKS, .2F, .6F);
        } else if (!pressed && variant != EldritchBlockRules.DEVICE_WOOD_PRESSURE_PLATE) {
            level.setBlock(pos, state.setValue(VARIANT, EldritchBlockRules.DEVICE_WOOD_PRESSURE_PLATE), Block.UPDATE_ALL);
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), net.minecraft.sounds.SoundSource.BLOCKS, .2F, .5F);
        }
        level.scheduleTick(pos, this, 1);
    }

    /** BlockWoodenDevice:551-576 - the detection box and the owner filter. */
    private boolean detectPlate(Level level, BlockPos pos, BlockState state) {
        TileArcanePressurePlate plate = level.getBlockEntity(pos) instanceof TileArcanePressurePlate tile ? tile : null;
        int setting = plate == null ? 0 : plate.getSetting();
        String owner = plate == null ? "" : plate.getOwner();
        List<String> access = plate == null ? List.of() : plate.getAccessList();
        AABB box = new AABB(pos.getX() + EldritchBlockRules.PLATE_INSET, pos.getY(), pos.getZ() + EldritchBlockRules.PLATE_INSET,
                pos.getX() + 1.0D - EldritchBlockRules.PLATE_INSET, pos.getY() + EldritchBlockRules.PLATE_HEIGHT,
                pos.getZ() + 1.0D - EldritchBlockRules.PLATE_INSET);
        List<Entity> found = level.getEntities(null, box);
        for (Entity entity : found) {
            boolean player = entity instanceof Player;
            String entityName = entity instanceof Player named ? named.getGameProfile().getName() : "";
            boolean isOwner = player && (entityName.equals(owner) || access.contains("0" + entityName) || access.contains("1" + entityName));
            if (!EldritchBlockRules.plateDetects(setting, !entity.isAlive(), player, isOwner, access.contains("0" + entityName) || access.contains("1" + entityName))) {
                continue;
            }
            return true;
        }
        return false;
    }

    /** BlockWoodenDevice:540-548 - the plate needs a scheduled tick. */
    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        level.scheduleTick(pos, this, 1);
    }

    /** BlockWoodenDevice:363-401 - the owner cycles the sensitivity of the plate. */
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TileArcanePressurePlate plate)) return InteractionResult.PASS;
        if (!plate.getOwner().equals(player.getGameProfile().getName())
                && !plate.getAccessList().contains("1" + player.getGameProfile().getName())) return InteractionResult.PASS;
        int setting = EldritchBlockRules.plateNextSetting(plate.getSetting());
        plate.setSetting((byte) setting);
        player.displayClientMessage(Component.translatable(switch (setting) {
            case EldritchBlockRules.PLATE_SETTING_ALL -> "chat.thaumcraft.plate.all";
            case EldritchBlockRules.PLATE_SETTING_NOT_OWNER -> "chat.thaumcraft.plate.not_owner";
            default -> "chat.thaumcraft.plate.owner";
        }), true);
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), net.minecraft.sounds.SoundSource.BLOCKS, .1F, .9F);
        plate.setChanged();
        level.sendBlockUpdated(pos, state, state, 3);
        return InteractionResult.CONSUME;
    }
}
