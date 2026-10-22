package thaumcraft.common.testing;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.common.config.ThaumcraftAlembics;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftDistillation;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.items.wands.FocusVis;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileAlchemyFurnace;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TileNode;

/** Finite stock fixture; a real comparator callback attempts to export the newly credited unit. */
public final class VisEssentiaAccountingChecks {
    private VisEssentiaAccountingChecks() {}

    public static void primalNetwork(GameTestHelper test) {
        try (var f = NodeTransductionRuntimeChecks.fixture(test, false)) {
            f.open();
            var source = NodeTransductionRuntimeChecks.energize(f, f.base,
                    new AspectList().add(Aspect.AIR, 9).add(Aspect.EARTH, 4).add(Aspect.FIRE, 1), null, NodeType.NORMAL);
            NodeTransductionRuntimeChecks.sourceTick(f, source);
            f.check(source.getAvailableVis().visSize() == 6, "known raw stock: air3 earth2 fire1");
            f.check(AuraHelper.drainVis(f.level, f.base.east(), 4, true) == 4
                    && source.getAvailableVis().visSize() == 6, "simulation reports4, debits0");
            f.check(AuraHelper.drainVis(f.level, f.base.east(), 4, false) == 4
                    && source.getAvailableVis().getAmount(Aspect.AIR) == 0
                    && source.getAvailableVis().getAmount(Aspect.EARTH) == 1
                    && source.getAvailableVis().getAmount(Aspect.FIRE) == 1, "drain4 pays air3 then earth1; residual2");
            f.check(AuraHelper.drainVis(f.level, f.base.east(), 99, true) == 2
                    && source.getAvailableVis().visSize() == 2, "partial simulation leaves2 unchanged");
            f.check(AuraHelper.drainVis(f.level, f.base.east(), 99, false) == 2
                    && source.getAvailableVis().visSize() == 0
                    && AuraHelper.drainVis(f.level, f.base.east(), 1, false) == 0, "partial execution returns actual2, never requested99 or a second debit");
        }
        test.succeed();
    }

    public static void capsAndSpending(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            f.put(f.base, ThaumcraftContent.JAR.get());
            var jar = (TileJar) f.level.getBlockEntity(f.base);
            f.check(jar.addToContainer(Aspect.AIR, 63) == 0, "seed jar63");
            int remainder = jar.addToContainer(Aspect.AIR, 3);
            f.check(jar.getAmount() == 64 && remainder == 2, "normal cap: 63+offer3 = stored64+returned2");
            f.check(jar.addToContainer(Aspect.FIRE, 3) == 3 && jar.getAmount() == 64, "wrong aspect returns the whole offer");
            f.put(f.base.east(), ThaumcraftContent.VOID_JAR.get());
            var sink = (TileJar) f.level.getBlockEntity(f.base.east());
            sink.addToContainer(Aspect.AIR, 63);
            f.check(sink.addToContainer(Aspect.AIR, 3) == 0 && sink.getAmount() == 64, "original void sink destroys overflow2, not a conservation defect");
            f.put(f.base.above(), ThaumcraftContent.AURA_NODE.get());
            var node = (TileNode) f.level.getBlockEntity(f.base.above());
            node.setAspects(new AspectList().add(Aspect.AIR, 4));
            node.takeFromContainer(Aspect.AIR, 1);
            f.check(node.addToContainer(Aspect.AIR, 3) == 2 && node.getAspects().getAmount(Aspect.AIR) == 4,
                    "node cap: current3+offer3 = current4+returned2");
            var wand = new ItemStack(ThaumcraftContent.WAND.get());
            ItemWandCasting.withData(wand, new WandData("wood", "iron", Map.of("aer", 2499)));
            f.check(ItemWandCasting.addRealVis(wand, "aer", 100, false) == 99
                    && ItemWandCasting.getVis(wand, "aer") == 2499, "wand simulate: offer100 returns99, writes nothing");
            f.check(ItemWandCasting.addRealVis(wand, "aer", 100, true) == 99
                    && ItemWandCasting.getVis(wand, "aer") == 2500, "wand cap: 2499+100 =2500+99");
            f.check(FocusVis.debit(wand, Map.of("aer", 1)) && ItemWandCasting.getVis(wand, "aer") == 2499,
                    "focus raw price1 consumes exactly1 centivis");
            f.check(!FocusVis.debit(wand, Map.of("aer", 1, "ignis", 1)) && ItemWandCasting.getVis(wand, "aer") == 2499,
                    "unpayable second aspect cannot partially spend the first");
            f.check(ArcaneVis.debit(wand, Map.of("aer", 100)) && ItemWandCasting.getVis(wand, "aer") == 2399,
                    "arcane prepriced whole vis1 consumes exactly100 centivis");
            ItemWandCasting.withData(wand, new WandData("wood", "iron", Map.of("aer", 2499)));
            node.drainIntoWand(f.player, wand);
            f.check(node.getAspects().getAmount(Aspect.AIR) == 3 && ItemWandCasting.getVis(wand, "aer") == 2500,
                    "original tapping rounds leftover99/100 DOWN: node4->3, wand2499->2500, canonical loss99 centivis");
        }
        test.succeed();
    }

    public static void furnaceAlembicCallback(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            for (boolean typed : new boolean[] {false, true}) {
                f.reset();
                f.put(f.base, ThaumcraftDistillation.ALCHEMY_FURNACE.get());
                var furnace = (TileAlchemyFurnace) f.level.getBlockEntity(f.base);
                CompoundTag stock = furnace.saveWithoutMetadata(f.level.registryAccess());
                new AspectList().add(Aspect.AIR, 4).writeToNBT(stock);
                furnace.loadWithComponents(stock, f.level.registryAccess());
                f.put(f.base.above(), ThaumcraftAlembics.ALEMBIC.get());
                var alembic = (TileAlembic) f.level.getBlockEntity(f.base.above());
                if (typed) alembic.setFilter(Aspect.AIR);
                BlockPos comparator = f.base.above().east();
                f.put(comparator.below(), Blocks.STONE);
                f.level.setBlock(comparator, Blocks.COMPARATOR.defaultBlockState()
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST), TubeVariantsRuntimeChecks.QUIET);
                var probe = new ComparatorProbe(comparator, f.level.getBlockState(comparator));
                f.level.removeBlockEntity(comparator);
                f.level.setBlockEntity(probe);
                f.put(f.base.west(), ThaumcraftContent.JAR.get());
                var exported = (TileJar) f.level.getBlockEntity(f.base.west());
                probe.action = () -> {
                    if (alembic.takeFromContainer(Aspect.AIR, 1))
                        f.check(exported.addToContainer(Aspect.AIR, 1) == 0, "the extracted unit reaches a real jar");
                };
                f.check(furnace.getVis() == 4 && alembic.getAmount() == 0 && exported.getAmount() == 0,
                        "before: furnace4 + alembic0 + exported0 = 4");
                for (int tick = 0; tick < 40; tick++)
                    TileAlchemyFurnace.tick(f.level, f.base, furnace.getBlockState(), furnace);
                f.check(probe.calls == 1, "real native comparator queried exactly once during the furnace credit");
                int total = furnace.getVis() + alembic.getAmount() + exported.getAmount();
                f.check(total == 4, "conservation: furnace=" + furnace.getVis() + ", alembic=" + alembic.getAmount()
                        + ", exported=" + exported.getAmount() + "; expected total4, actual" + total);
                f.check(furnace.getVis() == 3, "one transferred unit is paid before notification, even if destination changes");
                f.check(alembic.getAmount() == 1 && exported.getAmount() == 0,
                        "paired endpoint lock defers reentrant extraction: after is exactly furnace3/alembic1/jar0");
                f.check(alembic.takeFromContainer(Aspect.AIR, 1) && exported.addToContainer(Aspect.AIR, 1) == 0
                        && furnace.getVis() == 3 && alembic.getAmount() == 0 && exported.getAmount() == 1,
                        "after notification unlock, the unit moves normally: furnace3/alembic0/jar1; total remains4");
            }
        }
        test.succeed();
    }

    private static final class ComparatorProbe extends ComparatorBlockEntity {
        Runnable action;
        int calls;
        ComparatorProbe(BlockPos pos, BlockState state) { super(pos, state); }
        @Override public int getOutputSignal() {
            if (action != null) { var once = action; action = null; calls++; once.run(); }
            return super.getOutputSignal();
        }
    }
}
