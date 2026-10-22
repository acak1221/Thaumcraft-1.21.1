package thaumcraft.common.blocks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
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
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
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
     * The soil of the inherited 1.7.10 {@code BlockBush}.
     *
     * <p>Original: the 1.7.10 {@code BlockBush.func_149854_a} - the MCP name of the method is
     * {@code canPlaceBlockOn} and the MCP mapping describes it as "is the block grass, dirt or
     * farmland". The three blocks are the whole table of the method.</p>
     */
    public static boolean bushSoil(BlockState soil) {
        return soil.is(Blocks.GRASS_BLOCK) || soil.is(Blocks.DIRT) || soil.is(Blocks.FARMLAND);
    }

    /**
     * The soil rule of the plant type, the part that needs no world.
     *
     * <p>The Plains type is the soil of the inherited {@code BlockBush} (grass, dirt and the
     * farmland), the Desert type adds the sand. The Cave type has no soil of its own in the
     * pure part: the Forge 1.7.10 table asks the world for it, see
     * {@link #soilSupports(PlantType, BlockState, BlockGetter, BlockPos)}.</p>
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
     *     case Cave:   return isSideSolid(world, x, y, z, UP);
     *     case Plains: return this == Blocks.grass || this == Blocks.dirt || this == Blocks.farmland;
     *   }
     * </pre>
     *
     * <p>The oracle's own block carries no soil rule of its own
     * (oracle {@code BlockCustomPlant.java:130-132} answers {@code canPlaceBlockAt} with a plain
     * {@code true}), so the whole rule of the item is the Forge table above.</p>
     */
    public static boolean soilSupports(PlantType type, BlockState soil) {
        // The inherited BlockBush soil is asked first, for every plant type.
        if (bushSoil(soil)) return true;
        // The Desert type adds the sand of the original.
        if (type == PlantType.DESERT) return soil.is(Blocks.SAND);
        return false;
    }

    /**
     * The whole soil rule of the plant type, the Cave case included.
     *
     * <p>The Cave case of the Forge 1.7.10 table is
     * {@code isSideSolid(world, x, y, z, UP)}: the soil must present a solid face on its top
     * side. The 1.21.1 counterpart of that test is
     * {@link #isSideSolidUp(BlockState, BlockGetter, BlockPos)}.</p>
     */
    public static boolean soilSupports(PlantType type, BlockState soil, BlockGetter level, BlockPos soilPos) {
        if (soilSupports(type, soil)) return true;
        if (type == PlantType.CAVE) return isSideSolidUp(soil, level, soilPos);
        return false;
    }

    /**
     * The 1.7.10 {@code Block.isSideSolid(world, x, y, z, UP)} of the Forge patch.
     *
     * <p>Original: {@code patches/minecraft/net/minecraft/block/Block.java.patch} of the Forge
     * 1.7.10 branch, the added method:</p>
     *
     * <pre>
     *   if (this instanceof BlockSlab)  return (((meta &amp; 8) == 8 &amp;&amp; (side == UP)) || func_149730_j());
     *   else if (this instanceof BlockFarmland) return (side != DOWN &amp;&amp; side != UP);
     *   else if (this instanceof BlockStairs) { boolean flipped = ((meta &amp; 4) != 0);
     *                                          return ((meta &amp; 3) + side.ordinal() == 5) || (side == UP &amp;&amp; flipped); }
     *   else if (this instanceof BlockSnow) return (meta &amp; 7) == 7;
     *   else if (this instanceof BlockHopper &amp;&amp; side == UP) return true;
     *   else if (this instanceof BlockCompressedPowered) return true;
     *   return isNormalCube(world, x, y, z);
     * </pre>
     *
     * <p>and {@code isNormalCube} is {@code getMaterial().isOpaque() &amp;&amp; renderAsNormalBlock()},
     * a full opaque cube. The 1.21.1 counterpart of a full opaque cube is a full collision
     * shape ({@code isCollisionShapeFullBlock}) that occludes the face behind it
     * ({@code canOcclude}): the glass and the leaves of 1.21.1 carry {@code noOcclusion}, so
     * they are not solid on the top side, exactly as the translucent material of the 1.7.10
     * glass and the leaves of the original were not.</p>
     */
    public static boolean isSideSolidUp(BlockState soil, BlockGetter level, BlockPos pos) {
        // The slab: only the upper half is solid on its top side.
        if (soil.getBlock() instanceof SlabBlock) return soil.getValue(SlabBlock.TYPE) == SlabType.TOP;
        // The hopper is solid on its top side.
        if (soil.getBlock() instanceof HopperBlock) return true;
        // The stairs: only the flipped ones (the upper half) are solid on their top side.
        if (soil.getBlock() instanceof StairBlock) return soil.getValue(StairBlock.HALF) == Half.TOP;
        // The snow layer: only the full layer of eight is solid.
        if (soil.getBlock() instanceof SnowLayerBlock) return soil.getValue(SnowLayerBlock.LAYERS) == 8;
        // The default of the original: a full opaque cube.
        return soil.isCollisionShapeFullBlock(level, pos) && soil.canOcclude();
    }

    /** The plant type the original <em>item</em> of this plant asks the soil for. */
    public PlantType itemPlantType() {
        return cinderpearl ? PlantType.DESERT : PlantType.PLAINS;
    }

    /**
     * The stay rule of the original plant, the soil of the inherited {@code BlockBush}.
     *
     * <p>Original: the 1.7.10 {@code BlockBush.func_149854_a} (the official client jar, the class
     * {@code ajr}) - the MCP name of the method is {@code canPlaceBlockOn} and the MCP mapping
     * describes it as "is the block grass, dirt or farmland". The three blocks are the whole table
     * of the method.</p>
     */
    @Override protected boolean mayPlaceOn(BlockState soil, BlockGetter level, BlockPos pos) {
        return bushSoil(soil);
    }

    /**
     * The stay rule of the original plant.
     *
     * <p>Oracle {@code thaumcraft/common/blocks/BlockCustomPlant.java:130-132} answers
     * {@code canPlaceBlockAt} with a plain {@code true}: the original plant is never refused a
     * position, and the soil rule of the original is the rule of the ITEM
     * ({@code BlockCustomPlantItem:74} asks the soil through the Forge table of
     * {@code Block.canSustainPlant}), see {@link PlantItem#canPlace}.</p>
     *
     * <p>The rule that removes the plant again is the inherited 1.7.10 {@code BlockBush}: the
     * plant did <b>not</b> override {@code func_149718_j} ({@code canBlockStay}, the official
     * client jar, the class {@code ajr}), so the plant breaks on the next neighbour change
     * ({@code func_149695_a}) and on the next random tick ({@code func_149674_a}, which the oracle
     * calls at {@code BlockCustomPlant:136}) when the block below it is not the grass, the dirt or
     * the farmland. 1.21.1 asks that same question in {@code canSurvive}, which
     * {@code BushBlock.updateShape} and the random tick of this plant use to remove it.</p>
     */
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
        private final PlantType plantType;
        public PlantItem(Block block, Item.Properties properties) { this(block, properties, PlantType.PLAINS); }
        /**
         * The item of a plant of a plant type.
         *
         * @param plantType the plant type the original <em>item</em> of the metadata asks the
         *                  soil for (oracle BlockCustomPlantItem:85-101)
         */
        public PlantItem(Block block, Item.Properties properties, PlantType plantType) {
            super(block, properties);
            this.plantType = plantType;
        }

        /** The plant type this item asks the soil for. */
        public PlantType plantType() { return plantType; }

        /** Original BlockCustomPlantItem:51 - only the top face of a block carries a plant. */
        @Override protected BlockState getPlacementState(BlockPlaceContext context) {
            if (context.getClickedFace() != Direction.UP) return null;
            return super.getPlacementState(context);
        }

        /**
         * Original BlockCustomPlantItem:54 - the item asks the soil for its own plant type.
         *
         * <p>Oracle {@code BlockCustomPlant:130-132} answers {@code canPlaceBlockAt} with a plain
         * {@code true}, so the original plant is never refused a position: the only gate of the
         * placement is the soil table of the item below. 1.21.1 asks the block whether it can
         * survive at the placement position ({@code BlockItem.canPlace}, the vanilla
         * {@code mustSurvive()} gate), so that gate is switched off here and the soil rule of the
         * original item is asked by {@link #canPlace(BlockPlaceContext, BlockState)} alone.</p>
         */
        @Override protected boolean mustSurvive() { return false; }

        /**
         * Original BlockCustomPlantItem:54 - the item asks the soil for its own plant type with
         * the Forge 1.7.10 table of {@code Block.canSustainPlant}.
         *
         * <p>Forced 1.21.1 change: the 1.21.1 {@code BlockItem.canPlace} asks the block whether
         * it can survive, and the original block of the plant answers {@code canPlaceBlockAt}
         * with a plain {@code true} (oracle BlockCustomPlant:130-132), so the soil rule of the
         * original item has to be asked here, where the plant type of the original item is
         * known.</p>
         */
        @Override protected boolean canPlace(BlockPlaceContext context, BlockState state) {
            BlockPos pos = context.getClickedPos();
            if (!soilSupports(plantType, context.getLevel().getBlockState(pos.below()),
                    context.getLevel(), pos.below())) return false;
            return super.canPlace(context, state);
        }

        /**
         * Original BlockCustomPlantItem:56 - the plant announces itself with the <b>break</b>
         * sound of its own block, with the volume {@code (breakVolume + 1) / 2} and the pitch
         * {@code breakPitch * 0.8}.
         *
         * <p>Forced 1.21.1 change: none. The 1.21.1 {@code BlockItem.place} plays the sound of
         * {@code getPlaceSound} with exactly the volume {@code (soundType.getVolume() + 1) / 2}
         * and the pitch {@code soundType.getPitch() * 0.8} of the original, so the sound event
         * of the original is the break sound of the sound type of the plant.</p>
         */
        @Override protected SoundEvent getPlaceSound(BlockState state) {
            return state.getSoundType().getBreakSound();
        }
    }
}
