package thaumcraft.common.testing;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.common.blocks.BlockTube;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.items.ItemJarLabel;
import thaumcraft.common.lib.essentia.EssentiaStorageEndpoint;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.lib.essentia.TubeGeometry;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileTube;

/** M06b label boundaries; raw label/essentia fixtures are not acquired survival
 * inputs. All paid interactions use a real connected native actor. Root owns
 * wrappers/execution. No production recipes, clocks, RNG or counters are edited. */
public final class TubeFilterLabelRuntimeChecks {
    private TubeFilterLabelRuntimeChecks() {}

    public static void labelsPayPreserveMarkerAndFenceCallbacks(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            f.open(); angledCameraAttachment(f); paymentAndMarker(f); authority(f); callbacks(f);
        }
        test.succeed();
    }

    /** Uses ordinary level-post retries, not a manual flush/clock jump. Wrapper timeout >=200 ticks. */
    public static void labelReturnsSurviveNativeCancellationAndFailure(GameTestHelper test) {
        var f = new Fixture(test); var attempts = new LinkedHashMap<UUID, Return>();
        EntityEvent.Add listener = (entity, world) -> {
            if (world != f.level || !(entity instanceof ItemEntity drop)) return EventResult.pass();
            Return output = attempts.get(drop.getUUID()); if (output == null || output.rejectedAt >= 0) return EventResult.pass();
            output.rejectedAt = f.level.getGameTime(); output.attempts++;
            f.check(output.old.getFilter() == null && ItemStack.matches(drop.getItem(), ItemJarLabel.blank()), "native ADD observes already-cleared label ownership and ONE genuine blank, not a bound label/filled tube");
            int queued = f.pendingRows().size();
            try {
                f.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK)); f.player.setShiftKeyDown(true); f.aim(Vec3.atCenterOf(output.pos));
                BlockTube.tryLabelInteraction(f.context(output.pos));
                f.check(output.old.getFilter() == null && f.pendingRows().size() == queued, "cancelled/reentrant native ADD cannot enqueue a second return from the old filter");
                if (output.failure) {
                    // A genuine replacement while the deferred output is in
                    // flight must not rediscover the old filter's ownership.
                    f.level.setBlock(output.pos, Blocks.STONE.defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
                    f.check(f.pendingRows().size() == queued && output.old.getFilter() == null, "native replacement during throwing ADD cannot duplicate a cleared return");
                    throw new IllegalStateException("tube-filter-ADD-failure-fixture");
                }
                return EventResult.interruptFalse();
            } finally { park(f); }
        };
        boolean[] registered = {false};
        Runnable close = () -> { if (registered[0]) { EntityEvent.ADD.unregister(listener); registered[0] = false; } f.close(); };
        try {
            f.open(); f.reset();
            for (int index = 0; index < 3; index++) {
                BlockPos pos = index == 0 ? f.base : index == 1 ? f.base.east(2) : f.base.above(2);
                TileTube tube = f.tube(pos, BlockTube.Kind.FILTER); f.mode(index == 0 ? GameType.CREATIVE : GameType.SURVIVAL); f.bind(tube, Aspect.WATER);
                if (index == 2) {
                    tube.addEssentia(Aspect.FIRE, 1, Direction.SOUTH);
                    f.check(f.level.destroyBlock(pos, true, f.player), "actual break of a configured, incompatibly buffered filter is an owned output fixture");
                } else {
                    f.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK)); f.player.setShiftKeyDown(true); f.aim(Vec3.atCenterOf(pos));
                    f.check(f.use(pos).consumesAction(), "native sneak removal creates the durable return before any ADD callback");
                }
                var rows = f.pendingRows().stream().filter(row -> BlockPos.of(row.getLong("Source")).equals(pos)).toList();
                f.check(tube.getFilter() == null && rows.size() == 1, "each removal/break clears its old owner and queues exactly one durable blank");
                var row = rows.getFirst(); ItemStack stack = ItemStack.parse(f.level.registryAccess(), row.getCompound("Stack")).orElseThrow();
                f.check(row.getInt("Count") == 1 && ItemStack.matches(stack, ItemJarLabel.blank()), "durable payload stores precisely one original plain blank label");
                attempts.put(row.getUUID("Id"), new Return(pos, tube, index == 1));
            }
            f.check(f.pendingRows().size() == 3 && f.drops().stream().filter(drop -> drop.getItem().is(ThaumcraftTubes.ESSENTIA_FILTER_ITEM.get())).count() == 1,
                    "two removals plus one native break own exactly three queued blanks and one plain filter item");
            EntityEvent.ADD.register(listener); registered[0] = true; park(f);
            test.onEachTick(() -> {
                if (f.closed) return;
                try {
                    var rows = f.pendingRows(); boolean complete = true;
                    f.check(rows.stream().allMatch(row -> attempts.containsKey(row.getUUID("Id"))), "no reentrant operation creates an extra pending return ID");
                    for (var entry : attempts.entrySet()) {
                        UUID id = entry.getKey(); Return expected = entry.getValue();
                        long pending = rows.stream().filter(row -> row.getUUID("Id").equals(id)).count();
                        var entity = f.level.getEntity(id); boolean visible = entity instanceof ItemEntity drop && !drop.isRemoved();
                        f.check(pending + (visible ? 1 : 0) == 1 && expected.old.getFilter() == null, "every paid return has exactly ONE pending-or-visible owner throughout native cancellation/retry");
                        if (visible) {
                            var drop = (ItemEntity) entity;
                            f.check(expected.attempts == 1 && expected.rejectedAt >= 0 && f.level.getGameTime() - expected.rejectedAt >= 20
                                    && ItemStack.matches(drop.getItem(), ItemJarLabel.blank()), "ordinary level-post retries the SAME rejected UUID after >=20 real native ticks, once, with the original blank payload");
                        } else complete = false;
                    }
                    if (!complete) return;
                    f.check(rows.isEmpty(), "successful actual delivery leaves no duplicate pending label");
                    close.run(); test.succeed();
                } catch (RuntimeException | Error failure) { close.run(); throw failure; }
            });
            test.runAfterDelay(140, () -> {
                if (f.closed) return;
                String detail = attempts.entrySet().stream().map(entry -> entry.getKey() + ":firstFailure=" + entry.getValue().rejectedAt + ",attempts=" + entry.getValue().attempts).toList().toString();
                close.run(); test.fail("filter labels did not retry through ordinary native level-post within140 ticks; " + detail);
            });
        } catch (RuntimeException | Error failure) { close.run(); throw failure; }
    }

    private static void angledCameraAttachment(Fixture f) {
        f.reset(); TileTube tube = f.tube(f.base, BlockTube.Kind.FILTER);
        f.put(f.base.west(), thaumcraft.common.config.ThaumcraftAlembics.ALEMBIC.get());
        f.tube(f.base.east(), BlockTube.Kind.RESTRICT); f.tube(f.base.east(2), BlockTube.Kind.ONEWAY);
        f.put(f.base.east(2).below(), ThaumcraftContent.JAR.get());
        f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemJarLabel.bound("potentia"));
        f.player.setPos(f.base.getX() + 1.4, f.base.getY() - 1, f.base.getZ() + 3.1);
        Vec3 point = Vec3.atCenterOf(f.base).add(0, 0, .16), delta = point.subtract(f.player.getEyePosition());
        f.player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        // A native living actor's view yaw is its HEAD, not just body yaw.
        // Match Fixture.aim's complete pose setup for this angled fixture.
        f.player.setYHeadRot(f.player.getYRot()); f.player.yHeadRotO = f.player.getYRot();
        f.player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z))));
        var first = f.level.clip(new net.minecraft.world.level.ClipContext(f.player.getEyePosition(),
                f.player.getEyePosition().add(f.player.getViewVector(1).scale(f.player.blockInteractionRange())),
                net.minecraft.world.level.ClipContext.Block.OUTLINE, net.minecraft.world.level.ClipContext.Fluid.NONE, f.player));
        f.check(TubeGeometry.hitsBlock(f.player, tube), "actual angled client-camera ray hits the filter outline, first=" + first.getBlockPos() + "/" + first.getLocation());
        var result = f.player.gameMode.useItemOn(f.player, f.level, f.player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(point, Direction.SOUTH, f.base, false));
        f.check(result.consumesAction() && tube.getFilter() == Aspect.ENERGY && f.player.getMainHandItem().isEmpty(),
                "native angled camera attachment pays its actual bound label on the connected three-variant network");
    }

    private static void paymentAndMarker(Fixture f) {
        f.reset(); TileTube tube = f.tube(f.base, BlockTube.Kind.FILTER); tube.addEssentia(Aspect.FIRE, 1, Direction.UP);
        f.check(tube instanceof IAspectContainer && ((IAspectContainer) tube).getAspects() == null, "blank filter alone exposes the original null marker; raw stored fire is NOT that marker");
        for (GameType mode : List.of(GameType.SURVIVAL, GameType.CREATIVE)) for (boolean secondary : new boolean[] {false, true}) {
            f.mode(mode); f.player.setShiftKeyDown(secondary); f.aim(Vec3.atCenterOf(f.base));
            ItemStack blank = ItemJarLabel.blank().copyWithCount(3); f.player.setItemInHand(InteractionHand.MAIN_HAND, blank); var before = tube.saveWithFullMetadata(f.level.registryAccess());
            f.use(f.base);
            f.check(tube.getFilter() == null && ItemStack.matches(f.player.getMainHandItem(), ItemJarLabel.blank().copyWithCount(3))
                    && before.equals(tube.saveWithFullMetadata(f.level.registryAccess())), "blank label never infers from the existing fire buffer, in real survival/creative and primary/secondary dispatch");
            ItemStack bound = ItemJarLabel.bound("aqua").copyWithCount(3); bound.set(DataComponents.CUSTOM_NAME, Component.literal("paid-filter-label-components"));
            ItemStack remainder = bound.copyWithCount(2); f.player.setItemInHand(InteractionHand.MAIN_HAND, bound);
            f.check(f.use(f.base).consumesAction() && tube.getFilter() == Aspect.WATER && tube.getEssentiaType(null) == Aspect.FIRE && tube.getEssentiaAmount(null) == 1
                    && f.player.getMainHandItem() != bound && ItemStack.matches(f.player.getMainHandItem(), remainder),
                    "one ACTUAL bound label pays even in creative Item.useOn, detaches the hand and preserves components/incompatible raw stock: " + mode + "/secondary=" + secondary);
            markerStubs(f, tube);
            f.player.setShiftKeyDown(false); var existing = f.player.getMainHandItem().copy();
            f.use(f.base); f.check(tube.getFilter() == Aspect.WATER && ItemStack.matches(existing, f.player.getMainHandItem()), "non-sneak reattachment cannot consume another label or replace a configured filter");
            for (boolean sneak : new boolean[] {false, true}) {
                f.player.setShiftKeyDown(sneak); ItemStack resonator = new ItemStack(ThaumcraftContent.RESONATOR.get()); f.player.setItemInHand(InteractionHand.MAIN_HAND, resonator);
                var saved = tube.saveWithFullMetadata(f.level.registryAccess()); int pending = f.pendingRows().size(); f.chat();
                f.check(f.use(f.base).consumesAction(), "real resonator item-first route handles configured filters before label removal, including secondary");
                var messages = f.chat();
                f.check(messages.size() == 2 && messages.getFirst().getContents() instanceof TranslatableContents contents
                        && contents.getKey().equals("tc.resonator1") && contents.getArgs()[0].equals("1") && contents.getArgs()[1].equals(Aspect.FIRE.getName())
                        && messages.getLast().getContents() instanceof TranslatableContents suction && suction.getKey().equals("tc.resonator2")
                        && saved.equals(tube.saveWithFullMetadata(f.level.registryAccess())) && f.pendingRows().size() == pending
                        && f.player.getMainHandItem() == resonator, "resonator reports actual fire1 and one suction line, never filter water/-1, never removes/charges/mutates the item or label");
            }
            f.player.setShiftKeyDown(true); ItemStack different = ItemJarLabel.bound("ignis").copyWithCount(2); f.player.setItemInHand(InteractionHand.MAIN_HAND, different);
            int blanks = f.blankDrops();
            f.check(f.use(f.base).consumesAction() && tube.getFilter() == null && f.player.getMainHandItem() == different
                    && ItemStack.matches(f.player.getMainHandItem(), ItemJarLabel.bound("ignis").copyWithCount(2))
                    && tube.getEssentiaType(null) == Aspect.FIRE && tube.getEssentiaAmount(null) == 1 && f.pendingRows().size() == 1,
                    "sneak removal PRECEDES different-label attachment, pays nothing again and returns one blank while retaining raw stock");
            f.flushFresh(); f.check(f.blankDrops() == blanks + 1 && ((IAspectContainer) tube).getAspects() == null, "one actual native blank arrives and the original marker returns to null");
            f.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK)); f.use(f.base);
            f.check(tube.getFilter() == null && f.pendingRows().isEmpty() && f.blankDrops() == blanks + 1, "replayed removal from an unconfigured filter cannot create another blank");
        }
        f.mode(GameType.CREATIVE); f.player.setShiftKeyDown(true); f.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        ItemStack offhand = ItemJarLabel.bound("aqua").copyWithCount(3); f.player.setItemInHand(InteractionHand.OFF_HAND, offhand);
        f.check(f.use(f.base, Direction.SOUTH, InteractionHand.OFF_HAND).consumesAction() && tube.getFilter() == Aspect.WATER
                && f.player.getOffhandItem() != offhand && ItemStack.matches(f.player.getOffhandItem(), ItemJarLabel.bound("aqua").copyWithCount(2))
                && f.player.getMainHandItem().is(Items.STICK), "creative SECONDARY native item dispatch pays exactly one from the actual OFF hand, without touching the other hand");
        f.player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY); f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        f.check(f.use(f.base).consumesAction() && tube.getFilter() == null, "native empty-hand sneak route also removes a filter label"); f.flushFresh();
        f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemJarLabel.bound("aqua").copyWithCount(3));
        f.check(BlockTube.tryLabelInteraction(f.context(f.base)).consumesAction() && tube.getFilter() == Aspect.WATER && f.player.getMainHandItem().getCount() == 2,
                "public BlockTube.tryLabelInteraction uses the same paid validated helper, including creative detachment");
    }

    private static void markerStubs(Fixture f, TileTube tube) {
        var container = (IAspectContainer) tube; var marker = container.getAspects();
        f.check(marker != null && marker.size() == 1 && marker.getAmount(Aspect.WATER) == -1 && marker.getAmount(Aspect.FIRE) == 0, "getAspects exposes exactly {configuredFilter:-1}, not the incompatible stored buffer");
        marker.aspects.clear(); container.setAspects(new AspectList().add(Aspect.ENERGY, 99));
        f.check(!container.doesContainerAccept(Aspect.WATER) && container.addToContainer(Aspect.WATER, 10) == 0
                && !container.takeFromContainer(Aspect.WATER, 1) && !container.takeFromContainer(new AspectList().add(Aspect.WATER, 1))
                && !container.doesContainerContainAmount(Aspect.WATER, -1) && !container.doesContainerContain(new AspectList().add(Aspect.WATER, 1))
                && container.containerContains(Aspect.WATER) == 0 && tube.getFilter() == Aspect.WATER
                && tube.getEssentiaType(null) == Aspect.FIRE && tube.getStoredEssentiaAmount() == 1 && container.getAspects().getAmount(Aspect.WATER) == -1,
                "original container stubs and mutating a returned marker cannot set filters, mint essentia or change atomic storage");
    }

    private static void authority(Fixture f) {
        f.reset(); TileTube tube = f.tube(f.base, BlockTube.Kind.FILTER); f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemJarLabel.bound("aqua").copyWithCount(3));
        boolean rawRefused = false;
        try { tube.setFilterRaw(Aspect.WATER); } catch (IllegalStateException expected) { rawRefused = true; }
        f.check(rawRefused && tube.getFilter() == null && f.player.getMainHandItem().getCount() == 3, "raw filter hook cannot bypass payment outside its authorized label mutation");
        var stale = f.context(f.base); f.player.setItemInHand(InteractionHand.MAIN_HAND, f.player.getMainHandItem().copy());
        unchangedRefusal(f, tube, stale, "detached/stale previous hand identity");
        f.check(tube.getBlockState().useItemOn(f.player.getMainHandItem().copy(), f.level, f.player, InteractionHand.MAIN_HAND, f.hit(f.base, Direction.SOUTH)) == ItemInteractionResult.FAIL
                && tube.getFilter() == null && f.player.getMainHandItem().getCount() == 3, "native early block dispatch rejects a fabricated held-stack argument too");
        ServerLevel other = f.level.getServer().getLevel(Level.NETHER);
        if (other != null && other != f.level) unchangedRefusal(f, tube, new DetachedContext(other, f.player, f.player.getMainHandItem(), f.hit(f.base, Direction.SOUTH)), "actor/context world mismatch");
        f.player.setPos(f.player.position().add(0, 0, 20)); unchangedRefusal(f, tube, f.context(f.base), "actual out-of-reach actor");
        f.aim(Vec3.atCenterOf(f.base)); f.level.setBlock(f.base.south(), Blocks.STONE.defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
        unchangedRefusal(f, tube, f.context(f.base), "intervening native occlusion despite a supplied target hit");
        f.level.setBlock(f.base.south(), Blocks.AIR.defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
        for (BlockTube.Kind kind : List.of(BlockTube.Kind.BASIC, BlockTube.Kind.VALVE, BlockTube.Kind.RESTRICT, BlockTube.Kind.ONEWAY)) {
            TileTube otherTube = f.tube(f.base.east(), kind); f.aim(Vec3.atCenterOf(otherTube.getBlockPos()));
            ItemStack held = f.player.getMainHandItem().copy();
            f.check(BlockTube.tryLabelInteraction(f.context(otherTube.getBlockPos())) == InteractionResult.PASS
                    && !(otherTube instanceof IAspectContainer) && otherTube.getFilter() == null && ItemStack.matches(held, f.player.getMainHandItem()), "non-filter variants do not acquire the marker/container API or a paid filter label: " + kind);
        }
    }

    private static void unchangedRefusal(Fixture f, TileTube tube, UseOnContext context, String why) {
        var saved = tube.saveWithFullMetadata(f.level.registryAccess()); var held = f.player.getMainHandItem().copy(); int queued = f.pendingRows().size();
        f.check(!BlockTube.tryLabelInteraction(context).consumesAction() && saved.equals(tube.saveWithFullMetadata(f.level.registryAccess()))
                && ItemStack.matches(held, f.player.getMainHandItem()) && f.pendingRows().size() == queued, "validated label refusal preserves both resources and creates no return: " + why);
    }

    private static void callbacks(Fixture f) {
        f.reset(); TileTube tube = f.tube(f.base, BlockTube.Kind.FILTER); f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemJarLabel.bound("aqua").copyWithCount(3));
        BlockPos sourcePos = f.base.east(); f.put(sourcePos, ThaumcraftTubes.ESSENTIA_TUBE.get()); f.level.removeBlockEntity(sourcePos);
        var source = new Probe(sourcePos, f.level.getBlockState(sourcePos)); f.level.setBlockEntity(source);
        int[] fenced = {0}; Runnable lockedAttempt = () -> {
            f.check(EssentiaTransfers.isLocked(tube) && EssentiaTransfers.isLocked(source), "native transfer holds both shared endpoint locks during sided query/notification");
            f.check(BlockTube.tryLabelInteraction(f.context(f.base)) == InteractionResult.FAIL && tube.getFilter() == null
                    && f.player.getMainHandItem().getCount() == 3 && f.pendingRows().isEmpty(), "filter-label mutation cannot enter a shared atomic storage callback"); fenced[0]++;
            boolean rawRefused = false;
            try { tube.setFilterRaw(Aspect.WATER); } catch (IllegalStateException expected) { rawRefused = true; }
            f.check(rawRefused && tube.getFilter() == null, "even an active STORAGE mutation cannot authorize unpaid filter ownership writes");
        };
        source.query = lockedAttempt; source.notice = lockedAttempt;
        f.check(EssentiaTransfers.pullOne(tube, Direction.EAST, Aspect.WATER) == 1 && fenced[0] == 2 && tube.getEssentiaAmount(null) == 1 && source.amount == 0,
                "real paired transfer conserves its unit while fencing BOTH query- and notification-time label reentry");
        f.mode(GameType.CREATIVE); f.player.setShiftKeyDown(true); f.aim(Vec3.atCenterOf(f.base));
        ItemStack input = f.player.getMainHandItem(); var menu = f.player.containerMenu; int[] called = {0};
        f.player.containerMenu = new ObserverMenu(() -> {
            called[0]++;
            f.check(tube.getFilter() == Aspect.WATER && f.player.getMainHandItem() != input && f.player.getMainHandItem().getCount() == 2
                    && EssentiaTransfers.isLocked(tube), "FIRST native inventory-notification callback sees the paid DETACHED hand and configured filter together, under the shared lock");
            f.check(BlockTube.tryLabelInteraction(f.context(f.base)) == InteractionResult.FAIL && tube.getFilter() == Aspect.WATER
                    && f.pendingRows().isEmpty() && tube.takeEssentia(Aspect.WATER, 1, Direction.SOUTH) == 0, "attachment notification cannot reenter removal or raw storage before lock release");
            throw new IllegalStateException("tube-filter-attachment-callback-fixture");
        });
        boolean failed = false;
        try { f.use(f.base); } catch (IllegalStateException expected) { failed = "tube-filter-attachment-callback-fixture".equals(expected.getMessage()); }
        finally { f.player.containerMenu = menu; }
        f.check(failed && called[0] == 1 && tube.getFilter() == Aspect.WATER && f.player.getMainHandItem().getCount() == 2
                && tube.getEssentiaAmount(null) == 1 && !EssentiaTransfers.isLocked(tube), "throwing post-commit native callback cannot refund/duplicate a paid label, purge stock or leak the lock");

        // Removal changes no hand inventory and need not broadcast a menu. Its
        // real deferred ADD callback is covered by the separate asynchronous
        // case. Here observe the harder nested-retirement path: a paid attach
        // notification replaces its OWN configured BE under the outer lock.
        f.mode(GameType.SURVIVAL); f.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK)); f.player.setShiftKeyDown(true);
        f.check(f.use(f.base).consumesAction() && tube.getFilter() == null && f.pendingRows().size() == 1, "ordinary removal first clears and durably returns the paid label after the failed attachment callback");
        f.flushFresh(); f.check(f.blankDrops() == 1, "the first paid label returns once, despite its attachment notification failure");
        f.mode(GameType.CREATIVE); f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemJarLabel.bound("aqua").copyWithCount(3)); f.player.setShiftKeyDown(true); called[0] = 0;
        f.player.containerMenu = new ObserverMenu(() -> {
            called[0]++;
            f.check(tube.getFilter() == Aspect.WATER && f.player.getMainHandItem().getCount() == 2 && EssentiaTransfers.isLocked(tube), "replacement callback begins with an already-paid filter and the original attachment lock");
            f.level.setBlock(f.base, Blocks.STONE.defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
            f.level.setBlock(f.base, ThaumcraftTubes.ESSENTIA_FILTER.get().defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
            f.check(tube.getFilter() == null && f.pendingRows().size() == 1 && ((TileTube) f.level.getBlockEntity(f.base)).getFilter() == null,
                    "native replacement/onRemove may retire the paid owner inside a callback, but clears it before ONE durable return");
            f.check(BlockTube.tryLabelInteraction(f.context(f.base)) == InteractionResult.FAIL && f.player.getMainHandItem().getCount() == 2
                    && ((TileTube) f.level.getBlockEntity(f.base)).getFilter() == null && f.pendingRows().size() == 1,
                    "shared outer mutation fences reattachment even to a DIFFERENT replacement BE, without another debit or return");
            throw new IllegalStateException("tube-filter-replacement-callback-fixture");
        });
        failed = false;
        try { f.use(f.base); } catch (IllegalStateException expected) { failed = "tube-filter-replacement-callback-fixture".equals(expected.getMessage()); }
        finally { f.player.containerMenu = menu; }
        f.check(failed && called[0] == 1 && tube.isRemoved() && tube.getFilter() == null && f.pendingRows().size() == 1
                && f.player.getMainHandItem().getCount() == 2 && !EssentiaTransfers.isLocked(tube), "throwing replacement callback leaves ONE durable blank, the detached paid hand, no old ownership and no leaked lock");
        f.flushFresh(); f.check(f.blankDrops() == 2, "post-replacement failure delivers exactly one more blank for the second actually paid label");
        f.player.setShiftKeyDown(false);
        f.check(f.use(f.base).consumesAction() && ((TileTube) f.level.getBlockEntity(f.base)).getFilter() == Aspect.WATER && f.player.getMainHandItem().getCount() == 1,
                "all nested mutation frames unwind after callback failure, allowing the next independently paid attachment normally");
    }

    private static void park(Fixture f) { f.player.setPos(Vec3.atCenterOf(f.base).add(6, 2, 6)); f.player.setDeltaMovement(Vec3.ZERO); }
    private static final class Return {
        final BlockPos pos; final TileTube old; final boolean failure; long rejectedAt = -1; int attempts;
        Return(BlockPos pos, TileTube old, boolean failure) { this.pos = pos; this.old = old; this.failure = failure; }
    }
    /** Observes a REAL production call-out; never invokes its action manually. */
    private static final class ObserverMenu extends AbstractContainerMenu {
        private Runnable action;
        ObserverMenu(Runnable action) { super(null, 97); this.action = action; }
        @Override public void broadcastChanges() { if (action != null) { Runnable next = action; action = null; next.run(); } }
        @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
        @Override public boolean stillValid(Player player) { return true; }
    }
    private static final class DetachedContext extends UseOnContext {
        DetachedContext(Level level, ServerPlayer player, ItemStack stack, BlockHitResult hit) { super(level, player, InteractionHand.MAIN_HAND, stack, hit); }
    }
    /** Synchronous adversarial endpoint only, never left across a native world tick. */
    private static final class Probe extends BlockEntity implements EssentiaStorageEndpoint {
        int amount = 1; Runnable query, notice;
        Probe(BlockPos pos, BlockState state) { super(ThaumcraftTubes.TUBE_BLOCK_ENTITY.get(), pos, state); }
        @Override public Aspect getStoredEssentiaType() { return amount > 0 ? Aspect.WATER : null; }
        @Override public int getStoredEssentiaAmount() { return amount; }
        @Override public int getEssentiaCapacity() { return 1; }
        @Override public void setEssentiaStorageRaw(Aspect aspect, int amount) { EssentiaTransfers.requireMutation(this); this.amount = amount; }
        @Override public void notifyEssentiaStorageChanged() { if (notice != null) notice.run(); }
        @Override public boolean isConnectable(Direction face) { return face != null; }
        @Override public boolean canInputFrom(Direction face) { return face != null; }
        @Override public boolean canOutputTo(Direction face) { return face != null; }
        @Override public void setSuction(Aspect aspect, int amount) {}
        @Override public Aspect getSuctionType(Direction face) { return null; }
        @Override public int getSuctionAmount(Direction face) { return 0; }
        @Override public Aspect getEssentiaType(Direction face) { if (query != null) { Runnable once = query; query = null; once.run(); } return getStoredEssentiaType(); }
        @Override public int getEssentiaAmount(Direction face) { return amount; }
        @Override public int getMinimumSuction() { return 0; }
        @Override public boolean renderExtendedTube() { return false; }
        @Override public int takeEssentia(Aspect aspect, int amount, Direction face) { throw new AssertionError("No callback take/add transfer"); }
        @Override public int addEssentia(Aspect aspect, int amount, Direction face) { throw new AssertionError("No callback take/add transfer"); }
    }
}
