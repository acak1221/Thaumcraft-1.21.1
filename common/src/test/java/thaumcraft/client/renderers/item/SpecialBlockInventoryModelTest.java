package thaumcraft.client.renderers.item;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class SpecialBlockInventoryModelTest {
    @Test void brainInventoryTurnsTheContentAboutTheJarCentre() {
        var pose = new PoseStack();
        JarBrainItemRenderer.contentPose(pose);
        Vector3f centre = pose.last().pose().transformPosition(new Vector3f(.5F, .5F, .5F));
        assertEquals(.5F, centre.x, 1E-6F); assertEquals(.5F, centre.y, 1E-6F); assertEquals(.5F, centre.z, 1E-6F);
        Vector3f front = pose.last().pose().transformPosition(new Vector3f(.75F, .5F, .5F));
        assertEquals(.5F, front.x, 1E-6F); assertEquals(.5F, front.y, 1E-6F); assertEquals(.25F, front.z, 1E-6F);
    }

    @Test void alembicPreservesTheOriginalInventoryDispatchersVerticalBasis() {
        var pose = new PoseStack();
        pose.translate(-.5F, -.5F, -.5F);
        AlembicItemRenderer.inventoryPose(pose);
        Vector3f origin = pose.last().pose().transformPosition(new Vector3f());
        assertEquals(-.5F, origin.x, 1E-6F);
        assertEquals(0, origin.y, 1E-6F);
        assertEquals(-.5F, origin.z, 1E-6F);
    }

    @Test void theSpecialItemsUseBuiltinHooksAndTheNativeBlockCallerBasis() throws Exception {
        JsonObject jar = resource("jar");
        for (String name : new String[]{"jar_brain", "flux_scrubber", "essentia_reservoir", "alembic"}) {
            JsonObject item = resource(name);
            assertEquals("minecraft:builtin/entity", item.get("parent").getAsString());
            assertEquals(jar.get("display"), item.get("display"));
            assertFalse(item.has("elements"), "the complete original meshes are supplied by their builtin hooks");
        }
    }

    private static JsonObject resource(String name) throws Exception {
        try (var stream = SpecialBlockInventoryModelTest.class.getClassLoader()
                .getResourceAsStream("assets/thaumcraft/models/item/" + name + ".json")) {
            assertNotNull(stream);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
