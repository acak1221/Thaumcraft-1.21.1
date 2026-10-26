package thaumcraft.common.items.wands.foci;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.items.wands.ItemWandCasting;

/** A real, single focus stack with its native component patch; never a mutable stack alias. */
public record FocusData(ItemStack focus) {
    public static final Codec<FocusData> CODEC = ItemStack.STRICT_CODEC.flatXmap(
            stack -> isSupportedFocus(stack) ? DataResult.success(new FocusData(stack))
                    : DataResult.error(() -> "Expected one supported, non-nested wand focus"),
            data -> DataResult.success(data.focus()));
    public static final StreamCodec<RegistryFriendlyByteBuf, FocusData> STREAM_CODEC = new StreamCodec<>() {
        @Override public FocusData decode(RegistryFriendlyByteBuf buffer) {
            ItemStack stack = ItemStack.STREAM_CODEC.decode(buffer);
            if (!isSupportedFocus(stack)) throw new DecoderException("Expected one supported, non-nested wand focus");
            return new FocusData(stack);
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, FocusData data) {
            ItemStack.STREAM_CODEC.encode(buffer, data.focus());
        }
    };

    public FocusData {
        if (!isSupportedFocus(focus)) throw new IllegalArgumentException("Expected one supported, non-nested wand focus");
        focus = focus.copy();
    }

    @Override public ItemStack focus() { return focus.copy(); }

    public static boolean isSupportedFocus(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getCount() == 1
                // A native focus, or a focus of another mod (original ItemWandCasting.setFocus and
                // WandManager.changeFocus took every ItemFocusBasic). An add-on focus is dispatched through
                // its OWN ItemFocusBasic hooks, never as Fire or any other native focus.
                && (isNativeFocus(stack.getItem()) || isAddonFocus(stack.getItem()))
                && !stack.has(ThaumcraftFoci.WAND_FOCUS.get())
                && !stack.has(ThaumcraftContent.WAND_DATA.get())
                && FocusUpgradeData.supportedForFocus(stack);
    }

    /** One of the ten native focus items. */
    public static boolean isNativeFocus(Item item) {
        return item instanceof ItemFocusFire || item instanceof ItemFocusExcavation
                || item instanceof ItemFocusFrost || item instanceof ItemFocusShock
                || item instanceof ItemFocusPortableHole || item instanceof ItemFocusWarding
                || item instanceof ItemFocusTrade || item instanceof ItemFocusHellbat
                || item instanceof ItemFocusPech || item instanceof ItemFocusPrimal;
    }

    /**
     * A focus item of another mod: an {@link ItemFocusBasic} that is not a native focus. It mounts on a wand, is selected and
     * casts through its own hooks ({@code onFocusRightClick}, {@code onUsingFocusTick}, ...), and is drawn with its colour,
     * ornament and depth layer.
     */
    public static boolean isAddonFocus(Item item) {
        return item instanceof ItemFocusBasic && !isNativeFocus(item);
    }

    /** Missing components do not rewrite old wand materials, capacity or stored vis. */
    public static ItemStack get(ItemStack wand) {
        if (wand == null || wand.isEmpty() || wand.getCount() != 1 || !(wand.getItem() instanceof ItemWandCasting)) return ItemStack.EMPTY;
        FocusData data = wand.get(ThaumcraftFoci.WAND_FOCUS.get());
        return data == null ? ItemStack.EMPTY : data.focus();
    }

    // Native ItemStack has identity equality. Component equality must instead
    // compare the whole stack, including custom names/data and component removals.
    @Override public boolean equals(Object other) {
        return this == other || other instanceof FocusData data && ItemStack.matches(focus, data.focus);
    }
    @Override public int hashCode() { return ItemStack.hashItemAndComponents(focus); }
}
