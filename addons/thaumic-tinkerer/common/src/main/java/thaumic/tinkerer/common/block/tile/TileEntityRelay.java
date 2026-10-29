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

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.client.ThaumcraftClientFX;
import thaumic.tinkerer.common.content.TileBlocks;

/**
 * A Levitational Locomotive Relay: it looks for a partner relay in a straight line (up to 32 blocks, same height), draws
 * the beam between them with sparkles, and links the Levitational Locomotives standing in the beam to the pair.
 *
 * <p>Like the original, the partner search runs on both sides (the client finds the partner on its own, so it can draw
 * the beam without a packet); the linking of locomotives runs on the server only. Only loaded chunks are consulted
 * (1.7.10 loaded chunks on demand).</p>
 */
public class TileEntityRelay extends BlockEntity {

    public boolean hasPartner;
    public int partnerX;
    public int partnerZ;

    public TileEntityRelay(BlockPos pos, BlockState state) {
        super(TileBlocks.relayType(), pos, state);
    }

    /** The relay at this position of the row (same height), or null when it is not there or not loaded. */
    private TileEntityRelay relayAt(int x, int z) {
        BlockPos pos = new BlockPos(x, worldPosition.getY(), z);
        if (level == null || !level.isLoaded(pos)) return null;
        return level.getBlockEntity(pos) instanceof TileEntityRelay relay ? relay : null;
    }

    public void verifyPartner() {
        BlockPos partnerPos = new BlockPos(partnerX, worldPosition.getY(), partnerZ);
        if (level == null || !level.isLoaded(partnerPos)) return;
        TileEntityRelay te = relayAt(partnerX, partnerZ);
        if (!(hasPartner && te != null && te.partnerX == worldPosition.getX() && te.partnerZ == worldPosition.getZ())) {
            hasPartner = false;
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityRelay relay) {
        relay.tick();
    }

    private void tick() {
        int xCoord = worldPosition.getX();
        int yCoord = worldPosition.getY();
        int zCoord = worldPosition.getZ();
        long time = level.getGameTime();

        verifyPartner();
        if (hasPartner) {
            int i = xCoord;
            int j;
            do {
                j = zCoord;
                do {
                    float xInc = 0;
                    float zInc = 0;
                    if (partnerZ - zCoord != 0) {
                        zInc = ((float) Math.copySign(.05, partnerZ - zCoord)) * (time % 20);
                    }

                    if (partnerX - xCoord != 0) {
                        xInc = ((float) Math.copySign(.05, partnerX - xCoord)) * (time % 20);
                    }
                    if (level.isClientSide) {
                        // 1.7.10: sparkle(.., xCoord < partnerX || zCoord > partnerX ? 2 : 14), kept with its comparison.
                        sparkle((float) (0.5 + i + xInc), (float) (yCoord + 0.5), (float) (j + 0.5 + zInc),
                                xCoord < partnerX || zCoord > partnerX ? 2 : 14);
                    }
                    j += step(partnerZ - zCoord);
                } while (j < partnerZ);
                i += step(partnerX - xCoord);

            } while (i < partnerX);
        }
        if (time % 40 == 0) {
            checkForPartner();
        }
        int i = xCoord;
        if (!level.isClientSide && hasPartner && time % 40 == 0) {
            do {

                int j = zCoord;
                do {
                    BlockPos pos = new BlockPos(i, yCoord, j);
                    if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof TileEntityMobilizer te) {
                        te.verifyRelay();
                        if (!te.linked) {
                            te.firstRelayX = xCoord;
                            te.firstRelayZ = zCoord;

                            te.secondRelayX = partnerX;
                            te.secondRelayZ = partnerZ;

                            te.linked = true;

                            if (xCoord != partnerX) {
                                te.movementDirection = Direction.EAST;
                            } else {
                                te.movementDirection = Direction.NORTH;
                            }
                            te.setChanged();
                        }
                    }

                    j += step(partnerZ - zCoord);
                } while (j < partnerZ);
                i += step(partnerX - xCoord);
            } while (i < partnerX);
        }
    }

    /**
     * The step of the original loops, {@code Math.copySign(1, difference)}: -1 for a negative difference, +1 otherwise
     * (zero included). The original added that float to an {@code int}, which stops advancing once the coordinate reaches
     * 2^24 in absolute value, so a linked pair beyond +-16,777,216 blocks hung the server thread in its tick loop. The
     * integer step is the same for every coordinate below that (see docs/deviations/TileBlocks.md, 2.3).
     */
    public static int step(int difference) {
        return difference < 0 ? -1 : 1;
    }

    /**
     * The beam sparkle: the Thaumcraft sparkle particle of the API, which rolls the original's four-in-six chance itself. The
     * original types were 2 (blue) and 14 (white); the API knows the colours 0 to 7, white being 6.
     */
    private void sparkle(float x, float y, float z, int type) {
        ThaumcraftClientFX.sparkle(level, x, y, z, type == 2 ? 2 : 6);
    }

    public void checkForPartner() {
        if (!hasPartner) {
            for (int i = -32; i < 32; i++) {
                if (i != 0) {
                    setPartners(relayAt(worldPosition.getX(), worldPosition.getZ() + i));
                    setPartners(relayAt(worldPosition.getX() + i, worldPosition.getZ()));
                }
            }
        }
    }

    private void setPartners(TileEntityRelay te) {
        if (te != null) {
            te.partnerX = worldPosition.getX();
            te.partnerZ = worldPosition.getZ();
            this.partnerX = te.worldPosition.getX();
            this.partnerZ = te.worldPosition.getZ();

            this.hasPartner = true;
            te.hasPartner = true;
            te.setChanged();
            this.setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);

        nbt.putBoolean("HasPartner", hasPartner);

        nbt.putInt("PartnerX", partnerX);
        nbt.putInt("PartnerZ", partnerZ);
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);

        hasPartner = nbt.getBoolean("HasPartner");

        partnerX = nbt.getInt("PartnerX");
        partnerZ = nbt.getInt("PartnerZ");
    }
}
