package thaumcraft.common.entities.ai.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import thaumcraft.common.lib.golems.GolemAiRules;

/**
 * Original {@code AIOpenDoor} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/misc/AIOpenDoor.java}).
 *
 * <p>The golem opens the door or the fence gate on its path and closes it again behind itself. The door stays open for
 * twenty ticks, and it is only closed when the goal was asked to close doors at all.</p>
 *
 * <p>The original registered this goal with priority 5, above the return home goal.</p>
 */
public final class GolemOpenDoor extends GolemDoorInteract {
    /** The original 1.7 goal priority of {@code AIOpenDoor}. */
    public static final int PRIORITY = 5;

    private final boolean closeDoors;
    private int openTicks;

    public GolemOpenDoor(Mob entity, boolean closeDoors) {
        super(entity);
        this.closeDoors = closeDoors;
    }

    @Override public boolean canContinueToUse() { return closeDoors && openTicks > 0 && super.canContinueToUse(); }

    @Override public void start() {
        openTicks = GolemAiRules.OPEN_DOOR_TICKS;
        super.start();
        BlockPos pos = new BlockPos(entityPosX, entityPosY, entityPosZ);
        if (targetDoor == null || targetDoor.isAir()) return;
        if (targetDoor.getBlock() instanceof DoorBlock door) {
            door.setOpen(theEntity, theEntity.level(), targetDoor, pos, true);
            theEntity.level().playSound(null, pos, net.minecraft.sounds.SoundEvents.WOODEN_DOOR_OPEN,
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            return;
        }
        if (targetDoor.getBlock() instanceof FenceGateBlock && !targetDoor.getValue(BlockStateProperties.OPEN)) {
            theEntity.level().setBlock(pos, targetDoor.setValue(BlockStateProperties.OPEN, true), 3);
            theEntity.level().playSound(null, pos, net.minecraft.sounds.SoundEvents.FENCE_GATE_OPEN,
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override public void stop() {
        if (!closeDoors) return;
        BlockPos pos = new BlockPos(entityPosX, entityPosY, entityPosZ);
        if (targetDoor == null || targetDoor.isAir()) return;
        if (targetDoor.getBlock() instanceof DoorBlock door) {
            door.setOpen(theEntity, theEntity.level(), targetDoor, pos, false);
            theEntity.level().playSound(null, pos, net.minecraft.sounds.SoundEvents.WOODEN_DOOR_CLOSE,
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            return;
        }
        if (targetDoor.getBlock() instanceof FenceGateBlock && targetDoor.getValue(BlockStateProperties.OPEN)) {
            theEntity.level().setBlock(pos, targetDoor.setValue(BlockStateProperties.OPEN, false), 3);
            theEntity.level().playSound(null, pos, net.minecraft.sounds.SoundEvents.FENCE_GATE_CLOSE,
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override public void tick() {
        if (openTicks > 0) --openTicks;
        super.tick();
    }

    /** The server level the door lives in, when there is one. */
    protected ServerLevel level() { return theEntity.level() instanceof ServerLevel server ? server : null; }
}
