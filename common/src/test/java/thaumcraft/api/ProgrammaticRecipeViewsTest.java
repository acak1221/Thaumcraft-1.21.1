package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.crafting.ProgrammaticRecipes;

/** Recipes an add-on registers through ThaumcraftApi are offered to EMI and JEI through stable native views. */
class ProgrammaticRecipeViewsTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void arcaneAndInfusionRegistrationsHaveStableViews() {
        var arcane = ThaumcraftApi.addArcaneCraftingRecipe("VIEWS", new ItemStack(Items.DIAMOND), new AspectList().add(Aspect.AIR, 1),
                "A", 'A', new ItemStack(Items.COAL));
        var infusion = ThaumcraftApi.addInfusionCraftingRecipe("VIEWS", new ItemStack(Items.EMERALD), 1, new AspectList().add(Aspect.GREED, 4),
                new ItemStack(Items.DIAMOND), new ItemStack[]{new ItemStack(Items.GOLD_INGOT)});
        try {
            int index = ThaumcraftApi.getProgrammaticCraftingRecipes().indexOf(arcane);
            var view = ProgrammaticRecipes.arcaneViews().stream().filter(holder -> holder.value() == arcane.nativeRecipe()).findFirst().orElseThrow();
            assertEquals("thaumcraft:api/arcane/" + index, view.id().toString(), "the id the arcane workbench gives the same match");
            assertSame(view, ProgrammaticRecipes.arcaneViews().stream().filter(holder -> holder.value() == arcane.nativeRecipe()).findFirst().orElseThrow(),
                    "a reload sees the same holder");
            assertTrue(ProgrammaticRecipes.infusionViews().stream().anyMatch(holder -> holder.value() == infusion.nativeRecipe()), "the infusion view");
        } finally {
            ThaumcraftApi.unregisterCraftingRecipe(arcane);
            ThaumcraftApi.unregisterCraftingRecipe(infusion);
        }
        assertTrue(ProgrammaticRecipes.arcaneViews().stream().noneMatch(holder -> holder.value() == arcane.nativeRecipe()), "removed with its registration");
    }
}
