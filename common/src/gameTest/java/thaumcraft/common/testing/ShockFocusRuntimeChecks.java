package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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
import thaumcraft.common.items.wands.foci.ItemFocusFire;
import thaumcraft.common.items.wands.foci.ItemFocusShock;
import thaumcraft.common.items.wands.foci.WandFocusActions;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.FocusSelectionNetworking;
import thaumcraft.common.lib.network.FocusSelectionPayload;
import thaumcraft.common.lib.network.ShockFxPayload;
import thaumcraft.common.tiles.TileArcaneWorkbench;
import thaumcraft.common.tiles.TileNode;

/** One native case, not wire/pixel/audio evidence. Research, charged paying wands,
 * raw ingredients, Fire focus and combat targets are explicit unit fixtures.
 * Shock output/payment are native. No RNG seed, test clock, sleep or hurt/iframe reset.
 * GameTest's accelerated clock is NOT real250ms. Delayed paths have both a
 * ten-second wall deadline and a bounded accelerated-tick allowance. */
public final class ShockFocusRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private ShockFocusRuntimeChecks() {}

    public static void run(GameTestHelper test, ServerPlayer player) {
        test.assertTrue(player.level() == test.getLevel() && player.containerMenu == player.inventoryMenu
                && !player.isUsingItem() && WandFocusActions.cooldownMillis(player) == 0, "owned actor has no pre-existing menu/use/cooldown to discard");
        var level = test.getLevel();
        var inventory = new ArrayList<ItemStack>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) inventory.add(player.getInventory().getItem(slot).copy());
        var knowledge = KnowledgeAccess.get(player).copy();
        var effects = player.getActiveEffects().stream().map(MobEffectInstance::new).toList();
        var position = player.position(); var motion = player.getDeltaMovement();
        var mode = player.gameMode.getGameModeForPlayer();
        int selected = player.getInventory().selected;
        float yaw = player.getYRot(), pitch = player.getXRot(), fall = player.fallDistance;
        float health = player.getHealth(), absorption = player.getAbsorptionAmount();
        boolean gravity = player.isNoGravity(), grounded = player.onGround(), sneaking = player.isShiftKeyDown(), sprinting = player.isSprinting();
        var abilities = new CompoundTag(); player.getAbilities().addSaveData(abilities);
        var food = new CompoundTag(); player.getFoodData().addAdditionalSaveData(food);
        var stat = Stats.ITEM_CRAFTED.get(ThaumcraftFoci.SHOCK_FOCUS.get());
        int crafted = player.getStats().getValue(stat);
        var holder = level.getRecipeManager().byKey(id("arcane/focus_shock")).orElseThrow();
        boolean learned = player.getRecipeBook().contains(holder);
        BlockPos table = test.absolutePos(new BlockPos(3, 2, 3));
        Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        Map<BlockPos, CompoundTag> tiles = new LinkedHashMap<>();
        // A narrow VERTICAL lane avoids neighbouring tests/players and allows a
        // true20-block entity ray without assuming the tiny template is20 wide.
        for (BlockPos mutable : BlockPos.betweenClosed(table.offset(-1, 0, -1), table.offset(1, 26, 1))) {
            BlockPos pos = mutable.immutable(); blocks.put(pos, level.getBlockState(pos));
            if (level.getBlockEntity(pos) != null) tiles.put(pos, level.getBlockEntity(pos).saveWithFullMetadata(level.registryAccess()));
        }
        List<Entity> targets = new ArrayList<>();
        boolean[] restored = {false};
        Runnable cleanup = () -> {
            if (restored[0]) return;
            restored[0] = true;
            player.stopUsingItem(); WandFocusActions.clearCooldown(player);
            player.closeContainer();
            if (level.getBlockEntity(table) instanceof TileArcaneWorkbench tile) tile.clearContent();
            targets.forEach(Entity::discard);
            blocks.forEach((pos, state) -> level.setBlock(pos, state, QUIET));
            tiles.forEach((pos, tag) -> { if (level.getBlockEntity(pos) != null) level.getBlockEntity(pos).loadWithComponents(tag, level.registryAccess()); });
            player.removeAllEffects(); player.getInventory().clearContent();
            for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().setItem(slot, inventory.get(slot).copy());
            player.getInventory().selected = selected; player.doTick(); // Native equipment modifier removal.
            for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().setItem(slot, inventory.get(slot));
            player.getInventory().selected = selected; player.setGameMode(mode);
            player.getAbilities().loadSaveData(abilities); player.onUpdateAbilities();
            player.setNoGravity(gravity); player.setOnGround(grounded); player.fallDistance = fall;
            player.setShiftKeyDown(sneaking); player.setSprinting(sprinting);
            player.setPos(position); player.setDeltaMovement(motion); player.setYRot(yaw); player.setXRot(pitch);
            player.getFoodData().readAdditionalSaveData(food); player.removeAllEffects(); effects.forEach(player::addEffect);
            player.setHealth(health); player.setAbsorptionAmount(absorption); KnowledgeAccess.get(player).copyFrom(knowledge);
            player.getStats().setValue(player, stat, crafted);
            if (!learned) player.resetRecipes(List.of(holder));
        };
        new Check(test, player, table, targets, cleanup).guard(() -> {
            player.getInventory().clearContent(); player.getInventory().selected = 0; player.removeAllEffects();
            player.setGameMode(GameType.SURVIVAL); player.setNoGravity(true); player.setShiftKeyDown(false); player.setSprinting(false);
            blocks.keySet().forEach(pos -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), QUIET));
        }, true);
    }

    private static final class Check {
        private final GameTestHelper test;
        private final ServerPlayer player;
        private final BlockPos table;
        private final Vec3 origin;
        private final List<Entity> targets;
        private final Runnable cleanup;
        private final long deadline;
        private final long wallDeadline;
        private ItemStack wand;
        private Cow depletedTarget;
        private int phase;
        private long lastPulseStarted;

        private Check(GameTestHelper test, ServerPlayer player, BlockPos table, List<Entity> targets, Runnable cleanup) {
            this.test = test; this.player = player; this.table = table; this.targets = targets; this.cleanup = cleanup;
            origin = new Vec3(table.getX() + .5, table.getY() + 1, table.getZ() + .5);
            deadline = test.getTick() + 30000;
            // The wall bound only catches a hang: the tick bound is the real limit of the case.
            // A loaded runner stretches the eleven real 250ms cooldowns of the sequence,
            // ten seconds were too few there and sixty seconds failed on the slowest run.
            wallDeadline = System.nanoTime() + 180_000_000_000L;
        }

        private void guard(Runnable action, boolean begin) {
            try {
                action.run();
                if (begin) { aim(); craftAndSelect(); prices(); next(); }
            } catch (RuntimeException | Error failure) { cleanup.run(); throw failure; }
        }

        private void awaitCooldown(Runnable action) {
            if (test.getTick() >= deadline || System.nanoTime() >= wallDeadline) {
                cleanup.run(); test.fail("real Shock cooldown sequence exceeded one hundred eighty wall seconds/30000 accelerated native ticks; phase=" + phase); return;
            }
            if (WandFocusActions.cooldownMillis(player) > 0) test.runAfterDelay(1, () -> guard(() -> awaitCooldown(action), false));
            else {
                test.assertTrue(System.currentTimeMillis() - lastPulseStarted >= ItemFocusShock.COOLDOWN_MILLIS,
                        "eligible next action waits at least250 REAL milliseconds, not a count of nominal game ticks");
                action.run();
            }
        }

        private void aim() { player.setPos(origin); player.setDeltaMovement(Vec3.ZERO); player.setYRot(0); player.setXRot(-90); }

        private void craftAndSelect() {
            var level = test.getLevel();
            level.setBlock(table, ThaumcraftArcane.ARCANE_WORKBENCH.get().defaultBlockState(), QUIET);
            var tile = (TileArcaneWorkbench) level.getBlockEntity(table);
            var recipe = (ArcaneShapedRecipe) level.getRecipeManager().byKey(id("arcane/focus_shock")).orElseThrow().value();
            test.assertTrue(recipe.research().equals("FOCUSSHOCK") && recipe.pattern().equals(List.of("CQC", "Q#Q", "CQC"))
                    && recipe.vis().equals(Map.of("aer", 10, "ordo", 10, "perditio", 10)), "original literal Shock shape, craft gate and whole-vis costs");
            for (var ingredient : Map.of("C", ThaumcraftContent.SHARDS.get("aer").get(), "Q", Items.QUARTZ, "#", Items.POTATO).entrySet()) {
                var entries = recipe.key().get(ingredient.getKey()).getItems();
                test.assertTrue(entries.length == 1 && entries[0].is(ingredient.getValue()), "exact Shock ingredient, including raw POTATO center: " + ingredient.getKey());
            }
            for (int slot = 0; slot < 9; slot++) tile.setItem(slot, new ItemStack(slot == 4 ? Items.POTATO
                    : slot % 2 == 0 ? ThaumcraftContent.SHARDS.get("aer").get() : Items.QUARTZ));
            test.assertTrue(recipe.matches(ArcaneRecipeInput.snapshot(tile), level), "four real air shards, four quartz, one potato match");
            tile.setItem(4, new ItemStack(Items.BAKED_POTATO));
            test.assertTrue(!recipe.matches(ArcaneRecipeInput.snapshot(tile), level), "cooked potato is not an invented ore/tag replacement");
            tile.setItem(4, new ItemStack(Items.POTATO));
            ItemStack fire = new ItemStack(ThaumcraftFoci.FIRE_FOCUS.get()); fire.set(DataComponents.CUSTOM_NAME, Component.literal("retained Fire"));
            ItemStack payer = charged("gold", 2000); payer.set(ThaumcraftFoci.WAND_FOCUS.get(), new FocusData(fire)); tile.setItem(10, payer);
            KnowledgeAccess.get(player).removeResearch("FOCUSSHOCK");
            player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(table), Direction.UP, table, false));
            test.assertTrue(player.containerMenu instanceof ContainerArcaneWorkbench, "real block use opens native arcane menu");
            var menu = (ContainerArcaneWorkbench) player.containerMenu;
            var before = tile.saveWithoutMetadata(level.registryAccess()); menu.clicked(0, 0, ClickType.PICKUP, player);
            test.assertTrue(menu.getCarried().isEmpty() && before.equals(tile.saveWithoutMetadata(level.registryAccess())), "absent research cannot consume or produce");
            KnowledgeAccess.get(player).addResearch("FOCUSSHOCK");
            tile.getItem(10).set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(payer).withAmount("aer", 999)); menu.broadcastChanges();
            before = tile.saveWithoutMetadata(level.registryAccess()); menu.clicked(0, 0, ClickType.PICKUP, player);
            test.assertTrue(menu.getCarried().isEmpty() && before.equals(tile.saveWithoutMetadata(level.registryAccess())), "one short primal refuses atomically");
            tile.getItem(10).set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(payer).withAmount("aer", 2000)); menu.broadcastChanges();
            test.assertTrue(menu.craftable() && menu.requiredVis("aer") == 1000 && menu.requiredVis("ordo") == 1000 && menu.requiredVis("perditio") == 1000,
                    "gold-cap native preview prices each original10vis separately");
            menu.clicked(0, 0, ClickType.PICKUP, player);
            ItemStack shock = menu.getCarried(); menu.setCarried(ItemStack.EMPTY);
            test.assertTrue(shock.is(ThaumcraftFoci.SHOCK_FOCUS.get()) && shock.getCount() == 1, "one Shock focus is actually crafted, not supplied");
            for (int slot = 0; slot < 9; slot++) test.assertTrue(tile.getItem(slot).isEmpty(), "native craft consumes every ingredient once");
            wand = tile.removeItemNoUpdate(10);
            for (String tag : WandData.PRIMALS) test.assertTrue(ItemWandCasting.getVis(wand, tag) == (List.of("aer", "ordo", "perditio").contains(tag) ? 1000 : 2000), "actual craft debits only its three primals: " + tag);
            test.assertTrue(ItemStack.matches(FocusData.get(wand), fire), "crafting preserves the old Fire focus and its native components");
            player.closeContainer(); player.setItemInHand(InteractionHand.MAIN_HAND, wand);
            shock.set(DataComponents.CUSTOM_NAME, Component.literal("earned Shock"));
            var custom = new CompoundTag(); custom.putString("preserve", "shock-components"); shock.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
            player.getInventory().setItem(5, shock);
            var material = ItemWandCasting.data(wand);
            test.assertTrue(FocusData.isSupportedFocus(shock) && FocusSelectionNetworking.handle(player, new FocusSelectionPayload(InteractionHand.MAIN_HAND, 5))
                    && player.getInventory().getItem(5).isEmpty() && ItemStack.matches(player.getInventory().getItem(1), fire)
                    && ItemStack.matches(FocusData.get(wand), shock) && ItemWandCasting.data(wand).equals(material), "existing F-selection receiver moves Shock, returns Fire and preserves all wand data");
            var focus = ThaumcraftFoci.SHOCK_FOCUS.get();
            test.assertTrue(focus.getFocusColor(shock) == 10466239 && focus.getSortingHelper(shock).equals("BL-1-1-1-1-1") && !focus.isVisCostPerTick(shock)
                    && focus.getVisCost(shock).getAmount(Aspect.AIR) == 25 && focus.getActivationCooldown(shock) == 250
                    && focus.getAnimation(shock) == ItemFocusBasic.WandFocusAnimation.CHARGE, "original basic Shock API constants and per-activation tooltip semantics");
            test.assertTrue(!FocusData.isSupportedFocus(new ItemStack(Items.QUARTZ)) && !FocusData.isSupportedFocus(shock.copyWithCount(2)), "ordinary registered items and stacked foci are explicitly unsupported");
            ItemStack nested = shock.copy(); nested.set(ThaumcraftFoci.WAND_FOCUS.get(), new FocusData(fire));
            test.assertTrue(!FocusData.isSupportedFocus(nested), "a Shock focus cannot nest another focus");
            test.assertTrue(ItemStack.matches(wand, ItemStack.parse(level.registryAccess(), wand.save(level.registryAccess())).orElseThrow()), "native persistent component codec preserves both focus kinds and Shock data");
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
            try {
                ItemStack.STREAM_CODEC.encode(buffer, wand);
                test.assertTrue(ItemStack.matches(wand, ItemStack.STREAM_CODEC.decode(buffer)), "native stream component codec round-trips Shock without disguising it as Fire");
                var payload = new ShockFxPayload(level.dimension().location(), player.getId(), player.getEyePosition(), player.getEyePosition().add(0, 19, 0), 17, true);
                ShockFxPayload.STREAM_CODEC.encode(buffer, payload);
                test.assertTrue(payload.equals(ShockFxPayload.STREAM_CODEC.decode(buffer)), "small S2C codec carries only dimension, caster, finite geometry, seed and impact");
            } finally { buffer.release(); }
            boolean rejected = false;
            try { new ShockFxPayload(level.dimension().location(), player.getId(), new Vec3(Double.NaN, 0, 0), Vec3.ZERO, 0, false); }
            catch (IllegalArgumentException expected) { rejected = true; }
            test.assertTrue(rejected, "nonfinite bolt geometry is rejected before rendering");
            KnowledgeAccess.get(player).removeResearch("FOCUSSHOCK"); // USE remains legal without craft research.
        }

        private void prices() {
            ItemStack thaumium = charged("thaumium", 1000);
            test.assertTrue(ItemFocusShock.pulseCost(thaumium, player) == 22, "bare Thaumium25-centivis pulse truncates to22");
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ThaumcraftWearables.GOGGLES.get()));
            test.assertTrue(ItemFocusShock.pulseCost(thaumium, player) == 21, "worn goggles price Shock at21, not whole-vis arcane pricing");
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ThaumcraftWearables.ROBE_CHESTPLATE.get()));
            player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ThaumcraftWearables.ROBE_LEGGINGS.get()));
            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ThaumcraftWearables.ROBE_BOOTS.get()));
            test.assertTrue(WandVisDiscount.percentagePoints(player, Aspect.AIR) == 10 && ItemFocusShock.pulseCost(thaumium, player) == 19
                    && ItemFocusFire.tickCost(thaumium, player) == 7, "real robe/goggle ten-point FLOAT sum gives Shock19 and Fire7, not rounded20/8");
            for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) player.setItemSlot(slot, ItemStack.EMPTY);
        }

        private <T extends Entity> T target(EntityType<T> type, double height, double offset) {
            T entity = type.create(test.getLevel()); test.assertTrue(entity != null, "native target fixture exists");
            targets.add(entity); entity.setPos(origin.add(offset, height, 0)); entity.setNoGravity(true);
            if (entity instanceof Mob mob) mob.setNoAi(true);
            test.assertTrue(test.getLevel().addFreshEntity(entity), "target fixture is in the actual native entity index");
            return entity;
        }

        private void pulse(Entity target, boolean cancel) {
            aim();
            int before = ItemWandCasting.getVis(wand, "aer"), price = ItemFocusShock.pulseCost(wand, player);
            test.assertTrue(player.gameMode.useItem(player, test.getLevel(), wand, InteractionHand.MAIN_HAND).consumesAction()
                    && player.isUsingItem() && WandFocusActions.cooldownMillis(player) == 0, "real RMB starts indefinite use and CLEARS initial250ms cooldown even without research");
            int[] events = {0};
            EntityEvent.LivingHurt observer = (victim, source, amount) -> {
                if (victim != target || source.getEntity() != player) return EventResult.pass();
                events[0]++;
                test.assertTrue(ItemWandCasting.getVis(wand, "aer") == before - price && source.is(DamageTypes.PLAYER_ATTACK)
                        && !source.is(DamageTypeTags.BYPASSES_ARMOR) && amount == 4F, "actual protection/damage event sees ALREADY PAID four ordinary player-attributed damage");
                return cancel ? EventResult.interruptFalse() : EventResult.pass();
            };
            EntityEvent.LIVING_HURT.register(observer);
            try { lastPulseStarted = System.currentTimeMillis(); wand.onUseTick(test.getLevel(), player, player.getUseItemRemainingTicks()); }
            finally { EntityEvent.LIVING_HURT.unregister(observer); }
            test.assertTrue(ItemWandCasting.getVis(wand, "aer") == before - price, "eligible held pulse spends exact centivis even on miss, nonliving, refused/PvP damage and creative");
            if (cancel) test.assertTrue(events[0] == 1, "real native protection event was reached and cancelled, not bypassed by a private damage setter");
        }

        private void next() {
            aim(); targets.forEach(Entity::discard); targets.clear();
            switch (phase++) {
                case 0 -> {
                    Cow cow = target(EntityType.COW, 18, 1.2);
                    pulse(cow, false);
                    test.assertTrue(cow.getHealth() == cow.getMaxHealth() - 4 && cow.getRemainingFireTicks() <= 0,
                            "twenty-block entity ray hits beyond10 and outside an unexpanded crosshair box for4, with no fire");
                    // Do not assume5 ticks ==250ms when the server is lagging.
                    // Exercise refusal only while the real deadline is still ahead.
                    if (WandFocusActions.cooldownMillis(player) > 25) {
                        int paid = ItemWandCasting.getVis(wand, "aer");
                        wand.onUseTick(test.getLevel(), player, player.getUseItemRemainingTicks());
                        test.assertTrue(ItemWandCasting.getVis(wand, "aer") == paid && cow.getHealth() == cow.getMaxHealth() - 4, "same held native tick cannot replay during250ms cooldown");
                        player.stopUsingItem();
                        test.assertTrue(!player.gameMode.useItem(player, test.getLevel(), wand, InteractionHand.MAIN_HAND).consumesAction()
                                && !player.isUsingItem(), "release/RMB cannot reset an unexpired pulse cooldown");
                    }
                }
                case 1 -> {
                    Cow cow = target(EntityType.COW, 23, 0); pulse(cow, false);
                    test.assertTrue(cow.getHealth() == cow.getMaxHealth(), "expanded box beyond the actual20-block eye ray is a PAID miss");
                }
                case 2 -> {
                    Cow cow = target(EntityType.COW, 18, 0);
                    BlockPos wall = table.above(9);
                    try {
                        for (BlockPos cell : BlockPos.betweenClosed(wall.offset(-1, 0, -1), wall.offset(1, 0, 1))) test.getLevel().setBlock(cell, Blocks.STONE.defaultBlockState(), QUIET);
                        pulse(cow, false);
                        test.assertTrue(cow.getHealth() == cow.getMaxHealth(), "actual collision-block LOS refuses the living target but still pays the pulse");
                    } finally { for (BlockPos cell : BlockPos.betweenClosed(wall.offset(-1, 0, -1), wall.offset(1, 0, 1))) test.getLevel().setBlock(cell, Blocks.AIR.defaultBlockState(), QUIET); }
                }
                case 3 -> {
                    var boat = target(EntityType.BOAT, 15, 0);
                    player.setGameMode(GameType.CREATIVE);
                    try { pulse(boat, false); test.assertTrue(boat.isAlive() && boat.getDamage() == 0, "real pickable nonliving boat receives no hurt or destruction; creative still pays"); }
                    finally { player.setGameMode(GameType.SURVIVAL); }
                }
                case 4 -> {
                    var zombie = target(EntityType.ZOMBIE, 15, 0);
                    zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET)); zombie.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                    zombie.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS)); zombie.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
                    zombie.tick(); pulse(zombie, false);
                    test.assertTrue(zombie.getArmorValue() >= 20 && zombie.getHealth() < zombie.getMaxHealth() && zombie.getHealth() > zombie.getMaxHealth() - 4,
                            "native armor/toughness reduces the four-point hit instead of being bypassed");
                }
                case 5 -> { Cow cow = target(EntityType.COW, 15, 0); pulse(cow, true); test.assertTrue(cow.getHealth() == cow.getMaxHealth(), "cancelled native hurt remains PAID and cannot be retried/refunded"); }
                case 6 -> {
                    Cow cow = target(EntityType.COW, 15, 0);
                    test.assertTrue(cow.hurt(player.damageSources().playerAttack(player), 4), "native initial hurt creates the immunity-frame fixture");
                    int immunity = cow.invulnerableTime; float health = cow.getHealth(); pulse(cow, false);
                    test.assertTrue(cow.getHealth() == health && cow.invulnerableTime == immunity, "paid Shock respects existing native immunity frames without resetting them");
                }
                case 7 -> pvp(false, false);
                case 8 -> pvp(true, true);
                case 9 -> pvp(true, false);
                case 10 -> {
                    depletedTarget = target(EntityType.COW, 15, 0);
                    // A separate finite25-centivis INPUT, not earned charge/output.
                    wand.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(wand).withAmount("aer", 25));
                    pulse(depletedTarget, false);
                    test.assertTrue(ItemWandCasting.getVis(wand, "aer") == 0 && depletedTarget.getHealth() == 6, "last affordable pulse spends all25, exactly once");
                    test.runAfterDelay(1, () -> guard(() -> awaitCooldown(this::depleted), false));
                    return; // Native held ticks may stop an unfunded use before our next callback.
                }
                default -> throw new IllegalStateException("Unknown Shock check phase");
            }
            player.stopUsingItem();
            test.runAfterDelay(1, () -> guard(() -> awaitCooldown(this::next), false));
        }

        private void pvp(boolean enabled, boolean teamRefusal) {
            // A NEW isolated native player target, never another test's current player.
            var server = player.server; boolean pvp = server.isPvpAllowed();
            var profile = new GameProfile(UUID.randomUUID(), "shock-" + UUID.randomUUID().toString().substring(0, 8));
            var victim = new ServerPlayer(server, test.getLevel(), profile, ClientInformation.createDefault()); targets.add(victim);
            var connection = new Connection(PacketFlow.SERVERBOUND);
            var channel = new EmbeddedChannel(connection);
            var scoreboard = server.getScoreboard(); var oldTeam = player.getTeam();
            var team = teamRefusal ? scoreboard.addPlayerTeam("shock_" + victim.getId()) : null;
            try {
                // Unlike GameTestHelper's always-creative mock, this unique-name
                // native target is really survival and does not collide with any
                // existing test-mock-player name/team/PlayerList lookup.
                server.getPlayerList().placeNewPlayer(connection, victim, CommonListenerCookie.createInitial(profile, false));
                victim.setGameMode(GameType.SURVIVAL); victim.setNoGravity(true); victim.setPos(origin.add(0, 15, 0));
                // Native ticks expire login protection; never assign its private timer.
                for (int tick = 0; tick < 61; tick++) victim.tick();
                server.setPvpAllowed(enabled);
                if (team != null) {
                    team.setAllowFriendlyFire(false); scoreboard.addPlayerToTeam(player.getScoreboardName(), team); scoreboard.addPlayerToTeam(victim.getScoreboardName(), team);
                    test.assertTrue(!victim.canHarmPlayer(player), "native friendly-fire-disabled team refuses player-attributed damage");
                }
                float health = victim.getHealth(); pulse(victim, false);
                test.assertTrue(victim.getHealth() == health - (enabled && !teamRefusal ? 4 : 0),
                        teamRefusal ? "native team refusal still costs25" : enabled ? "native survival PvP control takes4 when allowed" : "server PvP-disabled target still costs25");
            } finally {
                server.setPvpAllowed(pvp);
                if (team != null) {
                    scoreboard.removePlayerTeam(team);
                    if (oldTeam != null) scoreboard.addPlayerToTeam(player.getScoreboardName(), oldTeam);
                }
                if (server.getPlayerList().getPlayer(victim.getUUID()) == victim) server.getPlayerList().remove(victim);
                victim.discard(); channel.finishAndReleaseAll();
            }
        }

        private void depleted() {
            if (player.isUsingItem()) wand.onUseTick(test.getLevel(), player, player.getUseItemRemainingTicks());
            test.assertTrue(!player.isUsingItem() && ItemWandCasting.getVis(wand, "aer") == 0 && depletedTarget.getHealth() == 6, "eligible unfunded native use stops with no free damage/partial debit");
            targets.forEach(Entity::discard); targets.clear();
            BlockPos nodePos = table.above(4);
            test.getLevel().setBlock(nodePos, ThaumcraftContent.AURA_NODE.get().defaultBlockState(), QUIET);
            var node = (TileNode) test.getLevel().getBlockEntity(nodePos); node.setAspects(new AspectList().add(Aspect.AIR, 5));
            aim(); player.gameMode.useItem(player, test.getLevel(), wand, InteractionHand.MAIN_HAND);
            wand.onUseTick(test.getLevel(), player, Integer.MAX_VALUE - 2);
            test.assertTrue(player.isUsingItem() && ItemWandCasting.getVis(wand, "aer") == 100 && node.getAspects().getAmount(Aspect.AIR) == 4,
                    "initial native node use still outranks equipped Shock, draining100 instead of paying25");
            player.setXRot(0); wand.onUseTick(test.getLevel(), player, player.getUseItemRemainingTicks());
            test.assertTrue(!player.isUsingItem() && ItemWandCasting.getVis(wand, "aer") == 100, "looking away ends node session, never converts it into a Shock pulse");
            cleanup.run(); test.succeed();
        }
    }

    private static ItemStack charged(String cap, int amount) {
        ItemStack wand = new ItemStack(ThaumcraftContent.WAND.get());
        var vis = new LinkedHashMap<String, Integer>(); for (String tag : WandData.PRIMALS) vis.put(tag, amount);
        wand.set(ThaumcraftContent.WAND_DATA.get(), new WandData("silverwood", cap, vis)); return wand;
    }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }
}
