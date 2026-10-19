package thaumcraft.common.items.golems;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Original ItemGolemCore:36-78; attachment/payment belongs to the actual golem. */
public final class ItemGolemCore extends Item {
    public static final int BLANK = 100;
    private final int core;

    public ItemGolemCore(Properties properties, int core) {
        super(properties);
        if (core != BLANK && (core == GolemData.NO_CORE || !GolemData.isSupportedCore(core))) {
            throw new IllegalArgumentException("Unsupported golem core: " + core);
        }
        this.core = core;
    }

    public int getCore() { return core; }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flags) {
        super.appendHoverText(stack, context, tooltip, flags);
        tooltip.add(Component.translatable(switch (core) {
            case BLANK -> "tooltip.thaumcraft.golem.core_blank";
            case GolemData.FILL -> "tooltip.thaumcraft.golem.core_fill";
            case GolemData.EMPTY_CORE -> "tooltip.thaumcraft.golem.core_empty";
            case GolemData.GATHER -> "tooltip.thaumcraft.golem.core_gather";
            case GolemData.HARVEST -> "tooltip.thaumcraft.golem.core_harvest";
            case GolemData.LUMBER -> "tooltip.thaumcraft.golem.core_lumber";
            case GolemData.GUARD -> "tooltip.thaumcraft.golem.core_guard";
            case GolemData.LIQUID -> "tooltip.thaumcraft.golem.core_liquid";
            case GolemData.ALCHEMY -> "tooltip.thaumcraft.golem.core_alchemy";
            case GolemData.USE -> "tooltip.thaumcraft.golem.core_use";
            case GolemData.BUTCHER -> "tooltip.thaumcraft.golem.core_butcher";
            case GolemData.SORTING -> "tooltip.thaumcraft.golem.core_sorting";
            case GolemData.FISHING -> "tooltip.thaumcraft.golem.core_fishing";
            default -> throw new IllegalStateException("Unsupported golem core: " + core);
        }));
        if (core == GolemData.FILL || core == GolemData.EMPTY_CORE)
            tooltip.add(Component.translatable("tooltip.thaumcraft.golem.container_routes_only").withStyle(ChatFormatting.GRAY));
    }
}
