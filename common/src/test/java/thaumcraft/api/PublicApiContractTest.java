package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ScanningManager;
import thaumcraft.common.crafting.ArcaneRecipeInput;

/**
 * The published contract of the TC4 API on the native 1.21.1 types.
 *
 * <p>Every assertion of this class is taken from the original 4.2.3.5 behaviour and names the oracle
 * place it comes from. The class only uses the public API of the port, so it also proves that an add-on
 * can rely on these answers.</p>
 */
class PublicApiContractTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    /**
     * Oracle {@code thaumcraft/api/ThaumcraftApiHelper.java:38-70}: the culling drops the cheapest aspect
     * until six aspects are left. The original started the search of the cheapest aspect at
     * {@code Short.MAX_VALUE}, so a list whose weighted amounts are all at least {@code Short.MAX_VALUE}
     * had no candidate at all: the original removed {@code null}, which changes nothing, and looped
     * forever. The native port stops instead of hanging the server.
     */
    @Test void cullingStopsWhenThereIsNoCheaperAspectToDrop() {
        AspectList tags = new AspectList();
        for (Aspect aspect : Aspect.getPrimalAspects()) tags.add(aspect, 40000);
        tags.add(Aspect.VOID, 40000);
        AspectList culled = ThaumcraftApiHelper.cullTags(tags);
        assertEquals(7, culled.size(), "nothing is cheaper than the cap of the original, so nothing is dropped");
        assertEquals(40000, culled.getAmount(Aspect.AIR));
    }

    /**
     * Oracle {@code thaumcraft/api/ThaumcraftApiHelper.java:68-74}: two damageable stacks are the same
     * item whatever their wear is; every other pair is the same item with the same damage value.
     */
    @Test void twoDamageableStacksAreTheSameItemWhateverTheirWearIs() {
        ItemStack fresh = new ItemStack(Items.IRON_PICKAXE);
        ItemStack worn = new ItemStack(Items.IRON_PICKAXE);
        worn.setDamageValue(37);
        assertTrue(ThaumcraftApiHelper.areItemsEqual(fresh, worn), "the original ignores the wear of a damageable item");
        assertTrue(ThaumcraftApiHelper.areItemsEqual(worn, fresh));
    }

    @Test void everyOtherPairOfStacksIsComparedByItemAndDamage() {
        assertTrue(ThaumcraftApiHelper.areItemsEqual(new ItemStack(Items.STONE), new ItemStack(Items.STONE)));
        assertFalse(ThaumcraftApiHelper.areItemsEqual(new ItemStack(Items.STONE), new ItemStack(Items.DIRT)));
        assertFalse(ThaumcraftApiHelper.areItemsEqual(new ItemStack(Items.STONE), null));
        assertTrue(ThaumcraftApiHelper.areItemsEqual(null, null));
    }

    /**
     * Oracle {@code thaumcraft/common/tiles/TileMagicWorkbench.getStackInRowAndColumn} reads the cell
     * {@code row + column * 3} of the grid, with the ROW as the first argument; the original recipes call
     * the helper that way (oracle {@code thaumcraft/common/lib/crafting/ArcaneSceptreRecipe.java:42-46}
     * asks for the cells {@code (1,0)}, {@code (2,1)}, {@code (0,2)}, {@code (1,1)} and {@code (2,0)}).
     */
    @Test void theGridLookupReadsTheCellOfTheRowAndOfTheColumn() {
        ArcaneRecipeInput input = new ArcaneRecipeInput(List.of(
                new ItemStack(Items.STONE), new ItemStack(Items.DIRT), new ItemStack(Items.SAND),
                new ItemStack(Items.GRAVEL), new ItemStack(Items.COBBLESTONE), new ItemStack(Items.CLAY),
                new ItemStack(Items.BRICKS), new ItemStack(Items.OBSIDIAN), new ItemStack(Items.GLASS)));
        assertEquals(Items.STONE, ThaumcraftApiHelper.getStackInRowAndColumn(input, 0, 0).getItem());
        assertEquals(Items.DIRT, ThaumcraftApiHelper.getStackInRowAndColumn(input, 1, 0).getItem());
        assertEquals(Items.GRAVEL, ThaumcraftApiHelper.getStackInRowAndColumn(input, 0, 1).getItem());
        assertEquals(Items.COBBLESTONE, ThaumcraftApiHelper.getStackInRowAndColumn(input, 1, 1).getItem());
        assertEquals(Items.GLASS, ThaumcraftApiHelper.getStackInRowAndColumn(input, 2, 2).getItem());
    }

    @Test void theGridLookupRefusesACellOutsideOfTheGrid() {
        ArcaneRecipeInput input = new ArcaneRecipeInput(List.of(
                new ItemStack(Items.STONE), ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
        assertEquals(Items.STONE, ThaumcraftApiHelper.getStackInRowAndColumn(input, 0, 0).getItem());
        assertTrue(ThaumcraftApiHelper.getStackInRowAndColumn(input, 3, 0).isEmpty());
        assertTrue(ThaumcraftApiHelper.getStackInRowAndColumn(input, 0, -1).isEmpty());
        assertTrue(ThaumcraftApiHelper.getStackInRowAndColumn(input, 0, 3).isEmpty());
    }

    /** The original item lookup answered with null for an unknown name; it never threw. */
    @Test void theItemLookupNeverThrowsForAMissingName() {
        assertNull(ItemApi.getItem(null, 0));
        assertNull(ItemApi.getItem("", 0));
        assertNull(ItemApi.getBlockItem(null));
        assertNull(ItemApi.getBlockItem(""));
        assertNull(ItemApi.getBlockItem("thaumcraft:not_a_block"));
    }

    @Test void theBlockItemLookupAnswersTheBlockOfTheName() {
        ItemStack stone = ItemApi.getBlockItem("minecraft:stone");
        assertNotNull(stone);
        assertEquals(Items.STONE, stone.getItem());
    }

    /**
     * The unknown aspect tag of the original became a key of the map of the aspect list
     * (oracle {@code thaumcraft/api/aspects/AspectList.java}, the {@code add} method has no null check) and
     * the original then crashed in {@code getPrimalAspects}, which calls {@code isPrimal()} on every key.
     * The native port drops the unknown tag instead, and every view of the list stays usable.
     */
    @Test void anUnknownAspectTagIsDroppedAndNeverBreaksTheList() {
        AspectList list = new AspectList();
        list.add(Aspect.AIR, 3).add(Aspect.FIRE, 2);
        CompoundTag tag = new CompoundTag();
        list.writeToNBT(tag);
        CompoundTag unknown = new CompoundTag();
        unknown.putString("key", "not_an_aspect");
        unknown.putInt("amount", 7);
        tag.getList("Aspects", Tag.TAG_COMPOUND).add(unknown);
        AspectList read = new AspectList();
        read.readFromNBT(tag);
        assertEquals(2, read.size(), "the unknown tag is not counted");
        assertEquals(3, read.getAmount(Aspect.AIR));
        assertEquals(2, read.getAmount(Aspect.FIRE));
        assertEquals(2, read.getAspects().length);
        assertNotNull(read.getAspectsSorted());
        assertNotNull(read.getAspectsSortedAmount());
        assertNotNull(read.getPrimalAspects());
    }

    /** The registry of scannable things is public, but the list it hands out is not editable. */
    @Test void theScannableRegistryHandsOutAnImmutableList() {
        List<thaumcraft.api.research.IScanThing> things = ScanningManager.scannableThings();
        assertThrows(UnsupportedOperationException.class, () -> things.add(null));
    }

    /** The public answer of the recipe key is the caller's own copy. */
    @Test void theCraftingRecipeKeyOfAMissingStackIsNull() {
        assertNull(ThaumcraftApi.getCraftingRecipeKey(null, new ItemStack(Items.STONE)));
        assertNull(ThaumcraftApi.getCraftingRecipeKey(null, null));
    }

    /** The public aspect list of every aspect is the original one. */
    @Test void everyAspectOfThePortIsPublished() {
        assertEquals(48, Aspect.aspects.size());
        assertEquals(48, ThaumcraftApiHelper.getAllAspects(1).size());
        assertSame(Aspect.AIR, Aspect.getAspect("aer"));
        assertNull(Aspect.getAspect("not_an_aspect"));
    }
}
