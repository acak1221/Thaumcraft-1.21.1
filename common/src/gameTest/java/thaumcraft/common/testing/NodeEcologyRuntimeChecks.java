package thaumcraft.common.testing;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ResearchSettings;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftEssences;
import thaumcraft.common.config.ThaumcraftMonsters;
import thaumcraft.common.config.ThaumcraftNaturals;
import thaumcraft.common.config.ThaumcraftSettings;
import thaumcraft.common.config.ThaumcraftTaint;
import thaumcraft.common.blocks.BlockTaintFibres;
import thaumcraft.common.entities.monster.EntityGiantBrainyZombie;
import thaumcraft.common.lib.world.BiomeColumns;
import thaumcraft.common.lib.world.NodeEcologyPolicy;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.testing.NodeTransductionRuntimeChecks.InputRandom;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.api.nodes.NodeModifier;

/** Real registered nodes/biomes/mobs; explicit RNG branch inputs never inject a biome or mob reward. */
public final class NodeEcologyRuntimeChecks {
    private NodeEcologyRuntimeChecks() {}

    public static void pure(GameTestHelper test) {
        try (var check = new Check(test)) {
            check.open(); check.pure(); test.succeed();
        }
    }

    public static void dark(GameTestHelper test) {
        var check = new Check(test);
        try { check.open(); check.darkBiomes(); check.darkRoom(); }
        catch (RuntimeException | Error failure) { check.close(); throw failure; }
        // Observe actual native light/entity readiness rather than assuming a fixed number of asynchronous lighting ticks.
        test.onEachTick(() -> {
            if (check.closed || !check.darkRoomReady()) return;
            try { check.darkSpawns(); test.succeed(); }
            finally { check.close(); }
        });
        test.runAfterDelay(180, () -> {
            if (!check.closed) { check.close(); test.fail("native DARK room did not reach zero light and entity ticking before180 world ticks"); }
        });
    }

    public static void embeddedHarvest(GameTestHelper test) {
        var check = new Check(test);
        try { check.open(); }
        catch (RuntimeException | Error failure) { check.close(); throw failure; }
        test.onEachTick(() -> {
            if (check.closed || !check.level.isPositionEntityTicking(check.pos)) return;
            try { check.embeddedHarvest(); test.succeed(); }
            finally { check.close(); }
        });
        test.runAfterDelay(160, () -> {
            if (!check.closed) { check.close(); test.fail("native embedded-node harvest area did not reach entity ticking before160 world ticks"); }
        });
    }

    public static void tainted(GameTestHelper test) {
        try (var check = new Check(test)) {
            check.open(); check.tainted(); test.succeed();
        }
    }

    private record Quart(int x, int y, int z) {}
    private static final class Check implements AutoCloseable {
        final GameTestHelper test; final Fixture f; final ServerLevel level; final BlockPos pos, sampled;
        final ResearchSettings settings = ThaumcraftSettings.server();
        final Map<ResourceKey<Level>, Integer> oldDimensions = new HashMap<>(NodeEcologyPolicy.dimensionBlacklist);
        final Map<ResourceKey<Biome>, Integer> oldBiomePolicy = new HashMap<>(NodeEcologyPolicy.biomeBlacklist);
        final Map<ChunkPos, Map<Quart, Holder<Biome>>> oldBiomes = new HashMap<>();
        final List<ChunkPos> forced = new ArrayList<>(); final List<Entity> mobs = new ArrayList<>();
        final Difficulty difficulty; final boolean difficultyLocked;
        TileNode node; boolean closed;
        Check(GameTestHelper test) {
            this.test = test; f = new Fixture(test); level = f.level; pos = f.base.above(100); sampled = pos.east(6);
            difficulty = level.getServer().getWorldData().getDifficulty();
            difficultyLocked = level.getServer().getWorldData().isDifficultyLocked();
        }
        void open() {
            f.open(); f.player.setInvulnerable(true); f.mode(GameType.CREATIVE);
            f.aim(Vec3.atCenterOf(pos).add(-6, 0, 0));
            NodeEcologyPolicy.dimensionBlacklist.clear(); NodeEcologyPolicy.biomeBlacklist.clear(); hard(false);
            // Only the bounded ecology/spawn area is held; retain preexisting forced tickets.
            for (int x = (pos.getX() - 16) >> 4; x <= (pos.getX() + 16) >> 4; x++) {
                for (int z = (pos.getZ() - 16) >> 4; z <= (pos.getZ() + 16) >> 4; z++) {
                    if (!level.getForcedChunks().contains(ChunkPos.asLong(x, z))) {
                        level.setChunkForced(x, z, true); forced.add(new ChunkPos(x, z));
                    }
                    level.getChunk(x, z);
                }
            }
            put(pos, Blocks.AIR);
        }
        void hard(boolean value) {
            ThaumcraftSettings.configureServer(new ResearchSettings(settings.cheatSheet(), settings.wardedStone(), settings.mirrors(), value,
                    settings.wuss(), settings.difficulty(), settings.aspectCap()));
        }
        void put(BlockPos target, Block block) {
            if (!f.states.containsKey(target)) {
                f.states.put(target, level.getBlockState(target));
                if (level.getBlockEntity(target) != null) f.originals.put(target, level.getBlockEntity(target));
            }
            f.put(target, block);
        }
        void fresh(NodeType type, boolean embedded) {
            put(pos, embedded ? ThaumcraftContent.SILVERWOOD_KNOT.get() : ThaumcraftContent.AURA_NODE.get());
            node = (TileNode) level.getBlockEntity(pos);
            node.initializeGenerated(new AspectList().add(Aspect.AIR, 10), type, NodeModifier.FADING);
        }
        void phase49() {
            for (int tick = 0; tick < 49; tick++) TileNode.serverTick(level, pos, level.getBlockState(pos), node);
            f.check(node.getTickCount() == 49, "real FADING node reaches count49 without manual counter mutation");
        }
        void tick() { TileNode.serverTick(level, pos, level.getBlockState(pos), node); }
        void paint(BlockPos target, ResourceKey<Biome> biome) {
            LevelChunk chunk = level.getChunkAt(target); ChunkPos owner = chunk.getPos();
            oldBiomes.computeIfAbsent(owner, ignored -> snapshot(chunk));
            BiomeColumns.set(level, target.getX(), target.getZ(), biome);
            f.check(BiomeColumns.surfaceHolder(level, target.getX(), target.getZ()).is(biome), "declared native stored biome fixture is present");
        }
        void biome(BlockPos target, ResourceKey<Biome> expected, String why) {
            f.check(BiomeColumns.surfaceHolder(level, target.getX(), target.getZ()).is(expected), why);
        }
        void offsets(int bound, boolean enabled) {
            int[] inputs = {6, 0, 0, 0}; int[] cursor = {0};
            try (var rng = new InputRandom(f, actual -> {
                f.check(actual == bound && cursor[0] < inputs.length, "original ecology uses only its four ordered horizontal offset draws");
                return inputs[cursor[0]++];
            }, true)) {
                tick(); f.check(rng.bounds.equals(enabled ? List.of(bound, bound, bound, bound) : List.of()),
                        "ecology blacklist gate retains the original offset draw boundary");
            }
        }
        void pure() {
            paint(pos, ThaumcraftBiomes.MAGICAL_FOREST); paint(sampled, ThaumcraftBiomes.EERIE);
            fresh(NodeType.PURE, false); phase49(); offsets(8, true);
            biome(sampled, ThaumcraftBiomes.EERIE, "a wild PURE aura node cannot convert arbitrary untainted Eerie terrain");

            paint(sampled, ThaumcraftBiomes.TAINT); fresh(NodeType.PURE, false); phase49(); offsets(8, true);
            biome(sampled, ThaumcraftBiomes.MAGICAL_FOREST, "a wild PURE node really cleanses the actual Taint biome on tick50");

            paint(sampled, ThaumcraftBiomes.EERIE); fresh(NodeType.PURE, true); phase49(); offsets(8, true);
            biome(sampled, ThaumcraftBiomes.MAGICAL_FOREST, "an actual embedded Silverwood PURE node also converts an untainted biome");
            for (int denied : new int[]{0, 2}) {
                paint(sampled, ThaumcraftBiomes.EERIE); NodeEcologyPolicy.biomeBlacklist.put(ThaumcraftBiomes.EERIE, denied);
                fresh(NodeType.PURE, true); phase49(); offsets(8, true);
                biome(sampled, ThaumcraftBiomes.EERIE, "PURE respects the sampled target biome blacklist level" + denied);
            }
            NodeEcologyPolicy.biomeBlacklist.put(ThaumcraftBiomes.EERIE, 1);
            fresh(NodeType.PURE, true); phase49(); offsets(8, true);
            biome(sampled, ThaumcraftBiomes.MAGICAL_FOREST, "original blacklist level1 still allows real PURE conversion");
            for (int denied : new int[]{0, 2}) {
                paint(sampled, ThaumcraftBiomes.TAINT); NodeEcologyPolicy.dimensionBlacklist.put(level.dimension(), denied);
                fresh(NodeType.PURE, false); phase49(); offsets(8, false);
                biome(sampled, ThaumcraftBiomes.TAINT, "dimension blacklist prevents PURE before consuming offset RNG");
            }
        }
        void embeddedHarvest() {
            f.check(level.isPositionEntityTicking(pos), "real embedded-harvest fixture has active native entity sections before creating drops");
            fresh(NodeType.PURE, true);
            node.initializeGenerated(new AspectList().add(Aspect.AIR, 25).add(Aspect.ORDER, 4), NodeType.PURE, NodeModifier.FADING);
            var knot = level.getBlockState(pos); var original = node;
            f.check(knot.is(BlockTags.LOGS) && knot.getCollisionShape(level, pos).bounds().equals(new AABB(0, 0, 0, 1, 1, 1))
                            && knot.getLightEmission() == 7 && knot.getDestroySpeed(level, pos) == 2.5F
                            && knot.getBlock().asItem() == Items.AIR
                            && knot.getBlock().getCloneItemStack(level, pos, knot).is(ThaumcraftNaturals.SILVERWOOD_LOG.get().asItem()),
                    "embedded node is a real tagged solid Silverwood log with original light/hardness and plain-log pick identity");
            f.mode(GameType.SURVIVAL); f.aim(Vec3.atCenterOf(pos));
            var scanner = new ItemStack(ThaumcraftContent.THAUMOMETER.get());
            f.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, scanner);
            f.check(scanner.getItem().use(level, f.player, net.minecraft.world.InteractionHand.MAIN_HAND).getResult().consumesAction()
                            && scanner.get(ThaumcraftContent.SCAN_SESSION.get()) != null
                            && scanner.get(ThaumcraftContent.SCAN_SESSION.get()).signature().equals("node:NODE" + node.getScanId()),
                    "actual thaumometer ray acquires the embedded node UUID rather than a plain-log item session");
            scanner.getItem().releaseUsing(scanner, level, f.player, 25); f.player.stopUsingItem();

            node.initializeGenerated(new AspectList().add(Aspect.AIR, 5).add(Aspect.ORDER, 4), NodeType.PURE, null);
            var wand = new ItemStack(ThaumcraftContent.WAND.get()); var vis = WandData.EMPTY;
            for (Aspect primal : Aspect.getPrimalAspects()) vis = vis.withAmount(primal.getTag(), primal == Aspect.AIR ? 2450 : 2500);
            wand.set(ThaumcraftContent.WAND_DATA.get(), vis); f.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, wand);
            f.check(wand.getItem().use(level, f.player, net.minecraft.world.InteractionHand.MAIN_HAND).getResult().consumesAction()
                            && ItemWandCasting.isUsingNode(f.player), "actual live-hand wand ray starts an embedded-node use session");
            wand.getItem().onUseTick(level, f.player, wand, 2000);
            f.check(ItemWandCasting.getVis(wand, "aer") == 2500 && node.getAspects().getAmount(Aspect.AIR) == 4 && node.getNodeVisBase(Aspect.AIR) == 5,
                    "real wand callback pays one embedded-node CURRENT unit for the final fifty centivis without changing BASE");
            wand.getItem().releaseUsing(wand, level, f.player, 2000); f.player.stopUsingItem();
            // Isolate recharge from biome painting; this is the original actual blacklist gate, not a replacement node or reward setter.
            NodeEcologyPolicy.dimensionBlacklist.put(level.dimension(), 0);
            for (int tick = 0; tick < 599; tick++) tick();
            f.check(node.getAspects().getAmount(Aspect.AIR) == 4, "embedded PURE aura remains drained before its original normal count600 recharge");
            tick(); NodeEcologyPolicy.dimensionBlacklist.remove(level.dimension());
            f.check(node.getAspects().getAmount(Aspect.AIR) == 5 && node.getNodeVisBase(Aspect.AIR) == 5,
                    "the actual embedded-node lifecycle restores one real missing current unit at count600");
            node.initializeGenerated(new AspectList().add(Aspect.AIR, 25).add(Aspect.ORDER, 4), NodeType.PURE, NodeModifier.FADING);
            f.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
            var old = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(8)).stream().map(Entity::getUUID).toList();
            try {
            f.check(f.player.gameMode.destroyBlock(pos), "actual connected survival actor harvests the embedded native knot");
            DeferredItemDrops.tick(level);
            var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(8), entity -> !old.contains(entity.getUUID())); mobs.addAll(drops);
            f.check(level.getBlockState(pos).isAir() && original.isRemoved()
                            && drops.stream().filter(entity -> entity.getItem().is(ThaumcraftNaturals.SILVERWOOD_LOG.get().asItem())).mapToInt(entity -> entity.getItem().getCount()).sum() == 1
                            && drops.stream().filter(entity -> entity.getItem().is(ThaumcraftEssences.WISP_ESSENCE.get())).mapToInt(entity -> entity.getItem().getCount()).sum() == 3
                            && drops.stream().filter(entity -> entity.getItem().is(ThaumcraftEssences.WISP_ESSENCE.get()))
                                    .allMatch(entity -> ThaumcraftEssences.WISP_ESSENCE.get().getAspects(entity.getItem()).size() == 1
                                            && ThaumcraftEssences.WISP_ESSENCE.get().getAspects(entity.getItem()).getAmount(Aspect.AIR) == 2),
                    "real player harvest pays one plain Silverwood log plus exactly original three AIR2 essences from CURRENT25, with no ORDER4 or whole-node item");
            drops.forEach(Entity::discard);
            fresh(NodeType.PURE, true); node.initializeGenerated(new AspectList().add(Aspect.AIR, 25), NodeType.PURE, NodeModifier.FADING);
            f.check(level.destroyBlock(pos, true), "ordinary environmental native destruction removes a second real knot");
            var unpaid = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(8), entity -> !old.contains(entity.getUUID())); mobs.addAll(unpaid);
            f.check(unpaid.size() == 1 && unpaid.getFirst().getItem().is(ThaumcraftNaturals.SILVERWOOD_LOG.get().asItem()) && unpaid.getFirst().getItem().getCount() == 1,
                    "generic knot destruction drops only its real plain log and cannot acquire the player-only essence receipt");
            } finally {
                // This entire synchronous private operation has no interleaved actor/test ticks; record its outputs even on an assertion/delivery failure.
                mobs.addAll(level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(8), entity -> !old.contains(entity.getUUID())));
            }
        }
        void darkBiomes() {
            paint(pos, ThaumcraftBiomes.MAGICAL_FOREST); paint(sampled, ThaumcraftBiomes.TAINT);
            NodeEcologyPolicy.biomeBlacklist.put(ThaumcraftBiomes.TAINT, 0);
            fresh(NodeType.DARK, false); phase49(); offsets(12, true);
            biome(sampled, ThaumcraftBiomes.EERIE, "DARK converts its sampled target despite that target's blacklist, with hardNodes=false");
            for (int denied : new int[]{0, 2}) {
                paint(sampled, ThaumcraftBiomes.TAINT); NodeEcologyPolicy.biomeBlacklist.put(ThaumcraftBiomes.MAGICAL_FOREST, denied);
                fresh(NodeType.DARK, false); phase49(); offsets(12, false);
                biome(sampled, ThaumcraftBiomes.TAINT, "DARK checks the center biome blacklist before consuming offsets");
            }
            NodeEcologyPolicy.biomeBlacklist.put(ThaumcraftBiomes.MAGICAL_FOREST, 1);
            fresh(NodeType.DARK, false); phase49(); offsets(12, true);
            biome(sampled, ThaumcraftBiomes.EERIE, "center blacklist level1 allows DARK despite the sampled target level0");
            NodeEcologyPolicy.biomeBlacklist.clear();
        }
        void tainted() {
            paint(pos, ThaumcraftBiomes.MAGICAL_FOREST); paint(sampled, ThaumcraftBiomes.EERIE);
            NodeEcologyPolicy.dimensionBlacklist.put(level.dimension(), 0);
            NodeEcologyPolicy.biomeBlacklist.put(ThaumcraftBiomes.EERIE, 0);
            fresh(NodeType.TAINTED, false); phase49(); offsets(8, true);
            biome(sampled, ThaumcraftBiomes.TAINT, "TAINTED still converts a real biome with hardNodes=false and without PURE/DARK blacklist gates");
            NodeEcologyPolicy.dimensionBlacklist.clear(); NodeEcologyPolicy.biomeBlacklist.clear();

            var fibre = pos.east(2); put(fibre, Blocks.AIR); put(fibre.below(), Blocks.STONE); put(fibre.above(), Blocks.AIR);
            fresh(NodeType.TAINTED, false); phase49(); hard(true);
            try (var rng = new TaintNodeInput(f)) { tick(); rng.assertDraws(); }
            f.check(level.getBlockState(fibre).is(ThaumcraftTaint.FIBRES.get()) && level.getBlockState(fibre).getValue(BlockTaintFibres.TYPE) == 0,
                    "hard TAINTED ecology invokes the actual native spreadFibres and creates its supported TYPE0 block after the original ordered offsets");
            put(fibre, Blocks.AIR); put(fibre.below(), Blocks.AIR); hard(false);

            paint(pos, ThaumcraftBiomes.MAGICAL_FOREST);
            fresh(NodeType.NORMAL, false); node.takeVis("aer", 1);
            for (int tick = 0; tick < 99; tick++) tick();
            paint(pos, ThaumcraftBiomes.TAINT); var current = node.getAspects(); var base = node.getAspectsBase();
            try (var rng = new InputRandom(f, bound -> { f.check(bound == 500, "native Taint infection reads original bound500"); return 0; }, true)) {
                tick(); f.check(rng.bounds.equals(List.of(500)), "non-PURE/non-TAINTED infection has exactly the original one-in500 draw on tick100");
            }
            f.check(node.getNodeType() == NodeType.TAINTED && node.getAspects().aspects.equals(current.aspects) && node.getAspectsBase().aspects.equals(base.aspects),
                    "actual Taint biome infects the current normal node while preserving physical current/BASE aura");

            paint(pos, ThaumcraftBiomes.MAGICAL_FOREST); paint(sampled, ThaumcraftBiomes.EERIE);
            fresh(NodeType.DARK, false); phase49(); offsets(12, true);
            for (int tick = 0; tick < 49; tick++) tick();
            paint(pos, ThaumcraftBiomes.TAINT);
            try (var rng = new InputRandom(f, bound -> { f.check(bound == 500, "infection precedes DARK offset draws in the original tick order"); return 0; }, true)) {
                tick(); f.check(rng.bounds.equals(List.of(500)) && node.getNodeType() == NodeType.TAINTED,
                        "a DARK node infected on tick100 cannot execute its former DARK biome/spawn handler afterward");
            }

            paint(pos, ThaumcraftBiomes.MAGICAL_FOREST); paint(sampled, ThaumcraftBiomes.MAGICAL_FOREST);
            fresh(NodeType.PURE, false); phase49(); offsets(8, true);
            for (int tick = 0; tick < 49; tick++) tick();
            paint(pos, ThaumcraftBiomes.TAINT); offsets(8, true);
            f.check(node.getNodeType() == NodeType.PURE, "actual PURE node is immune to Taint infection and spends no bound500 draw");
        }
        void darkRoom() {
            for (BlockPos mutable : BlockPos.betweenClosed(pos.offset(-9, -3, -9), pos.offset(9, 8, 9))) {
                BlockPos target = mutable.immutable();
                boolean shell = Math.abs(target.getX() - pos.getX()) == 9 || Math.abs(target.getZ() - pos.getZ()) == 9
                        || target.getY() == pos.getY() - 3 || target.getY() == pos.getY() + 8;
                put(target, shell ? Blocks.BEDROCK : Blocks.AIR);
            }
            level.getServer().setDifficulty(Difficulty.HARD, true);
            fresh(NodeType.NORMAL, false); hard(false); // No ecological writes or spawns while asynchronous room lighting settles.
        }
        boolean darkRoomReady() {
            BlockPos spawn = pos.offset(4, 0, 4);
            return level.isPositionEntityTicking(spawn) && level.getBrightness(LightLayer.SKY, spawn) == 0
                    && level.getMaxLocalRawBrightness(spawn) == 0 && level.getBlockState(spawn.below()).isAir();
        }
        void darkSpawns() {
            f.check(darkRoomReady(),
                    "actual native room provides zero sky/block brightness and no supporting floor at the declared hostile spawn");
            for (Vec3 offset : List.of(new Vec3(-4, 0, -4), new Vec3(-4, 0, 4), new Vec3(4, 0, -4))) {
                var giant = ThaumcraftMonsters.GIANT_BRAINY_ZOMBIE.get().create(level);
                f.check(giant != null, "real registered Giant Brainy Zombie factory");
                giant.setPos(Vec3.atLowerCornerOf(pos).add(offset)); giant.setNoAi(true); giant.setNoGravity(true);
                f.check(level.addFreshEntity(giant), "three actual existing hostile owners are admitted"); mobs.add(giant);
            }
            fresh(NodeType.DARK, false); hard(true); // Earlier native lighting ticks legitimately advanced the waiting ordinary tile.
            phase49();
            try (var rng = new DarkSpawnInput(f, true)) {
                tick(); rng.assertDraws(true);
            }
            var giants = giants(); var born = giants.stream().filter(entity -> !mobs.contains(entity)).toList();
            f.check(giants.size() == 4 && born.size() == 1 && born.getFirst().position().equals(Vec3.atLowerCornerOf(pos).add(4.5, 0, 4.5))
                            && !born.getFirst().isBaby() && born.getFirst().getMainHandItem().isEmpty() && born.getFirst().getOffhandItem().isEmpty(),
                    "original <=3 cap admits one actual fourth Giant in unsupported air, using unfinalized native construction and exact positional draws");
            mobs.add(born.getFirst());
            fresh(NodeType.DARK, false); phase49();
            try (var rng = new DarkSpawnInput(f, false)) {
                tick(); rng.assertDraws(false);
            }
            f.check(giants().size() == 4, "four existing native Giants prevent a fifth before position/yaw RNG");
        }
        List<EntityGiantBrainyZombie> giants() { return level.getEntitiesOfClass(EntityGiantBrainyZombie.class, new AABB(pos).inflate(10, 6, 10)); }
        Map<Quart, Holder<Biome>> snapshot(LevelChunk chunk) {
            var result = new HashMap<Quart, Holder<Biome>>(); int x0 = QuartPos.fromBlock(chunk.getPos().getMinBlockX()), z0 = QuartPos.fromBlock(chunk.getPos().getMinBlockZ());
            for (int x = x0; x < x0 + 4; x++) for (int z = z0; z < z0 + 4; z++)
                for (int y = QuartPos.fromBlock(level.getMinBuildHeight()); y < QuartPos.fromBlock(level.getMaxBuildHeight()); y++)
                    result.put(new Quart(x, y, z), chunk.getNoiseBiome(x, y, z));
            return result;
        }
        @Override public void close() {
            if (closed) return; closed = true;
            try {
                mobs.forEach(Entity::discard); ThaumcraftSettings.configureServer(settings);
                NodeEcologyPolicy.dimensionBlacklist.clear(); NodeEcologyPolicy.dimensionBlacklist.putAll(oldDimensions);
                NodeEcologyPolicy.biomeBlacklist.clear(); NodeEcologyPolicy.biomeBlacklist.putAll(oldBiomePolicy);
                level.getServer().setDifficulty(difficulty, true); level.getServer().setDifficultyLocked(difficultyLocked);
                oldBiomes.forEach((owner, biomes) -> {
                    LevelChunk chunk = level.getChunk(owner.x, owner.z);
                    chunk.fillBiomesFromNoise((x, y, z, sampler) -> biomes.get(new Quart(x, y, z)), level.getChunkSource().randomState().sampler());
                    chunk.setUnsaved(true); level.getChunkSource().chunkMap.resendBiomesForChunks(List.of(chunk));
                });
                f.close();
            } finally { forced.forEach(owner -> level.setChunkForced(owner.x, owner.z, false)); }
        }
    }

    /** Literal original branch inputs; entity brightness RNG remains the Giant's own native source. */
    private static final class DarkSpawnInput implements AutoCloseable {
        final Fixture f; final Field field; final RandomSource original, input; final boolean expectPosition;
        final List<String> draws = new ArrayList<>(); int integers, doubles;
        DarkSpawnInput(Fixture f, boolean expectPosition) {
            this.f = f; this.expectPosition = expectPosition; original = f.level.random;
            input = (RandomSource) Proxy.newProxyInstance(RandomSource.class.getClassLoader(), new Class<?>[]{RandomSource.class}, (proxy, method, args) -> {
                if (method.getDeclaringClass() == Object.class) return switch (method.getName()) {
                    case "toString" -> "declared original DARK spawn branch inputs";
                    case "hashCode" -> System.identityHashCode(proxy); case "equals" -> proxy == args[0]; default -> throw new AssertionError(method);
                };
                if (method.getName().equals("nextInt") && args != null && args.length == 1) {
                    int bound = (int) args[0]; draws.add("int" + bound);
                    int index = integers++;
                    // The original DARK handler spends four nextInt12 offsets, the hard-node coin,
                    // the optional spawn position and only then the original champion join roll
                    // (oracle EventHandlerEntity.entitySpawns) of the Giant it just spawned.
                    f.check(index < 4 && bound == 12 || index == 4 && expectPosition && bound == 3
                                    || index == 5 && expectPosition && bound == 100,
                            "DARK consumes only four offset draws then optional spawn Y and the original champion join roll");
                    // The champion roll of the Giant stays far above the promotion threshold of the
                    // original, so the Giant keeps its ordinary form and spends one single draw.
                    return index == 0 ? 6 : index == 4 ? 1 : index == 5 ? 97 : 0;
                }
                draws.add(method.getName());
                return switch (method.getName()) {
                    case "nextBoolean" -> true;
                    case "nextDouble" -> { f.check(expectPosition && doubles < 4, "only four original DARK position doubles"); yield doubles++ % 2 == 0 ? .9D : 0D; }
                    case "nextFloat" -> .5F;
                    default -> throw new AssertionError("Unexpected DARK world draw " + method);
                };
            });
            try { field = Level.class.getDeclaredField("random"); field.setAccessible(true); }
            catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
            try { field.set(f.level, input); f.check(f.level.random == input, "scoped DARK branch input owns the actual native world RNG field"); }
            catch (IllegalAccessException | RuntimeException | Error failure) {
                try { field.set(f.level, original); } catch (IllegalAccessException restore) { failure.addSuppressed(restore); }
                throw new IllegalStateException(failure);
            }
        }
        void assertDraws(boolean position) {
            var expected = new ArrayList<>(List.of("int12", "int12", "int12", "int12", "nextBoolean"));
            if (position) expected.addAll(List.of("nextDouble", "nextDouble", "int3", "nextDouble", "nextDouble", "nextFloat", "int100"));
            f.check(draws.equals(expected), "exact original DARK conversion/coin/cap/position/yaw/champion draw order; actual=" + draws);
        }
        @Override public void close() {
            try { boolean owned = f.level.random == input; field.set(f.level, original); f.check(owned && f.level.random == original, "finally restores original world RNG identity"); }
            catch (IllegalAccessException failure) { throw new IllegalStateException(failure); }
        }
    }

    /** Exact node biome offsets, hard-node coin and native fibre TYPE0 lottery. */
    private static final class TaintNodeInput implements AutoCloseable {
        final Fixture f; final Field field; final RandomSource original, input;
        final List<String> draws = new ArrayList<>(); int integers;
        TaintNodeInput(Fixture f) {
            this.f = f; original = f.level.random;
            input = (RandomSource) Proxy.newProxyInstance(RandomSource.class.getClassLoader(), new Class<?>[]{RandomSource.class}, (proxy, method, args) -> {
                if (method.getDeclaringClass() == Object.class) return switch (method.getName()) {
                    case "toString" -> "declared original TAINTED node branch inputs";
                    case "hashCode" -> System.identityHashCode(proxy); case "equals" -> proxy == args[0]; default -> throw new AssertionError(method);
                };
                if (method.getName().equals("nextInt") && args != null && args.length == 1) {
                    int bound = (int) args[0], index = integers++; draws.add("int" + bound);
                    f.check(index < 4 && bound == 8 || index >= 4 && index < 10 && bound == 5 || index == 10 && bound == 10,
                            "TAINTED keeps four biome offsets, six x/z/y spread offsets and the actual fibre lottery bound");
                    return index == 0 ? 6 : index == 4 ? 2 : index == 10 ? 1 : 0;
                }
                draws.add(method.getName());
                if (method.getName().equals("nextBoolean")) return true;
                throw new AssertionError("Unexpected TAINTED node draw " + method);
            });
            try { field = Level.class.getDeclaredField("random"); field.setAccessible(true); }
            catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
            try { field.set(f.level, input); f.check(f.level.random == input, "scoped TAINTED input owns the native world RNG field"); }
            catch (IllegalAccessException | RuntimeException | Error failure) {
                try { field.set(f.level, original); } catch (IllegalAccessException restore) { failure.addSuppressed(restore); }
                throw new IllegalStateException(failure);
            }
        }
        void assertDraws() {
            f.check(draws.equals(List.of("int8", "int8", "int8", "int8", "nextBoolean", "int5", "int5", "int5", "int5", "int5", "int5", "int10")),
                    "actual node→native fibre path retains original conversion/coin/x-z-y/lottery order; actual=" + draws);
        }
        @Override public void close() {
            try { boolean owned = f.level.random == input; field.set(f.level, original); f.check(owned && f.level.random == original, "finally restores original world RNG after TAINTED branch input"); }
            catch (IllegalAccessException failure) { throw new IllegalStateException(failure); }
        }
    }
}
