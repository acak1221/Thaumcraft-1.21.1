package thaumcraft.common.testing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.items.ItemEssence;
import thaumcraft.common.items.PhialData;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TileNode;
import java.util.List;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.Vec3;

/** Runtime assertions against mapped Minecraft, real registries, and applied mixins. */
public final class RuntimeChecks {
    private RuntimeChecks() {}
    public static void bookshelfWandCreatesOneFloatingBook(GameTestHelper test) {
        var player = test.makeMockPlayer(GameType.SURVIVAL);
        BlockPos relative = new BlockPos(2, 2, 2);
        var absolute = test.absolutePos(relative);
        test.setBlock(relative, net.minecraft.world.level.block.Blocks.BOOKSHELF);
        ItemStack wand = new ItemStack(ThaumcraftContent.WAND.get());
        wand.set(ThaumcraftContent.WAND_DATA.get(), WandData.EMPTY.withAmount("aer", 1234));
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        var hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(absolute), net.minecraft.core.Direction.UP, absolute, false);
        var context = new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, hit);
        var item = ThaumcraftContent.WAND.get();
        test.assertValueEqual(item.useOn(context), net.minecraft.world.InteractionResult.CONSUME, "native bookshelf trigger consumes interaction");
        test.assertBlockPresent(net.minecraft.world.level.block.Blocks.AIR, relative);
        var bounds = new net.minecraft.world.phys.AABB(absolute).inflate(1);
        var entities = test.getLevel().getEntitiesOfClass(thaumcraft.common.entities.EntitySpecialItem.class, bounds);
        test.assertValueEqual(entities.size(), 1, "one bookshelf yields exactly one special result");
        var book = entities.getFirst();
        test.assertTrue(book.getItem().is(ThaumcraftContent.THAUMONOMICON.get()) && book.getItem().getCount() == 1, "original normal-book output");
        test.assertValueEqual(ItemWandCasting.getVis(wand, "aer"), 1234, "trigger costs no vis");
        test.assertTrue(wand.getCount() == 1, "trigger retains wand");
        test.assertValueEqual(item.useOn(context), net.minecraft.world.InteractionResult.PASS, "repeated trigger on air does not create another book");
        test.assertValueEqual(test.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, bounds).size(), 1, "bookshelf drops no additional planks/books");
        // The original refuses every explosion and takes every other damage like a vanilla item
        // entity (oracle EntitySpecialItem.java:45-49: `if (source.isExplosion()) return false`).
        test.assertTrue(!book.hurt(test.getLevel().damageSources().explosion(null, null), 100) && book.isAlive(),
                "original special item refuses an explosion instead of burning");
        CompoundTag saved = new CompoundTag();
        test.assertTrue(book.save(saved), "special output can be saved with native type id");
        var restored = net.minecraft.world.entity.EntityType.loadEntityRecursive(saved, test.getLevel(), java.util.function.Function.identity());
        test.assertTrue(restored instanceof thaumcraft.common.entities.EntitySpecialItem
                && ((thaumcraft.common.entities.EntitySpecialItem)restored).getItem().is(ThaumcraftContent.THAUMONOMICON.get()), "native save/reload keeps type and book");
        // The original creates the result at (x+0.5, y+0.3, z+0.5) with a zero motion
        // (oracle WandManager.java:408-421) and its buoyancy adds 0.04 before the vanilla item
        // gravity takes 0.04 again (oracle EntitySpecialItem.java:37-43 with the 1.7.10
        // EntityItem.java:82), so the book hovers exactly where the wand left it.
        test.assertTrue(book.getX() == absolute.getX() + 0.5 && book.getY() == absolute.getY() + 0.3 && book.getZ() == absolute.getZ() + 0.5
                        && book.getDeltaMovement().lengthSqr() == 0.0, "the original creates the result at x+0.5/y+0.3/z+0.5 with a zero motion");
        double originalY = book.getY();
        // The fixture drives the entity's own ticks: a GameTest that waits for world ticks lets a
        // neighbouring scenario's cleanup discard the result, and the hover of the original is a
        // property of the entity's own tick, not of the surrounding world.
        for (int tick = 0; tick < 8; tick++) book.tick();
        test.assertTrue(book.isAlive() && Math.abs(book.getY() - originalY) < 0.001,
                "result hovers where the original created it instead of sinking");
        test.assertTrue(test.getLevel().getEntitiesOfClass(thaumcraft.common.entities.EntitySpecialItem.class, bounds).size() == 1
                        && book.getItem().getCount() == 1, "the floating result is not picked up before the player can reach it");
        test.succeed();
    }

    public static void normalBookOpensWithoutGrantingArbitraryResearch(GameTestHelper test, ServerPlayer player) {
        var knowledge = KnowledgeAccess.get(player);
        knowledge.addResearch("DISTILESSENTIA");
        knowledge.removeResearch("JARLABEL");
        ItemStack book = new ItemStack(ThaumcraftContent.THAUMONOMICON.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, book);
        var beforeAspects = knowledge.getAspectsDiscovered();
        var result = ThaumcraftContent.THAUMONOMICON.get().use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        test.assertTrue(result.getResult().consumesAction() && result.getObject() == book, "book use opens without consuming item");
        test.assertTrue(knowledge.isResearchComplete("JARLABEL"), "normal book applies original known-research sibling");
        test.assertTrue(!knowledge.isResearchComplete("INFUSION"), "book is not a creative cheat sheet");
        test.assertValueEqual(knowledge.getAspectsDiscovered(), beforeAspects, "reading book does not grant aspects");
        test.succeed();
    }
    public static void originalEntryRecipes(GameTestHelper test) {
        checkRecipe(test, "wand_cap_iron", ThaumcraftContent.IRON_WAND_CAP.get(), 1,
                Items.IRON_NUGGET, Items.IRON_NUGGET, Items.IRON_NUGGET,
                Items.IRON_NUGGET, null, Items.IRON_NUGGET, null, null, null);
        checkRecipe(test, "wand_wood_iron", ThaumcraftContent.WAND.get(), 1,
                null, null, ThaumcraftContent.IRON_WAND_CAP.get(),
                null, Items.STICK, null, ThaumcraftContent.IRON_WAND_CAP.get(), null, null);
        checkRecipe(test, "phial_empty", ThaumcraftContent.EMPTY_PHIAL.get(), 8,
                null, Items.CLAY_BALL, null, Items.GLASS, null, Items.GLASS, null, Items.GLASS, null);
        checkRecipe(test, "thaumometer", ThaumcraftContent.THAUMOMETER.get(), 1,
                ThaumcraftContent.SHARDS.get("aer").get(), null, ThaumcraftContent.SHARDS.get("ignis").get(),
                Items.GOLD_INGOT, Items.GLASS, Items.GOLD_INGOT,
                ThaumcraftContent.SHARDS.get("aqua").get(), null, ThaumcraftContent.SHARDS.get("terra").get());
        test.succeed();
    }

    private static void checkRecipe(GameTestHelper test, String id, ItemLike result, int count, ItemLike... ingredients) {
        List<ItemStack> grid = java.util.Arrays.stream(ingredients)
                .map(item -> item == null ? ItemStack.EMPTY : new ItemStack(item)).toList();
        var input = CraftingInput.of(3, 3, grid);
        var recipe = test.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, test.getLevel()).orElseThrow();
        test.assertValueEqual(recipe.id().toString(), "thaumcraft:" + id, "exact original recipe selected");
        ItemStack output = recipe.value().assemble(input, test.getLevel().registryAccess());
        test.assertTrue(output.is(result.asItem()) && output.getCount() == count, "original output and count for " + id);
    }
    public static void bootstrapAndItemComponents(GameTestHelper test) {
        test.assertValueEqual(Aspect.aspects.size(), 48, "original aspect count");
        test.assertValueEqual(ResearchCatalog.all().size(), 201, "original research definition count");
        ItemStack wand = new ItemStack(ThaumcraftContent.WAND.get());
        wand.set(ThaumcraftContent.WAND_DATA.get(), WandData.EMPTY.withAmount("aer", 2450));
        var registries = test.getLevel().registryAccess();
        ItemStack restored = ItemStack.parse(registries, wand.save(registries)).orElseThrow();
        test.assertValueEqual(ItemWandCasting.getVis(restored, "aer"), 2450, "persistent native wand component");
        ItemStack phial = ItemEssence.filled("ignis");
        ItemStack phialRestored = ItemStack.parse(registries, phial.save(registries)).orElseThrow();
        test.assertValueEqual(ItemEssence.data(phialRestored).tag(), "ignis", "persistent native phial component");
        test.assertValueEqual(PhialData.AMOUNT, 8, "original phial amount");
        test.succeed();
    }

    public static void playerKnowledgeMixinPersists(GameTestHelper test) {
        var player = test.makeMockPlayer(GameType.SURVIVAL);
        var knowledge = KnowledgeAccess.get(player);
        knowledge.initializePrimalAspects(player.getRandom());
        test.assertTrue(knowledge.getAspectPoolFor("aer") >= 15 && knowledge.getAspectPoolFor("aer") <= 19,
                "original starting primal pool");
        knowledge.setAspectPool("aer", 37);
        knowledge.addResearch("NODETAPPER1");
        knowledge.scanItem("@minecraft:stone");
        CompoundTag saved = new CompoundTag();
        player.addAdditionalSaveData(saved);
        test.assertTrue(saved.contains("thaumcraft:player_knowledge"), "Player mixin writes native knowledge NBT");
        var restored = test.makeMockPlayer(GameType.SURVIVAL);
        restored.readAdditionalSaveData(saved);
        var data = KnowledgeAccess.get(restored);
        test.assertValueEqual(data.getAspectPoolFor("aer"), 37, "Player mixin restores aspect pool");
        test.assertTrue(data.isResearchComplete("NODETAPPER1") && data.hasScannedItem("@minecraft:stone"),
                "research and scan persistence");
        test.succeed();
    }

    public static void jarDropRetainsEssentia(GameTestHelper test) {
        BlockPos relative = new BlockPos(2, 2, 2);
        test.setBlock(relative, ThaumcraftContent.JAR.get());
        TileJar jar = test.getBlockEntity(relative);
        test.assertValueEqual(jar.addToContainer(Aspect.FIRE, 64), 0, "jar capacity accepts 64");
        test.assertValueEqual(jar.addToContainer(Aspect.FIRE, 8), 8, "overflow rejected");
        test.assertValueEqual(jar.addToContainer(Aspect.WATER, 8), 8, "mixed essentia rejected");
        var level = test.getLevel();
        var absolute = test.absolutePos(relative);
        ItemStack dropped = Block.getDrops(jar.getBlockState(), level, absolute, jar).getFirst();
        CustomData payload = dropped.get(DataComponents.BLOCK_ENTITY_DATA);
        test.assertTrue(payload != null, "jar drop contains native block entity component");
        test.setBlock(relative, net.minecraft.world.level.block.Blocks.AIR);
        test.setBlock(relative, ThaumcraftContent.JAR.get());
        TileJar restored = test.getBlockEntity(relative);
        test.assertTrue(payload.loadInto(restored, level.registryAccess()), "drop block entity data restores");
        test.assertValueEqual(restored.getAmount(), 64, "essentia survives drop restoration");
        test.assertValueEqual(restored.getAspect(), Aspect.FIRE, "aspect survives drop restoration");
        test.assertTrue(restored.takeFromContainer(Aspect.FIRE, 8), "phial extraction unit");
        test.assertValueEqual(restored.getAmount(), 56, "phial extraction conserves amount");
        test.succeed();
    }

    public static void nodeDrainPreservesTc4Rounding(GameTestHelper test) {
        nodeDrainPreservesTc4Rounding(test, test.makeMockServerPlayerInLevel());
    }

    public static void nodeDrainPreservesTc4Rounding(GameTestHelper test, ServerPlayer player) {
        BlockPos relative = new BlockPos(2, 2, 2);
        test.setBlock(relative, ThaumcraftContent.AURA_NODE.get());
        TileNode node = test.getBlockEntity(relative);
        node.setAspects(new AspectList().add(Aspect.AIR, 3));
        var pos = test.absolutePos(relative);
        player.setPos(pos.getX() + 1.5, pos.getY(), pos.getZ() + 0.5);
        ItemStack wand = new ItemStack(ThaumcraftContent.WAND.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        node.drainIntoWand(player, wand);
        test.assertValueEqual(ItemWandCasting.getVis(wand, "aer"), 100, "one node vis is 100 centivis");
        test.assertValueEqual(node.getAspects().getAmount(Aspect.AIR), 2, "node loses one whole vis");
        wand.set(ThaumcraftContent.WAND_DATA.get(), WandData.EMPTY.withAmount("aer", 2450));
        node.drainIntoWand(player, wand);
        test.assertValueEqual(ItemWandCasting.getVis(wand, "aer"), 2500, "wand capacity clamps partial final unit");
        test.assertValueEqual(node.getAspects().getAmount(Aspect.AIR), 1, "original integer leftover rounds partial unit");
        String scanId = node.getScanId();
        var tag = node.saveWithId(test.getLevel().registryAccess());
        node.loadWithComponents(tag, test.getLevel().registryAccess());
        test.assertValueEqual(node.getScanId(), scanId, "stable node scan identity");
        test.succeed();
    }

    public static void thaumometerNodeScanUsesTc4Rewards(GameTestHelper test) {
        thaumometerNodeScanUsesTc4Rewards(test, test.makeMockServerPlayerInLevel());
    }

    public static void thaumometerNodeScanUsesTc4Rewards(GameTestHelper test, ServerPlayer player) {
        BlockPos relative = new BlockPos(2, 2, 2);
        test.setBlock(relative, ThaumcraftContent.AURA_NODE.get());
        TileNode node = test.getBlockEntity(relative);
        node.setAspects(new AspectList().add(Aspect.AIR, 25).add(Aspect.EARTH, 50));
        var pos = test.absolutePos(relative);
        player.setNoGravity(true);
        // GameTests encase the template in barriers. Keep the observer inside
        // its 8-cube bounds so the ray does not hit the front barrier wall.
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() - 1.5);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(pos));
        assertScannerBlockTarget(test, player, pos);
        ItemStack thaumometer = new ItemStack(ThaumcraftContent.THAUMOMETER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, thaumometer);
        var knowledge = KnowledgeAccess.get(player);
        knowledge.setAspectPool("aer", 0);
        knowledge.setAspectPool("terra", 0);
        var item = ThaumcraftContent.THAUMOMETER.get();
        item.use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        test.assertTrue(thaumometer.has(ThaumcraftContent.SCAN_SESSION.get()), "server observes native node target");
        item.onUseTick(test.getLevel(), player, thaumometer, 25);
        test.assertValueEqual(knowledge.getAspectPoolFor("aer"), 0, "starting scan grants no early reward");
        test.runAfterDelay(21, () -> {
            // The real player may have ticked the use already; this also covers
            // loaders whose mock player does not advance item use on its own.
            item.onUseTick(test.getLevel(), player, thaumometer, 4);
            test.assertValueEqual(knowledge.getAspectPoolFor("aer"), 4, "25 node vis rewards TC4 minimum four");
            test.assertValueEqual(knowledge.getAspectPoolFor("terra"), 5, "50 node vis rewards five");
            test.assertTrue(knowledge.hasScannedPhenomena("@NODE" + node.getScanId()), "node scan uses phenomena identity");
            test.assertTrue(knowledge.getScannedItems().isEmpty() && knowledge.getScannedEntities().isEmpty(),
                    "node scan does not pollute item or entity markers");
            test.assertValueEqual(node.getAspects().getAmount(Aspect.AIR), 25, "scan conserves node air vis");
            test.assertValueEqual(node.getAspects().getAmount(Aspect.EARTH), 50, "scan conserves node earth vis");
            test.assertTrue(!thaumometer.has(ThaumcraftContent.SCAN_SESSION.get()), "completion clears scan session");
            test.succeed();
        });
    }

    public static void thaumometerBlockPickSharesDroppedItemIdentity(GameTestHelper test) {
        thaumometerBlockPickSharesDroppedItemIdentity(test, test.makeMockServerPlayerInLevel());
    }

    public static void thaumometerBlockPickSharesDroppedItemIdentity(GameTestHelper test, ServerPlayer player) {
        BlockPos relative = new BlockPos(2, 2, 2);
        test.setBlock(relative.below(), net.minecraft.world.level.block.Blocks.STONE);
        test.setBlock(relative, net.minecraft.world.level.block.Blocks.REDSTONE_WIRE);
        var pos = test.absolutePos(relative);
        player.setNoGravity(true);
        player.setPos(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() - 1.5);
        // Dust has a shallow outline; aiming at a full-block centre misses it.
        player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(pos.getX() + 0.5, pos.getY() + 0.03125, pos.getZ() + 0.5));
        assertScannerBlockTarget(test, player, pos);
        ItemStack thaumometer = new ItemStack(ThaumcraftContent.THAUMOMETER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, thaumometer);
        var knowledge = KnowledgeAccess.get(player);
        // This fixture checks shared block/item identity, not parent discovery.
        // Original ConfigAspects:143 is potentia2 + machina1, unlike the legacy
        // fallback used when this scenario was first written.
        knowledge.setAspectPool("potentia", 0);
        for (var parent : Aspect.MECHANISM.getComponents()) knowledge.setAspectPool(parent.getTag(), 0);
        knowledge.setAspectPool("machina", 0);
        var item = ThaumcraftContent.THAUMOMETER.get();
        item.use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        var session = thaumometer.get(ThaumcraftContent.SCAN_SESSION.get());
        test.assertTrue(session != null && session.signature().equals("item:minecraft:redstone:0")
                        && !knowledge.hasScannedItem("@minecraft:redstone"),
                "placed wire starts its picked item-ID/metadata session without awarding its persistent item marker early");
        test.runAfterDelay(21, () -> {
            item.onUseTick(test.getLevel(), player, thaumometer, 4);
            test.assertValueEqual(knowledge.getAspectPoolFor("potentia"), 2, "placed wire uses original redstone potentia2");
            test.assertValueEqual(knowledge.getAspectPoolFor("machina"), 1, "placed wire also uses original redstone machina1");
            test.assertTrue(knowledge.hasScannedItem("@minecraft:redstone")
                            && !knowledge.hasScannedItem("@minecraft:redstone_wire"),
                    "placed wire records the item identity");
            test.assertBlockPresent(net.minecraft.world.level.block.Blocks.REDSTONE_WIRE, relative);

            ItemEntity dropped = new ItemEntity(test.getLevel(), pos.getX() + 0.5, pos.getY() + 0.35,
                    pos.getZ() - 0.75, new ItemStack(Items.REDSTONE));
            dropped.setNoGravity(true);
            dropped.setDeltaMovement(Vec3.ZERO);
            dropped.setPickUpDelay(100);
            test.getLevel().addFreshEntity(dropped);
            player.lookAt(EntityAnchorArgument.Anchor.EYES, dropped.position().add(0, dropped.getBbHeight() / 2.0, 0));
            item.use(test.getLevel(), player, InteractionHand.MAIN_HAND);
            var droppedSession = thaumometer.get(ThaumcraftContent.SCAN_SESSION.get());
            test.assertTrue(droppedSession != null && droppedSession.signature().equals("entity:" + dropped.getId())
                            && !droppedSession.signature().equals(session.signature()) && knowledge.hasScannedItem("@minecraft:redstone"),
                    "dropped redstone retains its type2 entity-instance use session while sharing the placed wire's persistent item marker");
            test.runAfterDelay(21, () -> {
                item.onUseTick(test.getLevel(), player, thaumometer, 4);
                test.assertValueEqual(knowledge.getAspectPoolFor("potentia"), 2, "dropped duplicate cannot award again");
                test.assertValueEqual(knowledge.getAspectPoolFor("machina"), 1, "duplicate also preserves the original machine pool");
                test.assertValueEqual(knowledge.getScannedItems().size(), 1, "one shared item scan marker");
                test.assertValueEqual(dropped.getItem().getCount(), 1, "scan does not consume the dropped item");
                test.assertTrue(knowledge.getScannedPhenomena().isEmpty() && knowledge.getScannedEntities().isEmpty(),
                        "wire and dropped item do not create node or entity markers");
                test.succeed();
            });
        });
    }

    private static void assertScannerBlockTarget(GameTestHelper test, ServerPlayer player, BlockPos expected) {
        var ray = player.pick(10.0D, 1.0F, true);
        test.assertTrue(ray.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                        && ray instanceof net.minecraft.world.phys.BlockHitResult block
                        && block.getBlockPos().equals(expected),
                "scanner fixture ray reaches " + expected + "; got " + ray.getType()
                        + " at " + (ray instanceof net.minecraft.world.phys.BlockHitResult block ? block.getBlockPos() : ray.getLocation())
                        + " from eye " + player.getEyePosition() + " looking " + player.getLookAngle());
    }
}
