package thaumcraft.common.testing;

import java.util.Collections;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.entities.golems.GolemFishingSmelting;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemUpgrade;
import thaumcraft.common.items.golems.GolemUseOptions;

/**
 * Fire upgrade of a Fishing golem: the catch the golem publishes goes through the server's registered furnace recipes.
 * A real Fishing golem with and without the upgrade, and the live recipe manager, are the INPUTS. This drives the smelting
 * step the fishing goal calls with a rolled catch; it does not cast a line, so the goal's own call site is compiled, not
 * exercised here.
 */
public final class GolemFishingSmeltingRuntimeChecks {
    private static final int TALLOW = 2;

    private GolemFishingSmeltingRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        var base = test.absolutePos(new net.minecraft.core.BlockPos(3, 3, 3));
        var spawned = new java.util.ArrayList<Entity>();
        try {
            var fire = fishing(test, level, List.of(GolemUpgrade.FIRE.id), base, spawned);
            var plain = fishing(test, level, List.of(), base.east(2), spawned);
            test.assertTrue(fire.getUpgradeAmount(GolemUpgrade.FIRE) == 1 && plain.getUpgradeAmount(GolemUpgrade.FIRE) == 0,
                    "one Fishing golem carries Fire, the other does not");

            ItemStack cod = new ItemStack(Items.COD);
            ItemStack cooked = GolemFishingSmelting.apply(level, fire, cod);
            test.assertTrue(cooked.is(Items.COOKED_COD) && cooked.getCount() == 1, "Fire cooks a cod: " + cooked);
            test.assertTrue(cod.is(Items.COD) && cod.getCount() == 1, "the rolled stack itself is not rewritten");
            test.assertTrue(GolemFishingSmelting.apply(level, fire, new ItemStack(Items.SALMON)).is(Items.COOKED_SALMON), "Fire cooks a salmon");

            test.assertTrue(GolemFishingSmelting.apply(level, plain, new ItemStack(Items.COD)).is(Items.COD),
                    "a Fishing golem without Fire delivers the raw cod");
            test.assertTrue(GolemFishingSmelting.apply(level, plain, new ItemStack(Items.SALMON)).is(Items.SALMON), "and the raw salmon");

            // Catches with no furnace recipe pass through untouched, with their own components.
            for (var item : List.of(Items.TROPICAL_FISH, Items.PUFFERFISH, Items.BOWL, Items.LEATHER, Items.STICK, Items.STRING, Items.BONE,
                    Items.INK_SAC, Items.LILY_PAD, Items.ROTTEN_FLESH, Items.NAME_TAG, Items.SADDLE, Items.NAUTILUS_SHELL, Items.TRIPWIRE_HOOK)) {
                ItemStack rolled = new ItemStack(item);
                ItemStack result = GolemFishingSmelting.apply(level, fire, rolled);
                test.assertTrue(result.is(item) && result.getCount() == 1, "Fire leaves " + item + " alone: " + result);
            }
            ItemStack empty = GolemFishingSmelting.apply(level, fire, ItemStack.EMPTY);
            test.assertTrue(empty.isEmpty(), "nothing caught stays nothing");
            test.assertTrue(GolemFishingSmelting.apply(level, null, new ItemStack(Items.COD)).is(Items.COD), "without a golem nothing is smelted");
        } finally {
            for (Entity entity : spawned) entity.discard();
        }
        test.succeed();
    }

    private static EntityGolemBase fishing(GameTestHelper test, ServerLevel level, List<Integer> upgrades,
                                           net.minecraft.core.BlockPos at, List<Entity> spawned) {
        var golem = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), level);
        golem.setGolemMaterial(TALLOW);
        golem.applyConfiguration(new GolemData(GolemData.FISHING, Collections.nCopies(GolemData.FILTER_SLOTS, ItemStack.EMPTY),
                Collections.nCopies(GolemData.FILTER_SLOTS, 0), false, List.of(), GolemUseOptions.DEFAULT, upgrades));
        golem.setPos(Vec3.atBottomCenterOf(at));
        golem.setHome(at, Direction.UP);
        test.assertTrue(level.addFreshEntity(golem), "the Fishing golem is admitted");
        spawned.add(golem);
        return golem;
    }
}
