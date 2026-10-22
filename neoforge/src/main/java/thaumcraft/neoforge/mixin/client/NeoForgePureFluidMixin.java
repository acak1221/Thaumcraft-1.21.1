package thaumcraft.neoforge.mixin.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.client.renderers.fluid.PureFluidRender;

/**
 * The NeoForge half of the clean water render of the TC4 purifying fluid.
 *
 * <p>NeoForge 21.1 owns no fluid render handler event. Its whole client fluid API is
 * {@code net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions}, and the
 * only drawing hook of that interface is {@code renderFluid(FluidState, BlockAndTintGetter,
 * BlockPos, VertexConsumer, BlockState)}, which
 * {@code net.minecraft.client.renderer.block.BlockRenderDispatcher#renderLiquid} calls before
 * the vanilla {@code LiquidBlockRenderer} draws the fluid. That hook belongs to the
 * {@code FluidType}, and the purifying fluid must share the vanilla water type on NeoForge:
 * the engine compares the type by identity ({@code net.neoforged.neoforge.common.extensions.
 * ILivingEntityExtension#isInFluidType} and {@code FluidType#canRideVehicleUnder}), so an own
 * type would make the purifying fluid invisible to every water rule. The water type already
 * owns its client extension - NeoForge registers it in
 * {@code net.neoforged.neoforge.client.ClientNeoForgeMod#onRegisterClientExtensions} - and
 * {@code net.neoforged.neoforge.client.extensions.common.ClientExtensionsManager#register}
 * refuses a second registration for the same type with an
 * {@code IllegalStateException}. The hook is therefore unreachable for this fluid, and the
 * vanilla draw path is the only remaining place.</p>
 *
 * <p>This mixin draws the shared original geometry of the purifying fluid and cancels the
 * vanilla draw, exactly where the NeoForge hook would have drawn it: at the head of
 * {@code BlockRenderDispatcher#renderLiquid}, before {@code LiquidBlockRenderer#tesselate}.</p>
 *
 * <p>Both targets and their descriptors were read from the NeoForge-patched class
 * {@code net/minecraft/client/renderer/block/BlockRenderDispatcher.class} of the patched
 * Minecraft archive of the NeoForge 21.1.259 userdev jar:</p>
 * <ul>
 *   <li>{@code public void renderLiquid(BlockPos, BlockAndTintGetter, VertexConsumer, BlockState, FluidState)}</li>
 *   <li>{@code public void onResourceManagerReload(ResourceManager)}</li>
 * </ul>
 */
@Mixin(BlockRenderDispatcher.class)
public abstract class NeoForgePureFluidMixin {
    /**
     * The client resource reload of the shared sheet. The Fabric API calls the same reload
     * from its own fluid renderer hook, so both loaders hold the same sprite.
     */
    @Inject(method = "onResourceManagerReload(Lnet/minecraft/server/packs/resources/ResourceManager;)V",
            at = @At("RETURN"), require = 1, allow = 1)
    private void thaumcraft$reloadPureFluidSheet(ResourceManager manager, CallbackInfo ci) {
        PureFluidRender.reload(Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS));
    }

    /** The original Forge Classic geometry of the purifying fluid. */
    @Inject(method = "renderLiquid(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)V",
            at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void thaumcraft$pureFluidRender(BlockPos pos, BlockAndTintGetter level, VertexConsumer out,
                                            BlockState state, FluidState fluid, CallbackInfo ci) {
        if (!PureFluidRender.render(pos, level, out, state, fluid)) return;
        ci.cancel();
    }
}
