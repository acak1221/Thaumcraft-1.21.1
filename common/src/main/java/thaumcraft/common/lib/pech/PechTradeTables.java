package thaumcraft.common.lib.pech;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import thaumcraft.common.config.ThaumcraftBaubles;
import thaumcraft.common.config.ThaumcraftColoredCandles;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftCrystalClusters;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.config.ThaumcraftFocusPouches;
import thaumcraft.common.config.ThaumcraftManaBeans;
import thaumcraft.common.config.ThaumcraftNaturals;
import thaumcraft.common.config.ThaumcraftPech;
import thaumcraft.common.lib.enchantment.EquipmentEnchantments;

/** Ordered ORIGINAL entries, sampled uniformly with rejection by value tier.
 * Duplicates are weights: the Forager has TWO Silverwood-sapling entries, and
 * Stalker's candle loop ends at14. Optional external-metal entries have no
 * activation in a fresh TC4-only world; they are not fake vanilla substitutes. */
public final class PechTradeTables {
    private PechTradeTables(){}
    public record Entry(int tier,ItemStack baseStack){
        public Entry{if(tier<1||tier>5||baseStack==null||baseStack.isEmpty())throw new IllegalArgumentException("Invalid Pech trade entry");baseStack=baseStack.copy();}
        @Override public ItemStack baseStack(){return baseStack.copy();}
    }
    public static List<Entry> entries(ServerLevel level,int type){
        var rows=new ArrayList<Entry>();
        rows.add(new Entry(1,new ItemStack(ThaumcraftManaBeans.MANA_BEAN.get())));
        switch(type){
            case 0->{
                for(String id:new String[]{"cluster_iron","cluster_gold","cluster_cinnabar"})rows.add(new Entry(1,new ItemStack(ThaumcraftContent.MATERIALS.get(id).get())));
                rows.add(new Entry(2,new ItemStack(Items.BLAZE_ROD)));rows.add(new Entry(2,new ItemStack(ThaumcraftNaturals.GREATWOOD_SAPLING.get())));
                potion(rows,2,Potions.STRENGTH);potion(rows,2,Potions.SWIFTNESS);
                rows.add(new Entry(3,new ItemStack(Items.EXPERIENCE_BOTTLE)));fragment(rows,3);rows.add(new Entry(3,new ItemStack(Items.GOLDEN_APPLE)));
                potion(rows,3,Potions.LONG_STRENGTH);potion(rows,3,Potions.LONG_NIGHT_VISION);
                rows.add(new Entry(5,new ItemStack(Items.ENCHANTED_GOLDEN_APPLE)));rows.add(new Entry(4,new ItemStack(ThaumcraftEquipment.THAUMIUM_PICKAXE.get())));
                rows.add(new Entry(5,new ItemStack(ThaumcraftNaturals.SILVERWOOD_SAPLING.get())));rows.add(new Entry(5,new ItemStack(ThaumcraftNaturals.SILVERWOOD_SAPLING.get())));
            }
            case 1->{
                for(String aspect:new String[]{"aer","ignis","aqua","terra","ordo","perditio"})rows.add(new Entry(1,new ItemStack(ThaumcraftContent.SHARDS.get(aspect).get())));
                fragment(rows,1);fragment(rows,2);potion(rows,2,Potions.REGENERATION);potion(rows,2,Potions.HEALING); // 8261: extended bit has no instantaneous effect.
                rows.add(new Entry(3,book(level,EquipmentEnchantments.HASTE)));rows.add(new Entry(3,new ItemStack(Items.GOLDEN_APPLE)));potion(rows,3,Potions.STRONG_REGENERATION);potion(rows,3,Potions.STRONG_HEALING);
                for(String aspect:new String[]{"aer","ignis","aqua","terra","ordo","perditio"})rows.add(new Entry(4,new ItemStack(ThaumcraftContent.CRYSTALS.get(aspect).get())));
                rows.add(new Entry(4,new ItemStack(ThaumcraftCrystalClusters.MIXED.get())));
                rows.add(new Entry(5,new ItemStack(Items.ENCHANTED_GOLDEN_APPLE)));rows.add(new Entry(5,book(level,EquipmentEnchantments.REPAIR)));
                rows.add(new Entry(5,new ItemStack(ThaumcraftFocusPouches.FOCUS_POUCH.get())));rows.add(new Entry(5,new ItemStack(ThaumcraftPech.FOCUS_PECH.get())));rows.add(new Entry(5,new ItemStack(ThaumcraftBaubles.VIS_AMULET_LESSER.get())));
            }
            case 2->{
                for(int meta=0;meta<15;meta++)rows.add(new Entry(1,new ItemStack(ThaumcraftColoredCandles.byMeta(meta))));
                rows.add(new Entry(2,new ItemStack(Items.GHAST_TEAR)));potion(rows,2,Potions.SWIFTNESS);potion(rows,2,Potions.STRENGTH);rows.add(new Entry(2,book(level,Enchantments.POWER)));
                rows.add(new Entry(3,new ItemStack(Items.EXPERIENCE_BOTTLE)));fragment(rows,3);potion(rows,3,Potions.LONG_INVISIBILITY);potion(rows,3,Potions.STRONG_REGENERATION);rows.add(new Entry(3,new ItemStack(Items.GOLDEN_APPLE)));
                rows.add(new Entry(5,new ItemStack(Items.ENCHANTED_GOLDEN_APPLE)));rows.add(new Entry(4,new ItemStack(ThaumcraftEquipment.THAUMIUM_BOOTS.get())));rows.add(new Entry(5,new ItemStack(ThaumcraftBaubles.RUNIC_RING_LESSER.get())));
                rows.add(new Entry(5,book(level,Enchantments.FLAME)));rows.add(new Entry(5,book(level,Enchantments.INFINITY)));
            }
            default->throw new IllegalArgumentException("Unknown Pech variant: "+type);
        }
        return List.copyOf(rows);
    }
    private static void fragment(List<Entry> rows,int tier){rows.add(new Entry(tier,new ItemStack(ThaumcraftContent.MATERIALS.get("knowledge_fragment").get())));}
    private static void potion(List<Entry> rows,int tier,net.minecraft.core.Holder<Potion> potion){ItemStack stack=new ItemStack(Items.POTION);stack.set(DataComponents.POTION_CONTENTS,new PotionContents(potion));rows.add(new Entry(tier,stack));}
    private static ItemStack book(ServerLevel level,ResourceKey<Enchantment> key){ItemStack stack=new ItemStack(Items.ENCHANTED_BOOK);stack.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key),1);return stack;}
}
