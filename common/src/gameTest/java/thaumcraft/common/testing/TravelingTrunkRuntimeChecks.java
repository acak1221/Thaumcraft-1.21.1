package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.container.ContainerTravelingTrunk;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.entities.golems.EntityTravelingTrunk;
import thaumcraft.common.items.golems.GolemUpgrade;
import thaumcraft.common.items.golems.TrunkRules;

/**
 * The Traveling Trunk on real content. A real connected owner, a second real player, real golem upgrade items, food, the bell, the
 * spawner item and the real menu are explicit INPUTS; what the trunk does with them (owner, rows, upgrade, open/stay, recall drops,
 * damage immunities, healing, hopping after the owner, the teleport when far, the Fire defence and Entropy's pull) is decided by the
 * trunk. {@link #basics} is synchronous; {@link #behavior} runs the walking phases. NOT a client/GUI check: the lid, the model and
 * the screen are unseen, and following the owner through a dimension change is not exercised.
 */
public final class TravelingTrunkRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE, DEADLINE = 1500, LAYER = 40;

    private TravelingTrunkRuntimeChecks() {}

    public static void basics(GameTestHelper test) {
        var check = new Check(test);
        try {
            check.start();
            check.recipe();
            check.spawnerPlacesTheTrunkForItsUser();
            check.everyUpgradeIsFittedOnce();
            check.menuOpensAndTogglesStay();
            check.waterBelongsToItsOwner();
            check.foodAndHealing();
            check.damageRules();
            check.bellRecall();
            check.persistence();
        } finally {
            check.finish();
        }
        test.succeed();
    }

    public static void behavior(GameTestHelper test) {
        var check = new Check(test);
        try {
            check.start();
            check.armPhases();
        } catch (RuntimeException failure) {
            check.finish();
            throw failure;
        }
        test.onEachTick(() -> {
            if (check.finished) return;
            try {
                if (check.poll()) { check.finish(); test.succeed(); }
            } catch (RuntimeException failure) {
                Thaumcraft.LOG.error("Trunk phases failed at tick {}: {} ({})", test.getTick(), failure.getMessage(), check.describe());
                check.finish();
                throw failure;
            }
        });
        test.runAtTickTime(DEADLINE, () -> {
            if (check.finished) return;
            String state = check.describe();
            check.finish();
            test.fail("the trunk phases did not finish within " + DEADLINE + " ticks: " + state);
        });
    }

    private interface Step { boolean done(); }

    private record Phase(String name, Runnable start, Step done) {}

    private static final class Check {
        private final GameTestHelper test;
        private final ServerLevel level;
        private final BlockPos base;
        private final Map<BlockPos, BlockState> original = new LinkedHashMap<>();
        private final List<Entity> spawned = new ArrayList<>();
        private final List<ChunkPos> forced = new ArrayList<>();
        private final List<Connection> connections = new ArrayList<>();
        private final List<EmbeddedChannel> channels = new ArrayList<>();
        private final List<ServerPlayer> players = new ArrayList<>();
        private final List<Phase> phases = new ArrayList<>();
        private ServerPlayer owner, stranger;
        private boolean touched, finished;
        private int phase = -1;
        private long phaseStart;

        private Check(GameTestHelper test) {
            this.test = test;
            level = test.getLevel();
            base = test.absolutePos(new BlockPos(3, 3, 3));
            for (BlockPos mutable : BlockPos.betweenClosed(base.offset(-3, -2, -3), base.offset(8, LAYER + 4, 3))) {
                original.put(mutable.immutable(), level.getBlockState(mutable));
            }
        }

        private ServerPlayer connect(String prefix, GameType mode) {
            var connection = new Connection(PacketFlow.SERVERBOUND);
            var channel = new EmbeddedChannel(connection);
            var player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), prefix + UUID.randomUUID().toString().substring(0, 8)), ClientInformation.createDefault());
            level.getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
            test.assertTrue(player.level() == level && level.getServer().getPlayerList().getPlayer(player.getUUID()) == player, "an actual connected player");
            player.setNoGravity(true);
            player.setGameMode(mode);
            connections.add(connection);
            channels.add(channel);
            players.add(player);
            return player;
        }

        private void start() {
            touched = true;
            forced.addAll(GolemChunks.hold(level, original.keySet()));
            for (BlockPos mutable : original.keySet()) {
                boolean floor = mutable.getY() == base.getY() - 1 || mutable.getY() == base.getY() - 1 + LAYER;
                level.setBlock(mutable, floor ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), QUIET);
            }
            owner = connect("trunk-owner-", GameType.SURVIVAL);
            stranger = connect("trunk-stranger-", GameType.SURVIVAL);
            owner.setPos(Vec3.atBottomCenterOf(base.offset(-2, 0, 0)));
            stranger.setPos(Vec3.atBottomCenterOf(base.offset(-2, 0, 2)));
        }

        private EntityTravelingTrunk trunk(BlockPos at, int upgrade, ServerPlayer master) {
            var trunk = ThaumcraftGolems.TRUNK.get().create(level);
            trunk.moveTo(at.getX() + .5, at.getY(), at.getZ() + .5, 0, 0);
            trunk.setOwner(master);
            if (upgrade != TrunkRules.NO_UPGRADE) trunk.setUpgrade(upgrade);
            test.assertTrue(level.addFreshEntity(trunk), "the trunk is admitted");
            spawned.add(trunk);
            return trunk;
        }

        private static Item upgradeItem(int upgrade) {
            for (GolemUpgrade kind : GolemUpgrade.values()) if (kind.id == upgrade) return ThaumcraftGolems.upgradeItem(kind);
            throw new IllegalArgumentException("upgrade " + upgrade);
        }

        private List<ItemEntity> drops() { return level.getEntitiesOfClass(ItemEntity.class, new AABB(Vec3.atLowerCornerOf(base.offset(-3, -1, -3)), Vec3.atLowerCornerOf(base.offset(9, 4, 4)))); }
        private void clearDrops() { drops().forEach(Entity::discard); }

        // ---- recipe -----------------------------------------------------------------------------------------------

        private void recipe() {
            var holder = level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "infusion/traveling_trunk"));
            test.assertTrue(holder.isPresent() && holder.get().value() instanceof InfusionStackRecipe, "the infusion recipe is loaded");
            var recipe = (InfusionStackRecipe) holder.get().value();
            test.assertTrue("TRAVELTRUNK".equals(recipe.research()) && recipe.result().is(ThaumcraftGolems.TRUNK_SPAWNER.get())
                            && recipe.instability() == 3 && recipe.aspects().equals(Map.of("motus", 4, "spiritus", 4, "iter", 4, "vacuos", 16)),
                    "TRAVELTRUNK, instability 3, motus 4 / spiritus 4 / iter 4 / vacuos 16 -> the trunk spawner");
        }

        // ---- the spawner -----------------------------------------------------------------------------------------

        private InteractionResult place(ItemStack stack, ServerPlayer user, BlockPos on) {
            user.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var hit = new BlockHitResult(Vec3.atCenterOf(on).add(0, .5, 0), Direction.UP, on, false);
            return stack.getItem() instanceof thaumcraft.common.items.golems.ItemTrunkSpawner spawner
                    ? spawner.useOn(new UseOnContext(user, InteractionHand.MAIN_HAND, hit)) : InteractionResult.PASS;
        }

        private List<EntityTravelingTrunk> trunksNear() {
            return level.getEntitiesOfClass(EntityTravelingTrunk.class, new AABB(Vec3.atLowerCornerOf(base.offset(-3, -1, -3)), Vec3.atLowerCornerOf(base.offset(9, 6, 4))));
        }

        private void spawnerPlacesTheTrunkForItsUser() {
            var floor = base.offset(3, -1, 0);
            var plain = new ItemStack(ThaumcraftGolems.TRUNK_SPAWNER.get());
            test.assertTrue(place(plain, owner, floor) == InteractionResult.CONSUME && plain.isEmpty(), "the spawner is used up in survival");
            var found = trunksNear();
            test.assertTrue(found.size() == 1, "one trunk stands there");
            var trunk = found.get(0);
            spawned.add(trunk);
            test.assertTrue(trunk.isOwner(owner) && !trunk.isOwner(stranger) && trunk.getOwnerName().equals(owner.getGameProfile().getName()), "the user is the owner");
            test.assertTrue(trunk.getUpgrade() == -1 && trunk.getRows() == 3 && trunk.slotCount() == 27 && trunk.getHealth() == 75F && !trunk.getStay(), "an empty trunk: 27 slots, 75 health");
            test.assertTrue(trunk.position().distanceTo(Vec3.atCenterOf(floor.above()).subtract(0, .5, 0)) < 1.0, "it stands on the clicked block");
            trunk.discard();

            // Upgrade, name and contents travel with the item; creative does not use it up.
            var carrying = new ItemStack(ThaumcraftGolems.TRUNK_SPAWNER.get());
            carrying.set(ThaumcraftGolems.TRUNK_UPGRADE.get(), TrunkRules.EARTH);
            carrying.set(DataComponents.CUSTOM_NAME, Component.literal("Bertha"));
            carrying.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND, 3), new ItemStack(Items.APPLE, 7))));
            var creative = connect("trunk-creative-", GameType.CREATIVE);
            test.assertTrue(place(carrying, creative, floor) == InteractionResult.CONSUME && carrying.getCount() == 1, "creative keeps the item");
            var restored = trunksNear().get(0);
            spawned.add(restored);
            test.assertTrue(restored.getUpgrade() == TrunkRules.EARTH && restored.getRows() == 4 && restored.slotCount() == 36, "the Earth upgrade gives four rows");
            test.assertTrue("Bertha".equals(restored.getCustomName().getString()) && restored.isOwner(creative), "the name and the user");
            test.assertTrue(restored.inventory.getItem(0).is(Items.DIAMOND) && restored.inventory.getItem(0).getCount() == 3
                            && restored.inventory.getItem(1).is(Items.APPLE) && restored.inventory.getItem(1).getCount() == 7, "the contents are restored in order");
            restored.discard();
        }

        // ---- upgrades --------------------------------------------------------------------------------------------

        private void everyUpgradeIsFittedOnce() {
            for (int upgrade = TrunkRules.AIR; upgrade <= TrunkRules.ENTROPY; upgrade++) {
                var trunk = trunk(base.offset(4, 0, 0), TrunkRules.NO_UPGRADE, owner);
                var item = new ItemStack(upgradeItem(upgrade), 2);
                owner.setItemInHand(InteractionHand.MAIN_HAND, item);
                test.assertTrue(owner.interactOn(trunk, InteractionHand.MAIN_HAND).consumesAction(), "upgrade " + upgrade + " is accepted");
                test.assertTrue(trunk.getUpgrade() == upgrade && trunk.getRows() == (upgrade == TrunkRules.EARTH ? 4 : 3) && item.getCount() == 1,
                        "upgrade " + upgrade + " fitted, one item used, rows " + trunk.getRows());
                // A second one is not taken: the menu opens instead (and is closed again).
                owner.interactOn(trunk, InteractionHand.MAIN_HAND);
                test.assertTrue(trunk.getUpgrade() == upgrade && item.getCount() == 1, "a trunk takes one upgrade only");
                owner.closeContainer();
                trunk.discard();
            }
        }

        // ---- menu ------------------------------------------------------------------------------------------------

        private void menuOpensAndTogglesStay() {
            var trunk = trunk(base.offset(4, 0, 0), TrunkRules.NO_UPGRADE, owner);
            trunk.inventory.setItem(0, new ItemStack(Items.DIAMOND, 5));
            owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            owner.setPos(Vec3.atBottomCenterOf(base.offset(3, 0, 0)));
            test.assertTrue(owner.interactOn(trunk, InteractionHand.MAIN_HAND).consumesAction(), "an empty hand opens the trunk");
            test.assertTrue(owner.containerMenu instanceof ContainerTravelingTrunk && trunk.isOpen(), "the trunk menu is open and so is the lid");
            var menu = (ContainerTravelingTrunk) owner.containerMenu;
            test.assertTrue(menu.slots.size() == 27 + 36 && menu.rows() == 3, "27 trunk slots and the player's 36");
            test.assertTrue(menu.slots.get(0).getItem().is(Items.DIAMOND) && menu.slots.get(0).x == 8 && menu.slots.get(0).y == 15
                            && menu.slots.get(10).x == 26 && menu.slots.get(10).y == 38 && menu.slots.get(27).y == 118 && menu.slots.get(54).y == 176,
                    "the original slot positions");
            test.assertTrue(menu.clickMenuButton(owner, ContainerTravelingTrunk.STAY_BUTTON) && trunk.getStay(), "button 1 makes it stay");
            test.assertTrue(menu.clickMenuButton(owner, ContainerTravelingTrunk.STAY_BUTTON) && !trunk.getStay(), "and again, follow");
            test.assertTrue(!menu.clickMenuButton(owner, 0) && !menu.clickMenuButton(owner, 2), "no other button exists");
            // Shift-click moves from the trunk to the player and back.
            menu.clicked(0, 0, ClickType.QUICK_MOVE, owner);
            test.assertTrue(trunk.inventory.getItem(0).isEmpty() && owner.getInventory().countItem(Items.DIAMOND) == 5, "shift-click takes the stack out");
            int playerSlot = menu.slots.size() - 1;
            for (int slot = 27; slot < menu.slots.size(); slot++) if (menu.slots.get(slot).getItem().is(Items.DIAMOND)) playerSlot = slot;
            menu.clicked(playerSlot, 0, ClickType.QUICK_MOVE, owner);
            test.assertTrue(trunk.inventory.getItem(0).is(Items.DIAMOND) && trunk.inventory.getItem(0).getCount() == 5, "and puts it back in the first slot");
            owner.closeContainer();
            test.assertTrue(!trunk.isOpen(), "closing shuts the lid");
            trunk.inventory.setItem(0, ItemStack.EMPTY);
            trunk.discard();
        }

        private void waterBelongsToItsOwner() {
            var trunk = trunk(base.offset(4, 0, 0), TrunkRules.WATER, owner);
            stranger.setPos(Vec3.atBottomCenterOf(base.offset(3, 0, 1)));
            stranger.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            test.assertTrue(stranger.interactOn(trunk, InteractionHand.MAIN_HAND).consumesAction() && !(stranger.containerMenu instanceof ContainerTravelingTrunk),
                    "a stranger cannot open a Water trunk");
            owner.setPos(Vec3.atBottomCenterOf(base.offset(3, 0, 0)));
            owner.interactOn(trunk, InteractionHand.MAIN_HAND);
            test.assertTrue(owner.containerMenu instanceof ContainerTravelingTrunk, "its owner can");
            owner.closeContainer();
            // An ordinary trunk opens for anyone.
            var open = trunk(base.offset(4, 0, 1), TrunkRules.NO_UPGRADE, owner);
            stranger.interactOn(open, InteractionHand.MAIN_HAND);
            test.assertTrue(stranger.containerMenu instanceof ContainerTravelingTrunk, "an ordinary trunk opens for a stranger");
            stranger.closeContainer();
            open.discard();
            trunk.discard();
        }

        // ---- food, healing, damage --------------------------------------------------------------------------------

        private void foodAndHealing() {
            var trunk = trunk(base.offset(4, 0, 0), TrunkRules.NO_UPGRADE, owner);
            trunk.setHealth(10F);
            var bread = new ItemStack(Items.BREAD, 3);
            owner.setItemInHand(InteractionHand.MAIN_HAND, bread);
            test.assertTrue(owner.interactOn(trunk, InteractionHand.MAIN_HAND).consumesAction(), "food is accepted by a hurt trunk");
            test.assertTrue(trunk.getHealth() == 15F && bread.getCount() == 2, "bread heals by its nutrition (5) and is eaten: " + trunk.getHealth());
            trunk.setHealth(75F);
            owner.interactOn(trunk, InteractionHand.MAIN_HAND);
            test.assertTrue(bread.getCount() == 2 && owner.containerMenu instanceof ContainerTravelingTrunk, "a healthy trunk does not eat; it opens");
            owner.closeContainer();
            trunk.discard();
        }

        private void damageRules() {
            var trunk = trunk(base.offset(4, 0, 0), TrunkRules.NO_UPGRADE, owner);
            // The original ignores the 1.7.10 "in wall" source (DamageSource.field_76367_g,
            // DamageSource.IN_WALL in the 1.12.2 line of this mod) and nothing else, so a
            // cactus hurts the trunk and being in a wall does not.
            test.assertTrue(trunk.hurt(level.damageSources().cactus(), 5F) && trunk.getHealth() == 70F,
                    "a cactus hurts the trunk: " + trunk.getHealth());
            test.assertTrue(!trunk.hurt(new net.minecraft.world.damagesource.DamageSource(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getOrThrow(net.minecraft.world.damagesource.DamageTypes.IN_WALL)), 5F) && trunk.getHealth() == 70F,
                    "being in a wall does nothing: " + trunk.getHealth());
            test.assertTrue(trunk.fireImmune() && trunk.getType().fireImmune(), "fire does not harm it");
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
            test.assertTrue(trunk.hurt(level.damageSources().mobAttack(owner), 5F) && trunk.getHealth() == 70F, "an ordinary hit lands: " + trunk.getHealth());
            trunk.discard();
            var water = trunk(base.offset(4, 0, 0), TrunkRules.WATER, owner);
            test.assertTrue(!water.hurt(level.damageSources().mobAttack(stranger), 50F) && water.getHealth() == 75F, "a Water trunk cannot be hurt");
            water.discard();
            // Death drops the whole inventory.
            var doomed = trunk(base.offset(4, 0, 0), TrunkRules.NO_UPGRADE, owner);
            doomed.inventory.setItem(0, new ItemStack(Items.GOLD_INGOT, 9));
            doomed.inventory.setItem(26, new ItemStack(Items.EMERALD, 2));
            clearDrops();
            doomed.die(level.damageSources().generic());
            int gold = 0, emeralds = 0;
            for (ItemEntity drop : drops()) { if (drop.getItem().is(Items.GOLD_INGOT)) gold += drop.getItem().getCount(); if (drop.getItem().is(Items.EMERALD)) emeralds += drop.getItem().getCount(); }
            test.assertTrue(gold == 9 && emeralds == 2 && doomed.inventory.isEmpty(), "everything it held is dropped: " + gold + " gold, " + emeralds + " emeralds");
            clearDrops();
            doomed.discard();
        }

        // ---- the bell --------------------------------------------------------------------------------------------

        private void bellRecall() {
            var bell = new ItemStack(ThaumcraftGolems.GOLEM_BELL.get());
            // Plain hit: the spawner keeps the upgrade and the name; contents only with Order.
            for (int upgrade : new int[] {TrunkRules.NO_UPGRADE, TrunkRules.FIRE, TrunkRules.ORDER}) {
                var trunk = trunk(base.offset(4, 0, 0), upgrade, owner);
                trunk.setCustomName(Component.literal("Rex"));
                trunk.inventory.setItem(3, new ItemStack(Items.DIAMOND, 4));
                owner.setItemInHand(InteractionHand.MAIN_HAND, bell);
                owner.setShiftKeyDown(false);
                clearDrops();
                test.assertTrue(!trunk.hurt(level.damageSources().playerAttack(owner), 1F) && trunk.isRemoved() && trunk.getHealth() >= 0, "the bell recalls and never harms (upgrade " + upgrade + ")");
                ItemStack spawner = ItemStack.EMPTY, diamonds = ItemStack.EMPTY;
                for (ItemEntity drop : drops()) { if (drop.getItem().is(ThaumcraftGolems.TRUNK_SPAWNER.get())) spawner = drop.getItem(); if (drop.getItem().is(Items.DIAMOND)) diamonds = drop.getItem(); }
                test.assertTrue(!spawner.isEmpty() && spawner.get(ThaumcraftGolems.TRUNK_UPGRADE.get()) == upgrade && "Rex".equals(spawner.getHoverName().getString()),
                        "a spawner with the upgrade " + upgrade + " and the name: " + spawner);
                if (upgrade == TrunkRules.ORDER) {
                    var kept = spawner.get(DataComponents.CONTAINER);
                    test.assertTrue(diamonds.isEmpty() && kept != null && kept.nonEmptyItems().iterator().hasNext() && kept.nonEmptyItems().iterator().next().is(Items.DIAMOND),
                            "Order keeps the contents inside the spawner and drops nothing");
                } else {
                    test.assertTrue(diamonds.getCount() == 4 && spawner.get(DataComponents.CONTAINER) == null, "without Order the contents are dropped and not kept");
                }
                clearDrops();
            }
            // Dismantling (sneaking): a blank spawner, every stack dropped, the upgrade item half of the time.
            int returned = 0;
            for (int round = 0; round < 40; round++) {
                var trunk = trunk(base.offset(4, 0, 0), TrunkRules.ORDER, owner);
                trunk.inventory.setItem(0, new ItemStack(Items.DIAMOND, 2));
                owner.setItemInHand(InteractionHand.MAIN_HAND, bell);
                owner.setShiftKeyDown(true);
                clearDrops();
                trunk.hurt(level.damageSources().playerAttack(owner), 1F);
                boolean blank = false, diamond = false, upgradeItem = false;
                for (ItemEntity drop : drops()) {
                    if (drop.getItem().is(ThaumcraftGolems.TRUNK_SPAWNER.get())) blank = drop.getItem().get(ThaumcraftGolems.TRUNK_UPGRADE.get()) == null && drop.getItem().get(DataComponents.CONTAINER) == null;
                    if (drop.getItem().is(Items.DIAMOND)) diamond = true;
                    if (drop.getItem().is(ThaumcraftGolems.UPGRADE_ORDER.get())) upgradeItem = true;
                }
                test.assertTrue(blank && diamond, "dismantling gives a blank spawner and the contents (round " + round + ")");
                if (upgradeItem) returned++;
                clearDrops();
            }
            owner.setShiftKeyDown(false);
            test.assertTrue(returned > 5 && returned < 35, "the upgrade item comes back about half of the time: " + returned + " of 40");
            // A Water trunk answers to its owner only.
            var water = trunk(base.offset(4, 0, 0), TrunkRules.WATER, owner);
            stranger.setItemInHand(InteractionHand.MAIN_HAND, bell);
            stranger.setShiftKeyDown(false);
            water.hurt(level.damageSources().playerAttack(stranger), 1F);
            test.assertTrue(!water.isRemoved(), "a stranger's bell leaves a Water trunk alone");
            owner.setItemInHand(InteractionHand.MAIN_HAND, bell);
            water.hurt(level.damageSources().playerAttack(owner), 1F);
            test.assertTrue(water.isRemoved(), "its owner's bell takes it");
            clearDrops();
        }

        // ---- saving ----------------------------------------------------------------------------------------------

        private void persistence() {
            var trunk = trunk(base.offset(4, 0, 0), TrunkRules.EARTH, owner);
            trunk.setStay(true);
            trunk.inventory.setItem(30, new ItemStack(Items.DIAMOND, 6));
            trunk.inventory.setItem(1, new ItemStack(Items.APPLE, 2));
            var tag = new CompoundTag();
            trunk.saveWithoutId(tag);
            test.assertTrue(tag.getByte("upgrade") == TrunkRules.EARTH && tag.getBoolean("Stay") && tag.getString("Owner").equals(owner.getGameProfile().getName())
                            && tag.getUUID("OwnerUUID").equals(owner.getUUID()) && tag.getList("Inventory", 10).size() == 2, "the original keys are written");
            var loaded = ThaumcraftGolems.TRUNK.get().create(level);
            loaded.load(tag.copy());
            test.assertTrue(loaded.getUpgrade() == TrunkRules.EARTH && loaded.getRows() == 4 && loaded.getStay() && loaded.isOwner(owner) && loaded.getOwnerName().equals(owner.getGameProfile().getName())
                            && loaded.inventory.getItem(30).is(Items.DIAMOND) && loaded.inventory.getItem(30).getCount() == 6 && loaded.inventory.getItem(1).is(Items.APPLE),
                    "reloading restores upgrade, rows, stay, owner and every slot including the fourth row");
            var broken = tag.copy();
            broken.putByte("upgrade", (byte) 9);
            var odd = ThaumcraftGolems.TRUNK.get().create(level);
            odd.load(broken);
            test.assertTrue(odd.getUpgrade() == -1 && odd.getRows() == 3, "an unknown upgrade reads as none");
            loaded.discard();
            odd.discard();
            trunk.inventory.clearContent();
            trunk.discard();
        }

        // ---- walking phases --------------------------------------------------------------------------------------

        private Entity current;
        private EntityTravelingTrunk follower, high, defender, puller;
        private Vec3 coalStart;
        private Zombie target;
        private Vec3 stayStart;
        private final List<ItemEntity> bait = new ArrayList<>();

        private void armPhases() {
            owner.setPos(Vec3.atBottomCenterOf(base.offset(-2, 0, 0)));
            phases.add(new Phase("follow", () -> {
                follower = trunk(base.offset(6, 0, 0), TrunkRules.NO_UPGRADE, owner);
            }, () -> follower.distanceTo(owner) <= TrunkRules.FOLLOW_DISTANCE + 0.5F));
            phases.add(new Phase("rest", () -> {
                stayStart = follower.position();
            }, () -> {
                test.assertTrue(follower.distanceTo(owner) <= TrunkRules.FOLLOW_DISTANCE + 2F, "a trunk within reach does not wander off");
                return test.getTick() - phaseStart > 40;
            }));
            phases.add(new Phase("stay", () -> {
                follower.setStay(true);
                follower.teleportTo(base.getX() + 6.5, base.getY(), base.getZ() + 0.5);
                stayStart = follower.position();
            }, () -> {
                test.assertTrue(follower.position().distanceToSqr(stayStart) < 0.25, "a trunk told to stay stays: moved to " + follower.position());
                return test.getTick() - phaseStart > 80;
            }));
            phases.add(new Phase("teleport", () -> {
                follower.discard();
                high = trunk(base.offset(0, LAYER, 0), TrunkRules.NO_UPGRADE, owner);
                test.assertTrue(high.distanceTo(owner) > TrunkRules.TELEPORT_DISTANCE, "the trunk starts far above its owner");
            }, () -> high.distanceTo(owner) < 4.5F));
            phases.add(new Phase("teleport lands on the ring", () -> {
                double dx = high.getX() - owner.getX(), dz = high.getZ() - owner.getZ();
                double ring = Math.max(Math.abs(dx), Math.abs(dz));
                test.assertTrue(ring >= 1.5 && Math.abs(high.getY() - owner.getY()) < 1.5, "it lands on the outer ring round the owner at the owner's height: " + high.position());
                high.discard();
            }, () -> true));
            phases.add(new Phase("fire defence", () -> {
                defender = trunk(base.offset(-1, 0, 1), TrunkRules.FIRE, owner);
                target = EntityType.ZOMBIE.create(level);
                target.setNoAi(true);
                target.setPos(Vec3.atBottomCenterOf(base.offset(3, 0, 1)));
                target.setPersistenceRequired();
                test.assertTrue(level.addFreshEntity(target), "the attacker is admitted");
                spawned.add(target);
                owner.setLastHurtByMob(target);
            }, () -> target.getHealth() < target.getMaxHealth() || !target.isAlive()));
            phases.add(new Phase("fire defence is angry", () -> {
                test.assertTrue(defender.getAnger() > 0 || !target.isAlive(), "the Fire trunk is angry at its owner's attacker");
                defender.discard();
                target.discard();
                owner.setLastHurtByMob(null);
            }, () -> true));
            phases.add(new Phase("no defence without Fire", () -> {
                defender = trunk(base.offset(-1, 0, 1), TrunkRules.NO_UPGRADE, owner);
                target = EntityType.ZOMBIE.create(level);
                target.setNoAi(true);
                target.setPos(Vec3.atBottomCenterOf(base.offset(3, 0, 1)));
                target.setPersistenceRequired();
                level.addFreshEntity(target);
                spawned.add(target);
                owner.setLastHurtByMob(target);
            }, () -> {
                test.assertTrue(defender.getAnger() == 0 && target.getHealth() == target.getMaxHealth(), "an ordinary trunk leaves the attacker alone");
                return test.getTick() - phaseStart > 60;
            }));
            phases.add(new Phase("entropy pull", () -> {
                defender.discard();
                target.discard();
                owner.setLastHurtByMob(null);
                puller = trunk(base.offset(4, 0, 1), TrunkRules.ENTROPY, owner);
                puller.setStay(true);
                bait.clear();
                bait.add(drop(new ItemStack(Items.DIAMOND, 5), puller.position().add(2.5, 0.3, 0)));
                bait.add(drop(new ItemStack(Items.EMERALD, 3), puller.position().add(0, 0.3, 2.0)));
                bait.add(drop(new ItemStack(Items.COAL, 9), puller.position().add(7.0, 0.3, 0)));
            }, () -> puller.inventory.contents().stream().anyMatch(stack -> stack.is(Items.DIAMOND) && stack.getCount() == 5)
                    && puller.inventory.contents().stream().anyMatch(stack -> stack.is(Items.EMERALD) && stack.getCount() == 3)));
            phases.add(new Phase("entropy leaves far items", () -> {
                test.assertTrue(!bait.get(2).isRemoved() && puller.inventory.contents().stream().noneMatch(stack -> stack.is(Items.COAL)), "an item beyond three blocks is left alone");
                coalStart = bait.get(2).position();
            }, () -> {
                // The pull is a drift, not a snap: an item inside the radius would creep towards the trunk over these ticks.
                test.assertTrue(!bait.get(2).isRemoved() && bait.get(2).position().distanceTo(coalStart) < 0.5 && puller.inventory.contents().stream().noneMatch(stack -> stack.is(Items.COAL)),
                        "the item seven blocks away does not drift towards the trunk: from " + coalStart + " to " + bait.get(2).position());
                return test.getTick() - phaseStart > 60;
            }));
            phases.add(new Phase("no pull without Entropy", () -> {
                puller.discard();
                bait.forEach(Entity::discard);
                puller = trunk(base.offset(4, 0, 1), TrunkRules.NO_UPGRADE, owner);
                puller.setStay(true);
                bait.clear();
                bait.add(drop(new ItemStack(Items.DIAMOND, 5), puller.position().add(1.0, 0.3, 0)));
            }, () -> {
                test.assertTrue(puller.inventory.isEmpty() && !bait.get(0).isRemoved(), "an ordinary trunk swallows nothing");
                return test.getTick() - phaseStart > 50;
            }));
            phases.add(new Phase("water heals every tick", () -> {
                puller.discard();
                bait.forEach(Entity::discard);
                defender = trunk(base.offset(4, 0, 1), TrunkRules.WATER, owner);
                defender.setStay(true);
                defender.setHealth(10F);
                puller = trunk(base.offset(5, 0, 1), TrunkRules.NO_UPGRADE, owner);
                puller.setStay(true);
                puller.setHealth(10F);
            }, () -> {
                if (test.getTick() - phaseStart < 25) return false;
                test.assertTrue(defender.getHealth() >= 30F && puller.getHealth() <= 12F, "Water heals every tick, an ordinary trunk one point in fifty ticks: "
                        + defender.getHealth() + " / " + puller.getHealth());
                return true;
            }));
        }

        private ItemEntity drop(ItemStack stack, Vec3 at) {
            var item = new ItemEntity(level, at.x, at.y, at.z, stack, 0, 0, 0);
            item.setNoPickUpDelay();
            level.addFreshEntity(item);
            spawned.add(item);
            return item;
        }

        private boolean poll() {
            long tick = test.getTick();
            if (phase < 0 || phase < phases.size() && phases.get(phase).done().done()) {
                phase++;
                if (phase >= phases.size()) return true;
                phaseStart = tick;
                phases.get(phase).start().run();
                return false;
            }
            return false;
        }

        private String describe() {
            return "phase " + phase + (phase >= 0 && phase < phases.size() ? " (" + phases.get(phase).name() + ")" : "")
                    + (follower != null ? ", follower " + follower.position() + " distance " + follower.distanceTo(owner) : "");
        }

        private void finish() {
            if (finished) return;
            finished = true;
            try {
                for (Entity entity : spawned) entity.discard();
                level.getEntitiesOfClass(EntityTravelingTrunk.class, new AABB(Vec3.atLowerCornerOf(base.offset(-4, -2, -4)), Vec3.atLowerCornerOf(base.offset(10, LAYER + 5, 5)))).forEach(Entity::discard);
                clearDrops();
                if (touched) original.forEach((pos, state) -> level.setBlock(pos, state, QUIET));
                for (ChunkPos chunk : forced) level.setChunkForced(chunk.x, chunk.z, false);
            } finally {
                for (ServerPlayer player : players) {
                    try {
                        if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player);
                    } finally { player.discard(); }
                }
                channels.forEach(EmbeddedChannel::finishAndReleaseAll);
            }
        }
    }
}
