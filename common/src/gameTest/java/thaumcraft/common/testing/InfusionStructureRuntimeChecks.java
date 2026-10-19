package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.crafting.IInfusionStabiliser;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.common.blocks.BlockInfusionPillar;
import thaumcraft.common.blocks.FluxFiniteBlock;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftFlux;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftMasonry;
import thaumcraft.common.config.ThaumcraftOres;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.infusion.InfusionInstability;
import thaumcraft.common.lib.infusion.InfusionStructure;
import thaumcraft.common.lib.potions.FluxEffects;
import thaumcraft.common.lib.potions.PotionFluxTaint;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.tiles.TileInfusionPillar;
import thaumcraft.common.tiles.TilePedestal;

/** One bounded native fixture, wired/run centrally. No synthetic recipes, registered test blocks, or zero-instability craft. */
public final class InfusionStructureRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private InfusionStructureRuntimeChecks() {}

    public static void run(GameTestHelper test, ServerPlayer player) {
        ServerLevel level = test.getLevel();
        // Keep the original 25x25 scan above the ordinary neighboring test fixtures.
        BlockPos matrix = test.absolutePos(new BlockPos(3, 26, 3));
        Map<BlockPos, BlockState> states = new LinkedHashMap<>();
        Map<BlockPos, CompoundTag> tiles = new LinkedHashMap<>();
        // Explicit fixture setup loads only its bounded chunks. Production scan
        // must NOT do this; the separate remote check below proves that boundary.
        for (int x = (matrix.getX() - 12) >> 4; x <= (matrix.getX() + 12) >> 4; ++x)
            for (int z = (matrix.getZ() - 12) >> 4; z <= (matrix.getZ() + 12) >> 4; ++z) level.getChunk(x, z);
        for (BlockPos mutable : BlockPos.betweenClosed(matrix.offset(-12, -11, -12), matrix.offset(12, 5, 12))) {
            BlockPos pos = mutable.immutable();
            states.put(pos, level.getBlockState(pos));
            if (level.getBlockEntity(pos) != null) tiles.put(pos, level.getBlockEntity(pos).saveWithFullMetadata(level.registryAccess()));
        }
        var knowledge = KnowledgeAccess.get(player).copy();
        var inventory = new ArrayList<ItemStack>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); ++slot) inventory.add(player.getInventory().getItem(slot).copy());
        var effects = player.getActiveEffects().stream().map(MobEffectInstance::new).toList();
        var position = player.position();
        var motion = player.getDeltaMovement();
        var mode = player.gameMode.getGameModeForPlayer();
        int selected = player.getInventory().selected;
        boolean sneaking = player.isShiftKeyDown();
        float health = player.getHealth();
        AABB box = new AABB(matrix).inflate(14);
        Set<UUID> existing = level.getEntitiesOfClass(Entity.class, box).stream().map(Entity::getUUID).collect(Collectors.toSet());
        long resumeSeed = level.random.nextLong();
        try {
            player.closeContainer();
            player.getInventory().clearContent();
            player.getInventory().selected = 0;
            player.setGameMode(GameType.SURVIVAL);
            player.setShiftKeyDown(false);
            player.removeAllEffects();
            for (var entry : states.entrySet()) if (!entry.getValue().isAir()) put(level, entry.getKey(), Blocks.AIR.defaultBlockState());
            formation(test, level, matrix, player);
            pedestal(test, level, matrix, player);
            surroundings(test, level, matrix, player);
            hazards(test, level, matrix, player, box);
        } finally {
            // Do not manufacture teardown drops from the fixture's inventory.
            for (BlockPos pos : states.keySet()) if (level.getBlockEntity(pos) instanceof TilePedestal pedestal) pedestal.clearContent();
            for (var entry : states.entrySet()) put(level, entry.getKey(), entry.getValue());
            for (var entry : tiles.entrySet()) if (level.getBlockEntity(entry.getKey()) != null)
                level.getBlockEntity(entry.getKey()).loadWithComponents(entry.getValue(), level.registryAccess());
            DeferredItemDrops.tick(level);
            for (Entity entity : level.getEntitiesOfClass(Entity.class, box)) if (entity != player && !existing.contains(entity.getUUID())) entity.discard();
            player.closeContainer();
            player.getInventory().clearContent();
            for (int slot = 0; slot < inventory.size(); ++slot) player.getInventory().setItem(slot, inventory.get(slot));
            player.getInventory().selected = selected;
            player.setGameMode(mode);
            player.setShiftKeyDown(sneaking);
            player.setPos(position);
            player.setDeltaMovement(motion);
            player.setHealth(health);
            player.removeAllEffects();
            effects.forEach(player::addEffect);
            KnowledgeAccess.get(player).copyFrom(knowledge);
            level.random.setSeed(resumeSeed);
        }
        test.succeed();
    }

    private static void formation(GameTestHelper test, ServerLevel level, BlockPos pos, ServerPlayer player) {
        put(level, pos, ThaumcraftInfusion.MATRIX.get().defaultBlockState());
        put(level, pos.below(2), ThaumcraftInfusion.PEDESTAL.get().defaultBlockState());
        for (int x : new int[] {-1, 1}) for (int z : new int[] {-1, 1}) {
            put(level, pos.offset(x, -2, z), ThaumcraftMasonry.ARCANE_STONE_BRICKS.get().defaultBlockState());
            put(level, pos.offset(x, -1, z), ThaumcraftMasonry.ARCANE_STONE.get().defaultBlockState());
        }
        var matrix = (TileInfusionMatrix) level.getBlockEntity(pos);
        ItemStack wand = new ItemStack(ThaumcraftContent.WAND.get());
        wand.set(ThaumcraftContent.WAND_DATA.get(), new WandData("greatwood", "iron", Map.of()));
        for (String primal : WandData.PRIMALS) ItemWandCasting.addRealVis(wand, primal, 5000, true);
        wand.set(DataComponents.CUSTOM_NAME, Component.literal("formation-components"));
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        near(player, pos);
        KnowledgeAccess.get(player).removeResearch("INFUSION");
        use(player, pos);
        test.assertTrue(!matrix.isActive() && ItemWandCasting.getVis(wand, "aer") == 5000, "unresearched native wand cannot form or charge the blueprint");
        KnowledgeAccess.get(player).addResearch("INFUSION");
        put(level, pos.below(), Blocks.GLASS.defaultBlockState());
        use(player, pos);
        test.assertTrue(!matrix.isActive() && ItemWandCasting.getVis(wand, "aer") == 5000, "all otherwise-empty blueprint cells must really be air");
        put(level, pos.below(), Blocks.AIR.defaultBlockState());
        wand.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(wand).withAmount("perditio", 2749));
        use(player, pos);
        test.assertTrue(!matrix.isActive() && ItemWandCasting.getVis(wand, "aer") == 5000
                && level.getBlockState(pos.offset(-1, -2, -1)).is(ThaumcraftMasonry.ARCANE_STONE_BRICKS.get()), "one short primal refuses without a partial structure/debit");
        wand.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(wand).withAmount("perditio", 5000));
        player.setPos(pos.getX() + 20, pos.getY(), pos.getZ());
        InfusionStructure.tryWandInteraction(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, hit(pos)));
        test.assertTrue(!matrix.isActive() && ItemWandCasting.getVis(wand, "aer") == 5000, "direct early-use hook also enforces actual native reach");
        near(player, pos);
        test.assertTrue(!InfusionStructure.isValid(level, pos) && use(player, pos).consumesAction()
                && matrix.isActive() && InfusionStructure.isValid(level, pos), "real registered wand use forms all four native pillar pairs and activates once");
        for (String primal : WandData.PRIMALS) test.assertTrue(ItemWandCasting.getVis(wand, primal) == 2250, "25 whole vis uses iron1.1 pricing, not fixed2500: " + primal);
        test.assertTrue(player.getMainHandItem() == wand && wand.getHoverName().getString().equals("formation-components"), "formation keeps the actual native hand object/components");
        int orientation = 2;
        for (int x : new int[] {-1, 1}) for (int z : new int[] {-1, 1}) {
            BlockPos lower = pos.offset(x, -2, z);
            var pillar = (TileInfusionPillar) level.getBlockEntity(lower);
            test.assertTrue(pillar.getOrientation() == orientation++ && pillar.getHalf() == DoubleBlockHalf.LOWER
                    && level.getBlockState(lower.above()).getValue(BlockInfusionPillar.HALF) == DoubleBlockHalf.UPPER,
                    "original corner orientation2/3/4/5 and lower-only BE survive flattening");
            test.assertTrue(Block.getDrops(level.getBlockState(lower), level, lower, pillar).getFirst().is(ThaumcraftMasonry.ARCANE_STONE_BRICKS.get().asItem())
                    && Block.getDrops(level.getBlockState(lower.above()), level, lower.above(), null).getFirst().is(ThaumcraftMasonry.ARCANE_STONE.get().asItem()), "each lower/top independently drops one brick/stone");
        }
        ItemStack paid = wand.copy();
        matrix.setActive(false);
        KnowledgeAccess.get(player).removeResearch("INFUSION");
        use(player, pos);
        use(player, pos); // Empty center: free attempted recipe-start, not another formation.
        test.assertTrue(matrix.isActive() && !matrix.isCrafting() && ItemStack.matches(wand, paid), "already-formed activation/start is free, even without the formation research");
        KnowledgeAccess.get(player).addResearch("INFUSION");
        BlockPos broken = pos.offset(-1, -1, -1);
        BlockState upper = level.getBlockState(broken), lower = level.getBlockState(broken.below());
        level.destroyBlock(broken, true);
        test.assertTrue(level.getBlockState(broken.below()).isAir() && !InfusionStructure.isValid(level, pos), "ordinary top breaking collapses the lower partner through native neighbors");
        for (int tick = 0; tick < 100; ++tick) TileInfusionMatrix.tick(level, pos, level.getBlockState(pos), matrix);
        test.assertTrue(!matrix.isActive(), "native idle validity cadence deactivates the broken altar");
        put(level, broken, upper); put(level, broken.below(), lower);
        use(player, pos);
        test.assertTrue(matrix.isActive() && ItemStack.matches(wand, paid), "repairing the real pillar pair permits free activation without a new charge");
        test.assertTrue(ArcaneVis.price(Map.of("aer", 25), goldWand(), player).get("aer") == 2500, "same shared pricing helper retains gold's original cap discount");
    }

    private static void pedestal(GameTestHelper test, ServerLevel level, BlockPos matrix, ServerPlayer player) {
        BlockPos pos = matrix.offset(3, -2, 0);
        put(level, pos, ThaumcraftInfusion.PEDESTAL.get().defaultBlockState());
        var pedestal = (TilePedestal) level.getBlockEntity(pos);
        ItemStack hand = named(3, "native-pedestal-insertion");
        player.setItemInHand(InteractionHand.MAIN_HAND, hand); near(player, pos);
        use(player, pos);
        test.assertTrue(pedestal.getStack() == pedestal.getItem(0) && pedestal.getStack().getCount() == 1
                && hand.getCount() == 2 && ItemStack.isSameItemSameComponents(hand, pedestal.getStack()), "native empty RMB inserts exactly one with its components");
        use(player, pos);
        test.assertTrue(pedestal.isEmpty() && hand.getCount() == 2 && level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4)).stream()
                .anyMatch(item -> item.getItem().is(Items.DIAMOND) && item.getItem().getCount() == 1 && item.position().distanceTo(player.position()) < 2), "occupied native RMB ejects the whole item at the player without taking the held stack");
        var emptyUpdate = pedestal.getUpdatePacket().getTag();
        test.assertTrue(!emptyUpdate.isEmpty() && emptyUpdate.getInt("Count") == 0 && !emptyUpdate.contains("Stack"),
                "empty native BE updates carry an explicit zero; ClientPacketListener would ignore an empty compound and leave a ghost item");
        pedestal.setItem(0, named(6, "ordinary-clamp"));
        test.assertTrue(pedestal.getStack().getCount() == 1 && pedestal.getMaxStackSize() == 1, "ordinary setter and automation retain original slot limit1");
        for (Direction side : Direction.values()) test.assertTrue(pedestal.getSlotsForFace(side).length == 1
                && !pedestal.canPlaceItemThroughFace(0, hand, side) && pedestal.canTakeItemThroughFace(0, pedestal.getStack(), side), "all six sides expose occupied extraction, not insertion");
        pedestal.removeItem(0, 1);
        ItemStack output = named(120, "full-infusion-result");
        pedestal.setInfusionStack(output.copy());
        var saved = pedestal.saveWithFullMetadata(level.registryAccess());
        level.removeBlockEntity(pos);
        var loaded = new TilePedestal(pos, level.getBlockState(pos));
        loaded.loadWithComponents(saved, level.registryAccess()); level.setBlockEntity(loaded);
        test.assertTrue(ItemStack.matches(output, loaded.getStack()) && !pedestal.stillValid(player) && loaded.stillValid(player), "native BE save/load preserves fullcount120/components and invalidates the old identity");
        level.setBlock(pos.above(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        DeferredItemDrops.tick(level);
        test.assertTrue(loaded.isEmpty() && level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4)).stream()
                .anyMatch(item -> ItemStack.matches(output, item.getItem())), "top obstruction drops the full overstack through existing durable native delivery");
        put(level, pos.above(), Blocks.AIR.defaultBlockState());
        put(level, pos, Blocks.AIR.defaultBlockState());
    }

    private static void surroundings(GameTestHelper test, ServerLevel level, BlockPos matrix, ServerPlayer player) {
        BlockPos a = matrix.offset(-8, -1, -8), b = matrix.offset(8, -1, 8), c = matrix.offset(-3, -10, 2), opposite = matrix.offset(3, -10, -2);
        for (BlockPos pos : List.of(a, a.below(9), b, c, matrix.above(3), matrix.below(6), matrix.offset(9, -2, 0), matrix.offset(2, 0, 0)))
            put(level, pos, ThaumcraftInfusion.PEDESTAL.get().defaultBlockState());
        ((TilePedestal) level.getBlockEntity(a)).setItem(0, new ItemStack(Items.DIAMOND));
        ((TilePedestal) level.getBlockEntity(b)).setItem(0, new ItemStack(Items.DIRT));
        var scan = InfusionStructure.scan(level, matrix);
        test.assertTrue(scan.pedestals().equals(List.of(a, c, b)) && scan.symmetry() == 2,
                "dx/dz/topmost-depth ordering, central-column/radius exclusion, and unlike occupied opposites match original scan");
        boolean immutable = false;
        try { scan.pedestals().add(matrix); } catch (UnsupportedOperationException expected) { immutable = true; }
        test.assertTrue(immutable, "scan's ordered positions are immutable");
        put(level, opposite, ThaumcraftInfusion.PEDESTAL.get().defaultBlockState());
        ((TilePedestal) level.getBlockEntity(c)).setItem(0, new ItemStack(Items.STICK));
        test.assertTrue(InfusionStructure.scan(level, matrix).symmetry() == 1, "occupied versus empty opposite is +1, not an item-type comparison");
        ((TilePedestal) level.getBlockEntity(opposite)).setItem(0, new ItemStack(Items.GOLD_INGOT));
        test.assertTrue(InfusionStructure.scan(level, matrix).symmetry() == 0, "both occupied cancels despite completely different items");

        BlockPos candle = matrix.offset(10, 4, 0), support = candle.below();
        put(level, support, Blocks.STONE.defaultBlockState());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftInfusion.TALLOW_CANDLE.get())); near(player, support);
        use(player, support);
        test.assertTrue(level.getBlockState(candle).is(ThaumcraftInfusion.TALLOW_CANDLE.get()) && level.getBlockState(candle).getLightEmission() == 14
                && level.getBlockState(candle).getCollisionShape(level, candle).isEmpty(), "native registered tallow candle places on support, lights14, and has no collision");
        level.destroyBlock(support, false);
        test.assertTrue(level.getBlockState(candle).isAir(), "native support loss drops/removes tallow rather than borrowing vanilla candle mechanics");
        for (int z = 1; z <= 6; ++z) for (int sign : new int[] {-1, 1}) {
            BlockPos pos = matrix.offset(sign * 12, 5, sign * z);
            put(level, pos.below(), Blocks.STONE.defaultBlockState());
            put(level, pos, ThaumcraftInfusion.TALLOW_CANDLE.get().defaultBlockState());
        }
        test.assertTrue(InfusionStructure.scan(level, matrix).symmetry() == -1, "six opposed tallow pairs at radius12/above matrix contribute -1.2 then truncate toward zero");
        for (int z = 1; z <= 6; ++z) for (int sign : new int[] {-1, 1})
            put(level, matrix.offset(sign * 11, 4, sign * z), sign < 0 ? Blocks.SKELETON_SKULL.defaultBlockState() : Blocks.SKELETON_WALL_SKULL.defaultBlockState());
        test.assertTrue(InfusionStructure.scan(level, matrix).symmetry() == -2, "native standing and wall skulls stabilize independently of the peripheral pedestal volume");
        for (var crystal : ThaumcraftContent.CRYSTALS.values()) test.assertTrue(crystal.get() instanceof IInfusionStabiliser stabiliser
                && stabiliser.canStabaliseInfusion(level, matrix), "existing flattened crystal actually implements original stabiliser hook");
        for (var ore : ThaumcraftOres.INFUSED.values()) test.assertTrue(!((Block) ore.get() instanceof IInfusionStabiliser), "infused ore is not a crystal stabiliser");
        BlockPos remote = new BlockPos(30_000_000, matrix.getY(), 30_000_000);
        test.assertTrue(!FluxFiniteBlock.loaded(level, remote) && InfusionStructure.scan(level, remote).pedestals().isEmpty()
                && !FluxFiniteBlock.loaded(level, remote), "actual full-volume scan never loads a remote chunk");
    }

    private static void hazards(GameTestHelper test, ServerLevel level, BlockPos pos, ServerPlayer player, AABB box) {
        var matrix = (TileInfusionMatrix) level.getBlockEntity(pos);
        for (BlockPos cell : BlockPos.betweenClosed(pos.offset(-12, -10, -12), pos.offset(12, 5, 12))) {
            if ((cell.getX() != pos.getX() || cell.getZ() != pos.getZ()) && level.getBlockEntity(cell) instanceof TilePedestal pedestal) {
                pedestal.clearContent(); put(level, cell, Blocks.AIR.defaultBlockState());
            }
        }
        BlockPos peripheral = pos.offset(3, -2, 0);
        put(level, peripheral, ThaumcraftInfusion.PEDESTAL.get().defaultBlockState());
        near(player, pos);
        matrix.startCrafting(player); // Original start rescans before finding the empty center; no recipe is injected.
        test.assertTrue(!matrix.isCrafting() && matrix.getPedestalPositions().equals(List.of(peripheral)), "hazard targets come from a real native surroundings rescan");
        player.setPos(pos.getX() + 14, pos.getY() + 1, pos.getZ());
        int[][] ejections = {{0, 0}, {2, 0}, {10, 0}, {13, 0}, {6, 1}, {17, 1}, {1, 2}, {11, 2}, {19, 3}, {7, 4}, {4, 5}, {15, 5}};
        for (int[] event : ejections) {
            put(level, peripheral.above(), Blocks.AIR.defaultBlockState());
            var pedestal = (TilePedestal) level.getBlockEntity(peripheral);
            ItemStack item = named(5, "hazard-" + event[0]); pedestal.setInfusionStack(item.copy());
            BlockState terrain = level.getBlockState(peripheral);
            var blastTarget = event[1] == 5 ? EntityType.IRON_GOLEM.create(level) : null;
            if (event[1] == 5 && blastTarget == null) throw new IllegalStateException("Native blast fixture missing");
            try {
                if (blastTarget != null) {
                    blastTarget.setPos(Vec3.atBottomCenterOf(peripheral.above())); blastTarget.setNoAi(true);
                    if (!level.addFreshEntity(blastTarget)) throw new IllegalStateException("Native blast fixture spawn refused");
                }
                InfusionInstability.apply(matrix, event[0]);
                if (blastTarget != null) test.assertTrue(blastTarget.getHealth() < blastTarget.getMaxHealth(), "type5 eject really explodes/damages, not just a visual/empty-slot handler: " + event[0]);
            } finally { if (blastTarget != null) blastTarget.discard(); }
            test.assertTrue(pedestal.isEmpty() && level.getBlockState(peripheral) == terrain, "eject/destroy event empties the real stack without destroying terrain: " + event[0]);
            if (event[1] == 1 || event[1] == 3) test.assertTrue(level.getBlockState(peripheral.above()).is(ThaumcraftFlux.FLUX_GOO.get())
                    && FluxFiniteBlock.quanta(level.getBlockState(peripheral.above())) == 8, "original goo metadata7 means eight real quanta: " + event[0]);
            if (event[1] == 2 || event[1] == 4) test.assertTrue(level.getBlockState(peripheral.above()).is(ThaumcraftFlux.FLUX_GAS.get())
                    && FluxFiniteBlock.quanta(level.getBlockState(peripheral.above())) == 8, "original gas metadata7 means eight real quanta: " + event[0]);
            long drops = level.getEntitiesOfClass(ItemEntity.class, box).stream().filter(entity -> ItemStack.matches(item, entity.getItem())).count();
            // Type5 really spawns BEFORE its explosion: the native blast is allowed to destroy that drop.
            if (event[1] < 3) test.assertTrue(drops == 1, "non-destructive hazard drops one whole component stack: " + event[0]);
            if (event[1] == 3 || event[1] == 4) test.assertTrue(drops == 0, "destructive hazard does not refund its component: " + event[0]);
        }
        var first = cow(level, pos.offset(2, 0, 0));
        var second = cow(level, pos.offset(3, 0, 0));
        try {
            for (int event : new int[] {3, 8, 14, 12}) {
                first.setHealth(first.getMaxHealth()); second.setHealth(second.getMaxHealth()); first.invulnerableTime = second.invulnerableTime = 0;
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(10));
                test.assertTrue(targets.size() == 2, "only the two native living fixtures are in the original hazard AABB");
                RandomSource expected = RandomSource.create(700 + event); level.random.setSeed(700 + event);
                int damage = 4 + expected.nextInt(4);
                InfusionInstability.apply(matrix, event);
                float otherDamage = targets.get(1).getMaxHealth() - targets.get(1).getHealth();
                // Native hurt sounds may consume the modern world's sound seed
                // between targets. Do not replace native hurt just to fake a tail RNG.
                test.assertTrue(targets.getFirst().getHealth() == targets.getFirst().getMaxHealth() - damage
                                && (event == 12 ? otherDamage >= 4 && otherDamage <= 7 : otherDamage == 0),
                        "native query order and magic damage4..7 distinguish zapfirst from zapall: " + event);
            }
            var taint = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ThaumcraftInfusion.FLUX_TAINT.get());
            for (int event : new int[] {5, 16, 18}) for (boolean chooseTaint : new boolean[] {true, false}) {
                first.removeAllEffects(); second.removeAllEffects();
                long seed = seed(random -> random.nextBoolean() == chooseTaint);
                RandomSource expected = RandomSource.create(seed); level.random.setSeed(seed);
                var targets = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(10));
                InfusionInstability.apply(matrix, event);
                for (int i = 0; i < targets.size(); ++i) {
                    LivingEntity target = targets.get(i);
                    if (i > 0 && event != 18) { test.assertTrue(target.getActiveEffects().isEmpty(), "harmfirst stops after the first native target"); continue; }
                    boolean poisoned = expected.nextBoolean();
                    MobEffectInstance effect = target.getEffect(poisoned ? taint : ThaumcraftFlux.visExhaust());
                    test.assertTrue(effect != null && effect.getDuration() == (poisoned ? 120 : 2400) && effect.getAmplifier() == 0
                            && effect.isAmbient() != poisoned && (poisoned || FluxEffects.isUncurable(effect)), "real taint120/0 or ambient uncurable exhaustion2400/0: " + event);
                }
            }
            first.removeAllEffects(); second.removeAllEffects(); first.setHealth(first.getMaxHealth()); first.invulnerableTime = 0;
            BlockState terrain = level.getBlockState(pos); level.random.setSeed(49);
            InfusionInstability.apply(matrix, 9);
            test.assertTrue(first.getHealth() < first.getMaxHealth() && level.getBlockState(pos) == terrain && InfusionStructure.isValid(level, pos), "event9 is a real damaging native explosion, never a terrain-destroying stand-in");
        } finally { first.discard(); second.discard(); }
        near(player, pos);
        for (boolean sticky : new boolean[] {true, false}) {
            var state = KnowledgeAccess.get(player); int oldSticky = state.getWarpSticky(), oldTemp = state.getWarpTemp(), oldPerm = state.getWarpPerm();
            long seed = seed(random -> { random.nextInt(1); return (random.nextFloat() < .25F) == sticky; });
            RandomSource expected = RandomSource.create(seed); expected.nextInt(1); expected.nextFloat();
            int amount = sticky ? 1 : 1 + expected.nextInt(5); level.random.setSeed(seed);
            InfusionInstability.apply(matrix, 20);
            test.assertTrue(state.getWarpSticky() == oldSticky + (sticky ? amount : 0) && state.getWarpTemp() == oldTemp + (sticky ? 0 : amount)
                    && state.getWarpPerm() == oldPerm && state.getWarpCounter() == state.getTotalWarp(), "event20 changes existing knowledge/counter using original sticky25% or temporary1..5 branch");
        }
        var effect = (PotionFluxTaint) ThaumcraftInfusion.FLUX_TAINT.get();
        var undead = EntityType.ZOMBIE.create(level); var tainted = new TaintedStand(level); var poisoned = cow(level, pos.offset(5, 0, 0));
        try {
            if (undead == null) throw new IllegalStateException("Native undead fixture missing");
            tainted.setHealth(10); float undeadHealth = undead.getHealth();
            effect.applyEffectTick(tainted, 0); effect.applyEffectTick(undead, 0);
            poisoned.setHealth(.5F); poisoned.invulnerableTime = 0; effect.applyEffectTick(poisoned, 0);
            test.assertTrue(tainted.getHealth() == 11 && undead.getHealth() == undeadHealth && poisoned.getHealth() == 0,
                    "real flux taint heals ITaintedMob, ignores native undead, and has no vanilla-poison nonlethal floor");
            test.assertTrue(effect.shouldApplyEffectTickThisTick(40, 0) && !effect.shouldApplyEffectTickThisTick(39, 0)
                    && effect.shouldApplyEffectTickThisTick(20, 1) && effect.shouldApplyEffectTickThisTick(1, 6), "taint period is exactly40>>amplifier, then every tick");
        } finally { if (undead != null) undead.discard(); tainted.discard(); poisoned.discard(); }
    }

    private static void put(ServerLevel level, BlockPos pos, BlockState state) { level.setBlock(pos, state, QUIET); }
    private static void near(ServerPlayer player, BlockPos pos) { player.setPos(pos.getX() + .5, pos.getY() + .2, pos.getZ() + 2.5); }
    private static BlockHitResult hit(BlockPos pos) { return new BlockHitResult(Vec3.atCenterOf(pos).add(0, .5, 0), Direction.UP, pos, false); }
    private static InteractionResult use(ServerPlayer player, BlockPos pos) { return player.gameMode.useItemOn(player, player.serverLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit(pos)); }
    private static ItemStack named(int count, String name) { ItemStack stack = new ItemStack(Items.DIAMOND, count); stack.set(DataComponents.CUSTOM_NAME, Component.literal(name)); return stack; }
    private static ItemStack goldWand() { ItemStack wand = new ItemStack(ThaumcraftContent.WAND.get()); wand.set(ThaumcraftContent.WAND_DATA.get(), new WandData("greatwood", "gold", Map.of())); return wand; }
    private static Cow cow(ServerLevel level, BlockPos pos) {
        Cow cow = EntityType.COW.create(level);
        if (cow == null) throw new IllegalStateException("Native cow fixture missing");
        cow.setPos(Vec3.atBottomCenterOf(pos)); cow.setNoAi(true);
        if (!level.addFreshEntity(cow)) throw new IllegalStateException("Native cow fixture spawn refused");
        return cow;
    }
    private static long seed(Predicate<RandomSource> condition) { for (long seed = 0; seed < 100_000; ++seed) if (condition.test(RandomSource.create(seed))) return seed; throw new IllegalStateException("No native RNG seed for infusion fixture"); }
    private static final class TaintedStand extends ArmorStand implements ITaintedMob { private TaintedStand(ServerLevel level) { super(level, 0, 0, 0); } }
}
