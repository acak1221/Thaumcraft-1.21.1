package thaumcraft.common.testing;

import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftAlchemy;
import thaumcraft.common.crafting.CrucibleCrafting;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.tiles.TileCrucible;

/** Loaded alt_bonemeal recipe, one real catalyst and a scoped actual crafting callback. */
public final class TailCrucibleCraftRuntimeChecks {
    private TailCrucibleCraftRuntimeChecks() {}

    public static void craftCallbackReplacementDoesNotLoseCatalystOutput(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open(); KnowledgeAccess.get(f.player).addResearch("ENTROPICPROCESSING");
            for (boolean replace : new boolean[]{false, true}) {
                f.reset(); f.put(f.base, ThaumcraftAlchemy.CRUCIBLE.get());
                var tile = (TileCrucible) f.level.getBlockEntity(f.base);
                var inputState = tile.saveWithoutMetadata(f.level.registryAccess());
                inputState.putShort("Heat", (short) 200); inputState.putInt("Water", 1000);
                new AspectList().add(Aspect.ENTROPY, 1).writeToNBT(inputState);
                tile.loadWithComponents(inputState, f.level.registryAccess());
                var catalyst = new ItemEntity(f.level, f.base.getX() + .5, f.base.getY() + .4,
                        f.base.getZ() + .5, new ItemStack(Items.BONE));
                catalyst.setDeltaMovement(Vec3.ZERO); catalyst.setThrower(f.player);
                test.assertTrue(f.level.addFreshEntity(catalyst), "actual catalyst entity admitted");
                var selected = CrucibleCrafting.find(f.level, catalyst, tile.getAspects());
                test.assertTrue(selected != null && selected.id().getPath().equals("crucible/alt_bonemeal"),
                        "the loaded recipe manager selects canonical one-perditio bone to four bonemeal recipe");
                int[] callbacks = {0};
                PlayerEvent.CraftItem listener = (actor, output, matrix) -> {
                    if (actor != f.player) return;
                    callbacks[0]++;
                    test.assertTrue(output.is(Items.BONE_MEAL) && output.getCount() == 4, "real craft event carries canonical output");
                    if (replace) f.level.setBlock(f.base, Blocks.STONE.defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
                };
                PlayerEvent.CRAFT_ITEM.register(listener);
                try {
                    f.level.getBlockState(f.base).entityInside(f.level, f.base, catalyst);
                    TileCrucible.tickOutputs(f.level);
                    int physical = f.drops().stream().filter(item -> item.getItem().is(Items.BONE_MEAL))
                            .mapToInt(item -> item.getItem().getCount()).sum();
                    test.assertTrue(callbacks[0] == 1 && catalyst.isRemoved() && physical == 4
                                    && (!replace || f.level.getBlockState(f.base).is(Blocks.STONE)),
                            "one real craft retains all four paid outputs across callback replacement; replace=" + replace
                                    + ", callbacks=" + callbacks[0] + ", consumed=" + catalyst.isRemoved()
                                    + ", physical=" + physical + ", pending=" + TileCrucible.pendingOutputCount(f.level));
                } finally {
                    PlayerEvent.CRAFT_ITEM.unregister(listener);
                    catalyst.discard(); TileCrucible.tickOutputs(f.level); f.drops();
                }
            }
            preparationReplacementKeepsUnpaidCatalyst(f);
        }
        test.succeed();
    }

    /** A real addon preparation hook replaces the owner before any payment or craft event. */
    private static void preparationReplacementKeepsUnpaidCatalyst(TubeVariantsRuntimeChecks.Fixture f) {
        f.reset(); f.put(f.base, ThaumcraftAlchemy.CRUCIBLE.get());
        var original = (TileCrucible) f.level.getBlockEntity(f.base);
        var state = original.saveWithoutMetadata(f.level.registryAccess());
        state.putShort("Heat", (short) 200); state.putInt("Water", 1000);
        new AspectList().add(Aspect.ENTROPY, 1).writeToNBT(state);
        original.loadWithComponents(state, f.level.registryAccess());
        int[] preparations = {0}, events = {0};
        var recipe = new thaumcraft.api.crafting.CrucibleRecipe("ENTROPICPROCESSING", new ItemStack(Items.EMERALD),
                Items.PHANTOM_MEMBRANE, new AspectList().add(Aspect.ENTROPY, 1)) {
            @Override public AspectList removeMatching(AspectList contents) {
                preparations[0]++;
                f.level.setBlock(f.base, Blocks.AIR.defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
                f.level.setBlock(f.base, ThaumcraftAlchemy.CRUCIBLE.get().defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
                var foreign = (TileCrucible) f.level.getBlockEntity(f.base);
                f.check(foreign != null && foreign != original && foreign.fillWater(37, true) == 37,
                        "real preparation callback installs a different native crucible owner");
                return super.removeMatching(contents);
            }
        };
        ThaumcraftApi.registerCraftingRecipe(recipe);
        PlayerEvent.CraftItem listener = (actor, output, matrix) -> { if (actor == f.player && output.is(Items.EMERALD)) events[0]++; };
        PlayerEvent.CRAFT_ITEM.register(listener);
        var catalyst = new ItemEntity(f.level, f.base.getX() + .5, f.base.getY() + .4,
                f.base.getZ() + .5, new ItemStack(Items.PHANTOM_MEMBRANE));
        catalyst.setThrower(f.player); catalyst.setDeltaMovement(Vec3.ZERO);
        f.check(f.level.addFreshEntity(catalyst), "unpaid addon catalyst admitted");
        int pending = TileCrucible.pendingOutputCount(f.level);
        try {
            f.level.getBlockState(f.base).entityInside(f.level, f.base, catalyst);
            var foreign = (TileCrucible) f.level.getBlockEntity(f.base);
            f.check(preparations[0] == 1 && events[0] == 0 && !catalyst.isRemoved() && catalyst.getItem().getCount() == 1
                            && foreign != original && foreign.getWaterAmount() == 37 && foreign.tagAmount() == 0
                            && TileCrucible.pendingOutputCount(f.level) == pending,
                    "prepayment owner replacement cannot consume the unpaid catalyst, publish an output or charge the foreign BE");
        } finally {
            PlayerEvent.CRAFT_ITEM.unregister(listener); ThaumcraftApi.unregisterCraftingRecipe(recipe);
            catalyst.discard(); TileCrucible.tickOutputs(f.level); f.drops();
        }
    }
}
