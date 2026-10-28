/**
 * Created by pixlepix on 7/25/14.
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

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.ThaumcraftFX;
import thaumcraft.api.TileThaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumic.tinkerer.common.content.SimpleBlocks;

/**
 * The block entity of an infused crop: its aspect, its primal tendencies (at most fifty points), the growth of the
 * crop (the crop does not grow by random ticks) and the exchange of tendencies with the crops next to it.
 */
public class TileInfusedGrain extends TileThaumcraft implements IAspectContainer {

    private final String NBT_MAIN_ASPECT = "mainAspect";
    private final String NBT_ASPEPCT_TENDENCIES = "aspectTendencies";
    @Nullable
    public Aspect aspect;
    public AspectList primalTendencies = new AspectList();

    public TileInfusedGrain(BlockPos pos, BlockState state) {
        super(SimpleBlocks.TILE_INFUSED_GRAIN.get(), pos, state);
    }

    /** The aspect of the crop; the air aspect while none was set (1.7.10 gave it to the tile on its first tick). */
    public Aspect aspectOrAir() {
        return aspect == null ? Aspect.AIR : aspect;
    }

    /**
     * 1.7.10 {@code updateEntity}, on the server (the particles of the exchange are sent by the server as Thaumcraft's
     * essentia trail, where 1.7.10 drew them on the client's own roll of the dice).
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, TileInfusedGrain tile) {
        RandomSource rand = level.random;

        // Growth
        if (level.getRawBrightness(pos.above(), 0) >= 9) {
            int l = state.getValue(CropBlock.AGE);
            if (l < 7) {
                int water = tile.primalTendencies.getAmount(Aspect.WATER);
                if (rand.nextInt(Math.max(1, (2510 - (int) Math.pow(water, 2)) * 6)) == 0) {
                    ++l;
                    level.setBlock(pos, state.setValue(CropBlock.AGE, l), 3);
                    tile.setChanged();
                }
            }
        }
        if (tile.aspect == null) {
            tile.aspect = Aspect.AIR;
        }
        // Aspect Exchange
        if (rand.nextInt(Math.max(1, (2550 - (int) Math.pow(tile.primalTendencies.getAmount(Aspect.AIR), 2)) * 10)) == 0
                && !tile.aspect.isPrimal()) {

            for (Direction dir : Direction.values()) {
                BlockPos neighbour = pos.relative(dir);
                if (!level.isLoaded(neighbour)) continue;
                if (level.getBlockEntity(neighbour) instanceof TileInfusedGrain other) {
                    // Exchange aspects
                    Aspect otherAspect = other.aspectOrAir();
                    if (otherAspect.isPrimal()) {
                        if (tile.primalTendencies.getAmount(otherAspect) < 5) {
                            tile.primalTendencies.add(otherAspect, 1);
                            tile.reduceSaturatedAspects();

                            tile.sync(level, pos);
                            trail(level, pos, neighbour, otherAspect);
                            return;
                        }
                    } else {
                        AspectList targetList = other.primalTendencies;
                        if (targetList.getAspects().length == 0 || targetList.getAspects()[0] == null) {
                            return;
                        }

                        Aspect moved = targetList.getAspects()[rand.nextInt(targetList.getAspects().length)];
                        if (targetList.getAmount(moved) >= tile.primalTendencies.getAmount(moved)) {
                            tile.primalTendencies.add(moved, 1);
                            targetList.reduce(moved, 1);
                            tile.reduceSaturatedAspects();
                            trail(level, pos, neighbour, moved);

                            tile.sync(level, pos);
                            other.setChanged();
                        }
                        return;
                    }
                }
            }
        }
    }

    private static void trail(Level level, BlockPos consumer, BlockPos source, Aspect aspect) {
        if (level instanceof ServerLevel server) ThaumcraftFX.essentiaTrail(server, consumer, source, aspect);
    }

    /** 1.7.10 {@code markBlockForUpdate}. */
    private void sync(Level level, BlockPos pos) {
        setChanged();
        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, 3);
    }

    public void reduceSaturatedAspects() {
        int sum = 0;
        for (Integer i : primalTendencies.aspects.values()) {
            sum += i;
        }
        if (sum > 50) {
            int toRemove = sum - 50;
            while (toRemove > 0) {
                Random rand = new Random();
                Aspect target = primalTendencies.getAspects()[rand.nextInt(primalTendencies.getAspects().length)];
                primalTendencies.remove(target, 1);
                toRemove--;
            }
        }
    }

    @Override
    public void writeCustomNBT(CompoundTag nbt) {
        super.writeCustomNBT(nbt);
        CompoundTag aspectCompound = new CompoundTag();
        new AspectList().add(aspect, 1).writeToNBT(aspectCompound);
        nbt.put(NBT_MAIN_ASPECT, aspectCompound);

        CompoundTag tendencyCompound = new CompoundTag();
        primalTendencies.writeToNBT(tendencyCompound);
        nbt.put(NBT_ASPEPCT_TENDENCIES, tendencyCompound);
    }

    @Override
    public void readCustomNBT(CompoundTag nbt) {
        super.readCustomNBT(nbt);
        AspectList aspectList = new AspectList();
        aspectList.readFromNBT(nbt.getCompound(NBT_MAIN_ASPECT));
        Aspect[] read = aspectList.getAspects();
        aspect = read.length > 0 ? read[0] : null;
        aspectList.readFromNBT(nbt.getCompound(NBT_ASPEPCT_TENDENCIES));
        primalTendencies = aspectList;
    }

    @Override
    public AspectList getAspects() {
        return new AspectList().add(aspect, 1);
    }

    @Override
    public void setAspects(AspectList paramAspectList) {}

    @Override
    public boolean doesContainerAccept(Aspect paramAspect) {

        return false;
    }

    @Override
    public int addToContainer(Aspect paramAspect, int paramInt) {
        return 0;
    }

    @Override
    public boolean takeFromContainer(Aspect paramAspect, int paramInt) {
        return false;
    }

    @Override
    @Deprecated
    public boolean takeFromContainer(AspectList paramAspectList) {
        return false;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect paramAspect, int paramInt) {
        return false;
    }

    @Override
    @Deprecated
    public boolean doesContainerContain(AspectList paramAspectList) {
        return false;
    }

    @Override
    public int containerContains(Aspect paramAspect) {
        return 0;
    }
}
