package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.internal.IInternalMethodHandler;

/**
 * The wiring of {@link ThaumcraftApiHelper#getBonusObjectTags(ItemStack, AspectList, Level)}: it asks the installed
 * handler's world-scoped service with the arguments in the handler's order and never hands out null. What the installed
 * handler computes for a real item is checked in a running world (GameTest {@code knowledgeApiBonusObjectTags}).
 */
class BonusObjectTagsApiTest {
    private IInternalMethodHandler installed;

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach void remember() { installed = ThaumcraftApi.internalMethods; }

    @AfterEach void restore() { ThaumcraftApi.internalMethods = installed; }

    @Test void theArgumentsReachTheHandlerInItsOwnOrderAndItsAnswerIsReturned() {
        List<Object> seen = new ArrayList<>();
        AspectList answer = new AspectList().add(Aspect.FIRE, 3);
        ThaumcraftApi.internalMethods = new IInternalMethodHandler() {
            @Override public AspectList getBonusObjectTags(Level level, ItemStack stack, AspectList bonus) {
                seen.add(level);
                seen.add(stack);
                seen.add(bonus);
                return answer;
            }
        };
        ItemStack stack = new ItemStack(Items.DIAMOND);
        AspectList bonus = new AspectList().add(Aspect.WATER, 1);
        assertSame(answer, ThaumcraftApiHelper.getBonusObjectTags(stack, bonus, null));
        assertEquals(3, seen.size());
        assertSame(stack, seen.get(1));
        assertSame(bonus, seen.get(2));
    }

    @Test void aHandlerThatKnowsNothingGivesAnEmptyListNotNull() {
        ThaumcraftApi.internalMethods = new IInternalMethodHandler() {};
        AspectList result = ThaumcraftApiHelper.getBonusObjectTags(new ItemStack(Items.DIAMOND), new AspectList().add(Aspect.WATER, 1), null);
        assertEquals(0, result.size());
        assertNotSame(result, ThaumcraftApiHelper.getBonusObjectTags(null, null, null), "every answer is a new list");
    }

    @Test void theInstalledHandlerWithoutAWorldOrAStackReturnsACopyOfTheBonus() {
        AspectList bonus = new AspectList().add(Aspect.WATER, 2).add(Aspect.FIRE, 1);
        AspectList noWorld = ThaumcraftApiHelper.getBonusObjectTags(new ItemStack(Items.DIAMOND), bonus, null);
        assertNotSame(bonus, noWorld);
        assertEquals(List.of(Aspect.WATER, Aspect.FIRE), List.of(noWorld.getAspects()));
        assertEquals(2, noWorld.getAmount(Aspect.WATER));
        noWorld.add(Aspect.AIR, 5);
        assertEquals(0, bonus.getAmount(Aspect.AIR), "the bonus list is never the one that is returned");

        AspectList noStack = ThaumcraftApiHelper.getBonusObjectTags(ItemStack.EMPTY, bonus, null);
        assertEquals(2, noStack.getAmount(Aspect.WATER));
        assertTrue(ThaumcraftApiHelper.getBonusObjectTags(null, null, null).size() == 0);
    }
}
