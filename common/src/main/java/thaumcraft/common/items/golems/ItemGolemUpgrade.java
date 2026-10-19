package thaumcraft.common.items.golems;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Original ItemGolemUpgrade:31-77, flattened from one damage-valued item to one item per type.
 * Installation belongs to the actual golem (right-click, no sneaking); only ported types exist.
 */
public final class ItemGolemUpgrade extends Item {
    private final GolemUpgrade kind;

    public ItemGolemUpgrade(Properties properties, GolemUpgrade kind) {
        super(properties);
        if (kind == null || !kind.isSupported()) throw new IllegalArgumentException("Unsupported golem upgrade: " + kind);
        this.kind = kind;
    }

    public GolemUpgrade kind() { return kind; }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flags) {
        super.appendHoverText(stack, context, tooltip, flags);
        tooltip.add(Component.translatable("tooltip.thaumcraft.golem.upgrade_" + kind.key));
        // Not every original effect of a type exists yet; never imply otherwise.
        tooltip.add(Component.translatable("tooltip.thaumcraft.golem.upgrade_" + kind.key + "_port").withStyle(ChatFormatting.GRAY));
    }
}
