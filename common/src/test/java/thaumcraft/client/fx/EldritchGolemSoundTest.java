package thaumcraft.client.fx;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EldritchGolemSoundTest {
    @Test void vanillaGolemSoundsMustNotUseTheThaumcraftNamespace() throws Exception {
        String source = Files.readString(Path.of("src/main/java/thaumcraft/common/entities/monster/EntityEldritchGolem.java"));
        // TC4 oracle EntityEldritchGolem:150,154,158,248 uses vanilla mob.irongolem.*.
        assertFalse(source.contains("sound(\"mob.irongolem."), "legacy vanilla ids are not Thaumcraft events");
        for (String event : new String[]{"IRON_GOLEM_HURT", "IRON_GOLEM_DEATH", "IRON_GOLEM_STEP", "IRON_GOLEM_ATTACK"}) {
            assertTrue(source.contains("SoundEvents." + event), event);
        }
    }
}
