package thaumcraft.common.lib.research;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.crafting.ArcaneShapelessRecipe;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.crafting.CrucibleRecipe;
import thaumcraft.common.items.ItemEssence;
import thaumcraft.common.items.ItemJarLabel;
import thaumcraft.common.items.PhialData;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandMaterials;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.config.ThaumcraftContent;

/**
 * Native crafting/arcane/crucible/STACK infusion tags and item bonuses shared by scans and
 * dissolution. Original ThaumcraftCraftingManager:215-807, ApiHelper:40-80.
 * Enchantment/tag-patch/runic infusion and addon recipe contracts remain pending.
 */
public final class ObjectAspectResolver {
    private static final Map<RecipeManager, RecipeIndex> INDEXES = new WeakHashMap<>();
    private static final Map<String, Aspect> ENCHANTMENT_TAGS = enchantmentTags();

    private ObjectAspectResolver() {}

    /** Original getObjectTags, per one item: capped base and dynamic wand/potion tags only.
     * No container, equipment or enchantment bonuses and no six-aspect culling; every return is an independent mutable list. */
    public static AspectList baseTags(ItemStack stack, Level level) {
        return baseTags(stack, level, stack == null ? 0 : stack.getDamageValue());
    }

    /** Native explicit metadata query supports wildcard/group keys without fabricating out-of-range item damage components. */
    public static AspectList baseTags(ItemStack stack, Level level, int metadata) {
        if (stack == null || stack.isEmpty() || level == null) return new AspectList();
        ObjectAspectCatalog.initializePublicTags();
        var registered = ObjectAspectRegistry.lookup(ThaumcraftApi.objectTags, stack.getItem(), metadata);
        // Original raw int[] key returns before dynamic bonuses and capAspects. Return a copy so readers cannot mutate addon state.
        if (registered != null && registered.range()) return registered.aspects().copy();
        AspectList base = registered == null ? new Resolution(level, index(level)).base(stack.getItem()) : registered.aspects().copy();
        if (stack.getItem() instanceof ItemWandCasting) {
            var data = ItemWandCasting.data(stack);
            int cost = WandMaterials.rodCraftCost(data.rod()) + WandMaterials.capCraftCost(data.cap());
            // This bonus uses SUM, unlike the assembly recipe's PRODUCT. Keep
            // the original zero instrumentum entry for the basic wood/iron pair.
            base.merge(Aspect.MAGIC, cost / 2).merge(Aspect.TOOL, cost / 3);
        }
        if (stack.getItem() instanceof PotionItem) {
            AspectList potion = potionTags(stack);
            for (Aspect aspect : potion.getAspects()) base.merge(aspect, potion.getAmount(aspect));
        }
        return cap(base);
    }

    /** Original generateTags branch used by complex registration: generated/capped base, before runtime item bonuses. */
    public static AspectList generatedTags(Item item, Level level) {
        ObjectAspectCatalog.initializePublicTags();
        return cap(new Resolution(level, index(level)).base(item));
    }

    /** Only scan preflight uses grouped metadata; ordinary dissolution/getObjectTags deliberately does not. */
    public record ItemWitness(ItemStack stack, int metadata) {}
    public static ItemWitness groupedScanWitness(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return new ItemWitness(ItemStack.EMPTY, 0);
        ItemStack result = stack.copyWithCount(1);
        int[] metadata = ThaumcraftApi.groupedObjectTags.get(List.of(stack.getItem(), stack.getDamageValue()));
        // Native damage access clamps to maxDamage, including0 for ordinary items. Keep the original lookup metadata separately.
        return new ItemWitness(result, metadata != null && metadata.length > 0 ? metadata[0] : stack.getDamageValue());
    }

    public static AspectList scanTags(ItemStack stack, Level level) {
        var witness = groupedScanWitness(stack);
        return get(witness.stack(), level, witness.metadata());
    }

    /** Amounts are per ONE item, not multiplied by the supplied stack count. */
    public static AspectList get(ItemStack stack, Level level) {
        return get(stack, level, stack == null ? 0 : stack.getDamageValue());
    }

    private static AspectList get(ItemStack stack, Level level, int metadata) {
        if (stack == null || stack.isEmpty() || level == null) return new AspectList();
        AspectList base = baseTags(stack, level, metadata);
        AspectList result = new AspectList();
        if (stack.getItem() instanceof IEssentiaContainerItem container) {
            AspectList contained = container.getAspects(stack);
            if (contained != null) for (Aspect aspect : contained.getAspects()) {
                if (aspect != null && contained.getAmount(aspect) > 0) result.add(aspect, contained.getAmount(aspect));
            }
        }
        PhialData phial = ItemEssence.data(stack);
        if (stack.getItem() instanceof ItemEssence && phial != null) result.add(Aspect.getAspect(phial.tag()), PhialData.AMOUNT);
        // Original ItemResource13 implements IEssentiaContainerItem too: its
        // bound tag contributes ONE aspect in getBonusTags:365-369. It is not
        // an eight-unit phial, nor should the new native component be ignored.
        if (stack.getItem() instanceof ItemJarLabel && ItemJarLabel.aspect(stack) != null) result.add(ItemJarLabel.aspect(stack), 1);
        result.add(base);
        AspectList bonus = bonusTags(stack);
        for (Aspect aspect : bonus.getAspects()) result.merge(aspect, bonus.getAmount(aspect));
        // Original getObjectTags caps only the base. getBonusTags:362-481 adds
        // contained essentia/bonuses, then cullTags selects six WITHOUT recapping amounts.
        return cull(result);
    }

    /**
     * The original bonus tags of one item: the equipment bonus and the enchantment bonus.
     *
     * <p>Oracle {@code ThaumcraftCraftingManager.getBonusTags} merges the equipment bonus of the
     * item class and then the enchantment bonus into the finished list, and finally adds the sum
     * of all enchantment levels as MAGIC. The production path is {@link #get(ItemStack, Level)};
     * this method is the same computation without the object base, so the parity test can pin the
     * original numbers.</p>
     */
    public static AspectList bonusTags(ItemStack stack) {
        AspectList tags = new AspectList();
        if (stack == null || stack.isEmpty()) return tags;
        equipmentTags(stack, tags);
        ItemEnchantments enchantments = stack.getOrDefault(stack.is(Items.ENCHANTED_BOOK)
                ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        int total = 0;
        for (var entry : enchantments.entrySet()) {
            int amount = entry.getIntValue();
            String id = entry.getKey().unwrapKey().map(key -> key.location().toString()).orElse("");
            Aspect aspect = ENCHANTMENT_TAGS.get(id);
            if (aspect != null) tags.merge(aspect, amount);
            total += amount;
        }
        if (total > 0) tags.merge(Aspect.MAGIC, total);
        return tags;
    }

    private static synchronized RecipeIndex index(Level level) {
        RecipeManager manager = level.getRecipeManager();
        Collection<RecipeHolder<?>> recipes = manager.getRecipes();
        RecipeIndex existing = INDEXES.get(manager);
        // Native byName.values changes identity when recipes are replaced, even
        // if RecipeManager itself survives /reload. No cached aspect results or
        // Level/RegistryAccess references survive a lookup or a server lifetime.
        if (existing != null && existing.source == recipes) return existing;
        Map<Item, List<RecipeHolder<?>>> outputs = new HashMap<>();
        recipes.stream().sorted(Comparator.comparing(holder -> holder.id().toString())).forEach(holder -> {
            var recipe = holder.value();
            if (!(recipe instanceof CrucibleRecipe || recipe instanceof ArcaneShapedRecipe || recipe instanceof ArcaneShapelessRecipe || recipe instanceof InfusionStackRecipe
                    || recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe)) return;
            ItemStack result = recipe.getResultItem(level.registryAccess());
            if (!result.isEmpty()) outputs.computeIfAbsent(result.getItem(), ignored -> new ArrayList<>()).add(holder);
        });
        var created = new RecipeIndex(recipes, outputs);
        INDEXES.put(manager, created);
        return created;
    }

    private record RecipeIndex(Collection<RecipeHolder<?>> source, Map<Item, List<RecipeHolder<?>>> outputs) {}

    private static final class Resolution {
        final Level level;
        final RecipeIndex recipes;
        final Set<Item> history = new LinkedHashSet<>();
        final Map<Item, AspectList> resolved = new HashMap<>();
        final Map<ResourceLocation, AspectList> complex = EarlyObjectAspectBindings.complex();

        Resolution(Level level, RecipeIndex recipes) { this.level = level; this.recipes = recipes; }

        AspectList base(Item item) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            var registered = ObjectAspectRegistry.lookup(ThaumcraftApi.objectTags, item, ObjectAspectRegistry.WILDCARD);
            if (registered != null) return registered.aspects().copy();
            if (resolved.containsKey(item)) return resolved.get(item).copy();
            if (history.contains(item) || history.size() >= 99) return new AspectList();
            history.add(item); // Source history is shared across siblings, not a popped call stack.
            List<RecipeHolder<?>> candidates = recipes.outputs.getOrDefault(item, List.of());
            // ConfigAspects:422 declares the converted crucible's direct base
            // as the real cauldron tags + fabrico4 + praecantatio4. Conversion
            // is not a crafting recipe, so ordinary RecipeManager lookup alone
            // would otherwise make this real machine unknown/valueless.
            AspectList result;
            if (item == ThaumcraftContent.MATERIALS.get("salis_mundus").get()) {
                // ConfigAspects421/AspectList25–37/InternalMethodHandler53–55:
                // getObjectTags, NOT getBonusTags/cull6 or smelting derivation.
                result = base(ThaumcraftContent.SHARDS.get("balanced").get()).add(Aspect.MAGIC, 2).remove(Aspect.CRYSTAL);
            } else if (item == ThaumcraftContent.THAUMONOMICON.get() || item == ThaumcraftContent.THAUMONOMICON_CHEAT.get()) {
                // ConfigAspects426 applies to both metadata variants. Wand conversion
                // has no native recipe; the original explicitly derives the bookshelf base.
                result = base(Items.BOOKSHELF).add(Aspect.MAGIC, 2).merge(Aspect.MIND, 2);
            } else if (id.getNamespace().equals("thaumcraft") && id.getPath().equals("crucible")) {
                result = base(Items.CAULDRON).add(Aspect.CRAFT, 4).add(Aspect.MAGIC, 4);
            } else result = fromCrucible(candidates);
            if (result == null) result = fromArcane(candidates);
            if (result == null) result = fromInfusion(candidates);
            if (result == null) result = fromCrafting(candidates);
            if (result == null) result = new AspectList();
            // Curated original complex registrations add to generated tags only
            // when no explicit direct binding existed (source Api:259-273).
            AspectList addition = complex.get(id);
            if (addition != null) result.add(addition);
            result = cap(result);
            resolved.put(item, result.copy());
            return result;
        }

        private AspectList fromCrucible(List<RecipeHolder<?>> candidates) {
            var selected = candidates.stream().filter(holder -> holder.value() instanceof CrucibleRecipe)
                    .min(Comparator.comparingInt(holder -> ((CrucibleRecipe) holder.value()).sourceOrder()));
            if (selected.isEmpty()) return null;
            CrucibleRecipe recipe = (CrucibleRecipe) selected.get().value();
            ItemStack[] catalysts = recipe.catalyst().getItems();
            if (catalysts.length == 0) return null;
            // Unlike ordinary crafting, original crucible output copies ALL
            // catalyst tags without dividing by the recipe output stack size.
            AspectList result = base(catalysts[0].getItem());
            addCost(result, recipe.aspects(), recipe.result().getCount());
            return positive(result);
        }

        private AspectList fromArcane(List<RecipeHolder<?>> candidates) {
            AspectList result = null;
            for (var holder : candidates) {
                if (!(holder.value() instanceof ArcaneShapedRecipe || holder.value() instanceof ArcaneShapelessRecipe)) continue;
                var recipe = (thaumcraft.common.crafting.NativeArcaneRecipe) holder.value();
                int width = recipe instanceof ArcaneShapedRecipe shaped ? shaped.pattern().getFirst().length() : recipe.getIngredients().size();
                int height = recipe instanceof ArcaneShapedRecipe shaped ? shaped.pattern().size() : 1;
                int outputCount = recipe.getResultItem(level.registryAccess()).getCount();
                // CraftingManager:585-611 walks every declared shapeless input
                // once, then applies the SAME .75 ingredients and sqrt(vis).
                result = ingredients(recipe.getIngredients(), width, height, outputCount);
                addCost(result, recipe.vis(), outputCount);
                result = positive(result);
            }
            return result;
        }

        private AspectList fromInfusion(List<RecipeHolder<?>> candidates) {
            // Original CraftingManager:630-656 uses the DECLARED center and
            // one of every declared component, not the interchangeable logs
            // which the runtime ore-fuzzy input matcher may also accept.
            for (var holder : candidates) if (holder.value() instanceof InfusionStackRecipe recipe) {
                List<Ingredient> input = new java.util.ArrayList<>();
                input.add(recipe.input());
                input.addAll(recipe.components());
                int outputCount = recipe.getResultItem(level.registryAccess()).getCount();
                AspectList result = ingredients(input, input.size(), 1, outputCount);
                addCost(result, recipe.aspects(), outputCount);
                return positive(result);
            }
            return null;
        }

        private AspectList fromCrafting(List<RecipeHolder<?>> candidates) {
            AspectList result = null;
            int least = Integer.MAX_VALUE;
            for (var holder : candidates) {
                var recipe = holder.value();
                if (!(recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe)) continue;
                ItemStack output = recipe.getResultItem(level.registryAccess());
                int width = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : recipe.getIngredients().size();
                int height = recipe instanceof ShapedRecipe shaped ? shaped.getHeight() : 1;
                AspectList tags = ingredients(recipe.getIngredients(), width, height, output.getCount());
                if (tags.visSize() > 0 && tags.visSize() < least) { result = tags; least = tags.visSize(); }
            }
            return result;
        }

        private AspectList ingredients(List<Ingredient> input, int width, int height, int outputCount) {
            AspectList sum = new AspectList();
            for (int x = 0; x < width; x++) for (int y = 0; y < height; y++) {
                Ingredient ingredient = input.get(x + y * width);
                if (ingredient.isEmpty()) continue;
                for (ItemStack alternative : ingredient.getItems()) {
                    Item container = alternative.getItem().getCraftingRemainingItem();
                    if (container == alternative.getItem()) continue;
                    AspectList tags = base(alternative.getItem());
                    if (tags.size() == 0) continue;
                    // Source:762-783 reduces container tags from the still-empty
                    // OUTPUT, not mid. Preserve that no-op, not a new deduction.
                    sum.add(tags);
                    break;
                }
            }
            AspectList result = new AspectList();
            for (Aspect aspect : sum.getAspects()) result.add(aspect, (int) (sum.getAmount(aspect) * .75F / outputCount));
            return positive(result);
        }
    }

    private static void addCost(AspectList target, Map<String, Integer> cost, int outputCount) {
        for (var entry : cost.entrySet()) target.add(Aspect.getAspect(entry.getKey()), (int) (Math.sqrt(entry.getValue()) / outputCount));
    }

    private static AspectList positive(AspectList tags) {
        for (Aspect aspect : tags.getAspects()) if (tags.getAmount(aspect) <= 0) tags.remove(aspect);
        return tags;
    }

    private static AspectList cap(AspectList tags) {
        AspectList result = new AspectList();
        for (Aspect aspect : tags.getAspects()) result.merge(aspect, Math.min(64, tags.getAmount(aspect)));
        return result;
    }

    /**
     * The original sword bonus is the constant five, not a tier scale.
     *
     * <p>Oracle {@code ThaumcraftCraftingManager.getBonusTags} (the 1.7.10 jar bytecode) computes
     * {@code merge(WEAPON, (int)(ItemSword.func_150931_i() + 1.0F))}. The 1.7.10 vanilla
     * {@code ItemSword} returns the constant base damage 4.0F for every sword - the tool material's
     * damage is added when the attack-damage attribute modifier is built, not inside
     * {@code func_150931_i()}. The 1.12 reference of this repository keeps the same expression with
     * the renamed {@code ItemSword.getAttackDamage()}
     * ({@code src/main/java/thaumcraft/common/lib/crafting/ThaumcraftCraftingManager.java:92-94}),
     * and TC 1.12's own {@code ItemThaumiumSword} has to override BOTH {@code getAttackDamage()}
     * and {@code getAttributeModifiers()} to six
     * ({@code src/main/java/thaumcraft/common/items/equipment/ItemThaumiumSword.java:17-25}) -
     * which is only needed when the vanilla value is a constant that does not contain the
     * material damage. Every sword of the original therefore contributes WEAPON 5.</p>
     */
    public static final int SWORD_WEAPON_BONUS = 5;

    /**
     * The original branch order of {@code getBonusTags} is armour, sword, bow, pickaxe, tool,
     * shears/hoe, and every branch leaves the chain.
     *
     * <p>The 1.7.10 {@code ItemHoe} extends {@code ItemTool}, so a hoe is caught by the tool branch
     * and never reaches the shears/hoe branch: the original gives a hoe the TOOL bonus of the tool
     * material and no harvest bonus. The 1.21.1 {@code HoeItem} extends {@code DiggerItem} exactly
     * like the 1.7.10 {@code ItemHoe} extends {@code ItemTool}, so the native tool branch has to
     * keep the hoe. The 1.7.10 {@code ItemShears} extends {@code Item} and keeps the harvest
     * bonus alone, like the 1.21.1 {@code ShearsItem}.</p>
     */
    private static void equipmentTags(ItemStack stack, AspectList tags) {
        Item item = stack.getItem();
        if (item instanceof ArmorItem armor) tags.merge(Aspect.ARMOR, armor.getDefense());
        else if (item instanceof SwordItem) tags.merge(Aspect.WEAPON, SWORD_WEAPON_BONUS);
        else if (item instanceof BowItem) tags.merge(Aspect.WEAPON, 3).merge(Aspect.FLIGHT, 1);
        else if (item instanceof PickaxeItem pick && legacyHarvestLevel(pick.getTier()) >= 0) tags.merge(Aspect.MINE, legacyHarvestLevel(pick.getTier()) + 1);
        else if (item instanceof DiggerItem tool && legacyHarvestLevel(tool.getTier()) >= 0) tags.merge(Aspect.TOOL, legacyHarvestLevel(tool.getTier()) + 1);
        else if (item instanceof ShearsItem) {
            int durability = stack.getMaxDamage();
            tags.merge(Aspect.HARVEST, durability <= 59 ? 1 : durability <= 131 || durability <= 32 ? 2 : durability <= 250 ? 3 : 4);
        }
    }

    private static int legacyHarvestLevel(Tier tier) {
        // 1.21 removed the integer harvest level. Only original anchored tiers
        // have an anchored mapping; attack damage is NOT the harvest API and
        // must not be used to invent bonuses for arbitrary modern/modded tiers.
        if (tier == Tiers.WOOD || tier == Tiers.GOLD) return 0;
        if (tier == Tiers.STONE) return 1;
        if (tier == Tiers.IRON) return 2;
        if (tier == Tiers.DIAMOND || tier == ThaumcraftEquipment.THAUMIUM_TIER || tier == ThaumcraftEquipment.ELEMENTAL_TIER) return 3;
        return -1;
    }

    /**
     * The original potion aspects of {@code ThaumcraftCraftingManager.getObjectTags}.
     *
     * <p>Read from the bytecode of the 1.7.10 jar: the potion branch of {@code getObjectTags}
     * merges WATER 1 for every potion, ENTRPY 2 for a splash potion, MAGIC of two per amplifier
     * level for every effect and the effect aspect of three per amplifier level. The effect table
     * of the original is {@code blindness} DARKNESS, {@code confusion} ELDRITCH, {@code damageBoost}
     * WEAPON, {@code digSlowdown} TRAP, {@code moveSlowdown} TRAP, {@code digSpeed} TOOL,
     * {@code fireResistance} ARMOR plus FIRE, {@code harm} DEATH, {@code heal} HEAL,
     * {@code hunger} DEATH, {@code invisibility} SENSES, {@code jump} FLIGHT, {@code moveSpeed}
     * MOTION, {@code nightVision} SENSES, {@code poison} POISON, {@code regeneration} HEAL,
     * {@code resistance} ARMOR, {@code waterBreathing} AIR and {@code weakness} DEATH.</p>
     */
    public static AspectList potionTags(ItemStack stack) {
        AspectList tags = new AspectList();
        if (stack == null || stack.isEmpty()) return tags;
        tags.merge(Aspect.WATER, 1);
        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        if (stack.is(Items.SPLASH_POTION)) tags.merge(Aspect.ENTROPY, 2);
        for (MobEffectInstance effect : contents.getAllEffects()) {
            int strength = effect.getAmplifier() + 1;
            tags.merge(Aspect.MAGIC, strength * 2);
            Holder<MobEffect> type = effect.getEffect();
            Aspect aspect = type.equals(MobEffects.BLINDNESS) ? Aspect.DARKNESS : type.equals(MobEffects.CONFUSION) ? Aspect.ELDRITCH
                    : type.equals(MobEffects.WITHER) ? Aspect.ELDRITCH
                    : type.equals(MobEffects.DAMAGE_BOOST) ? Aspect.WEAPON : type.equals(MobEffects.DIG_SLOWDOWN) ? Aspect.TRAP
                    : type.equals(MobEffects.DIG_SPEED) ? Aspect.TOOL : type.equals(MobEffects.HARM) || type.equals(MobEffects.HUNGER) ? Aspect.DEATH
                    : type.equals(MobEffects.WEAKNESS) ? Aspect.DEATH : type.equals(MobEffects.HEAL) || type.equals(MobEffects.REGENERATION) ? Aspect.HEAL
                    : type.equals(MobEffects.NIGHT_VISION) || type.equals(MobEffects.INVISIBILITY) ? Aspect.SENSES
                    : type.equals(MobEffects.JUMP) ? Aspect.FLIGHT : type.equals(MobEffects.MOVEMENT_SLOWDOWN) ? Aspect.TRAP
                    : type.equals(MobEffects.MOVEMENT_SPEED) ? Aspect.MOTION : type.equals(MobEffects.POISON) ? Aspect.POISON
                    : type.equals(MobEffects.DAMAGE_RESISTANCE) ? Aspect.ARMOR : type.equals(MobEffects.WATER_BREATHING) ? Aspect.AIR : null;
            if (type.equals(MobEffects.FIRE_RESISTANCE)) tags.merge(Aspect.ARMOR, strength).merge(Aspect.FIRE, strength * 2);
            else if (aspect != null) tags.merge(aspect, strength * 3);
        }
        return tags;
    }

    private static AspectList cull(AspectList source) {
        AspectList result = source.copy();
        while (result.size() > 6) {
            Aspect lowest = null;
            float minimum = 32767;
            for (Aspect aspect : result.getAspects()) {
                float weight = result.getAmount(aspect);
                if (aspect.isPrimal()) weight *= .9F;
                else for (Aspect component : aspect.getComponents()) if (!component.isPrimal()) {
                    weight *= 1.1F;
                    for (Aspect grandparent : component.getComponents()) if (!grandparent.isPrimal()) weight *= 1.05F;
                }
                if (weight < minimum) { minimum = weight; lowest = aspect; }
            }
            if (lowest == null) break; // Defensive bound for externally supplied overflowing enchantment data.
            result.remove(lowest);
        }
        return result;
    }

    /**
     * The original enchantment table of {@code ThaumcraftCraftingManager.getBonusTags}.
     *
     * <p>Read from the 1.7.10 jar bytecode: the enchantment branch of {@code getBonusTags} compares
     * the stored effect id with the fourteen vanilla enchantment constants and merges the listed
     * aspect. The two entries that are easy to transpose are {@code Enchantment.field_92091_k}
     * (thorns), which the original merges into {@code WEAPON}, and {@code Enchantment.field_77339_k}
     * (smite), which the original merges into {@code ENTROPY}.</p>
     */
    private static Map<String, Aspect> enchantmentTags() {
        Map<String, Aspect> tags = new LinkedHashMap<>();
        addEnchantments(tags, Aspect.WATER, "aqua_affinity");
        addEnchantments(tags, Aspect.BEAST, "bane_of_arthropods", "lure");
        addEnchantments(tags, Aspect.ARMOR, "blast_protection", "fire_protection", "projectile_protection", "protection");
        addEnchantments(tags, Aspect.TOOL, "efficiency");
        addEnchantments(tags, Aspect.FLIGHT, "feather_falling");
        addEnchantments(tags, Aspect.FIRE, "fire_aspect", "flame");
        addEnchantments(tags, Aspect.GREED, "fortune", "looting", "luck_of_the_sea");
        addEnchantments(tags, Aspect.CRAFT, "infinity");
        addEnchantments(tags, Aspect.AIR, "knockback", "punch", "respiration");
        addEnchantments(tags, Aspect.WEAPON, "power", "sharpness", "thorns");
        addEnchantments(tags, Aspect.EXCHANGE, "silk_touch");
        addEnchantments(tags, Aspect.ENTROPY, "smite");
        addEnchantments(tags, Aspect.EARTH, "unbreaking");
        tags.put("thaumcraft:haste", Aspect.MOTION);
        tags.put("thaumcraft:repair", Aspect.TOOL);
        return Map.copyOf(tags);
    }

    private static void addEnchantments(Map<String, Aspect> target, Aspect aspect, String... ids) {
        for (String id : ids) target.put("minecraft:" + id, aspect);
    }
}
