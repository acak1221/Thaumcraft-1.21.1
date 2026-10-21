package thaumcraft.common.mixin;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Minecraft 1.21.1 calls every world whose level stems are not vanilla-like an
 * "experimental" world and asks the player to confirm it before the world is loaded.
 *
 * <p>The rule is {@code net.minecraft.client.gui.screens.worldselection.WorldOpenFlows}:
 * <pre>
 * boolean bl3 = bl2 = worldData.worldGenSettingsLifecycle() != Lifecycle.stable();
 * if (bl || bl2) { this.askForBackup(...); return; }
 * </pre>
 * and the lifecycle itself is built in {@code net.minecraft.world.level.levelgen.WorldDimensions.bake}:
 * <ul>
 *   <li>the baked level stem registry is {@code Lifecycle.experimental()} as soon as the
 *       world has more than the three vanilla level stems
 *       ({@code list.size() == VANILLA_DIMENSION_COUNT ? Lifecycle.stable() : Lifecycle.experimental()});</li>
 *   <li>every level stem that is not {@code minecraft:overworld}, {@code minecraft:the_nether}
 *       or {@code minecraft:the_end} is registered with
 *       {@code WorldDimensions.checkStability(...)} = {@code Lifecycle.experimental()}, and
 *       {@code MappedRegistry.register} adds that lifecycle to the registry.</li>
 * </ul>
 *
 * <p>The TC4 port registers the Outer Lands as a real level stem
 * ({@code data/thaumcraft/dimension/outer_lands.json}, read into
 * {@code minecraft:level_stem}), because that is the only way 1.21.1 creates the level
 * ({@code MinecraftServer.createLevels} walks the level stem registry). The port supports
 * that dimension completely, so the vanilla "this world is not supported" question is a
 * porting artifact of the port's own dimension and not a property of the world.
 *
 * <p>The level stem registry therefore reports the stable lifecycle, and the vanilla world
 * open flow takes its normal path. The genuine vanilla question about a legacy customised
 * world ({@code WorldOptions.isOldCustomizedWorld}) is untouched.
 */
@Mixin(MappedRegistry.class)
public abstract class Tc4LevelStemLifecycleMixin {
    @Inject(method = "registryLifecycle", at = @At("HEAD"), cancellable = true)
    private void thaumcraft$stableLevelStems(CallbackInfoReturnable<Lifecycle> cir) {
        if (((MappedRegistry<?>) (Object) this).key().equals(Registries.LEVEL_STEM)) {
            cir.setReturnValue(Lifecycle.stable());
        }
    }
}
