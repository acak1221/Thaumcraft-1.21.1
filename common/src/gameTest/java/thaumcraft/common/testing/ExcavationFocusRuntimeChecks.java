package thaumcraft.common.testing;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import io.netty.buffer.Unpooled;
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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.config.ThaumcraftWearables;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.WandVisDiscount;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.ItemFocusExcavation;
import thaumcraft.common.items.wands.foci.WandFocusActions;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.ExcavationFxPayload;
import thaumcraft.common.lib.network.FocusSelectionNetworking;
import thaumcraft.common.lib.network.FocusSelectionPayload;
import thaumcraft.common.tiles.TileArcaneWorkbench;
import thaumcraft.common.tiles.TileNode;

/** One bounded native case. Charged wands, raw inputs, old foci/gear, research
 * and targets are explicit SERVER FIXTURES, not client acquisition. Actual
 * arcane output/payment/selection/codecs and ServerPlayer.doTick held-use are
 * exercised. No sleep, fake clock, RNG control, beam injection or manual drops.
 * Native protection/loot matrix belongs to the separate harvest runtime case. */
public final class ExcavationFocusRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private ExcavationFocusRuntimeChecks() {}

    public static void run(GameTestHelper test, ServerPlayer player) {
        test.assertTrue(player.level() == test.getLevel() && player.containerMenu == player.inventoryMenu && !player.isUsingItem()
                && WandFocusActions.cooldownMillis(player) == 0 && ItemFocusExcavation.progress(player) == null, "owned actor starts with no menu/use/cooldown/progress to discard");
        var level = test.getLevel(); var inventory = new ArrayList<ItemStack>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) inventory.add(player.getInventory().getItem(slot).copy());
        var knowledge = KnowledgeAccess.get(player).copy(); var effects = player.getActiveEffects().stream().map(MobEffectInstance::new).toList();
        var position = player.position(); var motion = player.getDeltaMovement(); var mode = player.gameMode.getGameModeForPlayer();
        int selected = player.getInventory().selected; float yaw = player.getYRot(), pitch = player.getXRot(), fall = player.fallDistance;
        float health = player.getHealth(), absorption = player.getAbsorptionAmount();
        boolean gravity = player.isNoGravity(), grounded = player.onGround(), sneaking = player.isShiftKeyDown(), sprinting = player.isSprinting();
        var abilities = new CompoundTag(); player.getAbilities().addSaveData(abilities);
        var food = new CompoundTag(); player.getFoodData().addAdditionalSaveData(food);
        var stat = Stats.ITEM_CRAFTED.get(ThaumcraftFoci.EXCAVATION_FOCUS.get()); int crafted = player.getStats().getValue(stat);
        var holder = level.getRecipeManager().byKey(id("arcane/focus_excavation")).orElseThrow(); boolean learned = player.getRecipeBook().contains(holder);
        BlockPos table = test.absolutePos(new BlockPos(3, 2, 3));
        Map<BlockPos, BlockState> blocks = new LinkedHashMap<>(); Map<BlockPos, CompoundTag> tiles = new LinkedHashMap<>();
        for (BlockPos cell : BlockPos.betweenClosed(table.offset(-1, 0, -1), table.offset(1, 14, 1))) {
            BlockPos pos = cell.immutable(); blocks.put(pos, level.getBlockState(pos));
            if (level.getBlockEntity(pos) != null) tiles.put(pos, level.getBlockEntity(pos).saveWithFullMetadata(level.registryAccess()));
        }
        AABB bounds = new AABB(Vec3.atLowerCornerOf(table.offset(-1, 0, -1)), Vec3.atLowerCornerOf(table.offset(2, 15, 2)));
        Set<UUID> entities = level.getEntities((Entity) null, bounds).stream().map(Entity::getUUID).collect(Collectors.toSet());
        try {
            player.getInventory().clearContent(); player.getInventory().selected = 0; player.removeAllEffects();
            player.setGameMode(GameType.SURVIVAL); player.setNoGravity(true); player.setShiftKeyDown(false); player.setSprinting(false);
            blocks.keySet().forEach(pos -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), QUIET));
            var check = new Check(test, player, table); check.craftAndSelect(); check.prices(); check.transitions();
        } finally {
            if (player.isUsingItem()) player.releaseUsingItem();
            player.stopUsingItem(); ItemFocusExcavation.clearPlayer(player); WandFocusActions.clearCooldown(player); player.closeContainer();
            if (level.getBlockEntity(table) instanceof TileArcaneWorkbench tile) tile.clearContent();
            level.getEntities((Entity) null, bounds, entity -> !entities.contains(entity.getUUID()) && (entity instanceof ItemEntity || entity instanceof ExperienceOrb)).forEach(Entity::discard);
            blocks.forEach((pos, state) -> level.setBlock(pos, state, QUIET));
            tiles.forEach((pos, tag) -> { if (level.getBlockEntity(pos) != null) level.getBlockEntity(pos).loadWithComponents(tag, level.registryAccess()); });
            player.removeAllEffects(); player.getInventory().clearContent();
            for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().setItem(slot, inventory.get(slot).copy());
            player.getInventory().selected = selected; player.doTick();
            for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().setItem(slot, inventory.get(slot));
            player.getInventory().selected = selected; player.setGameMode(mode); player.getAbilities().loadSaveData(abilities); player.onUpdateAbilities();
            player.setNoGravity(gravity); player.setOnGround(grounded); player.fallDistance = fall; player.setShiftKeyDown(sneaking); player.setSprinting(sprinting);
            player.setPos(position); player.setDeltaMovement(motion); player.setYRot(yaw); player.setXRot(pitch);
            player.getFoodData().readAdditionalSaveData(food); effects.forEach(player::addEffect); player.setHealth(health); player.setAbsorptionAmount(absorption);
            KnowledgeAccess.get(player).copyFrom(knowledge); player.getStats().setValue(player, stat, crafted); if (!learned) player.resetRecipes(List.of(holder));
        }
        test.succeed();
    }

    private static final class Check {
        private final GameTestHelper test;
        private final ServerPlayer player;
        private final BlockPos table, target, other;
        private final Vec3 origin;
        private ItemStack wand, earned;
        private Check(GameTestHelper test, ServerPlayer player, BlockPos table) {
            this.test = test; this.player = player; this.table = table; target = table.above(7); other = target.east();
            origin = new Vec3(table.getX() + .5, table.getY() + 1, table.getZ() + .5); aim(false);
        }
        private void craftAndSelect() {
            var level = test.getLevel(); level.setBlock(table, ThaumcraftArcane.ARCANE_WORKBENCH.get().defaultBlockState(), QUIET);
            var tile = (TileArcaneWorkbench) level.getBlockEntity(table);
            var recipe = (ArcaneShapedRecipe) level.getRecipeManager().byKey(id("arcane/focus_excavation")).orElseThrow().value();
            test.assertTrue(recipe.research().equals("FOCUSEXCAVATION") && recipe.pattern().equals(List.of("CQC", "Q#Q", "CQC"))
                    && recipe.vis().equals(Map.of("terra", 20, "ordo", 5, "perditio", 5)), "original research, literal recipe and20/5/5 whole-vis costs");
            for (int slot = 0; slot < 9; slot++) tile.setItem(slot, new ItemStack(slot == 4 ? Items.EMERALD
                    : slot % 2 == 0 ? ThaumcraftContent.SHARDS.get("terra").get() : Items.QUARTZ));
            test.assertTrue(recipe.matches(ArcaneRecipeInput.snapshot(tile), level) && recipe.key().get("#").test(new ItemStack(Items.EMERALD))
                    && !recipe.key().get("#").test(new ItemStack(Items.DIAMOND)), "four earth shards/four quartz/emerald match, not an invented diamond center");
            ItemStack fire = named(new ItemStack(ThaumcraftFoci.FIRE_FOCUS.get()), "old Fire");
            wand = charged("gold", 5000); wand.set(ThaumcraftFoci.WAND_FOCUS.get(), new FocusData(fire)); tile.setItem(10, wand);
            KnowledgeAccess.get(player).removeResearch("FOCUSEXCAVATION");
            player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(table), Direction.UP, table, false));
            test.assertTrue(player.containerMenu instanceof ContainerArcaneWorkbench, "real block use opens the arcane menu");
            var menu = (ContainerArcaneWorkbench) player.containerMenu; var before = tile.saveWithoutMetadata(level.registryAccess());
            menu.clicked(0, 0, ClickType.PICKUP, player);
            test.assertTrue(menu.getCarried().isEmpty() && before.equals(tile.saveWithoutMetadata(level.registryAccess())), "unresearched craft refuses atomically");
            KnowledgeAccess.get(player).addResearch("FOCUSEXCAVATION");
            tile.getItem(10).set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(tile.getItem(10)).withAmount("terra", 1999)); menu.broadcastChanges();
            before = tile.saveWithoutMetadata(level.registryAccess()); menu.clicked(0, 0, ClickType.PICKUP, player);
            test.assertTrue(menu.getCarried().isEmpty() && before.equals(tile.saveWithoutMetadata(level.registryAccess())), "one-short terra cannot partially pay/craft");
            tile.getItem(10).set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(tile.getItem(10)).withAmount("terra", 5000)); menu.broadcastChanges();
            test.assertTrue(menu.craftable() && menu.requiredVis("terra") == 2000 && menu.requiredVis("ordo") == 500 && menu.requiredVis("perditio") == 500, "native exact gold prices");
            menu.clicked(0, 0, ClickType.PICKUP, player); earned = menu.getCarried(); menu.setCarried(ItemStack.EMPTY);
            test.assertTrue(earned.is(ThaumcraftFoci.EXCAVATION_FOCUS.get()) && earned.getCount() == 1, "one actual paid Excavation output, not a supplied result");
            for (int slot = 0; slot < 9; slot++) test.assertTrue(tile.getItem(slot).isEmpty(), "every raw ingredient is consumed once");
            wand = tile.removeItemNoUpdate(10);
            for (String tag : WandData.PRIMALS) test.assertTrue(ItemWandCasting.getVis(wand, tag) == (tag.equals("terra") ? 3000 : tag.equals("ordo") || tag.equals("perditio") ? 4500 : 5000), "exact craft debit: " + tag);
            player.closeContainer(); player.setItemInHand(InteractionHand.MAIN_HAND, wand); earned = named(earned, "earned Excavation");
            player.getInventory().setItem(5, earned); var materials = ItemWandCasting.data(wand);
            test.assertTrue(select(5) && player.getInventory().getItem(5).isEmpty() && ItemStack.matches(player.getInventory().getItem(1), fire)
                    && ItemStack.matches(FocusData.get(wand), earned) && ItemWandCasting.data(wand).equals(materials), "native selection moves earned third focus and returns exact old Fire");
            ItemStack shock = named(new ItemStack(ThaumcraftFoci.SHOCK_FOCUS.get()), "old Shock"); player.getInventory().setItem(8, shock);
            test.assertTrue(select(8) && ItemStack.matches(FocusData.get(wand), shock) && select(2) && ItemStack.matches(FocusData.get(wand), earned), "Shock/Excavation round-trip preserves both component-bearing stacks");
            var focus = ThaumcraftFoci.EXCAVATION_FOCUS.get();
            test.assertTrue(focus.getFocusColor(earned) == 409606 && focus.getSortingHelper(earned).equals("BE-1-1-1-1-1") && focus.getActivationCooldown(earned) == 0
                    && focus.getAnimation(earned) == ItemFocusBasic.WandFocusAnimation.CHARGE && focus.isVisCostPerTick(earned)
                    && focus.getVisCost(earned).getAmount(Aspect.EARTH) == 15, "original BASIC constants, including misleading per-tick tooltip");
            for (ItemStack kind : List.of(fire, earned, shock)) {
                ItemStack copy = wand.copy(); copy.set(ThaumcraftFoci.WAND_FOCUS.get(), new FocusData(kind));
                test.assertTrue(ItemStack.matches(copy, ItemStack.parse(level.registryAccess(), copy.save(level.registryAccess())).orElseThrow()), "persistent native codec preserves each of the three focus kinds");
                var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
                try { ItemStack.STREAM_CODEC.encode(buffer, copy); test.assertTrue(ItemStack.matches(copy, ItemStack.STREAM_CODEC.decode(buffer)), "stream preserves full focus components"); }
                finally { buffer.release(); }
            }
            ItemStack nested = earned.copy(); nested.set(ThaumcraftFoci.WAND_FOCUS.get(), new FocusData(fire));
            test.assertTrue(!FocusData.isSupportedFocus(nested) && !FocusData.isSupportedFocus(earned.copyWithCount(2)) && !FocusData.isSupportedFocus(new ItemStack(Items.EMERALD)), "nested/stacked/unknown items stay unsupported");
            var payload = new ExcavationFxPayload(level.dimension().location(), player.getId(), player.getEyePosition(), Vec3.atCenterOf(target), target, 9, true, true, false);
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
            try { ExcavationFxPayload.STREAM_CODEC.encode(buffer, payload); test.assertTrue(payload.equals(ExcavationFxPayload.STREAM_CODEC.decode(buffer)), "finite S2C geometry/cracks/paid-refusal indicator round-trip"); }
            finally { buffer.release(); }
            KnowledgeAccess.get(player).removeResearch("FOCUSEXCAVATION"); // All actual use below remains legal.
        }
        private void prices() {
            test.assertTrue(ItemFocusExcavation.attemptCost(wand, player) == 15, "gold base attempt costs15 CENTIVIS");
            ItemStack thaumium = charged("thaumium", 1000);
            test.assertTrue(ItemFocusExcavation.attemptCost(thaumium, player) == 13, "bare Thaumium truncates13.5 to13");
            gear(true);
            test.assertTrue(WandVisDiscount.percentagePoints(player, Aspect.EARTH) == 10 && ItemFocusExcavation.attemptCost(thaumium, player) == 11, "actual full robe/goggle FLOAT discount gives11, NOT12");
            gear(false);
        }
        private void transitions() {
            test.assertTrue(Blocks.STONE.defaultBlockState().is(ItemFocusExcavation.FAST_BLOCKS) && Blocks.DIRT.defaultBlockState().is(ItemFocusExcavation.FAST_BLOCKS)
                    && !Blocks.IRON_BLOCK.defaultBlockState().is(ItemFocusExcavation.FAST_BLOCKS) && !Blocks.CLAY.defaultBlockState().is(ItemFocusExcavation.FAST_BLOCKS)
                    && !Blocks.GLASS.defaultBlockState().is(ItemFocusExcavation.FAST_BLOCKS) && !Blocks.SNOW_BLOCK.defaultBlockState().is(ItemFocusExcavation.FAST_BLOCKS), "explicit original materials, NOT modern tool tags");
            begin(); timed(Blocks.STONE, 8); timed(Blocks.DIRT, 4);
            put(target, Blocks.STONE); tick(3); progress(target, .5F); int paid = vis();
            player.releaseUsingItem(); progress(target, .5F); tick(2); progress(target, .5F);
            test.assertTrue(vis() == paid, "ordinary release and idle native ticks neither pay nor reset");
            begin(); tick(4); progress(target, 1.5F); test.assertTrue(vis() == paid, "same-world resume inherits old progress without interim debit");
            tick(1); test.assertTrue(test.getLevel().isEmptyBlock(target) && vis() == paid - 15, "resumed stone completes on original eighth HELD tick");

            put(target, Blocks.STONE); tick(2); progress(target, .25F); put(target, Blocks.DIRT); tick(1); progress(target, .5F);
            paid = vis(); tick(1); test.assertTrue(vis() == paid - 15 && test.getLevel().isEmptyBlock(target), "same-coordinate replacement inherits progress, not state identity");
            put(target, Blocks.DIRT); tick(1); progress(target, 0); player.releaseUsingItem();
            ItemStack old = wand; int oldVis = vis(); wand = charged("gold", 3000); wand.set(ThaumcraftFoci.WAND_FOCUS.get(), new FocusData(earned));
            player.setItemInHand(InteractionHand.MAIN_HAND, wand); begin();
            old.onUseTick(test.getLevel(), player, player.getUseItemRemainingTicks()); progress(target, 0);
            test.assertTrue(vis() == 3000 && ItemWandCasting.getVis(old, "terra") == oldVis, "stale wand callback cannot advance/pay on behalf of the new actual hand");
            tick(1); progress(target, .25F); tick(2);
            test.assertTrue(test.getLevel().isEmptyBlock(target) && vis() == 2985 && ItemWandCasting.getVis(old, "terra") == oldVis, "different fixture wand inherits even ZERO coordinate progress; only actual hand pays");

            put(target, Blocks.DIRT); tick(2); progress(target, .25F); put(other, Blocks.DIRT); aim(true); tick(1); progress(other, 0);
            put(target, Blocks.BEDROCK); aim(false); paid = vis(); tick(2); progress(other, 0);
            test.assertTrue(vis() == paid, "negative hardness does nothing, preserving PREVIOUS target/progress");
            aim(true); tick(1); progress(other, .25F); put(target, Blocks.AIR); put(other, Blocks.AIR); aim(false); tick(1);
            test.assertTrue(ItemFocusExcavation.progress(player).amount() == 0 && !ItemFocusExcavation.progress(player).pos().equals(other) && vis() == paid, "miss resets coordinates/progress without payment");
            timed(Blocks.TNT, 2); // Native full collision cube with hardness0; not instantly on a NEW coordinate.
            put(target, Blocks.IRON_BLOCK); tick(2); progress(target, .05F);
            miss(); put(target, Blocks.OBSIDIAN); tick(2); progress(target, .75F);
            miss(); put(target, Blocks.STONE); tick(3); progress(target, .5F);
            wand.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(wand).withAmount("terra", 14)); aim(true); tick(1); progress(target, .5F);
            test.assertTrue(!player.isUsingItem() && vis() == 14, "FIRST affordability stops before miss/ray/reset, with no partial debit");
            wand.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(wand).withAmount("terra", 15)); aim(false); begin(); tick(4); progress(target, 1.5F); tick(1);
            test.assertTrue(vis() == 0 && test.getLevel().isEmptyBlock(target), "depleted last affordable completion spends exactly15");
            var reset = ItemFocusExcavation.progress(player); tick(1);
            test.assertTrue(!player.isUsingItem() && vis() == 0 && reset.equals(ItemFocusExcavation.progress(player)), "next unfunded native tick stops without debit or progress reset");

            // Explicit finite found-focus/wand fixtures; no craft research is restored.
            wand = charged("thaumium", 11); wand.set(ThaumcraftFoci.WAND_FOCUS.get(), new FocusData(earned)); player.setItemInHand(InteractionHand.MAIN_HAND, wand);
            gear(true); begin(); timed(Blocks.DIRT, 4);
            test.assertTrue(vis() == 0, "real full-gear completion actually debits11 rather than merely reporting that price");
            player.stopUsingItem(); gear(false); wand = charged("gold", 300); wand.set(ThaumcraftFoci.WAND_FOCUS.get(), new FocusData(earned)); player.setItemInHand(InteractionHand.MAIN_HAND, wand); begin();
            put(target, Blocks.DIRT); tick(3); paid = vis(); int expected = paid - 15; int[] events = {0};
            var drops = test.getLevel().getEntities((Entity) null, new AABB(target).inflate(1)).stream().map(Entity::getUUID).collect(Collectors.toSet());
            BlockEvent.Break deny = (world, pos, state, actor, xp) -> {
                if (world != test.getLevel() || !pos.equals(target) || actor != player) return EventResult.pass();
                events[0]++; test.assertTrue(vis() == expected, "real native BREAK event observes ALREADY PAID completion"); return EventResult.interruptFalse();
            };
            BlockEvent.BREAK.register(deny);
            try { tick(1); } finally { BlockEvent.BREAK.unregister(deny); }
            test.assertTrue(events[0] == 1 && vis() == expected && test.getLevel().getBlockState(target).is(Blocks.DIRT)
                    && ItemFocusExcavation.progress(player).amount() == 0 && !ItemFocusExcavation.progress(player).pos().equals(target), "cancelled completion is paid once and reset regardless of failure");
            test.assertTrue(test.getLevel().getEntities((Entity) null, new AABB(target).inflate(1), entity -> !drops.contains(entity.getUUID()) && (entity instanceof ItemEntity || entity instanceof ExperienceOrb)).isEmpty(), "cancelled attempt emits no item/XP");
            tick(1); progress(target, 0); test.assertTrue(vis() == expected, "cancelled same-coordinate retry starts again at zero, no replayed completion");
            miss(); player.stopUsingItem(); player.setGameMode(GameType.CREATIVE); begin(); timed(Blocks.DIRT, 4);
            player.stopUsingItem(); player.setGameMode(GameType.SURVIVAL); begin();
            put(target, Blocks.STONE); player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 100, 4)); tick(2); progress(target, .25F); player.removeAllEffects();
            test.assertTrue(!KnowledgeAccess.get(player).isResearchComplete("FOCUSEXCAVATION"), "active found focus works without its crafting research");
            BlockPos nodePos = table.above(4); put(nodePos, Blocks.WATER); tick(1); progress(target, .5F);
            put(nodePos, ThaumcraftContent.AURA_NODE.get());
            var node = (TileNode) test.getLevel().getBlockEntity(nodePos); node.setAspects(new AspectList().add(Aspect.EARTH, 5));
            var cow = EntityType.COW.create(test.getLevel()); test.assertTrue(cow != null, "native entity obstruction fixture exists");
            try {
                cow.setNoAi(true); cow.setNoGravity(true); cow.setPos(origin.add(0, 3, 0));
                test.assertTrue(test.getLevel().addFreshEntity(cow), "entity fixture enters the native world index");
                paid = vis(); tick(1); progress(target, .75F);
                test.assertTrue(cow.getHealth() == cow.getMaxHealth() && vis() == paid && node.getAspects().getAmount(Aspect.EARTH) == 5
                        && !ItemWandCasting.isUsingNode(player), "held COLLIDER/fluid-NONE ray passes water/noncolliding node/entities with no damage, drain or interim debit");
            } finally { cow.discard(); }
            miss(); player.stopUsingItem();
            int earth = vis(); begin(); tick(3); // MAX_VALUE-2 is the first native count divisible by5.
            test.assertTrue(ItemWandCasting.isUsingNode(player) && vis() == earth + 100 && node.getAspects().getAmount(Aspect.EARTH) == 4, "native node-use priority still drains instead of excavating");
            player.setXRot(0); tick(1); test.assertTrue(!player.isUsingItem() && vis() == earth + 100, "node session cannot convert into focus while looking away");
        }
        private boolean select(int slot) { return FocusSelectionNetworking.handle(player, new FocusSelectionPayload(InteractionHand.MAIN_HAND, slot)); }
        private void gear(boolean wear) {
            player.setItemSlot(EquipmentSlot.HEAD, wear ? new ItemStack(ThaumcraftWearables.GOGGLES.get()) : ItemStack.EMPTY);
            player.setItemSlot(EquipmentSlot.CHEST, wear ? new ItemStack(ThaumcraftWearables.ROBE_CHESTPLATE.get()) : ItemStack.EMPTY);
            player.setItemSlot(EquipmentSlot.LEGS, wear ? new ItemStack(ThaumcraftWearables.ROBE_LEGGINGS.get()) : ItemStack.EMPTY);
            player.setItemSlot(EquipmentSlot.FEET, wear ? new ItemStack(ThaumcraftWearables.ROBE_BOOTS.get()) : ItemStack.EMPTY);
        }
        private void aim(boolean alternate) { player.setPos(origin.add(alternate ? 1 : 0, 0, 0)); player.setDeltaMovement(Vec3.ZERO); player.setYRot(0); player.setXRot(-90); }
        private void begin() { test.assertTrue(player.gameMode.useItem(player, test.getLevel(), wand, InteractionHand.MAIN_HAND).consumesAction() && player.isUsingItem(), "real RMB begins native CHARGE use without research"); }
        private void tick(int ticks) { for (int i = 0; i < ticks; i++) { player.setDeltaMovement(Vec3.ZERO); player.doTick(); } }
        private int vis() { return ItemWandCasting.getVis(wand, "terra"); }
        private void put(BlockPos pos, Block block) { test.getLevel().setBlock(pos, block.defaultBlockState(), QUIET); }
        private void miss() { put(target, Blocks.AIR); aim(false); tick(1); }
        private void progress(BlockPos pos, float amount) {
            var progress = ItemFocusExcavation.progress(player);
            test.assertTrue(progress != null && progress.pos().equals(pos) && progress.amount() == amount, "exact native progress " + pos + "=" + amount + ", actual=" + progress);
        }
        private void timed(Block block, int ticks) {
            put(target, block); int before = vis(), cost = ItemFocusExcavation.attemptCost(wand, player);
            for (int i = 1; i <= ticks; i++) {
                tick(1);
                test.assertTrue(vis() == before - (i == ticks ? cost : 0) && (i == ticks ? test.getLevel().isEmptyBlock(target) : test.getLevel().getBlockState(target).is(block)),
                        "true held native timing/no interim debit " + block + " tick" + i + "/" + ticks + ",vis=" + vis());
            }
        }
    }
    private static ItemStack charged(String cap, int amount) {
        ItemStack wand = new ItemStack(ThaumcraftContent.WAND.get()); var vis = new LinkedHashMap<String, Integer>();
        for (String tag : WandData.PRIMALS) vis.put(tag, amount);
        wand.set(ThaumcraftContent.WAND_DATA.get(), new WandData("silverwood", cap, vis)); return wand;
    }
    private static ItemStack named(ItemStack stack, String name) {
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name)); var tag = new CompoundTag(); tag.putString("preserve", name);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag)); return stack;
    }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }
}
