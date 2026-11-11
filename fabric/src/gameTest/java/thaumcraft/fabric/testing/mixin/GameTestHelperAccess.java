package thaumcraft.fabric.testing.mixin;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Test-mod-only lifetime observation for the inventory scanning checks; no production accessor is installed. */
@Mixin(GameTestHelper.class)
public interface GameTestHelperAccess {
    @Accessor("testInfo") GameTestInfo thaumcraft$testInfo();
}
