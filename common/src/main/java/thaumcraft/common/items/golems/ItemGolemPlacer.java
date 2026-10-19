package thaumcraft.common.items.golems;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.entities.golems.EnumGolemType;
import thaumcraft.common.mixin.EntityLevelCallbackAccess;

/** ItemGolemPlacer:164-240, flattened to supported material items. Native useOn replaces Forge's
 * use-first hook; no block is replaced and no research gate is added here. */
public final class ItemGolemPlacer extends Item {
    private final int material;
    private static final ThreadLocal<Boolean> placing = new ThreadLocal<>();

    /** Native ADD callbacks must not recall/work a freshly admitted candidate
     * before this call has settled the real source placer. Server-thread local. */
    public static boolean isPlacementInProgress() { return Boolean.TRUE.equals(placing.get()); }

    public ItemGolemPlacer(Properties properties) { this(properties, 0); }

    public ItemGolemPlacer(Properties properties, int material) {
        super(properties);
        if (material < 0 || material > 7) {
            throw new IllegalArgumentException("Unsupported golem material: " + material);
        }
        this.material = material;
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack held = context.getItemInHand();
        if (player == null || player.isShiftKeyDown()) return InteractionResult.PASS;
        if (held.isEmpty() || held.getItem() != this || player.getItemInHand(context.getHand()) != held
                || !player.isAlive() || player.isSpectator() || player.level() != context.getLevel()) return InteractionResult.FAIL;
        if (configuration(held) == null) return InteractionResult.FAIL;
        if (context.getLevel().isClientSide) return InteractionResult.SUCCESS;
        if (!(context.getLevel() instanceof ServerLevel server) || !server.getServer().isSameThread()
                || Boolean.TRUE.equals(placing.get())) return InteractionResult.FAIL;

        placing.set(true);
        EntityGolemBase golem = null;
        try {
            BlockPos clicked = context.getClickedPos();
            Direction face = context.getClickedFace();
            BlockPos adjacent = clicked.relative(face);
            if (!mayPlace(context, server, player, held, clicked, adjacent, face)) return InteractionResult.FAIL;
            ItemStack before = held.copy();
            GolemData data = configuration(held);
            if (data == null) return InteractionResult.FAIL;
            BlockState clickedState = server.getBlockState(clicked);
            // Original precedence: top of wooden fence OR any side of nether-brick fence.
            double offset = (face == Direction.UP && clickedState.is(Blocks.OAK_FENCE)
                    || clickedState.is(Blocks.NETHER_BRICK_FENCE)) ? .5 : 0;
            double x = adjacent.getX() + .5, y = adjacent.getY() + offset, z = adjacent.getZ() + .5;
            golem = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), server);
            golem.setGolemMaterial(material);
            golem.applyConfiguration(data);
            if (data.core() == GolemData.ALCHEMY) golem.applyAspectCargo(GolemAspectCargo.readPlacer(before));
            else golem.applyWaterCargo(GolemWaterCargo.readPlacer(before));
            Component name = before.get(DataComponents.CUSTOM_NAME);
            golem.setCustomName(name == null ? null : name.copy());
            golem.setOwnerName(player.getGameProfile().getName());
            golem.setPos(x, y, z);
            golem.setHome(BlockPos.containing(x, y, z), face);
            golem.setPersistenceRequired();
            if (!golem.isAlive() || golem.isRemoved() || !mayPlace(context, server, player, held, clicked, adjacent, face)
                    || !server.getBlockState(clicked).equals(clickedState) || !ItemStack.matches(held, before)
                    || !safeBody(server, golem)) {
                discardDetached(golem);
                return InteractionResult.FAIL;
            }
            golem.setYRot(server.random.nextFloat() * 360F);
            golem.setYHeadRot(golem.getYRot());
            golem.setYBodyRot(golem.getYRot());

            boolean added = false;
            try {
                added = server.addFreshEntity(golem);
            } catch (RuntimeException failure) {
                Thaumcraft.LOG.warn("Straw golem admission threw at {}", adjacent, failure);
            }
            // A real native owner wins even if a later ADD hook throws, removes or
            // changes the entity. Never retry/refund/roll back that world action.
            if (!added && !nativeOwned(golem)) {
                discardDetached(golem);
                return InteractionResult.FAIL;
            }
            if (!player.getAbilities().instabuild) {
                if (player.getItemInHand(context.getHand()) == held && ItemStack.matches(held, before)) {
                    held.shrink(1);
                } else {
                    // A callback may already have paid/replaced/exhausted this hand.
                    // Do not charge another stack, restore old stock, or spawn again.
                    Thaumcraft.LOG.warn("Admitted straw golem kept; placement hand changed during admission at {}", adjacent);
                }
            }
            try {
                player.getInventory().setChanged();
                player.containerMenu.broadcastChanges();
                if (!golem.isRemoved()) golem.playAmbientSound();
            } catch (RuntimeException failure) {
                Thaumcraft.LOG.warn("Straw golem post-placement notification failed at {}", adjacent, failure);
            }
            return InteractionResult.CONSUME;
        } catch (RuntimeException failure) {
            // Preparation failed, not an invitation to reset malformed data or
            // mutate somebody else's stock/entity. Native admission is never undone.
            if (golem != null && nativeOwned(golem)) throw failure;
            if (golem != null) discardDetached(golem);
            Thaumcraft.LOG.warn("Straw golem placement failed; retaining the placer", failure);
            return InteractionResult.FAIL;
        } finally {
            placing.remove();
        }
    }

    private static boolean mayPlace(UseOnContext context, ServerLevel level, Player player, ItemStack held,
                                    BlockPos clicked, BlockPos adjacent, Direction face) {
        return player.isAlive() && !player.isSpectator() && !player.isShiftKeyDown() && player.level() == level
                && player.getItemInHand(context.getHand()) == held && !held.isEmpty()
                && level.hasChunkAt(clicked) && level.hasChunkAt(adjacent)
                && level.isInWorldBounds(clicked) && level.isInWorldBounds(adjacent)
                && level.getWorldBorder().isWithinBounds(clicked) && level.getWorldBorder().isWithinBounds(adjacent)
                && player.canInteractWithBlock(clicked, 0) && level.mayInteract(player, clicked)
                && level.mayInteract(player, adjacent) && player.mayUseItemAt(clicked, face, held)
                && player.mayUseItemAt(adjacent, face, held);
    }

    private static boolean safeBody(ServerLevel level, Entity golem) {
        AABB body = golem.getBoundingBox();
        if (body.minY < level.getMinBuildHeight() || body.maxY > level.getMaxBuildHeight()
                || !level.getWorldBorder().isWithinBounds(BlockPos.containing(body.minX, body.minY, body.minZ))
                || !level.getWorldBorder().isWithinBounds(BlockPos.containing(body.maxX, body.maxY, body.maxZ))) return false;
        // Collision shapes may extend from neighboring cells. Never load a chunk
        // merely to place a worker or to look for its supporting floor.
        AABB reads = body.inflate(1);
        for (int x = Mth.floor(reads.minX) >> 4; x <= Mth.floor(reads.maxX) >> 4; x++) {
            for (int z = Mth.floor(reads.minZ) >> 4; z <= Mth.floor(reads.maxZ) >> 4; z++) {
                if (!level.hasChunkAt(new BlockPos(x << 4, Mth.floor(body.minY), z << 4))) return false;
            }
        }
        if (!level.noCollision(golem, body) || level.containsAnyLiquid(body)
                || !level.getEntities(golem, body, EntitySelector.NO_SPECTATORS).isEmpty()) return false;
        // Allow the original small fall onto a chest/slab top, but not unsupported
        // air placement. Use real collision geometry, not a full-cube approximation.
        AABB floor = new AABB(body.minX, body.minY - 1, body.minZ, body.maxX, body.minY, body.maxZ);
        return level.getBlockCollisions(golem, floor).iterator().hasNext();
    }

    private static boolean nativeOwned(Entity entity) {
        if (((EntityLevelCallbackAccess) entity).thaumcraft$getLevelCallback() != EntityInLevelCallback.NULL) return true;
        return entity.level() instanceof ServerLevel level
                && (level.getEntity(entity.getUUID()) == entity || level.getEntity(entity.getId()) == entity);
    }

    private static void discardDetached(Entity entity) {
        if (!nativeOwned(entity) && !entity.isRemoved()) entity.discard();
    }

    /** Absence alone means fresh/coreless. Unsupported payloads never become EMPTY. */
    private GolemData configuration(ItemStack stack) {
        if (stack.has(DataComponents.ENTITY_DATA)
                || stack.has(DataComponents.BLOCK_ENTITY_DATA) || stack.has(DataComponents.BLOCK_STATE)) return null;
        GolemData data = stack.get(ThaumcraftGolems.GOLEM_DATA.get());
        if (data == null) {
            if (stack.has(ThaumcraftGolems.GOLEM_DATA.get())) return null;
            data = GolemData.EMPTY;
        }
        if (data.core() == GolemData.USE && !data.useOptions().supportsDefaultBaseline()) return null;
        // Never the original's silent truncation: more upgrades than this material has slots is invalid.
        int[] fitted = GolemUpgrades.fit(data.upgrades(), GolemUpgrades.slotCount(EnumGolemType.getType(material).upgrades, data.advanced()));
        if (fitted == null) return null;
        // Capacity is material + the placer's own Earth upgrades (original getCarryLimit/getFluidCarryLimit).
        int earth = GolemUpgrades.count(fitted, GolemUpgrade.EARTH);
        try {
            if (data.core() == GolemData.ALCHEMY) {
                GolemAspectCargo stock = GolemAspectCargo.readPlacer(stack);
                stock.validate(material, earth);
            } else {
                GolemWaterCargo water = GolemWaterCargo.readPlacer(stack);
                water.validate(material, earth);
                if (water.amountMb() > 0 && data.core() != GolemData.LIQUID) return null;
            }
            return data;
        } catch (IllegalArgumentException invalid) { return null; }
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flags) {
        super.appendHoverText(stack, context, tooltip, flags);
        GolemData data = configuration(stack);
        if (data == null) {
            tooltip.add(Component.translatable("tooltip.thaumcraft.golem.invalid_configuration").withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.translatable(switch (data.core()) {
                case GolemData.FILL -> "tooltip.thaumcraft.golem.core_fill";
                case GolemData.EMPTY_CORE -> "tooltip.thaumcraft.golem.core_empty";
                case GolemData.GATHER -> "tooltip.thaumcraft.golem.core_gather";
                case GolemData.HARVEST -> "tooltip.thaumcraft.golem.core_harvest";
                case GolemData.LUMBER -> "tooltip.thaumcraft.golem.core_lumber";
                case GolemData.GUARD -> "tooltip.thaumcraft.golem.core_guard";
                case GolemData.LIQUID -> "tooltip.thaumcraft.golem.core_liquid";
                case GolemData.ALCHEMY -> "tooltip.thaumcraft.golem.core_alchemy";
                case GolemData.USE -> "tooltip.thaumcraft.golem.core_use";
                case GolemData.BUTCHER -> "tooltip.thaumcraft.golem.core_butcher";
                case GolemData.SORTING -> "tooltip.thaumcraft.golem.core_sorting";
                case GolemData.FISHING -> "tooltip.thaumcraft.golem.core_fishing";
                default -> "tooltip.thaumcraft.golem.coreless";
            }));
            // Original ItemGolemPlacer:113: the "advanced" line sits between the core and the upgrades.
            if (data.advanced()) tooltip.add(Component.translatable("tc.adv"));
            if (!data.upgrades().isEmpty()) {
                // Original ItemGolemPlacer:114-121: installed upgrades listed in blue, empty slots skipped.
                MutableComponent line = Component.empty().withStyle(ChatFormatting.BLUE);
                for (int id : data.upgrades()) {
                    GolemUpgrade upgrade = GolemUpgrade.byId(id);
                    if (upgrade != null) line.append(Component.translatable("item.thaumcraft.golem_upgrade_" + upgrade.key)).append(" ");
                }
                tooltip.add(line);
            }
            if (data.core() == GolemData.GUARD && !GolemGuardTargets.isDefault(data.guardTargets())) {
                // The owner's Order switches (hostile monsters only is the unmarked default).
                MutableComponent kinds = Component.empty();
                String[] names = {"monsters", "animals", "players", "creepers"};
                boolean first = true;
                for (int index = 0; index < names.length; index++) {
                    if (!GolemGuardTargets.allows(data.guardTargets(), 1 << index)) continue;
                    if (!first) kinds.append(", ");
                    kinds.append(Component.translatable("tc.port.golem.guard." + names[index]));
                    first = false;
                }
                if (first) kinds.append("-");
                tooltip.add(Component.translatable("tooltip.thaumcraft.golem.guard_targets", kinds).withStyle(ChatFormatting.GRAY));
            }
            if (data.core() == GolemData.ALCHEMY) {
                GolemAspectCargo stock = GolemAspectCargo.readPlacer(stack);
                if (stock.amount() > 0) tooltip.add(Component.translatable("tooltip.thaumcraft.golem.aspect_cargo",
                        stock.aspect().getName(), stock.amount()).withStyle(ChatFormatting.GRAY));
            } else {
                int water = GolemWaterCargo.readPlacer(stack).amountMb();
                if (water > 0) tooltip.add(Component.translatable("tooltip.thaumcraft.golem.water_cargo", water).withStyle(ChatFormatting.GRAY));
            }
        }
    }
}
