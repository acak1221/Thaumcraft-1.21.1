package thaumic.tinkerer.fabric.clientsmoke;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import thaumic.tinkerer.client.lib.LibResources;

/**
 * Every {@code LibResources} path (the textures the screens and renderers load by name) exists. Test-only code of the port
 * (no 1.7.10 counterpart); CC BY-NC-SA 3.0 like the add-on, not distributed.
 *
 * <p>The group is the group of the first class of the sources that uses the constant; a constant no class uses is reported
 * as information when its file is missing, since nothing would draw it.</p>
 */
final class GuiTexturesCheck implements SmokeCheck {
    @Override
    public String name() {
        return "gui_and_model_textures";
    }

    @Override
    public void run(SmokeRun run) throws IOException, IllegalAccessException {
        ModelFiles files = new ModelFiles(run.client().getResourceManager());
        List<Path> sources = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(run.sourceRoot().resolve("common/src/main/java/thaumic/tinkerer"))) {
            stream.filter(p -> p.toString().endsWith(".java")).forEach(sources::add);
        }
        int checked = 0;
        for (Field field : LibResources.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != String.class) continue;
            String value = (String) field.get(null);
            if (!value.startsWith("thaumictinkerer:") || !value.endsWith(".png")) continue;
            checked++;
            String user = firstUser(sources, field.getName());
            String group = user == null ? "Unassigned" : run.content().groups().groupOfClass(user);
            ResourceLocation id = ResourceLocation.parse(value);
            if (files.exists(id)) {
                run.report().pass(PHASE, "resource_path", group, "texture:" + id.getPath(), field.getName() + " exists");
            } else if (user == null) {
                run.report().info(PHASE, "unused_resource_constant", "LibResources." + field.getName() + " -> " + id + " does not exist and no class uses it");
            } else if (usedOnlyByUnregisteredRenderer(sources, user)) {
                run.report().info(PHASE, "texture_of_an_unregistered_renderer", "LibResources." + field.getName() + " -> " + id + " does not exist; only " + user
                        + " uses it, and its javadoc says 1.7.10 never registered it either");
            } else {
                run.report().fail(PHASE, "missing_texture", group, "texture:" + id.getPath(),
                        "LibResources." + field.getName() + " -> " + id + " does not exist; used by " + user);
            }
        }
        run.report().info(PHASE, name(), checked + " LibResources paths looked at");
    }

    private static boolean usedOnlyByUnregisteredRenderer(List<Path> sources, String user) throws IOException {
        for (Path file : sources) {
            if (file.getFileName().toString().equals(user + ".java")) return Files.readString(file).contains("never registered");
        }
        return false;
    }

    private static String firstUser(List<Path> sources, String constant) throws IOException {
        for (Path file : sources) {
            String name = file.getFileName().toString();
            if (name.equals("LibResources.java")) continue;
            if (Files.readString(file).contains("LibResources." + constant)) return name.replace(".java", "");
        }
        return null;
    }
}
