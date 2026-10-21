package thaumcraft.common.tiles;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.WarpEvents;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.ResearchNoticePayload;
import thaumcraft.common.lib.network.WarpMessageNetworking;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.lib.world.OuterLandsDimensionRules;
import thaumcraft.common.lib.world.OuterLandsProvider;
import thaumcraft.common.lib.world.OuterLandsWiringRules;
import dev.architectury.networking.NetworkManager;

/**
 * Original TileEldritchPortal: the eldritch gate. A player that walks into it
 * is moved between the Outer Lands and the overworld.
 *
 * <p>Oracle {@code TileEldritchPortal:54-84} scans every five ticks, refuses a
 * player that is already travelling or that rides, and moves the player
 * through the {@code TeleporterThaumcraft}, which looks for the nearest eldritch
 * gate in the target dimension.
 */
public final class TileEldritchPortal extends BlockEntity {
    /** Original TileEldritchPortal:30 - the open counter of the portal. */
    public int opencount = -1;
    private int count = 0;

    /** The ambient sound of the gate, oracle TileEldritchPortal:57-59. */
    private static final SoundEvent EVIL_PORTAL =
            SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "evilportal"));

    /** The research the original completes when a player enters the Outer Lands. */
    public static final String OUTER_LANDS_RESEARCH = "ENTEROUTER";

    public TileEldritchPortal(BlockPos pos, BlockState state) {
        super(ThaumcraftEldritchBlocks.ELDRITCH_PORTAL_TILE.get(), pos, state);
    }

    public int getOpenCount() { return opencount; }
    public void setOpenCount(int opencount) { this.opencount = opencount; }

    /** Original TileEldritchPortal:52-79 - the portal transfer. */
    public static void tick(Level level, BlockPos pos, BlockState state, TileEldritchPortal portal) {
        portal.count++;
        if (level.isClientSide) {
            // Oracle TileEldritchPortal:57-59 - the gate repeats its sound every 250 ticks.
            if (portal.count % EldritchBlockRules.PORTAL_SOUND_PERIOD == 0 || portal.count == 0) {
                level.playLocalSound(pos.getX() + .5D, pos.getY() + .5D, pos.getZ() + .5D,
                        EVIL_PORTAL, SoundSource.AMBIENT, 1.0F, 1.0F, false);
            }
            if (portal.opencount < EldritchBlockRules.PORTAL_OPEN_COUNT_MAX) portal.opencount++;
            return;
        }
        if (portal.count % EldritchBlockRules.PORTAL_SCAN_PERIOD != 0) return;
        // Oracle TileEldritchPortal:63 - the block box grown by half a block in every direction.
        AABB box = new AABB(pos.getX() - .5D, pos.getY() - 1.0D, pos.getZ() - .5D,
                pos.getX() + 1.5D, pos.getY() + 2.0D, pos.getZ() + 1.5D);
        for (Player player : level.getEntitiesOfClass(Player.class, box)) {
            if (!(player instanceof ServerPlayer server)) continue;
            if (!EldritchBlockRules.portalCanTransfer(server.isChangingDimension(), server.isPassenger())) continue;
            if (server.getPortalCooldown() > 0) continue;
            server.setPortalCooldown(EldritchBlockRules.PORTAL_COOLDOWN);
            transfer(server);
        }
    }

    /**
     * Original TileEldritchPortal:63-82 - the portal swaps the two dimensions.
     *
     * <p>The 1.7 original used {@code MinecraftServer.transferPlayerToDimension} with the
     * {@code TeleporterThaumcraft}, which looks for the nearest eldritch gate in the target
     * dimension and drops the player one block beside it. The native port uses the 1.21.1
     * dimension change of {@code ServerPlayer}, which carries the player over with its
     * inventory, and the same gate search.
     */
    private static void transfer(ServerPlayer player) {
        ServerLevel outer = player.getServer().getLevel(OuterLandsProvider.DIMENSION);
        ServerLevel target = player.level() == outer ? player.getServer().overworld() : outer;
        if (target == null) return;
        BlockPos destination = findGate(target, player.blockPosition());
        double x;
        double y;
        double z;
        if (destination != null) {
            // Oracle TeleporterThaumcraft:102-108 - the player lands one block beside the gate.
            x = destination.getX() + .5D + OuterLandsDimensionRules.gateLandingOffset(target.getRandom().nextBoolean());
            y = OuterLandsDimensionRules.gateLandingHeight(destination.getY());
            z = destination.getZ() + .5D + OuterLandsDimensionRules.gateLandingOffset(target.getRandom().nextBoolean());
        } else {
            // Oracle TeleporterThaumcraft:45-51 - without a gate the player lands above the ground.
            x = player.getX();
            z = player.getZ();
            y = target.getHeight(Heightmap.Types.WORLD_SURFACE, (int) Math.floor(x), (int) Math.floor(z)) + 4.0D;
        }
        player.setDeltaMovement(Vec3.ZERO);
        player.teleportTo(target, x, y, z, player.getYRot(), player.getXRot());
        if (target == outer) completeOuterLandsResearch(player);
    }

    /**
     * The nearest eldritch gate of a dimension.
     *
     * <p>Oracle {@code TeleporterThaumcraft:57-112} walks a square of one hundred and twenty
     * eight blocks around the player and keeps the closest gate. The native port walks the
     * chunks of the same square and reads the gate from the block entities of the chunk, and
     * it only loads a chunk that is not there yet when the chunk is next to the player.
     */
    private static BlockPos findGate(ServerLevel level, BlockPos from) {
        int chunkRadius = OuterLandsWiringRules.PORTAL_SEARCH_RADIUS >> 4;
        int originX = from.getX() >> 4;
        int originZ = from.getZ() >> 4;
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int ring = 0; ring <= chunkRadius; ++ring) {
            for (int dx = -ring; dx <= ring; ++dx) {
                for (int dz = -ring; dz <= ring; ++dz) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    int chunkX = originX + dx;
                    int chunkZ = originZ + dz;
                    if (!level.hasChunk(chunkX, chunkZ)) {
                        if (ring > 1) continue;
                        level.getChunk(chunkX, chunkZ);
                    }
                    for (BlockEntity entity : level.getChunk(chunkX, chunkZ).getBlockEntities().values()) {
                        if (!(entity instanceof TileEldritchPortal)) continue;
                        BlockPos pos = entity.getBlockPos();
                        double distance = pos.distSqr(from);
                        if (distance >= bestDistance) continue;
                        bestDistance = distance;
                        best = pos;
                    }
                }
            }
            if (best != null) return best;
        }
        return null;
    }

    /**
     * Original TileEldritchPortal:75-78 - the first visit to the Outer Lands completes the
     * research of the Outer Lands.
     */
    private static void completeOuterLandsResearch(ServerPlayer player) {
        PlayerKnowledge knowledge;
        try {
            knowledge = KnowledgeAccess.get(player);
        } catch (RuntimeException unavailable) {
            return;
        }
        if (knowledge.isResearchComplete(OUTER_LANDS_RESEARCH)) return;
        var definition = ResearchCatalog.get(OUTER_LANDS_RESEARCH);
        if (definition == null || !knowledge.addResearch(OUTER_LANDS_RESEARCH)) return;
        int warp = definition.warp();
        if (warp > 0 && !WarpEvents.isWussMode()) {
            knowledge.addWarpPerm(warp - warp / 2);
            knowledge.addWarpSticky(warp / 2);
            knowledge.setWarpCounter(knowledge.getTotalWarp());
        }
        WarpMessageNetworking.research(player, List.of(OUTER_LANDS_RESEARCH));
        NetworkManager.sendToPlayer(player, new ResearchNoticePayload(OUTER_LANDS_RESEARCH));
    }

    /**
     * Original TileEldritchPortal stores nothing: the open counter and the tick counter are
     * runtime state of the client and of the server, they never reach the saved data.
     */
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        opencount = -1;
        count = 0;
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
