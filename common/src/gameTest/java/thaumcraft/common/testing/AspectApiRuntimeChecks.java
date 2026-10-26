package thaumcraft.common.testing;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftAlembics;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftDeconstruction;
import thaumcraft.common.config.ThaumcraftEssences;
import thaumcraft.common.config.ThaumcraftMonsters;
import thaumcraft.common.config.ThaumcraftThaumatorium;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.container.ContainerDeconstructionTable;
import thaumcraft.common.container.ContainerResearchTable;
import thaumcraft.common.container.ContainerThaumatorium;
import thaumcraft.common.crafting.CrucibleRecipeDisplay;
import thaumcraft.common.crafting.ProgrammaticRecipes;
import thaumcraft.common.entities.monster.EntityWisp;
import thaumcraft.common.items.ItemCrystalEssence;
import thaumcraft.common.items.ItemEssence;
import thaumcraft.common.items.ItemJarLabel;
import thaumcraft.common.items.ItemManaBean;
import thaumcraft.common.items.ItemResearchNotes;
import thaumcraft.common.items.ItemWispEssence;
import thaumcraft.common.items.relics.ItemThaumometer;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.essentia.EssentiaStorageEndpoint;
import thaumcraft.common.lib.network.AspectDiscoveryPayload;
import thaumcraft.common.lib.network.AspectPoolPayload;
import thaumcraft.common.lib.network.KnowledgeSyncPayload;
import thaumcraft.common.lib.network.ResearchTableCombineNetworking;
import thaumcraft.common.lib.network.ResearchTableCombinePayload;
import thaumcraft.common.lib.network.ResearchTableStatePayload;
import thaumcraft.common.lib.research.EntityAspectBindings;
import thaumcraft.common.lib.research.ObjectAspectCatalog;
import thaumcraft.common.lib.research.ObjectAspectResolver;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.research.ResearchNoteData;
import thaumcraft.common.lib.research.ResearchNotePuzzle;
import thaumcraft.common.lib.research.ScanManager;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileCentrifuge;
import thaumcraft.common.tiles.TileDeconstructionTable;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TileResearchTable;
import thaumcraft.common.tiles.TileThaumatorium;

/**
 * An aspect registered by an add-on works end to end on the real server, like the 48 original ones.
 *
 * <p>The add-on aspect is a stand-in for Magic Bees' tempus (vacuos + ordo; no original aspect uses
 * that pair). It is registered here, on the GameTest side only, under a unique tag, and removed in
 * every exit path, so the 48-aspect assertions of the other scenarios keep holding. The scenarios
 * run in their own batches; every one except the thaumometer scan finishes inside one server tick.</p>
 */
public final class AspectApiRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private AspectApiRuntimeChecks() {}

    /** The add-on call of 1.7.10, with an own texture namespace. */
    static Aspect registerTestAspect(String purpose) {
        String tag = "gametest_" + purpose + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return new Aspect(tag, 0xB68CFF, new Aspect[]{Aspect.VOID, Aspect.ORDER},
                ResourceLocation.fromNamespaceAndPath("thaumcraft_gametest", "textures/aspects/tempus.png"), 1);
    }

    static void unregister(Aspect aspect) {
        if (aspect == null) return;
        synchronized (Aspect.aspects) { Aspect.aspects.remove(aspect.getTag(), aspect); }
    }

    /** Object tag registration, the thaumometer target, a refused scan without the parent and a real twenty-tick scan. */
    public static void objectTagScanDiscoversIt(GameTestHelper test, ServerPlayer player) {
        ServerLevel level = test.getLevel();
        var saved = new SavedPlayer(player);
        var oldTags = ThaumcraftApi.objectTags;
        var oldGroups = ThaumcraftApi.groupedObjectTags;
        Aspect tempus = registerTestAspect("scan");
        ItemEntity[] drop = {null};
        boolean[] closed = {false};
        Runnable cleanup = () -> {
            if (closed[0]) return;
            closed[0] = true;
            try {
                if (drop[0] != null) drop[0].discard();
                ThaumcraftApi.objectTags = oldTags;
                ThaumcraftApi.groupedObjectTags = oldGroups;
                saved.restore();
            } finally {
                unregister(tempus);
            }
        };
        try {
            ObjectAspectCatalog.initializePublicTags();
            ThaumcraftApi.objectTags = new ConcurrentHashMap<>(oldTags);
            ThaumcraftApi.groupedObjectTags = new ConcurrentHashMap<>(oldGroups);
            ThaumcraftApi.registerObjectTag(new ItemStack(Items.CLOCK), new AspectList().add(tempus, 4));
            AspectList tags = ObjectAspectResolver.scanTags(new ItemStack(Items.CLOCK), level);
            test.assertTrue(tags.size() == 1 && tags.getAmount(tempus) == 4, "the public object tag of the add-on aspect replaces the clock baseline: " + tags.aspects);

            var knowledge = KnowledgeAccess.get(player);
            knowledge.reset();
            knowledge.setInitializedAspects(true);
            test.assertTrue(!ScanManager.completeScan(player, "@minecraft:clock", tags, ScanManager.ScanKind.ITEM)
                            && !knowledge.hasDiscoveredAspect(tempus.getTag()) && !knowledge.hasScannedItem("@minecraft:clock"),
                    "without vacuos the production scan completion refuses the add-on compound as a missing parent, like an original compound");
            knowledge.addDiscoveredAspect(Aspect.VOID.getTag());

            player.stopUsingItem();
            player.setGameMode(GameType.SURVIVAL);
            player.getInventory().clearContent();
            player.setPos(Vec3.atBottomCenterOf(test.absolutePos(new BlockPos(2, 2, 1))));
            ItemStack tool = new ItemStack(ThaumcraftContent.THAUMOMETER.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, tool);
            Vec3 eye = player.getEyePosition();
            drop[0] = new ItemEntity(level, eye.x, eye.y - .125, eye.z + 3, new ItemStack(Items.CLOCK), 0, 0, 0);
            drop[0].setNoGravity(true);
            drop[0].setNeverPickUp();
            test.assertTrue(level.addFreshEntity(drop[0]), "a real clock item entity enters the world");
            aim(player, drop[0].getBoundingBox().getCenter());
            var readout = ItemThaumometer.readout(player, tool);
            test.assertTrue(readout != null && readout.aspects().size() == 1 && readout.aspects().getAmount(tempus) == 4,
                    "the thaumometer target of the clock carries the add-on aspect");
            test.assertTrue(player.gameMode.useItem(player, level, tool, InteractionHand.MAIN_HAND).consumesAction()
                    && tool.has(ThaumcraftContent.SCAN_SESSION.get()), "a real survival use of the thaumometer starts the scan of the clock");
            long started = level.getGameTime();
            test.onEachTick(() -> {
                if (closed[0]) return;
                try {
                    if (player.isUsingItem()) tool.getItem().onUseTick(level, player, tool, player.getUseItemRemainingTicks());
                    if (!knowledge.hasScannedItem("@minecraft:clock")) return;
                    test.assertTrue(level.getGameTime() - started >= ItemThaumometer.SCAN_TICKS, "the scan completes after its twenty real ticks, not earlier");
                    test.assertTrue(knowledge.hasDiscoveredAspect(tempus.getTag()) && knowledge.getAspectPoolFor(tempus.getTag()) == 6
                                    && !tool.has(ThaumcraftContent.SCAN_SESSION.get()),
                            "the real scan discovers the add-on aspect with amount4 plus the first-discovery bonus2");
                    cleanup.run();
                    test.succeed();
                } catch (RuntimeException | Error error) {
                    cleanup.run();
                    throw error;
                }
            });
            test.runAfterDelay(80, () -> {
                if (!closed[0]) {
                    cleanup.run();
                    test.fail("the add-on aspect scan did not complete within 80 real ticks");
                }
            });
        } catch (RuntimeException | Error error) {
            cleanup.run();
            throw error;
        }
    }

    /** A bound label, alembic -> tube -> jar transport, phial fill and pour, block entity reload and the client update tag. */
    public static void essentiaFillsAJarAndSurvivesReload(GameTestHelper test, ServerPlayer player) {
        ServerLevel level = test.getLevel();
        BlockPos jarPos = test.absolutePos(new BlockPos(3, 2, 3)), tubePos = jarPos.above(), sourcePos = tubePos.east();
        var region = new SavedRegion(level, jarPos.offset(-1, -1, -1), jarPos.offset(2, 2, 1));
        var saved = new SavedPlayer(player);
        Aspect tempus = registerTestAspect("jar");
        try {
            region.clear();
            player.stopUsingItem();
            player.getInventory().clearContent();
            player.setGameMode(GameType.SURVIVAL);
            player.setShiftKeyDown(false);
            player.setPos(Vec3.atCenterOf(jarPos).add(0, 0, 2));
            level.setBlock(jarPos, ThaumcraftContent.JAR.get().defaultBlockState(), QUIET);
            level.setBlock(tubePos, ThaumcraftTubes.ESSENTIA_TUBE.get().defaultBlockState(), QUIET);
            level.setBlock(sourcePos, ThaumcraftAlembics.ALEMBIC.get().defaultBlockState(), QUIET);
            var jar = (TileJar) level.getBlockEntity(jarPos);
            var source = (TileAlembic) level.getBlockEntity(sourcePos);

            var bind = level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "jar_label_bind")).orElseThrow();
            var recipe = (CraftingRecipe) bind.value();
            var input = CraftingInput.of(2, 1, List.of(ItemJarLabel.blank(), ItemEssence.filled(tempus.getTag())));
            test.assertTrue(recipe.matches(input, level), "the production label recipe accepts a blank label and an add-on phial");
            ItemStack label = recipe.assemble(input, level.registryAccess());
            test.assertTrue(ItemJarLabel.aspect(label) == tempus, "the bound label names the add-on aspect");
            player.setItemInHand(InteractionHand.MAIN_HAND, label);
            use(player, jarPos, Direction.NORTH);
            test.assertTrue(jar.getFilter() == tempus && jar.getSuctionType(null) == tempus && jar.getSuctionAmount(null) == 64,
                    "a real click labels the empty jar with the add-on aspect and gives it the original typed suction64");

            source.setFilter(tempus);
            test.assertTrue(source.addToContainer(tempus, 32) == 0, "the alembic holds exactly thirty-two add-on units");
            ticks(test, level, jarPos, tubePos, sourcePos, tempus, 160, 32);
            test.assertTrue(jar.getAmount() == 32 && jar.getAspect() == tempus && source.getAmount() == 0,
                    "the real tube and jar tickers move every add-on unit into the labelled jar");

            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftContent.EMPTY_PHIAL.get()));
            use(player, jarPos, Direction.UP);
            ItemStack filled = find(player, ThaumcraftContent.FILLED_PHIAL.get().asItem());
            test.assertTrue(jar.getAmount() == 24 && !filled.isEmpty() && ItemEssence.data(filled) != null
                    && ItemEssence.data(filled).tag().equals(tempus.getTag()), "an empty phial takes eight add-on units from the jar");
            var registries = level.registryAccess();
            test.assertTrue(ItemStack.matches(filled, ItemStack.parse(registries, filled.save(registries)).orElseThrow())
                    && ItemStack.matches(filled, roundTrip(ItemStack.STREAM_CODEC, filled, registries)), "the add-on phial survives disk and network codecs");
            player.getInventory().clearContent();
            player.setItemInHand(InteractionHand.MAIN_HAND, filled.copyWithCount(1));
            use(player, jarPos, Direction.UP);
            test.assertTrue(jar.getAmount() == 32 && player.getMainHandItem().is(ThaumcraftContent.EMPTY_PHIAL.get()), "the phial pours the eight units back");

            for (BlockPos pos : new BlockPos[]{jarPos, tubePos, sourcePos}) reload(level, pos);
            jar = (TileJar) level.getBlockEntity(jarPos);
            source = (TileAlembic) level.getBlockEntity(sourcePos);
            test.assertTrue(jar.getAmount() == 32 && jar.getAspect() == tempus && jar.getFilter() == tempus && source.getFilter() == tempus,
                    "the saved block entities restore the add-on contents, label and alembic filter");
            var update = jar.getUpdateTag(registries);
            BlockEntity client = ((EntityBlock) jar.getBlockState().getBlock()).newBlockEntity(jarPos, jar.getBlockState());
            client.loadWithComponents(update, registries);
            test.assertTrue(client instanceof TileJar view && view.getAspect() == tempus && view.getAmount() == 32 && view.getFilter() == tempus,
                    "the client update tag of the jar names the add-on aspect");
        } finally {
            try {
                region.restore();
                saved.restore();
            } finally {
                unregister(tempus);
            }
        }
        test.succeed();
    }

    /** The real research table menu and receiver combine the two parents, the shared bonus persists and the hex notes link it. */
    public static void combinesAtTheResearchTable(GameTestHelper test, ServerPlayer player) {
        ServerLevel level = test.getLevel();
        BlockPos pos = test.absolutePos(new BlockPos(3, 2, 3));
        var region = new SavedRegion(level, pos.north(), pos);
        var saved = new SavedPlayer(player);
        Aspect tempus = registerTestAspect("table");
        try {
            test.assertTrue(Aspect.getCombinationResult(Aspect.VOID, Aspect.ORDER) == tempus, "no original aspect owns vacuos+ordo, so the pair names the add-on aspect");
            region.clear();
            player.closeContainer();
            player.getInventory().clearContent();
            player.setShiftKeyDown(false);
            player.setPos(Vec3.atCenterOf(pos));
            var knowledge = KnowledgeAccess.get(player);
            knowledge.reset();
            knowledge.setInitializedAspects(true);
            knowledge.setAspectPool(Aspect.VOID.getTag(), 2);
            knowledge.setAspectPool(Aspect.ORDER.getTag(), 2);
            level.setBlock(pos, ThaumcraftContent.TABLE.get().state(2), QUIET);
            level.setBlock(pos.north(), ThaumcraftContent.TABLE.get().state(7), QUIET);
            var table = (TileResearchTable) level.getBlockEntity(pos);
            player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            test.assertTrue(player.containerMenu instanceof ContainerResearchTable, "the real table opens its combine menu");
            var menu = (ContainerResearchTable) player.containerMenu;
            test.assertTrue(ResearchTableCombineNetworking.handle(player, intent(menu, "vacuos", "ordo")), "the production receiver accepts the funded pair");
            test.assertTrue(knowledge.hasDiscoveredAspect(tempus.getTag()) && knowledge.getAspectPoolFor(tempus.getTag()) == 3
                            && knowledge.getAspectPoolFor("vacuos") == 1 && knowledge.getAspectPoolFor("ordo") == 1,
                    "the first combination discovers the add-on compound with 1+2 RP and pays one of each parent");
            test.assertTrue(ResearchTableCombineNetworking.handle(player, intent(menu, "ordo", "vacuos"))
                    && knowledge.getAspectPoolFor(tempus.getTag()) == 4, "the reversed known pair gives one more RP");

            table.setBonusPools(Map.of(tempus.getTag(), 2));
            var restored = new TileResearchTable(pos, table.getBlockState());
            restored.loadWithComponents(table.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
            test.assertTrue(restored.bonusFor(tempus.getTag()) == 2, "a shared table bonus of the add-on aspect persists");
            var state = roundTrip(ResearchTableStatePayload.STREAM_CODEC,
                    new ResearchTableStatePayload(menu.containerId, menu.sessionToken(), table.tableRevision(), table.bonusPools()), level.registryAccess());
            test.assertTrue(state.bonusPools().equals(Map.of(tempus.getTag(), 2)), "the table state payload shows the add-on bonus to the client");

            var note = new ResearchNoteData("NITOR", 0x123456, false, 0,
                    List.of(new ResearchNoteData.Cell(0, 0, 1, "vacuos"), new ResearchNoteData.Cell(1, 0, 2, tempus.getTag())));
            ItemStack notes = ItemResearchNotes.create(note, false);
            test.assertTrue(ItemStack.matches(notes, roundTrip(ItemStack.STREAM_CODEC, notes, level.registryAccess()))
                            && ResearchNotePuzzle.isDirectlyConnected(tempus.getTag(), "vacuos") && !ResearchNotePuzzle.isDirectlyConnected(tempus.getTag(), "aer"),
                    "a research note cell may hold the add-on aspect, and the hex rule links it to its parents only");
        } finally {
            try {
                player.closeContainer();
                region.restore();
                saved.restore();
            } finally {
                unregister(tempus);
            }
        }
        test.succeed();
    }

    /** The knowledge snapshot, the award notifications and every item component that names the aspect, through the network codecs. */
    public static void roundTripsThroughClientCodecs(GameTestHelper test, ServerPlayer player) {
        RegistryAccess registries = test.getLevel().registryAccess();
        var saved = new SavedPlayer(player);
        Aspect tempus = registerTestAspect("codec");
        try {
            var knowledge = KnowledgeAccess.get(player);
            knowledge.reset();
            knowledge.setInitializedAspects(true);
            knowledge.addDiscoveredAspect(Aspect.VOID.getTag());
            knowledge.setAspectPool(tempus.getTag(), 5);
            var sync = roundTrip(KnowledgeSyncPayload.STREAM_CODEC, new KnowledgeSyncPayload(knowledge.save()), registries);
            var client = new PlayerKnowledge();
            client.load(sync.state());
            test.assertTrue(client.hasDiscoveredAspect(tempus.getTag()) && client.getAspectPoolFor(tempus.getTag()) == 5,
                    "the full knowledge snapshot, as the client applies it, carries the add-on discovery and pool");
            test.assertTrue(roundTrip(AspectDiscoveryPayload.STREAM_CODEC, new AspectDiscoveryPayload(tempus.getTag()), registries).key().equals(tempus.getTag())
                            && roundTrip(AspectPoolPayload.STREAM_CODEC, new AspectPoolPayload(tempus.getTag(), (short) 3, (short) 5), registries).key().equals(tempus.getTag()),
                    "the discovery and research point notifications name the add-on aspect");

            ItemStack wisp = new ItemStack(ThaumcraftEssences.WISP_ESSENCE.get());
            ThaumcraftEssences.WISP_ESSENCE.get().setAspects(wisp, new AspectList().add(tempus, 2));
            ItemStack phial = ItemEssence.filled(tempus.getTag());
            List<ItemStack> stacks = List.of(phial, ItemCrystalEssence.create(tempus), ItemManaBean.create(tempus), wisp, ItemJarLabel.bound(tempus.getTag()));
            for (ItemStack stack : stacks) {
                test.assertTrue(ItemStack.matches(stack, roundTrip(ItemStack.STREAM_CODEC, stack, registries))
                                && ItemStack.matches(stack, ItemStack.parse(registries, stack.save(registries)).orElseThrow()),
                        "the add-on component survives the network and disk codecs: " + stack);
            }
            test.assertTrue(ItemCrystalEssence.color(stacks.get(1)) == (0xff000000 | tempus.getColor()) && ItemWispEssence.color(wisp) == (0xff000000 | tempus.getColor()),
                    "crystal and wisp essence of the add-on aspect take its colour");

            var wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
            try {
                ItemStack.STREAM_CODEC.encode(wire, phial);
                unregister(tempus);
                boolean refused = false;
                try { ItemStack.STREAM_CODEC.decode(wire); } catch (RuntimeException expected) { refused = true; }
                test.assertTrue(refused, "a side that never registered the aspect refuses the phial: both sides must register it (Aspect networking contract)");
            } finally {
                wire.release();
            }
        } finally {
            try {
                saved.restore();
            } finally {
                unregister(tempus);
            }
        }
        test.succeed();
    }

    /** The thaumatorium stock and a deconstruction reward of an add-on primal through the real menu data synchroniser. */
    public static void menusSynchroniseIt(GameTestHelper test, ServerPlayer player) {
        ServerLevel level = test.getLevel();
        RegistryAccess registries = level.registryAccess();
        BlockPos machinePos = test.absolutePos(new BlockPos(2, 2, 2)), tablePos = machinePos.east(3);
        var region = new SavedRegion(level, machinePos, tablePos);
        var saved = new SavedPlayer(player);
        Aspect tempus = registerTestAspect("menu");
        Aspect primal = null;
        try {
            primal = new Aspect("gametest_primal_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12), 0x445566, "5", 1);
            region.clear();
            player.closeContainer();
            level.setBlock(machinePos, ThaumcraftThaumatorium.THAUMATORIUM.get().defaultBlockState(), QUIET);
            var machine = (TileThaumatorium) level.getBlockEntity(machinePos);
            CompoundTag stock = machine.saveWithFullMetadata(registries);
            ListTag rows = new ListTag();
            CompoundTag row = new CompoundTag();
            row.putString("key", tempus.getTag());
            row.putInt("amount", 5);
            rows.add(row);
            stock.put("Essentia", rows);
            machine.loadWithComponents(stock, registries);
            test.assertTrue(!machine.isQuarantined() && machine.containerContains(tempus) == 5, "the thaumatorium loads a stored add-on stock");
            var recipes = ProgrammaticRecipes.crucibles(level.getRecipeManager());
            var server = new ContainerThaumatorium(31, player.getInventory(), machine, recipes);
            var opening = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
            ContainerThaumatorium client;
            try {
                opening.writeBlockPos(machinePos);
                CrucibleRecipeDisplay.write(opening, registries, recipes);
                client = new ContainerThaumatorium(31, player.getInventory(), opening);
            } finally {
                opening.release();
            }
            int[] data = initialData(server);
            for (int index = 0; index < data.length; index++) client.setData(index, data[index]);
            test.assertTrue(server.stored(tempus) == 5 && client.stored(tempus) == 5,
                    "the thaumatorium menu opened after the add-on registration synchronises its add-on stock");

            level.setBlock(tablePos, ThaumcraftDeconstruction.TABLE.get().defaultBlockState(), QUIET);
            var table = (TileDeconstructionTable) level.getBlockEntity(tablePos);
            for (Aspect reward : new Aspect[]{Aspect.FIRE, primal}) {
                CompoundTag tag = table.saveWithFullMetadata(registries);
                tag.putString("Aspect", reward.getTag());
                table.loadWithComponents(tag, registries);
                test.assertTrue(table.reward() == reward, "the deconstruction table loads the primal reward " + reward.getTag());
                var serverMenu = new ContainerDeconstructionTable(32, player.getInventory(), table);
                var buffer = new FriendlyByteBuf(Unpooled.buffer());
                ContainerDeconstructionTable clientMenu;
                try {
                    buffer.writeBlockPos(tablePos);
                    clientMenu = new ContainerDeconstructionTable(32, player.getInventory(), buffer);
                } finally {
                    buffer.release();
                }
                int[] slots = initialData(serverMenu);
                for (int index = 0; index < slots.length; index++) clientMenu.setData(index, slots[index]);
                test.assertTrue(serverMenu.reward() == reward && clientMenu.reward() == reward,
                        "the reward slot shows the original and the add-on primal reward on both sides: " + reward.getTag());
            }
        } finally {
            try {
                player.closeContainer();
                region.restore();
                saved.restore();
            } finally {
                unregister(tempus);
                unregister(primal);
            }
        }
        test.succeed();
    }

    /** A Wisp of the add-on aspect is scannable (late Wisp rows), and the centrifuge splits the add-on compound into a parent. */
    public static void wispScansAndCentrifugeSplitsIt(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        BlockPos pos = test.absolutePos(new BlockPos(3, 2, 3));
        var region = new SavedRegion(level, pos.offset(-1, -1, -1), pos.offset(1, 1, 1));
        var previous = ThaumcraftApi.scanEntities;
        Aspect tempus = registerTestAspect("wisp");
        EntityWisp wisp = null;
        try {
            ThaumcraftApi.scanEntities = new ArrayList<>(previous);
            wisp = ThaumcraftMonsters.WISP.get().create(level);
            test.assertTrue(wisp != null, "the real Wisp entity type creates a Wisp");
            wisp.setPos(Vec3.atCenterOf(pos));
            wisp.setAspectTag(tempus.getTag());
            var target = EntityAspectBindings.describe(wisp);
            Map<Aspect, Integer> expected = new LinkedHashMap<>();
            expected.put(tempus, 2); expected.put(Aspect.MAGIC, 1); expected.put(Aspect.AIR, 1);
            test.assertTrue(target != null && target.aspects().aspects.equals(expected)
                            && target.signature().equals("@entity:Thaumcraft.WispType" + tempus.getTag()),
                    "original ConfigAspects:98-99 - a Wisp of the add-on aspect scans as aspect2 + praecantatio1 + aer1: " + (target == null ? null : target.aspects().aspects));

            region.clear();
            level.setBlock(pos, ThaumcraftTubes.CENTRIFUGE.get().defaultBlockState(), QUIET);
            var centrifuge = (TileCentrifuge) level.getBlockEntity(pos);
            test.assertTrue(centrifuge.addEssentia(tempus, 1, Direction.DOWN) == 1 && centrifuge.getInputAspect() == tempus,
                    "the centrifuge accepts one unit of the add-on compound");
            reload(level, pos);
            centrifuge = (TileCentrifuge) level.getBlockEntity(pos);
            test.assertTrue(centrifuge.getInputAspect() == tempus, "the pending add-on input survives the block entity reload");
            for (int tick = 0; tick < 120 && centrifuge.getOutputAspect() == null; tick++) tick(level, pos, ThaumcraftTubes.CENTRIFUGE_BLOCK_ENTITY.get());
            test.assertTrue(centrifuge.getInputAspect() == null && (centrifuge.getOutputAspect() == Aspect.VOID || centrifuge.getOutputAspect() == Aspect.ORDER),
                    "the real centrifuge ticker splits the add-on compound into one of its two parents: " + centrifuge.getOutputAspect());
        } finally {
            try {
                if (wisp != null) wisp.discard();
                ThaumcraftApi.scanEntities = previous;
                region.restore();
            } finally {
                unregister(tempus);
            }
        }
        test.succeed();
    }

    /**
     * The creative tab, built after the add-on registered, lists its phial once and keeps the 48 original
     * phials in their order. The late phial is published, so it sits where the loader puts published
     * stacks (Fabric: before the appended ones), not necessarily next to the original phials.
     */
    public static void creativeTabListsItsPhial(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        var parameters = new CreativeModeTab.ItemDisplayParameters(level.enabledFeatures(), true, level.registryAccess());
        CreativeModeTab tab = ThaumcraftContent.TAB.get();
        Aspect tempus = registerTestAspect("tab");
        try {
            tab.buildContents(parameters);
            List<String> phials = new ArrayList<>();
            for (ItemStack stack : tab.getDisplayItems()) if (stack.is(ThaumcraftContent.FILLED_PHIAL.get())) phials.add(ItemEssence.data(stack).tag());
            List<String> original = new ArrayList<>(Aspect.aspects.keySet()).subList(0, 48);
            test.assertTrue(phials.size() == 49 && phials.stream().filter(original::contains).toList().equals(original)
                            && phials.stream().filter(tempus.getTag()::equals).count() == 1,
                    "original ItemEssence.getSubItems lists the live registry: the 48 original phials in order and the add-on phial once: " + phials);
        } finally {
            unregister(tempus);
            // Leave no stack of the removed aspect in the built tab.
            tab.buildContents(parameters);
        }
        test.succeed();
    }

    private static ResearchTableCombinePayload intent(ContainerResearchTable menu, String left, String right) {
        return new ResearchTableCombinePayload(menu.containerId, menu.sessionToken(), menu.tableRevision(), left, right);
    }

    private static <T> T roundTrip(StreamCodec<RegistryFriendlyByteBuf, T> codec, T value, RegistryAccess registries) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        try {
            codec.encode(buffer, value);
            T decoded = codec.decode(buffer);
            if (buffer.isReadable()) throw new AssertionError("trailing bytes after " + value);
            return decoded;
        } finally {
            buffer.release();
        }
    }

    /** The initial data slot values the real synchroniser sends when a menu opens. */
    private static int[] initialData(AbstractContainerMenu menu) {
        int[][] captured = {null};
        menu.setSynchronizer(new ContainerSynchronizer() {
            @Override public void sendInitialData(AbstractContainerMenu container, NonNullList<ItemStack> items, ItemStack carried, int[] data) { captured[0] = data.clone(); }
            @Override public void sendSlotChange(AbstractContainerMenu container, int slot, ItemStack stack) {}
            @Override public void sendCarriedChange(AbstractContainerMenu container, ItemStack stack) {}
            @Override public void sendDataChange(AbstractContainerMenu container, int id, int value) {}
        });
        if (captured[0] == null) throw new AssertionError("the menu sent no initial data");
        return captured[0];
    }

    private static void aim(ServerPlayer player, Vec3 at) {
        var delta = at.subtract(player.getEyePosition());
        player.setYRot((float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90));
        player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z))));
    }

    private static void use(ServerPlayer player, BlockPos pos, Direction face) {
        player.gameMode.useItemOn(player, player.serverLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos).relative(face, .5), face, pos, false));
    }

    private static ItemStack find(ServerPlayer player, net.minecraft.world.item.Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(item)) return player.getInventory().getItem(slot);
        }
        return ItemStack.EMPTY;
    }

    private static void ticks(GameTestHelper test, ServerLevel level, BlockPos jar, BlockPos tube, BlockPos source, Aspect aspect, int count, int total) {
        for (int tick = 0; tick < count; tick++) {
            tick(level, tube, ThaumcraftTubes.TUBE_BLOCK_ENTITY.get());
            tick(level, jar, ThaumcraftContent.JAR_BLOCK_ENTITY.get());
            int actual = 0;
            for (BlockPos pos : new BlockPos[]{jar, tube, source}) {
                var endpoint = (EssentiaStorageEndpoint) level.getBlockEntity(pos);
                test.assertTrue(endpoint.getStoredEssentiaAmount() == 0 || endpoint.getStoredEssentiaType() == aspect, "every endpoint holds only the add-on aspect");
                actual += endpoint.getStoredEssentiaAmount();
            }
            test.assertTrue(actual == total, "exact per-tick conservation of the add-on essentia: expected " + total + ", got " + actual);
        }
    }

    private static <T extends BlockEntity> void tick(ServerLevel level, BlockPos pos, BlockEntityType<T> type) {
        BlockState state = level.getBlockState(pos);
        var ticker = ((EntityBlock) state.getBlock()).getTicker(level, state, type);
        if (ticker == null) throw new IllegalStateException("Missing native server ticker at " + pos);
        ticker.tick(level, pos, state, type.getBlockEntity(level, pos));
    }

    private static void reload(ServerLevel level, BlockPos pos) {
        BlockEntity before = level.getBlockEntity(pos);
        var saved = before.saveWithFullMetadata(level.registryAccess());
        BlockState state = level.getBlockState(pos);
        level.removeBlockEntity(pos);
        BlockEntity restored = ((EntityBlock) state.getBlock()).newBlockEntity(pos, state);
        if (restored == null || restored.getType() != before.getType()) throw new IllegalStateException("Wrong registered block entity factory at " + pos);
        restored.loadWithComponents(saved, level.registryAccess());
        level.setBlockEntity(restored);
    }

    /** The blocks and block entities of a box, restored exactly after the scenario. */
    private static final class SavedRegion {
        private final ServerLevel level;
        private final Map<BlockPos, BlockState> states = new LinkedHashMap<>();
        private final Map<BlockPos, BlockEntity> originals = new LinkedHashMap<>();

        SavedRegion(ServerLevel level, BlockPos from, BlockPos to) {
            this.level = level;
            for (BlockPos mutable : BlockPos.betweenClosed(from, to)) {
                BlockPos pos = mutable.immutable();
                states.put(pos, level.getBlockState(pos));
                if (level.getBlockEntity(pos) != null) originals.put(pos, level.getBlockEntity(pos));
            }
        }

        void clear() {
            for (BlockPos pos : originals.keySet()) level.removeBlockEntity(pos);
            for (BlockPos pos : states.keySet()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), QUIET);
        }

        void restore() {
            for (BlockPos pos : states.keySet()) level.removeBlockEntity(pos);
            for (var entry : states.entrySet()) level.setBlock(entry.getKey(), entry.getValue(), QUIET);
            for (BlockEntity original : originals.values()) {
                original.clearRemoved();
                level.setBlockEntity(original);
            }
        }
    }

    /** The inventory, position, mode and knowledge of the shared mock player. */
    private static final class SavedPlayer {
        private final ServerPlayer player;
        private final List<ItemStack> inventory = new ArrayList<>();
        private final Vec3 position;
        private final float yaw, pitch;
        private final GameType mode;
        private final int selected;
        private final boolean sneaking;
        private final PlayerKnowledge knowledge;

        SavedPlayer(ServerPlayer player) {
            this.player = player;
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) inventory.add(player.getInventory().getItem(slot).copy());
            position = player.position();
            yaw = player.getYRot();
            pitch = player.getXRot();
            mode = player.gameMode.getGameModeForPlayer();
            selected = player.getInventory().selected;
            sneaking = player.isShiftKeyDown();
            knowledge = KnowledgeAccess.get(player).copy();
        }

        void restore() {
            player.stopUsingItem();
            player.getInventory().clearContent();
            for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().setItem(slot, inventory.get(slot));
            player.getInventory().selected = selected;
            player.setGameMode(mode);
            player.setShiftKeyDown(sneaking);
            player.setPos(position);
            player.setYRot(yaw);
            player.setXRot(pitch);
            KnowledgeAccess.get(player).copyFrom(knowledge);
        }
    }
}
