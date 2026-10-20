package thaumcraft.common.crafting;

/** Original metadata potion identities used by the four native runic accessory upgrades. */
public final class InfusionPotionRequirement {
    private static final java.util.Set<String> IDS=java.util.Set.of("minecraft:strong_strength","minecraft:strong_swiftness","minecraft:long_regeneration","minecraft:strong_harming");
    private InfusionPotionRequirement(){}
    public static boolean known(String id){return IDS.contains(id);}
    public static thaumcraft.common.lib.loot.LootPotionVariants.Variant legacyVariant(String id) {
        int metadata=switch(id) {
            case "minecraft:strong_strength" -> 8233;
            case "minecraft:strong_swiftness" -> 8226;
            case "minecraft:long_regeneration" -> 8257;
            case "minecraft:strong_harming" -> 16428;
            default -> throw new IllegalArgumentException("Unsupported original runic potion");
        };
        return thaumcraft.common.lib.loot.LootPotionVariants.all().stream().filter(variant->variant.metadata()==metadata).findFirst().orElseThrow();
    }
}
