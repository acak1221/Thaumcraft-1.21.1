package thaumcraft.common.testing;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.config.ThaumcraftOres;
import thaumcraft.common.crafting.InfusionRecipeInput;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.lib.utils.SpecialMining;

/**
 * The Pickaxe of the Core on real content: its numbers and repair, the infusion that makes it, the fire a hit sets (and the player-versus-
 * player rule), the five uses a right click costs, and the special mining: the cluster that replaces a raw ore or a cinnabar ore among the
 * drops, with the original chance (0.2 + 0.075 per fortune level, x 0.9 for gold and cinnabar), the same count, only for a player mining
 * with the pickaxe and without Silk Touch, through the block drop list that player breaking, Excavation and the Bore share, and through a
 * real survival break. The chance is measured over hundreds of draws with wide bounds. NOT the client ore scan of the right click, the
 * Bore's own slot or an Excavation focus with Dowsing (not ported).
 */
public final class ElementalPickaxeRuntimeChecks {
    private ElementalPickaxeRuntimeChecks() {}

    private static Item item(String id) { return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)); }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }

    public static void run(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        numbers(test);
        recipe(test, level);
        var attacker = test.makeMockServerPlayerInLevel();
        attacker.setGameMode(GameType.SURVIVAL);
        var victim = test.makeMockServerPlayerInLevel();
        victim.setGameMode(GameType.SURVIVAL);
        boolean pvp = level.getServer().isPvpAllowed();
        try {
            blows(test, attacker, victim);
            click(test, level, attacker);
            rolls(test);
            drops(test, level, attacker);
            breaking(test, level, attacker);
        } finally {
            level.getServer().setPvpAllowed(pvp);
        }
        test.succeed();
    }

    private static ItemStack pickaxe() { return new ItemStack(ThaumcraftEquipment.ELEMENTAL_PICKAXE.get()); }

    private static void numbers(GameTestHelper test) {
        ItemStack pick = pickaxe();
        test.assertTrue(pick.getMaxDamage() == 1500 && pick.getItem().getEnchantmentValue() == 18 && pick.getRarity() == Rarity.RARE,
                "1500 uses, enchantability 18, rare: " + pick.getMaxDamage() + " " + pick.getItem().getEnchantmentValue() + " " + pick.getRarity());
        test.assertTrue(pick.getDestroySpeed(Blocks.STONE.defaultBlockState()) == 10F && pick.getDestroySpeed(Blocks.DIRT.defaultBlockState()) == 1F,
                "speed 10 on stone, none on dirt: " + pick.getDestroySpeed(Blocks.STONE.defaultBlockState()));
        test.assertTrue(pick.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()) && pick.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState()),
                "harvest tier 3 reaches obsidian and diamond");
        double[] damage = {0, 0};
        pick.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.is(Attributes.ATTACK_DAMAGE)) damage[0] += modifier.amount();
            if (attribute.is(Attributes.ATTACK_SPEED)) damage[1] += modifier.amount();
        });
        test.assertTrue(damage[0] == 5.0 && damage[1] == -2.8F, "legacy pickaxe 2 + material 3 = 5 damage, -2.8 speed: " + damage[0] + " " + damage[1]);
        test.assertTrue(pick.getItem().isValidRepairItem(pick, new ItemStack(item("thaumcraft:thaumium_ingot"))) && !pick.getItem().isValidRepairItem(pick, new ItemStack(Items.IRON_INGOT))
                        && !pick.getItem().isValidRepairItem(pick, new ItemStack(Items.DIAMOND)), "repaired with the thaumium ingot alone");
        test.assertTrue(pick.is(ItemTags.PICKAXES), "a pickaxe by tag");
    }

    private static void recipe(GameTestHelper test, ServerLevel level) {
        var infusion = (InfusionStackRecipe) level.getRecipeManager().byKey(id("infusion/elemental_pickaxe")).orElseThrow(() -> new AssertionError("no recipe")).value();
        test.assertTrue(infusion.research().equals("ELEMENTALPICK") && infusion.instability() == 1 && infusion.result().is(item("thaumcraft:elemental_pickaxe"))
                        && infusion.result().getCount() == 1 && infusion.aspects().equals(Map.of("ignis", 8, "perfodio", 8, "sensus", 8)) && infusion.components().size() == 4,
                "the infusion: ELEMENTALPICK, instability 1, 8 fire + 8 mining + 8 senses, four components: " + infusion.aspects());
        ItemStack centre = new ItemStack(item("thaumcraft:thaumium_pickaxe"));
        List<ItemStack> parts = List.of(new ItemStack(item("thaumcraft:fire_shard")), new ItemStack(item("thaumcraft:fire_shard")),
                new ItemStack(Items.DIAMOND), new ItemStack(item("thaumcraft:greatwood_log")));
        test.assertTrue(infusion.matches(new InfusionRecipeInput(centre, parts), level), "a thaumium pickaxe, two fire shards, a diamond and a greatwood log complete it");
        test.assertTrue(!infusion.matches(new InfusionRecipeInput(new ItemStack(Items.IRON_PICKAXE), parts), level), "an iron pickaxe is not the centre");
        test.assertTrue(!infusion.matches(new InfusionRecipeInput(new ItemStack(item("thaumcraft:thaumium_axe")), parts), level), "nor is the thaumium axe");
        test.assertTrue(!infusion.matches(new InfusionRecipeInput(centre, List.of(parts.get(0), parts.get(1), new ItemStack(Items.EMERALD), parts.get(3))), level), "an emerald is not the diamond");
        test.assertTrue(!infusion.matches(new InfusionRecipeInput(centre, List.of(parts.get(0), new ItemStack(item("thaumcraft:water_shard")), parts.get(2), parts.get(3))), level),
                "a water shard is not the second fire shard");
    }

    private static void blows(GameTestHelper test, ServerPlayer attacker, ServerPlayer victim) {
        Pig pig = test.spawnWithNoFreeWill(EntityType.PIG, 1, 2, 1);
        pickaxe().hurtEnemy(pig, attacker);
        test.assertTrue(pig.getRemainingFireTicks() == 40, "a hit sets a pig alight for two seconds: " + pig.getRemainingFireTicks());
        Pig plain = test.spawnWithNoFreeWill(EntityType.PIG, 3, 2, 1);
        new ItemStack(item("thaumcraft:thaumium_pickaxe")).hurtEnemy(plain, attacker);
        new ItemStack(Items.IRON_PICKAXE).hurtEnemy(plain, attacker);
        test.assertTrue(!plain.isOnFire(), "the thaumium and iron pickaxes light nothing");
        var server = attacker.serverLevel().getServer();
        server.setPvpAllowed(false);
        pickaxe().hurtEnemy(victim, attacker);
        test.assertTrue(!victim.isOnFire(), "with player-versus-player off a player is spared");
        pickaxe().hurtEnemy(plain, attacker);
        test.assertTrue(plain.isOnFire(), "a mob is not, with it off");
        server.setPvpAllowed(true);
        pickaxe().hurtEnemy(victim, attacker);
        test.assertTrue(victim.isOnFire(), "with it on a player burns too");
        victim.clearFire();
    }

    private static void click(GameTestHelper test, ServerLevel level, ServerPlayer player) {
        BlockPos pos = test.absolutePos(new BlockPos(2, 2, 2));
        ItemStack pick = pickaxe();
        player.setItemInHand(InteractionHand.MAIN_HAND, pick);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        InteractionResult result = pick.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        test.assertTrue(result == InteractionResult.CONSUME && pick.getDamageValue() == 5, "a right click costs five uses and consumes its native interaction: " + result + " " + pick.getDamageValue());
        player.setGameMode(GameType.CREATIVE);
        ItemStack creative = pickaxe();
        player.setItemInHand(InteractionHand.MAIN_HAND, creative);
        creative.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        test.assertTrue(creative.getDamageValue() == 0, "a creative player pays nothing");
        player.setGameMode(GameType.SURVIVAL);
    }

    /** A seed whose first draw satisfies the test, so the roll is known without a stub of the whole random interface. */
    private static long seed(java.util.function.Predicate<Float> wanted) {
        for (long seed = 1; ; ++seed) if (wanted.test(RandomSource.create(seed).nextFloat())) return seed;
    }

    private static void rolls(GameTestHelper test) {
        ItemStack raw = new ItemStack(Items.RAW_IRON, 5);
        ItemStack hit = SpecialMining.find(raw, SpecialMining.chance(0), RandomSource.create(seed(roll -> roll < 0.05F)));
        test.assertTrue(hit.is(item("thaumcraft:cluster_iron")) && hit.getCount() == 5, "a low roll turns five raw iron into five iron clusters: " + hit);
        ItemStack miss = SpecialMining.find(raw, SpecialMining.chance(0), RandomSource.create(seed(roll -> roll > 0.9F)));
        test.assertTrue(miss.is(Items.RAW_IRON) && miss.getCount() == 5 && miss != raw, "a high roll leaves a copy of the five raw iron");
        ItemStack gold = SpecialMining.find(new ItemStack(Items.RAW_GOLD, 2), SpecialMining.chance(0), RandomSource.create(seed(roll -> roll < 0.05F)));
        test.assertTrue(gold.is(item("thaumcraft:cluster_gold")) && gold.getCount() == 2, "gold: " + gold);
        ItemStack cinnabar = SpecialMining.find(new ItemStack(ThaumcraftOres.CINNABAR.get()), SpecialMining.chance(0), RandomSource.create(seed(roll -> roll < 0.05F)));
        test.assertTrue(cinnabar.is(item("thaumcraft:cluster_cinnabar")) && cinnabar.getCount() == 1, "cinnabar: " + cinnabar);
        // The 0.9 factor: a roll of 0.19 is inside iron's 0.2 and outside gold's 0.18.
        long between = seed(roll -> roll > 0.185F && roll < 0.2F);
        test.assertTrue(SpecialMining.find(new ItemStack(Items.RAW_IRON), SpecialMining.chance(0), RandomSource.create(between)).is(item("thaumcraft:cluster_iron"))
                        && SpecialMining.find(new ItemStack(Items.RAW_GOLD), SpecialMining.chance(0), RandomSource.create(between)).is(Items.RAW_GOLD),
                "a roll between 0.18 and 0.2 turns iron and not gold");
        // One draw is taken per drop even when nothing is listed, like the original.
        long any = 77L;
        RandomSource used = RandomSource.create(any);
        RandomSource reference = RandomSource.create(any);
        reference.nextFloat();
        ItemStack coal = SpecialMining.find(new ItemStack(Items.COAL, 3), SpecialMining.chance(0), used);
        test.assertTrue(coal.is(Items.COAL) && coal.getCount() == 3 && used.nextInt() == reference.nextInt(), "an unlisted drop still takes one draw");
    }

    /** Convert-or-not tally over many drop lists of one block, mined by {@code miner} with {@code tool}. */
    private static int clusters(ServerLevel level, BlockPos pos, Block block, Entity miner, ItemStack tool, int trials, Item cluster, Item plain) {
        int swapped = 0;
        for (int trial = 0; trial < trials; ++trial) {
            List<ItemStack> drops = Block.getDrops(block.defaultBlockState(), level, pos, null, miner, tool);
            if (drops.size() != 1) throw new AssertionError("one drop expected: " + drops);
            ItemStack drop = drops.get(0);
            if (drop.is(cluster)) ++swapped;
            else if (!drop.is(plain)) throw new AssertionError("unexpected drop " + drop + " of " + block);
        }
        return swapped;
    }

    private static void drops(GameTestHelper test, ServerLevel level, ServerPlayer player) {
        BlockPos pos = test.absolutePos(new BlockPos(2, 2, 2));
        var enchantments = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        ItemStack fortune = pickaxe();
        fortune.enchant(enchantments.getHolderOrThrow(Enchantments.FORTUNE), 3);
        ItemStack silk = pickaxe();
        silk.enchant(enchantments.getHolderOrThrow(Enchantments.SILK_TOUCH), 1);
        Item clusterIron = item("thaumcraft:cluster_iron");
        Item clusterGold = item("thaumcraft:cluster_gold");
        Item clusterCinnabar = item("thaumcraft:cluster_cinnabar");
        int n = 400;
        int iron = clusters(level, pos, Blocks.IRON_ORE, player, pickaxe(), n, clusterIron, Items.RAW_IRON);
        test.assertTrue(iron >= 40 && iron <= 120, "iron ore: about a fifth of " + n + " drops turn into iron clusters: " + iron);
        int gold = clusters(level, pos, Blocks.GOLD_ORE, player, pickaxe(), n, clusterGold, Items.RAW_GOLD);
        test.assertTrue(gold >= 33 && gold <= 111, "gold ore: nine tenths of that, 0.18: " + gold);
        int cinnabar = clusters(level, pos, ThaumcraftOres.CINNABAR.get(), player, pickaxe(), n, clusterCinnabar, ThaumcraftOres.CINNABAR.get().asItem());
        test.assertTrue(cinnabar >= 33 && cinnabar <= 111, "cinnabar ore: 0.18 as well: " + cinnabar);
        int lucky = 0;
        for (int trial = 0; trial < n; ++trial) {
            ItemStack drop = Block.getDrops(Blocks.IRON_ORE.defaultBlockState(), level, pos, null, player, fortune).get(0);
            if (drop.is(clusterIron)) { ++lucky; test.assertTrue(drop.getCount() >= 1 && drop.getCount() <= 4, "the cluster takes the count of the drop: " + drop); }
            else test.assertTrue(drop.is(Items.RAW_IRON), "or the raw iron stays: " + drop);
        }
        test.assertTrue(lucky >= 120 && lucky <= 220, "fortune 3 lifts the chance to 0.425: " + lucky);
        test.assertTrue(clusters(level, pos, Blocks.IRON_ORE, player, new ItemStack(item("thaumcraft:thaumium_pickaxe")), n, clusterIron, Items.RAW_IRON) == 0, "the thaumium pickaxe never swaps");
        test.assertTrue(clusters(level, pos, Blocks.IRON_ORE, player, new ItemStack(Items.DIAMOND_PICKAXE), n, clusterIron, Items.RAW_IRON) == 0, "nor does a diamond pickaxe");
        test.assertTrue(clusters(level, pos, Blocks.IRON_ORE, player, new ItemStack(item("thaumcraft:elemental_axe")), n, clusterIron, Items.RAW_IRON) == 0, "nor the axe of the stream");
        test.assertTrue(clusters(level, pos, Blocks.IRON_ORE, null, pickaxe(), n, clusterIron, Items.RAW_IRON) == 0, "nobody mining, nothing swaps");
        Pig pig = test.spawnWithNoFreeWill(EntityType.PIG, 1, 2, 1);
        test.assertTrue(clusters(level, pos, Blocks.IRON_ORE, pig, pickaxe(), n, clusterIron, Items.RAW_IRON) == 0, "a pig is no miner");
        int silky = 0;
        for (int trial = 0; trial < n; ++trial) {
            if (Block.getDrops(ThaumcraftOres.CINNABAR.get().defaultBlockState(), level, pos, null, player, silk).get(0).is(clusterCinnabar)) ++silky;
        }
        test.assertTrue(silky == 0, "Silk Touch keeps the ore, the Bore's rule: " + silky);
        int coal = 0;
        for (int trial = 0; trial < 100; ++trial) {
            for (ItemStack drop : Block.getDrops(Blocks.COAL_ORE.defaultBlockState(), level, pos, null, player, pickaxe())) {
                if (!drop.is(Items.COAL)) ++coal;
            }
        }
        test.assertTrue(coal == 0, "ores with no cluster are left alone");
    }

    private static void breaking(GameTestHelper test, ServerLevel level, ServerPlayer player) {
        BlockPos pos = test.absolutePos(new BlockPos(2, 2, 2));
        AABB bounds = new AABB(pos).inflate(2);
        var before = level.getEntitiesOfClass(ItemEntity.class, bounds);
        ItemStack pick = pickaxe();
        player.setItemInHand(InteractionHand.MAIN_HAND, pick);
        int blocks = 120;
        for (int index = 0; index < blocks; ++index) {
            level.setBlock(pos, Blocks.IRON_ORE.defaultBlockState(), Block.UPDATE_ALL);
            test.assertTrue(player.gameMode.destroyBlock(pos) && level.getBlockState(pos).isAir(), "a survival break removes the ore");
        }
        int raw = 0, cluster = 0;
        for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, bounds)) {
            if (before.contains(drop)) continue;
            if (drop.getItem().is(Items.RAW_IRON)) raw += drop.getItem().getCount();
            else if (drop.getItem().is(item("thaumcraft:cluster_iron"))) cluster += drop.getItem().getCount();
            else throw new AssertionError("unexpected item " + drop.getItem());
            drop.discard();
        }
        test.assertTrue(raw + cluster == blocks && cluster >= 8 && cluster <= 45,
                "120 real breaks drop 120 pieces, about a fifth of them clusters: " + raw + " raw, " + cluster + " clusters");
        test.assertTrue(pick.getDamageValue() == blocks, "one use per block: " + pick.getDamageValue());
    }
}
