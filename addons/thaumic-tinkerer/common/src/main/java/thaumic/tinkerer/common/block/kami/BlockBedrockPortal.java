/**
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block.kami;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;
import thaumic.tinkerer.common.dim.TeleporterBedrock;
import thaumic.tinkerer.common.dim.WorldProviderBedrock;

/**
 * The Bedrock portal: an unbreakable, collision-free block that sends whoever stands in it between the surface and the
 * Bedrock dimension (made from bedrock by the Awakened Ichorium Pickaxe, see the KamiGear group). It has no item.
 *
 * <p>1.7.10 did this twice over: the block moved <i>players</i> from {@code onEntityCollidedWithBlock} (with the
 * {@code TeleporterBedrock} that placed nothing, and the cavity work of {@link TeleporterBedrock}), and its tile entity
 * {@code TileBedrockPortal} sent <i>every</i> entity in the block to the dimension with the vanilla teleporter (which also
 * built a Nether portal there). Here the block is a vanilla {@link Portal}: an entity that stands in it is sent by the
 * game's own portal processing, at the end of its tick, to the destination of {@link #getPortalDestination}: the
 * Bedrock dimension from a surface world (any entity), the Overworld from the Bedrock dimension (players only, as
 * before). The tile entity is not needed any more.</p>
 *
 * <p>Right-clicking the portal turns it back into bedrock (1.7.10 {@code onBlockActivated}); the click then goes on, so
 * a held block is placed against it as before.</p>
 */
public class BlockBedrockPortal extends Block implements Portal {
    public static final MapCodec<BlockBedrockPortal> CODEC = simpleCodec(BlockBedrockPortal::new);

    public BlockBedrockPortal() {
        this(BlockBehaviour.Properties.of().mapColor(MapColor.NONE).sound(SoundType.STONE).strength(-1.0F, 6000000.0F)
                .noCollission().noOcclusion().noLootTable().pushReaction(PushReaction.BLOCK));
    }

    public BlockBedrockPortal(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    /** 1.7.10 {@code onBlockActivated}: the portal turns back into bedrock; the use of the held item goes on. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!level.isClientSide) level.setBlock(pos, Blocks.BEDROCK.defaultBlockState(), Block.UPDATE_ALL);
        return InteractionResult.PASS;
    }

    /**
     * 1.7.10 {@code onEntityCollidedWithBlock} and {@code TileBedrockPortal.updateEntity}: an entity in the block is
     * handed to the portal processing. Only a surface world and the Bedrock dimension have a destination; in the Bedrock
     * dimension only players leave (1.7.10's tile entity "travelled" the others to the dimension they were in).
     */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(level instanceof ServerLevel)) return;
        boolean bedrock = WorldProviderBedrock.isBedrock(level);
        if (bedrock ? !(entity instanceof Player) : !WorldProviderBedrock.isSurfaceWorld(level)) return;
        if (entity.canUsePortal(false)
                && Shapes.joinIsNotEmpty(Shapes.create(entity.getBoundingBox().move(-pos.getX(), -pos.getY(), -pos.getZ())),
                        Shapes.block(), BooleanOp.AND)) {
            entity.setAsInsidePortal(this, pos);
        }
    }

    /** No wait: the 1.7.10 transfer was immediate. */
    @Override
    public int getPortalTransitionTime(ServerLevel level, Entity entity) {
        return 0;
    }

    /** No screen effect either. */
    @Override
    public Portal.Transition getLocalTransition() {
        return Portal.Transition.NONE;
    }

    @Nullable
    @Override
    public DimensionTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
        MinecraftServer server = level.getServer();
        if (WorldProviderBedrock.isBedrock(level)) {
            // 1.7.10 sent players to dimension 0 regardless of where they came from.
            ServerLevel overworld = server.getLevel(Level.OVERWORLD);
            if (overworld == null || !(entity instanceof Player)) return null;
            return TeleporterBedrock.toSurface(overworld, entity.getX(), entity.getZ(), level.getRandom());
        }
        if (!WorldProviderBedrock.isSurfaceWorld(level)) return null;
        ServerLevel bedrock = server.getLevel(WorldProviderBedrock.KEY);
        if (bedrock == null) return null;
        return TeleporterBedrock.toBedrock(bedrock, pos.getX(), pos.getZ(), defaultBlockState());
    }
}
