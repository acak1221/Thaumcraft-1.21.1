package thaumcraft.client.lib;

import dev.architectury.event.events.client.ClientTooltipEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.WandVisDiscount;

/**
 * The vis discount of everything the player wears, as one line of a wand's tooltip (the port's own).
 *
 * <p>Original ItemWandCasting:132-183 folds the equipment discount into the percentages of the wand
 * tooltip. The 1.21.1 item tooltip has no player, so those percentages are the wand's own (cap, focus,
 * sceptre); this client line adds what the armour, the accessories and vis exhaustion change, with the
 * same rule that prices a spell or an arcane recipe ({@link WandVisDiscount#percentagePoints}).</p>
 */
public final class WandDiscountTooltip {
    private static boolean initialized;
    private WandDiscountTooltip() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        ClientTooltipEvent.ITEM.register((stack, lines, context, flag) -> {
            var player = Minecraft.getInstance().player;
            if (player == null || !(stack.getItem() instanceof ItemWandCasting)) return;
            List<String> names = new ArrayList<>();
            List<Integer> points = new ArrayList<>();
            for (String tag : WandData.PRIMALS) {
                Aspect aspect = Aspect.getAspect(tag);
                if (aspect == null) continue;
                names.add(aspect.getName());
                points.add(WandVisDiscount.percentagePoints(player, aspect));
            }
            line(names, points).ifPresent(lines::add);
        });
    }

    /**
     * Nothing while the equipment changes nothing; one number when every primal is changed alike; the
     * primals that are changed, each with its number, otherwise (the hover harness favours air).
     *
     * @param points net percentage points per primal: positive lowers the cost, negative (vis exhaustion) raises it
     */
    static Optional<Component> line(List<String> names, List<Integer> points) {
        if (points.stream().allMatch(value -> value == 0)) return Optional.empty();
        boolean alike = points.stream().distinct().count() == 1;
        boolean worse = points.stream().allMatch(value -> value <= 0);
        ChatFormatting colour = worse ? ChatFormatting.RED : ChatFormatting.AQUA;
        if (alike) return Optional.of(Component.translatable("tc.port.wand.gear_discount", signed(points.getFirst())).withStyle(colour));
        List<String> parts = new ArrayList<>();
        for (int index = 0; index < points.size(); index++) {
            if (points.get(index) != 0) parts.add(names.get(index) + " " + signed(points.get(index)) + "%");
        }
        return Optional.of(Component.translatable("tc.port.wand.gear_discount_each", String.join(", ", parts)).withStyle(colour));
    }

    /** The change of the cost: a discount of five points reads -5, an exhaustion of ten reads +10. */
    private static String signed(int points) { return points > 0 ? "-" + points : "+" + -points; }
}
