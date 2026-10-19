package thaumcraft.common.tiles;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.visnet.TileVisNode;
import thaumcraft.api.visnet.VisNetHandler;
import thaumcraft.common.blocks.BlockVisRelay;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftNodeDevices;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.utils.DeferredItemDrops;

/** Original relay link attunement and five-tick RGB pulse. No stock, amulet,
 * player-entity-ID map, aspect filter or independent generation lives here. */
public final class TileVisRelay extends TileVisNode {
    private static final int[] COLORS = {0xffff7e, 0xff3c01, 0x0090ff, 0x00a000, 0xeeccff, 0x555577};
    private static final String[] SHARDS = {"aer", "ignis", "aqua", "terra", "ordo", "perditio", "balanced"};
    private static final SoundEvent CRYSTAL = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "crystal"));
    private byte color = -1;
    private int pulse;
    private float pRed = .5F, pGreen = .5F, pBlue = .5F;
    private CompoundTag rejected;
    private boolean ticking, interacting, dropping, supportDropQueued;

    public TileVisRelay(BlockPos pos, BlockState state) { super(ThaumcraftNodeDevices.VIS_RELAY_BLOCK_ENTITY.get(), pos, state); }
    @Override public int getRange() { return 8; }
    @Override public boolean isSource() { return false; }
    @Override public byte getAttunement() { return color; }
    @Override protected boolean isNetworkEnabled() { return rejected == null; }
    @Override protected boolean matchesTickState(BlockState expected) {
        BlockState now = getBlockState();
        return now == expected || expected.is(ThaumcraftNodeDevices.VIS_RELAY.get()) && now.is(ThaumcraftNodeDevices.VIS_RELAY.get())
                && expected.setValue(BlockVisRelay.LINKED, now.getValue(BlockVisRelay.LINKED)) == now;
    }
    public boolean isQuarantined() { return rejected != null; }
    public Direction getOrientation() { return getBlockState().getValue(BlockVisRelay.FACING); }
    public int getPulse() { return pulse; }
    public float getPulseRed() { return pRed; }
    public float getPulseGreen() { return pGreen; }
    public float getPulseBlue() { return pBlue; }
    public static int colorFor(int index) {
        if (index < 0 || index >= COLORS.length) throw new IllegalArgumentException("Relay color must be0..5");
        return COLORS[index];
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileVisRelay relay) {
        if (relay.level != level || !relay.worldPosition.equals(pos) || relay.getBlockState() != state || relay.isRemoved()
                || relay.ticking || relay.interacting || relay.dropping || relay.isNodeTickActive() || relay.isConsumeActive()) return;
        relay.ticking = true;
        try {
            if (!relay.isCurrentNode()) return;
            if (level.isClientSide) {
                relay.pRed = Math.min(1, relay.pRed + .025F);
                relay.pGreen = Math.min(1, relay.pGreen + .025F);
                relay.pBlue = Math.min(1, relay.pBlue + .025F);
            } else if (!VisNetHandler.isNativeServerLevel(level)) return;
            if (relay.pulse > 0) --relay.pulse; // drawEffect precedes original graph phase.
            if (!level.isClientSide) TileVisNode.tick(level, pos, state, relay);
        } finally { relay.ticking = false; }
    }
    @Override public void parentChanged() {
        Level world = level; BlockState state = getBlockState();
        if (world == null || !isCurrentNode()) return;
        if (world.isClientSide) { world.getLightEngine().checkBlock(worldPosition); return; }
        // Original BlockMetalDevice766-770:10 with a parent,2 without. Native
        // emission is state-only, so cache a DISPLAY hint, never a routing gate.
        boolean linked = getParent() != null;
        if (state.getValue(BlockVisRelay.LINKED) != linked && isCurrentNode() && level == world && getBlockState() == state) {
            world.setBlock(worldPosition, state.setValue(BlockVisRelay.LINKED, linked), Block.UPDATE_CLIENTS);
        }
    }
    @Override public void triggerConsumeEffect(Aspect aspect) {
        if (level == null || level.isClientSide || pulse != 0 || !isCurrentNode() || rejected != null) return;
        int index = aspect == Aspect.AIR ? 0 : aspect == Aspect.FIRE ? 1 : aspect == Aspect.WATER ? 2
                : aspect == Aspect.EARTH ? 3 : aspect == Aspect.ORDER ? 4 : aspect == Aspect.ENTROPY ? 5 : -1;
        if (index < 0) return; // Legitimate API compounds can flow; they have no original primal pulse color.
        pulse = 5; // Commit the throttle BEFORE block-event callbacks.
        level.blockEvent(worldPosition, getBlockState().getBlock(), 0, index);
    }
    @Override public boolean triggerEvent(int id, int value) {
        if (id != 0) return super.triggerEvent(id, value);
        if (value < 0 || value >= COLORS.length || !isCurrentNode() || rejected != null) return false;
        if (level.isClientSide) {
            int rgb = COLORS[value];
            float red = (rgb >> 16 & 255) / 255F, green = (rgb >> 8 & 255) / 255F, blue = (rgb & 255) / 255F;
            setPulse(red, green, blue);
            Set<TileVisNode> seen = Collections.newSetFromMap(new IdentityHashMap<>()); seen.add(this);
            TileVisNode at = this;
            while (true) {
                WeakReference<TileVisNode> ref = at.getParent(); TileVisNode parent = ref == null ? null : ref.get();
                if (!(parent instanceof TileVisRelay relay) || !seen.add(relay) || relay.pulse != 0 || !relay.isCurrentNode()) break;
                relay.setPulse(red, green, blue); at = relay;
            }
        }
        return true;
    }
    private void setPulse(float red, float green, float blue) { pulse = 5; pRed = red; pGreen = green; pBlue = blue; }

    /** Both native block use and root's secondary-wand item hook enter here.
     * The actor/held stack is never consumed, charged, tagged or reassigned. */
    public InteractionResult interact(UseOnContext context) {
        if (interacting || ticking || dropping || isNodeTickActive() || isConsumeActive()) return InteractionResult.FAIL;
        interacting = true;
        try {
            Level world = level; BlockState state = getBlockState(); Player player = context.getPlayer(); ItemStack held = context.getItemInHand();
            if (world == null || context.getLevel() != world || !worldPosition.equals(context.getClickedPos()) || player == null) return InteractionResult.PASS;
            boolean wand = held.getItem() instanceof ItemWandCasting; int shard = shardIndex(held);
            if (!wand && (shard < 0 || player.isShiftKeyDown())) return InteractionResult.PASS;
            if (!world.isClientSide && !VisNetHandler.isNativeServerLevel(world)) return InteractionResult.FAIL;
            if (rejected != null || !isCurrentNode() || player.level() != world || player.isSpectator()
                    || player.getItemInHand(context.getHand()) != held || !player.canInteractWithBlock(worldPosition, 0)
                    || !world.mayInteract(player, worldPosition) || !player.mayUseItemAt(worldPosition, context.getClickedFace(), held)
                    || !isCurrentNode() || level != world || getBlockState() != state || player.getItemInHand(context.getHand()) != held) return InteractionResult.FAIL;
            if (world.isClientSide) return InteractionResult.SUCCESS;
            byte next = wand ? (byte) (color == 5 ? -1 : color + 1) : shard == 6 || shard == color ? (byte) -1 : (byte) shard;
            color = next; nodeRefresh = true; // New policy exists before detach/sync call-outs.
            removeThisNode();
            if (!isCurrentNode() || level != world || !matchesTickState(state)) return InteractionResult.CONSUME;
            state = getBlockState(); // Detaching may legitimately change ONLY the LINKED light hint.
            setChanged();
            if (!isCurrentNode() || level != world || getBlockState() != state) return InteractionResult.CONSUME;
            world.sendBlockUpdated(worldPosition, state, state, 3);
            if (isCurrentNode() && level == world && getBlockState() == state) world.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), CRYSTAL, SoundSource.BLOCKS, .2F, 1);
            return InteractionResult.CONSUME;
        } finally { interacting = false; }
    }
    private static int shardIndex(ItemStack stack) {
        for (int i = 0; i < SHARDS.length; ++i) {
            var shard = ThaumcraftContent.SHARDS.get(SHARDS[i]);
            if (shard != null && stack.is(shard.get())) return i;
        }
        return -1;
    }

    /** Original opposite support needs only non-AIR. Capture native plain loot,
     * retire the exact owner, THEN use accepted durable physical-drop delivery. */
    public void dropIfUnsupported() {
        if (dropping || interacting || ticking || supportDropQueued || level == null || level.isClientSide) return;
        dropping = true;
        try {
            if (!VisNetHandler.isNativeServerLevel(level) || !isCurrentNode()) return;
            ServerLevel world = (ServerLevel) level; BlockState state = getBlockState();
            BlockPos support = worldPosition.relative(getOrientation().getOpposite());
            if (!world.isInWorldBounds(support) || !world.hasChunkAt(support) || !world.getBlockState(support).isAir()) return;
            List<ItemStack> drops = world.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)
                    ? Block.getDrops(state, world, worldPosition, this).stream().map(ItemStack::copy).toList() : List.of();
            if (!isCurrentNode() || level != world || getBlockState() != state || !world.hasChunkAt(support) || !world.getBlockState(support).isAir() || !isCurrentNode()) return;
            try { world.removeBlock(worldPosition, false); }
            finally {
                if (!supportDropQueued && isRemoved() && world.hasChunkAt(worldPosition) && world.getBlockEntity(worldPosition) != this) {
                    supportDropQueued = true;
                    DeferredItemDrops.enqueueBlockHarvest(world, worldPosition, drops);
                }
            }
        } finally { dropping = false; }
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (ticking || interacting || dropping || isConsumeActive()) throw new IllegalStateException("Relay reload during native mutation");
        super.loadAdditional(tag, registries);
        rejected = null; color = -1;
        if (tag.contains("RejectedVisRelay")) {
            rejected = tag.get("RejectedVisRelay") instanceof CompoundTag saved ? saved.copy() : tag.copy();
        } else if (!tag.contains("color", Tag.TAG_BYTE) || tag.getByte("color") < -1 || tag.getByte("color") > 5
                || !tag.contains("orientation", Tag.TAG_BYTE) || tag.getByte("orientation") < 0 || tag.getByte("orientation") > 5
                || tag.getByte("orientation") != getOrientation().get3DDataValue()) {
            rejected = tag.copy();
        } else color = tag.getByte("color");
        if (level == null || !level.isClientSide) { pulse = 0; pRed = pGreen = pBlue = .5F; }
        supportDropQueued = false;
        if (level != null && level.isClientSide) parentChanged();
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("orientation", (byte) getOrientation().get3DDataValue()); // Native FACING remains authoritative.
        tag.putByte("color", color);
        if (rejected != null) tag.put("RejectedVisRelay", rejected.copy());
    }
}
