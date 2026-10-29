package thaumic.tinkerer.basicitems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumic.tinkerer.common.core.helper.ExperienceHelper;
import thaumic.tinkerer.common.core.helper.ItemNBTHelper;
import thaumic.tinkerer.common.core.helper.MiscHelper;
import thaumic.tinkerer.common.item.BasicItemComponents.ShareData;
import thaumic.tinkerer.common.item.CleansingLogic;
import thaumic.tinkerer.common.item.ItemCleansingTalisman;
import thaumic.tinkerer.common.item.ItemInfusedInkwell;
import thaumic.tinkerer.common.item.ItemRevealingHelm;
import thaumic.tinkerer.common.item.ItemSpellCloth;
import thaumic.tinkerer.common.item.ItemXPTalisman;

/**
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 *
 * <p>The plain rules of the BasicItems group: the numbers of the items, the experience curve of the experience helper, the
 * decisions of the Talisman of Remedium, the codec of the tome's data and the small helpers that other groups use. Nothing
 * here needs a world or a registered item.</p>
 */
class BasicItemsLogicTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    // ---- the numbers of the original ------------------------------------------------------------------------------

    @Test
    void theNumbersAreThoseOfTheOriginalLibFeatures() {
        assertEquals(35, ItemSpellCloth.USES, "LibFeatures.SPELL_CLOTH_USES");
        assertEquals(100, ItemCleansingTalisman.USES, "LibFeatures.CLEANSING_TALISMAN_USES");
        assertEquals(3, ItemXPTalisman.RANGE, "LibFeatures.XP_TALISMAN_RANGE");
        assertEquals(1500, ItemXPTalisman.MAX_XP, "LibFeatures.XP_TALISMAN_MAX_XP");
        assertEquals(10, ItemXPTalisman.BOTTLE_COST, "LibFeatures.XP_TALISMAN_ENCHANTING_BOTTLE_COST");
        assertEquals(800, ItemInfusedInkwell.INK, "setMaxDamage(800)");
        assertEquals(500, ItemRevealingHelm.DURABILITY, "setMaxDamage(500)");
        assertEquals(5, ItemRevealingHelm.VIS_DISCOUNT, "getVisDiscount");
    }

    @Test
    void theFiveWarpEffectsThatWearTheTalismanTenTimesAsMuch() {
        assertEquals(Set.of("thaumcraft:blurred_vision", "thaumcraft:death_gaze", "thaumcraft:infectious_vis_exhaust",
                "thaumcraft:sun_scorned", "thaumcraft:unnatural_hunger"), ItemCleansingTalisman.WARP_EFFECTS);
    }

    // ---- experience ------------------------------------------------------------------------------------------------

    @Test
    void theExperienceOfALevelFollowsTheCurveOfTheGame() {
        // The points that lead from level 0: 2L+7 per level up to 15, 5L-38 up to 30, 9L-158 beyond.
        int[][] known = { { 0, 0 }, { 1, 7 }, { 2, 16 }, { 5, 55 }, { 10, 160 }, { 15, 315 }, { 16, 352 }, { 17, 394 },
                { 20, 550 }, { 30, 1395 }, { 31, 1507 }, { 32, 1628 }, { 50, 5345 } };
        for (int[] pair : known) {
            assertEquals(pair[1], ExperienceHelper.getExperienceForLevel(pair[0]), "level " + pair[0]);
        }
        assertEquals(0, ExperienceHelper.getExperienceForLevel(-3), "a negative level has no points");
    }

    @Test
    void theStepsBetweenTheLevelsAreTheSizeOfTheExperienceBar() {
        for (int level = 0; level < 100; level++) {
            // Player.getXpNeededForNextLevel of the game
            int bar = level >= 30 ? 112 + (level - 30) * 9 : level >= 15 ? 37 + (level - 15) * 5 : 7 + level * 2;
            assertEquals(bar, ExperienceHelper.getExperienceForLevel(level + 1) - ExperienceHelper.getExperienceForLevel(level),
                    "bar of level " + level);
        }
    }

    @Test
    void theLevelOfAnAmountOfExperienceInvertsTheCurve() {
        for (int level = 0; level < 80; level++) {
            int points = ExperienceHelper.getExperienceForLevel(level);
            assertEquals(level, ExperienceHelper.getLevelForExperience(points), "exactly level " + level);
            assertEquals(level, ExperienceHelper.getLevelForExperience(points + 1), "just above level " + level);
            if (level > 0) {
                assertEquals(level - 1, ExperienceHelper.getLevelForExperience(points - 1), "just below level " + level);
            }
        }
        assertEquals(0, ExperienceHelper.getLevelForExperience(0));
        assertEquals(0, ExperienceHelper.getLevelForExperience(6));
        assertEquals(1, ExperienceHelper.getLevelForExperience(7));
    }

    // ---- the Talisman of Remedium ---------------------------------------------------------------------------------

    @Test
    void aFirstHarmfulEffectIsRemovedAtACost() {
        CleansingLogic.State state = new CleansingLogic.State();
        CleansingLogic.Plan plan = CleansingLogic.plan(state, false, List.of("minecraft:poison"));
        assertEquals(Set.of("minecraft:poison"), plan.toRemove());
        assertEquals("minecraft:poison", plan.removed(), "the removal costs durability");
        assertFalse(plan.permanentRemoved());
        assertEquals("minecraft:poison", state.lastRemoved);
        assertTrue(state.permanent.isEmpty());
    }

    @Test
    void onlyOneNewEffectIsRemovedPerPassAndTheOthersWaitTheirTurn() {
        CleansingLogic.State state = new CleansingLogic.State();
        List<String> harmful = List.of("minecraft:poison", "minecraft:slowness", "minecraft:hunger");
        CleansingLogic.Plan first = CleansingLogic.plan(state, false, harmful);
        assertEquals(Set.of("minecraft:poison"), first.toRemove(), "the first in the list goes first");
        assertEquals("minecraft:poison", first.removed());

        // The poison is gone; the next pass takes the next effect.
        CleansingLogic.Plan second = CleansingLogic.plan(state, false, List.of("minecraft:slowness", "minecraft:hunger"));
        assertEquals(Set.of("minecraft:slowness"), second.toRemove());
        assertEquals("minecraft:slowness", second.removed());
        CleansingLogic.Plan third = CleansingLogic.plan(state, false, List.of("minecraft:hunger"));
        assertEquals("minecraft:hunger", third.removed());
        CleansingLogic.Plan fourth = CleansingLogic.plan(state, false, List.of());
        assertEquals("", fourth.removed(), "nothing to remove costs nothing");
        assertTrue(fourth.toRemove().isEmpty());
        assertEquals("", state.lastRemoved);
    }

    @Test
    void anEffectThatComesBackRightAfterItsRemovalBecomesPermanentAndIsRemovedForFree() {
        CleansingLogic.State state = new CleansingLogic.State();
        CleansingLogic.plan(state, false, List.of("minecraft:poison"));

        // The same effect again on the next pass: a beacon, a warp effect that is applied again and again.
        CleansingLogic.Plan again = CleansingLogic.plan(state, false, List.of("minecraft:poison"));
        assertEquals(Set.of("minecraft:poison"), again.toRemove(), "it is removed again");
        assertEquals("", again.removed(), "but for nothing");
        assertEquals(Set.of("minecraft:poison"), state.permanent);
        assertFalse(again.permanentRemoved());

        CleansingLogic.Plan free = CleansingLogic.plan(state, false, List.of("minecraft:poison"));
        assertEquals("", free.removed());
        assertEquals(Set.of("minecraft:poison"), state.permanent, "it stays permanent while it keeps coming");

        // A different effect meanwhile is still removed at a cost.
        CleansingLogic.Plan other = CleansingLogic.plan(state, false, List.of("minecraft:poison", "minecraft:weakness"));
        assertEquals(Set.of("minecraft:poison", "minecraft:weakness"), other.toRemove());
        assertEquals("minecraft:weakness", other.removed());

        // The effect stops coming: it is forgotten, and the pass says so (it plays the sound).
        CleansingLogic.Plan gone = CleansingLogic.plan(state, false, List.of());
        assertTrue(gone.permanentRemoved(), "the permanent effect is no longer there");
        assertTrue(state.permanent.isEmpty());
        // When it comes again it is a fresh effect with a cost.
        CleansingLogic.Plan fresh = CleansingLogic.plan(state, false, List.of("minecraft:poison"));
        assertEquals("minecraft:poison", fresh.removed());
    }

    @Test
    void burningIsTreatedLikeAnEffectAndGoesBeforeThePotions() {
        CleansingLogic.State state = new CleansingLogic.State();
        CleansingLogic.Plan plan = CleansingLogic.plan(state, true, List.of("minecraft:poison"));
        assertEquals(Set.of(CleansingLogic.BURNING), plan.toRemove(), "the potion waits");
        assertEquals(CleansingLogic.BURNING, plan.removed());

        CleansingLogic.Plan next = CleansingLogic.plan(state, true, List.of("minecraft:poison"));
        assertEquals(Set.of(CleansingLogic.BURNING, "minecraft:poison"), next.toRemove(),
                "burning is on fire permanently now, the potion takes its turn");
        assertEquals("minecraft:poison", next.removed());
        assertTrue(state.permanent.contains(CleansingLogic.BURNING));
    }

    // ---- the Tome of Knowledge Sharing -----------------------------------------------------------------------------

    private static ShareData sampleTome() {
        return new ShareData("Steve", UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"),
                List.of("GOGGLES", "THAUMIUM", "SPELL_CLOTH"), List.of("aer", "ignis", "metallum"));
    }

    @Test
    void theTomeDataSurvivesItsCodecs() {
        ShareData data = sampleTome();

        JsonElement json = ShareData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
        assertEquals(data, ShareData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(), "JSON");

        Tag nbt = ShareData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        assertEquals(data, ShareData.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow(), "NBT");
        CompoundTag tag = (CompoundTag) nbt;
        assertEquals("Steve", tag.getString("player"));
        assertEquals(3, tag.getList("research", Tag.TAG_STRING).size());
        assertEquals(3, tag.getList("aspects", Tag.TAG_STRING).size());

        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        ShareData.STREAM_CODEC.encode(buffer, data);
        assertEquals(data, ShareData.STREAM_CODEC.decode(buffer), "network");
        assertEquals(0, buffer.readableBytes(), "the codec reads what it wrote");
    }

    @Test
    void theTomeDataIsAnImmutableSnapshot() {
        java.util.ArrayList<String> research = new java.util.ArrayList<>(List.of("A"));
        ShareData data = new ShareData("Alex", UUID.randomUUID(), research, List.of());
        research.add("B");
        assertEquals(List.of("A"), data.research(), "later changes of the list do not reach the tome");
        try {
            data.research().add("C");
            throw new AssertionError("the list of a tome cannot be changed");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    // ---- the stack tag helper --------------------------------------------------------------------------------------

    @Test
    void theNbtHelperReadsDefaultsAndKeepsWhatIsWritten() {
        ItemStack stack = new ItemStack(Items.STICK);
        assertFalse(ItemNBTHelper.detectNBT(stack), "a fresh stack has no tag");
        assertEquals(7, ItemNBTHelper.getInt(stack, "n", 7), "the default of a missing key");
        assertEquals("d", ItemNBTHelper.getString(stack, "s", "d"));
        assertTrue(ItemNBTHelper.getBoolean(stack, "b", true));
        assertNull(ItemNBTHelper.getCompound(stack, "c", true), "nullify on fail");
        assertTrue(ItemNBTHelper.getCompound(stack, "c", false).isEmpty(), "an empty tag otherwise");

        ItemNBTHelper.setInt(stack, "n", 42);
        ItemNBTHelper.setString(stack, "s", "text");
        ItemNBTHelper.setBoolean(stack, "b", false);
        ItemNBTHelper.setLong(stack, "l", 1L << 40);
        ItemNBTHelper.setDouble(stack, "d", 2.5);
        ItemNBTHelper.setFloat(stack, "f", 1.5F);
        ItemNBTHelper.setShort(stack, "sh", (short) 9);
        ItemNBTHelper.setByte(stack, "by", (byte) 3);
        assertTrue(ItemNBTHelper.detectNBT(stack));
        assertEquals(42, ItemNBTHelper.getInt(stack, "n", 0));
        assertEquals("text", ItemNBTHelper.getString(stack, "s", ""));
        assertFalse(ItemNBTHelper.getBoolean(stack, "b", true));
        assertEquals(1L << 40, ItemNBTHelper.getLong(stack, "l", 0));
        assertEquals(2.5, ItemNBTHelper.getDouble(stack, "d", 0));
        assertEquals(1.5F, ItemNBTHelper.getFloat(stack, "f", 0));
        assertEquals((short) 9, ItemNBTHelper.getShort(stack, "sh", (short) 0));
        assertEquals((byte) 3, ItemNBTHelper.getByte(stack, "by", (byte) 0));

        CompoundTag inner = new CompoundTag();
        inner.putInt("x", 5);
        ItemNBTHelper.setCompound(stack, "c", inner);
        inner.putInt("x", 6);
        assertEquals(5, ItemNBTHelper.getCompound(stack, "c", true).getInt("x"), "the compound was copied in");

        ItemNBTHelper.removeTag(stack, "n");
        assertFalse(ItemNBTHelper.verifyExistance(stack, "n"));
        assertEquals("text", ItemNBTHelper.getString(stack, "s", ""), "the other keys stay");
    }

    @Test
    void theNbtOfAStackIsACopyAndACopiedStackIsIndependent() {
        ItemStack stack = new ItemStack(Items.STICK);
        ItemNBTHelper.setInt(stack, "n", 1);

        CompoundTag copy = ItemNBTHelper.getNBT(stack);
        copy.putInt("n", 99);
        assertEquals(1, ItemNBTHelper.getInt(stack, "n", 0), "getNBT does not hand out the live tag");

        ItemStack other = stack.copy();
        ItemNBTHelper.setInt(other, "n", 2);
        assertEquals(1, ItemNBTHelper.getInt(stack, "n", 0), "a copy of the stack does not share its tag");
        assertEquals(2, ItemNBTHelper.getInt(other, "n", 0));

        ItemNBTHelper.update(stack, tag -> tag.putInt("m", 8));
        assertEquals(8, ItemNBTHelper.getInt(stack, "m", 0), "update keeps the change");

        ItemStack empty = new ItemStack(Items.STICK);
        ItemNBTHelper.initNBT(empty);
        assertTrue(ItemNBTHelper.detectNBT(empty));
        assertNotSame(ItemNBTHelper.getNBT(empty), ItemNBTHelper.getNBT(empty));
    }

    // ---- the odds and ends of MiscHelper ---------------------------------------------------------------------------

    @Test
    void theDistanceIsEuclidean() {
        assertEquals(13.0, MiscHelper.pointDistanceSpace(0, 0, 0, 3, 4, 12), 1e-9);
        assertEquals(0.0, MiscHelper.pointDistanceSpace(1, 2, 3, 1, 2, 3), 1e-9);
        assertEquals(5.0, MiscHelper.pointDistanceSpace(-1, 0, 0, 3, 3, 0), 1e-9);
    }

    @Test
    void multiplyingAnAspectListTruncatesEachAmountAndLeavesTheOriginalAlone() {
        AspectList list = new AspectList().add(Aspect.AIR, 10).add(Aspect.FIRE, 7);
        AspectList half = MiscHelper.multiplyAspectList(list, 0.5);
        assertEquals(5, half.getAmount(Aspect.AIR));
        assertEquals(3, half.getAmount(Aspect.FIRE), "7 / 2 truncates to 3");
        assertEquals(10, list.getAmount(Aspect.AIR), "the original is unchanged");

        AspectList same = MiscHelper.multiplyAspectList(list, 1);
        assertNotSame(list, same, "a factor of 1 is still a copy");
        assertEquals(10, same.getAmount(Aspect.AIR));
        assertEquals(7, same.getAmount(Aspect.FIRE));
    }
}
