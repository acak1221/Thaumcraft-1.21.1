package thaumcraft.fabric.clienttest;

import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;

/** Pure synthetic accounting checks, NOT native callback/world/AI proof.
 * Root executes on the real clientTest Java21 classpath only AFTER freeze. */
public final class BoreClientEvidenceMotionChecks {
    private BoreClientEvidenceMotionChecks() {}
    private static final class Owner {}
    private static final class OtherClass {}
    private static final BoreClientEvidenceMotion.Point ZERO = point(0, 0, 0);
    private static int assertions;
    private static void check(boolean valid, String message) { assertions++; if (!valid) throw new AssertionError(message); }
    private static void rejects(Runnable action) {
        assertions++;
        try { action.run(); } catch (IllegalArgumentException | IllegalStateException expected) { return; }
        throw new AssertionError("Expected rejected motion evidence");
    }
    private static BoreClientEvidenceMotion.Point point(double x, double y, double z) { return new BoreClientEvidenceMotion.Point(x, y, z); }
    private static Map<String, Object> properties() {
        return Map.of("type", "minecraft:pig", "health", 10.0F, "name", "Pig", "named", false, "NoAI", false, "persistent", true, "invulnerable", false);
    }
    private static Map<String, Object> changed(String key, Object value) {
        var result = new LinkedHashMap<>(properties()); result.put(key, value); return result;
    }
    private static BoreClientEvidenceMotion.Observation observation(int counter, double x) {
        return new BoreClientEvidenceMotion.Observation(counter, point(x, -60, 299), ZERO, properties());
    }
    private static void normalAndOrdering() {
        Object owner = new Owner(); var initial = observation(40, 10); var motion = new BoreClientEvidenceMotion(owner, 77, initial);
        motion.observe(owner, 77, initial); // actual ordering modeled: checkDespawn BEFORE body HEAD.
        check(motion.post(owner, 77, initial).equals("NO_BODY_RETURN_THIS_POST_POSITION_AND_COUNTER_UNCHANGED"), "Initial binding is not a RETURN");
        motion.head(owner, 78, initial);
        var after = observation(41, 10.1); motion.observe(owner, 78, after);
        rejects(() -> motion.post(owner, 78, after)); // observing inside a frame did not create a receipt.
        var receipt = motion.returned(owner, 78, after);
        check(receipt.before().equals(initial) && receipt.after().equals(after) && receipt.tick() == 78, "Exact body snapshots");
        check(motion.post(owner, 78, after).equals("ACTUAL_NATIVE_BODY_RETURN"), "Paired ordinary motion accepted");
        rejects(() -> motion.post(owner, 79, observation(41, 11))); // position changed AFTER RETURN.
        rejects(() -> motion.head(owner, 79, observation(41, 11))); // cannot launder it as next HEAD.
        rejects(() -> motion.head(owner, 79, observation(0, 10.1))); // counter reset without unload.
        rejects(() -> motion.verify(new Owner(), after));
        rejects(() -> motion.returned(owner, 78, after));
        // Other native bodies can push velocity AFTER this body's RETURN without
        // changing its position; the current velocity is observed, not frozen.
        var pushed = new BoreClientEvidenceMotion.Observation(41, after.position(), point(.2, 0, .1), properties());
        motion.verify(owner, pushed);
        check(motion.post(owner, 79, pushed).equals("NO_BODY_RETURN_THIS_POST_POSITION_AND_COUNTER_UNCHANGED"), "Unchanged position without a current body is explicitly not tick proof");
        rejects(() -> motion.post(owner, 80, observation(42, 10.1))); // unchanged position + alleged counter alone is NOT proof.
    }
    private static void failuresStayStrict() {
        for (var change : Map.<String, Object>of("health", 9.0F, "name", "Renamed", "named", true, "NoAI", true, "persistent", false, "invulnerable", true, "type", "minecraft:cow").entrySet()) {
            Object owner = new Owner(); var initial = observation(0, 0); var motion = new BoreClientEvidenceMotion(owner, 1, initial);
            var bad = new BoreClientEvidenceMotion.Observation(0, initial.position(), ZERO, changed(change.getKey(), change.getValue()));
            rejects(() -> motion.head(owner, 1, bad));
            motion.head(owner, 1, initial);
            var badReturn = new BoreClientEvidenceMotion.Observation(1, point(.1, -60, 299), ZERO, bad.stable());
            rejects(() -> motion.returned(owner, 1, badReturn));
        }
        for (var invalid : new String[]{"named", "NoAI", "persistent"}) {
            var properties = changed(invalid, !invalid.equals("persistent"));
            rejects(() -> new BoreClientEvidenceMotion(new Owner(), 1, new BoreClientEvidenceMotion.Observation(0, ZERO, ZERO, properties)));
        }
        Object owner = new Owner(); var initial = observation(0, 0); var motion = new BoreClientEvidenceMotion(owner, 1, initial);
        rejects(() -> motion.returned(owner, 1, observation(1, .1))); // missing HEAD.
        motion.head(owner, 1, initial); rejects(() -> motion.head(owner, 1, initial));
        rejects(() -> motion.returned(owner, 2, observation(1, .1))); // wrong native tick.
        rejects(() -> motion.returned(owner, 1, observation(0, .1))); // actual count did not advance.
        rejects(() -> motion.returned(new Owner(), 1, observation(1, .1))); // wrong actual object, not alleged ID.
        motion.returned(owner, 1, observation(1, .1));
        rejects(() -> motion.head(owner, 1, observation(1, .1))); // same-tick replay.
        rejects(() -> motion.rebind(new Owner(), 2, observation(0, 5))); // no native unload boundary.
        rejects(() -> point(Double.NaN, 0, 0)); rejects(() -> point(0, Double.POSITIVE_INFINITY, 0));
    }
    private static void gapsAndRebind() {
        Object oldOwner = new Owner(); var initial = observation(0, 0); var motion = new BoreClientEvidenceMotion(oldOwner, 5, initial);
        motion.head(oldOwner, 5, initial); var last = observation(1, .1); motion.returned(oldOwner, 5, last);
        motion.unload(oldOwner, 6, last); check(motion.unloaded() && !motion.frameOpen(), "Witnessed gap without an open body");
        rejects(() -> motion.verify(oldOwner, last));
        rejects(() -> motion.rebind(oldOwner, 7, observation(0, 50))); // no same-object reset/resurrection.
        rejects(() -> motion.rebind(new OtherClass(), 7, observation(0, 50)));
        Object newOwner = new Owner();
        rejects(() -> motion.rebind(newOwner, 7, new BoreClientEvidenceMotion.Observation(0, ZERO, ZERO, changed("health", 9.0F))));
        var rebound = observation(0, 50); motion.rebind(newOwner, 7, rebound);
        check(motion.lifetime() == 2 && motion.receipt() == null && !motion.unloaded(), "Gap does not inherit old native RETURN proof");
        check(motion.post(newOwner, 7, rebound).equals("NO_BODY_RETURN_THIS_POST_POSITION_AND_COUNTER_UNCHANGED"), "Reobserved gap drift is binding only");
        motion.head(newOwner, 8, rebound); motion.returned(newOwner, 8, observation(1, 50.1));
        check(motion.post(newOwner, 8, observation(1, 50.1)).equals("ACTUAL_NATIVE_BODY_RETURN"), "Fresh actual body after rebind");
        rejects(() -> motion.head(oldOwner, 9, observation(1, 50.1)));
        Object owner = new Owner(); var during = new BoreClientEvidenceMotion(owner, 20, initial);
        during.head(owner, 20, initial); during.unload(owner, 20, observation(1, .2));
        rejects(() -> during.rebind(new Owner(), 21, rebound)); // body RETURN has not closed.
        rejects(() -> during.unloadedReturn(new Owner(), 20));
        during.unloadedReturn(owner, 20); // deliberately takes NO stale-owner observations.
        check(!during.frameOpen() && during.unloaded() && during.receipt() == null, "Unload RETURN is not fabricated position proof");
        during.rebind(new Owner(), 21, rebound);
    }
    private static void immutableInput() {
        Object owner = new Owner(); var input = new LinkedHashMap<>(properties()); input.put("position", "FIRST");
        var first = new BoreClientEvidenceMotion.Observation(0, ZERO, ZERO, input); var motion = new BoreClientEvidenceMotion(owner, 1, first);
        input.put("health", 1.0F); input.put("position", "CHANGED");
        check(first.stable().get("health").equals(10.0F) && !first.stable().containsKey("position") && input.containsKey("position"), "Detached nonposition baseline without mutating caller");
        motion.head(owner, 1, first); motion.returned(owner, 1, new BoreClientEvidenceMotion.Observation(1, point(.1, 0, 0), ZERO, properties()));
        try { first.stable().put("health", 1.0F); throw new AssertionError("Mutable protected baseline"); }
        catch (UnsupportedOperationException expected) { assertions++; }
    }
    private static void boundedLatestReceipt() {
        Object owner = new Owner(); var initial = observation(0, 0); var motion = new BoreClientEvidenceMotion(owner, 100, initial);
        for (int i = 0; i < 16_000; i++) {
            long tick = i + 100L; var after = observation(i + 1, (i + 1) * .01);
            motion.head(owner, tick, initial); motion.returned(owner, tick, after);
            check(motion.post(owner, tick, after).equals("ACTUAL_NATIVE_BODY_RETURN") && motion.receipt().tick() == tick, "Bounded latest receipt fixture"); initial = after;
        }
        check(motion.lifetime() == 1 && !motion.frameOpen(), "No lifetime resets or open frames in fixture");
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("One NEW synthetic-result JSON path required");
        normalAndOrdering(); failuresStayStrict(); gapsAndRebind(); immutableInput(); boundedLatestReceipt();
        try (var out = Files.newBufferedWriter(Path.of(args[0]), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("status", "PURE_MOTION_ACCOUNTING_CHECKS_PASSED_NOT_GAMEPLAY", "assertions", assertions,
                    "synthetic_body_pairs", 16_000, "retained_receipt_per_tracker", 1,
                    "limitations", "No native hooks/world/AI/owned-area or actual game heap proof; Ledger provenance/current-owner/footprint integration needs fresh real execution"), out);
            out.write('\n');
        }
    }
}
