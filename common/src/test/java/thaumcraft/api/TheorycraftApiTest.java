package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import thaumcraft.api.research.theorycraft.ITheorycraftAid;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;
import thaumcraft.api.research.theorycraft.TheorycraftManager;

/** The theorycraft extension points of the public API. */
class TheorycraftApiTest {
    /** A card of the test. */
    public static final class TestCard extends TheorycraftCard {
        @Override public int getInspirationCost() { return 4; }
        @Override public String getLocalizedName() { return "tc.research_name.TESTCARD"; }
        @Override public String getLocalizedText() { return "tc.research_text.TESTCARD"; }
        @Override public boolean activate(Player player, ResearchTableData data) {
            data.addInspiration(getInspirationCost());
            return true;
        }
    }

    /** An aid of the test. */
    public static final class TestAid implements ITheorycraftAid {
        @Override public Object getAidObject() { return this; }
        @SuppressWarnings("unchecked")
        @Override public Class<TheorycraftCard>[] getCards() { return new Class[]{TestCard.class}; }
    }

    @Test void aCardIsRegisteredOnce() {
        TheorycraftManager.registerCard(TestCard.class);
        assertNotNull(TheorycraftManager.card(TestCard.class.getName()));
        assertEquals(TheorycraftCard.class, TheorycraftManager.card(TestCard.class.getName()).getSuperclass());
        assertNull(TheorycraftManager.card("not.a.Card"));
    }

    @Test void anAidIsRegisteredOnce() {
        TestAid aid = new TestAid();
        TheorycraftManager.registerAid(aid);
        assertNotNull(TheorycraftManager.aid(TestAid.class.getName()));
        assertEquals(1, TheorycraftManager.aids.size());
        assertNotNull(aid.getAidObject());
        assertEquals(1, aid.getCards().length);
    }

    @Test void aCardKeepsItsSeedAcrossTheNativeTag() {
        TestCard card = new TestCard();
        card.setSeed(4711L);
        assertEquals(4711L, card.getSeed());
        CompoundTag tag = card.serialize();
        TestCard read = new TestCard();
        read.deserialize(tag);
        assertEquals(4711L, read.getSeed());
    }

    @Test void aTableSessionCountsItsCategoriesAndInspiration() {
        ResearchTableData data = new ResearchTableData((Player) null, null);
        assertFalse(data.hasTotal("BASICS"));
        data.addTotal("BASICS", 3);
        data.addTotal("BASICS", 4);
        assertTrue(data.hasTotal("BASICS"));
        assertEquals(7, data.getTotal("BASICS"));
        data.addInspiration(5);
        assertEquals(5, data.inspiration);
    }

    @Test void aCardPaysItsInspirationCost() {
        TestCard card = new TestCard();
        ResearchTableData data = new ResearchTableData((Player) null, null);
        assertTrue(card.activate(null, data));
        assertEquals(card.getInspirationCost(), data.inspiration);
        assertNull(card.getRequiredItems());
        assertFalse(card.isAidOnly());
    }
}
