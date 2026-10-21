package thaumcraft.common.testing;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockAiry;
import thaumcraft.common.blocks.BlockCosmeticOpaque;
import thaumcraft.common.blocks.BlockCosmeticSolid;
import thaumcraft.common.blocks.BlockEldritch;
import thaumcraft.common.lib.world.FamilyVariant;

/**
 * Minecraft 1.21.1 wants a model for every state of every block, and the client logs each state whose
 * blockstate file has no entry as "missing model for variant". This reads the REAL registered blocks
 * (not a list): for every Thaumcraft block whose only property is the original metadata "variant",
 * every state must be named by the packaged blockstate file; and the blocks that the hardness groups
 * split off the original metadata families must own exactly the metadata of their group, so that no
 * state exists that no block can ever place. Not a client check: the client log is what shows the
 * warnings themselves.
 */
public final class FamilyBlockStatesRuntimeChecks {
    private FamilyBlockStatesRuntimeChecks() {}

    private static JsonObject blockstate(String path) {
        try (var stream = Thaumcraft.class.getResourceAsStream("/assets/thaumcraft/blockstates/" + path + ".json")) {
            if (stream == null) return null;
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static Set<Integer> values(Block block, Property<?> property) {
        Set<Integer> values = new TreeSet<>();
        for (BlockState state : block.getStateDefinition().getPossibleStates()) values.add((Integer) state.getValue(property));
        return values;
    }

    public static void run(GameTestHelper test) {
        List<String> uncovered = new ArrayList<>();
        int checked = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            var id = BuiltInRegistries.BLOCK.getKey(block);
            if (!Thaumcraft.MODID.equals(id.getNamespace())) continue;
            var properties = block.getStateDefinition().getProperties();
            if (properties.size() != 1 || !FamilyVariant.NAME.equals(properties.iterator().next().getName())) continue;
            JsonObject file = blockstate(id.getPath());
            if (file == null || !file.has("variants")) continue;
            Property<?> property = properties.iterator().next();
            Set<String> named = new TreeSet<>(file.getAsJsonObject("variants").keySet());
            Set<String> missing = new TreeSet<>();
            for (int value : values(block, property)) if (!named.contains("variant=" + value)) missing.add("variant=" + value);
            checked++;
            if (!missing.isEmpty()) uncovered.add(id + " " + missing);
        }
        test.assertTrue(uncovered.isEmpty(), "every state of every variant block has a model entry; missing: " + uncovered);
        test.assertTrue(checked >= 10, "the check really visited the variant blocks: " + checked);

        // The group blocks own exactly the metadata of their group, and their blockstate file names exactly those.
        int groups = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            var id = BuiltInRegistries.BLOCK.getKey(block);
            if (!Thaumcraft.MODID.equals(id.getNamespace())) continue;
            List<Integer> hosted = switch (block) {
                case BlockCosmeticSolid solid -> solid.variants();
                case BlockAiry airy -> airy.variants();
                case BlockEldritch eldritch -> eldritch.variants();
                case BlockCosmeticOpaque opaque -> opaque.variants();
                default -> null;
            };
            if (hosted == null) continue;
            groups++;
            var properties = block.getStateDefinition().getProperties();
            if (hosted.size() < 2) {
                test.assertTrue(properties.isEmpty(), id + " hosts one value and has no property");
                continue;
            }
            Property<?> property = properties.iterator().next();
            test.assertTrue(values(block, property).equals(new TreeSet<>(hosted)),
                    id + " owns exactly its metadata " + hosted + ", not " + values(block, property));
            JsonObject file = blockstate(id.getPath());
            test.assertTrue(file != null && file.has("variants")
                            && file.getAsJsonObject("variants").keySet().equals(hosted.stream().map(value -> "variant=" + value).collect(java.util.stream.Collectors.toSet())),
                    id + " blockstate names exactly " + hosted);
        }
        test.assertTrue(groups >= 12, "the check visited the group blocks of the four families: " + groups);
        test.succeed();
    }
}
