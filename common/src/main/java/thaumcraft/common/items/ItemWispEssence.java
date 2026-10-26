package thaumcraft.common.items;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.common.config.ThaumcraftEssences;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;

/** Original ItemWispEssence:40-105; subtypes/NBT become native components. */
public final class ItemWispEssence extends Item implements IEssentiaContainerItem {
    // Original displayAspects: the 48 original aspects in practice, see OriginalDisplayAspects.
    private static final Aspect[] DISPLAY_ASPECTS = OriginalDisplayAspects.ALL;
    // Native TooltipContext no longer supplies the viewing player. ClientSetup
    // supplies a lazy local-player lookup; no client classes or player cache are
    // loaded on a dedicated server. Without a viewer, never reveal a tag name.
    private static Supplier<? extends Player> tooltipViewer = () -> null;

    public ItemWispEssence(Properties properties) { super(properties.stacksTo(64)); }

    public static void setTooltipViewer(Supplier<? extends Player> viewer) { tooltipViewer = Objects.requireNonNull(viewer, "viewer"); }
    public static WispEssenceData data(ItemStack stack) { return stack == null ? null : stack.get(ThaumcraftEssences.WISP_ESSENCE_DATA.get()); }

    @Override public AspectList getAspects(ItemStack stack) {
        WispEssenceData data = data(stack);
        return data == null ? null : data.toAspects();
    }

    @Override public void setAspects(ItemStack stack, AspectList aspects) {
        if (aspects == null || aspects.size() == 0) stack.remove(ThaumcraftEssences.WISP_ESSENCE_DATA.get());
        else stack.set(ThaumcraftEssences.WISP_ESSENCE_DATA.get(), WispEssenceData.fromAspects(aspects));
    }

    /** Native ARGB (opaque), not the legacy RGB-only tint which 1.21 hides. */
    public static int color(ItemStack stack) {
        WispEssenceData data = data(stack);
        Aspect aspect = data == null ? DISPLAY_ASPECTS[(int) Math.floorMod(System.currentTimeMillis() / 500L, DISPLAY_ASPECTS.length)]
                : Aspect.getAspect(data.firstTag());
        return 0xff000000 | aspect.getColor();
    }

    /** One real aspect2 variant per registered aspect, ItemWispEssence:58-63. */
    public static List<ItemStack> creativeVariants() {
        List<ItemStack> result = new ArrayList<>();
        for (Aspect aspect : Aspect.aspects.values()) {
            ItemStack stack = new ItemStack(ThaumcraftEssences.WISP_ESSENCE.get());
            ThaumcraftEssences.WISP_ESSENCE.get().setAspects(stack, new AspectList().add(aspect, 2));
            result.add(stack);
        }
        return List.copyOf(result);
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flags) {
        Player viewer = tooltipViewer.get();
        appendAspectTooltip(stack, viewer != null && viewer.level().isClientSide ? viewer : null, tooltip);
        super.appendHoverText(stack, context, tooltip, flags);
    }

    /** Explicit-viewer form also lets the native runtime check discovery gating. */
    public void appendAspectTooltip(ItemStack stack, Player viewer, List<Component> tooltip) {
        AspectList aspects = getAspects(stack);
        if (aspects == null) return;
        for (Aspect aspect : aspects.getAspectsSorted()) {
            if (viewer != null && KnowledgeAccess.get(viewer).hasDiscoveredAspect(aspect.getTag())) {
                tooltip.add(Component.literal(aspect.getName() + " x" + aspects.getAmount(aspect)));
            } else tooltip.add(Component.translatable("tc.aspect.unknown"));
        }
    }
}
