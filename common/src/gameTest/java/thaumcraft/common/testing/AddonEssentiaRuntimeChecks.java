package thaumcraft.common.testing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectSource;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.lib.events.EssentiaHandler;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TileTube;

/**
 * An add-on essentia source, a block entity that implements nothing but the public {@link IEssentiaTransport}, feeds the
 * mod's own receivers through its {@code takeEssentia}, as every TC4 tube and jar asked any neighbouring transport.
 *
 * <p>The infusion matrix drains the same source as an {@link IAspectSource} through its {@code takeFromContainer}, as
 * the original EssentiaHandler did.</p>
 *
 * <p>The source block and its block entity type are test-only registrations of the GameTest mod (Fabric initialiser,
 * NeoForge RegisterEvent). Every tick checks that the source, the tube and the jar together hold exactly the supplied
 * stock.</p>
 */
public final class AddonEssentiaRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static Block sourceBlock;
    private static BlockEntityType<AddonSource> sourceType;
    private AddonEssentiaRuntimeChecks() {}

    public static synchronized void registerTestProbes() {
        if (sourceBlock != null) return;
        var id = ResourceLocation.fromNamespaceAndPath("thaumcraft_gametest", "addon_essentia_source");
        sourceBlock = Registry.register(BuiltInRegistries.BLOCK, id, new AddonSourceBlock(BlockBehaviour.Properties.of()));
        sourceType = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, BlockEntityType.Builder.of(AddonSource::new, sourceBlock).build(null));
    }

    /** Jar under the source, then source, tube and jar; an over-paying source and a refusing one. */
    public static void addonSourceFeedsTubesAndJars(GameTestHelper test) {
        test.assertTrue(sourceBlock != null && sourceType != null, "the GameTest mod registered the add-on source before registry freeze");
        ServerLevel level = test.getLevel();
        BlockPos source = test.absolutePos(new BlockPos(2, 4, 2)), below = source.below(), bottom = below.below();
        try {
            // A jar directly below: the jar's own pull (original TileJarFillable) asks the add-on source.
            AddonSource direct = place(level, source, 6, 0, false);
            level.setBlock(below, ThaumcraftContent.JAR.get().defaultBlockState(), QUIET);
            for (int tick = 0; tick < 200 && direct.amount > 0; tick++) {
                tick(level, below, ThaumcraftContent.JAR_BLOCK_ENTITY.get());
                conserved(test, level, 6, source, below);
            }
            TileJar jar = (TileJar) level.getBlockEntity(below);
            test.assertTrue(direct.amount == 0 && jar.getAmount() == 6 && jar.getAspect() == Aspect.FIRE,
                    "the jar took all six units from the add-on source: " + direct.amount + " / " + jar.getAmount());

            // Source, tube, jar: the tube pulls from the add-on source (original TileTube.equalizeWithNeighbours).
            clear(level, source, below, bottom);
            AddonSource piped = place(level, source, 5, 0, false);
            level.setBlock(below, ThaumcraftTubes.ESSENTIA_TUBE.get().defaultBlockState(), QUIET);
            level.setBlock(bottom, ThaumcraftContent.JAR.get().defaultBlockState(), QUIET);
            for (int tick = 0; tick < 400 && ((TileJar) level.getBlockEntity(bottom)).getAmount() < 5; tick++) {
                tick(level, below, ThaumcraftTubes.TUBE_BLOCK_ENTITY.get());
                conserved(test, level, 5, source, below, bottom);
                tick(level, bottom, ThaumcraftContent.JAR_BLOCK_ENTITY.get());
                conserved(test, level, 5, source, below, bottom);
            }
            test.assertTrue(piped.amount == 0 && ((TileJar) level.getBlockEntity(bottom)).getAmount() == 5,
                    "the tube carried all five units from the add-on source to the jar");

            // A source that pays two units for a request of one gets the surplus back through its addEssentia.
            clear(level, source, below, bottom);
            AddonSource generous = place(level, source, 4, 1, false);
            level.setBlock(below, ThaumcraftContent.JAR.get().defaultBlockState(), QUIET);
            for (int tick = 0; tick < 200 && generous.amount > 0; tick++) {
                tick(level, below, ThaumcraftContent.JAR_BLOCK_ENTITY.get());
                conserved(test, level, 4, source, below);
            }
            test.assertTrue(generous.amount == 0 && ((TileJar) level.getBlockEntity(below)).getAmount() == 4 && generous.returned > 0,
                    "one unit per pull is credited and the surplus returns to the source");

            // A source that refuses keeps its stock; nothing is credited.
            clear(level, source, below, bottom);
            AddonSource refusing = place(level, source, 3, 0, true);
            level.setBlock(below, ThaumcraftContent.JAR.get().defaultBlockState(), QUIET);
            for (int tick = 0; tick < 60; tick++) {
                tick(level, below, ThaumcraftContent.JAR_BLOCK_ENTITY.get());
                conserved(test, level, 3, source, below);
            }
            test.assertTrue(refusing.takeCalls > 0 && refusing.amount == 3 && ((TileJar) level.getBlockEntity(below)).getAmount() == 0,
                    "a refusing source was asked and kept its stock");
        } finally {
            clear(level, source, below, bottom);
        }
        test.succeed();
    }

    /** The infusion matrix takes the units it still wants from an add-on IAspectSource, one per drain. */
    public static void addonAspectSourcePaysInfusion(GameTestHelper test) {
        test.assertTrue(sourceBlock != null && sourceType != null, "the GameTest mod registered the add-on source before registry freeze");
        ServerLevel level = test.getLevel();
        var registries = level.registryAccess();
        BlockPos matrixPos = test.absolutePos(new BlockPos(2, 2, 2)), sourcePos = matrixPos.offset(3, 2, -2);
        try {
            level.setBlock(matrixPos, ThaumcraftInfusion.MATRIX.get().defaultBlockState(), Block.UPDATE_ALL);
            TileInfusionMatrix matrix = (TileInfusionMatrix) level.getBlockEntity(matrixPos);
            AddonSource stock = place(level, sourcePos, 2, 0, false);
            EssentiaHandler.clear(level);
            // A running ritual that still wants three ignis (the saved state of a supported recipe, as in the reservoir check).
            CompoundTag demand = matrix.saveWithFullMetadata(registries);
            demand.putBoolean("active", true);
            demand.putBoolean("crafting", true);
            demand.putInt("recipetype", 0);
            var recipe = (InfusionStackRecipe) level.getRecipeManager()
                    .byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "infusion/essentia_reservoir")).orElseThrow().value();
            ItemStack result = recipe.getResultItem(registries);
            CompoundTag output = new CompoundTag();
            output.put("Stack", result.copyWithCount(1).save(registries));
            output.putInt("StoredCount", result.getCount());
            demand.putString("rotype", "@");
            demand.put("recipeout", output);
            ListTag rows = new ListTag();
            CompoundTag row = new CompoundTag();
            row.putString("key", Aspect.FIRE.getTag());
            row.putInt("amount", 3);
            rows.add(row);
            demand.put("Aspects", rows);
            matrix.loadWithComponents(demand, registries);
            test.assertTrue(matrix.getPendingEssentia(Aspect.FIRE) == 3, "the matrix wants 3 ignis: " + matrix.getPendingEssentia(Aspect.FIRE));
            test.assertTrue(EssentiaHandler.drainEssentia(matrix, Aspect.FIRE) && stock.amount == 1 && matrix.getPendingEssentia(Aspect.FIRE) == 2,
                    "one unit leaves the add-on source and the matrix wants one less");
            test.assertTrue(EssentiaHandler.drainEssentia(matrix, Aspect.FIRE) && stock.amount == 0 && matrix.getPendingEssentia(Aspect.FIRE) == 1,
                    "the second unit empties the source");
            test.assertTrue(!EssentiaHandler.drainEssentia(matrix, Aspect.FIRE) && stock.amount == 0 && matrix.getPendingEssentia(Aspect.FIRE) == 1,
                    "an empty source pays nothing and the demand stays");
            EssentiaHandler.clear(level);
            AddonSource refusing = place(level, sourcePos, 3, 0, true);
            test.assertTrue(!EssentiaHandler.drainEssentia(matrix, Aspect.FIRE) && refusing.amount == 3 && matrix.getPendingEssentia(Aspect.FIRE) == 1,
                    "a refusing source keeps its stock and the demand stays");
            CompoundTag stop = matrix.saveWithFullMetadata(registries);
            stop.putBoolean("crafting", false);
            stop.putBoolean("active", false);
            matrix.loadWithComponents(stop, registries);
        } finally {
            EssentiaHandler.clear(level);
            clear(level, sourcePos, matrixPos);
        }
        test.succeed();
    }

    private static AddonSource place(ServerLevel level, BlockPos pos, int amount, int surplus, boolean refuse) {
        level.setBlock(pos, sourceBlock.defaultBlockState(), QUIET);
        AddonSource source = (AddonSource) level.getBlockEntity(pos);
        source.amount = amount;
        source.surplus = surplus;
        source.refuse = refuse;
        return source;
    }

    private static void clear(ServerLevel level, BlockPos... positions) {
        for (BlockPos pos : positions) {
            level.removeBlockEntity(pos);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), QUIET);
        }
    }

    private static <T extends BlockEntity> void tick(ServerLevel level, BlockPos pos, BlockEntityType<T> type) {
        BlockState state = level.getBlockState(pos);
        var ticker = ((EntityBlock) state.getBlock()).getTicker(level, state, type);
        if (ticker == null) throw new IllegalStateException("Missing native server ticker at " + pos);
        ticker.tick(level, pos, state, type.getBlockEntity(level, pos));
    }

    private static void conserved(GameTestHelper test, ServerLevel level, int total, BlockPos source, BlockPos... receivers) {
        int actual = ((AddonSource) level.getBlockEntity(source)).amount;
        for (BlockPos pos : receivers) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof TileJar jar) actual += jar.getAmount();
            else if (entity instanceof TileTube tube) actual += tube.getEssentiaAmount(null);
        }
        test.assertTrue(actual == total, "the supplied stock is conserved every tick: expected " + total + ", got " + actual);
    }

    /** A test-only block of the add-on source. */
    static final class AddonSourceBlock extends Block implements EntityBlock {
        AddonSourceBlock(Properties properties) { super(properties); }
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new AddonSource(pos, state); }
    }

    /** A plain add-on transport and aspect source: fire essentia offered on every face, never pulling. */
    static final class AddonSource extends BlockEntity implements IEssentiaTransport, IAspectSource {
        int amount, surplus, takeCalls, returned;
        boolean refuse;
        AddonSource(BlockPos pos, BlockState state) { super(sourceType, pos, state); }
        @Override public boolean isConnectable(Direction face) { return true; }
        @Override public boolean canInputFrom(Direction face) { return false; }
        @Override public boolean canOutputTo(Direction face) { return true; }
        @Override public void setSuction(Aspect aspect, int suction) {}
        @Override public Aspect getSuctionType(Direction face) { return null; }
        @Override public int getSuctionAmount(Direction face) { return 0; }
        @Override public Aspect getEssentiaType(Direction face) { return amount > 0 ? Aspect.FIRE : null; }
        @Override public int getEssentiaAmount(Direction face) { return amount; }
        @Override public int takeEssentia(Aspect aspect, int request, Direction face) {
            takeCalls++;
            if (refuse || aspect != Aspect.FIRE || request <= 0 || amount <= 0) return 0;
            int paid = Math.min(amount, request + surplus);
            amount -= paid;
            return paid;
        }
        @Override public int addEssentia(Aspect aspect, int add, Direction face) {
            if (aspect != Aspect.FIRE || add <= 0) return 0;
            amount += add;
            returned += add;
            return add;
        }
        @Override public int getMinimumSuction() { return 0; }
        @Override public boolean renderExtendedTube() { return false; }
        @Override public AspectList getAspects() { return amount > 0 ? new AspectList().add(Aspect.FIRE, amount) : new AspectList(); }
        @Override public void setAspects(AspectList aspects) {}
        @Override public boolean doesContainerAccept(Aspect aspect) { return aspect == Aspect.FIRE; }
        @Override public int addToContainer(Aspect aspect, int add) { return add - addEssentia(aspect, add, null); }
        @Override public boolean takeFromContainer(Aspect aspect, int take) { return takeEssentia(aspect, take, null) == take; }
        @Override public boolean takeFromContainer(AspectList aspects) { return false; }
        @Override public boolean doesContainerContainAmount(Aspect aspect, int need) { return aspect == Aspect.FIRE && amount >= need; }
        @Override public boolean doesContainerContain(AspectList aspects) { return false; }
        @Override public int containerContains(Aspect aspect) { return aspect == Aspect.FIRE ? amount : 0; }
    }
}
