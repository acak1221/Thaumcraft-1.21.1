package thaumcraft.common.testing;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.entities.EntityFollowingItem;
import thaumcraft.common.entities.EntityPermanentItem;

/** Native flying loot reaches its actual owner once, then behaves as an ordinary special item. */
public final class FollowingItemReviewRuntimeChecks {
    private FollowingItemReviewRuntimeChecks() {}

    public static void arrivalReleasesSyncedTarget(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            BlockPos pos = f.base.above();
            var chunk = new ChunkPos(pos);
            boolean forced = !f.level.getForcedChunks().contains(chunk.toLong());
            if (forced) f.level.setChunkForced(chunk.x, chunk.z, true);
            var drops = new ArrayList<Entity>();
            try {
                GameTestChunkAwait.entityTicking(test, pos);
                f.player.setPos(Vec3.atBottomCenterOf(pos));
                Vec3 destination = f.player.position().add(0, f.player.getBbHeight() / 2, 0);
                var drop = new EntityFollowingItem(f.level, destination.x, destination.y, destination.z,
                        new ItemStack(Items.DIAMOND), f.player, 10);
                drop.setDeltaMovement(Vec3.ZERO); drop.setPickUpDelay(32767);
                test.assertTrue(f.level.addFreshEntity(drop), "native following item is admitted at the arrival point");
                drops.add(drop); drop.tick();
                test.assertTrue(drop.getTarget() == null && drop.getTargetX() == 0 && drop.getTargetY() == 0 && drop.getTargetZ() == 0,
                        "arrival clears BOTH the private target and its synced identity instead of reacquiring it next tick");
                f.player.setPos(f.player.position().add(3, 0, 0));
                double x = drop.getX(); drop.tick();
                test.assertTrue(drop.getTarget() == null && Math.abs(drop.getX() - x) < 1e-6 && drop.getDeltaMovement().y < 0,
                        "arrived loot falls in place even when its original player moves away");

                var traveling = new EntityFollowingItem(f.level, destination.x - 3, destination.y, destination.z,
                        new ItemStack(Items.DIAMOND), f.player, 3);
                test.assertTrue(f.level.addFreshEntity(traveling), "second native control item is admitted far from its target");
                drops.add(traveling); traveling.tick();
                test.assertTrue(traveling.getTarget() == f.player && traveling.getDeltaMovement().x > 0,
                        "an item that has not arrived continues to track the actual moving player");
            } finally {
                drops.forEach(Entity::discard);
                if (forced) f.level.setChunkForced(chunk.x, chunk.z, false);
            }
        }
        test.succeed();
    }

    public static void followingSparkleTypeSurvivesNativeSave(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            var original = new EntityFollowingItem(f.level, f.base.getX(), f.base.getY(), f.base.getZ(), new ItemStack(Items.DIAMOND));
            original.setSparkleType(10);
            var saved = original.saveWithoutId(new CompoundTag());
            var loaded = new EntityFollowingItem(f.level); loaded.load(saved);
            test.assertTrue(loaded.getSparkleType() == 10 && ItemStack.matches(original.getItem(), loaded.getItem()),
                    "native save/load preserves the original following-item effect kind and its actual item stack");
        }
        test.succeed();
    }

    public static void specialSubtypesKeepOriginalExplosionImmunity(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            var drops = new ArrayList<net.minecraft.world.entity.item.ItemEntity>();
            try {
                for (boolean following : new boolean[]{true, false}) {
                    var drop = following
                            ? new EntityFollowingItem(f.level, f.base.getX(), f.base.getY(), f.base.getZ(), new ItemStack(Items.DIAMOND))
                            : new EntityPermanentItem(f.level, f.base.getX(), f.base.getY(), f.base.getZ(), new ItemStack(Items.DIAMOND));
                    drops.add(drop);
                    test.assertTrue(!drop.hurt(f.level.damageSources().explosion(null, null), 100) && drop.isAlive(),
                            "the special-item subtype retains original explosion immunity: " + (following ? "following" : "permanent"));
                    test.assertTrue(drop.hurt(f.level.damageSources().onFire(), 100) && !drop.isAlive(),
                            "ordinary fire damage remains native; the special-item subtype is not fireproof: " + (following ? "following" : "permanent"));
                }
            } finally { drops.forEach(Entity::discard); }
        }
        test.succeed();
    }
}
