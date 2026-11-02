package thaumcraft.api.crafting.transfer;

import java.util.Locale;

/** Arranging ingredients never crafts the result or spends wand vis. */
public enum RecipeTransferResult {
    SUCCESS, NO_TARGET, STALE_MENU, UNKNOWN_RECIPE, UNSUPPORTED, LOCKED_RESEARCH,
    MISSING_INGREDIENTS, INVENTORY_FULL, FAILED;

    public String messageKey() { return "tc.recipe_transfer." + name().toLowerCase(Locale.ROOT); }
}
