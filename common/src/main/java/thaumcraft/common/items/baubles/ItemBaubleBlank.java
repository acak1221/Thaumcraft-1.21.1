package thaumcraft.common.items.baubles;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.IRunicArmor;
import thaumcraft.api.baubles.BaubleType;
import thaumcraft.api.baubles.IBauble;

/** Original mundane amulet/ring/belt are wearable runic-augmentation targets, base charge zero. */
public final class ItemBaubleBlank extends Item implements IBauble, IRunicArmor, thaumcraft.api.IVisDiscountGear {
    private final BaubleType type;
    private final thaumcraft.api.aspects.Aspect discount;
    public ItemBaubleBlank(Properties properties, BaubleType type) { this(properties,type,null); }
    public ItemBaubleBlank(Properties properties, BaubleType type, thaumcraft.api.aspects.Aspect discount) { super(properties); this.type = type; this.discount=discount; }
    public thaumcraft.api.aspects.Aspect discountAspect() { return discount; }
    @Override public int getVisDiscount(ItemStack stack,net.minecraft.world.entity.player.Player player,thaumcraft.api.aspects.Aspect aspect) { return discount!=null&&discount==aspect?1:0; }
    @Override public net.minecraft.network.chat.Component getName(ItemStack stack) {
        if(discount==null)return super.getName(stack);
        return net.minecraft.network.chat.Component.literal(net.minecraft.network.chat.Component.translatable("item.ItemBaubleBlanks.3.name").getString().replace("%TYPE",discount.getName()));
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,java.util.List<net.minecraft.network.chat.Component> lines,net.minecraft.world.item.TooltipFlag flag) {
        if(discount!=null)lines.add(net.minecraft.network.chat.Component.literal(discount.getName()+" ").append(net.minecraft.network.chat.Component.translatable("tc.discount")).append(": 1%").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
    }
    @Override public BaubleType getBaubleType(ItemStack stack) { return type; }
    @Override public int getRunicCharge(ItemStack stack) { return 0; }
}
