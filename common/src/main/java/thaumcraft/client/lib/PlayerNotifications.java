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
    private static final AspectFlightQueue FLIGHTS = new AspectFlightQueue();
    private static long lastSound;
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
        dev.architectury.event.events.client.ClientGuiEvent.INIT_POST.register((screen, access) -> {
            if (screen instanceof net.minecraft.client.gui.screens.ChatScreen && thaumcraft.common.config.ThaumcraftTweaks.clearNotifications())
                access.addRenderableWidget(new ClearButton(screen));
        });
        WarpMessageNetworking.initClient(PlayerNotifications::receive);
        thaumcraft.common.lib.network.KnowledgeNotificationNetworking.initClient(PlayerNotifications::receivePool,
                PlayerNotifications::receiveDiscovery, ResearchNotifications::receive);
    }
    public static void reset() { queue.clear(); FLIGHTS.clear(); ResearchNotifications.reset(); level = null; lastSound=0; }

    /** The rows the HUD draws now. */
    public static int visibleRows() { return Math.min(queue.update(now()).size(), maximum); }

    /** Tweak (TC4Tweaks): the clear button of open chat empties the notification stack. */
    public static void clearStack() { queue.clear(); FLIGHTS.clear(); }

    /**
     * The button open chat shows over the notification stack while it has rows; a setting adopted
     * from TC4Tweaks. TC4 has no way to dismiss its notifications.
     */
    private static final class ClearButton extends net.minecraft.client.gui.components.Button {
        private final net.minecraft.client.gui.screens.Screen screen;

        ClearButton(net.minecraft.client.gui.screens.Screen screen) {
            super(0, 0, Minecraft.getInstance().font.width(net.minecraft.network.chat.Component.translatable("tc.port.notifications.clear")) + 10, 12,
                    net.minecraft.network.chat.Component.translatable("tc.port.notifications.clear"), button -> clearStack(), DEFAULT_NARRATION);
            this.screen = screen;
        }

        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            int rows = visibleRows();
            // Above the stack: row i of the HUD sits about twelve plus eight times i above the bottom.
            setPosition(screen.width - width - 2, Math.max(0, screen.height - 26 - rows * 8));
            if (rows > 0) super.renderWidget(graphics, mouseX, mouseY, delta);
        }

        @Override public boolean mouseClicked(double x, double y, int button) {
            return visibleRows() > 0 && super.mouseClicked(x, y, button);
        }

        @Override public boolean isMouseOver(double x, double y) {
            return visibleRows() > 0 && super.isMouseOver(x, y);
        }
    }
    static boolean current() {
        var client = Minecraft.getInstance();
        if (level != client.level) { queue.clear(); FLIGHTS.clear(); ResearchNotifications.reset(); level = client.level; lastSound=0; }
        return client.level != null && client.player != null;
    }
    public static void receive(WarpMessagePayload payload) {
        if (!current()) return;
        var message = WarpNotificationRules.message(payload.kind(), payload.delta());
        if (message == null) return;
        if (message.whispers()) Minecraft.getInstance().player.playSound(WHISPERS, .5F, 1F);
        addNotification(Component.translatable(message.translationKey()).getString());
    }
    private static void receivePool(thaumcraft.common.lib.network.AspectPoolPayload payload) {
        if (!current()) return;
        var aspect = thaumcraft.api.aspects.Aspect.getAspect(payload.key());
        // Full native KnowledgeSync is the sole authority: never overwrite raw33000 with this old wire total−32536.
        if (aspect == null || payload.amount() <= 0) return;
        String text = Component.translatable("tc.addaspectpool").getString().replace("%s", Short.toString(payload.amount())).replace("%n", aspect.getName());
        addNotification(text, aspect.getImage(), aspect.getColor());
        var client=Minecraft.getInstance();
        for(int unit=0;unit<payload.amount();unit++) {
            long created=now()+client.level.random.nextInt(1000);
            float x=.4F+client.level.random.nextFloat()*.2F,y=.4F+client.level.random.nextFloat()*.2F;
            FLIGHTS.add(aspect.getTag(),aspect.getColor(),x,y,created);
        }
        if(System.currentTimeMillis()>lastSound) {
            client.player.playSound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,.1F,.9F+client.level.random.nextFloat()*.2F);
            lastSound=System.currentTimeMillis()+100L;
        }
    }
    private static void receiveDiscovery(thaumcraft.common.lib.network.AspectDiscoveryPayload payload) {
        if(!current())return;
        var aspect=thaumcraft.api.aspects.Aspect.getAspect(payload.key());if(aspect==null)return;
        addNotification("§6"+Component.translatable("tc.addaspectdiscovery").getString().replace("%n",aspect.getName()),aspect.getImage(),aspect.getColor());
        var client=Minecraft.getInstance();
        client.player.playSound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,.2F,.5F+client.level.random.nextFloat()*.2F);
        ResearchNotifications.highlightAspects();
    }
    public static void addNotification(String text) { addNotification(text, null, 0xFFFFFF); }
    public static void addNotification(String text, ResourceLocation image, int color) {
        if (current()) queue.add(text, image == null ? null : image.toString(), color, now());
    }
    public static void renderHud(GuiGraphics graphics) {
        if (!current()) return;
        var client = Minecraft.getInstance(); long time = now();
        double scaledWidth=client.getWindow().getWidth()/client.getWindow().getGuiScale(),scaledHeight=client.getWindow().getHeight()/client.getWindow().getGuiScale();
        int screenWidth=(int)scaledWidth,screenHeight=(int)scaledHeight;
        var entries = queue.update(time);
        var flights=FLIGHTS.update(time);
        if (client.options.hideGui) return;
        float shift = -8F;
        float[] oldColor = RenderSystem.getShaderColor().clone();
        try {
            graphics.setColor(1,1,1,1);
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
                    graphics.pose().translate(screenWidth - size - 10, screenHeight - index * 8 + shift, 0);
                    graphics.pose().scale(.5F, .5F, .5F);
                    graphics.drawString(client.font, entry.text(), -4, -8, (alpha / 2 << 24) | 0xFFFFFF, true);
                } finally { graphics.pose().popPose(); }
                if (entry.image() != null) {
                    graphics.pose().pushPose();
                    try {
                        graphics.pose().translate(screenWidth - 9, screenHeight - index * 8 + shift - 6, 0);
                        graphics.setColor((entry.color() >> 16 & 255) / 255F, (entry.color() >> 8 & 255) / 255F, (entry.color() & 255) / 255F, alpha / 511F);
                        graphics.blit(ResourceLocation.parse(entry.image()), 0, 0, 0F, 0F, 8, 8, 8, 8);
                    } finally { graphics.pose().popPose(); graphics.setColor(1, 1, 1, 1); }
                }
                if (index != entries.size() - 1 || entry.created() <= time) continue;
                float scale = (float) (entry.created() - time) / (queue.delay() / 4);
                int incomingAlpha = 255 - (int) (scale * 240F);
                graphics.pose().pushPose();
                try {
                    graphics.pose().translate(screenWidth - 5 - 8 * scale - (1 - scale) * (1 - scale) * (1 - scale) * size * 3,
                            screenHeight - index * 8 + shift - 2 - 8 * scale, 0);
                    graphics.pose().scale(scale, scale, scale);
                    graphics.setColor(1, 1, 1, .5F - incomingAlpha / 511F);
                    graphics.blit(PARTICLES, 0, 0, 16 * ((client.player.tickCount + index * 3) % 16), 80, 16, 16);
                } finally { graphics.pose().popPose(); graphics.setColor(1, 1, 1, 1); }
            }
            float mainAlpha=0;
            for(var flight:flights) {
                var frame=AspectFlightQueue.frame(flight,time,scaledWidth,scaledHeight);if(frame==null)continue;
                var aspect=thaumcraft.api.aspects.Aspect.getAspect(flight.aspect());if(aspect==null)continue;
                mainAlpha=Math.max(mainAlpha,frame.alpha());graphics.pose().pushPose();
                try {
                    graphics.pose().translate(frame.x(),frame.y(),0);graphics.pose().scale(.075F*frame.alpha(),.075F*frame.alpha(),.075F*frame.alpha());
                    graphics.setColor((flight.color()>>16&255)/255F,(flight.color()>>8&255)/255F,(flight.color()&255)/255F,frame.alpha()*.66F);
                    graphics.blit(aspect.getImage(),0,0,0F,0F,256,256,256,256);
                } finally {graphics.pose().popPose();graphics.setColor(1,1,1,1);}
            }
            if(mainAlpha>0) {
                graphics.setColor(1,1,1,mainAlpha);
                graphics.blit(ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/items/thaumonomicon.png"),screenWidth-16,0,0F,0F,16,16,16,16);
            }
        } finally { graphics.setColor(oldColor[0], oldColor[1], oldColor[2], oldColor[3]); }
    }
}
