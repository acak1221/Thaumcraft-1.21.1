package thaumcraft.common.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import thaumcraft.common.lib.world.ElementalShovelHarvest;
import thaumcraft.common.lib.world.ExcavationHarvest;
import thaumcraft.common.lib.world.StreamAxeHarvest;

/** Native events inject BEFORE these callsites (callback order1000, redirect10000).
 * Required exact matches fail closed on target drift; runtime probes must still
 * demonstrate actual transformed BREAK/BEFORE/CANCELED/AFTER composition. */
@Mixin(ServerPlayerGameMode.class)
public abstract class ExcavationServerPlayerGameModeMixin implements ExcavationHarvest.NativeScope {
    @Shadow protected ServerLevel level;
    @Shadow @Final protected ServerPlayer player;

    @WrapMethod(method = "destroyBlock(Lnet/minecraft/core/BlockPos;)Z", require = 1, allow = 1)
    private boolean thaumcraft$excavationInvocation(BlockPos pos, Operation<Boolean> original) {
        // Axe of the Stream: a claimed chop breaks the endpoint through a nested ordinary
        // call and leaves the anchor; false makes vanilla resynchronise the client's prediction.
        if (StreamAxeHarvest.claim((ServerPlayerGameMode) (Object) this, level, player, pos)) return false;
        // Shovel of the Earthmover: decided before the anchor goes, the other eight blocks follow it by ordinary nested breaks.
        var face = ElementalShovelHarvest.plan((ServerPlayerGameMode) (Object) this, level, player, pos);
        var before = face == null ? null : level.getBlockState(pos);
        var frame = ExcavationHarvest.enterNative((ServerPlayerGameMode) (Object) this, level, player, pos);
        boolean destroyed;
        try { destroyed = original.call(pos); }
        finally { if (frame != null) frame.close(); }
        if (face != null && destroyed) ElementalShovelHarvest.finish((ServerPlayerGameMode) (Object) this, level, player, pos, before, face);
        return destroyed;
    }

    @Unique private ExcavationHarvest.Frame thaumcraft$excavationFrame() {
        return ExcavationHarvest.currentNative((ServerPlayerGameMode) (Object) this);
    }

    @Redirect(method = "destroyBlock(Lnet/minecraft/core/BlockPos;)Z", require = 1, allow = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;playerWillDestroy(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState thaumcraft$directSpellNotPlayerBreak(Block block, Level world, BlockPos pos, BlockState state, Player actor) {
        var frame = thaumcraft$excavationFrame();
        return frame == null ? block.playerWillDestroy(world, pos, state, actor) : frame.snapshot();
    }

    // The three removals below live in the loader specific mixins: the vanilla
    // 1.21.1 call graph of destroyBlock and the NeoForge 21.1 call graph differ
    // (NeoForge routes the removal through its own private removeBlock helper and
    // BlockState.onDestroyedByPlayer, and replaces hasCorrectToolForDrops with
    // BlockState.canHarvestBlock). Both variants keep require=1 and fail closed.
    //   Fabric  : thaumcraft.fabric.mixin.compat.FabricExcavationRemovalMixin
    //   NeoForge: thaumcraft.neoforge.mixin.NeoForgeExcavationRemovalMixin

    @Redirect(method = "destroyBlock(Lnet/minecraft/core/BlockPos;)Z", require = 1, allow = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayerGameMode;isCreative()Z"))
    private boolean thaumcraft$onlyBypassCreativeEarlyReturn(ServerPlayerGameMode mode) {
        return thaumcraft$excavationFrame() == null && mode.isCreative();
    }

    @Redirect(method = "destroyBlock(Lnet/minecraft/core/BlockPos;)Z", require = 1, allow = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;mineBlock(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)V"))
    private void thaumcraft$noSpellToolWear(ItemStack stack, Level world, BlockState state, BlockPos pos, Player actor) {
        if (thaumcraft$excavationFrame() == null) stack.mineBlock(world, state, pos, actor);
    }

    @Redirect(method = "destroyBlock(Lnet/minecraft/core/BlockPos;)Z", require = 1, allow = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;playerDestroy(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/item/ItemStack;)V"))
    private void thaumcraft$nativeDirectDropsWithoutPlayerStats(Block block, Level world, Player actor, BlockPos pos,
                                                               BlockState state, BlockEntity tile, ItemStack tool) {
        var frame = thaumcraft$excavationFrame();
        if (frame == null) block.playerDestroy(world, actor, pos, state, tile, tool);
        else frame.dropCapturedResources();
    }
}
