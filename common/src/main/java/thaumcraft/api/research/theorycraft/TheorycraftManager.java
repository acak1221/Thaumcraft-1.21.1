package thaumcraft.api.research.theorycraft;

import java.util.HashMap;

/**
 * The registry of theorycraft aids and cards.
 *
 * <p>The original registration order is kept: the first registration of a name wins.</p>
 */
public class TheorycraftManager {
    public static final HashMap<String, ITheorycraftAid> aids = new HashMap<>();
    public static final HashMap<String, Class<TheorycraftCard>> cards = new HashMap<>();

    private TheorycraftManager() {}

    public static void registerAid(ITheorycraftAid aid) {
        if (aid == null) return;
        aids.putIfAbsent(aid.getClass().getName(), aid);
    }

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
