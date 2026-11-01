package thaumcraft.compat.jei;

import com.google.gson.Gson;
import java.util.List;
import java.util.function.Function;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.items.CrystalEssenceData;
import thaumcraft.common.items.PhialData;
import thaumcraft.common.items.wands.WandData;

/** Recipe identities for the items whose original subtypes now live in data components. */
final class ItemSubtypes {
    private static final Gson JSON = new Gson();

    private ItemSubtypes() {}

    static ISubtypeInterpreter<ItemStack> wand(DataComponentType<WandData> wandData, DataComponentType<Boolean> sceptre) {
        return interpreter(stack -> {
            WandData data = stack.getOrDefault(wandData, WandData.EMPTY);
            return List.of(data.rod(), data.cap(), stack.getOrDefault(sceptre, false));
        });
    }

    static ISubtypeInterpreter<ItemStack> crystal(DataComponentType<CrystalEssenceData> component) {
        return interpreter(stack -> {
            CrystalEssenceData data = stack.get(component);
            // Explicitly empty and uninitialized crystals are different native stacks. The complete
            // ordered payload matters: the first aspect supplies the tint, and amounts are per item.
            return data == null ? List.of("absent") : List.of("aspects", data.amounts().entrySet().stream()
                    .map(entry -> List.of(entry.getKey(), entry.getValue())).toList());
        });
    }

    static ISubtypeInterpreter<ItemStack> phial(DataComponentType<PhialData> component) {
        return interpreter(stack -> {
            PhialData data = stack.get(component);
            return data == null ? List.of("absent") : List.of("aspect", data.tag());
        });
    }

    private static ISubtypeInterpreter<ItemStack> interpreter(Function<ItemStack, Object> identity) {
        return new ISubtypeInterpreter<>() {
            @Override public Object getSubtypeData(ItemStack stack, UidContext context) { return identity.apply(stack); }

            @SuppressWarnings("deprecation")
            @Override public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                // Also keep JEI's string identifiers distinct and stable when it persists bookmarks.
                return JSON.toJson(identity.apply(stack));
            }
        };
    }
}
