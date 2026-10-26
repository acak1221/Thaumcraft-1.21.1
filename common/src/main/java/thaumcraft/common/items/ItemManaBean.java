package thaumcraft.common.items;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.common.blocks.BlockManaPod;
import thaumcraft.common.config.ThaumcraftFlux;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftManaBeans;
import thaumcraft.common.config.ThaumcraftWarp;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.tiles.TileManaPod;

/** Actual TC4 mana bean: typed food, ten-tick use and hanging-pod planting.
 * Fresh contents use ITEM-local RNG. The effect lottery is the original fresh
 * TC4 40-slot Potion array, including its eight null entries, never all modern
 * registered effects. Explicit empty or unrelated component patches remain
 * initialized legacy NBT and are not rerolled. */
public final class ItemManaBean extends Item implements IEssentiaContainerItem {
    // Original displayAspects: the 48 original aspects in practice, see OriginalDisplayAspects.
    private static final Aspect[] DISPLAY_ASPECTS=OriginalDisplayAspects.ALL;
    /** Original ItemManaBean:68 - the random potion of a fresh bean lasts 160 ticks plus a random of eighty. */
    public static final int EFFECT_DURATION = 160;
    /** Original ItemManaBean:68 - the random part of the duration of the potion of a fresh bean. */
    public static final int EFFECT_DURATION_RANDOM = 80;
    /** Original ItemManaBean:71 - a quarter of the beans add their aspect to the pool of the player. */
    public static final float ASPECT_POOL_CHANCE = 0.25F;
    private static Supplier<? extends Player> tooltipViewer=()->null;
    private final Random random=new Random();
    public ItemManaBean(Properties properties){super(properties.stacksTo(64).food(new FoodProperties.Builder().nutrition(1).saturationModifier(.5F).alwaysEdible().build()));}
    public static void setTooltipViewer(Supplier<? extends Player> viewer){tooltipViewer=Objects.requireNonNull(viewer);}
    public static CrystalEssenceData data(ItemStack stack){return stack==null||stack.isEmpty()||!(stack.getItem() instanceof ItemManaBean)?null:stack.get(ThaumcraftManaBeans.MANA_BEAN_DATA.get());}
    public static ItemStack create(Aspect aspect){
        if(aspect==null||Aspect.getAspect(aspect.getTag())!=aspect)throw new IllegalArgumentException("Noncanonical bean aspect");
        ItemStack stack=new ItemStack(ThaumcraftManaBeans.MANA_BEAN.get());
        stack.set(ThaumcraftManaBeans.MANA_BEAN_DATA.get(),CrystalEssenceData.fromAspects(new AspectList().add(aspect,1)));return stack;
    }
    @Override public AspectList getAspects(ItemStack stack){var data=data(stack);return data==null||data.isEmpty()?null:data.toAspects();}
    @Override public void setAspects(ItemStack stack,AspectList aspects){
        if(stack==null||stack.isEmpty()||stack.getItem()!=this)throw new IllegalArgumentException("Not this mana bean");
        CrystalEssenceData validated=CrystalEssenceData.fromAspects(aspects);stack.set(ThaumcraftManaBeans.MANA_BEAN_DATA.get(),validated);
    }
    /** Original onCreated initialization, callable on staged Pech reward copies. */
    public void initializeFresh(ItemStack stack){
        if(!stack.isEmpty()&&stack.getItem()==this&&stack.getComponentsPatch().isEmpty()&&DISPLAY_ASPECTS.length>0)
            setAspects(stack,new AspectList().add(DISPLAY_ASPECTS[random.nextInt(DISPLAY_ASPECTS.length)],1));
    }
    @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected){if(!level.isClientSide)initializeFresh(stack);super.inventoryTick(stack,level,entity,slot,selected);}
    @Override public void onCraftedBy(ItemStack stack,Level level,Player player){initializeFresh(stack);super.onCraftedBy(stack,level,player);}
    @Override public int getUseDuration(ItemStack stack,LivingEntity consumer){return 10;}
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity consumer){
        ServerPlayer actor=consumer instanceof ServerPlayer player&&level instanceof ServerLevel server&&player.level()==server&&server.getServer().isSameThread()&&player.isAlive()
                &&!stack.isEmpty()&&stack.getItem()==this&&stack.has(DataComponents.FOOD)&&player.isUsingItem()&&player.getUseItemRemainingTicks()==0
                &&player.getUseItem()==stack&&player.getItemInHand(player.getUsedItemHand())==stack?player:null;
        CrystalEssenceData contents=data(stack); // Native food payment can empty the actual stack.
        ItemStack result=super.finishUsingItem(stack,level,consumer);
        if(actor!=null){
            int slot=level.random.nextInt(40);Holder<MobEffect> effect=effectAt(slot);
            if(effect!=null){
                if(effect.value().isInstantenous()){
                    // 1.7 Saturation was PotionHealth, but affectEntity only
                    // handles Heal/Harm: keep its no-op and NO duration draw.
                    if(slot!=23)effect.value().applyInstantenousEffect(actor,actor,actor,2,3);
                }
                else actor.addEffect(new MobEffectInstance(effect,EFFECT_DURATION+level.random.nextInt(EFFECT_DURATION_RANDOM),0));
            }
            if(level.random.nextFloat()<ASPECT_POOL_CHANCE&&contents!=null&&!contents.isEmpty()){
                KnowledgeAccess.get(actor).addAspectPool(contents.firstTag(),1);KnowledgeEvents.sync(actor);
                thaumcraft.common.lib.network.KnowledgeNotificationNetworking.pool(actor,contents.firstTag(),1);
            }
        }
        return result;
    }
    /** Numeric slots are an INTERNAL canonical lottery, never serialized legacy IDs. */
    public static Holder<MobEffect> effectAt(int slot){return switch(slot){
        case 1->MobEffects.MOVEMENT_SPEED;case 2->MobEffects.MOVEMENT_SLOWDOWN;case 3->MobEffects.DIG_SPEED;case 4->MobEffects.DIG_SLOWDOWN;
        case 5->MobEffects.DAMAGE_BOOST;case 6->MobEffects.HEAL;case 7->MobEffects.HARM;case 8->MobEffects.JUMP;case 9->MobEffects.CONFUSION;
        case 10->MobEffects.REGENERATION;case 11->MobEffects.DAMAGE_RESISTANCE;case 12->MobEffects.FIRE_RESISTANCE;case 13->MobEffects.WATER_BREATHING;
        case 14->MobEffects.INVISIBILITY;case 15->MobEffects.BLINDNESS;case 16->MobEffects.NIGHT_VISION;case 17->MobEffects.HUNGER;
        case 18->MobEffects.WEAKNESS;case 19->MobEffects.POISON;case 20->MobEffects.WITHER;case 21->MobEffects.HEALTH_BOOST;
        case 22->MobEffects.ABSORPTION;case 23->MobEffects.SATURATION;
        case 31->BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ThaumcraftInfusion.FLUX_TAINT.get());
        case 32->ThaumcraftFlux.visExhaust();case 33->ThaumcraftFlux.infectiousVisExhaust();
        case 34->ThaumcraftWarp.effect(ThaumcraftWarp.UNNATURAL_HUNGER);case 35->ThaumcraftWarp.effect(ThaumcraftWarp.WARP_WARD);
        case 36->ThaumcraftWarp.effect(ThaumcraftWarp.DEATH_GAZE);case 37->ThaumcraftWarp.effect(ThaumcraftWarp.BLURRED_VISION);
        case 38->ThaumcraftWarp.effect(ThaumcraftWarp.SUN_SCORNED);case 39->ThaumcraftWarp.effect(ThaumcraftWarp.THAUMARHIA);
        default->null;
    };}
    @Override public InteractionResult useOn(UseOnContext context){
        Player player=context.getPlayer();Level level=context.getLevel();var clicked=context.getClickedPos();ItemStack held=context.getItemInHand();
        if(player==null||context.getClickedFace()!=Direction.DOWN||!player.mayUseItemAt(clicked,Direction.DOWN,held)
                ||!level.mayInteract(player,clicked)||!level.getBiome(clicked).is(ThaumcraftManaBeans.MAGICAL_BIOMES)
                ||!BlockManaPod.isSupport(level.getBlockState(clicked)))return InteractionResult.PASS;
        var pos=clicked.below();
        // Original clicking an occupied underside succeeds without spending a bean.
        if(!level.isEmptyBlock(pos)||level.isClientSide)return InteractionResult.sidedSuccess(level.isClientSide);
        if(!player.isAlive()||player.getItemInHand(context.getHand())!=held||held.isEmpty()||held.getItem()!=this)return InteractionResult.FAIL;
        var before=level.getBlockState(pos);var placed=ThaumcraftManaBeans.MANA_POD.get().defaultBlockState();
        if(!level.setBlock(pos,placed,2))return InteractionResult.FAIL;
        if(level.getBlockState(pos)!=placed||!(level.getBlockEntity(pos) instanceof TileManaPod pod)){
            if(level.getBlockState(pos)==placed)level.setBlock(pos,before,2);return InteractionResult.FAIL;
        }
        var data=data(held);if(data!=null&&!data.isEmpty())pod.setAspect(Aspect.getAspect(data.firstTag()));
        if(!player.getAbilities().instabuild)held.shrink(1);
        return InteractionResult.CONSUME;
    }
    public static int color(ItemStack stack){
        var data=data(stack);Aspect aspect=data==null||data.isEmpty()?(DISPLAY_ASPECTS.length==0?null:DISPLAY_ASPECTS[(int)Math.floorMod(System.currentTimeMillis()/500L,DISPLAY_ASPECTS.length)]):Aspect.getAspect(data.firstTag());
        return 0xff000000|(aspect==null?0xffffff:aspect.getColor());
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tooltip,TooltipFlag flags){Player viewer=tooltipViewer.get();appendAspectTooltip(stack,viewer!=null&&viewer.level().isClientSide?viewer:null,tooltip);super.appendHoverText(stack,context,tooltip,flags);}
    public void appendAspectTooltip(ItemStack stack,Player viewer,List<Component> tooltip){
        AspectList aspects=getAspects(stack);if(aspects==null)return;
        for(Aspect aspect:aspects.getAspectsSorted())tooltip.add(viewer!=null&&KnowledgeAccess.get(viewer).hasDiscoveredAspect(aspect.getTag())?Component.literal(aspect.getName()+" x"+aspects.getAmount(aspect)):Component.translatable("tc.aspect.unknown"));
    }
}
