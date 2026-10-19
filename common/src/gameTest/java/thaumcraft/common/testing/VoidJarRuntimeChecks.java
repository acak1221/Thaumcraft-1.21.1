package thaumcraft.common.testing;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftAlembics;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftLabels;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.items.ItemEssence;
import thaumcraft.common.items.ItemJar;
import thaumcraft.common.items.ItemJarLabel;
import thaumcraft.common.items.JarLabelData;
import thaumcraft.common.lib.essentia.EssentiaStorageEndpoint;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TileTube;

/** One real-loader case. Explicit server stock fixtures are not survival acquisition. */
public final class VoidJarRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final SavedData.Factory<DeferredItemDrops> QUEUE = new SavedData.Factory<>(DeferredItemDrops::new, DeferredItemDrops::load, null);
    private VoidJarRuntimeChecks() {}

    public static void run(GameTestHelper test, ServerPlayer player) {
        Fixture f = new Fixture(test, player);
        try {
            f.open();
            transport(f);
            callbacks(f);
            phialsLabelsAndPlacement(f);
            heldJars(f);
            rejectedOutput(f);
            // Retry uses ordinary level-post and the real clock. The shared
            // queue has its own batch wake in addition to each output's minimum
            // twenty-tick cooldown: another fresh enqueue can move that wake.
            // Like the existing table-output case, observe actual delivery with
            // a bounded70-tick deadline; twenty-two is not a production promise.
            // Do not force a queue tick/flush, reset its clock or rewrite data.
            player.setPos(f.jarPos.getX() - 1.5, f.jarPos.getY(), f.jarPos.getZ() - 1.5);
            player.setDeltaMovement(Vec3.ZERO);
            test.onEachTick(() -> {
                if (f.closed) return;
                try {
                    var drops = f.drops().stream().filter(drop -> f.marked(drop.getItem())).toList();
                    if (drops.isEmpty() && f.pendingMarked() == 1) return;
                    f.check(drops.size() == 1 && ItemJar.getAmount(drops.getFirst().getItem()) == 32
                            && ItemJar.isVoid(drops.getFirst().getItem()) && f.pendingMarked() == 0
                            && drops.getFirst().getUUID().equals(f.rejectedId) && f.level.getGameTime() - f.rejectedAt >= 20,
                            "cancelled native ADD retries exactly one paid component-bearing jar, with no pending duplicate; elapsedNativeTicks="
                                    + (f.level.getGameTime() - f.rejectedAt) + "; pending=" + f.pendingMarked()
                                    + "; localMarked=" + drops.stream().map(drop -> drop.getUUID() + "@" + drop.position()
                                            + ":" + drop.getItem().save(f.level.registryAccess())).toList()
                                    + "; wideMarked=" + f.level.getEntitiesOfClass(ItemEntity.class, f.bounds.inflate(64)).stream()
                                            .filter(drop -> f.marked(drop.getItem())).map(drop -> drop.getUUID() + "@" + drop.position()
                                                    + ":" + drop.getItem().save(f.level.registryAccess())).toList());
                    thaumcraft.common.Thaumcraft.LOG.info("Void jar paid output retried through native level-post after {} ticks, same UUID {}, one jar32 and zero pending duplicates",
                            f.level.getGameTime() - f.rejectedAt, f.rejectedId);
                    f.close(); test.succeed();
                } catch (RuntimeException | Error error) { f.close(); throw error; }
            });
            test.runAfterDelay(70, () -> {
                if (f.closed) return;
                String diagnostic = "elapsedNativeTicks=" + (f.level.getGameTime() - f.rejectedAt) + "; pending=" + f.pendingMarked()
                        + "; sourceChunkLoaded=" + f.level.hasChunkAt(f.sourcePos) + "; stableUUID=" + f.rejectedId;
                f.close(); test.fail("cancelled paid jar did not retry through ordinary level-post within70 native ticks; " + diagnostic);
            });
        } catch (RuntimeException | Error error) { f.close(); throw error; }
    }

    private static void transport(Fixture f) {
        TileJar jar = f.jar(), normal = f.normal();
        f.check(jar.isVoid() && !normal.isVoid() && jar.getMaxAmount() == 64 && normal.getMaxAmount() == 64,
                "two real blocks share the registered BE without a fake/unlimited capacity");
        for (TileJar tile : new TileJar[] {jar, normal}) {
            for (Direction face : Direction.values()) f.check(tile.isConnectable(face) == (face == Direction.UP)
                    && tile.canInputFrom(face) == (face == Direction.UP) && tile.canOutputTo(face) == (face == Direction.UP), "jar ports remain top-only");
            f.check(!tile.isConnectable(null) && tile.getSuctionAmount(null) == 32, "UNKNOWN is query-only");
            tile.setFilter(Aspect.WATER);
            f.check(tile.getSuctionAmount(null) == (tile.isVoid() ? 48 : 64) && tile.getMinimumSuction() == (tile.isVoid() ? 48 : 64), "nonfull labelled pressure");
            tile.addToContainer(Aspect.WATER, 64);
            f.check(tile.getSuctionAmount(null) == (tile.isVoid() ? 32 : 0) && tile.getMinimumSuction() == (tile.isVoid() ? 48 : 64), "full pressure and minimum are different contracts");
            tile.setFilter(null);
            f.check(tile.getSuctionAmount(null) == (tile.isVoid() ? 32 : 0) && tile.getMinimumSuction() == 32, "full unlabelled pressure");
        }
        var state = jar.getBlockState();
        f.check(state.getCollisionShape(f.level, f.jarPos).bounds().equals(new AABB(0, 0, 0, 1, 1, 1))
                && state.getShape(f.level, f.jarPos).bounds().equals(new AABB(.1875, 0, .1875, .8125, .75, .8125))
                && state.getAnalogOutputSignal(f.level, f.jarPos) == 15 && state.getLightEmission() == 9,
                "full entity collision, compact selection, comparator15 and original light9");
        f.check(jar.addToContainer(Aspect.WATER, Integer.MAX_VALUE) == 0 && jar.getAmount() == 64
                && normal.addToContainer(Aspect.WATER, 8) == 8 && jar.addToContainer(Aspect.FIRE, 8) == 8
                && jar.addToContainer(Aspect.WATER, -1) == -1, "bounded overflow consumes only compatible positive offers");
        jar.setFilter(Aspect.WATER);
        TileTube tube = f.tube(); TileAlembic source = f.source();
        tube.addEssentia(Aspect.WATER, 1, Direction.UP); tube.setSuction(Aspect.WATER, 32);
        f.jarTicks(5);
        f.check(tube.getEssentiaAmount(null) == 1, "equal pressure refuses even a full void sink");
        tube.setSuction(Aspect.WATER, 31); f.jarTicks(4);
        f.check(tube.getEssentiaAmount(null) == 1, "four local ticks cannot pay a transfer");
        f.jarTicks(1);
        f.check(jar.getAmount() == 64 && tube.getEssentiaAmount(null) == 0, "fifth tick deliberately deletes exactly the one buffered unit");
        // New counted batch: both jars64 + source16. Every native endpoint tick
        // is checked; only a full jar's actual tube debit increments deletion.
        source.addToContainer(Aspect.WATER, 16);
        int deleted = 0;
        for (int tick = 0; tick < 180; tick++) {
            TileTube.tick(f.level, f.tubePos, tube.getBlockState(), tube);
            f.check(jar.getAmount() + normal.getAmount() + tube.getEssentiaAmount(null) + source.getAmount() + deleted == 144, "tube tick conserves all supplied stock");
            int buffered = tube.getEssentiaAmount(null);
            f.jarTicks(1);
            deleted += buffered - tube.getEssentiaAmount(null);
            f.check(jar.getAmount() == 64 && jar.getAmount() + normal.getAmount() + tube.getEssentiaAmount(null) + source.getAmount() + deleted == 144, "stored plus explicit voided equals supplied after EVERY jar tick");
        }
        f.check(deleted == 16 && source.getAmount() == 0 && tube.getEssentiaAmount(null) == 0, "real alembic/tube/full-void network drains the entire bounded batch");
        tube.addEssentia(Aspect.FIRE, 1, Direction.UP); tube.setSuction(Aspect.FIRE, 0); f.jarTicks(5);
        f.check(tube.getEssentiaAmount(null) == 1 && jar.getAmount() == 64, "wrong aspect is never an overflow deletion");
        tube.takeEssentia(Aspect.FIRE, 1, Direction.UP);
        tube.addEssentia(Aspect.WATER, 1, Direction.UP); tube.setSideOpen(Direction.DOWN, false); f.jarTicks(5);
        f.check(tube.getEssentiaAmount(null) == 1, "closed opposite port cannot pay a unit");
        tube.setSideOpen(Direction.DOWN, true); tube.takeEssentia(Aspect.WATER, 1, Direction.UP);
        CompoundTag forged = normal.saveWithFullMetadata(f.level.registryAccess()); forged.putBoolean("void", true);
        normal.loadWithComponents(forged, f.level.registryAccess());
        f.check(!normal.isVoid() && normal.getSuctionAmount(null) == 0, "saved arbitrary data cannot turn the ordinary block into a sink");
    }

    private static void callbacks(Fixture f) {
        TileJar jar = f.jar();
        // Adversarial endpoint fixtures exist only during this synchronous
        // section; gameplay above/below uses actual registered native endpoints.
        Probe ordinarySource = f.probe(f.normalPos.above(), 1, false);
        f.check(EssentiaTransfers.pullOne(f.normal(), Direction.UP, Aspect.WATER) == 0 && ordinarySource.amount == 1,
                "ordinary full receiver cannot opt into deletion");
        f.level.removeBlockEntity(f.normalPos.above()); f.level.setBlock(f.normalPos.above(), Blocks.AIR.defaultBlockState(), QUIET);
        Probe replaced = f.probe(f.tubePos, 1, false);
        replaced.query = () -> f.level.removeBlockEntity(f.tubePos);
        f.check(EssentiaTransfers.pullOne(jar, Direction.UP, Aspect.WATER) == 0 && replaced.amount == 1,
                "source replacement during a sided query cannot pay an orphaned buffer");
        Probe source = f.probe(f.tubePos, 1, false);
        source.query = () -> source.open = false;
        f.check(EssentiaTransfers.pullOne(jar, Direction.UP, Aspect.WATER) == 0 && source.amount == 1,
                "a source query closing the port is revalidated before raw mutation");
        source.open = true;
        TileJar fullJar = jar;
        source.notice = () -> {
            f.check(EssentiaTransfers.isLocked(source) && EssentiaTransfers.isLocked(fullJar)
                    && source.amount == 0 && fullJar.getAmount() == 64
                    && EssentiaTransfers.pullOne(fullJar, Direction.UP, Aspect.WATER) == 0
                    && !fullJar.takeFromContainer(Aspect.WATER, 1) && fullJar.addToContainer(Aspect.WATER, 1) == 1,
                    "notification observes the paid pair with both actors locked against reentry");
            throw new IllegalStateException("void-jar-notification-fixture");
        };
        boolean threw = false;
        try { EssentiaTransfers.pullOne(jar, Direction.UP, Aspect.WATER); }
        catch (IllegalStateException expected) { threw = "void-jar-notification-fixture".equals(expected.getMessage()); }
        f.check(threw && source.amount == 0 && jar.getAmount() == 64 && !EssentiaTransfers.isLocked(jar), "post-commit exception never refunds a deleted unit or leaves a lock");
        source.notice = null; source.amount = 1; source.type = Aspect.WATER;
        CompoundTag saved = jar.saveWithFullMetadata(f.level.registryAccess());
        Probe receiver = f.probe(f.jarPos, 64, true);
        f.check(EssentiaTransfers.pullOne(receiver, Direction.UP, Aspect.WATER) == 1 && receiver.amount == 64
                && receiver.notices == 0 && source.amount == 0, "full sink notifies only the changed source, never the unchanged receiver");
        f.restoreJar(saved);
        jar = f.jar(); jar.clearContents(); jar.setFilter(null);
        source.amount = 1; source.type = Aspect.WATER; source.minimum = 99;
        f.jarTicks(5);
        f.check(jar.getAmount() == 0 && source.amount == 1, "empty unlabelled fallback respects source minimum");
        jar.setFilter(Aspect.WATER); f.jarTicks(5);
        f.check(jar.getAmount() == 1 && source.amount == 0, "typed pull preserves original minimum-suction bypass");
        TileJar stale = jar; saved = jar.saveWithFullMetadata(f.level.registryAccess()); f.restoreJar(saved);
        f.check(stale.isRemoved() && !stale.takeFromContainer(Aspect.WATER, 1)
                && EssentiaTransfers.pullOne(stale, Direction.UP, Aspect.WATER) == 0 && f.jar().isVoid(), "replaced identity cannot mutate or transfer");
        f.level.removeBlockEntity(f.tubePos); f.level.setBlock(f.tubePos, Blocks.AIR.defaultBlockState(), QUIET);
    }

    private static void phialsLabelsAndPlacement(Fixture f) {
        var p = f.player; TileJar jar = f.jar();
        jar.clearContents(); jar.setFilter(Aspect.WATER); jar.addToContainer(Aspect.WATER, 8);
        p.setShiftKeyDown(true); p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftContent.EMPTY_PHIAL.get(), 3));
        f.check(f.use(f.jarPos, jar.getFacing()).consumesAction() && jar.getAmount() == 0 && jar.getFilter() == Aspect.WATER
                && f.count(ThaumcraftContent.EMPTY_PHIAL.get()) == 2 && f.count(ThaumcraftContent.FILLED_PHIAL.get()) == 1,
                "real sneaking item dispatch extracts8 BEFORE label removal, paying one empty");
        p.getInventory().clearContent(); p.setGameMode(GameType.CREATIVE);
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemEssence.filled("aqua").copyWithCount(2));
        f.check(f.use(f.jarPos, Direction.UP).consumesAction() && jar.getAmount() == 8
                && f.count(ThaumcraftContent.FILLED_PHIAL.get()) == 1 && f.count(ThaumcraftContent.EMPTY_PHIAL.get()) == 1,
                "secondary-use creative insertion still pays exactly one phial");
        p.setGameMode(GameType.SURVIVAL); p.setShiftKeyDown(false);
        jar.addToContainer(Aspect.WATER, 48); p.setItemInHand(InteractionHand.MAIN_HAND, ItemEssence.filled("aqua"));
        f.check(f.use(f.jarPos, Direction.UP).consumesAction() && jar.getAmount() == 64, "exactly eight free spaces accept a whole phial");
        for (int amount : new int[] {64, 57}) {
            if (jar.getAmount() > amount) jar.takeFromContainer(Aspect.WATER, jar.getAmount() - amount);
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemEssence.filled("aqua")); ItemStack before = p.getMainHandItem().copy();
            f.use(f.jarPos, Direction.UP);
            f.check(jar.getAmount() == amount && ItemStack.matches(before, p.getMainHandItem()), "full/57-unit void refuses phial without partial insertion or deletion");
        }
        jar.clearContents(); jar.addToContainer(Aspect.FIRE, 8);
        for (String tag : new String[] {"aqua", "ignis"}) {
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemEssence.filled(tag)); ItemStack before = p.getMainHandItem().copy(); f.use(f.jarPos, Direction.UP);
            f.check(jar.getAmount() == 8 && jar.getAspect() == Aspect.FIRE && ItemStack.matches(before, p.getMainHandItem()), "manual phial independently checks filter AND stored type");
        }
        jar.clearContents(); jar.addToContainer(Aspect.WATER, 7); p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftContent.EMPTY_PHIAL.get()));
        f.use(f.jarPos, Direction.UP); f.check(jar.getAmount() == 7 && p.getMainHandItem().is(ThaumcraftContent.EMPTY_PHIAL.get()), "seven cannot fill an eight-unit phial");
        jar.addToContainer(Aspect.WATER, 1); f.use(f.jarPos, Direction.UP);
        f.check(jar.getAmount() == 0 && jar.getAspect() == null && jar.getFilter() == Aspect.WATER, "final extraction clears type but keeps label");
        jar.addToContainer(Aspect.WATER, 16); p.setShiftKeyDown(true); p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        f.use(f.jarPos, jar.getFacing().getOpposite());
        f.check(jar.getAmount() == 0 && jar.getAspect() == Aspect.WATER && jar.getFilter() == Aspect.WATER, "other-face sneak dump retains labelled empty type");
        int labels = f.blankDrops(); f.use(f.jarPos, jar.getFacing()); DeferredItemDrops.tick(f.level);
        f.check(jar.getFilter() == null && f.blankDrops() == labels + 1, "label-face removal returns one blank label, not contents");
        p.setShiftKeyDown(false); p.setItemInHand(InteractionHand.MAIN_HAND, ItemJarLabel.blank()); f.use(f.jarPos, Direction.UP);
        f.check(jar.getFilter() == null && !p.getMainHandItem().isEmpty(), "blank cannot bind an empty jar's stale type");
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemJarLabel.bound("ignis")); f.use(f.jarPos, Direction.UP);
        f.check(jar.getAmount() == 0 && jar.getFilter() == Aspect.FIRE && jar.getAspect() == Aspect.FIRE && p.getMainHandItem().isEmpty(), "a paid bound label seeds identity but never essentia");
        p.setShiftKeyDown(true); f.use(f.jarPos, jar.getFacing()); DeferredItemDrops.tick(f.level); p.setShiftKeyDown(false);
        jar.addToContainer(Aspect.WATER, 8); p.setItemInHand(InteractionHand.MAIN_HAND, ItemJarLabel.bound("ignis")); f.use(f.jarPos, Direction.UP);
        f.check(jar.getFilter() == Aspect.WATER && jar.getAmount() == 8, "actual contents override a differently pre-bound label");
        CompoundTag saved = jar.saveWithFullMetadata(f.level.registryAccess()); Direction facing = jar.getFacing(); f.restoreJar(saved);
        f.check(f.jar().getAmount() == 8 && f.jar().getFilter() == Aspect.WATER && f.jar().getFacing() == facing && f.jar().getUpdatePacket() != null, "registered void BE save/load/update preserves contents, filter and facing");
        labels = f.blankDrops(); f.level.destroyBlock(f.jarPos, true, p);
        var drops = f.drops().stream().filter(drop -> ItemJar.isVoid(drop.getItem())).toList();
        f.check(drops.size() == 1 && drops.getFirst().getItem().getMaxStackSize() == 1
                && ItemJar.getAmount(drops.getFirst().getItem()) == 8 && ItemJar.getFilter(drops.getFirst().getItem()) == Aspect.WATER
                && f.blankDrops() == labels, "native break yields one variant/content/filter-bearing nonstackable jar, no loose duplicate label");
        ItemStack item = ItemStack.parse(f.level.registryAccess(), drops.getFirst().getItem().save(f.level.registryAccess())).orElseThrow(); drops.getFirst().discard();
        p.setItemInHand(InteractionHand.MAIN_HAND, item); f.use(f.jarPos.below(), Direction.UP);
        f.check(f.jar().isVoid() && f.jar().getAmount() == 8 && f.jar().getFilter() == Aspect.WATER
                && f.jar().getFacing() == p.getDirection().getOpposite(), "real item placement restores state with fresh player-derived facing");
    }

    private static void heldJars(Fixture f) {
        var p = f.player; TileAlembic source = f.source(); p.getInventory().clearContent(); p.setShiftKeyDown(false);
        ItemStack jar = ItemJar.withContents(new ItemStack(ThaumcraftContent.VOID_JAR.get()), Aspect.WATER, 60);
        jar.set(DataComponents.CUSTOM_NAME, Component.literal("retained-void-components"));
        jar.set(ThaumcraftLabels.JAR_LABEL_DATA.get(), new JarLabelData("aqua"));
        source.addToContainer(Aspect.WATER, 32); p.setItemInHand(InteractionHand.MAIN_HAND, jar);
        f.use(f.sourcePos, Direction.UP);
        f.check(source.getAmount() == 0 && ItemJar.getAmount(p.getMainHandItem()) == 64
                && ItemJar.getFilter(p.getMainHandItem()) == Aspect.WATER && p.getMainHandItem().getHoverName().getString().equals("retained-void-components"), "held60 drains32: stores4, deliberately deletes28, retains components/filter");
        source.addToContainer(Aspect.WATER, 32); source.setFilter(Aspect.WATER); p.setShiftKeyDown(true);
        f.use(f.sourcePos, Direction.DOWN);
        f.check(source.getAmount() == 32 && source.getFilter() == null, "held-jar secondary path preserves alembic label-removal precedence");
        p.setGameMode(GameType.CREATIVE); f.use(f.sourcePos, Direction.UP);
        f.check(source.getAmount() == 0 && ItemJar.getAmount(p.getMainHandItem()) == 64 && p.getMainHandItem().getCount() == 1, "full held void drains all32 through real creative secondary dispatch without a duplicate jar");
        p.setGameMode(GameType.SURVIVAL); p.setShiftKeyDown(false); source.addToContainer(Aspect.WATER, 32);
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemJar.withContents(new ItemStack(ThaumcraftContent.JAR.get()), Aspect.WATER, 60)); f.use(f.sourcePos, Direction.UP);
        f.check(source.getAmount() == 28 && ItemJar.getAmount(p.getMainHandItem()) == 64, "ordinary held jar drains ONLY four free spaces");
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemJar.withContents(new ItemStack(ThaumcraftContent.VOID_JAR.get()), Aspect.FIRE, 8));
        ItemStack before = p.getMainHandItem().copy(); f.use(f.sourcePos, Direction.UP);
        f.check(source.getAmount() == 28 && ItemStack.matches(before, p.getMainHandItem()), "wrong held contents cannot be deleted or exchanged");
        ItemStack filtered = new ItemStack(ThaumcraftContent.VOID_JAR.get()); filtered.set(ThaumcraftLabels.JAR_LABEL_DATA.get(), new JarLabelData("ignis"));
        p.setItemInHand(InteractionHand.MAIN_HAND, filtered); before = filtered.copy(); f.use(f.sourcePos, Direction.UP);
        f.check(source.getAmount() == 28 && ItemStack.matches(before, p.getMainHandItem()), "wrong filter refuses even an empty held void jar");
        source.clearContents();
    }

    private static void rejectedOutput(Fixture f) {
        var p = f.player; TileAlembic source = f.source(); source.setFilter(null); source.addToContainer(Aspect.WATER, 32);
        p.setGameMode(GameType.CREATIVE); p.setShiftKeyDown(true);
        for (int slot = 0; slot < p.getInventory().getContainerSize(); slot++) p.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        ItemStack input = new ItemStack(ThaumcraftContent.VOID_JAR.get(), 2); input.set(DataComponents.CUSTOM_NAME, Component.literal(f.marker));
        p.setItemInHand(InteractionHand.MAIN_HAND, input);
        f.check(f.use(f.sourcePos, Direction.UP).consumesAction() && source.getAmount() == 0 && p.getMainHandItem().getCount() == 1 && f.pendingMarked() == 1, "full CREATIVE inventory still pays one empty jar and durably retains its filled output");
        boolean[] called = {false}, paid = {false};
        EntityEvent.Add listener = (entity, world) -> {
            if (world != f.level || !(entity instanceof ItemEntity drop) || !f.marked(drop.getItem())) return EventResult.pass();
            called[0] = true;
            f.rejectedId = drop.getUUID();
            paid[0] = source.getAmount() == 0 && p.getMainHandItem().getCount() == 1 && ItemJar.getAmount(drop.getItem()) == 32;
            f.use(f.sourcePos, Direction.UP);
            paid[0] &= source.getAmount() == 0 && p.getMainHandItem().getCount() == 1;
            return EventResult.interruptFalse();
        };
        EntityEvent.ADD.register(listener);
        try { DeferredItemDrops.tick(f.level); }
        finally { EntityEvent.ADD.unregister(listener); }
        f.check(called[0] && paid[0] && f.pendingMarked() == 1
                && f.drops().stream().noneMatch(drop -> f.marked(drop.getItem())), "actual cancelled/reentrant native ADD observes paid state and retains ONE pending output");
        f.rejectedAt = f.level.getGameTime();
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper test; final ServerLevel level; final ServerPlayer player;
        final BlockPos jarPos, normalPos, tubePos, sourcePos; final AABB bounds;
        final String marker = "void-jar-output-" + UUID.randomUUID();
        final Map<BlockPos, BlockState> states = new LinkedHashMap<>(); final Map<BlockPos, BlockEntity> originals = new LinkedHashMap<>();
        final List<ItemStack> inventory = new ArrayList<>(); final Set<UUID> oldDrops;
        final Vec3 position, motion; final GameType mode; final CompoundTag abilities = new CompoundTag();
        final int selected; final boolean sneaking; boolean closed; long rejectedAt; UUID rejectedId;
        Fixture(GameTestHelper test, ServerPlayer player) {
            this.test = test; this.level = test.getLevel(); this.player = player;
            jarPos = test.absolutePos(new BlockPos(3, 2, 3)); normalPos = jarPos.west(); tubePos = jarPos.above(); sourcePos = tubePos.east(); bounds = new AABB(jarPos).inflate(4);
            oldDrops = level.getEntitiesOfClass(ItemEntity.class, bounds).stream().map(ItemEntity::getUUID).collect(Collectors.toSet());
            position = player.position(); motion = player.getDeltaMovement(); mode = player.gameMode.getGameModeForPlayer(); selected = player.getInventory().selected; sneaking = player.isShiftKeyDown(); player.getAbilities().addSaveData(abilities);
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) inventory.add(player.getInventory().getItem(slot).copy());
            for (BlockPos mutable : BlockPos.betweenClosed(jarPos.offset(-2, -1, -2), jarPos.offset(2, 2, 1))) {
                BlockPos pos = mutable.immutable(); states.put(pos, level.getBlockState(pos));
                if (level.getBlockEntity(pos) != null) originals.put(pos, level.getBlockEntity(pos));
            }
        }
        void open() {
            for (BlockPos pos : originals.keySet()) level.removeBlockEntity(pos);
            for (BlockPos pos : states.keySet()) level.setBlock(pos, pos.getY() == jarPos.getY() - 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), QUIET);
            player.closeContainer(); player.getInventory().clearContent(); player.getInventory().selected = 0; player.setGameMode(GameType.SURVIVAL); player.setShiftKeyDown(false);
            player.setPos(jarPos.getX() + .5, jarPos.getY(), jarPos.getZ() + 2); player.setDeltaMovement(Vec3.ZERO);
            level.setBlock(jarPos, ThaumcraftContent.VOID_JAR.get().defaultBlockState(), QUIET); level.setBlock(normalPos, ThaumcraftContent.JAR.get().defaultBlockState(), QUIET);
            level.setBlock(tubePos, ThaumcraftTubes.ESSENTIA_TUBE.get().defaultBlockState(), QUIET); level.setBlock(sourcePos, ThaumcraftAlembics.ALEMBIC.get().defaultBlockState(), QUIET);
        }
        TileJar jar() { return (TileJar) level.getBlockEntity(jarPos); }
        TileJar normal() { return (TileJar) level.getBlockEntity(normalPos); }
        TileTube tube() { return (TileTube) level.getBlockEntity(tubePos); }
        TileAlembic source() { return (TileAlembic) level.getBlockEntity(sourcePos); }
        void check(boolean value, String message) { test.assertTrue(value, message); }
        InteractionResult use(BlockPos pos, Direction side) { return player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos).relative(side, .5), side, pos, false)); }
        void jarTicks(int count) { for (int i = 0; i < count; i++) { BlockState state = level.getBlockState(jarPos); var ticker = ((EntityBlock) state.getBlock()).getTicker(level, state, ThaumcraftContent.JAR_BLOCK_ENTITY.get()); if (ticker == null) throw new IllegalStateException("Missing real jar ticker"); ticker.tick(level, jarPos, state, jar()); } }
        void restoreJar(CompoundTag saved) { level.removeBlockEntity(jarPos); BlockEntity restored = ((EntityBlock) level.getBlockState(jarPos).getBlock()).newBlockEntity(jarPos, level.getBlockState(jarPos)); if (restored == null) throw new IllegalStateException("Missing jar factory"); restored.loadWithComponents(saved, level.registryAccess()); level.setBlockEntity(restored); }
        Probe probe(BlockPos pos, int amount, boolean sink) { level.removeBlockEntity(pos); level.setBlock(pos, (pos.equals(jarPos) ? ThaumcraftContent.VOID_JAR : ThaumcraftContent.JAR).get().defaultBlockState(), QUIET); level.removeBlockEntity(pos); Probe probe = new Probe(pos, level.getBlockState(pos), amount, sink); level.setBlockEntity(probe); return probe; }
        int count(net.minecraft.world.item.Item item) { int count = 0; for (int i = 0; i < player.getInventory().getContainerSize(); i++) if (player.getInventory().getItem(i).is(item)) count += player.getInventory().getItem(i).getCount(); return count; }
        List<ItemEntity> drops() { return level.getEntitiesOfClass(ItemEntity.class, bounds).stream().filter(drop -> !oldDrops.contains(drop.getUUID())).toList(); }
        int blankDrops() { return drops().stream().filter(drop -> drop.getItem().getItem() instanceof ItemJarLabel && ItemJarLabel.aspect(drop.getItem()) == null).mapToInt(drop -> drop.getItem().getCount()).sum(); }
        boolean marked(ItemStack stack) { return marker.equals(stack.getHoverName().getString()); }
        int pendingMarked() { var queue = level.getDataStorage().get(QUEUE, "thaumcraft_pending_inventory_drops"); if (queue == null) return 0; int count = 0; for (Tag raw : queue.save(new CompoundTag(), level.registryAccess()).getList("Pending", Tag.TAG_COMPOUND)) { var row = (CompoundTag) raw; ItemStack stack = ItemStack.parse(level.registryAccess(), row.getCompound("Stack")).orElse(ItemStack.EMPTY); if (marked(stack)) count += row.getInt("Count"); } return count; }
        @Override public void close() {
            if (closed) return; closed = true;
            drops().forEach(ItemEntity::discard);
            // On assertion failure discard only this fixture's still-pending
            // output, never an unrelated queue entry or the entire SavedData.
            var queue = level.getDataStorage().get(QUEUE, "thaumcraft_pending_inventory_drops");
            if (queue != null && pendingMarked() > 0) {
                CompoundTag saved = queue.save(new CompoundTag(), level.registryAccess()); var pending = saved.getList("Pending", Tag.TAG_COMPOUND);
                for (int i = pending.size() - 1; i >= 0; i--) if (marked(ItemStack.parse(level.registryAccess(), pending.getCompound(i).getCompound("Stack")).orElse(ItemStack.EMPTY))) pending.remove(i);
                level.getDataStorage().set("thaumcraft_pending_inventory_drops", DeferredItemDrops.load(saved, level.registryAccess()));
            }
            if (level.getBlockEntity(sourcePos) instanceof TileAlembic source) source.setFilter(null);
            for (BlockPos pos : states.keySet()) level.removeBlockEntity(pos);
            for (var entry : states.entrySet()) level.setBlock(entry.getKey(), entry.getValue(), QUIET);
            for (BlockEntity original : originals.values()) { original.clearRemoved(); level.setBlockEntity(original); }
            player.getInventory().clearContent(); for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().setItem(slot, inventory.get(slot));
            player.getInventory().selected = selected; player.setGameMode(mode); player.getAbilities().loadSaveData(abilities); player.onUpdateAbilities(); player.setShiftKeyDown(sneaking); player.setPos(position); player.setDeltaMovement(motion);
        }
    }

    /** Callback-only adversarial adapter; never left installed across a world tick. */
    private static final class Probe extends BlockEntity implements EssentiaStorageEndpoint {
        Aspect type = Aspect.WATER; int amount, minimum, notices; boolean open = true; final boolean sink; Runnable query, notice;
        Probe(BlockPos pos, BlockState state, int amount, boolean sink) { super(ThaumcraftContent.JAR_BLOCK_ENTITY.get(), pos, state); this.amount = amount; this.sink = sink; }
        @Override public Aspect getStoredEssentiaType() { return type; }
        @Override public int getStoredEssentiaAmount() { return amount; }
        @Override public int getEssentiaCapacity() { return 64; }
        @Override public boolean allowsEssentiaOverflow() { return sink; }
        @Override public void setEssentiaStorageRaw(Aspect type, int amount) { EssentiaTransfers.requireMutation(this); this.type = type; this.amount = amount; }
        @Override public void notifyEssentiaStorageChanged() { notices++; if (notice != null) notice.run(); }
        @Override public boolean isConnectable(Direction face) { return face != null && open; }
        @Override public boolean canInputFrom(Direction face) { return isConnectable(face); }
        @Override public boolean canOutputTo(Direction face) { return isConnectable(face); }
        @Override public void setSuction(Aspect aspect, int amount) {}
        @Override public Aspect getSuctionType(Direction face) { return null; }
        @Override public int getSuctionAmount(Direction face) { return 0; }
        @Override public Aspect getEssentiaType(Direction face) { if (query != null) { Runnable action = query; query = null; action.run(); } return type; }
        @Override public int getEssentiaAmount(Direction face) { return amount; }
        @Override public int takeEssentia(Aspect aspect, int amount, Direction face) { throw new AssertionError("Callback-bearing take must never implement atomic transfer"); }
        @Override public int addEssentia(Aspect aspect, int amount, Direction face) { throw new AssertionError("Callback-bearing add must never implement atomic transfer"); }
        @Override public int getMinimumSuction() { return minimum; }
        @Override public boolean renderExtendedTube() { return false; }
    }
}
