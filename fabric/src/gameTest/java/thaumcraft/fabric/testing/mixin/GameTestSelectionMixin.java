package thaumcraft.fabric.testing.mixin;

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

/** Default runs require every acceptance case; remote footprint probes are explicit diagnostics. */
@Mixin(GameTestRegistry.class)
public abstract class GameTestSelectionMixin {
    @ModifyReturnValue(method="getAllTestFunctions",at=@At("RETURN"))
    private static Collection<TestFunction> thaumcraft$selectDiagnostics(Collection<TestFunction> functions) {
        String filter=System.getProperty("thaumcraft.gameTestFilter","").trim();
        if(filter.isEmpty()) {
            // Explicit footprint diagnostics create remote chunks on purpose;
            // do not increase the memory footprint of ordinary acceptance runs.
            var acceptance = new ArrayList<TestFunction>();
            for (var function : functions) {
                String name = function.testName().toLowerCase(Locale.ROOT);
                if (!name.endsWith(".regenerationfootprint") && !name.endsWith(".structurecandidatefootprint")) acceptance.add(function);
            }
            return List.copyOf(acceptance);
        }
        var selected=new LinkedHashSet<TestFunction>();
        for(String value:filter.split(",",-1)){
            String name=value.trim().toLowerCase(Locale.ROOT);
            var matches=new ArrayList<TestFunction>();
            for(var function:functions){
                String actual=function.testName().toLowerCase(Locale.ROOT);
                if(!name.isEmpty()&&(actual.equals(name)||actual.endsWith("."+name)))matches.add(function);
            }
            if(matches.size()!=1)throw new IllegalArgumentException("Expected one GameTest for '"+value+"', found "+matches.size());
            selected.add(matches.getFirst());
        }
        thaumcraft.common.Thaumcraft.LOG.info("Focused GameTest diagnostics: {}",selected.stream().map(TestFunction::testName).toList());
        return List.copyOf(selected);
    }
}
