package thaumcraft.common.lib.research;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

/** The closed original conditional expressions, evaluated as facts rather than executable legacy Java. */
public final class ResearchConditions {
    private static volatile boolean nativeReady;
    private ResearchConditions() {}
    public static void initializeNative() { nativeReady = true; }
    public static void setFeatures(boolean wardsAllowed, boolean mirrorsAllowed) {
        var settings = thaumcraft.common.config.ThaumcraftSettings.server();
        thaumcraft.common.config.ThaumcraftSettings.configureServer(settings.withFeatures(wardsAllowed, mirrorsAllowed));
    }
    public static boolean available(List<String> conditions) { return evaluate(conditions, ResearchConditions::fact); }
    public static boolean evaluate(List<String> conditions, Predicate<String> facts) {
        for (String expression : conditions) {
            String text = expression.strip();
            if (!text.startsWith("if (") || !text.endsWith(")")) return false;
            for (String term : text.substring(4, text.length() - 1).split("&&", -1)) {
                String value = term.strip();
                if (!value.matches("Config\\.[A-Za-z][A-Za-z0-9]*") || !facts.test(value.substring(7))) return false;
            }
        }
        return true;
    }
    private static boolean fact(String name) {
        return switch (name) {
            case "wardedStone" -> thaumcraft.common.config.ThaumcraftSettings.current().wardedStone();
            case "allowMirrors" -> thaumcraft.common.config.ThaumcraftSettings.current().mirrors();
            // Copper exists in vanilla 1.21.1, independently of optional material tags.
            case "foundCopperIngot", "foundCopperOre" -> true;
            case "foundSilverIngot" -> tagged("ingots/silver");
            case "foundSilverOre" -> tagged("ores/silver");
            case "foundLeadIngot" -> tagged("ingots/lead");
            case "foundLeadOre" -> tagged("ores/lead");
            case "foundTinIngot" -> tagged("ingots/tin");
            case "foundTinOre" -> tagged("ores/tin");
            default -> false;
        };
    }
    private static boolean tagged(String path) {
        if (!nativeReady) return false;
        for (String namespace : List.of("c", "forge")) {
            var key = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, path));
            var members = BuiltInRegistries.ITEM.getTag(key);
            if (members.isPresent() && members.get().size() > 0) return true;
        }
        return false;
    }
}
