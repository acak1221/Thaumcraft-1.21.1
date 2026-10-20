package thaumcraft.common.items;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.common.config.ThaumcraftSpa;
import thaumcraft.common.lib.fluid.PureBucketActions;

/** TC4 ItemBucketPure. Native BucketItem supplies only the bidirectional fluid
 * container identity; its modern placement/creative/Nether use is overridden. */
public final class ItemBucketPure extends BucketItem {
    public ItemBucketPure(Properties properties){super(ThaumcraftSpa.PURE_FLUID_SOURCE.get(),properties);}
    /** Original Item.rayTrace(..., true): source-only fullHit, even when full. */
    public static BlockHitResult fullHit(Level level,Player player){return getPlayerPOVHitResult(level,player,ClipContext.Fluid.SOURCE_ONLY);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){return PureBucketActions.place(level,player,hand);}
    @Override public boolean emptyContents(Player player,Level level,BlockPos pos,BlockHitResult hit){return tryPlaceContainedLiquid(level,pos.getX(),pos.getY(),pos.getZ());}
    /** Original public placement entry, with no water-tag ultraWarm evaporation. */
    public boolean tryPlaceContainedLiquid(Level level,int x,int y,int z){return PureBucketActions.placeContents(level,new BlockPos(x,y,z));}
}
