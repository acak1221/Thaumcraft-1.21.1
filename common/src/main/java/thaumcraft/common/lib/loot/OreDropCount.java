package thaumcraft.common.lib.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import thaumcraft.common.config.ThaumcraftOres;

/** TC4's uniform 1..(base+fortune), not vanilla ore_drops or a sum of two rolls. */
public final class OreDropCount extends LootItemConditionalFunction {
    public static final MapCodec<OreDropCount> CODEC = RecordCodecBuilder.mapCodec(instance ->
            commonFields(instance).and(Codec.intRange(1, 2).fieldOf("base_bound").forGetter(function -> function.baseBound))
                    .apply(instance, OreDropCount::new));
    private final int baseBound;

    private OreDropCount(List<LootItemCondition> conditions, int baseBound) {
        super(conditions);
        this.baseBound = baseBound;
    }

    @Override public LootItemFunctionType<OreDropCount> getType() { return ThaumcraftOres.ORE_DROP_COUNT.get(); }
    @Override public Set<LootContextParam<?>> getReferencedContextParams() { return Set.of(LootContextParams.TOOL); }

    @Override protected ItemStack run(ItemStack stack, LootContext context) {
        ItemStack tool = context.getParamOrNull(LootContextParams.TOOL);
        int fortune = tool == null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(context.getLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.FORTUNE), tool);
        // Original BlockCustomOre:137,139 draws from world.rand, including nextInt(1)
        // for unenchanted amber. XP has its own independent RNG in the block.
        stack.setCount(1 + context.getLevel().random.nextInt(baseBound + Math.max(0, fortune)));
        return stack;
    }
}
