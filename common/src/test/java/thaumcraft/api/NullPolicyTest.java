package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;

/** Only unusable inputs change policy; legitimate absence sentinels remain untouched. */
class NullPolicyTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static void rejects(String argument, Runnable call) {
        var error = assertThrows(IllegalArgumentException.class, call::run);
        assertTrue(error.getMessage().contains(argument), error.getMessage());
    }

    @Test void aspectConstructorsRejectNullTagBeforeBuildingImage() {
        rejects("tag", () -> new Aspect(null, 0, (Aspect[]) null));
        rejects("tag", () -> new Aspect(null, 0, (Aspect[]) null, 1));
        rejects("tag", () -> new Aspect(null, 0, "f", 1));
        rejects("tag", () -> new Aspect(null, 0, null,
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "test"), 1));
    }

    @Test void aspectConstructorsRejectEmptyTagWithoutRegisteringIt() {
        try {
            rejects("tag", () -> new Aspect("", 0, (Aspect[]) null));
            assertFalse(Aspect.aspects.containsKey(""));
            rejects("tag", () -> new Aspect("", 0, null,
                    ResourceLocation.fromNamespaceAndPath("thaumcraft", "test"), 1));
            assertFalse(Aspect.aspects.containsKey(""));
        } finally {
            Aspect.aspects.remove("");
        }
    }

    @Test void listOverloadsRejectNullWithArgumentNameAndPreserveContents() {
        AspectList list = new AspectList().add(Aspect.AIR, 7);
        rejects("other", () -> list.add((AspectList) null));
        rejects("other", () -> list.merge((AspectList) null));
        assertEquals(7, list.getAmount(Aspect.AIR));
    }

    @Test void nullNbtIsRejectedBeforeClearingContentsInBothOverloads() {
        AspectList list = new AspectList().add(Aspect.AIR, 7);
        rejects("tag", () -> list.readFromNBT(null));
        assertEquals(7, list.getAmount(Aspect.AIR));
        rejects("tag", () -> list.readFromNBT(null, "Custom"));
        assertEquals(7, list.getAmount(Aspect.AIR));
    }

    @Test void crucibleConstructorRejectsNullResultAndTagsAtBoundary() {
        rejects("result", () -> new CrucibleRecipe("TEST", null, Items.STONE, new AspectList()));
        rejects("tags", () -> new CrucibleRecipe("TEST", new ItemStack(Items.STONE), Items.STONE, null));
    }

    @Test void crucibleRemovalRejectsNullContentsAtBoundary() {
        CrucibleRecipe recipe = new CrucibleRecipe("TEST", new ItemStack(Items.STONE), Items.STONE,
                new AspectList().add(Aspect.AIR, 1));
        rejects("itags", () -> recipe.removeMatching(null));
    }
}
