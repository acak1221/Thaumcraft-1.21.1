package thaumcraft.common.items.wands;

import java.util.List;

/**
 * The numbers the wand's item model selector reads: one float per part, matched against the {@code overrides} of {@code wand.json}.
 * The client registers these as the item properties {@code thaumcraft:wand_rod}, {@code wand_cap} and {@code wand_focus}; they live here
 * so that the model files and their selector can be checked without a client. A rod's value is its place in {@link WandMaterials#RODS}
 * over the number of gaps, a cap's is its place over the gaps, and a focus has a fixed one; every value is a binary fraction, so the
 * {@code >=} test of a model override is exact.
 */
public final class WandModels {
    /** The focus part of a wand's model: the suffix of the model file and the property value. */
    public record Focus(String suffix, float value) {}

    public static final Focus NONE = new Focus("", 0F);
    public static final Focus OTHER = new Focus("fire", .25F);
    public static final Focus EXCAVATION = new Focus("excavation", .5F);
    public static final Focus FROST = new Focus("frost", .75F);
    public static final Focus SHOCK = new Focus("shock", 1F);
    public static final List<Focus> FOCI = List.of(NONE, OTHER, EXCAVATION, FROST, SHOCK);

    private WandModels() {}

    public static float rodValue(String tag) {
        for (int index = 0; index < WandMaterials.RODS.size(); index++) {
            if (WandMaterials.RODS.get(index).tag().equals(tag)) return index / (float) (WandMaterials.RODS.size() - 1);
        }
        return 0F;
    }

    public static float capValue(String tag) {
        for (int index = 0; index < WandMaterials.CAPS.size(); index++) {
            if (WandMaterials.CAPS.get(index).tag().equals(tag)) return index / (float) (WandMaterials.CAPS.size() - 1);
        }
        return 0F;
    }

    /** The model of a wand, like {@code thaumcraft:item/wand_obsidian_gold_frost}. */
    public static String model(String rod, String cap, Focus focus) {
        return "thaumcraft:item/wand_" + rod + "_" + cap + (focus.suffix().isEmpty() ? "" : "_" + focus.suffix());
    }
}
