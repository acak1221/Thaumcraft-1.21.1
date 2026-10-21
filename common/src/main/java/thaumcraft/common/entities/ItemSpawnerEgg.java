package thaumcraft.common.entities;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * TC4 ItemSpawnerEgg, the creative spawn egg of the original mod.
 * <p>
 * The original was a single creative item whose metadata selected the mob
 * that it spawned; the name of the egg became the custom name of the mob and
 * the persistent data of the egg was handed to the spawned mob.
 * <p>
 * The native 1.21.1 tree spawns one spawn egg per entity type, and the
 * vanilla {@link SpawnEggItem} already provides: the placement on the clicked
 * face, the spawn into a liquid when the egg is thrown, the custom name of the
 * egg through the custom name component and the persistent entity data of the
 * egg through the entity data component. This subclass keeps the original
 * variant list, the original spawn helper and the original placement, and it
 * applies the persistent entity data of the egg itself so the behaviour does
 * not depend on the vanilla internals.
 */
public class ItemSpawnerEgg extends SpawnEggItem {
    /** The original list of the spawnable variants. */
    private static final List<Variant> VARIANTS = new ArrayList<>();

    /** One original spawn egg variant. */
    public record Variant(String name, EntityType<? extends Mob> type, int background, int highlight) {}

    public ItemSpawnerEgg(EntityType<? extends Mob> type, int background, int highlight, Item.Properties properties) {
        super(type, background, highlight, properties);
    }

    /** Original {@code addMapping}: the name of the mob and its two colours. */
    public static void addMapping(String name, EntityType<? extends Mob> type, int background, int highlight) {
        VARIANTS.add(new Variant(name, type, background, highlight));
    }

    public static List<Variant> variants() {
        return List.copyOf(VARIANTS);
    }

    /**
     * Original {@code spawnCreature}: a random yaw, the vanilla spawn
     * preparation, the name and the persistent data of the egg.
     */
    public static Mob spawnCreature(ServerLevel level, EntityType<?> type, double x, double y, double z,
                                    ItemStack stack, Player player) {
        Entity entity = type.create(level);
        if (!(entity instanceof Mob mob)) return null;
        mob.moveTo(x, y, z, level.random.nextFloat() * 360F, 0F);
        mob.setYBodyRot(mob.getYRot());
        mob.setYHeadRot(mob.getYRot());
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.SPAWN_EGG, null);
        Component name = stack.get(DataComponents.CUSTOM_NAME);
        if (name != null) mob.setCustomName(name);
        CustomData data = stack.get(DataComponents.ENTITY_DATA);
        if (data != null && !data.isEmpty()) data.loadInto(mob);
        level.addFreshEntity(mob);
        return mob;
    }

    /**
     * The original places the mob one block towards the clicked face, in the
     * middle of that block, and half a block higher when the clicked block
     * holds water.
     */
    @Override public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        Direction direction = context.getClickedFace();
        BlockState state = server.getBlockState(clicked);
        boolean creative = creative(context.getPlayer());
        if (server.getBlockEntity(clicked) instanceof Spawner spawner) {
            // The native spawn egg also fills a spawner block.
            spawner.setEntityId(getType(stack), server.getRandom());
            server.sendBlockUpdated(clicked, state, state, 3);
            server.gameEvent(context.getPlayer(), GameEvent.BLOCK_CHANGE, clicked);
            if (spendsStack(creative)) stack.shrink(1);
            return InteractionResult.CONSUME;
        }
        BlockPos target = clicked.relative(direction);
        double offset = direction == Direction.UP && state.getFluidState().is(FluidTags.WATER) ? 0.5D : 0D;
        Mob mob = spawnCreature(server, getType(stack), target.getX() + 0.5D, target.getY() + offset, target.getZ() + 0.5D,
                stack, context.getPlayer());
        if (mob != null) {
            if (spendsStack(creative)) stack.shrink(1);
            server.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, target);
        }
        return InteractionResult.CONSUME;
    }

    /**
     * Original {@code ItemSpawnerEgg.java:99-101} —
     * {@code if (!player.field_71075_bZ.field_75098_d) { --stack.field_77994_a; }}:
     * the creative spawn egg of the original is never spent, because the
     * creative mode of the player is the only thing that decides it. The
     * 1.21.1 {@link SpawnEggItem} always spends the stack (its own
     * {@code useOn} calls {@code itemstack.shrink(1)} in both of its branches),
     * so the original rule has to be reproduced here.
     *
     * @param creative the creative mode of the player that used the egg
     * @return true when the stack of the egg is spent
     */
    public static boolean spendsStack(boolean creative) {
        return !creative;
    }

    /** The creative mode of the player; a player without abilities is never creative. */
    private static boolean creative(Player player) {
        return player != null && player.getAbilities().instabuild;
    }
}
