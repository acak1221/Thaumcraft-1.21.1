package thaumcraft.compat.emi;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiStack;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.lib.ScannedAspectSources;

/** The live player's scanned item variants; never a recipe index of undiscovered sources. */
final class AspectSources {
    private final ScannedAspectSources sources = new ScannedAspectSources(this::ingredients);
    private final Map<Aspect, Converted> converted = new IdentityHashMap<>();
    private List<EmiStack> previousIndex;
    private int previousSize;

    private record Converted(List<ScannedAspectSources.Source> original, List<EmiAspectSourcesRecipe.Source> pages) {}

    List<EmiAspectSourcesRecipe.Source> forAspect(Aspect aspect) {
        var index = EmiApi.getIndexStacks();
        if (previousIndex != index || previousSize != index.size()) {
            previousIndex = index; previousSize = index.size();
            sources.invalidateIngredients(); converted.clear();
        }
        var current = sources.forAspect(aspect);
        Converted cached = converted.get(aspect);
        if (cached == null || cached.original() != current) {
            cached = new Converted(current, current.stream()
                    .map(source -> new EmiAspectSourcesRecipe.Source(source.item(), source.amount())).toList());
            converted.put(aspect, cached);
        }
        return cached.pages();
    }

    static boolean scanned(ItemStack item) {
        var client = Minecraft.getInstance();
        return client.player != null && client.level != null && !item.isEmpty()
                && ThaumcraftApiHelper.hasScannedItem(client.player, item);
    }

    /** Called only when the shared cache refreshes its ingredient catalogue, never from a slot draw. */
    private Collection<ItemStack> ingredients() {
        List<ItemStack> result = new ArrayList<>();
        for (EmiStack stack : EmiApi.getIndexStacks()) {
            ItemStack item = stack.getItemStack();
            if (!item.isEmpty()) result.add(item.copyWithCount(1));
        }
        return result;
    }
}
