package thaumcraft.client.gui;

import com.mojang.math.Axis;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.container.ContainerResearchTable;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.ResearchTableCopyNetworking;
import thaumcraft.common.lib.research.ResearchAspectCombination;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.research.ResearchNoteData;
import thaumcraft.common.lib.research.ResearchNoteData.Cell;
import thaumcraft.common.lib.research.ResearchNoteEditing;
import thaumcraft.common.lib.research.ResearchNotePuzzle;
import thaumcraft.common.lib.research.ScribingInk;
import thaumcraft.common.lib.utils.HexUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Original-sized palette, combination circles and hex editing; server snapshots own materials. */
public final class GuiResearchTable extends ThaumcraftContainerScreen<ContainerResearchTable> {
    private static final ResourceLocation BACKGROUND = texture("gui/guiresearchtable2.png");
    private static final ResourceLocation PARCHMENT = texture("misc/parchment3.png");
    private static final ResourceLocation HEX = texture("gui/hex1.png"), UNKNOWN = texture("aspects/_unknown.png");
    private static final ResourceLocation PARTICLES = texture("misc/particles.png");
    private static final ResourceLocation ASPECT_BACK = texture("aspects/_back.png");
    /** The original rune sheet of the research table (oracle GuiResearchTable:517-518). */
    private static final ResourceLocation SCRIPT = texture("misc/script.png");
    private int page;
    private int handledButton = -1;
    private String draggedAspect;
    private long dragRevision, dragSession;
    private ItemStack dragNote = ItemStack.EMPTY;
    private String selectedLeft, selectedRight;
    private long selectionSession, combinePressedUntil;
    /** The runes of the sheet of the original (oracle GuiResearchTable:83,408-441). */
    private final Map<String, Rune> runes = new LinkedHashMap<>();
    private long lastRuneCheck;

    /** One rune of the original sheet: its hex, its sprite and the moment it fades away. */
    private record Rune(int q, int r, long start, long decay, int sprite) {}

    public GuiResearchTable(ContainerResearchTable menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = imageHeight = 255;
    }

    private static ResourceLocation texture(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/" + path); }

    @Override protected void init() {
        super.init();
        cancelDrag();
        handledButton = -1;
        selectedLeft = selectedRight = null;
        selectionSession = menu.sessionToken();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        validateDrag();
        validateSelection();
        super.render(graphics, mouseX, mouseY, delta);
        if (draggedAspect != null) {
            Aspect aspect = Aspect.getAspect(draggedAspect);
            graphics.pose().pushPose();
            try {
                graphics.pose().translate(0, 0, 300);
                tinted(graphics, aspect.getImage(), mouseX - 8, mouseY - 8, aspect.getColor(), 1);
            } finally {
                graphics.pose().popPose();
            }
            return;
        }
        renderTooltip(graphics, mouseX, mouseY);
        if (minecraft.player == null || hoveredSlot != null) return;
        String tag = aspectAt(mouseX, mouseY);
        if (tag != null) {
            tooltip(graphics, aspectTooltip(tag), mouseX, mouseY);
            drawComponents(graphics, tag, mouseX, mouseY);
            return;
        }
        String selected = inside(mouseX, mouseY, 13, 139, 16, 16) ? selectedLeft
                : inside(mouseX, mouseY, 71, 139, 16, 16) ? selectedRight : null;
        if (selected != null) {
            tooltip(graphics, aspectTooltip(selected), mouseX, mouseY);
            return;
        }
        if (inside(mouseX, mouseY, 35, 139, 32, 16) && selectedLeft != null && selectedRight != null) {
            var lines = new ArrayList<Component>();
            lines.add(Component.translatable("tc.port.table.combine"));
            lines.addAll(aspectTooltip(selectedLeft));
            lines.addAll(aspectTooltip(selectedRight));
            if (!menu.editReady()) lines.add(Component.translatable("tc.port.table.waiting").withStyle(ChatFormatting.GRAY));
            tooltip(graphics, lines, mouseX, mouseY);
            return;
        }
        ResearchNoteData note = note();
        if (copyVisible(note) && inside(mouseX, mouseY, 37, 5, 24, 24)) {
            var lines = new ArrayList<Component>();
            lines.add(Component.translatable("tc.research.copy"));
            lines.add(Component.translatable(Items.PAPER.getDescriptionId()).append(" × 1").withStyle(
                    ResearchTableCopyNetworking.findSupply(minecraft.player.getInventory(), Items.PAPER) >= 0 ? ChatFormatting.GRAY : ChatFormatting.RED));
            lines.add(Component.translatable(Items.BLACK_DYE.getDescriptionId()).append(" × 1").withStyle(
                    ResearchTableCopyNetworking.findSupply(minecraft.player.getInventory(), Items.BLACK_DYE) >= 0 ? ChatFormatting.GRAY : ChatFormatting.RED));
            var definition = ResearchCatalog.get(note.key());
            if (definition != null) {
                for (var entry : definition.aspectCost().entrySet().stream().sorted(java.util.Comparator.comparing(
                        entry -> Aspect.getAspect(entry.getKey()).getName())).toList()) {
                    long price = (long) entry.getValue() + note.copies();
                    lines.add(Component.literal(Aspect.getAspect(entry.getKey()).getName() + " × " + price).withStyle(
                            price <= poolFor(entry.getKey()) ? ChatFormatting.GRAY : ChatFormatting.RED));
                }
            }
            if (!menu.editReady()) lines.add(Component.translatable("tc.port.table.waiting").withStyle(ChatFormatting.GRAY));
            tooltip(graphics, lines, mouseX, mouseY);
            return;
        }
        if (inside(mouseX, mouseY, 79, 120, 13, 12)) {
            var lines = new ArrayList<Component>();
            var definition = note == null ? null : ResearchCatalog.get(note.key());
            if (definition != null) lines.add(Component.translatable(definition.nameKey()));
            lines.add(Component.translatable("tc.port.table.help"));
            lines.add(Component.translatable("tc.port.table.combine_help"));
            if (note != null && note.complete()) lines.add(Component.translatable("tc.port.table.learn").withStyle(ChatFormatting.GREEN));
            if (note != null && !note.complete() && !hasInk()) lines.add(Component.translatable("tc.port.table.no_ink").withStyle(ChatFormatting.RED));
            if (!menu.editReady()) lines.add(Component.translatable("tc.port.table.waiting").withStyle(ChatFormatting.GRAY));
            tooltip(graphics, lines, mouseX, mouseY);
            return;
        }
        Cell cell = cellAt(note, mouseX, mouseY);
        if (cell == null || cell.aspect() == null) return;
        if (KnowledgeAccess.get(minecraft.player).hasDiscoveredAspect(cell.aspect())) {
            var lines = aspectTooltip(cell.aspect());
            if (note.complete()) lines.add(Component.translatable("tc.port.table.learn").withStyle(ChatFormatting.GREEN));
            tooltip(graphics, lines, mouseX, mouseY);
        } else graphics.renderTooltip(font, Component.translatable("tc.port.table.unknown_aspect"), mouseX, mouseY);
    }

    @Override protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        blitAlpha(graphics, BACKGROUND, leftPos, topPos, 0, 0, 255, 167, 256, 256, 1, 1, 1, 1);
        blitAlpha(graphics, BACKGROUND, leftPos + 40, topPos + 167, 0, 166, 184, 88, 256, 256, 1, 1, 1, 1);
        drawPalette(graphics);
        drawCombination(graphics);
        graphics.drawString(font, "?", leftPos + 82, topPos + 121, 0xCEB995, false);
        var note = note();
        if (copyVisible(note)) {
            // Original button5 location and sprite; only an authoritative acknowledged revision can request copying.
            blitAlpha(graphics, BACKGROUND, leftPos + 37, topPos + 5, 232, 200, 24, 24, 256, 256,
                    1, 1, 1, menu.canCopy() ? 1 : .4F);
        }
        if (note == null) {
            // Original GuiResearchTable:406-409 - the runes of the sheet die with the note.
            runes.clear();
            graphics.drawWordWrap(font, Component.translatable("tc.port.table.empty"), leftPos + 105, topPos + 72, 130, 0xCEB995);
            return;
        }
        blitAlpha(graphics, PARCHMENT, leftPos + 94, topPos + 8, 0, 0, 150, 150, 256, 256, 1, 1, 1, 1);
        drawNote(graphics, note);
        var definition = ResearchCatalog.get(note.key());
        if (definition != null) graphics.drawString(font,
                font.plainSubstrByWidth(Component.translatable(definition.nameKey()).getString(), 134), leftPos + 101, topPos + 11, 0x806344, false);
        graphics.drawString(font, Component.translatable(note.complete() ? "tc.port.table.complete" : "tc.port.table.incomplete"),
                leftPos + 101, topPos + 144, 0x806344, false);
        if (!note.complete() && !hasInk()) graphics.drawString(font, font.plainSubstrByWidth(Component.translatable("tc.port.table.no_ink").getString(), 134),
                leftPos + 101, topPos + 156, 0xE6A0A0, false);
        else if (!menu.editReady()) graphics.drawString(font, font.plainSubstrByWidth(Component.translatable("tc.port.table.waiting").getString(), 134),
                leftPos + 101, topPos + 156, 0xCEB995, false);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Original art already labels the inventory and provides both item slots.
    }

    private ResearchNoteData note() { return menu.getSlot(1).getItem().get(ThaumcraftContent.RESEARCH_NOTE_DATA.get()); }

    private boolean copyVisible(ResearchNoteData note) {
        return minecraft.player != null && note != null && note.complete()
                && menu.getSlot(1).getItem().is(ThaumcraftContent.DISCOVERY.get())
                && KnowledgeAccess.get(minecraft.player).isResearchComplete("RESEARCHDUPE");
    }

    private boolean hasInk() { return ScribingInk.consumeFromTable(menu.getSlot(0).getItem(), false); }

    private boolean editable(ResearchNoteData note) {
        return minecraft.player != null && menu.editReady() && note != null && !note.complete()
                && menu.getSlot(1).getItem().is(ThaumcraftContent.RESEARCH_NOTE.get()) && hasInk() && menu.getCarried().isEmpty();
    }

    private int poolFor(String tag) {
        return minecraft.player == null ? 0 : ResearchNoteEditing.queryPool(KnowledgeAccess.get(minecraft.player).getAspectPoolFor(tag));
    }

    private boolean funded(String tag) {
        return minecraft.player != null && Aspect.getAspect(tag) != null && KnowledgeAccess.get(minecraft.player).hasDiscoveredAspect(tag)
                && (poolFor(tag) > 0 || menu.bonusFor(tag) > 0);
    }

    private List<String> palette() {
        List<String> tags = minecraft.player == null ? List.of() : KnowledgeAccess.get(minecraft.player).getAspectsDiscovered().keySet().stream()
                .filter(tag -> Aspect.getAspect(tag) != null && tag.equals(Aspect.getAspect(tag).getTag())).sorted().toList();
        page = Math.min(page, lastPage(tags));
        return tags;
    }

    private static int lastPage(List<String> tags) { return Math.max(0, (tags.size() - 20) / 5); }

    /**
     * The mouse wheel turns the aspect palette like its two arrows, an input adopted from TC4Tweaks
     * (KNOWN_DEVIATIONS §8). TC4 pages the palette with the arrows only.
     */
    @Override public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY != 0 && inside(x, y, 10, 40, 80, 90)) {
            int before = page;
            page = Math.clamp(page - (int) Math.signum(scrollY), 0, lastPage(palette()));
            if (page != before) sound("key", .3F, 1);
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    private void drawPalette(GuiGraphics graphics) {
        List<String> tags = palette();
        for (int drawn = 0, index = page * 5; drawn < 25 && index < tags.size(); drawn++, index++) {
            String tag = tags.get(index);
            Aspect aspect = Aspect.getAspect(tag);
            int pool = poolFor(tag), bonus = menu.bonusFor(tag);
            int x = leftPos + 10 + drawn / 5 * 16, y = topPos + 40 + drawn % 5 * 16;
            tinted(graphics, aspect.getImage(), x, y, aspect.getColor(), pool > 0 || bonus > 0 ? 1 : .33F);
            if (pool > 0) smallNumber(graphics, pool, x + 16, y + 11);
            if (bonus > 0) {
                blitAlpha(graphics, PARTICLES, x - 4, y - 4, minecraft.player.tickCount % 16 * 16, 80, 16, 16, 256, 256, 1, 1, 1, 1);
                if (bonus > 1) smallNumber(graphics, bonus, x + 8, y + 2);
            }
        }
        if (page > 0) blitAlpha(graphics, BACKGROUND, leftPos + 27, topPos + 121, 184, 208, 24, 8, 256, 256, 1, 1, 1, 1);
        if (page < lastPage(tags)) blitAlpha(graphics, BACKGROUND, leftPos + 51, topPos + 121, 208, 208, 24, 8, 256, 256, 1, 1, 1, 1);
    }

    private void drawCombination(GuiGraphics graphics) {
        if (selectedLeft != null) {
            Aspect aspect = Aspect.getAspect(selectedLeft);
            tinted(graphics, aspect.getImage(), leftPos + 13, topPos + 139, aspect.getColor(), 1);
        }
        if (selectedRight != null) {
            Aspect aspect = Aspect.getAspect(selectedRight);
            tinted(graphics, aspect.getImage(), leftPos + 71, topPos + 139, aspect.getColor(), 1);
        }
        if (selectedLeft == null || selectedRight == null || minecraft.player == null) return;
        boolean payable = ResearchAspectCombination.canCombine(KnowledgeAccess.get(minecraft.player), menu.bonusPools(), selectedLeft, selectedRight);
        float alpha = payable && menu.editReady() ? 1 : .33F;
        // Original button/pressed overlay and animated orb, not a replacement
        // icon or a client-side preview that would reveal an unknown compound.
        blitAlpha(graphics, BACKGROUND, leftPos + 35, topPos + 139, 184, 184, 32, 16, 256, 256, 1, 1, 1, alpha);
        if (combinePressedUntil > System.nanoTime()) {
            blitAlpha(graphics, BACKGROUND, leftPos + 35, topPos + 139, 184, 168, 32, 16, 256, 256, 1, 1, 1, 1);
        } else {
            int count = minecraft.player.tickCount, x = leftPos + 43, y = topPos + 139;
            blitAlpha(graphics, PARTICLES, x, y, 128 + count % 8 * 32, 128, 16, 16, 256, 256,
                    .85F + (float) Math.sin((count + x) / 10D) * .15F,
                    .85F + (float) Math.sin((count + x + y) / 11D) * .15F,
                    .85F + (float) Math.sin((count + y) / 12D) * .15F, alpha);
        }
    }

    private void drawComponents(GuiGraphics graphics, String tag, int x, int y) {
        Aspect aspect = Aspect.getAspect(tag);
        if (aspect.isPrimal() || !KnowledgeAccess.get(minecraft.player).isResearchComplete("RESEARCHER1")) return;
        Aspect[] components = aspect.getComponents();
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0, 0, 400);
            for (int i = 0; i < 2; i++) {
                graphics.pose().pushPose();
                try {
                    graphics.pose().translate(x + 6 + i * 18, y + 6, 0);
                    graphics.pose().scale(1.25F, 1.25F, 1);
                    tinted(graphics, ASPECT_BACK, 0, 0, 0xFFFFFF, 1);
                } finally {
                    graphics.pose().popPose();
                }
                tinted(graphics, components[i].getImage(), x + 8 + i * 18, y + 8, components[i].getColor(), 1);
            }
        } finally {
            graphics.pose().popPose();
        }
    }

    private void smallNumber(GuiGraphics graphics, int amount, int right, int y) {
        String text = Integer.toString(amount);
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(right, y, 0);
            graphics.pose().scale(.5F, .5F, 1);
            graphics.drawString(font, text, -font.width(text), 0, 0xFFFFFF, true);
        } finally {
            graphics.pose().popPose();
        }
    }

    private ArrayList<Component> aspectTooltip(String tag) {
        var lines = new ArrayList<Component>();
        lines.add(Component.literal(Aspect.getAspect(tag).getName()));
        lines.add(Component.translatable("tc.aspect." + tag).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tc.port.table.pool", poolFor(tag), menu.bonusFor(tag)).withStyle(ChatFormatting.GRAY));
        return lines;
    }

    private void tooltip(GuiGraphics graphics, List<Component> lines, int x, int y) {
        graphics.renderTooltip(font, lines.stream().flatMap(line -> font.split(line, Math.min(260, width - 24)).stream()).toList(), x, y);
    }

    private boolean inside(double x, double y, int localX, int localY, int width, int height) {
        return x >= leftPos + localX && y >= topPos + localY && x < leftPos + localX + width && y < topPos + localY + height;
    }

    private boolean overSlot(double x, double y) {
        return menu.slots.stream().anyMatch(slot -> slot.isActive() && isHovering(slot.x, slot.y, 16, 16, x, y));
    }

    private String aspectAt(double x, double y) {
        if (!inside(x, y, 10, 40, 80, 80)) return null;
        List<String> tags = palette();
        int index = page * 5 + (int) (x - leftPos - 10) / 16 * 5 + (int) (y - topPos - 40) / 16;
        return index < tags.size() ? tags.get(index) : null;
    }

    private Cell cellAt(ResearchNoteData note, double x, double y) {
        if (note == null) return null;
        var hex = new HexUtils.Pixel(x - leftPos - 169, y - topPos - 83).toHex(9);
        return note.cellAt(hex.q, hex.r);
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        cancelDrag();
        handledButton = -1;
        // Item slots, shift-click and vanilla carried/quick-craft gestures stay native.
        if (overSlot(x, y) || !menu.getCarried().isEmpty()) return super.mouseClicked(x, y, button);
        if (minecraft.player == null) return super.mouseClicked(x, y, button);
        if (button == 0 && copyVisible(note()) && inside(x, y, 37, 5, 24, 24)) {
            handledButton = button;
            if (menu.requestCopy()) sound("cameraclack", .4F, 1);
            return true;
        }
        List<String> tags = palette();
        if (button >= 0 && button <= 2) {
            if (inside(x, y, 35, 139, 32, 16) && selectedLeft != null && selectedRight != null) {
                handledButton = button;
                combine(selectedLeft, selectedRight);
                return true;
            }
            if ((selectedLeft != null && inside(x, y, 11, 137, 16, 16))
                    || (selectedRight != null && inside(x, y, 71, 137, 16, 16))) {
                if (inside(x, y, 11, 137, 16, 16)) selectedLeft = null;
                else selectedRight = null;
                handledButton = button;
                sound("hhoff", .2F, 1 + minecraft.player.getRandom().nextFloat() * .1F);
                return true;
            }
            String tag = aspectAt(x, y);
            if (tag != null && hasShiftDown() && minecraft.player != null
                    && KnowledgeAccess.get(minecraft.player).isResearchComplete("RESEARCHER2") && !Aspect.getAspect(tag).isPrimal()) {
                Aspect[] components = Aspect.getAspect(tag).getComponents();
                handledButton = button;
                combine(components[0].getTag(), components[1].getTag());
                return true;
            }
            if (button == 1 && tag != null && menu.editReady() && funded(tag)) {
                handledButton = button;
                chooseFirstEmpty(tag);
                return true;
            }
        }
        if (button == 0) {
            if ((page > 0 && inside(x, y, 27, 121, 24, 8)) || (page < lastPage(tags) && inside(x, y, 51, 121, 24, 8))) {
                page += inside(x, y, 27, 121, 24, 8) ? -1 : 1;
                handledButton = button;
                sound("key", .3F, 1);
                return true;
            }
            if (inside(x, y, 10, 40, 80, 80)) {
                handledButton = button;
                String tag = aspectAt(x, y);
                if (tag != null && menu.editReady() && funded(tag)) {
                    draggedAspect = tag;
                    dragNote = menu.getSlot(1).getItem().copy();
                    dragRevision = menu.tableRevision();
                    dragSession = menu.sessionToken();
                    sound("hhoff", .2F, 1 + minecraft.player.getRandom().nextFloat() * .1F);
                }
                return true;
            }
        }
        ResearchNoteData note = note();
        if (button >= 0 && button <= 2 && editable(note)) {
            Cell cell = cellAt(note, x, y);
            // Unknown-to-actor placed aspects are also erasable. The server uses
            // the current cell's aspect; ERASE must not carry a client aspect.
            if (cell != null && cell.type() == 2 && cell.aspect() != null) {
                handledButton = button;
                if (menu.requestEdit(ResearchNoteEditing.Action.ERASE, cell.q(), cell.r(), null)) {
                    sound("hhon", .3F, 1);
                    sound("erase", .2F, 1 + minecraft.player.getRandom().nextFloat() * .1F);
                }
                return true;
            }
        }
        return super.mouseClicked(x, y, button);
    }

    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        validateDrag();
        return button == handledButton || super.mouseDragged(x, y, button, dx, dy);
    }

    @Override public boolean mouseReleased(double x, double y, int button) {
        if (button != handledButton) return super.mouseReleased(x, y, button);
        validateDrag();
        String tag = draggedAspect;
        cancelDrag();
        handledButton = -1;
        ResearchNoteData note = note();
        if (button == 0 && tag != null && !overSlot(x, y) && menu.editReady() && menu.getCarried().isEmpty() && funded(tag)) {
            Cell cell = editable(note) ? cellAt(note, x, y) : null;
            if (cell != null && cell.type() == 0 && menu.requestEdit(ResearchNoteEditing.Action.PLACE, cell.q(), cell.r(), tag)) {
                sound("hhon", .3F, 1);
                sound("write", .2F, 1);
            } else if (inside(x, y, 4, 130, 32, 32)) {
                selectedLeft = tag;
                selectionSession = menu.sessionToken();
                sound("hhoff", .2F, 1 + minecraft.player.getRandom().nextFloat() * .1F);
            } else if (inside(x, y, 63, 130, 32, 32)) {
                selectedRight = tag;
                selectionSession = menu.sessionToken();
                sound("hhoff", .2F, 1 + minecraft.player.getRandom().nextFloat() * .1F);
            } else if (tag.equals(aspectAt(x, y))) {
                chooseFirstEmpty(tag);
            }
        }
        return true;
    }

    private void validateDrag() {
        if (draggedAspect != null && (!menu.editReady() || !menu.getCarried().isEmpty() || menu.tableRevision() != dragRevision || menu.sessionToken() != dragSession
                || !ItemStack.matches(dragNote, menu.getSlot(1).getItem()) || !funded(draggedAspect))) cancelDrag();
    }

    private void validateSelection() {
        if (selectionSession != menu.sessionToken()) {
            selectedLeft = selectedRight = null;
            selectionSession = menu.sessionToken();
        }
        if (selectedLeft != null && !funded(selectedLeft)) selectedLeft = null;
        if (selectedRight != null && !funded(selectedRight)) selectedRight = null;
    }

    private void chooseFirstEmpty(String tag) {
        if (selectedLeft == null) selectedLeft = tag;
        else if (selectedRight == null) selectedRight = tag;
        else return;
        selectionSession = menu.sessionToken();
        sound("hhoff", .2F, 1 + minecraft.player.getRandom().nextFloat() * .1F);
    }

    private void combine(String left, String right) {
        if (combinePressedUntil > System.nanoTime() || !menu.requestCombine(left, right)) return;
        combinePressedUntil = System.nanoTime() + 200_000_000L;
        sound("cameraclack", .4F, 1);
        sound("hhon", .3F, 1);
    }

    private void cancelDrag() {
        draggedAspect = null;
        dragNote = ItemStack.EMPTY;
    }

    private void sound(String name, float volume, float pitch) {
        if (minecraft.player != null) minecraft.player.playSound(SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath("thaumcraft", name)), volume, pitch);
    }

    private void drawNote(GuiGraphics graphics, ResearchNoteData note) {
        if (minecraft.player == null) return;
        drawRunes(graphics, note);
        Set<String> known = KnowledgeAccess.get(minecraft.player).getAspectsDiscovered().keySet();
        Set<String> connected = new HashSet<>();
        for (Cell cell : note.cells()) if (cell.type() == 1 && known.contains(cell.aspect())) visit(note, cell, known, connected);
        for (Cell source : note.cells()) {
            if (!connected.contains(source.coordinate())) continue;
            var hex = new HexUtils.Hex(source.q(), source.r());
            for (int direction = 0; direction < 3; direction++) {
                var other = hex.getNeighbour(direction);
                Cell target = note.cellAt(other.q, other.r);
                if (target == null || !connected.contains(target.coordinate()) || !known.contains(source.aspect())
                        || !known.contains(target.aspect()) || !ResearchNotePuzzle.isDirectlyConnected(source.aspect(), target.aspect())) continue;
                var p1 = hex.toPixel(9); var p2 = other.toPixel(9);
                line(graphics, leftPos + 169 + p1.x, topPos + 83 + p1.y, leftPos + 169 + p2.x, topPos + 83 + p2.y);
            }
        }
        for (Cell cell : note.cells()) {
            var point = new HexUtils.Hex(cell.q(), cell.r()).toPixel(9);
            int x = leftPos + 161 + (int) Math.round(point.x), y = topPos + 75 + (int) Math.round(point.y);
            if (!note.complete()) {
                if (cell.type() == 1) {
                    int count = minecraft.player.tickCount, frame = count % 8;
                    blitAlpha(graphics, PARTICLES, x, y, 128 + frame * 32, 128, 16, 16, 256, 256,
                            .85F + (float) Math.sin((count + x) / 10D) * .15F,
                            .85F + (float) Math.sin((count + x + y) / 11D) * .15F,
                            .85F + (float) Math.sin((count + y) / 12D) * .15F, 1);
                } else tinted(graphics, HEX, x, y, 0xFFFFFF, .25F);
            }
            if (cell.aspect() == null) continue;
            if (!known.contains(cell.aspect())) {
                tinted(graphics, UNKNOWN, x, y, 0, .5F);
            } else {
                Aspect aspect = Aspect.getAspect(cell.aspect());
                float alpha = cell.type() == 1 || connected.contains(cell.coordinate()) ? 1 : .66F;
                tinted(graphics, aspect.getImage(), x, y, aspect.getColor(), alpha);
            }
        }
    }

    /**
     * The runes of the sheet of the original (oracle {@code GuiResearchTable.java:408-441}).
     *
     * <p>Every 250 milliseconds the original puts one rune on a free hex of the sheet: the hex is
     * drawn at random around the centre of the sheet, the sprite is one of the sixteen cells of
     * the script sheet and the rune fades in and out over fifteen to twenty five seconds.</p>
     */
    private void drawRunes(GuiGraphics graphics, ResearchNoteData note) {
        long time = System.currentTimeMillis();
        if (lastRuneCheck < time) {
            lastRuneCheck = time + 250L;
            var random = minecraft.level == null ? null : minecraft.level.getRandom();
            if (random != null) {
                int offsetX = random.nextInt(120) - 60;
                int offsetY = random.nextInt(120) - 60;
                var hex = new HexUtils.Pixel(offsetX, offsetY).toHex(9);
                String key = hex.q + ":" + hex.r;
                if (!runes.containsKey(key) && note.cellAt(hex.q, hex.r) == null) {
                    runes.put(key, new Rune(hex.q, hex.r, time,
                            lastRuneCheck + 15000L + random.nextInt(10000), random.nextInt(16)));
                }
            }
        }
        if (runes.isEmpty()) return;
        var stale = new ArrayList<String>();
        for (var entry : runes.entrySet()) {
            Rune rune = entry.getValue();
            if (rune.decay < time) { stale.add(entry.getKey()); continue; }
            var point = new HexUtils.Hex(rune.q, rune.r).toPixel(9);
            float progress = (float) (time - rune.start) / (float) (rune.decay - rune.start);
            float alpha = 0.5F;
            if (progress < 0.25F) alpha = progress * 2.0F;
            else if (progress > 0.5F) alpha = 1.0F - progress;
            drawRune(graphics, leftPos + 169 + point.x, topPos + 83 + point.y, rune.sprite, alpha * 0.66F);
        }
        for (String key : stale) runes.remove(key);
    }

    /**
     * One rune of the original sheet (oracle {@code GuiResearchTable.java:517-540}): the cell of
     * the script sheet, ten pixels wide, black, turned by ninety degrees in the plane.
     */
    private void drawRune(GuiGraphics graphics, double x, double y, int sprite, float alpha) {
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(x, y, 0);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(90));
            blitAlpha(graphics, SCRIPT, -5, -5, sprite * 16, 0, 10, 10, 256, 16, 0, 0, 0, alpha);
        } finally {
            graphics.pose().popPose();
        }
    }

    private static void visit(ResearchNoteData note, Cell source, Set<String> known, Set<String> connected) {
        if (!connected.add(source.coordinate())) return;
        var hex = new HexUtils.Hex(source.q(), source.r());
        for (int direction = 0; direction < 6; direction++) {
            var other = hex.getNeighbour(direction);
            Cell target = note.cellAt(other.q, other.r);
            if (target != null && target.type() > 0 && known.contains(source.aspect()) && known.contains(target.aspect())
                    && ResearchNotePuzzle.isDirectlyConnected(source.aspect(), target.aspect())) visit(note, target, known, connected);
        }
    }

    /**
     * The line of the original sheet (oracle {@code GuiResearchTable.java:385-405}).
     *
     * <p>Three pixels wide, the colour {@code 0.0, 0.6, 0.8}, the additive blend
     * {@code (770, 1)} and an alpha that pulses between 0.3 and 0.9 with the ticks of the
     * player.</p>
     */
    private void line(GuiGraphics graphics, double x, double y, double endX, double endY) {
        int count = minecraft.player == null ? 0 : minecraft.player.tickCount;
        float alpha = 0.3F + (float) Math.sin(count + x) * 0.3F + 0.3F;
        var pose = graphics.pose();
        pose.pushPose();
        try {
            pose.translate(x, y, 0);
            pose.mulPose(Axis.ZP.rotation((float) Math.atan2(endY - y, endX - x)));
            graphics.flush();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                    com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
            int color = ((int) Math.round(Math.max(0F, Math.min(1F, alpha)) * 255F) << 24) | 0x0099CC;
            graphics.fill(0, -1, (int) Math.round(Math.hypot(endX - x, endY - y)), 2, color);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        } finally {
            pose.popPose();
        }
    }

    private static void tinted(GuiGraphics graphics, ResourceLocation texture, int x, int y, int color, float alpha) {
        blitAlpha(graphics, texture, x, y, 0, 0, 16, 16, 16, 16,
                (color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, alpha);
    }

    private static void blitAlpha(GuiGraphics graphics, ResourceLocation texture, int x, int y, int u, int v,
                                  int width, int height, int textureWidth, int textureHeight,
                                  float red, float green, float blue, float alpha) {
        // The plain 1.21.1 texture blit does not enable blending. Flush queued
        // text first so its render type cannot reset this alpha state mid-draw.
        graphics.flush();
        graphics.setColor(red, green, blue, alpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try {
            graphics.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
        } finally {
            graphics.setColor(1, 1, 1, 1);
            RenderSystem.disableBlend();
        }
    }
}
