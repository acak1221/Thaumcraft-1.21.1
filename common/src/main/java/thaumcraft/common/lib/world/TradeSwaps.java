package thaumcraft.common.lib.world;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.items.wands.FocusVis;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.FocusUpgradeData;
import thaumcraft.common.items.wands.foci.ItemFocusTrade;
import thaumcraft.common.lib.utils.DeferredItemDrops;

/** Original one-success-per-level-tick swap queue. Queued work owns no inventory, vis, drops or chunk tickets. */
public final class TradeSwaps {
    public interface Protection { boolean allows(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState before, BlockState after); }
    private record Job(ServerPlayer player, ItemStack wand, int slot, BlockPos pos, BlockState source, BlockState target, int life) {}
    private static final Map<ServerLevel, ArrayDeque<Job>> JOBS = new WeakHashMap<>();
    private static final ThreadLocal<Boolean> ACTIVE = new ThreadLocal<>();
    private static final SoundEvent WAND = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "wand"));
    private static Protection protection;
    private static boolean initialized;
    private TradeSwaps() {}

    public static void install(Protection nativeProtection) { protection = java.util.Objects.requireNonNull(nativeProtection); }
    public static void init() {
        if (initialized) return;
        initialized = true;
        TickEvent.SERVER_LEVEL_PRE.register(TradeSwaps::tick);
        LifecycleEvent.SERVER_LEVEL_UNLOAD.register(JOBS::remove);
    }
    public static boolean actor(ServerPlayer player, ItemStack wand) {
        return player.server.isSameThread() && player.level() instanceof ServerLevel && player.isAlive() && !player.isRemoved()
                && !player.isSpectator() && !player.hasDisconnected() && player.connection != null
                && player.server.getPlayerList().getPlayer(player.getUUID()) == player && player.getMainHandItem() == wand
                && ArcaneVis.isCraftingWand(wand) && wand.getCount() == 1 && FocusData.get(wand).getItem() instanceof ItemFocusTrade;
    }
    public static boolean available(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        return level.isInWorldBounds(pos) && level.getWorldBorder().isWithinBounds(pos) && level.hasChunkAt(pos) && level.mayInteract(player, pos);
    }
    public static boolean exposed(Level level, BlockPos pos) {
        for (Direction face : Direction.values()) {
            BlockPos next = pos.relative(face);
            if (level.hasChunkAt(next) && !level.getBlockState(next).isSolidRender(level, next)) return true;
        }
        return false;
    }
    public static BlockState selection(ServerLevel level, ItemStack wand) {
        var data = wand.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!data.contains("picked", net.minecraft.nbt.Tag.TAG_COMPOUND)) return null;
        BlockState state = NbtUtils.readBlockState(level.registryAccess().lookupOrThrow(Registries.BLOCK), data.getCompound("picked"));
        return selectable(state) ? state.getBlock().defaultBlockState() : null;
    }
    public static boolean remember(ServerPlayer player, ItemStack wand, BlockPos pos) {
        if (ACTIVE.get() != null || !actor(player, wand) || !available(player, pos)) return false;
        BlockState state = player.serverLevel().getBlockState(pos);
        if (!selectable(state) || player.serverLevel().getBlockEntity(pos) != null) return false;
        var data = wand.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        // Original picked an ItemStack, not the live block's liquid, double-slab quantity, power or growth state.
        data.put("picked", NbtUtils.writeBlockState(state.getBlock().defaultBlockState()));
        wand.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        return true;
    }
    public static boolean enqueue(ServerPlayer player, ItemStack wand, BlockPos pos, int life) {
        if (ACTIVE.get() != null || life < 0 || life > 8 || !actor(player, wand) || !available(player, pos)) return false;
        ServerLevel level = player.serverLevel();
        BlockState source = level.getBlockState(pos), target = selection(level, wand);
        if (!sourceAllowed(level, pos, source) || target == null || source == target) return false;
        JOBS.computeIfAbsent(level, ignored -> new ArrayDeque<>()).addLast(new Job(player, wand, player.getInventory().selected, pos.immutable(), source, target, life));
        level.playSound(null, player.blockPosition(), WAND, SoundSource.PLAYERS, .25F, 1F);
        return true;
    }
    private static boolean sourceAllowed(ServerLevel level, BlockPos pos, BlockState source) {
        return !source.isAir() && !source.hasBlockEntity() && level.getBlockEntity(pos) == null && source.getDestroySpeed(level, pos) >= 0
                && !source.is(net.minecraft.tags.TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("thaumcraft", "taint")));
    }
    private static boolean selectable(BlockState state) {
        return !state.isAir() && !state.hasBlockEntity() && state.getBlock().asItem() instanceof net.minecraft.world.item.BlockItem
                && !(state.getBlock() instanceof net.minecraft.world.level.block.DoorBlock)
                && !(state.getBlock() instanceof net.minecraft.world.level.block.BedBlock)
                && !(state.getBlock() instanceof net.minecraft.world.level.block.DoublePlantBlock);
    }
    public static void tick(ServerLevel level) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Trade swaps require the server thread");
        if (ACTIVE.get() != null) return;
        ArrayDeque<Job> queue = JOBS.get(level);
        if (queue == null) return;
        ACTIVE.set(Boolean.TRUE);
        try {
            while (!queue.isEmpty()) {
                Job job = queue.removeFirst();
                try {
                    if (!swap(level, job)) continue;
                    if (job.life > 0) {
                        for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
                            if (x == 0 && y == 0 && z == 0) continue;
                            BlockPos next = job.pos.offset(x, y, z);
                            if (level.hasChunkAt(next) && level.getBlockState(next) == job.source && exposed(level, next))
                                queue.addLast(new Job(job.player, job.wand, job.slot, next, job.source, job.target, job.life - 1));
                        }
                    }
                    break;
                } catch (RuntimeException failure) { Thaumcraft.LOG.warn("Trade swap stopped at {}; actual paid state retained", job.pos, failure); }
            }
            if (queue.isEmpty()) JOBS.remove(level);
        } finally { ACTIVE.remove(); }
    }
    private static boolean current(ServerLevel level, Job job) {
        return job.player.level() == level && actor(job.player, job.wand) && job.player.getInventory().selected == job.slot
                && available(job.player, job.pos) && level.getBlockState(job.pos) == job.source && sourceAllowed(level, job.pos, job.source)
                && selection(level, job.wand) == job.target;
    }
    private static int materialSlot(ServerPlayer player, BlockState target) {
        for (int slot = 0; slot < 36; slot++) if (player.getInventory().getItem(slot).is(target.getBlock().asItem())) return slot;
        return -1;
    }
    private static boolean swap(ServerLevel level, Job job) {
        if (protection == null || !current(level, job)) return false;
        ServerPlayer player = job.player;
        ItemStack beforeWand = job.wand.copy();
        boolean creative = player.getAbilities().instabuild;
        int slot = materialSlot(player, job.target);
        if (!creative && slot < 0) return false;
        ItemStack material = creative ? ItemStack.EMPTY : player.getInventory().getItem(slot), beforeMaterial = material.copy();
        ItemStack focus = FocusData.get(job.wand);
        Map<String, Integer> costs = FocusVis.price(((ItemFocusTrade) focus.getItem()).getVisCost(focus), job.wand, player);
        if (!FocusVis.canPay(job.wand, costs) || !protection.allows(level, player, job.pos, job.source, job.target)
                || !current(level, job) || !ItemStack.matches(job.wand, beforeWand)
                || !creative && (player.getInventory().getItem(slot) != material || !ItemStack.matches(material, beforeMaterial))) return false;
        ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        int fortune = FocusUpgradeData.level(focus, FocusUpgradeType.treasure);
        if (fortune > 0) tool.enchant(enchantments.getOrThrow(Enchantments.FORTUNE), fortune);
        if (FocusUpgradeData.level(focus, FocusUpgradeType.silktouch) > 0) tool.enchant(enchantments.getOrThrow(Enchantments.SILK_TOUCH), 1);
        List<ItemStack> drops = creative ? List.of() : Block.getDrops(job.source, level, job.pos, null, player, tool);
        if (!current(level, job) || !ItemStack.matches(job.wand, beforeWand)
                || !creative && (player.getInventory().getItem(slot) != material || !ItemStack.matches(material, beforeMaterial))) return false;
        // Protection and loot callbacks can change worn vis discounts without touching the wand.
        costs = FocusVis.price(((ItemFocusTrade) focus.getItem()).getVisCost(focus), job.wand, player);
        if (!FocusVis.canPay(job.wand, costs)) return false;
        ItemStack paid = beforeWand.copy();
        if (!creative && !FocusVis.debit(paid, costs)) return false;
        // Reserve both payments before native placement can notify neighbours. No output exists until the replacement succeeds.
        if (!creative) {
            material.shrink(1);
            job.wand.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(paid));
        }
        boolean changed = level.setBlock(job.pos, job.target, Block.UPDATE_ALL);
        if (!changed && level.getBlockState(job.pos) == job.source) {
            if (!creative) {
                if (player.getInventory().getItem(slot) == material) material.grow(1);
                else {
                    ItemStack refund = beforeMaterial.copyWithCount(1);
                    if (!player.getInventory().add(refund) && !refund.isEmpty()) DeferredItemDrops.enqueue(level, job.pos, List.of(refund));
                }
                if (ItemWandCasting.data(job.wand).equals(ItemWandCasting.data(paid)))
                    job.wand.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(beforeWand));
            }
            return false;
        }
        // A callback may remove the placed block after the native commit; it cannot make either paid input spendable again.
        for (ItemStack drop : drops) if (!player.getInventory().add(drop) && !drop.isEmpty()) DeferredItemDrops.enqueue(level, job.pos, List.of(drop));
        if (level.getBlockState(job.pos).is(job.target.getBlock()))
            job.target.getBlock().setPlacedBy(level, job.pos, level.getBlockState(job.pos), player, new ItemStack(job.target.getBlock()));
        level.levelEvent(2001, job.pos, Block.getId(job.source));
        // Original ServerTickEventsFML:154 - the swapped block flashes with the
        // original pale blue 0xC0C0FF of the PacketFXBlockSparkle.
        thaumcraft.common.lib.network.BlockSparkleFxNetworking.sendBlockSparkle(level, job.pos, 0xC0C0FF);
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        return true;
    }
}
