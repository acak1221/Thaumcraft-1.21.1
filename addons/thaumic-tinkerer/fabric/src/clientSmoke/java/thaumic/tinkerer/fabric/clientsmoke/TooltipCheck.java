package thaumic.tinkerer.fabric.clientsmoke;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * The tooltip of a stack, as the game builds it for the player, searched for translation keys that have no text. Test-only
 * code of the port (no 1.7.10 counterpart); CC BY-NC-SA 3.0 like the add-on, not distributed.
 *
 * <p>Two things are looked for in each line: a translatable component whose key the language does not have, and a plain line
 * that is nothing but a key ({@code ttmisc.something}), which is what an add-on gets from translating a key twice or from
 * not translating it at all.</p>
 */
final class TooltipCheck {
    private static final Pattern RAW_KEY = Pattern.compile("^[a-z][A-Za-z0-9_]*(?:\\.[A-Za-z0-9_]+)+$");

    private TooltipCheck() {}

    static boolean isRawKey(String text) {
        return RAW_KEY.matcher(text.trim()).matches();
    }

    /** The lines of the tooltip and the problems found in them. */
    record Result(List<String> lines, List<String> problems) {}

    static Result of(Minecraft client, ItemStack stack) {
        List<String> lines = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        List<Component> tooltip = stack.getTooltipLines(Item.TooltipContext.of(client.level), client.player, TooltipFlag.Default.NORMAL);
        for (Component line : tooltip) {
            String text = line.getString();
            lines.add(text);
            if (isRawKey(text)) problems.add("the line '" + text + "' is a raw translation key");
            missingKeys(line, problems);
        }
        return new Result(lines, problems);
    }

    private static void missingKeys(Component component, List<String> problems) {
        if (component.getContents() instanceof TranslatableContents translatable) {
            if (!Language.getInstance().has(translatable.getKey())) problems.add("translation key '" + translatable.getKey() + "' has no text");
            for (Object argument : translatable.getArgs()) {
                if (argument instanceof Component nested) missingKeys(nested, problems);
            }
        }
        for (Component sibling : component.getSiblings()) missingKeys(sibling, problems);
    }
}
