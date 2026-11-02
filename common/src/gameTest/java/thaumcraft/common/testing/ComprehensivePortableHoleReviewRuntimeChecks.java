package thaumcraft.common.testing;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.config.ThaumcraftHoles;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.lib.world.PortableHoles;
import thaumcraft.common.tiles.TileHole;

/** Owned fresh-world fixtures exercise the actual survival focus and native hole ticker. */
public final class ComprehensivePortableHoleReviewRuntimeChecks {
    private ComprehensivePortableHoleReviewRuntimeChecks() {}

    public static void creationRespectsWorldBorder(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            var border = f.level.getWorldBorder();
            double x = border.getCenterX(), z = border.getCenterZ(), size = border.getSize();
            try {
                f.put(f.base, Blocks.STONE);
                f.put(f.base.east(), Blocks.STONE);
                edge(border, f.base);
                f.check(border.isWithinBounds(f.base) && !border.isWithinBounds(f.base.east()),
                        "owned stone cells straddle the real native world-border plane");
                f.check(!PortableHoles.create(f.level, f.base.east(), -1, 1, 120)
                                && f.level.getBlockState(f.base.east()).is(Blocks.STONE),
                        "a hole cannot replace ordinary stone outside the actual configurable world border");
                f.check(PortableHoles.create(f.level, f.base, -1, 1, 120)
                                && f.level.getBlockEntity(f.base) instanceof TileHole,
                        "the same ordinary stone inside the border remains a supported portable-hole target");
            } finally {
                border.setCenter(x, z); border.setSize(size);
            }
        }
        test.succeed();
    }

    public static void focusRefusesBorderCrossingBeforePayment(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            var border = f.level.getWorldBorder();
            double x = border.getCenterX(), z = border.getCenterZ(), size = border.getSize();
            try {
                wall(f);
                edge(border, f.base);
                var wand = wand();
                f.player.setItemInHand(InteractionHand.MAIN_HAND, wand);
                f.aim(Vec3.atCenterOf(f.base));
                var hit = f.player.pick(f.player.blockInteractionRange(), 1, true);
                f.check(hit.getType() == HitResult.Type.BLOCK && hit instanceof net.minecraft.world.phys.BlockHitResult block
                                && block.getBlockPos().equals(f.base) && block.getDirection() == Direction.SOUTH
                                && border.isWithinBounds(f.base) && !border.isWithinBounds(f.base.east())
                                && f.level.mayInteract(f.player, f.base),
                        "an actual connected survival actor points at a permitted first block while its lateral ring crosses the border");
                ItemStack before = wand.copy();
                f.player.gameMode.useItem(f.player, f.level, wand, InteractionHand.MAIN_HAND);
                f.check(ItemStack.matches(wand, before) && f.level.getBlockState(f.base).is(Blocks.STONE)
                                && f.level.getBlockState(f.base.east()).is(Blocks.STONE),
                        "native focus activation refuses the disallowed 3x3 footprint before any material mutation or vis debit");
            } finally {
                border.setCenter(x, z); border.setSize(size);
            }
        }
        test.succeed();
    }

    public static void delayedSpreadRespectsChangedBorderAndRestores(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            var border = f.level.getWorldBorder();
            double x = border.getCenterX(), z = border.getCenterZ(), size = border.getSize();
            try {
                wall(f);
                f.check(PortableHoles.create(f.level, f.base, Direction.SOUTH.get3DDataValue(), 3, 120),
                        "initial hole and its saved chain were created while the footprint was permitted");
                edge(border, f.base);
                tick(f, f.base);
                f.check(f.level.getBlockState(f.base.east()).is(Blocks.STONE)
                                && f.level.getBlockState(f.base.north()).is(ThaumcraftHoles.HOLE_BLOCK.get()),
                        "the actual delayed hole ticker keeps permitted chain progress but cannot cut its outside-border ring");
                for (int index = 1; index < 120; index++) tick(f, f.base);
                f.check(f.level.getBlockState(f.base).is(Blocks.STONE),
                        "a boundary change never suppresses restoration of an already paid temporary hole");
            } finally {
                border.setCenter(x, z); border.setSize(size);
            }
        }
        test.succeed();
    }

    private static void edge(WorldBorder border, BlockPos source) {
        border.setCenter(source.getX() - 4, source.getZ() + .5);
        border.setSize(10); // maxX == source.x+1: source is inside, source.east is outside.
    }

    private static void wall(TubeVariantsRuntimeChecks.Fixture f) {
        for (int dz = -1; dz <= 0; dz++) for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++)
            f.put(f.base.offset(dx, dy, dz), Blocks.STONE);
    }

    private static ItemStack wand() {
        var wand = new ItemStack(ThaumcraftContent.WAND.get());
        wand.set(ThaumcraftContent.WAND_DATA.get(), new WandData("silverwood", "gold", Map.of("aer", 1000, "perditio", 1000)));
        wand.set(ThaumcraftFoci.WAND_FOCUS.get(), new FocusData(new ItemStack(ThaumcraftFoci.PORTABLE_HOLE_FOCUS.get())));
        return wand;
    }

    private static void tick(TubeVariantsRuntimeChecks.Fixture f, BlockPos pos) {
        var state = f.level.getBlockState(pos);
        var tile = f.level.getBlockEntity(pos);
        var ticker = state.getTicker(f.level, ThaumcraftHoles.HOLE_BLOCK_ENTITY.get());
        f.check(tile instanceof TileHole && ticker != null, "the actual registered native hole ticker remains available");
        ticker.tick(f.level, pos, state, (TileHole) tile);
    }
}
