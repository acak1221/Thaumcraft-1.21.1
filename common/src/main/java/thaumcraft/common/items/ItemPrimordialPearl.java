package thaumcraft.common.items;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.FluxFiniteBlock;
import thaumcraft.common.config.ThaumcraftFlux;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.tiles.TileNode;

/** Original ItemEldritchObject metadata3, flattened; node use consumes even in creative. */
public final class ItemPrimordialPearl extends Item {
    private enum Phase { PREPARING, PAID, APPLIED }
    private static final ThreadLocal<Receipt> ACTIVE = new ThreadLocal<>();
    private static final class Receipt {
        final ItemPrimordialPearl item;
        final ServerPlayer player;
        final InteractionHand hand;
        final ItemStack stack, snapshot;
        final int count;
        final TileNode node;
        TileNode.PrimalPearlChange change;
        Phase phase = Phase.PREPARING;
        Receipt(ItemPrimordialPearl item, ServerPlayer player, InteractionHand hand, ItemStack stack, TileNode node) {
            this.item = item; this.player = player; this.hand = hand; this.stack = stack;
            this.snapshot = stack.copy(); this.count = stack.getCount(); this.node = node;
        }
    }
    public ItemPrimordialPearl(Properties properties) { super(properties.stacksTo(1).rarity(Rarity.EPIC)); }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.ItemEldritchObject.text.5").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("item.ItemEldritchObject.text.6").withStyle(ChatFormatting.DARK_PURPLE));
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (ACTIVE.get() != null) return InteractionResult.FAIL;
        var player = context.getPlayer(); var world = context.getLevel();
        ItemStack stack = context.getItemInHand(); BlockPos pos = context.getClickedPos();
        if (player == null || player.isSpectator() || player.level() != world || stack.isEmpty() || stack.getItem() != this
                || player.getItemInHand(context.getHand()) != stack || !world.isLoaded(pos)) return InteractionResult.FAIL;
        if (!(world.getBlockEntity(pos) instanceof TileNode node)) return InteractionResult.PASS;
        if (world.isClientSide) { player.swing(context.getHand()); return InteractionResult.SUCCESS; }
        if (!(world instanceof ServerLevel level) || !(player instanceof ServerPlayer actor) || !level.getServer().isSameThread()) return InteractionResult.FAIL;
        Receipt receipt = new Receipt(this, actor, context.getHand(), stack, node);
        ACTIVE.set(receipt);
        boolean paid = false;
        try {
            if (!actorCurrent(receipt, level, pos, context) || !effectAreaLoaded(level, pos)) return InteractionResult.FAIL;
            boolean researched = KnowledgeAccess.get(actor).isResearchComplete("PRIMNODE");
            receipt.change = node.preparePrimalPearl(level.random, researched);
            if (receipt.change == null) return InteractionResult.FAIL;
            // Allocate the creative-wrapper replacement before committing any debit.
            ItemStack remainder = stack.copy(); remainder.shrink(1);
            if (!actorCurrent(receipt, level, pos, context) || !effectAreaLoaded(level, pos)
                    || existing(level, pos) != node || !node.matchesPreparedPrimalPearl(receipt.change)
                    || !stackCurrent(receipt)) return InteractionResult.FAIL;
            // Native stack debit and opaque node assignment are adjacent, without world queries or callbacks.
            stack.shrink(1); receipt.phase = Phase.PAID; paid = true;
            node.applyPreparedPrimalPearl(receipt.change);
            // ServerPlayerGameMode restores the original argument's count in creative; detach its hand reference.
            if (actor.getItemInHand(receipt.hand) == stack) actor.setItemInHand(receipt.hand, remainder);
            actor.swing(receipt.hand);
            node.setChanged();
            BlockState state = node.getBlockState();
            if (existing(level, pos) == node) level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
            // Original six-argument createExplosion(..., smoking=true) has NO fire and DOES damage blocks.
            level.explode(null, pos.getX() + .5D, pos.getY() + 1.5D, pos.getZ() + .5D,
                    3.0F + level.random.nextFloat() * (researched ? 3 : 5), false, Level.ExplosionInteraction.BLOCK);
            placeTrials(level, pos);
            return InteractionResult.CONSUME;
        } catch (RuntimeException failure) {
            if (!paid) throw failure;
            // A spent pearl never refunds/retries an already applied explosion or a callback-partial effect.
            Thaumcraft.LOG.warn("Primordial pearl effects interrupted after payment at {}", pos, failure);
            return InteractionResult.CONSUME;
        } finally { ACTIVE.remove(); }
    }
    /** Closed native debit receipt. Node raw-apply consumes it once; no world calls are legal here. */
    public static void requirePreparedNodeMutation(TileNode node, TileNode.PrimalPearlChange change) {
        Receipt receipt = ACTIVE.get();
        if (receipt == null || receipt.phase != Phase.PAID || receipt.node != node || receipt.change != change)
            throw new IllegalStateException("Primordial pearl node mutation requires its exact paid native item receipt");
        receipt.phase = Phase.APPLIED;
    }
    private static boolean stackCurrent(Receipt receipt) {
        return receipt.player.getItemInHand(receipt.hand) == receipt.stack && receipt.stack.getItem() == receipt.item
                && receipt.stack.getCount() == receipt.count && receipt.count > 0
                && ItemStack.isSameItemSameComponents(receipt.stack, receipt.snapshot);
    }
    private static boolean actorCurrent(Receipt receipt, ServerLevel level, BlockPos pos, UseOnContext context) {
        ServerPlayer actor = receipt.player;
        return actor.level() == level && !actor.isRemoved() && !actor.isSpectator()
                && level.getServer().getPlayerList().getPlayer(actor.getUUID()) == actor
                && actor.canInteractWithBlock(pos, 0) && level.mayInteract(actor, pos)
                && actor.mayUseItemAt(pos, context.getClickedFace(), receipt.stack)
                && level.getWorldBorder().isWithinBounds(pos) && stackCurrent(receipt)
                && existing(level, pos) == receipt.node;
    }
    private static BlockEntity existing(ServerLevel level, BlockPos pos) {
        var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null ? null : chunk.getBlockEntity(pos);
    }
    private static boolean effectAreaLoaded(ServerLevel level, BlockPos pos) {
        // Max strength<8: unresisted rays travel<14 blocks; allow native neighbor checks too.
        for (int x = (pos.getX() - 18) >> 4; x <= (pos.getX() + 18) >> 4; x++)
            for (int z = (pos.getZ() - 18) >> 4; z <= (pos.getZ() + 18) >> 4; z++)
                if (level.getChunkSource().getChunkNow(x, z) == null) return false;
        return true;
    }
    private static void placeTrials(ServerLevel level, BlockPos origin) {
        for (int trial = 0; trial < 33; trial++) {
            int x = level.random.nextInt(6) - level.random.nextInt(6);
            int y = level.random.nextInt(6) - level.random.nextInt(6);
            int z = level.random.nextInt(6) - level.random.nextInt(6);
            BlockPos target = origin.offset(x, y, z);
            if (!FluxFiniteBlock.loaded(level, target) || !level.getWorldBorder().isWithinBounds(target)) continue;
            BlockState before = level.getBlockState(target);
            if (!before.isAir() || existing(level, target) != null) continue;
            FluxFiniteBlock phase = target.getY() < origin.getY() ? ThaumcraftFlux.FLUX_GOO.get() : ThaumcraftFlux.FLUX_GAS.get();
            BlockState born = phase.withGeometry(phase.defaultBlockState().setValue(FluxFiniteBlock.LEVEL, 8), level, target);
            if (FluxFiniteBlock.loaded(level, target) && level.getBlockState(target) == before && existing(level, target) == null)
                level.setBlock(target, born, Block.UPDATE_ALL);
        }
    }
}
