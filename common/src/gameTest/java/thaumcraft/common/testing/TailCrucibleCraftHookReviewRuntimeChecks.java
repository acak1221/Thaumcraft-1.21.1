package thaumcraft.common.testing;

import dev.architectury.event.events.common.PlayerEvent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftAlchemy;
import thaumcraft.common.crafting.CrucibleCrafting;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.mixin.EntityLevelCallbackAccess;
import thaumcraft.common.tiles.TileCrucible;

/** Real live craft-output edits, native source removal and recursive queue dispatch. */
public final class TailCrucibleCraftHookReviewRuntimeChecks {
    private TailCrucibleCraftHookReviewRuntimeChecks() {}
    private static final String QUEUE_ID = "thaumcraft_pending_crucible_actions";
    // The real paid queue must already be cached when a callback observes it.
    // These deliberately unusable constructors cannot create or replace an owner.
    private static final SavedData.Factory<SavedData> OBSERVE_QUEUE = new SavedData.Factory<>(
            () -> { throw new IllegalStateException("Expected the live paid crucible queue"); },
            (tag, registries) -> { throw new IllegalStateException("Expected the cached paid crucible queue"); }, null);

    private enum Edit { NAMED_SPLIT, EMPTY, THROW_AFTER_SPLIT, THROW_INVALID_MAX }

    public static void craftHooksKeepLiveOutputUntilCallbacksFinish(GameTestHelper test) {
        var failures = new ArrayList<String>();
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open(); KnowledgeAccess.get(f.player).addResearch("ENTROPICPROCESSING");
            for (Edit edit : Edit.values()) runCase(f, edit, failures);
        }
        test.assertTrue(failures.isEmpty(), "native mutable crucible CRAFT contract and callback custody: " + failures);
        test.succeed();
    }

    private static void runCase(TubeVariantsRuntimeChecks.Fixture f, Edit edit, List<String> failures) {
        f.reset(); f.put(f.base, ThaumcraftAlchemy.CRUCIBLE.get());
        var tile = (TileCrucible) f.level.getBlockEntity(f.base);
        var initial = tile.saveWithoutMetadata(f.level.registryAccess());
        initial.putShort("Heat", (short) 200); initial.putInt("Water", 1000);
        new AspectList().add(Aspect.ENTROPY, 1).writeToNBT(initial);
        tile.loadWithComponents(initial, f.level.registryAccess());
        var catalyst = new ItemEntity(f.level, f.base.getX() + .5, f.base.getY() + .4,
                f.base.getZ() + .5, new ItemStack(Items.BONE));
        catalyst.setThrower(f.player); catalyst.setDeltaMovement(Vec3.ZERO);
        f.check(f.level.addFreshEntity(catalyst), "actual native hook catalyst admitted");
        var recipe = CrucibleCrafting.find(f.level, catalyst, tile.getAspects());
        f.check(recipe != null && recipe.id().getPath().equals("crucible/alt_bonemeal"), "loaded canonical bone recipe selected");

        int[] removals = {0}, crafts = {0};
        var marker = new IllegalStateException("owned craft-hook observer failure after mutation");
        String name = "tail-live-crucible-" + edit;
        long seed = 0x57431aL + edit.ordinal();
        var reference = RandomSource.create(seed);
        float expectedHookRoll = reference.nextFloat();
        double secondX = 0, secondZ = 0;
        if (edit == Edit.NAMED_SPLIT || edit == Edit.THROW_AFTER_SPLIT) {
            secondX = (reference.nextFloat() - reference.nextFloat()) * .01F;
            secondZ = (reference.nextFloat() - reference.nextFloat()) * .01F;
        }
        float expectedNextRoll = reference.nextFloat();
        var nativeCallback = ((EntityLevelCallbackAccess) catalyst).thaumcraft$getLevelCallback();
        catalyst.setLevelCallback(new EntityInLevelCallback() {
            @Override public void onMove() { nativeCallback.onMove(); }
            @Override public void onRemove(Entity.RemovalReason reason) {
                removals[0]++;
                if (crafts[0] != 0) failures.add(edit + " native REMOVE must precede CRAFT");
                observeFallback(f, edit + " REMOVE before recursive dispatch", failures);
                TileCrucible.tickOutputs(f.level);
                observeFallback(f, edit + " REMOVE after recursive dispatch", failures);
                nativeCallback.onRemove(reason);
            }
        });
        PlayerEvent.CraftItem listener = (actor, output, matrix) -> {
            if (actor != f.player) return;
            crafts[0]++;
            if (removals[0] != 1 || !catalyst.isRemoved() || tile.getWaterAmount() != 950 || tile.tagAmount() != 0)
                failures.add(edit + " CRAFT must observe exactly one already-paid catalyst removal");
            if (!output.is(Items.BONE_MEAL) || output.getCount() != 4 || !matrix.getItem(0).is(Items.BONE)
                    || matrix.getItem(0).getCount() != 1) failures.add(edit + " original live output and pre-consumption matrix missing");
            observeFallback(f, edit + " CRAFT before mutation", failures);
            float hookRoll = f.level.random.nextFloat();
            if (hookRoll != expectedHookRoll) failures.add(edit + " fallback preparation consumed the hook's world RNG draw");
            output.set(DataComponents.CUSTOM_NAME, Component.literal(name));
            output.setCount(edit == Edit.EMPTY ? 0 : 128);
            if (edit == Edit.THROW_INVALID_MAX) output.set(DataComponents.MAX_STACK_SIZE, 0);
            TileCrucible.tickOutputs(f.level);
            observeFallback(f, edit + " CRAFT after live mutation and recursive dispatch", failures);
            if (edit == Edit.THROW_AFTER_SPLIT || edit == Edit.THROW_INVALID_MAX) throw marker;
        };
        PlayerEvent.CRAFT_ITEM.register(listener);
        Throwable escaped = null;
        try {
            f.level.random.setSeed(seed);
            try { f.level.getBlockState(f.base).entityInside(f.level, f.base, catalyst); }
            catch (RuntimeException | Error failure) { escaped = failure; }
            if ((edit == Edit.THROW_AFTER_SPLIT || edit == Edit.THROW_INVALID_MAX) != causedBy(escaped, marker))
                failures.add(edit + " unexpected hook exception outcome: " + escaped);
            if (edit == Edit.THROW_INVALID_MAX && (marker.getSuppressed().length != 1
                    || !(marker.getSuppressed()[0] instanceof IllegalArgumentException)))
                failures.add(edit + " original hook failure must retain the output-planning failure as suppressed");
            TileCrucible.tickOutputs(f.level);
            var outputs = physical(f);
            int count = outputs.stream().mapToInt(item -> item.getItem().getCount()).sum();
            int expected = edit == Edit.EMPTY ? 0 : edit == Edit.THROW_INVALID_MAX ? 4 : 128;
            if (removals[0] != 1 || crafts[0] != 1 || !catalyst.isRemoved() || count != expected)
                failures.add(edit + " final paid output/removal mismatch: remove=" + removals[0] + ", craft=" + crafts[0] + ", count=" + count);
            if (edit == Edit.THROW_INVALID_MAX) {
                if (outputs.size() != 1 || outputs.getFirst().getItem().get(DataComponents.CUSTOM_NAME) != null
                        || outputs.getFirst().getItem().getCount() != 4)
                    failures.add(edit + " malformed edited output must retain the original valid unnamed four-item fallback");
            } else if (outputs.stream().anyMatch(item -> !name.equals(item.getItem().getHoverName().getString()) || item.getItem().getCount() != 64))
                failures.add(edit + " event name/count changes did not reach canonical64-item split outputs");
            if ((edit == Edit.NAMED_SPLIT || edit == Edit.THROW_AFTER_SPLIT) && outputs.size() == 2) {
                final double expectedX = secondX, expectedZ = secondZ;
                long first = outputs.stream().filter(item -> item.getDeltaMovement().equals(new Vec3(0, .1F, 0))).count();
                long second = outputs.stream().filter(item -> item.getDeltaMovement().equals(new Vec3(expectedX, .1F, expectedZ))).count();
                if (first != 1 || second != 1) failures.add(edit + " original post-hook native split momentum was changed");
            }
            if (!ownedRows(f).isEmpty()) failures.add(edit + " completed result leaves an extra fallback owner");
            if (f.level.random.nextFloat() != expectedNextRoll) failures.add(edit + " output splits must consume world RNG after the hook");
        } finally {
            PlayerEvent.CRAFT_ITEM.unregister(listener);
            if (!catalyst.isRemoved()) { catalyst.setLevelCallback(nativeCallback); catalyst.discard(); }
            TileCrucible.tickOutputs(f.level); f.drops();
        }
    }

    private static boolean causedBy(Throwable failure, Throwable expected) {
        for (Throwable cursor = failure; cursor != null; cursor = cursor.getCause()) if (cursor == expected) return true;
        return false;
    }

    private static void observeFallback(TubeVariantsRuntimeChecks.Fixture f, String phase, List<String> failures) {
        var rows = ownedRows(f);
        int count = rows.stream().mapToInt(row -> row.getInt("Count")).sum();
        boolean unnamed = rows.stream().allMatch(row -> ItemStack.parse(f.level.registryAccess(), row.getCompound("Stack"))
                .orElse(ItemStack.EMPTY).get(DataComponents.CUSTOM_NAME) == null);
        if (rows.size() != 1 || count != 4 || !unnamed || !physical(f).isEmpty())
            failures.add(phase + " must retain the initial paid fallback without emitting it; rows=" + rows.size()
                    + ", count=" + count + ", unnamed=" + unnamed + ", physical=" + physical(f).stream().map(item -> item.getItem().toString()).toList());
    }

    private static List<CompoundTag> ownedRows(TubeVariantsRuntimeChecks.Fixture f) {
        var queue = f.level.getDataStorage().get(OBSERVE_QUEUE, QUEUE_ID);
        if (queue == null) return List.of();
        var rows = new ArrayList<CompoundTag>();
        for (Tag raw : queue.save(new CompoundTag(), f.level.registryAccess()).getList("Outputs", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag) raw;
            if (row.getLong("Source") == f.base.asLong()) rows.add(row.copy());
        }
        return rows;
    }

    private static List<ItemEntity> physical(TubeVariantsRuntimeChecks.Fixture f) {
        return f.drops().stream().filter(item -> item.getItem().is(Items.BONE_MEAL)).toList();
    }
}
