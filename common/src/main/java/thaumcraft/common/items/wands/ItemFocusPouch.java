package thaumcraft.common.items.wands;

import dev.architectury.registry.menu.MenuRegistry;
import java.util.Arrays;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import thaumcraft.common.container.ContainerFocusPouch;

/** Original18-slot API. ConfigItems registered its Bauble subclass as ONE item. */
public class ItemFocusPouch extends Item {
    public static final int SIZE=18;
    public ItemFocusPouch(Properties properties) { super(properties.stacksTo(1).rarity(Rarity.RARE)); }
    @Override public boolean isFoil(ItemStack stack) { return false; }
    /** Every result is independent. Oversized native container payloads are
     * refused without rewriting/truncating their retained original component. */
    public ItemStack[] getInventory(ItemStack pouch) {
        requirePouch(pouch);
        var stored=pouch.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).stream().toList();
        if(stored.size()>SIZE)throw new IllegalArgumentException("Focus pouch payload exceeds eighteen slots");
        ItemStack[] contents=new ItemStack[SIZE];Arrays.fill(contents,ItemStack.EMPTY);
        for(int slot=0;slot<stored.size();slot++)contents[slot]=stored.get(slot).copy();
        return contents;
    }
    /** Server API retains complete physical entries, including externally saved
     * foreign items; native GUI insertion separately requires ItemFocusBasic. */
    public void setInventory(ItemStack pouch,ItemStack[] contents) {
        requirePouch(pouch);
        if(contents==null||contents.length!=SIZE)throw new IllegalArgumentException("Focus pouch needs eighteen slots");
        ItemStack[] copied=new ItemStack[SIZE];
        for(int slot=0;slot<SIZE;slot++) {
            ItemStack entry=contents[slot];
            if(entry==null)entry=ItemStack.EMPTY;
            if(!entry.isEmpty()&&(entry.getCount()<=0||entry.getCount()>99))throw new IllegalArgumentException("Unrepresentable native pouch entry count");
            copied[slot]=entry.copy();
        }
        pouch.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(Arrays.asList(copied)));
    }
    private static void requirePouch(ItemStack pouch) {
        if(pouch==null||pouch.getCount()!=1||!(pouch.getItem() instanceof ItemFocusPouch))throw new IllegalArgumentException("One physical focus pouch required");
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        ItemStack pouch=player.getItemInHand(hand);
        if(!level.isClientSide) {
            if(!(player instanceof ServerPlayer server)||!server.isAlive()||server.isSpectator()||pouch.getCount()!=1)return InteractionResultHolder.fail(pouch);
            if(!server.server.isSameThread())throw new IllegalStateException("Focus pouch menu requires the server thread");
            if(server.server.getPlayerList().getPlayer(server.getUUID())!=server||server.containerMenu!=server.inventoryMenu
                    ||!server.inventoryMenu.getCarried().isEmpty())return InteractionResultHolder.fail(pouch);
            try { getInventory(pouch); }catch(RuntimeException malformed){return InteractionResultHolder.fail(pouch);}
            MenuRegistry.openExtendedMenu(server,new SimpleMenuProvider((id,inventory,viewer)->new ContainerFocusPouch(id,inventory,hand),
                    Component.translatable("container.focuspouch")),data->data.writeBoolean(hand==InteractionHand.OFF_HAND));
        }
        return InteractionResultHolder.sidedSuccess(pouch,level.isClientSide);
    }
}
