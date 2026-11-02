package thaumcraft.common.testing;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.entities.golems.EntityTravelingTrunk;
import thaumcraft.common.items.golems.TrunkRules;

/** Native item use and the actual loader ADD callback, with explicitly supplied survival stock. */
public final class TrunkSpawnerReviewRuntimeChecks {
    private TrunkSpawnerReviewRuntimeChecks() {}

    public static void reentrantPlacementKeepsOnePayloadOwner(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            BlockPos floor = f.base;
            var forced = force(test, floor);
            var admitted = new ArrayList<EntityTravelingTrunk>();
            boolean[] reentered = {false};
            InteractionResult[] nested = {null};
            var stack = carryingTrunk();
            var hit = hit(floor);
            EntityEvent.Add observer = (entity, world) -> {
                if (world == f.level && entity instanceof EntityTravelingTrunk trunk && trunk.isOwner(f.player)) {
                    admitted.add(trunk);
                    if (!reentered[0]) {
                        reentered[0] = true;
                        nested[0] = f.player.gameMode.useItemOn(f.player, f.level, f.player.getMainHandItem(),
                                InteractionHand.MAIN_HAND, hit);
                    }
                }
                return EventResult.pass();
            };
            try {
                f.put(floor, Blocks.STONE); f.aim(Vec3.atCenterOf(floor));
                f.player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                EntityEvent.ADD.register(observer);
                var result = f.player.gameMode.useItemOn(f.player, f.level, stack, InteractionHand.MAIN_HAND, hit);
                EntityEvent.ADD.unregister(observer);
                test.assertTrue(reentered[0], "the actual native trunk ADD callback attempted the nested placement");
                test.assertTrue(result.consumesAction() && stack.isEmpty(), "one real survival placer is paid");
                int diamonds = admitted.stream().filter(Entity::isAlive).flatMap(trunk -> trunk.inventory.contents().stream())
                        .filter(item -> item.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum();
                test.assertTrue(admitted.size() == 1 && diamonds == 7,
                        "one survival placer has exactly one native trunk/payload owner despite reentry: trunks="
                                + admitted.size() + ", diamonds=" + diamonds + ", nested=" + nested[0]);
                test.assertTrue(!nested[0].consumesAction(), "a nested use cannot claim a second successful placement");
            } finally {
                EntityEvent.ADD.unregister(observer); admitted.forEach(Entity::discard); unforce(f, forced);
            }
        }
        test.succeed();
    }

    public static void cancellationAndCreativePreserveOriginalPlacementRules(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            BlockPos floor = f.base;
            var forced = force(test, floor);
            var attempts = new ArrayList<EntityTravelingTrunk>();
            EntityEvent.Add cancel = (entity, world) -> {
                if (world == f.level && entity instanceof EntityTravelingTrunk trunk && trunk.isOwner(f.player)) {
                    attempts.add(trunk); return EventResult.interruptFalse();
                }
                return EventResult.pass();
            };
            try {
                f.put(floor, Blocks.STONE); f.aim(Vec3.atCenterOf(floor));
                var stack = carryingTrunk(); var before = stack.copy();
                f.player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                EntityEvent.ADD.register(cancel);
                var canceled = f.player.gameMode.useItemOn(f.player, f.level, stack, InteractionHand.MAIN_HAND, hit(floor));
                EntityEvent.ADD.unregister(cancel);
                test.assertTrue(!canceled.consumesAction() && ItemStack.matches(stack, before) && attempts.size() == 1
                                && f.level.getEntity(attempts.getFirst().getUUID()) == null,
                        "canceled native ADD keeps exact source contents and leaves no native trunk");
                f.mode(GameType.CREATIVE);
                var accepted = f.player.gameMode.useItemOn(f.player, f.level, stack, InteractionHand.MAIN_HAND, hit(floor));
                var trunks = f.level.getEntitiesOfClass(EntityTravelingTrunk.class, f.bounds, trunk -> trunk.isOwner(f.player));
                attempts.addAll(trunks);
                test.assertTrue(accepted.consumesAction() && ItemStack.matches(stack, before) && trunks.size() == 1
                                && trunks.getFirst().getUpgrade() == TrunkRules.ORDER
                                && trunks.getFirst().inventory.getItem(0).is(Items.DIAMOND)
                                && trunks.getFirst().inventory.getItem(0).getCount() == 7,
                        "guard unwinds after cancellation; creative keeps original placer and native payload/upgrade");
            } finally {
                EntityEvent.ADD.unregister(cancel); attempts.forEach(Entity::discard); unforce(f, forced);
            }
        }
        test.succeed();
    }

    private static ItemStack carryingTrunk() {
        var stack = new ItemStack(ThaumcraftGolems.TRUNK_SPAWNER.get());
        stack.set(ThaumcraftGolems.TRUNK_UPGRADE.get(), TrunkRules.ORDER);
        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND, 7))));
        return stack;
    }

    private static BlockHitResult hit(BlockPos floor) {
        return new BlockHitResult(Vec3.atCenterOf(floor).add(0, .5, 0), Direction.UP, floor, false);
    }

    private static List<ChunkPos> force(GameTestHelper test, BlockPos floor) {
        var level = test.getLevel(); var owned = new ArrayList<ChunkPos>();
        for (BlockPos pos : List.of(floor, floor.south(2))) {
            var chunk = new ChunkPos(pos);
            if (!level.getForcedChunks().contains(chunk.toLong())) {
                level.setChunkForced(chunk.x, chunk.z, true); owned.add(chunk);
            }
            GameTestChunkAwait.entityTicking(test, pos);
        }
        return owned;
    }

    private static void unforce(TubeVariantsRuntimeChecks.Fixture f, List<ChunkPos> owned) {
        for (var chunk : owned) f.level.setChunkForced(chunk.x, chunk.z, false);
    }
}
