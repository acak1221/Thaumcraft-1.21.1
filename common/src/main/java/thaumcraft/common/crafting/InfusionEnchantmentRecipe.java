package thaumcraft.common.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import thaumcraft.api.IRepairable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.lib.crafting.InfusionMatching;
import thaumcraft.common.lib.infusion.InfusionEnchantmentRules;

/** Original enchantment infusion: one level added to the actual centre after XP, essentia and component payments. */
public record InfusionEnchantmentRecipe(String research, ResourceLocation enchantment, List<Ingredient> components,
                                        Map<String, Integer> aspects, int instability, int firstLevelMinimumCost)
        implements Recipe<InfusionRecipeInput> {
    private static final Codec<String> ASPECT = Codec.STRING.comapFlatMap(tag -> Aspect.getAspect(tag) == null
            ? DataResult.error(() -> "Unknown infusion aspect " + tag) : DataResult.success(tag), tag -> tag);
    public static final MapCodec<InfusionEnchantmentRecipe> CODEC = RecordCodecBuilder.<InfusionEnchantmentRecipe>mapCodec(instance -> instance.group(
            Codec.STRING.optionalFieldOf("research", "").forGetter(InfusionEnchantmentRecipe::research),
            ResourceLocation.CODEC.fieldOf("enchantment").forGetter(InfusionEnchantmentRecipe::enchantment),
            Ingredient.CODEC_NONEMPTY.listOf(1, InfusionComponentPredicate.MAX_COMPONENTS).fieldOf("components").forGetter(InfusionEnchantmentRecipe::components),
            Codec.unboundedMap(ASPECT, Codec.intRange(1, Integer.MAX_VALUE)).fieldOf("aspects").forGetter(InfusionEnchantmentRecipe::aspects),
            Codec.intRange(0, 24).fieldOf("instability").forGetter(InfusionEnchantmentRecipe::instability),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("first_level_minimum_cost").forGetter(InfusionEnchantmentRecipe::firstLevelMinimumCost)
    ).apply(instance, InfusionEnchantmentRecipe::new)).flatXmap(InfusionEnchantmentRecipe::validate, InfusionEnchantmentRecipe::validate);
    public static final Serializer SERIALIZER = new Serializer();

    public InfusionEnchantmentRecipe {
        components = List.copyOf(components);
        aspects = Collections.unmodifiableMap(new LinkedHashMap<>(aspects));
    }
    private DataResult<InfusionEnchantmentRecipe> validate() {
        return components.isEmpty() || components.size() > InfusionComponentPredicate.MAX_COMPONENTS || components.stream().anyMatch(Ingredient::isEmpty)
                || aspects.isEmpty() || aspects.entrySet().stream().anyMatch(e -> Aspect.getAspect(e.getKey()) == null || e.getValue() <= 0)
                || aspects.values().stream().mapToLong(Integer::longValue).sum() > Integer.MAX_VALUE
                || instability < 0 || instability > 24 || firstLevelMinimumCost < 0
                ? DataResult.error(() -> "Invalid enchantment infusion") : DataResult.success(this);
    }
    public Holder.Reference<Enchantment> enchantment(HolderLookup.Provider registries) {
        return registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, enchantment));
    }
    /** Original item.isItemTool means a damageable, non-stackable item, even when it already has enchantments. */
    public static boolean canApply(ItemStack central, Holder<Enchantment> target) {
        if (central.isEmpty() || central.getMaxStackSize() != 1 || !central.isDamageableItem() || central.getCount() != 1) return false;
        if (!originalCategory(central, target)) return false;
        var old = central.getEnchantments();
        if (old.getLevel(target) >= target.value().getMaxLevel()) return false;
        for (Holder<Enchantment> existing : old.keySet()) if (!existing.equals(target) && !Enchantment.areCompatible(target, existing)) return false;
        return true;
    }
    private static boolean originalCategory(ItemStack central, Holder<Enchantment> target) {
        String id = target.unwrapKey().map(k -> k.location().toString()).orElse("");
        var item = central.getItem();
        return switch (id) {
            case "minecraft:protection", "minecraft:fire_protection", "minecraft:blast_protection", "minecraft:projectile_protection", "minecraft:thorns" -> item instanceof ArmorItem;
            case "minecraft:feather_falling" -> item instanceof ArmorItem armor && armor.getEquipmentSlot() == EquipmentSlot.FEET;
            case "minecraft:respiration", "minecraft:aqua_affinity" -> item instanceof ArmorItem armor && armor.getEquipmentSlot() == EquipmentSlot.HEAD;
            case "minecraft:sharpness", "minecraft:smite", "minecraft:bane_of_arthropods" -> item instanceof SwordItem || item instanceof AxeItem;
            case "minecraft:knockback", "minecraft:fire_aspect", "minecraft:looting" -> item instanceof SwordItem;
            case "minecraft:efficiency" -> item instanceof DiggerItem && !(item instanceof HoeItem) || item instanceof ShearsItem;
            case "minecraft:silk_touch", "minecraft:fortune" -> item instanceof DiggerItem && !(item instanceof HoeItem);
            case "minecraft:unbreaking" -> true;
            case "thaumcraft:repair" -> item instanceof IRepairable;
            case "thaumcraft:haste" -> item instanceof ArmorItem armor && armor.getEquipmentSlot() == EquipmentSlot.FEET
                    || item instanceof thaumcraft.common.items.armor.ItemHoverHarness;
            case "minecraft:power", "minecraft:punch", "minecraft:flame", "minecraft:infinity" -> item instanceof BowItem;
            default -> target.value().canEnchant(central);
        };
    }
    public int currentLevel(ItemStack central, HolderLookup.Provider registries) { return central.getEnchantments().getLevel(enchantment(registries)); }
    public int calculatedInstability(ItemStack central) {
        int total = central.getEnchantments().entrySet().stream().mapToInt(e -> e.getIntValue()).sum();
        return InfusionEnchantmentRules.instability(instability, total);
    }
    public int xpLevels(ItemStack central, HolderLookup.Provider registries) {
        return InfusionEnchantmentRules.xpLevels(firstLevelMinimumCost, currentLevel(central, registries));
    }
    public AspectList aspectCost(ItemStack central, HolderLookup.Provider registries) {
        Holder<Enchantment> target = enchantment(registries);
        var old = central.getEnchantments();
        List<Integer> otherLevels = new ArrayList<>();
        // Native holder-map order replaces the old NBT enchantment-list order. Keep the original per-entry float additions.
        for (var entry : old.entrySet()) if (!entry.getKey().equals(target)) otherLevels.add(entry.getIntValue());
        float modifier = InfusionEnchantmentRules.essentiaModifier(old.getLevel(target), otherLevels);
        AspectList result = new AspectList();
        aspects.forEach((tag, amount) -> result.add(Aspect.getAspect(tag), InfusionEnchantmentRules.aspectCost(amount, modifier)));
        return result;
    }
    public List<InfusionComponentPredicate> selectedComponents() { return components.stream().map(InfusionComponentPredicate::new).toList(); }
    /** Native cost/category guards shared with original virtual-comparator recipes. */
    public boolean matchesAdmission(InfusionRecipeInput offered, Level level) {
        try {
            if (!canApply(offered.central(), enchantment(level.registryAccess())) || calculatedInstability(offered.central()) > 24
                    || offered.components().size() != components.size()) return false;
            xpLevels(offered.central(), level.registryAccess()); // Reject overflowing foreign cost declarations before admission.
            AspectList cost = aspectCost(offered.central(), level.registryAccess());
            if (java.util.Arrays.stream(cost.getAspects()).mapToLong(cost::getAmount).sum() > Integer.MAX_VALUE) return false;
        } catch (RuntimeException missingEnchantment) { return false; }
        return true;
    }

    @Override public boolean matches(InfusionRecipeInput offered, Level level) {
        if (!matchesAdmission(offered, level)) return false;
        List<ItemStack> remaining = new ArrayList<>(offered.components());
        for (Ingredient required : components) {
            int found = -1;
            for (int i = 0; i < remaining.size(); i++) {
                ItemStack stack = remaining.get(i);
                if (stack.getCount() <= stack.getMaxStackSize() && InfusionMatching.matches(required, stack)) { found = i; break; }
            }
            if (found < 0) return false;
            remaining.remove(found);
        }
        return remaining.isEmpty();
    }
    public static ItemStack raise(ItemStack central, ResourceLocation target, HolderLookup.Provider registries) {
        ItemStack result = central.copy();
        var holder = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, target));
        result.enchant(holder, result.getEnchantments().getLevel(holder) + 1);
        return result;
    }
    @Override public ItemStack assemble(InfusionRecipeInput offered, HolderLookup.Provider registries) { return raise(offered.central(), enchantment, registries); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) {
        try {
            var holder = enchantment(registries);
            var sample = holder.value().getSupportedItems().stream().filter(item -> canApply(new ItemStack(item), holder)).findFirst();
            if (sample.isEmpty()) return ItemStack.EMPTY;
            ItemStack result = new ItemStack(sample.get()); result.enchant(holder, 1); return result;
        } catch (RuntimeException unknownDatapackTarget) { return ItemStack.EMPTY; }
    }
    @Override public boolean canCraftInDimensions(int width, int height) { return false; }
    @Override public NonNullList<Ingredient> getIngredients() { var result = NonNullList.<Ingredient>create(); result.addAll(components); return result; }
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeSerializer<?> getSerializer() { return ThaumcraftInfusion.ENCHANTMENT_RECIPE_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ThaumcraftInfusion.ENCHANTMENT_RECIPE_TYPE.get(); }

    public static final class Serializer implements RecipeSerializer<InfusionEnchantmentRecipe> {
        public static final StreamCodec<RegistryFriendlyByteBuf, InfusionEnchantmentRecipe> STREAM_CODEC = StreamCodec.of(Serializer::encode, Serializer::decode);
        private static void encode(RegistryFriendlyByteBuf buffer, InfusionEnchantmentRecipe recipe) {
            recipe.validate().getOrThrow(); buffer.writeUtf(recipe.research); buffer.writeResourceLocation(recipe.enchantment);
            buffer.writeVarInt(recipe.instability); buffer.writeVarInt(recipe.firstLevelMinimumCost);
            buffer.writeVarInt(recipe.components.size());
            for (Ingredient component : recipe.components) Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, component);
            buffer.writeVarInt(recipe.aspects.size());
            recipe.aspects.forEach((tag, amount) -> { buffer.writeUtf(tag); buffer.writeVarInt(amount); });
        }
        private static InfusionEnchantmentRecipe decode(RegistryFriendlyByteBuf buffer) {
            String research = buffer.readUtf(); ResourceLocation enchantment = buffer.readResourceLocation();
            int instability = buffer.readVarInt(), minimum = buffer.readVarInt(), size = buffer.readVarInt();
            if (size < 1 || size > InfusionComponentPredicate.MAX_COMPONENTS) throw new IllegalArgumentException("Invalid enchantment component count");
            List<Ingredient> components = new ArrayList<>();
            for (int i = 0; i < size; i++) components.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
            int count = buffer.readVarInt();
            if (count < 1 || count > Aspect.aspects.size()) throw new IllegalArgumentException("Invalid enchantment aspect count");
            Map<String, Integer> aspects = new LinkedHashMap<>();
            for (int i = 0; i < count; i++) if (aspects.put(buffer.readUtf(), buffer.readVarInt()) != null) throw new IllegalArgumentException("Duplicate enchantment aspect");
            return new InfusionEnchantmentRecipe(research, enchantment, components, aspects, instability, minimum).validate().getOrThrow();
        }
        @Override public MapCodec<InfusionEnchantmentRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, InfusionEnchantmentRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
