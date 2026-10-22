package thaumcraft.common.testing;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import java.util.List;
import java.util.UUID;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.visnet.TileVisNode;
import thaumcraft.api.visnet.VisNetHandler;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.common.blocks.BlockVisRelay;
import thaumcraft.common.config.ThaumcraftFocalManipulation;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.config.ThaumcraftNodeDevices;
import thaumcraft.common.container.ContainerFocalManipulator;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileFocalManipulator;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.tiles.TileNodeEnergized;
import thaumcraft.api.nodes.NodeType;

/** New bounded native checks, registered only by root wrappers. Each fixture owns
 * its connected real actor. Direct ticker advancement is declared SERVER state
 * testing, not elapsed world/client time or natural acquisition evidence. */
public final class FocalManipulatorRuntimeChecks {
    private FocalManipulatorRuntimeChecks() {}

    /** Wrapper timeout240; synchronous checks, no new test block registrations. */
    public static void admissionAndCancellation(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            f.open();
            var tile = machine(f, f.base); var menu = open(f, tile); levels(f, 7);
            long revision = menu.selectionRevision(); ItemStack baseline = tile.getItem(0).copy();
            f.check(!menu.requestStart(f.player, menu.sessionId(), revision, FocusUpgradeType.frugal.id)
                    && f.player.experienceLevel == 7 && tile.getSize() == 0, "rank1 requires EIGHT native levels, not XP points");
            levels(f, 8);
            f.check(!menu.clickMenuButton(f.player, FocusUpgradeType.frugal.id)
                    && !tile.startCraft(FocusUpgradeType.frugal.id, f.player, revision), "vanilla ID-only button and direct tile entry cannot bypass authenticated session admission");
            for (int id : new int[]{-1, 8, 20, Integer.MAX_VALUE})
                f.check(!menu.requestStart(f.player, menu.sessionId(), revision, id), "invalid/unimplemented paid branch refused: " + id);
            f.check(!menu.requestStart(f.player, UUID.randomUUID(), revision, FocusUpgradeType.frugal.id)
                    && !menu.requestStart(f.player, menu.sessionId(), revision + 1, FocusUpgradeType.frugal.id), "forged session and stale selection revision spend nothing");
            KnowledgeAccess.get(f.player).removeResearch("FOCALMANIPULATION");
            f.check(!menu.requestStart(f.player, menu.sessionId(), revision, FocusUpgradeType.frugal.id), "research checked at authoritative start, not only block opening");
            KnowledgeAccess.get(f.player).addResearch("FOCALMANIPULATION");
            f.check(menu.requestStart(f.player, menu.sessionId(), revision, FocusUpgradeType.frugal.id)
                    && f.player.experienceLevel == 0 && tile.getSize() == 800 && tile.getRank() == 1
                    && ItemStack.matches(tile.getItem(0), baseline), "exact threshold deducts eight LEVELS once, defers physical upgrade until paid vis");
            f.check(!menu.requestStart(f.player, menu.sessionId(), revision, FocusUpgradeType.frugal.id), "same admitted packet cannot replay");
            long paidRevision = tile.getSelectionRevision(); var debt = tile.getAspects(); debt.aspects.clear();
            f.player.closeContainer(); ticks(f, tile, 31);
            f.check(tile.getAspects().visSize() == 800 && tile.getSelectionRevision() == paidRevision
                    && focus(tile).getUpgradeLevel(tile.getItem(0), FocusUpgradeType.frugal) == 0, "no power means no passive progress; closed menu/player presence does not cancel job or advance selection revision");
            tile.setItem(0, named("replacement-no-free-upgrade"));
            f.check(tile.getSize() == 0 && tile.getAspects().visSize() == 0 && tile.getSelectionRevision() > paidRevision,
                    "external setter cancels BEFORE a later pulse; original clear-debt/replacement free apply is closed");
            ticks(f, tile, 10);
            f.check(focus(tile).getUpgradeLevel(tile.getItem(0), FocusUpgradeType.frugal) == 0 && f.player.experienceLevel == 0,
                    "replacement never receives old paid upgrade; cancellation never refunds XP");

            f.mode(GameType.CREATIVE); levels(f, 7); menu = open(f, tile);
            f.check(!start(f, menu, FocusUpgradeType.frugal), "creative STILL needs eight levels");
            levels(f, 8); f.check(start(f, menu, FocusUpgradeType.frugal) && f.player.experienceLevel == 8, "creative exact threshold starts without level debit");
            tile.removeItem(0, 1); f.check(tile.getSize() == 0, "raw decrement cancels even without a setter follow-up");
            f.mode(GameType.SURVIVAL);
            for (int operation = 0; operation < 4; operation++) {
                tile.setItem(0, named("cancel-" + operation)); levels(f, 64); menu = open(f, tile); f.check(start(f, menu, FocusUpgradeType.frugal), "owned cancellation fixture admitted");
                switch (operation) {
                    case 0 -> tile.removeItemNoUpdate(0);
                    case 1 -> tile.clearContent();
                    case 2 -> tile.setItem(0, tile.getItem(0)); // even same setter must not clear only debt
                    case 3 -> { tile.getItem(0).set(DataComponents.CUSTOM_NAME, Component.literal("live-reference-mutation")); tile.getSelectionRevision(); }
                    default -> throw new AssertionError(operation);
                }
                f.check(tile.getSize() == 0 && tile.getAspects().size() == 0 && f.player.experienceLevel == 56, "each external mutation path cancels without refund: " + operation);
            }
            tile.setItem(0, new ItemStack(Items.STICK)); ticks(f, tile, 5);
            f.check(tile.getItem(0).is(Items.STICK) && tile.getSize() == 0, "raw foreign item stays physical but cannot be cast/upgraded/crash completion");
            tile.setItem(0, named("automation"));
            for (Direction face : Direction.values()) f.check(tile.getSlotsForFace(face).length == 1 && tile.getSlotsForFace(face)[0] == 0
                    && tile.canPlaceItemThroughFace(0, named("candidate"), face) && !tile.canPlaceItemThroughFace(0, new ItemStack(Items.STICK), face)
                    && tile.canTakeItemThroughFace(0, tile.getItem(0), face), "original all-face focus insertion and unrestricted extraction: " + face);
            ItemStack holes = named("ordered-holes"); var holeFocus = (ItemFocusBasic) holes.getItem();
            f.check(holeFocus.applyUpgrade(holes, FocusUpgradeType.potency, 1) && holeFocus.applyUpgrade(holes, FocusUpgradeType.potency, 3),
                    "declared SERVER API fixture has ranks1/3 and an un-compacted hole at2");
            tile.setItem(0, holes); levels(f, 64); menu = open(f, tile);
            f.check(start(f, menu, FocusUpgradeType.frugal) && tile.getRank() == 2 && tile.getSize() == 1600
                    && f.player.experienceLevel == 48 && holeFocus.getAppliedUpgrades(tile.getItem(0))[2] == FocusUpgradeType.potency.id,
                    "machine uses FIRST hole2, pays16 levels/400 per key, retains later occupied rank3");
            tile.clearContent();
            ItemStack full = named("five-filled-ranks");
            for (int rank = 1; rank <= 5; rank++) f.check(((ItemFocusBasic) full.getItem()).applyUpgrade(full, FocusUpgradeType.frugal, rank), "declared full SERVER fixture rank" + rank);
            tile.setItem(0, full); levels(f, 64); menu = open(f, tile);
            f.check(!start(f, menu, FocusUpgradeType.frugal) && f.player.experienceLevel == 64 && tile.getSize() == 0,
                    "five occupied ranks safely refuse sixth admission without indexing/charging");
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                UUID session = UUID.randomUUID(); buffer.writeBlockPos(f.base); buffer.writeUUID(session);
                var mirror = new ContainerFocalManipulator(99, f.player.getInventory(), buffer);
                long words = 0x1234abcdffff8023L;
                for (int word = 0; word < 4; word++) mirror.setData(word, (short) (words >>> (word * 16)));
                f.check(mirror.sessionId().equals(session) && mirror.tablePos().equals(f.base) && mirror.selectionRevision() == words,
                        "extended opening buffer and FOUR native signed-short data slots round-trip UUID/64-bit revision (server-side codec check, not client evidence)");
            } finally { buffer.release(); }
            tile.setItem(0, named("automation"));
            menu = open(f, tile); f.player.getInventory().setItem(9, named("held-focus"));
            f.check(menu.quickMoveStack(f.player, 1).isEmpty() && f.player.getInventory().getItem(9).getItem() instanceof ItemFocusBasic
                    && f.player.getInventory().getItem(0).isEmpty(), "occupied focus slot refuses held-focus quick move with NO fallback shuffle");
            tile.removeItem(0, 1);
            f.check(!menu.quickMoveStack(f.player, 1).isEmpty() && f.player.getInventory().getItem(9).isEmpty(), "player MAIN slot1 moves into actual machine slot0");
            f.check(!menu.quickMoveStack(f.player, 0).isEmpty() && f.player.getInventory().getItem(9).getItem() instanceof ItemFocusBasic, "machine quick move uses ascending main inventory before hotbar");
            f.player.getInventory().setItem(10, new ItemStack(Items.COBBLESTONE));
            f.check(!menu.quickMoveStack(f.player, 2).isEmpty() && f.player.getInventory().getItem(0).is(Items.COBBLESTONE), "nonfocus MAIN moves to ascending hotbar");
            f.check(!menu.quickMoveStack(f.player, 28).isEmpty() && f.player.getInventory().getItem(10).is(Items.COBBLESTONE), "nonfocus hotbar moves to ascending main");
        }
        test.succeed();
    }

    /** Wrapper timeout240; actual converted built-in source, raw partial grants,
     * all five phases, factory reload, malformed-state quarantine and completion. */
    public static void sourceDebitsAndReload(GameTestHelper test) {
        try (var f = NodeTransductionRuntimeChecks.fixture(test, false)) {
            f.open(); var source = source(f, f.base.west(4));
            for (int phase = 0; phase < 5; phase++) {
                var tile = machine(f, f.base); ticks(f, tile, phase); levels(f, 64); var menu = open(f, tile);
                NodeTransductionRuntimeChecks.sourceTick(f, source);
                f.check(start(f, menu, FocusUpgradeType.frugal) && tile.getTickCount() == phase, "start preserves live phase" + phase);
                ticks(f, tile, 4 - phase);
                f.check(tile.getAspects().visSize() == 800, "no drain before original fifth-tick pulse at phase" + phase);
                tick(f, tile);
                f.check(tile.getAspects().visSize() == 400 && source.getAvailableVis().visSize() == 0, "same pulse drains100 EACH of four primals, not100 total/whole-vis conversion");
                tile.clearContent();
            }
            var tile = machine(f, f.base); levels(f, 64); var menu = open(f, tile);
            NodeTransductionRuntimeChecks.sourceTick(f, source);
            for (Aspect aspect : List.of(Aspect.AIR, Aspect.EARTH, Aspect.WATER, Aspect.ENTROPY))
                f.check(source.consumeVis(aspect, 93) == 93, "declared SERVER fixture pre-consumes actual source stock leaving7, no fake grant API");
            f.check(start(f, menu, FocusUpgradeType.frugal), "partially powered job starts through actual session");
            ticks(f, tile, 5); long revision = tile.getSelectionRevision();
            f.check(tile.getAspects().visSize() == 772 && source.getAvailableVis().visSize() == 0, "four independent ACTUAL grants7 reduce800 by28, raw quantum1 never becomes100");
            ticks(f, tile, 5);
            f.check(tile.getAspects().visSize() == 772 && tile.getSelectionRevision() == revision, "same exhausted source epoch gives0; debt pulses are NOT selection changes");
            CompoundTag saved = tile.saveWithFullMetadata(f.level.registryAccess());
            var old = tile; var oldMenu = menu;
            tile = NodeTransductionRuntimeChecks.reload(f, tile, saved, TileFocalManipulator.class);
            f.check(!tile.isQuarantined() && tile.getTickCount() == 0 && tile.getAspects().visSize() == 772
                    && f.player.experienceLevel == 56 && tile.getSelectionRevision() == revision, "real factory reload resumes exact remaining/binding/revision, zero transient phase, NO second XP charge");
            f.check(!oldMenu.requestStart(f.player, oldMenu.sessionId(), revision, FocusUpgradeType.frugal.id), "old menu cannot act on a same-position replacement BE");
            var beforeStaleDrop = f.pendingRows().size(); old.evacuate();
            f.check(f.pendingRows().size() == beforeStaleDrop, "retired BE cannot evacuate duplicate physical baseline");
            menu = open(f, tile); f.check(!menu.sessionId().equals(oldMenu.sessionId()), "reopen gets a fresh server session UUID despite unchanged persisted revision");
            NodeTransductionRuntimeChecks.sourceTick(f, source); ticks(f, tile, 5);
            f.check(tile.getAspects().visSize() == 372 && f.player.experienceLevel == 56, "new native source epoch pays exactly100 per owed primal");
            NodeTransductionRuntimeChecks.sourceTick(f, source); ticks(f, tile, 5);
            f.check(tile.getSize() == 0 && tile.getRank() == -1 && tile.getUpgrade() == FocusUpgradeType.frugal.id
                    && focus(tile).getUpgradeLevel(tile.getItem(0), FocusUpgradeType.frugal) == 1
                    && source.getAvailableVis().visSize() == 28, "final pulse pays only93 EACH, upgrades once immediately, retains stale upgrade/transient rank-1");
            ticks(f, tile, 6);
            f.check(tile.getRank() == 0 && focus(tile).getUpgradeLevel(tile.getItem(0), FocusUpgradeType.frugal) == 1, "next idle tick normalizes rank, later pulses cannot replay completion");
            malformedPersistence(f, tile, saved);
        }
        test.succeed();
    }

    private static void malformedPersistence(Fixture f, TileFocalManipulator current, CompoundTag valid) {
        for (String defect : List.of("schema", "zero-debt", "rank", "binding", "missing-job", "duplicate-aspect", "wrong-type")) {
            CompoundTag bad = valid.copy(); CompoundTag state = bad.getCompound("FocalState");
            switch (defect) {
                case "schema" -> state.putInt("Schema", 777);
                case "zero-debt" -> { for (Tag raw : state.getList("Remaining", Tag.TAG_COMPOUND)) ((CompoundTag) raw).putInt("amount", 0); }
                case "rank" -> state.putInt("Rank", 0);
                case "binding" -> state.getCompound("Job").putLong("Revision", state.getLong("Revision") + 1);
                case "missing-job" -> state.remove("Job");
                case "duplicate-aspect" -> { ListTag rows = state.getList("Remaining", Tag.TAG_COMPOUND); rows.add(rows.getCompound(0).copy()); }
                case "wrong-type" -> state.putInt("Revision", 1);
                default -> throw new AssertionError(defect);
            }
            current = NodeTransductionRuntimeChecks.reload(f, current, bad, TileFocalManipulator.class);
            tick(f, current);
            f.check(current.isQuarantined() && current.getSize() == 0 && current.getItem(0).isEmpty()
                    && current.saveWithoutMetadata(f.level.registryAccess()).getCompound("RejectedFocal").equals(bad), "WHOLE malformed paid state retained inert, never zero-debt free application: " + defect);
            current = NodeTransductionRuntimeChecks.reload(f, current, current.saveWithFullMetadata(f.level.registryAccess()), TileFocalManipulator.class);
            f.check(current.isQuarantined() && current.getUpdateTag(f.level.registryAccess()).getCompound("RejectedFocal").equals(bad), "second actual reload retains evidence without nesting/default normalization");
        }
    }

    /** Wrapper timeout240. Probe is an installed registered-type graph participant
     * using final native debit/attachment APIs; only POST-debit pulse differs. */
    public static void postDebitFences(GameTestHelper test) {
        try (var f = NodeTransductionRuntimeChecks.fixture(test, false)) {
            f.open(); var source = source(f, f.base.west(7));
            f.check(source.getNodeCounter() == 0 && source.getRefillEpoch() == 0 && source.getAvailableVis().visSize() == 0
                    && VisNetHandler.getSources(f.level).stream().noneMatch(ref -> ref.get() == source),
                    "conversion alone leaves this exact source unregistered with zero pulse before its first native tick");
            // energize() advances the CONVERTER, not the new source. Register/refill
            // through the actual source ticker BEFORE the relay's first/40 graph
            // phase; never substitute addSource/setParent or a fabricated grant.
            NodeTransductionRuntimeChecks.sourceTick(f, source);
            f.check(source.getNodeCounter() == 1 && source.getRefillEpoch() == 1 && source.getAvailableVis().visSize() == 400
                    && VisNetHandler.getSources(f.level).stream().filter(ref -> ref.get() == source).count() == 1,
                    "first native source tick registers the exact converted owner once and earns four independent100 budgets");
            var relay = pulseRelay(f, f.base);
            TileVisNode.tick(f.level, relay.getBlockPos(), relay.getBlockState(), relay);
            f.check(VisNetRuntimeChecks.parent(relay) == source, "owned real source/registered-type pulse probe use actual network attachment");
            var root = relay.getRootSource();
            f.check(root != null && root.get() == source && source.getChildren().stream().filter(ref -> ref.get() == relay).count() == 1,
                    "native relay attachment has the exact registered root and one reciprocal source-child edge");
            BlockPos machinePos = f.base.east(7);
            for (int scenario = 0; scenario < 3; scenario++) {
                var tile = machine(f, machinePos); levels(f, 64); var menu = open(f, tile);
                NodeTransductionRuntimeChecks.sourceTick(f, source); f.check(start(f, menu, FocusUpgradeType.frugal), "post-debit fixture paid admission");
                CompoundTag paid = tile.saveWithFullMetadata(f.level.registryAccess()); int branch = scenario;
                var replacement = new TileFocalManipulator[1];
                relay.action = () -> {
                    f.check(source.getAvailableVis().getAmount(Aspect.WATER) == 0 && source.getAvailableVis().visSize() == 300
                            && VisNetHandler.isConsumeActive(f.level), "first callback ALREADY sees one100 debit; no other primal charged yet");
                    boolean refused = false;
                    try { tile.loadWithComponents(paid, f.level.registryAccess()); } catch (IllegalStateException expected) { refused = true; }
                    f.check(refused, "same-owner reload cannot restore debt during active debit");
                    var detached = new TileFocalManipulator(machinePos, tile.getBlockState()); refused = false;
                    try { detached.loadWithComponents(paid, f.level.registryAccess()); } catch (IllegalStateException expected) { refused = true; }
                    f.check(refused, "detached load cannot resurrect a second paid owner during active frame either");
                    if (branch == 0) tile.setItem(0, named("post-debit-replacement"));
                    else if (branch == 1) { tile.getItem(0).set(DataComponents.CUSTOM_NAME, Component.literal("post-debit-live-mutation")); }
                    else {
                        f.level.removeBlockEntity(machinePos);
                        replacement[0] = new TileFocalManipulator(machinePos, tile.getBlockState()); f.level.setBlockEntity(replacement[0]);
                        replacement[0].setItem(0, named("post-debit-new-owner"));
                    }
                    tick(f, tile); // reentrant old/current ticker cannot advance another frame
                };
                int beforePulses = relay.pulses; ticks(f, tile, 5);
                TileFocalManipulator live = replacement[0] == null ? tile : replacement[0];
                f.check(relay.pulses == beforePulses + 1 && source.getAvailableVis().visSize() == 300
                        && live.getSize() == 0 && focus(live).getUpgradeLevel(live.getItem(0), FocusUpgradeType.frugal) == 0
                        && f.player.experienceLevel == 56, "post-debit identity fence stops credit/next aspect/apply; spent100 is neither refunded nor doubly reused: " + scenario);
                ticks(f, live, 5);
                f.check(source.getAvailableVis().visSize() == 300, "cancelled/new owner never drains an extra pulse");
                live.clearContent();
            }
        }
        test.succeed();
    }

    /** Wrapper timeout240; actual ADD refusal, stable-ID durable retry (<80 real
     * ticks), exact physical components; no new source/probe registrations. */
    public static void durableDropRefusal(GameTestHelper test) {
        var f = new Fixture(test); boolean[] registered = {false};
        EntityEvent.Add[] listener = new EntityEvent.Add[1];
        Runnable close = () -> { if (registered[0]) { EntityEvent.ADD.unregister(listener[0]); registered[0] = false; } f.close(); };
        try {
            f.open(); var tile = machine(f, f.base); ItemStack physical = tile.getItem(0).copy();
            levels(f, 64); var menu = open(f, tile); f.check(start(f, menu, FocusUpgradeType.frugal), "breaking fixture has paid XP and outstanding debt");
            var blockDrops = Block.getDrops(tile.getBlockState(), f.level, f.base, tile);
            f.check(blockDrops.size() == 1 && blockDrops.getFirst().is(ThaumcraftFocalManipulation.FOCAL_MANIPULATOR_ITEM.get()), "ordinary dropped machine carries no paid job/inventory");
            f.check(f.level.destroyBlock(f.base, true, f.player), "actual native break evacuates active physical slot"); f.drops();
            var rows = f.pendingRows(); f.check(rows.size() == 1 && tile.getItem(0).isEmpty() && tile.getSize() == 0 && f.player.experienceLevel == 56,
                    "break retires job, keeps level cost spent, enqueues ONE physical focus separate from block loot");
            UUID id = rows.getFirst().getUUID("Id"); int[] calls = {0}; long[] first = {-1}; Throwable[] unexpected = {null};
            listener[0] = (entity, world) -> {
                if (world != f.level || !entity.getUUID().equals(id)) return EventResult.pass();
                try {
                    f.check(entity instanceof ItemEntity drop && ItemStack.matches(drop.getItem(), physical), "real ADD candidate retains exact named focus/components");
                    calls[0]++;
                    if (calls[0] == 1) { first[0] = f.level.getGameTime(); return EventResult.interruptFalse(); }
                    f.check(f.level.getGameTime() - first[0] >= 20, "retry waits actual native world ticks, never rewritten clock");
                    return EventResult.pass();
                } catch (RuntimeException | Error failure) { unexpected[0] = failure; throw failure; }
            };
            EntityEvent.ADD.register(listener[0]); registered[0] = true;
            f.player.setPos(f.base.getX() + 7, f.base.getY() + 2, f.base.getZ() + 7);
            DeferredItemDrops.tick(f.level);
            f.check(unexpected[0] == null && calls[0] == 1 && f.level.getEntity(id) == null && f.pendingRows().size() == 1,
                    "actual cancelled ADD keeps same one durable focus owner instead of clearing/loss/second UUID");
            test.onEachTick(() -> {
                if (f.closed) return;
                try {
                    f.check(unexpected[0] == null && f.player.experienceLevel == 56, "retry callbacks neither fail silently nor refund paid levels");
                    var delivered = f.level.getEntity(id);
                    if (!(delivered instanceof ItemEntity drop)) return;
                    f.check(calls[0] >= 2 && ItemStack.matches(drop.getItem(), physical) && f.pendingRows().isEmpty(), "one exact stable-ID focus delivered after refusal without resurrecting job");
                    f.drops(); DeferredItemDrops.tick(f.level);
                    f.check(f.level.getEntity(id) == delivered && f.pendingRows().isEmpty(), "further flush cannot duplicate accepted physical focus");
                    close.run(); test.succeed();
                } catch (RuntimeException | Error failure) { close.run(); throw failure; }
            });
            test.runAfterDelay(80, () -> { if (!f.closed) { close.run(); test.fail("Focal physical drop did not settle within80 real ticks"); } });
        } catch (RuntimeException | Error failure) { close.run(); throw failure; }
    }

    private static TileFocalManipulator machine(Fixture f, BlockPos pos) {
        f.player.closeContainer();
        if (f.level.getBlockEntity(pos) instanceof TileFocalManipulator old) old.clearContent();
        f.put(pos, Blocks.AIR); f.put(pos, ThaumcraftFocalManipulation.FOCAL_MANIPULATOR.get());
        var tile = (TileFocalManipulator) f.level.getBlockEntity(pos);
        f.check(tile != null && !tile.isQuarantined(), "registered machine factory creates fresh empty native owner");
        tile.setItem(0, named("owned-native-focal-input"));
        KnowledgeAccess.get(f.player).addResearch("FOCALMANIPULATION");
        return tile;
    }
    private static ContainerFocalManipulator open(Fixture f, TileFocalManipulator tile) {
        f.player.closeContainer(); f.aim(Vec3.atCenterOf(tile.getBlockPos())); tile.openMenu(f.player);
        f.check(f.player.containerMenu instanceof ContainerFocalManipulator && f.player.containerMenu.slots.size() == 37, "actual native extended menu/session opens original37 slots");
        return (ContainerFocalManipulator) f.player.containerMenu;
    }
    private static boolean start(Fixture f, ContainerFocalManipulator menu, FocusUpgradeType type) { return menu.requestStart(f.player, menu.sessionId(), menu.selectionRevision(), type.id); }
    private static void levels(Fixture f, int levels) { f.player.experienceLevel = levels; f.player.experienceProgress = .25F; f.player.totalExperience = 1000; }
    private static ItemStack named(String name) { var stack = new ItemStack(ThaumcraftFoci.EXCAVATION_FOCUS.get()); stack.set(DataComponents.CUSTOM_NAME, Component.literal(name)); return stack; }
    private static ItemFocusBasic focus(TileFocalManipulator tile) { return (ItemFocusBasic) tile.getItem(0).getItem(); }
    private static void tick(Fixture f, TileFocalManipulator tile) {
        var ticker = tile.getBlockState().getTicker(f.level, ThaumcraftFocalManipulation.FOCAL_MANIPULATOR_BLOCK_ENTITY.get());
        if (ticker == null) throw new IllegalStateException("Missing registered focal ticker");
        ticker.tick(f.level, tile.getBlockPos(), tile.getBlockState(), tile);
    }
    private static void ticks(Fixture f, TileFocalManipulator tile, int count) { for (int i = 0; i < count; i++) tick(f, tile); }
    private static TileNodeEnergized source(Fixture f, BlockPos pos) {
        var base = new AspectList();
        for (Aspect aspect : List.of(Aspect.AIR, Aspect.EARTH, Aspect.WATER, Aspect.ENTROPY)) base.add(aspect, 10000);
        return NodeTransductionRuntimeChecks.energize(f, pos, base, null, NodeType.NORMAL);
    }
    private static PulseRelay pulseRelay(Fixture f, BlockPos pos) {
        f.put(pos.below(), Blocks.STONE); f.put(pos, Blocks.AIR);
        BlockState state = ThaumcraftNodeDevices.VIS_RELAY.get().defaultBlockState().setValue(BlockVisRelay.FACING, Direction.UP);
        f.level.setBlock(pos, state, TubeVariantsRuntimeChecks.QUIET); f.level.removeBlockEntity(pos);
        var relay = new PulseRelay(pos, state); f.level.setBlockEntity(relay); return relay;
    }
    private static final class PulseRelay extends TileVisNode {
        Runnable action; int pulses;
        PulseRelay(BlockPos pos, BlockState state) { super(ThaumcraftNodeDevices.VIS_RELAY_BLOCK_ENTITY.get(), pos, state); }
        @Override public int getRange() { return 8; }
        @Override public boolean isSource() { return false; }
        @Override public void triggerConsumeEffect(Aspect aspect) {
            ++pulses; Runnable once = action; action = null;
            if (once == null) return;
            try { once.run(); } catch (RuntimeException unexpected) { throw new AssertionError("Unexpected focal POST-debit callback failure", unexpected); }
        }
    }
}
