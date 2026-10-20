package thaumcraft.common.lib.loot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.IntPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.Enchantment;
import thaumcraft.api.IRepairable;

/** Original1.7 EnchantmentHelper/Forge candidate and bilateral-compatibility patches.
 * The modern registry supplies only output holders, never rules/weights/candidates. */
public final class OriginalLootEnchantments {
    enum Kind { ARMOR, BOOTS, HELMET, WEAPON, DAMAGE, DIGGER, BOW, FISHING, DAMAGEABLE, HASTE, REPAIR }
    record Rule(int id,String key,int weight,int maximum,Kind kind) {}
    record Pick(int id,int level) {}
    private static Rule rule(int id,String key,int weight,int maximum,Kind kind){return new Rule(id,key,weight,maximum,kind);}
    static final List<Rule> RULES=List.of(
            rule(0,"protection",10,4,Kind.ARMOR),rule(1,"fire_protection",5,4,Kind.ARMOR),rule(2,"feather_falling",5,4,Kind.BOOTS),
            rule(3,"blast_protection",2,4,Kind.ARMOR),rule(4,"projectile_protection",5,4,Kind.ARMOR),rule(5,"respiration",2,3,Kind.HELMET),
            rule(6,"aqua_affinity",2,1,Kind.HELMET),rule(7,"thorns",1,3,Kind.ARMOR),
            rule(16,"sharpness",10,5,Kind.DAMAGE),rule(17,"smite",5,5,Kind.DAMAGE),rule(18,"bane_of_arthropods",5,5,Kind.DAMAGE),
            rule(19,"knockback",5,2,Kind.WEAPON),rule(20,"fire_aspect",2,2,Kind.WEAPON),rule(21,"looting",2,3,Kind.WEAPON),
            rule(32,"efficiency",10,5,Kind.DIGGER),rule(33,"silk_touch",1,1,Kind.DIGGER),rule(34,"unbreaking",5,3,Kind.DAMAGEABLE),
            rule(35,"fortune",2,3,Kind.DIGGER),rule(48,"power",10,5,Kind.BOW),rule(49,"punch",2,2,Kind.BOW),
            rule(50,"flame",2,1,Kind.BOW),rule(51,"infinity",1,1,Kind.BOW),rule(61,"luck_of_the_sea",2,3,Kind.FISHING),
            rule(62,"lure",2,3,Kind.FISHING),rule(150,"thaumcraft:haste",3,3,Kind.HASTE),rule(151,"thaumcraft:repair",2,2,Kind.REPAIR));
    private OriginalLootEnchantments() {}

    /** Returns the replacement book stack; ordinary equipment is mutated just as the original helper did. */
    public static ItemStack enchant(ServerLevel level,ItemStack stack,RandomSource random,int power) {
        java.util.Objects.requireNonNull(level);java.util.Objects.requireNonNull(stack);java.util.Objects.requireNonNull(random);
        if(!level.getServer().isSameThread())throw new IllegalStateException("Loot enchantment requires server thread");
        return enchant(level.registryAccess(),stack,random,power);
    }
    /** World generation uses its immutable registry snapshot and the generator's own RNG.
     * This overload performs no level queries, chunk writes or access to a server RNG. */
    public static ItemStack enchant(net.minecraft.core.HolderLookup.Provider registries,ItemStack stack,RandomSource random,int power) {
        java.util.Objects.requireNonNull(registries);java.util.Objects.requireNonNull(stack);java.util.Objects.requireNonNull(random);
        if(stack.isEmpty())return stack;
        boolean book=stack.is(Items.BOOK);
        var selected=select(random,enchantability(stack),power,id->book||canApply(find(id),stack));
        ItemStack result=book?stack.transmuteCopy(Items.ENCHANTED_BOOK):stack;
        var registry=registries.lookupOrThrow(Registries.ENCHANTMENT);
        for(Pick pick:selected) {
            Rule rule=find(pick.id());
            var key=ResourceKey.<Enchantment>create(Registries.ENCHANTMENT,ResourceLocation.parse(rule.key().contains(":")?rule.key():"minecraft:"+rule.key()));
            result.enchant(registry.getOrThrow(key),pick.level());
        }
        return result;
    }
    private static int enchantability(ItemStack stack) {
        // Vanilla1.7 ItemHoe did not override Item's zero enchantability; TC4's three hoes explicitly returned5.
        if(stack.getItem() instanceof HoeItem&&BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("minecraft"))return 0;
        return stack.getItem().getEnchantmentValue();
    }
    static boolean canApply(Rule rule,ItemStack stack) {
        var item=stack.getItem();boolean armor=item instanceof ArmorItem;
        boolean digger=item instanceof DiggerItem&&!(item instanceof HoeItem);
        return switch(rule.kind()) {
            case ARMOR -> armor;
            case BOOTS -> armor&&((ArmorItem)item).getEquipmentSlot()==EquipmentSlot.FEET;
            case HELMET -> armor&&((ArmorItem)item).getEquipmentSlot()==EquipmentSlot.HEAD;
            case WEAPON -> item instanceof SwordItem;
            case DAMAGE -> item instanceof SwordItem||item instanceof AxeItem;
            case DIGGER -> digger||(rule.id()==32||rule.id()==33)&&stack.is(Items.SHEARS);
            case BOW -> item instanceof BowItem;
            case FISHING -> item instanceof FishingRodItem;
            case DAMAGEABLE -> item.components().getOrDefault(net.minecraft.core.component.DataComponents.MAX_DAMAGE, 0) > 0; // Original override falls back to ITEM breakability even with Unbreakable NBT.
            case HASTE -> armor&&((ArmorItem)item).getEquipmentSlot()==EquipmentSlot.FEET
                    ||BuiltInRegistries.ITEM.getKey(item).equals(ResourceLocation.fromNamespaceAndPath("thaumcraft","hover_harness"));
            case REPAIR -> stack.isDamageableItem()&&item instanceof IRepairable;
        };
    }
    static Rule find(int id) { return RULES.stream().filter(rule->rule.id()==id).findFirst().orElseThrow(); }
    static int minCost(int id,int level) {
        return switch(id) {
            case 0,16 -> 1+(level-1)*11;
            case 1 -> 10+(level-1)*8;
            case 2 -> 5+(level-1)*6;
            case 3,17,18,34 -> 5+(level-1)*8;
            case 4 -> 3+(level-1)*6;
            case 5 -> level*10;
            case 6 -> 1;
            case 7,20 -> 10+(level-1)*20;
            case 19 -> 5+(level-1)*20;
            case 21,35,61,62,150 -> 15+(level-1)*9;
            case 32,48 -> 1+(level-1)*10;
            case 33 -> 15;
            case 49 -> 12+(level-1)*20;
            case 50,51 -> 20;
            case 151 -> 20+(level-1)*10;
            default -> throw new IllegalArgumentException("Unknown original enchantment id");
        };
    }
    static int maxCost(int id,int level) {
        return switch(id) {
            case 0,16,17,18 -> minCost(id,level)+20;
            case 1,3 -> minCost(id,level)+12;
            case 2 -> minCost(id,level)+10;
            case 4,48 -> minCost(id,level)+15;
            case 5 -> minCost(id,level)+30;
            case 6 -> 41;
            case 49 -> minCost(id,level)+25;
            case 50,51 -> 50;
            default -> 1+level*10+50; // Original subclasses call SUPER minimum, not their override.
        };
    }
    static boolean vanillaCompatible(int first,int second) {
        if(first==second)return false;
        if(first>=0&&first<=4&&second>=0&&second<=4)return first==2||second==2;
        if(first>=16&&first<=18&&second>=16&&second<=18)return false;
        if(first==33&&second==35)return false;
        if((first==21||first==35||first==61)&&second==33)return false;
        return !(first==151&&second==34);
    }
    static boolean compatible(int first,int second) {
        // Actual Forge1.7 patch asks BOTH original enchantments, including asymmetric Silk/Loot and Repair/Unbreaking.
        return vanillaCompatible(first,second)&&vanillaCompatible(second,first);
    }
    /** Exact draw order/float arithmetic; Integer HashMap keys are ORIGINAL ids, not native registry order. */
    static List<Pick> select(RandomSource random,int enchantability,int suppliedLevel,IntPredicate eligible) {
        return select(random,enchantability,suppliedLevel,eligible,OriginalLootEnchantments::compatible);
    }
    /** Shared arithmetic also lets the executable unpatched vanilla oracle verify its own one-way policy. */
    static List<Pick> select(RandomSource random,int enchantability,int suppliedLevel,IntPredicate eligible,
            java.util.function.BiPredicate<Integer,Integer> compatibility) {
        if(enchantability<=0)return List.of();
        int half=enchantability/2;
        int extra=1+random.nextInt((half>>1)+1)+random.nextInt((half>>1)+1);
        int boosted=extra+suppliedLevel;
        float spread=(random.nextFloat()+random.nextFloat()-1F)*.15F;
        int power=(int)(boosted*(1F+spread)+.5F);if(power<1)power=1;
        var candidates=new HashMap<Integer,Pick>();
        for(Rule rule:RULES) {
            if(!eligible.test(rule.id()))continue;
            for(int rank=1;rank<=rule.maximum();rank++)if(power>=minCost(rule.id(),rank)&&power<=maxCost(rule.id(),rank))
                candidates.put(rule.id(),new Pick(rule.id(),rank));
        }
        if(candidates.isEmpty())return List.of();
        var selected=new ArrayList<Pick>();selected.add(weighted(random,candidates));
        for(int remaining=power;random.nextInt(50)<=remaining;remaining>>=1) {
            candidates.keySet().removeIf(candidate->selected.stream().anyMatch(old->!compatibility.test(old.id(),candidate)));
            if(!candidates.isEmpty())selected.add(weighted(random,candidates));
        }
        return List.copyOf(selected);
    }
    private static Pick weighted(RandomSource random,HashMap<Integer,Pick> candidates) {
        int total=0;for(Pick pick:candidates.values())total+=find(pick.id()).weight();
        int roll=random.nextInt(total);
        for(Pick pick:candidates.values())if((roll-=find(pick.id()).weight())<0)return pick;
        throw new IllegalStateException("Original weighted selection exhausted candidates");
    }
}
