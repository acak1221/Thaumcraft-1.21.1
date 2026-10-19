package thaumcraft.common.testing;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.crafting.InfusionRecipeInput;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.items.golems.GolemData;

/**
 * Lumber core on a real Iron golem in a bounded platform lane. The platform, trunk, golem
 * and its configuration are explicit INPUTS; scanning, walking, working, the guarded
 * removal, protection events and loot are never supplied. Observes the actual BREAK event
 * and the actor it sees. Not a save/reload, multiplayer, third-party-protection or visual test.
 */
public final class LumberGolemRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int RADIUS = 8, TRUNK = 5, DEADLINE = 1400;
    private LumberGolemRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        recipe(test);
        var level = test.getLevel();
        var base = test.absolutePos(new BlockPos(3, 3, 3));
        Map<BlockPos, BlockState> original = new LinkedHashMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(base.offset(-RADIUS, -1, -RADIUS), base.offset(RADIUS, 9, RADIUS))) {
            original.put(pos.immutable(), level.getBlockState(pos));
        }
        Set<Integer> existingEntities = new HashSet<>();
        level.getAllEntities().forEach(entity -> existingEntities.add(entity.getId()));
        // The golem's pathfinder refuses to plan across unloaded chunks: force the bounded
        // lane's neighbourhood, but only release chunks this test forced itself.
        List<ChunkPos> forced = new ArrayList<>();
        var center = new ChunkPos(base);
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
            var chunk = new ChunkPos(center.x + dx, center.z + dz);
            if (!level.getForcedChunks().contains(chunk.toLong())) { level.setChunkForced(chunk.x, chunk.z, true); forced.add(chunk); }
        }
        for (BlockPos pos : original.keySet()) level.setBlock(pos, pos.getY() == base.getY() - 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), QUIET);

        BlockPos trunk = base.offset(4, 0, 0);
        for (int index = 0; index < TRUNK; index++) level.setBlock(trunk.above(index), Blocks.OAK_LOG.defaultBlockState(), QUIET);
        List<String> breakActors = new ArrayList<>();
        List<BlockPos> breakPositions = new ArrayList<>();
        BlockEvent.Break listener = (world, pos, state, actor, xp) -> {
            if (actor != null && actor.getGameProfile().getName().equals("FakeThaumcraftGolem")) {
                breakActors.add(actor.getGameProfile().getName() + (actor.hasPermissions(2) ? ":op" : ":plain") + (actor.getInventory().isEmpty() ? ":empty" : ":holding"));
                breakPositions.add(pos.immutable());
            }
            return EventResult.pass();
        };
        BlockEvent.BREAK.register(listener);

        var golem = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), level);
        golem.setGolemMaterial(6);
        golem.applyConfiguration(new GolemData(GolemData.LUMBER, List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY)));
        golem.setPos(Vec3.atBottomCenterOf(base));
        golem.setHome(base, Direction.UP);
        golem.setPersistenceRequired();

        Runnable cleanup = () -> {
            BlockEvent.BREAK.unregister(listener);
            golem.discard();
            level.getEntitiesOfClass(ItemEntity.class, new AABB(Vec3.atLowerCornerOf(base.offset(-RADIUS - 2, -2, -RADIUS - 2)),
                    Vec3.atLowerCornerOf(base.offset(RADIUS + 3, 12, RADIUS + 3))), item -> !existingEntities.contains(item.getId())).forEach(Entity::discard);
            original.forEach((pos, state) -> level.setBlock(pos, state, QUIET));
            for (ChunkPos chunk : forced) level.setChunkForced(chunk.x, chunk.z, false);
        };
        boolean[] done = {false};
        try {
            test.assertTrue(level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS),
                    "native mobGriefing and block-drops rules are explicit inputs");
            test.assertTrue(level.addFreshEntity(golem) && golem.hasSupportedCore() && golem.getGolemCore() == GolemData.LUMBER,
                    "the Lumber core is a supported active worker on a real golem");
        } catch (RuntimeException failure) {
            done[0] = true;
            cleanup.run();
            throw failure;
        }
        test.onEachTick(() -> {
            if (done[0]) return;
            try {
                int remaining = 0;
                for (int index = 0; index < TRUNK; index++) if (level.getBlockState(trunk.above(index)).is(Blocks.OAK_LOG)) remaining++;
                long drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(Vec3.atLowerCornerOf(trunk.offset(-3, -2, -3)),
                        Vec3.atLowerCornerOf(trunk.offset(4, 8, 4))), item -> !existingEntities.contains(item.getId()) && item.getItem().is(Items.OAK_LOG))
                        .stream().mapToInt(item -> item.getItem().getCount()).sum();
                if (remaining >= TRUNK) return;
                // First cut observed: it must be the FAR end of the trunk, never the whole tree at once.
                test.assertTrue(remaining == TRUNK - 1 && !level.getBlockState(trunk.above(TRUNK - 1)).is(Blocks.OAK_LOG)
                                && level.getBlockState(trunk).is(Blocks.OAK_LOG),
                        "the golem removed exactly ONE log, the far top end, leaving the base standing: " + remaining + " left");
                test.assertTrue(drops == 1, "the cut dropped exactly one real oak log in the world, not into the golem: " + drops);
                test.assertTrue(golem.getCarriedItem().isEmpty(), "Lumber golems carry nothing");
                test.assertTrue(breakPositions.size() == 1 && breakPositions.get(0).equals(trunk.above(TRUNK - 1)),
                        "the protection BREAK event fired once, at the ACTUAL endpoint: " + breakPositions);
                test.assertTrue(breakActors.equals(List.of("FakeThaumcraftGolem:plain:empty")),
                        "...from the native plain, empty-handed machine actor: " + breakActors);
                done[0] = true;
                cleanup.run();
                test.succeed();
            } catch (RuntimeException failure) {
                done[0] = true;
                cleanup.run();
                throw failure;
            }
        });
        test.runAtTickTime(DEADLINE, () -> {
            if (done[0]) return;
            done[0] = true;
            cleanup.run();
            test.fail("the Lumber golem did not fell a log within " + DEADLINE + " ticks");
        });
    }

    /** Acquisition: Harvest core centre, Axe of the Stream + three pristine iron axes, meto (not messis). */
    private static void recipe(GameTestHelper test) {
        var level = test.getLevel();
        var holder = level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "infusion/golem_core_lumber"));
        test.assertTrue(holder.isPresent() && holder.get().value() instanceof InfusionStackRecipe, "Lumber core infusion recipe loaded");
        var recipe = (InfusionStackRecipe) holder.get().value();
        test.assertTrue(recipe.research().equals("CORELUMBER") && recipe.instability() == 2
                        && recipe.aspects().equals(Map.of("instrumentum", 16, "arbor", 16, "meto", 16))
                        && recipe.result().is(ThaumcraftGolems.CORE_LUMBER.get()) && recipe.inputDamage().equals(Optional.empty()),
                "original cost instrumentum16/arbor16/meto16 and instability 2");
        var center = new ItemStack(ThaumcraftGolems.CORE_HARVEST.get());
        var axe = new ItemStack(ThaumcraftEquipment.ELEMENTAL_AXE.get());
        var worn = new ItemStack(Items.IRON_AXE); worn.setDamageValue(1);
        test.assertTrue(recipe.matches(new InfusionRecipeInput(center, List.of(axe.copy(), new ItemStack(Items.IRON_AXE),
                        new ItemStack(Items.IRON_AXE), new ItemStack(Items.IRON_AXE))), level),
                "Harvest core + Axe of the Stream + three pristine iron axes is accepted");
        test.assertTrue(!recipe.matches(new InfusionRecipeInput(center, List.of(axe.copy(), worn.copy(),
                        new ItemStack(Items.IRON_AXE), new ItemStack(Items.IRON_AXE))), level),
                "a worn iron axe is refused");
        test.assertTrue(!recipe.matches(new InfusionRecipeInput(center, List.of(new ItemStack(Items.DIAMOND_AXE),
                        new ItemStack(Items.IRON_AXE), new ItemStack(Items.IRON_AXE), new ItemStack(Items.IRON_AXE))), level),
                "the Axe of the Stream cannot be replaced by another axe");
        var fishing = (InfusionStackRecipe) level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "infusion/golem_core_fishing")).orElseThrow().value();
        test.assertTrue(fishing.aspects().equals(Map.of("aqua", 16, "meto", 16, "bestia", 16)), "Fishing cost uses the original HARVEST aspect, meto");
    }
}
