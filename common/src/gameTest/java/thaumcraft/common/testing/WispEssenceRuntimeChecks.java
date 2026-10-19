package thaumcraft.common.testing;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.EntityEvent;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.common.config.ThaumcraftAlembics;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftDistillation;
import thaumcraft.common.config.ThaumcraftEssences;
import thaumcraft.common.container.ContainerAlchemyFurnace;
import thaumcraft.common.items.ItemWispEssence;
import thaumcraft.common.items.WispEssenceData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.ObjectAspectResolver;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.lib.world.NodeHarvest;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileAlchemyFurnace;
import thaumcraft.common.tiles.TileNode;

/** One focused native harvest -> physical pickup -> finite distillation check.
 * NORMAL nodes/current vis, empty machines/filters, floor and ONE coal are
 * fixtures. No essence output, filled aspect buffer or auram is supplied.
 * Root owns the wrapper/bootstrap and execution; this source claims no PASS. */
public final class WispEssenceRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private WispEssenceRuntimeChecks() {}

    public static void run(GameTestHelper test, ServerPlayer player) {
        ServerLevel level = test.getLevel();
        BlockPos pos = test.absolutePos(new BlockPos(3, 2, 3)), furnacePos = pos.east(2);
        AABB bounds = new AABB(pos).inflate(4);
        var states = new LinkedHashMap<BlockPos, BlockState>();
        var tiles = new LinkedHashMap<BlockPos, CompoundTag>();
        for (BlockPos mutable : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(2, 3, 1))) {
            BlockPos saved = mutable.immutable();
            states.put(saved, level.getBlockState(saved));
            if (level.getBlockEntity(saved) != null) tiles.put(saved, level.getBlockEntity(saved).saveWithFullMetadata(level.registryAccess()));
        }
        test.assertTrue(player.level() == level && player.containerMenu == player.inventoryMenu, "fixture has a same-level player and no pre-existing menu to discard");
        var inventory = new ArrayList<ItemStack>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) inventory.add(player.getInventory().getItem(slot).copy());
        var knowledge = KnowledgeAccess.get(player).copy();
        var food = new CompoundTag(); player.getFoodData().addAdditionalSaveData(food);
        var abilities = new CompoundTag(); player.getAbilities().addSaveData(abilities);
        var position = player.position(); var motion = player.getDeltaMovement();
        var mode = player.gameMode.getGameModeForPlayer();
        int selected = player.getInventory().selected;
        float yaw = player.getYRot(), pitch = player.getXRot(), fall = player.fallDistance;
        boolean sneaking = player.isShiftKeyDown(), grounded = player.onGround();
        boolean tileDrops = level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS);
        var minedStat = Stats.BLOCK_MINED.get(ThaumcraftContent.AURA_NODE.get());
        var pickupStat = Stats.ITEM_PICKED_UP.get(ThaumcraftEssences.WISP_ESSENCE.get());
        int mined = player.getStats().getValue(minedStat), pickedUp = player.getStats().getValue(pickupStat);
        DeferredItemDrops.tick(level);
        Set<UUID> existing = ids(level, bounds);
        try {
            player.getInventory().clearContent(); player.getInventory().selected = 0;
            player.setGameMode(GameType.SURVIVAL); player.setShiftKeyDown(false);
            player.setPos(Vec3.atBottomCenterOf(pos.west())); player.setDeltaMovement(Vec3.ZERO); player.setOnGround(true); player.fallDistance = 0;
            level.getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS).set(true, level.getServer());
            states.keySet().forEach(saved -> level.setBlock(saved, saved.getY() == pos.getY() - 1
                    ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), QUIET));

            int[] current = {4, 5, 9, 10, 20, 70}, expected = {0, 1, 1, 2, 3, 8};
            for (int index = 0; index < current.length; index++) {
                for (ItemEntity drop : harvest(test, player, pos, Aspect.AIR, current[index], expected[index])) {
                    // Only the pickup delay is a timing fixture. Inventory items
                    // are transferred by the actual native entity pickup path.
                    drop.setNoPickUpDelay(); drop.playerTouch(player);
                    test.assertTrue(drop.isRemoved(), "actual harvested essence enters the native player inventory");
                }
            }
            test.assertTrue(inventoryCount(player) == 15, "inclusive thresholds4/5/9/10/20/70 yield0/1/1/2/3/8 separate physical items");
            int earnedSlot = essenceSlot(player);
            componentAndTooltip(test, player, player.getInventory().getItem(earnedSlot));
            distill(test, player, furnacePos, earnedSlot);
            authority(test, player, pos, bounds);

            // Original creative quirk and doTileDrops apply independently.
            for (GameType gameType : List.of(GameType.SURVIVAL, GameType.CREATIVE)) {
                player.setGameMode(gameType);
                for (boolean drops : new boolean[] {true, false}) {
                    level.getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS).set(drops, level.getServer());
                    harvest(test, player, pos, Aspect.AIR, 10, drops ? 2 : 0).forEach(ItemEntity::discard);
                }
            }
        } finally {
            player.closeContainer();
            NodeHarvest.endTick(level);
            // Never evacuate unconsumed fixture inputs into the world on cleanup.
            if (level.getBlockEntity(furnacePos) instanceof TileAlchemyFurnace furnace) furnace.clearContent();
            for (int height = 1; height <= 2; height++) if (level.getBlockEntity(furnacePos.above(height)) instanceof TileAlembic alembic) {
                alembic.clearContents(); alembic.setFilter(null);
            }
            states.forEach((saved, state) -> level.setBlock(saved, state, QUIET));
            tiles.forEach((saved, tag) -> {
                if (level.getBlockEntity(saved) != null) level.getBlockEntity(saved).loadWithComponents(tag, level.registryAccess());
            });
            DeferredItemDrops.tick(level);
            drops(level, bounds, existing).forEach(ItemEntity::discard);
            player.getInventory().clearContent();
            for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().setItem(slot, inventory.get(slot));
            player.getInventory().selected = selected; player.setGameMode(mode); player.setShiftKeyDown(sneaking);
            player.getAbilities().loadSaveData(abilities); player.onUpdateAbilities();
            player.setPos(position); player.setDeltaMovement(motion); player.setYRot(yaw); player.setXRot(pitch);
            player.setOnGround(grounded); player.fallDistance = fall;
            player.getFoodData().readAdditionalSaveData(food); KnowledgeAccess.get(player).copyFrom(knowledge);
            player.getStats().setValue(player, minedStat, mined); player.getStats().setValue(player, pickupStat, pickedUp);
            level.getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS).set(tileDrops, level.getServer());
        }
        test.succeed();
    }

    private static List<ItemEntity> harvest(GameTestHelper test, ServerPlayer player, BlockPos pos, Aspect aspect, int current, int expected) {
        ServerLevel level = test.getLevel();
        AABB bounds = new AABB(pos).inflate(2);
        Set<UUID> before = ids(level, bounds);
        TileNode node = place(level, pos, aspect, current);
        var state = level.getBlockState(pos);
        test.assertTrue(state.getDestroySpeed(level, pos) == 2 && state.getBlock().getExplosionResistance() == 200
                && state.getDestroyProgress(player, level, pos) > 0 && Block.getDrops(state, level, pos, node).isEmpty(),
                "NORMAL node is genuinely survival-mineable with original hardness2/resistance200 and no whole-node self loot");
        test.assertTrue(player.gameMode.destroyBlock(pos) && level.isEmptyBlock(pos) && node.isRemoved(), "real native player removal commits the node harvest");
        test.assertTrue(drops(level, bounds, before).isEmpty(), "no essence ADD occurs inside removal/native harvest callbacks");
        NodeHarvest.endTick(level); DeferredItemDrops.tick(level);
        List<ItemEntity> result = drops(level, bounds, before);
        test.assertTrue(result.size() == expected, "CURRENT " + aspect.getTag() + current + " (not base70) creates exactly " + expected + " one-item entities");
        for (ItemEntity drop : result) {
            ItemStack stack = drop.getItem();
            AspectList contained = ThaumcraftEssences.WISP_ESSENCE.get().getAspects(stack);
            test.assertTrue(stack.is(ThaumcraftEssences.WISP_ESSENCE.get()) && stack.getCount() == 1 && contained != null
                    && contained.size() == 1 && contained.getAmount(aspect) == 2, "each earned essence contains ONLY its own aspect2, with no injected item base");
            var nativeSave = drop.saveWithoutId(new CompoundTag());
            Vec3 offset = drop.position().subtract(Vec3.atLowerCornerOf(pos)), motion = drop.getDeltaMovement();
            test.assertTrue(nativeSave.getShort("PickupDelay") == 10 && offset.x >= .149999 && offset.x < .850001
                    && offset.y >= .149999 && offset.y < .850001 && offset.z >= .149999 && offset.z < .850001
                    && Math.abs(motion.x) <= .100001 && Math.abs(motion.z) <= .100001 && Math.abs(motion.y - .2F) < 1e-9,
                    "actual queued harvest retains original block scatter/default item motion/pickup10, not inventory evacuation scatter or pickup0");
        }
        player.gameMode.destroyBlock(pos); state.getBlock().destroy(level, pos, state);
        DeferredItemDrops.tick(level);
        test.assertTrue(drops(level, bounds, before).size() == expected, "second removal/replayed commit cannot duplicate earned essences");
        return result;
    }

    private static void componentAndTooltip(GameTestHelper test, ServerPlayer player, ItemStack earned) {
        var item = ThaumcraftEssences.WISP_ESSENCE.get();
        ThaumcraftEssences.register(); // Idempotent after the real root bootstrap.
        test.assertTrue(item instanceof IEssentiaContainerItem && earned.getMaxStackSize() == 64 && !earned.isDamageableItem()
                && ItemWispEssence.data(earned).amount("aer") == 2 && ItemWispEssence.color(earned) == (0xff000000 | Aspect.AIR.getColor()),
                "original plain64-stack item/API and opaque native first-aspect tint");
        test.assertTrue(ItemStack.matches(earned, ItemStack.parse(test.getLevel().registryAccess(), earned.save(test.getLevel().registryAccess())).orElseThrow()), "actual harvested native ItemStack save/load retains its contained aspects/count");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), test.getLevel().registryAccess());
        try {
            ItemStack.STREAM_CODEC.encode(buffer, earned);
            test.assertTrue(ItemStack.matches(earned, ItemStack.STREAM_CODEC.decode(buffer)) && !buffer.isReadable(), "native item packet round trip carries the actual registered component");
        } finally { buffer.release(); }
        var returned = item.getAspects(earned); returned.aspects.clear();
        test.assertTrue(item.getAspects(earned).getAmount(Aspect.AIR) == 2, "mutable API AspectList is a copy, never live immutable component storage");
        ItemStack untagged = new ItemStack(item);
        test.assertTrue(item.getAspects(untagged) == null && ItemWispEssence.data(untagged) == null
                && ItemWispEssence.color(untagged) >>> 24 == 255, "untagged view remains nullable and cycles an opaque display tint, not free contained aspects");
        var variants = ItemWispEssence.creativeVariants();
        test.assertTrue(variants.size() == Aspect.aspects.size() && variants.stream().allMatch(stack -> item.getAspects(stack).size() == 1
                && item.getAspects(stack).visSize() == 2), "original creative subitems contain exactly one real aspect2 per registered aspect");
        var tags = ObjectAspectResolver.get(earned, test.getLevel());
        test.assertTrue(tags.size() == 2 && tags.getAmount(Aspect.AIR) == 2 && tags.getAmount(Aspect.AURA) == 2, "real root object binding/resolver adds containedAIR2 plus separate baseAURA2 exactly once");

        KnowledgeAccess.get(player).copyFrom(new PlayerKnowledge());
        ItemStack aura = new ItemStack(item); item.setAspects(aura, new AspectList().add(Aspect.AURA, 2));
        var tooltip = new ArrayList<Component>(); item.appendAspectTooltip(aura, player, tooltip);
        test.assertTrue(tooltip.equals(List.of(Component.translatable("tc.aspect.unknown"))), "undiscovered contained aspect is unknown, not blindly revealed");
        KnowledgeAccess.get(player).addDiscoveredAspect("auram"); tooltip.clear(); item.appendAspectTooltip(aura, player, tooltip);
        test.assertTrue(tooltip.size() == 1 && tooltip.getFirst().getString().equals("Auram x2"), "discovered aspect shows the original name and CONTAINED amount, not base-plus-bonus4");
        item.setAspects(aura, new AspectList());
        test.assertTrue(item.getAspects(aura) == null, "empty API list removes the component rather than creating a corrupt empty container");

        var ordered = new LinkedHashMap<String, Integer>(); ordered.put("ignis", 2); ordered.put("aer", 3);
        var data = new WispEssenceData(ordered); ordered.clear();
        var encoded = WispEssenceData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        var decoded = WispEssenceData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        test.assertTrue(data.equals(decoded) && decoded.firstTag().equals("ignis") && decoded.toAspects().getAmount(Aspect.AIR) == 3, "ordered multi-aspect codec copies input and retains first-aspect order");
        ListTag duplicate = new ListTag(); duplicate.add(row("aer", 2)); duplicate.add(row("aer", 2));
        ListTag invalid = new ListTag(); invalid.add(row("aer", 0));
        ListTag unknown = new ListTag(); unknown.add(row("not_a_registered_wisp_aspect", 2));
        test.assertTrue(WispEssenceData.CODEC.parse(NbtOps.INSTANCE, duplicate).error().isPresent()
                && WispEssenceData.CODEC.parse(NbtOps.INSTANCE, invalid).error().isPresent()
                && WispEssenceData.CODEC.parse(NbtOps.INSTANCE, unknown).error().isPresent(), "corruption rejects duplicate/nonpositive/unknown data explicitly instead of silently losing entries");
        var badPacket = Unpooled.buffer();
        try {
            ByteBufCodecs.VAR_INT.encode(badPacket, 1); ByteBufCodecs.STRING_UTF8.encode(badPacket, "aer"); ByteBufCodecs.VAR_INT.encode(badPacket, -1);
            boolean rejected = false;
            try { WispEssenceData.STREAM_CODEC.decode(badPacket); } catch (DecoderException expected) { rejected = true; }
            test.assertTrue(rejected, "native component stream enforces the same amount validation");
        } finally { badPacket.release(); }
        String addonTag = "wisp_runtime_" + UUID.randomUUID().toString().replace("-", "");
        Aspect addon = new Aspect(addonTag, 0x123456, new Aspect[] {Aspect.AIR, Aspect.FIRE});
        try {
            WispEssenceData addonData = WispEssenceData.fromAspects(new AspectList().add(addon, 2));
            test.assertTrue(WispEssenceData.CODEC.parse(NbtOps.INSTANCE, WispEssenceData.CODEC.encodeStart(NbtOps.INSTANCE, addonData).getOrThrow()).getOrThrow()
                    .toAspects().getAmount(addon) == 2, "known addon-registered aspect IDs are not discarded by a built-in whitelist");
        } finally { Aspect.aspects.remove(addonTag, addon); }
    }

    private static void distill(GameTestHelper test, ServerPlayer player, BlockPos pos, int earnedSlot) {
        ServerLevel level = test.getLevel();
        level.setBlock(pos, ThaumcraftDistillation.ALCHEMY_FURNACE.get().defaultBlockState(), QUIET);
        var furnace = (TileAlchemyFurnace) level.getBlockEntity(pos);
        var outputs = new ArrayList<TileAlembic>();
        for (int height = 1; height <= 2; height++) {
            level.setBlock(pos.above(height), ThaumcraftAlembics.ALEMBIC.get().defaultBlockState(), QUIET);
            var alembic = (TileAlembic) level.getBlockEntity(pos.above(height));
            alembic.setFilter(height == 1 ? Aspect.AURA : Aspect.AIR); outputs.add(alembic);
        }
        test.assertTrue(furnace.getVis() == 0 && outputs.stream().allMatch(output -> output.getAmount() == 0), "machines start empty; filters designate types, not supplied source buffers");
        player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        test.assertTrue(player.containerMenu instanceof ContainerAlchemyFurnace, "real block interaction opens the native furnace menu");
        var menu = (ContainerAlchemyFurnace) player.containerMenu;
        test.assertTrue(menu.quickMoveStack(player, earnedSlot < 9 ? 29 + earnedSlot : 2 + earnedSlot - 9).getCount() == 15
                && furnace.getItem(0).getCount() == 15 && inventoryCount(player) == 0, "native quick-move transfers only the fifteen physically earned essences into the real input slot");
        player.getInventory().setItem(9, new ItemStack(Items.COAL));
        test.assertTrue(menu.quickMoveStack(player, 2).is(Items.COAL) && furnace.getItem(1).getCount() == 1, "one finite raw coal fixture enters the actual fuel slot");
        player.closeContainer();
        for (int tick = 0; tick < 1_500; tick++) {
            TileAlchemyFurnace.tick(level, pos, level.getBlockState(pos), furnace);
            test.assertTrue(furnace.getVis() >= 0 && furnace.getVis() <= 50 && 4 * furnace.getItem(0).getCount() + furnace.getVis()
                    + outputs.get(0).getAmount() + outputs.get(1).getAmount() == 60, "every real cook/extraction conserves the sixty earned aspect units within capacity50");
        }
        test.assertTrue(furnace.getItem(0).isEmpty() && furnace.getItem(1).isEmpty() && furnace.getVis() == 0
                && outputs.get(0).getAspect() == Aspect.AURA && outputs.get(0).getAmount() == 30
                && outputs.get(1).getAspect() == Aspect.AIR && outputs.get(1).getAmount() == 30,
                "real finite furnace/alembics process physical node drops into auram30+aer30 without injected essentia");
    }

    private static void authority(GameTestHelper test, ServerPlayer player, BlockPos pos, AABB bounds) {
        ServerLevel level = test.getLevel();
        Set<UUID> before = ids(level, bounds);
        TileNode node = place(level, pos, Aspect.AIR, 70);
        BlockState state = level.getBlockState(pos);
        BlockEvent.Break denied = (world, broken, ignored, actor, xp) -> world == level && broken.equals(pos) && actor == player
                ? EventResult.interruptFalse() : EventResult.pass();
        BlockEvent.BREAK.register(denied);
        try {
            test.assertTrue(!player.gameMode.destroyBlock(pos) && level.getBlockEntity(pos) == node && !node.isRemoved(), "real loader BREAK denial preserves the live node");
        } finally { BlockEvent.BREAK.unregister(denied); }
        DeferredItemDrops.tick(level);
        test.assertTrue(drops(level, bounds, before).isEmpty(), "denied break emits no essence");
        player.setGameMode(GameType.ADVENTURE);
        test.assertTrue(!player.gameMode.destroyBlock(pos) && level.getBlockEntity(pos) == node, "native adventure restriction also denies harvest");
        player.setGameMode(GameType.SURVIVAL);

        state.getBlock().playerWillDestroy(level, pos, state, player);
        state.getBlock().destroy(level, pos, state); // Not removed: cannot commit.
        DeferredItemDrops.tick(level);
        test.assertTrue(drops(level, bounds, before).isEmpty() && level.getBlockEntity(pos) == node, "a prepared but uncommitted/canceled removal is not a harvest");
        state.getBlock().playerWillDestroy(level, pos, state, player);
        ServerLevel other = level.getServer().getLevel(level.dimension() == Level.NETHER ? Level.OVERWORLD : Level.NETHER);
        test.assertTrue(other != null && other != level, "second native dimension exists for scope isolation");
        NodeHarvest.prepare(other, pos, state, player, node); NodeHarvest.committed(other, pos, state);
        NodeHarvest.endTick(level); // Expire the uncommitted player attempt.
        level.removeBlock(pos, false); state.getBlock().destroy(level, pos, state);
        DeferredItemDrops.tick(level);
        test.assertTrue(drops(level, bounds, before).isEmpty(), "wrong-dimension hooks and later non-player removal cannot redeem a stale snapshot");

        node = place(level, pos, Aspect.AIR, 70);
        state = level.getBlockState(pos);
        state.getBlock().playerWillDestroy(level, pos, state, player);
        var saved = node.saveWithFullMetadata(level.registryAccess());
        level.removeBlockEntity(pos);
        var replacement = new TileNode(ThaumcraftContent.NODE_BLOCK_ENTITY.get(), pos, state);
        replacement.loadWithComponents(saved, level.registryAccess()); level.setBlockEntity(replacement);
        NodeHarvest.endTick(level); DeferredItemDrops.tick(level);
        test.assertTrue(drops(level, bounds, before).isEmpty() && replacement.getAspects().getAmount(Aspect.AIR) == 70, "native BE removal/reload does not harvest or destroy the replacement's current vis");
        replacement.initializeNatural(new AspectList().add(Aspect.AIR, 5), null);
        test.assertTrue(player.gameMode.destroyBlock(pos), "replacement can be genuinely harvested on its own identity");
        NodeHarvest.endTick(level); DeferredItemDrops.tick(level);
        var replacedDrops = drops(level, bounds, before);
        test.assertTrue(replacedDrops.size() == 1, "replacement harvest earns current5's ONE drop, never old node70's eight");
        replacedDrops.forEach(ItemEntity::discard);

        node = place(level, pos, Aspect.AIR, 0);
        node.initializeNatural(new AspectList().add(Aspect.AIR, 0), null);
        for (int tick = 0; tick < 1_200 && !node.isRemoved(); tick++) TileNode.serverTick(level, pos, level.getBlockState(pos), node);
        DeferredItemDrops.tick(level);
        test.assertTrue(node.isRemoved() && drops(level, bounds, before).isEmpty(), "natural pruning/death is not a player harvest even after a node used to have base vis");
        place(level, pos, Aspect.AIR, 70); level.destroyBlock(pos, true); DeferredItemDrops.tick(level);
        test.assertTrue(drops(level, bounds, before).isEmpty(), "ordinary non-player destroyBlock/dropResources cannot mint node essences");

        node = place(level, pos, Aspect.AIR, 10);
        node.initializeNatural(new AspectList().add(Aspect.AIR, 10).add(Aspect.FIRE, 5).add(Aspect.AURA, 4).add(Aspect.EARTH, 20), null);
        node.takeVis("terra", 16);
        player.gameMode.destroyBlock(pos); NodeHarvest.endTick(level); DeferredItemDrops.tick(level);
        var mixed = drops(level, bounds, before);
        test.assertTrue(mixed.size() == 3 && mixed.stream().filter(drop -> ItemWispEssence.data(drop.getItem()).amount("aer") == 2).count() == 2
                && mixed.stream().filter(drop -> ItemWispEssence.data(drop.getItem()).amount("ignis") == 2).count() == 1,
                "EACH current aspect has its own inclusive threshold; auram4 and depleted terra4/base20 add nothing");
        mixed.forEach(ItemEntity::discard);

        var auraDrops = harvest(test, player, pos, Aspect.AURA, 5, 1);
        var auraTags = ObjectAspectResolver.get(auraDrops.getFirst().getItem(), level);
        test.assertTrue(auraTags.size() == 1 && auraTags.getAmount(Aspect.AURA) == 4, "an actual auram2-containing harvest resolves to auram4, not auram2 or6");
        auraDrops.forEach(ItemEntity::discard);

        // A native output ADD callback reenters a new player harvest at the SAME
        // position. Deferred delivery must neither erase nor duplicate either.
        boolean[] reentered = {false}, removedBeforeAdd = {false};
        EntityEvent.Add listener = (entity, world) -> {
            if (world == level && entity instanceof ItemEntity drop && drop.getItem().is(ThaumcraftEssences.WISP_ESSENCE.get())
                    && bounds.contains(drop.position()) && !reentered[0]) {
                reentered[0] = true; removedBeforeAdd[0] = level.isEmptyBlock(pos);
                place(level, pos, Aspect.AIR, 5); player.gameMode.destroyBlock(pos);
            }
            return EventResult.pass();
        };
        EntityEvent.ADD.register(listener);
        try {
            place(level, pos, Aspect.AIR, 70); player.gameMode.destroyBlock(pos);
            NodeHarvest.endTick(level); DeferredItemDrops.tick(level); DeferredItemDrops.tick(level);
            var reentrantDrops = drops(level, bounds, before);
            test.assertTrue(reentered[0] && removedBeforeAdd[0] && level.isEmptyBlock(pos) && reentrantDrops.size() == 9,
                    "real native reentrant ADD/harvest preserves old eight plus replacement one, with no removal-frame spawn or duplicate");
            reentrantDrops.forEach(ItemEntity::discard);
        } finally { EntityEvent.ADD.unregister(listener); }
    }

    private static TileNode place(ServerLevel level, BlockPos pos, Aspect aspect, int current) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), QUIET);
        level.setBlock(pos, ThaumcraftContent.AURA_NODE.get().defaultBlockState(), QUIET);
        var node = (TileNode) level.getBlockEntity(pos);
        // The primal threshold cases deliberately retain BASE70 after draining.
        node.initializeNatural(new AspectList().add(aspect, aspect.isPrimal() ? 70 : current), null);
        if (aspect.isPrimal()) node.takeVis(aspect.getTag(), 70 - current);
        return node;
    }
    private static int essenceSlot(ServerPlayer player) {
        for (int slot = 0; slot < 36; slot++) if (player.getInventory().getItem(slot).is(ThaumcraftEssences.WISP_ESSENCE.get())) return slot;
        throw new IllegalStateException("No physically earned wisp essence in inventory");
    }
    private static int inventoryCount(ServerPlayer player) {
        int count = 0;
        for (int slot = 0; slot < 36; slot++) if (player.getInventory().getItem(slot).is(ThaumcraftEssences.WISP_ESSENCE.get())) count += player.getInventory().getItem(slot).getCount();
        return count;
    }
    private static Set<UUID> ids(ServerLevel level, AABB bounds) { return level.getEntitiesOfClass(ItemEntity.class, bounds).stream().map(ItemEntity::getUUID).collect(Collectors.toSet()); }
    private static List<ItemEntity> drops(ServerLevel level, AABB bounds, Set<UUID> before) {
        return level.getEntitiesOfClass(ItemEntity.class, bounds, drop -> !before.contains(drop.getUUID()) && drop.getItem().is(ThaumcraftEssences.WISP_ESSENCE.get()));
    }
    private static CompoundTag row(String tag, int amount) { var row = new CompoundTag(); row.putString("tag", tag); row.putInt("amount", amount); return row; }
}
