package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.visnet.TileVisNode;
import thaumcraft.api.visnet.VisNetHandler;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftVisCharging;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;

/** Original Vis Charge Relay: an eight-block VisNet relay charging the actual workbench wand immediately below it, five CV per primal per tick. */
public final class TileMagicWorkbenchCharger extends TileVisNode {
    private static final SoundEvent CRYSTAL = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "crystal"));
    private static final String[] SHARDS = {"aer", "ignis", "aqua", "terra", "ordo", "perditio", "balanced"};
    private static final String[] ASPECTS = {"aer", "terra", "ignis", "aqua", "ordo", "perditio"};
    private byte color = -1;
    private int pulse;
    private float red = .5F, green = .5F, blue = .5F;
    private boolean ticking, interacting;
    private CompoundTag rejected;
    private Charge charge;

    private static final class Charge {
        final TileArcaneWorkbench table;
        final BlockState tableState;
        final ItemStack wand;
        final WandData before;
        final WandData[] after;
        final Aspect aspect;
        final int limit;
        int credited;
        Charge(TileArcaneWorkbench table, ItemStack wand, Aspect aspect, int limit) {
            this.table = table; tableState = table.getBlockState(); this.wand = wand;
            before = ItemWandCasting.data(wand); this.aspect = aspect; this.limit = limit;
            after = new WandData[limit + 1];
            for (int units = 1; units <= limit; units++) after[units] = before.withAmount(aspect.getTag(), before.amount(aspect.getTag()) + units);
        }
    }

    public TileMagicWorkbenchCharger(BlockPos pos, BlockState state) { super(ThaumcraftVisCharging.VIS_CHARGE_RELAY_TILE.get(), pos, state); }
    @Override public int getRange() { return 8; }
    @Override public boolean isSource() { return false; }
    @Override public byte getAttunement() { return color; }
    @Override protected boolean isNetworkEnabled() { return rejected == null; }
    public Direction getOrientation() { return Direction.DOWN; }
    public int getPulse() { return pulse; }
    public float getPulseRed() { return red; }
    public float getPulseGreen() { return green; }
    public float getPulseBlue() { return blue; }
    public boolean isQuarantined() { return rejected != null; }
    public boolean hasPreparedVisCharge() { return ticking && charge != null; }

    public static void tick(Level level, BlockPos pos, BlockState state, TileMagicWorkbenchCharger tile) {
        if (tile.level != level || !tile.worldPosition.equals(pos) || tile.isRemoved() || tile.getBlockState() != state
                || tile.ticking || tile.interacting || tile.isNodeTickActive() || tile.isConsumeActive()) return;
        tile.ticking = true;
        try {
            if (!tile.isCurrentNode()) return;
            if (tile.pulse > 0) --tile.pulse;
            if (level.isClientSide) {
                tile.red = Math.min(1, tile.red + .025F); tile.green = Math.min(1, tile.green + .025F); tile.blue = Math.min(1, tile.blue + .025F);
                return;
            }
            if (!VisNetHandler.isNativeServerLevel(level)) return;
            TileVisNode.tick(level, pos, state, tile);
            if (!tile.isCurrentNode() || tile.rejected != null || !level.hasChunkAt(pos.below())
                    || !(level.getBlockEntity(pos.below()) instanceof TileArcaneWorkbench table)) return;
            for (String tag : ASPECTS) {
                if (!tile.isCurrentNode() || table.isRemoved() || level.getBlockEntity(pos.below()) != table) break;
                ItemStack wand = table.getItem(TileArcaneWorkbench.WAND_SLOT);
                if (wand.isEmpty() || !(wand.getItem() instanceof ItemWandCasting)) break;
                WandData data = ItemWandCasting.data(wand);
                int limit = Math.min(5, ItemWandCasting.getMaxVis(wand) - data.amount(tag));
                if (limit <= 0) continue;
                tile.charge = new Charge(table, wand, Aspect.getAspect(tag), limit);
                try {
                    tile.consumeVis(tile.charge.aspect, limit); // The closed native grant hook credits BEFORE any pulse callback.
                    if (tile.charge.credited > 0 && !table.isRemoved() && level.getBlockEntity(pos.below()) == table) table.setChanged();
                } finally { tile.charge = null; }
            }
        } finally { tile.charge = null; tile.ticking = false; }
    }

    /** Read-only final pre-debit fence called only by the native VisNet executor after route observations. */
    public boolean readyForVisGrant(Aspect aspect, int units) {
        Charge prepared = charge;
        return ticking && rejected == null && prepared != null && prepared.credited == 0 && prepared.aspect == aspect
                && units > 0 && units <= prepared.limit && isConsumeActive() && isCurrentNode()
                && level instanceof ServerLevel server && server.getServer().isSameThread()
                && server.hasChunkAt(worldPosition.below()) && server.getBlockEntity(worldPosition.below()) == prepared.table
                && !prepared.table.isRemoved() && prepared.table.getBlockState() == prepared.tableState
                && server.getBlockState(worldPosition.below()) == prepared.tableState
                && prepared.table.getItem(TileArcaneWorkbench.WAND_SLOT) == prepared.wand
                && ItemWandCasting.data(prepared.wand).equals(prepared.before);
    }

    /** Exact executor-only callback-free component replacement. Prepared real wand owns the paid credit before removal/drop callbacks. */
    public void acceptPreparedVisGrant(Aspect aspect, int units) {
        VisNetHandler.requireWorkbenchGrant(this, aspect, units);
        Charge prepared = charge;
        if (!ticking || prepared == null || prepared.credited != 0 || prepared.aspect != aspect || units <= 0 || units > prepared.limit
                || !ItemWandCasting.data(prepared.wand).equals(prepared.before)) throw new IllegalStateException("No exact prepared workbench credit");
        prepared.wand.set(ThaumcraftContent.WAND_DATA.get(), prepared.after[units]);
        prepared.credited = units;
    }

    public InteractionResult interact(UseOnContext context) {
        if (ticking || interacting || isNodeTickActive() || isConsumeActive()) return InteractionResult.FAIL;
        Level world = level; var player = context.getPlayer(); ItemStack held = context.getItemInHand();
        if (world == null || player == null || context.getLevel() != world || !worldPosition.equals(context.getClickedPos())) return InteractionResult.PASS;
        boolean wand = held.getItem() instanceof ItemWandCasting;
        int shard = -1;
        for (int index = 0; index < SHARDS.length; index++) if (held.is(ThaumcraftContent.SHARDS.get(SHARDS[index]).get())) { shard = index; break; }
        if (!wand && (shard < 0 || player.isShiftKeyDown())) return InteractionResult.PASS;
        if (!world.isClientSide && !VisNetHandler.isNativeServerLevel(world)) return InteractionResult.FAIL;
        if (!isCurrentNode() || rejected != null || player.level() != world || player.isSpectator() || player.getItemInHand(context.getHand()) != held
                || !player.canInteractWithBlock(worldPosition, 0) || !world.mayInteract(player, worldPosition)
                || !player.mayUseItemAt(worldPosition, context.getClickedFace(), held)) return InteractionResult.FAIL;
        if (world.isClientSide) return InteractionResult.SUCCESS;
        interacting = true;
        try {
            color = wand ? (byte) (color == 5 ? -1 : color + 1) : shard == 6 || shard == color ? (byte) -1 : (byte) shard;
            nodeRefresh = true;
            removeThisNode();
            if (isCurrentNode()) { setChanged(); world.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }
            if (isCurrentNode()) world.playSound(null, worldPosition, CRYSTAL, SoundSource.BLOCKS, .2F, 1);
            return InteractionResult.CONSUME;
        } finally { interacting = false; }
    }

    @Override public void triggerConsumeEffect(Aspect aspect) {
        if (level == null || level.isClientSide || pulse != 0 || rejected != null || !isCurrentNode()) return;
        int index = aspect == Aspect.AIR ? 0 : aspect == Aspect.FIRE ? 1 : aspect == Aspect.WATER ? 2
                : aspect == Aspect.EARTH ? 3 : aspect == Aspect.ORDER ? 4 : aspect == Aspect.ENTROPY ? 5 : -1;
        if (index < 0) return;
        pulse = 5; level.blockEvent(worldPosition, getBlockState().getBlock(), 0, index);
    }
    @Override public boolean triggerEvent(int id, int value) {
        if (id != 0) return super.triggerEvent(id, value);
        if (value < 0 || value > 5 || rejected != null || !isCurrentNode()) return false;
        if (level.isClientSide) {
            int rgb = TileVisRelay.colorFor(value);
            red = Math.min(1, (rgb >> 16 & 255) / 255F + .1F); green = Math.min(1, (rgb >> 8 & 255) / 255F + .1F); blue = Math.min(1, (rgb & 255) / 255F + .1F); pulse = 5;
        }
        return true;
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (ticking || interacting || isConsumeActive()) throw new IllegalStateException("Charger reload during native mutation");
        super.loadAdditional(tag, registries);
        rejected = null; color = -1;
        if (tag.contains("RejectedVisChargeRelay")) rejected = tag.get("RejectedVisChargeRelay") instanceof CompoundTag saved ? saved.copy() : tag.copy();
        else if (tag.contains("color") && (!tag.contains("color", Tag.TAG_BYTE) || tag.getByte("color") < -1 || tag.getByte("color") > 5)) rejected = tag.copy();
        else if (tag.contains("color")) color = tag.getByte("color");
        charge = null; pulse = 0; red = green = blue = .5F;
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (rejected != null) tag.put("RejectedVisChargeRelay", rejected.copy());
        else { tag.putByte("color", color); tag.putShort("orientation", (short) 0); }
    }
}
