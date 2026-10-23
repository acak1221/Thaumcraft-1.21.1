package thaumcraft.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Check declarations too: a runtime registry can hide a rejected duplicate supplier. */
class NuggetOwnershipTest {
    @Test void everyLiteralItemIdHasExactlyOneOwner() throws Exception {
        var owners = new TreeMap<String, ArrayList<String>>();
        var declaration = Pattern.compile("\\bITEMS\\.register\\(\\s*\"([^\"]+)\"");
        try (var files = Files.walk(Path.of("src/main/java/thaumcraft/common/config"))) {
            for (var file : files.filter(p -> p.toString().endsWith(".java")).sorted().toList()) {
                var matcher = declaration.matcher(Files.readString(file));
                while (matcher.find()) owners.computeIfAbsent(matcher.group(1), key -> new ArrayList<>()).add(file.getFileName().toString());
            }
        }
        var duplicates = new TreeMap<>(owners);
        duplicates.entrySet().removeIf(entry -> entry.getValue().size() == 1);
        assertEquals("{}", duplicates.toString(), "duplicate literal item registrations");
        for (String id : new String[] {"thaumium_nugget", "void_nugget"}) {
            assertEquals(java.util.List.of("ThaumcraftContent.java"), owners.get(id));
        }
    }

    @Test void wandIngredientsAliasTheBehavioralOwner() {
        assertSame(ThaumcraftContent.MATERIALS.get("thaumium_nugget"), ThaumcraftWands.THAUMIUM_NUGGET);
        assertSame(ThaumcraftContent.MATERIALS.get("void_nugget"), ThaumcraftWands.VOID_NUGGET);
    }
}
