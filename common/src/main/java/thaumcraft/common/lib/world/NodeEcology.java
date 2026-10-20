package thaumcraft.common.lib.world;

import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.blocks.BlockTaintFibres;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftMonsters;
import thaumcraft.common.config.ThaumcraftSettings;
import thaumcraft.common.entities.monster.EntityGiantBrainyZombie;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.tiles.TileNode.NodeType;

/** Original TileNode703..798 handlers, called in their original order by the sole native node ticker. */
public final class NodeEcology {
    private NodeEcology() {}

    /** True requests only the original non-PURE/non-TAINTED -> TAINTED transition; the owning tile performs nodeChange itself. */
    public static boolean taint(TileNode node, BooleanSupplier current) {
        if (!(node.getLevel() instanceof ServerLevel level) || !current.getAsBoolean()) return false;
        BlockPos origin = node.getBlockPos(); NodeType type = node.getNodeType(); int count = node.getTickCount();
        if (type == NodeType.TAINTED && count % 50 == 0) {
            int x = origin.getX() + level.random.nextInt(8) - level.random.nextInt(8);
            int z = origin.getZ() + level.random.nextInt(8) - level.random.nextInt(8);
            if (!current.getAsBoolean()) return false;
            var biome = BiomeColumns.surfaceHolder(level, x, z);
            if (biome != null && !biome.is(ThaumcraftBiomes.TAINT) && current.getAsBoolean()) BiomeColumns.set(level, x, z, ThaumcraftBiomes.TAINT);
            if (!current.getAsBoolean() || !ThaumcraftSettings.server().hardNodes() || !level.random.nextBoolean()) return false;
            x = origin.getX() + level.random.nextInt(5) - level.random.nextInt(5);
            z = origin.getZ() + level.random.nextInt(5) - level.random.nextInt(5);
            int y = origin.getY() + level.random.nextInt(5) - level.random.nextInt(5);
            if (current.getAsBoolean()) BlockTaintFibres.spreadFibres(level, x, y, z);
        }
        if (!current.getAsBoolean() || type == NodeType.PURE || type == NodeType.TAINTED || count % 100 != 0) return false;
        var biome = BiomeColumns.surfaceHolder(level, origin.getX(), origin.getZ());
        return biome != null && biome.is(ThaumcraftBiomes.TAINT) && level.random.nextInt(500) == 0 && current.getAsBoolean();
    }

    public static void pure(TileNode node, BooleanSupplier current) {
        if (!(node.getLevel() instanceof ServerLevel level) || !current.getAsBoolean()
                || !NodeEcologyPolicy.permitsDimension(level.dimension()) || node.getNodeType() != NodeType.PURE || node.getTickCount() % 50 != 0) return;
        BlockPos origin = node.getBlockPos();
        int x = origin.getX() + level.random.nextInt(8) - level.random.nextInt(8);
        int z = origin.getZ() + level.random.nextInt(8) - level.random.nextInt(8);
        if (!current.getAsBoolean()) return;
        var biome = BiomeColumns.surfaceHolder(level, x, z);
        if (biome == null || !NodeEcologyPolicy.permitsLevel(NodeEcologyPolicy.biomeLevel(biome))
                || biome.is(ThaumcraftBiomes.MAGICAL_FOREST) || !current.getAsBoolean()) return;
        if (biome.is(ThaumcraftBiomes.TAINT) || node.getBlockState().is(ThaumcraftContent.SILVERWOOD_KNOT.get()))
            BiomeColumns.set(level, x, z, ThaumcraftBiomes.MAGICAL_FOREST);
    }

    public static void dark(TileNode node, BooleanSupplier current) {
        if (!(node.getLevel() instanceof ServerLevel level) || !current.getAsBoolean()) return;
        BlockPos origin = node.getBlockPos();
        var center = BiomeColumns.surfaceHolder(level, origin.getX(), origin.getZ());
        if (center == null || !NodeEcologyPolicy.permitsLevel(NodeEcologyPolicy.biomeLevel(center))
                || !NodeEcologyPolicy.permitsDimension(level.dimension()) || node.getNodeType() != NodeType.DARK || node.getTickCount() % 50 != 0) return;
        int x = origin.getX() + level.random.nextInt(12) - level.random.nextInt(12);
        int z = origin.getZ() + level.random.nextInt(12) - level.random.nextInt(12);
        if (!current.getAsBoolean()) return;
        var target = BiomeColumns.surfaceHolder(level, x, z);
        // Original checks ONLY the center biome's blacklist, not this sampled target's blacklist.
        if (target != null && !target.is(ThaumcraftBiomes.EERIE) && current.getAsBoolean()) BiomeColumns.set(level, x, z, ThaumcraftBiomes.EERIE);
        if (!current.getAsBoolean() || !ThaumcraftSettings.server().hardNodes() || !level.random.nextBoolean()
                || level.getNearestPlayer(origin.getX() + .5, origin.getY() + .5, origin.getZ() + .5, 24, false) == null
                || !current.getAsBoolean()) return;
        // The original constructs a Giant BEFORE counting the existing class; <=3 permits one fourth Giant.
        var giant = ThaumcraftMonsters.GIANT_BRAINY_ZOMBIE.get().create(level);
        if (giant == null || !current.getAsBoolean()
                || level.getEntitiesOfClass(EntityGiantBrainyZombie.class, new AABB(origin).inflate(10, 6, 10)).size() > 3) return;
        double spawnX = origin.getX() + (level.random.nextDouble() - level.random.nextDouble()) * 5;
        double spawnY = origin.getY() + level.random.nextInt(3) - 1;
        double spawnZ = origin.getZ() + (level.random.nextDouble() - level.random.nextDouble()) * 5;
        giant.moveTo(spawnX, spawnY, spawnZ, level.random.nextFloat() * 360, 0);
        if (!current.getAsBoolean() || !canSpawnDarkGiant(level, giant) || !current.getAsBoolean()) return;
        // No finalizeSpawn/onSpawnWithEgg: the original path never performs its equipment/baby setup.
        if (level.addFreshEntity(giant)) {
            level.levelEvent(2004, origin, 0);
            giant.spawnAnim();
        }
    }

    /** EntityMob.getCanSpawnHere -> Creature -> Living, with native geometry/brightness access and the Giant's own RNG. */
    static boolean canSpawnDarkGiant(ServerLevel level, EntityGiantBrainyZombie giant) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false;
        BlockPos pos = BlockPos.containing(giant.getX(), giant.getBoundingBox().minY, giant.getZ());
        if (level.getBrightness(LightLayer.SKY, pos) > giant.getRandom().nextInt(32)) return false;
        int brightness = level.getMaxLocalRawBrightness(pos, level.isThundering() ? 10 : level.getSkyDarken());
        if (brightness > giant.getRandom().nextInt(8)) return false;
        return level.isUnobstructed(giant) && level.noCollision(giant) && !level.containsAnyLiquid(giant.getBoundingBox())
                && giant.getWalkTargetValue(pos, level) >= 0;
    }
}
