package thaumcraft.common.items.relics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.lib.essentia.TubeGeometry;
import thaumcraft.common.tiles.TileTube;
import thaumcraft.common.tiles.TileTubeBuffer;

/** Original free, nondamageable transport inspector; deliberately not an essentia transfer endpoint. */
public final class ItemResonator extends Item {
    private static final SoundEvent KNOCK = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "alembicknock"));

    public ItemResonator(Properties properties) { super(properties.stacksTo(1).rarity(Rarity.UNCOMMON)); }

    /**
     * Oracle ItemResonator:77-79 glints for NBT PRESENCE, not enchantment. Native
     * non-default component patches are the saved per-stack equivalent (including
     * names/custom data); item prototype defaults do not count. Never add fake data
     * to manufacture a glint on an ordinary freshly crafted resonator.
     */
    @Override public boolean isFoil(ItemStack stack) { return !stack.getComponentsPatch().isEmpty(); }

    /** Secondary-use bypasses block use; early block dispatch calls this SAME helper. */
    @Override public InteractionResult useOn(UseOnContext context) { return tryInspect(context); }

    /**
     * Item-first integration point. A handled result must be returned immediately
     * by block dispatch, BEFORE labels, bare-hand actions or a valve wheel toggle.
     * No per-tick deduplication: two actual clicks are two inspections; one native
     * dispatch consumes its action here and never falls through to the item twice.
     */
    public static InteractionResult tryInspect(UseOnContext context) {
        ItemStack held = context.getItemInHand();
        if (held.isEmpty() || !(held.getItem() instanceof ItemResonator)) return InteractionResult.PASS;
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (player == null || player.level() != level || player.getItemInHand(context.getHand()) != held
                || !player.isAlive() || player.isSpectator()) return InteractionResult.FAIL;
        // Refuse off-thread invocations before even querying chunks/block entities.
        if (!level.isClientSide && (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer)
                || !server.getServer().isSameThread())) return InteractionResult.FAIL;
        if (!level.isInWorldBounds(pos) || !level.getWorldBorder().isWithinBounds(pos) || !level.hasChunkAt(pos)) return InteractionResult.FAIL;
        BlockEntity tile = level.getBlockEntity(pos);
        if (!(tile instanceof IEssentiaTransport transport)) return InteractionResult.PASS;
        if (!validTarget(context, tile)) return InteractionResult.FAIL;
        Direction face = context.getClickedFace();
        if (tile instanceof TileTube tube) {
            TubeGeometry.IndexedHit hit = TubeGeometry.retrace(player, tube);
            if (hit == null) return InteractionResult.FAIL;
            if (hit.index() >= 0 && hit.index() < 6) face = Direction.from3DDataValue(hit.index());
        } else if (tile instanceof TileTubeBuffer buffer) {
            TubeGeometry.IndexedHit hit = TubeGeometry.retrace(player, buffer);
            if (hit == null) return InteractionResult.FAIL;
            if (hit.index() >= 0 && hit.index() < 6) face = Direction.from3DDataValue(hit.index());
        } else if (!visible(player, pos)) return InteractionResult.FAIL;
        if (level.isClientSide) {
            // Native SUCCESS requests the client swing. Calling swing here as
            // well would send a second swing from the ordinary RMB dispatcher.
            return InteractionResult.SUCCESS;
        }
        // Oracle ItemResonator:94-106. Query the PUBLIC sided transport contract,
        // even for addon transports that do not support our atomic storage API.
        // ONLY a genuine buffer gets the sorted multi-type branch. Do not make
        // even one random representative query, or treat filter:-1 as stock.
        AspectList buffered = tile instanceof TileTubeBuffer buffer ? buffer.getAspects() : null;
        Aspect stored = buffered == null ? transport.getEssentiaType(face) : null;
        int amount = stored == null ? 0 : transport.getEssentiaAmount(face);
        Aspect suction = transport.getSuctionType(face);
        int pressure = transport.getSuctionAmount(face);
        // Addon getters are callbacks; do not report from an orphaned/replaced
        // target or a changed hand/actor after they return.
        if (!validTarget(context, tile) || !visible(player, pos)) return InteractionResult.FAIL;
        if (stored != null) player.displayClientMessage(Component.translatable("tc.resonator1", Integer.toString(amount), stored.getName()), false);
        else if (buffered != null) for (Aspect aspect : buffered.getAspectsSorted()) {
            player.displayClientMessage(Component.translatable("tc.resonator1", Integer.toString(buffered.getAmount(aspect)), aspect.getName()), false);
        }
        Object suctionName = suction == null ? Component.translatable("tc.resonator3") : suction.getName();
        player.displayClientMessage(Component.translatable("tc.resonator2", Integer.toString(pressure), suctionName), false);
        level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), KNOCK, SoundSource.BLOCKS, .5F, 1.9F + level.random.nextFloat() * .1F);
        return InteractionResult.CONSUME;
    }

    private static boolean validTarget(UseOnContext context, BlockEntity tile) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        // Inspection is read-only: no research, vis, durability, build-permission,
        // active-use or cooldown gate is introduced for the original free tool.
        return player != null && player.level() == level && player.isAlive() && !player.isSpectator()
                && player.getItemInHand(context.getHand()) == context.getItemInHand()
                && !context.getItemInHand().isEmpty() && context.getItemInHand().getItem() instanceof ItemResonator
                && level.hasChunkAt(pos) && level.isInWorldBounds(pos) && level.getWorldBorder().isWithinBounds(pos)
                && player.canInteractWithBlock(pos, 0) && !tile.isRemoved() && tile.getLevel() == level
                && tile.getBlockPos().equals(pos) && level.getBlockEntity(pos) == tile
                && level.getBlockState(pos).is(tile.getBlockState().getBlock());
    }

    private static boolean visible(Player player, BlockPos pos) {
        Vec3 start = player.getEyePosition(1), end = start.add(player.getViewVector(1).scale(player.blockInteractionRange()));
        BlockHitResult first = player.level().clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return first.getType() == HitResult.Type.BLOCK && first.getBlockPos().equals(pos);
    }
}
