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

/** Ordinary core settings, one fluid ghost or six item ghosts; none for Sorting/Guard. An Order Guard shows its four target
 * switches (original GuiGolem:167-189) at the original positions. No client-side filter mutation, upgrade controls or
 * fabricated preview entity: every switch is server state, shown from synced menu data. */
public final class GuiGolem extends AbstractContainerScreen<ContainerGolem> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/guigolem.png");

    public GuiGolem(ContainerGolem menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = ContainerGolem.IMAGE_WIDTH;
        imageHeight = ContainerGolem.IMAGE_HEIGHT;
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
            // Order: a small colour chip under each ghost (grey = any marker, otherwise the dye colour).
            for (int i = 0; i < menu.ghostSlotCount(); i++) {
                int color = menu.slotColor(i);
                int x = leftPos + 100 + i / 2 * 28 + 4, y = topPos + 16 + i % 2 * 31 + 18;
                graphics.fill(x - 1, y - 1, x + 9, y + 6, 0xff373737);
                graphics.fill(x, y, x + 8, y + 5, color < 0 ? 0xff8b8b8b : 0xff000000 | net.minecraft.world.item.DyeColor.byId(color).getTextureDiffuseColor());
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
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
                    leftPos + 4, topPos + 14, leftPos + 40, topPos + 76, 42, 0, mouseX, mouseY, golem);
        }
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int core = menu.core();
        if (menu.pageCount() > 1) {
            // Each Fire upgrade of a Fill/Empty/Gather/Use golem adds a page of six ghosts.
            boolean first = menu.page() == 0, last = menu.page() >= menu.pageCount() - 1;
            graphics.drawString(font, Component.literal("<"), 104, PAGER_Y + 1, first ? 0xff777777 : 0xfffdfdfd, false);
            graphics.drawString(font, Component.literal(">"), 162, PAGER_Y + 1, last ? 0xff777777 : 0xfffdfdfd, false);
            Component label = Component.literal((menu.page() + 1) + "/" + menu.pageCount());
            graphics.drawString(font, label, 141 - font.width(label) / 2, PAGER_Y + 1, 0xfffdfdfd, false);
        }
        if (core >= 0 && core <= 2 || core == 4 || core == 5 || core == 8 || core == 10) {
            // Original80–91: choose the caption from current synced core data.
            graphics.pose().pushPose();
            try {
                graphics.pose().scale(.5F, .5F, 1);
                graphics.drawWordWrap(font, Component.translatable("golemblurb." + core + ".text"), 80, 22, 110, 0xffdddddd);
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

    private static final int PAGER_Y = 72;
    // Original GuiGolem:376-379: Sorting keeps them inside the panel, the others float beside it.
    private int matchX() { return menu.core() == 10 ? 66 : 180; }
    private int matchY(int index) { return 24 + 10 * index + (menu.core() == 10 ? 12 : 0); }

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
            for (int i = 0; i < menu.ghostSlotCount(); i++) {
                if (isHovering(100 + i / 2 * 28 + 4, 16 + i % 2 * 31 + 18, 8, 5, mouseX, mouseY)) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ContainerGolem.COLOR_BUTTON + i);
                    return true;
                }
            }
        }
        if (button == 0 && menu.pageCount() > 1 && minecraft != null && minecraft.gameMode != null) {
            // The page is server state; wait for the synced menu data, never flip it locally.
            if (isHovering(100, PAGER_Y, 12, 10, mouseX, mouseY) && menu.page() > 0) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ContainerGolem.PREVIOUS_PAGE);
                return true;
            }
            if (isHovering(158, PAGER_Y, 12, 10, mouseX, mouseY) && menu.page() < menu.pageCount() - 1) {
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
