package thaumcraft.common.testing;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftAlembics;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.items.ItemJar;
import thaumcraft.common.items.ItemJarLabel;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileAlembic;

/** Isolated SERVER input fixture only, not live client source acquisition. */
public final class BoreClientPrerequisiteRuntimeChecks {
    private BoreClientPrerequisiteRuntimeChecks() {}

    public static void nativeChunkEnvelopeIsNotLiveOwnerTag(GameTestHelper test) {
        NodeStabilizerRuntimeChecks.requireLoadedFootprint(test);
        try(var f=new Fixture(test)) {
            f.open();f.put(f.base,ThaumcraftArcane.ARCANE_WORKBENCH.get());
            var tile=(thaumcraft.common.tiles.TileArcaneWorkbench)f.level.getBlockEntity(f.base);
            tile.setItem(0,new ItemStack(Items.IRON_INGOT,2));
            var direct=tile.saveWithFullMetadata(f.level.registryAccess());
            var chunk=f.level.getChunkAt(f.base).getBlockEntityNbtForSaving(f.base,f.level.registryAccess());
            f.check(chunk!=null && chunk.contains("keepPacked",net.minecraft.nbt.Tag.TAG_BYTE) && !chunk.getBoolean("keepPacked")
                    && !direct.contains("keepPacked"),"actual vanilla chunk adds false keepPacked envelope absent from the live owner's full tag");
            var compared=chunk.copy();compared.remove("keepPacked");
            f.check(compared.equals(direct) && tile.getItem(0).is(Items.IRON_INGOT) && tile.getItem(0).getCount()==2,
                    "ONLY false outer envelope differs; real slot counts, components, type and coordinates remain byte-typed equal");
            f.check(tile.saveWithFullMetadata(f.level.registryAccess()).equals(direct) && chunk.contains("keepPacked"),
                    "native reads and detached normalization do not mutate owner inventory or the chunk tag");
        }
        test.succeed();
    }

    public static void heldJarRemovesLabelWithOccupiedOffhand(GameTestHelper test) {
        NodeStabilizerRuntimeChecks.requireLoadedFootprint(test);
        try (var f = new Fixture(test)) {
            f.open(); f.put(f.base, ThaumcraftAlembics.ALEMBIC.get()); f.aim(Vec3.atCenterOf(f.base));
            var alembic = (TileAlembic) f.level.getBlockEntity(f.base);
            f.check(alembic.addToContainer(Aspect.METAL, 1) == 0, "declared one-unit native test input, never a live client grant");
            f.player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.APPLE));
            ItemStack offhand = f.player.getOffhandItem().copy();
            f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemJarLabel.blank());
            f.check(f.use(f.base).consumesAction() && alembic.getFilter() == Aspect.METAL
                    && f.player.getMainHandItem().isEmpty(), "native blank label binds the actual nonempty metal buffer once");
            f.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftContent.JAR.get()));
            f.check(f.use(f.base).consumesAction() && alembic.getAmount() == 0 && alembic.getFilter() == Aspect.METAL
                    && ItemJar.getAmount(f.player.getMainHandItem()) == 1 && ItemJar.getAspect(f.player.getMainHandItem()) == Aspect.METAL,
                    "actual jar transaction acquires the test unit, leaving the typed filter and empty buffer");
            ItemStack retainedJar = f.player.getMainHandItem(); ItemStack expectedJar = retainedJar.copy();
            f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            f.player.setShiftKeyDown(true);
            f.check(f.use(f.base) == InteractionResult.PASS && alembic.getFilter() == Aspect.METAL && f.pendingRows().isEmpty(),
                    "native secondary-use with occupied offhand really skips empty-main block interaction");
            f.player.setItemInHand(InteractionHand.MAIN_HAND, retainedJar);
            f.check(f.use(f.base).consumesAction() && alembic.getFilter() == null && alembic.getAmount() == 0
                    && ItemStack.matches(f.player.getMainHandItem(), expectedJar) && ItemStack.matches(f.player.getOffhandItem(), offhand),
                    "native ItemJar dispatch removes ONLY the filter; both exact held contents and the apple survive");
            f.player.setShiftKeyDown(false); f.flushFresh();
            var drops = f.drops();
            f.check(drops.size() == 1 && ItemStack.matches(drops.getFirst().getItem(), ItemJarLabel.blank()),
                    "one actual physical blank-label return, no extra vessel, essence clearing or fabricated result");
        }
        test.succeed();
    }
}
