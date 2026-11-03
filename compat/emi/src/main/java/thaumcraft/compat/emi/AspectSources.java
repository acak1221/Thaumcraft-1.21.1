package thaumcraft.compat.emi;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiStack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.ObjectAspectResolver;

/** The live player's scanned item variants; never a recipe index of undiscovered sources. */
final class AspectSources {
    private ClientLevel previousLevel;
    private LocalPlayer previousPlayer;
    private List<EmiStack> previousIndex;
    private int previousSize;
    private Set<String> previousScans = Set.of();
    private List<ItemStack> known = List.of();

    List<EmiAspectSourcesRecipe.Source> forAspect(Aspect aspect) {
        List<EmiAspectSourcesRecipe.Source> result = new ArrayList<>();
        for (ItemStack item : knownItems()) {
            if (!scanned(item)) continue;
            var tags = ObjectAspectResolver.get(item, Minecraft.getInstance().level);
            int amount = tags == null ? 0 : tags.getAmount(aspect);
            if (amount > 0) result.add(new EmiAspectSourcesRecipe.Source(item.copyWithCount(1), amount));
        }
        return List.copyOf(result);
    }

    static boolean scanned(ItemStack item) {
        var client = Minecraft.getInstance();
        return client.player != null && client.level != null && !item.isEmpty()
                && ThaumcraftApiHelper.hasScannedItem(client.player, item);
    }

    /** Cache scan membership only. Resolve current object tags separately, retaining component variants. */
    private List<ItemStack> knownItems() {
        var client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            previousLevel = null; previousPlayer = null; previousIndex = null;
            previousScans = Set.of(); known = List.of();
            return known;
        }
        var index = EmiApi.getIndexStacks();
        var scans = KnowledgeAccess.get(client.player).getScannedItems();
        if (previousLevel != client.level || previousPlayer != client.player || previousIndex != index
                || previousSize != index.size() || !previousScans.equals(scans)) {
            List<ItemStack> items = new ArrayList<>();
            for (EmiStack stack : index) {
                ItemStack item = stack.getItemStack();
                if (scanned(item)) items.add(item.copyWithCount(1));
            }
            items.sort(Comparator.comparing(item -> item.getHoverName().getString()));
            known = List.copyOf(items); previousLevel = client.level; previousPlayer = client.player;
            previousIndex = index; previousSize = index.size(); previousScans = Set.copyOf(scans);
        }
        return known;
    }
}
