package thaumcraft.common.entities.golems;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Pure rules of the Order upgrade's Harvest replanting (original AIHarvestCrops:185-245): the drop search box, the
 * nine planting cells, the vanilla seed family and the push that brings fresh drops toward the golem.
 * No world access; the guarded placement itself lives in {@link GolemHarvestPlatform}.
 */
public final class GolemReplant {
    /** {@code EntityUtils.getEntitiesInRange(.., 6.0)}: an axis-aligned box of this half-extent around the golem, not a sphere. */
    public static final double RANGE = 6.0;
    /** Original {@code xm}/{@code zm} (AIHarvestCrops:229-230): the harvested column first, then its eight neighbours. */
    private static final int[] DX = {0, 0, 1, 1, -1, 0, -1, -1, 1};
    private static final int[] DZ = {0, 1, 0, 1, 0, -1, -1, 1, -1};
    public static final int CELLS = DX.length;
    /** A drop younger than this many ticks is pushed (original {@code ticksExisted < 2}). */
    public static final int FRESH_TICKS = 2;

    // The original IPlantable / ItemSeedFood family. Cocoa beans have their own path; anything else is ignored.
    private static final Map<Item, Block> SEEDS = Map.of(
            Items.WHEAT_SEEDS, Blocks.WHEAT,
            Items.BEETROOT_SEEDS, Blocks.BEETROOTS,
            Items.MELON_SEEDS, Blocks.MELON_STEM,
            Items.PUMPKIN_SEEDS, Blocks.PUMPKIN_STEM,
            Items.CARROT, Blocks.CARROTS,
            Items.POTATO, Blocks.POTATOES,
            Items.NETHER_WART, Blocks.NETHER_WART);

    private GolemReplant() {}

    /** The crop block a stack plants, or null for anything outside the seed family (including cocoa beans). */
    public static Block seedBlock(ItemStack stack) {
        return stack == null || stack.isEmpty() ? null : SEEDS.get(stack.getItem());
    }

    /** The cell the {@code index}-th attempt plants into: the ground block one below the harvested position, offset sideways. */
    public static BlockPos ground(BlockPos harvested, int index) {
        return harvested.offset(DX[index], -1, DZ[index]);
    }

    /**
     * Original :201-205. The unit vector from the golem to the drop, scaled by -1/4 horizontally with a fixed
     * 0.075 lift: the push is TOWARD the golem. {@code Vec3.normalize} returns zero for an (almost) zero vector.
     */
    public static Vec3 nudge(Vec3 golem, Vec3 drop) {
        Vec3 direction = drop.subtract(golem).normalize();
        return new Vec3(-direction.x / 4.0, 0.075, -direction.z / 4.0);
    }
}
