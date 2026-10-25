package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.InfusionEnchantmentRecipe;
import thaumcraft.common.blocks.BlockInfusionPillar;
import thaumcraft.common.config.ThaumcraftAlchemy;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftThaumatorium;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.container.ContainerThaumatorium;
import thaumcraft.common.crafting.ProgrammaticRecipes;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.tiles.TileArcaneWorkbench;
import thaumcraft.common.tiles.TileCrucible;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TilePedestal;
import thaumcraft.common.tiles.TileThaumatorium;

/** Declared addon registrations must be executed by real machines, not merely returned by API getters. */
public final class ProgrammaticRecipeRuntimeChecks {
    private ProgrammaticRecipeRuntimeChecks() {}
    private static final String RESEARCH = "API_REVIEW_REGRESSION";

    public static void arcaneAndCrucible(GameTestHelper test) {
        try (var actor = new TubeVariantsRuntimeChecks.Fixture(test)) {
            actor.open();
            arcaneAndCrucible(test, actor.player);
        }
        test.succeed();
    }

    public static void infusionsAndVirtualComparator(GameTestHelper test) {
        try (var actor = new TubeVariantsRuntimeChecks.Fixture(test)) {
            actor.open();
            infusionsAndVirtualComparator(test, actor.player);
        }
        test.succeed();
    }

    public static void thaumatoriumOpeningSnapshot(GameTestHelper test) {
        try (var actor = new TubeVariantsRuntimeChecks.Fixture(test)) {
            actor.open();
            try (var fixture = new Fixture(test, actor.player)) {
                BlockPos pos = fixture.origin;
                fixture.place(pos.below(), ThaumcraftAlchemy.CRUCIBLE.get().defaultBlockState());
                fixture.place(pos.above(), ThaumcraftThaumatorium.THAUMATORIUM_TOP.get().defaultBlockState());
                fixture.place(pos, ThaumcraftThaumatorium.THAUMATORIUM.get().defaultBlockState());
                TileThaumatorium machine = (TileThaumatorium) fixture.level.getBlockEntity(pos);
                actor.player.setPos(Vec3.atCenterOf(pos).add(0, 1, 0));
                KnowledgeAccess.get(actor.player).addResearch(RESEARCH);
                ItemStack expected = named(Items.DIAMOND_BLOCK);
                var recipe = fixture.register(ThaumcraftApi.addCrucibleRecipe(RESEARCH, expected, named(Items.PHANTOM_MEMBRANE),
                        new AspectList().add(Aspect.AIR, 2).add(Aspect.EARTH, 3)));
                machine.setItem(0, named(Items.PHANTOM_MEMBRANE));
                var serverRecipes = ProgrammaticRecipes.crucibles(fixture.level.getRecipeManager());
                var serverMenu = new ContainerThaumatorium(122, actor.player.getInventory(), machine, serverRecipes);
                actor.player.containerMenu = serverMenu;
                int selected = -1;
                for (int index = 0; index < serverRecipes.size(); index++) if (serverRecipes.get(index).value().programmatic() == recipe) selected = index;
                test.assertTrue(selected >= 0 && serverMenu.clickMenuButton(actor.player, selected)
                        && machine.isRecipeSelected(serverRecipes.get(selected).id()), "the server's authoritative menu index selects the actual addon formula");
                FriendlyByteBuf opening = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
                try {
                    opening.writeBlockPos(pos);
                    thaumcraft.common.crafting.CrucibleRecipeDisplay.write(opening, fixture.level.registryAccess(), serverRecipes);
                    // Simulate a client without this server-only registration in the shared-process test.
                    ThaumcraftApi.unregisterCraftingRecipe(recipe);
                    var clientMenu = new ContainerThaumatorium(122, actor.player.getInventory(), opening);
                    var preview = clientMenu.recipes().get(selected);
                    test.assertTrue(clientMenu.recipes().size() == serverMenu.recipes().size() && preview.id().equals(serverRecipes.get(selected).id())
                            && preview.value().programmatic() == null && ItemStack.matches(expected, preview.value().getResultItem(fixture.level.registryAccess()))
                            && preview.value().aspects().equals(Map.of("aer", 2, "terra", 3)),
                            "opening data retains exact output components, costs and indices without consulting or recreating client addon callbacks");
                } finally { opening.release(); }
                for (int invalid : new int[]{-1, thaumcraft.common.crafting.CrucibleRecipeDisplay.MAX_RECIPES + 1}) {
                    FriendlyByteBuf malformed = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
                    try {
                        malformed.writeVarInt(invalid);
                        boolean rejected = false;
                        try { thaumcraft.common.crafting.CrucibleRecipeDisplay.read(malformed, fixture.level.registryAccess()); }
                        catch (io.netty.handler.codec.DecoderException expectedFailure) { rejected = true; }
                        test.assertTrue(rejected, "invalid opening formula count is rejected before allocation: " + invalid);
                    } finally { malformed.release(); }
                }
                machine.setItem(0, ItemStack.EMPTY);
            }
        }
        test.succeed();
    }

    public static void arcaneAndCrucible(GameTestHelper test, ServerPlayer player) {
        Fixture fixture = new Fixture(test, player);
        TileArcaneWorkbench table = null;
        try {
            KnowledgeAccess.get(player).addResearch("API_NAME_FIRST");
            test.assertTrue(thaumcraft.api.ThaumcraftApiHelper.isResearchComplete(player.getGameProfile().getName(), "API_NAME_FIRST"),
                    "the username API resolves the first joined player's knowledge without a secret fixture bootstrap");
            var originalHandler = ThaumcraftApi.internalMethods;
            try {
                int[] customCalls = {0};
                ThaumcraftApi.internalMethods = new thaumcraft.api.internal.IInternalMethodHandler() {
                    @Override public boolean isResearchComplete(String username, String key) {
                        customCalls[0]++; return username.equals(player.getGameProfile().getName()) && key.equals("CUSTOM_HANDLER");
                    }
                };
                test.assertTrue(thaumcraft.api.ThaumcraftApiHelper.isResearchComplete(player, "CUSTOM_HANDLER") && customCalls[0] == 1,
                        "the player-scoped helper preserves legacy addon username overrides");
            } finally { ThaumcraftApi.internalMethods = originalHandler; }
            BlockPos pos = fixture.origin;
            fixture.place(pos, ThaumcraftArcane.ARCANE_WORKBENCH.get().defaultBlockState());
            player.setPos(Vec3.atCenterOf(pos).add(0, 1, 0));
            player.setGameMode(GameType.SURVIVAL);
            table = (TileArcaneWorkbench) fixture.level.getBlockEntity(pos);
            var shaped = fixture.register(ThaumcraftApi.addArcaneCraftingRecipe(RESEARCH, new ItemStack(Items.DIAMOND),
                    new AspectList().add(Aspect.AIR, 1), "AB", 'A', named(Items.RABBIT_FOOT), 'B', named(Items.GHAST_TEAR)));
            var shapeless = fixture.register(ThaumcraftApi.addShapelessArcaneCraftingRecipe(RESEARCH, new ItemStack(Items.EMERALD),
                    new AspectList().add(Aspect.AIR, 1), named(Items.BLAZE_POWDER), named(Items.AMETHYST_SHARD)));
            table.setItem(10, wand(330));
            table.setItem(0, named(Items.RABBIT_FOOT)); table.setItem(1, named(Items.GHAST_TEAR));
            var menu = new ContainerArcaneWorkbench(121, player.getInventory(), table); player.containerMenu = menu;
            menu.broadcastChanges();
            test.assertTrue(menu.getSlot(0).getItem().isEmpty(), "programmatic research gate is checked by the actual menu");
            KnowledgeAccess.get(player).addResearch(RESEARCH);
            menu.broadcastChanges();
            test.assertTrue(menu.getSlot(0).getItem().is(Items.DIAMOND), "addArcaneCraftingRecipe reaches the real result slot");
            menu.clicked(0, 0, ClickType.PICKUP, player);
            test.assertTrue(menu.getCarried().is(Items.DIAMOND) && table.getItem(0).isEmpty() && table.getItem(1).isEmpty()
                    && ItemWandCasting.getVis(table.getItem(10), "aer") == 220, "shaped addon output pays both physical inputs and110 centivis once");
            menu.setCarried(ItemStack.EMPTY);
            table.setItem(0, named(Items.BLAZE_POWDER)); table.setItem(8, named(Items.AMETHYST_SHARD));
            menu.broadcastChanges(); menu.clicked(0, 0, ClickType.PICKUP, player);
            test.assertTrue(menu.getCarried().is(Items.EMERALD) && table.getItem(0).isEmpty() && table.getItem(8).isEmpty()
                    && ItemWandCasting.getVis(table.getItem(10), "aer") == 110, "shapeless addon recipe also executes, including distant matrix cells");
            menu.setCarried(ItemStack.EMPTY); player.closeContainer(); table.clearContent();

            BlockPos cruciblePos = pos.east(3);
            fixture.place(cruciblePos, ThaumcraftAlchemy.CRUCIBLE.get().defaultBlockState());
            TileCrucible crucible = (TileCrucible) fixture.level.getBlockEntity(cruciblePos);
            CompoundTag state = crucible.saveWithFullMetadata(fixture.level.registryAccess());
            state.putShort("Heat", (short) 151); state.putInt("Water", 1000);
            new AspectList().add(Aspect.AIR, 3).writeToNBT(state); crucible.loadWithComponents(state, fixture.level.registryAccess());
            var recipe = fixture.register(ThaumcraftApi.addCrucibleRecipe(RESEARCH, new ItemStack(Items.NETHER_STAR), named(Items.PHANTOM_MEMBRANE), new AspectList().add(Aspect.AIR, 2)));
            var before = ProgrammaticRecipes.crucibles(fixture.level.getRecipeManager()).stream().filter(holder -> holder.value().programmatic() == recipe).findFirst().orElseThrow();
            ThaumcraftApi.installRecipeManager(fixture.level.getRecipeManager(), fixture.level.registryAccess());
            test.assertTrue(ProgrammaticRecipes.crucible(fixture.level.getRecipeManager(), before.id()) == before.value(), "recipe-context reload retains the same addon holder and persistent job id");
            ItemEntity catalyst = new ItemEntity(fixture.level, cruciblePos.getX() + .5, cruciblePos.getY() + 1, cruciblePos.getZ() + .5, named(Items.PHANTOM_MEMBRANE));
            catalyst.setThrower(player); fixture.level.addFreshEntity(catalyst); fixture.entities.add(catalyst);
            crucible.attemptSmelt(catalyst);
            test.assertTrue(catalyst.isRemoved() && crucible.getWaterAmount() == 950 && crucible.getAspects().getAmount(Aspect.AIR) == 1,
                    "addCrucibleRecipe consumes exactly one catalyst, two declared essentia and50mB through the actual crucible");
            TileCrucible.tickOutputs(fixture.level);
            var outputs = fixture.level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(cruciblePos).inflate(3), entity -> entity.getItem().is(Items.NETHER_STAR));
            fixture.entities.addAll(outputs);
            test.assertTrue(outputs.stream().mapToInt(entity -> entity.getItem().getCount()).sum() == 1, "real crucible queue emits exactly the registered addon output");
            var first = fixture.register(ThaumcraftApi.addCrucibleRecipe(RESEARCH, new ItemStack(Items.IRON_INGOT), Items.BRICK, new AspectList().add(Aspect.AIR, 1)));
            var second = fixture.register(ThaumcraftApi.addCrucibleRecipe(RESEARCH, new ItemStack(Items.GOLD_INGOT), Items.BRICK, new AspectList().add(Aspect.AIR, 1)));
            first.hash = second.hash = 0x7a52cf19;
            ProgrammaticRecipes.crucibles(fixture.level.getRecipeManager());
            var secondId = ProgrammaticRecipes.crucibles(fixture.level.getRecipeManager()).stream().filter(holder -> holder.value().programmatic() == second).findFirst().orElseThrow().id();
            ThaumcraftApi.unregisterCraftingRecipe(first);
            var third = fixture.register(ThaumcraftApi.addCrucibleRecipe(RESEARCH, new ItemStack(Items.COPPER_INGOT), Items.BRICK, new AspectList().add(Aspect.AIR, 1))); third.hash = second.hash;
            var collisionRecipes = ProgrammaticRecipes.crucibles(fixture.level.getRecipeManager());
            var thirdId = collisionRecipes.stream().filter(holder -> holder.value().programmatic() == third).findFirst().orElseThrow().id();
            test.assertTrue(!secondId.equals(thirdId) && ProgrammaticRecipes.crucible(fixture.level.getRecipeManager(), secondId).programmatic() == second,
                    "removing a colliding registration never reuses another persistent Thaumatorium job id");
        } finally {
            if (table != null && !table.isRemoved()) table.clearContent();
            fixture.close();
        }
    }

    public static void infusionsAndVirtualComparator(GameTestHelper test, ServerPlayer player) {
        Fixture fixture = new Fixture(test, player);
        List<TilePedestal> pedestals = new ArrayList<>();
        try {
            BlockPos centerPos = fixture.origin.above(8), matrixPos = centerPos.above(2);
            fixture.chunks.addAll(GolemChunks.hold(fixture.level, List.of(centerPos.offset(-13, 0, -13), centerPos.offset(13, 3, 13))));
            for (int x = -12; x <= 12; x++) for (int z = -12; z <= 12; z++) {
                fixture.place(centerPos.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                fixture.place(centerPos.offset(x, 0, z), Math.abs(x) <= 1 && Math.abs(z) <= 1 ? Blocks.AIR.defaultBlockState() : Blocks.SKELETON_SKULL.defaultBlockState());
            }
            fixture.place(centerPos, ThaumcraftInfusion.PEDESTAL.get().defaultBlockState());
            fixture.place(matrixPos, ThaumcraftInfusion.MATRIX.get().defaultBlockState());
            for (int x : new int[]{-1, 1}) for (int z : new int[]{-1, 1}) {
                fixture.place(centerPos.offset(x, 0, z), ThaumcraftInfusion.PILLAR.get().defaultBlockState().setValue(BlockInfusionPillar.HALF, DoubleBlockHalf.LOWER));
                fixture.place(centerPos.offset(x, 1, z), ThaumcraftInfusion.PILLAR.get().defaultBlockState().setValue(BlockInfusionPillar.HALF, DoubleBlockHalf.UPPER));
            }
            fixture.place(centerPos.west(3), ThaumcraftInfusion.PEDESTAL.get().defaultBlockState());
            fixture.place(centerPos.east(3), ThaumcraftInfusion.PEDESTAL.get().defaultBlockState());
            fixture.place(centerPos.offset(0, 0, 5), ThaumcraftContent.JAR.get().defaultBlockState());
            TilePedestal center = (TilePedestal) fixture.level.getBlockEntity(centerPos);
            TilePedestal component = (TilePedestal) fixture.level.getBlockEntity(centerPos.west(3));
            pedestals.add(center); pedestals.add(component);
            TileJar source = (TileJar) fixture.level.getBlockEntity(centerPos.offset(0, 0, 5)); source.addToContainer(Aspect.AIR, 3);
            TileInfusionMatrix matrix = (TileInfusionMatrix) fixture.level.getBlockEntity(matrixPos);
            player.setPos(Vec3.atCenterOf(matrixPos).add(0, 0, 2)); player.setGameMode(GameType.SURVIVAL);
            KnowledgeAccess.get(player).addResearch(RESEARCH);
            fixture.register(ThaumcraftApi.addInfusionCraftingRecipe(RESEARCH, named(Items.NETHER_STAR), 0, new AspectList().add(Aspect.AIR, 1),
                    named(Items.RABBIT_FOOT), new ItemStack[]{named(Items.GHAST_TEAR)}));
            center.setInfusionStack(named(Items.RABBIT_FOOT)); component.setInfusionStack(named(Items.GHAST_TEAR)); matrix.setActive(true);
            test.assertTrue(matrix.startCrafting(player), "programmatic stack infusion starts through the formed, registered matrix");
            CompoundTag saved = matrix.saveWithFullMetadata(fixture.level.registryAccess());
            fixture.level.removeBlockEntity(matrixPos);
            matrix = new TileInfusionMatrix(matrixPos, fixture.level.getBlockState(matrixPos));
            matrix.loadWithComponents(saved, fixture.level.registryAccess()); fixture.level.setBlockEntity(matrix);
            tickUntilDone(test, fixture.level, matrix);
            test.assertTrue(center.getItem(0).is(Items.NETHER_STAR) && component.isEmpty() && source.getAmount() == 2,
                    "stack infusion survives session reload and pays its real component and one finite essentia before output");

            var enchantment = fixture.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING);
            fixture.register(ThaumcraftApi.addInfusionEnchantmentRecipe(RESEARCH, enchantment, 0, new AspectList().add(Aspect.AIR, 1), new ItemStack[]{named(Items.PHANTOM_MEMBRANE)}));
            ItemStack tool = new ItemStack(Items.IRON_PICKAXE), ingredient = named(Items.PHANTOM_MEMBRANE);
            int[] comparisons = {0};
            InfusionEnchantmentRecipe refusing = new InfusionEnchantmentRecipe(RESEARCH, enchantment, 0, new AspectList().add(Aspect.AIR, 1), new ItemStack[]{ingredient}) {
                @Override protected boolean areItemStacksEqual(ItemStack offered, ItemStack required, boolean fuzzy) { comparisons[0]++; return false; }
            };
            test.assertTrue(!refusing.matches(new ArrayList<>(List.of(ingredient.copy())), tool.copy(), fixture.level, player) && comparisons[0] > 0,
                    "the original protected enchantment comparator can veto an otherwise valid native admission");
            InfusionEnchantmentRecipe mutating = new InfusionEnchantmentRecipe(RESEARCH, enchantment, 0, new AspectList().add(Aspect.AIR, 1), new ItemStack[]{ingredient}) {
                @Override protected boolean areItemStacksEqual(ItemStack offered, ItemStack required, boolean fuzzy) { offered.setCount(0); return false; }
            };
            var callerIngredients = new ArrayList<>(List.of(ingredient.copy()));
            test.assertTrue(!mutating.matches(callerIngredients, tool, fixture.level, player) && callerIngredients.getFirst().getCount() == 1 && tool.getCount() == 1,
                    "virtual comparators receive original-style copies, never consume caller-owned offered stacks");
            center.setInfusionStack(tool); component.setInfusionStack(ingredient); player.giveExperienceLevels(10);
            int beforeLevels = player.experienceLevel;
            test.assertTrue(matrix.startCrafting(player) && matrix.getRecipeType() == 1, "addInfusionEnchantmentRecipe reaches the actual enchantment ritual");
            int costLevels = matrix.getRemainingXPLevels();
            tickUntilDone(test, fixture.level, matrix);
            test.assertTrue(center.getItem(0).getEnchantments().getLevel(enchantment) == 1 && component.isEmpty() && source.getAmount() == 1
                    && player.experienceLevel == beforeLevels - costLevels, "programmatic enchantment output pays declared levels, physical component and finite essentia once");
        } finally {
            for (TilePedestal pedestal : pedestals) if (!pedestal.isRemoved()) pedestal.setInfusionStack(ItemStack.EMPTY);
            fixture.close();
        }
    }

    private static void tickUntilDone(GameTestHelper test, ServerLevel level, TileInfusionMatrix matrix) {
        // Actual production ticker; no clocks, costs or outcomes are replaced. The altar and stocks are declared fixtures.
        for (int tick = 0; tick < 3000 && matrix.isCrafting(); tick++) TileInfusionMatrix.tick(level, matrix.getBlockPos(), matrix.getBlockState(), matrix);
        test.assertTrue(!matrix.isCrafting(), "finite addon ritual completes on actual matrix ticker transitions");
    }
    private static ItemStack named(net.minecraft.world.item.Item item) { ItemStack stack = new ItemStack(item); stack.set(DataComponents.CUSTOM_NAME, Component.literal("programmatic-recipe-only")); return stack; }
    private static ItemStack wand(int amount) { ItemStack stack = new ItemStack(ThaumcraftContent.WAND.get()); stack.set(ThaumcraftContent.WAND_DATA.get(), WandData.EMPTY.withAmount("aer", amount)); return stack; }

    private static final class Fixture implements AutoCloseable {
        final ServerLevel level; final ServerPlayer player; final BlockPos origin;
        final Map<BlockPos, BlockState> states = new LinkedHashMap<>(); final Map<BlockPos, CompoundTag> tags = new LinkedHashMap<>();
        final List<Object> registrations = new ArrayList<>(); final List<ItemEntity> entities = new ArrayList<>(); final List<ChunkPos> chunks = new ArrayList<>();
        final Vec3 position; final GameType mode; final List<ItemStack> inventory = new ArrayList<>(); final CompoundTag knowledge;
        Fixture(GameTestHelper test, ServerPlayer player) {
            this.level = test.getLevel(); this.player = player; origin = test.absolutePos(new BlockPos(4, 4, 4)); position = player.position(); mode = player.gameMode.getGameModeForPlayer();
            knowledge = KnowledgeAccess.get(player).save();
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) inventory.add(player.getInventory().getItem(i).copy());
            player.closeContainer(); player.getInventory().clearContent();
        }
        <T> T register(T recipe) { registrations.add(recipe); return recipe; }
        void place(BlockPos pos, BlockState state) {
            if (!states.containsKey(pos)) { states.put(pos.immutable(), level.getBlockState(pos)); if (level.getBlockEntity(pos) != null) tags.put(pos.immutable(), level.getBlockEntity(pos).saveWithFullMetadata(level.registryAccess())); }
            level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        @Override public void close() {
            player.closeContainer(); for (ItemEntity entity : entities) if (!entity.isRemoved()) entity.discard();
            for (Object recipe : registrations) ThaumcraftApi.unregisterCraftingRecipe(recipe);
            states.forEach((pos, state) -> level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE));
            tags.forEach((pos, tag) -> { if (level.getBlockEntity(pos) != null) level.getBlockEntity(pos).loadWithComponents(tag, level.registryAccess()); });
            for (ChunkPos chunk : chunks) level.setChunkForced(chunk.x, chunk.z, false);
            for (int i = 0; i < inventory.size(); i++) player.getInventory().setItem(i, inventory.get(i));
            KnowledgeAccess.get(player).load(knowledge); player.setGameMode(mode); player.setPos(position);
        }
    }
}
