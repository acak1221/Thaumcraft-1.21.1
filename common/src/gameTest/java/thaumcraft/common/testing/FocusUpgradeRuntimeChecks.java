package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.entities.projectile.EntityEmber;
import thaumcraft.common.entities.projectile.EntityFrostShard;
import thaumcraft.common.items.wands.FocusVis;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.FocusUpgradeData;
import thaumcraft.common.items.wands.foci.ItemFocusExcavation;
import thaumcraft.common.items.wands.foci.ItemFocusFire;
import thaumcraft.common.items.wands.foci.ItemFocusFrost;
import thaumcraft.common.items.wands.foci.ItemFocusShock;
import thaumcraft.common.items.wands.foci.WandFocusActions;
import thaumcraft.common.lib.network.FocusSelectionNetworking;
import thaumcraft.common.lib.network.FocusSelectionPayload;

/** Synchronous native fixtures, not acquisition/client/cooldown evidence. Each
 * invocation owns its connected actor, lane and outputs. No clock/RNG control,
 * manual drops or manual hurt. Root supplies a <=240-tick wrapper. */
public final class FocusUpgradeRuntimeChecks {
    private FocusUpgradeRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        try (var actor = new Actor(test)) {
            data(actor); casting(actor); excavationSpeed(actor);
        }
        test.succeed();
    }

    private static void data(Actor a) {
        for (var item : List.of(ThaumcraftFoci.FIRE_FOCUS.get(), ThaumcraftFoci.FROST_FOCUS.get(),
                ThaumcraftFoci.SHOCK_FOCUS.get(), ThaumcraftFoci.EXCAVATION_FOCUS.get())) {
            ItemStack focus = new ItemStack(item);
            a.require(!focus.has(ThaumcraftFoci.FOCUS_UPGRADES.get()) && FocusUpgradeData.read(focus).equals(FocusUpgradeData.EMPTY)
                    && !focus.has(ThaumcraftFoci.FOCUS_UPGRADES.get()), "missing upgrades read empty without rewriting input");
            for (int rank = 1; rank <= 5; rank++) {
                var choices = item.getPossibleUpgradesByRank(focus, rank);
                a.require(java.util.Arrays.equals(choices, originalChoices(item, rank)),
                        "all four foci retain exact original rank candidates and relative order: " + item + " rank" + rank);
            }
            a.require(item.applyUpgrade(focus, FocusUpgradeType.potency, 5) && item.applyUpgrade(focus, FocusUpgradeType.frugal, 1), "public API preserves ordered holes");
            for (int rank : new int[]{Integer.MIN_VALUE, 0, 6, Integer.MAX_VALUE})
                a.require(!item.applyUpgrade(focus, FocusUpgradeType.frugal, rank), "invalid rank refuses before indexing");
            a.require(!item.applyUpgrade(focus, FocusUpgradeType.potency, 1) && item.getUpgradeLevel(focus, FocusUpgradeType.frugal) == 1, "occupied rank cannot respec");
            short[] copied = item.getAppliedUpgrades(focus); copied[0] = 0;
            a.require(item.getAppliedUpgrades(focus)[0] == 1, "original-shaped array getter never aliases component");
            var tag = new CompoundTag(); tag.putString("owner-fixture", UUID.randomUUID().toString());
            focus.set(DataComponents.CUSTOM_NAME, Component.literal("ordered native upgrade fixture"));
            focus.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            a.equip(ItemStack.EMPTY, "gold", 500);
            WandData material = ItemWandCasting.data(a.wand);
            a.player.getInventory().setItem(5, focus.copy());
            a.require(FocusSelectionNetworking.handle(a.player, new FocusSelectionPayload(InteractionHand.MAIN_HAND, 5))
                    && ItemStack.matches(focus, FocusData.get(a.wand)) && ItemWandCasting.data(a.wand).equals(material), "real selection preserves full upgraded focus and wand materials/vis");
            ItemStack saved = ItemStack.parse(a.level.registryAccess(), a.wand.save(a.level.registryAccess())).orElseThrow();
            a.require(ItemStack.matches(saved, a.wand), "native item save reload preserves nested five-slot data and unrelated components");
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), a.level.registryAccess());
            try {
                ItemStack.STREAM_CODEC.encode(buffer, a.wand);
                a.require(ItemStack.matches(a.wand, ItemStack.STREAM_CODEC.decode(buffer)), "native component network round trip preserves full nested stack");
            } finally { buffer.release(); }
            ItemStack detached = FocusData.get(a.wand); detached.remove(ThaumcraftFoci.FOCUS_UPGRADES.get());
            a.require(ItemStack.matches(focus, FocusData.get(a.wand)), "detached focus cannot edit equipped wrapper");
            a.require(FocusSelectionNetworking.handle(a.player, new FocusSelectionPayload(InteractionHand.MAIN_HAND, -1)), "upgraded focus can be removed through real production receiver");
            a.require(a.player.getInventory().items.stream().anyMatch(stack -> ItemStack.matches(stack, focus)), "removal returns exact upgraded item rather than reconstructing it");
            a.player.getInventory().clearContent();
        }
        ItemStack illegal = new ItemStack(ThaumcraftFoci.FIRE_FOCUS.get());
        illegal.set(ThaumcraftFoci.FOCUS_UPGRADES.get(), new FocusUpgradeData(List.of((short)2, (short)-1, (short)-1, (short)-1, (short)-1)));
        a.require(!FocusUpgradeData.supportedForFocus(illegal) && !FocusData.isSupportedFocus(illegal), "directly attached Treasure on Fire is not silently basic");
        boolean rejected = false;
        try { new FocusData(illegal); } catch (IllegalArgumentException expected) { rejected = true; }
        a.require(rejected && FocusUpgradeType.get(8) == FocusUpgradeType.silktouch && FocusUpgradeType.get(20) == FocusUpgradeType.dowsing
                        && FocusUpgradeType.get(21) == null,
                "invalid nested data is rejected, Silk8/Dowsing20 retain original ids and unknown21 remains unavailable");
    }

    private static FocusUpgradeType[] originalChoices(ItemFocusBasic item, int rank) {
        var f = FocusUpgradeType.frugal; var p = FocusUpgradeType.potency;
        if (item instanceof ItemFocusFire) return switch (rank) {
            case 2, 4 -> new FocusUpgradeType[]{f,p,FocusUpgradeType.alchemistsfire};
            case 3 -> new FocusUpgradeType[]{f,p,FocusUpgradeType.fireball,FocusUpgradeType.firebeam};
            default -> new FocusUpgradeType[]{f,p};
        };
        if (item instanceof ItemFocusFrost) return switch (rank) {
            case 1, 5 -> new FocusUpgradeType[]{f,p,FocusUpgradeType.alchemistsfrost};
            case 3 -> new FocusUpgradeType[]{f,p,FocusUpgradeType.scattershot,FocusUpgradeType.iceboulder,FocusUpgradeType.alchemistsfrost};
            default -> new FocusUpgradeType[]{f,p};
        };
        if (item instanceof ItemFocusShock) return switch (rank) {
            case 3 -> new FocusUpgradeType[]{f,p,FocusUpgradeType.chainlightning,FocusUpgradeType.earthshock};
            case 4, 5 -> new FocusUpgradeType[]{f,p,FocusUpgradeType.enlarge};
            default -> new FocusUpgradeType[]{f,p};
        };
        return switch (rank) {
            case 2, 4 -> new FocusUpgradeType[]{f,p,FocusUpgradeType.enlarge};
            case 3 -> new FocusUpgradeType[]{f,p,FocusUpgradeType.treasure,FocusUpgradeType.dowsing};
            case 5 -> new FocusUpgradeType[]{f,p,FocusUpgradeType.treasure,FocusUpgradeType.silktouch};
            default -> new FocusUpgradeType[]{f,p,FocusUpgradeType.treasure};
        };
    }

    private static void casting(Actor a) {
        a.clearLane();
        ItemStack fire = a.focus(ThaumcraftFoci.FIRE_FOCUS.get(), FocusUpgradeType.frugal, 1, FocusUpgradeType.potency, 2, FocusUpgradeType.potency, 3);
        a.equip(fire, "gold", 100);
        a.require(ItemFocusFire.tickCost(a.wand, a.player) == 9 && ArcaneVis.price(Map.of("ignis", 10), a.wand, a.player).get("ignis") == 1000, "Frugal affects casting, not whole-vis arcane recipes");
        a.begin(); a.tick(1);
        var embers = a.projectiles(EntityEmber.class);
        a.require(a.vis("ignis") == 91 && embers.size() == 4 && embers.stream().allMatch(ember -> ember.getDamage() == 4F && ember.getFirey() == 0), "one paid native Fire tick produces2+P damage2+P basic embers, no specialization");
        a.player.stopUsingItem(); a.purgeOutputs();

        ItemStack frugalFrost = new ItemStack(ThaumcraftFoci.FROST_FOCUS.get());
        for (int rank = 1; rank <= 5; rank++) a.require(ThaumcraftFoci.FROST_FOCUS.get().applyUpgrade(frugalFrost, FocusUpgradeType.frugal, rank), "five legitimate Frugal ranks");
        a.equip(frugalFrost, "thaumium", 0);
        a.wand.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(a.wand).withAmount("aqua", 1));
        var zero = ItemFocusFrost.activationCost(a.wand, a.player);
        a.require(zero.equals(Map.of("aqua", 1, "ignis", 0, "perditio", 0)) && FocusVis.canPay(a.wand, zero)
                && !ArcaneVis.canPay(a.wand, zero), "casting permits legitimate zero aspects without weakening recipe validator");
        a.require(!FocusVis.canPay(a.wand, Map.of("vitium", 0)) && !FocusVis.canPay(a.wand, Map.of("aqua", -1)), "zero does not authorize arbitrary/negative aspect entries");
        a.begin();
        a.require(a.vis("aqua") == 0 && a.projectiles(EntityFrostShard.class).size() == 1, "native Frost activation succeeds with zero fire/entropy rather than becoming unusable");
        a.purgeOutputs();

        ItemStack frost = a.focus(ThaumcraftFoci.FROST_FOCUS.get(), FocusUpgradeType.potency, 1, FocusUpgradeType.potency, 2);
        a.equip(frost, "gold", 100); a.begin();
        var shards = a.projectiles(EntityFrostShard.class);
        a.require(shards.size() == 1 && shards.getFirst().getDamage() == 6F
                && shards.getFirst().getBbWidth() == .15F + 6F * .15F,
                "basic Frost Potency changes actual projectile damage and native size without extra shots");
        a.purgeOutputs();

        ItemStack shock = a.focus(ThaumcraftFoci.SHOCK_FOCUS.get(), FocusUpgradeType.frugal, 1, FocusUpgradeType.potency, 2, FocusUpgradeType.potency, 3);
        a.equip(shock, "gold", 100); a.aim();
        var cow = EntityType.COW.create(a.level); a.require(cow != null, "owned native living target exists");
        try {
            cow.setNoAi(true); cow.setNoGravity(true); cow.setPos(a.root.getX() + .5, a.root.getY(), a.root.getZ() + .5);
            a.require(a.level.addFreshEntity(cow), "native target enters actual world index");
            a.require(a.level.getEntities(a.player, a.player.getBoundingBox().expandTowards(a.player.getLookAngle().scale(20)).inflate(1.1F))
                    .stream().noneMatch(entity -> entity != cow && entity.isPickable()), "owned Shock ray contains no unrelated pickable entity to damage");
            a.begin(); a.tick(1);
            a.require(cow.getHealth() == cow.getMaxHealth() - 6F && a.vis("aer") == 78 && cow.getRemainingFireTicks() <= 0,
                    "native basic Shock deals4+P with Frugal pulse payment and no ignition/chain");
        } finally { cow.discard(); }
    }

    private static void excavationSpeed(Actor a) {
        ItemStack focus = a.focus(ThaumcraftFoci.EXCAVATION_FOCUS.get(), FocusUpgradeType.frugal, 1, FocusUpgradeType.potency, 2);
        a.clearLane(); a.equip(focus, "gold", 100); a.put(a.root, Blocks.STONE.defaultBlockState()); a.aim(); a.begin();
        for (int tick = 1; tick <= 4; tick++) {
            a.tick(1); var progress = ItemFocusExcavation.progress(a.player);
            a.require(progress != null && progress.amount() == (tick - 1) * .5F && a.vis("terra") == 100, "Potency fast progress preserves initialization/pre-increment/no-interim-payment ordering");
        }
        a.tick(1); a.require(a.level.isEmptyBlock(a.root) && a.vis("terra") == 87, "P1 stone completes on fifth held tick and Frugal attempt pays13");
        for (var block : List.of(Blocks.IRON_BLOCK, Blocks.OBSIDIAN)) {
            a.clearLane(); a.equip(focus, "gold", 100); a.put(a.root, block.defaultBlockState()); a.aim(); a.begin(); a.tick(2);
            a.require(ItemFocusExcavation.progress(a.player).amount() == (block == Blocks.OBSIDIAN ? 1.5F : .15F)
                    && a.vis("terra") == 100, "Potency slow increment and exact obsidian multiplier, no vanilla speed substitution");
        }
    }

    /** Shared by the native focus-upgrade runtime helpers, never a production test hook. */
    static final class Actor implements AutoCloseable {
        final GameTestHelper test;
        final ServerLevel level;
        final ServerPlayer player;
        final BlockPos root;
        final AABB bounds;
        private final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        private final EmbeddedChannel channel = new EmbeddedChannel(connection);
        private final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        private final Map<BlockPos, CompoundTag> tiles = new LinkedHashMap<>();
        private final Set<UUID> previous = new HashSet<>();
        ItemStack wand = ItemStack.EMPTY;

        Actor(GameTestHelper test) { this(test, new BlockPos(4, 4, 4)); }
        Actor(GameTestHelper test, BlockPos relativeRoot) {
            this.test = test; level = test.getLevel(); root = test.absolutePos(relativeRoot);
            bounds = new AABB(Vec3.atLowerCornerOf(root.offset(-4, -4, -4)), Vec3.atLowerCornerOf(root.offset(6, 6, 5)));
            for (BlockPos cell : BlockPos.betweenClosed(root.offset(-4, -4, -4), root.offset(5, 5, 4))) {
                var pos = cell.immutable(); blocks.put(pos, level.getBlockState(pos));
                if (level.getBlockEntity(pos) != null) tiles.put(pos, level.getBlockEntity(pos).saveWithFullMetadata(level.registryAccess()));
            }
            level.getAllEntities().forEach(entity -> previous.add(entity.getUUID()));
            require(level.getEntities((Entity)null, bounds, entity -> entity instanceof ItemEntity || entity instanceof ExperienceOrb || entity instanceof LivingEntity).isEmpty(),
                    "owned lane contains no old living entity/item/XP to merge, damage, move or discard");
            var profile = new GameProfile(UUID.randomUUID(), "fup-" + UUID.randomUUID().toString().substring(0, 8));
            player = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
            level.getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(profile, false));
            require(player.level() == level && level.getServer().getPlayerList().getPlayer(player.getUUID()) == player, "owned real connected native actor, no shared mock/inventory");
            player.setGameMode(GameType.SURVIVAL); player.setNoGravity(true);
            for (int x = (root.getX() - 12) >> 4; x <= (root.getX() + 12) >> 4; x++)
                for (int z = (root.getZ() - 2) >> 4; z <= (root.getZ() + 2) >> 4; z++) level.getChunk(x, z);
            aim();
        }
        void require(boolean condition, String message) { test.assertTrue(condition, message); }
        ItemStack focus(ItemFocusBasic item, Object... pairs) {
            ItemStack focus = new ItemStack(item);
            for (int pair = 0; pair < pairs.length; pair += 2)
                require(item.applyUpgrade(focus, (FocusUpgradeType)pairs[pair], (Integer)pairs[pair + 1]), "fixture uses supported public upgrade application");
            return focus;
        }
        static ItemStack charged(ItemStack focus, String cap, int amount) {
            ItemStack result = new ItemStack(ThaumcraftContent.WAND.get()); var vis = new LinkedHashMap<String, Integer>();
            for (String tag : WandData.PRIMALS) vis.put(tag, amount);
            result.set(ThaumcraftContent.WAND_DATA.get(), new WandData("silverwood", cap, vis));
            if (!focus.isEmpty()) result.set(ThaumcraftFoci.WAND_FOCUS.get(), new FocusData(focus));
            return result;
        }
        void equip(ItemStack focus, String cap, int amount) {
            player.stopUsingItem(); ItemFocusExcavation.clearPlayer(player); WandFocusActions.clearCooldown(player);
            wand = charged(focus, cap, amount); player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        }
        void aim() { player.setPos(root.getX() + 3.5, root.getY() + .5 - player.getEyeHeight(), root.getZ() + .5); player.setYRot(90); player.setXRot(0); player.setDeltaMovement(Vec3.ZERO); }
        void begin() { require(player.gameMode.useItem(player, level, wand, InteractionHand.MAIN_HAND).consumesAction(), "real native RMB invokes supported focus"); }
        void tick(int count) { for (int tick = 0; tick < count; tick++) { player.setDeltaMovement(Vec3.ZERO); player.doTick(); } }
        int vis(String tag) { return ItemWandCasting.getVis(wand, tag); }
        void put(BlockPos pos, BlockState state) { level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE); }
        <T extends Projectile> List<T> projectiles(Class<T> type) {
            List<T> result = new ArrayList<>();
            level.getAllEntities().forEach(entity -> { if (type.isInstance(entity) && ((Projectile)entity).getOwner() == player) result.add(type.cast(entity)); });
            return result;
        }
        List<ItemEntity> items() { return level.getEntities((Entity)null, bounds, entity -> !previous.contains(entity.getUUID()) && entity instanceof ItemEntity).stream().map(ItemEntity.class::cast).toList(); }
        void purgeOutputs() {
            level.getEntities((Entity)null, bounds, entity -> !previous.contains(entity.getUUID())
                    && (entity instanceof ItemEntity || entity instanceof ExperienceOrb || entity instanceof Projectile projectile && projectile.getOwner() == player)).forEach(Entity::discard);
        }
        void clearLane() {
            player.stopUsingItem(); purgeOutputs();
            blocks.keySet().forEach(pos -> { level.removeBlockEntity(pos); if (!level.isEmptyBlock(pos)) put(pos, Blocks.AIR.defaultBlockState()); });
        }
        @Override public void close() {
            try {
                player.stopUsingItem(); ItemFocusExcavation.clearPlayer(player); WandFocusActions.clearCooldown(player); clearLane();
                blocks.forEach(this::put);
                tiles.forEach((pos, tag) -> { if (level.getBlockEntity(pos) != null) level.getBlockEntity(pos).loadWithComponents(tag, level.registryAccess()); });
            } finally {
                try { if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player); }
                finally { player.discard(); channel.finishAndReleaseAll(); }
            }
        }
    }
}
