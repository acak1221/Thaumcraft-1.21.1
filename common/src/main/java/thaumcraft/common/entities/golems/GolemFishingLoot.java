package thaumcraft.common.entities.golems;

import java.util.List;
import java.util.Objects;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import thaumcraft.common.items.golems.GolemUpgrade;
import thaumcraft.common.items.golems.GolemUpgrades;

/**
 * Finite ordinary TC4 fishing results, not permission to produce or grant a catch.
 * The caller owns world sampling, cast success, protection checks and one-time output.
 * The golem's upgrades enter only through the three chances (Entropy lowers junk, Order raises treasure, original
 * AIFish:183-184) and the catch count (Air, AIFish:118-120); the random source is always the caller's.
 */
public final class GolemFishingLoot {
    private static final Entry[] FISH = {
        new Entry(Items.COD, 60),
        new Entry(Items.SALMON, 25),
        new Entry(Items.TROPICAL_FISH, 2),
        new Entry(Items.PUFFERFISH, 13)
    };

    private static final Entry[] JUNK = {
        new Entry(Items.LEATHER_BOOTS, 1, 10, 0.9f, false),
        new Entry(Items.LEATHER, 10),
        new Entry(Items.BONE, 10),
        new Entry(Items.POTION, 10),
        new Entry(Items.STRING, 5),
        new Entry(Items.FISHING_ROD, 1, 2, 0.9f, false),
        new Entry(Items.BOWL, 10),
        new Entry(Items.STICK, 5),
        new Entry(Items.INK_SAC, 10, 5, 0.0f, false),
        new Entry(Items.TRIPWIRE_HOOK, 10),
        new Entry(Items.ROTTEN_FLESH, 10)
    };

    private static final Entry[] TREASURE = {
        new Entry(Items.LILY_PAD, 1),
        new Entry(Items.NAME_TAG, 1),
        new Entry(Items.SADDLE, 1),
        new Entry(Items.BOW, 1, 1, 0.25f, true),
        new Entry(Items.FISHING_ROD, 1, 1, 0.25f, true),
        new Entry(Items.BOOK, 1, 1, 0.0f, true)
    };

    // Classic 1.7 vanilla IDs in ascending order; native compatibility remains authoritative.
    // TC4's additional enchantments are not replaced with newer vanilla enchantments.
    private static final List<ResourceKey<Enchantment>> CLASSIC_ENCHANTMENTS = List.of(
        Enchantments.PROTECTION,
        Enchantments.FIRE_PROTECTION,
        Enchantments.FEATHER_FALLING,
        Enchantments.BLAST_PROTECTION,
        Enchantments.PROJECTILE_PROTECTION,
        Enchantments.RESPIRATION,
        Enchantments.AQUA_AFFINITY,
        Enchantments.THORNS,
        Enchantments.SHARPNESS,
        Enchantments.SMITE,
        Enchantments.BANE_OF_ARTHROPODS,
        Enchantments.KNOCKBACK,
        Enchantments.FIRE_ASPECT,
        Enchantments.LOOTING,
        Enchantments.EFFICIENCY,
        Enchantments.SILK_TOUCH,
        Enchantments.UNBREAKING,
        Enchantments.FORTUNE,
        Enchantments.POWER,
        Enchantments.PUNCH,
        Enchantments.FLAME,
        Enchantments.INFINITY,
        Enchantments.LUCK_OF_THE_SEA,
        Enchantments.LURE
    );

    private GolemFishingLoot() {
    }

    public record Pond(int adjacentWater, int skyWater, int depthWater) {
        public Pond {
            if (adjacentWater < 0 || adjacentWater > 4
                || skyWater < 0 || skyWater > adjacentWater
                || depthWater < 0 || depthWater > 3 * adjacentWater) {
                throw new IllegalArgumentException("Invalid fishing pond counts: A="
                    + adjacentWater + ", S=" + skyWater + ", D=" + depthWater);
            }
        }

        float quality() {
            return 0.00003f * (adjacentWater + skyWater) + 0.000015f * depthWater;
        }

        /** Junk chance with no Entropy upgrade. */
        public float junkChance() { return junkChance(0); }

        /** Junk chance with n Entropy upgrades: {@code 0.1 - 0.025 n}, minus the pond's water-and-sky bonus (AIFish:183). */
        public float junkChance(int entropy) {
            return Mth.clamp(GolemUpgrades.fishJunkBase(entropy) - 0.005f * (adjacentWater + skyWater), 0.0f, 1.0f);
        }

        /** Treasure chance with no Order upgrade. */
        public float treasureChance() { return treasureChance(0); }

        /** Treasure chance with n Order upgrades: {@code 0.05 + 0.0125 n}, plus the pond's water, sky and depth bonus (AIFish:184). */
        public float treasureChance(int order) {
            return Mth.clamp(GolemUpgrades.fishTreasureBase(order) + 0.00125f * (adjacentWater + skyWater)
                + 0.001f * depthWater, 0.0f, 1.0f);
        }
    }

    /** Returns one fresh result after an already-authorized successful cast; never spawns it. */
    public static ItemStack rollCatch(ServerLevel level, RandomSource random, Pond pond) {
        return rollCatch(level, random, pond, 0, 0);
    }

    /** One fresh result for a golem with the given Entropy and Order upgrade counts; never spawns it. */
    public static ItemStack rollCatch(ServerLevel level, RandomSource random, Pond pond, int entropy, int order) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(random, "random");
        Objects.requireNonNull(pond, "pond");

        float category = random.nextFloat();
        Entry[] pool = switch (GolemUpgrades.fishCategory(category, pond.junkChance(entropy), pond.treasureChance(order))) {
            case JUNK -> JUNK;
            case TREASURE -> TREASURE;
            case FISH -> FISH;
        };
        return createStack(level, random, choose(random, pool));
    }

    /**
     * The result a golem publishes for one catch: rolled with its own Entropy and Order upgrades, then cooked when it carries
     * Fire. This is the single call the Fishing goal makes per catch.
     */
    public static ItemStack nextCatch(ServerLevel level, RandomSource random, EntityGolemBase golem, Pond pond) {
        Objects.requireNonNull(golem, "golem");
        ItemStack rolled = rollCatch(level, random, pond, golem.getUpgradeAmount(GolemUpgrade.ENTROPY), golem.getUpgradeAmount(GolemUpgrade.ORDER));
        return GolemFishingSmelting.apply(level, golem, rolled);
    }

    /** How many results one success gives this golem: 1, or 2 by chance with Air (original AIFish:118-120). */
    public static int catchCount(RandomSource random, EntityGolemBase golem) {
        Objects.requireNonNull(random, "random");
        Objects.requireNonNull(golem, "golem");
        return GolemUpgrades.fishCatches(golem.getUpgradeAmount(GolemUpgrade.AIR), random::nextInt);
    }

    private static Entry choose(RandomSource random, Entry[] pool) {
        int totalWeight = 0;
        for (Entry entry : pool) {
            totalWeight += entry.weight();
        }
        int choice = random.nextInt(totalWeight);
        for (Entry entry : pool) {
            choice -= entry.weight();
            if (choice < 0) {
                return entry;
            }
        }
        throw new IllegalStateException("Invalid fixed fishing loot pool");
    }

    private static ItemStack createStack(ServerLevel level, RandomSource random, Entry entry) {
        ItemStack stack = new ItemStack(entry.item(), entry.count());
        if (entry.item() == Items.POTION) {
            stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));
        }
        if (entry.damageParameter() > 0.0f) {
            // WeightedRandomFishable.func_150708_a: two nested draws, then clamp DAMAGE.
            int maxDamage = stack.getMaxDamage();
            int cap = (int) (entry.damageParameter() * maxDamage);
            int damage = maxDamage - random.nextInt(random.nextInt(cap) + 1);
            stack.setDamageValue(Math.max(1, Math.min(cap, damage)));
        }
        if (entry.enchant()) {
            Registry<Enchantment> enchantments = level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);
            // Retain the returned stack: BOOK becomes a fresh ENCHANTED_BOOK with stored enchants.
            stack = EnchantmentHelper.enchantItem(random, stack, 30,
                CLASSIC_ENCHANTMENTS.stream().<Holder<Enchantment>>map(enchantments::getHolderOrThrow));
            if (EnchantmentHelper.getEnchantmentsForCrafting(stack).isEmpty()) {
                throw new IllegalStateException("No classic fishing enchantment available in native registry");
            }
        }
        return stack;
    }

    private record Entry(Item item, int count, int weight, float damageParameter, boolean enchant) {
        private Entry(Item item, int weight) {
            this(item, 1, weight, 0.0f, false);
        }
    }
}
