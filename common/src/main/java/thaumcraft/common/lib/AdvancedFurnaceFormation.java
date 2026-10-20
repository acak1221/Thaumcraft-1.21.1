package thaumcraft.common.lib;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.blocks.BlockAdvancedAlchemyFurnace;
import thaumcraft.common.config.ThaumcraftAdvancedFurnace;
import thaumcraft.common.config.ThaumcraftAlchemy;
import thaumcraft.common.config.ThaumcraftAlembics;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftDistillation;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.tiles.TileAlchemyFurnaceAdvanced;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileAlchemyFurnaceAdvancedPart;

/** Original first-normal-furnace 3x2x3 search; only17 constrained cells transform, upper center remains owned by its existing block. */
public final class AdvancedFurnaceFormation {
    public enum Status { COMPLETE, INCOMPLETE, UNLOADED }
    private static final ThreadLocal<Boolean> MUTATING=new ThreadLocal<>();
    private static final ThreadLocal<Birth> BIRTH=new ThreadLocal<>();
    public record Identity(BlockPos center,UUID machine,boolean standaloneCreative) {}
    private record Birth(BlockPos pos,int part,Identity identity) {}
    /** Native block-entity factories can inherit only the CURRENT exact paid position's identity. */
    public static Identity birthIdentity(TileAlchemyFurnaceAdvancedPart part) {
        Birth birth=BIRTH.get();
        return birth!=null && isMutationActive() && part.getLevel()==null && part.getBlockPos().equals(birth.pos)
                && part.getBlockState().is(ThaumcraftAdvancedFurnace.FURNACE.get())
                && part.getBlockState().getValue(BlockAdvancedAlchemyFurnace.PART)==birth.part ? birth.identity:null;
    }
    private record Piece(BlockPos pos,BlockState before,BlockEntity tile,int part,BlockState formed) {}
    private AdvancedFurnaceFormation() {}
    public static boolean isMutationActive() { return MUTATING.get()!=null; }
    /** Native UPDATE_ALL would query neighbor terrain; unavailable retirement uses a quiet known-shape write. */
    public static int reversionFlags(ServerLevel world,BlockPos pos) {
        for(var direction:net.minecraft.core.Direction.values()) {
            BlockPos neighbor=pos.relative(direction);
            if(!world.isOutsideBuildHeight(neighbor) && world.getChunkSource().getChunkNow(neighbor.getX()>>4,neighbor.getZ()>>4)==null)
                return Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        }
        return Block.UPDATE_ALL;
    }

    private static boolean loaded(ServerLevel world,BlockPos pos) { return !world.isOutsideBuildHeight(pos) && world.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null; }
    private static BlockEntity existing(ServerLevel world,BlockPos pos) { var chunk=world.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4); return chunk==null?null:chunk.getBlockEntity(pos); }
    public static Status structure(TileAlchemyFurnaceAdvanced core) {
        if(!(core.getLevel() instanceof ServerLevel world)) return Status.UNLOADED;
        if(core.isStandaloneCreative()) return core.isCurrent()?Status.COMPLETE:Status.INCOMPLETE;
        BlockPos origin=core.getBlockPos();
        for(int x=-1;x<=1;x++) for(int y=0;y<=1;y++) for(int z=-1;z<=1;z++) {
            int part=AdvancedFurnaceRules.part(x,y,z); if(part<0) continue;
            if(!loaded(world,origin.offset(x,y,z))) return Status.UNLOADED;
        }
        for(int x=-1;x<=1;x++) for(int y=0;y<=1;y++) for(int z=-1;z<=1;z++) {
            int part=AdvancedFurnaceRules.part(x,y,z); if(part<0) continue;
            BlockPos pos=origin.offset(x,y,z); BlockState state=world.getBlockState(pos);
            if(!state.is(ThaumcraftAdvancedFurnace.FURNACE.get()) || state.getValue(BlockAdvancedAlchemyFurnace.PART)!=part) return Status.INCOMPLETE;
            var installed=existing(world,pos);
            if(!(installed instanceof TileAlchemyFurnaceAdvancedPart child) || !child.belongsTo(core)
                    || part==0 && installed!=core || part==1 && !(installed instanceof thaumcraft.common.tiles.TileAlchemyFurnaceAdvancedNozzle)) return Status.INCOMPLETE;
        }
        return Status.COMPLETE;
    }
    public static InteractionResult tryForm(UseOnContext context) {
        if(isMutationActive() || !(context.getItemInHand().getItem() instanceof ItemWandCasting)) return InteractionResult.PASS;
        Level raw=context.getLevel(); BlockPos clicked=context.getClickedPos();
        if(!(raw instanceof ServerLevel world)) return InteractionResult.PASS;
        if(!world.getServer().isSameThread() || world.getServer().getLevel(world.dimension())!=world) return InteractionResult.FAIL;
        BlockState trigger=world.getBlockState(clicked);
        // Original trigger7 exists only on advanced/ordinary alchemical constructs.
        // An unrelated wand target must continue through the other native item handlers.
        if(!trigger.is(ThaumcraftAdvancedFurnace.ADVANCED_CONSTRUCT.get()) && !trigger.is(ThaumcraftDistillation.ALCHEMICAL_CONSTRUCT.get())) return InteractionResult.PASS;
        if(!(context.getPlayer() instanceof ServerPlayer player) || player.isSpectator()) return InteractionResult.FAIL;
        if(!KnowledgeAccess.get(player).isResearchComplete("ADVALCHEMYFURNACE")) return InteractionResult.PASS;
        MUTATING.set(true);
        try {
            for(int x=-1;x<=1;x++) for(int y=-1;y<=1;y++) for(int z=-1;z<=1;z++) {
                BlockPos center=clicked.offset(x,y,z);
                if(!loaded(world,center) || !world.getBlockState(center).is(ThaumcraftDistillation.ALCHEMY_FURNACE.get())) continue;
                // Original immediately returns on the FIRST normal furnace candidate, even if another one could form.
                return form(context,world,player,center);
            }
            return InteractionResult.PASS;
        } finally { MUTATING.remove(); }
    }
    private static InteractionResult form(UseOnContext context,ServerLevel world,ServerPlayer player,BlockPos center) {
        ItemStack wand=context.getItemInHand(); ItemStack beforeWand=wand.copy();
        List<Piece> pieces=new ArrayList<>();
        for(int x=-1;x<=1;x++) for(int y=0;y<=1;y++) for(int z=-1;z<=1;z++) {
            int part=AdvancedFurnaceRules.part(x,y,z); if(part<0) continue;
            BlockPos pos=center.offset(x,y,z);
            if(!loaded(world,pos) || !world.getWorldBorder().isWithinBounds(pos) || !world.mayInteract(player,pos)
                    || !player.mayUseItemAt(pos,context.getClickedFace(),wand)) return InteractionResult.FAIL;
            BlockState before=world.getBlockState(pos); BlockEntity tile=existing(world,pos);
            Block wanted=part==0?ThaumcraftDistillation.ALCHEMY_FURNACE.get():part==2?ThaumcraftAlembics.ALEMBIC.get():part==3?ThaumcraftDistillation.ALCHEMICAL_CONSTRUCT.get():ThaumcraftAdvancedFurnace.ADVANCED_CONSTRUCT.get();
            if(!before.is(wanted)) return InteractionResult.FAIL;
            pieces.add(new Piece(pos,before,tile,part,ThaumcraftAdvancedFurnace.FURNACE.get().defaultBlockState().setValue(BlockAdvancedAlchemyFurnace.PART,part)));
        }
        var price=ArcaneVis.price(AdvancedFurnaceRules.FORM_VIS,wand,player); ItemStack paid=wand.copy();
        if(!ArcaneVis.debit(paid,price)) return InteractionResult.FAIL;
        if(player.level()!=world || player.isRemoved() || player.isSpectator() || world.getServer().getPlayerList().getPlayer(player.getUUID())!=player || player.getItemInHand(context.getHand())!=wand || !ItemStack.matches(wand,beforeWand)
                || !player.canInteractWithBlock(context.getClickedPos(),0) || !KnowledgeAccess.get(player).isResearchComplete("ADVALCHEMYFURNACE")) return InteractionResult.FAIL;
        for(Piece piece:pieces) if(!loaded(world,piece.pos) || world.getBlockState(piece.pos)!=piece.before || existing(world,piece.pos)!=piece.tile
                || !world.mayInteract(player,piece.pos) || !player.mayUseItemAt(piece.pos,context.getClickedFace(),wand)) return InteractionResult.FAIL;
        if(player.getItemInHand(context.getHand())!=wand || !ItemStack.matches(wand,beforeWand)) return InteractionResult.FAIL;
        Identity identity=new Identity(center.immutable(),UUID.randomUUID(),false);
        // Spend the actual held wand before any callback-bearing world installation. No refund/replay after this point.
        wand.set(ThaumcraftContent.WAND_DATA.get(),ItemWandCasting.data(paid));
        boolean complete=true;
        try {
        for(Piece piece:pieces) {
            if(!loaded(world,piece.pos) || world.getBlockState(piece.pos)!=piece.before || existing(world,piece.pos)!=piece.tile) { complete=false; break; }
            BIRTH.set(new Birth(piece.pos,piece.part,identity));
            try { if(!world.setBlock(piece.pos,piece.formed,Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE)) { complete=false; break; } }
            finally { BIRTH.remove(); }
        }
        // Partial paid installations retain their durable owner and revert through ordinary orphan/core checks.
        if(!complete) return InteractionResult.CONSUME;
        for(Piece piece:pieces) if(loaded(world,piece.pos) && world.getBlockState(piece.pos)==piece.formed && reversionFlags(world,piece.pos)==Block.UPDATE_ALL) world.updateNeighborsAt(piece.pos,piece.formed.getBlock());
        world.playSound(null,center,net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft","wand")),net.minecraft.sounds.SoundSource.BLOCKS,1,1);
        return InteractionResult.CONSUME;
        } catch(RuntimeException failure) { thaumcraft.common.Thaumcraft.LOG.warn("Advanced furnace installation interrupted after payment at {}",center,failure); return InteractionResult.CONSUME; }
    }
    public static BlockState original(int part) {
        return (part==0?ThaumcraftDistillation.ALCHEMY_FURNACE.get():part==2?ThaumcraftAlembics.ALEMBIC.get():part==3?ThaumcraftDistillation.ALCHEMICAL_CONSTRUCT.get():ThaumcraftAdvancedFurnace.ADVANCED_CONSTRUCT.get()).defaultBlockState();
    }
    public static void broken(Level level,BlockPos pos,int part) {
        if(!(level instanceof ServerLevel world) || isMutationActive()) return;
        var removed=existing(world,pos);
        if(!(removed instanceof TileAlchemyFurnaceAdvancedPart owned) || !owned.hasFormationIdentity()) return;
        if(part==0 && removed instanceof TileAlchemyFurnaceAdvanced core) { disassemble(core,true); return; }
        BlockPos target=owned.ownerPosition();
        if(loaded(world,target) && existing(world,target) instanceof TileAlchemyFurnaceAdvanced core && owned.belongsTo(core)) core.requestDestroy();
    }
    public static void disassemble(TileAlchemyFurnaceAdvanced core,boolean coreBeingBroken) {
        if(!(core.getLevel() instanceof ServerLevel world) || isMutationActive() || core.isStandaloneCreative()) return;
        BlockPos center=core.getBlockPos();
        // Known missing pieces revert; unknown terrain waits so no detached/orphan half is silently abandoned.
        boolean unknownTerrain=false;
        for(int x=-1;x<=1;x++) for(int y=0;y<=1;y++) for(int z=-1;z<=1;z++)
            if(AdvancedFurnaceRules.part(x,y,z)>=0 && !loaded(world,center.offset(x,y,z))) unknownTerrain=true;
        if(!coreBeingBroken && unknownTerrain) return;
        MUTATING.set(true);
        try {
            for(int x=-1;x<=1;x++) for(int y=0;y<=1;y++) for(int z=-1;z<=1;z++) {
                int expected=AdvancedFurnaceRules.part(x,y,z); if(expected<0 || expected==0) continue;
                BlockPos pos=center.offset(x,y,z); if(!loaded(world,pos)) continue; BlockState state=world.getBlockState(pos);
                if(state.is(ThaumcraftAdvancedFurnace.FURNACE.get()) && state.getValue(BlockAdvancedAlchemyFurnace.PART)==expected
                        && existing(world,pos) instanceof TileAlchemyFurnaceAdvancedPart owned && owned.belongsTo(core))
                    // Even an inner part's vanilla flags3 shape cascade can reach a second neighbor in an
                    // unavailable chunk. A partial native retirement therefore keeps EVERY own write quiet.
                    world.setBlock(pos,original(expected),unknownTerrain?Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE:reversionFlags(world,pos));
            }
            if(!coreBeingBroken && existing(world,center)==core && world.getBlockState(center).is(ThaumcraftAdvancedFurnace.FURNACE.get()))
                world.setBlock(center,original(0),reversionFlags(world,center));
        } finally { MUTATING.remove(); }
    }
}
