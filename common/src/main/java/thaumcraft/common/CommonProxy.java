package thaumcraft.common;

import java.util.Map;
import java.util.Set;
import net.minecraft.world.entity.player.Player;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.research.ResearchManager;

/**
 * The published server side surface of the original {@code thaumcraft.common.CommonProxy}.
 *
 * <p>Oracle {@code thaumcraft/common/CommonProxy.java:63-156} (CFR 0.152 decompilation of
 * {@code Thaumcraft-1.7.10-4.2.3.5.jar}). The original kept the knowledge of every player in one
 * global object behind the static proxy field {@code Thaumcraft.proxy} and published it with
 * {@code getPlayerKnowledge()} and the five global maps of the knowledge.</p>
 *
 * <p>Forced 1.21.1 change: the native port has no static proxy and no global knowledge registry;
 * the knowledge of a player lives in the data of that player and is reached with
 * {@link KnowledgeAccess}. This class is therefore the published server side surface of the
 * original proxy: it keeps the names and the meaning of the original members, but the knowledge
 * is asked for <b>per player</b>. An add-on that used {@code Thaumcraft.proxy.getPlayerKnowledge()}
 * and then asked the knowledge for a player name now asks the proxy for the player.</p>
 *
 * <p>The forty client effect entry points of the original (oracle
 * {@code thaumcraft/common/CommonProxy.java:158-327}) are not part of this class: the native port
 * draws every one of them with its own client effect classes, and the visual parity of those
 * entry points is the business of the client lane.</p>
 */
public final class CommonProxy {
    /** The single instance of the facade. */
    public static final CommonProxy INSTANCE = new CommonProxy();

    private CommonProxy() {}

    /**
     * Oracle {@code CommonProxy:69-71} - the knowledge of a player.
     *
     * @param player the player whose knowledge is asked for
     * @return the knowledge of that player
     */
    public PlayerKnowledge getPlayerKnowledge(Player player) {
        return KnowledgeAccess.get(player);
    }

    /**
     * Oracle {@code CommonProxy:73-75} - the research manager of the game.
     *
     * @return the research manager
     */
    public ResearchManager getResearchManager() {
        return ResearchManager.INSTANCE;
    }

    /**
     * Oracle {@code CommonProxy:77-80} - the completed research of a player.
     *
     * @param player the player whose research is asked for
     * @return the keys of the research that the player completed
     */
    public Set<String> getCompletedResearch(Player player) {
        return KnowledgeAccess.get(player).getResearchComplete();
    }

    /**
     * Oracle {@code CommonProxy:81-84} - the objects that a player scanned.
     *
     * @param player the player whose scans are asked for
     * @return the keys of the scanned objects
     */
    public Set<String> getScannedObjects(Player player) {
        return KnowledgeAccess.get(player).getScannedItems();
    }

    /**
     * Oracle {@code CommonProxy:85-88} - the entities that a player scanned.
     *
     * @param player the player whose scans are asked for
     * @return the keys of the scanned entities
     */
    public Set<String> getScannedEntities(Player player) {
        return KnowledgeAccess.get(player).getScannedEntities();
    }

    /**
     * Oracle {@code CommonProxy:89-92} - the phenomena that a player scanned.
     *
     * @param player the player whose scans are asked for
     * @return the keys of the scanned phenomena
     */
    public Set<String> getScannedPhenomena(Player player) {
        return KnowledgeAccess.get(player).getScannedPhenomena();
    }

    /**
     * Oracle {@code CommonProxy:93-96} - the aspects that a player discovered.
     *
     * @param player the player whose knowledge is asked for
     * @return the discovered aspects with their amounts
     */
    public Map<String, Integer> getKnownAspects(Player player) {
        return KnowledgeAccess.get(player).getAspectsDiscovered();
    }
}
