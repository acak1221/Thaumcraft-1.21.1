package thaumcraft.common.testing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftNodes;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.world.ThaumcraftNormalNodeFeature;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.api.nodes.NodeModifier;

/** One native runtime check; loader registration and execution belong to the root session. */
public final class NaturalNodeRuntimeChecks {
    private NaturalNodeRuntimeChecks() {}

    public static void run(GameTestHelper test, ServerPlayer player) {
        var level = test.getLevel();
        var registries = level.registryAccess();
        test.assertTrue(registries.registryOrThrow(Registries.PLACED_FEATURE).containsKey(ThaumcraftNodes.NODE_KEY),
                "natural node placed feature is registered from bundled data");
        BlockPos relative = new BlockPos(2, 2, 2);
        BlockPos pos = test.absolutePos(relative);
        test.setBlock(relative, Blocks.AIR);
        // Fixed seed, no retry/reroll: exercise the ACTUAL original wild-node
        // initialization branch, not the creative block's 25-each constructor.
        test.assertTrue(ThaumcraftNormalNodeFeature.createRandomNodeAt(level, pos, RandomSource.create(0)),
                "fixed naturally rolled NORMAL node places in an Overworld biome");
        TileNode node = test.getBlockEntity(relative);
        test.assertTrue(node.isNatural() && node.getNodeType() == NodeType.NORMAL
                        && node.getNodeModifier() == null, "natural normal state replaces creative fixture");
        AspectList initial = node.getAspects();
        test.assertTrue(initial.getPrimalAspects().length > 0, "generated seed has wand-compatible vis");
        Aspect drained = initial.getPrimalAspects()[0];
        int capacity = node.getNodeVisBase(drained);
        test.assertTrue(capacity > 1 && initial.size() < 6, "actual spread, not six creative primals");
        for (Aspect aspect : initial.getAspects()) {
            test.assertValueEqual(node.getNodeVisBase(aspect), initial.getAmount(aspect), "generated current starts at base");
        }
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(pos.getX() + 1.5, pos.getY(), pos.getZ() + .5);
        ItemStack wand = new ItemStack(ThaumcraftContent.WAND.get());
        WandData wandData = WandData.EMPTY;
        for (Aspect aspect : Aspect.getPrimalAspects()) {
            wandData = wandData.withAmount(aspect.getTag(), aspect == drained ? 2450 : 2500);
        }
        wand.set(ThaumcraftContent.WAND_DATA.get(), wandData);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        node.drainIntoWand(player, wand);
        test.assertValueEqual(ItemWandCasting.getVis(wand, drained.getTag()), 2500, "natural node charges survival wand");
        test.assertValueEqual(node.getAspects().getAmount(drained), capacity - 1, "final partial centivis costs a whole node unit");
        test.assertValueEqual(node.getNodeVisBase(drained), capacity, "drain does not immediately reduce capacity");

        String scanId = node.getScanId();
        var saved = node.saveWithId(registries);
        test.setBlock(relative, Blocks.AIR);
        test.setBlock(relative, ThaumcraftContent.AURA_NODE.get());
        TileNode restored = test.getBlockEntity(relative);
        restored.loadWithComponents(saved, registries);
        test.assertValueEqual(restored.getScanId(), scanId, "scan identity survives real block entity replacement");
        test.assertTrue(restored.isNatural(), "natural origin persists");
        test.assertValueEqual(restored.getAspects().getAmount(drained), capacity - 1, "current vis persists separately");
        test.assertValueEqual(restored.getNodeVisBase(drained), capacity, "capacity persists separately");
        TileNode clientCopy = new TileNode(ThaumcraftContent.NODE_BLOCK_ENTITY.get(), pos, restored.getBlockState());
        clientCopy.loadWithComponents(restored.getUpdateTag(registries), registries);
        test.assertValueEqual(clientCopy.getScanId(), scanId, "update tag carries stable scan identity");
        test.assertValueEqual(clientCopy.getAspects().getAmount(drained), capacity - 1, "update tag carries current vis");
        test.assertValueEqual(clientCopy.getNodeVisBase(drained), capacity, "update tag carries separate capacity");
        tick(restored, 599);
        test.assertValueEqual(restored.getAspects().getAmount(drained), capacity - 1, "no refill before 600 normal ticks");
        tick(restored, 1);
        test.assertValueEqual(restored.getAspects().getAmount(drained), capacity, "one normal regeneration unit after 600 ticks");

        // Legitimate compound/zero entries and modifier flags must also survive
        // persistence. FADING disables recharge but not original zero pruning.
        restored.setAspects(new AspectList().add(Aspect.AIR, 1).add(Aspect.PLANT, 7));
        test.assertValueEqual(restored.takeVis("aer", 1), 1, "drain to zero");
        test.assertTrue(restored.getAspects().aspects.containsKey(Aspect.AIR), "zero entry remains until original prune");
        test.assertValueEqual(restored.takeVis("herba", 1), 0, "wand API does not consume compound aspects");
        var depleted = restored.saveWithId(registries);
        depleted.putByte("modifier", (byte)NodeModifier.FADING.ordinal());
        restored.loadWithComponents(depleted, registries);
        test.assertTrue(restored.getNodeModifier() == NodeModifier.FADING, "modifier survives persistence");
        test.assertValueEqual(restored.getAspects().getAmount(Aspect.PLANT), 7, "compound is not discarded on load");
        test.assertValueEqual(restored.getNodeVisBase(Aspect.PLANT), 7, "compound base is not discarded");
        tick(restored, 1199);
        test.assertTrue(restored.getAspects().aspects.containsKey(Aspect.AIR), "zero survives until tick 1200");
        tick(restored, 1);
        test.assertValueEqual(restored.getNodeVisBase(Aspect.AIR), 0, "zero pruning permanently loses one base");
        test.assertTrue(!restored.getAspects().aspects.containsKey(Aspect.AIR), "exhausted base removes current tag");
        test.assertValueEqual(restored.getAspects().getAmount(Aspect.PLANT), 7, "pruning conserves undrained compound");
        restored.setAspects(new AspectList().add(Aspect.AIR, 1));
        restored.takeVis("aer", 1);
        restored.loadWithComponents(restored.saveWithId(registries), registries);
        tick(restored, 1200);
        test.assertTrue(level.isEmptyBlock(pos), "last exhausted aspect kills the node without a free item");
        test.succeed();
    }

    private static void tick(TileNode node, int count) {
        for (int i = 0; i < count; i++) TileNode.serverTick(node.getLevel(), node.getBlockPos(), node.getBlockState(), node);
    }
}
