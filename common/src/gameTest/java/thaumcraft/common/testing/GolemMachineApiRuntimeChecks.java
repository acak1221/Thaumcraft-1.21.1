package thaumcraft.common.testing;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.golems.GolemApi;
import thaumcraft.api.golems.GolemMaterial;
import thaumcraft.api.golems.GolemView;
import thaumcraft.api.machines.IArcaneBore;
import thaumcraft.api.machines.IArcaneEar;
import thaumcraft.api.machines.IBrainJar;
import thaumcraft.api.machines.IDeconstructor;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.common.config.ThaumcraftArcaneBore;
import thaumcraft.common.config.ThaumcraftArcaneEar;
import thaumcraft.common.config.ThaumcraftDeconstruction;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemMarker;
import thaumcraft.common.items.golems.GolemUpgrade;
import thaumcraft.common.items.golems.GolemUseOptions;
import thaumcraft.common.items.wands.foci.FocusUpgradeData;
import thaumcraft.common.lib.research.DeconstructionRules;
import thaumcraft.common.lib.research.ObjectAspectResolver;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileArcaneBore;
import thaumcraft.common.tiles.TileDeconstructionTable;
import thaumcraft.common.tiles.TileJarBrain;
import thaumcraft.common.tiles.TileSensor;

/**
 * {@code thaumcraft.api.golems} and {@code thaumcraft.api.machines} against real golems and real block entities: every
 * answer of the API is compared with the entity's or block entity's own state, every write is checked to change that
 * state, to be in the data the server syncs (decoded through the vanilla packet codec into a fresh copy) and to come back
 * from a save, and every invalid input and foreign thread is refused without a change. Only the API is used for the
 * reads and writes; the internals appear only as the witness. NOT a client check: no client received the packets.
 */
public final class GolemMachineApiRuntimeChecks {
    private static final int TALLOW = 2, THAUMIUM = 7, STRAW = 0;
    private static final ResourceKey<Level> ELSEWHERE = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "api_test_elsewhere"));

    private GolemMachineApiRuntimeChecks() {}

    // ---- golems ----------------------------------------------------------------------------------------------------

    /** A golem with a core, accessories, an upgrade, a home and markers of three dimensions; a bare one; a pig; a removed golem. */
    public static void golemViewReadsTheRealGolem(GameTestHelper test) {
        var golems = new Golems(test);
        try {
            BlockPos at = test.absolutePos(new BlockPos(2, 2, 2));
            var markers = List.of(new GolemMarker(test.getLevel().dimension().location(), at.east(3), Direction.UP, 3),
                    new GolemMarker(Level.NETHER.location(), new BlockPos(-40, 70, 12), Direction.WEST),
                    new GolemMarker(ELSEWHERE.location(), new BlockPos(5, -60, 5), Direction.DOWN, 15));
            EntityGolemBase golem = golems.spawn(THAUMIUM, GolemData.FILL, List.of(GolemUpgrade.ORDER.id), "HGM", markers, at, Direction.SOUTH);
            golem.setHealth(13.5F);
            var view = GolemApi.view(golem).orElseThrow(() -> new AssertionError("a live golem has a view"));
            test.assertTrue(GolemApi.isGolem(golem) && view.id().equals(golem.getUUID()) && view.dimension().equals(test.getLevel().dimension()),
                    "the view names the golem and its dimension");
            test.assertTrue(view.materialId() == golem.getGolemMaterial() && view.material() == GolemMaterial.THAUMIUM
                            && "Thaumium".equals(view.material().displayName()), "material " + view.materialId() + " is the golem's own");
            test.assertTrue(view.core() == golem.getGolemCore() && view.core() == GolemData.FILL && view.hasCore(), "core " + view.core());
            test.assertTrue(view.decoration().equals(golem.getDecoration()) && view.decoration().equals("HGM"), "decoration " + view.decoration());
            test.assertTrue(view.health() == golem.getHealth() && view.health() == 13.5F, "health " + view.health());
            test.assertTrue(view.position().equals(golem.position()), "position " + view.position() + " vs " + golem.position());
            test.assertTrue(view.home().equals(golem.getHomePosition()) && view.home().equals(at) && view.homeFacing() == golem.getHomeFacing()
                    && view.homeFacing() == Direction.SOUTH, "home " + view.home() + " " + view.homeFacing());
            test.assertTrue(apiMarkers(golem.getMarkers()).equals(view.markers()) && view.markers().size() == 3, "markers " + view.markers());
            var overworld = view.markers().get(0);
            test.assertTrue(overworld.pos().equals(at.east(3)) && overworld.dimension().equals(test.getLevel().dimension())
                    && overworld.side() == Direction.UP && overworld.color() == 3, "an Order marker keeps its colour: " + overworld);
            test.assertTrue(view.markers().get(1).dimension().equals(Level.NETHER) && view.markers().get(1).color() == GolemView.Marker.ANY_COLOR
                    && view.markers().get(2).dimension().equals(ELSEWHERE) && view.markers().get(2).color() == 15,
                    "markers of other dimensions are read as they are kept");
            boolean frozen = false;
            try { view.markers().clear(); } catch (UnsupportedOperationException expected) { frozen = true; }
            test.assertTrue(frozen && golem.getMarkers().size() == 3, "the view's markers cannot be changed and do not alias the golem's");
            golem.setHealth(9F);
            test.assertTrue(view.health() == 13.5F && GolemApi.view(golem).orElseThrow().health() == 9F, "a view is a snapshot; a new one reads the new state");

            EntityGolemBase bare = golems.spawn(STRAW, GolemData.NO_CORE, List.of(), "", List.of(), at.west(), Direction.UP);
            var bareView = GolemApi.view(bare).orElseThrow();
            test.assertTrue(bareView.material() == GolemMaterial.STRAW && bareView.core() == GolemView.NO_CORE && !bareView.hasCore()
                    && bareView.decoration().isEmpty() && bareView.markers().isEmpty(), "a bare straw golem: no core, no accessory, no marker");

            var pig = EntityType.PIG.create(test.getLevel());
            pig.setPos(Vec3.atBottomCenterOf(at.north()));
            test.assertTrue(test.getLevel().addFreshEntity(pig), "a real pig is admitted");
            golems.spawned.add(pig);
            test.assertTrue(!GolemApi.isGolem(pig) && GolemApi.view(pig).isEmpty(), "a pig is no golem");

            bare.discard();
            test.assertTrue(GolemApi.isGolem(bare) && GolemApi.view(bare).isEmpty(), "a removed golem is still a golem but has no view");
        } finally {
            golems.close();
        }
        test.succeed();
    }

    /** setHome and setMarkers through the API: the golem's state, the sync data, the save, and the golem's own side effects. */
    public static void golemWritesChangeTheGolemSyncAndSurviveReload(GameTestHelper test) {
        var golems = new Golems(test);
        var level = test.getLevel();
        try {
            BlockPos at = test.absolutePos(new BlockPos(2, 2, 2));
            EntityGolemBase golem = golems.spawn(TALLOW, GolemData.FILL, List.of(GolemUpgrade.ORDER.id), "", List.of(), at, Direction.UP);
            BlockPos home = at.offset(1, 0, 2);
            golem.getEntityData().packDirty(); // What the admission left for the tracker; the next batch is the write's alone.
            test.assertTrue(GolemApi.setHome(golem, home, Direction.NORTH), "a plain golem takes a new home");
            test.assertTrue(golem.getHomePosition().equals(home) && golem.getHomeFacing() == Direction.NORTH
                    && golem.getRestrictCenter().equals(home) && golem.getRestrictRadius() == 32F,
                    "the golem's own setHome ran: home, side and restriction " + golem.getRestrictCenter() + "/" + golem.getRestrictRadius());
            var view = GolemApi.view(golem).orElseThrow();
            test.assertTrue(view.home().equals(home) && view.homeFacing() == Direction.NORTH, "the next view reads the new home");
            var dirty = golem.getEntityData().packDirty();
            test.assertTrue(dirty != null, "the new home is marked for the next entity data sync");
            var mirror = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), level);
            var wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
            try {
                ClientboundSetEntityDataPacket.STREAM_CODEC.encode(wire, new ClientboundSetEntityDataPacket(golem.getId(), dirty));
                mirror.getEntityData().assignValues(ClientboundSetEntityDataPacket.STREAM_CODEC.decode(wire).packedItems());
            } finally { wire.release(); }
            test.assertTrue(mirror.getHomePosition().equals(home) && mirror.getHomeFacing() == Direction.NORTH,
                    "the vanilla entity data packet carries the home to a fresh copy: " + mirror.getHomePosition() + " " + mirror.getHomeFacing());
            mirror.discard();

            var markers = List.of(new GolemView.Marker(at.east(2), level.dimension(), Direction.UP),
                    new GolemView.Marker(new BlockPos(100, 64, -100), Level.NETHER, Direction.EAST, 5),
                    new GolemView.Marker(new BlockPos(0, 0, 0), ELSEWHERE, Direction.SOUTH, GolemView.Marker.ANY_COLOR));
            test.assertTrue(GolemApi.setMarkers(golem, markers), "an Order golem takes coloured markers and markers of other dimensions");
            test.assertTrue(apiMarkers(golem.getMarkers()).equals(markers) && GolemApi.view(golem).orElseThrow().markers().equals(markers),
                    "the golem keeps exactly these markers, in this order: " + golem.getMarkers());
            var editable = new ArrayList<>(markers);
            test.assertTrue(GolemApi.setMarkers(golem, editable), "the same list again is accepted");
            editable.clear();
            test.assertTrue(golem.getMarkers().size() == 3, "the API copied the list, it does not keep the caller's");

            var tag = new CompoundTag();
            golem.saveWithoutId(tag);
            test.assertTrue(tag.getInt("HomeX") == home.getX() && tag.getInt("HomeY") == home.getY() && tag.getInt("HomeZ") == home.getZ()
                    && tag.getByte("HomeFacing") == Direction.NORTH.get3DDataValue() && tag.getList("Markers", 10).size() == 3,
                    "the save holds the home, the side and the three markers");
            var reloaded = golems.load(tag);
            var after = GolemApi.view(reloaded).orElseThrow();
            test.assertTrue(after.home().equals(home) && after.homeFacing() == Direction.NORTH && after.markers().equals(markers)
                    && after.id().equals(golem.getUUID()), "a reloaded golem reads the same home and markers: " + after.markers());

            var reordered = List.of(markers.get(2), markers.get(0));
            test.assertTrue(GolemApi.setMarkers(golem, reordered) && apiMarkers(golem.getMarkers()).equals(reordered),
                    "a new list replaces every marker, in its own order");
            test.assertTrue(GolemApi.setMarkers(golem, List.of()) && golem.getMarkers().isEmpty(), "an empty list removes every marker");
            var full = new ArrayList<GolemView.Marker>();
            for (int i = 0; i < GolemApi.MAX_MARKERS; i++) full.add(new GolemView.Marker(at.offset(i % 8, 0, i / 8), level.dimension(), Direction.UP));
            test.assertTrue(GolemApi.setMarkers(golem, full) && golem.getMarkers().size() == GolemApi.MAX_MARKERS, "sixty-four markers fit");
            BlockPos lastBlock = new BlockPos(at.getX(), level.getMaxBuildHeight() - 1, at.getZ());
            test.assertTrue(GolemApi.setHome(golem, lastBlock, Direction.DOWN) && golem.getHomePosition().equals(lastBlock),
                    "the top block of the world is a valid home");
        } finally {
            golems.close();
        }
        test.succeed();
    }

    /** Null, out-of-world, too many, a colour without Order, a pig, a removed or unsupported golem, a foreign thread. */
    public static void golemApiRefusesInvalidInputAndOtherThreads(GameTestHelper test) {
        var golems = new Golems(test);
        var level = test.getLevel();
        try {
            BlockPos at = test.absolutePos(new BlockPos(2, 2, 2));
            EntityGolemBase plain = golems.spawn(TALLOW, GolemData.FILL, List.of(), "", List.of(), at, Direction.UP);
            var start = List.of(new GolemView.Marker(at.east(), level.dimension(), Direction.UP));
            test.assertTrue(GolemApi.setMarkers(plain, start), "the fixture's marker is set");

            test.assertTrue(!GolemApi.setHome(plain, null, Direction.UP) && !GolemApi.setHome(plain, at.east(), null),
                    "a null home or side is refused");
            test.assertTrue(!GolemApi.setHome(plain, new BlockPos(at.getX(), level.getMaxBuildHeight(), at.getZ()), Direction.UP)
                            && !GolemApi.setHome(plain, new BlockPos(at.getX(), level.getMinBuildHeight() - 1, at.getZ()), Direction.UP)
                            && !GolemApi.setHome(plain, new BlockPos(40_000_000, 64, 0), Direction.UP), "a home outside the world is refused");
            test.assertTrue(plain.getHomePosition().equals(at) && plain.getHomeFacing() == Direction.UP, "every refused home left the home alone");

            test.assertTrue(!GolemApi.setMarkers(plain, null), "a null list is refused");
            test.assertTrue(!GolemApi.setMarkers(plain, Arrays.asList(start.getFirst(), null)), "a list with a null marker is refused");
            var tooMany = new ArrayList<>(Collections.nCopies(GolemApi.MAX_MARKERS + 1, start.getFirst()));
            test.assertTrue(!GolemApi.setMarkers(plain, tooMany), "sixty-five markers are refused");
            test.assertTrue(!GolemApi.setMarkers(plain, List.of(new GolemView.Marker(at.east(), level.dimension(), Direction.UP, 4))),
                    "a coloured marker needs the Order upgrade, as with the bell");
            test.assertTrue(apiMarkers(plain.getMarkers()).equals(start), "every refused list left the markers alone: " + plain.getMarkers());

            var pig = EntityType.PIG.create(level);
            pig.setPos(Vec3.atBottomCenterOf(at.north()));
            test.assertTrue(level.addFreshEntity(pig), "a real pig is admitted");
            golems.spawned.add(pig);
            test.assertTrue(!GolemApi.setHome(pig, at, Direction.UP) && !GolemApi.setMarkers(pig, start), "a pig is no golem to edit");

            for (Runnable call : List.<Runnable>of(() -> GolemApi.view(plain), () -> GolemApi.setHome(plain, at.west(), Direction.EAST),
                    () -> GolemApi.setMarkers(plain, List.of()))) {
                test.assertTrue(offThread(call) instanceof IllegalStateException, "a server golem refuses a foreign thread");
            }
            test.assertTrue(plain.getHomePosition().equals(at) && apiMarkers(plain.getMarkers()).equals(start),
                    "the foreign thread changed nothing");
            test.assertTrue(offThread(() -> GolemApi.view(pig)) == null && offThread(() -> GolemApi.isGolem(plain)) == null,
                    "an entity that is no golem, and isGolem, answer on any thread");

            // A golem whose saved material this version does not know: readable, and it refuses every edit.
            var tag = new CompoundTag();
            plain.saveWithoutId(tag);
            tag.putInt("GolemType", 42);
            var unsupported = golems.load(tag);
            var unknown = GolemApi.view(unsupported).orElseThrow();
            test.assertTrue(unknown.materialId() == 42 && unknown.material() == null, "an unknown material reads as its number without a name");
            test.assertTrue(!GolemApi.setHome(unsupported, at.west(), Direction.EAST) && !GolemApi.setMarkers(unsupported, List.of())
                            && unsupported.getHomePosition().equals(at) && unsupported.getMarkers().size() == 1,
                    "an unsupported golem refuses edits and keeps its data");

            plain.discard();
            test.assertTrue(!GolemApi.setHome(plain, at.west(), Direction.EAST) && !GolemApi.setMarkers(plain, List.of())
                    && plain.getHomePosition().equals(at), "a removed golem is not edited");
        } finally {
            golems.close();
        }
        test.succeed();
    }

    // ---- machines --------------------------------------------------------------------------------------------------

    /** The Arcane Ear through IArcaneEar: the note, its range, the hand tuning that follows it, the save, the sync data, threads. */
    public static void arcaneEarNoteIsTunedSyncedAndSaved(GameTestHelper test) {
        NodeStabilizerRuntimeChecks.requireLoadedFootprint(test);
        try (var f = new Fixture(test)) {
            f.open();
            BlockPos pos = f.base;
            f.put(pos.below(), Blocks.STONE);
            f.put(pos, ThaumcraftArcaneEar.ARCANE_EAR.get());
            var tile = (TileSensor) f.level.getBlockEntity(pos);
            IArcaneEar ear = tile;
            test.assertTrue(ear.getNote() == 0 && ear.getNote() == tile.getNote(), "a new ear listens for note 0");
            var chunk = f.level.getChunkAt(pos);
            chunk.setUnsaved(false);
            ear.setNote(17);
            test.assertTrue(ear.getNote() == 17 && chunk.isUnsaved(), "setNote changes the note and marks the block for saving");
            test.assertTrue(syncedNote(f, tile) == 17, "the block entity update packet carries the note to a fresh copy");

            chunk.setUnsaved(false);
            ear.setNote(17);
            test.assertTrue(ear.getNote() == 17 && !chunk.isUnsaved(), "the note the ear already has changes nothing");
            for (int bad : new int[] {-1, 25, 127, Integer.MIN_VALUE}) {
                boolean refused = false;
                try { ear.setNote(bad); } catch (IllegalArgumentException expected) { refused = true; }
                test.assertTrue(refused && ear.getNote() == 17, "note " + bad + " is refused and the note stays 17");
            }
            test.assertTrue(!chunk.isUnsaved(), "a refused note does not touch the save");
            test.assertTrue(offThread(() -> ear.setNote(3)) instanceof IllegalStateException && ear.getNote() == 17,
                    "a server ear is not tuned from another thread");

            ear.setNote(IArcaneEar.MAX_NOTE);
            tile.changePitch();
            test.assertTrue(ear.getNote() == 0, "the hand tuning goes on from the API's note and wraps after 24");
            ear.setNote(9);
            var saved = tile.saveWithFullMetadata(f.level.registryAccess());
            test.assertTrue(saved.getByte("note") == 9, "the save holds note 9");
            var reloaded = NodeTransductionRuntimeChecks.reload(f, tile, saved, TileSensor.class);
            test.assertTrue(((IArcaneEar) reloaded).getNote() == 9, "a reloaded ear listens for note 9");

            var detached = new TileSensor(pos, f.level.getBlockState(pos));
            detached.setNote(5);
            test.assertTrue(detached.getNote() == 0, "an ear that is not placed in a level is not tuned");
        }
        test.succeed();
    }

    /** The Brain in a Jar and the Deconstruction Table: values of the real block entities after their own ticks, saves and sync data. */
    public static void brainJarAndDeconstructorReadTheirBlockEntities(GameTestHelper test) {
        NodeStabilizerRuntimeChecks.requireLoadedFootprint(test);
        try (var f = new Fixture(test)) {
            f.open();
            ServerLevel level = f.level;
            BlockPos jarPos = f.base.east();
            f.put(jarPos.below(), Blocks.STONE);
            f.put(jarPos, ThaumcraftEldritchBlocks.JAR_BRAIN.get());
            var jarTile = (TileJarBrain) level.getBlockEntity(jarPos);
            IBrainJar jar = jarTile;
            test.assertTrue(jar.getXp() == 0 && jar.getXpMax() == 2000 && jar.getXpMax() == jarTile.xpMax, "an empty jar of the original capacity");
            var orb = new ExperienceOrb(level, jarPos.getX() + .5, jarPos.getY() + .5, jarPos.getZ() + .5, 37);
            test.assertTrue(level.addFreshEntity(orb), "a real experience orb inside the jar");
            TileJarBrain.tick(level, jarPos, level.getBlockState(jarPos), jarTile);
            test.assertTrue(orb.isRemoved() && jar.getXp() == 37 && jar.getXp() == jarTile.xp, "the jar's own tick swallowed the orb: " + jar.getXp());
            var jarMirror = new TileJarBrain(jarPos, level.getBlockState(jarPos));
            jarMirror.loadWithComponents(decodeUpdate(f, jarTile), level.registryAccess());
            test.assertTrue(((IBrainJar) jarMirror).getXp() == 37, "the update packet carries the experience");
            var jarReloaded = NodeTransductionRuntimeChecks.reload(f, jarTile, jarTile.saveWithFullMetadata(level.registryAccess()), TileJarBrain.class);
            test.assertTrue(((IBrainJar) jarReloaded).getXp() == 37, "a reloaded jar holds the same experience");

            BlockPos tablePos = f.base.west();
            f.put(tablePos.below(), Blocks.STONE);
            f.put(tablePos, ThaumcraftDeconstruction.TABLE.get());
            var tableTile = (TileDeconstructionTable) level.getBlockEntity(tablePos);
            IDeconstructor table = tableTile;
            test.assertTrue(!table.hasItem() && table.getReward() == null, "an empty table: no item, no primal");
            var input = new ItemStack(Items.IRON_INGOT, 2);
            tableTile.setItem(0, input);
            test.assertTrue(table.hasItem() && table.getReward() == null, "an input and no primal yet");
            var primals = DeconstructionRules.primals(ObjectAspectResolver.get(input.copyWithCount(1), level));
            test.assertTrue(primals.visSize() > 0, "iron has primals");
            for (int tick = 0; tick < 39; tick++) TileDeconstructionTable.tick(level, tablePos, level.getBlockState(tablePos), tableTile);
            long seed = -1;
            for (long candidate = 0; candidate < 100000 && seed < 0; candidate++) {
                if (net.minecraft.util.RandomSource.create(candidate).nextInt(80) < primals.visSize()) seed = candidate;
            }
            test.assertTrue(seed >= 0, "a seed for the original chance");
            level.random.setSeed(seed);
            TileDeconstructionTable.tick(level, tablePos, level.getBlockState(tablePos), tableTile);
            Aspect reward = tableTile.reward();
            test.assertTrue(reward != null && reward.isPrimal() && table.getReward() == reward && table.hasItem()
                    && tableTile.getItem(0).getCount() == 1, "the table's own work found " + reward + " and kept one ingot");
            var tableMirror = new TileDeconstructionTable(tablePos, level.getBlockState(tablePos));
            tableMirror.loadWithComponents(decodeUpdate(f, tableTile), level.registryAccess());
            test.assertTrue(((IDeconstructor) tableMirror).getReward() == reward && ((IDeconstructor) tableMirror).hasItem(),
                    "the update packet carries the primal and the input");
            var tableReloaded = NodeTransductionRuntimeChecks.reload(f, tableTile, tableTile.saveWithFullMetadata(level.registryAccess()),
                    TileDeconstructionTable.class);
            test.assertTrue(((IDeconstructor) tableReloaded).getReward() == reward && ((IDeconstructor) tableReloaded).hasItem(),
                    "a reloaded table answers the same");
            tableReloaded.clearContent();
            test.assertTrue(!((IDeconstructor) tableReloaded).hasItem() && ((IDeconstructor) tableReloaded).getReward() == reward,
                    "the waiting primal does not need the input");
            var quarantine = new CompoundTag();
            quarantine.put("RejectedDeconstruction", tableReloaded.saveWithFullMetadata(level.registryAccess()));
            var rejected = new TileDeconstructionTable(tablePos, level.getBlockState(tablePos));
            rejected.loadWithComponents(quarantine, level.registryAccess());
            test.assertTrue(rejected.isQuarantined() && !((IDeconstructor) rejected).hasItem() && ((IDeconstructor) rejected).getReward() == null,
                    "a table with unreadable data answers no item and no primal");
        }
        test.succeed();
    }

    /** The Arcane Bore: the answers of the original peripheral for three pickaxes, powered and not, before and after a reload. */
    public static void arcaneBoreAnswersTheOriginalPeripheralQuestions(GameTestHelper test) {
        NodeStabilizerRuntimeChecks.requireLoadedFootprint(test);
        try (var f = new Fixture(test)) {
            f.open();
            f.put(f.base.below(), ThaumcraftArcaneBore.ARCANE_BORE_BASE.get());
            f.put(f.base, ThaumcraftArcaneBore.ARCANE_BORE.get());
            var top = (TileArcaneBore) f.level.getBlockEntity(f.base);
            IArcaneBore bore = top;
            try {
                test.assertTrue(!bore.hasFocus() && !bore.hasPickaxe() && bore.getPickaxe().isEmpty() && !bore.isPickaxeBroken()
                                && !bore.isPowered() && !bore.isWorking() && bore.getRadius() == 2 && bore.getSpeed() == 0
                                && !bore.hasNativeClusters() && bore.getPickaxeFortune() == 0 && !bore.hasSilkTouch(),
                        "an empty bore: no tools, radius 2, speed 0");
                var focus = new ItemStack(ThaumcraftFoci.EXCAVATION_FOCUS.get());
                focus.set(ThaumcraftFoci.FOCUS_UPGRADES.get(), FocusUpgradeData.EMPTY.withUpgrade(FocusUpgradeType.potency, 1)
                        .withUpgrade(FocusUpgradeType.enlarge, 2).withUpgrade(FocusUpgradeType.enlarge, 4));
                var pick = new ItemStack(Items.IRON_PICKAXE);
                pick.enchant(enchantment(f, Enchantments.FORTUNE), 3);
                pick.enchant(enchantment(f, Enchantments.EFFICIENCY), 2);
                pick.setDamageValue(10);
                top.setItem(0, focus);
                top.setItem(1, pick);
                test.assertTrue(bore.hasFocus() && bore.hasPickaxe() && bore.hasFocus() == top.hasFocus(), "both tools are in");
                test.assertTrue(ItemStack.matches(bore.getPickaxe(), pick), "the pickaxe slot reads as the stack put in");
                bore.getPickaxe().setDamageValue(200);
                test.assertTrue(top.getItem(1).getDamageValue() == 10, "the pickaxe copy does not reach the bore");
                test.assertTrue(bore.getRadius() == 4 && bore.getRadius() == top.stats().radius() && 1 + 2 * bore.getRadius() == top.stats().width(),
                        "two Enlarge: radius " + bore.getRadius() + ", the original peripheral's width " + top.stats().width());
                test.assertTrue(bore.getSpeed() == 3 && bore.getSpeed() == top.stats().speed(), "one Potency and Efficiency 2: speed " + bore.getSpeed());
                test.assertTrue(bore.getPickaxeFortune() == 3 && !bore.hasSilkTouch() && !bore.hasNativeClusters() && !bore.isPickaxeBroken(),
                        "Fortune 3 iron pickaxe");
                test.assertTrue(!bore.isPowered() && !bore.isWorking(), "unpowered, the bore does not work");
                BlockPos power = f.base.below().east();
                f.put(power, Blocks.REDSTONE_BLOCK);
                test.assertTrue(bore.isPowered() && bore.isPowered() == top.isPowered() && bore.isWorking(), "a redstone block at the base powers it and it can work");
                f.put(power, Blocks.AIR);
                test.assertTrue(!bore.isPowered() && !bore.isWorking(), "without the redstone block it stops again");

                var reloaded = NodeTransductionRuntimeChecks.reload(f, top, top.saveWithFullMetadata(f.level.registryAccess()), TileArcaneBore.class);
                top = reloaded;
                bore = reloaded;
                test.assertTrue(bore.hasFocus() && bore.hasPickaxe() && bore.getRadius() == 4 && bore.getSpeed() == 3
                        && bore.getPickaxeFortune() == 3 && ItemStack.matches(bore.getPickaxe(), pick), "a reloaded bore answers the same at once");

                var core = new ItemStack(ThaumcraftEquipment.ELEMENTAL_PICKAXE.get());
                core.enchant(enchantment(f, Enchantments.SILK_TOUCH), 1);
                top.setItem(1, core);
                test.assertTrue(bore.hasNativeClusters() && bore.hasSilkTouch() && bore.getPickaxeFortune() == 0 && bore.getSpeed() == 1,
                        "the Pickaxe of the Core with Silk Touch: native clusters, silk, no fortune, speed of the focus alone");

                var worn = new ItemStack(Items.WOODEN_PICKAXE);
                worn.setDamageValue(worn.getMaxDamage() - 1);
                top.setItem(1, worn);
                f.put(power, Blocks.REDSTONE_BLOCK);
                test.assertTrue(bore.hasPickaxe() && bore.isPickaxeBroken() && bore.isPowered() && !bore.isWorking(),
                        "a pickaxe one use from breaking: broken, and the powered bore does not work");
                f.put(power, Blocks.AIR);
            } finally {
                top.setItem(0, ItemStack.EMPTY);
                top.setItem(1, ItemStack.EMPTY);
            }
            test.assertTrue(top.isEmpty(), "the tools are taken out before the fixture is cleared");
        }
        test.succeed();
    }

    // ---- helpers ---------------------------------------------------------------------------------------------------

    private static List<GolemView.Marker> apiMarkers(List<GolemMarker> markers) {
        return markers.stream().map(marker -> new GolemView.Marker(marker.pos(), ResourceKey.create(Registries.DIMENSION, marker.dimension()),
                marker.face(), marker.color())).toList();
    }

    private static net.minecraft.core.Holder<Enchantment> enchantment(Fixture f, ResourceKey<Enchantment> key) {
        return f.level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(key);
    }

    /** The block entity update packet through the vanilla codec, as a client receives it; returns its tag. */
    private static CompoundTag decodeUpdate(Fixture f, BlockEntity tile) {
        var packet = (ClientboundBlockEntityDataPacket) tile.getUpdatePacket();
        var wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), f.level.registryAccess());
        try {
            ClientboundBlockEntityDataPacket.STREAM_CODEC.encode(wire, packet);
            var received = ClientboundBlockEntityDataPacket.STREAM_CODEC.decode(wire);
            f.check(received.getPos().equals(tile.getBlockPos()) && received.getType() == tile.getType(), "the packet names the block entity");
            return received.getTag();
        } finally { wire.release(); }
    }

    private static int syncedNote(Fixture f, TileSensor tile) {
        var mirror = new TileSensor(tile.getBlockPos(), tile.getBlockState());
        mirror.loadWithComponents(decodeUpdate(f, tile), f.level.registryAccess());
        return mirror.getNote();
    }

    /** Runs the call on a new thread and returns what it threw, or null. */
    private static Throwable offThread(Runnable call) {
        var thrown = new AtomicReference<Throwable>();
        var thread = new Thread(() -> {
            try { call.run(); } catch (Throwable failure) { thrown.set(failure); }
        }, "thaumcraft-api-foreign-thread");
        thread.start();
        try { thread.join(10_000); } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        }
        if (thread.isAlive()) throw new AssertionError("the foreign-thread call did not return");
        return thrown.get();
    }

    /** Golems and other entities this scenario spawned or loaded; all discarded at the end. */
    private static final class Golems {
        final GameTestHelper test;
        final List<Entity> spawned = new ArrayList<>();

        Golems(GameTestHelper test) { this.test = test; }

        EntityGolemBase spawn(int material, int core, List<Integer> upgrades, String decoration, List<GolemMarker> markers,
                              BlockPos at, Direction facing) {
            var level = test.getLevel();
            var golem = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), level);
            golem.setGolemMaterial(material);
            int slots = GolemData.FILTER_SLOTS;
            golem.applyConfiguration(new GolemData(core, Collections.nCopies(slots, ItemStack.EMPTY), Collections.nCopies(slots, 0), false,
                    markers, GolemUseOptions.DEFAULT, upgrades, List.of(), thaumcraft.common.items.golems.GolemGuardTargets.DEFAULT, false, decoration));
            golem.setPos(Vec3.atBottomCenterOf(at));
            golem.setHome(at, facing);
            test.assertTrue(level.addFreshEntity(golem), "the golem is admitted");
            spawned.add(golem);
            test.assertTrue(golem.getGolemMaterial() == material && golem.getGolemCore() == core && golem.getDecoration().equals(decoration)
                    && golem.getMarkers().equals(markers), "the fixture golem is configured as asked");
            return golem;
        }

        /** A golem read from a save, as the level's loader makes it; never added to the level. */
        EntityGolemBase load(CompoundTag tag) {
            var golem = ThaumcraftGolems.GOLEM.get().create(test.getLevel());
            golem.load(tag.copy());
            spawned.add(golem);
            return golem;
        }

        void close() { for (Entity entity : spawned) entity.discard(); }
    }
}
