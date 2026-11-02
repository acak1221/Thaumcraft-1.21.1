package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import dev.architectury.registry.menu.MenuRegistry;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.crafting.transfer.RecipeTransferResult;
import thaumcraft.api.crafting.transfer.RecipeTransferTarget;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.ShapelessArcaneRecipe;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftMasonry;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneWandRecipe;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.transfer.RecipeTransferRecipes;
import thaumcraft.common.crafting.transfer.RecipeTransfers;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.RecipeTransferNetworking;
import thaumcraft.common.lib.network.RecipeTransferPayload;
import thaumcraft.common.tiles.TileArcaneWorkbench;

/** Native source inventory, registered recipes, actual menus and registered server actors. */
public final class RecipeTransferRuntimeChecks {
    private RecipeTransferRuntimeChecks() {}

    public static void nativeFillAndPersistence(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            ItemStack wand = wand(1000);
            wand.set(DataComponents.CUSTOM_NAME, Component.literal("untouched-transfer-wand"));
            f.tile.setItem(10, wand.copy());
            f.player.getInventory().setItem(0, new ItemStack(Items.STONE, 8));
            f.player.getInventory().setItem(1, new ItemStack(ThaumcraftContent.SHARDS.get("aer").get()));
            test.assertTrue(RecipeTransferNetworking.handle(f.player, new RecipeTransferPayload(
                            f.player.inventoryMenu.containerId, id("thaumcraft:arcane/arcane_stone"), false, true)) == RecipeTransferResult.SUCCESS
                            && f.player.containerMenu instanceof ContainerArcaneWorkbench,
                    "the actual book packet handler opens a reachable native workbench and transfers real player ingredients");
            f.menu = (ContainerArcaneWorkbench) f.player.containerMenu;
            for (int slot = 0; slot < 9; slot++) test.assertTrue(f.tile.getItem(slot).getCount() == 1
                    && (slot == 4 ? f.tile.getItem(slot).is(ThaumcraftContent.SHARDS.get("aer").get()) : f.tile.getItem(slot).is(Items.STONE)),
                    "the canonical literal 3x3 pattern is placed in physical cell " + slot);
            test.assertTrue(f.player.getInventory().getItem(0).isEmpty() && f.player.getInventory().getItem(1).isEmpty()
                            && ItemStack.matches(f.tile.getItem(10), wand) && f.tile.getItem(9).isEmpty()
                            && f.menu.getCarried().isEmpty() && f.player.getInventory().countItem(ThaumcraftMasonry.ARCANE_STONE.get().asItem()) == 0,
                    "transfer moves inputs once, leaves every wand component/centivis intact, and never issues crafted output");
            var matrix = f.matrix();
            var saved = f.tile.saveWithFullMetadata(f.level.registryAccess());
            ContainerArcaneWorkbench oldMenu = f.menu;
            f.player.closeContainer();
            test.assertTrue(ItemStack.listMatches(matrix, f.matrix()), "closing the real native menu keeps the transferred shared matrix");
            f.level.removeBlockEntity(f.pos);
            var loaded = BlockEntity.loadStatic(f.pos, f.level.getBlockState(f.pos), saved, f.level.registryAccess());
            test.assertTrue(loaded instanceof TileArcaneWorkbench, "the registered block entity factory reloads a transferred native workbench");
            f.tile = (TileArcaneWorkbench) loaded;
            f.level.setBlockEntity(f.tile);
            test.assertTrue(ItemStack.listMatches(matrix, f.matrix()) && ItemStack.matches(wand, f.tile.getItem(10)),
                    "native save/load retains the exact arranged inputs and unspent wand");
            var before = f.snapshot();
            test.assertTrue(RecipeTransfers.transfer(f.player, (RecipeTransferTarget) oldMenu,
                            id("thaumcraft:arcane/arcane_stone"), true) == RecipeTransferResult.STALE_MENU
                            && ItemStack.listMatches(before, f.snapshot()),
                    "a replaced/closed workbench menu cannot rearrange its replacement");
        }
        test.succeed();
    }

    public static void maximumReuseAndComponents(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            ItemStack logs = new ItemStack(Items.OAK_LOG, 8);
            logs.set(DataComponents.CUSTOM_NAME, Component.literal("component-exact-input"));
            ItemStack tool = new ItemStack(Items.IRON_PICKAXE); tool.setDamageValue(87);
            tool.set(DataComponents.CUSTOM_NAME, Component.literal("unrelated-damaged-tool"));
            f.player.getInventory().setItem(0, logs.copy()); f.player.getInventory().setItem(1, tool.copy());
            f.tile.setItem(10, wand(600)); ItemStack beforeWand = f.tile.getItem(10).copy(); f.open();
            test.assertTrue(f.transfer(id("minecraft:oak_planks"), true) == RecipeTransferResult.SUCCESS
                            && ItemStack.matches(logs, f.tile.getItem(0)),
                    "maximum native transfer fills the available eight sets and keeps exact source components");
            test.assertTrue(f.player.getInventory().getItem(0).isEmpty() && ItemStack.matches(tool, f.player.getInventory().getItem(1))
                            && ItemStack.matches(beforeWand, f.tile.getItem(10)),
                    "unrelated damaged equipment and every stored wand primal remain unchanged");
            var before = f.snapshot();
            test.assertTrue(f.transfer(id("minecraft:oak_planks"), false) == RecipeTransferResult.SUCCESS
                            && ItemStack.listMatches(before, f.snapshot()),
                    "a one-set request reuses an already valid eight-stack matrix without clearing or shrinking it");
            f.reset(); f.player.getInventory().setItem(0, new ItemStack(Items.OAK_LOG, 64)); f.open();
            test.assertTrue(f.transfer(id("minecraft:oak_planks"), true) == RecipeTransferResult.SUCCESS
                            && f.tile.getItem(0).getCount() == 64 && f.player.getInventory().getItem(0).isEmpty(),
                    "maximum transfer respects the physical 64-item stack boundary instead of multiplying output counts");
            // Explicit native public-API fixture: eight incompatible component variants must fill nine equivalent cells.
            var constrained = ThaumcraftApi.addArcaneCraftingRecipe("", new ItemStack(Items.BARRIER),
                    new AspectList().add(Aspect.AIR, 1), "SSS", "SSS", "SSS", 'S', Items.STONE);
            try {
                var constrainedId = RecipeTransferRecipes.canonicalId(f.level, constrained.nativeRecipe()).orElseThrow();
                f.reset(); supplyVariantPool(f); f.open();
                test.assertTrue(f.transfer(constrainedId, true) == RecipeTransferResult.SUCCESS
                                && f.matrix().stream().allMatch(stack -> stack.is(Items.STONE) && stack.getCount() == 32),
                        "eight 64-item component variants cover nine equivalent native cells at a maximum of 32 sets");
                for (int variant = 0; variant < 8; variant++) {
                    ItemStack exact = variantStone(variant);
                    int preserved = f.snapshot().stream().filter(stack -> ItemStack.isSameItemSameComponents(stack, exact))
                            .mapToInt(ItemStack::getCount).sum();
                    test.assertTrue(preserved == 64, "maximum filling preserves every individual named component variant " + variant);
                }
                for (int slot = 8; slot < 36; slot++) test.assertTrue(
                        ItemStack.matches(f.player.getInventory().getItem(slot), new ItemStack(Items.COBBLESTONE, 64)),
                        "maximum planning leaves the unrelated full source cell unchanged: " + slot);
                f.reset(); supplyVariantPool(f);
                var displaced = List.of(Items.DIAMOND, Items.EMERALD, Items.IRON_INGOT, Items.GOLD_INGOT,
                        Items.COPPER_INGOT, Items.REDSTONE, Items.LAPIS_LAZULI, Items.QUARTZ, Items.AMETHYST_SHARD);
                for (int cell = 0; cell < 9; cell++) f.tile.setItem(cell, new ItemStack(displaced.get(cell)));
                f.open(); before = f.snapshot();
                test.assertTrue(f.transfer(constrainedId, true) == RecipeTransferResult.INVENTORY_FULL
                                && ItemStack.listMatches(before, f.snapshot()),
                        "a maximum search with eight variants, 28 occupied source cells and nine displaced item kinds refuses atomically");
            } finally { ThaumcraftApi.unregisterCraftingRecipe(constrained); }
        }
        test.succeed();
    }

    public static void missingFullAndUnknownArePure(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            f.player.getInventory().setItem(0, new ItemStack(Items.PAPER, 2));
            f.player.getInventory().setItem(1, new ItemStack(Items.LEATHER)); f.open();
            var before = f.snapshot();
            test.assertTrue(f.transfer(id("minecraft:book"), false) == RecipeTransferResult.MISSING_INGREDIENTS
                            && ItemStack.listMatches(before, f.snapshot()),
                    "three repeated paper ingredients cannot be paid by two items; failed planning is byte/component pure");
            test.assertTrue(f.transfer(id("thaumcraft:does_not_exist_transfer_probe"), false) == RecipeTransferResult.UNKNOWN_RECIPE
                            && ItemStack.listMatches(before, f.snapshot()),
                    "a client-supplied unknown id cannot dictate ingredients or modify any physical inventory");
            f.reset();
            for (int slot = 0; slot < 36; slot++) f.player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
            f.player.getInventory().setItem(0, new ItemStack(Items.OAK_LOG));
            f.tile.setItem(0, new ItemStack(Items.DIAMOND)); f.tile.setItem(1, new ItemStack(Items.EMERALD)); f.open();
            before = f.snapshot();
            test.assertTrue(f.transfer(id("minecraft:oak_planks"), false) == RecipeTransferResult.INVENTORY_FULL
                            && ItemStack.listMatches(before, f.snapshot()),
                    "returning two displaced matrix kinds into only one freed source cell refuses atomically, without drops or voiding");
        }
        test.succeed();
    }

    public static void researchAndWandAreUnaffected(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            f.player.getInventory().setItem(0, new ItemStack(Items.STRING, 8));
            f.player.getInventory().setItem(1, new ItemStack(Items.WHITE_WOOL, 2));
            ItemStack emptyWand = wand(0); f.tile.setItem(10, emptyWand.copy());
            KnowledgeAccess.get(f.player).removeResearch("ENCHFABRIC"); f.open();
            var before = f.snapshot();
            test.assertTrue(f.transfer(id("thaumcraft:arcane/enchanted_fabric"), false) == RecipeTransferResult.LOCKED_RESEARCH
                            && ItemStack.listMatches(before, f.snapshot()),
                    "a genuinely gated native research recipe refuses before touching player items or the matrix");
            KnowledgeAccess.get(f.player).addResearch("ENCHFABRIC");
            test.assertTrue(f.transfer(id("thaumcraft:arcane/enchanted_fabric"), false) == RecipeTransferResult.SUCCESS,
                    "known research permits ingredient arrangement even while a real inserted wand has no vis");
            for (int slot = 0; slot < 9; slot++) test.assertTrue(
                    slot == 4 ? f.tile.getItem(slot).is(Items.WHITE_WOOL)
                            : Set.of(1, 3, 5, 7).contains(slot) ? f.tile.getItem(slot).is(Items.STRING) : f.tile.getItem(slot).isEmpty(),
                    "the native enchanted-fabric pattern preserves all four literal empty corners: " + slot);
            test.assertTrue(ItemStack.matches(emptyWand, f.tile.getItem(10)) && f.menu.getCarried().isEmpty() && !f.menu.craftable(),
                    "ingredient transfer never charges/spends the wand or turns a nonfunded preview into a physical result");
            f.menu.clicked(0, 0, ClickType.PICKUP, f.player);
            test.assertTrue(f.menu.getCarried().isEmpty() && f.tile.getItem(4).getCount() == 1,
                    "normal native result authority still rejects taking the arranged recipe without vis");
            f.reset();
            var known = KnowledgeAccess.get(f.player);
            for (String research : List.of("CAP_gold", "ROD_greatwood", "CAP_iron", "ROD_wood")) known.addResearch(research);
            var prototype = ArcaneWandRecipe.displayInput("greatwood", "gold").items();
            int source = 0;
            for (ItemStack part : prototype) if (!part.isEmpty()) f.player.getInventory().setItem(source++, part.copy());
            f.tile.setItem(10, emptyWand.copy()); f.open();
            test.assertTrue(f.transfer(id("thaumcraft:wand/greatwood/gold"), false) == RecipeTransferResult.SUCCESS
                            && ItemStack.listMatches(prototype, f.matrix()) && ItemStack.matches(emptyWand, f.tile.getItem(10)),
                    "a researched dynamic wand variant resolves its canonical server parts from an empty matrix without spending vis");
            before = f.snapshot();
            test.assertTrue(f.transfer(id("thaumcraft:wand/greatwood/iron"), false) == RecipeTransferResult.MISSING_INGREDIENTS
                            && ItemStack.listMatches(before, f.snapshot()),
                    "an already valid gold-capped variant cannot incorrectly satisfy a request for unowned iron caps");
        }
        test.succeed();
    }

    public static void staleSessionsAndReachRefuse(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            f.player.getInventory().setItem(0, new ItemStack(Items.OAK_LOG)); f.open();
            var before = f.snapshot();
            var wrongWindow = new RecipeTransferPayload((f.menu.containerId + 1) % 256, id("minecraft:oak_planks"), false, false);
            test.assertTrue(RecipeTransferNetworking.handle(f.player, wrongWindow) != RecipeTransferResult.SUCCESS
                            && ItemStack.listMatches(before, f.snapshot()),
                    "the actual common packet handler rejects a stale container id with no inventory mutation");
            ServerPlayer other = f.newActor();
            var wrongActor = new RecipeTransferPayload(f.menu.containerId, id("minecraft:oak_planks"), false, false);
            test.assertTrue(RecipeTransferNetworking.handle(other, wrongActor) != RecipeTransferResult.SUCCESS
                            && RecipeTransfers.transfer(other, (RecipeTransferTarget) f.menu,
                                    id("minecraft:oak_planks"), false) == RecipeTransferResult.STALE_MENU
                            && ItemStack.listMatches(before, f.snapshot()),
                    "another registered player cannot act on this player's open workbench session");
            f.player.teleportTo(f.level, f.pos.getX() + 24.5, f.pos.getY() + 1, f.pos.getZ() + .5, 0, 0);
            test.assertTrue(RecipeTransferNetworking.handle(f.player, wrongActor) != RecipeTransferResult.SUCCESS
                            && ItemStack.listMatches(before, f.snapshot()),
                    "even a matching menu id cannot transfer after the actor leaves the actual workbench reach");
            f.player.teleportTo(f.level, f.pos.getX() + .5, f.pos.getY() + 1, f.pos.getZ() + 1.5, 0, 0);
            int closedId = f.menu.containerId; f.player.closeContainer();
            test.assertTrue(RecipeTransferNetworking.handle(f.player,
                            new RecipeTransferPayload(closedId, id("minecraft:oak_planks"), false, false)) != RecipeTransferResult.SUCCESS
                            && ItemStack.listMatches(before, f.snapshot()),
                    "a late request from the closed menu cannot arrange ingredients into a newly opened or inventory session");
        }
        test.succeed();
    }

    public static void shapedAndShapelessVanilla(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            f.player.getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 4)); f.open();
            test.assertTrue(f.transfer(id("minecraft:crafting_table"), false) == RecipeTransferResult.SUCCESS,
                    "a real native two-by-two shaped recipe transfers into the three-by-three workbench");
            for (int slot = 0; slot < 9; slot++) test.assertTrue(Set.of(0, 1, 3, 4).contains(slot)
                            ? f.tile.getItem(slot).is(Items.OAK_PLANKS) : f.tile.getItem(slot).isEmpty(),
                    "the two-wide recipe preserves row spacing and blank third column/bottom row: " + slot);
            f.reset(); f.player.getInventory().setItem(0, new ItemStack(Items.PAPER, 3));
            f.player.getInventory().setItem(1, new ItemStack(Items.LEATHER)); f.open();
            test.assertTrue(f.transfer(id("minecraft:book"), false) == RecipeTransferResult.SUCCESS,
                    "the real native shapeless book recipe pays all three paper units and one leather");
            int paper = f.matrix().stream().filter(s -> s.is(Items.PAPER)).mapToInt(ItemStack::getCount).sum();
            int leather = f.matrix().stream().filter(s -> s.is(Items.LEATHER)).mapToInt(ItemStack::getCount).sum();
            test.assertTrue(paper == 3 && leather == 1 && f.tile.getItem(10).isEmpty() && f.menu.getCarried().isEmpty()
                            && f.player.getInventory().countItem(Items.BOOK) == 0,
                    "vanilla fill needs no wand and arranges only paid inputs rather than issuing its displayed book output");
            f.reset();
            for (int slot = 0; slot < 3; slot++) f.player.getInventory().setItem(slot, new ItemStack(Items.MILK_BUCKET));
            f.player.getInventory().setItem(3, new ItemStack(Items.SUGAR, 2));
            f.player.getInventory().setItem(4, new ItemStack(Items.EGG));
            f.player.getInventory().setItem(5, new ItemStack(Items.WHEAT, 3)); f.open();
            test.assertTrue(f.transfer(id("minecraft:cake"), true) == RecipeTransferResult.SUCCESS,
                    "a maximum request transfers the native cake's three nonstackable milk buckets exactly once");
            for (int slot = 0; slot < 9; slot++) test.assertTrue(f.tile.getItem(slot).getCount() == 1
                            && f.tile.getItem(slot).is(slot < 3 ? Items.MILK_BUCKET : slot == 4 ? Items.EGG : slot < 6 ? Items.SUGAR : Items.WHEAT),
                    "the exact native cake pattern preserves distinct nonstackable paid ingredient cells " + slot);
            test.assertTrue(f.player.getInventory().countItem(Items.BUCKET) == 0
                            && f.player.getInventory().countItem(Items.CAKE) == 0 && f.menu.getCarried().isEmpty(),
                    "arranging a remainder-producing native recipe creates neither empty buckets nor crafted cake");
            // Explicit supplied API fixture: its overlap reproduces the planner's scarce-item ambiguity;
            // all vanilla/native cases above use their bundled canonical JSON, without replacing the manager.
            var overlap = ThaumcraftApi.addShapelessArcaneCraftingRecipe("", new ItemStack(Items.BARRIER),
                    new AspectList().add(Aspect.AIR, 1), Ingredient.of(Items.STONE, Items.COBBLESTONE), Items.STONE);
            try {
                var overlapId = RecipeTransferRecipes.canonicalId(f.level, overlap.nativeRecipe()).orElseThrow();
                f.reset(); f.player.getInventory().setItem(0, new ItemStack(Items.STONE));
                f.player.getInventory().setItem(1, new ItemStack(Items.COBBLESTONE)); f.open();
                test.assertTrue(f.transfer(overlapId, false) == RecipeTransferResult.SUCCESS
                                && f.tile.getItem(0).is(Items.COBBLESTONE) && f.tile.getItem(1).is(Items.STONE)
                                && overlap.nativeRecipe().matches(ArcaneRecipeInput.snapshot(f.tile), f.level),
                        "overlapping alternative/exact native shapeless ingredients reserve the scarce stone for its exact cell");
                f.reset(); f.player.getInventory().setItem(0, new ItemStack(Items.STONE)); f.open();
                var before = f.snapshot();
                test.assertTrue(f.transfer(overlapId, false) == RecipeTransferResult.MISSING_INGREDIENTS
                                && ItemStack.listMatches(before, f.snapshot()),
                        "one stone cannot pay both overlapping requirements and failed native API transfer preserves every input");
            } finally { ThaumcraftApi.unregisterCraftingRecipe(overlap); }
            int[] vetoCalls = {0};
            var veto = ThaumcraftApi.registerCraftingRecipe(new ShapelessArcaneRecipe("", new ItemStack(Items.BARRIER),
                    new AspectList().add(Aspect.AIR, 1), Items.STONE) {
                @Override public boolean matches(ArcaneRecipeInput input, Level level, Player actor) {
                    if (actor == f.player) vetoCalls[0]++;
                    return false;
                }
            });
            try {
                f.reset(); f.player.getInventory().setItem(0, new ItemStack(Items.STONE)); f.open();
                var vetoId = RecipeTransferRecipes.canonicalId(f.level, veto.nativeRecipe()).orElseThrow();
                var offered = new ArrayList<>(f.matrix()); offered.set(0, new ItemStack(Items.STONE));
                test.assertTrue(veto.nativeRecipe() != veto.nativeRecipe()
                                && veto.nativeRecipe().matches(new ArcaneRecipeInput(offered), f.level),
                        "the supplied shapeless API subclass has fresh native views with valid transferable native ingredients");
                var before = f.snapshot(); vetoCalls[0] = 0;
                test.assertTrue(RecipeTransferNetworking.handle(f.player,
                                new RecipeTransferPayload(f.menu.containerId, vetoId, false, false)) != RecipeTransferResult.SUCCESS
                                && vetoCalls[0] > 0 && ItemStack.listMatches(before, f.snapshot()),
                        "the actual open-menu packet path respects the live shapeless API callback veto and preserves every physical input");
            } finally { ThaumcraftApi.unregisterCraftingRecipe(veto); }
        }
        test.succeed();
    }

    private static ResourceLocation id(String value) { return ResourceLocation.parse(value); }
    private static ItemStack variantStone(int variant) {
        ItemStack result = new ItemStack(Items.STONE, 64);
        result.set(DataComponents.CUSTOM_NAME, Component.literal("transfer-variant-" + variant)); return result;
    }
    private static void supplyVariantPool(Fixture fixture) {
        for (int slot = 0; slot < 36; slot++) fixture.player.getInventory().setItem(slot,
                slot < 8 ? variantStone(slot) : new ItemStack(Items.COBBLESTONE, 64));
    }
    private static ItemStack wand(int amount) {
        ItemStack result = new ItemStack(ThaumcraftContent.WAND.get()); WandData data = WandData.EMPTY;
        for (String primal : WandData.PRIMALS) data = data.withAmount(primal, amount);
        result.set(ThaumcraftContent.WAND_DATA.get(), data); return result;
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper test;
        final ServerLevel level;
        final BlockPos pos;
        final net.minecraft.world.level.block.state.BlockState oldState;
        final long forcedChunk;
        final boolean ownedForce;
        final Set<UUID> oldDrops;
        final List<ServerPlayer> actors = new ArrayList<>();
        final List<EmbeddedChannel> channels = new ArrayList<>();
        final ServerPlayer player;
        TileArcaneWorkbench tile;
        ContainerArcaneWorkbench menu;

        Fixture(GameTestHelper test) {
            this.test = test; level = test.getLevel(); pos = test.absolutePos(new BlockPos(3, 2, 3)).atY(160);
            test.assertTrue(level.getBlockEntity(pos) == null, "fresh supplied transfer fixture cell has no foreign block entity");
            oldState = level.getBlockState(pos);
            forcedChunk = new ChunkPos(pos).toLong(); ownedForce = !level.getForcedChunks().contains(forcedChunk);
            if (ownedForce) level.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
            level.getChunkAt(pos);
            GameTestChunkAwait.entityTicking(test, pos);
            oldDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4)).stream().map(ItemEntity::getUUID).collect(Collectors.toSet());
            level.setBlock(pos, ThaumcraftArcane.ARCANE_WORKBENCH.get().defaultBlockState(), Block.UPDATE_ALL);
            tile = (TileArcaneWorkbench) level.getBlockEntity(pos);
            player = newActor(); KnowledgeAccess.get(player).addResearch("ARCANESTONE");
            player.getInventory().clearContent();
        }
        ServerPlayer newActor() {
            var connection = new Connection(PacketFlow.SERVERBOUND); var channel = new EmbeddedChannel(connection);
            channels.add(channel);
            var profile = new GameProfile(UUID.randomUUID(), "tct-" + UUID.randomUUID().toString().substring(0, 8));
            var actor = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
            actors.add(actor);
            level.getServer().getPlayerList().placeNewPlayer(connection, actor, CommonListenerCookie.createInitial(profile, false));
            actor.setGameMode(GameType.SURVIVAL); actor.setNoGravity(true); actor.setInvulnerable(true);
            actor.teleportTo(level, pos.getX() + .5, pos.getY() + 1, pos.getZ() + 1.5, 0, 0);
            return actor;
        }
        void open() {
            player.closeContainer(); MenuRegistry.openExtendedMenu(player, tile, data -> data.writeBlockPos(pos));
            test.assertTrue(player.containerMenu instanceof ContainerArcaneWorkbench, "the actual registered extended workbench menu opened");
            menu = (ContainerArcaneWorkbench) player.containerMenu;
        }
        void reset() { player.closeContainer(); player.getInventory().clearContent(); tile.clearContent(); }
        List<ItemStack> matrix() { return java.util.stream.IntStream.range(0, 9).mapToObj(i -> tile.getItem(i).copy()).toList(); }
        List<ItemStack> snapshot() {
            var all = new ArrayList<ItemStack>();
            for (int i = 0; i < 11; i++) all.add(tile.getItem(i).copy());
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) all.add(player.getInventory().getItem(i).copy());
            return all;
        }
        RecipeTransferResult transfer(ResourceLocation recipe, boolean maximum) {
            return RecipeTransfers.transfer(player, (RecipeTransferTarget) menu, recipe, maximum);
        }
        @Override public void close() {
            for (var actor : actors) { actor.closeContainer(); actor.getInventory().clearContent(); level.getServer().getPlayerList().remove(actor); }
            if (level.getBlockEntity(pos) instanceof TileArcaneWorkbench current) current.clearContent();
            level.setBlock(pos, oldState, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            for (var entity : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4))) if (!oldDrops.contains(entity.getUUID())) entity.discard();
            if (ownedForce) level.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, false);
            for (var channel : channels) { channel.close(); channel.finishAndReleaseAll(); }
        }
    }
}
