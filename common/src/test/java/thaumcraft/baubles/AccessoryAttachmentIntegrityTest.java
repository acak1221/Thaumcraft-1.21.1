package thaumcraft.baubles;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.*;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.baubles.PlayerAccessoryInventory;
import static org.junit.jupiter.api.Assertions.*;

class AccessoryAttachmentIntegrityTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }
    @Test void nonCompoundOuterAttachmentSurvivesCloneDeathAndSyncEnvelope() {
        var owner = new PlayerAccessoryInventory(null);
        Tag bad = StringTag.valueOf("unread accessories"); owner.loadAttachment(bad,null);
        assertTrue(owner.isRejected()); assertEquals(bad,owner.saveAttachment(null));
        assertTrue(owner.removeItem(0,1).isEmpty());
        var clone = new PlayerAccessoryInventory(null); clone.loadAttachment(owner.saveAttachment(null),null);
        assertEquals(bad, clone.saveAttachment(null));
        var synced = new PlayerAccessoryInventory(null); synced.load(owner.save(null),null);
        assertEquals(bad, synced.saveAttachment(null));
    }
}
