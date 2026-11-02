package thaumcraft.common.testing;

import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.client.renderers.item.WandRenderIdentity;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.config.ThaumcraftWandEnchantments;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.foci.FocusData;

/** Actual registered components/stacks, so charge-only comparison cannot erase a visible change. */
public final class WandEquipRuntimeChecks {
    private WandEquipRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        ItemStack resting = new ItemStack(ThaumcraftContent.WAND.get());
        ItemStack charging = resting.copy();
        charging.set(ThaumcraftContent.WAND_DATA.get(), WandData.EMPTY.withAmount("aer", 100));
        test.assertTrue(!ItemStack.matches(resting, charging) && WandRenderIdentity.matches(resting, charging),
                "a genuine vis-only slot sync keeps the equipped wand instead of lowering it");
        test.assertTrue(WandRenderIdentity.matches(charging, resting), "vis spending also preserves the equipped model");
        test.assertTrue(ItemStack.matches(new ItemStack(ThaumcraftContent.WAND.get()), resting)
                        && charging.get(ThaumcraftContent.WAND_DATA.get()).amount("aer") == 100,
                "render equality never edits either physical stack or its charge");

        ItemStack changed = charging.copy();
        changed.set(ThaumcraftContent.WAND_DATA.get(), new WandData("greatwood", "iron", Map.of("aer", 100)));
        test.assertTrue(!WandRenderIdentity.matches(resting, changed), "changing the rod still equips the new model");
        changed = charging.copy();
        changed.set(ThaumcraftContent.WAND_DATA.get(), new WandData("wood", "gold", Map.of("aer", 100)));
        test.assertTrue(!WandRenderIdentity.matches(resting, changed), "changing the cap still equips the new model");
        changed = charging.copy();
        changed.set(DataComponents.CUSTOM_NAME, Component.literal("another wand"));
        test.assertTrue(!WandRenderIdentity.matches(resting, changed), "a custom name change is not hidden by charge matching");
        changed = charging.copy();
        changed.set(ThaumcraftFoci.WAND_FOCUS.get(), new FocusData(new ItemStack(ThaumcraftFoci.FIRE_FOCUS.get())));
        test.assertTrue(!WandRenderIdentity.matches(resting, changed), "installing a focus remains a visible equipped change");
        changed = charging.copy();
        changed.set(ThaumcraftWandEnchantments.WAND_SCEPTRE.get(), true);
        test.assertTrue(!WandRenderIdentity.matches(resting, changed), "the sceptre ornament is still a visible equipped change");
        changed = charging.copyWithCount(2);
        test.assertTrue(!WandRenderIdentity.matches(resting, changed), "a stack quantity change is never erased");
        ItemStack ordinary = new ItemStack(Items.STICK), renamed = ordinary.copy();
        renamed.set(DataComponents.CUSTOM_NAME, Component.literal("changed ordinary item"));
        test.assertTrue(!WandRenderIdentity.matches(ordinary, renamed) && WandRenderIdentity.matches(ordinary, ordinary.copy()),
                "ordinary items retain exactly vanilla equip matching");
        test.assertTrue(!WandRenderIdentity.matches(resting, ordinary)
                        && !WandRenderIdentity.matches(resting, ItemStack.EMPTY),
                "selecting a different item or an empty hand keeps the native unequip transition");
        test.succeed();
    }
}
