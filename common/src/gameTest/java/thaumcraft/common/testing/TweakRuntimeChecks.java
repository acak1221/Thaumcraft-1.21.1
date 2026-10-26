package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftBaubles;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.config.ThaumcraftLoot;
import thaumcraft.common.config.ThaumcraftShockOrbs;
import thaumcraft.common.config.ThaumcraftTweaks;
import thaumcraft.common.entities.projectile.EntityShockOrb;
import thaumcraft.common.items.ItemEssence;
import thaumcraft.common.lib.infusion.InfusionItemData;
import thaumcraft.common.lib.loot.LootTables;
import thaumcraft.api.internal.WeightedRandomLoot;
import thaumcraft.common.tiles.TileJar;

/**
 * The settings adopted from TC4Tweaks ({@link ThaumcraftTweaks}). Every check drives the actual
 * production path twice: with the tweak and with the original TC4 rule.
 */
public final class TweakRuntimeChecks {
    private TweakRuntimeChecks() {}

    /** Loot bags: a fresh enchantment for every book and a fresh charge for every vis amulet. */
    public static void loot(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        LootTables.initialize(); // the first draw would otherwise add the original pools to the test pool
        var common = WeightedRandomLoot.lootBagCommon;
        var uncommon = WeightedRandomLoot.lootBagUncommon;
        var rare = WeightedRandomLoot.lootBagRare;
        try {
            pool(new ItemStack(Items.BOOK));
            ItemStack pooled = WeightedRandomLoot.lootBagCommon.getFirst().item;
            Set<ItemEnchantments> books = new HashSet<>();
            try (var tweak = ThaumcraftTweaks.override(ThaumcraftTweaks.DEFAULTS)) {
                for (long seed = 0; seed < 16; seed++) {
                    ItemStack book = LootTables.generateLoot(level, 0, RandomSource.create(seed));
                    ItemEnchantments stored = book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
                    test.assertTrue(book.is(Items.ENCHANTED_BOOK) && !stored.isEmpty(), "every drawn book is enchanted: " + book);
                    books.add(stored);
                }
                test.assertTrue(WeightedRandomLoot.lootBagCommon.getFirst().item == pooled && pooled.is(Items.BOOK)
                        && pooled.getComponentsPatch().isEmpty(), "the pool keeps its own plain book");
                test.assertTrue(books.size() >= 2, "books drawn with different random draws differ: " + books.size());
            }
            try (var original = ThaumcraftTweaks.override(ThaumcraftTweaks.ORIGINAL)) {
                ItemStack first = LootTables.generateLoot(level, 0, RandomSource.create(1));
                test.assertTrue(!WeightedRandomLoot.lootBagCommon.getFirst().item.is(Items.BOOK),
                        "the original converts the pooled book once");
                for (long seed = 2; seed < 8; seed++)
                    test.assertTrue(ItemStack.matches(first, LootTables.generateLoot(level, 0, RandomSource.create(seed))),
                            "the original repeats its first enchantment");
            }

            ItemStack template = ThaumcraftLoot.lootAmulet();
            pool(template);
            var amulet = ThaumcraftBaubles.VIS_AMULET_LESSER.get();
            Set<List<Integer>> charges = new HashSet<>();
            try (var tweak = ThaumcraftTweaks.override(ThaumcraftTweaks.DEFAULTS)) {
                for (long seed = 0; seed < 16; seed++) {
                    ItemStack drawn = LootTables.generateLoot(level, 0, RandomSource.create(seed));
                    List<Integer> charge = new ArrayList<>();
                    for (Aspect primal : Aspect.getPrimalAspects()) {
                        int vis = amulet.getVis(drawn, primal);
                        test.assertTrue(vis >= 0 && vis <= 400 && vis % 100 == 0, "the original zero to four whole vis: " + vis);
                        charge.add(vis);
                    }
                    charges.add(charge);
                }
                test.assertTrue(charges.size() >= 2, "amulets drawn with different random draws differ: " + charges.size());
                test.assertTrue(ItemStack.matches(template, WeightedRandomLoot.lootBagCommon.getFirst().item),
                        "the pooled amulet keeps its charge");
            }
            try (var original = ThaumcraftTweaks.override(ThaumcraftTweaks.ORIGINAL)) {
                for (long seed = 0; seed < 4; seed++)
                    test.assertTrue(ItemStack.matches(template, LootTables.generateLoot(level, 0, RandomSource.create(seed))),
                            "the original hands out the one charge of the run");
            }
        } finally {
            WeightedRandomLoot.lootBagCommon = common;
            WeightedRandomLoot.lootBagUncommon = uncommon;
            WeightedRandomLoot.lootBagRare = rare;
        }
        test.succeed();
    }

    private static void pool(ItemStack only) {
        WeightedRandomLoot.lootBagCommon = new ArrayList<>();
        WeightedRandomLoot.lootBagUncommon = new ArrayList<>();
        WeightedRandomLoot.lootBagRare = new ArrayList<>();
        ThaumcraftApi.addLootBagItem(only, 1, 0);
    }

    /** Earth shock: by default the orb's burst and its field hurt creatures, never items or experience. */
    public static void earthShock(GameTestHelper test) {
        for (ThaumcraftTweaks.Values setting : List.of(ThaumcraftTweaks.DEFAULTS, ThaumcraftTweaks.ORIGINAL)) {
            boolean tweak = setting == ThaumcraftTweaks.DEFAULTS;
            var f = NodeTransductionRuntimeChecks.fixture(test, false);
            var owned = new ArrayList<Entity>();
            try (var scope = ThaumcraftTweaks.override(setting)) {
                f.open();
                f.put(f.base.below(), Blocks.STONE);
                Vec3 origin = Vec3.atCenterOf(f.base);
                var cow = EntityType.COW.create(f.level);
                cow.setPos(origin.add(1, 0, 0)); cow.setNoAi(true); cow.setNoGravity(true);
                ItemEntity item = new ItemEntity(f.level, origin.x - 1, origin.y, origin.z, new ItemStack(Items.DIAMOND));
                ExperienceOrb experience = new ExperienceOrb(f.level, origin.x, origin.y, origin.z + 1, 5);
                for (Entity entity : List.of(cow, item, experience)) { entity.setNoGravity(true); f.level.addFreshEntity(entity); owned.add(entity); }
                var orb = new EntityShockOrb(f.level, f.player);
                owned.add(orb); orb.configure(4, 5); orb.setPos(origin); f.level.addFreshEntity(orb);
                orb.setDeltaMovement(0, -1, 0); orb.tick();
                test.assertTrue(orb.isRemoved() && cow.getHealth() < cow.getMaxHealth(), "the orb bursts and hurts the cow (" + tweak + ")");
                test.assertTrue(item.isRemoved() != tweak && experience.isRemoved() != tweak,
                        "the burst " + (tweak ? "spares" : "destroys, as the original,") + " the dropped item and the experience orb: item="
                                + item.isRemoved() + ", xp=" + experience.isRemoved());

                BlockPos field = f.base.above(2);
                f.put(field, ThaumcraftShockOrbs.SHOCK_TRAP.get());
                var state = f.level.getBlockState(field);
                ItemEntity inside = new ItemEntity(f.level, field.getX() + .5, field.getY() + .1, field.getZ() + .5, new ItemStack(Items.EMERALD));
                inside.setNoGravity(true); f.level.addFreshEntity(inside); owned.add(inside);
                for (int hit = 0; hit < 10 && !inside.isRemoved(); hit++) state.entityInside(f.level, field, inside);
                test.assertTrue(inside.isRemoved() != tweak,
                        "the shock field " + (tweak ? "spares" : "destroys, as the original,") + " an item inside it: " + inside.isRemoved());
            } finally {
                for (Entity entity : owned) entity.discard();
                f.close();
            }
        }
        test.succeed();
    }

    /** Infusion keeps the centre item's data on armour and tools, and the recipe's own data wins. */
    public static void infusionData(GameTestHelper test) {
        var registry = test.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        ItemStack centre = new ItemStack(Items.IRON_SWORD);
        var sharp = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        sharp.set(registry.getHolderOrThrow(Enchantments.SHARPNESS), 3);
        centre.set(DataComponents.ENCHANTMENTS, sharp.toImmutable());
        centre.set(DataComponents.CUSTOM_NAME, Component.literal("Edge"));
        centre.setDamageValue(240);
        ItemStack result = new ItemStack(ThaumcraftEquipment.THAUMIUM_SWORD.get());

        try (var tweak = ThaumcraftTweaks.override(ThaumcraftTweaks.DEFAULTS)) {
            ItemStack kept = InfusionItemData.apply(centre, result);
            test.assertTrue(kept != result && result.getComponentsPatch().isEmpty(), "the recipe's own result is never changed");
            test.assertTrue(kept.getEnchantments().getLevel(registry.getHolderOrThrow(Enchantments.SHARPNESS)) == 3
                    && Component.literal("Edge").equals(kept.get(DataComponents.CUSTOM_NAME)) && kept.getDamageValue() == 240,
                    "the result keeps the centre's enchantment, name and wear: " + kept.getComponentsPatch());

            ItemStack enchanted = result.copy();
            var own = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            own.set(registry.getHolderOrThrow(Enchantments.UNBREAKING), 1);
            enchanted.set(DataComponents.ENCHANTMENTS, own.toImmutable());
            ItemStack recipeWins = InfusionItemData.apply(centre, enchanted);
            test.assertTrue(recipeWins.getEnchantments().equals(own.toImmutable()) && recipeWins.has(DataComponents.CUSTOM_NAME),
                    "a component the recipe sets itself stays the recipe's; the others are kept");

            ItemStack gold = InfusionItemData.apply(centre, new ItemStack(Items.GOLDEN_SWORD));
            test.assertTrue(gold.getDamageValue() == gold.getMaxDamage() - 1, "carried wear never breaks the result: " + gold.getDamageValue());

            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
            book.set(DataComponents.CUSTOM_NAME, Component.literal("Edge"));
            test.assertTrue(InfusionItemData.apply(book, result) == result
                    && InfusionItemData.apply(centre, new ItemStack(Items.DIAMOND)).getComponentsPatch().isEmpty(),
                    "only armour and tools on both sides take part");
        }
        try (var original = ThaumcraftTweaks.override(ThaumcraftTweaks.ORIGINAL)) {
            test.assertTrue(InfusionItemData.apply(centre, result) == result, "the original result is the recipe's own stack");
        }
        test.succeed();
    }

    /** Filling the last empty phial in hand keeps the filled one in that hand. */
    public static void filledToHand(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            f.mode(GameType.SURVIVAL);
            BlockPos jarPos = f.base;
            f.put(jarPos, ThaumcraftContent.JAR.get());
            TileJar jar = (TileJar) f.level.getBlockEntity(jarPos);
            var inventory = f.player.getInventory();
            for (ThaumcraftTweaks.Values setting : List.of(ThaumcraftTweaks.DEFAULTS, ThaumcraftTweaks.ORIGINAL)) {
                boolean tweak = setting == ThaumcraftTweaks.DEFAULTS;
                try (var scope = ThaumcraftTweaks.override(setting)) {
                    inventory.clearContent();
                    inventory.selected = 4;
                    jar.clearContents(); jar.addToContainer(Aspect.WATER, 16);
                    f.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftContent.EMPTY_PHIAL.get()));
                    test.assertTrue(f.use(jarPos, Direction.UP, InteractionHand.MAIN_HAND).consumesAction() && jar.getAmount() == 8, "the phial takes eight essentia");
                    ItemStack hand = inventory.getItem(4), first = inventory.getItem(0);
                    test.assertTrue(tweak ? hand.is(ThaumcraftContent.FILLED_PHIAL.get()) && first.isEmpty()
                                    : hand.isEmpty() && first.is(ThaumcraftContent.FILLED_PHIAL.get()),
                            "the filled phial goes " + (tweak ? "into the hand" : "to the first free slot, as the original") + ": hand=" + hand + ", first=" + first);

                    // A stack of the same filled phial with room is used first, as by the original.
                    inventory.clearContent();
                    inventory.setItem(7, ItemEssence.filled("aqua"));
                    f.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftContent.EMPTY_PHIAL.get()));
                    test.assertTrue(f.use(jarPos, Direction.UP, InteractionHand.MAIN_HAND).consumesAction() && jar.getAmount() == 0, "the second phial empties the jar");
                    test.assertTrue(inventory.getItem(7).getCount() == 2 && inventory.getItem(4).isEmpty(),
                            "a stack with room takes the filled phial: " + inventory.getItem(7) + ", hand=" + inventory.getItem(4));
                }
            }
            inventory.clearContent();
        }
        test.succeed();
    }
}
