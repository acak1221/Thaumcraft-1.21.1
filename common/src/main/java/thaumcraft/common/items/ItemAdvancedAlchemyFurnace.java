package thaumcraft.common.items;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.blocks.BlockAdvancedAlchemyFurnace;
import thaumcraft.common.lib.AdvancedFurnaceFormation;
import thaumcraft.common.tiles.TileAlchemyFurnaceAdvanced;
import thaumcraft.common.tiles.TileAlchemyFurnaceAdvancedPart;

/** Original creative metadata0 block-item places ONE working core; ordinary native placement/count semantics. */
public final class ItemAdvancedAlchemyFurnace extends BlockItem {
    private static final ThreadLocal<Placement> ACTIVE=new ThreadLocal<>();
    private static final class Placement {
        final ItemAdvancedAlchemyFurnace item; final BlockPos pos; final AdvancedFurnaceFormation.Identity identity;
        boolean factory;
        Placement(ItemAdvancedAlchemyFurnace item,BlockPos pos) { this.item=item; this.pos=pos.immutable(); identity=new AdvancedFurnaceFormation.Identity(this.pos,UUID.randomUUID(),true); }
    }
    public ItemAdvancedAlchemyFurnace(Block block,Properties properties) { super(block,properties); }
    @Override public InteractionResult place(BlockPlaceContext context) {
        if(ACTIVE.get()!=null) return InteractionResult.FAIL;
        var player=context.getPlayer(); ItemStack stack=context.getItemInHand();
        if(player==null || player.isSpectator() || stack.isEmpty() || stack.getItem()!=this || player.getItemInHand(context.getHand())!=stack) return InteractionResult.FAIL;
        if(context.getLevel().isClientSide) return super.place(context);
        if(!(context.getLevel() instanceof ServerLevel level) || !(player instanceof ServerPlayer actor) || !level.getServer().isSameThread()
                || level.getServer().getLevel(level.dimension())!=level || level.getServer().getPlayerList().getPlayer(actor.getUUID())!=actor) return InteractionResult.FAIL;
        ACTIVE.set(new Placement(this,context.getClickedPos()));
        try { return super.place(context); } finally { ACTIVE.remove(); }
    }
    @Override protected boolean placeBlock(BlockPlaceContext context,BlockState state) {
        Placement placement=ACTIVE.get();
        if(context.getLevel().isClientSide) return super.placeBlock(context,state);
        if(placement==null || placement.item!=this || !placement.pos.equals(context.getClickedPos()) || state.getValue(BlockAdvancedAlchemyFurnace.PART)!=0) return false;
        placement.factory=true;
        try { return super.placeBlock(context,state); } finally { placement.factory=false; }
    }
    /** Block-item has no saved slurry/energy/owner payload. Native implicit components do not supply any of those private fields. */
    @Override protected boolean updateCustomBlockEntityTag(BlockPos pos,Level level,Player player,ItemStack stack,BlockState state) { return false; }
    public static AdvancedFurnaceFormation.Identity birthIdentity(TileAlchemyFurnaceAdvancedPart part) {
        Placement placement=ACTIVE.get();
        return placement!=null && placement.factory && part instanceof TileAlchemyFurnaceAdvanced && part.getLevel()==null
                && placement.pos.equals(part.getBlockPos()) && part.getBlockState().getValue(BlockAdvancedAlchemyFurnace.PART)==0?placement.identity:null;
    }
}
