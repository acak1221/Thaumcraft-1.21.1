package thaumcraft.common.lib.research;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;

/**
 * The research manager of the original, as the published surface of the native port.
 *
 * <p>Oracle {@code thaumcraft/common/lib/research/ResearchManager.java:389-609} (CFR 0.152
 * decompilation of {@code Thaumcraft-1.7.10-4.2.3.5.jar}) - the manager answers whether a player
 * completed a research, completes a research, completes an aspect and records the objects, the
 * entities and the phenomena that a player scanned. The original addressed the player by its
 * name, because the knowledge of every player lived in one global object; the native port keeps
 * the knowledge of a player in the data of that player, so the manager asks for the player.</p>
 *
 * <p>Forced 1.21.1 change: none of the meanings. The global object of the original is the
 * per player state of the native port, and the original save of the player data is the native
 * data of the player.</p>
 */
public final class ResearchManager {
    /** The single instance of the manager. */
    public static final ResearchManager INSTANCE = new ResearchManager();

    private ResearchManager() {}

    /**
     * Oracle {@code ResearchManager:389-397} - {@code isResearchComplete(playername, key)}.
     *
     * @param player the player
     * @param key    the key of the research
     * @return whether the player completed that research
     */
    public boolean isResearchComplete(Player player, String key) {
        return player != null && key != null && KnowledgeAccess.get(player).isResearchComplete(key);
    }

    /**
     * Oracle {@code ResearchManager:497-527} - {@code completeResearch(player, key)}.
     *
     * <p>The original completed the research, its siblings and its parents and paid the warp of
     * the definition; the native completion path does the same.</p>
     *
     * @param player the player
     * @param key    the key of the research
     * @return whether the research was completed
     */
    public boolean completeResearch(Player player, String key) {
        if (player == null || key == null) return false;
        var knowledge = KnowledgeAccess.get(player);
        if (!ResearchAdministration.grant(knowledge, key)) return false;
        sync(player);
        return true;
    }

    /**
     * Oracle {@code ResearchManager:530-543} - {@code completeAspect(player, aspect, amount)}.
     *
     * @param player the player
     * @param aspect the aspect
     * @param amount the amount of the aspect
     */
    public void completeAspect(Player player, Aspect aspect, short amount) {
        if (player == null || aspect == null) return;
        KnowledgeAccess.get(player).addAspectPool(aspect.getTag(), amount);
        sync(player);
    }

    /**
     * Oracle {@code ResearchManager:545-556,593-597} - {@code completeScannedObject(player, object)}.
     *
     * @param player the player
     * @param object the key of the scanned object
     */
    public void completeScannedObject(Player player, String object) {
        if (player == null || object == null) return;
        KnowledgeAccess.get(player).scanItem(object);
        sync(player);
    }

    /**
     * Oracle {@code ResearchManager:561-575,599-603} - {@code completeScannedEntity(player, key)}.
     *
     * @param player the player
     * @param key    the key of the scanned entity
     */
    public void completeScannedEntity(Player player, String key) {
        if (player == null || key == null) return;
        KnowledgeAccess.get(player).scanEntity(key);
        sync(player);
    }

    /**
     * Oracle {@code ResearchManager:577-591,605-609} - {@code completeScannedPhenomena(player, key)}.
     *
     * @param player the player
     * @param key    the key of the scanned phenomena
     */
    public void completeScannedPhenomena(Player player, String key) {
        if (player == null || key == null) return;
        KnowledgeAccess.get(player).scanPhenomena(key);
        sync(player);
    }

    /** The native correction of the knowledge of a player, the original save of the player data. */
    private static void sync(Player player) {
        if (player instanceof ServerPlayer server) KnowledgeEvents.sync(server);
    }
}
