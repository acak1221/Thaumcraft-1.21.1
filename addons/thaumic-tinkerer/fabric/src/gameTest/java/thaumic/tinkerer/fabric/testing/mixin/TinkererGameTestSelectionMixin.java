package thaumic.tinkerer.fabric.testing.mixin;

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
import thaumic.tinkerer.common.ThaumicTinkerer;

/**
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on; modelled on the selection mixin
 * of the Thaumcraft harness.
 *
 * <p>{@code -PtinkererGameTestFilter=a,b,group*} (system property {@code thaumictinkerer.gameTestFilter}) runs only the
 * named tests: an entry is a test name (the part after the class name, or the whole lower-case name), or ends with
 * {@code *} to take every test whose lower-case name starts with the text before it (e.g. {@code kamiworldgametests.*}
 * for a whole group). An entry that matches nothing is an error, so a typo cannot silently run nothing. Without the
 * property every test runs. The tests of the Thaumcraft harness are not part of this run.</p>
 */
@Mixin(GameTestRegistry.class)
public abstract class TinkererGameTestSelectionMixin {
    @ModifyReturnValue(method = "getAllTestFunctions", at = @At("RETURN"))
    private static Collection<TestFunction> thaumictinkerer$select(Collection<TestFunction> functions) {
        String filter = System.getProperty("thaumictinkerer.gameTestFilter", "").trim();
        if (filter.isEmpty()) return functions;
        var selected = new LinkedHashSet<TestFunction>();
        for (String value : filter.split(",", -1)) {
            String name = value.trim().toLowerCase(Locale.ROOT);
            if (name.isEmpty()) continue;
            boolean prefix = name.endsWith("*");
            if (prefix) name = name.substring(0, name.length() - 1);
            var matches = new ArrayList<TestFunction>();
            for (var function : functions) {
                String actual = function.testName().toLowerCase(Locale.ROOT);
                boolean hit = prefix ? actual.startsWith(name) || actual.contains("." + name)
                        : actual.equals(name) || actual.endsWith("." + name);
                if (hit) matches.add(function);
            }
            if (matches.isEmpty() || (!prefix && matches.size() != 1)) {
                throw new IllegalArgumentException("Expected " + (prefix ? "at least one" : "one") + " GameTest for '" + value
                        + "', found " + matches.size());
            }
            selected.addAll(matches);
        }
        ThaumicTinkerer.LOG.info("Focused GameTests: {}", selected.stream().map(TestFunction::testName).toList());
        return List.copyOf(selected);
    }
}
