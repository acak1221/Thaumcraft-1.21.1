package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.BlockInfusionPillar;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.crafting.InfusionEnchantmentRecipe;
import thaumcraft.common.crafting.InfusionRecipeInput;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TilePedestal;

/** Declared formed altar, stabilizers, raw components, levels and finite essence are fixtures. Both custom enchantments must be produced by natural altar ticks. */
public final class CustomEnchantmentInfusionRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private CustomEnchantmentInfusionRuntimeChecks() {}
    public static void run(GameTestHelper test) {
        Check check = new Check(test);
        try { check.prepare(); check.observe(); } catch (RuntimeException | Error failure) { check.finish(); throw failure; }
        test.runAfterDelay(5, () -> {
            if (check.started) return;
            check.started = true;
            try { check.startRepair(); }
            catch (RuntimeException | Error failure) { check.finish(); throw failure; }
        });
    }
    private static final class Check {
        final GameTestHelper test;
        final ServerLevel level;
        final BlockPos centerPos, matrixPos;
        final Map<BlockPos, BlockState> oldStates = new LinkedHashMap<>();
        final List<ChunkPos> forced = new ArrayList<>();
        final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        final EmbeddedChannel channel = new EmbeddedChannel(connection);
        final ServerPlayer player;
        TileInfusionMatrix matrix;
        TilePedestal center, first, second;
        TileJar magic, weapon;
        boolean started, naturalStarted, finished;
        Check(GameTestHelper test) {
            this.test = test; level = test.getLevel(); centerPos = test.absolutePos(new BlockPos(3, 80, 3)); matrixPos = centerPos.above(2);
            player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "ench-" + UUID.randomUUID().toString().substring(0, 8)), ClientInformation.createDefault());
        }
        void set(BlockPos pos, BlockState state) { oldStates.putIfAbsent(pos, level.getBlockState(pos)); level.setBlock(pos, state, QUIET); }
        void prepare() {
            forced.addAll(GolemChunks.hold(level, List.of(centerPos.offset(-13, 0, -13), centerPos.offset(13, 3, 13))));
            for (int x = -12; x <= 12; x++) for (int z = -12; z <= 12; z++) {
                set(centerPos.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                set(centerPos.offset(x, 0, z), Math.abs(x) <= 1 && Math.abs(z) <= 1 ? Blocks.AIR.defaultBlockState() : Blocks.SKELETON_SKULL.defaultBlockState());
            }
            set(centerPos, ThaumcraftInfusion.PEDESTAL.get().defaultBlockState());
            set(matrixPos, ThaumcraftInfusion.MATRIX.get().defaultBlockState());
            for (int x : new int[]{-1,1}) for (int z : new int[]{-1,1}) {
                set(centerPos.offset(x, 0, z), ThaumcraftInfusion.PILLAR.get().defaultBlockState().setValue(BlockInfusionPillar.HALF, DoubleBlockHalf.LOWER));
                set(centerPos.offset(x, 1, z), ThaumcraftInfusion.PILLAR.get().defaultBlockState().setValue(BlockInfusionPillar.HALF, DoubleBlockHalf.UPPER));
            }
            set(centerPos.west(3), ThaumcraftInfusion.PEDESTAL.get().defaultBlockState());
            set(centerPos.east(3), ThaumcraftInfusion.PEDESTAL.get().defaultBlockState());
            set(centerPos.offset(-8, 0, 9), ThaumcraftContent.JAR.get().defaultBlockState());
            set(centerPos.offset(-7, 0, 9), ThaumcraftContent.JAR.get().defaultBlockState());
            center = (TilePedestal) level.getBlockEntity(centerPos);
            first = (TilePedestal) level.getBlockEntity(centerPos.west(3)); second = (TilePedestal) level.getBlockEntity(centerPos.east(3));
            matrix = (TileInfusionMatrix) level.getBlockEntity(matrixPos);
            magic = (TileJar) level.getBlockEntity(centerPos.offset(-8, 0, 9)); weapon = (TileJar) level.getBlockEntity(centerPos.offset(-7, 0, 9));
        }
        int phase;
        TileJar craft, order, travel, flight;
        void startRepair() {
            level.getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
            player.setGameMode(GameType.SURVIVAL); player.setNoGravity(true); player.setPos(Vec3.atCenterOf(matrixPos).add(0, 0, 2)); player.giveExperienceLevels(16);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftContent.WAND.get()));
            useWand(); test.assertTrue(matrix.isActive(), "the actual wand activates the formed custom-infusion altar");
            KnowledgeAccess.get(player).addResearch("INFUSIONENCHANTMENT");
            craft = jar(-6, Aspect.CRAFT); order = jar(-5, Aspect.ORDER); travel = jar(-4, Aspect.TRAVEL); flight = jar(-3, Aspect.FLIGHT);
            test.assertTrue(magic.addToContainer(Aspect.MAGIC, 32) == 0, "finite magic essentia for both rituals");
            center.setInfusionStack(new ItemStack(thaumcraft.common.config.ThaumcraftEquipment.THAUMIUM_SWORD.get()));
            first.setInfusionStack(new ItemStack(Items.ANVIL)); second.setInfusionStack(material("salis_mundus"));
            useWand(); test.assertTrue(matrix.isCrafting() && matrix.getRemainingXPLevels() == 6 && player.experienceLevel == 16, "Repair starts at six real XP levels with no up-front debit");
            naturalStarted = true;
        }
        void observe() {
            // Register onEachTick before the scheduled-task iterator exists; never rehash it from the delayed start.
            test.onEachTick(() -> {
                if (finished || !naturalStarted) return;
                try {
                    int initialLevels = phase == 0 ? 16 : 10, debt = phase == 0 ? 6 : 5;
                    test.assertTrue(player.experienceLevel + debt - matrix.getRemainingXPLevels() == initialLevels,
                            "each custom XP level is paired with exactly one session progress debit");
                    if (matrix.getRemainingXPLevels() > 0) test.assertTrue(magic.getAmount() == (phase == 0 ? 32 : 24)
                            && first.getItem(0).is(phase == 0 ? Items.ANVIL : material("nitor").getItem()) && !second.isEmpty(),
                            "custom XP is paid completely before any physical essence/component payment");
                    if (matrix.isCrafting()) return;
                    if (phase == 0) {
                        var repair = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(thaumcraft.common.lib.enchantment.EquipmentEnchantments.REPAIR);
                        test.assertTrue(center.getItem(0).is(thaumcraft.common.config.ThaumcraftEquipment.THAUMIUM_SWORD.get())
                                && center.getItem(0).getEnchantments().getLevel(repair) == 1 && player.experienceLevel == 10
                                && magic.getAmount() == 24 && craft.getAmount() == 22 && order.getAmount() == 22 && first.isEmpty() && second.isEmpty(),
                                "natural Repair infusion adds its real enchantment after six levels, twenty-eight essentia and both actual components");
                        player.getInventory().add(center.getItem(0).copy());
                        center.setInfusionStack(new ItemStack(Items.LEATHER_BOOTS)); first.setInfusionStack(material("nitor")); second.setInfusionStack(material("salis_mundus"));
                        phase = 1; useWand();
                        test.assertTrue(matrix.isCrafting() && matrix.getRemainingXPLevels() == 5, "real wand starts Haste on actual boots");
                    } else {
                        var haste = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(thaumcraft.common.lib.enchantment.EquipmentEnchantments.HASTE);
                        test.assertTrue(center.getItem(0).is(Items.LEATHER_BOOTS) && center.getItem(0).getEnchantments().getLevel(haste) == 1
                                && player.experienceLevel == 5 && magic.getAmount() == 20 && travel.getAmount() == 24 && flight.getAmount() == 24
                                && craft.getAmount() == 22 && order.getAmount() == 22 && first.isEmpty() && second.isEmpty(),
                                "natural Haste infusion adds its real enchantment after five levels, twenty essentia and both actual components");
                        finish(); test.succeed();
                    }
                } catch (RuntimeException | Error failure) { finish(); throw failure; }
            });
            test.runAfterDelay(1500, () -> { if (!finished) { finish(); test.fail("custom altar rituals did not complete on ordinary world ticks, phase=" + phase); } });
        }
        ItemStack material(String path) { return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thaumcraft", path))); }
        TileJar jar(int x, Aspect aspect) {
            BlockPos pos = centerPos.offset(x, 0, 9); set(pos, ThaumcraftContent.JAR.get().defaultBlockState());
            TileJar jar = (TileJar)level.getBlockEntity(pos); test.assertTrue(jar.addToContainer(aspect, 32) == 0, "declared finite jar for " + aspect.getTag()); return jar;
        }
        void useWand() {
            var hit = new BlockHitResult(Vec3.atCenterOf(matrixPos), Direction.UP, matrixPos, false);
            test.assertTrue(player.getMainHandItem().getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(), "real wand handles altar action");
        }
        void finish() {
            if (finished) return; finished = true;
            try {
                if (matrix != null && !matrix.isRemoved()) {
                    var tag = matrix.saveWithFullMetadata(level.registryAccess()); tag.putBoolean("crafting", false); tag.putBoolean("active", false); matrix.loadWithComponents(tag, level.registryAccess());
                }
                // These are declared fixtures; remove their contents before pedestal removal can evacuate them.
                for (TilePedestal pedestal : new TilePedestal[]{center, first, second}) {
                    if (pedestal != null && !pedestal.isRemoved()) pedestal.setInfusionStack(ItemStack.EMPTY);
                }
                oldStates.forEach((pos, state) -> level.setBlock(pos, state, QUIET));
                for (ChunkPos pos : forced) level.setChunkForced(pos.x, pos.z, false);
            } finally {
                if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player);
                player.discard(); channel.finishAndReleaseAll();
            }
        }
    }
}
