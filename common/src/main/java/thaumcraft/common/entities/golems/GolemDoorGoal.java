package thaumcraft.common.entities.golems;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import thaumcraft.common.lib.golems.GolemDoorRules;

/**
 * The door goal of the original, reproduced.
 *
 * <p>Oracle {@code thaumcraft/common/entities/ai/misc/AIDoorInteract.java:24-95} and
 * {@code …/AIOpenDoor.java:117-171}. The original goal</p>
 *
 * <ul>
 * <li>runs only while the golem is <b>horizontally blocked</b>
 *     ({@code AIDoorInteract.func_75250_a}:40-42 — the 1.7.10 field {@code collidedHorizontally},
 *     the 1.21.1 field {@code horizontalCollision}) and while it has an <b>unfinished path</b>
 *     ({@code :45-46});</li>
 * <li>scans the <b>next two points of the current path</b> — the loop bound is
 *     {@code Math.min(currentPathIndex + 2, pathLength)} — and takes the first point within a
 *     <b>squared distance of 2.25</b> of the golem that holds a door ({@code :47-57});</li>
 * <li>falls back to the <b>golem's own block</b> when no path point holds a door ({@code :58-65});</li>
 * <li>accepts only the <b>plain wooden door</b> and the <b>fence gate</b>
 *     ({@code findUsableDoor}:91-94);</li>
 * <li>opens the door for <b>twenty ticks</b> ({@code AIOpenDoor.func_75249_e}:135) and closes it
 *     again when the goal stops ({@code func_75251_c}:152-164).</li>
 * </ul>
 *
 * <p>Two quirks of the 4.2.3.5 jar are reproduced on purpose:</p>
 *
 * <ul>
 * <li>{@code AIOpenDoor.func_75249_e} does <b>not</b> call the base {@code startExecuting}: the
 *     counter stays at the 200 of {@code shouldExecute} and the two direction vectors of the base
 *     class stay zero, so the dot product of {@code AIDoorInteract.func_75246_d}:82-89 is always
 *     zero and {@code hasStoppedDoorInteraction} is never set. The door therefore stays open for
 *     the whole twenty ticks. (The 1.12.2 reference does call the base method and stops the
 *     interaction as soon as the golem passes the door; the 4.2.3.5 jar wins.)</li>
 * <li>the fence gate is only opened when it is closed, and its direction is turned towards the
 *     golem when the gate faces the other way ({@code AIOpenDoor.func_75249_e}:139-148).</li>
 * </ul>
 *
 * <p>The 1.7.10 goal took <b>no</b> mutex bits, so it ran beside every other goal of the golem.
 * The 1.21.1 selector has no such goal, so this one takes the {@code JUMP} flag, which no other
 * goal of the golem uses, and it is not interruptable.</p>
 */
final class GolemDoorGoal extends Goal {
    private final EntityGolemBase golem;
    private BlockPos door;
    private boolean gate;
    /** {@code AIDoorInteract.count}: 200 after the door was found, one less every tick. */
    private int count;
    /** {@code AIOpenDoor.field_75360_j}: the twenty ticks the door stays open. */
    private int openTicks;

    GolemDoorGoal(EntityGolemBase golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Flag.JUMP));
    }

    @Override public boolean canUse() {
        // Original AIDoorInteract.func_75250_a:40-46.
        if (!golem.canWork()) return false;
        if (!golem.horizontalCollision) return false;
        Path path = golem.getNavigation().getPath();
        if (path == null || path.isDone()) return false;
        // Original :47-57: the next two points of the path.
        int limit = GolemDoorRules.scanLimit(path.getNextNodeIndex(), path.getNodeCount());
        for (int i = 0; i < limit; i++) {
            BlockPos point = path.getNodePos(i);
            if (!GolemDoorRules.withinReach(golem.distanceToSqr(point.getX(), golem.getY(), point.getZ()))) continue;
            if (!select(point)) continue;
            count = GolemDoorRules.counter();
            return true;
        }
        // Original :58-65: the golem's own block is the fallback.
        if (!select(golem.blockPosition())) return false;
        count = GolemDoorRules.counter();
        return true;
    }

    @Override public boolean canContinueToUse() {
        // Original AIOpenDoor.func_75253_b:130 — the twenty tick window and the counter of the base class.
        return openTicks > 0 && count > 0;
    }

    @Override public void start() {
        // Original AIOpenDoor.func_75249_e:134-135 — the base startExecuting is NOT called.
        openTicks = GolemDoorRules.openTicks();
        open();
    }

    @Override public void tick() {
        if (--openTicks <= 0) return;
        // Original AIDoorInteract.func_75246_d:82 — the counter of the base class.
        --count;
    }

    @Override public void stop() {
        // Original AIOpenDoor.func_75251_c:152-164 — the door is closed again.
        close();
        door = null;
    }

    @Override public boolean isInterruptable() { return false; }

    /** Original AIDoorInteract.findUsableDoor:91-94 — the plain wooden door or the fence gate. */
    private boolean select(BlockPos pos) {
        if (!(golem.level() instanceof ServerLevel server)) return false;
        BlockState state = server.getBlockState(pos);
        if (state.is(Blocks.OAK_DOOR)) { door = pos.immutable(); gate = false; return true; }
        if (state.is(Blocks.OAK_FENCE_GATE)) { door = pos.immutable(); gate = true; return true; }
        return false;
    }

    private void open() {
        if (door == null || !(golem.level() instanceof ServerLevel server)) return;
        BlockState state = server.getBlockState(door);
        if (!gate) {
            if (state.getBlock() instanceof DoorBlock doorBlock) doorBlock.setOpen(golem, server, state, door, true);
            return;
        }
        if (!(state.getBlock() instanceof FenceGateBlock)) return;
        if (state.getValue(FenceGateBlock.OPEN)) return; // The original opens a closed gate only.
        BlockState next = state;
        // Original AIOpenDoor.func_75249_e:141-146 — a gate that faces the other way is turned first.
        Direction facing = Direction.fromYRot(golem.getYRot());
        Direction gateFacing = state.getValue(HorizontalDirectionalBlock.FACING);
        if (GolemDoorRules.gateIndex(gateFacing) == (GolemDoorRules.gateIndex(facing) + 2) % 4) {
            next = next.setValue(HorizontalDirectionalBlock.FACING, facing);
        }
        next = next.setValue(FenceGateBlock.OPEN, true);
        server.setBlock(door, next, 3);
        server.playSound(null, door, SoundEvents.FENCE_GATE_OPEN, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private void close() {
        if (door == null || !(golem.level() instanceof ServerLevel server)) return;
        BlockState state = server.getBlockState(door);
        if (!gate) {
            if (state.getBlock() instanceof DoorBlock doorBlock) doorBlock.setOpen(golem, server, state, door, false);
            return;
        }
        if (!(state.getBlock() instanceof FenceGateBlock) || !state.getValue(FenceGateBlock.OPEN)) return;
        server.setBlock(door, state.setValue(FenceGateBlock.OPEN, false), 3);
        server.playSound(null, door, SoundEvents.FENCE_GATE_CLOSE, SoundSource.BLOCKS, 1.0F, 1.0F);
    }
}
