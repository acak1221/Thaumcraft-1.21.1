package thaumcraft.common.entities.ai.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import thaumcraft.common.lib.golems.GolemAiRules;

/**
 * Original {@code AIDoorInteract} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/misc/AIDoorInteract.java}).
 *
 * <p>The shared part of the door goals: the mob walks its own path and looks for a door or a fence gate on the way. The
 * first node of the path that is within one and a half blocks and holds a door is the door the goal works on; when the path
 * offers none, the block the mob stands on is checked instead. The goal then counts down two hundred ticks and stops as
 * soon as the mob walks past the door.</p>
 */
public abstract class GolemDoorInteract extends Goal {
    protected final Mob theEntity;
    protected int entityPosX;
    protected int entityPosY;
    protected int entityPosZ;
    protected BlockState targetDoor;
    protected boolean hasStoppedDoorInteraction;
    protected float entityPositionX;
    protected float entityPositionZ;
    protected int count;

    protected GolemDoorInteract(Mob entity) {
        this.theEntity = entity;
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public boolean canUse() {
        Path path = theEntity.getNavigation().getPath();
        if (path != null && !theEntity.getNavigation().isDone()) {
            int nodes = path.getNodeCount();
            for (int index = 0; index < Math.min(nodes + 2, nodes); index++) {
                var node = path.getNode(index);
                entityPosX = node.x;
                entityPosY = node.y;
                entityPosZ = node.z;
                if (!GolemAiRules.doorInReach(theEntity.distanceToSqr(entityPosX + 0.5, theEntity.getY(), entityPosZ + 0.5))) continue;
                targetDoor = findUsableDoor(entityPosX, entityPosY, entityPosZ);
                if (targetDoor == null || targetDoor.isAir()) continue;
                count = GolemAiRules.DOOR_BUDGET;
                return true;
            }
            BlockPos here = theEntity.blockPosition();
            entityPosX = here.getX();
            entityPosY = here.getY();
            entityPosZ = here.getZ();
            targetDoor = findUsableDoor(entityPosX, entityPosY, entityPosZ);
            if (targetDoor != null && !targetDoor.isAir()) count = GolemAiRules.DOOR_BUDGET;
            return targetDoor != null && !targetDoor.isAir();
        }
        return false;
    }

    @Override public boolean canContinueToUse() { return count > 0 && !hasStoppedDoorInteraction; }

    @Override public void start() {
        count = GolemAiRules.DOOR_START_BUDGET;
        hasStoppedDoorInteraction = false;
        entityPositionX = (float) ((double) ((float) entityPosX + 0.5f) - theEntity.getX());
        entityPositionZ = (float) ((double) ((float) entityPosZ + 0.5f) - theEntity.getZ());
    }

    @Override public void tick() {
        if (count > 0) --count;
        float dx = (float) ((double) ((float) entityPosX + 0.5f) - theEntity.getX());
        float dz = (float) ((double) ((float) entityPosZ + 0.5f) - theEntity.getZ());
        if (entityPositionX * dx + entityPositionZ * dz < 0.0F) hasStoppedDoorInteraction = true;
    }

    /** Original {@code findUsableDoor}: an ordinary door or a fence gate, nothing else. */
    protected BlockState findUsableDoor(int x, int y, int z) {
        BlockState state = theEntity.level().getBlockState(new BlockPos(x, y, z));
        if (state.getBlock() instanceof DoorBlock || state.getBlock() instanceof FenceGateBlock) return state;
        return Blocks.AIR.defaultBlockState();
    }
}
