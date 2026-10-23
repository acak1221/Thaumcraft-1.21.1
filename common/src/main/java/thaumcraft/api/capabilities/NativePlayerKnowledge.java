package thaumcraft.api.capabilities;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.research.ResearchDefinition;
import thaumcraft.common.lib.research.PlayerKnowledge;

/**
 * The native {@link IPlayerKnowledge} implementation.
 *
 * <p>The completed research, the discovered aspects and the warp live in the native
 * {@link PlayerKnowledge}; only the per-research stage and flag markers are kept here, because the original
 * surface had no native counterpart for them.</p>
 */
public class NativePlayerKnowledge implements IPlayerKnowledge {
    private final PlayerKnowledge knowledge;
    private final Map<String, Integer> stages = new HashMap<>();
    private final Map<String, Set<EnumResearchFlag>> flags = new HashMap<>();

    public NativePlayerKnowledge(PlayerKnowledge knowledge) {
        this.knowledge = knowledge;
    }

    /** The native knowledge behind this view. */
    public PlayerKnowledge knowledge() { return knowledge; }

    @Override public void clear() { knowledge.reset(); stages.clear(); flags.clear(); }

    @Override public EnumResearchStatus getResearchStatus(String key) {
        if (key == null || !isResearchKnown(key)) return EnumResearchStatus.UNKNOWN;
        if (knowledge.isResearchComplete(key)) return EnumResearchStatus.COMPLETE;
        ResearchDefinition definition = ResearchCatalog.get(key);
        if (definition != null && definition.hasPrerequisites(knowledge.getResearchComplete())) return EnumResearchStatus.IN_PROGRESS;
        return EnumResearchStatus.UNKNOWN;
    }

    @Override public boolean isResearchComplete(String key) {
        return key != null && knowledge.isResearchComplete(key);
    }

    @Override public boolean isResearchKnown(String key) {
        return key != null && ResearchCatalog.get(key) != null;
    }

    @Override public int getResearchStage(String key) {
        if (key == null) return 0;
        Integer stage = stages.get(key);
        if (stage != null) return stage;
        ResearchDefinition definition = ResearchCatalog.get(key);
        return definition == null ? 0 : definition.pages().size();
    }

    @Override public boolean addResearch(String key) {
        if (!isResearchKnown(key)) return false;
        ResearchDefinition definition = ResearchCatalog.get(key);
        if (definition != null && !definition.hasPrerequisites(knowledge.getResearchComplete())) return false;
        return knowledge.addResearch(key);
    }

    @Override public boolean setResearchStage(String key, int stage) {
        if (key == null || stage < 0) return false;
        stages.put(key, stage);
        return true;
    }

    @Override public boolean removeResearch(String key) {
        if (key == null) return false;
        stages.remove(key);
        flags.remove(key);
        return knowledge.removeResearch(key);
    }

    @Override public Set<String> getResearchList() { return knowledge.getResearchComplete(); }

    @Override public boolean setResearchFlag(String key, EnumResearchFlag flag) {
        if (key == null || flag == null) return false;
        flags.computeIfAbsent(key, ignored -> new HashSet<>()).add(flag);
        return true;
    }

    @Override public boolean clearResearchFlag(String key, EnumResearchFlag flag) {
        if (key == null || flag == null) return false;
        Set<EnumResearchFlag> set = flags.get(key);
        return set != null && set.remove(flag);
    }

    @Override public boolean hasResearchFlag(String key, EnumResearchFlag flag) {
        if (key == null || flag == null) return false;
        Set<EnumResearchFlag> set = flags.get(key);
        return set != null && set.contains(flag);
    }

    /** Returns false for null/non-OBSERVATION types, null categories/formulas, nonpositive amounts or formulas without a target. */
    @Override public boolean addKnowledge(EnumKnowledgeType type, ResearchCategory category, int amount) {
        if (type != EnumKnowledgeType.OBSERVATION || category == null || category.formula == null || amount <= 0) return false;
        Aspect target = category.formula.getAspectsSortedAmount().length > 0 ? category.formula.getAspectsSortedAmount()[0] : null;
        if (target == null) return false;
        knowledge.addAspectPool(target.getTag(), amount);
        return true;
    }

    /** Null type is read as THEORY; null category/formula yields zero, and other formulas are applied without validation. */
    @Override public int getKnowledge(EnumKnowledgeType type, ResearchCategory category) {
        if (category == null) return 0;
        return category.applyFormula(rawAspects(type, category));
    }

    /** Null type is read as THEORY; null category/formula yields zero, and other formulas are summed without validation. */
    @Override public int getKnowledgeRaw(EnumKnowledgeType type, ResearchCategory category) {
        if (category == null || category.formula == null) return 0;
        int total = 0;
        for (Aspect aspect : category.formula.getAspects()) total += rawAmount(type, aspect);
        return total;
    }

    @Override public void sync(ServerPlayer player) {
        if (player != null) KnowledgeEvents.sync(player);
    }

    private thaumcraft.api.aspects.AspectList rawAspects(EnumKnowledgeType type, ResearchCategory category) {
        thaumcraft.api.aspects.AspectList result = new thaumcraft.api.aspects.AspectList();
        if (category.formula == null) return result;
        for (Aspect aspect : category.formula.getAspects()) result.add(aspect, rawAmount(type, aspect));
        return result;
    }

    /** Observation knowledge is the discovered aspect pool; theory knowledge is what the completed research paid. */
    private int rawAmount(EnumKnowledgeType type, Aspect aspect) {
        if (aspect == null) return 0;
        if (type == EnumKnowledgeType.OBSERVATION) return knowledge.getAspectPoolFor(aspect.getTag());
        int total = 0;
        for (String key : knowledge.getResearchComplete()) {
            ResearchDefinition definition = ResearchCatalog.get(key);
            if (definition == null) continue;
            total += definition.aspectCost().getOrDefault(aspect.getTag(), 0);
        }
        return total;
    }
}
