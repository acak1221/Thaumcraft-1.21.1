package thaumcraft.api.capabilities;

import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.api.research.ResearchCategory;

/**
 * The player knowledge contract of the add-on surface.
 *
 * <p>The native port keeps the knowledge of a player in
 * {@code thaumcraft.common.lib.research.PlayerKnowledge}; this interface is the public view of it, and
 * {@link NativePlayerKnowledge} is the implementation that is bound to that native state.</p>
 */
public interface IPlayerKnowledge {
    /** The two kinds of knowledge a category may report. */
    enum EnumKnowledgeType {
        THEORY(32, true, "T"),
        OBSERVATION(16, true, "O");

        private final short progression;
        private final boolean hasFields;
        private final String abbreviation;

        EnumKnowledgeType(int progression, boolean hasFields, String abbreviation) {
            this.progression = (short) progression;
            this.hasFields = hasFields;
            this.abbreviation = abbreviation;
        }

        public int getProgression() { return progression; }

        public boolean hasFields() { return hasFields; }

        public String getAbbreviation() { return abbreviation; }
    }

    /** The state of one research key. */
    enum EnumResearchStatus { UNKNOWN, COMPLETE, IN_PROGRESS }

    /** The per-research markers of the research screen. */
    enum EnumResearchFlag { PAGE, RESEARCH, POPUP }

    /** Forgets everything this player discovered. */
    void clear();

    EnumResearchStatus getResearchStatus(String key);

    boolean isResearchComplete(String key);

    /** True when the research key exists in the catalogue. */
    boolean isResearchKnown(String key);

    /** The number of stages of the research. */
    int getResearchStage(String key);

    boolean addResearch(String key);

    boolean setResearchStage(String key, int stage);

    boolean removeResearch(String key);

    /** The research keys this player completed. */
    Set<String> getResearchList();

    boolean setResearchFlag(String key, EnumResearchFlag flag);

    boolean clearResearchFlag(String key, EnumResearchFlag flag);

    boolean hasResearchFlag(String key, EnumResearchFlag flag);

    /** Adds writable knowledge and reports whether it was accepted.
     * The native TC4 view accepts OBSERVATION as aspect-pool credit. THEORY is a read-only
     * projection of completed research costs, so NativePlayerKnowledge returns false for it
     * without changing player state; it is not a separate TC6 progression balance.
     */
    boolean addKnowledge(EnumKnowledgeType type, ResearchCategory category, int amount);

    /** The knowledge of the category, as the category formula reports it. */
    int getKnowledge(EnumKnowledgeType type, ResearchCategory category);

    /** The raw knowledge of the category, before the category formula.
     * In the native view THEORY sums the category's aspects across completed TC4 research costs.
     */
    int getKnowledgeRaw(EnumKnowledgeType type, ResearchCategory category);

    /** Sends the current knowledge to the client. */
    void sync(ServerPlayer player);
}
