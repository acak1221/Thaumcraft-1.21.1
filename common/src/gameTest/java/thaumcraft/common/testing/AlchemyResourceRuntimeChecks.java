package thaumcraft.common.testing;

import dev.architectury.registry.fuel.FuelRegistry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftAlchemy;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftResources;
import thaumcraft.common.entities.projectile.EntityAlumentum;
import thaumcraft.common.tiles.TileCrucible;

/** One loader-bound physical flow, including native lighting and real projectile/furnace ticks. */
public final class AlchemyResourceRuntimeChecks {
    private AlchemyResourceRuntimeChecks() {}

    public static void run(GameTestHelper test, ServerPlayer player) {
        ServerLevel level = test.getLevel();
        BlockPos pos = test.absolutePos(new BlockPos(3, 3, 3));
        AABB box = new AABB(pos).inflate(5);
        Map<BlockPos, BlockState> states = new LinkedHashMap<>();
        Map<BlockPos, CompoundTag> tiles = new LinkedHashMap<>();
        for (BlockPos mutable : BlockPos.betweenClosed(pos.offset(-4, -3, -4), pos.offset(4, 4, 4))) {
            BlockPos saved = mutable.immutable();
            states.put(saved, level.getBlockState(saved));
            if (level.getBlockEntity(saved) != null) tiles.put(saved, level.getBlockEntity(saved).saveWithFullMetadata(level.registryAccess()));
        }
        var inventory = new ArrayList<ItemStack>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); ++slot) inventory.add(player.getInventory().getItem(slot).copy());
        var mode = player.gameMode.getGameModeForPlayer();
        var position = player.position();
        var motion = player.getDeltaMovement();
        float yaw = player.getYRot(), pitch = player.getXRot(), exhaustion = player.getFoodData().getExhaustionLevel();
        int selected = player.getInventory().selected;
        boolean sneaking = player.isShiftKeyDown(), grief = level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        Set<UUID> oldDrops = level.getEntitiesOfClass(ItemEntity.class, box).stream().map(ItemEntity::getUUID).collect(Collectors.toSet());
        List<EntityAlumentum> shots = new ArrayList<>();
        List<Cow> victims = new ArrayList<>();
        Runnable restore = () -> {
            for (var shot : shots) shot.discard();
            for (var victim : victims) victim.discard();
            if (level.getBlockEntity(pos.above()) instanceof FurnaceBlockEntity furnace) furnace.clearContent();
            for (var entry : states.entrySet()) level.setBlock(entry.getKey(), entry.getValue(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            for (var entry : tiles.entrySet()) if (level.getBlockEntity(entry.getKey()) != null) level.getBlockEntity(entry.getKey()).loadWithComponents(entry.getValue(), level.registryAccess());
            for (var drop : level.getEntitiesOfClass(ItemEntity.class, box)) if (!oldDrops.contains(drop.getUUID())) drop.discard();
            level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(grief, level.getServer());
            player.getInventory().clearContent();
            for (int slot = 0; slot < inventory.size(); ++slot) player.getInventory().setItem(slot, inventory.get(slot));
            player.getInventory().selected = selected;
            player.setGameMode(mode);
            player.setShiftKeyDown(sneaking);
            player.setPos(position);
            player.setDeltaMovement(motion);
            player.setYRot(yaw); player.setXRot(pitch);
            player.getFoodData().setExhaustion(exhaustion);
        };
        try {
            player.closeContainer();
            player.getInventory().clearContent();
            player.getInventory().selected = 0;
            player.setGameMode(GameType.SURVIVAL);
            player.setShiftKeyDown(false);
            player.setPos(Vec3.atCenterOf(pos).add(0, 2, 1));
            level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftContent.MATERIALS.get("nitor").get(), 2));
            test.assertTrue(use(player, pos.below(), Direction.UP).consumesAction()
                    && level.getBlockState(pos).is(ThaumcraftResources.NITOR.get()) && player.getMainHandItem().getCount() == 1,
                    "native survival item use places exactly one registered Nitor and pays one item");
            var nitor = level.getBlockState(pos);
            var bounds = nitor.getShape(level, pos).bounds();
            test.assertTrue(nitor.getLightEmission() == 15 && nitor.getDestroySpeed(level, pos) == 0
                    && nitor.getCollisionShape(level, pos).isEmpty() && Math.abs(bounds.minX - .3) < 1e-6 && Math.abs(bounds.maxX - .7) < 1e-6,
                    "BlockAiry metadata1 keeps default hardness0, light15, 0.3..0.7 targeting and no collision");
            test.assertTrue(nitor.getBlock().getCloneItemStack(level, pos, nitor).is(ThaumcraftContent.MATERIALS.get("nitor").get()),
                    "pick-block resolves the existing flattened resource item, not AIR or a duplicate item");
            // Give the actual threaded light engine time; checking only a
            // constant lightLevel function would not prove luminous placement.
            test.runAfterDelay(6, () -> {
                try {
                    test.assertTrue(level.getBrightness(LightLayer.BLOCK, pos) == 15, "native placed Nitor actually emits block light15");
                    rest(test, player, pos, box, oldDrops, shots, victims);
                } finally { restore.run(); }
                test.succeed();
            });
        } catch (RuntimeException | Error failure) {
            restore.run();
            throw failure;
        }
    }

    private static void rest(GameTestHelper test, ServerPlayer player, BlockPos pos, AABB box, Set<UUID> oldDrops,
                             List<EntityAlumentum> shots, List<Cow> victims) {
        ServerLevel level = test.getLevel();
        var nitorItem = ThaumcraftContent.MATERIALS.get("nitor").get();
        var alumentum = ThaumcraftContent.MATERIALS.get("alumentum").get();
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        test.assertTrue(player.gameMode.destroyBlock(pos) && level.getBlockState(pos).isAir(), "native survival breaking removes the luminous block");
        var drops = level.getEntitiesOfClass(ItemEntity.class, box).stream().filter(drop -> !oldDrops.contains(drop.getUUID())).toList();
        test.assertTrue(drops.size() == 1 && drops.getFirst().getItem().is(nitorItem) && drops.getFirst().getItem().getCount() == 1,
                "actual harvest/loot produces exactly one MATERIALS Nitor, with no duplicate manual drop");
        drops.forEach(ItemEntity::discard);

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(nitorItem, 2));
        level.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        test.assertTrue(!use(player, pos.below(), Direction.UP).consumesAction() && player.getMainHandItem().getCount() == 2
                && level.getBlockState(pos).is(Blocks.STONE), "occupied-target placement neither skips another block nor consumes a resource");
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        player.setGameMode(GameType.ADVENTURE);
        test.assertTrue(!use(player, pos.below(), Direction.UP).consumesAction() && player.getMainHandItem().getCount() == 2
                && level.getBlockState(pos).isAir(), "native adventure/mayUseItem permission refusal is atomic");
        player.setGameMode(GameType.CREATIVE);
        level.setBlock(pos, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1), Block.UPDATE_ALL);
        test.assertTrue(use(player, pos, Direction.EAST).consumesAction() && level.getBlockState(pos).is(ThaumcraftResources.NITOR.get())
                && level.getBlockState(pos.east()).isAir() && player.getMainHandItem().getCount() == 1,
                "horizontal thin-snow click replaces it in place, forces UP, and pays one even through the native creative wrapper");

        player.setGameMode(GameType.SURVIVAL);
        level.setBlock(pos.above(), ThaumcraftAlchemy.CRUCIBLE.get().defaultBlockState(), Block.UPDATE_ALL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        test.assertTrue(use(player, pos.above(), Direction.UP).consumesAction() && level.getBlockEntity(pos.above()) instanceof TileCrucible,
                "native bucket interaction fills the actual crucible above the placed Nitor");
        var crucible = (TileCrucible)level.getBlockEntity(pos.above());
        for (int tick = 0; tick < 151; ++tick) TileCrucible.tick(level, pos.above(), level.getBlockState(pos.above()), crucible);
        test.assertTrue(crucible.getHeat() == 151 && crucible.getWaterAmount() == 1000
                && level.getBlockState(pos).is(ThaumcraftResources.NITOR.get()), "exact thaumcraft:nitor, not vanilla fire, heats a real filled crucible beyond150");

        level.setBlock(pos.above(), Blocks.FURNACE.defaultBlockState(), Block.UPDATE_ALL);
        var furnace = (FurnaceBlockEntity)level.getBlockEntity(pos.above());
        furnace.setItem(0, new ItemStack(Items.IRON_ORE));
        furnace.setItem(1, new ItemStack(alumentum, 2));
        AbstractFurnaceBlockEntity.serverTick(level, pos.above(), level.getBlockState(pos.above()), furnace);
        test.assertTrue(FuelRegistry.get(new ItemStack(alumentum)) == 6400 && furnace.saveWithoutMetadata(level.registryAccess()).getInt("BurnTime") == 6400
                && furnace.getItem(1).getCount() == 1 && level.getBlockState(pos.above()).getValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT),
                "actual vanilla furnace consumes one Alumentum and lights for the oracle's exact6400 ticks on this loader");
        for (int tick = 1; tick < 200; ++tick) AbstractFurnaceBlockEntity.serverTick(level, pos.above(), level.getBlockState(pos.above()), furnace);
        test.assertTrue(furnace.getItem(2).is(Items.IRON_INGOT) && furnace.getItem(2).getCount() == 1 && furnace.getItem(1).getCount() == 1,
                "registered fuel really smelts iron, rather than merely returning a nominal burn-time constant");
        furnace.clearContent();
        for (BlockPos clear : BlockPos.betweenClosed(pos.offset(-3, -2, -3), pos.offset(3, 3, 3))) level.setBlock(clear, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);

        var dropped = new ItemEntity(level, pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5, new ItemStack(alumentum));
        test.assertTrue(level.addFreshEntity(dropped), "physical Alumentum item fixture spawns");
        dropped.tick();
        test.assertTrue(!dropped.isRemoved() && dropped.getItem().is(alumentum) && liveShots(level, box).isEmpty(),
                "a dropped/crafted resource remains an item; it never recursively creates a projectile");
        dropped.discard();
        var ammo = new ItemStack(alumentum, 2);
        ammo.set(DataComponents.CUSTOM_NAME, Component.literal("tracked-alumentum"));
        player.setItemInHand(InteractionHand.MAIN_HAND, ammo);
        for (boolean grief : new boolean[]{false, true}) {
            level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(grief, level.getServer());
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
            Cow victim = cow(test, level, pos, victims, false);
            var shot = shoot(test, player, pos, box, shots);
            if (!grief) {
                test.assertTrue(player.getMainHandItem().getCount() == 1 && shot.getOwner() == player && shot.getType() == ThaumcraftResources.ALUMENTUM.get()
                        && shot.getItem().is(alumentum) && shot.getItem().getCount() == 1 && shot.getItem().has(DataComponents.CUSTOM_NAME),
                        "real native use debits one, spawns the registered entity and tracks a singleton item plus owner");
                var saved = new CompoundTag();
                shot.saveWithoutId(saved);
                var loaded = ThaumcraftResources.ALUMENTUM.get().create(level);
                loaded.load(saved);
                test.assertTrue(loaded.getOwner() == player && ItemStack.matches(loaded.getItem(), shot.getItem())
                        && loaded.getDeltaMovement().equals(shot.getDeltaMovement()), "native projectile NBT reload preserves owner, item components and motion");
            }
            impact(test, shot);
            test.assertTrue(grief ? level.getBlockState(pos).isAir() : level.getBlockState(pos).is(Blocks.DIRT),
                    "actual projectile impact " + (grief ? "destroys terrain with mobGriefing=true" : "preserves terrain with mobGriefing=false"));
            test.assertTrue(victim.getHealth() < victim.getMaxHealth() && victim.getLastDamageSource() != null
                    && victim.getLastDamageSource().is(DamageTypeTags.IS_EXPLOSION) && victim.getLastDamageSource().getEntity() == null,
                    "actual unattributed explosion damages a nearby entity even when terrain damage is disabled");
            victim.discard();
        }
        test.assertTrue(player.getMainHandItem().isEmpty() && liveShots(level, box).isEmpty(), "two real survival shots exhaust two items and both impact entities are removed");
        player.gameMode.useItem(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND);
        test.assertTrue(liveShots(level, box).isEmpty(), "empty-hand reuse cannot create unpaid Alumentum");
        player.setGameMode(GameType.CREATIVE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(alumentum, 2));
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        Cow direct = cow(test, level, pos, victims, true);
        impact(test, shoot(test, player, pos, box, shots));
        test.assertTrue(player.getMainHandItem().getCount() == 2 && direct.getHealth() < direct.getMaxHealth()
                && direct.getLastDamageSource() != null && direct.getLastDamageSource().is(DamageTypeTags.IS_EXPLOSION),
                "a real entity collision explodes without invented direct-hit damage and creative throwing keeps ammunition");
        for (BlockPos check : BlockPos.betweenClosed(pos.offset(-3, -2, -3), pos.offset(3, 3, 3))) {
            test.assertTrue(!level.getBlockState(check).is(Blocks.FIRE) && !level.getBlockState(check).is(Blocks.SOUL_FIRE), "Alumentum blasts create no vanilla fire");
        }
    }

    private static Cow cow(GameTestHelper test, ServerLevel level, BlockPos pos, List<Cow> victims, boolean direct) {
        Cow cow = EntityType.COW.create(level);
        cow.setNoAi(true);
        cow.setPos(pos.getX() + (direct ? .5 : -.5), pos.getY() + (direct ? 0 : -1), pos.getZ() + (direct ? .5 : 1.7));
        victims.add(cow);
        test.assertTrue(level.addFreshEntity(cow), "real living explosion target spawns");
        return cow;
    }
    private static EntityAlumentum shoot(GameTestHelper test, ServerPlayer player, BlockPos pos, AABB box, List<EntityAlumentum> shots) {
        player.setPos(pos.getX() - 1.5, pos.getY() + .6 - player.getEyeHeight(), pos.getZ() + .5);
        player.setYRot(-90); player.setXRot(0);
        var result = player.gameMode.useItem(player, player.serverLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
        var created = liveShots(player.serverLevel(), box).stream().filter(shot -> !shots.contains(shot)).toList();
        test.assertTrue(result.consumesAction() && created.size() == 1, "native item use spawns exactly one real Alumentum");
        var shot = created.getFirst();
        shots.add(shot);
        test.assertTrue(shot.getDeltaMovement().length() > .65 && shot.getDeltaMovement().length() < .85, "native throwable launches at TC4 speed0.75 with ordinary inaccuracy");
        // Keep the fixture player outside the blast, without substituting an
        // ownerless projectile or invoking protected impact hooks directly.
        player.setPos(pos.getX() + 4.5, pos.getY() + 4, pos.getZ() + 4.5);
        return shot;
    }
    private static void impact(GameTestHelper test, EntityAlumentum shot) {
        for (int tick = 0; tick < 8 && !shot.isRemoved(); ++tick) shot.tick();
        test.assertTrue(shot.isRemoved(), "native flight/collision produces an explosion and terminal removal, not a policy-only assertion");
    }
    private static List<EntityAlumentum> liveShots(ServerLevel level, AABB box) {
        return level.getEntitiesOfClass(EntityAlumentum.class, box, shot -> !shot.isRemoved());
    }
    private static net.minecraft.world.InteractionResult use(ServerPlayer player, BlockPos pos, Direction side) {
        var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(side.getNormal()).scale(.5)), side, pos, false);
        return player.gameMode.useItemOn(player, player.serverLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
    }
}
