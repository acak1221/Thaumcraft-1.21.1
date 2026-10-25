package thaumcraft.common.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import thaumcraft.common.blocks.BlockJar;
import thaumcraft.common.items.ItemJar;
import thaumcraft.common.lib.crafting.InfusionMatching;

/** One selected pedestal requirement, not a general component/NBT predicate engine. */
public record InfusionComponentPredicate(Ingredient ingredient, Optional<Integer> damage,
                                         boolean waterPotion, boolean emptyJar, Optional<String> potion,
                                         Optional<ItemStack> requiredStack) {
    public InfusionComponentPredicate {
        requiredStack = requiredStack.map(stack -> stack.copyWithCount(1));
    }
    public Optional<ItemStack> requiredStack() { return requiredStack.map(ItemStack::copy); }
    public static final int MAX_COMPONENTS = 4096;
    // The existing selected-session ingredient key is retained. Missing damage
    // and false typed flags mean the old semantics, including old disk saves.
    public static final Codec<InfusionComponentPredicate> CODEC = RecordCodecBuilder.<InfusionComponentPredicate>create(instance -> instance.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(InfusionComponentPredicate::ingredient),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("damage").forGetter(InfusionComponentPredicate::damage),
            Codec.BOOL.optionalFieldOf("water_potion", false).forGetter(InfusionComponentPredicate::waterPotion),
            Codec.BOOL.optionalFieldOf("empty_jar", false).forGetter(InfusionComponentPredicate::emptyJar),
            Codec.STRING.optionalFieldOf("potion").forGetter(InfusionComponentPredicate::potion),
            ItemStack.STRICT_CODEC.optionalFieldOf("required_stack").forGetter(InfusionComponentPredicate::requiredStack)
    ).apply(instance, InfusionComponentPredicate::new)).flatXmap(InfusionComponentPredicate::validate, InfusionComponentPredicate::validate);

    public InfusionComponentPredicate(Ingredient ingredient) { this(ingredient, Optional.empty()); }

    public InfusionComponentPredicate(Ingredient ingredient, Optional<Integer> damage, boolean waterPotion, boolean emptyJar, Optional<String> potion) {
        this(ingredient, damage, waterPotion, emptyJar, potion, Optional.empty());
    }

    /** A virtual recipe is checked once, then its accepted physical components form a serializable session. */
    public static InfusionComponentPredicate snapshot(ItemStack stack) {
        ItemStack required = stack.copyWithCount(1);
        return new InfusionComponentPredicate(Ingredient.of(required), Optional.empty(), false, false, Optional.empty(), Optional.of(required));
    }

    public InfusionComponentPredicate(Ingredient ingredient, Optional<Integer> damage) { this(ingredient, damage, false, false,Optional.empty()); }
    public InfusionComponentPredicate(Ingredient ingredient,Optional<Integer> damage,boolean waterPotion,boolean emptyJar) { this(ingredient,damage,waterPotion,emptyJar,Optional.empty()); }

    public boolean matches(ItemStack offered) {
        if (requiredStack.isPresent() && !ArcaneIngredientMatching.matchesStack(requiredStack.get(), offered)) return false;
        if (validate().error().isPresent() || !InfusionMatching.matches(ingredient, damage, offered)) return false;
        if (waterPotion && (!offered.is(Items.POTION)
                || !new PotionContents(Potions.WATER).equals(offered.get(DataComponents.POTION_CONTENTS)))) return false;
        if(potion.isPresent()) {
            PotionContents contents=offered.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY);
            // Loot keeps exact old durations/custom-only effects, while native brewing uses a holder.
            // Both are finite original identities; arbitrary extra/mixed custom effects never qualify.
            PotionContents legacy=thaumcraft.common.lib.loot.LootTables.potion(InfusionPotionRequirement.legacyVariant(potion.get())).get(DataComponents.POTION_CONTENTS);
            if(!contents.is(potionHolder(potion.get()))&&!contents.equals(legacy))return false;
        }
        return !emptyJar || isNormalJar(offered) && ItemJar.isEmptyIngredient(offered);
    }

    /** Pure book/recipe preview: never mutates the ingredient or creates stock. */
    public ItemStack displayStack(ItemStack ingredientPreview) {
        if(potion.isPresent()) {
            ItemStack preview=ingredientPreview.copy();preview.set(DataComponents.POTION_CONTENTS,new PotionContents(potionHolder(potion.get())));return preview;
        }
        if (!waterPotion) return ingredientPreview.copy();
        ItemStack water = new ItemStack(Items.POTION);
        water.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));
        return water;
    }

    private DataResult<InfusionComponentPredicate> validate() {
        if (requiredStack.isPresent() && (requiredStack.get().isEmpty() || !ingredient.test(requiredStack.get())))
            return DataResult.error(() -> "Programmatic infusion snapshot must satisfy its ingredient");
        if ((waterPotion && emptyJar) || potion.isPresent() && (waterPotion || emptyJar || !knownPotion(potion.get())))
            return DataResult.error(() -> "Incompatible or unknown typed infusion component requirements");
        if (damage.isPresent()) {
            int value = damage.get();
            if (value < 0 || ingredient.isEmpty() || ingredient.getItems().length == 0) return invalidDamage();
            for (ItemStack declared : ingredient.getItems()) {
                // Check the DECLARED stock tool, not actual isDamageableItem():
                // additional actual Unbreakable/name/enchantment data is allowed.
                if (!declared.has(DataComponents.DAMAGE) || declared.getMaxDamage() <= value) return invalidDamage();
            }
        }
        if (waterPotion || emptyJar || potion.isPresent()) {
            if (ingredient.isEmpty() || ingredient.getItems().length == 0) {
                return DataResult.error(() -> "A typed infusion component requires a nonempty declared ingredient");
            }
            for (ItemStack declared : ingredient.getItems()) {
                if (waterPotion && !declared.is(Items.POTION)) {
                    return DataResult.error(() -> "Infusion water_potion requires only normal potion items in the declared ingredient");
                }
                if(potion.isPresent() && !declared.is(potion.get().equals("minecraft:strong_harming")?Items.SPLASH_POTION:Items.POTION))
                    return DataResult.error(() -> "Infusion potion requires its original normal/splash item form");
                if (emptyJar && !isNormalJar(declared)) {
                    return DataResult.error(() -> "Infusion empty_jar requires only normal non-void jar items in the declared ingredient");
                }
            }
        }
        return DataResult.success(this);
    }

    private static boolean knownPotion(String id) {
        return InfusionPotionRequirement.known(id);
    }
    private static net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> potionHolder(String id) {
        return switch(id) {
            case "minecraft:strong_strength" -> Potions.STRONG_STRENGTH;
            case "minecraft:strong_swiftness" -> Potions.STRONG_SWIFTNESS;
            case "minecraft:long_regeneration" -> Potions.LONG_REGENERATION;
            case "minecraft:strong_harming" -> Potions.STRONG_HARMING;
            default -> throw new IllegalArgumentException("Unsupported original infusion potion");
        };
    }
    private static boolean isNormalJar(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemJar item
                && item.getBlock() instanceof BlockJar jar && !jar.isVoid();
    }

    private static DataResult<InfusionComponentPredicate> invalidDamage() {
        return DataResult.error(() -> "Infusion component damage requires a damageable declared ingredient and damage below its maximum");
    }

    public static DataResult<List<Declaration>> validateDeclarations(List<Ingredient> components, List<Declaration> declarations) {
        return Declaration.validateIndices(components.size(), declarations).flatMap(checked -> {
            for (Declaration declaration : checked) {
                var requirement = new InfusionComponentPredicate(components.get(declaration.component()), declaration.optionalDamage(),
                        declaration.waterPotion(), declaration.emptyJar(),declaration.potion());
                if (requirement.validate().error().isPresent()) return DataResult.error(() -> "Invalid infusion component predicate at index " + declaration.component());
            }
            return DataResult.success(checked);
        });
    }

    /** Recipe-level indices become paired session entries BEFORE anything is consumed. */
    public record Declaration(int component, int damage, boolean waterPotion, boolean emptyJar,Optional<String> potion) {
        public static final Codec<Declaration> CODEC = RecordCodecBuilder.<Declaration>create(instance -> instance.group(
                Codec.intRange(0, MAX_COMPONENTS - 1).fieldOf("component").forGetter(Declaration::component),
                Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("damage").forGetter(Declaration::optionalDamage),
                Codec.BOOL.optionalFieldOf("water_potion", false).forGetter(Declaration::waterPotion),
                Codec.BOOL.optionalFieldOf("empty_jar", false).forGetter(Declaration::emptyJar),
                Codec.STRING.optionalFieldOf("potion").forGetter(Declaration::potion)
        ).apply(instance, (component, damage, waterPotion, emptyJar,potion) ->
                new Declaration(component, damage.orElse(-1), waterPotion, emptyJar,potion)))
                .flatXmap(Declaration::validate, Declaration::validate);

        public Declaration(int component, int damage) { this(component, damage, false, false,Optional.empty()); }
        public Declaration(int component,int damage,boolean waterPotion,boolean emptyJar) { this(component,damage,waterPotion,emptyJar,Optional.empty()); }

        /** -1 is internal absence for typed declarations only, never JSON wear. */
        public Optional<Integer> optionalDamage() { return damage == -1 ? Optional.empty() : Optional.of(damage); }

        private DataResult<Declaration> validate() {
            if (component < 0 || component >= MAX_COMPONENTS || damage < -1
                    || (damage == -1 && !waterPotion && !emptyJar && potion.isEmpty()) || (waterPotion && emptyJar)
                    || potion.isPresent() && (waterPotion || emptyJar || !InfusionPotionRequirement.known(potion.get()))) {
                return DataResult.error(() -> "Invalid infusion component predicate index/damage/typed requirement");
            }
            return DataResult.success(this);
        }

        /** Registry-independent alignment validation; never allocates from an unchecked count. */
        public static DataResult<List<Declaration>> validateIndices(int componentCount, List<Declaration> declarations) {
            if (componentCount < 1 || componentCount > MAX_COMPONENTS || declarations.size() > componentCount) {
                return DataResult.error(() -> "Invalid infusion component predicate count");
            }
            boolean[] seen = new boolean[componentCount];
            for (Declaration declaration : declarations) {
                int index = declaration.component();
                if (index < 0 || index >= componentCount || declaration.validate().error().isPresent() || seen[index]) {
                    return DataResult.error(() -> "Invalid or duplicate infusion component predicate index/requirements");
                }
                seen[index] = true;
            }
            return DataResult.success(List.copyOf(declarations));
        }
    }
}
