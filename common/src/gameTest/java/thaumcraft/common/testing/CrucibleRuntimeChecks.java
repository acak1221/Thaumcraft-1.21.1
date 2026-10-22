package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftAlchemy;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.crafting.CrucibleCrafting;
import thaumcraft.common.entities.EntitySpecialItem;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.ObjectAspectResolver;
import thaumcraft.common.tiles.TileCrucible;

/** One physical alchemy flow, bound/run by the central loader test integration. No injected recipes or aspect pools. */
public final class CrucibleRuntimeChecks {
    private CrucibleRuntimeChecks() {}

    public static void run(GameTestHelper test, ServerPlayer player) {
        ServerLevel level = test.getLevel();
        BlockPos pos = test.absolutePos(new BlockPos(3, 2, 3));
        Map<BlockPos, BlockState> states = new LinkedHashMap<>();
        Map<BlockPos, CompoundTag> blockEntities = new LinkedHashMap<>();
        for (BlockPos mutable : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            BlockPos saved = mutable.immutable();
            states.put(saved, level.getBlockState(saved));
            if (level.getBlockEntity(saved) != null) blockEntities.put(saved, level.getBlockEntity(saved).saveWithFullMetadata(level.registryAccess()));
        }
        var inventory = new ArrayList<ItemStack>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); ++slot) inventory.add(player.getInventory().getItem(slot).copy());
        var knowledge = KnowledgeAccess.get(player).copy();
        var oldPosition = player.position();
        var gameType = player.gameMode.getGameModeForPlayer();
        int selected = player.getInventory().selected;
        boolean sneaking = player.isShiftKeyDown();
        AABB box = new AABB(pos).inflate(3);
        Set<UUID> oldDrops = level.getEntitiesOfClass(ItemEntity.class, box).stream().map(ItemEntity::getUUID).collect(Collectors.toSet());
        try {
            player.closeContainer();
            player.getInventory().clearContent();
            player.getInventory().selected = 0;
            player.setGameMode(GameType.SURVIVAL);
            player.setShiftKeyDown(false);
            player.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
            KnowledgeAccess.get(player).reset();
            level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(pos.below(), Blocks.LAVA.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(pos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3), Block.UPDATE_ALL);
            ItemStack wand = new ItemStack(ThaumcraftContent.WAND.get());
            wand.set(DataComponents.CUSTOM_NAME, Component.literal("crucible-keeps-the-actual-wand"));
            player.setItemInHand(InteractionHand.MAIN_HAND, wand.copy());
            test.assertTrue(use(player, pos).consumesAction() && level.getBlockEntity(pos) instanceof TileCrucible,
                    "native wand use converts a water cauldron without CRUCIBLE research or any vis");
            TileCrucible tile = (TileCrucible) level.getBlockEntity(pos);
            test.assertTrue(ItemStack.matches(player.getMainHandItem(), wand) && tile.getWaterAmount() == 0,
                    "original conversion keeps the named wand and discards the old cauldron water rather than granting a free fill");
            test.assertTrue(!level.getBlockState(pos).getCollisionShape(level, pos).bounds().equals(new AABB(0, 0, 0, 1, 1, 1)),
                    "crucible collision is the original low cauldron walls, not a solid cube");

            ItemEntity coal = drop(level, pos, Items.COAL, 2, null);
            collide(level, pos, coal);
            test.assertTrue(coal.getItem().getCount() == 2 && tile.tagAmount() == 0, "cold/dry real item collision refuses smelting");
            player.setGameMode(GameType.CREATIVE);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            test.assertTrue(use(player, pos).consumesAction() && tile.getWaterAmount() == 1000
                    && player.getInventory().countItem(Items.WATER_BUCKET) == 0 && player.getInventory().countItem(Items.BUCKET) == 1,
                    "native filling consumes even a creative water bucket and returns exactly one empty container");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            var fullBucket = player.getMainHandItem().copy();
            use(player, pos);
            test.assertTrue(ItemStack.matches(fullBucket, player.getMainHandItem()) && tile.getWaterAmount() == 1000,
                    "a full tank consumes no container");
            player.setGameMode(GameType.SURVIVAL);
            for (int tick = 0; tick < 151; ++tick) TileCrucible.tick(level, pos, level.getBlockState(pos), tile);
            test.assertTrue(tile.getHeat() == 151 && tile.getWaterAmount() == 1000, "actual lava heating reaches the original heat>150 threshold without a water debit");

            collide(level, pos, coal);
            test.assertTrue(coal.getItem().getCount() == 1 && tile.getAspects().getAmount(Aspect.ENERGY) == 2,
                    "JAR-confirmed shrinking loop consumes only half of a two-coal physical stack");
            collide(level, pos, coal);
            ItemEntity torches = drop(level, pos, Items.TORCH, 3, null);
            collide(level, pos, torches);
            test.assertTrue(torches.getItem().getCount() == 1 && tile.getAspects().getAmount(Aspect.LIGHT) == 2,
                    "three torches dissolve two first, with no actor/research/discovery prerequisite");
            collide(level, pos, torches);
            test.assertTrue(tile.getAspects().getAmount(Aspect.ENERGY) == 4 && tile.getAspects().getAmount(Aspect.FIRE) == 4
                    && tile.getAspects().getAmount(Aspect.LIGHT) == 3 && tile.getWaterAmount() == 1000,
                    "original coal potentia2/ignis2 and torch lux1 accumulate through real collisions without dissolving water cost");
            ItemEntity unknown = drop(level, pos, Items.BARRIER, 2, player);
            var beforeUnknown = tile.saveWithoutMetadata(level.registryAccess());
            collide(level, pos, unknown);
            test.assertTrue(!unknown.isRemoved() && unknown.getItem().getCount() == 2 && unknown.getDeltaMovement().y > .34
                    && beforeUnknown.equals(tile.saveWithoutMetadata(level.registryAccess())), "an untagged physical item bounces without deletion or invented aspects");

            int pending = TileCrucible.pendingOutputCount(level);
            var knowledgeBeforeDissolving = KnowledgeAccess.get(player).save();
            AspectList beforeRefusal = tile.getAspects();
            AspectList dustTags = ObjectAspectResolver.get(new ItemStack(Items.GLOWSTONE_DUST), level);
            ItemEntity refused = drop(level, pos, Items.GLOWSTONE_DUST, 1, player);
            test.assertTrue(CrucibleCrafting.find(level, refused, tile.getAspects()) == null, "native thrower knowledge, not a client-supplied name, gates NITOR");
            collide(level, pos, refused);
            test.assertTrue(refused.isRemoved() && same(tile.getAspects(), beforeRefusal.add(dustTags))
                    && TileCrucible.pendingOutputCount(level) == pending && tile.getWaterAmount() == 1000,
                    "a catalyst without its research dissolves ordinarily rather than yielding an unpaid result");
            test.assertTrue(knowledgeBeforeDissolving.equals(KnowledgeAccess.get(player).save()), "ordinary crucible dissolution grants no fake bonus research points or aspect discoveries");

            KnowledgeAccess.get(player).addResearch("NITOR");
            AspectList beforeCraft = tile.getAspects();
            ItemEntity catalyst = drop(level, pos, Items.GLOWSTONE_DUST, 1, player);
            for (Aspect aspect : new Aspect[] {Aspect.ENERGY, Aspect.FIRE, Aspect.LIGHT}) {
                AspectList shortPool = tile.getAspects();
                shortPool.remove(aspect, shortPool.getAmount(aspect) - 2);
                var snapshot = shortPool.copy();
                test.assertTrue(CrucibleCrafting.find(level, catalyst, shortPool) == null && same(shortPool, snapshot)
                        && catalyst.getItem().getCount() == 1 && !catalyst.isRemoved(),
                        "Nitor below threshold refuses selection without consuming catalyst or aspect pool: " + aspect.getTag());
            }
            collide(level, pos, catalyst);
            test.assertTrue(catalyst.isRemoved() && tile.getWaterAmount() == 950
                    && same(tile.getAspects(), beforeCraft.remove(Aspect.ENERGY, 3).remove(Aspect.FIRE, 3).remove(Aspect.LIGHT, 3))
                    && TileCrucible.pendingOutputCount(level) == pending + 1,
                    "actual registered Nitor JSON consumes one catalyst, potentia3/ignis3/lux3 and 50 water exactly once");
            TileCrucible.tickOutputs(level);
            var outputs = level.getEntitiesOfClass(EntitySpecialItem.class, box).stream()
                    .filter(item -> !oldDrops.contains(item.getUUID()) && item.getItem().is(ThaumcraftContent.MATERIALS.get("nitor").get())).toList();
            test.assertTrue(outputs.size() == 1 && outputs.getFirst().getItem().getCount() == 1
                    && TileCrucible.pendingOutputCount(level) == pending, "paid recipe materializes exactly one real floating/fire-resistant Nitor item");
            var beforeSpecial = tile.saveWithoutMetadata(level.registryAccess());
            collide(level, pos, outputs.getFirst());
            test.assertTrue(!outputs.getFirst().isRemoved() && beforeSpecial.equals(tile.saveWithoutMetadata(level.registryAccess())), "special outputs cannot be re-dissolved by the crucible collision");

            var saved = tile.saveWithFullMetadata(level.registryAccess());
            var savedAspects = tile.getAspects();
            level.removeBlockEntity(pos);
            var loaded = new TileCrucible(pos, level.getBlockState(pos));
            loaded.loadWithComponents(saved, level.registryAccess());
            level.setBlockEntity(loaded);
            test.assertTrue(loaded.getHeat() == 151 && loaded.getWaterAmount() == 950 && same(loaded.getAspects(), savedAspects),
                    "native BE replacement/reload preserves exact short heat, water and ordered aspect amounts");
            player.setItemInHand(InteractionHand.MAIN_HAND, PotionContents.createItemStack(Items.POTION, Potions.WATER));
            use(player, pos);
            test.assertTrue(loaded.getWaterAmount() == 1000 && player.getInventory().countItem(Items.GLASS_BOTTLE) == 1,
                    "a water bottle supplies a quarter bucket, consumes once, and wastes excess beyond tank capacity");
            player.setItemInHand(InteractionHand.MAIN_HAND, wand.copy());
            player.setShiftKeyDown(true);
            test.assertTrue(use(player, pos).consumesAction() && loaded.getWaterAmount() == 0 && loaded.tagAmount() == 0
                    && ItemStack.matches(player.getMainHandItem(), wand), "actual sneak-wand use spills remnants, empties water/aspects and keeps the wand");
        } finally {
            // Empty BEFORE restoring the fixture so cleanup does not enqueue a
            // later onRemove spill that would mutate another runtime check.
            if (level.getBlockEntity(pos) instanceof TileCrucible tile) tile.spillRemnants();
            TileCrucible.tickOutputs(level);
            for (var entry : states.entrySet()) level.setBlock(entry.getKey(), entry.getValue(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            for (var entry : blockEntities.entrySet()) if (level.getBlockEntity(entry.getKey()) != null) level.getBlockEntity(entry.getKey()).loadWithComponents(entry.getValue(), level.registryAccess());
            for (var entity : level.getEntitiesOfClass(ItemEntity.class, box)) if (!oldDrops.contains(entity.getUUID())) entity.discard();
            player.getInventory().clearContent();
            for (int slot = 0; slot < inventory.size(); ++slot) player.getInventory().setItem(slot, inventory.get(slot));
            player.getInventory().selected = selected;
            player.setGameMode(gameType);
            player.setShiftKeyDown(sneaking);
            player.setPos(oldPosition);
            KnowledgeAccess.get(player).copyFrom(knowledge);
        }
        test.succeed();
    }

    private static boolean same(AspectList left, AspectList right) { return left.aspects.equals(right.aspects); }
    private static ItemEntity drop(ServerLevel level, BlockPos pos, Item item, int count, ServerPlayer thrower) {
        ItemEntity entity = new ItemEntity(level, pos.getX() + .5, pos.getY() + .4, pos.getZ() + .5, new ItemStack(item, count));
        entity.setDeltaMovement(Vec3.ZERO);
        if (thrower != null) entity.setThrower(thrower);
        if (!level.addFreshEntity(entity)) throw new IllegalStateException("Crucible fixture source spawn was rejected");
        return entity;
    }
    private static void collide(ServerLevel level, BlockPos pos, ItemEntity entity) { level.getBlockState(pos).entityInside(level, pos, entity); }
    private static net.minecraft.world.InteractionResult use(ServerPlayer player, BlockPos pos) {
        var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, .5, 0), Direction.UP, pos, false);
        return player.gameMode.useItemOn(player, player.serverLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
    }
}
