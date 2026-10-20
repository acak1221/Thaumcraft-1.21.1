package thaumcraft.common.items.golems;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Original ItemGolemDecoration, flattened from one damage-valued item to one item per accessory. Installation belongs to the
 * actual golem (right-click, no sneaking); only accessories whose effect is ported exist ({@link GolemDecorations#SUPPORTED}).
 */
public final class ItemGolemDecoration extends Item {
    private final char letter;

    public ItemGolemDecoration(Properties properties, char letter) {
        super(properties);
        if (!GolemDecorations.isSupported(letter)) throw new IllegalArgumentException("Unsupported golem decoration: " + letter);
        this.letter = letter;
    }

    public char letter() { return letter; }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flags) {
        super.appendHoverText(stack, context, tooltip, flags);
        tooltip.add(Component.translatable("tooltip.thaumcraft.golem.decoration_" + Character.toLowerCase(letter)));
    }
}
