package thaumcraft.common.testing;

import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.items.equipment.ElementalShovelGeometry;

/** Exact native dispatch results, not merely proof that an effect happened. */
public final class InteractionResultRuntimeChecks {
    private InteractionResultRuntimeChecks() {}

    public static void pickaxeConsumesPaidScan(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            var player = test.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.SURVIVAL);
            player.setPos(Vec3.atCenterOf(lane.origin.south(2)));
            lane.put(lane.origin, Blocks.STONE.defaultBlockState());
            for (var hand : InteractionHand.values()) {
                var pickaxe = new ItemStack(ThaumcraftEquipment.ELEMENTAL_PICKAXE.get());
                player.setItemInHand(hand, pickaxe);
                var hit = new BlockHitResult(Vec3.atCenterOf(lane.origin), Direction.UP, lane.origin, false);
                var result = player.gameMode.useItemOn(player, lane.level, pickaxe, hand, hit);
                test.assertTrue(pickaxe.getDamageValue() == 5, "one scan pays exactly five durability in " + hand);
                test.assertTrue(result == InteractionResult.CONSUME,
                        "paid native scan must stop fallback, not return " + result + " in " + hand);
                test.assertTrue(lane.level.getBlockState(lane.origin).is(Blocks.STONE), "scan never transforms its target");
                player.setItemInHand(hand, ItemStack.EMPTY);
            }
        } finally { lane.clean(); }
        test.succeed();
    }

    public static void shovelConsumesOnlyPaidPlacement(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            var player = test.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.SURVIVAL);
            player.setPos(Vec3.atCenterOf(lane.origin.south(2)));
            lane.put(lane.origin, Blocks.STONE.defaultBlockState());
            var hit = new BlockHitResult(Vec3.atCenterOf(lane.origin), Direction.UP, lane.origin, false);
            for (var hand : InteractionHand.values()) {
                for (var cell : ElementalShovelGeometry.layout(lane.origin, Direction.UP, 0, player.getYRot()))
                    lane.put(cell, Blocks.AIR.defaultBlockState());
                player.getInventory().clearContent();
                var shovel = new ItemStack(ThaumcraftEquipment.ELEMENTAL_SHOVEL.get());
                player.setItemInHand(hand, shovel);
                test.assertTrue(player.gameMode.useItemOn(player, lane.level, shovel, hand, hit) == InteractionResult.PASS
                        && shovel.getDamageValue() == 0, "no stock is a genuine no-op/PASS in " + hand);
                player.getInventory().add(new ItemStack(Items.STONE, 1));
                var result = player.gameMode.useItemOn(player, lane.level, shovel, hand, hit);
                long placed = ElementalShovelGeometry.layout(lane.origin, Direction.UP, 0, player.getYRot()).stream()
                        .filter(cell -> lane.level.getBlockState(cell).is(Blocks.STONE)).count();
                test.assertTrue(placed == 1 && player.getInventory().countItem(Items.STONE) == 0
                        && shovel.getDamageValue() == 1, "one native click places/pays exactly one available block in " + hand);
                test.assertTrue(result == InteractionResult.CONSUME,
                        "paid native placement must stop fallback, not return " + result + " in " + hand);
                test.assertTrue(player.gameMode.useItemOn(player, lane.level, shovel, hand, hit) == InteractionResult.PASS
                        && shovel.getDamageValue() == 1, "exhausted stock remains a no-op/PASS in " + hand);
            }
        } finally { lane.clean(); }
        test.succeed();
    }
}
