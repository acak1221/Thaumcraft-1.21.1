package thaumcraft.common.items.armor;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/** Original armor tick and EventHandlerEntity's client previous-step restore.
 * An actual player's worn feet item is the sole source of movement effects. */
public final class TravellerBoots {
    private static final Map<Player,Double> PREVIOUS_STEP=Collections.synchronizedMap(new WeakHashMap<>());
    private TravellerBoots() {}
    public static boolean worn(Player player) {var stack=player.getItemBySlot(EquipmentSlot.FEET);return stack.getCount()==1&&stack.getItem() instanceof ItemBootsTraveller;}
    public static void tick(Player player) {
        boolean worn=worn(player);
        if(worn) {
            if(!player.getAbilities().flying&&player.zza>0) {
                if(player.level().isClientSide&&!player.isShiftKeyDown()) {
                    var step=player.getAttribute(Attributes.STEP_HEIGHT);
                    if(step!=null) {PREVIOUS_STEP.putIfAbsent(player,step.getBaseValue());step.setBaseValue(1);}
                }
                if(player.onGround()) {
                    float bonus=TravelGearRules.groundAcceleration(player.isInWater()),angle=player.getYRot()*(float)Math.PI/180F;
                    float sin=Mth.sin(angle),cos=Mth.cos(angle);
                    player.setDeltaMovement(player.getDeltaMovement().add(0F*cos-bonus*sin,0,bonus*cos+0F*sin));
                }
            }
            player.fallDistance=TravelGearRules.afterBootFall(player.fallDistance);
        }
        if(player.level().isClientSide&&(player.isShiftKeyDown()||!worn))restoreStep(player);
    }
    public static float airAcceleration(Player player,float vanilla) {
        return worn(player)&&!player.getAbilities().flying&&player.zza>0&&!player.onGround()?TravelGearRules.airAcceleration(Hover.getHover(player)):vanilla;
    }
    public static void jumped(Player player) {if(worn(player))player.setDeltaMovement(player.getDeltaMovement().add(0,TravelGearRules.jumpBonus(),0));}
    /** The client disconnect event carries no player, so the step of a player that is already gone is left alone. */
    public static void restoreStep(Player player) {if(player==null)return;Double previous=PREVIOUS_STEP.remove(player);var step=player.getAttribute(Attributes.STEP_HEIGHT);if(previous!=null&&step!=null)step.setBaseValue(previous);}
}
