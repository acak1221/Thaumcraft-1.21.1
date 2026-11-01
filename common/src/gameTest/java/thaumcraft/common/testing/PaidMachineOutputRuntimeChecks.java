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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.config.ThaumcraftLegacyTiles;
import thaumcraft.common.entities.golems.EntityTravelingTrunk;
import thaumcraft.common.items.golems.TrunkRules;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.lib.utils.ExperienceOrbQueue;
import thaumcraft.common.mixin.ExperienceOrbCountAccessor;
import thaumcraft.common.tiles.TileArcaneFurnace;

/** Paid completion/retirement through native registered sources and real ADD
 * cancellation, recursive hooks and SavedData replacement. Progress=1, explicit
 * bonus lookup and mock owner are fixtures; publication is production code. */
public final class PaidMachineOutputRuntimeChecks {
    private static final String ITEMS = "thaumcraft_pending_inventory_drops";
    private static final SavedData.Factory<DeferredItemDrops> FACTORY = new SavedData.Factory<>(DeferredItemDrops::new, DeferredItemDrops::load, null);
    private PaidMachineOutputRuntimeChecks() {}
    private static DeferredItemDrops queue(ServerLevel level) { return level.getDataStorage().get(FACTORY, ITEMS); }
    private static CompoundTag savedItems(ServerLevel level) { return queue(level).save(new CompoundTag(), level.registryAccess()); }
    private static boolean owns(CompoundTag saved, UUID id) {
        return saved.getList("Pending", Tag.TAG_COMPOUND).stream().anyMatch(raw -> ((CompoundTag) raw).getUUID("Id").equals(id));
    }
    private static void reloadItems(ServerLevel level, CompoundTag saved) { level.getDataStorage().set(ITEMS, DeferredItemDrops.load(saved, level.registryAccess())); }
    private static void reloadExperience(ServerLevel level, CompoundTag saved) { level.getDataStorage().set(ExperienceOrbQueue.ID, ExperienceOrbQueue.load(saved, level.registryAccess())); }
    private static TileArcaneFurnace furnace(WaveLane lane, int count) {
        lane.put(lane.origin, ThaumcraftLegacyTiles.ARCANE_FURNACE.get().defaultBlockState());
        var furnace = (TileArcaneFurnace) lane.level.getBlockEntity(lane.origin);
        furnace.setInventorySlotContents(0, new ItemStack(Items.CACTUS, count));
        furnace.facingX = furnace.facingZ = 0;
        furnace.furnaceCookTime = furnace.furnaceMaxCookTime = 1;
        furnace.speedyTime = 2;
        return furnace;
    }
    private static EntityTravelingTrunk trunk(WaveLane lane, int upgrade, net.minecraft.server.level.ServerPlayer owner, double dx) {
        var trunk = lane.add(ThaumcraftGolems.TRUNK.get().create(lane.level));
        trunk.setPos(lane.origin.getX() + .5 + dx, lane.origin.getY(), lane.origin.getZ() + .5);
        trunk.setOwner(owner); trunk.setUpgrade(upgrade);
        if (!lane.level.addFreshEntity(trunk)) throw new IllegalStateException("Native trunk fixture refused");
        return trunk;
    }
    private static void finish(WaveLane lane, TileArcaneFurnace furnace, List<? extends Entity> candidates) {
        candidates.forEach(Entity::discard);
        lane.level.getEntitiesOfClass(ExperienceOrb.class, lane.area(5)).forEach(Entity::discard);
        if (furnace != null) { furnace.markBlazeReleased(); furnace.setInventorySlotContents(0, ItemStack.EMPTY); }
        lane.clean();
    }

    public static void furnaceCanceledCompletionRetainsResultBonusAndExperience(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 150, 3);
        var furnace = furnace(lane, 2);
        var before = furnace.saveWithoutMetadata(lane.level.registryAccess());
        var bonus = new ItemStack(Items.DIAMOND, 9);
        bonus.set(DataComponents.CUSTOM_NAME, Component.literal("paid-component-bonus"));
        List<Entity> attempts = new ArrayList<>();
        int[] recursive = {0}, rewindBlocked = {0};
        boolean[] cancel = {true};
        EntityEvent.Add hook = (entity, level) -> {
            if (level != lane.level || !lane.area(4).contains(entity.position()) || !(entity instanceof ItemEntity || entity instanceof ExperienceOrb)) return EventResult.pass();
            attempts.add(entity);
            if (!cancel[0]) return EventResult.pass();
            TileArcaneFurnace.tick(lane.level, lane.origin, furnace.getBlockState(), furnace); recursive[0]++;
            try { furnace.loadWithComponents(before, lane.level.registryAccess()); }
            catch (IllegalStateException expected) { rewindBlocked[0]++; }
            return EventResult.interruptFalse();
        };
        try {
            TileArcaneFurnace.setSmeltingBonus(input -> input.is(Items.CACTUS) ? bonus.copy() : null);
            long seed = 0;
            while (true) {
                var random = RandomSource.create(seed); random.nextDouble(); random.nextDouble();
                if (random.nextFloat() < .25F) break;
                seed++;
            }
            lane.level.random.setSeed(seed);
            EntityEvent.ADD.register(hook);
            TileArcaneFurnace.tick(lane.level, lane.origin, furnace.getBlockState(), furnace);
            test.assertTrue(attempts.size() == 3 && recursive[0] == 3 && rewindBlocked[0] == 3 && furnace.getStackInSlot(0).getCount() == 1,
                    "one native completion pays once before all three canceled result/bonus/XP callbacks; recursive tick and stale source load cannot pay again");
            var items = savedItems(lane.level); var experience = ExperienceOrbQueue.snapshot(lane.level);
            test.assertTrue(attempts.stream().allMatch(entity -> owns(entity instanceof ExperienceOrb ? experience : items, entity.getUUID()))
                            && lane.level.getEntitiesOfClass(ItemEntity.class, lane.area(4)).isEmpty()
                            && lane.level.getEntitiesOfClass(ExperienceOrb.class, lane.area(4)).isEmpty(),
                    "each refused entity has exactly its original stable durable owner and no physical owner");
            var paidSource = furnace.saveWithoutMetadata(lane.level.registryAccess());
            reloadItems(lane.level, items); reloadExperience(lane.level, experience);
            furnace.loadWithComponents(paidSource, lane.level.registryAccess());
            cancel[0] = false;
            DeferredItemDrops.tick(lane.level); ExperienceOrbQueue.tick(lane.level);
            var dyes = lane.level.getEntitiesOfClass(ItemEntity.class, lane.area(4), item -> item.getItem().is(Items.GREEN_DYE));
            var bonuses = lane.level.getEntitiesOfClass(ItemEntity.class, lane.area(4), item -> item.getItem().is(Items.DIAMOND));
            int xp = lane.level.getEntitiesOfClass(ExperienceOrb.class, lane.area(4)).stream()
                    .mapToInt(orb -> orb.getValue() * ((ExperienceOrbCountAccessor) orb).thaumcraft$getOrbCount()).sum();
            test.assertTrue(dyes.size() == 1 && dyes.getFirst().getItem().getCount() == 1 && bonuses.size() == 1
                            && ItemStack.matches(bonuses.getFirst().getItem(), bonus.copyWithCount(1)) && xp == 1
                            && furnace.getStackInSlot(0).getCount() == 1,
                    "save/reload retries the exact component-bearing bonus, one dye and input-recipe XP without spending another cactus");
            DeferredItemDrops.tick(lane.level); ExperienceOrbQueue.tick(lane.level);
            test.assertTrue(attempts.size() == 6, "each stable paid row is emitted only once after its canceled attempt");
            test.succeed();
        } finally {
            EntityEvent.ADD.unregister(hook); TileArcaneFurnace.setSmeltingBonus(ThaumcraftApi::getSmeltingBonus);
            finish(lane, furnace, attempts);
        }
    }

    public static void paidQueuesSaveReceiptsBeforeReplacementTick(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 180, 3);
        var furnace = furnace(lane, 1);
        List<Entity> consumed = new ArrayList<>();
        List<CompoundTag> checkpoints = new ArrayList<>();
        EntityEvent.Add hook = (entity, level) -> {
            if (level != lane.level || !lane.area(4).contains(entity.position()) || !(entity instanceof ItemEntity || entity instanceof ExperienceOrb)) return EventResult.pass();
            var stale = entity instanceof ExperienceOrb ? ExperienceOrbQueue.snapshot(lane.level) : savedItems(lane.level);
            consumed.add(entity);
            // Real deliberate native destruction transfers the paid row. Then
            // an addon installs and saves its earlier pending snapshot BEFORE
            // any tick has a chance to reconcile the replacement queue.
            entity.discard();
            if (entity instanceof ExperienceOrb) {
                reloadExperience(lane.level, stale);
                var checkpoint = ExperienceOrbQueue.snapshot(lane.level); checkpoints.add(checkpoint);
                reloadExperience(lane.level, checkpoint);
            } else {
                reloadItems(lane.level, stale);
                var checkpoint = savedItems(lane.level); checkpoints.add(checkpoint);
                reloadItems(lane.level, checkpoint);
            }
            throw new AssertionError("declared addon Error after native paid destruction and checkpoint");
        };
        try {
            TileArcaneFurnace.setSmeltingBonus(input -> null);
            EntityEvent.ADD.register(hook);
            TileArcaneFurnace.tick(lane.level, lane.origin, furnace.getBlockState(), furnace);
            EntityEvent.ADD.unregister(hook);
            test.assertTrue(consumed.size() == 2 && checkpoints.size() == 2 && furnace.getStackInSlot(0).isEmpty(),
                    "the real item and XP ADD hooks consumed two paid rows; post-publication Error cannot restore their source");
            test.assertTrue(!owns(checkpoints.get(0), consumed.get(0).getUUID()) && !owns(checkpoints.get(1), consumed.get(1).getUUID()),
                    "a canonical replacement saved before its first tick folds the native live receipt into its checkpoint");
            DeferredItemDrops.tick(lane.level); ExperienceOrbQueue.tick(lane.level);
            reloadItems(lane.level, savedItems(lane.level)); reloadExperience(lane.level, ExperienceOrbQueue.snapshot(lane.level));
            DeferredItemDrops.tick(lane.level); ExperienceOrbQueue.tick(lane.level);
            test.assertTrue(lane.level.getEntitiesOfClass(ItemEntity.class, lane.area(4)).isEmpty()
                            && lane.level.getEntitiesOfClass(ExperienceOrb.class, lane.area(4)).isEmpty(),
                    "replacement checkpoint reload never recreates already destroyed result or XP");
            test.succeed();
        } finally {
            EntityEvent.ADD.unregister(hook); TileArcaneFurnace.setSmeltingBonus(ThaumcraftApi::getSmeltingBonus);
            finish(lane, furnace, consumed);
        }
    }

    public static void orderRecallRetriesAndPreservesSlotIndices(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 210, 3);
        var owner = test.makeMockServerPlayerInLevel();
        var trunk = trunk(lane, TrunkRules.ORDER, owner, 0);
        trunk.setCustomName(Component.literal("order-hole-26"));
        var stock = new ItemStack(Items.DIAMOND, 5); stock.set(DataComponents.CUSTOM_NAME, Component.literal("last-slot-stock"));
        trunk.inventory.setItem(26, stock.copy());
        var before = new CompoundTag(); trunk.addAdditionalSaveData(before);
        List<Entity> attempts = new ArrayList<>();
        int[] recursiveRecall = {0}, rewindBlocked = {0};
        boolean[] cancel = {true};
        EntityEvent.Add hook = (entity, level) -> {
            if (level != lane.level || !(entity instanceof ItemEntity item) || !item.getItem().is(ThaumcraftGolems.TRUNK_SPAWNER.get()) || !lane.area(4).contains(item.position())) return EventResult.pass();
            attempts.add(entity);
            if (!cancel[0]) return EventResult.pass();
            if (trunk.recall(owner, false)) recursiveRecall[0]++;
            trunk.inventory.dropAll(); trunk.die(trunk.damageSources().generic());
            try { trunk.readAdditionalSaveData(before); } catch (IllegalStateException expected) { rewindBlocked[0]++; }
            return EventResult.interruptFalse();
        };
        try {
            EntityEvent.ADD.register(hook);
            test.assertTrue(trunk.recall(owner, false), "the native order trunk commits its recall");
            test.assertTrue(trunk.isRemoved() && trunk.inventory.isEmpty() && attempts.size() == 1 && recursiveRecall[0] == 0 && rewindBlocked[0] == 1,
                    "retirement and empty source slots precede the canceled ADD; callback recall/death/drop/reload cannot replay them");
            var saved = savedItems(lane.level);
            test.assertTrue(owns(saved, attempts.getFirst().getUUID()), "the refused configured spawner retains durable custody of all its contents");
            reloadItems(lane.level, saved); cancel[0] = false; DeferredItemDrops.tick(lane.level);
            var drops = lane.level.getEntitiesOfClass(ItemEntity.class, lane.area(4), item -> item.getItem().is(ThaumcraftGolems.TRUNK_SPAWNER.get()));
            test.assertTrue(drops.size() == 1 && attempts.size() == 2, "one stable configured spawner is retried after save/load");
            ItemStack spawner = drops.getFirst().getItem();
            var slots = spawner.get(DataComponents.CONTAINER).stream().toList();
            test.assertTrue(slots.size() == 27 && slots.get(0).isEmpty() && ItemStack.matches(slots.get(26), stock)
                            && spawner.getHoverName().getString().equals("order-hole-26"),
                    "the Order component keeps the empty leading slots, slot26 stock and name");
            // Declared transfer into the mock owner's hand, followed by the real
            // native item use to test the inverse slot restoration as well.
            owner.setItemInHand(InteractionHand.MAIN_HAND, spawner.copy()); drops.getFirst().discard();
            lane.put(lane.origin.below(), Blocks.STONE.defaultBlockState());
            var context = new UseOnContext(owner, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(lane.origin.below()), Direction.UP, lane.origin.below(), false));
            test.assertTrue(owner.getMainHandItem().getItem().useOn(context).consumesAction(), "the configured spawner uses the native placement path");
            var placed = lane.level.getEntitiesOfClass(EntityTravelingTrunk.class, lane.area(4));
            test.assertTrue(placed.size() == 1 && placed.getFirst().inventory.getItem(0).isEmpty()
                            && ItemStack.matches(placed.getFirst().inventory.getItem(26), stock),
                    "recall, queue save/load and actual placement preserve slot26 rather than packing stock into slot0");
            DeferredItemDrops.tick(lane.level);
            test.assertTrue(attempts.size() == 2, "retired trunk and settled queue have no second output right");
            test.succeed();
        } finally { EntityEvent.ADD.unregister(hook); finish(lane, null, attempts); }
    }

    public static void trunkDeathAndInventoryDropsRetainCanceledStock(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 240, 3);
        var owner = test.makeMockServerPlayerInLevel();
        var dropped = trunk(lane, TrunkRules.NO_UPGRADE, owner, -1);
        var dying = trunk(lane, TrunkRules.NO_UPGRADE, owner, 1);
        dropped.inventory.setItem(4, new ItemStack(Items.DIAMOND, 5));
        dying.inventory.setItem(26, new ItemStack(Items.DIAMOND, 7));
        List<Entity> attempts = new ArrayList<>();
        boolean[] cancel = {true}; int[] recursiveRecall = {0};
        EntityEvent.Add hook = (entity, level) -> {
            if (level != lane.level || !(entity instanceof ItemEntity item) || !item.getItem().is(Items.DIAMOND) || !lane.area(4).contains(item.position())) return EventResult.pass();
            attempts.add(entity);
            if (!cancel[0]) return EventResult.pass();
            var producer = item.getX() < lane.origin.getX() + .5 ? dropped : dying;
            producer.inventory.dropAll();
            if (producer.recall(owner, false)) recursiveRecall[0]++;
            producer.die(producer.damageSources().generic());
            return EventResult.interruptFalse();
        };
        try {
            EntityEvent.ADD.register(hook);
            // Keep callbacks scoped to the current producer: this explicit
            // direct drop precedes death, whose stock remains native until paid.
            dropped.inventory.dropAll();
            dying.setHealth(0); dying.die(dying.damageSources().generic());
            test.assertTrue(dropped.inventory.isEmpty() && dying.inventory.isEmpty() && attempts.size() == 2 && recursiveRecall[0] == 0,
                    "direct inventory evacuation and native death each pay their original stock once despite canceled recursive drop/death hooks");
            test.assertTrue(dropped.isAlive() && !dying.isAlive(), "inventory evacuation leaves its trunk alive while death remains terminal");
            var saved = savedItems(lane.level);
            test.assertTrue(attempts.stream().allMatch(entity -> owns(saved, entity.getUUID())), "both canceled stocks remain owned by SavedData");
            reloadItems(lane.level, saved); cancel[0] = false; DeferredItemDrops.tick(lane.level);
            int total = lane.level.getEntitiesOfClass(ItemEntity.class, lane.area(4), item -> item.getItem().is(Items.DIAMOND)).stream().mapToInt(item -> item.getItem().getCount()).sum();
            test.assertTrue(total == 12 && attempts.size() == 4, "save/reload delivers all twelve original diamonds exactly once");
            test.succeed();
        } finally { EntityEvent.ADD.unregister(hook); finish(lane, null, attempts); }
    }
}
