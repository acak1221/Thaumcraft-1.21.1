package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.baubles.BaubleType;
import thaumcraft.api.baubles.BaublesApi;
import thaumcraft.api.baubles.IBauble;
import thaumcraft.api.wands.IFocusPouch;
import thaumcraft.common.items.wands.ItemFocusPouch;
import thaumcraft.common.lib.baubles.PlayerAccessories;
import thaumcraft.common.lib.baubles.PlayerAccessoryInventory;

/**
 * The shape and the world-free answers of three additions for add-ons that wear and carry things: the hand-equip opt-out
 * {@link IBauble#equipsOnUse}, the worn-accessories view {@link BaublesApi#getWorn} and the focus pouch interface
 * {@link IFocusPouch}. What they do to a real player, wand and pouch is checked by the GameTests of
 * {@code BaublesApiRuntimeChecks} and {@code AddonPouchRuntimeChecks}.
 */
class BaublesAndPouchApiTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void equipsOnUseIsADefaultMethodThatAnswersTrueSoExistingBaublesAreUnchanged() throws Exception {
        Method method = IBauble.class.getMethod("equipsOnUse", ItemStack.class, Player.class);
        assertTrue(method.isDefault(), "a default method: no existing implementor has to change");
        assertEquals(boolean.class, method.getReturnType());
        IBauble plain = stack -> BaubleType.RING;
        assertTrue(plain.equipsOnUse(new ItemStack(Items.STICK), null), "true without looking at its arguments");
        assertTrue(plain.equipsOnUse(ItemStack.EMPTY, null));
        // The one abstract method is still the only one: a lambda above would not compile otherwise.
        long abstractMethods = Arrays.stream(IBauble.class.getMethods()).filter(m -> Modifier.isAbstract(m.getModifiers())).count();
        assertEquals(1, abstractMethods);
    }

    @Test void anItemMayOptOutAndTheOptOutDoesNotTouchTheOtherAnswers() {
        IBauble optOut = new IBauble() {
            @Override public BaubleType getBaubleType(ItemStack stack) { return BaubleType.AMULET; }
            @Override public boolean equipsOnUse(ItemStack stack, Player player) { return false; }
        };
        assertFalse(optOut.equipsOnUse(new ItemStack(Items.STICK), null));
        assertTrue(optOut.canEquip(new ItemStack(Items.STICK), null), "the menu's canEquip is its own answer");
        assertTrue(optOut.canUnequip(new ItemStack(Items.STICK), null));
        assertEquals(BaubleType.AMULET, optOut.getBaubleType(ItemStack.EMPTY));
    }

    @Test void theFocusPouchInterfaceIsTheOriginalPairOfMethodsAndTheNativePouchIsOne() throws Exception {
        List<String> abstractMethods = Arrays.stream(IFocusPouch.class.getMethods()).filter(m -> Modifier.isAbstract(m.getModifiers()))
                .map(m -> m.getName() + Arrays.toString(m.getParameterTypes())).sorted().collect(Collectors.toList());
        assertEquals(List.of("getInventory[class net.minecraft.world.item.ItemStack]",
                "setInventory[class net.minecraft.world.item.ItemStack, class [Lnet.minecraft.world.item.ItemStack;]"), abstractMethods);
        assertEquals(ItemStack[].class, IFocusPouch.class.getMethod("getInventory", ItemStack.class).getReturnType());
        assertEquals(void.class, IFocusPouch.class.getMethod("setInventory", ItemStack.class, ItemStack[].class).getReturnType());
        assertTrue(IFocusPouch.class.isAssignableFrom(ItemFocusPouch.class), "the native pouch is offered through the interface like an add-on one");
        assertEquals(128, IFocusPouch.MAX_SELECTABLE_CELLS);
        assertTrue(ItemFocusPouch.SIZE <= IFocusPouch.MAX_SELECTABLE_CELLS);
        // The native methods keep their public signatures; they now override the interface's.
        assertTrue(Modifier.isPublic(ItemFocusPouch.class.getMethod("getInventory", ItemStack.class).getModifiers()));
        assertTrue(Modifier.isPublic(ItemFocusPouch.class.getMethod("setInventory", ItemStack.class, ItemStack[].class).getModifiers()));
    }

    @Test void getWornIsAStaticUtilityOfTheBaublesPackageThatCannotBeInstantiated() throws Exception {
        Method method = BaublesApi.class.getMethod("getWorn", LivingEntity.class);
        assertTrue(Modifier.isStatic(method.getModifiers()));
        assertEquals(List.class, method.getReturnType());
        assertTrue(Modifier.isFinal(BaublesApi.class.getModifiers()));
        var constructor = BaublesApi.class.getDeclaredConstructors()[0];
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
    }

    @Test void nothingAndNoPlayerWearNothing() {
        assertTrue(BaublesApi.getWorn(null).isEmpty(), "null has no accessory slots");
        assertThrows(UnsupportedOperationException.class, () -> BaublesApi.getWorn(null).add(ItemStack.EMPTY), "and the empty answer is read-only too");
    }

    @Test void anEmptyOrUnreadableAccessoryInventoryListsNothingAndTheListIsReadOnly() {
        PlayerAccessoryInventory empty = new PlayerAccessoryInventory(null);
        List<ItemStack> none = PlayerAccessories.worn(empty);
        assertTrue(none.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> none.add(new ItemStack(Items.STICK)));

        CompoundTag malformed = new CompoundTag();
        ListTag items = new ListTag();
        items.add(StringTag.valueOf("unreadable accessory payload"));
        malformed.put("Items", items);
        PlayerAccessoryInventory unreadable = new PlayerAccessoryInventory(null);
        unreadable.load(malformed, null);
        assertTrue(unreadable.isRejected());
        List<ItemStack> rejected = PlayerAccessories.worn(unreadable);
        assertNotNull(rejected);
        assertTrue(rejected.isEmpty(), "a retained unreadable attachment holds nothing that is worn");
        assertThrows(UnsupportedOperationException.class, () -> rejected.add(new ItemStack(Items.STICK)));
    }
}
