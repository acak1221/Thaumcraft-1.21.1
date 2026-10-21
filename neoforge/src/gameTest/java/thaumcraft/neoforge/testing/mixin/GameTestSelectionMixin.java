package thaumcraft.neoforge.testing.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.TestFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Diagnostic selection only; the default run still requires every authored case.
 *
 * <p>The NeoForge module owns the same focused diagnostics as the Fabric harness: the system
 * property {@code thaumcraft.gameTestFilter} selects a comma separated list of scenario names, and
 * an unknown or ambiguous name is an error instead of a silent empty run.</p>
 */
@Mixin(GameTestRegistry.class)
public abstract class GameTestSelectionMixin {
    @ModifyReturnValue(method = "getAllTestFunctions", at = @At("RETURN"))
    private static Collection<TestFunction> thaumcraft$selectDiagnostics(Collection<TestFunction> functions) {
        String filter = System.getProperty("thaumcraft.gameTestFilter", "").trim();
        if (filter.isEmpty()) return functions;
        var selected = new LinkedHashSet<TestFunction>();
        for (String value : filter.split(",", -1)) {
            String name = value.trim().toLowerCase(Locale.ROOT);
            var matches = new ArrayList<TestFunction>();
            for (var function : functions) {
                String actual = function.testName().toLowerCase(Locale.ROOT);
                if (!name.isEmpty() && (actual.equals(name) || actual.endsWith("." + name))) matches.add(function);
            }
            if (matches.size() != 1) throw new IllegalArgumentException("Expected one GameTest for '" + value + "', found " + matches.size());
            selected.add(matches.getFirst());
        }
        thaumcraft.common.Thaumcraft.LOG.info("Focused GameTest diagnostics: {}", selected.stream().map(TestFunction::testName).toList());
        return List.copyOf(selected);
    }
}
