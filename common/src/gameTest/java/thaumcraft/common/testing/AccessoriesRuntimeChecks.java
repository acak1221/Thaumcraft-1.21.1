package thaumcraft.common.testing;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.baubles.IBauble;
import thaumcraft.common.config.ThaumcraftBaubles;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftRunic;
import thaumcraft.common.container.ContainerAccessories;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.WandManager;
import thaumcraft.common.items.wands.WandVisDiscount;
import thaumcraft.common.lib.baubles.PlayerAccessories;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.AccessoryNetworking;
import thaumcraft.common.lib.runic.RunicArmor;

/** Declared actual items/research, real native click packets, player NBT/death and PlayerList respawn. */
public final class AccessoriesRuntimeChecks {
    private AccessoriesRuntimeChecks() {}
    public static void menuPersistenceAndDeath(GameTestHelper test) {
        var f=new TubeVariantsRuntimeChecks.Fixture(test);ServerPlayer current=f.player;
        boolean keep=f.level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
        try {
            focusCentivis(test);
            f.open();var inventory=PlayerAccessories.get(f.player);
            ItemStack ring=RunicArmor.augment(RunicArmor.augment(RunicArmor.augment(new ItemStack(ThaumcraftBaubles.RUNIC_RING_LESSER.get()))));
            ring.set(DataComponents.CUSTOM_NAME,Component.literal("declared native ring"));
            f.player.getInventory().setItem(9,ring.copy());
            f.player.getInventory().setItem(10,new ItemStack(ThaumcraftBaubles.VIS_AMULET_LESSER.get()));
            f.player.getInventory().setItem(11,new ItemStack(ThaumcraftBaubles.MUNDANE_BELT.get()));
            f.player.getInventory().setItem(12,new ItemStack(ThaumcraftBaubles.VIS_AMULET.get()));
            f.player.getInventory().setItem(13,new ItemStack(ThaumcraftBaubles.DISCOUNT_RINGS.get("aer").get()));
            KnowledgeAccess.get(f.player).removeResearch("VISAMULET");
            test.assertTrue(AccessoryNetworking.open(f.player)&&f.player.containerMenu instanceof ContainerAccessories,"real validated open request creates the native four-slot menu");
            var menu=(ContainerAccessories)f.player.containerMenu;
            for(int index:new int[]{4,5,6,8})click(f.player,index,ClickType.QUICK_MOVE);
            test.assertTrue(inventory.getItem(0).is(ThaumcraftBaubles.VIS_AMULET_LESSER.get())&&ItemStack.matches(inventory.getItem(1),ring)
                    &&inventory.getItem(2).is(ThaumcraftBaubles.DISCOUNT_RINGS.get("aer").get())&&inventory.getItem(3).is(ThaumcraftBaubles.MUNDANE_BELT.get())
                    &&f.player.getInventory().getItem(9).isEmpty()&&menu.getCarried().isEmpty(),"actual native quick-move fills amulet/ring/ring/belt once and preserves signed hardening/name components");
            click(f.player,7,ClickType.PICKUP);click(f.player,0,ClickType.PICKUP);
            test.assertTrue(menu.getCarried().is(ThaumcraftBaubles.VIS_AMULET.get())&&inventory.getItem(0).is(ThaumcraftBaubles.VIS_AMULET_LESSER.get()),"greater vis amulet cannot replace an equipped lesser before actual VISAMULET research");
            KnowledgeAccess.get(f.player).addResearch("VISAMULET");click(f.player,0,ClickType.PICKUP);click(f.player,7,ClickType.PICKUP);
            test.assertTrue(menu.getCarried().isEmpty()&&inventory.getItem(0).is(ThaumcraftBaubles.VIS_AMULET.get()),"same native swap succeeds after actual research, returning the former item to its source slot");
            var before=inventory.save(f.level.registryAccess());
            f.player.connection.handleContainerClick(new ServerboundContainerClickPacket(menu.containerId+1,menu.getStateId(),1,0,ClickType.QUICK_MOVE,ItemStack.EMPTY,new Int2ObjectOpenHashMap<>()));
            test.assertTrue(before.equals(inventory.save(f.level.registryAccess())),"forged different window id cannot move the real worn item");
            test.assertTrue(WandVisDiscount.percentagePoints(f.player,Aspect.AIR)==1&&WandVisDiscount.percentagePoints(f.player,Aspect.EARTH)==0,"actual apprentice ring discounts exactly its one original primal by one percentage point");
            var amulet=ThaumcraftBaubles.VIS_AMULET.get();var worn=inventory.getItem(0);
            amulet.storeVis(worn,Aspect.AIR,9);amulet.storeVis(worn,Aspect.EARTH,9);
            ItemStack wand=new ItemStack(ThaumcraftContent.WAND.get());wand.set(ThaumcraftContent.WAND_DATA.get(),new WandData("greatwood","gold",Map.of("aer",100,"terra",100)));f.player.getInventory().setItem(35,wand);
            test.assertTrue(WandManager.consumeVisFromInventory(f.player,new AspectList().add(Aspect.AIR,10).add(Aspect.EARTH,10))
                    &&amulet.getVis(worn,Aspect.AIR)==9&&amulet.getVis(worn,Aspect.EARTH)==9&&ItemWandCasting.getVis(wand,"aer")==91&&ItemWandCasting.getVis(wand,"terra")==90,
                    "a short amulet pays nothing; one independent full wand pays9 aer (10*.99F truncates) and10 terra with actual aspect discounts");
            test.assertTrue(WandManager.consumeVisFromInventory(f.player,new AspectList().add(Aspect.AIR,9).add(Aspect.EARTH,9))
                    &&amulet.getVis(worn,Aspect.AIR)==0&&amulet.getVis(worn,Aspect.EARTH)==0&&ItemWandCasting.getVis(wand,"aer")==91&&ItemWandCasting.getVis(wand,"terra")==90,
                    "one full worn amulet has original raw-cost priority over inventory35 and never applies wand discounts");
            f.player.closeContainer();var saved=f.player.saveWithoutId(new CompoundTag());
            inventory.removeItem(1,1);f.player.load(saved);
            test.assertTrue(ItemStack.matches(PlayerAccessories.get(f.player).getItem(1),ring)&&RunicArmor.finalCharge(PlayerAccessories.get(f.player).getItem(1))==4,
                    "actual player NBT save/load restores the actual slot and full signed hardening/name components");
            var canonical=inventory.save(f.level.registryAccess());var malformed=canonical.copy();var entries=malformed.getList("Items",10);entries.add(entries.getCompound(0).copy());
            inventory.load(malformed,f.level.registryAccess());
            test.assertTrue(inventory.isRejected()&&inventory.isEmpty()&&inventory.save(f.level.registryAccess()).equals(malformed),"duplicate-slot NBT is preserved inactive rather than duplicated or silently discarded");
            inventory.load(canonical,f.level.registryAccess());f.player.getInventory().clearContent();
            f.player.setRespawnPosition(f.level.dimension(),f.base.below(),0,true,false);
            f.level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(false,f.level.getServer());
            test.assertTrue(f.player.hurt(f.level.damageSources().fellOutOfWorld(),Float.MAX_VALUE)&&!f.player.isAlive(),"actual native out-of-world damage executes survival death despite login protection");
            var drops=f.drops().stream().filter(drop->drop.getItem().getItem() instanceof IBauble).toList();
            test.assertTrue(inventory.isEmpty()&&drops.size()==4&&drops.stream().filter(drop->ItemStack.matches(drop.getItem(),ring)).count()==1
                    &&drops.stream().allMatch(drop->drop.getItem().getCount()==1),"native death drops exactly four owned accessories, including one component-preserving ring, and empties the original slots");
            current=f.level.getServer().getPlayerList().respawn(f.player,false,Entity.RemovalReason.KILLED);
            test.assertTrue(current!=f.player&&PlayerAccessories.get(current).isEmpty(),"actual PlayerList respawn after non-kept death cannot copy the already dropped accessories");
            current.setPos(f.base.getX()+.5,f.base.getY(),f.base.getZ()+.5);current.setNoGravity(true);
            PlayerAccessories.get(current).setItem(1,ring.copy());current.setRespawnPosition(f.level.dimension(),f.base.below(),0,true,false);
            f.level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true,f.level.getServer());
            var previous=current;int dropCount=f.drops().size();
            test.assertTrue(current.hurt(f.level.damageSources().fellOutOfWorld(),Float.MAX_VALUE)&&!current.isAlive()&&ItemStack.matches(PlayerAccessories.get(current).getItem(1),ring),"keepInventory native death retains the worn original stack");
            current=f.level.getServer().getPlayerList().respawn(previous,false,Entity.RemovalReason.KILLED);
            test.assertTrue(ItemStack.matches(PlayerAccessories.get(current).getItem(1),ring)&&PlayerAccessories.get(previous).isEmpty()&&f.drops().size()==dropCount,
                    "native respawn transfers kept accessory ownership once, preserving components and producing no duplicate drop");
        } finally {
            f.level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(keep,f.level.getServer());
            if(current!=f.player)current.server.getPlayerList().remove(current);
            f.close();
        }
        test.succeed();
    }

    /** Original ItemWandCasting:247-259, consumeAllVis per-aspect truncation and actual component debit. */
    private static void focusCentivis(GameTestHelper test) {
        var focus = new ItemStack(thaumcraft.common.config.ThaumcraftFoci.FIRE_FOCUS.get());
        focus.set(thaumcraft.common.config.ThaumcraftFoci.FOCUS_UPGRADES.get(),
                new thaumcraft.common.items.wands.foci.FocusUpgradeData(java.util.List.of((short)1, (short)1, (short)1, (short)1, (short)1)));
        var wand = new ItemStack(ThaumcraftContent.WAND.get());
        wand.set(ThaumcraftContent.WAND_DATA.get(), new WandData("greatwood", "thaumium", Map.of("aer", 100, "terra", 100, "ignis", 100)));
        wand.set(thaumcraft.common.config.ThaumcraftFoci.WAND_FOCUS.get(), new thaumcraft.common.items.wands.foci.FocusData(focus));
        var costs = thaumcraft.common.items.wands.FocusVis.price(new AspectList().add(Aspect.AIR, 5)
                .add(Aspect.EARTH, 2).add(Aspect.FIRE, 15), wand, null);
        test.assertTrue(costs.equals(Map.of("aer", 1, "terra", 0, "ignis", 5)),
                "five Frugal: original .39999998 float truncates 5/2/15 centivis to 1/0/5");
        test.assertTrue(thaumcraft.common.items.wands.FocusVis.debit(wand, costs), "actual casting debit succeeds");
        test.assertTrue(ItemWandCasting.getVis(wand, "aer") == 99 && ItemWandCasting.getVis(wand, "terra") == 100
                && ItemWandCasting.getVis(wand, "ignis") == 95, "actual component loses 1/0/5, not rounded prices or whole vis");
        var before = ItemWandCasting.data(wand);
        test.assertTrue(!thaumcraft.common.items.wands.FocusVis.debit(wand, Map.of("aer", 100, "ignis", 1))
                && before.equals(ItemWandCasting.data(wand)), "insufficient multi-aspect price cannot partially debit");
        focus.set(thaumcraft.common.config.ThaumcraftFoci.FOCUS_UPGRADES.get(),
                new thaumcraft.common.items.wands.foci.FocusUpgradeData(java.util.List.of((short)1, (short)-1, (short)-1, (short)-1, (short)-1)));
        wand.set(ThaumcraftContent.WAND_DATA.get(), new WandData("greatwood", "gold", Map.of("ignis", 100)));
        wand.set(thaumcraft.common.config.ThaumcraftFoci.WAND_FOCUS.get(), new thaumcraft.common.items.wands.foci.FocusData(focus));
        var thirteen = thaumcraft.common.items.wands.FocusVis.price(new AspectList().add(Aspect.FIRE, 15), wand, null);
        test.assertTrue(thirteen.equals(Map.of("ignis", 13)) && thaumcraft.common.items.wands.FocusVis.debit(wand, thirteen)
                && ItemWandCasting.getVis(wand, "ignis") == 87, "one Frugal truncates 13.5 to 13 and debits it");
    }
    private static void click(ServerPlayer player,int slot,ClickType type){
        var menu=player.containerMenu;
        player.connection.handleContainerClick(new ServerboundContainerClickPacket(menu.containerId,menu.getStateId(),slot,0,type,ItemStack.EMPTY,new Int2ObjectOpenHashMap<>()));
    }
}
