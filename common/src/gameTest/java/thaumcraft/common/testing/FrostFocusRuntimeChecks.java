package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import io.netty.util.ReferenceCountUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
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
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.config.ThaumcraftFrost;
import thaumcraft.common.config.ThaumcraftWearables;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.entities.projectile.EntityFrostShard;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.WandVisDiscount;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.ItemFocusFrost;
import thaumcraft.common.items.wands.foci.WandFocusActions;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.FocusSelectionNetworking;
import thaumcraft.common.lib.network.FocusSelectionPayload;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.tiles.TileArcaneWorkbench;

/** ONE bounded server case; charged wands/cap changes, raw inputs, old foci,
 * research and real worn gear are explicit SERVER FIXTURES, not client acquisition.
 * A unique native player proves real survival/creative, never GameTest's always-
 * creative mock. Native RMB/doTick, arcane output/payment, four-focus codecs and
 * real ADD cancellation are exercised; projectile combat belongs to its own case.
 * No sleep, fake clock, RNG seed, damage/iframe resets or synthetic FX. The caller
 * needs >=36000 timeoutTicks: internal bounds are30000 accelerated ticks/10 wall s. */
public final class FrostFocusRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private FrostFocusRuntimeChecks() {}

    public static void run(GameTestHelper test, ServerPlayer supplied) {
        test.assertTrue(supplied.level() == test.getLevel(), "wrapper actor belongs to this test level; its data is never changed");
        var check = new Check(test);
        check.guard(check::start);
    }

    private static final class Check implements AutoCloseable {
        private final GameTestHelper test;
        private final ServerLevel level;
        private final BlockPos table;
        private final Vec3 origin;
        private final AABB bounds;
        private final ServerPlayer player;
        private final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        private EmbeddedChannel channel;
        private final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        private final Map<BlockPos, CompoundTag> tiles = new LinkedHashMap<>();
        private final List<EntityFrostShard> shards = new ArrayList<>();
        private final List<ItemStack> foci = new ArrayList<>();
        private final List<String> lastSoundPackets = new ArrayList<>();
        private final long tickDeadline, wallDeadline;
        private PlayerKnowledge knowledge;
        private boolean learned, worldTouched, closed;
        private int crafted, phase;
        private long lastActivation;
        private ItemStack wand, earned;

        private Check(GameTestHelper test) {
            this.test = test; level = test.getLevel(); table = test.absolutePos(new BlockPos(3, 2, 3));
            origin = Vec3.atLowerCornerOf(table).add(.5, 1, .5);
            bounds = new AABB(Vec3.atLowerCornerOf(table.offset(-1, 0, -1)), Vec3.atLowerCornerOf(table.offset(2, 11, 2)));
            for (BlockPos cell : BlockPos.betweenClosed(table.offset(-1, 0, -1), table.offset(1, 10, 1))) {
                BlockPos pos = cell.immutable(); blocks.put(pos, level.getBlockState(pos));
                if (level.getBlockEntity(pos) != null) tiles.put(pos, level.getBlockEntity(pos).saveWithFullMetadata(level.registryAccess()));
            }
            var profile = new GameProfile(UUID.randomUUID(), "frost-" + UUID.randomUUID().toString().substring(0, 8));
            player = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
            tickDeadline = test.getTick() + 30000; wallDeadline = System.nanoTime() + 10_000_000_000L;
        }

        private void guard(Runnable action) {
            if (closed) return;
            try {
                test.assertTrue(test.getTick() < tickDeadline && System.nanoTime() < wallDeadline,
                        "Frost sequence stays within30000 accelerated ticks/ten real seconds; phase=" + phase);
                action.run();
            } catch (RuntimeException | Error failure) {
                try { close(); } catch (RuntimeException | Error cleanupFailure) { failure.addSuppressed(cleanupFailure); }
                throw failure;
            }
        }

        private void start() {
            channel = new EmbeddedChannel(connection);
            level.getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
            test.assertTrue(player.level() == level && level.getServer().getPlayerList().getPlayer(player.getUUID()) == player,
                    "unique actual connected native actor, not the supplied mock or another test's player");
            knowledge = KnowledgeAccess.get(player).copy();
            var holder = level.getRecipeManager().byKey(id("arcane/focus_frost")).orElseThrow();
            learned = player.getRecipeBook().contains(holder); crafted = player.getStats().getValue(Stats.ITEM_CRAFTED.get(ThaumcraftFoci.FROST_FOCUS.get()));
            player.getInventory().clearContent(); player.getInventory().selected = 0; player.removeAllEffects();
            player.setNoGravity(true); player.setShiftKeyDown(false); player.setSprinting(false); mode(GameType.SURVIVAL);
            worldTouched = true; clearWorld(); aim(); craftAndSelect(); prices(); next();
        }

        private void craftAndSelect() {
            level.setBlock(table, ThaumcraftArcane.ARCANE_WORKBENCH.get().defaultBlockState(), QUIET);
            var tile = (TileArcaneWorkbench) level.getBlockEntity(table);
            var recipe = (ArcaneShapedRecipe) level.getRecipeManager().byKey(id("arcane/focus_frost")).orElseThrow().value();
            test.assertTrue(recipe.research().equals("FOCUSFROST") && recipe.pattern().equals(List.of("CQC", "Q#Q", "CQC"))
                    && recipe.vis().equals(Map.of("aqua", 10, "ordo", 10, "perditio", 10)), "original Frost gate, literal shape and10/10/10 WHOLE vis");
            for (var ingredient : Map.of("C", ThaumcraftContent.SHARDS.get("aqua").get(), "Q", Items.QUARTZ, "#", Items.DIAMOND).entrySet()) {
                var entries = recipe.key().get(ingredient.getKey()).getItems();
                test.assertTrue(entries.length == 1 && entries[0].is(ingredient.getValue()), "exact original Frost ingredient: " + ingredient.getKey());
            }
            for (int slot = 0; slot < 9; slot++) tile.setItem(slot, new ItemStack(slot == 4 ? Items.DIAMOND
                    : slot % 2 == 0 ? ThaumcraftContent.SHARDS.get("aqua").get() : Items.QUARTZ));
            test.assertTrue(recipe.matches(ArcaneRecipeInput.snapshot(tile), level), "four water shards/four quartz/one real diamond match");
            tile.setItem(4, new ItemStack(Items.EMERALD));
            test.assertTrue(!recipe.matches(ArcaneRecipeInput.snapshot(tile), level), "Excavation's emerald is not a Frost diamond substitute");
            tile.setItem(4, new ItemStack(Items.DIAMOND));
            ItemStack fire = named(new ItemStack(ThaumcraftFoci.FIRE_FOCUS.get()), "old Fire");
            wand = charged("gold", 2000); wand.set(ThaumcraftFoci.WAND_FOCUS.get(), new FocusData(fire)); tile.setItem(10, wand);
            KnowledgeAccess.get(player).removeResearch("FOCUSFROST");
            player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(table), Direction.UP, table, false));
            test.assertTrue(player.containerMenu instanceof ContainerArcaneWorkbench, "real block use opens the native arcane menu");
            var menu = (ContainerArcaneWorkbench) player.containerMenu;
            refuseCraft(tile, menu, "absent research cannot debit ingredients, vis or create an output");
            KnowledgeAccess.get(player).addResearch("FOCUSFROST"); menu.broadcastChanges();
            test.assertTrue(menu.craftable() && menu.requiredVis("aqua") == 1000 && menu.requiredVis("ordo") == 1000 && menu.requiredVis("perditio") == 1000,
                    "bare gold prices the three original10vis at1000 each");
            tile.getItem(10).set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(tile.getItem(10)).withAmount("perditio", 999));
            refuseCraft(tile, menu, "one-short third primal cannot partially debit either earlier primal");
            // Cap/material and charge are explicit finite server INPUTS, not an earned cap/recharge story.
            tile.getItem(10).set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(charged("thaumium", 2000))); gear(true); menu.broadcastChanges();
            test.assertTrue(menu.craftable() && menu.requiredVis("aqua") == 799 && menu.requiredVis("ordo") == 799 && menu.requiredVis("perditio") == 799,
                    "actual worn robes/goggles and Thaumium FLOAT modifier price10 whole vis at799, not800");
            tile.getItem(10).set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(tile.getItem(10)).withAmount("perditio", 798));
            refuseCraft(tile, menu, "discounted one-short third primal is atomic too");
            tile.getItem(10).set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(tile.getItem(10)).withAmount("perditio", 2000)); menu.broadcastChanges();
            menu.clicked(0, 0, ClickType.PICKUP, player); earned = menu.getCarried(); menu.setCarried(ItemStack.EMPTY);
            test.assertTrue(earned.is(ThaumcraftFoci.FROST_FOCUS.get()) && earned.getCount() == 1, "ONE Frost focus is really crafted, not supplied");
            for (int slot = 0; slot < 9; slot++) test.assertTrue(tile.getItem(slot).isEmpty(), "native craft consumes each raw input once");
            wand = tile.removeItemNoUpdate(10);
            for (String tag : WandData.PRIMALS) test.assertTrue(ItemWandCasting.getVis(wand, tag) == (List.of("aqua", "ordo", "perditio").contains(tag) ? 1201 : 2000), "actual discounted craft debit: " + tag);
            test.assertTrue(ItemStack.matches(FocusData.get(wand), fire), "arcane crafting retains the exact old Fire component patch");
            player.closeContainer(); player.setItemInHand(InteractionHand.MAIN_HAND, wand); earned = named(earned, "earned Frost");
            ItemStack excavation = named(new ItemStack(ThaumcraftFoci.EXCAVATION_FOCUS.get()), "old Excavation");
            ItemStack shock = named(new ItemStack(ThaumcraftFoci.SHOCK_FOCUS.get()), "old Shock");
            foci.addAll(List.of(fire.copy(), excavation.copy(), earned.copy(), shock.copy()));
            player.getInventory().setItem(5, earned); player.getInventory().setItem(6, excavation); player.getInventory().setItem(7, shock);
            var data = ItemWandCasting.data(wand);
            for (ItemStack desired : List.of(earned, excavation, shock, fire, earned)) {
                select(desired);
                test.assertTrue(ItemStack.matches(FocusData.get(wand), desired) && ItemWandCasting.data(wand).equals(data), "actual selection moves each of four component-bearing kinds without changing wand data");
                retained();
            }
            var focus = ThaumcraftFoci.FROST_FOCUS.get(); var costs = focus.getVisCost(earned);
            test.assertTrue(focus.getFocusColor(earned) == 5204428 && focus.getSortingHelper(earned).equals("BF-1-1-1-1-1") && focus.getActivationCooldown(earned) == 200
                    && !focus.isVisCostPerTick(earned) && focus.getAnimation(earned) == ItemFocusBasic.WandFocusAnimation.WAVE
                    && costs.size() == 3 && costs.getAmount(Aspect.WATER) == 5 && costs.getAmount(Aspect.FIRE) == 2 && costs.getAmount(Aspect.ENTROPY) == 2,
                    "original BASIC Frost API: BF/color5204428/200ms/inherited WAVE/per-activation5/2/2 CENTIVIS");
            test.assertTrue(foci.stream().map(stack -> ((ItemFocusBasic) stack.getItem()).getSortingHelper(stack)).toList().equals(List.of("AF-1-1-1-1-1", "BE-1-1-1-1-1", "BF-1-1-1-1-1", "BL-1-1-1-1-1")), "all four original sorting keys include the five empty rank slots");
            codecs(level.registryAccess()); KnowledgeAccess.get(player).removeResearch("FOCUSFROST");
        }

        private void refuseCraft(TileArcaneWorkbench tile, ContainerArcaneWorkbench menu, String reason) {
            menu.broadcastChanges(); var before = tile.saveWithoutMetadata(level.registryAccess());
            menu.clicked(0, 0, ClickType.PICKUP, player);
            test.assertTrue(menu.getCarried().isEmpty() && before.equals(tile.saveWithoutMetadata(level.registryAccess())), reason);
        }

        private void codecs(RegistryAccess registries) {
            for (ItemStack kind : foci) {
                ItemStack copy = wand.copy(); copy.set(ThaumcraftFoci.WAND_FOCUS.get(), new FocusData(kind));
                test.assertTrue(FocusData.isSupportedFocus(kind) && ItemStack.matches(copy, ItemStack.parse(registries, copy.save(registries)).orElseThrow()), "persistent native codec retains each of four actual focus classes and full components");
                var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
                try { ItemStack.STREAM_CODEC.encode(buffer, copy); test.assertTrue(ItemStack.matches(copy, ItemStack.STREAM_CODEC.decode(buffer)), "stream codec preserves each full equipped focus stack"); }
                finally { buffer.release(); }
            }
            ItemStack nested = earned.copy(); nested.set(ThaumcraftFoci.WAND_FOCUS.get(), new FocusData(foci.getFirst()));
            ItemStack material = earned.copy(); material.set(ThaumcraftContent.WAND_DATA.get(), WandData.EMPTY);
            test.assertTrue(!FocusData.isSupportedFocus(earned.copyWithCount(2)) && !FocusData.isSupportedFocus(ItemStack.EMPTY), "stacked/empty foci remain unsupported");
            var ops = RegistryOps.create(NbtOps.INSTANCE, registries);
            for (ItemStack invalid : List.of(nested, material, new ItemStack(Items.QUARTZ))) {
                test.assertTrue(!FocusData.isSupportedFocus(invalid) && FocusData.CODEC.parse(ops, ItemStack.STRICT_CODEC.encodeStart(ops, invalid).getOrThrow()).error().isPresent(), "nested, wand-data-bearing and ordinary registered items are rejected by persistent focus decoding");
                var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries); boolean rejected = false;
                try { ItemStack.STREAM_CODEC.encode(buffer, invalid); try { FocusData.STREAM_CODEC.decode(buffer); } catch (DecoderException expected) { rejected = true; } }
                finally { buffer.release(); }
                test.assertTrue(rejected, "native stream focus decoder rejects the same nested/material/non-focus boundary");
            }
        }

        private void prices() {
            gear(false);
            test.assertTrue(ItemFocusFrost.activationCost(charged("gold", 30), player).equals(Map.of("aqua", 5, "ignis", 2, "perditio", 2)), "bare gold basic prices5/2/2 centivis");
            test.assertTrue(ItemFocusFrost.activationCost(wand, player).equals(Map.of("aqua", 4, "ignis", 1, "perditio", 1)), "bare Thaumium truncates each centivis price separately");
            gear(true);
            for (Aspect aspect : List.of(Aspect.WATER, Aspect.FIRE, Aspect.ENTROPY)) test.assertTrue(WandVisDiscount.percentagePoints(player, aspect) == 10
                    && WandVisDiscount.modifier(wand, player, aspect) == .79999995F, "native worn gear gives the original FLOAT modifier, not rounded decimal arithmetic");
            test.assertTrue(ItemFocusFrost.activationCost(wand, player).equals(Map.of("aqua", 3, "ignis", 1, "perditio", 1)), "full robe/goggle Thaumium BASIC costs3/1/1, NOT4/2/2");
        }

        private void next() {
            switch (phase++) {
                case 0 -> activate(true, false, false); // Actual crafted wand, actual full native gear,3/1/1.
                case 1 -> {
                    gear(false); wand.set(ThaumcraftContent.WAND_DATA.get(), new WandData("silverwood", "gold", ItemWandCasting.data(wand).vis()));
                    activate(true, false, true); globalCooldown();
                }
                case 2 -> activate(true, true, true); // Real native ADD cancellation, including reentrant RMB.
                case 3, 4, 5 -> {
                    String shortTag = List.of("aqua", "ignis", "perditio").get(phase - 4);
                    fixtureCharge(30); int required = ItemFocusFrost.activationCost(wand, player).get(shortTag);
                    wand.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(wand).withAmount(shortTag, required - 1));
                    activate(false, false, false);
                }
                case 6 -> { fixtureCharge(30); mode(GameType.CREATIVE); activate(true, false, false); retained(); mode(GameType.SURVIVAL); }
                case 7 -> {
                    fixtureCharge(30); var data = ItemWandCasting.data(wand);
                    for (var entry : ItemFocusFrost.activationCost(wand, player).entrySet()) data = data.withAmount(entry.getKey(), entry.getValue());
                    wand.set(ThaumcraftContent.WAND_DATA.get(), data); activate(true, false, false);
                    test.assertTrue(List.of("aqua", "ignis", "perditio").stream().allMatch(tag -> ItemWandCasting.getVis(wand, tag) == 0), "last affordable activation spends all three exact amounts once");
                }
                case 8 -> {
                    activate(false, false, false); retained();
                    test.assertTrue(!KnowledgeAccess.get(player).isResearchComplete("FOCUSFROST"), "all actual found/equipped focus use stayed legal without crafting research");
                    close(); test.succeed(); return;
                }
                default -> throw new IllegalStateException("Unknown Frost check phase");
            }
            test.runAfterDelay(1, () -> guard(this::awaitCooldown));
        }

        private void awaitCooldown() {
            if (WandFocusActions.cooldownMillis(player) > 0) test.runAfterDelay(1, () -> guard(this::awaitCooldown));
            else {
                test.assertTrue(System.currentTimeMillis() - lastActivation >= ItemFocusFrost.COOLDOWN_MILLIS, "next eligible activation waited >=200 REAL milliseconds, not nominal native ticks");
                next();
            }
        }

        private void activate(boolean funded, boolean cancel, boolean reenter) {
            aim(); test.assertTrue(WandFocusActions.cooldownMillis(player) == 0 && !player.isUsingItem(), "ordinary Frost begins with no held channel and an actually expired cooldown");
            var before = new ArrayList<ItemStack>(); for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) before.add(player.getInventory().getItem(slot).copy());
            ItemStack expected = wand.copy(); var costs = ItemFocusFrost.activationCost(wand, player);
            test.assertTrue(ArcaneVis.debit(expected, costs) == funded, "fixture affordability agrees with all three atomic costs");
            int[] attempts = {0}; boolean[] paidFirst = {true}, nestedRefused = {!reenter}; long[] remaining = {0};
            EntityEvent.Add observer = (entity, world) -> {
                if (world != level || !(entity instanceof EntityFrostShard shard) || shard.getOwner() != player) return EventResult.pass();
                attempts[0]++; shards.add(shard);
                paidFirst[0] &= ItemStack.matches(wand, expected) && shard.getType() == ThaumcraftFrost.FROST_SHARD.get() && shard.getDamage() == 3F;
                if (reenter && attempts[0] == 1) {
                    remaining[0] = WandFocusActions.cooldownMillis(player);
                    nestedRefused[0] = !player.gameMode.useItem(player, level, wand, InteractionHand.MAIN_HAND).consumesAction();
                }
                return cancel ? EventResult.interruptFalse() : EventResult.pass();
            };
            iceSounds(); EntityEvent.ADD.register(observer);
            try {
                lastActivation = System.currentTimeMillis();
                test.assertTrue(player.gameMode.useItem(player, level, wand, InteractionHand.MAIN_HAND).consumesAction() && !player.isUsingItem(), "native RMB invokes inherited WAVE once, not CHARGE, even when unfunded/unresearched");
                player.setDeltaMovement(Vec3.ZERO); player.doTick();
            } finally { EntityEvent.ADD.unregister(observer); }
            test.assertTrue(!EntityEvent.ADD.isRegistered(observer) && attempts[0] == (funded ? 1 : 0) && paidFirst[0], "native ADD sees exactly ONE registered owner-attributed basic damage3 shard, ALREADY PAID in every primal");
            test.assertTrue(!reenter || remaining[0] > 0 && nestedRefused[0], "immediate native reentrant RMB is refused by the real200ms cooldown; no second payment/spawn");
            test.assertTrue(player.getMainHandItem() == wand && ItemStack.matches(wand, expected), "actual held wand pays once, with no partial debit/refund, retained focus/components/materials and other primals");
            for (int slot = 1; slot < before.size(); slot++) test.assertTrue(ItemStack.matches(player.getInventory().getItem(slot), before.get(slot)), "native activation retains every other inventory/gear stack: " + slot);
            var indexed = level.getEntitiesOfClass(EntityFrostShard.class, bounds, shard -> shard.getOwner() == player && !shard.isRemoved());
            test.assertTrue(indexed.size() == (funded && !cancel ? 1 : 0), "native entity index has exactly the accepted projectile, or none after genuine ADD refusal/unfunded activation");
            if (cancel) test.assertTrue(level.getEntity(shards.getLast().getUUID()) == null, "cancelled projectile UUID never entered the world; no deferred item/output reservation or retry path is involved");
            int soundCount = iceSounds();
            test.assertTrue(soundCount == (funded ? 1 : 0), "exactly one native ice sound for every PAID attempt INCLUDING cancelled ADD; none when unfunded (not an audio/visual claim); phase="
                    + phase + "; funded=" + funded + "; canceledADD=" + cancel + "; nativeCount=" + soundCount + "; outbound=" + lastSoundPackets);
            shards.forEach(Entity::discard); retained();
        }

        private void globalCooldown() {
            ItemStack shock = foci.getLast(); var before = ItemWandCasting.data(wand); select(shock);
            test.assertTrue(WandFocusActions.cooldownMillis(player) > 0 && !player.gameMode.useItem(player, level, wand, InteractionHand.MAIN_HAND).consumesAction()
                    && !player.isUsingItem() && ItemWandCasting.data(wand).equals(before), "switching to a DIFFERENT actual focus cannot reset the player's still-live Frost cooldown or pay/start Shock");
            select(earned); retained();
        }

        private int iceSounds() {
            // Native server tick suspends packet flushing for existing players.
            // Later GameTest phases execute INSIDE that tick; the first phase's
            // newly connected actor had not been suspended yet. Flush ONLY this
            // owned embedded transport's real pending packets before observing,
            // never call playSound/inject a packet or change the server clock.
            connection.flushChannel(); channel.runPendingTasks(); int count = 0; Object packet; lastSoundPackets.clear();
            while ((packet = channel.readOutbound()) != null) {
                try {
                    if (lastSoundPackets.size() < 24) lastSoundPackets.add(packet instanceof ClientboundSoundPacket sound
                            ? sound.getClass().getSimpleName() + ":" + sound.getSound().value().getLocation() + "@" + new Vec3(sound.getX(), sound.getY(), sound.getZ())
                                    + "; listener=" + player.position()
                            : packet.getClass().getSimpleName());
                    if (packet instanceof ClientboundSoundPacket sound && sound.getSound().value().getLocation().equals(id("ice"))
                            && new Vec3(sound.getX(), sound.getY(), sound.getZ()).distanceToSqr(player.position()) < 9) {
                        test.assertTrue(sound.getSource() == SoundSource.PLAYERS && sound.getVolume() == .4F && sound.getPitch() >= 1F && sound.getPitch() <= 1.1F, "native original ice volume/pitch range without RNG control"); count++;
                    }
                } finally { ReferenceCountUtil.release(packet); }
            }
            return count;
        }

        private void select(ItemStack desired) {
            int source = -1;
            for (int slot = 0; slot < 36; slot++) if (ItemStack.matches(player.getInventory().getItem(slot), desired)) { source = slot; break; }
            test.assertTrue(source >= 0 && FocusSelectionNetworking.handle(player, new FocusSelectionPayload(InteractionHand.MAIN_HAND, source)), "production selection receiver moves a REAL inventory focus, not a client-supplied stack");
        }
        private void retained() {
            var actual = new ArrayList<ItemStack>(); actual.add(FocusData.get(wand));
            for (int slot = 0; slot < 36; slot++) if (player.getInventory().getItem(slot).getItem() instanceof ItemFocusBasic) actual.add(player.getInventory().getItem(slot));
            test.assertTrue(actual.size() == 4 && foci.stream().allMatch(expected -> actual.stream().filter(stack -> ItemStack.matches(stack, expected)).count() == 1), "exactly the four component-bearing real foci survive; no creative copy, loss or substitution");
        }
        private void fixtureCharge(int amount) { wand.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(charged("gold", amount))); }
        private void mode(GameType mode) { player.setGameMode(mode); test.assertTrue(player.isCreative() == (mode == GameType.CREATIVE), "unique REAL actor's native survival/creative state is actually " + mode); }
        private void aim() { player.setPos(origin); player.setDeltaMovement(Vec3.ZERO); player.setYRot(0); player.setXRot(-90); }
        private void gear(boolean wear) {
            player.setItemSlot(EquipmentSlot.HEAD, wear ? new ItemStack(ThaumcraftWearables.GOGGLES.get()) : ItemStack.EMPTY);
            player.setItemSlot(EquipmentSlot.CHEST, wear ? new ItemStack(ThaumcraftWearables.ROBE_CHESTPLATE.get()) : ItemStack.EMPTY);
            player.setItemSlot(EquipmentSlot.LEGS, wear ? new ItemStack(ThaumcraftWearables.ROBE_LEGGINGS.get()) : ItemStack.EMPTY);
            player.setItemSlot(EquipmentSlot.FEET, wear ? new ItemStack(ThaumcraftWearables.ROBE_BOOTS.get()) : ItemStack.EMPTY);
        }
        private void clearWorld() { blocks.keySet().forEach(pos -> { level.removeBlockEntity(pos); if (!level.isEmptyBlock(pos)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), QUIET); }); }

        @Override public void close() {
            if (closed) return; closed = true;
            try {
                player.stopUsingItem(); WandFocusActions.clearCooldown(player);
                if (player.containerMenu != player.inventoryMenu) player.containerMenu.setCarried(ItemStack.EMPTY);
                player.closeContainer(); player.getInventory().clearContent(); player.removeAllEffects();
                if (knowledge != null) {
                    KnowledgeAccess.get(player).copyFrom(knowledge); player.getStats().setValue(player, Stats.ITEM_CRAFTED.get(ThaumcraftFoci.FROST_FOCUS.get()), crafted);
                    var holder = level.getRecipeManager().byKey(id("arcane/focus_frost")).orElseThrow(); if (!learned) player.resetRecipes(List.of(holder));
                }
            } finally {
                try {
                    shards.forEach(Entity::discard);
                    if (worldTouched) {
                        clearWorld(); blocks.forEach((pos, state) -> level.setBlock(pos, state, QUIET));
                        tiles.forEach((pos, tag) -> { if (level.getBlockEntity(pos) != null) level.getBlockEntity(pos).loadWithComponents(tag, level.registryAccess()); });
                    }
                } finally {
                    try { if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player); }
                    finally { player.discard(); if (channel != null) channel.finishAndReleaseAll(); }
                }
            }
        }
    }

    private static ItemStack charged(String cap, int amount) {
        ItemStack stack = new ItemStack(ThaumcraftContent.WAND.get()); var vis = new LinkedHashMap<String, Integer>();
        for (String tag : WandData.PRIMALS) vis.put(tag, amount);
        stack.set(ThaumcraftContent.WAND_DATA.get(), new WandData("silverwood", cap, vis)); return stack;
    }
    private static ItemStack named(ItemStack stack, String name) {
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name)); var data = new CompoundTag(); data.putString("preserve", name);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data)); return stack;
    }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }
}
