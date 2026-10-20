package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftWands;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.ArcaneWandRecipe;
import thaumcraft.common.crafting.InfusionRecipeInput;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.WandMaterials;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;

/**
 * The six elemental wand rods on real content: the loaded infusion recipes (centre, the balance shard and the rod's own shard, the
 * essentia of the original, instability 3, the rod as result), the arcane wand assembly with each rod (cost rod x cap, locked behind
 * the rod's research for a real player), and the rods' slow refill of their own primal aspect in wands that a REAL connected player
 * carries: every two-hundredth tick of the player, up to a tenth of the capacity, and nowhere else. Players and wands are explicit
 * inputs; the recipes, the assembled wands and the refill are never supplied. NOT a client check (the models and icons are unseen), a
 * full altar ritual with these recipes or the staff versions of the rods.
 */
public final class WandRodsRuntimeChecks {
    private record Rod(String tag, String center, String shard, String primal, String second) {}

    private static final List<Rod> RODS = List.of(
            new Rod("obsidian", "minecraft:obsidian", "earth_shard", "terra", "tenebrae"),
            new Rod("ice", "minecraft:ice", "water_shard", "aqua", "gelum"),
            new Rod("quartz", "minecraft:quartz_block", "order_shard", "ordo", "vitreus"),
            new Rod("reed", "minecraft:sugar_cane", "air_shard", "aer", "motus"),
            new Rod("blaze", "minecraft:blaze_rod", "fire_shard", "ignis", "bestia"),
            new Rod("bone", "minecraft:bone", "entropy_shard", "perditio", "mortuus"));

    private WandRodsRuntimeChecks() {}

    private static Item item(String id) { return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)); }

    public static void run(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        recipes(test, level);
        var connection = new Connection(PacketFlow.SERVERBOUND);
        var channel = new EmbeddedChannel(connection);
        var player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "rods-" + UUID.randomUUID().toString().substring(0, 8)),
                ClientInformation.createDefault());
        level.getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
        player.setGameMode(GameType.SURVIVAL);
        try {
            test.assertTrue(player.level() == level && level.getServer().getPlayerList().getPlayer(player.getUUID()) == player, "a real connected player carries the wands");
            assembly(test, level, player);
            refill(test, level, connection, channel, player);
        } catch (RuntimeException failure) {
            cleanup(level, connection, channel, player);
            throw failure;
        }
    }

    private static void recipes(GameTestHelper test, ServerLevel level) {
        for (Rod rod : RODS) {
            var holder = level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "infusion/wand_rod_" + rod.tag())).orElseThrow(
                    () -> new AssertionError("no infusion recipe for the " + rod.tag() + " rod"));
            var recipe = (InfusionStackRecipe) holder.value();
            test.assertTrue(recipe.research().equals("ROD_" + rod.tag()) && recipe.instability() == 3
                            && recipe.result().is(item("thaumcraft:wand_rod_" + rod.tag())) && recipe.result().getCount() == 1,
                    rod.tag() + ": research ROD_" + rod.tag() + ", instability 3, one rod as the result");
            // Original: the primal twice the rod's cost (6 x 2), Magic and the secondary aspect once (the secondary uses the BLAZE cost, also 6).
            test.assertTrue(recipe.aspects().equals(Map.of(rod.primal(), 12, "praecantatio", 6, rod.second(), 6)), rod.tag() + ": the essentia are " + recipe.aspects());
            test.assertTrue(recipe.input().test(new ItemStack(item(rod.center()))) && recipe.components().size() == 2, rod.tag() + ": the centre is " + rod.center() + " with two shards");
            var offered = new InfusionRecipeInput(new ItemStack(item(rod.center())), List.of(
                    new ItemStack(item("thaumcraft:balance_shard")), new ItemStack(item("thaumcraft:" + rod.shard()))));
            test.assertTrue(recipe.matches(offered, level), rod.tag() + ": the balance shard and the " + rod.shard() + " complete it");
            var wrong = new InfusionRecipeInput(new ItemStack(item(rod.center())), List.of(
                    new ItemStack(item("thaumcraft:balance_shard")), new ItemStack(item("thaumcraft:" + (rod.shard().equals("fire_shard") ? "air_shard" : "fire_shard")))));
            test.assertTrue(!recipe.matches(wrong, level), rod.tag() + ": another shard does not");
        }
        test.assertTrue(level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "infusion/wand_rod_greatwood")).isEmpty(),
                "the greatwood rod is still an arcane recipe, not an infusion");
    }

    private static void assembly(GameTestHelper test, ServerLevel level, ServerPlayer player) {
        var assembly = level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "arcane/wand_assembly")).orElseThrow();
        ArcaneWandRecipe recipe = (ArcaneWandRecipe) assembly.value();
        for (Rod rod : RODS) {
            NonNullList<ItemStack> grid = NonNullList.withSize(9, ItemStack.EMPTY);
            ItemStack cap = ThaumcraftWands.capItem("thaumium");
            grid.set(6, cap.copy());
            grid.set(2, cap.copy());
            grid.set(4, ThaumcraftWands.rodItem(rod.tag()));
            var input = new ArcaneRecipeInput(grid);
            test.assertTrue(recipe.matches(input, level), rod.tag() + ": the rod and two thaumium caps make a wand");
            ItemStack wand = recipe.assemble(input, level.registryAccess());
            WandData data = ItemWandCasting.data(wand);
            test.assertTrue(data.rod().equals(rod.tag()) && data.cap().equals("thaumium") && data.vis().isEmpty() && data.capacity() == 7500,
                    rod.tag() + ": the assembled wand holds 75 vis and starts empty: " + data);
            test.assertTrue(wand.get(ThaumcraftWands.WAND_CRAFT_COST.get()) == 36 && recipe.vis(input).values().stream().allMatch(cost -> cost == 36),
                    rod.tag() + ": the cost is rod 6 x cap 6 = 36 of each primal");
            var knowledge = KnowledgeAccess.get(player);
            knowledge.addResearch("CAP_thaumium");
            test.assertTrue(!recipe.hasResearch(input, player), rod.tag() + ": locked behind ROD_" + rod.tag());
            knowledge.addResearch("ROD_" + rod.tag());
            test.assertTrue(recipe.hasResearch(input, player), rod.tag() + ": open with ROD_" + rod.tag());
            knowledge.removeResearch("ROD_" + rod.tag());
            knowledge.removeResearch("CAP_thaumium");
        }
    }

    private static void refill(GameTestHelper test, ServerLevel level, Connection connection, EmbeddedChannel channel, ServerPlayer player) {
        List<ItemStack> wands = new ArrayList<>();
        for (Rod rod : RODS) wands.add(wand(rod.tag(), Map.of()));
        ItemStack greatwood = wand("greatwood", Map.of());
        ItemStack rich = wand("obsidian", Map.of("terra", 800));
        ItemStack stashed = wand("blaze", Map.of());
        int slot = 0;
        for (ItemStack wand : wands) player.getInventory().setItem(slot++, wand);
        player.getInventory().setItem(slot++, greatwood);
        player.getInventory().setItem(slot, rich);
        player.getInventory().setItem(35, stashed);
        // A tick that is not a multiple of 200 changes nothing; one that is, refills.
        player.tickCount = 195;
        long start = test.getTick();
        // A player that no network handler ticks does not tick its inventory either; doTick is what the connection calls every tick.
        test.onEachTick(player::doTick);
        test.onEachTick(() -> {
            if (test.getTick() - start == 3) {
                for (ItemStack wand : wands) test.assertTrue(ItemWandCasting.data(wand).vis().isEmpty(), "before the two-hundredth tick no wand has been refilled: " + ItemWandCasting.data(wand));
            }
        });
        test.runAtTickTime(start + 12, () -> {
            try {
                test.assertTrue(player.tickCount >= 200 && player.tickCount < 215, "the player passed its two-hundredth tick: " + player.tickCount);
                for (int index = 0; index < RODS.size(); index++) {
                    WandData data = ItemWandCasting.data(wands.get(index));
                    Rod rod = RODS.get(index);
                    test.assertTrue(data.vis().equals(Map.of(rod.primal(), 100)), rod.tag() + " refilled only " + rod.primal() + " by one whole vis: " + data);
                }
                test.assertTrue(ItemWandCasting.data(greatwood).vis().isEmpty(), "a greatwood rod refills nothing");
                test.assertTrue(ItemWandCasting.data(rich).amount("terra") == 800, "a wand above a tenth of its capacity is left alone");
                test.assertTrue(ItemWandCasting.data(stashed).amount("ignis") == 100, "a wand in the far inventory slots refills too (every inventory stack is ticked)");
                // And it goes on, 200 ticks later.
                player.tickCount = 395;
                test.runAtTickTime(start + 24, () -> {
                    try {
                        for (int index = 0; index < RODS.size(); index++) {
                            Rod rod = RODS.get(index);
                            test.assertTrue(ItemWandCasting.data(wands.get(index)).amount(rod.primal()) == 200, rod.tag() + " refilled again on the next period");
                        }
                        test.succeed();
                    } finally { cleanup(level, connection, channel, player); }
                });
            } catch (RuntimeException failure) {
                cleanup(level, connection, channel, player);
                throw failure;
            }
        });
    }

    private static ItemStack wand(String rod, Map<String, Integer> vis) {
        ItemStack stack = new ItemStack(ThaumcraftContent.WAND.get());
        stack.set(ThaumcraftContent.WAND_DATA.get(), new WandData(rod, "iron", vis));
        return stack;
    }

    private static void cleanup(ServerLevel level, Connection connection, EmbeddedChannel channel, ServerPlayer player) {
        try {
            if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player);
        } finally { player.discard(); channel.finishAndReleaseAll(); }
    }
}
