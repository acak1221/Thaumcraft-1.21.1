package thaumcraft.common.entities.golems;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import thaumcraft.common.items.golems.GolemUpgrade;

/**
 * Fire upgrade of a Fishing golem (original AIFish:124-130): the paid catch is replaced by the result of the furnace
 * recipe for it, when one exists. The replacement is the recipe's own result stack, not scaled by the catch's count.
 * Registered 1.21 smelting recipes stand in for the old FurnaceRecipes table (cod and salmon cook; junk and treasure do not).
 * The original (AIFish:127-129) also set the delivered item alight for two seconds; {@link #burningTicks} is that flame, carried
 * with the paid catch through the durable drop queue.
 */
public final class GolemFishingSmelting {
    /** Original {@code entityitem.setFire(2)}: two seconds. */
    public static final int BURNING_TICKS = 40;

    private GolemFishingSmelting() {}

    /** How long the catch of this golem burns when it is delivered: two seconds with a Fire upgrade, otherwise not at all. */
    public static int burningTicks(EntityGolemBase golem) {
        return golem != null && golem.getUpgradeAmount(GolemUpgrade.FIRE) > 0 ? BURNING_TICKS : 0;
    }

    /** The catch the golem actually publishes: smelted with at least one Fire upgrade, otherwise untouched. */
    public static ItemStack apply(ServerLevel level, EntityGolemBase golem, ItemStack rolled) {
        if (rolled == null || rolled.isEmpty() || golem == null || golem.getUpgradeAmount(GolemUpgrade.FIRE) <= 0) return rolled;
        return smelted(level, rolled);
    }

    /** The furnace result for {@code stack}, or {@code stack} itself when no smelting recipe accepts it. */
    public static ItemStack smelted(ServerLevel level, ItemStack stack) {
        Optional<RecipeHolder<SmeltingRecipe>> recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level);
        if (recipe.isEmpty()) return stack;
        ItemStack result = recipe.get().value().assemble(new SingleRecipeInput(stack), level.registryAccess());
        return result.isEmpty() ? stack : result.copy();
    }
}
