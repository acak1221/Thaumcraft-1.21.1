package thaumcraft.fabric.clienttest;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;

/** Pure detached accounting for ONE eligible protected mobile UUID. This class
 * cannot establish native provenance: Ledger must obtain actual reads inside
 * the real ServerLevel.tickNonPassenger HEAD/RETURN, on its server thread.
 * No world, movement, RNG, codec, permission or native tick method is invoked. */
final class BoreClientEvidenceMotion {
    record Point(double x, double y, double z) {
        Point {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
                throw new IllegalArgumentException("Nonfinite native mobile vector");
        }
    }
    record Observation(int counter, Point position, Point velocity, Map<String, Object> stable) {
        Observation {
            if (position == null || velocity == null) throw new IllegalArgumentException("Missing native mobile vectors");
            stable = nonPosition(stable);
        }
    }
    record Receipt(long tick, long lifetime, Observation before, Observation after) {}
    private record Frame(long tick, Observation before) {}
    private final Class<?> ownerClass;
    private final Map<String, Object> baseline;
    private WeakReference<Object> owner;
    private Observation anchor;
    private Frame frame;
    private Receipt receipt;
    private long lifetime = 1, bindingTick, lastPost = -1, unloadTick = -1;
    private boolean unloaded;

    BoreClientEvidenceMotion(Object actualOwner, long tick, Observation first) {
        if (actualOwner == null) throw new IllegalArgumentException("Missing mobile owner");
        eligible(first.stable()); ownerClass = actualOwner.getClass(); baseline = first.stable();
        owner = new WeakReference<>(actualOwner); anchor = first; bindingTick = tick;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> nonPosition(Map<String, Object> observed) {
        var values = new LinkedHashMap<String, Object>(observed); values.remove("position");
        return (Map<String, Object>) BoreClientEvidence.freeze(values);
    }
    private static void eligible(Map<String, Object> properties) {
        require(Boolean.FALSE.equals(properties.get("named")) && Boolean.FALSE.equals(properties.get("NoAI"))
                && Boolean.TRUE.equals(properties.get("persistent")), "Named/NoAI/nonpersistent entities cannot enter mobile protection");
        require(properties.containsKey("type") && properties.containsKey("health") && properties.containsKey("name")
                && properties.containsKey("invulnerable"), "Missing original protected mobile properties");
    }
    private void identity(Object actualOwner) {
        require(actualOwner != null && owner.get() == actualOwner && actualOwner.getClass() == ownerClass, "Mobile owner replaced without witnessed unload/rebind");
    }
    private void stable(Observation actual) {
        eligible(actual.stable()); require(baseline.equals(actual.stable()), "Protected mobile NONPOSITION properties changed");
    }
    private void atAnchor(Observation actual) {
        require(actual.counter() == anchor.counter() && actual.position().equals(anchor.position()), "Mobile position/counter changed outside a paired native body");
    }
    void verify(Object actualOwner, Observation actual) {
        identity(actualOwner); stable(actual); require(!unloaded && frame == null, "Missing mobile RETURN or current owner is unloaded"); atAnchor(actual);
    }
    /** checkDespawn can observe the same object before HEAD or within its body.
     * In-body observations never advance the anchor or create RETURN evidence. */
    void observe(Object actualOwner, long tick, Observation actual) {
        identity(actualOwner); stable(actual); require(!unloaded, "Unloaded owner needs genuine native rebind");
        require(tick >= bindingTick && tick >= lastPost && (receipt == null || tick >= receipt.tick()), "Reversed mobile observation/unload tick");
        if (frame == null) atAnchor(actual);
        else require(frame.tick() == tick && actual.counter() == frame.before().counter() + 1, "Observation outside the actual open native body");
    }
    void head(Object actualOwner, long tick, Observation before) {
        verify(actualOwner, before);
        require(tick >= bindingTick && tick > lastPost && (receipt == null || tick > receipt.tick()), "Duplicate/reversed mobile native body");
        frame = new Frame(tick, before);
    }
    Receipt returned(Object actualOwner, long tick, Observation after) {
        identity(actualOwner); stable(after);
        require(!unloaded && frame != null && frame.tick() == tick, "Missing/mismatched mobile native HEAD/RETURN");
        require(after.counter() == frame.before().counter() + 1, "Actual native entity counter did not advance exactly once");
        Point a = frame.before().position(), b = after.position();
        require(Double.isFinite(b.x() - a.x()) && Double.isFinite(b.y() - a.y()) && Double.isFinite(b.z() - a.z()), "Nonfinite native displacement");
        // No invented distance/speed threshold. Provenance is the real outer
        // native body, not a claim that each internal move was SELF/vanilla-only.
        receipt = new Receipt(tick, lifetime, frame.before(), after); anchor = after; frame = null; return receipt;
    }
    String post(Object actualOwner, long tick, Observation actual) {
        verify(actualOwner, actual); require(tick >= bindingTick && tick > lastPost, "Nonmonotonic mobile POST");
        require(receipt == null || receipt.tick() <= tick, "Mobile RETURN from the future"); lastPost = tick;
        return receipt != null && receipt.tick() == tick ? "ACTUAL_NATIVE_BODY_RETURN"
                : "NO_BODY_RETURN_THIS_POST_POSITION_AND_COUNTER_UNCHANGED";
    }
    void unload(Object actualOwner, long tick, Observation actual) {
        observe(actualOwner, tick, actual); unloaded = true; unloadTick = tick;
        // Preserve an open frame only until its real RETURN closes. No removed
        // owner position/health is read or turned into a paired receipt there.
    }
    void unloadedReturn(Object actualOwner, long tick) {
        identity(actualOwner);
        require(unloaded && frame != null && frame.tick() == tick && unloadTick == tick, "Unpaired RETURN after mobile unload"); frame = null;
    }
    void rebind(Object actualOwner, long tick, Observation observed) {
        require(unloaded && frame == null && actualOwner != null && owner.get() != actualOwner && actualOwner.getClass() == ownerClass,
                "Mobile rebind requires a completed native unload and new same-class owner");
        stable(observed); require(tick >= unloadTick && lifetime < Long.MAX_VALUE, "Invalid mobile rebind boundary");
        owner = new WeakReference<>(actualOwner); lifetime++; anchor = observed; bindingTick = tick; receipt = null; unloaded = false;
        // This is an observed binding AFTER an uncovered interval, NOT proof of
        // movement inside the gap or an invented tick/RETURN at this position.
    }
    boolean unloaded() { return unloaded; }
    boolean frameOpen() { return frame != null; }
    long lifetime() { return lifetime; }
    long bindingTick() { return bindingTick; }
    long unloadTick() { return unloadTick; }
    Receipt receipt() { return receipt; }
    Map<String, Object> baseline() { return baseline; }
    private static void require(boolean valid, String message) { if (!valid) throw new IllegalStateException(message); }
}
