package thaumcraft.compat.jei;

import java.util.List;
import java.util.Map;
import mezz.jei.api.recipe.IFocus;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.client.lib.ScannedAspectSources;

/** The recipe pages the JEI categories draw; the infusion pages are {@code RecipeViews.Infusion}. */
final class Pages {
    private Pages() {}

    /** The nine workbench cells in slot order ({@code Ingredient.EMPTY} for an empty one), the result and the vis. */
    record Arcane(ResourceLocation id, String research, List<Ingredient> grid, ItemStack result, Map<String, Integer> vis, boolean shapeless) {}

    /** The catalyst thrown into the essentia the crucible holds. */
    record Crucible(ResourceLocation id, String research, Ingredient catalyst, ItemStack result, Map<String, Integer> aspects) {}

    /** A compound aspect and the two the research table combines into it. */
    record Combination(Aspect aspect) {}

    /** One aspect contained in a scanned item, not a recipe that manufactures an item. */
    record Source(ItemStack item, Aspect aspect, int amount, Pending pending, Object connection) {
        Source(ItemStack item, Aspect aspect, int amount) { this(item, aspect, amount, null); }
        Source(ItemStack item, Aspect aspect, int amount, Pending pending) {
            this(item, aspect, amount, pending, Minecraft.getInstance().getConnection());
        }
        Source { item = item.isEmpty() ? ItemStack.EMPTY : item.copyWithCount(1); }
        @Override public ItemStack item() { return current() ? item.copy() : ItemStack.EMPTY; }

        /** Only for a guarded native slot: history can rebuild it while scans are revoked. */
        ItemStack slotSnapshot() { return item.copy(); }

        boolean current() {
            Minecraft client = Minecraft.getInstance();
            return connection != null && connection == client.getConnection() && client.player != null
                    && client.level != null && !item.isEmpty() && ThaumcraftApiHelper.hasScannedItem(client.player, item);
        }

        static Source loading(ScannedAspectSources sources, IFocus<AspectIngredient.Stack> focus) {
            return new Source(ItemStack.EMPTY, focus.getTypedValue().getIngredient().aspect(), 0, new Pending(sources, focus));
        }
    }

    /** State belongs to this visible placeholder, never to a process-wide selected aspect. */
    static final class Pending {
        final ScannedAspectSources sources;
        final IFocus<AspectIngredient.Stack> focus;
        boolean refreshScheduled;
        boolean refreshCompleted;

        Pending(ScannedAspectSources sources, IFocus<AspectIngredient.Stack> focus) {
            this.sources = sources; this.focus = focus;
        }
    }
}
