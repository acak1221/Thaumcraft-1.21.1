package thaumcraft.common.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.lib.ArcaneEarNotes;

/** Original NoteBlockEvent.Play point: the vanilla air check has passed, before queuing the audiovisual block event. */
@Mixin(NoteBlock.class)
abstract class ArcaneEarNoteMixin {
    @Inject(method = "playNote", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;blockEvent(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;II)V"))
    private void thaumcraft$hearNote(Entity source, BlockState state, Level level, BlockPos pos, CallbackInfo callback) {
        if (level instanceof ServerLevel server) ArcaneEarNotes.record(server, pos, state.getValue(NoteBlock.INSTRUMENT), state.getValue(NoteBlock.NOTE));
    }
}
