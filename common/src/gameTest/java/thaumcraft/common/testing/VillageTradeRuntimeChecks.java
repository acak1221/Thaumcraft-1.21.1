package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftVillageProfessions;
import thaumcraft.common.lib.world.VillageBankerManager;
import thaumcraft.common.lib.world.VillageWizardManager;
import thaumcraft.common.lib.world.VillageWorldGenRules;

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
        int generated = 0;
        for (int[] chunk : bankers) {
            if (generated >= MAX_GENERATED) break;
            ++generated;
            level.getChunk(chunk[0], chunk[1]);
        }

        // The banker of the house carries the table of the original.
        Villager banker = null;
        for (Villager villager : bankers(level)) {
            if (villager.getVillagerData().getProfession() == ThaumcraftVillageProfessions.BANKER.get()) {
                banker = villager;
                break;
            }
        }
        test.assertTrue(banker != null, "the house of the banker carries a villager of the banker profession, "
                + generated + " chunks were generated");
        var offers = banker.getOffers();
        test.assertTrue(offers.size() == VillageBankerManager.trades().size(),
                "the banker carries the twelve trades of the original, found " + offers.size());
        test.assertTrue(hasBookTrade(offers), "the banker buys the book for seven to nine gold coins");
        test.succeed();
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
    private static List<Villager> bankers(ServerLevel level) {
        var box = new AABB(-4096, level.getMinBuildHeight(), -4096,
                4096, level.getMaxBuildHeight(), 4096);
        return new ArrayList<>(level.getEntitiesOfClass(Villager.class, box));
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
