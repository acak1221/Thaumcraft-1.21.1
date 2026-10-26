package thaumcraft.common.testing;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.BlockArcaneFurnace;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.tiles.LegacyTileRules;
import thaumcraft.common.tiles.TileArcaneFurnace;
import thaumcraft.common.tiles.TileArcaneFurnaceNozzle;
import thaumcraft.common.tiles.TileJar;

/**
 * The essentia nozzle of the arcane furnace on a real server (oracle TileArcaneFurnaceNozzle:18-135).
 *
 * <p>The nozzle under the core looks away from it, offers the suction 128 to a jar of fire essentia below
 * and draws one unit on every fifth tick while the furnace has less than sixty speedy ticks; every draw
 * gives the furnace six hundred speedy ticks. Until 2026-10-26 the nozzle had no ticker in the port: it
 * never found its core, never connected and never drew anything.</p>
 */
public final class ArcaneFurnaceNozzleRuntimeChecks {
    private ArcaneFurnaceNozzleRuntimeChecks() {}

    public static void nozzleFeedsTheFurnaceFireEssentia(GameTestHelper test) {
        var level = test.getLevel();
        var player = test.makeMockServerPlayerInLevel();
        var knowledge = KnowledgeAccess.get(player);
        knowledge.reset();
        knowledge.setInitializedAspects(true);
        knowledge.addResearch("INFERNALFURNACE");
        // The real furnace of the original: the shell formed by a wand (oracle WandManager:332-407).
        BlockPos base = new BlockPos(1, 3, 1);
        SurvivalProgressionRuntimeChecks.buildShell(test, base);
        ItemStack wand = new ItemStack(ThaumcraftContent.WAND.get());
        wand.set(ThaumcraftContent.WAND_DATA.get(), new WandData("primal_staff", "gold", Map.of())
                .withAmount("ignis", 10000).withAmount("terra", 10000));
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        test.assertTrue(ThaumcraftContent.WAND.get().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                SurvivalProgressionRuntimeChecks.hit(test.absolutePos(base)))).consumesAction(), "the wand forms the furnace");
        BlockPos core = test.absolutePos(base.offset(1, 1, 1)), nozzle = core.below(), jar = nozzle.below();
        test.assertTrue(level.getBlockEntity(core) instanceof TileArcaneFurnace
                        && level.getBlockState(nozzle).getValue(BlockArcaneFurnace.VARIANT) == 5
                        && level.getBlockEntity(nozzle) instanceof TileArcaneFurnaceNozzle,
                "the formed furnace has its core and the essentia nozzle under it");
        level.setBlock(jar, ThaumcraftContent.JAR.get().defaultBlockState(), Block.UPDATE_ALL);
        var source = (TileJar) level.getBlockEntity(jar);
        test.assertTrue(source.addToContainer(Aspect.FIRE, 8) == 0 && source.getAmount() == 8, "the jar under the nozzle holds eight fire");
        var furnace = (TileArcaneFurnace) level.getBlockEntity(core);
        var tile = (TileArcaneFurnaceNozzle) level.getBlockEntity(nozzle);
        test.runAfterDelay(12, () -> {
            test.assertTrue(tile.getFacing() == Direction.DOWN && tile.getFurnace() == furnace,
                    "the nozzle found its core and looks away from it: " + tile.getFacing());
            test.assertTrue(source.getAmount() == 7 && furnace.speedyTime >= LegacyTileRules.NOZZLE_SPEEDY_GRANT - 12,
                    "one fire essentia gave the furnace its six hundred speedy ticks: jar " + source.getAmount()
                            + ", speedy " + furnace.speedyTime);
            level.setBlock(jar, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            test.succeed();
        });
    }
}
