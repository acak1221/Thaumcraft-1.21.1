package thaumcraft.common.config;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockColoredTallowCandle;

/** Original cloth metadata order,0white through15black, with original Utils.colors. */
public final class ThaumcraftColoredCandles {
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Thaumcraft.MODID,Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Thaumcraft.MODID,Registries.ITEM);
    private static final int[] RGB={0xf0f0f0,15435844,12801229,6719955,14602026,4312372,14188952,0x434343,0xa0a0a0,2651799,8073150,2437522,5320730,3887386,11743532,0x1e1b1b};
    public static final List<RegistrySupplier<BlockColoredTallowCandle>> COLORED_BLOCKS;
    public static final List<RegistrySupplier<Item>> COLORED_ITEMS;
    static {
        var blocks=new ArrayList<RegistrySupplier<BlockColoredTallowCandle>>();var items=new ArrayList<RegistrySupplier<Item>>();
        for(int meta=1;meta<16;meta++) {
            String id=DyeColor.byId(meta).getName()+"_tallow_candle";
            var block=BLOCKS.register(id,()->new BlockColoredTallowCandle(BlockBehaviour.Properties.of()));blocks.add(block);
            items.add(ITEMS.register(id,()->new BlockItem(block.get(),new Item.Properties())));
        }
        COLORED_BLOCKS=List.copyOf(blocks);COLORED_ITEMS=List.copyOf(items);
    }
    private static boolean registered;
    private ThaumcraftColoredCandles() {}
    private static void valid(int meta) { if(meta<0||meta>15)throw new IllegalArgumentException("Unknown original candle metadata"); }
    public static Item byMeta(int meta) { valid(meta);return meta==0?ThaumcraftInfusion.TALLOW_CANDLE_ITEM.get():COLORED_ITEMS.get(meta-1).get(); }
    public static Block blockByMeta(int meta) { valid(meta);return meta==0?ThaumcraftInfusion.TALLOW_CANDLE.get():COLORED_BLOCKS.get(meta-1).get(); }
    public static int color(int meta) { valid(meta);return RGB[meta]; }
    public static int meta(Block block) {
        if(block==ThaumcraftInfusion.TALLOW_CANDLE.get())return 0;
        for(int i=0;i<COLORED_BLOCKS.size();i++)if(block==COLORED_BLOCKS.get(i).get())return i+1;
        return -1;
    }
    public static void register() {
        if(registered)return;registered=true;BLOCKS.register();ITEMS.register();
        for(var item:COLORED_ITEMS)CreativeTabRegistry.append(ThaumcraftContent.TAB,item);
    }
}
