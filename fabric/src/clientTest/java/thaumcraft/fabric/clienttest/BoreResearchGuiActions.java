package thaumcraft.fabric.clienttest;

import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.gui.GuiResearchTable;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.container.ContainerResearchTable;
import thaumcraft.common.items.ItemResearchNotes;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.ResearchAspectCombination;
import thaumcraft.common.lib.research.ResearchNoteEditing;
import thaumcraft.common.lib.research.ScribingInk;
import thaumcraft.common.lib.utils.HexUtils.Hex;

/** Ignored source staging: actual screen input only, not a resource/packet API.
 * Caller owns foreground/native Shift preflight, timing, acknowledgements and
 * finite acquisition. This helper never creates a note or changes knowledge,
 * inventory, ink, palette fields, native clocks, RNG or server/table state. */
public final class BoreResearchGuiActions {
    private BoreResearchGuiActions() {}

    public record Sent(int container, long session, long revision, String kind,
                       String first, String second, int q, int r, ItemStack noteBefore, ItemStack inkBefore) {
        public Sent { noteBefore = noteBefore.copy(); inkBefore = inkBefore.copy(); }
        @Override public ItemStack noteBefore() { return noteBefore.copy(); }
        @Override public ItemStack inkBefore() { return inkBefore.copy(); }
    }
    private record View(Minecraft client, GuiResearchTable screen, ContainerResearchTable menu, int left, int top) {}
    private record Point(double x, double y) {}

    /** At most ONE actual palette-arrow click per call. No server action. */
    public static boolean makeVisible(Minecraft client, String tag) {
        View view = ready(client);
        List<String> tags = palette(client);
        int index = tags.indexOf(tag);
        require(index >= 0, "Requested aspect is not actually discovered: " + tag);
        int page = intField(view.screen, "page");
        int last = Math.max(0, (tags.size() - 20) / 5);
        require(page >= 0 && page <= last, "Unbound research palette page");
        if (index >= page * 5 && index < page * 5 + 25) return true;
        int direction = index < page * 5 ? -1 : 1;
        require(direction < 0 ? page > 0 : page < last, "Aspect cannot be reached by a real palette arrow");
        click(view, view.left + (direction < 0 ? 39 : 63), view.top + 125, 0);
        require(intField(view.screen, "page") == page + direction, "Native research palette arrow did not move exactly once");
        return false;
    }

    /** Drag the real visible palette icon onto one actual blank note hex.
     * The normal screen creates its own session/revision-bounded C2S request. */
    public static Sent placeVisible(Minecraft client, String tag, int q, int r) {
        View view = ready(client);
        var note = ItemResearchNotes.data(view.menu.getSlot(1).getItem());
        require(note != null && !note.complete() && note.cellAt(q, r) != null && note.cellAt(q, r).type() == 0,
                "Only a blank cell of the actual unfinished note is editable");
        require(ScribingInk.consumeFromTable(view.menu.getSlot(0).getItem(), false), "Real research table ink is exhausted");
        Point palette = point(view, tag);
        var pixel = new Hex(q, r).toPixel(9);
        double x = view.left + 169 + pixel.x, y = view.top + 83 + pixel.y;
        Sent sent = before(view, "PLACE", tag, null, q, r);
        require(view.screen.mouseClicked(palette.x, palette.y, 0), "Native research palette click was not consumed");
        require(tag.equals(stringField(view.screen, "draggedAspect")), "Actual palette did not begin the requested funded drag");
        require(view.screen.mouseDragged(x, y, 0, x - palette.x, y - palette.y), "Native research drag was not consumed");
        require(view.screen.mouseReleased(x, y, 0), "Native note-hex release was not consumed");
        sent(view, sent);
        return sent;
    }

    /** Normal right-click palette selection and combine-button press. Never
     * writes selectedLeft/Right or calls the networking backend directly. */
    public static Sent combineVisible(Minecraft client, String first, String second) {
        View view = ready(client);
        require(System.nanoTime() >= (Long) field(view.screen, "combinePressedUntil"),
                "Wait for the actual GUI combine cooldown; a network acknowledgement alone is insufficient");
        require(ResearchAspectCombination.canCombine(KnowledgeAccess.get(client.player), view.menu.bonusPools(), first, second),
                "Actual research pools/bonuses cannot fund the requested pair");
        // Caller must make both icons visible in one viewport before this action.
        Point left = point(view, first), right = point(view, second);
        if (stringField(view.screen, "selectedLeft") != null) click(view, view.left + 21, view.top + 147, 0);
        if (stringField(view.screen, "selectedRight") != null) click(view, view.left + 79, view.top + 147, 0);
        require(stringField(view.screen, "selectedLeft") == null && stringField(view.screen, "selectedRight") == null,
                "Native selection circles did not release previous presentation");
        click(view, left.x, left.y, 1);
        click(view, right.x, right.y, 1);
        require(first.equals(stringField(view.screen, "selectedLeft")) && second.equals(stringField(view.screen, "selectedRight")),
                "Native palette selection does not match the requested real pair");
        Sent sent = before(view, "COMBINE", first, second, 0, 0);
        click(view, view.left + 51, view.top + 147, 0);
        sent(view, sent);
        return sent;
    }

    private static View ready(Minecraft client) {
        require(client != null && client.isSameThread() && client.player != null && client.gameMode != null
                && client.getConnection() != null && client.screen instanceof GuiResearchTable,
                "Research input requires the actual connected client table screen");
        var screen = (GuiResearchTable) client.screen;
        require(client.player.containerMenu instanceof ContainerResearchTable && screen.getMenu() == client.player.containerMenu,
                "Research input cannot target a stale/replaced menu");
        var menu = (ContainerResearchTable) client.player.containerMenu;
        require(menu.editReady() && menu.getCarried().isEmpty() && !Screen.hasShiftDown() && !client.options.keyShift.isDown(),
                "Wait for real acknowledgement, an empty cursor and released native Shift");
        return new View(client, screen, menu, (screen.width - 255) / 2, (screen.height - 255) / 2);
    }
    private static List<String> palette(Minecraft client) {
        return KnowledgeAccess.get(client.player).getAspectsDiscovered().keySet().stream()
                .filter(tag -> Aspect.getAspect(tag) != null && tag.equals(Aspect.getAspect(tag).getTag())).sorted().toList();
    }
    private static Point point(View view, String tag) {
        int index = palette(view.client).indexOf(tag), page = intField(view.screen, "page"), local = index - page * 5;
        require(index >= 0 && local >= 0 && local < 25, "Requested aspect is not actually visible: " + tag);
        var knowledge = KnowledgeAccess.get(view.client.player);
        require(ResearchNoteEditing.queryPool(knowledge.getAspectPoolFor(tag)) > 0 || view.menu.bonusFor(tag) > 0,
                "Requested visible aspect has no real funding");
        return new Point(view.left + 18 + local / 5 * 16, view.top + 48 + local % 5 * 16);
    }
    private static Sent before(View view, String kind, String first, String second, int q, int r) {
        require(view.menu.editReady(), "No second research request while an acknowledgement is pending");
        return new Sent(view.menu.containerId, view.menu.sessionToken(), view.menu.tableRevision(), kind,
                first, second, q, r, view.menu.getSlot(1).getItem(), view.menu.getSlot(0).getItem());
    }
    private static void sent(View view, Sent before) {
        require(view.client.screen == view.screen && view.client.player.containerMenu == view.menu
                && view.menu.containerId == before.container && view.menu.sessionToken() == before.session
                && view.menu.tableRevision() == before.revision && !view.menu.editReady(),
                "The real GUI did not submit exactly one pending session-bound intent");
        require(ItemStack.matches(view.menu.getSlot(1).getItem(), before.noteBefore)
                && ItemStack.matches(view.menu.getSlot(0).getItem(), before.inkBefore),
                "Client research input must not optimistically change note or ink");
    }
    private static void click(View view, double x, double y, int button) {
        require(view.screen.mouseClicked(x, y, button), "Native research click was not consumed");
        require(view.screen.mouseReleased(x, y, button), "Native research release was not consumed");
    }
    /** Read-only custom GUI fields, never mapped native names or private writes. */
    private static Object field(GuiResearchTable screen, String name) {
        try { Field field = GuiResearchTable.class.getDeclaredField(name); field.setAccessible(true); return field.get(screen); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("Research screen observation changed: " + name, failure); }
    }
    private static int intField(GuiResearchTable screen, String name) { return (Integer) field(screen, name); }
    private static String stringField(GuiResearchTable screen, String name) { return (String) field(screen, name); }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
