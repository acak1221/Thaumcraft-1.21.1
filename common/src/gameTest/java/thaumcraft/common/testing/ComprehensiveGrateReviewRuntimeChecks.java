package thaumcraft.common.testing;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import thaumcraft.common.blocks.BlockItemGrate;
import thaumcraft.common.config.ThaumcraftGrate;
import thaumcraft.common.entities.EntityItemGrate;
import thaumcraft.common.lib.GrateRules;
import thaumcraft.common.lib.utils.DeferredItemDrops;

/** Actual native hopper debit, refused entity admission and recovery of the original grate item type. */
public final class ComprehensiveGrateReviewRuntimeChecks {
    private static final String QUEUE_ID = "thaumcraft_pending_inventory_drops";
    private static final SavedData.Factory<DeferredItemDrops> QUEUE = new SavedData.Factory<>(DeferredItemDrops::new, DeferredItemDrops::load, null);
    private ComprehensiveGrateReviewRuntimeChecks() {}

    public static void nativeHopperRetainsCanceledGrateOutput(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 230, 3);
        String marker = "grate-hopper-review-" + UUID.randomUUID();
        var open = lane.origin;
        var closed = open.east(3);
        lane.put(open, ThaumcraftGrate.ITEM_GRATE.get().defaultBlockState());
        lane.put(closed, ThaumcraftGrate.ITEM_GRATE.get().defaultBlockState().setValue(BlockItemGrate.CLOSED, true));
        lane.put(open.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        lane.put(closed.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        var source = (HopperBlockEntity) lane.level.getBlockEntity(open.above());
        var refused = (HopperBlockEntity) lane.level.getBlockEntity(closed.above());
        ItemStack stock = new ItemStack(Items.DIAMOND);
        stock.set(DataComponents.CUSTOM_NAME, Component.literal(marker));
        source.setItem(0, stock.copy()); refused.setItem(0, stock.copy());
        List<ItemEntity> attempts = new ArrayList<>();
        boolean[] paidBeforeAdmission = {false};
        EntityEvent.Add observer = (entity, world) -> {
            if (world == lane.level && entity instanceof ItemEntity item && marked(item.getItem(), marker)) {
                attempts.add(item);
                paidBeforeAdmission[0] = source.isEmpty();
                return EventResult.interruptFalse();
            }
            return EventResult.pass();
        };
        EntityEvent.ADD.register(observer);
        try {
            HopperBlockEntity.pushItemsTick(lane.level, closed.above(), lane.level.getBlockState(closed.above()), refused);
            test.assertTrue(refused.getItem(0).getCount() == 1 && attempts.isEmpty(), "a closed grate leaves the real hopper stock untouched");
            HopperBlockEntity.pushItemsTick(lane.level, open.above(), lane.level.getBlockState(open.above()), source);
            DeferredItemDrops.tick(lane.level);
            test.assertTrue(source.isEmpty(), "the ordinary native hopper has actually paid its one input");
            var queue = lane.level.getDataStorage().get(QUEUE, QUEUE_ID);
            test.assertTrue(queue != null && attempts.size() == 1 && paidBeforeAdmission[0] && drops(lane, marker).isEmpty(),
                    "a refused native grate item has one durable output owner after the hopper debit, not a lost input");
            var saved = queue.save(new CompoundTag(), lane.level.registryAccess());
            List<CompoundTag> owned = new ArrayList<>();
            for (Tag raw : saved.getList("Pending", Tag.TAG_COMPOUND)) {
                var row = (CompoundTag) raw;
                var stack = ItemStack.parse(lane.level.registryAccess(), row.getCompound("Stack")).orElse(ItemStack.EMPTY);
                if (marked(stack, marker)) owned.add(row);
            }
            test.assertTrue(owned.size() == 1 && owned.getFirst().getInt("Count") == 1,
                    "the exact one paid item and its custom components are preserved by the actual SavedData codec");
            var restored = DeferredItemDrops.load(saved, lane.level.registryAccess());
            EntityEvent.ADD.unregister(observer);
            lane.level.getDataStorage().set(QUEUE_ID, restored);
            restored.flush(lane.level);
            var drops = drops(lane, marker);
            test.assertTrue(drops.size() == 1 && drops.getFirst() instanceof EntityItemGrate
                            && ItemStack.matches(drops.getFirst().getItem(), stock)
                            && drops.getFirst().getDeltaMovement().x == 0 && drops.getFirst().getDeltaMovement().z == 0
                            && drops.getFirst().getDeltaMovement().y == GrateRules.FALL_SPEED,
                    "one recovery retry emits the original EntityItemGrate with exact stock and straight downward motion");
            test.assertTrue(drops.getFirst().getY() == open.getY() + GrateRules.SPAWN_HEIGHT && source.isEmpty(),
                    "native ejection height is retained, and a recovered output never refunds or recharges the source hopper");
            test.succeed();
        } finally {
            EntityEvent.ADD.unregister(observer);
            source.clearContent(); refused.clearContent();
            drops(lane, marker).forEach(ItemEntity::discard);
            lane.clean();
        }
    }

    private static boolean marked(ItemStack stack, String marker) {
        var name = stack.get(DataComponents.CUSTOM_NAME);
        return name != null && name.getString().equals(marker);
    }
    private static List<ItemEntity> drops(WaveLane lane, String marker) {
        return lane.level.getEntitiesOfClass(ItemEntity.class, lane.area(4), item -> marked(item.getItem(), marker));
    }
}
