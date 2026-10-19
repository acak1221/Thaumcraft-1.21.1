package thaumcraft.common.testing;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.entities.golems.GolemFishingSmelting;
import thaumcraft.common.lib.utils.DeferredItemDrops;

/**
 * The flame of a Fire golem's catch (original AIFish:127-129, {@code entityitem.setFire(2)}), through the durable drop queue that
 * delivers every fishing result: a catch queued as burning is delivered burning, one queued plain is not, the flame lasts two seconds
 * and does not destroy the fish; the flame is saved with the paid row and read back, and a row that carries a flame it could never have
 * (not a fishing catch, a zero, an oversized or mistyped flame) is quarantined instead of delivered. The golem's own decision (Fire
 * upgrade means two seconds) is checked on real golems in the smelting check, and what real lines bring in in the live fishing check.
 * NOT a client check (the flame is unseen) and not a world-save round trip beyond the queue's own NBT.
 */
public final class GolemFishingBurnRuntimeChecks {
    private GolemFishingBurnRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        BlockPos base = test.absolutePos(new BlockPos(3, 3, 3));
        AABB watch = new AABB(base).inflate(3, 6, 3);
        Set<UUID> existing = new HashSet<>();
        level.getAllEntities().forEach(entity -> existing.add(entity.getUUID()));
        BlockPos floor = base.below();
        var saved = new java.util.LinkedHashMap<BlockPos, net.minecraft.world.level.block.state.BlockState>();
        for (BlockPos pos : BlockPos.betweenClosed(floor.offset(-1, 0, -1), floor.offset(1, 0, 1))) {
            saved.put(pos.immutable(), level.getBlockState(pos));
            level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
        }
        try {
            persistence(test, level, base);
            test.assertTrue(rejects(level, base, -1) && rejects(level, base, 601), "a burning time below zero or above the cap is refused at the door");
            DeferredItemDrops.enqueueGolemFishingCatch(level, base.below(), new Vec3(0, .2, 0), new ItemStack(Items.COOKED_COD), GolemFishingSmelting.BURNING_TICKS);
            DeferredItemDrops.enqueueGolemFishingCatch(level, base.below(), new Vec3(0, .2, 0), new ItemStack(Items.COD), 0);
            DeferredItemDrops.tick(level);
            List<ItemEntity> fresh = level.getEntitiesOfClass(ItemEntity.class, watch, item -> !existing.contains(item.getUUID()));
            test.assertTrue(fresh.size() == 2, "both catches were delivered: " + fresh);
            ItemEntity burning = fresh.stream().filter(item -> item.getItem().is(Items.COOKED_COD)).findFirst().orElseThrow();
            ItemEntity plain = fresh.stream().filter(item -> item.getItem().is(Items.COD)).findFirst().orElseThrow();
            int flame = burning.getRemainingFireTicks();
            test.assertTrue(GolemFishingSmelting.BURNING_TICKS == 40 && flame > 0 && flame <= 40 && burning.isOnFire() && plain.getRemainingFireTicks() <= 0 && !plain.isOnFire(),
                    "the Fire catch is delivered alight (" + flame + " ticks left), the plain one is not (" + plain.getRemainingFireTicks() + ")");
            test.runAfterDelay(60, () -> {
                try {
                    test.assertTrue(burning.isAlive() && burning.getItem().is(Items.COOKED_COD) && burning.getItem().getCount() == 1,
                            "two seconds of flame cost the fish nothing: it is still there");
                    test.assertTrue(burning.getRemainingFireTicks() <= 0 && !burning.isOnFire(), "and the flame is out after two seconds (" + burning.getRemainingFireTicks() + ")");
                    test.assertTrue(plain.isAlive() && plain.getItem().is(Items.COD), "the plain catch was never touched");
                    test.succeed();
                } finally {
                    fresh.forEach(Entity::discard);
                    saved.forEach((pos, state) -> level.setBlock(pos, state, 2));
                }
            });
        } catch (RuntimeException failure) {
            level.getEntitiesOfClass(ItemEntity.class, watch, item -> !existing.contains(item.getUUID())).forEach(Entity::discard);
            saved.forEach((pos, state) -> level.setBlock(pos, state, 2));
            throw failure;
        }
    }

    private static boolean rejects(ServerLevel level, BlockPos base, int fire) {
        try {
            DeferredItemDrops.enqueueGolemFishingCatch(level, base.below(), Vec3.ZERO, new ItemStack(Items.COD), fire);
            return false;
        } catch (IllegalArgumentException expected) { return true; }
    }

    /** The queue's own NBT: a paid burning row is read back and written again with its flame; impossible flames are quarantined. */
    private static void persistence(GameTestHelper test, ServerLevel level, BlockPos base) {
        var registries = level.registryAccess();
        UUID good = UUID.randomUUID();
        CompoundTag tag = new CompoundTag();
        tag.putInt("Version", 3);
        ListTag rows = new ListTag();
        rows.add(row(registries, good, 20, -1, "Fire", 40));
        UUID quiet = UUID.randomUUID();
        rows.add(row(registries, quiet, 20, -1, null, 0));
        tag.put("Pending", rows);
        tag.put("Settled", new ListTag());
        var loaded = DeferredItemDrops.load(tag, registries);
        var written = loaded.save(new CompoundTag(), registries);
        ListTag pending = written.getList("Pending", Tag.TAG_COMPOUND);
        int flames = 0, plain = 0;
        for (Tag raw : pending) {
            CompoundTag row = (CompoundTag) raw;
            if (row.getUUID("Id").equals(good) && row.getInt("Fire") == 40) flames++;
            if (row.getUUID("Id").equals(quiet) && !row.contains("Fire")) plain++;
        }
        test.assertTrue(pending.size() == 2 && flames == 1 && plain == 1 && written.getList("Quarantined", Tag.TAG_COMPOUND).isEmpty(),
                "a saved burning row keeps its flame through a load and a save, a plain one stays plain: " + pending);

        // Flames that no Fire golem could have queued.
        List<CompoundTag> impossible = List.of(
                row(registries, UUID.randomUUID(), 10, -1, "Fire", 40),   // not a fishing catch (the pickup delay of a golem throw)
                row(registries, UUID.randomUUID(), 20, -1, "Fire", 0),    // a zero flame is never written
                row(registries, UUID.randomUUID(), 20, -1, "Fire", 601),  // above the cap
                row(registries, UUID.randomUUID(), 20, 3, "Fire", 40));   // on a crystal row
        for (CompoundTag bad : impossible) {
            CompoundTag one = new CompoundTag();
            one.putInt("Version", 3);
            ListTag only = new ListTag();
            only.add(bad);
            one.put("Pending", only);
            one.put("Settled", new ListTag());
            var data = DeferredItemDrops.load(one, registries).save(new CompoundTag(), registries);
            test.assertTrue(data.getList("Pending", Tag.TAG_COMPOUND).isEmpty() && data.getList("Quarantined", Tag.TAG_COMPOUND).size() == 1,
                    "an impossible flame is quarantined, not delivered: " + bad);
        }
        CompoundTag mistyped = row(registries, UUID.randomUUID(), 20, -1, null, 0);
        mistyped.putString("Fire", "40");
        CompoundTag one = new CompoundTag();
        one.putInt("Version", 3);
        ListTag only = new ListTag();
        only.add(mistyped);
        one.put("Pending", only);
        one.put("Settled", new ListTag());
        var data = DeferredItemDrops.load(one, registries).save(new CompoundTag(), registries);
        test.assertTrue(data.getList("Pending", Tag.TAG_COMPOUND).isEmpty() && data.getList("Quarantined", Tag.TAG_COMPOUND).size() == 1,
                "a flame of the wrong NBT type is quarantined too");
    }

    private static CompoundTag row(net.minecraft.core.HolderLookup.Provider registries, UUID id, int pickupDelay, int crystalFace, String key, int fire) {
        CompoundTag row = new CompoundTag();
        row.putUUID("Id", id);
        row.putLong("Source", BlockPos.ZERO.asLong());
        row.put("Stack", new ItemStack(Items.COD).save(registries));
        row.putInt("Count", 1);
        row.putInt("PickupDelay", pickupDelay);
        row.putDouble("X", .5); row.putDouble("Y", 1); row.putDouble("Z", .5);
        row.putDouble("DX", 0); row.putDouble("DY", .2); row.putDouble("DZ", 0);
        if (crystalFace >= 0) row.putInt("CrystalFace", crystalFace);
        if (key != null) row.putInt(key, fire);
        return row;
    }
}
