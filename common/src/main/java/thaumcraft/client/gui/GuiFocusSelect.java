package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import thaumcraft.client.ThaumcraftFocusClient;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.common.lib.network.FocusSelectionNetworking;
import thaumcraft.common.items.wands.FocusSelectionSources;

/**
 * Native non-pausing version of TC4 REHWandHandler's held-F radial. All stacks
 * here are previews; only (hand, physical source) or (hand, -1) leaves this GUI.
 */
public final class GuiFocusSelect extends Screen {
    private static final ResourceLocation RADIAL = texture("textures/misc/radial.png");
    private static final ResourceLocation RADIAL_INNER = texture("textures/misc/radial2.png");
    private static final int PAGE_SIZE = 12;
    private final InteractionHand hand;
    private final LocalPlayer openingPlayer;
    private final ClientLevel openingLevel;
    private final int openingHotbarSlot;
    private final ItemStack openingWand;
    private final ItemStack equipped;
    private final List<Choice> choices = new ArrayList<>();
    private final List<FocusButton> focusButtons = new ArrayList<>();
    private int page;
    private int selectedIndex = -1;
    private boolean keyboardNavigation;
    private boolean closed;
    private int centerX, centerY;
    private float radius;

    private record Choice(int inventorySlot, ItemStack preview) {}

    public GuiFocusSelect(InteractionHand hand) {
        super(Component.translatable("key.thaumcraft.change_focus"));
        this.hand = hand;
        Minecraft client = Minecraft.getInstance();
        openingPlayer = client.player;
        openingLevel = client.level;
        openingHotbarSlot = openingPlayer == null ? -1 : openingPlayer.getInventory().selected;
        openingWand = openingPlayer == null ? ItemStack.EMPTY : openingPlayer.getItemInHand(hand).copy();
        equipped = FocusData.get(openingWand);
        if (openingPlayer != null) {
            choices.addAll(availableChoices(openingPlayer, true));
        }
    }

    private static List<Choice> availableChoices(LocalPlayer player, boolean copy) {
        var result = new ArrayList<Choice>();
        for(var choice:FocusSelectionSources.choices(player))result.add(new Choice(choice.address(),choice.focus()));
        return result;
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath("thaumcraft", path);
    }

    @Override protected void init() {
        centerX = width / 2;
        centerY = height / 2;
        focusButtons.clear();
        int start = page * PAGE_SIZE;
        int count = Math.min(PAGE_SIZE, choices.size() - start);
        // Original radius, topmost first, clockwise 360/count spacing and
        // 20x20 hitboxes. Paging only bounds large inventories of duplicates.
        radius = 16F + count * 2.5F;
        for (int offset = 0; offset < count; offset++) {
            int index = start + offset;
            double angle = -Math.PI / 2 + Math.PI * 2 * offset / count;
            FocusButton button = new FocusButton(index,
                    centerX + (int)Math.round(Math.cos(angle) * radius) - 10,
                    centerY + (int)Math.round(Math.sin(angle) * radius) - 10);
            focusButtons.add(addRenderableWidget(button));
        }
        if (pageCount() > 1) {
            addRenderableWidget(new QuietButton(centerX - 130, 42, 76, 16,
                    Component.translatable("tc.port.focus.previous"), button -> changePage(-1)));
            addRenderableWidget(new QuietButton(centerX + 54, 42, 76, 16,
                    Component.translatable("tc.port.focus.next"), button -> changePage(1)));
        }
        Button remove = new QuietButton(centerX - 128, height - 24, 170, 20,
                Component.translatable("tc.port.focus.remove", minecraft.options.keyShift.getTranslatedKeyMessage(),
                        ThaumcraftFocusClient.CHANGE_FOCUS.getTranslatedKeyMessage()), button -> request(-1));
        remove.active = !equipped.isEmpty();
        addRenderableWidget(remove);
        addRenderableWidget(new QuietButton(centerX + 48, height - 24, 80, 20,
                Component.translatable("gui.cancel"), button -> onClose()));
    }

    // Opening via a key must not preselect the first focus merely because
    // Minecraft's last input type was keyboard. Release over blank space cancels.
    @Override protected void setInitialFocus() {}

    @Override public boolean isPauseScreen() { return false; }

    // In1.21 Screen.render calls this from super.render. Without the override
    // it blurs not only the world, but our already-drawn header/radial too.
    // Original held-F selection is an unblurred world overlay.
    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {}

    @Override public void tick() {
        if (!contextValid()) onClose();
    }

    /** Tick fallback also handles releases lost while native mappings reset. */
    public void focusKeyState(boolean down) {
        if (closed) return;
        if (!contextValid()) {
            onClose();
        } else if (!down) {
            int index = keyboardNavigation ? selectedIndex : choiceAt(mouseX(), mouseY());
            if (index >= 0) request(choices.get(index).inventorySlot());
            else onClose();
        }
    }

    private boolean contextValid() {
        if (closed || minecraft == null || minecraft.screen != this || !minecraft.isWindowActive()
                || minecraft.getOverlay() != null || openingPlayer == null
                || minecraft.player != openingPlayer || minecraft.level != openingLevel
                || openingPlayer.getInventory().selected != openingHotbarSlot
                || ThaumcraftFocusClient.wandHand(openingPlayer) != hand
                || !ItemStack.matches(openingWand, openingPlayer.getItemInHand(hand))) return false;
        // Cancel if a displayed inventory slot changes, including custom
        // components/count, or if the eligible list changes. A release may not
        // silently select a different stack that arrived under the old icon.
        List<Choice> live = availableChoices(openingPlayer, false);
        if (live.size() != choices.size()) return false;
        for (int index = 0; index < live.size(); index++) {
            Choice expected = choices.get(index), actual = live.get(index);
            if (expected.inventorySlot() != actual.inventorySlot() || !ItemStack.matches(expected.preview(), actual.preview())) return false;
        }
        return true;
    }

    private void request(int inventorySlot) {
        if (!contextValid()) {
            onClose();
            return;
        }
        if (inventorySlot < 0 && equipped.isEmpty()) return;
        // No local equip, inventory mutation, vis prediction or success sound.
        // Original cameraticks is played by a successful server transition.
        FocusSelectionNetworking.request(hand, inventorySlot);
        onClose();
    }

    @Override public void onClose() {
        closed = true;
        // Let Minecraft own cursor release/re-grab and key-state restoration.
        // Never replace a different screen that superseded this one.
        if (minecraft != null && minecraft.screen == this) minecraft.setScreen(null);
    }

    @Override public void removed() {
        // Escape, disconnect, death, another screen and focus loss never commit.
        closed = true;
        super.removed();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (!contextValid()) {
            onClose();
            return;
        }
        if (!keyboardNavigation) selectedIndex = choiceAt(mouseX, mouseY);
        // No vanilla blurred menu background: TC4's radial overlays the world.
        graphics.fill(0, 0, width, 38, 0x60000000);
        centered(graphics, title, 10, 0xFFE8D8B4);
        centered(graphics, Component.translatable("tc.port.focus.current", equipped.isEmpty()
                ? Component.translatable("tc.port.focus.none") : equipped.getHoverName()), 26, 0xFFCDBFDD);
        if (pageCount() > 1) {
            centered(graphics, Component.translatable("tc.port.focus.page", page + 1, pageCount()), 46, 0xFFE8D8B4);
        }
        float angle = (openingPlayer.tickCount % 720 + delta) / 2F;
        drawRing(graphics, RADIAL, radius * 2.75F, angle);
        drawRing(graphics, RADIAL_INNER, radius * 2.55F, -angle);
        graphics.renderOutline(centerX - 10, centerY - 10, 20, 20, 0xFF86729B);
        if (!equipped.isEmpty()) graphics.renderItem(equipped, centerX - 8, centerY - 8);
        super.render(graphics, mouseX, mouseY, delta);
        Component selected = selectedIndex >= 0 ? entryMessage(choices.get(selectedIndex))
                : Component.translatable(choices.isEmpty() ? "tc.port.focus.empty" : "tc.port.focus.choose");
        // Keep selector feedback clear of vanilla's held-name/armor/hotbar HUD
        // at the normal viewport instead of drawing two labels over that HUD.
        int infoY = Math.max(40, Math.min(height - 108, centerY + Math.round(radius * 1.4F) + 16));
        centered(graphics, selected, infoY, 0xFFE8D8B4);
        centered(graphics, Component.translatable("tc.port.focus.help",
                ThaumcraftFocusClient.CHANGE_FOCUS.getTranslatedKeyMessage()), infoY + 12, 0xFFC8BBCF);
        int hover = choiceAt(mouseX, mouseY);
        if (hover >= 0) {
            Choice choice = choices.get(hover);
            List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(minecraft, choice.preview()));
            int pouchSlot=FocusSelectionSources.pouchSlot(choice.inventorySlot());
            lines.add(Component.translatable(pouchSlot<0?"tc.port.focus.slot":"tc.port.focus.pouch_slot",
                    pouchSlot<0?choice.inventorySlot()+1:pouchSlot+1).withStyle(ChatFormatting.GRAY));
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        } else if (!equipped.isEmpty() && Math.abs(mouseX - centerX) <= 10 && Math.abs(mouseY - centerY) <= 10) {
            graphics.renderTooltip(font, equipped, mouseX, mouseY);
        }
    }

    private void centered(GuiGraphics graphics, Component text, int y, int color) {
        graphics.drawCenteredString(font, font.plainSubstrByWidth(text.getString(), Math.max(1, width - 16)), centerX, y, color);
    }

    private void drawRing(GuiGraphics graphics, ResourceLocation texture, float diameter, float angle) {
        int size = Math.round(diameter);
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(angle));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor(0.5F, 0.5F, 0.5F, 0.5F);
        try {
            graphics.blit(texture, -size / 2, -size / 2, size, size, 0, 0, 256, 256, 256, 256);
        } finally {
            graphics.setColor(1F, 1F, 1F, 1F);
            graphics.pose().popPose();
            RenderSystem.disableBlend();
        }
    }

    private Component entryMessage(Choice choice) {
        return Component.translatable("tc.port.focus.entry", choice.preview().getHoverName(), choice.inventorySlot() + 1);
    }

    private int pageCount() { return Math.max(1, (choices.size() + PAGE_SIZE - 1) / PAGE_SIZE); }

    private void changePage(int direction) {
        page = Math.floorMod(page + direction, pageCount());
        selectedIndex = -1;
        clearFocus();
        rebuildWidgets();
    }

    private void cycle(int direction) {
        if (choices.isEmpty()) return;
        int index = Math.floorMod((selectedIndex < 0 ? (direction > 0 ? -1 : 0) : selectedIndex) + direction, choices.size());
        if (page != index / PAGE_SIZE) {
            page = index / PAGE_SIZE;
            rebuildWidgets();
        }
        selectedIndex = index;
        keyboardNavigation = true;
        for (FocusButton button : focusButtons) if (button.index == index) setFocused(button);
        triggerImmediateNarration(false);
    }

    private int choiceAt(double x, double y) {
        for (FocusButton button : focusButtons) if (button.isMouseOver(x, y)) return button.index;
        return -1;
    }

    private double mouseX() {
        return minecraft.mouseHandler.xpos() * width / minecraft.getWindow().getScreenWidth();
    }

    private double mouseY() {
        return minecraft.mouseHandler.ypos() * height / minecraft.getWindow().getScreenHeight();
    }

    @Override public void mouseMoved(double x, double y) {
        keyboardNavigation = false;
        selectedIndex = choiceAt(x, y);
        clearFocus();
        super.mouseMoved(x, y);
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        if (ThaumcraftFocusClient.CHANGE_FOCUS.matchesMouse(button)) return true;
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            onClose();
            return true;
        }
        keyboardNavigation = false;
        return super.mouseClicked(x, y, button);
    }

    @Override public boolean mouseReleased(double x, double y, int button) {
        if (ThaumcraftFocusClient.CHANGE_FOCUS.matchesMouse(button)) {
            focusKeyState(false);
            return true;
        }
        return super.mouseReleased(x, y, button);
    }

    @Override public boolean mouseScrolled(double x, double y, double amountX, double amountY) {
        if (amountY != 0 && !choices.isEmpty()) {
            cycle(amountY < 0 ? 1 : -1);
            return true;
        }
        return super.mouseScrolled(x, y, amountX, amountY);
    }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (ThaumcraftFocusClient.CHANGE_FOCUS.matches(key, scanCode)) return true;
        if (key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE) {
            request(-1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_DOWN
                || key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_UP) {
            cycle(key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_DOWN ? 1 : -1);
            return true;
        }
        keyboardNavigation = true;
        boolean handled = super.keyPressed(key, scanCode, modifiers);
        selectedIndex = getFocused() instanceof FocusButton button ? button.index : -1;
        return handled;
    }

    @Override public boolean keyReleased(int key, int scanCode, int modifiers) {
        if (ThaumcraftFocusClient.CHANGE_FOCUS.matches(key, scanCode)) {
            focusKeyState(false);
            return true;
        }
        return super.keyReleased(key, scanCode, modifiers);
    }

    private final class FocusButton extends AbstractButton {
        private final int index;

        private FocusButton(int index, int x, int y) {
            super(x, y, 20, 20, entryMessage(choices.get(index)));
            this.index = index;
        }

        @Override public void onPress() { request(choices.get(index).inventorySlot()); }

        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            boolean selected = selectedIndex == index;
            if (selected || isHoveredOrFocused()) {
                graphics.fill(getX() - 2, getY() - 2, getRight() + 2, getBottom() + 2, 0xB044315B);
                graphics.renderOutline(getX() - 2, getY() - 2, 24, 24, 0xFFE2C685);
            }
            float scale = selected || isHoveredOrFocused() ? 1.3F : 1F;
            graphics.pose().pushPose();
            graphics.pose().translate(getX() + 10, getY() + 10, 0);
            graphics.pose().scale(scale, scale, 1);
            ItemStack stack = choices.get(index).preview();
            graphics.renderItem(stack, -8, -8);
            graphics.renderItemDecorations(font, stack, -8, -8);
            graphics.pose().popPose();
        }

        @Override public void playDownSound(SoundManager sounds) {}
        @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
    }

    /** Do not predict TC4's server-owned equip/remove cameraticks sound. */
    private static final class QuietButton extends Button {
        private QuietButton(int x, int y, int width, int height, Component message, OnPress press) {
            super(x, y, width, height, message, press, DEFAULT_NARRATION);
        }

        @Override public void playDownSound(SoundManager sounds) {}
    }
}
