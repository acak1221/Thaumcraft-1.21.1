package thaumcraft.common.testing;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.LockSupport;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftVillageProfessions;
import thaumcraft.common.lib.world.VillageBankerManager;
import thaumcraft.common.lib.world.VillageWizardManager;
import thaumcraft.common.lib.world.VillageWorldGenRules;
import thaumcraft.common.mixin.PersistentEntitySectionManagerAccess;
import thaumcraft.common.mixin.ServerLevelEntityManagerAccess;

/**
 * The trade tables of the TC4 villages on the villagers of the two professions.
 *
 * <p>Oracle {@code thaumcraft/common/lib/world/VillageBankerManager.java:41-56} and
 * {@code thaumcraft/common/lib/world/VillageWizardManager.java:35-53}: the two managers of
 * the original were Forge village trade handlers. They added their recipes to the trade
 * list of every villager of their profession, and the pieces of the village placed the
 * villager with the profession themselves
 * ({@code ComponentBankerHome:112} and {@code ComponentBankerHome:116-118}).
 *
 * <p>This check generates the chunks the village plan gives a banker home to, finds the
 * banker of the house and reads its trade list. It also checks that every item of the two
 * tables is a registered item: the two edible nuggets of the original were the items the
 * trade table of the wizard named wrongly for a long time.
 */
public final class VillageTradeRuntimeChecks {
    /** The chunk coordinates the scan of the village cells covers. */
    private static final int SCAN_RADIUS = 96;
    /** The number of component chunks that are generated at most. */
    private static final int MAX_GENERATED = 12;
    private static final Duration ENTITY_READINESS_BUDGET = Duration.ofSeconds(10);
    /**
     * A ticket that keeps a generated chunk loaded while the check reads its entities.
     * The ticket of the original types ({@code TicketType.POST_TELEPORT}) expires after
     * five ticks, so the check needs a ticket of its own.
     */
    private static final TicketType<ChunkPos> HELD =
            TicketType.create("thaumcraft_gametest_hold", Comparator.comparingLong(ChunkPos::toLong));

    private VillageTradeRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        var key = ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath("thaumcraft_gametest", "natural"));
        ServerLevel level = test.getLevel().getServer().getLevel(key);
        test.assertTrue(level != null, "test-only normal terrain dimension is loaded");
        long seed = level.getSeed();

        // The professions of the original are registered.
        test.assertTrue(BuiltInRegistries.VILLAGER_PROFESSION
                        .containsKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "banker")),
                "the banker profession of the original is registered");
        test.assertTrue(BuiltInRegistries.VILLAGER_PROFESSION
                        .containsKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "wizard")),
                "the wizard profession of the original is registered");

        // Every item of the two tables is a registered item.
        for (VillageWorldGenRules.TradeSpec trade : VillageBankerManager.trades()) {
            test.assertTrue(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(trade.payItem())),
                    "the banker trade pays with the registered item " + trade.payItem());
            for (String item : trade.receiveItems()) {
                test.assertTrue(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(item)),
                        "the banker trade gives the registered item " + item);
            }
        }
        for (VillageWorldGenRules.TradeSpec trade : VillageWizardManager.trades()) {
            test.assertTrue(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(trade.payItem())),
                    "the wizard trade pays with the registered item " + trade.payItem());
            for (String item : trade.receiveItems()) {
                test.assertTrue(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(item)),
                        "the wizard trade gives the registered item " + item);
            }
        }

        // The chunks the village plan gives a banker home to are generated.
        List<int[]> bankers = componentChunks(seed, VillageWorldGenRules.HouseType.BANKER_HOME);
        test.assertTrue(!bankers.isEmpty(), "the village plan gives chunks to the banker home");
        // The chunk system unloads a chunk that no ticket holds, and the entities of a
        // chunk leave the world with the chunk. The check holds a ticket on every chunk
        // it generates, so the villager of the piece stays in the world while it is read.
        List<ChunkPos> held = new ArrayList<>();
        int generated = 0;
        try {
            for (int[] chunk : bankers) {
                if (generated >= MAX_GENERATED) break;
                ++generated;
                ChunkPos pos = new ChunkPos(chunk[0], chunk[1]);
                level.getChunkSource().addRegionTicket(HELD, pos, 1, pos);
                held.add(pos);
                level.getChunk(chunk[0], chunk[1]);
            }
        } catch (RuntimeException | Error failure) {
            releaseHeld(level, held);
            throw failure;
        }
        final int generatedChunks = generated;
        // WorldGenRegion stores its villagers in ProtoChunk NBT. Native FULL/tracking
        // transitions and the asynchronous entity loading inbox must both settle before
        // the live lookup can establish that a villager is absent. Five simulated ticks
        // alone provide no wall-clock budget for those native tasks.
        long deadline = System.nanoTime() + ENTITY_READINESS_BUDGET.toNanos();
        long tickDeadline = test.getTick() + 2000;
        test.runAfterDelay(5, () -> awaitEntities(test, level, held, generatedChunks, deadline, tickDeadline, true));
    }

    private static void awaitEntities(GameTestHelper test, ServerLevel level, List<ChunkPos> held,
                                      int generatedChunks, long deadline, long tickDeadline, boolean first) {
        boolean retrying = false;
        try {
            boolean ready = held.stream().allMatch(pos -> nativeEntitiesReady(level, pos));
            if (first || ready) {
                thaumcraft.common.Thaumcraft.LOG.info("Village trade fixture native readiness (ready={}, testTick={}): {}",
                        ready, test.getTick(), readinessStates(level, held));
            }
            if (!ready) {
                test.assertTrue(System.nanoTime() < deadline && test.getTick() < tickDeadline,
                        "held village chunks did not finish native entity loading/tracking within " + ENTITY_READINESS_BUDGET
                                + ": " + readinessStates(level, held));
                // Yield to the disk executor, then return to an ordinary level tick so
                // PersistentEntitySectionManager itself processes its loading inbox.
                LockSupport.parkNanos(1_000_000L);
                test.runAfterDelay(1, () -> awaitEntities(test, level, held, generatedChunks, deadline, tickDeadline, false));
                retrying = true;
                return;
            }
            Villager banker = null;
            List<Villager> found = villagers(level);
            for (Villager villager : found) {
                if (villager.getVillagerData().getProfession() == ThaumcraftVillageProfessions.BANKER.get()) {
                    banker = villager;
                    break;
                }
            }
            test.assertTrue(banker != null, "the house of the banker carries a villager of the banker profession, "
                    + generatedChunks + " chunks were generated, " + found.size() + " villagers of the world found: "
                    + professions(found) + "; native readiness=" + readinessStates(level, held));
            var offers = banker.getOffers();
            test.assertTrue(offers.size() == VillageBankerManager.trades().size(),
                    "the banker carries the twelve trades of the original, found " + offers.size());
            test.assertTrue(hasBookTrade(offers), "the banker buys the book for seven to nine gold coins");
            test.succeed();
        } finally {
            if (!retrying) releaseHeld(level, held);
        }
    }

    private static boolean nativeEntitiesReady(ServerLevel level, ChunkPos pos) {
        var chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
        if (chunk == null || chunk.getFullStatus() == null || !chunk.getFullStatus().isOrAfter(FullChunkStatus.FULL)
                || !level.areEntitiesLoaded(pos.toLong())) return false;
        var manager = ((ServerLevelEntityManagerAccess) level).thaumcraft$getEntityManager();
        var sections = ((PersistentEntitySectionManagerAccess) manager).thaumcraft$getSectionStorage();
        try (var stream = sections.getExistingSectionsInChunk(pos.toLong())) {
            return stream.allMatch(section -> section.getStatus().isAccessible());
        }
    }

    private static List<String> readinessStates(ServerLevel level, List<ChunkPos> held) {
        var manager = ((ServerLevelEntityManagerAccess) level).thaumcraft$getEntityManager();
        var sections = ((PersistentEntitySectionManagerAccess) manager).thaumcraft$getSectionStorage();
        List<String> states = new ArrayList<>();
        for (ChunkPos pos : held) {
            var chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
            long bufferedVillagers;
            List<String> visibility;
            try (var stream = sections.getExistingSectionsInChunk(pos.toLong())) {
                var existing = stream.toList();
                bufferedVillagers = existing.stream().flatMap(EntitySection::getEntities).filter(entity -> entity instanceof Villager).count();
                visibility = existing.stream().map(section -> section.getStatus().toString()).distinct().toList();
            }
            states.add(pos + "[full=" + (chunk == null ? "absent" : chunk.getFullStatus())
                    + ",entitiesLoaded=" + level.areEntitiesLoaded(pos.toLong()) + ",sectionVillagers=" + bufferedVillagers
                    + ",sectionVisibility=" + visibility
                    + ",pendingTasks=" + level.getChunkSource().getPendingTasksCount() + ']');
        }
        return states;
    }

    private static void releaseHeld(ServerLevel level, List<ChunkPos> held) {
        for (ChunkPos pos : held) level.getChunkSource().removeRegionTicket(HELD, pos, 1, pos);
        held.clear();
    }

    /**
     * The chunks the village plan gives a component to, the same decision the feature
     * of the component makes for every chunk of the world.
     */
    private static List<int[]> componentChunks(long seed, VillageWorldGenRules.HouseType type) {
        List<int[]> chunks = new ArrayList<>();
        for (int chunkX = -SCAN_RADIUS; chunkX <= SCAN_RADIUS; ++chunkX) {
            for (int chunkZ = -SCAN_RADIUS; chunkZ <= SCAN_RADIUS; ++chunkZ) {
                VillageWorldGenRules.VillagePlan plan = VillageWorldGenRules.villagePlan(seed, chunkX, chunkZ);
                int house = VillageWorldGenRules.houseIndex(plan, chunkX, chunkZ);
                if (house < 0) continue;
                if (plan.houses().get(house) != type) continue;
                chunks.add(new int[] {chunkX, chunkZ});
            }
        }
        return chunks;
    }

    /** The villagers of the loaded part of the world. */
    private static List<Villager> villagers(ServerLevel level) {
        var box = new AABB(-4096, level.getMinBuildHeight(), -4096,
                4096, level.getMaxBuildHeight(), 4096);
        return new ArrayList<>(level.getEntitiesOfClass(Villager.class, box));
    }

    /** The professions of the villagers, for the failure message of the check. */
    private static String professions(List<Villager> villagers) {
        var names = new ArrayList<String>();
        for (Villager villager : villagers) {
            names.add(String.valueOf(villager.getVillagerData().getProfession()));
        }
        return names.toString();
    }

    /** The trade of the book, oracle VillageBankerManager:47. */
    private static boolean hasBookTrade(List<MerchantOffer> offers) {
        for (MerchantOffer offer : offers) {
            var result = offer.getResult();
            if (!result.is(net.minecraft.world.item.Items.BOOK)) continue;
            var cost = offer.getCostA();
            if (cost.getCount() < 7 || cost.getCount() > 9) return false;
            return cost.is(ThaumcraftContent.MATERIALS.get("gold_coin").get());
        }
        return false;
    }
}
