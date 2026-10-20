package thaumcraft.common.testing;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.crafting.IInfusionStabiliser;
import thaumcraft.common.config.ThaumcraftColoredCandles;
import thaumcraft.common.config.ThaumcraftInfusion;

/** All sixteen physical identities, actual vanilla crafting result-slot takes,
 * native BlockItem placement and support-loss drops. Declared fixture inputs do
 * not prove natural acquisition or a visually inspected client. */
public final class ColoredCandlesRuntimeChecks {
    private ColoredCandlesRuntimeChecks() {}
    public static void run(GameTestHelper test) {
        try(var a=new FocusUpgradeRuntimeChecks.Actor(test,new BlockPos(4,260,4))) {
            a.clearLane();crafting(a);placement(a);
        }
        test.succeed();
    }
    private static void click(FocusUpgradeRuntimeChecks.Actor a,int slot) {
        var menu=a.player.containerMenu;
        a.player.connection.handleContainerClick(new ServerboundContainerClickPacket(menu.containerId,menu.getStateId(),slot,0,ClickType.PICKUP,ItemStack.EMPTY,new Int2ObjectOpenHashMap<>()));
    }
    private static void crafting(FocusUpgradeRuntimeChecks.Actor a) {
        a.put(a.root,Blocks.CRAFTING_TABLE.defaultBlockState());a.player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        a.require(a.player.gameMode.useItemOn(a.player,a.level,ItemStack.EMPTY,InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(a.root),Direction.UP,a.root,false)).consumesAction()
                        &&a.player.containerMenu instanceof CraftingMenu,"actual crafting-table interaction opens the native3x3 menu");
        var menu=(CraftingMenu)a.player.containerMenu;
        for(int meta=1;meta<16;meta++)craft(a,menu,ThaumcraftInfusion.TALLOW_CANDLE_ITEM.get(),DyeItem.byColor(DyeColor.byId(meta)),ThaumcraftColoredCandles.byMeta(meta));
        craft(a,menu,ThaumcraftInfusion.TALLOW_CANDLE_ITEM.get(),Items.LAPIS_LAZULI,ThaumcraftColoredCandles.byMeta(11));
        craft(a,menu,ThaumcraftInfusion.TALLOW_CANDLE_ITEM.get(),Items.INK_SAC,ThaumcraftColoredCandles.byMeta(15));
        craft(a,menu,ThaumcraftInfusion.TALLOW_CANDLE_ITEM.get(),Items.COCOA_BEANS,ThaumcraftColoredCandles.byMeta(12));
        for(int meta=0;meta<16;meta++)craft(a,menu,ThaumcraftColoredCandles.byMeta(meta),Items.BONE_MEAL,ThaumcraftInfusion.TALLOW_CANDLE_ITEM.get());
        for(Item first:List.of(ThaumcraftColoredCandles.byMeta(1),ThaumcraftInfusion.TALLOW_CANDLE_ITEM.get())) {
            Item second=first==ThaumcraftInfusion.TALLOW_CANDLE_ITEM.get()?Items.WHITE_DYE:Items.RED_DYE;
            menu.getSlot(1).set(new ItemStack(first));menu.getSlot(2).set(new ItemStack(second));menu.slotsChanged(menu.getSlot(1).container);
            a.require(menu.getSlot(0).getItem().isEmpty(),"only white candle accepts dye; original bleach requires bone meal rather than modern white dye");
            click(a,0);a.require(menu.getCarried().isEmpty()&&menu.getSlot(1).getItem().is(first)&&menu.getSlot(2).getItem().is(second),"refused actual result take consumes no physical input");
            menu.getSlot(1).set(ItemStack.EMPTY);menu.getSlot(2).set(ItemStack.EMPTY);
        }
        a.player.closeContainer();a.put(a.root,Blocks.AIR.defaultBlockState());a.player.getInventory().clearContent();
    }
    private static void craft(FocusUpgradeRuntimeChecks.Actor a,CraftingMenu menu,Item candle,Item dye,Item output) {
        menu.getSlot(1).set(new ItemStack(candle));menu.getSlot(2).set(new ItemStack(dye));menu.slotsChanged(menu.getSlot(1).container);
        a.require(menu.getSlot(0).getItem().is(output)&&menu.getSlot(0).getItem().getCount()==1,"registered native dye/bleach recipe resolves exactly one "+output);
        click(a,0);a.require(menu.getCarried().is(output)&&menu.getCarried().getCount()==1&&menu.getSlot(1).getItem().isEmpty()&&menu.getSlot(2).getItem().isEmpty(),
                "actual native result-slot take consumes one candle and one pigment exactly once");
        menu.setCarried(ItemStack.EMPTY); // Discard only this check's witnessed output.
    }
    private static void placement(FocusUpgradeRuntimeChecks.Actor a) {
        int[] original={0xf0f0f0,15435844,12801229,6719955,14602026,4312372,14188952,0x434343,0xa0a0a0,2651799,8073150,2437522,5320730,3887386,11743532,0x1e1b1b};
        BlockPos floor=a.root.below();
        for(int meta=0;meta<16;meta++) {
            a.purgeOutputs();a.put(floor,Blocks.STONE.defaultBlockState());Item item=ThaumcraftColoredCandles.byMeta(meta);a.player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));
            a.require(a.player.gameMode.useItemOn(a.player,a.level,a.player.getMainHandItem(),InteractionHand.MAIN_HAND,
                            new BlockHitResult(Vec3.atCenterOf(floor).add(0,.5,0),Direction.UP,floor,false)).consumesAction()
                            &&a.level.getBlockState(a.root).is(ThaumcraftColoredCandles.blockByMeta(meta))&&a.player.getMainHandItem().isEmpty(),
                    "actual BlockItem placement consumes one original color identity "+meta);
            var state=a.level.getBlockState(a.root);
            a.require(ThaumcraftColoredCandles.meta(state.getBlock())==meta&&ThaumcraftColoredCandles.color(meta)==original[meta]&&state.getLightEmission()==14
                            &&state.getCollisionShape(a.level,a.root).isEmpty()&&state.getBlockSupportShape(a.level,a.root).isEmpty()
                            &&state.getBlock() instanceof IInfusionStabiliser stabiliser&&stabiliser.canStabaliseInfusion(a.level,a.root),
                    "physical candle keeps original metadata/RGB,14light, no collision/support, and infusion stabilisation");
            List<ItemStack> drops=Block.getDrops(state,a.level,a.root,null,a.player,ItemStack.EMPTY);
            a.require(drops.size()==1&&drops.getFirst().is(item)&&drops.getFirst().getCount()==1,"actual native candle loot preserves its own flattened color once");
            a.level.destroyBlock(floor,false,a.player);
            a.require(a.level.isEmptyBlock(a.root)&&a.items().size()==1&&a.items().getFirst().getItem().is(item)&&a.items().getFirst().getItem().getCount()==1,
                    "native support destruction pops exactly the matching candle without losing or duplicating color");
            a.purgeOutputs();
        }
    }
}
