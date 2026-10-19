package thaumcraft.client;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.client.gui.GuiFocalManipulator;
import thaumcraft.client.renderers.tile.TileFocalManipulatorRenderer;
import thaumcraft.common.config.ThaumcraftFocalManipulation;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.network.FocalManipulatorNetworking;
import thaumcraft.common.tiles.TileFocalManipulator;

/** Client-only integration. Common menu/network/wand classes never load Screen,
 * Minecraft or a renderer on dedicated servers. Root owns the bootstrap call. */
public final class ThaumcraftFocalClient {
    private static boolean initialized;
    private ThaumcraftFocalClient() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        MenuRegistry.registerScreenFactory(ThaumcraftFocalManipulation.FOCAL_MANIPULATOR_MENU.get(), GuiFocalManipulator::new);
        BlockEntityRendererRegistry.register(ThaumcraftFocalManipulation.FOCAL_MANIPULATOR_BLOCK_ENTITY.get(), TileFocalManipulatorRenderer::new);
        TileFocalManipulator.setClientEffects(new FocalManipulatorClientEffects());
        FocalManipulatorNetworking.setClientAckReceiver(ack -> {
            if (Minecraft.getInstance().screen instanceof GuiFocalManipulator screen) screen.acceptAck(ack);
        });
        ItemWandCasting.setFocusUpgradeTooltip(ThaumcraftFocalClient::appendWandUpgradeTooltip);
    }

    private static void appendWandUpgradeTooltip(ItemStack focusStack, List<Component> tooltip) {
        // TooltipFlag.advanced is F3+H, not Shift. Query native input only on the
        // client thread; a server-generated tooltip must not touch GLFW input.
        if (!Minecraft.getInstance().isSameThread() || !Screen.hasShiftDown()
                || !(focusStack.getItem() instanceof ItemFocusBasic focus)) return;
        var counts = new LinkedHashMap<FocusUpgradeType, Integer>();
        try {
            for (short id : focus.getAppliedUpgrades(focusStack)) {
                if (id == -1) continue;
                FocusUpgradeType type = FocusUpgradeType.get(id);
                if (type == null) throw new IllegalArgumentException("Unknown focus upgrade");
                counts.merge(type, 1, Integer::sum);
            }
        } catch (IllegalArgumentException invalid) {
            tooltip.add(Component.translatable("tc.port.focus.upgrades.invalid").withStyle(ChatFormatting.RED));
            return;
        }
        counts.forEach((type, count) -> {
            var line = Component.translatable(type.name).withStyle(ChatFormatting.DARK_PURPLE);
            if (count > 1) line.append(" ").append(Component.translatable("enchantment.level." + count));
            tooltip.add(line);
        });
    }
}
