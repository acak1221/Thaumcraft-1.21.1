package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftDeconstruction;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.container.ContainerDeconstructionTable;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.DeconstructionRules;
import thaumcraft.common.lib.research.ObjectAspectResolver;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.tiles.TileArcaneWorkbench;
import thaumcraft.common.tiles.TileDeconstructionTable;

/** Native crafting, real 40-tick work/RNG, authoritative RP take, hopper policy and actual saved ownership. */
public final class DeconstructionRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private DeconstructionRuntimeChecks() {}
    public static void run(GameTestHelper test) {
        var check = new Check(test);
        try { check.prepare(); } catch (RuntimeException | Error failure) { check.finish(); throw failure; }
        test.runAfterDelay(5, () -> {
            try { check.acquire(); check.workAndClaim(); check.automation(); check.breakPreservesActualInput(); check.ordinaryTicks(); }
            catch (RuntimeException | Error failure) { check.finish(); throw failure; }
        });
    }
    private static final class Check {
        final GameTestHelper test;
        final ServerLevel level;
        final ServerPlayer player;
        final BlockPos pos;
        final Map<BlockPos, BlockState> original = new LinkedHashMap<>();
        final List<ChunkPos> forced = new ArrayList<>();
        final Set<UUID> oldEntities = new HashSet<>();
        TileArcaneWorkbench workbench;
        TileDeconstructionTable table;
        boolean finished;
        Check(GameTestHelper test) { this.test = test; level = test.getLevel(); player = test.makeMockServerPlayerInLevel(); pos = test.absolutePos(new BlockPos(3, 100, 3)); }
        ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }
        void set(BlockPos where, BlockState state) { original.putIfAbsent(where, level.getBlockState(where)); level.setBlock(where, state, QUIET); }
        void prepare() {
            forced.addAll(GolemChunks.hold(level, List.of(pos.offset(-3, -3, -3), pos.offset(3, 3, 3))));
            level.getAllEntities().forEach(entity -> oldEntities.add(entity.getUUID()));
            for (BlockPos where : BlockPos.betweenClosed(pos.offset(-3, -3, -3), pos.offset(3, 3, 3))) set(where.immutable(), Blocks.AIR.defaultBlockState());
            player.getInventory().clearContent(); player.setPos(Vec3.atCenterOf(pos).add(1, 0, 2));
            set(pos.below(), Blocks.STONE.defaultBlockState());
            set(pos.east(2), ThaumcraftArcane.ARCANE_WORKBENCH.get().defaultBlockState());
            workbench = (TileArcaneWorkbench) level.getBlockEntity(pos.east(2));
        }
        void acquire() {
            var recipe = (ArcaneShapedRecipe) level.getRecipeManager().byKey(id("arcane/deconstruction_table")).orElseThrow().value();
            test.assertTrue(recipe.research().equals("DECONSTRUCTOR") && recipe.pattern().equals(List.of(" S ", "ATP")) && recipe.vis().equals(Map.of("perditio", 20))
                            && recipe.getResultItem(level.registryAccess()).is(ThaumcraftDeconstruction.ITEM.get()), "the real Deconstructor recipe retains original research, pattern and Entropy20");
            workbench.setItem(1, new ItemStack(ThaumcraftContent.THAUMOMETER.get()));
            workbench.setItem(3, new ItemStack(Items.GOLDEN_AXE)); workbench.setItem(4, new ItemStack(ThaumcraftContent.TABLE_ITEM.get())); workbench.setItem(5, new ItemStack(Items.GOLDEN_PICKAXE));
            ItemStack wand = new ItemStack(ThaumcraftContent.WAND.get());
            var vis = new LinkedHashMap<String, Integer>(); for (String primal : WandData.PRIMALS) vis.put(primal, 5000);
            wand.set(ThaumcraftContent.WAND_DATA.get(), new WandData("silverwood", "iron", vis)); workbench.setItem(10, wand);
            var menu = new ContainerArcaneWorkbench(31, player.getInventory(), workbench); player.containerMenu = menu;
            KnowledgeAccess.get(player).removeResearch("DECONSTRUCTOR"); menu.broadcastChanges();
            var before = workbench.saveWithoutMetadata(level.registryAccess());
            menu.clicked(0, 0, ClickType.PICKUP, player);
            test.assertTrue(menu.getCarried().isEmpty() && workbench.saveWithoutMetadata(level.registryAccess()).equals(before), "unresearched acquisition spends no tools, base table, thaumometer or vis");
            KnowledgeAccess.get(player).addResearch("DECONSTRUCTOR"); menu.broadcastChanges();
            Map<String, Integer> price = ArcaneVis.price(recipe.vis(), workbench.getItem(10), player);
            test.assertTrue(menu.craftable() && menu.getSlot(0).getItem().is(ThaumcraftDeconstruction.ITEM.get()), "actual registered pattern matches the four original ingredients");
            menu.clicked(0, 0, ClickType.PICKUP, player);
            test.assertTrue(menu.getCarried().is(ThaumcraftDeconstruction.ITEM.get()) && menu.getCarried().getCount() == 1
                            && workbench.getItem(1).isEmpty() && workbench.getItem(3).isEmpty() && workbench.getItem(4).isEmpty() && workbench.getItem(5).isEmpty(), "one paid native craft consumes exactly the four physical ingredients");
            for (String primal : WandData.PRIMALS) test.assertTrue(ItemWandCasting.data(workbench.getItem(10)).amount(primal) == 5000 - price.getOrDefault(primal, 0), "exact Deconstructor vis debit " + primal);
            ItemStack result = menu.getCarried(); menu.setCarried(ItemStack.EMPTY); player.containerMenu = player.inventoryMenu;
            result.set(DataComponents.CUSTOM_NAME, Component.literal("deconstruction-owner-fixture")); player.setItemInHand(InteractionHand.MAIN_HAND, result);
            var hit = new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0, .5, 0), Direction.UP, pos.below(), false);
            test.assertTrue(result.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction()
                            && player.getMainHandItem().isEmpty() && level.getBlockEntity(pos) instanceof TileDeconstructionTable, "the actually crafted block item places the registered ticking table");
            table = (TileDeconstructionTable) level.getBlockEntity(pos);
            test.assertTrue(table.getCustomName() != null && table.getCustomName().getString().equals("deconstruction-owner-fixture"), "native block-item custom-name component transfers to the actual table");
        }
        void ticks(int count) { for (int tick = 0; tick < count; tick++) TileDeconstructionTable.tick(level, pos, level.getBlockState(pos), table); }
        long seed(AspectList primals, boolean succeeds) {
            for (long seed = 0; seed < 100000; seed++) if ((RandomSource.create(seed).nextInt(80) < primals.visSize()) == succeeds) return seed;
            throw new AssertionError("No deterministic fixture seed");
        }
        void workAndClaim() {
            var invalid = new ItemStack(Items.BARRIER);
            test.assertTrue(ObjectAspectResolver.get(invalid, level).size() == 0 && !table.canPlaceItem(0, invalid), "an untagged real item cannot enter the input through a native Slot");
            table.setItem(0, invalid); ticks(45);
            test.assertTrue(table.getItem(0).is(Items.BARRIER) && table.breakTime() == 0 && table.reward() == null, "an externally supplied untagged item is preserved and never starts work");
            ItemStack input = new ItemStack(Items.IRON_INGOT, 3); input.set(DataComponents.CUSTOM_NAME, Component.literal("exact-input-components")); table.setItem(0, input);
            AspectList primals = DeconstructionRules.primals(ObjectAspectResolver.get(input.copyWithCount(1), level));
            test.assertTrue(primals.visSize() > 0 && primals.visSize() < 80, "the real iron input has both success and failure outcomes");
            ticks(39);
            test.assertTrue(table.breakTime() == 1 && table.getItem(0).getCount() == 3 && table.reward() == null, "39 real work ticks leave every input intact and exactly one tick remaining");
            level.random.setSeed(seed(primals, false)); ticks(1);
            test.assertTrue(table.breakTime() == 0 && table.getItem(0).getCount() == 2 && table.reward() == null, "a failed original 1-in-80 chance still consumes exactly one item on tick40");
            ticks(39); long successSeed = seed(primals, true); RandomSource oracle = RandomSource.create(successSeed);
            oracle.nextInt(80); Aspect expected = primals.getAspects()[oracle.nextInt(primals.size())];
            level.random.setSeed(successSeed); ticks(1);
            test.assertTrue(table.getItem(0).getCount() == 1 && table.reward() == expected && table.breakTime() == 0, "the success path pays one physical item and performs the separate distinct-primal draw");
            ticks(90); test.assertTrue(table.getItem(0).getCount() == 1 && table.reward() == expected, "an unclaimed point pauses processing without consuming another item");
            var stale = new ContainerDeconstructionTable(32, player.getInventory(), table); player.containerMenu = stale;
            var saved = table.saveWithoutMetadata(level.registryAccess()); var loaded = new TileDeconstructionTable(pos, level.getBlockState(pos)); loaded.loadWithComponents(saved, level.registryAccess());
            test.assertTrue(!loaded.isQuarantined() && loaded.reward() == expected && loaded.breakTime() == 0 && ItemStack.matches(input.copyWithCount(1), loaded.getItem(0))
                            && loaded.getCustomName().equals(table.getCustomName()), "real NBT preserves the unclaimed primal, full item components/count and custom name");
            level.removeBlockEntity(pos); level.setBlockEntity(loaded); table = loaded;
            test.assertTrue(!stale.stillValid(player) && !stale.clickMenuButton(player, 1) && table.reward() == expected, "an old menu cannot claim from a replacement table at the same coordinates");
            var menu = new ContainerDeconstructionTable(33, player.getInventory(), table); player.containerMenu = menu;
            int beforePool = KnowledgeAccess.get(player).getAspectPoolFor(expected.getTag());
            test.assertTrue(!menu.clickMenuButton(player, 0) && !menu.clickMenuButton(player, 2), "invalid reward buttons grant no RP");
            Vec3 near = player.position(); player.setPos(Vec3.atCenterOf(pos).add(20, 0, 0));
            test.assertTrue(!menu.clickMenuButton(player, 1) && table.reward() == expected, "a remote menu request cannot take the shared table reward"); player.setPos(near);
            player.containerMenu = player.inventoryMenu;
            test.assertTrue(!menu.clickMenuButton(player, 1) && table.reward() == expected, "a closed menu has no claim authority"); player.containerMenu = menu;
            test.assertTrue(menu.reward() == expected && menu.clickMenuButton(player, 1) && table.reward() == null && menu.reward() == null
                            && KnowledgeAccess.get(player).getAspectPoolFor(expected.getTag()) == beforePool + 1 && table.getItem(0).getCount() == 1,
                    "one actual button1 claim clears the reward and grants exactly ONE RP without spending another item");
            test.assertTrue(!menu.clickMenuButton(player, 1) && KnowledgeAccess.get(player).getAspectPoolFor(expected.getTag()) == beforePool + 1, "replaying button1 cannot claim the same primal twice");
            ticks(9); saved = table.saveWithoutMetadata(level.registryAccess()); var partial = new TileDeconstructionTable(pos, level.getBlockState(pos)); partial.loadWithComponents(saved, level.registryAccess());
            test.assertTrue(table.breakTime() == 31 && partial.breakTime() == 0 && partial.reward() == null && ItemStack.matches(table.getItem(0), partial.getItem(0)), "elapsed work restarts after reload while the actual unconsumed input remains");
            var malformed = saved.copy(); malformed.putString("Aspect", "unknown_deconstruction_primal"); var rejected = new TileDeconstructionTable(pos, level.getBlockState(pos)); rejected.loadWithComponents(malformed, level.registryAccess());
            test.assertTrue(rejected.isQuarantined() && rejected.getItem(0).isEmpty() && rejected.saveWithoutMetadata(level.registryAccess()).getCompound("RejectedDeconstruction").equals(malformed), "an unknown saved reward preserves its original payload and closes the table instead of becoming a free point");
            player.containerMenu = player.inventoryMenu; table.clearContent(); ticks(1);
        }
        HopperBlockEntity hopper(BlockPos where, Direction direction, ItemStack contents) {
            set(where, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, direction)); var hopper = (HopperBlockEntity) level.getBlockEntity(where); hopper.setItem(0, contents); return hopper;
        }
        void hopperTicks(HopperBlockEntity hopper, int count) { for (int tick = 0; tick < count; tick++) HopperBlockEntity.pushItemsTick(level, hopper.getBlockPos(), level.getBlockState(hopper.getBlockPos()), hopper); }
        void automation() {
            var above = hopper(pos.above(), Direction.DOWN, new ItemStack(Items.IRON_INGOT, 2)); hopperTicks(above, 20);
            test.assertTrue(above.getItem(0).getCount() == 2 && table.isEmpty() && table.getSlotsForFace(Direction.UP).length == 0
                            && !table.canPlaceItemThroughFace(0, above.getItem(0), Direction.UP), "a real hopper above cannot insert through the original closed upper face");
            var side = hopper(pos.west(), Direction.EAST, new ItemStack(Items.BARRIER)); hopperTicks(side, 20);
            test.assertTrue(side.getItem(0).is(Items.BARRIER) && table.isEmpty(), "native side-hopper insertion applies the actual aspect filter");
            side.setItem(0, new ItemStack(Items.IRON_INGOT, 2)); hopperTicks(side, 20);
            test.assertTrue(side.isEmpty() && table.getItem(0).getCount() == 2 && above.getItem(0).getCount() == 2, "the allowed side accepts exactly the two actual hopper items without leaking the upper fixture");
            var below = hopper(pos.below(), Direction.EAST, ItemStack.EMPTY);
            test.assertTrue(HopperBlockEntity.suckInItems(level, below) && below.getItem(0).is(Items.IRON_INGOT) && below.getItem(0).getCount() == 1 && table.getItem(0).getCount() == 1,
                    "a native hopper below extracts one physical input through the lower face");
            above.clearContent(); side.clearContent(); below.clearContent(); table.clearContent();
            set(pos.above(), Blocks.AIR.defaultBlockState()); set(pos.west(), Blocks.AIR.defaultBlockState()); set(pos.below(), Blocks.STONE.defaultBlockState());
        }
        void breakPreservesActualInput() {
            ItemStack stored = new ItemStack(Items.IRON_INGOT, 5); stored.set(DataComponents.CUSTOM_NAME, Component.literal("deconstruction-break-input")); table.setItem(0, stored);
            var drops = Block.getDrops(level.getBlockState(pos), level, pos, table);
            test.assertTrue(drops.size() == 1 && drops.getFirst().is(ThaumcraftDeconstruction.ITEM.get()) && drops.getFirst().getCount() == 1, "the actual block loot is one deconstruction table");
            var retired = table; set(pos, Blocks.AIR.defaultBlockState()); retired.evacuate(); DeferredItemDrops.tick(level);
            var physical = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3), item -> !oldEntities.contains(item.getUUID()) && item.getItem().is(Items.IRON_INGOT));
            test.assertTrue(retired.isEmpty() && physical.stream().mapToInt(item -> item.getItem().getCount()).sum() == 5
                            && physical.stream().allMatch(item -> ItemStack.isSameItemSameComponents(item.getItem(), stored)), "real block removal evacuates the five named physical inputs exactly once through the durable queue");
            physical.forEach(ItemEntity::discard);
            set(pos, ThaumcraftDeconstruction.TABLE.get().defaultBlockState()); table = (TileDeconstructionTable) level.getBlockEntity(pos);
        }
        void ordinaryTicks() {
            set(pos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState()); table.setItem(0, new ItemStack(Items.STONE, 2));
            test.runAfterDelay(45, () -> {
                try {
                    test.assertTrue(table.getItem(0).is(Items.STONE) && table.getItem(0).getCount() == 1 && (table.reward() == null || table.reward().isPrimal()),
                            "ordinary registered block ticks consume one item in forty ticks even next to redstone, without any manual tick calls");
                    test.succeed();
                } finally { finish(); }
            });
        }
        void finish() {
            if (finished) return; finished = true; player.containerMenu = player.inventoryMenu;
            if (table != null && table.current()) table.clearContent(); if (workbench != null && !workbench.isRemoved()) workbench.clearContent();
            for (BlockPos where : original.keySet()) if (level.getBlockEntity(where) instanceof HopperBlockEntity hopper) hopper.clearContent();
            original.forEach((where, state) -> level.setBlock(where, state, QUIET));
            for (var drop : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4))) if (!oldEntities.contains(drop.getUUID())) drop.discard();
            for (ChunkPos chunk : forced) level.setChunkForced(chunk.x, chunk.z, false);
        }
    }
}
