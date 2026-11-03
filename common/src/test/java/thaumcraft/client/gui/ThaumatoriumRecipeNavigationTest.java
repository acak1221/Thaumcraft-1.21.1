package thaumcraft.client.gui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class ThaumatoriumRecipeNavigationTest {
    @Test void arrowsReachEveryAvailableFormulaUsingItsServerIndex() {
        var navigation = new ThaumatoriumRecipeNavigation();
        navigation.refresh(12, List.of(2, 6, 11)::contains);
        assertEquals(3, navigation.size());
        assertEquals(2, navigation.recipeIndex());
        assertFalse(navigation.hasPrevious());
        assertTrue(navigation.click(136, 28));
        assertEquals(6, navigation.recipeIndex());
        assertTrue(navigation.click(136, 28));
        assertEquals(11, navigation.recipeIndex());
        assertFalse(navigation.hasNext());
        assertTrue(navigation.click(136, 20));
        assertEquals(6, navigation.recipeIndex());
    }

    @Test void everyPixelOfTheOriginalNextArrowAdvances() {
        for (int x = 128; x < 144; x++) for (int y = 24; y < 32; y++) {
            var navigation = new ThaumatoriumRecipeNavigation();
            navigation.refresh(2, index -> true);
            assertTrue(navigation.click(x, y));
            assertEquals(1, navigation.recipeIndex(), "next arrow at " + x + "," + y);
        }
    }

    @Test void arrowEdgesDoNotOverlapTheOutputOrEachOther() {
        var navigation = new ThaumatoriumRecipeNavigation();
        navigation.refresh(3, index -> true);
        assertFalse(navigation.click(127.99, 24));
        assertFalse(navigation.click(144, 24));
        assertFalse(navigation.click(128, 15.99));
        assertFalse(navigation.click(128, 32));
        assertTrue(navigation.click(128, 24));
        assertEquals(1, navigation.page());
        assertTrue(navigation.click(128, 23.99));
        assertEquals(0, navigation.page());
    }

    @Test void repeatedClicksAtTheEndsKeepTheCurrentRecipeVisible() {
        var navigation = new ThaumatoriumRecipeNavigation();
        navigation.refresh(2, index -> true);
        for (int i = 0; i < 10; i++) navigation.click(136, 20);
        assertEquals(0, navigation.recipeIndex());
        for (int i = 0; i < 10; i++) navigation.click(136, 28);
        assertEquals(1, navigation.recipeIndex());
        assertEquals(1, navigation.page());
    }

    @Test void changingCatalystsClampsThePageBeforeResolvingTheNextPreview() {
        var navigation = new ThaumatoriumRecipeNavigation();
        navigation.refresh(8, index -> index < 3);
        navigation.click(136, 28);
        navigation.click(136, 28);
        navigation.refresh(8, index -> index == 7);
        assertEquals(0, navigation.page());
        assertEquals(7, navigation.recipeIndex());
        assertFalse(navigation.hasNext());
        assertFalse(navigation.click(136, 28));
        navigation.refresh(8, index -> false);
        assertEquals(-1, navigation.recipeIndex());
        assertEquals(0, navigation.size());
        navigation.refresh(8, index -> index == 4);
        assertEquals(4, navigation.recipeIndex());
    }
}
