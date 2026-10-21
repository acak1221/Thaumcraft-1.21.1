package thaumcraft.common.items.baubles;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.api.IRunicArmor;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.baubles.BaubleType;
import thaumcraft.api.baubles.IBauble;
import thaumcraft.common.config.ThaumcraftBaubles;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;

/** Original ItemAmuletVis metadata0/1 flattened; all amounts remain raw centivis. */
public final class ItemAmuletVis extends Item implements IBauble, IRunicArmor {
    private final boolean lesser;
    public ItemAmuletVis(Properties properties, boolean lesser) { super(properties); this.lesser = lesser; }
    public int getMaxVis(ItemStack stack) { return lesser ? 2500 : 25000; }
    private DataComponentType<VisAmuletData> component() { return lesser ? ThaumcraftBaubles.LESSER_VIS.get() : ThaumcraftBaubles.VIS.get(); }
    public VisAmuletData data(ItemStack stack) { return stack.getOrDefault(component(), VisAmuletData.EMPTY); }
    public int getVis(ItemStack stack, Aspect aspect) { return data(stack).amount(aspect.getTag()); }
    public void storeVis(ItemStack stack, Aspect aspect, int amount) { stack.set(component(), data(stack).withAmount(aspect.getTag(), amount, getMaxVis(stack))); }
    public int addRealVis(ItemStack stack, Aspect aspect, int amount, boolean commit) {
        if (!aspect.isPrimal() || amount < 0) return 0;
        int room = getMaxVis(stack) - getVis(stack, aspect), accepted = Math.min(room, amount);
        if (commit) storeVis(stack, aspect, getVis(stack, aspect) + accepted);
        return amount - accepted;
    }
    /** Original public whole-vis API; unlike raw network payment, this multiplies by100. */
    public int addVis(ItemStack stack,Aspect aspect,int amount,boolean commit) {
        if(!aspect.isPrimal()||amount<0)return 0;
        int raw=Math.multiplyExact(amount,100);return addRealVis(stack,aspect,raw,commit)/100;
    }
    public boolean consumeAllVis(ItemStack stack, AspectList cost, boolean commit) {
        if (cost == null || cost.size() == 0) return false;
        VisAmuletData next = data(stack);
        for (Aspect aspect : cost.getAspects()) if (!aspect.isPrimal() || cost.getAmount(aspect) < 0 || next.amount(aspect.getTag()) < cost.getAmount(aspect)) return false;
        if (commit) {
            for (Aspect aspect : cost.getAspects()) next = next.withAmount(aspect.getTag(), next.amount(aspect.getTag()) - cost.getAmount(aspect), getMaxVis(stack));
            stack.set(component(), next);
        }
        return true;
    }
    @Override public BaubleType getBaubleType(ItemStack stack) { return BaubleType.AMULET; }
    @Override public int getRunicCharge(ItemStack stack) { return 0; }
    @Override public boolean canEquip(ItemStack stack, LivingEntity wearer) {
        return lesser || wearer instanceof Player player && KnowledgeAccess.get(player).isResearchComplete("VISAMULET");
    }
    @Override public void onWornTick(ItemStack stack, LivingEntity wearer) {
        if (!(wearer instanceof Player player) || player.level().isClientSide || player.tickCount % 5 != 0) return;
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof ItemWandCasting) {
            var wand = ItemWandCasting.data(held);
            // The amulet fills the wand up to its own capacity; a sceptre holds one and a half times
            // the capacity of its rod (original ItemWandCasting:119-121).
            int capacity = ItemWandCasting.getMaxVis(held);
            for (Aspect aspect : Aspect.getPrimalAspects()) {
                int transfer = Math.min(5, Math.min(capacity - wand.amount(aspect.getTag()), getVis(stack, aspect)));
                if (transfer <= 0) continue;
                storeVis(stack, aspect, getVis(stack, aspect) - transfer);
                wand = wand.withAmount(aspect.getTag(), wand.amount(aspect.getTag()) + transfer);
            }
            held.set(ThaumcraftContent.WAND_DATA.get(), wand);
        }
        thaumcraft.common.lib.baubles.PlayerAccessories.chargeAmulet(player, stack, this);
        player.getInventory().setChanged();
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        if (lesser) lines.add(Component.translatable("item.thaumcraft.vis_amulet_lesser.description").withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("item.thaumcraft.vis_amulet.capacity", getMaxVis(stack) / 100).withStyle(ChatFormatting.GOLD));
        for (Aspect aspect : Aspect.getPrimalAspects()) if (getVis(stack, aspect) > 0)
            lines.add(Component.literal(aspect.getName() + " x " + java.text.DecimalFormat.getNumberInstance().format(getVis(stack, aspect) / 100F)));
    }
}
