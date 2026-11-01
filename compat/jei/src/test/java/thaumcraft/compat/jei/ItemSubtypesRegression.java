package thaumcraft.compat.jei;

import com.mojang.serialization.Codec;
import java.util.Map;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.common.util.StackHelper;
import mezz.jei.library.ingredients.subtypes.SubtypeManager;
import mezz.jei.library.load.registration.SubtypeRegistration;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.items.CrystalEssenceData;
import thaumcraft.common.items.PhialData;
import thaumcraft.common.items.wands.WandData;

/**
 * Standalone regression runner against the configured JEI implementation, not a mock comparison.
 * Compile/run with the named Minecraft/common/loader classpath and JEI 19.43.0.398 runtime JAR.
 * JEI stays compile-only in the production build; this runner adds no production dependency.
 *
 * <p>Vanilla item bases and local native component types are explicit fixtures: a standalone JVM
 * does not publish Thaumcraft's deferred registry entries. The production subtype factories and
 * JEI's real registration, manager, equality and UID implementation are exercised in both contexts.</p>
 */
public final class ItemSubtypesRegression {
    private static int checks;

    public static void main(String[] args) {
        try { run(); }
        catch (Throwable failure) {
            try (var output = new java.io.PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.err))) {
                failure.printStackTrace(output);
            }
            System.exit(1);
        }
    }

    private static void run() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        check(ThaumcraftJeiPlugin.class.getDeclaredMethod("registerItemSubtypes", ISubtypeRegistration.class)
                .getDeclaringClass() == ThaumcraftJeiPlugin.class, "plugin implements the subtype registration hook");

        var wandData = DataComponentType.<WandData>builder().persistent(WandData.CODEC).build();
        var sceptre = DataComponentType.<Boolean>builder().persistent(Codec.BOOL).build();
        var crystalData = DataComponentType.<CrystalEssenceData>builder().persistent(CrystalEssenceData.CODEC).build();
        var phialData = DataComponentType.<PhialData>builder().persistent(PhialData.CODEC).build();
        var registration = new SubtypeRegistration();
        registration.registerSubtypeInterpreter(Items.STICK, ItemSubtypes.wand(wandData, sceptre));
        registration.registerSubtypeInterpreter(Items.PRISMARINE_CRYSTALS, ItemSubtypes.crystal(crystalData));
        registration.registerSubtypeInterpreter(Items.GLASS_BOTTLE, ItemSubtypes.phial(phialData));
        var helper = new StackHelper(new SubtypeManager(registration.getInterpreters()));

        var wooden = wand(wandData, "wood", "iron", 0);
        var greatwood = wand(wandData, "greatwood", "iron", 0);
        var gold = wand(wandData, "wood", "gold", 0);
        var sceptreStack = wooden.copy(); sceptreStack.set(sceptre, true);
        var charged = wand(wandData, "wood", "iron", 1000);
        charged.setCount(2);
        charged.set(DataComponents.CUSTOM_NAME, Component.literal("cosmetic name"));

        var air = new ItemStack(Items.PRISMARINE_CRYSTALS);
        air.set(crystalData, CrystalEssenceData.fromAspects(new AspectList().add(Aspect.AIR, 1)));
        var fire = new ItemStack(Items.PRISMARINE_CRYSTALS);
        fire.set(crystalData, CrystalEssenceData.fromAspects(new AspectList().add(Aspect.FIRE, 1)));
        var moreAir = air.copy();
        moreAir.set(crystalData, CrystalEssenceData.fromAspects(new AspectList().add(Aspect.AIR, 2)));
        var airStack = air.copyWithCount(32);
        airStack.set(DataComponents.CUSTOM_NAME, Component.literal("another cosmetic name"));
        var emptyCrystal = new ItemStack(Items.PRISMARINE_CRYSTALS);
        var initializedCrystal = emptyCrystal.copy(); initializedCrystal.set(crystalData, CrystalEssenceData.EMPTY);
        var firstAir = new ItemStack(Items.PRISMARINE_CRYSTALS);
        firstAir.set(crystalData, CrystalEssenceData.fromAspects(new AspectList().add(Aspect.AIR, 1).add(Aspect.FIRE, 1)));
        var firstFire = new ItemStack(Items.PRISMARINE_CRYSTALS);
        firstFire.set(crystalData, CrystalEssenceData.fromAspects(new AspectList().add(Aspect.FIRE, 1).add(Aspect.AIR, 1)));

        var airPhial = new ItemStack(Items.GLASS_BOTTLE); airPhial.set(phialData, new PhialData(Aspect.AIR.getTag()));
        var firePhial = new ItemStack(Items.GLASS_BOTTLE); firePhial.set(phialData, new PhialData(Aspect.FIRE.getTag()));
        for (UidContext context : UidContext.values()) {
            check(helper.hasSubtypes(wooden), "JEI recognizes wand subtypes");
            distinct(helper, wooden, greatwood, context, "rod");
            distinct(helper, wooden, gold, context, "cap");
            distinct(helper, wooden, sceptreStack, context, "sceptre");
            equivalent(helper, wooden, charged, context, "wand vis/count/name are not recipe variants");
            distinct(helper, air, fire, context, "crystal aspect");
            distinct(helper, air, moreAir, context, "crystal per-item amount");
            distinct(helper, emptyCrystal, initializedCrystal, context, "uninitialized versus explicitly empty crystal");
            distinct(helper, firstAir, firstFire, context, "first crystal aspect and complete order");
            equivalent(helper, air, airStack, context, "crystal stack count/name");
            distinct(helper, airPhial, firePhial, context, "phial aspect");
            equivalent(helper, airPhial, airPhial.copyWithCount(16), context, "phial stack count");
        }
        // Bootstrap routes System.out through logging; use the original stream for an unambiguous runner verdict.
        try (var output = new java.io.PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.out))) {
            output.println("JEI subtype regression PASS: " + checks + " checks using native JEI equality and UIDs");
        }
    }

    private static ItemStack wand(DataComponentType<WandData> type, String rod, String cap, int vis) {
        var result = new ItemStack(Items.STICK);
        result.set(type, new WandData(rod, cap, Map.of("aer", vis)));
        return result;
    }

    private static void distinct(StackHelper helper, ItemStack first, ItemStack second, UidContext context, String why) {
        check(!helper.isEquivalent(first, second, context), context + ": distinguish " + why);
        check(!helper.getUidForStack(first, context).equals(helper.getUidForStack(second, context)), context + ": distinct UID for " + why);
    }

    private static void equivalent(StackHelper helper, ItemStack first, ItemStack second, UidContext context, String why) {
        check(helper.isEquivalent(first, second, context), context + ": normalize " + why);
        check(helper.getUidForStack(first, context).equals(helper.getUidForStack(second, context)), context + ": stable UID for " + why);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        ++checks;
    }
}
