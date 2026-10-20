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
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.lib.crafting.InfusionMatching;

/**
 * Native STACK-output infusion recipes. Ordinary ingredients deliberately impose
 * no component/NBT equality; only declared damage/water-potion/empty-jar
 * requirements are checked. Central input damage applies at admission, never
 * to running-ritual retention.
 * A recipe may name a {@link InfusionResultPatches result patch}: its output is then the actual central item patched
 * (original {@code Object[]{key, tag}} results, e.g. ADVANCEDGOLEM) and {@code result} is only the sample shown in the book.
 * General legacy NBT predicates, metadata wildcards and enchantment recipes are NOT represented or registered by
 * this serializer.
 */
public record InfusionStackRecipe(String research, Ingredient input, List<Ingredient> components,
                                  Map<String, Integer> aspects, int instability, ItemStack result,
                                  List<InfusionComponentPredicate.Declaration> componentPredicates,
                                  Optional<Integer> inputDamage, Optional<String> resultPatch)
        implements Recipe<InfusionRecipeInput> {
    private static final Codec<String> ASPECT = Codec.STRING.comapFlatMap(tag -> Aspect.getAspect(tag) == null
            ? DataResult.error(() -> "Unknown infusion aspect: " + tag) : DataResult.success(tag), tag -> tag);
    public static final MapCodec<InfusionStackRecipe> CODEC = RecordCodecBuilder.<InfusionStackRecipe>mapCodec(instance -> instance.group(
            Codec.STRING.optionalFieldOf("research", "").forGetter(InfusionStackRecipe::research),
            Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(InfusionStackRecipe::input),
            Ingredient.CODEC_NONEMPTY.listOf().fieldOf("components").forGetter(InfusionStackRecipe::components),
            Codec.unboundedMap(ASPECT, Codec.intRange(1, Integer.MAX_VALUE)).fieldOf("aspects").forGetter(InfusionStackRecipe::aspects),
            // Both original shortage RNG bounds must be positive; no invented clamp in the cycle.
            Codec.intRange(0, 24).fieldOf("instability").forGetter(InfusionStackRecipe::instability),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(InfusionStackRecipe::result),
            InfusionComponentPredicate.Declaration.CODEC.listOf(0, InfusionComponentPredicate.MAX_COMPONENTS)
                    .optionalFieldOf("component_predicates", List.of()).forGetter(InfusionStackRecipe::componentPredicates),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("input_damage").forGetter(InfusionStackRecipe::inputDamage),
            Codec.STRING.optionalFieldOf("result_patch").forGetter(InfusionStackRecipe::resultPatch)
    ).apply(instance, InfusionStackRecipe::new)).flatXmap(InfusionStackRecipe::validate, InfusionStackRecipe::validate);
    public static final Serializer SERIALIZER = new Serializer();

    public InfusionStackRecipe {
        components = List.copyOf(components);
        componentPredicates = List.copyOf(componentPredicates);
        aspects = Collections.unmodifiableMap(new LinkedHashMap<>(aspects));
        result = result.copy();
    }

    public InfusionStackRecipe(String research, Ingredient input, List<Ingredient> components,
                               Map<String, Integer> aspects, int instability, ItemStack result) {
        this(research, input, components, aspects, instability, result, List.of());
    }

    public InfusionStackRecipe(String research, Ingredient input, List<Ingredient> components,
                               Map<String, Integer> aspects, int instability, ItemStack result,
                               List<InfusionComponentPredicate.Declaration> componentPredicates) {
        this(research, input, components, aspects, instability, result, componentPredicates, Optional.empty());
    }

    public InfusionStackRecipe(String research, Ingredient input, List<Ingredient> components,
                               Map<String, Integer> aspects, int instability, ItemStack result,
                               List<InfusionComponentPredicate.Declaration> componentPredicates,
                               Optional<Integer> inputDamage) {
        this(research, input, components, aspects, instability, result, componentPredicates, inputDamage, Optional.empty());
    }

    private DataResult<InfusionStackRecipe> validate() {
        if (input.isEmpty() || components.isEmpty() || components.size() > InfusionComponentPredicate.MAX_COMPONENTS
                || components.stream().anyMatch(Ingredient::isEmpty)
                || result.isEmpty() || aspects.isEmpty() || instability < 0 || instability > 24
                || aspects.entrySet().stream().anyMatch(entry -> Aspect.getAspect(entry.getKey()) == null || entry.getValue() <= 0)
                || aspects.values().stream().mapToLong(Integer::longValue).sum() > Integer.MAX_VALUE) {
            return DataResult.error(() -> "Infusion requires a central input, components, result, valid aspect cost and instability 0..24");
        }
        if (resultPatch.isPresent() && !InfusionResultPatches.known(resultPatch.get())) {
            return DataResult.error(() -> "Unknown infusion result patch: " + resultPatch.get());
        }
        return InfusionComponentPredicate.validateDeclarations(components, componentPredicates).flatMap(checked -> {
            if (inputDamage.isEmpty()) return DataResult.success(this);
            return InfusionComponentPredicate.validateDeclarations(List.of(input),
                    List.of(new InfusionComponentPredicate.Declaration(0, inputDamage.get())))
                    .map(ignored -> this).mapError(error -> "Invalid infusion input_damage: " + error);
        });
    }

    @Override public boolean matches(InfusionRecipeInput offered, Level level) {
        if (!InfusionMatching.matches(input, inputDamage, offered.central()) || offered.components().size() != components.size()) return false;
        // A patch recipe is only craftable when its patch can be applied to this very centre.
        if (resultPatch.isPresent() && !InfusionResultPatches.canApply(resultPatch.get(), offered.central())) return false;
        // Original InfusionRecipe:51-70: remove the first match, not a sorted or
        // count-flattened inventory. One occupied pedestal supplies one component.
        List<ItemStack> remaining = new ArrayList<>(offered.components());
        if (validate().error().isPresent()) return false;
        for (InfusionComponentPredicate required : selectedComponents()) {
            int match = -1;
            for (int index = 0; index < remaining.size(); index++) {
                if (required.matches(remaining.get(index))) { match = index; break; }
            }
            if (match < 0) return false;
            remaining.remove(match);
        }
        return remaining.isEmpty();
    }

    public Optional<Integer> componentDamage(int index) {
        if (index < 0 || index >= components.size()) throw new IndexOutOfBoundsException(index);
        for (var declaration : componentPredicates) if (declaration.component() == index) return declaration.optionalDamage();
        return Optional.empty();
    }

    /** Paired snapshots prevent index drift when an earlier component is consumed. */
    public List<InfusionComponentPredicate> selectedComponents() {
        InfusionComponentPredicate.validateDeclarations(components, componentPredicates).getOrThrow();
        List<InfusionComponentPredicate> selected = new ArrayList<>(components.size());
        for (Ingredient component : components) selected.add(new InfusionComponentPredicate(component));
        for (var declaration : componentPredicates) {
            selected.set(declaration.component(), new InfusionComponentPredicate(components.get(declaration.component()),
                    declaration.optionalDamage(), declaration.waterPotion(), declaration.emptyJar(),declaration.potion()));
        }
        return List.copyOf(selected);
    }

    public AspectList aspectCost() {
        AspectList cost = new AspectList();
        aspects.forEach((tag, amount) -> cost.add(Aspect.getAspect(tag), amount));
        return cost;
    }

    public ItemStack result() { return result.copy(); }
    /** The fixed result, or the centre with the recipe's patch applied (empty when it cannot be applied). */
    @Override public ItemStack assemble(InfusionRecipeInput offered, HolderLookup.Provider registries) {
        return resultPatch.isPresent() ? InfusionResultPatches.apply(resultPatch.get(), offered.central()) : result.copy();
    }
    /** What the book and tooltips show: the sample result, patched when the recipe patches its centre. */
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) {
        if (resultPatch.isEmpty()) return result.copy();
        ItemStack shown = InfusionResultPatches.apply(resultPatch.get(), result);
        return shown.isEmpty() ? result.copy() : shown;
    }
    @Override public boolean canCraftInDimensions(int width, int height) { return false; }
    @Override public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(input);
        ingredients.addAll(components);
        return ingredients;
    }
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeSerializer<?> getSerializer() { return ThaumcraftInfusion.INFUSION_RECIPE_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ThaumcraftInfusion.INFUSION_RECIPE_TYPE.get(); }

    public static final class Serializer implements RecipeSerializer<InfusionStackRecipe> {
        // An NBT compound map codec would lose aspect insertion order in the
        // login recipe packet. Encode its entries explicitly, in the same order.
        public static final StreamCodec<RegistryFriendlyByteBuf, InfusionStackRecipe> STREAM_CODEC = StreamCodec.of(Serializer::encode, Serializer::decode);

        private static void encode(RegistryFriendlyByteBuf buffer, InfusionStackRecipe recipe) {
            recipe.validate().getOrThrow();
            buffer.writeUtf(recipe.research);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.input);
            buffer.writeVarInt(recipe.components.size());
            for (Ingredient component : recipe.components) Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, component);
            buffer.writeVarInt(recipe.aspects.size());
            recipe.aspects.forEach((tag, amount) -> { buffer.writeUtf(tag); buffer.writeVarInt(amount); });
            buffer.writeVarInt(recipe.instability);
            ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
            buffer.writeVarInt(recipe.componentPredicates.size());
            for (var declaration : recipe.componentPredicates) {
                buffer.writeVarInt(declaration.component());
                buffer.writeVarInt(declaration.damage());
                buffer.writeBoolean(declaration.waterPotion());
                buffer.writeBoolean(declaration.emptyJar());
                buffer.writeBoolean(declaration.potion().isPresent());
                declaration.potion().ifPresent(buffer::writeUtf);
            }
            buffer.writeBoolean(recipe.inputDamage.isPresent());
            recipe.inputDamage.ifPresent(buffer::writeVarInt);
            buffer.writeBoolean(recipe.resultPatch.isPresent());
            recipe.resultPatch.ifPresent(buffer::writeUtf);
        }

        private static InfusionStackRecipe decode(RegistryFriendlyByteBuf buffer) {
            String research = buffer.readUtf();
            Ingredient input = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
            int componentCount = buffer.readVarInt();
            if (componentCount < 1 || componentCount > InfusionComponentPredicate.MAX_COMPONENTS) throw new IllegalArgumentException("Invalid infusion component count");
            List<Ingredient> components = new ArrayList<>(componentCount);
            for (int index = 0; index < componentCount; index++) components.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
            int aspectCount = buffer.readVarInt();
            if (aspectCount < 1 || aspectCount > Aspect.aspects.size()) throw new IllegalArgumentException("Invalid infusion aspect count");
            Map<String, Integer> aspects = new LinkedHashMap<>();
            for (int index = 0; index < aspectCount; index++) {
                if (aspects.put(buffer.readUtf(), buffer.readVarInt()) != null) throw new IllegalArgumentException("Duplicate infusion aspect");
            }
            int instability = buffer.readVarInt();
            ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
            int predicateCount = buffer.readVarInt();
            if (predicateCount < 0 || predicateCount > componentCount) throw new IllegalArgumentException("Invalid infusion component predicate count");
            List<InfusionComponentPredicate.Declaration> predicates = new ArrayList<>(predicateCount);
            for (int index = 0; index < predicateCount; index++) {
                int component=buffer.readVarInt(),damage=buffer.readVarInt();boolean water=buffer.readBoolean(),emptyJar=buffer.readBoolean();
                Optional<String> potion=buffer.readBoolean()?Optional.of(buffer.readUtf()):Optional.empty();
                predicates.add(new InfusionComponentPredicate.Declaration(component,damage,water,emptyJar,potion));
            }
            Optional<Integer> inputDamage = buffer.readBoolean() ? Optional.of(buffer.readVarInt()) : Optional.empty();
            Optional<String> resultPatch = buffer.readBoolean() ? Optional.of(buffer.readUtf()) : Optional.empty();
            var recipe = new InfusionStackRecipe(research, input, components, aspects, instability, result, predicates, inputDamage, resultPatch);
            return recipe.validate().getOrThrow();
        }

        @Override public MapCodec<InfusionStackRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, InfusionStackRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
