package thaumcraft.common.testing;

import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import thaumcraft.common.blocks.BlockArcaneBore;
import thaumcraft.common.blocks.BlockArcaneBoreBase;
import thaumcraft.common.config.ThaumcraftArcaneBore;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.container.ContainerArcaneBore;
import thaumcraft.common.lib.bore.BoreWork;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileArcaneBore;
import thaumcraft.common.tiles.TileArcaneBoreBase;

/** SERVER-only geometry/menu fixtures. No manual machine tick, finished loot,
 * boost credit or client-time evidence is supplied by these native checks. */
public final class BoreBlockRuntimeChecks {
    private BoreBlockRuntimeChecks() {}

    public static void extendedCollisionAndWandDirections(GameTestHelper test) {
        NodeStabilizerRuntimeChecks.requireLoadedFootprint(test);
        try (var f = new Fixture(test)) {
            f.open();
            for (Direction mining : Direction.values()) {
                f.reset();
                Direction attachment = mining == Direction.DOWN ? Direction.DOWN : Direction.UP;
                var topPos = f.base;
                var basePos = topPos.relative(attachment.getOpposite());
                f.put(basePos, ThaumcraftArcaneBore.ARCANE_BORE_BASE.get());
                f.put(topPos, ThaumcraftArcaneBore.ARCANE_BORE.get());
                var desired = f.level.getBlockState(topPos).setValue(BlockArcaneBore.ATTACHMENT, attachment).setValue(BlockArcaneBore.MINING, mining);
                f.level.setBlock(topPos, desired, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                var top = (TileArcaneBore) f.level.getBlockEntity(topPos);
                f.check(top.isCurrent() && top.attachment() == attachment && top.miningDirection() == mining,
                        "actual installed top, attachment independent from mining: " + mining);
                var outline = desired.getShape(f.level, topPos, CollisionContext.empty()).bounds();
                var collision = desired.getCollisionShape(f.level, topPos, CollisionContext.empty()).bounds();
                f.check(outline.equals(collision) && collision.getXsize() == (mining.getAxis() == Direction.Axis.X ? 2 : 1)
                        && collision.getYsize() == (mining.getAxis() == Direction.Axis.Y ? 2 : 1)
                        && collision.getZsize() == (mining.getAxis() == Direction.Axis.Z ? 2 : 1), "two-cell shape/collision, not decorative cube: " + mining);
                Vec3 probe = Vec3.atCenterOf(topPos).relative(mining, 1.25);
                var query = new AABB(probe.subtract(.04, .04, .04), probe.add(.04, .04, .04));
                f.check(f.level.getBlockState(topPos.relative(mining)).isAir(), "extension collision probe cell contains no second solid block");
                f.check(f.level.getBlockCollisions(null, query).iterator().hasNext(), "native neighbouring-cell collision traversal reaches extended Bore shape: " + mining);
                for (Direction side : Direction.values()) {
                    f.check(desired.isSignalSource() && desired.getSignal(f.level, topPos, side) == 0
                            && desired.getDirectSignal(f.level, topPos, side) == 0, "wire-connectable without generated power: " + side);
                }
                f.check(!BoreWork.nativeTickAllowed(top), "direct helper cannot manufacture native ticker origin");
                var before = top.saveWithoutMetadata(f.level.registryAccess());
                TileArcaneBore.tick(f.level, topPos, desired, top);
                f.check(before.equals(top.saveWithoutMetadata(f.level.registryAccess())) && top.getCountdown() == 0
                        && top.selectedTarget() == null, "manual public ticker cannot search, debit, rotate or grant output");
                f.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftContent.WAND.get()));
                var wand = f.player.getMainHandItem().copy();
                for (Direction side : Direction.values()) {
                    f.aim(Vec3.atCenterOf(topPos));
                    f.check(f.use(topPos, side, InteractionHand.MAIN_HAND).consumesAction() && top.miningDirection() == side
                            && top.attachment() == attachment, "real item-first wand selects clicked side and retains attachment: " + side);
                    f.check(ItemStack.matches(wand, f.player.getMainHandItem()), "zero-vis rotation preserves exact wand");
                }
                var base = (TileArcaneBoreBase) f.level.getBlockEntity(basePos);
                for (Direction side : Direction.values()) {
                    f.aim(Vec3.atCenterOf(basePos));
                    f.check(f.use(basePos, side, InteractionHand.MAIN_HAND).consumesAction()
                            && base.getBlockState().getValue(BlockArcaneBoreBase.NOZZLE) == side, "all6 native nozzle orientations independent from head");
                    f.check(ItemStack.matches(wand, f.player.getMainHandItem()), "nozzle rotation also costs0vis");
                }
            }
        }
        test.succeed();
    }

    public static void nativeMenuAndReload(GameTestHelper test) {
        NodeStabilizerRuntimeChecks.requireLoadedFootprint(test);
        try (var f = new Fixture(test)) {
            f.open(); f.put(f.base.below(), ThaumcraftArcaneBore.ARCANE_BORE_BASE.get()); f.put(f.base, ThaumcraftArcaneBore.ARCANE_BORE.get());
            var top = (TileArcaneBore) f.level.getBlockEntity(f.base); f.aim(Vec3.atCenterOf(f.base));
            f.check(f.use(f.base).consumesAction() && f.player.containerMenu instanceof ContainerArcaneBore, "actual empty-hand native menu opening");
            var menu = (ContainerArcaneBore) f.player.containerMenu;
            var focus = new ItemStack(ThaumcraftFoci.EXCAVATION_FOCUS.get());
            focus.set(DataComponents.CUSTOM_NAME, Component.literal("nativeBoreFocus"));
            var pick = new ItemStack(Items.WOODEN_PICKAXE); pick.setDamageValue(3);
            pick.set(DataComponents.CUSTOM_NAME, Component.literal("nativeBoreOperatingPick"));
            var expectedFocus = focus.copy(); var expectedPick = pick.copy();
            f.player.getInventory().setItem(9, focus); f.player.getInventory().setItem(10, pick);
            f.check(!menu.quickMoveStack(f.player, 2).isEmpty() && !menu.quickMoveStack(f.player, 3).isEmpty()
                    && f.player.getInventory().getItem(9).isEmpty() && f.player.getInventory().getItem(10).isEmpty()
                    && ItemStack.matches(top.getItem(0), expectedFocus) && ItemStack.matches(top.getItem(1), expectedPick), "real quick moves transfer full components to the2native slots");
            f.player.getInventory().setItem(11, new ItemStack(Items.STICK));
            f.check(menu.quickMoveStack(f.player, 4).isEmpty() && f.player.getInventory().getItem(11).is(Items.STICK), "unrelated input has no invented player-inventory shuffle");
            ItemStack escaped = top.getItem(1); escaped.setDamageValue(20);
            f.check(ItemStack.matches(top.getItem(1), expectedPick), "read getter cannot mutate installed pick through a copy");
            var saved = top.saveWithFullMetadata(f.level.registryAccess()); var old = top;
            top = NodeTransductionRuntimeChecks.reload(f, top, saved, TileArcaneBore.class);
            f.check(top.isCurrent() && !top.isQuarantined() && !old.lifetimeId().equals(top.lifetimeId())
                    && ItemStack.matches(top.getItem(0), expectedFocus) && ItemStack.matches(top.getItem(1), expectedPick), "actual native factory reload retains physical tools with fresh lifetime");
            f.check(!menu.stillValid(f.player) && menu.quickMoveStack(f.player, 0).isEmpty(), "old menu cannot debit replacement same-position owner");
            f.player.closeContainer(); f.aim(Vec3.atCenterOf(f.base));
            f.check(f.use(f.base).consumesAction() && f.player.containerMenu instanceof ContainerArcaneBore, "new owner opens fresh native menu");
            var currentMenu = (ContainerArcaneBore) f.player.containerMenu;
            f.check(!currentMenu.quickMoveStack(f.player, 0).isEmpty() && !currentMenu.quickMoveStack(f.player, 1).isEmpty()
                    && top.isEmpty(), "both actual tools extract once after reload without duplicate getter stock");
            f.check(f.player.getInventory().countItem(expectedFocus.getItem()) == 1 && f.player.getInventory().countItem(expectedPick.getItem()) == 1,
                    "one focus and operating pick remain physically owned");
            var retainedFocus = f.player.getInventory().items.stream().filter(stack -> stack.is(expectedFocus.getItem())).toList();
            var retainedPick = f.player.getInventory().items.stream().filter(stack -> stack.is(expectedPick.getItem())).toList();
            f.check(retainedFocus.size() == 1 && retainedPick.size() == 1
                    && ItemStack.matches(retainedFocus.getFirst(), expectedFocus) && ItemStack.matches(retainedPick.getFirst(), expectedPick),
                    "native extraction preserves complete names/components/counts and actual pick damage3, not merely item identities");
        }
        test.succeed();
    }
}
