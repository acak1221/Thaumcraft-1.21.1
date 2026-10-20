package thaumcraft.common.items.wands;

import java.util.List;

/**
 * The numbers the wand's item model selector reads: one float per part, matched against the {@code overrides} of {@code wand.json}.
 * The client registers these as the item properties {@code thaumcraft:wand_rod}, {@code wand_cap} and {@code wand_focus}; they live here
 * so that the model files and their selector can be checked without a client. A rod's value is its place in {@link WandMaterials#RODS}
 * over {@link #ROD_SCALE}, a cap's its place over {@link #CAP_SCALE}, and a focus has a fixed one; the scales are powers of two (not
 * the number of parts, which would give thirds and fifths), so every value is an exact binary fraction and the {@code >=} test of a model
 * override is exact however many parts are added later.
 */
public final class WandModels {
    /** The focus part of a wand's model: the suffix of the model file and the property value. */
    public record Focus(String suffix, float value) {}

    public static final Focus NONE = new Focus("", 0F);
    public static final Focus OTHER = new Focus("fire", .25F);
    public static final Focus EXCAVATION = new Focus("excavation", .5F);
    public static final Focus FROST = new Focus("frost", .75F);
    public static final Focus WARDING = new Focus("warding", .8125F);
    public static final Focus TRADE = new Focus("trade", .875F);
    public static final Focus PORTABLE_HOLE = new Focus("portable_hole", .9375F);
    public static final Focus SHOCK = new Focus("shock", 1F);
    public static final List<Focus> FOCI = List.of(NONE, OTHER, EXCAVATION, FROST, WARDING, TRADE, PORTABLE_HOLE, SHOCK);

    /** At most 33 rods (places 0..32) and 5 caps (0..4) fit these scales; the JUnit test fails before a model silently goes wrong. */
    public static final float ROD_SCALE = 32F, CAP_SCALE = 4F;

    private WandModels() {}

    public static float rodValue(String tag) {
        for (int index = 0; index < WandMaterials.RODS.size(); index++) {
            if (WandMaterials.RODS.get(index).tag().equals(tag)) return index / ROD_SCALE;
        }
        return 0F;
    }

    public static float capValue(String tag) {
        for (int index = 0; index < WandMaterials.CAPS.size(); index++) {
            if (WandMaterials.CAPS.get(index).tag().equals(tag)) return index / CAP_SCALE;
        }
        return 0F;
    }

    /** The model of a wand, like {@code thaumcraft:item/wand_obsidian_gold_frost}. */
    public static String model(String rod, String cap, Focus focus) {
        return "thaumcraft:item/wand_" + rod + "_" + cap + (focus.suffix().isEmpty() ? "" : "_" + focus.suffix());
    }
}
