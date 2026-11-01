package thaumcraft.client.gui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.crafting.InfusionStackRecipe;

class InfusionBookPreviewTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }

    @Test void centralPreviewAppliesDeclaredWearWithoutDiscardingComponentsOrChangingTheIngredient() {
        ItemStack selected = new ItemStack(Items.DIAMOND_PICKAXE);
        selected.setDamageValue(17);
        selected.set(DataComponents.CUSTOM_NAME, Component.literal("Named ingredient"));
        var recipe = new InfusionStackRecipe("TEST", Ingredient.of(selected), List.of(Ingredient.of(Items.DIAMOND)),
                Map.of("aer", 1), 1, new ItemStack(Items.EMERALD), List.of(), Optional.of(4));

        ItemStack displayed = ThaumcraftRecipeDisplay.infusionCentral(recipe, selected);

        assertEquals(4, displayed.getDamageValue(), "the book shows wear accepted by this recipe, rather than an invalid pristine exemplar");
        assertEquals(selected.get(DataComponents.CUSTOM_NAME), displayed.get(DataComponents.CUSTOM_NAME));
        assertEquals(1, displayed.getCount());
        assertEquals(17, selected.getDamageValue(), "previewing cannot rewrite the shared ingredient stack");
        assertNotSame(selected, displayed);
    }
}
