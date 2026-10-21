package thaumcraft.common.items.wands.foci;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.FocusVis;
import thaumcraft.common.items.wands.WandVisDiscount;
import thaumcraft.common.lib.network.ExcavationFxNetworking;
import thaumcraft.common.lib.world.ExcavationHarvest;

/**
 * Original held-channel Excavation with its Frugal/Potency/Treasure/Enlarge/Silk/Dowsing upgrades. Native
 * held ticks, not ordinary mining speed/attack, drive coordinate-only progress.
 * Server-owned weak IDENTITY keys avoid Entity.equals' numeric-ID aliasing;
 * values retain no player, level, stack or block state. Dimension, death/logout
 * and player-instance boundaries are explicit safety adaptations to name keys.
 */
public final class ItemFocusExcavation extends ItemFocusBasic {
    public static final int BASE_COST_CENTIVIS = 15;
    public static final double RANGE = 10;
    public static final ResourceLocation FAST_BLOCKS_ID = ResourceLocation.fromNamespaceAndPath("thaumcraft", "excavation_fast");
    public static final TagKey<Block> FAST_BLOCKS = TagKey.create(Registries.BLOCK, FAST_BLOCKS_ID);
    private static final BlockPos RESET = new BlockPos(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
    private static final SoundEvent RUMBLE = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "rumble"));
    private static final ReferenceQueue<ServerPlayer> COLLECTED = new ReferenceQueue<>();
    private static final Map<PlayerKey, Progress> PROGRESS = new HashMap<>();
    private static final ExcavationVisCosts.Tooltip TOOLTIP_COST = new ExcavationVisCosts.Tooltip();

    public ItemFocusExcavation(Properties properties) { super(properties); }

    @Override public AspectList getVisCost(ItemStack focus) {
        boolean realFocus = focus.getItem() instanceof ItemFocusExcavation;
        boolean silk = realFocus && FocusUpgradeData.level(focus, FocusUpgradeType.silktouch) > 0;
        boolean dowsing = realFocus && FocusUpgradeData.level(focus, FocusUpgradeType.dowsing) > 0;
        var cost = new AspectList();
        TOOLTIP_COST.cost(silk, dowsing).forEach((tag, amount) -> cost.add(Aspect.getAspect(tag), amount));
        return cost;
    }
    @Override public boolean isVisCostPerTick(ItemStack focus) { return true; } // Original tooltip, NOT a per-tick debit.
    @Override public int getActivationCooldown(ItemStack focus) { return 0; }
    @Override public int getFocusColor(ItemStack focus) { return 409606; }
    @Override public String getSortingHelper(ItemStack focus) { return "BE" + super.getSortingHelper(focus); }
    @Override public WandFocusAnimation getAnimation(ItemStack focus) { return WandFocusAnimation.CHARGE; }
    @Override public FocusUpgradeType[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
        return switch (rank) {
            case 1 -> new FocusUpgradeType[]{FocusUpgradeType.frugal, FocusUpgradeType.potency, FocusUpgradeType.treasure};
            case 3 -> new FocusUpgradeType[]{FocusUpgradeType.frugal, FocusUpgradeType.potency, FocusUpgradeType.treasure, FocusUpgradeType.dowsing};
            case 5 -> new FocusUpgradeType[]{FocusUpgradeType.frugal, FocusUpgradeType.potency, FocusUpgradeType.treasure, FocusUpgradeType.silktouch};
            case 2, 4 -> new FocusUpgradeType[]{FocusUpgradeType.frugal, FocusUpgradeType.potency, FocusUpgradeType.enlarge};
            default -> new FocusUpgradeType[0];
        };
    }

    @Override public ItemStack onFocusRightClick(ItemStack wand, Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand); // Research gates crafting, not found-focus use.
        return wand;
    }

    public static int attemptCost(ItemStack wand, Player player) {
        // Preserve original wrong-stack pricing: normal wand use is base15,
        // never a speculative Silk/Dowsing surcharge from its nested focus.
        return (int) (BASE_COST_CENTIVIS * WandVisDiscount.focusModifier(wand, player, Aspect.EARTH));
    }

    @Override public void onUsingFocusTick(ItemStack wand, Player actor, int remaining) {
        if (!(actor instanceof ServerPlayer player) || player.level().isClientSide) return;
        if (!player.server.isSameThread()) throw new IllegalStateException("Excavation focus use requires the server thread");
        if (!player.isAlive() || player.isRemoved()) { clearPlayer(player); return; }
        InteractionHand hand = player.getUsedItemHand();
        if (!liveUse(player, wand, hand)) return;
        int price = attemptCost(wand, player);
        Map<String, Integer> costs = Map.of("terra", price);
        // FIRST: no ray, progress initialization/reset, sound or visual refresh
        // when unfunded. Creative neither bypasses this nor completes instantly.
        if (!FocusVis.canPay(wand, costs)) { player.stopUsingItem(); return; }

        var level = player.serverLevel();
        Progress progress = state(player);
        if (progress.busy) return; // Native callbacks cannot recursively spend/advance this channel.
        progress.busy = true;
        try {
            ItemStack before = wand.copy();
            Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
            BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(RANGE)),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            boolean impact = hit.getType() == HitResult.Type.BLOCK;
            Vec3 endpoint = impact ? hit.getLocation() : player.position().add(look.scale(RANGE));
            BlockPos pos = hit.getBlockPos();
            boolean eligible = impact && level.hasChunkAt(pos) && level.mayInteract(player, pos);
            var block = eligible ? level.getBlockState(pos) : Blocks.AIR.defaultBlockState();
            float hardness = eligible ? block.getDestroySpeed(level, pos) : -1;
            if (player.serverLevel() != level || price != attemptCost(wand, player)
                    || !liveUse(player, wand, hand) || !ItemStack.matches(wand, before) || !FocusVis.canPay(wand, costs)
                    || find(player) != progress) return;

            long now = System.currentTimeMillis();
            if (!impact) progress.soundAfter = 0;
            else if (progress.soundAfter < now) {
                progress.soundAfter = now + 1200L;
                level.playSound(null, endpoint.x, endpoint.y, endpoint.z, RUMBLE, SoundSource.PLAYERS, .3F, 1F);
            }
            if (player.serverLevel() != level || !liveUse(player, wand, hand)
                    || !ItemStack.matches(wand, before) || find(player) != progress) return;
            int crack = -1; // NO update, not a request to clear native cracks.
            boolean completed = false, success = false;
            if (!eligible) progress.reset();
            else if (hardness >= 0) { // Negative hardness deliberately preserves the PREVIOUS coordinates/progress.
                if (!progress.pos.equals(pos)) { progress.pos = pos.immutable(); progress.amount = 0; }
                else {
                    float old = progress.amount;
                    // Forward the ORIGINAL ratio, even >9 after replacing a
                    // block with a softer one: native renderer then clears it.
                    if (old > 0 && hardness > 0) crack = (int) (old / hardness * 9F);
                    if (old >= hardness) {
                        ItemStack paid = before.copy();
                        if (price != attemptCost(wand, player) || player.serverLevel() != level || find(player) != progress || !liveUse(player, wand, hand)
                                || !ItemStack.matches(wand, before) || !FocusVis.debit(paid, costs)) return;
                        // Commit BEFORE native protection/refusal, even if that
                        // attempt fails. Do not restore/refund a cancelled break.
                        wand.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(paid));
                        player.getInventory().setChanged();
                        completed = true;
                        try { success = ExcavationHarvest.attemptAndEnlarge(player, pos, wand); }
                        finally {
                            progress.reset();
                            player.inventoryMenu.broadcastChanges();
                            if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
                        }
                    } else {
                        int potency = ItemWandCasting.focusPotency(wand);
                        float speed = block.is(FAST_BLOCKS) ? .25F + potency * .25F : .05F + potency * .1F;
                        if (block.is(Blocks.OBSIDIAN)) speed *= 3F;
                        progress.amount = old + speed;
                    }
                }
            }
            float yaw = player.getYRot() * (float) Math.PI / 180;
            Vec3 origin = eye.add(-Math.cos(yaw) * .066, -.06, -Math.sin(yaw) * .04).add(look.scale(.3));
            if (player.serverLevel() == level && find(player) == progress)
                ExcavationFxNetworking.send(player, origin, endpoint, pos, crack, impact, completed, success);
        } finally { progress.busy = false; }
    }

    private static boolean liveUse(ServerPlayer player, ItemStack wand, InteractionHand hand) {
        return player.isAlive() && !player.isRemoved() && !player.isSpectator() && player.isUsingItem()
                && player.getUsedItemHand() == hand && player.getUseItem() == wand && player.getItemInHand(hand) == wand
                && wand.getCount() == 1 && ArcaneVis.isCraftingWand(wand) && !ItemWandCasting.isUsingNode(player)
                && FocusData.get(wand).getItem() instanceof ItemFocusExcavation;
    }

    // DO NOT override onPlayerStoppedUsingFocus. Original :269 overrides ITEM
    // stop, but ItemWandCasting:561-570 calls the inherited empty FOCUS stop.
    // Release/resume, replacing the block at the same coordinates, and swapping
    // wands therefore inherit progress, including the initial (0,0,0) quirk.

    /** Lifecycle ONLY: root wires death, dimension change and logout, never ordinary release. */
    public static void clearPlayer(ServerPlayer player) { reap(); PROGRESS.remove(new PlayerKey(player)); }
    /** Server-stop cleanup; there is deliberately no independent client progress simulation. */
    public static void clearAll() { PROGRESS.clear(); reap(); }
    /** Read-only live progress for HUD/diagnostics; missing state is not initialized by observation. */
    public static ProgressView progress(ServerPlayer player) {
        Progress state = find(player);
        return state == null || !state.dimension.equals(player.level().dimension().location()) ? null
                : new ProgressView(state.dimension, state.pos, state.amount);
    }
    public record ProgressView(ResourceLocation dimension, BlockPos pos, float amount) {}

    private static Progress state(ServerPlayer player) {
        reap();
        PlayerKey key = new PlayerKey(player);
        Progress state = PROGRESS.get(key);
        ResourceLocation dimension = player.level().dimension().location();
        if (state == null || !state.dimension.equals(dimension)) {
            state = new Progress(dimension); PROGRESS.put(key, state);
        }
        return state;
    }
    private static Progress find(ServerPlayer player) { reap(); return PROGRESS.get(new PlayerKey(player)); }
    private static void reap() { PlayerKey key; while ((key = (PlayerKey) COLLECTED.poll()) != null) PROGRESS.remove(key); }
    private static final class Progress {
        private final ResourceLocation dimension;
        private BlockPos pos = BlockPos.ZERO;
        private float amount;
        private long soundAfter;
        private boolean busy;
        private Progress(ResourceLocation dimension) { this.dimension = dimension; }
        private void reset() { pos = RESET; amount = 0; }
    }
    private static final class PlayerKey extends WeakReference<ServerPlayer> {
        private final int hash;
        private PlayerKey(ServerPlayer player) { super(player, COLLECTED); hash = System.identityHashCode(player); }
        @Override public int hashCode() { return hash; }
        @Override public boolean equals(Object other) {
            return this == other || other instanceof PlayerKey key && get() != null && get() == key.get();
        }
    }
}
