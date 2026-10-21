package thaumcraft.golems;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.entities.ai.pech.PechItemGoal;
import thaumcraft.common.entities.ai.pech.PechTradePlayer;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.entities.golems.EntityGolemBase.GoalPriority;

/**
 * The goal priorities of the TC4 goal set that survives in the native tree.
 *
 * <p>TC4 1.7 gave the LOWEST priority number the HIGHEST precedence, and the native 1.21.1 selector reads the
 * number the same way: {@code GoalSelector} hands the flags of a running goal only to a goal whose own number is
 * STRICTLY LOWER ({@code WrappedGoal.canBeReplacedBy}: {@code other.getPriority() < this.getPriority()}), so the
 * goal with the lowest number is the one that runs. A ported TC4 goal therefore keeps the number it publishes in
 * its {@code PRIORITY} constant and the whole selector keeps the original 1.7 order.</p>
 *
 * <p>The numbers below are the ones the TC4 4.2.3.5 oracle registers
 * ({@code thaumcraft/common/entities/golems/EntityGolemBase.java:267-350} and
 * {@code thaumcraft/common/entities/monster/EntityPech.java:146-153}); they are pinned here so that the native
 * goals and the ported Pech goals cannot drift apart. The golem numbers live in
 * {@link EntityGolemBase.GoalPriority}, the one place that publishes them.</p>
 */
class GolemGoalPriorityTest {

    @BeforeAll static void bootstrapNative() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    // ---- the direction of the number ------------------------------------------------------------------------------

    @Test void theNativeMappingKeepsTheOriginalOrder() {
        for (int original = 0; original <= 8; original++) {
            assertEquals(original, EntityGolemBase.nativeGoalPriority(original),
                    "the native selector reads the number like 1.7 did: the lowest number has the highest precedence");
        }
    }

    // ---- the shared goals of every core --------------------------------------------------------------------------

    @Test void theSharedGoalsKeepTheirOriginalNumbers() {
        assertEquals(0, GoalPriority.CREEPER_FLIGHT, "AIAvoidCreeperSwell, the highest 1.7 precedence");
        assertEquals(5, GoalPriority.OPEN_DOOR, "AIOpenDoor");
        assertEquals(6, GoalPriority.RETURN_HOME, "AIReturnHome");
        assertEquals(7, GoalPriority.WATCH_CLOSEST, "EntityAIWatchClosest");
        assertEquals(8, GoalPriority.LOOK_IDLE, "EntityAILookIdle");
    }

    @Test void theOriginalGolemHadNoSwimmingGoal() {
        // 1.7.10 registered EntityAISwimming in the individual mobs (the wolf, the horse, the ocelot, the sheep),
        // never in EntityLiving/EntityLivingBase/EntityCreature/EntityGolem, and the original golem cleared its
        // whole goal list (tasks.taskEntries.clear(), oracle EntityGolemBase:265) before it registered its own goals.
        // The number the swimming goal would have carried is published, but no goal of the golem carries it: the
        // number one belongs to the emptying of the Decanting and of the Alchemy golem.
        assertEquals(1, GoalPriority.SWIM);
        assertEquals(1, GoalPriority.LIQUID_EMPTY);
        assertEquals(1, GoalPriority.ESSENTIA_EMPTY);
    }

    // ---- the combat goals ------------------------------------------------------------------------------------------

    @Test void theCombatGoalsKeepTheirOriginalNumbers() {
        assertEquals(2, GoalPriority.DART_ATTACK, "AIDartAttack, in front of the melee");
        assertEquals(3, GoalPriority.MELEE, "AIGolemAttackOnCollide");
        assertEquals(1, GoalPriority.HURT_BY_TARGET, "AIHurtByTarget, in the target selector");
        assertEquals(2, GoalPriority.NEAREST_TARGET, "AINearestAttackableTarget, in the target selector");
        assertEquals(1, GoalPriority.OLDEST_TARGET, "AINearestButcherTarget, in the target selector");
    }

    // ---- the fluid and the alchemy goals ---------------------------------------------------------------------------

    @Test void theFluidAndAlchemyGoalsKeepTheirOriginalNumbers() {
        assertEquals(1, GoalPriority.LIQUID_EMPTY, "AILiquidEmpty");
        assertEquals(2, GoalPriority.LIQUID_GATHER, "AILiquidGather");
        assertEquals(3, GoalPriority.LIQUID_GOTO, "AILiquidGoto");
        assertEquals(1, GoalPriority.ESSENTIA_EMPTY, "AIEssentiaEmpty");
        assertEquals(2, GoalPriority.ESSENTIA_GATHER, "AIEssentiaGather");
        assertEquals(3, GoalPriority.ESSENTIA_GOTO, "AIEssentiaGoto");
    }

    // ---- the interact goals ----------------------------------------------------------------------------------------

    @Test void theInteractGoalsKeepTheirOriginalNumbers() {
        assertEquals(2, GoalPriority.HARVEST_CROPS, "AIHarvestCrops");
        assertEquals(2, GoalPriority.HARVEST_LOGS, "AIHarvestLogs");
        assertEquals(2, GoalPriority.FISH, "AIFish");
        assertEquals(0, GoalPriority.USE_ITEM, "AIUseItem, together with the home replacement");
    }

    // ---- the inventory goals ---------------------------------------------------------------------------------------

    @Test void theInventoryGoalsKeepTheirOriginalNumbers() {
        assertEquals(0, GoalPriority.HOME_REPLACE, "AIHomeReplace");
        assertEquals(4, GoalPriority.FILL_GOTO, "AIFillGoto");
        assertEquals(3, GoalPriority.EMPTY_GOTO, "AIEmptyGoto");
        assertEquals(3, GoalPriority.SORTING_GOTO, "AISortingGoto");
        assertEquals(4, GoalPriority.HOME_TAKE, "AIHomeTake");
    }

    // ---- the Pech goals ---------------------------------------------------------------------------------------------

    @Test void thePechGoalsKeepTheirOriginalNumbers() {
        assertEquals(1, PechTradePlayer.PRIORITY, "AIPechTradePlayer, registered by the oracle at one");
        assertEquals(3, PechItemGoal.PRIORITY, "AIPechItemEntityGoto");
    }

    // ---- the order the original selector gave -----------------------------------------------------------------------

    @Test void theOriginalOrderSurvivesThePort() {
        // The dart (2) must run before the melee (3) and the melee before the door (5),
        // the return home (6), the watch (7) and the idle look (8), exactly as in 1.7.
        int dart = EntityGolemBase.nativeGoalPriority(GoalPriority.DART_ATTACK);
        int melee = EntityGolemBase.nativeGoalPriority(GoalPriority.MELEE);
        int door = EntityGolemBase.nativeGoalPriority(GoalPriority.OPEN_DOOR);
        int home = EntityGolemBase.nativeGoalPriority(GoalPriority.RETURN_HOME);
        assertTrue(dart < melee, "the dart keeps the higher precedence of the melee");
        assertTrue(melee < door, "the melee keeps the higher precedence of the door");
        assertTrue(door < home, "the door keeps the higher precedence of the return home");
        // The creeper flight outranks every work goal, the Use replacement outranks the take.
        assertTrue(EntityGolemBase.nativeGoalPriority(GoalPriority.CREEPER_FLIGHT) < dart);
        assertTrue(EntityGolemBase.nativeGoalPriority(GoalPriority.USE_ITEM)
                < EntityGolemBase.nativeGoalPriority(GoalPriority.HOME_TAKE));
        // The Pech trade goal outranks the pickup, the pickup outranks the door.
        assertTrue(PechTradePlayer.PRIORITY < PechItemGoal.PRIORITY);
    }
}
