package thaumcraft.common.lib.fluid;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.blocks.BlockFluidPure;
import thaumcraft.common.config.ThaumcraftSpa;
import thaumcraft.common.items.ItemBucketPure;
import thaumcraft.common.lib.utils.DeferredItemDrops;

/** Removed FillBucketEvent + ItemBucketPure, with canonical source/hand claims
 * before world callbacks. No native water placement sound, stat or evaporation. */
public final class PureBucketActions {
    private static final Set<UUID> USING=new HashSet<>();
    private static final Map<ServerLevel,Set<BlockPos>> CELLS=new IdentityHashMap<>();
    private PureBucketActions(){}
    private static boolean canonical(ServerLevel level){return level.getServer().isSameThread()&&level.getServer().getLevel(level.dimension())==level;}
    private static boolean actor(ServerLevel level,Player player){return canonical(level)&&player instanceof ServerPlayer server&&server.server.getPlayerList().getPlayer(player.getUUID())==player&&player.level()==level&&!player.isRemoved()&&player.isAlive()&&!player.isSpectator();}
    private static boolean loaded(ServerLevel level,BlockPos pos){return !level.isOutsideBuildHeight(pos)&&level.getWorldBorder().isWithinBounds(pos)&&level.hasChunkAt(pos);}
    private static boolean source(BlockState state){return state.getBlock() instanceof BlockFluidPure&&state.getValue(LiquidBlock.LEVEL)==0;}
    private static boolean claim(ServerLevel level,BlockPos pos){return CELLS.computeIfAbsent(level,ignored->new HashSet<>()).add(pos.immutable());}
    private static void release(ServerLevel level,BlockPos pos){var cells=CELLS.get(level);if(cells!=null){cells.remove(pos);if(cells.isEmpty())CELLS.remove(level);}}

    /** Native BucketPickup entry. Flowing1..7 always refuses without mutation.
     * Rejected source removal cannot mint a second physical filled container. */
    public static ItemStack capture(@Nullable Player player,LevelAccessor world,BlockPos pos,BlockState expected){
        if(!(world instanceof ServerLevel level)||!canonical(level)||!loaded(level,pos)||!source(expected)||level.getBlockState(pos)!=expected
                ||player!=null&&!actor(level,player)||!claim(level,pos))return ItemStack.EMPTY;
        try{
            if(level.getBlockState(pos)!=expected)return ItemStack.EMPTY;
            boolean changed=level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
            return changed&&level.getBlockState(pos).isAir()?new ItemStack(ThaumcraftSpa.BUCKET_PURE.get()):ItemStack.EMPTY;
        }finally{release(level,pos);}
    }

    /** Null delegates every other native bucket/fluid to its ordinary use. */
    public static @Nullable InteractionResultHolder<ItemStack> fill(Level world,Player player,InteractionHand hand){
        var held=player.getItemInHand(hand);if(!held.is(Items.BUCKET))return null;
        var hit=ItemBucketPure.fullHit(world,player);if(hit.getType()!=HitResult.Type.BLOCK)return null;
        var pos=hit.getBlockPos();if(!world.hasChunkAt(pos)||!source(world.getBlockState(pos)))return null;
        if(player.isSpectator()||!world.mayInteract(player,pos)||!player.mayUseItemAt(pos,hit.getDirection(),held))return InteractionResultHolder.fail(held);
        if(world.isClientSide)return InteractionResultHolder.success(held);
        if(!(world instanceof ServerLevel level)||!actor(level,player)||!USING.add(player.getUUID()))return InteractionResultHolder.fail(held);
        var expected=held.copy();
        try{
            if(!loaded(level,pos)||player.getItemInHand(hand)!=held||!ItemStack.matches(held,expected))return InteractionResultHolder.fail(held);
            // Debit the actual empty-container unit before source callbacks.
            // A callback replacing the hand cannot redirect that debit to a new item.
            boolean creative=player.getAbilities().instabuild;
            var paid=creative?ItemStack.EMPTY:held.copyWithCount(1);
            var remainder=held;
            if(!creative){remainder=held.copy();remainder.shrink(1);held.shrink(1);player.setItemInHand(hand,remainder);}
            var filled=capture(player,level,pos,level.getBlockState(pos));
            if(filled.isEmpty()){
                if(!paid.isEmpty())credit(level,player,hand,remainder,paid);
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
            // Forge ALLOW creative consumes the source but returns its original
            // EMPTY bucket; modern ItemUtils would invent a filled inventory item.
            if(!creative)credit(level,player,hand,remainder,filled);
            return InteractionResultHolder.consume(player.getItemInHand(hand));
        }finally{USING.remove(player.getUUID());}
    }

    private static void credit(ServerLevel level,Player player,InteractionHand hand,ItemStack remainder,ItemStack output){
        if(remainder.isEmpty()&&player.getItemInHand(hand)==remainder){player.setItemInHand(hand,output);return;}
        if(!player.getInventory().add(output))DeferredItemDrops.enqueueBucketReturn(level,player,output);
        player.getInventory().setChanged();
    }

    public static InteractionResultHolder<ItemStack> place(Level world,Player player,InteractionHand hand){
        var held=player.getItemInHand(hand);if(!(held.getItem() instanceof ItemBucketPure))return InteractionResultHolder.pass(held);
        var hit=ItemBucketPure.fullHit(world,player);if(hit.getType()!=HitResult.Type.BLOCK)return InteractionResultHolder.pass(held);
        var target=hit.getBlockPos().relative(hit.getDirection());
        if(player.isSpectator()||!world.hasChunkAt(target)||!world.getWorldBorder().isWithinBounds(target)||world.isOutsideBuildHeight(target)
                ||!world.mayInteract(player,target)||!player.mayUseItemAt(target,hit.getDirection(),held)
                ||!replaceable(world.getBlockState(target)))return InteractionResultHolder.fail(held);
        if(world.isClientSide)return InteractionResultHolder.success(held);
        if(!(world instanceof ServerLevel level)||!actor(level,player)||!USING.add(player.getUUID()))return InteractionResultHolder.fail(held);
        var expected=held.copy();
        try{
            if(player.getItemInHand(hand)!=held||!ItemStack.matches(held,expected)||!loaded(level,target)||!replaceable(level.getBlockState(target))||!claim(level,target))return InteractionResultHolder.fail(held);
            try{
                // Original returns EMPTY once the material gate admits the
                // attempt, even when the world refuses its final setBlock.
                // Claim payment BEFORE destroy/drop/world callbacks can reenter.
                var result=player.getAbilities().instabuild?held:new ItemStack(Items.BUCKET);
                if(result!=held){held.shrink(1);player.setItemInHand(hand,result);}
                placeClaimed(level,target);
                return InteractionResultHolder.consume(player.getItemInHand(hand));
            }finally{release(level,target);}
        }finally{USING.remove(player.getUUID());}
    }
    private static boolean replaceable(BlockState state){return state.isAir()||!BlockFluidPure.legacySolid(state);}
    public static boolean placeContents(Level world,BlockPos pos){
        if(!(world instanceof ServerLevel level)||!canonical(level)||!loaded(level,pos)||!replaceable(level.getBlockState(pos))||!claim(level,pos))return false;
        try{placeClaimed(level,pos);return true;}finally{release(level,pos);}
    }
    private static void placeClaimed(ServerLevel level,BlockPos pos){
        var state=level.getBlockState(pos);
        if(!state.liquid())level.destroyBlock(pos,true);
        // A native drop/onRemove callback may deliberately replace this cell.
        // Its new owner is not the original paid target and must not be erased.
        var after=level.getBlockState(pos);
        if(canonical(level)&&loaded(level,pos)&&(after==state||after.isAir()))level.setBlock(pos,ThaumcraftSpa.PURE_WATER.get().defaultBlockState(),Block.UPDATE_ALL);
    }
}
