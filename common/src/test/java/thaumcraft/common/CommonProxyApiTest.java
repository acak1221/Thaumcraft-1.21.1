package thaumcraft.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.research.ResearchManager;

/**
 * The published server side surface of the original proxy.
 *
 * <p>Oracle {@code thaumcraft/common/CommonProxy.java:63-96} (CFR 0.152 decompilation of
 * {@code Thaumcraft-1.7.10-4.2.3.5.jar}) - the proxy published the knowledge of a player, the
 * research manager and the five maps of the knowledge. The native port has no static proxy and
 * no global knowledge registry: the knowledge lives in the data of the player, so the published
 * surface asks for the player. The test asserts the names and the meaning of that surface and
 * never touches a world.</p>
 */
class CommonProxyApiTest {
    @Test void theProxyPublishesTheOriginalMembers() throws Exception {
        assertNotNull(CommonProxy.INSTANCE, "the facade of the original proxy is reachable");
        Method knowledge = CommonProxy.class.getMethod("getPlayerKnowledge", Player.class);
        assertEquals(PlayerKnowledge.class, knowledge.getReturnType(),
                "the original getPlayerKnowledge answers with the knowledge of the player");
        Method manager = CommonProxy.class.getMethod("getResearchManager");
        assertEquals(ResearchManager.class, manager.getReturnType(),
                "the original getResearchManager answers with the research manager");
        assertEquals(java.util.Set.class, CommonProxy.class.getMethod("getCompletedResearch", Player.class).getReturnType());
        assertEquals(java.util.Set.class, CommonProxy.class.getMethod("getScannedObjects", Player.class).getReturnType());
        assertEquals(java.util.Set.class, CommonProxy.class.getMethod("getScannedEntities", Player.class).getReturnType());
        assertEquals(java.util.Set.class, CommonProxy.class.getMethod("getScannedPhenomena", Player.class).getReturnType());
        assertEquals(java.util.Map.class, CommonProxy.class.getMethod("getKnownAspects", Player.class).getReturnType());
    }

    @Test void theResearchManagerPublishesTheOriginalMembers() throws Exception {
        assertSame(ResearchManager.INSTANCE, CommonProxy.INSTANCE.getResearchManager(),
                "the research manager of the proxy is the one of the game");
        assertTrue(ResearchManager.class.getMethod("isResearchComplete", Player.class, String.class) != null);
        assertTrue(ResearchManager.class.getMethod("completeResearch", Player.class, String.class) != null);
        assertTrue(ResearchManager.class.getMethod("completeScannedObject", Player.class, String.class) != null);
        assertTrue(ResearchManager.class.getMethod("completeScannedEntity", Player.class, String.class) != null);
        assertTrue(ResearchManager.class.getMethod("completeScannedPhenomena", Player.class, String.class) != null);
    }
}
