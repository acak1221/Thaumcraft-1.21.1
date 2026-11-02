package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.lib.utils.EntityUtils;
import thaumcraft.common.lib.utils.InventoryUtils;
import thaumcraft.common.lib.utils.Utils;

/** Native level/entity/registry fixtures for the public compatibility helpers. */
public final class TailUtilityReviewRuntimeChecks {
    private TailUtilityReviewRuntimeChecks() {}

    public static void chunkLookupUsesBlockCoordinatesWithoutLoading(GameTestHelper test) {
        var level = test.getLevel();
        BlockPos loaded = test.absolutePos(new BlockPos(1, 2, 1));
        test.assertTrue(level.hasChunkAt(loaded), "the native test fixture chunk is already loaded");
        List<String> failures = new ArrayList<>();
        check(failures, Utils.isChunkLoaded(level, loaded.getX(), loaded.getZ()), "a loaded block coordinate must remain loaded");

        // The loaded coarse chunk contains 256 block-coordinate probe positions whose real
        // chunks lie in a different 16x16 band. Discover an unloaded one through native
        // queries only; never create or unload terrain merely to manufacture the result.
        BlockPos unloaded = null;
        int coarseX = loaded.getX() >> 4, coarseZ = loaded.getZ() >> 4;
        for (int x = 0; x < 16 && unloaded == null; x++) for (int z = 0; z < 16 && unloaded == null; z++) {
            int blockX = (coarseX * 16 + x) * 16 + 8;
            int blockZ = (coarseZ * 16 + z) * 16 + 8;
            if (!level.hasChunkAt(blockX, blockZ) && level.hasChunkAt(blockX >> 4, blockZ >> 4))
                unloaded = new BlockPos(blockX, loaded.getY(), blockZ);
        }
        test.assertTrue(unloaded != null, "the native fixture has a distinct unloaded chunk and a loaded coarse-address control");
        check(failures, !Utils.isChunkLoaded(level, unloaded.getX(), unloaded.getZ()), "the helper must query the requested unloaded chunk: " + unloaded);
        check(failures, !level.hasChunkAt(unloaded), "the helper must not load a refused chunk");

        for (int x : new int[]{-257, -256, -255, -17, -16, -15, -1, 0, 15, 16, 255, 256, 257}) {
            boolean expected = level.hasChunkAt(x, loaded.getZ());
            check(failures, Utils.isChunkLoaded(level, x, loaded.getZ()) == expected, "native block/chunk boundary differs at x=" + x);
            check(failures, level.hasChunkAt(x, loaded.getZ()) == expected, "chunk inspection must remain nonloading at x=" + x);
        }
        test.assertTrue(failures.isEmpty(), String.join("; ", failures));
        test.succeed();
    }

    public static void lineOfSightAndPointingDistinguishNativeMiss(GameTestHelper test) {
        var level = test.getLevel();
        BlockPos base = test.absolutePos(new BlockPos(2, 2, 2)).atY(205);
        Map<BlockPos, BlockState> saved = new LinkedHashMap<>();
        var viewer = test.makeMockPlayer(GameType.SURVIVAL);
        Pig target = EntityType.PIG.create(level);
        test.assertTrue(target != null, "the native pig type creates the target");
        List<String> failures = new ArrayList<>();
        try {
            for (BlockPos mutable : BlockPos.betweenClosed(base.offset(-2, -1, -1), base.offset(2, 4, 6))) {
                BlockPos pos = mutable.immutable();
                test.assertTrue(level.hasChunkAt(pos) && level.getBlockEntity(pos) == null, "the owned LOS fixture replaces no foreign block entity");
                saved.put(pos, level.getBlockState(pos));
            }
            for (BlockPos pos : saved.keySet()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            level.setBlock(base.below(), Blocks.CHEST.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            BlockEntity origin = level.getBlockEntity(base.below());
            test.assertTrue(origin != null, "the real installed chest supplies the block-entity LOS overload");
            viewer.setPos(base.getX() + .5, base.getY(), base.getZ() + .5);
            viewer.setYRot(0); viewer.setXRot(0);
            target.setPos(base.getX() + .5, base.getY() + 1, base.getZ() + 4.5);
            target.setNoAi(true); target.setNoGravity(true); target.setInvulnerable(true);
            test.assertTrue(level.addFreshEntity(target) && level.getEntity(target.getUUID()) == target, "the target is admitted to the actual native entity index");
            test.assertTrue(target.isPickable() && viewer.getLookAngle().z > .99, "the native target is pickable and the viewer faces it");

            check(failures, EntityUtils.canEntityBeSeen(target, viewer), "entity-to-entity unobstructed LOS must succeed");
            Vec3 eye = viewer.getEyePosition();
            check(failures, EntityUtils.canEntityBeSeen(target, eye.x, eye.y, eye.z), "coordinate-to-entity unobstructed LOS must succeed");
            check(failures, EntityUtils.canEntityBeSeen(target, origin), "block-entity-to-entity unobstructed LOS must succeed");
            check(failures, EntityUtils.getPointedEntity(level, viewer, .5, 6, 1.1F) == target, "ordinary pointing selects a native pickable pig");
            check(failures, EntityUtils.getPointedEntity(level, viewer, .5, 6, 1.1F, true) == target, "the non-colliding opt-in still requires an unobstructed ray");
            check(failures, EntityUtils.getPointedEntity(level, viewer, 6, (Class<?>) null) == target, "the class-filter overload selects an unobstructed native pig");
            check(failures, EntityUtils.getPointedEntity(level, viewer, 6, Pig.class) == null, "an explicitly excluded type remains excluded");
            check(failures, EntityUtils.getPointedEntity(level, viewer, 5, 6, 1.1F) == null, "minimum distance still refuses the nearby target");

            for (int x = -1; x <= 1; x++) for (int y = 0; y <= 3; y++)
                level.setBlock(base.offset(x, y, 2), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            check(failures, !EntityUtils.canEntityBeSeen(target, viewer), "a real opaque wall blocks entity LOS");
            check(failures, !EntityUtils.canEntityBeSeen(target, eye.x, eye.y, eye.z), "a real opaque wall blocks coordinate LOS");
            check(failures, !EntityUtils.canEntityBeSeen(target, origin), "a real opaque wall blocks block-entity LOS");
            check(failures, EntityUtils.getPointedEntity(level, viewer, .5, 6, 1.1F) == null, "ordinary pointing refuses a target behind the wall");
            check(failures, EntityUtils.getPointedEntity(level, viewer, 6, (Class<?>) null) == null, "class-filter pointing refuses a target behind the wall");
        } finally {
            target.discard(); viewer.discard();
            saved.forEach((pos, state) -> level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE));
        }
        test.assertTrue(failures.isEmpty(), String.join("; ", failures));
        test.succeed();
    }

    public static void unbreakingOnlyDecidesWhetherOneWearPointIsAvoided(GameTestHelper test) {
        var level = test.getLevel();
        var unbreaking = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING);
        List<String> failures = new ArrayList<>();
        ItemStack plain = new ItemStack(Items.IRON_PICKAXE);
        ItemStack plainResult = InventoryUtils.damageItem(1, plain, level);
        check(failures, plainResult == plain && plain.getDamageValue() == 1 && plain.getCount() == 1, "plain tool wear remains exactly one point");

        long paidSeed = seedForToolRoll(0), avoidedSeed = seedForToolRoll(1);
        for (boolean avoided : new boolean[]{false, true}) {
            ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
            tool.enchant(unbreaking, 1);
            test.assertTrue(InventoryUtils.unbreakingLevel(level, tool) == 1, "the actual native stack carries Unbreaking1");
            level.random.setSeed(avoided ? avoidedSeed : paidSeed);
            ItemStack result = InventoryUtils.damageItem(1, tool, level);
            check(failures, result == tool && tool.getCount() == 1 && tool.getDamageValue() == (avoided ? 0 : 1),
                    "one Unbreaking tool point must " + (avoided ? "be avoided" : "add exactly one wear") + ", actual=" + tool.getDamageValue());
            check(failures, InventoryUtils.unbreakingLevel(level, tool) == 1, "wear preserves the enchantment component");
        }

        // Original EnchantmentDurability treats armor's first float below .6 as
        // an unavoidable point; the helper must not consume a random amount of wear.
        long armorPaidSeed = seedForArmorRoll(false), armorAvoidedSeed = seedForArmorRoll(true);
        for (boolean avoided : new boolean[]{false, true}) {
            ItemStack armor = new ItemStack(Items.IRON_CHESTPLATE);
            armor.enchant(unbreaking, 1);
            level.random.setSeed(avoided ? armorAvoidedSeed : armorPaidSeed);
            InventoryUtils.damageItem(1, armor, level);
            check(failures, armor.getCount() == 1 && armor.getDamageValue() == (avoided ? 0 : 1),
                    "the original armor avoidance coin adds only zero or one wear, avoided=" + avoided + ", actual=" + armor.getDamageValue());
        }
        test.assertTrue(failures.isEmpty(), String.join("; ", failures));
        test.succeed();
    }

    private static long seedForToolRoll(int roll) {
        for (long seed = 0; seed < 100_000; seed++) if (RandomSource.create(seed).nextInt(2) == roll) return seed;
        throw new AssertionError("No deterministic native Unbreaking tool roll");
    }

    private static long seedForArmorRoll(boolean avoided) {
        for (long seed = 0; seed < 100_000; seed++) {
            RandomSource random = RandomSource.create(seed);
            float coin = random.nextFloat();
            if (!avoided && coin < .6F || avoided && coin >= .6F && random.nextInt(2) > 0) return seed;
        }
        throw new AssertionError("No deterministic native Unbreaking armor roll");
    }

    private static void check(List<String> failures, boolean condition, String message) {
        if (!condition) failures.add(message);
    }
}
