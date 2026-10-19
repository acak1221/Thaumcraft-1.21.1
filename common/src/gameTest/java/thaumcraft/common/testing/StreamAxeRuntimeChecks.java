package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.IRepairable;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.config.ThaumcraftNaturals;
import thaumcraft.common.crafting.InfusionRecipeInput;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.lib.world.StreamAxeItemMotion;

/**
 * Axe of the Stream through the transformed native game mode, with a unique real
 * connected player. Trunks, the axe, loose items and the protection listener are
 * explicit INPUTS; chopping, loot, wear and following are never supplied. Observes the
 * actual BREAK dispatch, not a manual event. Not a client/prediction, multiplayer or
 * third-party protection test.
 */
public final class StreamAxeRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private StreamAxeRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        try (var check = new Check(test)) {
            check.start();
            check.recipeAndItem();
            check.claimedChopBreaksOnlyTheEndpointAndDropsFollow();
            check.everyRecognisedLogFamily();
            check.sneakingAndOrdinaryBlocksStayOrdinary();
            check.protectionRefusalNeverFallsBack();
            check.creativeChopIsFree();
            check.heldVacuum();
        }
        test.succeed();
    }

    private static final class Check implements AutoCloseable {
        private final GameTestHelper test;
        private final ServerLevel level;
        private final ServerPlayer player;
        private final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        private final EmbeddedChannel channel = new EmbeddedChannel(connection);
        private final BlockPos base;
        private final AABB lane;
        private final Map<BlockPos, BlockState> original = new LinkedHashMap<>();
        private final Set<UUID> existing = new HashSet<>();
        private final List<BlockPos> breakEvents = new ArrayList<>();
        private final BlockEvent.Break listener;
        private BlockPos cancelAt;
        private boolean registered, touched;
        private ItemStack axe;

        private Check(GameTestHelper test) {
            this.test = test;
            level = test.getLevel();
            base = test.absolutePos(new BlockPos(3, 3, 3));
            lane = new AABB(Vec3.atLowerCornerOf(base.offset(-3, -2, -3)), Vec3.atLowerCornerOf(base.offset(14, 9, 4)));
            for (BlockPos mutable : BlockPos.betweenClosed(base.offset(-3, -2, -3), base.offset(13, 8, 3))) {
                original.put(mutable.immutable(), level.getBlockState(mutable));
            }
            level.getAllEntities().forEach(entity -> existing.add(entity.getUUID()));
            var profile = new GameProfile(UUID.randomUUID(), "axe-" + UUID.randomUUID().toString().substring(0, 8));
            player = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
            listener = (world, pos, state, actor, xp) -> {
                if (world != level || actor != player) return EventResult.pass();
                breakEvents.add(pos.immutable());
                return cancelAt != null && cancelAt.equals(pos) ? EventResult.interruptFalse() : EventResult.pass();
            };
        }

        private void start() {
            test.assertTrue(level.getEntities((Entity) null, lane, entity -> entity instanceof ItemEntity).isEmpty(),
                    "owned lane has no pre-existing items to merge or follow");
            level.getServer().getPlayerList().placeNewPlayer(connection, player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false));
            test.assertTrue(player.level() == level && level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)
                    && level.getServer().getPlayerList().getPlayer(player.getUUID()) == player,
                    "actual connected player and native block-drops rule are explicit inputs");
            player.setNoGravity(true);
            BlockEvent.BREAK.register(listener);
            registered = true;
            touched = true;
        }

        /** Fresh survival lane, fresh pristine axe in the main hand, player three blocks east of the trunk. */
        private void prepare(GameType mode) {
            clearLane();
            breakEvents.clear();
            cancelAt = null;
            player.setShiftKeyDown(false);
            player.setGameMode(mode);
            axe = new ItemStack(ThaumcraftEquipment.ELEMENTAL_AXE.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, axe);
            player.setPos(Vec3.atBottomCenterOf(base).add(3, 0, 0));
            level.setBlock(base.below(), Blocks.STONE.defaultBlockState(), QUIET);
        }

        private void trunk(Block log, int height) {
            for (int index = 0; index < height; index++) level.setBlock(base.above(index), log.defaultBlockState(), QUIET);
        }

        private List<ItemEntity> items() {
            return level.getEntitiesOfClass(ItemEntity.class, lane, entity -> !existing.contains(entity.getUUID()));
        }

        private void clearLane() {
            items().forEach(Entity::discard);
            original.forEach((pos, state) -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), QUIET));
        }

        private boolean following(ItemEntity item) {
            return item instanceof StreamAxeItemMotion.Following f && f.thaumcraft$streamFollowing();
        }

        private void recipeAndItem() {
            var holder = level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "infusion/elemental_axe"));
            test.assertTrue(holder.isPresent() && holder.get().value() instanceof InfusionStackRecipe, "elemental axe infusion recipe loaded");
            var recipe = (InfusionStackRecipe) holder.get().value();
            test.assertTrue(recipe.research().equals("ELEMENTALAXE") && recipe.instability() == 1
                            && recipe.aspects().equals(Map.of("aqua", 16, "arbor", 8))
                            && recipe.inputDamage().equals(Optional.of(0))
                            && recipe.result().is(ThaumcraftEquipment.ELEMENTAL_AXE.get()),
                    "researched infusion: instability 1, aqua16/arbor8, pristine centre, genuine output");
            var shard = new ItemStack(ThaumcraftContent.SHARDS.get("aqua").get());
            var components = List.of(shard.copy(), shard.copy(), new ItemStack(Items.DIAMOND),
                    new ItemStack(ThaumcraftNaturals.GREATWOOD_LOG.get()));
            var thaumium = new ItemStack(ThaumcraftEquipment.THAUMIUM_AXE.get());
            var worn = thaumium.copy(); worn.setDamageValue(1);
            test.assertTrue(recipe.matches(new InfusionRecipeInput(thaumium, components), level),
                    "pristine thaumium axe + two water shards + diamond + Greatwood log is accepted");
            test.assertTrue(!recipe.matches(new InfusionRecipeInput(worn, components), level),
                    "a worn central thaumium axe is refused at admission");

            var stack = new ItemStack(ThaumcraftEquipment.ELEMENTAL_AXE.get());
            test.assertTrue(stack.getMaxDamage() == 1500 && stack.getRarity() == Rarity.RARE
                            && stack.getItem() instanceof TieredItem tiered && tiered.getTier().getEnchantmentValue() == 18
                            && tiered.getTier().getRepairIngredient().test(new ItemStack(ThaumcraftContent.MATERIALS.get("thaumium_ingot").get()))
                            && stack.getDestroySpeed(Blocks.OAK_LOG.defaultBlockState()) == 10.0F
                            && stack.getItem() instanceof IRepairable
                            && stack.is(net.minecraft.tags.ItemTags.AXES),
                    "tier: 1500 uses, speed 10, enchantability 18, rare, repairable with a thaumium ingot");
        }

        private void claimedChopBreaksOnlyTheEndpointAndDropsFollow() {
            prepare(GameType.SURVIVAL);
            test.assertTrue(level.mayInteract(player, base), "fixture is not under unrelated spawn protection");
            trunk(Blocks.OAK_LOG, 5);
            BlockPos anchor = base.above(1), endpoint = base.above(4);
            var old = new ItemEntity(level, endpoint.getX() + .5, endpoint.getY() + .5, endpoint.getZ() + .5, new ItemStack(Items.STICK), 0, 0, 0);
            level.addFreshEntity(old);
            boolean returned = player.gameMode.destroyBlock(anchor);
            test.assertTrue(!returned, "claimed chop returns false so vanilla resynchronises the predicted break");
            test.assertTrue(level.getBlockState(anchor).is(Blocks.OAK_LOG) && level.getBlockState(base).is(Blocks.OAK_LOG)
                            && level.getBlockState(base.above(2)).is(Blocks.OAK_LOG) && level.getBlockState(base.above(3)).is(Blocks.OAK_LOG)
                            && level.getBlockState(endpoint).isAir(),
                    "only the furthest same-block log is removed; the clicked anchor is intact");
            test.assertTrue(breakEvents.equals(List.of(endpoint)),
                    "genuine BREAK dispatched exactly once, for the endpoint only: " + breakEvents);
            test.assertTrue(player.getMainHandItem() == axe && axe.getDamageValue() == 1, "wear is exactly one, once");
            var logs = items().stream().filter(item -> item.getItem().is(Items.OAK_LOG)).toList();
            test.assertTrue(logs.size() == 1 && logs.get(0).getItem().getCount() == 1, "real native loot, one oak log");
            test.assertTrue(following(logs.get(0)), "the freshly dropped log follows the player");
            test.assertTrue(!following(old), "an older nearby item is never captured");
            test.assertTrue(StreamAxeItemMotion.followNewDrops(player, endpoint, Set.of(old.getId())) == 0,
                    "an item that already follows is never stolen or restarted");

            player.setPos(Vec3.atBottomCenterOf(base).add(9, 0, 0));
            ItemEntity log = logs.get(0);
            Vec3 centre = player.position().add(0, player.getBbHeight() / 2.0, 0);
            double before = log.position().distanceTo(centre);
            for (int tick = 0; tick < 4; tick++) log.tick();
            double after = log.position().distanceTo(centre);
            test.assertTrue(after < before - 0.15 && log.isNoGravity() && log.isAlive(),
                    "followed drop flies toward the player (" + before + " -> " + after + ")");
        }

        private void everyRecognisedLogFamily() {
            for (Block log : List.of(Blocks.SPRUCE_LOG, Blocks.BIRCH_LOG, Blocks.JUNGLE_LOG, Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG,
                    ThaumcraftNaturals.GREATWOOD_LOG.get(), ThaumcraftNaturals.SILVERWOOD_LOG.get())) {
                prepare(GameType.SURVIVAL);
                trunk(log, 4);
                boolean returned = player.gameMode.destroyBlock(base);
                test.assertTrue(!returned && level.getBlockState(base).is(log) && level.getBlockState(base.above(3)).isAir()
                                && level.getBlockState(base.above(2)).is(log) && breakEvents.equals(List.of(base.above(3))),
                        "bottom log hit chops the top of a " + log + " trunk");
            }
            prepare(GameType.SURVIVAL);
            trunk(Blocks.OAK_LOG, 3);
            level.setBlock(base.above(3), Blocks.STRIPPED_OAK_LOG.defaultBlockState(), QUIET);
            boolean returned = player.gameMode.destroyBlock(base.above(1));
            test.assertTrue(!returned && level.getBlockState(base.above(2)).isAir() && level.getBlockState(base.above(3)).is(Blocks.STRIPPED_OAK_LOG),
                    "a different Block (stripped log) is not a link of the walk");
        }

        private void sneakingAndOrdinaryBlocksStayOrdinary() {
            prepare(GameType.SURVIVAL);
            trunk(Blocks.OAK_LOG, 5);
            player.setShiftKeyDown(true);
            boolean returned = player.gameMode.destroyBlock(base.above(1));
            test.assertTrue(returned && level.getBlockState(base.above(1)).isAir() && level.getBlockState(base.above(4)).is(Blocks.OAK_LOG)
                            && axe.getDamageValue() == 1 && breakEvents.equals(List.of(base.above(1))),
                    "Shift: the clicked log breaks like an ordinary axe, nothing is redirected");
            prepare(GameType.SURVIVAL);
            level.setBlock(base, Blocks.STONE.defaultBlockState(), QUIET);
            test.assertTrue(player.gameMode.destroyBlock(base) && level.getBlockState(base).isAir() && breakEvents.equals(List.of(base)),
                    "a non-log block is an ordinary break");
            prepare(GameType.SURVIVAL);
            trunk(Blocks.OAK_LOG, 3);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
            test.assertTrue(player.gameMode.destroyBlock(base.above(1)) && level.getBlockState(base.above(1)).isAir()
                            && level.getBlockState(base.above(2)).is(Blocks.OAK_LOG),
                    "any other axe is untouched by the redirect");
        }

        private void protectionRefusalNeverFallsBack() {
            prepare(GameType.SURVIVAL);
            trunk(Blocks.OAK_LOG, 5);
            cancelAt = base.above(4);
            boolean returned = player.gameMode.destroyBlock(base.above(1));
            boolean intact = true;
            for (int index = 0; index < 5; index++) intact &= level.getBlockState(base.above(index)).is(Blocks.OAK_LOG);
            test.assertTrue(!returned && intact && axe.getDamageValue() == 0 && items().isEmpty()
                            && breakEvents.equals(List.of(base.above(4))),
                    "a protection BREAK cancel at the endpoint leaves the whole trunk, wear and loot untouched; no fallback break");
        }

        private void creativeChopIsFree() {
            prepare(GameType.CREATIVE);
            trunk(Blocks.OAK_LOG, 5);
            boolean returned = player.gameMode.destroyBlock(base.above(1));
            test.assertTrue(!returned && level.getBlockState(base.above(1)).is(Blocks.OAK_LOG) && level.getBlockState(base.above(4)).isAir()
                            && axe.getDamageValue() == 0 && items().isEmpty(),
                    "creative: endpoint removed, anchor intact, no wear, no loot");
        }

        private void heldVacuum() {
            prepare(GameType.SURVIVAL);
            player.setPos(Vec3.atBottomCenterOf(base));
            var near = new ItemEntity(level, base.getX() + 5.5, base.getY(), base.getZ() + .5, new ItemStack(Items.STICK), 0, 0, 0);
            var far = new ItemEntity(level, base.getX() + 12.5, base.getY(), base.getZ() + .5, new ItemStack(Items.STICK), 0, 0, 0);
            level.addFreshEntity(near);
            level.addFreshEntity(far);
            StreamAxeItemMotion.vacuum(player);
            test.assertTrue(near.getDeltaMovement().x < -0.05 && near.getDeltaMovement().x >= -0.35
                            && far.getDeltaMovement().equals(Vec3.ZERO),
                    "held use pulls items within 10 blocks toward the player, never beyond: " + near.getDeltaMovement());
            for (int repeat = 0; repeat < 6; repeat++) StreamAxeItemMotion.vacuum(player);
            test.assertTrue(near.getDeltaMovement().x >= -0.35 - 1.0E-9 && axe.getDamageValue() == 0,
                    "velocity components stay clamped to 0.35 and the vacuum costs no wear");
        }

        @Override public void close() {
            if (registered) BlockEvent.BREAK.unregister(listener);
            try {
                if (touched) {
                    clearLane();
                    original.forEach((pos, state) -> level.setBlock(pos, state, QUIET));
                }
            } finally {
                try {
                    if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player);
                } finally { player.discard(); channel.finishAndReleaseAll(); }
            }
        }
    }
}
