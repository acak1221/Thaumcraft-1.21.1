package thaumcraft.client.fx;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Direct-created native particles do not need a ParticleType/provider or particle JSON. */
class ParticleResourceAuditTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/thaumcraft");
    private static void exists(String resource) {
        assertTrue(Files.isRegularFile(ASSETS.resolve(resource)), resource);
    }

    @Test void directParticleTexturesAndAnimatedFramesExist() throws Exception {
        try (var paths = Files.walk(Path.of("src/main/java/thaumcraft/client/fx/particles"))) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                var source = Files.readString(path);
                var literals = Pattern.compile("\"(textures/[^\"]+\\.(?:png|obj))\"").matcher(source);
                while (literals.find()) exists(literals.group(1));
                var helpers = Pattern.compile("VisNetRender\\.texture\\(\"([^\"]+)\"\\)").matcher(source);
                while (helpers.find()) exists("textures/" + helpers.group(1) + ".png");
            }
        }
        // Shared VisNetRender bindings and the two dynamically selected frame families.
        for (String name : new String[]{"misc/particles", "misc/particles2", "misc/nodes", "misc/p_large", "misc/p_small", "misc/beamh"}) {
            exists("textures/" + name + ".png");
        }
        exists("textures/models/hemis.obj");
        for (int frame = 0; frame < 16; frame++) exists("textures/models/hemis" + frame + ".png");
        for (int frame = 1; frame < 16; frame++) exists("textures/models/ripple" + frame + ".png");
    }
}
