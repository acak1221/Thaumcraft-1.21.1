/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [14 Sep 2013, 15:05:23 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.enchantment.core;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumic.tinkerer.client.lib.LibResources;
import thaumic.tinkerer.common.core.helper.MiscHelper;
import thaumic.tinkerer.common.enchantment.ModEnchantments;
import thaumic.tinkerer.common.enchantment.core.rule.BasicCompatibilityRule;
import thaumic.tinkerer.common.enchantment.core.rule.WhitelistCompatibilityRule;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibMisc;
import thaumic.tinkerer.common.lib.LibResearch;

/**
 * The table of the Enchanter: which enchantments it offers, what each level costs and which research opens it, and the
 * rules of which enchantments may stand together on one item.
 *
 * <p>The table is the one of 1.7.10: twenty-four vanilla enchantments, Haste and Repair of Thaumcraft and the fourteen of
 * Thaumic Tinkerer (enchantments of 1.21.1 that 1.7.10 did not have, such as Mending, are not offered, as they were not
 * in the original). The cost of level {@code i} is the cost of level one times {@code i * (1 + i * 0.2)}, cut to whole
 * numbers per aspect.</p>
 */
public final class EnchantmentManager {

    /** One row of the table: everything that does not depend on the level. */
    public record Entry(String texture, boolean vanilla, AspectList level1Aspects, String research) {}

    private static final Map<ResourceKey<Enchantment>, Entry> TABLE = new LinkedHashMap<>();
    private static final Map<ResourceKey<Enchantment>, Map<Integer, EnchantmentData>> DATA_CACHE = new HashMap<>();

    public static final Multimap<ResourceKey<Enchantment>, IEnchantmentRule> rules = ArrayListMultimap.create();

    private static boolean initialised;

    private EnchantmentManager() {}

    /** Fills the table and the rules. Needs the aspects of Thaumcraft, so it runs when Thaumcraft is ready. Idempotent. */
    public static synchronized void initEnchantmentData() {
        if (initialised) return;
        initialised = true;

        registerExponentialCostData(Enchantments.PROTECTION, LibResources.ENCHANT_PROTECTION, true,
                new AspectList().add(Aspect.EARTH, 10).add(Aspect.ENTROPY, 7));
        registerExponentialCostData(Enchantments.FIRE_PROTECTION, LibResources.ENCHANT_FIRE_PROTECTION, true,
                new AspectList().add(Aspect.FIRE, 10).add(Aspect.ENTROPY, 3).add(Aspect.WATER, 4));
        registerExponentialCostData(Enchantments.FEATHER_FALLING, LibResources.ENCHANT_FEATHER_FALLING, true,
                new AspectList().add(Aspect.AIR, 16).add(Aspect.ORDER, 5));
        registerExponentialCostData(Enchantments.BLAST_PROTECTION, LibResources.ENCHANT_BLAST_PROTECTION, true,
                new AspectList().add(Aspect.EARTH, 5).add(Aspect.FIRE, 5).add(Aspect.ENTROPY, 8));
        registerExponentialCostData(Enchantments.PROJECTILE_PROTECTION, LibResources.ENCHANT_PROJECTILE_PROTECTION, true,
                new AspectList().add(Aspect.AIR, 10).add(Aspect.ENTROPY, 7));
        registerExponentialCostData(Enchantments.RESPIRATION, LibResources.ENCHANT_RESPIRATION, true,
                new AspectList().add(Aspect.WATER, 10).add(Aspect.AIR, 8).add(Aspect.ORDER, 5));
        registerExponentialCostData(Enchantments.AQUA_AFFINITY, LibResources.ENCHANT_AQUA_AFFINITY, true,
                new AspectList().add(Aspect.WATER, 25).add(Aspect.ORDER, 20).add(Aspect.EARTH, 5));
        registerExponentialCostData(Enchantments.THORNS, LibResources.ENCHANT_THORNS, true,
                new AspectList().add(Aspect.EARTH, 10).add(Aspect.ENTROPY, 12));

        registerExponentialCostData(Enchantments.SHARPNESS, LibResources.ENCHANT_SHARPNESS, true,
                new AspectList().add(Aspect.ORDER, 10));
        registerExponentialCostData(Enchantments.SMITE, LibResources.ENCHANT_SMITE, true,
                new AspectList().add(Aspect.ORDER, 5).add(Aspect.AIR, 5));
        registerExponentialCostData(Enchantments.BANE_OF_ARTHROPODS, LibResources.ENCHANT_BANE_OF_ARTHROPODS, true,
                new AspectList().add(Aspect.ORDER, 5).add(Aspect.FIRE, 5));
        registerExponentialCostData(Enchantments.KNOCKBACK, LibResources.ENCHANT_KNOCKBACK, true,
                new AspectList().add(Aspect.ENTROPY, 5).add(Aspect.AIR, 10));
        registerExponentialCostData(Enchantments.FIRE_ASPECT, LibResources.ENCHANT_FIRE_ASPECT, true,
                new AspectList().add(Aspect.FIRE, 15).add(Aspect.EARTH, 4));
        registerExponentialCostData(Enchantments.LOOTING, LibResources.ENCHANT_LOOTING, true,
                new AspectList().add(Aspect.AIR, 10).add(Aspect.FIRE, 10).add(Aspect.WATER, 10).add(Aspect.EARTH, 10)
                        .add(Aspect.ORDER, 15).add(Aspect.ENTROPY, 15));

        registerExponentialCostData(Enchantments.EFFICIENCY, LibResources.ENCHANT_EFFICIENCY, true,
                new AspectList().add(Aspect.ENTROPY, 12).add(Aspect.EARTH, 4));
        registerExponentialCostData(Enchantments.SILK_TOUCH, LibResources.ENCHANT_SILK_TOUCH, true,
                new AspectList().add(Aspect.ORDER, 50).add(Aspect.EARTH, 10).add(Aspect.ENTROPY, 10));
        registerExponentialCostData(Enchantments.UNBREAKING, LibResources.ENCHANT_UNBREAKING, true,
                new AspectList().add(Aspect.ORDER, 15).add(Aspect.WATER, 8).add(Aspect.EARTH, 8));
        registerExponentialCostData(Enchantments.FORTUNE, LibResources.ENCHANT_FORTUNE, true,
                new AspectList().add(Aspect.AIR, 10).add(Aspect.FIRE, 10).add(Aspect.WATER, 10).add(Aspect.EARTH, 10)
                        .add(Aspect.ORDER, 15).add(Aspect.ENTROPY, 15));

        registerExponentialCostData(Enchantments.POWER, LibResources.ENCHANT_POWER, true,
                new AspectList().add(Aspect.EARTH, 5).add(Aspect.ORDER, 10));
        registerExponentialCostData(Enchantments.PUNCH, LibResources.ENCHANT_PUNCH, true,
                new AspectList().add(Aspect.AIR, 4).add(Aspect.EARTH, 10).add(Aspect.ENTROPY, 5));
        registerExponentialCostData(Enchantments.FLAME, LibResources.ENCHANT_FLAME, true,
                new AspectList().add(Aspect.ENTROPY, 5).add(Aspect.FIRE, 20).add(Aspect.EARTH, 5));
        registerExponentialCostData(Enchantments.INFINITY, LibResources.ENCHANT_INFINITY, true,
                new AspectList().add(Aspect.ENTROPY, 40).add(Aspect.ORDER, 40).add(Aspect.EARTH, 10));

        // 1.7.10 field_151369_A (Lure) and field_151370_z (Luck of the Sea). Beast is not a primal aspect, so the wand
        // is never asked for it (the Enchanter only collects the six primals); kept as it was.
        registerExponentialCostData(Enchantments.LURE, LibResources.ENCHANT_LURE, true,
                new AspectList().add(Aspect.WATER, 20).add(Aspect.BEAST, 20));
        registerExponentialCostData(Enchantments.LUCK_OF_THE_SEA, LibResources.ENCHANT_LUCK_OF_THE_SEA, true,
                new AspectList().add(Aspect.ENTROPY, 20).add(Aspect.WATER, 20));

        registerExponentialCostData(ThaumcraftApi.ENCHANTMENT_HASTE, LibResources.ENCHANT_HASTE, true,
                new AspectList().add(Aspect.AIR, 10).add(Aspect.ENTROPY, 5).add(Aspect.EARTH, 5));
        registerExponentialCostData(ThaumcraftApi.ENCHANTMENT_REPAIR, LibResources.ENCHANT_REPAIR, true,
                new AspectList().add(Aspect.WATER, 20).add(Aspect.FIRE, 20).add(Aspect.EARTH, 20).add(Aspect.AIR, 20)
                        .add(Aspect.ORDER, 20).add(Aspect.ENTROPY, 5));

        registerExponentialCostData(ModEnchantments.ascentBoost, LibResources.ENCHANT_ASCENT_BOOST, false,
                new AspectList().add(Aspect.ENTROPY, 8).add(Aspect.AIR, 10), LibResearch.KEY_ENCHANT_ASCENT_BOOST);
        registerExponentialCostData(ModEnchantments.slowFall, LibResources.ENCHANT_SLOW_FALL, false,
                new AspectList().add(Aspect.ORDER, 8).add(Aspect.AIR, 10), LibResearch.KEY_ENCHANT_SLOW_FALL);
        registerExponentialCostData(ModEnchantments.autoSmelt, LibResources.ENCHANT_AUTO_SMELT, false,
                new AspectList().add(Aspect.ENTROPY, 20).add(Aspect.FIRE, 30), LibResearch.KEY_ENCHANT_AUTO_SMELT);
        registerExponentialCostData(ModEnchantments.desintegrate, LibResources.ENCHANT_DESINTEGRATE, false,
                new AspectList().add(Aspect.ENTROPY, 25).add(Aspect.AIR, 10).add(Aspect.EARTH, 10),
                LibResearch.KEY_ENCHANT_DESINTEGRATE);
        registerExponentialCostData(ModEnchantments.quickDraw, LibResources.ENCHANT_QUICK_DRAW, false,
                new AspectList().add(Aspect.ORDER, 10).add(Aspect.AIR, 10).add(Aspect.WATER, 5),
                LibResearch.KEY_ENCHANT_QUICK_DRAW);
        registerExponentialCostData(ModEnchantments.vampirism, LibResources.ENCHANT_VAMPIRISM, false,
                new AspectList().add(Aspect.ENTROPY, 8).add(Aspect.FIRE, 10).add(Aspect.WATER, 10),
                LibResearch.KEY_ENCHANT_VAMPIRISM);

        // The research of the entries below is Vampirism in the original (a copy-and-paste habit of 1.7.10 that the
        // port keeps): the researches TTENCH_FOCUSED, TTENCH_DISPERSED, ... exist in the book, but the Enchanter only asks
        // for TTENCH_VAMPIRISM.
        registerExponentialCostData(ModEnchantments.focusedStrike, LibResources.ENCHANT_FOCUSED_STRIKE, false,
                new AspectList().add(Aspect.ORDER, 12).add(Aspect.AIR, 10).add(Aspect.WATER, 10),
                LibResearch.KEY_ENCHANT_VAMPIRISM);
        registerExponentialCostData(ModEnchantments.dispersedStrikes, LibResources.ENCHANT_DISPERSED_STRIKE, false,
                new AspectList().add(Aspect.ENTROPY, 12).add(Aspect.FIRE, 10).add(Aspect.EARTH, 10),
                LibResearch.KEY_ENCHANT_VAMPIRISM);
        registerExponentialCostData(ModEnchantments.valiance, LibResources.ENCHANT_VALIANCE, false,
                new AspectList().add(Aspect.ORDER, 12).add(Aspect.FIRE, 10).add(Aspect.EARTH, 10),
                LibResearch.KEY_ENCHANT_VAMPIRISM);
        registerExponentialCostData(ModEnchantments.finalStrike, LibResources.ENCHANT_FINAL_STRIKE, false,
                new AspectList().add(Aspect.ENTROPY, 16).add(Aspect.FIRE, 16), LibResearch.KEY_ENCHANT_VAMPIRISM);
        registerExponentialCostData(ModEnchantments.tunnel, LibResources.ENCHANT_TUNNEL, false,
                new AspectList().add(Aspect.EARTH, 16).add(Aspect.ORDER, 16), LibResearch.KEY_ENCHANT_VAMPIRISM);

        registerExponentialCostData(ModEnchantments.shatter, LibResources.ENCHANT_SHATTER, false,
                new AspectList().add(Aspect.EARTH, 16).add(Aspect.ENTROPY, 16), LibResearch.KEY_ENCHANT_VAMPIRISM);
        registerExponentialCostData(ModEnchantments.shockwave, LibResources.ENCHANT_SHOCKWAVE, false,
                new AspectList().add(Aspect.EARTH, 16).add(Aspect.AIR, 16), LibResearch.KEY_ENCHANT_VAMPIRISM);
        registerExponentialCostData(ModEnchantments.pounce, LibResources.ENCHANT_POUNCE, false,
                new AspectList().add(Aspect.EARTH, 16).add(Aspect.AIR, 16), LibResearch.KEY_ENCHANT_VAMPIRISM);

        registerCompatibilityRules();
        registerExtraRules();
    }

    // ---- the table ------------------------------------------------------------------------------------------------

    /** The enchantments the Enchanter knows, in the order of the table. */
    public static List<ResourceKey<Enchantment>> enchantments() {
        return Collections.unmodifiableList(new ArrayList<>(TABLE.keySet()));
    }

    public static boolean isKnown(ResourceKey<Enchantment> enchantment) {
        return TABLE.containsKey(enchantment);
    }

    /** The row of the table, null for an enchantment the Enchanter does not know. */
    @Nullable
    public static Entry entry(ResourceKey<Enchantment> enchantment) {
        return TABLE.get(enchantment);
    }

    /**
     * The icon, cost and research of one level of an enchantment ({@code enchantmentData.get(id).get(level)} of the
     * original), null when the Enchanter does not know the enchantment or the level is below one.
     */
    @Nullable
    public static synchronized EnchantmentData data(ResourceKey<Enchantment> enchantment, int level) {
        Entry entry = TABLE.get(enchantment);
        if (entry == null || level < 1) return null;
        return DATA_CACHE.computeIfAbsent(enchantment, key -> new HashMap<>()).computeIfAbsent(level, lvl -> new EnchantmentData(
                entry.texture(), entry.vanilla(), costOf(entry.level1Aspects(), lvl), entry.research()));
    }

    /** The vis of one level: level one times {@code i * (1 + i * 0.2)}, per aspect cut to a whole number. */
    public static AspectList costOf(AspectList level1Aspects, int level) {
        double i = level;
        return MiscHelper.multiplyAspectList(level1Aspects, i * (1D + i * 0.2));
    }

    // ---- who may apply what ---------------------------------------------------------------------------------------

    /**
     * {@code canApply(stack, enchant, currentEnchants)}: whether the enchantment may be added to the stack next to the ones
     * already chosen. The item has to be one the enchantment is made for (the item tag of its data), the enchantment must
     * not be chosen yet, and neither the rules of Thaumic Tinkerer nor the exclusive sets of the data pack may forbid it
     * next to a chosen one.
     */
    public static boolean canApply(@Nullable RegistryAccess access, ItemStack stack, ResourceKey<Enchantment> enchantment,
            List<ResourceKey<Enchantment>> currentEnchants) {
        if (stack.isEmpty() || currentEnchants.contains(enchantment)) return false;

        Optional<Holder.Reference<Enchantment>> holder = ModEnchantments.holder(access, enchantment);
        if (holder.isEmpty() || !holder.get().value().isSupportedItem(stack)) return false;
        if (!allowsItem(enchantment, stack)) return false;

        if (rulesForbid(enchantment, currentEnchants)) return false;
        for (ResourceKey<Enchantment> other : currentEnchants) {
            Optional<Holder.Reference<Enchantment>> otherHolder = ModEnchantments.holder(access, other);
            if (otherHolder.isPresent() && !Enchantment.areCompatible(holder.get(), otherHolder.get())) return false;
        }
        return true;
    }

    /**
     * Whether the rules of Thaumic Tinkerer (the {@code canApplyTogether} of its fourteen enchantments) forbid the enchantment
     * next to the chosen ones, from either side of the pair: 1.7.10 asked both enchantments of every pair, so a rule of the
     * enchantment that is already chosen forbids the new one in the same way. The exclusive sets of the data pack are asked
     * separately ({@link Enchantment#areCompatible}).
     */
    public static boolean rulesForbid(ResourceKey<Enchantment> enchantment, List<ResourceKey<Enchantment>> currentEnchants) {
        for (IEnchantmentRule rule : rules.get(enchantment)) {
            if (rule.cantApplyAlongside(currentEnchants)) return true;
        }
        for (ResourceKey<Enchantment> other : currentEnchants) {
            for (IEnchantmentRule rule : rules.get(other)) {
                if (rule.cantApplyAlongside(List.of(enchantment))) return true;
            }
        }
        return false;
    }

    /**
     * The items an enchantment is refused on although the item tag of its data lists them: Flaming Touch on the Axe of the
     * Stream, Vampirism on the Cursed Spirit's Blade, Shockwave and Slow Fall on the gem boots of the Kami armor. (The
     * tags of the data leave these items out already; this keeps the rule when another data pack adds them.)
     */
    public static boolean allowsItem(ResourceKey<Enchantment> enchantment, ItemStack stack) {
        ResourceLocation item = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (enchantment.equals(ModEnchantments.autoSmelt)) {
            return !item.equals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "elemental_axe"));
        }
        if (enchantment.equals(ModEnchantments.vampirism)) {
            return !item.equals(LibMisc.id(LibItemNames.BLOOD_SWORD));
        }
        if (enchantment.equals(ModEnchantments.shockwave) || enchantment.equals(ModEnchantments.slowFall)) {
            return !item.equals(LibMisc.id(LibItemNames.ICHOR_BOOTS_GEM));
        }
        return true;
    }

    /**
     * {@code canEnchantmentBeUsed(player, enchant)}: the Enchanter offers an enchantment to a player who finished the research
     * of its entry (none for the vanilla ones).
     */
    public static boolean canEnchantmentBeUsed(Player player, ResourceKey<Enchantment> enchantment) {
        EnchantmentData data = data(enchantment, 1);
        if (data == null) return false;
        return data.research.isEmpty() || ThaumcraftApiHelper.isResearchComplete(player, data.research);
    }

    /**
     * The first {@code limit} enchantments (in the order of the table) the Enchanter would offer for the stack to this
     * player next to the ones already chosen: the list of buttons of its screen. Nothing for an item that is already
     * enchanted or has no enchantability ({@code asignEnchantButtons}).
     */
    public static List<ResourceKey<Enchantment>> offered(Player player, ItemStack stack,
            List<ResourceKey<Enchantment>> chosen, int limit) {
        List<ResourceKey<Enchantment>> offered = new ArrayList<>();
        if (stack.isEmpty() || stack.isEnchanted() || stack.getItem().getEnchantmentValue() == 0) return offered;

        RegistryAccess access = player.level().registryAccess();
        for (ResourceKey<Enchantment> enchantment : TABLE.keySet()) {
            if (canApply(access, stack, enchantment, chosen) && canEnchantmentBeUsed(player, enchantment)) {
                offered.add(enchantment);
                if (offered.size() >= limit) break;
            }
        }
        return offered;
    }

    // ---- registration ---------------------------------------------------------------------------------------------

    private static void registerExponentialCostData(ResourceKey<Enchantment> enchantment, String texture, boolean vanilla,
            AspectList level1Aspects) {
        registerExponentialCostData(enchantment, texture, vanilla, level1Aspects, "");
    }

    private static void registerExponentialCostData(ResourceKey<Enchantment> enchantment, String texture, boolean vanilla,
            AspectList level1Aspects, String research) {
        TABLE.put(enchantment, new Entry(texture, vanilla, level1Aspects, research));
    }

    /**
     * The {@code canApplyTogether} of the enchantments of Thaumic Tinkerer. 1.7.10 turned the method of every pair of the
     * enchantments registered at that moment into a rule; the port writes the rules of the fourteen, and the exclusive
     * sets of the data cover the pairs between the other enchantments.
     */
    private static void registerCompatibilityRules() {
        ResourceKey<Enchantment> unbreaking = Enchantments.UNBREAKING;
        ResourceKey<Enchantment> repair = ThaumcraftApi.ENCHANTMENT_REPAIR;

        // SlowFall: everything but Feather Falling (and, from Shockwave's rule, Shockwave).
        rules.put(ModEnchantments.slowFall, new BasicCompatibilityRule(Enchantments.FEATHER_FALLING));

        // Flaming Touch and Disintegrate: only Unbreaking and Repair.
        rules.put(ModEnchantments.autoSmelt, new WhitelistCompatibilityRule(Set.of(unbreaking, repair)));
        rules.put(ModEnchantments.desintegrate, new WhitelistCompatibilityRule(Set.of(unbreaking, repair)));

        // Dispersed and Focused Strikes: Unbreaking, Repair, Knockback and Looting.
        rules.put(ModEnchantments.dispersedStrikes,
                new WhitelistCompatibilityRule(Set.of(unbreaking, repair, Enchantments.KNOCKBACK, Enchantments.LOOTING)));
        rules.put(ModEnchantments.focusedStrike,
                new WhitelistCompatibilityRule(Set.of(unbreaking, repair, Enchantments.KNOCKBACK, Enchantments.LOOTING)));

        // Final Strike and Valiance: Unbreaking, Repair, Sharpness and Smite.
        rules.put(ModEnchantments.finalStrike,
                new WhitelistCompatibilityRule(Set.of(unbreaking, repair, Enchantments.SHARPNESS, Enchantments.SMITE)));
        rules.put(ModEnchantments.valiance,
                new WhitelistCompatibilityRule(Set.of(unbreaking, repair, Enchantments.SHARPNESS, Enchantments.SMITE)));

        // Quick Draw: not with Punch.
        rules.put(ModEnchantments.quickDraw, new BasicCompatibilityRule(Enchantments.PUNCH));

        // Shatter: not with Efficiency and Disintegrate. Tunnel: not with Shatter and Efficiency.
        rules.put(ModEnchantments.shatter, new BasicCompatibilityRule(Enchantments.EFFICIENCY));
        rules.put(ModEnchantments.shatter, new BasicCompatibilityRule(ModEnchantments.desintegrate));
        rules.put(ModEnchantments.tunnel, new BasicCompatibilityRule(ModEnchantments.shatter));
        rules.put(ModEnchantments.tunnel, new BasicCompatibilityRule(Enchantments.EFFICIENCY));

        // Shockwave: not with Feather Falling and Slow Fall.
        rules.put(ModEnchantments.shockwave, new BasicCompatibilityRule(Enchantments.FEATHER_FALLING));
        rules.put(ModEnchantments.shockwave, new BasicCompatibilityRule(ModEnchantments.slowFall));

        // Vampirism: not with Fire Aspect and Knockback.
        rules.put(ModEnchantments.vampirism, new BasicCompatibilityRule(Enchantments.FIRE_ASPECT));
        rules.put(ModEnchantments.vampirism, new BasicCompatibilityRule(Enchantments.KNOCKBACK));

        // Ascent Boost and Pounce have no rule beyond "not twice".
    }

    private static void registerExtraRules() {}
}
