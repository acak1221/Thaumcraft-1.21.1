package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.world.EldritchBlockRules;

/**
 * Original TileEldritchPortal: the eldritch gate. A player that walks into it
 * is moved between the Outer Lands and the overworld.
 */
public final class TileEldritchPortal extends BlockEntity {
    /** Original TileEldritchPortal:30 - the open counter of the portal. */
    public int opencount = -1;
    private int count = 0;

    /** The original Outer Lands dimension, the portal target. */
    public static final ResourceKey<Level> OUTER = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "outer"));

    public TileEldritchPortal(BlockPos pos, BlockState state) {
        super(ThaumcraftEldritchBlocks.ELDRITCH_PORTAL_TILE.get(), pos, state);
    }

    public int getOpenCount() { return opencount; }
    public void setOpenCount(int opencount) { this.opencount = opencount; }

    /** Original TileEldritchPortal:52-79 - the portal transfer. */
    public static void tick(Level level, BlockPos pos, BlockState state, TileEldritchPortal portal) {
        portal.count++;
        if (level.isClientSide) {
            if (portal.opencount < EldritchBlockRules.PORTAL_OPEN_COUNT_MAX) portal.opencount++;
            return;
        }
        if (portal.count % EldritchBlockRules.PORTAL_SCAN_PERIOD != 0) return;
        for (Player player : level.getEntitiesOfClass(Player.class,
                new net.minecraft.world.phys.AABB(pos.getX() + .5D, pos.getY() + .5D, pos.getZ() + .5D, .5D, 1.0D, .5D))) {
            if (!(player instanceof ServerPlayer server)) continue;
            if (!EldritchBlockRules.portalCanTransfer(server.isChangingDimension(), server.isPassenger())) continue;
            if (server.getPortalCooldown() > 0) continue;
            server.setPortalCooldown(EldritchBlockRules.PORTAL_COOLDOWN);
            transfer(server);
        }
    }

    /** Original TileEldritchPortal:63-78 - the portal swaps the two dimensions. */
    private static void transfer(ServerPlayer player) {
        ServerLevel outer = player.getServer().getLevel(OUTER);
        if (outer != null && player.level() != outer) {
            player.changeDimension(new net.minecraft.world.level.portal.DimensionTransition(outer, player,
                    net.minecraft.world.level.portal.DimensionTransition.PLAY_PORTAL_SOUND));
            return;
        }
        if (player.level() == outer) {
            ServerLevel overworld = player.getServer().overworld();
            player.changeDimension(new net.minecraft.world.level.portal.DimensionTransition(overworld, player,
                    net.minecraft.world.level.portal.DimensionTransition.PLAY_PORTAL_SOUND));
        }
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        opencount = tag.contains("opencount") ? tag.getInt("opencount") : -1;
        count = tag.getInt("count");
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("opencount", opencount);
        tag.putInt("count", count);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
