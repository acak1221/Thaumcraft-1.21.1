package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.common.container.ContainerGolem;
import thaumcraft.common.items.golems.GolemGuardTargets;
import thaumcraft.common.items.golems.GolemItemMatch;
import thaumcraft.common.items.golems.GolemUpgrade;
import thaumcraft.common.lib.utils.Utils;

/** Ordinary core settings, one fluid ghost or six item ghosts; none for Sorting/Guard. An Order Guard shows its four target
 * switches (original GuiGolem:167-189) at the original positions. No client-side filter mutation, upgrade controls or
 * fabricated preview entity: every switch is server state, shown from synced menu data. */
public final class GuiGolem extends AbstractContainerScreen<ContainerGolem> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/guigolem.png");
    /** Original GuiGolem:74-77 — an advanced golem shows a random threat line in one case of four. */
    private int threat = -1;
    private boolean threatDecided;

    public GuiGolem(ContainerGolem menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = ContainerGolem.IMAGE_WIDTH;
        imageHeight = ContainerGolem.IMAGE_HEIGHT;
    }

    /**
     * Original GuiGolem:74-77 — the threat line of an advanced golem is drawn once, when the menu
     * opens: one case of four the golem says something else instead of its core's blurb.
     */
    private int threat() {
        if (!threatDecided) {
            threatDecided = true;
            var golem = menu.golem();
            if (golem != null && golem.isAdvanced() && golem.level().getRandom().nextInt(4) == 0) {
                threat = golem.level().getRandom().nextInt(9);
            }
        }
        return threat;
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.setColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        var golem = menu.golem();
        int materialFrame = golem == null ? 0 : Math.max(0, Math.min(7, golem.getGolemMaterial())) * 24;
        for (int i = 0; i < menu.ghostSlotCount(); i++) {
            graphics.blit(BACKGROUND, leftPos + 96 + i / 2 * 28, topPos + 12 + i % 2 * 31, 184, materialFrame, 24, 24, 256, 256);
        }
        if (menu.canColorSlots()) {
            // Order: the original draws a 24x12 bar above every ghost slot and a 6x6 colour square in its middle,
            // tinted with the original palette (GuiGolem:134-145). The bar itself is transparent in the texture,
            // the square is a grey bevel that the tint turns into the marker colour.
            for (int i = 0; i < menu.ghostSlotCount(); i++) {
                graphics.blit(BACKGROUND, leftPos + 96 + i / 2 * 28, topPos + 4 + i % 2 * 31, 72, 168, 24, 12, 256, 256);
                int color = menu.slotColor(i);
                if (color < 0) continue;
                int rgb = Utils.colors[color];
                graphics.setColor(((rgb >> 16) & 0xFF) / 255F, ((rgb >> 8) & 0xFF) / 255F, (rgb & 0xFF) / 255F, 1F);
                graphics.blit(BACKGROUND, leftPos + 105 + i / 2 * 28, topPos + 7 + i % 2 * 31, 0, 176, 6, 6, 256, 256);
                graphics.setColor(1, 1, 1, 1);
            }
        }
        if (menu.canSwitchGuardTargets()) {
            for (int i = 0; i < GolemGuardTargets.BUTTON_COUNT; i++) {
                graphics.blit(BACKGROUND, leftPos + 104, topPos + 5 + 16 * i, 8, 168, 8, 8, 256, 256);
                if (GolemGuardTargets.allows(menu.guardTargets(), 1 << i)) graphics.blit(BACKGROUND, leftPos + 104, topPos + 5 + 16 * i, 8, 176, 8, 8, 256, 256);
            }
        }
        if (menu.core() == 0) {
            graphics.blit(BACKGROUND, leftPos + 62, topPos + 54, 8, 168, 8, 8, 256, 256);
            // Original190–202: checked means precise (any-amount is false).
            if (!menu.isFillAnyAmount()) graphics.blit(BACKGROUND, leftPos + 62, topPos + 54, 8, 176, 8, 8, 256, 256);
        }
        if (menu.pageCount() > 1) {
            // Original GuiGolem:154-165: the page arrows are texture arrows, the dimmed one is the disabled end.
            boolean first = menu.page() == 0, last = menu.page() >= menu.pageCount() - 1;
            graphics.blit(BACKGROUND, leftPos + 111, topPos + 68, 0, first ? 200 : 208, 24, 8, 256, 256);
            graphics.blit(BACKGROUND, leftPos + 135, topPos + 68, 24, last ? 200 : 208, 24, 8, 256, 256);
        }
        if (menu.canSwitchItemMatch()) {
            // Original GuiGolem:376-389: three boxes at the Sorting corner or beside the panel; a lit box is a switch that is on.
            for (int i = 0; i < GolemItemMatch.BUTTON_COUNT; i++) {
                graphics.blit(BACKGROUND, leftPos + matchX(), topPos + matchY(i), 8, 168, 8, 8, 256, 256);
                if ((menu.itemMatch() & (1 << i)) != 0) graphics.blit(BACKGROUND, leftPos + matchX(), topPos + matchY(i), 8, 176, 8, 8, 256, 256);
            }
        }
        RenderSystem.disableBlend();
        if (golem != null && golem.isAlive()) {
            // Native helper saves/restores body/head rotations and render state.
            // It renders this real entity; there is no tick/AI or NoAI change.
            // Original GuiGolem:267-317 draws the golem in the 320x240 viewport of the screen centre, which is the
            // centre of the panel: the preview belongs there, not in the top left corner.
            // Original GuiGolem:289-303 — the preview is drawn with the body yaw of -20 and the head pitch of -5
            // and it does NOT follow the mouse; the rotations are set for the frame and restored afterwards.
            float body = golem.yBodyRot, bodyOld = golem.yBodyRotO;
            float head = golem.yHeadRot, headOld = golem.yHeadRotO;
            float yaw = golem.getYRot();
            golem.yBodyRot = -20F;
            golem.yBodyRotO = -5F;
            golem.yHeadRot = 0F;
            golem.yHeadRotO = -5F;
            golem.setYRot(0F);
            try {
                // The mouse is pinned to the centre of the box, so the preview keeps the original's fixed pose.
                InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
                        leftPos + 63, topPos + 52, leftPos + 113, topPos + 114, 42,
                        0F, (leftPos + 63 + leftPos + 113) / 2.0F, (topPos + 52 + topPos + 114) / 2.0F, golem);
            } finally {
                golem.yBodyRot = body;
                golem.yBodyRotO = bodyOld;
                golem.yHeadRot = head;
                golem.yHeadRotO = headOld;
                golem.setYRot(yaw);
            }
        }
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int core = menu.core();
        if (menu.pageCount() > 1) {
            // Original GuiGolem:88-91: the page counter is half-size text on the right of the panel.
            graphics.pose().pushPose();
            try {
                graphics.pose().scale(.5F, .5F, 1);
                Component label = Component.literal((menu.page() + 1) + "/" + menu.pageCount());
                graphics.drawString(font, label, 323, 140, 0xfffdfdfd, false);
            } finally { graphics.pose().popPose(); }
        }
        if (menu.canColorSlots() && showColorNames()) {
            // Original GuiGolem:102-114: the colour name of the hovered colour bar, above the panel.
            for (int i = 0; i < menu.ghostSlotCount(); i++) {
                int overX = mouseX - (leftPos + 96 + i / 2 * 28);
                int overY = mouseY - (topPos + 4 + i % 2 * 31);
                if (overX < 0 || overY < 0 || overX >= 24 || overY >= 12) continue;
                int color = menu.slotColor(i);
                Component name = color < 0 ? Component.translatable("tc.port.golem.color.any")
                        : Component.translatable("tc.port.golem.color." + color);
                graphics.drawString(font, name, leftPos + 133 - font.width(name) / 2, topPos - 6, 0xfffdfdfd, false);
                break;
            }
        }
        if (core >= 0) {
            // Original80–91: the core's blurb, or the threat line of an advanced golem.
            graphics.pose().pushPose();
            try {
                graphics.pose().scale(.5F, .5F, 1);
                int line = threat();
                Component blurb = line >= 0 ? Component.translatable("golemthreat." + line + ".text")
                        : Component.translatable("golemblurb." + core + ".text");
                graphics.drawWordWrap(font, blurb, 80, 22, 110, 0xffdddddd);
            } finally { graphics.pose().popPose(); }
        }
        if (core == 0) {
            // Original mode semantics/half-size placement, localized for native language selection.
            Component text = Component.translatable(menu.isFillAnyAmount() ? "tc.port.golem.any" : "tc.port.golem.precise");
            graphics.pose().pushPose();
            try {
                graphics.pose().translate(66, 48, 0);
                graphics.pose().scale(.5F, .5F, 1);
                graphics.drawString(font, text, -font.width(text) / 2, 0, 0xfffdfdfd, false);
            } finally { graphics.pose().popPose(); }
        }
        if (menu.canSwitchItemMatch()) {
            // Half-size captions beside the boxes (original GuiGolem:380-390), bright while the switch is on.
            String[] keys = {"tc.port.golem.match.tags", "tc.port.golem.match.damage", "tc.port.golem.match.components"};
            for (int i = 0; i < keys.length; i++) {
                graphics.pose().pushPose();
                try {
                    graphics.pose().translate(matchX() + 10, matchY(i) + 2, 0);
                    graphics.pose().scale(.5F, .5F, 1);
                    graphics.drawString(font, Component.translatable(keys[i]), 0, 0, (menu.itemMatch() & (1 << i)) != 0 ? 0xfffdfdfd : 0xff666666, false);
                } finally { graphics.pose().popPose(); }
            }
        }
        if (core == 4 && menu.canSwitchGuardTargets()) {
            // Original colours; full-size labels at x=122 like GuiGolem:185-188, localised.
            graphics.drawString(font, Component.translatable("tc.port.golem.guard.monsters"), 122, 6, 0xffffcccc, false);
            graphics.drawString(font, Component.translatable("tc.port.golem.guard.animals"), 122, 22, 0xffffffcc, false);
            graphics.drawString(font, Component.translatable("tc.port.golem.guard.players"), 122, 38, 0xffccccff, false);
            graphics.drawString(font, Component.translatable("tc.port.golem.guard.creepers"), 122, 54, 0xffccffcc, false);
        } else if (core == 4) {
            graphics.pose().pushPose();
            try {
                graphics.pose().translate(96, 14, 0);
                graphics.pose().scale(.5F, .5F, 1);
                graphics.drawWordWrap(font, Component.translatable("tc.port.golem.guard_baseline"), 0, 0, 136, 0xffdddddd);
            } finally { graphics.pose().popPose(); }
        }
        if (core == 5) {
            graphics.pose().pushPose();
            try {
                graphics.pose().translate(96, 43, 0);
                graphics.pose().scale(.5F, .5F, 1);
                graphics.drawWordWrap(font, Component.translatable("tc.port.golem.water_filter"), 0, 0, 136, 0xffdddddd);
                var golem = menu.golem();
                if (golem != null) graphics.drawString(font,
                        Component.translatable("tooltip.thaumcraft.golem.water_cargo", golem.getWaterAmountMb()), 0, 54, 0xffdddddd, false);
            } finally { graphics.pose().popPose(); }
        }
        if (core == 8) {
            // The first native unit supports DEFAULT only. These ordinary modes
            // are pending, not upgrade-gated; no working-looking fake toggle.
            graphics.pose().pushPose();
            try {
                graphics.pose().translate(42, 44, 0);
                graphics.pose().scale(.5F, .5F, 1);
                graphics.drawWordWrap(font, Component.translatable("tc.port.golem.use_default"), 0, 0, 106, 0xffdddddd);
            } finally { graphics.pose().popPose(); }
        }
    }

    // Original GuiGolem:376-379: Sorting keeps them inside the panel, the others float beside it.
    private int matchX() { return menu.core() == 10 ? 66 : 180; }
    private int matchY(int index) { return 24 + 10 * index + (menu.core() == 10 ? 12 : 0); }

    /**
     * Original GuiGolem:102 — the colour name above the panel is only shown when the golem carries the Entropy
     * upgrade (the original's {@code getUpgradeAmount(5)}), whatever the Order colour chip does.
     */
    private boolean showColorNames() {
        var golem = menu.golem();
        return golem != null && golem.getUpgradeAmount(GolemUpgrade.ENTROPY) > 0;
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && menu.canSwitchGuardTargets() && minecraft != null && minecraft.gameMode != null) {
            // The switch is server state; wait for the synced menu data, never flip it locally.
            for (int i = 0; i < GolemGuardTargets.BUTTON_COUNT; i++) {
                if (isHovering(104, 5 + 16 * i, 66, 8, mouseX, mouseY)) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ContainerGolem.GUARD_BUTTON + i);
                    return true;
                }
            }
        }
        if (button == 0 && menu.canSwitchItemMatch() && minecraft != null && minecraft.gameMode != null) {
            // Server state again: wait for the synced menu data, never flip a switch locally.
            for (int i = 0; i < GolemItemMatch.BUTTON_COUNT; i++) {
                if (isHovering(matchX(), matchY(i), 64, 8, mouseX, mouseY)) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ContainerGolem.ITEM_MATCH_BUTTON + i);
                    return true;
                }
            }
        }
        if (button == 0 && menu.canColorSlots() && minecraft != null && minecraft.gameMode != null) {
            // The colour is server state; wait for the synced menu data, never recolour locally.
            // Original GuiGolem:328-342: the left third of the colour bar lowers the colour of that slot, the right
            // third raises it; the button ids are the golem's own slot ids (below the slot count) and the slot count
            // plus the slot id (above it), exactly as the original menu expects them.
            int slotBase = menu.page() * ContainerGolem.GHOSTS;
            for (int i = 0; i < menu.ghostSlotCount(); i++) {
                if (isHovering(96 + i / 2 * 28, 4 + i % 2 * 31, 8, 12, mouseX, mouseY)) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, slotBase + i);
                    return true;
                }
                if (isHovering(96 + 16 + i / 2 * 28, 4 + i % 2 * 31, 8, 12, mouseX, mouseY)) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, menu.slotCount() + slotBase + i);
                    return true;
                }
            }
        }
        if (button == 0 && menu.pageCount() > 1 && minecraft != null && minecraft.gameMode != null) {
            // The page is server state; wait for the synced menu data, never flip it locally.
            if (isHovering(111, 68, 24, 8, mouseX, mouseY) && menu.page() > 0) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ContainerGolem.PREVIOUS_PAGE);
                return true;
            }
            if (isHovering(135, 68, 24, 8, mouseX, mouseY) && menu.page() < menu.pageCount() - 1) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ContainerGolem.NEXT_PAGE);
                return true;
            }
        }
        if (button == 0 && menu.core() == 0 && isHovering(62, 54, 8, 8, mouseX, mouseY)) {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 50);
            return true; // Wait for server menu data; never flip local mode.
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderWantedCounts(GuiGraphics graphics) {
        if (menu.core() != 0 || !menu.hasGhostFilters()) return;
        for (int i = 0; i < ContainerGolem.GHOSTS; i++) {
            var slot = menu.getSlot(i);
            var exemplar = slot.getItem();
            if (!exemplar.isEmpty()) {
                // A logical stock target up to256, never an oversized stack.
                graphics.renderItemDecorations(font, exemplar, leftPos + slot.x, topPos + slot.y,
                        Integer.toString(menu.getWantedCount(i)));
            }
        }
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderWantedCounts(graphics);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
