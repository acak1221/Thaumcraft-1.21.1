package thaumcraft.common.blocks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Original BlockCustomPlant:62-68,119-143,174-182,194-211,224-229. */
public final class BlockCustomPlant extends BushBlock implements OriginalPistonPushable {
    public static final MapCodec<BlockCustomPlant> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.fieldOf("cinderpearl").forGetter(BlockCustomPlant::isCinderpearl), propertiesCodec()).apply(instance, BlockCustomPlant::new));
    private static final VoxelShape SHAPE = Block.box(1.6, 0, 1.6, 14.4, 12.8, 14.4);
    private static ClientEffects clientEffects;
    private final boolean cinderpearl;
    public BlockCustomPlant(boolean cinderpearl, Properties properties) { super(properties); this.cinderpearl = cinderpearl; }
    public boolean isCinderpearl() { return cinderpearl; }
    @Override protected MapCodec<BlockCustomPlant> codec() { return CODEC; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    /**
     * Original BlockCustomPlant:62-68 - the soil that carries the plant type.
     *
     * <p>Forge 1.7 accepts the inherited {@code BlockBush} soils first, then the Desert plant
     * type adds sand (both old metadata variants). The rule is a pure function of the plant
     * type and the soil, so it can be asserted without a world and without constructing a
     * block (a constructed block would leave an unregistered intrusive holder behind and
     * break the registry freeze of the whole test suite).</p>
     *
     * @param cinderpearl the Desert plant type of the original metadata three
     * @param soil        the block the plant would stand on
     */
    public static boolean soilSupports(boolean cinderpearl, BlockState soil) {
        return soilSupports(cinderpearl ? PlantType.DESERT : PlantType.PLAINS, soil);
    }

    /**
     * The plant types of the original Forge {@code EnumPlantType}. Minecraft 1.21.1 has no
     * {@code IPlantable} and no {@code EnumPlantType}, so the plant type of the original is
     * answered by the block itself and the soil rule is a pure function of the type and the soil.
     */
    public enum PlantType { PLAINS, DESERT, CAVE }

    /**
     * Original BlockCustomPlantItem:85-101 - the plant type the original <em>item</em> of the
     * metadata asks the soil for: the metadata three is Desert, the metadata four and five are
     * Cave and every other metadata is Plains.
     *
     * <p>The original block answers a different table (BlockCustomPlant:119-128, see
     * {@link #blockPlantType(int)}): only the metadata five differs, the item wants a Cave soil
     * for the manashroom while the block itself is a Plains plant.</p>
     */
    public static PlantType itemPlantType(int metadata) {
        if (metadata == 3) return PlantType.DESERT;
        if (metadata == 4 || metadata == 5) return PlantType.CAVE;
        return PlantType.PLAINS;
    }

    /**
     * Original BlockCustomPlant:119-128 - the plant type the original <em>block</em> of the
     * metadata answers: the metadata three is Desert, the metadata four is Cave and every other
     * metadata, the manashroom included, is Plains.
     */
    public static PlantType blockPlantType(int metadata) {
        if (metadata == 3) return PlantType.DESERT;
        if (metadata == 4) return PlantType.CAVE;
        return PlantType.PLAINS;
    }

    /**
     * The soil rule of the plant type.
     *
     * <p>The Plains type is the soil of the inherited {@code BlockBush} (grass, dirt and the
     * farmland), the Desert type adds the sand. The Cave type has no counterpart that the oracle
     * can prove: the Forge 1.7.10 table of the Cave soils is not part of the decompiled original,
     * so the port keeps the Plains soil of the original's own block rule for the two Cave plants,
     * which is the soil the original's world generator puts them on
     * (oracle BiomeGenMagicalForest:147).</p>
     *
     * <p>The inherited soil is asked FIRST and for EVERY plant type, exactly as the Forge 1.7.10
     * table of {@code Block.canSustainPlant} does: the check of the inherited {@code BlockBush}
     * soil (grass, dirt, farmland) comes before the switch of the plant type, so the Desert plant
     * of the original (the cinderpearl, the metadata three) also stands on the inherited soil
     * and only ADDS the sand to it. The table of the original is:</p>
     *
     * <pre>
     * Block.canSustainPlant (Forge 1.7.10, patches/minecraft/net/minecraft/block/Block.java.patch)
     *   if (plantable instanceof BlockBush &amp;&amp; ((BlockBush) plantable).func_149854_a(this)) return true;
     *   switch (plantType) {
     *     case Desert: return this == Blocks.sand;
     *     case Plains: return this == Blocks.grass || this == Blocks.dirt || this == Blocks.farmland;
     *   }
     * </pre>
     *
     * <p>The oracle's own block carries no soil rule of its own
     * (oracle {@code BlockCustomPlant.java:130-132} answers {@code canPlaceBlockAt} with a plain
     * {@code true}), so the whole rule is the Forge table above.</p>
     */
    public static boolean soilSupports(PlantType type, BlockState soil) {
        // The inherited BlockBush soil is asked first, for every plant type.
        if (soil.is(Blocks.GRASS_BLOCK) || soil.is(Blocks.DIRT) || soil.is(Blocks.COARSE_DIRT)
                || soil.is(Blocks.PODZOL) || soil.is(Blocks.FARMLAND)) return true;
        // The Desert type adds the sand of the original (both old metadata variants).
        if (type == PlantType.DESERT) return soil.is(Blocks.SAND) || soil.is(Blocks.RED_SAND);
        return false;
    }

    @Override protected boolean mayPlaceOn(BlockState soil, BlockGetter level, BlockPos pos) {
        return soilSupports(cinderpearl, soil);
    }
    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return mayPlaceOn(level.getBlockState(pos.below()), level, pos.below());
    }

    /**
     * Oracle BlockCustomPlant:55-56 - the 1.7.10 class extended {@code
     * net.minecraft.block.BlockBush} and did not implement {@code ITileEntityProvider}, so the
     * piston of the original moved the plant for every metadata, the ethereal bloom (the metadata
     * four, the tile at BlockCustomPlant:97-108) included, and the move destroyed the tile of
     * the bloom.
     *
     * <p>The port carries no tile for the bloom, so the answer of the vanilla code is already the
     * answer of the original; the contract is written down so that the answer stays the answer
     * of the original when the tile of the bloom is ported.
     */
    @Override public boolean isPushableInOriginal(BlockState state) {
        return OriginalPistonRule.Family.CUSTOM_PLANT.isPushableInOriginal(0);
    }

    @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) level.destroyBlock(pos, true);
    }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (clientEffects != null) clientEffects.tick(level, pos, cinderpearl, random);
    }
    public interface ClientEffects { void tick(Level level, BlockPos pos, boolean cinderpearl, RandomSource random); }
    public static void setClientEffects(ClientEffects effects) { clientEffects = effects; }
    /**
     * The item of a custom plant.
     *
     * <p>Original {@code BlockCustomPlantItem:50-65}: the item places the plant only on the top
     * face of a block whose soil supports its plant type, it plays the step sound of the plant
     * block and it consumes one item. 1.21.1 has no {@code IPlantable}, so the plant type of the
     * original metadata is answered by the block itself and the soil of the port is the
     * canonical one; the placement of the 1.12 reference, which let the plant die on the next
     * tick instead of refusing the placement, is not the behaviour of the 1.7.10 original.</p>
     *
     * <p>Oracle: {@code thaumcraft/common/blocks/BlockCustomPlantItem.java}, lines 50-65.</p>
     */
    public static final class PlantItem extends BlockItem {
        public PlantItem(Block block, Item.Properties properties) { super(block, properties); }

        /** Original BlockCustomPlantItem:51 - only the top face of a block carries a plant. */
        @Override protected BlockState getPlacementState(BlockPlaceContext context) {
            if (context.getClickedFace() != Direction.UP) return null;
            return super.getPlacementState(context);
        }

        /** Original BlockCustomPlantItem:54 - the soil below has to support the plant. */
        @Override protected boolean mustSurvive() { return true; }

        /**
         * Original BlockCustomPlantItem:56 - the plant announces itself with the step sound of
         * its own block. The pitch of the original (0.8) is a property of the 1.7.10 sound
         * type, 1.21.1 has no per block sound pitch.
         */
        @Override protected SoundEvent getPlaceSound(BlockState state) { return SoundEvents.GRASS_STEP; }
    }
}
