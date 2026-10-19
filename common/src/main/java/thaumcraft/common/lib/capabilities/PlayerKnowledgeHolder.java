package thaumcraft.common.lib.capabilities;

import thaumcraft.common.lib.research.PlayerKnowledge;

/** Implemented on each vanilla Player by the common mixin. */
public interface PlayerKnowledgeHolder {
    PlayerKnowledge thaumcraft$getPlayerKnowledge();
}
