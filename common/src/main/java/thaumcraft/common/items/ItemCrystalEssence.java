package thaumcraft.common.items;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;

/** Original ItemCrystalEssence:39-120. Contents are per ITEM, not per stack.
 * No prototype component: absent/EMPTY/unrelated-NBT are different legacy states.
 * Machine output is pretyped and never enters the untyped lazy RNG branch. */
public final class ItemCrystalEssence extends Item implements IEssentiaContainerItem {
    // Original displayAspects: the 48 original aspects in practice, see OriginalDisplayAspects.
    private static final Aspect[] DISPLAY_ASPECTS = OriginalDisplayAspects.ALL;
    private static Supplier<? extends Player> tooltipViewer = () -> null;
    private final Random random = new Random(); // ORIGINAL item-local, never world RNG.

    public ItemCrystalEssence(Properties properties) { super(properties.stacksTo(64)); }
    public static void setTooltipViewer(Supplier<? extends Player> viewer) { tooltipViewer = Objects.requireNonNull(viewer, "viewer"); }
    public static CrystalEssenceData data(ItemStack stack) {
        return stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemCrystalEssence) ? null : stack.get(ThaumcraftContent.CRYSTAL_ESSENCE_DATA.get());
    }

    /** ONLY canonical registered input, exactly one typed item with contents1. */
    public static ItemStack create(Aspect aspect) {
        if (aspect == null || Aspect.getAspect(aspect.getTag()) != aspect) throw new IllegalArgumentException("Noncanonical crystal input");
        CrystalEssenceData data = CrystalEssenceData.fromAspects(new AspectList().add(aspect, 1));
        var stack = new ItemStack(ThaumcraftContent.CRYSTAL_ESSENCE.get());
        stack.set(ThaumcraftContent.CRYSTAL_ESSENCE_DATA.get(), data);
        return stack;
    }

    @Override public AspectList getAspects(ItemStack stack) {
        CrystalEssenceData data = data(stack);
        return data == null || data.isEmpty() ? null : data.toAspects();
    }

    @Override public void setAspects(ItemStack stack, AspectList aspects) {
        if (stack == null || stack.isEmpty() || stack.getItem() != this) throw new IllegalArgumentException("Not this crystal item");
        // NULL/invalid WHOLE writes throw without modifying old data or ANY other
        // component. Valid empty writes retain explicit EMPTY (legacy NBT exists).
        CrystalEssenceData validated = CrystalEssenceData.fromAspects(aspects);
        stack.set(ThaumcraftContent.CRYSTAL_ESSENCE_DATA.get(), validated);
    }

    private void initializeFresh(ItemStack stack) {
        // Native prototype defaults are not NBT. A nonempty PATCH, including
        // custom name, CUSTOM_DATA (even {}), explicit EMPTY or removed defaults,
        // is the conservative legacy hasTagCompound boundary. Never reroll it.
        if (!stack.isEmpty() && stack.getItem() == this && stack.getComponentsPatch().isEmpty() && DISPLAY_ASPECTS.length != 0) {
            setAspects(stack, new AspectList().add(DISPLAY_ASPECTS[random.nextInt(DISPLAY_ASPECTS.length)], 1));
        }
    }

    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide) initializeFresh(stack);
        super.inventoryTick(stack, level, entity, slot, selected);
    }

    /** Original onCreated has no side guard; server sync remains authoritative. */
    @Override public void onCraftedBy(ItemStack stack, Level level, Player player) {
        initializeFresh(stack);
        super.onCraftedBy(stack, level, player);
    }

    /** Opaque native ARGB; the 500ms cycle is display ONLY, never contents RNG. */
    public static int color(ItemStack stack) {
        CrystalEssenceData data = data(stack);
        Aspect aspect = data == null || data.isEmpty() ? (DISPLAY_ASPECTS.length == 0 ? null
                : DISPLAY_ASPECTS[(int) Math.floorMod(System.currentTimeMillis() / 500L, DISPLAY_ASPECTS.length)]) : Aspect.getAspect(data.firstTag());
        return 0xff000000 | (aspect == null ? 0xffffff : aspect.getColor());
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flags) {
        Player viewer = tooltipViewer.get();
        appendAspectTooltip(stack, viewer != null && viewer.level().isClientSide ? viewer : null, tooltip);
        super.appendHoverText(stack, context, tooltip, flags);
    }

    /** Explicit-viewer form for native knowledge regressions, no client linkage. */
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
