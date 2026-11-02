package thaumcraft.common.testing;

import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.blocks.ItemVariantBlock;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.tiles.TileEldritchCrystal;
import thaumcraft.common.tiles.TileWardingStone;

/** Native contact, durable orientation and real grounded/airborne entity inputs. */
public final class TailEldritchContactRuntimeChecks {
    private TailEldritchContactRuntimeChecks() {}

    public static void nothingDamagesSurvivalAndSparesCreative(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            for (int tick = 0; tick < 61; tick++) f.level.tickNonPassenger(f.player); // Advance native age and login damage protection.
            test.assertTrue(f.player.tickCount > 20, "the native contact actor is past the original twenty-tick grace period");
            f.player.invulnerableTime = 0;
            test.assertTrue(f.player.hurt(f.level.damageSources().generic(), 1), "native survival damage control is admitted");
            f.player.setHealth(20); f.player.invulnerableTime = 0;
            f.put(f.base, ThaumcraftEldritchBlocks.ELDRITCH_NOTHING.get());
            var state = f.level.getBlockState(f.base);
            state.entityInside(f.level, f.base, f.player);
            float survival = f.player.getHealth();
            f.mode(GameType.CREATIVE); f.player.setHealth(20); f.player.invulnerableTime = 0;
            state.entityInside(f.level, f.base, f.player);
            float creative = f.player.getHealth();
            var fresh = EntityType.PIG.create(f.level);
            test.assertTrue(fresh != null, "real pig exists");
            try {
                fresh.setPos(Vec3.atCenterOf(f.base));
                test.assertTrue(f.level.addFreshEntity(fresh), "fresh contact control is admitted");
                float before = fresh.getHealth();
                state.entityInside(f.level, f.base, fresh);
                test.assertTrue(fresh.getHealth() == before, "original first twenty ticks remain immune");
            } finally { fresh.discard(); }
            test.assertTrue(survival == 12 && creative == 20,
                    "original nothing hurts ordinary survival by eight and exempts creative; survival=" + survival + ", creative=" + creative);
        }
        test.succeed();
    }

    public static void crystalOrientationSurvivesSaveAndUpdate(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open(); f.put(f.base, ThaumcraftEldritchBlocks.ELDRITCH_CRYSTAL.get());
            var tile = (TileEldritchCrystal) f.level.getBlockEntity(f.base);
            test.assertTrue(tile != null && tile.getOrientation() == Direction.UP.get3DDataValue(), "original constructor orientation is UP");
            tile.setOrientation(Direction.WEST);
            var saved = tile.saveWithFullMetadata(f.level.registryAccess());
            var restored = BlockEntity.loadStatic(f.base, tile.getBlockState(), saved, f.level.registryAccess());
            var client = new TileEldritchCrystal(f.base, tile.getBlockState());
            client.loadWithComponents(tile.getUpdateTag(f.level.registryAccess()), f.level.registryAccess());
            int disk = restored instanceof TileEldritchCrystal crystal ? crystal.getOrientation() : -1;
            test.assertTrue(disk == Direction.WEST.get3DDataValue() && client.getOrientation() == Direction.WEST.get3DDataValue(),
                    "registered crystal retains its explicitly configured orientation through native disk and initial update tag; disk="
                            + disk + ", update=" + client.getOrientation() + ", saved=" + saved);
        }
        test.succeed();
    }

    public static void wardingPushesOnlyAirborneMobs(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            var state = ((ItemVariantBlock) ThaumcraftEldritchBlocks.PAVING_STONE_WARDING_ITEM.get()).variantState();
            f.level.setBlock(f.base, state, TubeVariantsRuntimeChecks.QUIET);
            var tile = (TileWardingStone) f.level.getBlockEntity(f.base);
            var ticker = state.getTicker(f.level, ThaumcraftEldritchBlocks.WARDING_STONE_TILE.get());
            test.assertTrue(tile != null && ticker != null, "actual warding paving stone provides its native ticker");
            var pig = EntityType.PIG.create(f.level);
            test.assertTrue(pig != null, "real ward target exists");
            try {
                pig.setPos(f.base.getX() + .5, f.base.getY() + 1, f.base.getZ() + .5);
                pig.setNoAi(true); pig.setNoGravity(true); pig.setYRot(0); pig.setOnGround(true);
                test.assertTrue(f.level.addFreshEntity(pig), "native grounded target admitted");
                pig.setDeltaMovement(Vec3.ZERO);
                for (int i = 0; i < 5; i++) ticker.tick(f.level, f.base, state, tile);
                Vec3 grounded = pig.getDeltaMovement();
                pig.setOnGround(false); pig.setDeltaMovement(Vec3.ZERO);
                for (int i = 0; i < 5; i++) ticker.tick(f.level, f.base, state, tile);
                Vec3 airborne = pig.getDeltaMovement();
                f.put(f.base.west(), Blocks.REDSTONE_BLOCK); pig.setDeltaMovement(Vec3.ZERO);
                for (int i = 0; i < 5; i++) ticker.tick(f.level, f.base, state, tile);
                Vec3 powered = pig.getDeltaMovement();
                for (int i = 0; i < 85; i++) ticker.tick(f.level, f.base, state, tile);
                boolean sealed = !f.level.getBlockState(f.base.above()).isAir() && !f.level.getBlockState(f.base.above(2)).isAir();
                test.assertTrue(grounded.equals(Vec3.ZERO) && Math.abs(airborne.z + .2) < 1e-6
                                && Math.abs(airborne.y + .1) < 1e-6 && powered.equals(Vec3.ZERO) && sealed,
                        "original skips grounded mobs, pushes airborne mobs only without power and seals independently; grounded="
                                + grounded + ", airborne=" + airborne + ", powered=" + powered + ", sealed=" + sealed);
            } finally { pig.discard(); }
        }
        test.succeed();
    }
}
