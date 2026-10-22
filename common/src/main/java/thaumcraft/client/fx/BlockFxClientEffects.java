package thaumcraft.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import thaumcraft.client.fx.particles.FXArc;
import thaumcraft.client.fx.particles.ThaumcraftBoltParticle;
import thaumcraft.common.entities.monster.EntityCultistPortal;
import thaumcraft.common.lib.network.BlockFxPayload;

/**
 * The client half of the original block visual channels.
 *
 * <ul>
 * <li>The arc lightning of the original {@code PacketFXBlockArc} (oracle
 * {@code thaumcraft/common/lib/network/fx/PacketFXBlockArc.java:47-63}): the source entity draws the arc to
 * the block, with the original colours of the eldritch warden and of the cultist portal.</li>
 * <li>The node bolt of the original {@code PacketFXBlockZap} (oracle
 * {@code thaumcraft/common/lib/network/fx/PacketFXBlockZap.java:44-47}): the bolt and the original zap sound.</li>
 * <li>The crucible bubbles of the original {@code PacketFXBlockBubble} (oracle
 * {@code thaumcraft/common/lib/network/fx/PacketFXBlockBubble.java:44-56}).</li>
 * </ul>
 */
public final class BlockFxClientEffects {
    private static final SoundEvent ZAP = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "zap"));

    private BlockFxClientEffects() {}

    /** The visual answer of one payload; nothing here changes the world. */
    public static void accept(BlockFxPayload payload) {
        var client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null || !level.dimension().location().equals(payload.dimension())) return;
        switch (payload.kind()) {
            case BlockFxPayload.ARC -> arc(level, payload);
            case BlockFxPayload.ZAP -> zap(level, payload);
            case BlockFxPayload.BUBBLE -> bubble(level, payload);
            case BlockFxPayload.SPARKLE -> sparkle(level, payload);
            default -> { /* The payload codec rejects other kinds. */ }
        }
    }

    /** The original PacketFXBlockArc: the arc of the source entity to the block. */
    private static void arc(ClientLevel level, BlockFxPayload payload) {
        Entity source = level.getEntity(payload.source());
        if (source == null) return;
        float red = .3F - level.random.nextFloat() * .1F;
        float green = 0F;
        float blue = .5F + level.random.nextFloat() * .2F;
        if (source instanceof EntityCultistPortal) {
            red = .5F + level.random.nextFloat() * .2F;
            green = 0F;
            blue = 0F;
        }
        BlockPos target = payload.target();
        FXArc.arc(level, source.getX(), source.getY() + source.getBbHeight() / 2.0F, source.getZ(),
                target.getX() + .5D, target.getY() + 1, target.getZ() + .5D, red, green, blue, .5F);
    }

    /** The original PacketFXBlockZap: the node bolt and the zap sound. */
    private static void zap(ClientLevel level, BlockFxPayload payload) {
        ThaumcraftBoltParticle.between(level, payload.origin(), payload.destination());
        Vec3 origin = payload.origin();
        level.playLocalSound(origin.x, origin.y, origin.z, ZAP, SoundSource.BLOCKS, .1F,
                1.0F + level.random.nextFloat() * .2F, false);
    }

    /** The original PacketFXBlockBubble: the crucible bubbles around the block. */
    private static void bubble(ClientLevel level, BlockFxPayload payload) {
        int rgb = payload.rgb();
        float red = (rgb >> 16 & 0xFF) / 255F;
        float green = (rgb >> 8 & 0xFF) / 255F;
        float blue = (rgb & 0xFF) / 255F;
        BlockPos target = payload.target();
        int count = count(1);
        for (int a = 0; a < count; a++) {
            thaumcraft.client.ThaumcraftEldritchItemsClient.bubble(level, target.getX(),
                    target.getY() + level.random.nextFloat(), target.getZ() + level.random.nextFloat(), red, green, blue);
            thaumcraft.client.ThaumcraftEldritchItemsClient.bubble(level, target.getX() + 1,
                    target.getY() + level.random.nextFloat(), target.getZ() + level.random.nextFloat(), red, green, blue);
            thaumcraft.client.ThaumcraftEldritchItemsClient.bubble(level, target.getX() + level.random.nextFloat(),
                    target.getY() + level.random.nextFloat(), target.getZ(), red, green, blue);
            thaumcraft.client.ThaumcraftEldritchItemsClient.bubble(level, target.getX() + level.random.nextFloat(),
                    target.getY() + level.random.nextFloat(), target.getZ() + 1, red, green, blue);
        }
    }

    /**
     * The original PacketFXBlockSparkle: the sparkle burst at a block.
     *
     * <p>Oracle {@code thaumcraft/common/lib/network/fx/PacketFXBlockSparkle.java:50-52} and
     * {@code thaumcraft/client/ClientProxy.java:632-644} - the packet asks for seven sparkles and
     * the client draws them with the colour of the packet.</p>
     */
    private static void sparkle(ClientLevel level, BlockFxPayload payload) {
        int count = count(7);
        for (int a = 0; a < count; a++) {
            Minecraft.getInstance().particleEngine.add(
                    new thaumcraft.client.fx.particles.BlockSparkleParticle(level, payload.target(), payload.rgb()));
        }
    }

    /** The original particleCount: nothing at the minimal setting, the base at the decreased one, twice at all. */
    private static int count(int base) {
        ParticleStatus setting = Minecraft.getInstance().options.particles().get();
        return setting == ParticleStatus.MINIMAL ? 0 : setting == ParticleStatus.DECREASED ? base : base * 2;    }
}
