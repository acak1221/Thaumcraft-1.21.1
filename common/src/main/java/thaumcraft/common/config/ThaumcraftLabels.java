package thaumcraft.common.config;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.items.ItemEssence;
import thaumcraft.common.items.ItemJarLabel;
import thaumcraft.common.items.JarLabelData;

/** ConfigRecipes:416-428: ordinary label crafting, with no invented arcane cost. */
public final class ThaumcraftLabels {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Thaumcraft.MODID, Registries.DATA_COMPONENT_TYPE);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Thaumcraft.MODID, Registries.RECIPE_SERIALIZER);

    public static final RegistrySupplier<DataComponentType<JarLabelData>> JAR_LABEL_DATA = COMPONENTS.register("jar_label_data",
            () -> DataComponentType.<JarLabelData>builder().persistent(JarLabelData.CODEC).networkSynchronized(JarLabelData.STREAM_CODEC).build());
    public static final RegistrySupplier<RecipeSerializer<BindRecipe>> BIND_SERIALIZER = SERIALIZERS.register("jar_label_bind",
            () -> new SimpleCraftingRecipeSerializer<>(BindRecipe::new));
    public static final RegistrySupplier<RecipeSerializer<ClearRecipe>> CLEAR_SERIALIZER = SERIALIZERS.register("jar_label_clear",
            () -> new SimpleCraftingRecipeSerializer<>(ClearRecipe::new));

    private ThaumcraftLabels() {}

    public static void register() {
        COMPONENTS.register();
        SERIALIZERS.register();
    }

    /** One blank label + one real eight-unit phial. The phial is a catalyst. */
    public static final class BindRecipe extends CustomRecipe {
        public BindRecipe(CraftingBookCategory category) { super(category); }

        private static int phialSlot(CraftingInput input) {
            boolean label = false;
            int phial = -1;
            for (int slot = 0; slot < input.size(); ++slot) {
                ItemStack stack = input.getItem(slot);
                if (stack.isEmpty()) continue;
                if (stack.getItem() instanceof ItemJarLabel && ItemJarLabel.data(stack) == null && !label) label = true;
                else if (stack.is(ThaumcraftContent.FILLED_PHIAL.get()) && ItemEssence.data(stack) != null && phial < 0) phial = slot;
                else return -1;
            }
            return label ? phial : -1;
        }

        @Override public boolean matches(CraftingInput input, Level level) { return phialSlot(input) >= 0; }
        @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
            int phial = phialSlot(input);
            return phial < 0 ? ItemStack.EMPTY : ItemJarLabel.bound(ItemEssence.data(input.getItem(phial)).tag());
        }
        @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
            NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
            int phial = phialSlot(input);
            // EventHandlerWorld:155-161 refunded the original phial. Return ONE
            // copy, not the entire input stack, including all native components.
            if (phial >= 0) remaining.set(phial, input.getItem(phial).copyWithCount(1));
            return remaining;
        }
        @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }
        @Override public RecipeSerializer<?> getSerializer() { return BIND_SERIALIZER.get(); }
    }

    /** The original NBT-insensitive JarLabelNull recipe also accepts a blank. */
    public static final class ClearRecipe extends CustomRecipe {
        public ClearRecipe(CraftingBookCategory category) { super(category); }
        private static boolean onlyLabel(CraftingInput input) {
            boolean found = false;
            for (ItemStack stack : input.items()) {
                if (stack.isEmpty()) continue;
                if (found || !(stack.getItem() instanceof ItemJarLabel)) return false;
                found = true;
            }
            return found;
        }
        @Override public boolean matches(CraftingInput input, Level level) { return onlyLabel(input); }
        @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
            return onlyLabel(input) ? ItemJarLabel.blank() : ItemStack.EMPTY;
        }
        @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 1; }
        @Override public RecipeSerializer<?> getSerializer() { return CLEAR_SERIALIZER.get(); }
    }
}
