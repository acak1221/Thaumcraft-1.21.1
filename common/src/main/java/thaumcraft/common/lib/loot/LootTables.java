package thaumcraft.common.lib.loot;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.internal.WeightedRandomLoot;
import thaumcraft.common.config.ThaumcraftBaubles;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.config.ThaumcraftLoot;

/** Config.initLoot's public pools and Utils.generateLoot's original draw order. */
public final class LootTables {
    private static boolean initialized;
    private LootTables() {}
    public static void initialize() {
        if (initialized) return;
        initialized = true;
        Item coin = ThaumcraftContent.MATERIALS.get("gold_coin").get();
        add(new ItemStack(coin),2500,0); add(new ItemStack(coin,2),2250,1); add(new ItemStack(coin,3),2000,2);
        add(new ItemStack(ThaumcraftContent.PRIMORDIAL_PEARL.get()),1,2); add(new ItemStack(Items.NETHER_STAR),1,2);
        add(new ItemStack(Items.DIAMOND),10,0); add(new ItemStack(Items.DIAMOND),50,1,2);
        add(new ItemStack(Items.EMERALD),15,0); add(new ItemStack(Items.EMERALD),75,1,2);
        add(new ItemStack(Items.GOLD_INGOT),100,0,1,2); add(new ItemStack(Items.ENDER_PEARL),100,0,1,2);
        add(new ItemStack(ThaumcraftContent.MATERIALS.get("knowledge_fragment").get()),25,0,1,2);
        add(new ItemStack(ThaumcraftBaubles.MUNDANE_AMULET.get()),10,0);
        add(new ItemStack(ThaumcraftBaubles.MUNDANE_RING.get()),10,0);
        add(new ItemStack(ThaumcraftBaubles.MUNDANE_BELT.get()),10,0);
        for (String primal : List.of("aer","terra","ignis","aqua","ordo","perditio")) {
            add(new ItemStack(ThaumcraftBaubles.DISCOUNT_RINGS.get(primal).get()),5,1);
            add(new ItemStack(ThaumcraftBaubles.DISCOUNT_RINGS.get(primal).get()),7,2);
        }
        add(ThaumcraftLoot.lootAmulet(),6,1,2); add(new ItemStack(ThaumcraftBaubles.RUNIC_RING_LESSER.get()),5,1,2);
        add(new ItemStack(Items.EXPERIENCE_BOTTLE),5,0); add(new ItemStack(Items.EXPERIENCE_BOTTLE),10,1); add(new ItemStack(Items.EXPERIENCE_BOTTLE),20,2);
        for (int tier=0;tier<3;tier++) add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE),tier+1,tier);
        for (int tier=0;tier<3;tier++) add(new ItemStack(Items.GOLDEN_APPLE),3*(tier+1),tier);
        add(new ItemStack(Items.BOOK),10,0,1,2);
        for (var variant : LootPotionVariants.all()) add(potion(variant),variant.weight(),0,1,2);
    }
    private static void add(ItemStack item,int weight,int... tiers) { ThaumcraftApi.addLootBagItem(item,weight,tiers); }

    /** Native custom effects avoid silently applying a second modern built-in potion effect. */
    public static ItemStack potion(LootPotionVariants.Variant variant) {
        ItemStack stack = new ItemStack(variant.splash()?Items.SPLASH_POTION:Items.POTION);
        stack.set(DataComponents.POTION_CONTENTS,new PotionContents(Optional.empty(),Optional.of(variant.color()),
                List.of(new MobEffectInstance(effect(variant.effectId()),variant.duration(),variant.amplifier()))));
        stack.set(DataComponents.ITEM_NAME,Component.translatable("item.minecraft."+(variant.splash()?"splash_potion":"potion")+".effect."+variant.name()));
        return stack;
    }
    private static Holder<MobEffect> effect(int id) {
        return switch(id) { case 1->MobEffects.MOVEMENT_SPEED;case 2->MobEffects.MOVEMENT_SLOWDOWN;case 5->MobEffects.DAMAGE_BOOST;case 6->MobEffects.HEAL;case 7->MobEffects.HARM;case 10->MobEffects.REGENERATION;case 12->MobEffects.FIRE_RESISTANCE;case 13->MobEffects.WATER_BREATHING;case 14->MobEffects.INVISIBILITY;case 16->MobEffects.NIGHT_VISION;case 18->MobEffects.WEAKNESS;case 19->MobEffects.POISON;default->throw new IllegalArgumentException("Not an original loot potion effect");};
    }
    public static ItemStack generateLoot(ServerLevel level,int rarity,RandomSource random) {
        if (!level.getServer().isSameThread() || level.getServer().getLevel(level.dimension()) != level)
            throw new IllegalStateException("Loot generation requires the canonical server level/thread");
        initialize();
        ItemStack result;
        WeightedRandomLoot selected = null;
        if (rarity>0 && random.nextFloat()<.025F*rarity) {
            result=gear(level,rarity,random);
            if (result==null) result=generateLoot(level,rarity,random);
        } else {
            List<WeightedRandomLoot> entries=rarity==1?WeightedRandomLoot.lootBagUncommon:rarity==2?WeightedRandomLoot.lootBagRare:WeightedRandomLoot.lootBagCommon;
            int total=0; for(var entry:entries) total+=entry.itemWeight;
            int draw=random.nextInt(total);
            for(var entry:entries) { draw-=entry.itemWeight; if(draw<0) { selected=entry;break; } }
            if(selected==null) throw new IllegalStateException("Original weighted loot pool has no result");
            result=selected.item;
        }
        if(result.is(Items.BOOK)) {
            ItemStack original=result;
            result=OriginalLootEnchantments.enchant(level,result,random,(int)(5F+rarity*.75F*random.nextInt(18)));
            // All registrations of the SAME original stack observe its in-place BOOK conversion.
            // Modern stacks need a replacement; rebind aliases in all three public pools together.
            if(selected!=null) for(var pool:List.of(WeightedRandomLoot.lootBagCommon,WeightedRandomLoot.lootBagUncommon,WeightedRandomLoot.lootBagRare))
                for(var entry:pool) if(entry.item==original)entry.item=result;
        }
        rebindEnchantments(level,result);
        return result.copy();
    }
    /** Static public templates can outlive a server registry. Rebind their exact keys/levels, with NO RNG. */
    private static void rebindEnchantments(ServerLevel level,ItemStack stack) {
        var registry=level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        for(var component:List.of(DataComponents.ENCHANTMENTS,DataComponents.STORED_ENCHANTMENTS)) {
            ItemEnchantments old=stack.get(component); if(old==null||old.isEmpty())continue;
            var replacement=new ItemEnchantments.Mutable(old); replacement.removeIf(holder->true);
            for(var entry:old.entrySet()) replacement.set(registry.getHolderOrThrow(entry.getKey().unwrapKey().orElseThrow()),entry.getIntValue());
            stack.set(component,replacement.toImmutable());
        }
    }
    private static ItemStack gear(ServerLevel level,int rarity,RandomSource random) {
        int quality=random.nextInt(2);
        if(random.nextFloat()<.2F)quality++; if(random.nextFloat()<.15F)quality++; if(random.nextFloat()<.1F)quality++;
        if(random.nextFloat()<.095F)quality++; if(random.nextFloat()<.095F)quality++;
        Item item=getGearItemForSlot(random.nextInt(5),quality); if(item==null)return null;
        ItemStack stack=new ItemStack(item); stack.setDamageValue(random.nextInt(1+stack.getMaxDamage()/6));
        if(random.nextInt(4)<rarity) stack=OriginalLootEnchantments.enchant(level,stack,random,(int)(5F+rarity*.75F*random.nextInt(18)));
        return stack.copy();
    }
    public static Item getGearItemForSlot(int slot,int quality) {
        if(quality<0||quality>6)return null;
        return switch(slot) {
            case 4 -> switch(quality) { case 0->Items.LEATHER_HELMET;case 1->Items.GOLDEN_HELMET;case 2->Items.CHAINMAIL_HELMET;case 3->Items.IRON_HELMET;case 4->ThaumcraftEquipment.THAUMIUM_HELMET.get();case 5->Items.DIAMOND_HELMET;default->ThaumcraftEquipment.VOID_HELMET.get();};
            case 3 -> switch(quality) { case 0->Items.LEATHER_CHESTPLATE;case 1->Items.GOLDEN_CHESTPLATE;case 2->Items.CHAINMAIL_CHESTPLATE;case 3->Items.IRON_CHESTPLATE;case 4->ThaumcraftEquipment.THAUMIUM_CHESTPLATE.get();case 5->Items.DIAMOND_CHESTPLATE;default->ThaumcraftEquipment.VOID_CHESTPLATE.get();};
            case 2 -> switch(quality) { case 0->Items.LEATHER_LEGGINGS;case 1->Items.GOLDEN_LEGGINGS;case 2->Items.CHAINMAIL_LEGGINGS;case 3->Items.IRON_LEGGINGS;case 4->ThaumcraftEquipment.THAUMIUM_LEGGINGS.get();case 5->Items.DIAMOND_LEGGINGS;default->ThaumcraftEquipment.VOID_LEGGINGS.get();};
            case 1 -> switch(quality) { case 0->Items.LEATHER_BOOTS;case 1->Items.GOLDEN_BOOTS;case 2->Items.CHAINMAIL_BOOTS;case 3->Items.IRON_BOOTS;case 4->ThaumcraftEquipment.THAUMIUM_BOOTS.get();case 5->Items.DIAMOND_BOOTS;default->ThaumcraftEquipment.VOID_BOOTS.get();};
            case 0 -> switch(quality) { case 0->Items.IRON_AXE;case 1->Items.IRON_SWORD;case 2->Items.GOLDEN_AXE;case 3->Items.GOLDEN_SWORD;case 4->ThaumcraftEquipment.THAUMIUM_SWORD.get();case 5->Items.DIAMOND_SWORD;default->ThaumcraftEquipment.VOID_SWORD.get();};
            default -> null;
        };
    }
}
