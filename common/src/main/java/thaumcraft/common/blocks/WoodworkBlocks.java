package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import thaumcraft.common.config.ThaumcraftWoodwork;

/** Public constructors for vanilla wood behavior and native sign entities with owned registry types. */
public final class WoodworkBlocks {
    private WoodworkBlocks() {}
    public static final class Door extends DoorBlock { public Door(BlockSetType type, Properties props) { super(type, props); } }
    public static final class Trapdoor extends TrapDoorBlock { public Trapdoor(BlockSetType type, Properties props) { super(type, props); } }
    public static final class Button extends ButtonBlock { public Button(BlockSetType type, Properties props) { super(type, 30, props); } }
    public static final class Plate extends PressurePlateBlock { public Plate(BlockSetType type, Properties props) { super(type, props); } }

    public static final class StandingSign extends StandingSignBlock {
        public StandingSign(WoodType type, Properties props) { super(type, props); }
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new SignTile(pos, state); }
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) { return ticker(type, false); }
    }
    public static final class WallSign extends WallSignBlock {
        public WallSign(WoodType type, Properties props) { super(type, props); }
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new SignTile(pos, state); }
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) { return ticker(type, false); }
    }
    public static final class CeilingSign extends CeilingHangingSignBlock {
        public CeilingSign(WoodType type, Properties props) { super(type, props); }
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new HangingSignTile(pos, state); }
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) { return ticker(type, true); }
    }
    public static final class WallHangingSign extends net.minecraft.world.level.block.WallHangingSignBlock {
        public WallHangingSign(WoodType type, Properties props) { super(type, props); }
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new HangingSignTile(pos, state); }
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) { return ticker(type, true); }
    }
    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity> BlockEntityTicker<T> ticker(BlockEntityType<T> type, boolean hanging) {
        if (type != (hanging ? ThaumcraftWoodwork.HANGING_SIGN_TYPE.get() : ThaumcraftWoodwork.SIGN_TYPE.get())) return null;
        return (level, pos, state, tile) -> SignBlockEntity.tick(level, pos, state, (SignBlockEntity) tile);
    }
    public static final class SignTile extends SignBlockEntity {
        public SignTile(BlockPos pos, BlockState state) { super(ThaumcraftWoodwork.SIGN_TYPE.get(), pos, state); }
    }
    public static final class HangingSignTile extends HangingSignBlockEntity {
        public HangingSignTile(BlockPos pos, BlockState state) { super(pos, state); }
        // Vanilla's hanging constructor hardcodes its type. Keep the actual native class for
        // editor/line sizing while all validation, serialization and packets use our registry type.
        @Override public BlockEntityType<?> getType() { return ThaumcraftWoodwork.HANGING_SIGN_TYPE.get(); }
        @Override public boolean isValidBlockState(BlockState state) { return getType().isValid(state); }
    }
}
