package thaumcraft.golems;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.entities.ai.combat.GolemAttackOnCollide;
import thaumcraft.common.entities.ai.combat.GolemAvoidCreeperSwell;
import thaumcraft.common.entities.ai.combat.GolemDartAttack;
import thaumcraft.common.entities.ai.combat.GolemHurtByTarget;
import thaumcraft.common.entities.ai.combat.GolemNearestAttackableTarget;
import thaumcraft.common.entities.ai.combat.GolemNearestButcherTarget;
import thaumcraft.common.entities.ai.fluid.GolemEssentiaEmpty;
import thaumcraft.common.entities.ai.fluid.GolemEssentiaGather;
import thaumcraft.common.entities.ai.fluid.GolemEssentiaGoto;
import thaumcraft.common.entities.ai.fluid.GolemLiquidEmpty;
import thaumcraft.common.entities.ai.fluid.GolemLiquidGather;
import thaumcraft.common.entities.ai.fluid.GolemLiquidGoto;
import thaumcraft.common.entities.ai.interact.GolemFish;
import thaumcraft.common.entities.ai.interact.GolemHarvestCrops;
import thaumcraft.common.entities.ai.interact.GolemHarvestLogs;
import thaumcraft.common.entities.ai.interact.GolemUseItem;
import thaumcraft.common.entities.ai.inventory.GolemEmptyDrop;
import thaumcraft.common.entities.ai.inventory.GolemEmptyGoto;
import thaumcraft.common.entities.ai.inventory.GolemEmptyPlace;
import thaumcraft.common.entities.ai.inventory.GolemFillGoto;
import thaumcraft.common.entities.ai.inventory.GolemFillTake;
import thaumcraft.common.entities.ai.inventory.GolemHomeDrop;
import thaumcraft.common.entities.ai.inventory.GolemHomePlace;
import thaumcraft.common.entities.ai.inventory.GolemHomeReplace;
import thaumcraft.common.entities.ai.inventory.GolemHomeTake;
import thaumcraft.common.entities.ai.inventory.GolemHomeTakeSorting;
import thaumcraft.common.entities.ai.inventory.GolemItemPickup;
import thaumcraft.common.entities.ai.inventory.GolemSortingGoto;
import thaumcraft.common.entities.ai.inventory.GolemSortingPlace;
import thaumcraft.common.entities.ai.misc.GolemOpenDoor;
import thaumcraft.common.entities.ai.misc.GolemReturnHome;
import thaumcraft.common.entities.golems.EntityGolemBase;

/**
 * The goal priorities of the ported TC4 goal set and their native 1.21.1 mapping.
 *
 * <p>TC4 1.7 gave the LOWEST priority number the HIGHEST precedence, and the native 1.21.1 selector reads the
 * number the same way: {@code GoalSelector} hands the flags of a running goal only to a goal whose own number is
 * STRICTLY LOWER ({@code WrappedGoal.canBeReplacedBy}), so the goal with the lowest number is the one that runs.
 * A ported goal therefore keeps the number it publishes in its {@code PRIORITY} constant and the whole selector
 * keeps the original 1.7 order.</p>
 *
 * <p>The numbers below are the ones the TC4 4.2.3.5 oracle registers
 * ({@code thaumcraft/common/entities/golems/EntityGolemBase.java:265-351}); they are pinned here so that the
 * ported set and the native goals cannot drift apart.</p>
 */
class GolemGoalPriorityTest {

    @BeforeAll static void bootstrapNative() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    // ---- the shared goals of every core ----------------------------------------------------------------------------

    @Test void theSharedGoalsKeepTheirOriginalNumbers() {
        assertEquals(0, GolemAvoidCreeperSwell.PRIORITY, "AIAvoidCreeperSwell, the highest 1.7 precedence");
        assertEquals(5, GolemOpenDoor.PRIORITY, "AIOpenDoor");
        assertEquals(6, GolemReturnHome.PRIORITY, "AIReturnHome");
    }

    // ---- the combat goals ------------------------------------------------------------------------------------------

    @Test void theCombatGoalsKeepTheirOriginalNumbers() {
        assertEquals(2, GolemDartAttack.PRIORITY, "AIDartAttack, in front of the melee");
        assertEquals(3, GolemAttackOnCollide.PRIORITY, "AIGolemAttackOnCollide");
        assertEquals(1, GolemHurtByTarget.PRIORITY, "AIHurtByTarget, in the target selector");
        assertEquals(2, GolemNearestAttackableTarget.PRIORITY, "AINearestAttackableTarget, in the target selector");
        assertEquals(1, GolemNearestButcherTarget.PRIORITY, "AINearestButcherTarget, in the target selector");
    }

    // ---- the fluid and the alchemy goals ---------------------------------------------------------------------------

    @Test void theFluidAndAlchemyGoalsKeepTheirOriginalNumbers() {
        assertEquals(1, GolemLiquidEmpty.PRIORITY, "AILiquidEmpty");
        assertEquals(2, GolemLiquidGather.PRIORITY, "AILiquidGather");
        assertEquals(3, GolemLiquidGoto.PRIORITY, "AILiquidGoto");
        assertEquals(1, GolemEssentiaEmpty.PRIORITY, "AIEssentiaEmpty");
        assertEquals(2, GolemEssentiaGather.PRIORITY, "AIEssentiaGather");
        assertEquals(3, GolemEssentiaGoto.PRIORITY, "AIEssentiaGoto");
    }

    // ---- the interact goals ----------------------------------------------------------------------------------------

    @Test void theInteractGoalsKeepTheirOriginalNumbers() {
        assertEquals(2, GolemHarvestCrops.PRIORITY, "AIHarvestCrops");
        assertEquals(2, GolemHarvestLogs.PRIORITY, "AIHarvestLogs");
        assertEquals(2, GolemFish.PRIORITY, "AIFish");
        assertEquals(0, GolemUseItem.PRIORITY, "AIUseItem, together with the home replacement");
    }

    // ---- the inventory goals ---------------------------------------------------------------------------------------

    @Test void theInventoryGoalsKeepTheirOriginalNumbers() {
        assertEquals(0, GolemHomeReplace.PRIORITY, "AIHomeReplace");
        assertEquals(1, GolemHomePlace.PRIORITY, "AIHomePlace");
        assertEquals(1, GolemEmptyPlace.PRIORITY, "AIEmptyPlace");
        assertEquals(1, GolemSortingPlace.PRIORITY, "AISortingPlace");
        assertEquals(2, GolemHomeDrop.PRIORITY, "AIHomeDrop");
        assertEquals(2, GolemEmptyDrop.PRIORITY, "AIEmptyDrop");
        assertEquals(2, GolemItemPickup.PRIORITY, "AIItemPickup");
        assertEquals(3, GolemEmptyGoto.PRIORITY, "AIEmptyGoto");
        assertEquals(3, GolemFillTake.PRIORITY, "AIFillTake");
        assertEquals(3, GolemSortingGoto.PRIORITY, "AISortingGoto");
        assertEquals(4, GolemFillGoto.PRIORITY, "AIFillGoto");
        assertEquals(4, GolemHomeTake.PRIORITY, "AIHomeTake");
        assertEquals(4, GolemHomeTakeSorting.PRIORITY, "AIHomeTakeSorting");
    }

    // ---- the native mapping ----------------------------------------------------------------------------------------

    @Test void theNativeMappingKeepsTheOriginalOrder() {
        for (int original = 0; original <= 8; original++) {
            assertEquals(original, EntityGolemBase.nativeGoalPriority(original),
                    "the native selector reads the number like 1.7 did: the lowest number has the highest precedence");
        }
        // The dart (2) must run before the melee (3) and the melee before the door (5),
        // the return home (6), the watch (7) and the idle look (8), exactly as in 1.7.
        int dart = EntityGolemBase.nativeGoalPriority(GolemDartAttack.PRIORITY);
        int melee = EntityGolemBase.nativeGoalPriority(GolemAttackOnCollide.PRIORITY);
        int door = EntityGolemBase.nativeGoalPriority(GolemOpenDoor.PRIORITY);
        int home = EntityGolemBase.nativeGoalPriority(GolemReturnHome.PRIORITY);
        assertTrue(dart < melee, "the dart keeps the higher precedence of the melee");
        assertTrue(melee < door, "the melee keeps the higher precedence of the door");
        assertTrue(door < home, "the door keeps the higher precedence of the return home");
    }
}
