package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.config.ThaumcraftVillageProfessions;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.entities.golems.GolemHelper;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemMarker;
import thaumcraft.common.items.golems.GolemUpgrade;
import thaumcraft.common.items.golems.GolemUseOptions;
import thaumcraft.common.lib.world.VillageBankerManager;
import thaumcraft.common.lib.world.VillageWizardManager;

/** Actual merchant menus, ordinary golem ticks and native unloaded-chunk witnesses. */
public final class TailGolemVillageReviewRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final ResourceLocation WATER = ResourceLocation.withDefaultNamespace("water");
    private static final TicketType<ChunkPos> OWNER = TicketType.create("thaumcraft_review_use_owner",
            Comparator.comparingLong(ChunkPos::toLong));

    private TailGolemVillageReviewRuntimeChecks() {}

    /** New offers must let a real merchant menu charge the first payment and deliver its reward. */
    public static void freshVillageOffersCanActuallyTrade(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            List<String> observations = new ArrayList<>();
            boolean banker = tradeFirstOffer(test, f, ThaumcraftVillageProfessions.BANKER.get(),
                    VillageBankerManager.trades().size(), observations);
            boolean wizard = tradeFirstOffer(test, f, ThaumcraftVillageProfessions.WIZARD.get(),
                    VillageWizardManager.trades().size(), observations);
            test.assertTrue(banker && wizard,
                    "fresh banker and wizard offers each pay for one actual native menu trade: " + observations);
        }
        test.succeed();
    }

    private static boolean tradeFirstOffer(GameTestHelper test, TubeVariantsRuntimeChecks.Fixture f,
                                            VillagerProfession profession, int expectedCount,
                                            List<String> observations) {
        Villager villager = EntityType.VILLAGER.create(f.level);
        test.assertTrue(villager != null, "native villager is available");
        villager.setVillagerData(villager.getVillagerData().setProfession(profession));
        villager.setNoAi(true);
        villager.setPersistenceRequired();
        villager.setPos(Vec3.atBottomCenterOf(f.base));
        test.assertTrue(f.level.addFreshEntity(villager), "real profession villager is admitted");
        MerchantMenu menu = null;
        try {
            var offers = villager.getOffers(); // Exercises VillagerVillageTradesMixin, not apply() directly.
            test.assertTrue(offers.size() == expectedCount && !offers.isEmpty(),
                    profession + " builds its actual complete native offer list: " + offers.size());
            var offer = offers.getFirst();
            ItemStack payment = offer.getCostA().copy(), reward = offer.getResult().copy();
            test.assertTrue(!payment.isEmpty() && !reward.isEmpty(), "first offer resolves real registered stacks");
            boolean available = !offer.isOutOfStock();
            villager.setTradingPlayer(f.player);
            menu = new MerchantMenu(781, f.player.getInventory(), villager);
            f.player.containerMenu = menu;
            menu.setSelectionHint(0);
            menu.getSlot(0).set(payment.copy());
            menu.getSlot(1).set(ItemStack.EMPTY);
            ItemStack shown = menu.getSlot(2).getItem().copy();
            menu.clicked(2, 0, ClickType.PICKUP, f.player);
            ItemStack delivered = menu.getCarried();
            boolean paid = menu.getSlot(0).getItem().isEmpty();
            boolean traded = available && ItemStack.matches(shown, reward)
                    && ItemStack.matches(delivered, reward) && paid && offer.getUses() == 1;
            observations.add(profession + " available=" + available + ", shown=" + shown
                    + ", delivered=" + delivered + ", paymentLeft=" + menu.getSlot(0).getItem()
                    + ", uses=" + offer.getUses() + "/" + offer.getMaxUses());
            return traded;
        } finally {
            if (menu != null) {
                menu.setCarried(ItemStack.EMPTY);
                menu.getSlot(0).set(ItemStack.EMPTY);
                menu.getSlot(1).set(ItemStack.EMPTY);
                menu.removed(f.player);
            }
            f.player.containerMenu = f.player.inventoryMenu;
            villager.setTradingPlayer(null);
            villager.discard();
        }
    }

    /** Oracle Gather's Entropy fuzzy comparison accepts the same item with different tags. */
    public static void gatherEntropyPicksNamedDiamond(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            List<ChunkPos> held = GolemChunks.hold(f.level, List.of(f.base));
            try {
                ItemStack named = new ItemStack(Items.DIAMOND);
                named.set(DataComponents.CUSTOM_NAME, Component.literal("Gather review diamond"));
                PickupResult exact = pickup(test, f, new ItemStack(Items.DIAMOND), List.of());
                PickupResult strict = pickup(test, f, named.copy(), List.of());
                PickupResult entropy = pickup(test, f, named.copy(), List.of(GolemUpgrade.ENTROPY.id));
                test.assertTrue(exact.picked() && !strict.picked() && strict.dropRetained(),
                        "actual Gather ticks admit exact diamond and retain named diamond without Entropy: exact="
                                + exact + ", strict=" + strict);
                test.assertTrue(entropy.picked() && ItemStack.isSameItemSameComponents(entropy.cargo(), named)
                                && entropy.cargo().getCount() == 1 && !entropy.dropRetained(),
                        "Entropy Gather takes one actual named diamond without changing its components: " + entropy);
            } finally {
                for (ChunkPos chunk : held) f.level.setChunkForced(chunk.x, chunk.z, false);
            }
        }
        test.succeed();
    }

    private record PickupResult(boolean picked, boolean dropRetained, ItemStack cargo, String work) {}

    private static PickupResult pickup(GameTestHelper test, TubeVariantsRuntimeChecks.Fixture f,
                                        ItemStack actual, List<Integer> upgrades) {
        BlockPos home = f.base.below();
        EntityGolemBase golem = golem(test, f.level, home, GolemData.GATHER,
                new ItemStack(Items.DIAMOND), List.of(), GolemUseOptions.DEFAULT, upgrades);
        ItemEntity drop = new ItemEntity(f.level, home.getX() + .65, home.getY(), home.getZ() + .5, actual);
        drop.setNoPickUpDelay();
        drop.setNoGravity(true);
        test.assertTrue(f.level.addFreshEntity(drop), "physical Gather input is admitted");
        try {
            test.assertTrue(golem.canWork(), "ordinary configured Gather can work: " + golem.describeWorkState());
            // Actual Mob.tick runs the installed selector, work scan, navigation and pickup authority.
            // The real drop starts within reach; no private goal or matching formula is invoked here.
            for (int tick = 0; tick < 100 && !golem.hasCargo(); tick++) {
                golem.setPos(Vec3.atBottomCenterOf(home));
                golem.setDeltaMovement(Vec3.ZERO);
                golem.setOnGround(true);
                f.level.tickNonPassenger(golem);
            }
            return new PickupResult(golem.hasCargo(), drop.isAlive(), golem.getCarriedItem(), golem.describeWorkState());
        } finally {
            drop.discard();
            golem.discard();
        }
    }

    /** The real Decanting search must reject a remote unloaded marker before reading its block. */
    public static void liquidSearchDoesNotLoadRemoteMarker(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            BlockPos remote = freshRemote(test, f.level);
            GolemMarker marker = marker(f.level, remote, Direction.UP);
            EntityGolemBase golem = golem(test, f.level, f.base.below(), GolemData.LIQUID,
                    new ItemStack(Items.WATER_BUCKET), List.of(marker), GolemUseOptions.DEFAULT, List.of());
            EntityGolemBase control = null;
            try {
                test.assertTrue(golem.canWork(), "real Decanting configuration is admitted");
                test.assertTrue(f.level.getChunkSource().getChunkNow(remote.getX() >> 4, remote.getZ() >> 4) == null,
                        "remote marker begins without a loaded native LevelChunk");
                BlockPos selected = GolemHelper.findPossibleLiquid(golem, WATER);
                boolean untouched = f.level.getChunkSource().getChunkNow(remote.getX() >> 4, remote.getZ() >> 4) == null;
                BlockPos source = f.base.east();
                f.put(source, Blocks.WATER);
                control = golem(test, f.level, f.base.below(), GolemData.LIQUID,
                        new ItemStack(Items.WATER_BUCKET), List.of(marker(f.level, source, Direction.UP)),
                        GolemUseOptions.DEFAULT, List.of());
                BlockPos found = GolemHelper.findPossibleLiquid(control, WATER);
                test.assertTrue(source.equals(found), "loaded real water source is still found: " + found);
                test.assertTrue(selected == null && untouched,
                        "out-of-range unloaded fluid marker does not generate or load a native chunk; selected="
                                + selected + ", stillUnloaded=" + untouched + ", marker=" + remote);
            } finally {
                if (control != null) control.discard();
                golem.discard();
            }
        }
        test.succeed();
    }

    /** Actual Use home-seed search must also guard the opposite-face click cell. */
    public static void useEmptySpaceDoesNotLoadOppositeChunk(GameTestHelper test) {
        try (var cleanup = new TubeVariantsRuntimeChecks.Fixture(test)) {
            cleanup.open();
            ServerLevel level = test.getLevel();
            ChunkPos owner = freshOwnerWithUnloadedWest(test, level);
            int x = owner.getMinBlockX(), z = owner.getMinBlockZ() + 8;
            int y = Math.min(160, level.getMaxBuildHeight() - 8);
            BlockPos home = new BlockPos(x + 6, y, z), marked = new BlockPos(x, y, z);
            BlockPos click = marked.west();
            Map<BlockPos, BlockState> original = new LinkedHashMap<>();
            Map<BlockPos, BlockEntity> originals = new LinkedHashMap<>();
            for (BlockPos pos : List.of(home, home.below(), home.below(2), marked, marked.east())) {
                original.put(pos, level.getBlockState(pos));
                if (level.getBlockEntity(pos) != null) originals.put(pos, level.getBlockEntity(pos));
            }
            // The shared fixture also retires this lane's exact deferred cargo UUIDs on failure.
            cleanup.states.putAll(original);
            cleanup.originals.putAll(originals);
            EntityGolemBase golem = null;
            try {
                for (BlockPos pos : original.keySet()) {
                    level.removeBlockEntity(pos);
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), QUIET);
                }
                level.setBlock(home.below(2), Blocks.STONE.defaultBlockState(), QUIET);
                level.setBlock(home.below(), Blocks.CHEST.defaultBlockState(), QUIET);
                var chest = (ChestBlockEntity) level.getBlockEntity(home.below());
                test.assertTrue(chest != null, "actual Use home inventory exists wholly inside owner chunk");
                chest.setItem(0, new ItemStack(Items.DIAMOND));
                golem = golem(test, level, home, GolemData.USE, ItemStack.EMPTY,
                        List.of(marker(level, marked, Direction.EAST)), new GolemUseOptions(true, false, false), List.of());
                awaitTracked(test, level, golem);
                test.assertTrue(golem.canWork() && golem.getUseOptions().emptySpace(),
                        "ordinary real Use worker has the empty-space option and a physical home seed: "
                                + golem.describeWorkState() + ", options=" + golem.getUseOptions());
                test.assertTrue(level.getChunkSource().getChunkNow(click.getX() >> 4, click.getZ() >> 4) == null,
                        "opposite-face click chunk starts unloaded after all fixture preparation");
                // Native selector reaches Take.canUse -> findHomeSeed -> findTarget before any path request.
                for (int tick = 0; tick < 40; tick++) {
                    golem.setPos(Vec3.atBottomCenterOf(home));
                    golem.setDeltaMovement(Vec3.ZERO);
                    level.tickNonPassenger(golem);
                }
                boolean untouched = level.getChunkSource().getChunkNow(click.getX() >> 4, click.getZ() >> 4) == null;
                test.assertTrue(untouched && !golem.hasCargo() && chest.getItem(0).getCount() == 1,
                        "empty-space Use skips an unloaded behind-marker click cell without loading it or taking its seed; "
                                + "stillUnloaded=" + untouched + ", cargo=" + golem.getCarriedItem()
                                + ", home=" + chest.getItem(0) + ", " + golem.describeWorkState());
            } finally {
                if (golem != null) golem.discard();
                // Distance zero holds owner at FULL/accessible, while west remains outside FULL.
                // Every recorded coordinate is in owner; shared cleanup never reads the click chunk.
                level.getChunkSource().removeRegionTicket(OWNER, owner, 0, owner);
            }
        }
        test.succeed();
    }

    private static EntityGolemBase golem(GameTestHelper test, ServerLevel level, BlockPos home, int core,
                                         ItemStack exemplar, List<GolemMarker> markers,
                                         GolemUseOptions options, List<Integer> upgrades) {
        List<ItemStack> filters = new ArrayList<>(Collections.nCopies(GolemData.FILTER_SLOTS, ItemStack.EMPTY));
        List<Integer> counts = new ArrayList<>(Collections.nCopies(GolemData.FILTER_SLOTS, 0));
        filters.set(0, exemplar);
        if (!exemplar.isEmpty() && core != GolemData.LIQUID) counts.set(0, 1);
        EntityGolemBase golem = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), level);
        golem.setGolemMaterial(2); // Ordinary tallow accepts the single Entropy upgrade and carries eight items.
        golem.applyConfiguration(new GolemData(core, filters, counts, false, markers, options, upgrades));
        golem.setNoGravity(true);
        golem.setPersistenceRequired();
        golem.setPos(Vec3.atBottomCenterOf(home));
        golem.setHome(home, Direction.UP);
        test.assertTrue(level.addFreshEntity(golem), "real configured golem is admitted");
        return golem;
    }

    private static GolemMarker marker(ServerLevel level, BlockPos pos, Direction face) {
        return new GolemMarker(level.dimension().location(), pos, face, GolemMarker.ANY);
    }

    /** FULL terrain precedes the native entity-section visibility callback; pump that already ticketed transition. */
    private static void awaitTracked(GameTestHelper test, ServerLevel level, EntityGolemBase golem) {
        long deadline = System.nanoTime() + java.time.Duration.ofSeconds(10).toNanos();
        while (level.getEntity(golem.getUUID()) != golem && System.nanoTime() < deadline) {
            if (!level.getChunkSource().pollTask()) LockSupport.parkNanos(1_000_000);
        }
        test.assertTrue(level.getEntity(golem.getUUID()) == golem,
                "already ticketed owner reaches native entity visibility without loading west: "
                        + level.getChunkSource().getChunkDebugData(new ChunkPos(golem.blockPosition())));
    }

    private static BlockPos freshRemote(GameTestHelper test, ServerLevel level) {
        int nonce = UUID.randomUUID().hashCode() & 0xffff;
        for (int attempt = 0; attempt < 8; attempt++) {
            BlockPos pos = new BlockPos(8_000_000 + nonce * 16 + attempt * 8192,
                    Math.min(160, level.getMaxBuildHeight() - 8), 8_000_000);
            if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) return pos;
        }
        test.fail("could not select an actually unloaded remote marker within eight read-only probes");
        throw new IllegalStateException("unreachable");
    }

    private static ChunkPos freshOwnerWithUnloadedWest(GameTestHelper test, ServerLevel level) {
        int nonce = UUID.randomUUID().hashCode() & 0x7fff;
        for (int attempt = 0; attempt < 8; attempt++) {
            ChunkPos owner = new ChunkPos(100_000 + nonce + attempt * 1024, 140_000);
            if (level.getChunkSource().getChunkNow(owner.x - 1, owner.z) != null) continue;
            level.getChunkSource().addRegionTicket(OWNER, owner, 0, owner);
            level.getChunk(owner.x, owner.z); // Only owner is deliberately made FULL.
            if (level.getChunkSource().getChunkNow(owner.x - 1, owner.z) == null) return owner;
            level.getChunkSource().removeRegionTicket(OWNER, owner, 0, owner);
        }
        test.fail("could not prepare one loaded native chunk with an unloaded west neighbour");
        throw new IllegalStateException("unreachable");
    }
}
