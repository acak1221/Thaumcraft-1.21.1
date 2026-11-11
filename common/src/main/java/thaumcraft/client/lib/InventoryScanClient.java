package thaumcraft.client.lib;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.client.gui.HoveredSlotAspectRow;
import thaumcraft.common.items.relics.ItemThaumometer;
import thaumcraft.common.lib.inventoryscan.InventoryScanning;
import thaumcraft.common.lib.inventoryscan.ScanReply;
import thaumcraft.common.lib.inventoryscan.ScanRequest;
import thaumcraft.common.mixin.client.AbstractContainerScreenAccess;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.EntityAspectBindings;
import thaumcraft.common.lib.research.ObjectAspectResolver;
import thaumcraft.common.lib.research.ScanManager;

/**
 * Original cursor-hover GUI, with correlated server-owned timing and rewards.
 *
 * <p>Moved into the mod from its companion add-on Thaumic Inventory Scanning for Thaumcraft 4 Reload;
 * the setting {@code inventory_scanning} switches it off.</p>
 */
public final class InventoryScanClient {
    private static final ResourceLocation BACK = texture("_back");
    private static final ResourceLocation UNKNOWN = texture("_unknown");
    private static final SoundEvent CAMERA = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "cameraticks"));
    private static final Session SESSION = new Session(ignored -> false);
    private static Screen observedScreen;
    private static Target target;
    private static ItemStack targetStack = ItemStack.EMPTY;
    private static ItemStack cursorStack = ItemStack.EMPTY;

    private InventoryScanClient() {}

    /** Physical client only: the channel, the tick, the tooltip of the thaumometer and the disconnect reset. */
    public static void init() {
        installTransport(request -> {
            if (!dev.architectury.networking.NetworkManager.canServerReceive(ScanRequest.TYPE)) return false;
            dev.architectury.networking.NetworkManager.sendToServer(request);
            return true;
        });
        InventoryScanning.initClient(InventoryScanClient::accept);
        dev.architectury.event.events.client.ClientTickEvent.CLIENT_POST.register(client -> tick());
        dev.architectury.event.events.client.ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> disconnect());
        // Search trees build tooltips off the render thread, where the keyboard cannot be read.
        dev.architectury.event.events.client.ClientTooltipEvent.ITEM.register((stack, lines, context, flag) ->
                appendTooltips(stack, lines, Minecraft.getInstance().isSameThread() && Screen.hasShiftDown()));
    }

    public static void installTransport(Predicate<ScanRequest> transport) {
        SESSION.transport = Objects.requireNonNull(transport);
    }

    public static void tick() {
        var minecraft = Minecraft.getInstance();
        if (!InventoryScanning.enabled()) {
            if (target != null) clearTarget();
            return;
        }
        if (observedScreen != minecraft.screen) {
            clearTarget();
            observedScreen = minecraft.screen;
        }
        var candidate = select(minecraft.screen, mouseX(minecraft), mouseY(minecraft));
        if (candidate == null) {
            if (target != null) clearTarget();
            return;
        }
        if (!candidate.target.equals(target) || !ItemStack.matches(candidate.stack, targetStack)
                || !ItemStack.matches(candidate.cursor, cursorStack)) {
            target = candidate.target;
            targetStack = candidate.stack.copy();
            cursorStack = candidate.cursor.copy();
            SESSION.begin(target.menu.containerId, target.slot);
        }
        SESSION.tick();
        if (SESSION.cameraTick() && minecraft.player != null && minecraft.level != null) {
            var player = minecraft.player;
            minecraft.level.playLocalSound(player.getX(), player.getY(), player.getZ(), CAMERA, SoundSource.PLAYERS,
                    .2F, .45F + minecraft.level.random.nextFloat() * .1F, false);
        }
    }

    public static void accept(ScanReply reply) { SESSION.accept(reply); }

    public static void disconnect() {
        clearTarget();
        observedScreen = null;
    }

    /** Called by loader-native item-tooltip events, including their asynchronous search-tree callers. */
    public static void appendTooltips(ItemStack stack, List<Component> lines, boolean shift) {
        if (!(stack.getItem() instanceof ItemThaumometer) || !InventoryScanning.enabled()) return;
        lines.add(Component.translatable("tc.port.inventoryscan.thaumometerTooltip").withStyle(ChatFormatting.GOLD));
        if (shift) {
            lines.add(Component.translatable("tc.port.inventoryscan.thaumometerTooltipMore1").withStyle(ChatFormatting.DARK_AQUA));
            lines.add(Component.translatable("tc.port.inventoryscan.thaumometerTooltipMore2").withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    public static void render(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
        var minecraft = Minecraft.getInstance();
        if (!InventoryScanning.enabled()) return;
        if (!(screen instanceof AbstractContainerScreen<?> container) || minecraft.player == null
                || !(container.getMenu().getCarried().getItem() instanceof ItemThaumometer)) return;
        var visible = ((AbstractContainerScreenAccess) container).thaumcraft$findSlot(mouseX, mouseY);
        if (container instanceof CreativeModeInventoryScreen) {
            // Vanilla creative clicks synchronize slots, not the carried cursor. Keep the
            // server's real cursor requirement and explain this unsupported screen honestly.
            var lines = visible != null && visible.hasItem()
                    ? new ArrayList<>(Screen.getTooltipFromItem(minecraft, visible.getItem())) : new ArrayList<Component>();
            lines.add(Component.translatable("tc.port.inventoryscan.creativeInventoryUnsupported").withStyle(ChatFormatting.GOLD));
            graphics.renderComponentTooltip(minecraft.font, lines, mouseX, mouseY);
            return;
        }
        var candidate = select(screen, mouseX, mouseY);
        boolean portrait = portrait(container, mouseX, mouseY);
        graphics.pose().pushPose();
        try {
            // Vanilla suppresses hovered-item tooltips while carrying an item. Restore exactly that tooltip.
            if (!portrait && visible != null && visible.hasItem()) {
                graphics.renderTooltip(minecraft.font, visible.getItem(), mouseX, mouseY);
                drawItemAspects(graphics, visible.getItem(), mouseX, mouseY);
            } else if (portrait) drawSelfAspects(graphics, minecraft.player, mouseX, mouseY);
            Component label = null;
            if (candidate != null && candidate.target.equals(target) && ItemStack.matches(candidate.stack, targetStack)
                    && ItemStack.matches(candidate.cursor, cursorStack)) {
                if (!SESSION.available) label = Component.translatable("tc.port.inventoryscan.serverUnavailable");
                else if (SESSION.status == ScanReply.SCANNING && SESSION.acknowledged)
                    label = Component.translatable("tc.port.inventoryscan.scanning").append(Session.dots(SESSION.progress));
            }
            if (label != null) {
                graphics.pose().translate(0, 0, 600);
                graphics.drawString(minecraft.font, label.copy().withStyle(ChatFormatting.GOLD), mouseX, mouseY - 30, 0xFFFFFFFF, true);
            }
        } finally { graphics.pose().popPose(); }
    }

    private static Candidate select(Screen screen, double mouseX, double mouseY) {
        var player = Minecraft.getInstance().player;
        if (!(screen instanceof AbstractContainerScreen<?> container) || screen instanceof CreativeModeInventoryScreen
                || player == null || player.isSpectator()) return null;
        var cursor = container.getMenu().getCarried();
        if (!(cursor.getItem() instanceof ItemThaumometer)) return null;
        if (portrait(container, mouseX, mouseY)) {
            if (container.getMenu() != player.inventoryMenu || player.containerMenu != player.inventoryMenu) return null;
            var self = EntityAspectBindings.describe(player);
            if (self != null && ScanManager.hasBeenScanned(KnowledgeAccess.get(player), self.signature(), ScanManager.ScanKind.ENTITY))
                return null; // Original isValidScanTarget rejects duplicates before timer/camera ticks; the aspect row stays independent.
            return new Candidate(new Target(container, player.inventoryMenu, -1), ItemStack.EMPTY, cursor);
        }
        var visible = ((AbstractContainerScreenAccess) container).thaumcraft$findSlot(mouseX, mouseY);
        if (visible == null) return null;
        var menu = container.getMenu();
        var slot = visible;
        if (menu != player.containerMenu) return null;
        if (!eligible(slot, player) || slot.index < 0 || slot.index >= menu.slots.size()
                || menu.slots.get(slot.index) != slot) return null;
        return new Candidate(new Target(container, menu, slot.index), slot.getItem(), cursor);
    }

    private static boolean eligible(Slot slot, Player player) {
        return slot != null && slot.isActive() && slot.hasItem() && slot.mayPickup(player) && !(slot instanceof ResultSlot);
    }

    private static boolean portrait(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        if (!(screen instanceof InventoryScreen)) return false;
        var access = (AbstractContainerScreenAccess) screen;
        double x = mouseX - access.thaumcraft$leftPos(), y = mouseY - access.thaumcraft$topPos();
        return x >= 26 && x < 78 && y >= 8 && y < 78;
    }

    private static void clearTarget() {
        SESSION.cancel();
        target = null;
        targetStack = ItemStack.EMPTY;
        cursorStack = ItemStack.EMPTY;
    }

    private static double mouseX(Minecraft minecraft) {
        return minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getScreenWidth();
    }

    private static double mouseY(Minecraft minecraft) {
        return minecraft.mouseHandler.ypos() * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getScreenHeight();
    }

    private static void drawItemAspects(GuiGraphics graphics, ItemStack stack, int mouseX, int mouseY) {
        var player = Minecraft.getInstance().player;
        if (player == null || !HoveredSlotAspectRow.isScanned(KnowledgeAccess.get(player).getScannedItems(),
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) return;
        drawAspects(graphics, ObjectAspectResolver.get(stack, player.level()), mouseX, mouseY, false);
    }

    private static void drawSelfAspects(GuiGraphics graphics, Player player, int mouseX, int mouseY) {
        var self = EntityAspectBindings.describe(player);
        if (self == null || !ScanManager.hasBeenScanned(KnowledgeAccess.get(player), self.signature(), ScanManager.ScanKind.ENTITY)) return;
        drawAspects(graphics, self.aspects(), mouseX, mouseY, true);
    }

    /** Original forced row: no Shift/config gate, and undiscovered aspect images remain concealed. */
    private static void drawAspects(GuiGraphics graphics, AspectList list, int mouseX, int mouseY, boolean self) {
        var minecraft = Minecraft.getInstance();
        if (list == null || list.size() == 0 || minecraft.player == null) return;
        var knowledge = KnowledgeAccess.get(minecraft.player);
        graphics.flush();
        boolean depth = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0, 0, 500);
            RenderSystem.disableDepthTest();
            int index = self ? 1 : 0; // Original self row increments X before its first icon.
            for (Aspect aspect : list.getAspectsSortedAmount()) {
                if (aspect == null) continue;
                int x = HoveredSlotAspectRow.iconX(mouseX, index++) + HoveredSlotAspectRow.rowShift();
                int y = HoveredSlotAspectRow.iconY(mouseY) + HoveredSlotAspectRow.rowShift();
                graphics.pose().pushPose();
                try {
                    graphics.pose().translate(x - 2, y - 2, 0);
                    graphics.pose().scale(1.25F, 1.25F, 1);
                    blit(graphics, BACK, 0, 0, 0xFFFFFF);
                } finally { graphics.pose().popPose(); }
                if (knowledge.hasDiscoveredAspect(aspect.getTag())) {
                    blit(graphics, aspect.getImage(), x, y, aspect.getColor());
                    graphics.pose().pushPose();
                    try {
                        graphics.pose().translate(x, y, 0);
                        graphics.pose().scale(.5F, .5F, 1);
                        String amount = Integer.toString(list.getAmount(aspect));
                        int tx = 32 - minecraft.font.width(amount), ty = 32 - minecraft.font.lineHeight;
                        graphics.drawString(minecraft.font, amount, tx - 1, ty, 0xFF000000, false);
                        graphics.drawString(minecraft.font, amount, tx + 1, ty, 0xFF000000, false);
                        graphics.drawString(minecraft.font, amount, tx, ty - 1, 0xFF000000, false);
                        graphics.drawString(minecraft.font, amount, tx, ty + 1, 0xFF000000, false);
                        graphics.drawString(minecraft.font, amount, tx, ty, 0xFFFFFFFF, false);
                    } finally { graphics.pose().popPose(); }
                } else blit(graphics, UNKNOWN, x, y, 0xFFFFFF);
            }
        } finally {
            graphics.flush();
            graphics.pose().popPose();
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
        }
    }

    private static void blit(GuiGraphics graphics, ResourceLocation texture, int x, int y, int color) {
        graphics.flush();
        graphics.setColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try { graphics.blit(texture, x, y, 0, 0, 16, 16, 16, 16); }
        finally { graphics.setColor(1, 1, 1, 1); RenderSystem.disableBlend(); }
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/aspects/" + name + ".png");
    }

    private record Target(AbstractContainerScreen<?> screen, AbstractContainerMenu menu, int slot) {}
    private record Candidate(Target target, ItemStack stack, ItemStack cursor) {}

    /** No Minecraft singleton or GUI state: packet correlation and heartbeat behavior are independently testable. */
    static final class Session {
        private Predicate<ScanRequest> transport;
        private int serial, container, slot, request, heartbeat, elapsed;
        private boolean active;
        boolean acknowledged;
        boolean available = true;
        int progress, status = ScanReply.CANCELLED;

        Session(Predicate<ScanRequest> transport) { this.transport = transport; }

        static String dots(int progress) {
            float fraction = (float) progress / InventoryScanning.SCAN_TICKS;
            return fraction >= .75F ? "..." : fraction >= .5F ? ".." : fraction >= .25F ? "." : "";
        }

        void begin(int container, int slot) {
            cancel();
            if (serial == Integer.MAX_VALUE) { available = false; return; }
            this.container = container; this.slot = slot; request = ++serial;
            heartbeat = elapsed = progress = 0;
            status = ScanReply.SCANNING; acknowledged = false; active = true;
            available = transport.test(new ScanRequest(container, slot, request, true));
            if (!available) status = ScanReply.CANCELLED;
        }

        void tick() {
            if (!active || !available || status != ScanReply.SCANNING) return;
            elapsed++;
            if (++heartbeat >= InventoryScanning.HEARTBEAT_TICKS) {
                heartbeat = 0;
                if (!transport.test(new ScanRequest(container, slot, request, true))) {
                    available = false; status = ScanReply.CANCELLED;
                }
            }
        }

        void accept(ScanReply reply) {
            if (!active || !available || reply.requestId() != request || status != ScanReply.SCANNING
                    || reply.status() < ScanReply.SCANNING || reply.status() > ScanReply.CANCELLED) return;
            acknowledged = true;
            progress = Math.max(progress, Math.clamp(reply.progress(), 0, InventoryScanning.SCAN_TICKS));
            status = reply.status();
        }

        boolean cameraTick() { return active && available && acknowledged && status == ScanReply.SCANNING && elapsed >= 3; }

        void cancel() {
            if (active && available && status == ScanReply.SCANNING)
                transport.test(new ScanRequest(container, slot, request, false));
            active = acknowledged = false;
            elapsed = heartbeat = progress = 0;
            status = ScanReply.CANCELLED;
            if (serial < Integer.MAX_VALUE) serial++; // Screens/disconnects also invalidate delayed receipts; never reuse an ID.
        }
    }
}
