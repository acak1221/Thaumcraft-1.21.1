/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block.tile;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.ThaumcraftFX;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.blocks.BlocksTC;
import thaumic.tinkerer.common.content.Machines;
import thaumic.tinkerer.common.core.helper.EnumMobAspect;
import thaumic.tinkerer.common.item.ItemMobAspect;

/**
 * The Tablet of Necromancy. Every 300 ticks (unless it is powered by redstone) it looks for arcane pedestals around it
 * (within five blocks, at its own height) that carry soul aspects; three of them whose aspects are the aspects of a
 * mob summon that mob. Plain soul aspects are used up; infused ones are kept but the tablet works only every 1200
 * ticks, and remembers them.
 */
public class TileSummon extends BlockEntity {

    public TileSummon(BlockPos pos, BlockState state) {
        super(Machines.TILE_SUMMON.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileSummon tablet) {
        tablet.update((ServerLevel) level);
    }

    /** The pedestal at a position, or null: only the arcane pedestal counts (1.7.10: {@code TilePedestal}). */
    private static Container pedestalAt(Level level, BlockPos pos) {
        if (!level.getBlockState(pos).is(BlocksTC.get("arcane_pedestal"))) return null;
        return level.getBlockEntity(pos) instanceof Container container ? container : null;
    }

    private void update(ServerLevel level) {
        if (level.getGameTime() % 300 != 0 || level.hasNeighborSignal(worldPosition)) {
            return;
        }

        for (int radius = 1; radius < 6; radius++) {
            List<BlockPos> pedestals = new ArrayList<>();
            for (int x = worldPosition.getX() - radius; x <= worldPosition.getX() + radius; x++) {
                for (int z = worldPosition.getZ() - radius; z <= worldPosition.getZ() + radius; z++) {
                    BlockPos at = new BlockPos(x, worldPosition.getY(), z);
                    Container pedestal = pedestalAt(level, at);
                    if (pedestal != null && pedestal.getItem(0).getItem() instanceof ItemMobAspect) {
                        pedestals.add(at);
                    }
                }
            }

            for (int i = 0; i < pedestals.size(); i++) {
                for (int j = 0; j < pedestals.size(); j++) {
                    for (BlockPos pos3 : pedestals) {
                        BlockPos pos1 = pedestals.get(i);
                        BlockPos pos2 = pedestals.get(j);

                        if (pos1.equals(pos2) || pos2.equals(pos3) || pos1.equals(pos3)) continue;
                        Container ped1 = pedestalAt(level, pos1);
                        Container ped2 = pedestalAt(level, pos2);
                        Container ped3 = pedestalAt(level, pos3);
                        if (ped1 == null || ped2 == null || ped3 == null) continue;

                        List<Aspect> aspects = new ArrayList<>();
                        aspects.add(ItemMobAspect.getAspect(ped1.getItem(0)));
                        aspects.add(ItemMobAspect.getAspect(ped2.getItem(0)));
                        aspects.add(ItemMobAspect.getAspect(ped3.getItem(0)));

                        for (EnumMobAspect recipe : EnumMobAspect.values()) {
                            if (!recipe.matchesAspectSet(aspects)) {
                                continue;
                            }

                            boolean isInfused = ItemMobAspect.isInfused(ped1.getItem(0))
                                    && ItemMobAspect.isInfused(ped2.getItem(0))
                                    && ItemMobAspect.isInfused(ped3.getItem(0));

                            if (isInfused && level.getGameTime() % 1200 != 0) return;

                            if (!isInfused) {
                                clear(level, pos1, ped1);
                                clear(level, pos2, ped2);
                                clear(level, pos3, ped3);
                            }

                            if (!isInfused || ItemMobAspect.lastUsedTabletMatches(ped1.getItem(0), worldPosition)
                                    && ItemMobAspect.lastUsedTabletMatches(ped2.getItem(0), worldPosition)
                                    && ItemMobAspect.lastUsedTabletMatches(ped3.getItem(0), worldPosition)) {

                                spawn(level, recipe);

                                ThaumcraftFX.essentiaTrail(level, worldPosition, pos1, aspects.get(0));
                                ThaumcraftFX.essentiaTrail(level, worldPosition, pos2, aspects.get(1));
                                ThaumcraftFX.essentiaTrail(level, worldPosition, pos3, aspects.get(2));
                            }
                            if (isInfused) {
                                mark(level, pos1, ped1);
                                mark(level, pos2, ped2);
                                mark(level, pos3, ped3);
                            }

                            return;
                        }
                    }
                }
            }
        }
    }

    /** The mob appears on top of the tablet, as {@code spawn.setLocationAndAngles(x + .5, y + 1, z + .5, 0, 0)}. */
    private void spawn(ServerLevel level, EnumMobAspect recipe) {
        Mob spawn = recipe.createEntity(level);
        if (spawn == null) return;
        spawn.moveTo(worldPosition.getX() + .5, worldPosition.getY() + 1, worldPosition.getZ() + .5, 0, 0);
        // onSpawnWithEgg(null): the equipment and variants of a mob that comes out of a spawn egg (a wither skeleton
        // gets its stone sword here, a skeleton its bow).
        spawn.finalizeSpawn(level, level.getCurrentDifficultyAt(spawn.blockPosition()), MobSpawnType.SPAWN_EGG, null);
        recipe.finalizeSummoned(spawn);
        level.addFreshEntityWithPassengers(spawn);
        spawn.playAmbientSound();
    }

    private static void clear(ServerLevel level, BlockPos pos, Container pedestal) {
        pedestal.setItem(0, ItemStack.EMPTY);
        syncPedestal(level, pos);
    }

    /** An infused soul aspect remembers this tablet as the last one that used it. */
    private void mark(ServerLevel level, BlockPos pos, Container pedestal) {
        ItemStack stack = pedestal.getItem(0);
        if (stack.isEmpty()) return;
        ItemMobAspect.markLastUsedTablet(stack, worldPosition);
        pedestal.setChanged();
        syncPedestal(level, pos);
    }

    private static void syncPedestal(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, 3);
    }
}
