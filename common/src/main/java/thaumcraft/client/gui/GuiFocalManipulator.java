package thaumcraft.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2i;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.common.container.ContainerFocalManipulator;
import thaumcraft.common.lib.foci.FocalUpgradeCosts;
import thaumcraft.common.lib.network.FocalManipulatorAckPayload;
import thaumcraft.common.lib.network.FocalManipulatorNetworking;
import thaumcraft.common.tiles.TileFocalManipulator;

/** Original 192x233 layout and remaining-vis display, with cosmetic local
 * selection and session/revision-bound START. The one physical focus slot is
 * native menu inventory; neither selection nor ACK creates a result or progress. */
public final class GuiFocalManipulator extends ThaumcraftContainerScreen<ContainerFocalManipulator> {
    private static final ResourceLocation BACKGROUND = texture("gui/gui_wandtable.png");
    private static final ResourceLocation PARTICLES = texture("misc/particles.png");
    private static final SoundEvent CLICK = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "cameraclack"));
    private final DecimalFormat visFormat = new DecimalFormat("#######.#");
    private final RandomSource cosmeticRandom = RandomSource.create();
    private final List<GuiSparkle> sparkles = new ArrayList<>();
    private List<FocusUpgradeType> history = List.of(), choices = List.of();
    private ItemStack observedFocus = ItemStack.EMPTY;
    private long observedRevision = Long.MIN_VALUE, serverRevisionFloor = -1;
    private boolean observedActive, active, ready, invalidFocus;
    private int selected = -1, rank;
    private TileFocalManipulator displayTile;
    private FocalUpgradeCosts.Plan preview;
    private Pending pending;
    private long refusalUntil, nextSparkle;

    private record Pending(UUID session, long revision, int upgrade) {}

    public GuiFocalManipulator(ContainerFocalManipulator menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 192;
        imageHeight = 233;
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/" + path);
    }

    @Override protected void containerTick() {
        super.containerTick();
        refreshView();
    }

    /** Native slots arrive independently of BE/menu data. A coherent view is
     * required before admitting a START; missing/delayed state is not guessed. */
    private void refreshView() {
        ItemStack focusStack = menu.getSlot(0).getItem().copy();
        displayTile = menu.tile();
        long revision = menu.selectionRevision();
        active = displayTile != null && displayTile.getSize() > 0;
        ready = minecraft != null && minecraft.player != null && minecraft.player.containerMenu == menu
                && menu.stillValid(minecraft.player) && displayTile != null && !displayTile.isRemoved()
                && displayTile.getLevel() == minecraft.level && !displayTile.isQuarantined() && revision >= 0
                && revision >= serverRevisionFloor && displayTile.getSelectionRevision() == revision
                && ItemStack.matches(focusStack, displayTile.getItem(0));
        if (revision != observedRevision || active != observedActive || !ItemStack.matches(observedFocus, focusStack)) {
            selected = -1;
            preview = null;
            observedRevision = revision;
            observedActive = active;
            observedFocus = focusStack.copy();
        }
        history = choices = List.of();
        rank = 0;
        invalidFocus = false;
        if (focusStack.isEmpty() || !(focusStack.getItem() instanceof ItemFocusBasic focus)) return;
        try {
            short[] applied = focus.getAppliedUpgrades(focusStack.copy());
            if (applied.length != 5) throw new IllegalArgumentException("Expected five focus ranks");
            var completed = new ArrayList<FocusUpgradeType>(5);
            int firstEmpty = 0;
            while (firstEmpty < applied.length && applied[firstEmpty] >= 0) {
                FocusUpgradeType type = FocusUpgradeType.get(applied[firstEmpty]);
                if (type == null) throw new IllegalArgumentException("Unknown focus upgrade");
                completed.add(type);
                firstEmpty++;
            }
            history = List.copyOf(completed);
            rank = firstEmpty == 5 ? -1 : firstEmpty + 1;
            if (active) {
                rank = displayTile.getRank();
                selected = displayTile.getUpgrade();
            }
            if (rank < 1 || rank > 5) {
                selected = -1;
                preview = null;
                return;
            }
            var available = new ArrayList<FocusUpgradeType>();
            FocusUpgradeType[] candidates = focus.getPossibleUpgradesByRank(focusStack.copy(), rank);
            if (candidates != null) for (FocusUpgradeType type : candidates) {
                if (type != null && focus.canApplyUpgrade(focusStack.copy(), minecraft == null ? null : minecraft.player, type, rank)) available.add(type);
            }
            choices = List.copyOf(available); // Preserve the focus's original filtered order.
            FocusUpgradeType selection = FocusUpgradeType.get(selected);
            if (selection == null || choices.stream().noneMatch(type -> type.id == selected)) {
                selected = -1;
                preview = null;
            } else preview = FocalUpgradeCosts.plan(focusStack, selection, rank);
        } catch (IllegalArgumentException malformed) {
            invalidFocus = true;
            selected = -1;
            preview = null;
            choices = List.of();
        }
    }

    private boolean selectable() { return ready && !invalidFocus && !active && pending == null && rank > 0; }
    private boolean startEnabled() {
        return selectable() && selected >= 0 && preview != null && minecraft.player.experienceLevel >= preview.xpLevels();
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        refreshView();
        // Like the original, custom icon/bar actions accept either mouse button;
        // ordinary physical slots still delegate to the native container screen.
        if (inside(mouseX, mouseY, 48, 88, 96, 8)) {
            if (startEnabled()) {
                pending = new Pending(menu.sessionId(), menu.selectionRevision(), selected);
                refusalUntil = 0;
                FocalManipulatorNetworking.requestStart(menu, selected);
                clickSound();
            }
            return true;
        }
        for (int i = 0; i < choices.size(); i++) {
            if (!inside(mouseX, mouseY, 48 + i * 16, 104, 16, 16)) continue;
            if (selectable()) {
                int id = choices.get(i).id;
                selected = selected == id ? -1 : id;
                refusalUntil = 0;
                refreshView();
                clickSound();
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Called only by the queued client receiver for the currently open GUI.
     * Refusals release the exact pending intent too; stale ACKs never unlock it. */
    public void acceptAck(FocalManipulatorAckPayload ack) {
        if (minecraft == null || minecraft.screen != this || minecraft.player == null
                || minecraft.player.containerMenu != menu || pending == null || ack.containerId() != menu.containerId
                || !menu.sessionId().equals(ack.sessionId()) || !pending.session().equals(ack.sessionId())
                || pending.revision() != ack.expectedRevision() || pending.upgrade() != ack.upgradeId()) return;
        pending = null;
        serverRevisionFloor = Math.max(serverRevisionFloor, ack.serverRevision());
        refusalUntil = ack.accepted() ? 0 : System.currentTimeMillis() + 3000;
        refreshView();
    }

    @Override protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        refreshView();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try {
            graphics.setColor(1, 1, 1, 1);
            graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
            for (int i = 0; i < choices.size(); i++) {
                if (choices.get(i).id == selected) graphics.blit(BACKGROUND, leftPos + 48 + i * 16, topPos + 104, 200, 0, 16, 16);
                icon(graphics, choices.get(i), 48 + i * 16, 104);
            }
            for (int i = 0; i < history.size(); i++) icon(graphics, history.get(i), 56 + i * 16, 32);
            if (rank > 0 && selected >= 0 && preview != null && minecraft.player != null) {
                if (startEnabled()) graphics.blit(BACKGROUND, leftPos + 48, topPos + 88, 8, 240, 96, 8);
                graphics.blit(BACKGROUND, leftPos + 108, topPos + 59, 200, 16, 16, 16);
                int xp = rank * 8;
                graphics.drawString(font, Integer.toString(xp), leftPos + 125, topPos + 64,
                        0xff000000 | (minecraft.player.experienceLevel < xp ? 16151160 : 10092429), true);
                AspectList remaining = active ? displayTile.getAspects() : preview.aspects();
                drawAspectAmounts(graphics, remaining);
                if (active) drawRemainingBar(graphics, remaining);
            }
            queueHistorySparkle();
        } finally {
            graphics.setColor(1, 1, 1, 1);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }

    private void icon(GuiGraphics graphics, FocusUpgradeType type, int x, int y) {
        graphics.blit(type.icon, leftPos + x, topPos + y, 16, 16, 0, 0, 32, 32, 32, 32);
    }

    private void drawAspectAmounts(GuiGraphics graphics, AspectList amounts) {
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(leftPos + 49, topPos + 68 - amounts.size() * 2.5, 0);
            graphics.pose().scale(.5F, .5F, 1);
            int row = 0;
            for (Aspect aspect : amounts.getAspectsSorted()) {
                if (aspect == null) continue;
                int color = 0xff000000 | aspect.getColor();
                graphics.drawString(font, aspect.getName(), 0, row * 10, color, true);
                graphics.drawString(font, visFormat.format(amounts.getAmount(aspect) / 100F), 48, row * 10, color, true);
                row++;
            }
        } finally { graphics.pose().popPose(); }
    }

    private void drawRemainingBar(GuiGraphics graphics, AspectList remaining) {
        int total = displayTile.getSize();
        if (total <= 0) return;
        int offset = 0;
        for (Aspect aspect : remaining.getAspectsSorted()) {
            if (aspect == null || remaining.getAmount(aspect) <= 0) continue;
            int pixels = Mth.clamp((int)(remaining.getAmount(aspect) / (float)total * 96F), 0, 96 - offset);
            int rgb = aspect.getColor();
            graphics.setColor((rgb >> 16 & 255) / 255F, (rgb >> 8 & 255) / 255F, (rgb & 255) / 255F, .9F);
            if (pixels > 0) graphics.blit(BACKGROUND, leftPos + 48 + offset, topPos + 88, 112 + offset, 240, pixels, 8);
            offset += pixels;
            if (cosmeticRandom.nextInt(66) == 0) addSparkle(48 + offset, 92, (46 + rank * 16F - 48 - offset) / 9F, -6, rgb);
        }
        graphics.setColor(1, 1, 1, 1);
    }

    private void queueHistorySparkle() {
        long now = System.currentTimeMillis();
        if (rank < 1 || now < nextSparkle) return;
        nextSparkle = now + (active ? 10 : 500) + cosmeticRandom.nextInt(200);
        int red = (int)((.5F + cosmeticRandom.nextFloat() * .4F) * 255);
        int green = (int)((1 - cosmeticRandom.nextFloat() * .4F) * 255);
        int blue = (int)((1 - cosmeticRandom.nextFloat() * .4F) * 255);
        addSparkle(42 + rank * 16 + cosmeticRandom.nextInt(12), 34 + cosmeticRandom.nextInt(12), 0, 0, red << 16 | green << 8 | blue);
    }

    private void addSparkle(float x, float y, float dx, float dy, int rgb) {
        if (sparkles.size() < 128) sparkles.add(new GuiSparkle(x, y, dx, dy, rgb));
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        long now = System.currentTimeMillis();
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0, 0, 200);
            for (var iterator = sparkles.iterator(); iterator.hasNext();) {
                GuiSparkle sparkle = iterator.next();
                if (now > sparkle.nextFrame) {
                    sparkle.nextFrame = now + 50;
                    sparkle.frame++;
                    sparkle.x += sparkle.dx;
                    sparkle.y += sparkle.dy;
                }
                if (sparkle.frame >= 9) { iterator.remove(); continue; }
                int rgb = sparkle.rgb;
                graphics.setColor((rgb >> 16 & 255) / 255F, (rgb >> 8 & 255) / 255F, (rgb & 255) / 255F, .9F);
                graphics.blit(PARTICLES, (int)sparkle.x - 4, (int)sparkle.y - 4, 8, 8, sparkle.frame * 16, 112, 16, 16, 256, 256);
            }
            graphics.flush();
        } finally {
            graphics.pose().popPose();
            graphics.setColor(1, 1, 1, 1);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
        String status = displayTile != null && displayTile.isQuarantined() ? "tc.port.focal.quarantined"
                : invalidFocus ? "tc.port.focus.upgrades.invalid" : pending != null ? "tc.port.focal.pending"
                : !ready ? "tc.port.focal.not_ready" : now < refusalUntil ? "tc.port.focal.refused" : null;
        if (status != null) {
            graphics.pose().pushPose();
            try {
                graphics.pose().translate(96, 136, 0);
                graphics.pose().scale(.6F, .6F, 1);
                graphics.drawCenteredString(font, Component.translatable(status), 0, 0, 0xffd3b4b4);
            } finally { graphics.pose().popPose(); }
        }
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
        if (hoveredSlot != null) return;
        for (int i = 0; i < choices.size(); i++) {
            if (inside(mouseX, mouseY, 48 + i * 16, 104, 16, 16)) { upgradeTooltip(graphics, choices.get(i)); return; }
        }
        for (int i = 0; i < history.size(); i++) {
            if (inside(mouseX, mouseY, 56 + i * 16, 32, 16, 16)) { upgradeTooltip(graphics, history.get(i)); return; }
        }
        if (selected < 0) return;
        String key = inside(mouseX, mouseY, 48, 48, 36, 36) ? "wandtable.text1"
                : inside(mouseX, mouseY, 108, 58, 36, 16) ? "wandtable.text2"
                : startEnabled() && inside(mouseX, mouseY, 48, 88, 96, 8) ? "wandtable.text3" : null;
        if (key != null) graphics.renderTooltip(font, Component.translatable(key), mouseX, mouseY);
    }

    private void upgradeTooltip(GuiGraphics graphics, FocusUpgradeType type) {
        int wrap = Math.max(32, width - (leftPos + 176));
        var lines = new ArrayList<net.minecraft.util.FormattedCharSequence>();
        lines.addAll(font.split(Component.translatable(type.name).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.UNDERLINE), wrap));
        lines.addAll(font.split(Component.translatable(type.text), wrap));
        graphics.renderTooltip(font, lines, (screenWidth, screenHeight, x, y, tooltipWidth, tooltipHeight) ->
                new Vector2i(Math.max(4, Math.min(leftPos + 168, screenWidth - tooltipWidth - 4)),
                        Math.max(4, Math.min(topPos + 12, screenHeight - tooltipHeight - 4))), 0, 0);
    }

    private boolean inside(double x, double y, int localX, int localY, int width, int height) {
        return x >= leftPos + localX && x < leftPos + localX + width && y >= topPos + localY && y < topPos + localY + height;
    }

    private void clickSound() { if (minecraft.player != null) minecraft.player.playSound(CLICK, .4F, 1); }

    @Override public void removed() {
        pending = null;
        sparkles.clear();
        super.removed(); // Persistent table inventory/jobs are not GUI-owned.
    }

    private static final class GuiSparkle {
        float x, y;
        final float dx, dy;
        final int rgb;
        int frame;
        long nextFrame = System.currentTimeMillis() + 50;
        GuiSparkle(float x, float y, float dx, float dy, int rgb) {
            this.x = x; this.y = y; this.dx = dx; this.dy = dy; this.rgb = rgb;
        }
    }
}
