package thaumcraft.common.items;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.common.blocks.BlockAlembic;
import thaumcraft.common.blocks.BlockJar;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftLabels;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;

/** Native block/item unification of original ItemJarFilled, including real contained aspects. */
public final class ItemJar extends BlockItem implements IEssentiaContainerItem {
    public static final int CAPACITY = 64;
    // TooltipContext has no player in 1.21. The client supplies a lazy lookup,
    // never a cached player or a client-class dependency on a dedicated server.
    private static Supplier<? extends Player> tooltipViewer = () -> null;

    public ItemJar(Block block, Item.Properties properties) { super(block, properties); }

    public static void setTooltipViewer(Supplier<? extends Player> viewer) {
        tooltipViewer = Objects.requireNonNull(viewer, "viewer");
    }

    public static boolean isJar(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof ItemJar;
    }

    /** Variant is an actual block identity/property, never editable item NBT. */
    public static boolean isVoid(ItemStack stack) {
        return isJar(stack) && ((ItemJar) stack.getItem()).getBlock() instanceof BlockJar jar && jar.isVoid();
    }

    /** A retained zero-amount type is not itself contained essentia. */
    public static Aspect getAspect(ItemStack stack) {
        if (!isJar(stack)) return null;
        CompoundTag tag = blockData(stack);
        return tag.contains("Aspect", Tag.TAG_STRING) ? Aspect.getAspect(tag.getString("Aspect")) : null;
    }

    /** Malformed, untyped and out-of-capacity data never exposes free contained units. */
    public static int getAmount(ItemStack stack) {
        if (!isJar(stack)) return 0;
        CompoundTag tag = blockData(stack);
        if (!tag.contains("Amount", Tag.TAG_INT)) return 0;
        int amount = tag.getInt("Amount");
        return amount > 0 && amount <= CAPACITY && getAspect(stack) != null ? amount : 0;
    }

    public static Aspect getFilter(ItemStack stack) {
        if (!isJar(stack)) return null;
        CompoundTag tag = blockData(stack);
        // The scalar is the placed block's persisted filter. The native item
        // component also carries label-only drops without inventing contents.
        if (tag.contains("AspectFilter")) {
            if (!tag.contains("AspectFilter", Tag.TAG_STRING)) return null;
            String filter = tag.getString("AspectFilter");
            if (!filter.isEmpty()) return Aspect.getAspect(filter);
        }
        JarLabelData label = stack.get(ThaumcraftLabels.JAR_LABEL_DATA.get());
        return label == null ? null : Aspect.getAspect(label.tag());
    }

    /**
     * Plain original block-jar ingredients could never consume ItemJarFilled.
     * Refuse even malformed/unknown meaningful BE payloads instead of treating
     * failed parsing as an empty jar. Harmless item names/lore are not contents.
     */
    public static boolean isEmptyIngredient(ItemStack stack) {
        if (!isJar(stack) || stack.has(ThaumcraftLabels.JAR_LABEL_DATA.get())) return false;
        CompoundTag tag = blockData(stack);
        for (String key : tag.getAllKeys()) {
            switch (key) {
                case "id" -> {
                    if (!tag.contains(key, Tag.TAG_STRING) || !tag.getString(key).equals(
                            BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(ThaumcraftContent.JAR_BLOCK_ENTITY.get()).toString())) return false;
                }
                case "Aspect", "AspectFilter" -> {
                    if (!tag.contains(key, Tag.TAG_STRING) || !tag.getString(key).isEmpty()) return false;
                }
                case "Amount" -> {
                    if (!tag.contains(key, Tag.TAG_INT) || tag.getInt(key) != 0) return false;
                }
                case "facing" -> {
                    if (!(tag.contains(key, Tag.TAG_BYTE) || tag.contains(key, Tag.TAG_SHORT) || tag.contains(key, Tag.TAG_INT))
                            || tag.getInt(key) < 2 || tag.getInt(key) > 5) return false;
                }
                default -> { return false; }
            }
        }
        return true;
    }

    /**
     * Transaction output only: ONE copy, with all unrelated components/BE keys
     * and the filter retained. The caller, not this helper, pays the held input.
     * Invalid requested contents throw before changing any stack; no clamping
     * can silently destroy units. A zero request clears contents, not labels.
     */
    public static ItemStack withContents(ItemStack stack, Aspect aspect, int amount) {
        requireJar(stack);
        validateContents(aspect, amount);
        ItemStack result = stack.copyWithCount(1);
        writeContents(result, aspect, amount);
        return result;
    }

    @Override public AspectList getAspects(ItemStack stack) {
        int amount = getAmount(stack);
        return amount == 0 ? null : new AspectList().add(getAspect(stack), amount);
    }

    @Override public void setAspects(ItemStack stack, AspectList aspects) {
        requireJar(stack);
        if (aspects == null || aspects.size() == 0) {
            writeContents(stack, null, 0);
            return;
        }
        if (aspects.size() != 1) throw new IllegalArgumentException("A jar holds exactly one aspect");
        var entry = aspects.aspects.entrySet().iterator().next();
        if (entry.getValue() == null || entry.getValue() <= 0) throw new IllegalArgumentException("Positive jar amount required");
        validateContents(entry.getKey(), entry.getValue());
        // Unlike the original separate maxstack1 filled item, this BlockItem
        // can be a stack of empties. Never fill every jar in that stack at once.
        if (stack.getCount() != 1) throw new IllegalArgumentException("Split one empty jar before setting contents");
        writeContents(stack, entry.getKey(), entry.getValue());
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        InteractionResult interaction = BlockAlembic.tryJarInteraction(context);
        return interaction == InteractionResult.PASS ? super.useOn(context) : interaction;
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flags) {
        Player viewer = tooltipViewer.get();
        appendAspectTooltip(stack, viewer != null && viewer.level().isClientSide ? viewer : null, tooltip);
        // BlockItem's super delegates back to BlockJar's old filter-only
        // tooltip. This item now owns BOTH contents and filter: delegating
        // would duplicate that line (and bypass this viewer-aware policy).
    }

    /** Explicit viewer also permits server-side checks of original discovery gating. */
    public void appendAspectTooltip(ItemStack stack, Player viewer, List<Component> tooltip) {
        AspectList aspects = getAspects(stack);
        if (aspects != null) for (Aspect aspect : aspects.getAspectsSorted()) {
            tooltip.add(discovered(viewer, aspect) ? Component.literal(aspect.getName() + " x " + aspects.getAmount(aspect))
                    : Component.translatable("tc.aspect.unknown"));
        }
        Aspect filter = getFilter(stack);
        if (filter != null) tooltip.add((discovered(viewer, filter) ? Component.literal(filter.getName())
                : Component.translatable("tc.aspect.unknown")).withStyle(ChatFormatting.DARK_PURPLE));
    }

    private static boolean discovered(Player player, Aspect aspect) {
        return player != null && KnowledgeAccess.get(player).hasDiscoveredAspect(aspect.getTag());
    }

    private static void requireJar(ItemStack stack) {
        if (!isJar(stack)) throw new IllegalArgumentException("A native jar item is required");
    }

    private static void validateContents(Aspect aspect, int amount) {
        if (amount < 0 || amount > CAPACITY || amount > 0 && aspect == null
                || aspect != null && Aspect.getAspect(aspect.getTag()) != aspect) {
            throw new IllegalArgumentException("Known jar aspect and amount0..64 required");
        }
    }

    private static CompoundTag blockData(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    private static void writeContents(ItemStack stack, Aspect aspect, int amount) {
        CompoundTag tag = blockData(stack);
        if (amount == 0) {
            tag.remove("Aspect");
            tag.remove("Amount");
        } else {
            tag.putString("Aspect", aspect.getTag());
            tag.putInt("Amount", amount);
        }
        // Supplies the required native BE id, while retaining unrelated BE
        // fields. Removes the component if only the redundant id would remain.
        BlockItem.setBlockEntityData(stack, ThaumcraftContent.JAR_BLOCK_ENTITY.get(), tag);
        boolean labelled = stack.has(ThaumcraftLabels.JAR_LABEL_DATA.get())
                || tag.contains("AspectFilter") && (!tag.contains("AspectFilter", Tag.TAG_STRING) || !tag.getString("AspectFilter").isEmpty());
        // Removing an inherited native component means ABSENT, not "restore
        // the item default". Setting the default removes the patch correctly.
        stack.set(DataComponents.MAX_STACK_SIZE, amount > 0 || labelled ? 1 : stack.getItem().getDefaultMaxStackSize());
    }
}
