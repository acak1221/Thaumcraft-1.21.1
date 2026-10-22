package thaumcraft.client.lib;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Original PlayerNotifications queue timing, independent of rendering and the native game clock. */
public final class NotificationQueue {
    public record Notification(String text, String image, int color, long expire, long created) {}
    private final int delay;
    private List<Notification> notifications = new ArrayList<>();
    public NotificationQueue(int delay) {
        if (delay < 4) throw new IllegalArgumentException("Notification delay must support the original quarter-delay fade");
        this.delay = delay;
    }
    public int delay() { return delay; }
    public void add(String text, String image, int color, long now) {
        long bonus = notifications.isEmpty() ? delay / 2 : 0;
        notifications.add(new Notification(Objects.requireNonNull(text), image, color, now + delay + bonus, now + delay / 4));
    }
    public List<Notification> update(long now) {
        // The HUD calls this every frame, including when there is nothing to draw.
        if (notifications.isEmpty()) return List.of();
        var retained = new ArrayList<Notification>();
        boolean first = true;
        for (Notification value : notifications) {
            if (value.expire >= now) retained.add(first ? value : new Notification(value.text, value.image, value.color, now + delay, value.created));
            first = false; // Original first flag also advances when an expired prefix is removed.
        }
        notifications = retained;
        return List.copyOf(retained);
    }
    public void clear() { notifications.clear(); }
}
