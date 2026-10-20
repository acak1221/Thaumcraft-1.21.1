package thaumcraft.common.lib;

/** Original PacketWarpMessage's intentionally asymmetric negative-delta branches. */
public final class WarpNotificationRules {
    public record Message(String translationKey, boolean whispers) {}
    private WarpNotificationRules() {}
    public static Message message(byte type, int delta) {
        if (delta == 0) return null;
        if (type == 0 && delta > 0) return new Message("tc.addwarp", true);
        if (type == 1) return new Message(delta < 0 ? "tc.removewarpsticky" : "tc.addwarpsticky", delta > 0);
        if (delta > 0) return new Message("tc.addwarptemp", false);
        return null; // Permanent/temporary removal text is unreachable in the original packet handler.
    }
}
