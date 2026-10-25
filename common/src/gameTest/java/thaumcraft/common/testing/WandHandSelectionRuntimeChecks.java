package thaumcraft.common.testing;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.items.wands.WandHands;

/** Real native hand inventories use the same selector as focus input and the casting dial. */
public final class WandHandSelectionRuntimeChecks {
    private WandHandSelectionRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        try (var fixture = new TubeVariantsRuntimeChecks.Fixture(test)) {
            fixture.open();
            var player = fixture.player;
            test.assertTrue(!player.isSpectator() && !player.isCreative(), "real connected survival actor exercises native hand selection");
            ItemStack wand = new ItemStack(ThaumcraftContent.WAND.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, wand);
            test.assertTrue(WandHands.select(player) == InteractionHand.OFF_HAND, "offhand-only wand supplies both input and HUD");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
            test.assertTrue(WandHands.select(player) == InteractionHand.OFF_HAND, "ordinary main-hand item does not hide the offhand dial");
            player.setItemInHand(InteractionHand.MAIN_HAND, wand.copy());
            test.assertTrue(WandHands.select(player) == InteractionHand.MAIN_HAND, "main-hand wand wins consistently");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            test.assertTrue(WandHands.select(player) == null, "no wand has no dial or focus input");
            player.setItemInHand(InteractionHand.OFF_HAND, wand);
            player.setGameMode(GameType.SPECTATOR);
            test.assertTrue(player.isSpectator(), "the actual native actor enters spectator mode before selection");
            test.assertTrue(WandHands.select(player) == null, "spectator has no active casting dial");
        }
        test.succeed();
    }
}
