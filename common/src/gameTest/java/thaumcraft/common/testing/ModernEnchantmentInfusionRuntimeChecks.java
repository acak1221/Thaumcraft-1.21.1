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
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
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
import thaumcraft.common.lib.utils.ExperiencePoints;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TilePedestal;

/** Native new enchantment admission plus completely paid trident, crossbow, mace and treasure rituals. */
public final class ModernEnchantmentInfusionRuntimeChecks {
    public static final List<String> MODERN = List.of("loyalty", "impaling", "riptide", "channeling", "multishot",
            "piercing", "quick_charge", "density", "breach", "wind_burst", "soul_speed", "swift_sneak");
    private ModernEnchantmentInfusionRuntimeChecks() {}

    public static void nativeRecipesAndCompatibility(GameTestHelper test) {
        var level = test.getLevel();
        for (String name : MODERN) {
            var recipe = recipe(level, name);
            ItemStack central = central(name);
            var target = recipe.enchantment(level.registryAccess());
            List<ItemStack> parts = recipe.components().stream().map(i -> i.getItems()[0].copyWithCount(1)).toList();
            test.assertTrue(recipe.matches(new InfusionRecipeInput(central, parts), level),
                    "loaded modern JSON reaches the real native item/enchantment admission: " + name);
            test.assertTrue(!recipe.matches(new InfusionRecipeInput(new ItemStack(Items.STICK), parts), level),
                    "a modern recipe cannot enchant an ordinary incompatible item: " + name);
            ItemStack highest = central.copy(); highest.enchant(target, target.value().getMaxLevel());
            test.assertTrue(!recipe.matches(new InfusionRecipeInput(highest, parts), level), "native maximum level is preserved: " + name);
            if (target.value().getMaxLevel() > 1) {
                ItemStack next = central.copy(); next.enchant(target, 1);
                test.assertTrue(recipe.xpLevels(next, level.registryAccess()) == 2 * recipe.xpLevels(central, level.registryAccess())
                                && recipe.xpPoints(next, level.registryAccess()) == ExperiencePoints.atLevel(recipe.xpLevels(next, level.registryAccess()))
                                && recipe.xpPoints(next, level.registryAccess()) > recipe.xpPoints(central, level.registryAccess())
                                && recipe.aspectCost(next, level.registryAccess()).visSize() == 2 * recipe.aspectCost(central, level.registryAccess()).visSize(),
                        "later levels retain increasing nominal costs, converted fixed XP points and essentia: " + name);
            }
        }
        for (var pair : List.of(List.of("loyalty", "riptide"), List.of("multishot", "piercing"), List.of("density", "breach"))) {
            var first = recipe(level, pair.get(0)); var second = recipe(level, pair.get(1));
            ItemStack conflict = central(pair.get(0)); conflict.enchant(second.enchantment(level.registryAccess()), 1);
            test.assertTrue(!InfusionEnchantmentRecipe.canApply(conflict, first.enchantment(level.registryAccess())),
                    "modern incompatibilities remain native, not an infusion bypass: " + pair);
        }
        test.succeed();
    }

    public static void actualMaceAndTreasureInfusion(GameTestHelper test) {
        var fixture = new Fixture(test);
        try { fixture.prepare(); }
        catch (RuntimeException | Error failure) { fixture.close(); throw failure; }
        test.runAfterDelay(5, () -> {
            try {
                fixture.run(); test.succeed();
            } finally { fixture.close(); }
        });
    }

    private static InfusionEnchantmentRecipe recipe(ServerLevel level, String name) {
        return level.getRecipeManager().getAllRecipesFor(ThaumcraftInfusion.ENCHANTMENT_RECIPE_TYPE.get()).stream()
                .filter(holder -> holder.id().equals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "infusion_enchantment/modern/" + name)))
                .findFirst().orElseThrow().value();
    }
    private static ItemStack central(String name) {
        return new ItemStack(switch (name) {
            case "loyalty", "impaling", "riptide", "channeling" -> Items.TRIDENT;
            case "multishot", "piercing", "quick_charge" -> Items.CROSSBOW;
            case "density", "breach", "wind_burst" -> Items.MACE;
            case "soul_speed" -> Items.DIAMOND_BOOTS;
            case "swift_sneak" -> Items.DIAMOND_LEGGINGS;
            default -> throw new IllegalArgumentException(name);
        });
    }

    private static final class Fixture implements AutoCloseable {
        static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        final GameTestHelper test;
        final ServerLevel level;
        final BlockPos centerPos, matrixPos;
        final Map<BlockPos, BlockState> old = new LinkedHashMap<>();
        final List<ChunkPos> forced = new ArrayList<>();
        final List<TilePedestal> componentPedestals = new ArrayList<>();
        final Map<Aspect, TileJar> jars = new LinkedHashMap<>();
        final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        final EmbeddedChannel channel = new EmbeddedChannel(connection);
        final ServerPlayer actor;
        TileInfusionMatrix matrix;
        TilePedestal center;
        boolean closed;
        Fixture(GameTestHelper test) {
            this.test = test; level = test.getLevel(); centerPos = test.absolutePos(new BlockPos(3, 80, 3)); matrixPos = centerPos.above(2);
            actor = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "modernench-" + UUID.randomUUID().toString().substring(0, 5)), ClientInformation.createDefault());
        }
        void place(BlockPos pos, BlockState state) { old.putIfAbsent(pos, level.getBlockState(pos)); level.setBlock(pos, state, QUIET); }
        void prepare() {
            forced.addAll(GolemChunks.hold(level, List.of(centerPos.offset(-13, 0, -13), centerPos.offset(13, 3, 13))));
            for (int x = -12; x <= 12; x++) for (int z = -12; z <= 12; z++) {
                place(centerPos.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                place(centerPos.offset(x, 0, z), Math.abs(x) <= 1 && Math.abs(z) <= 1 ? Blocks.AIR.defaultBlockState() : Blocks.SKELETON_SKULL.defaultBlockState());
            }
            place(centerPos, ThaumcraftInfusion.PEDESTAL.get().defaultBlockState());
            place(matrixPos, ThaumcraftInfusion.MATRIX.get().defaultBlockState());
            for (int x : new int[]{-1, 1}) for (int z : new int[]{-1, 1}) {
                place(centerPos.offset(x, 0, z), ThaumcraftInfusion.PILLAR.get().defaultBlockState().setValue(BlockInfusionPillar.HALF, DoubleBlockHalf.LOWER));
                place(centerPos.offset(x, 1, z), ThaumcraftInfusion.PILLAR.get().defaultBlockState().setValue(BlockInfusionPillar.HALF, DoubleBlockHalf.UPPER));
            }
            for (var pos : List.of(centerPos.west(3), centerPos.east(3), centerPos.north(3), centerPos.south(3))) {
                place(pos, ThaumcraftInfusion.PEDESTAL.get().defaultBlockState());
                componentPedestals.add((TilePedestal) level.getBlockEntity(pos));
            }
            center = (TilePedestal) level.getBlockEntity(centerPos);
            matrix = (TileInfusionMatrix) level.getBlockEntity(matrixPos);
            int index = 0;
            for (Aspect aspect : List.of(Aspect.MAGIC, Aspect.EARTH, Aspect.WEAPON, Aspect.AIR, Aspect.MOTION, Aspect.TRAVEL, Aspect.SENSES)) {
                BlockPos pos = centerPos.offset(-8 + index++, 0, 9);
                place(pos, ThaumcraftContent.JAR.get().defaultBlockState());
                TileJar jar = (TileJar) level.getBlockEntity(pos);
                test.assertTrue(jar.addToContainer(aspect, 64) == 0, "finite physical source filled once for " + aspect.getTag()); jars.put(aspect, jar);
            }
        }
        void run() {
            level.getServer().getPlayerList().placeNewPlayer(connection, actor, CommonListenerCookie.createInitial(actor.getGameProfile(), false));
            actor.setGameMode(GameType.SURVIVAL); actor.setNoGravity(true); actor.setPos(Vec3.atCenterOf(matrixPos).add(0, 0, 2));
            actor.experienceLevel = 0; actor.experienceProgress = 0; actor.totalExperience = 0;
            actor.giveExperiencePoints(Math.toIntExact(ExperiencePoints.atLevel(100)));
            actor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftContent.WAND.get()));
            var hit = new BlockHitResult(Vec3.atCenterOf(matrixPos), Direction.UP, matrixPos, false);
            test.assertTrue(actor.getMainHandItem().getItem().useOn(new UseOnContext(actor, InteractionHand.MAIN_HAND, hit)).consumesAction()
                    && matrix.isActive(), "real wand forms the native altar for modern rituals");
            craft("density", "MODERNINFUSION", hit);
            craft("loyalty", "MODERNINFUSION", hit);
            craft("quick_charge", "MODERNINFUSION", hit);
            craft("wind_burst", "DEEPINFUSION", hit);
        }
        void craft(String name, String research, BlockHitResult hit) {
            var recipe = recipe(level, name); var target = recipe.enchantment(level.registryAccess());
            ItemStack input = central(name); input.setDamageValue(7); input.set(DataComponents.CUSTOM_NAME, Component.literal("Paid " + name));
            center.setItem(0, input);
            for (int i = 0; i < componentPedestals.size(); i++) componentPedestals.get(i).setItem(0,
                    i < recipe.components().size() ? recipe.components().get(i).getItems()[0].copyWithCount(1) : ItemStack.EMPTY);
            long xp = ExperiencePoints.available(actor);
            int xpPrice = recipe.xpPoints(input, level.registryAccess());
            var cost = recipe.aspectCost(input, level.registryAccess());
            Map<Aspect, Integer> before = new LinkedHashMap<>(); jars.forEach((aspect, jar) -> before.put(aspect, jar.getAmount()));
            if (!KnowledgeAccess.get(actor).isResearchComplete(research)) test.assertTrue(!matrix.startCrafting(actor) && ExperiencePoints.available(actor) == xp
                            && actor.totalExperience == xp
                            && componentPedestals.stream().filter(p -> !p.isEmpty()).count() == recipe.components().size(),
                    "unresearched modern ritual refuses without upfront XP or component payment: " + name);
            KnowledgeAccess.get(actor).addResearch(research);
            test.assertTrue(actor.getMainHandItem().getItem().useOn(new UseOnContext(actor, InteractionHand.MAIN_HAND, hit)).consumesAction()
                            && matrix.isCrafting() && matrix.getRecipeType() == 1,
                    "the actual wand starts the researched modern enchantment ritual: " + name);
            test.assertTrue(ExperiencePoints.available(actor) == xp && actor.totalExperience == xp && matrix.getRemainingXPPoints() == xpPrice,
                    "start records the full fixed point price without upfront debit or an unpaid result");
            for (int tick = 0; tick < 10000 && matrix.isCrafting(); tick++) TileInfusionMatrix.tick(level, matrixPos, level.getBlockState(matrixPos), matrix);
            ItemStack result = center.getItem(0);
            test.assertTrue(!matrix.isCrafting() && result.is(input.getItem()) && result.getEnchantments().getLevel(target) == 1
                            && result.getDamageValue() == 7 && result.getHoverName().equals(input.getHoverName()),
                    "paid native ritual adds exactly one enchantment level to the retained item: " + name);
            test.assertTrue(ExperiencePoints.available(actor) == xp - xpPrice && actor.totalExperience == xp - xpPrice
                            && componentPedestals.stream().allMatch(TilePedestal::isEmpty), "exact native XP points and every physical ingredient paid once: " + name);
            jars.forEach((aspect, jar) -> test.assertTrue(jar.getAmount() == before.get(aspect) - cost.getAmount(aspect),
                    "physical essence debit remains exact for " + name + "/" + aspect.getTag()));
            for (int tick = 0; tick < 40; tick++) TileInfusionMatrix.tick(level, matrixPos, level.getBlockState(matrixPos), matrix);
            test.assertTrue(result.getEnchantments().getLevel(target) == 1
                            && ExperiencePoints.available(actor) == xp - xpPrice && actor.totalExperience == xp - xpPrice,
                    "completed modern rituals never replay their result or debit");
            actor.getInventory().add(center.getItem(0).copy()); center.setItem(0, ItemStack.EMPTY);
        }
        @Override public void close() {
            if (closed) return; closed = true;
            if (matrix != null && !matrix.isRemoved()) {
                var tag = matrix.saveWithFullMetadata(level.registryAccess()); tag.putBoolean("crafting", false); tag.putBoolean("active", false); matrix.loadWithComponents(tag, level.registryAccess());
            }
            if (center != null && !center.isRemoved()) center.setInfusionStack(ItemStack.EMPTY);
            for (var pedestal : componentPedestals) if (!pedestal.isRemoved()) pedestal.setInfusionStack(ItemStack.EMPTY);
            old.forEach((pos, state) -> level.setBlock(pos, state, QUIET));
            for (var chunk : forced) level.setChunkForced(chunk.x, chunk.z, false);
            if (level.getServer().getPlayerList().getPlayer(actor.getUUID()) == actor) level.getServer().getPlayerList().remove(actor);
            actor.discard(); channel.finishAndReleaseAll();
        }
    }
}
