package thaumic.tinkerer.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumic.tinkerer.client.MachinesClient;
import thaumic.tinkerer.client.hooks.ItemRenderers;

/**
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 *
 * <p>The client wiring of the Machines group, which no server can run (a GameTest server cannot load the client classes
 * that the renderer's signature names: {@code PoseStack} is stripped): the Mob Display's built-in item renderer is queued
 * once on the loader layer's {@code ItemRenderers} from {@code MachinesClient.register()}, and the group's own Fabric glue of
 * the first version is gone, because two registrations of the same item are an error. The queue is read with
 * {@code ItemRenderers.get}; the registration of the real item {@code mob_display} is checked on the source, because a JUnit
 * test has no registry of the mod. Nothing here draws anything: the rendering of the mob has not been seen on a frame. The tests
 * run in {@code addons/thaumic-tinkerer}.</p>
 */
class MachinesClientWiringTest {
    private static final Path CLIENT = Path.of("common", "src", "main", "java", "thaumic", "tinkerer", "client", "MachinesClient.java");
    private static final Path OLD_GLUE = Path.of("fabric", "src", "main", "java", "thaumic", "tinkerer", "fabric", "client", "MachinesFabricClient.java");

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    /** The code of a source file without comments, so that a word in a Javadoc does not count. */
    private static String code(Path file) throws IOException {
        return Files.readString(file).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
    }

    private static String body(String code, String signature) {
        int start = code.indexOf(signature);
        assertTrue(start >= 0, signature + " exists");
        int open = code.indexOf('{', start);
        int depth = 0;
        for (int i = open; i < code.length(); i++) {
            if (code.charAt(i) == '{') depth++;
            if (code.charAt(i) == '}' && --depth == 0) return code.substring(open + 1, i);
        }
        throw new AssertionError("unbalanced braces after " + signature);
    }

    @Test
    void theHelperQueuesTheMobDisplayRendererForTheItemItIsGivenAndNoOther() {
        assertNull(ItemRenderers.get(Items.CLAY_BALL), "nothing is queued for the item before");
        MachinesClient.registerItemRenderers(Items.CLAY_BALL);
        assertNotNull(ItemRenderers.get(Items.CLAY_BALL), "the renderer is queued for the item");
        assertNull(ItemRenderers.get(Items.BRICK), "and for no other item");
        assertThrows(IllegalStateException.class, () -> MachinesClient.registerItemRenderers(Items.CLAY_BALL),
                "a second registration of the same item is refused: nothing else may register the Mob Display");
        // The queued renderer is RenderMobDisplay's: for a stack that is not a Mob Display it draws nothing, before it asks
        // the client for anything (null pose and buffers: it must not touch them).
        ItemRenderers.get(Items.CLAY_BALL).render(new ItemStack(Items.CLAY_BALL), ItemDisplayContext.GUI, null, null, 0, 0);
    }

    @Test
    void theClientRegistrarQueuesTheRealMobDisplayFirstAndOnce() throws IOException {
        String code = code(CLIENT);
        String register = body(code, "public static void register()");
        assertTrue(register.strip().startsWith("registerItemRenderers(TTRegistry.item(LibItemNames.MOB_DISPLAY));"),
                "register() queues the renderer of the item mob_display first");
        assertEquals(1, register.split("registerItemRenderers\\(", -1).length - 1, "and does it once");
        assertTrue(body(code, "public static void registerItemRenderers(Item mobDisplay)").contains(
                "ItemRenderers.register(mobDisplay, RenderMobDisplay::render);"), "with RenderMobDisplay::render");
        assertTrue(code.contains("import thaumic.tinkerer.client.hooks.ItemRenderers;"));
    }

    @Test
    void thereIsNoSecondPathToTheSameRenderer() throws IOException {
        assertFalse(Files.exists(OLD_GLUE), "the group's own Fabric glue of the renderer is gone");
        String code = code(CLIENT);
        assertFalse(code.contains("Class.forName"), "no loader class is reached by name");
        assertFalse(code.contains("Platform."), "no platform test");
        assertFalse(code.contains("MachinesFabricClient"));

        // the only user of Fabric's registry is the loader layer's backend
        List<String> users = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of("fabric", "src", "main"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                if (code(file).contains("BuiltinItemRendererRegistry")) users.add(file.getFileName().toString());
            }
        }
        assertEquals(List.of("FabricItemRenderers.java"), users, "one registration point per item: the backend of ItemRenderers");
    }
}
