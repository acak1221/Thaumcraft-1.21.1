package thaumcraft.api.research.theorycraft;

/**
 * An add-on that contributes theorycraft cards to the research table.
 */
public interface ITheorycraftAid {
    /** The object the aid represents; the research table only uses it for display. */
    Object getAidObject();

    /** The card classes this aid contributes. */
    @SuppressWarnings("unchecked")
    Class<TheorycraftCard>[] getCards();
}
