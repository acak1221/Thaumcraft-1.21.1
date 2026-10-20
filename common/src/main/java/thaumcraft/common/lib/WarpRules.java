package thaumcraft.common.lib;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.IntUnaryOperator;

/** Original WarpEvents.checkWarpEvent's arithmetic and conditional RNG ordering, independent of a player/world. */
public final class WarpRules {
    public static final int CHECK_INTERVAL = 2000;
    public enum Event {
        NONE, RESEARCH, NOISE, VIS_EXHAUST, THAUMARHIA, HUNGER, FOLLOWING, MIST,
        BLURRED, SUN_SCORNED, MINING_FATIGUE, INFECTIOUS_EXHAUST, NIGHT_VISION,
        DEATH_GAZE, SPIDERS, WATCHING, BLINDNESS, CLARITY, RESEARCH_MANY, REAL_SPIDERS, MIST_MANY
    }
    public record Decision(boolean triggered, int effectiveWarp, int severity, int remainingCounter, Event event) {}
    private WarpRules() {}

    public static boolean scheduled(int ticks, boolean wuss, boolean warded) {
        return !wuss && !warded && ticks > 0 && ticks % CHECK_INTERVAL == 0;
    }

    public static Decision decide(int totalWarp, int counter, int gearWarp, boolean devilMask, IntUnaryOperator random) {
        int chance = random.applyAsInt(100); // Original consumes this even when the counter/warp gate fails.
        long warp = (long) totalWarp + gearWarp;
        if (counter <= 0 || warp <= 0 || chance > Math.sqrt(counter)) return new Decision(false, 0, 0, counter, Event.NONE);
        // Long arithmetic only prevents corrupt/extreme native values wrapping; ordinary original values are identical.
        int effective = (int) Math.min(100L, (warp + warp + counter) / 3L);
        int after = (int) (counter - Math.max(5.0, Math.sqrt(counter) * 2.0));
        int severity = random.applyAsInt(effective);
        if (devilMask) severity -= 2 + random.applyAsInt(4);
        return new Decision(true, effective, severity, after, event(severity));
    }

    /** Preserve the original gaps: 0, 5..8 and 85..88 have no additional effect. */
    public static Event event(int effect) {
        if (effect <= 0) return Event.NONE;
        if (effect <= 4) return Event.RESEARCH;
        if (effect <= 8) return Event.NONE;
        if (effect <= 12) return Event.NOISE;
        if (effect <= 16) return Event.VIS_EXHAUST;
        if (effect <= 20) return Event.THAUMARHIA;
        if (effect <= 24) return Event.HUNGER;
        if (effect <= 28) return Event.FOLLOWING;
        if (effect <= 32) return Event.MIST;
        if (effect <= 36) return Event.BLURRED;
        if (effect <= 40) return Event.SUN_SCORNED;
        if (effect <= 44) return Event.MINING_FATIGUE;
        if (effect <= 48) return Event.INFECTIOUS_EXHAUST;
        if (effect <= 52) return Event.NIGHT_VISION;
        if (effect <= 56) return Event.DEATH_GAZE;
        if (effect <= 60) return Event.SPIDERS;
        if (effect <= 64) return Event.WATCHING;
        if (effect <= 68) return Event.MIST_MANY;
        if (effect <= 72) return Event.BLINDNESS;
        if (effect == 76) return Event.CLARITY;
        // Original else-if(eff==76), else-if(eff<=80): 73..75 DO apply hunger too.
        if (effect <= 80) return Event.HUNGER;
        if (effect <= 84) return Event.RESEARCH_MANY;
        if (effect <= 88) return Event.NONE;
        if (effect <= 92) return Event.REAL_SPIDERS;
        return Event.MIST_MANY;
    }

    public static List<String> unlocks(int permanentAndSticky, Set<String> known) {
        List<String> result = new ArrayList<>();
        if (permanentAndSticky > 10 && !known.contains("BATHSALTS") && !known.contains("@BATHSALTS")) result.add("@BATHSALTS");
        if (permanentAndSticky > 25 && !known.contains("ELDRITCHMINOR")) result.add("ELDRITCHMINOR");
        if (permanentAndSticky > 50 && !known.contains("ELDRITCHMAJOR")) result.add("ELDRITCHMAJOR");
        return List.copyOf(result);
    }

    public static int amplifier(int warp) { return Math.min(3, warp / 15); }
    public static int gazeRange(int amplifier) { return Math.min(8 + amplifier * 3, 24); }
    public static boolean inGazeCone(double forward, double distance, int range) {
        return distance > 0 && forward / distance > Math.cos(.75F / 2.0F) && forward < range;
    }
}
