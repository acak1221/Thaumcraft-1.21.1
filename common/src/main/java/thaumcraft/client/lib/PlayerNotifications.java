package thaumcraft.client.lib;

import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;
import dev.architectury.platform.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.WarpNotificationRules;
import thaumcraft.common.lib.network.WarpMessageNetworking;
import thaumcraft.common.lib.network.WarpMessagePayload;

/** Actual original lower-right half-scale notification HUD and requested-delta whispers consumer. */
public final class PlayerNotifications {
    private static final ResourceLocation PARTICLES = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/particles.png");
    private static final SoundEvent WHISPERS = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "whispers"));
    private static NotificationQueue queue = new NotificationQueue(5000);
    private static int maximum = 15;
    private static Object level;
    private static boolean initialized;
    private PlayerNotifications() {}
    private static long now() { return System.nanoTime() / 1_000_000L; }
    public static void init() {
        if (initialized) return;
        initialized = true;
        var file = Platform.getConfigFolder().resolve("thaumcraft-client.properties");
        try {
            var settings = new Properties();
            if (Files.exists(file)) {
                try (var reader = Files.newBufferedReader(file)) { settings.load(reader); }
            } else {
                Files.createDirectories(file.getParent());
                Files.writeString(file, "# Original TC4 client notification timing and number of visible rows.\nnotification_delay=5000\nnotification_max=15\n");
            }
            int delay = Integer.parseInt(settings.getProperty("notification_delay", "5000"));
            int rows = Integer.parseInt(settings.getProperty("notification_max", "15"));
            if (rows < 0) throw new IllegalArgumentException("Negative notification count");
            queue = new NotificationQueue(delay); maximum = rows;
        } catch (IOException | IllegalArgumentException failure) {
            Thaumcraft.LOG.error("Cannot read Thaumcraft client notifications at {}; using original defaults", file, failure);
            queue = new NotificationQueue(5000); maximum = 15;
        }
        WarpMessageNetworking.initClient(PlayerNotifications::receive);
    }
    public static void reset() { queue.clear(); level = null; }
    private static boolean current() {
        var client = Minecraft.getInstance();
        if (level != client.level) { queue.clear(); level = client.level; }
        return client.level != null && client.player != null;
    }
    public static void receive(WarpMessagePayload payload) {
        if (!current()) return;
        var message = WarpNotificationRules.message(payload.kind(), payload.delta());
        if (message == null) return;
        if (message.whispers()) Minecraft.getInstance().player.playSound(WHISPERS, .5F, 1F);
        addNotification(Component.translatable(message.translationKey()).getString());
    }
    public static void addNotification(String text) { addNotification(text, null, 0xFFFFFF); }
    public static void addNotification(String text, ResourceLocation image, int color) {
        if (current()) queue.add(text, image == null ? null : image.toString(), color, now());
    }
    public static void renderHud(GuiGraphics graphics) {
        if (!current()) return;
        var client = Minecraft.getInstance(); long time = now();
        var entries = queue.update(time);
        if (client.options.hideGui || entries.isEmpty()) return;
        float shift = -8F;
        float[] oldColor = RenderSystem.getShaderColor().clone();
        try {
            for (int index = 0; index < entries.size() && index < maximum; index++) {
                var entry = entries.get(index); int size = client.font.width(entry.text()) / 2;
                int alpha = 255;
                if (index == entries.size() - 1 && entry.created() > time) alpha = 255 - (int) ((float) (entry.created() - time) / (queue.delay() / 4) * 240F);
                if (entry.expire() < time + queue.delay()) {
                    alpha = (int) (255F - (float) (time + queue.delay() - entry.expire()) / queue.delay() * 240F);
                    shift = -8F * (alpha / 255F);
                }
                graphics.pose().pushPose();
                try {
                    graphics.pose().translate(graphics.guiWidth() - size - 10, graphics.guiHeight() - index * 8 + shift, 0);
                    graphics.pose().scale(.5F, .5F, .5F);
                    graphics.drawString(client.font, entry.text(), -4, -8, (alpha / 2 << 24) | 0xFFFFFF, true);
                } finally { graphics.pose().popPose(); }
                if (entry.image() != null) {
                    graphics.pose().pushPose();
                    try {
                        graphics.pose().translate(graphics.guiWidth() - 9, graphics.guiHeight() - index * 8 + shift - 6, 0);
                        graphics.setColor((entry.color() >> 16 & 255) / 255F, (entry.color() >> 8 & 255) / 255F, (entry.color() & 255) / 255F, alpha / 511F);
                        graphics.blit(ResourceLocation.parse(entry.image()), 0, 0, 0F, 0F, 8, 8, 8, 8);
                    } finally { graphics.pose().popPose(); graphics.setColor(1, 1, 1, 1); }
                }
                if (index != entries.size() - 1 || entry.created() <= time) continue;
                float scale = (float) (entry.created() - time) / (queue.delay() / 4);
                int incomingAlpha = 255 - (int) (scale * 240F);
                graphics.pose().pushPose();
                try {
                    graphics.pose().translate(graphics.guiWidth() - 5 - 8 * scale - (1 - scale) * (1 - scale) * (1 - scale) * size * 3,
                            graphics.guiHeight() - index * 8 + shift - 2 - 8 * scale, 0);
                    graphics.pose().scale(scale, scale, scale);
                    graphics.setColor(1, 1, 1, .5F - incomingAlpha / 511F);
                    graphics.blit(PARTICLES, 0, 0, 16 * ((client.player.tickCount + index * 3) % 16), 80, 16, 16);
                } finally { graphics.pose().popPose(); graphics.setColor(1, 1, 1, 1); }
            }
        } finally { graphics.setColor(oldColor[0], oldColor[1], oldColor[2], oldColor[3]); }
    }
}
