package thaumcraft.common.lib.loot;

import java.util.ArrayList;
import java.util.List;

/** The 72 nonempty 1.7 PotionHelper results reached by Config.initLoot's bit loop. */
public final class LootPotionVariants {
    public record Variant(int metadata, int weight, int effectId, String name, int duration, int amplifier, int color) {
        public boolean splash() { return (metadata & 0x4000) != 0; }
    }
    private static final List<Variant> VARIANTS = create();
    private LootPotionVariants() {}
    public static List<Variant> all() { return VARIANTS; }
    private static List<Variant> create() {
        List<Variant> result = new ArrayList<>();
        for (int low = 0; low <= 15; low++) {
            int effect = switch (low) { case 1 -> 10; case 2 -> 1; case 3 -> 12; case 4 -> 19; case 5 -> 6; case 6 -> 16; case 8 -> 18; case 9 -> 5; case 10 -> 2; case 12 -> 7; case 13 -> 13; case 14 -> 14; default -> 0; };
            if (effect == 0) continue;
            String name = switch (effect) { case 1 -> "swiftness"; case 2 -> "slowness"; case 5 -> "strength"; case 6 -> "healing"; case 7 -> "harming"; case 10 -> "regeneration"; case 12 -> "fire_resistance"; case 13 -> "water_breathing"; case 14 -> "invisibility"; case 16 -> "night_vision"; case 18 -> "weakness"; case 19 -> "poison"; default -> throw new AssertionError(); };
            int color = switch (effect) { case 1 -> 8171462; case 2 -> 5926017; case 5 -> 9643043; case 6 -> 16262179; case 7 -> 4393481; case 10 -> 13458603; case 12 -> 14981690; case 13 -> 3035801; case 14 -> 8356754; case 16 -> 2039713; case 18 -> 4738376; case 19 -> 5149489; default -> throw new AssertionError(); };
            boolean stronger = effect == 1 || effect == 5 || effect == 6 || effect == 7 || effect == 10 || effect == 19;
            for (int splash = 0; splash <= 1; splash++) for (int variant = 0; variant <= 2; variant++) {
                int duration = effect == 6 || effect == 7 ? 1 : effect == 10 || effect == 19 ? 900 : effect == 2 || effect == 18 ? 1800 : 3600;
                if (variant == 1 && stronger) duration = Math.max(1, duration / 2);
                if (variant == 2 && duration > 1) duration = duration / 3 * 8;
                if (splash == 1 && duration > 1) duration = (int) Math.round(duration * .75D + .5D);
                int metadata = low | (splash == 0 ? 0x2000 : 0x4000) | (variant == 1 ? 0x20 : variant == 2 ? 0x40 : 0);
                result.add(new Variant(metadata, variant + 1, effect, name, duration, variant == 1 && stronger ? 1 : 0, color));
            }
        }
        return List.copyOf(result);
    }
}
