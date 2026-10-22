package thaumcraft.common.tiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.visnet.VisNetHandler;
import thaumcraft.common.blocks.FluxFiniteBlock;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.essentia.EssentiaStorageEndpoint;
import thaumcraft.common.lib.tiles.TileParityRules;
import thaumcraft.common.lib.world.EldritchBlockRules;

/**
 * Original TileFluxScrubber: the flux scrubber. It removes the flux blocks
 * around it and turns them into essentia.
 *
 * <p>The scrubber is powered by the vis network: it draws AIR vis and spends
 * five of its power on every flux block it removes. The essentia it collects
 * leaves through the side the scrubber faces.</p>
 */
public final class TileFluxScrubber extends BlockEntity implements EssentiaStorageEndpoint {
    public int essentia = 0;
    public int charges = 0;
    public int power = 0;
    public Direction facing = Direction.DOWN;
    /**
     * Original TileFluxScrubber:37 - the counter of the scrubber. It is rolled
     * once, on the client as well as on the server (oracle
     * {@code thaumcraft/common/tiles/TileFluxScrubber.java:44-46}), and it is the
     * phase of the bob of the tip of the cap (oracle
     * {@code thaumcraft/client/renderers/tile/TileFluxScrubberRenderer.java:38}).
     * The original never writes it to the NBT, so it is rolled again after a
     * load.
     */
    private int count;
    /** The original candidate blocks of the scan, in the order they are drawn. */
    private final List<BlockPos> checklist = new ArrayList<>();

    public TileFluxScrubber(BlockPos pos, BlockState state) {
        super(ThaumcraftEldritchBlocks.FLUX_SCRUBBER_TILE.get(), pos, state);
    }

    public int getEssentia() { return essentia; }
    public int getCharges() { return charges; }
    public int getPower() { return power; }
    public Direction getFacing() { return facing; }
    /** The counter of the original, the phase of the bob of the tip of the cap. */
    public int getCount() { return count; }

    /**
     * Original TileFluxScrubber:44-46 - the counter is rolled when it is zero.
     * The original rolls it on the client as well, so the client and the server
     * of a scrubber do not share it.
     *
     * @param level the level whose random rolls the counter
     */
    public void rollCount(Level level) {
        if (count == 0) count = level.getRandom().nextInt(1000);
    }

    public void setFacing(Direction facing) {
        this.facing = facing == null ? Direction.DOWN : facing;
        setChanged();
    }

    /** Original TileFluxScrubber:33-53 - the scrubber collects and spends its charges. */
    public static void tick(Level level, BlockPos pos, BlockState state, TileFluxScrubber scrubber) {
        // Original TileFluxScrubber:44-46 - the counter is rolled on both sides.
        scrubber.rollCount(level);
        if (level.isClientSide) return;
        if (scrubber.charges >= EldritchBlockRules.FLUX_CHARGE_COST) {
            scrubber.charges -= EldritchBlockRules.FLUX_CHARGE_COST;
            if (level.getRandom().nextInt(EldritchBlockRules.FLUX_ESSENTIA_CHANCE) == 0) {
                scrubber.essentia = EldritchBlockRules.fluxScrubberEssentiaAfterCharge(scrubber.essentia);
                scrubber.setChanged();
            }
        }
        // Original TileFluxScrubber:58-60 - the scrubber is powered by the vis
        // network, it draws ten AIR vis while it has less than five power.
        if (scrubber.power < EldritchBlockRules.FLUX_POWER_MIN) {
            scrubber.power += VisNetHandler.drainVis(level, pos, Aspect.AIR, EldritchBlockRules.FLUX_POWER_DRAIN);
        }
        if (scrubber.power >= EldritchBlockRules.FLUX_POWER_MIN) {
            scrubber.checkFlux(level, pos);
        }
    }

    /**
     * Original TileFluxScrubber:55-88 - the flux blocks around the scrubber are
     * removed. Only the blocks inside the squared radius of sixteen are cleaned,
     * and at most sixteen candidates are examined per pass.
     */
    private void checkFlux(Level level, BlockPos pos) {
        int distance = EldritchBlockRules.FLUX_SCAN_DISTANCE;
        if (checklist.isEmpty()) {
            // Original TileFluxScrubber:73-83 - the candidates are the blocks of
            // the cube, shuffled, and the ones outside the radius never count.
            for (int a = -distance; a <= distance; a++) {
                for (int b = -distance; b <= distance; b++) {
                    for (int c = -distance; c <= distance; c++) {
                        if (!EldritchBlockRules.fluxScrubberInRange(a, b, c)) continue;
                        checklist.add(new BlockPos(pos.getX() + a, pos.getY() + b, pos.getZ() + c));
                    }
                }
            }
            shuffle(checklist, level.getRandom());
        }
        for (int examined = 0; examined < EldritchBlockRules.FLUX_SCAN_BATCH && !checklist.isEmpty(); examined++) {
            BlockPos target = checklist.remove(0);
            if (!level.hasChunkAt(target)) continue;
            BlockState fluxState = level.getBlockState(target);
            if (fluxState.isAir() || !isFlux(level, target)) continue;
            power -= EldritchBlockRules.FLUX_POWER_PER_BLOCK;
            // The original metadata of the flux block is its quanta, so the
            // block loses one quantum or vanishes at the lowest one.
            int metadata = fluxState.getValue(FluxFiniteBlock.LEVEL);
            int reduced = EldritchBlockRules.fluxScrubberReducedLevel(metadata);
            if (reduced < 0) {
                level.destroyBlock(target, false);
            } else {
                level.setBlock(target, fluxState.setValue(FluxFiniteBlock.LEVEL, reduced),
                        Block.UPDATE_ALL);
            }
            charges++;
            // Original TileFluxScrubber:100 - the removed flux block sparkles with the
            // original colour 0xDD00FF for the players around it.
            if (level instanceof ServerLevel server) {
                thaumcraft.common.lib.network.BlockFxNetworking.sendSparkle(server, target, 0xDD00FF);
            }
            setChanged();
            // Original TileFluxScrubber:100 - the removed flux block flashes with the
            // original magenta 0xDD00FF of the PacketFXBlockSparkle.
            if (level instanceof net.minecraft.server.level.ServerLevel server)
                thaumcraft.common.lib.network.BlockSparkleFxNetworking.sendBlockSparkle(server, target, 0xDD00FF);
            return;
        }
    }

    /**
     * Original TileFluxScrubber:59-62 - the original matches the flux MATERIAL, and the
     * flux goo and the flux gas are the two blocks that use it (oracle Config:876
     * {@code fluxGoomaterial = new MaterialTaint(...)}, oracle BlockFluxGoo:32 and
     * oracle BlockFluxGas:28 both pass that material to their constructor).
     *
     * <p>The native tree used to recognise the flux goo only, so the scrubber never
     * removed a single block of flux gas. Both native flux blocks are the two
     * subclasses of {@link FluxFiniteBlock}, which is the native port of the finite
     * fluid of the original, so the material test is the whole flux family.</p>
     */
    private boolean isFlux(Level level, BlockPos pos) {
        return TileParityRules.fluxScrubberRemoves(level.getBlockState(pos));
    }

    /** The original shuffles its candidates with the random of the world. */
    private static void shuffle(List<BlockPos> candidates, RandomSource random) {
        for (int index = candidates.size() - 1; index > 0; index--) {
            int other = random.nextInt(index + 1);
            Collections.swap(candidates, index, other);
        }
    }

    // ----------------------------------------------------- the essentia transport

    /** Original TileFluxScrubber:134-136 - only the front of the scrubber is connected. */
    @Override public boolean isConnectable(Direction face) { return face == facing; }

    /** Original TileFluxScrubber:139-141 - the essentia leaves through the front. */
    @Override public boolean canOutputTo(Direction face) { return face == facing; }

    /** Original TileFluxScrubber:144-146 - the scrubber never takes essentia in. */
    @Override public boolean canInputFrom(Direction face) { return false; }

    /** Original TileFluxScrubber:149-151 - the scrubber has no suction of its own. */
    @Override public void setSuction(Aspect aspect, int amount) {}

    /** Original TileFluxScrubber:163-165 - the scrubber has no suction type. */
    @Override public Aspect getSuctionType(Direction face) { return null; }

    /** Original TileFluxScrubber:168-170 - the scrubber has no suction. */
    @Override public int getSuctionAmount(Direction face) { return 0; }

    /** Original TileFluxScrubber:157-160 - the minimum suction of the scrubber is zero. */
    @Override public int getMinimumSuction() { return 0; }

    /** Original TileFluxScrubber:172-175 - the scrubber has no long tube. */
    @Override public boolean renderExtendedTube() { return false; }

    /** Original TileFluxScrubber:173-175 - the stored essentia is always of magic. */
    @Override public Aspect getEssentiaType(Direction face) { return Aspect.MAGIC; }

    /** Original TileFluxScrubber:177-180 - the amount of the stored essentia. */
    @Override public int getEssentiaAmount(Direction face) { return essentia; }

    /** Original TileFluxScrubber:183-188 - the essentia is taken from the front. */
    @Override public int takeEssentia(Aspect aspect, int amount, Direction face) {
        int taken = Math.min(essentia, amount);
        essentia -= taken;
        setChanged();
        return taken;
    }

    /** Original TileFluxScrubber:170-176 - the essentia is taken one unit at a time. */
    public int takeEssentia(int amount) {
        int taken = Math.min(essentia, amount);
        essentia -= taken;
        setChanged();
        return taken;
    }

    /** Original TileFluxScrubber:191-193 - the scrubber never takes essentia in. */
    @Override public int addEssentia(Aspect aspect, int amount, Direction face) { return 0; }

    // -------------------------------------------------------- the native stock

    /** The native stock of the scrubber is the collected essentia, always of magic. */
    @Override public Aspect getStoredEssentiaType() { return essentia > 0 ? Aspect.MAGIC : null; }

    @Override public int getStoredEssentiaAmount() { return essentia; }

    /** Original TileFluxScrubber:51-53 - at most four essentia are stored. */
    @Override public int getEssentiaCapacity() { return EldritchBlockRules.FLUX_ESSENTIA_MAX; }

    @Override public void setEssentiaStorageRaw(Aspect aspect, int amount) { essentia = amount; }

    @Override public void notifyEssentiaStorageChanged() { setChanged(); }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        facing = Direction.from3DDataValue(tag.getByte("facing"));
        charges = tag.getInt("charges");
        power = tag.getInt("power");
        essentia = tag.getInt("essentia");
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("facing", (byte) facing.get3DDataValue());
        tag.putInt("charges", charges);
        tag.putInt("power", power);
        tag.putInt("essentia", essentia);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
