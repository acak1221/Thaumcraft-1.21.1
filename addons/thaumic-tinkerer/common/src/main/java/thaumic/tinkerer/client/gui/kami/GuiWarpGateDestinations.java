/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [Jan 10, 2014, 6:08:23 PM (GMT)]
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client.gui.kami;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.architectury.networking.NetworkManager;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import thaumic.tinkerer.common.block.tile.kami.TileWarpGate;
import thaumic.tinkerer.common.item.kami.ItemSkyPearl;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.network.packet.kami.PacketWarpGateTeleport;
import thaumic.tinkerer.common.registry.TTRegistry;

/**
 * The map of the destinations of a Celestial Gateway, opened by sneaking on it: the gateway and the gateways its pearls are
 * attuned to, as dots on a map of the world (x across, z down) that is dragged with the mouse, with a line from the
 * gateway to each destination. Shift and left click on a dot teleports; the number keys 1 to 0 centre the map on the pearl
 * of that slot, space centres it on the gateway.
 *
 * <p>A pearl attuned to another dimension has no place on the map; it is listed on the left like the others, with a note
 * that it is in a different dimension, and shift and left click on its row teleports there (1.7.10 did not offer such
 * pearls at all).</p>
 */
public class GuiWarpGateDestinations extends Screen {

    private static final ResourceLocation ENDER_FIELD = ResourceLocation.withDefaultNamespace("textures/entity/end_portal.png");

    private final TileWarpGate warpGate;
    private ItemStack pearlIcon = ItemStack.EMPTY;

    /** The world coordinates of the top left corner of the map (1.7.10 {@code x}, {@code y}: x and z). */
    private double mapX;
    private double mapY;
    private int ticks;
    private final List<Component> tooltip = new ArrayList<>();
    /** What a frame drew, so that a click goes to the dot or the row that was under the mouse. */
    private final List<Hit> hits = new ArrayList<>();

    private record Hit(int minX, int minY, int maxX, int maxY, int index) {
        boolean contains(double x, double y) {
            return x >= minX && x <= maxX && y >= minY && y < maxY;
        }
    }

    public GuiWarpGateDestinations(TileWarpGate warpGate) {
        super(Component.translatable("ttmisc.destinations"));
        this.warpGate = warpGate;
    }

    private void centreOnGate() {
        mapX = warpGate.getBlockPos().getX() - width / 2;
        mapY = warpGate.getBlockPos().getZ() - height / 2;
    }

    @Override
    protected void init() {
        super.init();
        if (pearlIcon.isEmpty() && TTRegistry.hasItem(LibItemNames.SKY_PEARL)) pearlIcon = TTRegistry.stack(LibItemNames.SKY_PEARL);
        centreOnGate();
    }

    @Override
    public void tick() {
        ++ticks;
        // A gateway that was broken or unloaded has no destinations to show.
        if (warpGate.isRemoved()) onClose();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0) {
            mapX -= dragX;
            mapY -= dragY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_SPACE) {
            centreOnGate();
            return true;
        }
        if (keyCode >= InputConstants.KEY_0 && keyCode <= InputConstants.KEY_9) {
            // 1.7.10 keys 2..11 of the keyboard: the digits 1 to 9 and 0 are the slots 0 to 9.
            int num = keyCode == InputConstants.KEY_0 ? 9 : keyCode - InputConstants.KEY_1;
            ItemStack stack = num < warpGate.getContainerSize() ? warpGate.getItem(num) : ItemStack.EMPTY;
            if (!stack.isEmpty() && ItemSkyPearl.isAttuned(stack) && sameDimension(stack)) {
                mapX = ItemSkyPearl.getX(stack) - width / 2;
                mapY = ItemSkyPearl.getZ(stack) - height / 2;
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean sameDimension(ItemStack stack) {
        return warpGate.getLevel() != null && warpGate.getLevel().dimension().equals(ItemSkyPearl.getDim(stack));
    }

    /** 1.7.10 polled "left button down and shift" while the mouse was over a dot; a click is the same gesture. */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hasShiftDown()) {
            for (Hit hit : hits) {
                if (hit.contains(mouseX, mouseY)) {
                    NetworkManager.sendToServer(new PacketWarpGateTeleport(warpGate, hit.index()));
                    onClose();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        tooltip.clear();
        hits.clear();

        int gateX = (int) (warpGate.getBlockPos().getX() - mapX);
        int gateY = (int) (warpGate.getBlockPos().getZ() - mapY);

        // The pearls that have a place on the map: attuned to this dimension (index, stack, x, y on screen).
        List<int[]> coords = new ArrayList<>();
        List<ItemStack> coordStacks = new ArrayList<>();
        List<Integer> otherDimension = new ArrayList<>();

        for (int i = 0; i < warpGate.getContainerSize(); i++) {
            ItemStack stack = warpGate.getItem(i);
            if (stack.isEmpty() || !ItemSkyPearl.isAttuned(stack)) continue;
            if (!sameDimension(stack)) {
                otherDimension.add(i);
                continue;
            }
            coords.add(new int[] { i, (int) (ItemSkyPearl.getX(stack) - mapX), (int) (ItemSkyPearl.getZ(stack) - mapY) });
            coordStacks.add(stack);
        }

        float alpha = (float) ((Math.sin(ticks / 10D) + 1F) / 4F + 0.25F);
        for (int[] c : coords) drawLine(graphics, gateX, gateY, c[1], c[2], 2F, alpha);

        graphics.drawString(font, Component.translatable("ttmisc.destinations").withStyle(ChatFormatting.UNDERLINE), 3, 40,
                0xFFFFFF, true);
        drawPearlAt(graphics, 0, ItemStack.EMPTY, gateX, gateY, mouseX, mouseY);
        for (int i = 0; i < coords.size(); i++) {
            int[] c = coords.get(i);
            drawPearlAt(graphics, c[0], coordStacks.get(i), c[1], c[2], mouseX, mouseY);
        }
        for (int index : otherDimension) drawOtherDimensionRow(graphics, index, mouseX, mouseY);

        if (!tooltip.isEmpty()) graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);

        graphics.drawCenteredString(font, Component.translatable("ttmisc.numberKeys"), width / 2, 5, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("ttmisc.spaceToReset"), width / 2, 16, 0xFFFFFF);
    }

    /** The name of a destination: the pearl's own name when it has one, else "Unnamed Destination" (the gateway: "Entrance Point"). */
    private static Component destinationName(ItemStack stack) {
        if (stack.isEmpty()) return Component.translatable("ttmisc.entrancePoint");
        Component custom = stack.get(DataComponents.CUSTOM_NAME);
        return custom != null ? custom : Component.translatable("ttmisc.destination");
    }

    /** 1.7.10 {@code drawPearlAt}: the dot of a destination (or of the gateway), its row in the list, its tooltip. */
    public void drawPearlAt(GuiGraphics graphics, int index, ItemStack stack, int xp, int yp, int mx, int my) {
        int x = (int) (xp + mapX);
        int y = (int) (yp + mapY);

        graphics.pose().pushPose();
        graphics.pose().translate(xp, yp, 0);
        graphics.pose().scale(0.5F, 0.5F, 1F);
        if (!pearlIcon.isEmpty()) graphics.renderFakeItem(pearlIcon, -8, -8);
        graphics.pose().popPose();

        Component destName = destinationName(stack);
        Component destNum = Component.literal(" ")
                .append(Component.translatable("ttmisc.destinationInd", index + 1).withStyle(ChatFormatting.ITALIC));

        if (!stack.isEmpty()) {
            graphics.drawString(font, Component.literal((index + 1) + ": ").append(destName), 5, 54 + index * 11, 0xFFFFFF, false);
        }

        boolean hovered = mx >= xp - 4 && mx <= xp + 4 && my >= yp - 4 && my < yp + 4;
        if (hovered) {
            tooltip.add(Component.empty().append(destName).append(destNum).withStyle(ChatFormatting.AQUA));

            if (!stack.isEmpty()) {
                ItemSkyPearl.addInfo(stack, warpGate.getLevel().dimension(), Vec3.atLowerCornerOf(warpGate.getBlockPos()),
                        tooltip, true);
                tooltip.add(Component.translatable("ttmisc.clickToTeleport"));
            } else {
                tooltip.add(Component.literal("X: " + x));
                tooltip.add(Component.literal("Z: " + y));
            }
        }
        if (!stack.isEmpty()) hits.add(new Hit(xp - 4, yp - 4, xp + 4, yp + 4, index));
    }

    /** A pearl of another dimension: only a row of the list (name, "Different Dimension" and the dimension), no dot. */
    private void drawOtherDimensionRow(GuiGraphics graphics, int index, int mx, int my) {
        ItemStack stack = warpGate.getItem(index);
        Component destName = destinationName(stack);
        Component row = Component.literal((index + 1) + ": ").append(destName).append(" ")
                .append(Component.translatable("ttmisc.differentDim").withStyle(ChatFormatting.RED));
        int rowY = 54 + index * 11;
        graphics.drawString(font, row, 5, rowY, 0xFFFFFF, false);
        Hit hit = new Hit(5, rowY, 5 + font.width(row), rowY + 10, index);
        hits.add(hit);
        if (hit.contains(mx, my)) {
            tooltip.add(Component.empty().append(destName)
                    .append(Component.literal(" ").append(Component.translatable("ttmisc.destinationInd", index + 1)
                            .withStyle(ChatFormatting.ITALIC)))
                    .withStyle(ChatFormatting.AQUA));
            ItemSkyPearl.addInfo(stack, warpGate.getLevel().dimension(), Vec3.atLowerCornerOf(warpGate.getBlockPos()),
                    tooltip, true);
            tooltip.add(Component.literal(String.valueOf(ItemSkyPearl.getDim(stack).location())).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("ttmisc.clickToTeleport"));
        }
    }

    /** A line of the given width and translucency, drawn as a quad (1.7.10: {@code GL_LINES} of width 2, white). */
    private static void drawLine(GuiGraphics graphics, float x1, float y1, float x2, float y2, float lineWidth, float alpha) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 0.001F) return;
        float nx = -dy / length * lineWidth / 2F;
        float ny = dx / length * lineWidth / 2F;
        Matrix4f pose = graphics.pose().last().pose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.addVertex(pose, x1 + nx, y1 + ny, 0F).setColor(1F, 1F, 1F, alpha);
        buffer.addVertex(pose, x2 + nx, y2 + ny, 0F).setColor(1F, 1F, 1F, alpha);
        buffer.addVertex(pose, x2 - nx, y2 - ny, 0F).setColor(1F, 1F, 1F, alpha);
        buffer.addVertex(pose, x1 - nx, y1 - ny, 0F).setColor(1F, 1F, 1F, alpha);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }

    /**
     * 1.7.10 {@code drawDefaultBackground}: the texture of the End portal, tiled over the screen and tinted with a hue that
     * drifts with {@code sin(ticks / 150)}, at saturation 0.5 and brightness 0.4.
     */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float hue = (float) (Math.sin(ticks / 150D) + 1F / 2F);
        int rgb = Color.HSBtoRGB(hue, 0.5F, 0.4F);
        graphics.setColor(((rgb >> 16) & 0xFF) / 255F, ((rgb >> 8) & 0xFF) / 255F, (rgb & 0xFF) / 255F, 1F);
        graphics.blit(ENDER_FIELD, 0, 0, 0, 0F, 0F, width, height, 256, 256);
        graphics.setColor(1F, 1F, 1F, 1F);
    }
}
