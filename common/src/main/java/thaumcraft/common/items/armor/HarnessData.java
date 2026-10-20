package thaumcraft.common.items.armor;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftTravelGear;
import thaumcraft.common.items.ItemJar;

/** The embedded physical jar uses native ItemStack container persistence and
 * streams. No parallel aspect balance may manufacture an empty/full jar copy. */
public final class HarnessData {
    private HarnessData() {}
    public static boolean isHarness(ItemStack armor) { return armor!=null&&armor.getCount()==1&&armor.getItem() instanceof ItemHoverHarness; }
    public static ItemStack jar(ItemStack armor) {
        require(armor);var entries=armor.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).stream().toList();
        if(entries.size()>1)throw new IllegalArgumentException("Harness contains more than one physical slot");
        return entries.isEmpty()?ItemStack.EMPTY:entries.getFirst().copy();
    }
    /** Retain foreign preexisting saved entries so ordinary menu withdrawal can
     * recover them. Only native insertion and fuel use require a Potentia jar. */
    public static void setJar(ItemStack armor,ItemStack jar) {
        require(armor);if(jar==null||!jar.isEmpty()&&jar.getCount()>99)throw new IllegalArgumentException("Invalid retained harness entry");
        armor.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(jar.copy())));
    }
    public static int charge(ItemStack armor) {
        require(armor);int charge=armor.getOrDefault(ThaumcraftTravelGear.HOVER_CHARGE.get(),0);
        if(charge<0||charge>360)throw new IllegalArgumentException("Invalid harness charge counter");return charge;
    }
    public static boolean enabled(ItemStack armor) { return isHarness(armor)&&armor.getOrDefault(ThaumcraftTravelGear.HOVER_ENABLED.get(),false); }
    public static int fuel(ItemStack armor) {
        try { ItemStack jar=jar(armor);return validFuelJar(jar)?ItemJar.getAmount(jar):0; }catch(IllegalArgumentException invalid){return 0;}
    }
    public static boolean validFuelJar(ItemStack jar) { return jar!=null&&jar.getCount()==1&&ItemJar.isJar(jar)&&ItemJar.getAspect(jar)==Aspect.ENERGY&&ItemJar.getAmount(jar)>0; }
    public static void require(ItemStack armor) { if(!isHarness(armor))throw new IllegalArgumentException("One physical harness required"); }
}
