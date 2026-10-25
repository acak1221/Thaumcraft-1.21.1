package thaumcraft.client.lib;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.items.wands.WandData;

/** Observes the production paint path's draws, not Java source or duplicate drawing arithmetic. */
class CastingWandHudTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void installedFocusDrawsItsItemAndActualCooldownWhileEmptyFocusDrawsNeither() {
        var focus = new ItemStack(Items.STICK);
        var draw = new Recording();
        CastingWandHud.paint(frame(focus, ItemStack.EMPTY, 0, 1290, false), draw);
        assertEquals(List.of(focus), draw.items);
        assertTrue(draw.labels.stream().anyMatch(t -> t.centered && t.x == 0 && t.text.replace(',', '.').equals("1.3s")));
        assertEquals(0, draw.depth);
        assertFalse(draw.blending);

        var empty = new Recording();
        CastingWandHud.paint(frame(ItemStack.EMPTY, ItemStack.EMPTY, 0, 1290, false), empty);
        assertTrue(empty.items.isEmpty());
        assertTrue(empty.labels.isEmpty(), "no focus also suppresses a stale dispatcher deadline");
    }

    @Test void selectedTradeBlockReplacesFocusIconAndDrawsMaterialAmountWithOutline() {
        var focus = new ItemStack(Items.STICK);
        var picked = new ItemStack(Items.STONE);
        var draw = new Recording();
        CastingWandHud.paint(frame(focus, picked, 137, 1290, false), draw);
        assertEquals(List.of(picked), draw.items);
        assertEquals(5, draw.labels.size(), "four black neighbours and the white material amount");
        assertEquals(4, draw.labels.stream().filter(t -> t.color == 0xFF000000 && t.text.equals("137")).count());
        assertEquals(1, draw.labels.stream().filter(t -> t.color == 0xFFFFFFFF && t.text.equals("137")).count());
        assertFalse(draw.labels.stream().anyMatch(t -> t.text.endsWith("s")), "TC4 trade readout replaces the cooldown readout");
    }

    @Test void shiftEmitsFractionalDiscountedPriceBesideStoredWholeVis() {
        var draw = new Recording();
        CastingWandHud.paint(frame(new ItemStack(Items.STICK), ItemStack.EMPTY, 0, 0, true), draw);
        assertEquals(6, draw.labels.stream().filter(t -> t.centered && t.x == -32).count());
        assertTrue(draw.labels.stream().anyMatch(t -> t.centered && t.x == 8 && t.text.replace(',', '.').equals("6.25")));
        assertEquals(1, draw.labels.stream().filter(t -> t.centered && t.x == 8).count(), "only an aspect in the focus cost has a price");
        assertEquals(1, draw.blits.stream().filter(b -> b.u == 136).count(), "one priced-aspect marker");

        var ordinary = new Recording();
        CastingWandHud.paint(frame(new ItemStack(Items.STICK), ItemStack.EMPTY, 0, 0, false), ordinary);
        assertTrue(ordinary.labels.isEmpty(), "numeric prices and stocks are shift-only");
    }

    @Test void actualDialDrawsEmptyAndFullBarsAndBalancedPoses() {
        var draw = new Recording();
        CastingWandHud.paint(frame(ItemStack.EMPTY, ItemStack.EMPTY, 0, 0, false), draw);
        var bars = draw.blits.stream().filter(b -> b.u == 104).toList();
        assertEquals(6, bars.size());
        assertEquals(0, bars.get(0).height, "empty aer");
        assertEquals(30, bars.get(1).height, "full terra");
        assertEquals(35, bars.get(0).y);
        assertEquals(5, bars.get(1).y);
        assertEquals(0, draw.depth);
    }

    @Test void rendererFailureUnwindsEveryPoseAndColorState() {
        var draw = new Recording() {
            @Override public void item(ItemStack stack, int x, int y) { throw new IllegalStateException("renderer failed"); }
        };
        assertThrows(IllegalStateException.class, () -> CastingWandHud.paint(
                frame(new ItemStack(Items.STICK), ItemStack.EMPTY, 0, 0, false), draw));
        assertEquals(0, draw.depth);
        assertFalse(draw.blending);
        assertEquals(List.of(1F, 1F, 1F, 1F), draw.lastColor);
    }

    @Test void visHistoryBelongsToHandSlotMaterialAndClientOwner() {
        var samples = new CastingWandHud.Samples();
        Object level = new Object(), player = new Object();
        samples.owner(level, player);
        var main = new WandData("wood", "iron", Map.of("aer", 1000));
        var mainPaid = main.withAmount("aer", 900);
        var off = main.withAmount("aer", 400);
        assertSame(main, samples.previous(InteractionHand.MAIN_HAND, 0, main, false, 10));
        assertSame(main, samples.previous(InteractionHand.MAIN_HAND, 0, mainPaid, false, 11));
        assertSame(off, samples.previous(InteractionHand.OFF_HAND, 40, off, false, 12));
        assertSame(mainPaid, samples.previous(InteractionHand.MAIN_HAND, 1, mainPaid, false, 13));
        var otherMaterial = new WandData("greatwood", "gold", Map.of("aer", 1500));
        assertSame(otherMaterial, samples.previous(InteractionHand.MAIN_HAND, 0, otherMaterial, false, 14));
        assertSame(mainPaid, samples.previous(InteractionHand.MAIN_HAND, 0, mainPaid, false, 15));
        assertSame(mainPaid, samples.previous(InteractionHand.MAIN_HAND, 0, mainPaid, true, 16));
        assertSame(main, samples.previous(InteractionHand.MAIN_HAND, 0, main, true, 1017));
        samples.owner(level, new Object());
        assertSame(off, samples.previous(InteractionHand.MAIN_HAND, 0, off, true, 1018));
        samples.owner(new Object(), player);
        assertSame(main, samples.previous(InteractionHand.MAIN_HAND, 0, main, true, 1019));
    }

    @Test void snapshotBoundarySuppressesArrowBeforeSubsequentChangesLikeOriginalHud() {
        var samples = new CastingWandHud.Samples();
        var initial = new WandData("wood", "iron", Map.of("aer", 1000));
        samples.previous(InteractionHand.MAIN_HAND, 0, initial, false, 10);
        var paid = initial.withAmount("aer", 900);
        var beforeBoundary = new Recording();
        CastingWandHud.paint(historyFrame(paid, samples.previous(InteractionHand.MAIN_HAND, 0, paid, false, 1009)), beforeBoundary);
        assertEquals(1, beforeBoundary.blits.stream().filter(b -> b.u == 128).count(), "loss arrow before the sample expires");
        var boundary = new Recording();
        CastingWandHud.paint(historyFrame(paid, samples.previous(InteractionHand.MAIN_HAND, 0, paid, false, 1010)), boundary);
        assertEquals(0, boundary.blits.stream().filter(b -> b.u == 120 || b.u == 128).count(),
                "original ClientTickEventsFML updates oldvals before it draws arrows on the sampling tick");
        var refilled = paid.withAmount("aer", 950);
        var afterBoundary = new Recording();
        CastingWandHud.paint(historyFrame(refilled, samples.previous(InteractionHand.MAIN_HAND, 0, refilled, false, 1011)), afterBoundary);
        assertEquals(1, afterBoundary.blits.stream().filter(b -> b.u == 120).count(), "gain compares to the new snapshot");
    }

    private static CastingWandHud.Frame historyFrame(WandData data, WandData previous) {
        return new CastingWandHud.Frame(data, previous, 2500, null, Map.of(),
                ItemStack.EMPTY, ItemStack.EMPTY, 0, 0, false, false, 240);
    }

    private static CastingWandHud.Frame frame(ItemStack focus, ItemStack picked, int count, long cooldown, boolean sneaking) {
        var data = new WandData("wood", "iron", Map.of("aer", 0, "terra", 2500));
        return new CastingWandHud.Frame(data, data, 2500, new AspectList().add(Aspect.AIR, 1000),
                Map.of("aer", 6.25F), focus, picked, count, cooldown, sneaking, false, 240);
    }

    private record Label(String text, int x, int y, int color, boolean centered) {}
    private record Blit(int x, int y, int u, int width, int height) {}
    private static class Recording implements CastingWandHud.Draw {
        final List<ItemStack> items = new ArrayList<>();
        final List<Label> labels = new ArrayList<>();
        final List<Blit> blits = new ArrayList<>();
        List<Float> lastColor = List.of();
        int depth;
        boolean blending;
        public void push() { depth++; }
        public void pop() { assertTrue(depth > 0); depth--; }
        public void translate(float x, float y, float z) {}
        public void scale(float x, float y, float z) {}
        public void rotate(float degrees) {}
        public void blend(boolean enabled) { blending = enabled; }
        public void color(float r, float g, float b, float a) { lastColor = List.of(r, g, b, a); }
        public void blit(int x, int y, int u, int v, int width, int height) { blits.add(new Blit(x, y, u, width, height)); }
        public void item(ItemStack stack, int x, int y) { items.add(stack); }
        public void centered(String text, int x, int y) { labels.add(new Label(text, x, y, 0xFFFFFFFF, true)); }
        public void text(String text, int x, int y, int color) { labels.add(new Label(text, x, y, color, false)); }
        public int width(String text) { return text.length() * 6; }
        public int fontHeight() { return 9; }
    }
}
