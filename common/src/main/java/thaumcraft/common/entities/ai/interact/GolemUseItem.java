package thaumcraft.common.entities.ai.interact;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.entities.golems.GolemHarvestPlatform;
import thaumcraft.common.items.golems.GolemMarker;
import thaumcraft.common.lib.golems.GolemAiRules;

/**
 * Original {@code AIUseItem} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/interact/AIUseItem.java}).
 *
 * <p>The golem walks to the marked block that matches the item it carries and uses the item on it. The marked block has to
 * be empty when the golem may place things and solid when it may use things, the block behind the marker has to be free,
 * and the marker colour has to be one of the colours of the golem's slots that match the carried item.</p>
 *
 * <p>The original registered this goal with priority 0, together with the home replacement, so the Use work always wins
 * over the inventory work.</p>
 *
 * <p>The click itself needs a player actor: TC4 used a fake player with the profile "FakeThaumcraftGolem". The common
 * module cannot create one (the native fake player lives in the loader module), so the actor is a constructor dependency;
 * the default looks the native machine player up by its public UUID.</p>
 */
public final class GolemUseItem extends Goal {
    /** The original 1.7 goal priority of {@code AIUseItem}. */
    public static final int PRIORITY = 0;

    private final EntityGolemBase golem;
    private final GolemAiRules.Cargo cargo;
    private final Supplier<Player> actor;
    private GolemMarker marker;
    private BlockPos target;
    private BlockState expected;
    private int count;
    private int nextTick;

    public GolemUseItem(EntityGolemBase golem) {
        this(golem, GolemAiRules.cargoOf(golem), GolemUseItem::machineActor);
    }

    public GolemUseItem(EntityGolemBase golem, GolemAiRules.Cargo cargo, Supplier<Player> actor) {
        this.golem = golem;
        this.cargo = cargo;
        this.actor = actor;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.nextTick = golem.tickCount + golem.getRandom().nextInt(6);
    }

    /** The native machine actor, looked up by the public machine UUID; null when the loader did not publish one. */
    private static Player machineActor() {
        return null; // Resolved per level by the goal itself; see resolveActor.
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public boolean canUse() {
        ItemStack carried = cargo.carried();
        if (carried == null || carried.isEmpty()) return false;
        if (golem.tickCount < nextTick) return false;
        if (!golem.getNavigation().isDone()) return false;
        if (!findSomething()) return false;
        int budget = Math.max(1, GolemAiRules.USE_ITEM_TICK_BASE - (5 - golem.tickCount % GolemAiRules.USE_ITEM_TICK_BASE));
        nextTick = golem.tickCount + budget * GolemAiRules.USE_ITEM_TICK_SPAN;
        return true;
    }

    @Override public boolean canContinueToUse() {
        if (target == null || expected == null) return false;
        if (!golem.level().getBlockState(target).equals(expected)) return false;
        return --count > 0 && !golem.getNavigation().isDone();
    }

    @Override public void start() {
        count = GolemAiRules.pathBudget();
        golem.getNavigation().moveTo(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5,
                golem.getAttributeValue(Attributes.MOVEMENT_SPEED));
    }

    @Override public void stop() {
        count = 0;
        golem.getNavigation().stop();
    }

    @Override public void tick() {
        if (target == null) return;
        golem.getLookControl().setLookAt(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, 30, 30);
        double distance = golem.distanceToSqr(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
        if (distance > GolemAiRules.USE_ITEM_REACH_SQ) return;
        click();
    }

    /** Original {@code click}: the carried item is used on the marked block through the marked face. */
    private void click() {
        if (marker == null || target == null) return;
        if (!(golem.level() instanceof ServerLevel server)) return;
        Player player = resolveActor(server);
        if (player == null) return;
        Direction face = marker.face();
        BlockPos adjacent = target.relative(face);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(target), face, target, false);
        ItemStack carried = cargo.carried();
        if (carried == null || carried.isEmpty()) return;
        BlockState state = server.getBlockState(target);
        player.moveTo(golem.getX(), golem.getY(), golem.getZ(), golem.getYRot(), golem.getXRot());
        ItemStack before = carried.copy();
        state.useItemOn(carried, server, player, InteractionHand.MAIN_HAND, hit);
        if (!ItemStack.isSameItemSameComponents(before, carried) || before.getCount() != carried.getCount()) {
            cargo.setCarried(carried);
        }
        count = 0;
        golem.getNavigation().stop();
        if (!server.getBlockState(adjacent).isAir()) server.blockUpdated(adjacent, server.getBlockState(adjacent).getBlock());
    }

    private Player resolveActor(ServerLevel server) {
        Player supplied = actor == null ? null : actor.get();
        if (supplied != null) return supplied;
        return server.getServer().getPlayerList().getPlayer(GolemHarvestPlatform.MACHINE_UUID);
    }

    /**
     * Original {@code findSomething}: the markers whose colour matches one of the golem's slots that hold the carried item,
     * and whose block may be used or filled.
     */
    private boolean findSomething() {
        ItemStack carried = cargo.carried();
        if (carried == null || carried.isEmpty()) return false;
        if (!(golem.level() instanceof ServerLevel server)) return false;
        List<Integer> matchingColors = golem.colorsMatching(carried);
        for (int color : matchingColors) {
            for (GolemMarker candidate : golem.getMarkers()) {
                if (!GolemMarker.accepts(color, candidate.color())) continue;
                BlockPos pos = candidate.pos();
                if (!server.hasChunkAt(pos)) continue;
                boolean free = server.getBlockState(pos).isAir();
                if (free && !server.getBlockState(pos.relative(candidate.face())).isAir()) continue;
                marker = candidate;
                target = pos.immutable();
                expected = server.getBlockState(pos);
                return true;
            }
        }
        return false;
    }

    /** The marker the golem is about to use. */
    public GolemMarker marker() { return marker; }

    /** The entity that carries the marker list; the original's {@code theGolem}. */
    public Entity owner() { return golem; }
}
