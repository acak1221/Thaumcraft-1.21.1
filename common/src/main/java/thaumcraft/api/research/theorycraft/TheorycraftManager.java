package thaumcraft.api.research.theorycraft;

import java.util.HashMap;

/**
 * The registry of theorycraft aids and cards.
 *
     * <p>First registration of a name wins; HashMap iteration order is not guaranteed.
     * This is a TC6-style compatibility registry, not part of TC4's 1.7.10 aspect/hex research.
     * No 1.21.1 native research table consumes these registrations.</p>
     * @deprecated Use TC4 ResearchItem/ResearchPage and research-note contracts; no native theorycraft-card flow exists.
 */
@Deprecated
public class TheorycraftManager {
    public static final HashMap<String, ITheorycraftAid> aids = new HashMap<>();
    public static final HashMap<String, Class<TheorycraftCard>> cards = new HashMap<>();

    private TheorycraftManager() {}

    /** Retains a non-null aid first-wins by class name; null is ignored and gameplay is unaffected.
     * @deprecated Use ResearchItem/ResearchPage registration for native TC4 research, not theorycraft aids. */
    @Deprecated
    public static void registerAid(ITheorycraftAid aid) {
        if (aid == null) return;
        aids.putIfAbsent(aid.getClass().getName(), aid);
    }

    /** Stores a TheorycraftCard subclass first-wins by class name; null/other classes are ignored, with no gameplay consumer.
     * @deprecated Use ResearchItem/ResearchPage registration for native TC4 research, not theorycraft cards. */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static void registerCard(Class<?> card) {
        if (card == null || !TheorycraftCard.class.isAssignableFrom(card)) return;
        cards.putIfAbsent(card.getName(), (Class<TheorycraftCard>) card);
    }

    /** The card class of a name, or null. */
    public static Class<TheorycraftCard> card(String name) { return name == null ? null : cards.get(name); }

    /** The aid of a class name, or null. */
    public static ITheorycraftAid aid(String className) { return className == null ? null : aids.get(className); }
}
