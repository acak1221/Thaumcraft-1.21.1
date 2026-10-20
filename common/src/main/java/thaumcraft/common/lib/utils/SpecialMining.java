package thaumcraft.common.lib.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import thaumcraft.common.items.equipment.ItemElementalPickaxe;

/**
 * Original special mining results ({@code Utils.addSpecialMiningResult} / {@code findSpecialMiningResult}, Config:799-801, applied by
 * {@code EventHandlerWorld.harvestEvent} and the Arcane Bore): what a block drops can be swapped for a native cluster. A drop listed here
 * becomes its cluster, with the same count, when a roll of {@code 0.2 + fortune x 0.075} (times the entry's own factor) succeeds. It runs on
 * the drop list of {@code Block.getDrops(.. entity, tool)} when a player mines with the Pickaxe of the Core. The Primal Crusher, a wand
 * focus upgraded with Dowsing and the Arcane Bore holding that pickaxe are the other original carriers: the first two are not ported, and
 * the Bore's work still refuses any pickaxe but the vanilla ones and the Thaumium pickaxe (its actor would pass this rule as it stands).
 */
public final class SpecialMining {
    /** The roll with no fortune, original {@code 0.2f}. */
    public static final float BASE_CHANCE = 0.2F;
    /** Added per fortune level, original {@code 0.075f}. */
    public static final float FORTUNE_STEP = 0.075F;
    /** The iron cluster's own factor 1.0, the gold and cinnabar clusters' 0.9 (Config:799-801). */
    public static final float IRON = 1.0F, GOLD = 0.9F, CINNABAR = 0.9F;

    /** A drop's replacement and the factor on the roll. */
    public record Result(ResourceLocation output, float factor) {}

    private static final Map<ResourceLocation, Result> RESULTS = Map.of(
            // 1.21.1 iron and gold ore drop raw ore where 1.7.10's dropped the ore block itself; the raw items stand in for that drop.
            ResourceLocation.withDefaultNamespace("raw_iron"), new Result(thaumcraft("cluster_iron"), IRON),
            ResourceLocation.withDefaultNamespace("raw_gold"), new Result(thaumcraft("cluster_gold"), GOLD),
            thaumcraft("cinnabar_ore"), new Result(thaumcraft("cluster_cinnabar"), CINNABAR));

    private SpecialMining() {}

    private static ResourceLocation thaumcraft(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }

    /** The chance of a swap for a tool of this fortune level, before the entry's factor. */
    public static float chance(int fortune) { return BASE_CHANCE + fortune * FORTUNE_STEP; }

    /** Original {@code r <= chance * specialMiningChance}: a roll exactly on the threshold still succeeds. */
    public static boolean succeeds(float roll, float chance, float factor) { return roll <= chance * factor; }

    /** What the table says a dropped item turns into, if anything. */
    public static Result result(ItemStack stack) { return RESULTS.get(BuiltInRegistries.ITEM.getKey(stack.getItem())); }

    /** Whether this tool carries the original special mining. */
    public static boolean carries(ItemStack tool) { return tool.getItem() instanceof ItemElementalPickaxe; }

    /**
     * Original {@code findSpecialMiningResult}: one roll per drop (taken even when nothing is listed, as the original does), and on success
     * the listed cluster with the dropped count.
     */
    public static ItemStack find(ItemStack dropped, float chance, RandomSource random) {
        float roll = random.nextFloat();
        Result result = result(dropped);
        if (result == null || !succeeds(roll, chance, result.factor())) return dropped.copy();
        Item output = BuiltInRegistries.ITEM.get(result.output());
        return new ItemStack(output, dropped.getCount());
    }

    /**
     * The drops of one broken block after the special mining. Only a player mining with a carrying tool without Silk Touch is affected
     * (the Bore's own rule; the original player event had no Silk Touch check: declared deviation); the list is copied only when a swap
     * happens, and each swap plays the original orb sound.
     */
    public static List<ItemStack> apply(List<ItemStack> drops, ServerLevel level, BlockPos pos, Entity miner, ItemStack tool) {
        if (!(miner instanceof Player) || drops.isEmpty() || !carries(tool)) return drops;
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        if (EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(Enchantments.SILK_TOUCH), tool) > 0) return drops;
        int fortune = EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(Enchantments.FORTUNE), tool);
        float chance = chance(fortune);
        List<ItemStack> swapped = null;
        for (int index = 0; index < drops.size(); ++index) {
            ItemStack original = drops.get(index);
            ItemStack replaced = find(original, chance, level.random);
            if (ItemStack.isSameItemSameComponents(original, replaced)) continue;
            if (swapped == null) swapped = new ArrayList<>(drops);
            swapped.set(index, replaced);
            level.playSound(null, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS,
                    0.2F, 0.7F + level.random.nextFloat() * 0.2F);
        }
        return swapped == null ? drops : swapped;
    }
}
