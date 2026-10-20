package thaumcraft.common.items.armor;

import dev.architectury.registry.menu.MenuRegistry;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumcraft.api.IRepairable;
import thaumcraft.api.IRunicArmor;
import thaumcraft.api.IVisDiscountGear;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.container.ContainerHoverHarness;
import thaumcraft.common.items.ItemJar;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;

/** Original epic SPECIAL chest armor,400 durability. RMB opens the fuel menu;
 * equipping is an ordinary native armor-slot/dispenser operation. */
public final class ItemHoverHarness extends ArmorItem implements IRepairable,IRunicArmor,IVisDiscountGear {
    private static Supplier<? extends Player> tooltipViewer=()->null;
    public ItemHoverHarness(Holder<ArmorMaterial> material,Properties properties) { super(material,Type.CHESTPLATE,properties.durability(400).rarity(Rarity.EPIC)); }
    public static void setTooltipViewer(Supplier<? extends Player> viewer) { tooltipViewer=Objects.requireNonNull(viewer); }
    @Override public int getRunicCharge(ItemStack stack) { return 0; }
    @Override public int getVisDiscount(ItemStack stack,Player player,Aspect aspect) { return aspect==Aspect.AIR?5:2; }
    @Override public boolean isValidRepairItem(ItemStack stack,ItemStack repair) { return repair.is(Items.GOLD_INGOT); }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        ItemStack armor=player.getItemInHand(hand);
        if(!level.isClientSide) {
            if(!(player instanceof ServerPlayer server)||!server.server.isSameThread()||!server.isAlive()||server.isRemoved()||server.hasDisconnected()||server.isSpectator()
                    ||server.server.getPlayerList().getPlayer(server.getUUID())!=server||server.containerMenu!=server.inventoryMenu
                    ||!server.inventoryMenu.getCarried().isEmpty()||!HarnessData.isHarness(armor))return InteractionResultHolder.fail(armor);
            try {HarnessData.jar(armor);HarnessData.charge(armor);}catch(IllegalArgumentException invalid){return InteractionResultHolder.fail(armor);}
            MenuRegistry.openExtendedMenu(server,new SimpleMenuProvider((id,inventory,viewer)->new ContainerHoverHarness(id,inventory,hand),
                    Component.translatable("container.hoverharness")),buffer->buffer.writeBoolean(hand==InteractionHand.OFF_HAND));
        }
        return InteractionResultHolder.sidedSuccess(armor,level.isClientSide);
    }
    public void appendAspectTooltip(ItemStack armor,Player viewer,List<Component> lines) {
        try {ItemStack jar=HarnessData.jar(armor);if(ItemJar.getAmount(jar)>0){Aspect aspect=ItemJar.getAspect(jar);
            lines.add(viewer!=null&&KnowledgeAccess.get(viewer).hasDiscoveredAspect(aspect.getTag())?Component.literal(aspect.getName()+" x "+ItemJar.getAmount(jar)):Component.translatable("tc.aspect.unknown"));}}
        catch(IllegalArgumentException invalid) { /* Retain invalid saved material without advertising free fuel. */ }
        lines.add(Component.translatable("tc.visdiscount").append(": 2%").withStyle(ChatFormatting.DARK_PURPLE));
        lines.add(Component.translatable("tc.visdiscount").append(" (Aer): 5%").withStyle(ChatFormatting.DARK_PURPLE));
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tooltip,TooltipFlag flags) {
        super.appendHoverText(stack,context,tooltip,flags);Player viewer=tooltipViewer.get();appendAspectTooltip(stack,viewer!=null&&viewer.level().isClientSide?viewer:null,tooltip);
    }
}
