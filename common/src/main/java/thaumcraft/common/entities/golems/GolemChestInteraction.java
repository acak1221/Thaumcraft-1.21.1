package thaumcraft.common.entities.golems;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/**
 * The chest interaction of a golem transfer.
 *
 * <p>The original opened the chest of a transfer for five ticks and closed it again: every goal of
 * the inventory ladder called {@code IInventory.func_70295_k_()} (the open) when it started and
 * {@code IInventory.func_70305_f()} (the close) when it stopped
 * (TC4 4.2.3.5 oracle, {@code thaumcraft/common/entities/ai/inventory/AIHomePlace.java:69-78} and
 * {@code …/AIEmptyPlace.java:83-92}), and the switch was the configuration
 * {@code Config.golemChestInteract} (default true, oracle
 * {@code thaumcraft/common/config/Config.java:475,871}).</p>
 *
 * <p>Only a chest answered in 1.7.10: the vanilla {@code IInventory} declares the two calls as empty
 * methods and the vanilla {@code TileEntityChest} is the only inventory that overrides them — it
 * counts one more viewer, animates its lid and plays {@code random.chestopen} and
 * {@code random.chestclosed} at the volume 0.5 and the pitch 0.9 to 1.0.</p>
 *
 * <p><b>The 1.21.1 difference.</b> The lid of a 1.21.1 chest is animated by the number of
 * <b>players</b> that have its menu open ({@code ChestBlockEntity.getOpenCount}), so a mob can never
 * animate it. The audible part of the original's interaction is reproduced exactly: the chest-open
 * sound at the moment of the transfer and the chest-close sound when the goal of the transfer
 * stops, both with the volume 0.5 and the pitch 0.9 to 1.0 of the original. The visible lid is the
 * one part of the original's chest interaction that 1.21.1 does not offer to a mob.</p>
 */
public final class GolemChestInteraction {
    private GolemChestInteraction() {}

    /** The volume of the original's chest sound. */
    public static final float VOLUME = 0.5F;

    /**
     * The original opens the chest of a transfer.
     *
     * @param level the level of the chest
     * @param pos   the position of the chest
     */
    public static void open(Level level, BlockPos pos) {
        if (level == null || pos == null) return;
        level.playSound(null, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5,
                SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, VOLUME, level.getRandom().nextFloat() * 0.1F + 0.9F);
    }

    /**
     * The original closes the chest of a transfer again.
     *
     * @param level the level of the chest
     * @param pos   the position of the chest
     */
    public static void close(Level level, BlockPos pos) {
        if (level == null || pos == null) return;
        level.playSound(null, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5,
                SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, VOLUME, level.getRandom().nextFloat() * 0.1F + 0.9F);
    }
}
