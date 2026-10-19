package thaumcraft.common.testing;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.util.ReferenceCountUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.config.ThaumcraftAlembics;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftLabels;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneCrafting;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.crafting.ArcaneShapelessRecipe;
import thaumcraft.common.items.ItemJar;
import thaumcraft.common.items.JarLabelData;
import thaumcraft.common.items.relics.ItemResonator;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.essentia.TubeGeometry;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileArcaneWorkbench;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TileTube;

/** Two focused native cases; root owns wrappers/execution. No recipes are injected.
 * Raw materials, charged iron wand, research and inspector stock are explicit
 * SERVER INPUT fixtures, not survival acquisition or inspected client evidence.
 * An owned connected native actor avoids GameTest's always-creative mock. */
public final class ShapelessResonatorRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private ShapelessResonatorRuntimeChecks() {}

    public static void shapelessArcaneUsesManagerMenuAndPayment(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            f.open();
            var holder = f.level.getRecipeManager().byKey(id("arcane/essentia_valve")).orElseThrow();
            f.check(holder.value() instanceof ArcaneShapelessRecipe, "real manager loads a genuinely shapeless arcane valve, not shaped permutations");
            var recipe = (ArcaneShapelessRecipe) holder.value();
            f.check(recipe.getSerializer() == ThaumcraftArcane.ARCANE_SHAPELESS_SERIALIZER.get()
                    && recipe.getType() == ThaumcraftArcane.ARCANE_RECIPE_TYPE.get() && recipe.research().equals("TUBES")
                    && recipe.vis().equals(Map.of("aqua", 5, "ordo", 5)) && recipe.getIngredients().size() == 2,
                    "original TUBES gate, two ingredients and aqua5/ordo5 whole-vis cost are native recipe data");
            f.check(recipe.getResultItem(f.level.registryAccess()).is(ThaumcraftTubes.ESSENTIA_VALVE_ITEM.get())
                    && recipe.getResultItem(f.level.registryAccess()).getCount() == 1, "one original valve output");
            for (int tube = 0; tube < 9; tube++) for (int lever = 0; lever < 9; lever++) {
                if (tube == lever) continue;
                ArcaneRecipeInput input = valveInput(tube, lever, 3);
                var found = f.level.getRecipeManager().getRecipeFor(ThaumcraftArcane.ARCANE_RECIPE_TYPE.get(), input, f.level);
                f.check(found.isPresent() && found.get().id().equals(holder.id()) && recipe.matches(input, f.level),
                        "actual shapeless manager accepts arbitrary distinct slots " + tube + "/" + lever);
            }
            var wrong = new ArrayList<>(valveInput(1, 8, 2).items());
            wrong.set(4, new ItemStack(ThaumcraftTubes.ESSENTIA_TUBE_ITEM.get()));
            f.check(!recipe.matches(new ArcaneRecipeInput(wrong), f.level), "an extra otherwise matching input is refused");
            wrong.set(4, ItemStack.EMPTY); wrong.set(1, new ItemStack(ThaumcraftTubes.ESSENTIA_VALVE_ITEM.get()));
            f.check(!recipe.matches(new ArcaneRecipeInput(wrong), f.level), "flattened valve variant cannot substitute for the plain tube ingredient");
            wrong.set(1, new ItemStack(ThaumcraftTubes.ESSENTIA_TUBE_ITEM.get())); wrong.set(8, new ItemStack(Items.STONE_BUTTON));
            f.check(!recipe.matches(new ArcaneRecipeInput(wrong), f.level), "a button is not the original lever");
            matchingQuirks(f);
            codecs(f, recipe, holder.id());

            f.level.setBlock(f.base, ThaumcraftArcane.ARCANE_WORKBENCH.get().defaultBlockState(), QUIET);
            var tile = (TileArcaneWorkbench) f.level.getBlockEntity(f.base);
            f.aim(Vec3.atCenterOf(f.base));
            f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            f.check(f.use(f.base, Direction.UP, InteractionHand.MAIN_HAND).consumesAction()
                    && f.player.containerMenu instanceof ContainerArcaneWorkbench, "native block interaction opens the real arcane menu");
            var menu = (ContainerArcaneWorkbench) f.player.containerMenu;
            fillValve(tile, 8, 1, 3);
            tile.setItem(10, wand(1650));
            KnowledgeAccess.get(f.player).removeResearch("TUBES");
            menu.broadcastChanges();
            f.check(menu.getSlot(0).getItem().isEmpty(), "unknown TUBES has no shapeless preview");
            refuse(f, tile, menu, "missing research leaves all real materials and vis untouched");
            KnowledgeAccess.get(f.player).addResearch("TUBES");
            tile.setItem(4, new ItemStack(Items.LEVER)); menu.broadcastChanges();
            f.check(menu.getSlot(0).getItem().isEmpty(), "extra occupied native menu cell invalidates the shapeless preview");
            refuse(f, tile, menu, "extra native input refuses without paying or consuming ANY cell");
            tile.setItem(4, ItemStack.EMPTY); tile.setItem(8, new ItemStack(ThaumcraftTubes.ESSENTIA_VALVE_ITEM.get(), 3)); menu.broadcastChanges();
            f.check(menu.getSlot(0).getItem().isEmpty(), "wrong flattened tube variant has no native preview");
            refuse(f, tile, menu, "wrong variant is refused by the authoritative menu, not merely a predicate fixture");
            tile.setItem(8, new ItemStack(ThaumcraftTubes.ESSENTIA_TUBE_ITEM.get(), 3));
            tile.setItem(10, ItemStack.EMPTY); menu.broadcastChanges();
            f.check(menu.getSlot(0).getItem().is(ThaumcraftTubes.ESSENTIA_VALVE_ITEM.get()) && !menu.craftable(), "researched shapeless preview without a wand cannot be taken");
            refuse(f, tile, menu, "missing wand never turns a preview into a free valve");
            ItemStack shortWand = wand(1650);
            shortWand.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(shortWand).withAmount("ordo", 549));
            tile.setItem(10, shortWand); menu.broadcastChanges();
            refuse(f, tile, menu, "one short primal refuses the complete shapeless payment");
            tile.setItem(10, wand(1650)); menu.broadcastChanges();
            var match = ArcaneCrafting.find(f.player, tile);
            f.check(match != null && match.arcane() && match.recipe().id().equals(holder.id()) && menu.craftable()
                    && menu.requiredVis("aqua") == 550 && menu.requiredVis("ordo") == 550 && menu.requiredVis("aer") == 0
                    && match.remainders().size() == 9 && match.remainders().stream().allMatch(ItemStack::isEmpty),
                    "generic ArcaneCrafting naturally supplies original iron-cap550/550 prices and nine empty remainders");
            ItemStack first = take(menu, f.player);
            f.check(first.is(ThaumcraftTubes.ESSENTIA_VALVE_ITEM.get()) && first.getCount() == 1
                    && tile.getItem(8).getCount() == 2 && tile.getItem(1).getCount() == 2 && tile.getItem(9).isEmpty(),
                    "one paid pickup consumes exactly one from EACH occupied cell and never persists a result");
            paidVis(f, tile, 1100, "single shapeless take");
            menu.broadcastChanges(); KnowledgeAccess.get(f.player).removeResearch("TUBES");
            var before = tile.saveWithoutMetadata(f.level.registryAccess());
            f.check(menu.quickMoveStack(f.player, 0).isEmpty() && before.equals(tile.saveWithoutMetadata(f.level.registryAccess())), "stale funded preview rechecks actual actor research at take");
            KnowledgeAccess.get(f.player).addResearch("TUBES");
            for (int slot = 0; slot < 36; slot++) f.player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
            menu.broadcastChanges(); before = tile.saveWithoutMetadata(f.level.registryAccess());
            f.check(menu.quickMoveStack(f.player, 0).isEmpty() && before.equals(tile.saveWithoutMetadata(f.level.registryAccess())), "full native inventory refuses quick-move without paying or consuming");
            f.player.getInventory().clearContent();
            menu.clicked(0, 0, ClickType.QUICK_MOVE, f.player);
            f.check(f.player.getInventory().countItem(ThaumcraftTubes.ESSENTIA_VALVE_ITEM.get()) == 2
                    && tile.getItem(8).isEmpty() && tile.getItem(1).isEmpty(), "native shift-result repeats exactly the two remaining funded shapeless crafts");
            paidVis(f, tile, 0, "two native shift crafts");
            before = tile.saveWithoutMetadata(f.level.registryAccess());
            f.check(menu.quickMoveStack(f.player, 0).isEmpty() && before.equals(tile.saveWithoutMetadata(f.level.registryAccess())), "replayed shapeless extraction cannot duplicate output or debit vis");

            // The real valve has no container item. Exercise unchanged shared-menu
            // remainder delivery with an ACTUAL vanilla recipe, never inject a
            // synthetic remainder-bearing arcane recipe into the manager.
            tile.clearContent();
            Item[] cake = {Items.MILK_BUCKET, Items.MILK_BUCKET, Items.MILK_BUCKET, Items.SUGAR, Items.EGG, Items.SUGAR, Items.WHEAT, Items.WHEAT, Items.WHEAT};
            for (int slot = 0; slot < 9; slot++) tile.setItem(slot, new ItemStack(cake[slot]));
            menu.broadcastChanges(); int buckets = f.player.getInventory().countItem(Items.BUCKET);
            f.check(menu.quickMoveStack(f.player, 0).is(Items.CAKE) && f.player.getInventory().countItem(Items.BUCKET) == buckets + 3
                    && ArcaneRecipeInput.snapshot(tile).items().stream().allMatch(ItemStack::isEmpty), "shared native menu still delivers three real cake buckets exactly once, with no arcane wand/payment");
            craftResonator(f, tile, menu);
        }
        test.succeed();
    }

    public static void resonatorInspectsTransportsWithoutMutation(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            f.open(); KnowledgeAccess.get(f.player).removeResearch("TUBES");
            ItemStack tool = new ItemStack(ThaumcraftContent.RESONATOR.get());
            f.check(tool.getItem() instanceof ItemResonator && tool.getMaxStackSize() == 1 && !tool.isDamageableItem()
                    && tool.getRarity() == Rarity.UNCOMMON && !tool.hasFoil() && tool.getComponentsPatch().isEmpty(),
                    "plain original resonator is uncommon, stack1, nondamageable and has no fabricated glint data");
            ItemStack named = tool.copy(); named.set(DataComponents.CUSTOM_NAME, Component.literal("resonator-component-presence"));
            ItemStack tagged = tool.copy(); tagged.set(DataComponents.CUSTOM_DATA, CustomData.of(new CompoundTag()));
            f.check(named.hasFoil() && tagged.hasFoil() && ItemStack.matches(named, ItemStack.parse(f.level.registryAccess(), named.save(f.level.registryAccess())).orElseThrow()),
                    "native persisted component PRESENCE (even empty custom data), not enchantments, replaces original NBT-presence glint");
            f.player.setItemInHand(InteractionHand.MAIN_HAND, named);
            BlockPos jarPos = f.base, alembicPos = f.base.east(2), tubePos = f.base.above(2), valvePos = tubePos.east(2);
            f.level.setBlock(jarPos, ThaumcraftContent.JAR.get().defaultBlockState(), QUIET);
            f.level.setBlock(alembicPos, ThaumcraftAlembics.ALEMBIC.get().defaultBlockState(), QUIET);
            f.level.setBlock(tubePos, ThaumcraftTubes.ESSENTIA_TUBE.get().defaultBlockState(), QUIET);
            f.level.setBlock(tubePos.above(), ThaumcraftTubes.ESSENTIA_TUBE.get().defaultBlockState(), QUIET);
            f.level.setBlock(valvePos, ThaumcraftTubes.ESSENTIA_VALVE.get().defaultBlockState(), QUIET);
            var jar = (TileJar) f.level.getBlockEntity(jarPos); jar.setFilter(Aspect.WATER); jar.addToContainer(Aspect.WATER, 8);
            var alembic = (TileAlembic) f.level.getBlockEntity(alembicPos); alembic.setFilter(Aspect.FIRE); alembic.addToContainer(Aspect.FIRE, 8);
            var tube = (TileTube) f.level.getBlockEntity(tubePos); tube.addEssentia(Aspect.WATER, 1, Direction.UP); tube.setSuction(null, 17);
            var valve = (TileTube) f.level.getBlockEntity(valvePos); valve.addEssentia(Aspect.FIRE, 1, Direction.UP); valve.setSuction(Aspect.FIRE, 12);
            for (boolean secondary : new boolean[] {false, true}) {
                f.player.setShiftKeyDown(secondary);
                f.aim(Vec3.atLowerCornerOf(jarPos).add(.5, .4, .5));
                f.inspect(jarPos, jar.getFacing(), InteractionHand.MAIN_HAND, Aspect.WATER, 8, Aspect.WATER, 64,
                        () -> f.use(jarPos, jar.getFacing(), InteractionHand.MAIN_HAND));
                f.check(jar.getFilter() == Aspect.WATER && jar.getAmount() == 8, "normal/secondary resonator never removes a jar label or dumps contents");
                f.aim(Vec3.atCenterOf(alembicPos));
                f.inspect(alembicPos, Direction.SOUTH, InteractionHand.MAIN_HAND, Aspect.FIRE, 8, null, 0,
                        () -> f.use(alembicPos, Direction.SOUTH, InteractionHand.MAIN_HAND));
                f.check(alembic.getFilter() == Aspect.FIRE && alembic.getAmount() == 8, "normal/secondary resonator never removes an alembic label, changes facing or empties it");
                f.aim(Vec3.atLowerCornerOf(tubePos).add(.5, .85, .5));
                var arm = TubeGeometry.retrace(f.player, tube);
                f.check(arm != null && arm.index() == Direction.UP.get3DDataValue(), "actual indexed upper arm overrides a DIFFERENT supplied clicked face");
                f.inspect(tubePos, Direction.WEST, InteractionHand.MAIN_HAND, Aspect.WATER, 1, null, 17,
                        () -> f.use(tubePos, Direction.WEST, InteractionHand.MAIN_HAND));
                f.aim(Vec3.atCenterOf(valvePos));
                f.check(TubeGeometry.retrace(f.player, valve) != null && TubeGeometry.retrace(f.player, valve).index() == 6, "real valve core keeps the supplied clicked face");
                f.inspect(valvePos, Direction.SOUTH, InteractionHand.MAIN_HAND, Aspect.FIRE, 1, Aspect.FIRE, 12,
                        () -> f.use(valvePos, Direction.SOUTH, InteractionHand.MAIN_HAND));
            }
            f.player.setShiftKeyDown(true); f.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            f.player.setItemInHand(InteractionHand.OFF_HAND, named); f.aim(Vec3.atCenterOf(alembicPos));
            f.inspect(alembicPos, Direction.DOWN, InteractionHand.OFF_HAND, Aspect.FIRE, 8, null, 0,
                    () -> f.use(alembicPos, Direction.DOWN, InteractionHand.OFF_HAND));
            f.player.setItemInHand(InteractionHand.MAIN_HAND, named); f.player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            tube.takeEssentia(Aspect.WATER, 1, Direction.UP); tube.setSuction(null, 0); f.aim(Vec3.atCenterOf(tubePos));
            f.inspect(tubePos, Direction.EAST, InteractionHand.MAIN_HAND, null, 0, null, 0,
                    () -> f.use(tubePos, Direction.EAST, InteractionHand.MAIN_HAND));
            jar.clearContents(); f.aim(Vec3.atLowerCornerOf(jarPos).add(.5, .4, .5));
            f.inspect(jarPos, Direction.SOUTH, InteractionHand.MAIN_HAND, Aspect.WATER, 0, Aspect.WATER, 64,
                    () -> f.use(jarPos, Direction.SOUTH, InteractionHand.MAIN_HAND));
            f.check(!KnowledgeAccess.get(f.player).isResearchComplete("TUBES"), "all free inspections worked WITHOUT the recipe's TUBES research or any wand/vis");
            transportAndAuthority(f, jarPos, named);
        }
        test.succeed();
    }

    private static void matchingQuirks(Fixture f) {
        var overlapping = new ArcaneShapelessRecipe("", List.of(Ingredient.of(Items.OAK_PLANKS, Items.BIRCH_PLANKS), Ingredient.of(Items.OAK_PLANKS)), new ItemStack(Items.STONE), Map.of("aer", 1));
        var grid = NonNullList.withSize(9, ItemStack.EMPTY);
        grid.set(0, new ItemStack(Items.OAK_PLANKS)); grid.set(8, new ItemStack(Items.BIRCH_PLANKS));
        f.check(!overlapping.matches(new ArcaneRecipeInput(grid), f.level), "oracle greedy overlap quirk: first oak consumes broad ingredient, with NO backtracking");
        grid.set(0, new ItemStack(Items.BIRCH_PLANKS)); grid.set(8, new ItemStack(Items.OAK_PLANKS));
        f.check(overlapping.matches(new ArcaneRecipeInput(grid), f.level), "oracle greedy overlap quirk: birch then oak succeeds");
        var duplicates = new ArcaneShapelessRecipe("", List.of(Ingredient.of(Items.LEVER), Ingredient.of(Items.LEVER)), new ItemStack(Items.STONE), Map.of("aer", 1));
        grid = NonNullList.withSize(9, ItemStack.EMPTY); grid.set(4, new ItemStack(Items.LEVER, 2));
        f.check(!duplicates.matches(new ArcaneRecipeInput(grid), f.level), "two recipe ingredients require two occupied cells, not a count2 aggregate");
        grid.set(8, new ItemStack(Items.LEVER, 3));
        f.check(duplicates.matches(new ArcaneRecipeInput(grid), f.level), "two duplicate ingredients consume one item per cell regardless of stack size");
        var jarRecipe = new ArcaneShapelessRecipe("", List.of(Ingredient.of(ThaumcraftContent.JAR.get())), new ItemStack(Items.STONE), Map.of("aer", 1));
        ItemStack plain = new ItemStack(ThaumcraftContent.JAR.get()); plain.set(DataComponents.CUSTOM_NAME, Component.literal("empty-named-ingredient"));
        ItemStack labelled = plain.copy(); labelled.set(ThaumcraftLabels.JAR_LABEL_DATA.get(), new JarLabelData("aqua"));
        for (ItemStack jar : List.of(plain, labelled, ItemJar.withContents(plain, Aspect.WATER, 8))) {
            grid = NonNullList.withSize(9, ItemStack.EMPTY); grid.set(7, jar);
            f.check(jarRecipe.matches(new ArcaneRecipeInput(grid), f.level) == ItemJar.isEmptyIngredient(jar), "shapeless preserves shaped's plain-vs-filled/labelled ItemJar boundary, without rejecting ordinary names");
        }
        var containers = new ArcaneShapelessRecipe("", List.of(Ingredient.of(Items.MILK_BUCKET), Ingredient.of(Items.STICK)), new ItemStack(Items.STONE), Map.of("aer", 1));
        grid = NonNullList.withSize(9, ItemStack.EMPTY); grid.set(8, new ItemStack(Items.MILK_BUCKET)); grid.set(1, new ItemStack(Items.STICK));
        var input = new ArcaneRecipeInput(grid); var remainders = containers.getRemainingItems(input);
        f.check(containers.matches(input, f.level) && remainders.size() == 9 && remainders.get(8).is(Items.BUCKET)
                && remainders.stream().filter(stack -> !stack.isEmpty()).count() == 1, "native shapeless remainders retain their arbitrary original occupied-cell index");
    }

    private static void craftResonator(Fixture f, TileArcaneWorkbench tile, ContainerArcaneWorkbench menu) {
        tile.clearContent(); tile.setItem(10, wand(550));
        var recipe = (ArcaneShapedRecipe) f.level.getRecipeManager().byKey(id("arcane/resonator")).orElseThrow().value();
        f.check(recipe.research().equals("TUBES") && recipe.pattern().equals(List.of("I I", "INI", " S "))
                && recipe.vis().equals(Map.of("aer", 5, "aqua", 5)), "real original resonator uses the shared TUBES gate, literal shape and air5/water5");
        for (int slot : new int[] {0, 2, 3, 5}) tile.setItem(slot, new ItemStack(Items.IRON_INGOT));
        tile.setItem(4, new ItemStack(Items.QUARTZ)); tile.setItem(7, new ItemStack(Items.STICK));
        f.check(recipe.matches(ArcaneRecipeInput.snapshot(tile), f.level), "real recipe consumes four iron, one NETHER quartz and one wooden stick");
        tile.setItem(4, new ItemStack(Items.AMETHYST_SHARD));
        f.check(!recipe.matches(ArcaneRecipeInput.snapshot(tile), f.level), "modern amethyst is not original nether quartz");
        tile.setItem(4, new ItemStack(Items.QUARTZ)); menu.broadcastChanges();
        ItemStack resonator = take(menu, f.player);
        f.check(resonator.is(ThaumcraftContent.RESONATOR.get()) && resonator.getCount() == 1 && !resonator.hasFoil()
                && ArcaneRecipeInput.snapshot(tile).items().stream().allMatch(ItemStack::isEmpty)
                && ItemWandCasting.getVis(tile.getItem(10), "aer") == 0 && ItemWandCasting.getVis(tile.getItem(10), "aqua") == 0
                && ItemWandCasting.getVis(tile.getItem(10), "ordo") == 550, "real menu earns exactly one plain resonator and atomically pays ONLY air/water550 each");
    }

    private record BadJson(String reason, Consumer<JsonObject> corrupt) {}
    private static void codecs(Fixture f, ArcaneShapelessRecipe recipe, ResourceLocation recipeId) {
        var ops = RegistryOps.create(JsonOps.INSTANCE, f.level.registryAccess());
        var json = ArcaneShapelessRecipe.CODEC.codec().encodeStart(ops, recipe).getOrThrow().getAsJsonObject();
        var decoded = ArcaneShapelessRecipe.CODEC.codec().parse(ops, json).getOrThrow();
        f.check(decoded.matches(valveInput(7, 0, 1), f.level) && decoded.vis().equals(recipe.vis())
                && ItemStack.matches(decoded.result(), recipe.result()), "strict registry-aware JSON round trip retains the genuine shapeless recipe");
        var serializer = new ArcaneShapelessRecipe.Serializer();
        var wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), f.level.registryAccess());
        try {
            serializer.streamCodec().encode(wire, recipe);
            var received = serializer.streamCodec().decode(wire);
            f.check(received.matches(valveInput(4, 8, 1), f.level) && received.vis().equals(recipe.vis()) && wire.readableBytes() == 0, "validated stream round trip keeps ingredient matching, cost and complete framing");
        } finally { wire.release(); }
        wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), f.level.registryAccess());
        try {
            ClientboundUpdateRecipesPacket.STREAM_CODEC.encode(wire, new ClientboundUpdateRecipesPacket(f.level.getRecipeManager().getRecipes()));
            var packet = ClientboundUpdateRecipesPacket.STREAM_CODEC.decode(wire);
            f.check(packet.getRecipes().stream().anyMatch(entry -> entry.id().equals(recipeId) && entry.value() instanceof ArcaneShapelessRecipe), "actual full native update-recipes packet includes the new serializer and survives round trip");
        } finally { wire.release(); }
        for (BadJson invalid : List.of(
                new BadJson("zero ingredients", row -> row.add("ingredients", new JsonArray())),
                new BadJson("ten ingredients", row -> { var entries = new JsonArray(); for (int n = 0; n < 10; n++) entries.add(json.getAsJsonArray("ingredients").get(0).deepCopy()); row.add("ingredients", entries); }),
                new BadJson("empty ingredient object", row -> { var entries = new JsonArray(); entries.add(new JsonObject()); row.add("ingredients", entries); }),
                new BadJson("empty ingredient alternatives", row -> { var entries = new JsonArray(); entries.add(new JsonArray()); row.add("ingredients", entries); }),
                new BadJson("air result", row -> row.getAsJsonObject("result").addProperty("id", "minecraft:air")),
                new BadJson("zero result", row -> row.getAsJsonObject("result").addProperty("count", 0)),
                new BadJson("negative result", row -> row.getAsJsonObject("result").addProperty("count", -1)),
                new BadJson("nonnumeric result count", row -> row.getAsJsonObject("result").addProperty("count", "not-a-count")),
                new BadJson("oversized result", row -> row.getAsJsonObject("result").addProperty("count", 65)),
                new BadJson("missing result", row -> row.remove("result")),
                new BadJson("empty vis", row -> row.add("vis", new JsonObject())),
                new BadJson("missing vis", row -> row.remove("vis")),
                new BadJson("compound vis", row -> row.getAsJsonObject("vis").addProperty("metallum", 1)),
                new BadJson("unknown vis", row -> row.getAsJsonObject("vis").addProperty("made_up", 1)),
                new BadJson("zero vis", row -> row.getAsJsonObject("vis").addProperty("aqua", 0)),
                new BadJson("negative vis", row -> row.getAsJsonObject("vis").addProperty("aqua", -1)),
                new BadJson("overflowing vis", row -> row.getAsJsonObject("vis").addProperty("aqua", Integer.MAX_VALUE)))) {
            JsonObject malformed = json.deepCopy(); invalid.corrupt().accept(malformed);
            f.check(ArcaneShapelessRecipe.CODEC.codec().parse(ops, malformed).error().isPresent(), "JSON refuses " + invalid.reason());
            wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), f.level.registryAccess()); boolean rejected = false;
            try {
                ByteBufCodecs.TAG.encode(wire, JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, malformed));
                try { serializer.streamCodec().decode(wire); } catch (DecoderException expected) { rejected = true; }
            } finally { wire.release(); }
            f.check(rejected, "native stream decoder cannot bypass JSON validation: " + invalid.reason());
        }
        for (ArcaneShapelessRecipe invalid : List.of(
                new ArcaneShapelessRecipe("", List.of(), recipe.result(), recipe.vis()),
                new ArcaneShapelessRecipe("", List.of(Ingredient.EMPTY), recipe.result(), recipe.vis()),
                new ArcaneShapelessRecipe("", recipe.ingredients(), ItemStack.EMPTY, recipe.vis()),
                new ArcaneShapelessRecipe("", recipe.ingredients(), recipe.result(), Map.of("metallum", 1)),
                new ArcaneShapelessRecipe("", recipe.ingredients(), recipe.result(), Map.of("aqua", 0)))) {
            f.check(ArcaneShapelessRecipe.CODEC.codec().encodeStart(ops, invalid).error().isPresent(), "invalid direct construction cannot bypass JSON ENCODER validation");
            wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), f.level.registryAccess()); boolean rejected = false;
            try { try { serializer.streamCodec().encode(wire, invalid); } catch (EncoderException expected) { rejected = true; } }
            finally { wire.release(); }
            f.check(rejected, "invalid direct construction cannot bypass native stream ENCODER validation");
        }
    }

    private static void transportAndAuthority(Fixture f, BlockPos pos, ItemStack tool) {
        // IE-only addon boundary, not an atomic endpoint. Its IAspectContainer
        // method is a deliberately unreadable filter marker, NOT buffer storage.
        f.level.removeBlockEntity(pos); var probe = new Probe(pos, f.level.getBlockState(pos)); f.level.setBlockEntity(probe);
        f.player.setShiftKeyDown(false); f.aim(Vec3.atLowerCornerOf(pos).add(.5, .4, .5));
        f.inspect(pos, Direction.EAST, InteractionHand.MAIN_HAND, Aspect.AIR, 25, Aspect.WATER, 13,
                () -> ItemResonator.tryInspect(new UseOnContext(f.player, InteractionHand.MAIN_HAND, hit(pos, Direction.EAST))));
        f.check(probe.faces.equals(List.of(Direction.EAST, Direction.EAST, Direction.EAST, Direction.EAST)), "every IE-only sided getter uses the clicked face on a nonindexed transport");
        probe.faces.clear(); probe.stored = null;
        f.inspect(pos, Direction.NORTH, InteractionHand.MAIN_HAND, null, 0, Aspect.WATER, 13,
                () -> ItemResonator.tryInspect(new UseOnContext(f.player, InteractionHand.MAIN_HAND, hit(pos, Direction.NORTH))));
        f.check(probe.faces.equals(List.of(Direction.NORTH, Direction.NORTH, Direction.NORTH)), "null essentia type suppresses contents report/amount query; arbitrary filter marker is never enumerated");
        f.feedback(); var context = new UseOnContext(f.player, InteractionHand.MAIN_HAND, hit(pos, Direction.SOUTH));
        f.player.setItemInHand(InteractionHand.MAIN_HAND, tool.copy());
        f.check(ItemResonator.tryInspect(context) == InteractionResult.FAIL && f.feedback().empty(), "detached copied held identity cannot inspect or emit reports");
        f.player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        context = new UseOnContext(f.player, InteractionHand.MAIN_HAND, hit(pos, Direction.SOUTH));
        var offThreadContext = context;
        f.check(CompletableFuture.supplyAsync(() -> ItemResonator.tryInspect(offThreadContext)).orTimeout(2, TimeUnit.SECONDS).join() == InteractionResult.FAIL
                && f.feedback().empty(), "off-server-thread inspection fails before querying the transport/world or sending feedback");
        f.check(ItemResonator.tryInspect(new DetachedContext(f.level, null, tool, hit(pos, Direction.SOUTH))) == InteractionResult.FAIL && f.feedback().empty(), "null actor is never an inspection authority");
        ServerLevel other = f.level.getServer().getLevel(Level.NETHER);
        if (other != null && other != f.level) f.check(ItemResonator.tryInspect(new DetachedContext(other, f.player, tool, hit(pos, Direction.SOUTH))) == InteractionResult.FAIL && f.feedback().empty(), "context level must be the actual actor's world");
        f.player.setPos(f.player.position().add(0, 0, 20));
        f.check(ItemResonator.tryInspect(context) == InteractionResult.FAIL && f.feedback().empty(), "out-of-reach context cannot reveal remote transport state");
        f.aim(Vec3.atLowerCornerOf(pos).add(.5, .4, .5)); f.level.setBlock(pos.south(), Blocks.STONE.defaultBlockState(), QUIET);
        f.check(ItemResonator.tryInspect(context) == InteractionResult.FAIL && f.feedback().empty(), "a real intervening solid block occludes inspection despite a forged target hit");
        f.level.setBlock(pos.south(), Blocks.AIR.defaultBlockState(), QUIET);
        f.player.setGameMode(GameType.SPECTATOR); f.feedback();
        f.check(ItemResonator.tryInspect(context) == InteractionResult.FAIL && f.feedback().empty(), "spectator context cannot bypass actor validation");
        f.player.setGameMode(GameType.SURVIVAL); f.feedback();
        probe.query = () -> f.level.removeBlockEntity(pos);
        f.check(ItemResonator.tryInspect(context) == InteractionResult.FAIL && f.feedback().empty(), "addon query replacing/removing itself is revalidated before any chat/sound");
    }

    private static void refuse(Fixture f, TileArcaneWorkbench tile, ContainerArcaneWorkbench menu, String why) {
        var before = tile.saveWithoutMetadata(f.level.registryAccess()); menu.clicked(0, 0, ClickType.PICKUP, f.player);
        f.check(menu.getCarried().isEmpty() && before.equals(tile.saveWithoutMetadata(f.level.registryAccess())), why);
    }
    private static void paidVis(Fixture f, TileArcaneWorkbench tile, int remaining, String why) {
        for (String tag : WandData.PRIMALS) f.check(ItemWandCasting.getVis(tile.getItem(10), tag) == (tag.equals("aqua") || tag.equals("ordo") ? remaining : 1650), why + " pays only the original two primals: " + tag);
    }
    private static ArcaneRecipeInput valveInput(int tube, int lever, int count) {
        var input = NonNullList.withSize(9, ItemStack.EMPTY);
        input.set(tube, new ItemStack(ThaumcraftTubes.ESSENTIA_TUBE_ITEM.get(), count)); input.set(lever, new ItemStack(Items.LEVER, count));
        return new ArcaneRecipeInput(input);
    }
    private static void fillValve(TileArcaneWorkbench tile, int tube, int lever, int count) {
        var input = valveInput(tube, lever, count);
        for (int slot = 0; slot < 9; slot++) tile.setItem(slot, input.getItem(slot));
    }
    private static ItemStack wand(int amount) {
        var stack = new ItemStack(ThaumcraftContent.WAND.get()); var data = WandData.EMPTY;
        for (String tag : WandData.PRIMALS) data = data.withAmount(tag, amount);
        stack.set(ThaumcraftContent.WAND_DATA.get(), data); return stack;
    }
    private static ItemStack take(ContainerArcaneWorkbench menu, ServerPlayer player) {
        menu.clicked(0, 0, ClickType.PICKUP, player); var output = menu.getCarried().copy(); menu.setCarried(ItemStack.EMPTY); return output;
    }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }
    private static BlockHitResult hit(BlockPos pos, Direction face) { return new BlockHitResult(Vec3.atCenterOf(pos).relative(face, .5), face, pos, false); }
    private record Feedback(List<ClientboundSystemChatPacket> messages, List<ClientboundSoundPacket> sounds) {
        boolean empty() { return messages.isEmpty() && sounds.isEmpty(); }
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper test; final ServerLevel level; final ServerPlayer player; final BlockPos base; final AABB bounds;
        final Connection connection = new Connection(PacketFlow.SERVERBOUND); final EmbeddedChannel channel = new EmbeddedChannel(connection);
        final Map<BlockPos, BlockState> states = new LinkedHashMap<>(); final Map<BlockPos, BlockEntity> originals = new LinkedHashMap<>();
        final Set<UUID> oldDrops; boolean touched;
        Fixture(GameTestHelper test) {
            this.test = test; level = test.getLevel(); base = test.absolutePos(new BlockPos(3, 2, 3)); bounds = new AABB(base).inflate(6);
            for (BlockPos mutable : BlockPos.betweenClosed(base.offset(-1, -2, -1), base.offset(3, 4, 3))) {
                var pos = mutable.immutable(); states.put(pos, level.getBlockState(pos));
                if (level.getBlockEntity(pos) != null) originals.put(pos, level.getBlockEntity(pos));
            }
            oldDrops = level.getEntitiesOfClass(ItemEntity.class, bounds).stream().map(ItemEntity::getUUID).collect(Collectors.toSet());
            player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "m06-" + UUID.randomUUID().toString().substring(0, 8)), ClientInformation.createDefault());
        }
        void open() {
            level.getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
            check(player.level() == level && level.getServer().getPlayerList().getPlayer(player.getUUID()) == player, "owned real native connected actor belongs to this test level");
            player.setGameMode(GameType.SURVIVAL); player.setNoGravity(true); player.setShiftKeyDown(false); player.getInventory().clearContent();
            check(!player.isCreative(), "native actor is really survival, not GameTest's always-creative mock");
            touched = true;
            for (BlockPos pos : states.keySet()) { level.removeBlockEntity(pos); level.setBlock(pos, Blocks.AIR.defaultBlockState(), QUIET); }
            feedback();
        }
        void check(boolean value, String why) { test.assertTrue(value, why); }
        void aim(Vec3 target) {
            player.setPos(target.x, target.y - player.getEyeHeight(), target.z + 2);
            player.setYRot(180); player.setYHeadRot(180); player.yHeadRotO = 180; player.setXRot(0); player.setDeltaMovement(Vec3.ZERO);
        }
        InteractionResult use(BlockPos pos, Direction side, InteractionHand hand) { return player.gameMode.useItemOn(player, level, player.getItemInHand(hand), hand, hit(pos, side)); }
        Feedback feedback() {
            connection.flushChannel(); channel.runPendingTasks(); var messages = new ArrayList<ClientboundSystemChatPacket>(); var sounds = new ArrayList<ClientboundSoundPacket>(); Object packet;
            while ((packet = channel.readOutbound()) != null) {
                try {
                    if (packet instanceof ClientboundSystemChatPacket message) messages.add(message);
                    if (packet instanceof ClientboundSoundPacket sound) sounds.add(sound);
                } finally { ReferenceCountUtil.release(packet); }
            }
            return new Feedback(messages, sounds);
        }
        void inspect(BlockPos pos, Direction side, InteractionHand hand, Aspect stored, int amount, Aspect suction, int pressure, Supplier<InteractionResult> action) {
            BlockEntity tile = level.getBlockEntity(pos); BlockState state = level.getBlockState(pos);
            CompoundTag before = tile.saveWithFullMetadata(level.registryAccess()); ItemStack identity = player.getItemInHand(hand), held = identity.copy();
            var inventory = new ArrayList<ItemStack>(); for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) inventory.add(player.getInventory().getItem(slot).copy());
            feedback(); check(action.get().consumesAction(), "one native inspection consumes the dispatch before label/valve/bare-hand fallthrough: " + pos + "/" + side);
            Feedback sent = feedback();
            check(sent.messages().size() == (stored == null ? 1 : 2), "exactly one original contents report iff type is nonnull, plus one suction report; no duplicate/bare-hand chat");
            if (stored != null) message(sent.messages().getFirst(), "tc.resonator1", amount, stored.getName());
            message(sent.messages().getLast(), "tc.resonator2", pressure, suction == null ? null : suction.getName());
            check(sent.sounds().size() == 1, "one actual server sound, not a second inspection/alembic interaction");
            var sound = sent.sounds().getFirst();
            check(sound.getSound().value().getLocation().equals(id("alembicknock")) && sound.getSource() == SoundSource.BLOCKS
                    && sound.getVolume() == .5F && sound.getPitch() >= 1.9F && sound.getPitch() < 2.000001F,
                    "original server alembicknock volume.5/pitch1.9..2, with no controlled RNG");
            check(level.getBlockEntity(pos) == tile && level.getBlockState(pos) == state && before.equals(tile.saveWithFullMetadata(level.registryAccess()))
                    && player.getItemInHand(hand) == identity && ItemStack.matches(held, identity), "inspection preserves BE identity/state/contents/labels/valve flow AND exact held object/components/count");
            for (int slot = 0; slot < inventory.size(); slot++) check(ItemStack.matches(inventory.get(slot), player.getInventory().getItem(slot)), "inspection neither charges nor grants any inventory item, including the other hand: " + slot);
        }
        void message(ClientboundSystemChatPacket packet, String key, int amount, String aspect) {
            check(!packet.overlay() && packet.content().getContents() instanceof TranslatableContents, "original reports use server chat, not action bar or pretranslated literals");
            var text = (TranslatableContents) packet.content().getContents();
            check(text.getKey().equals(key) && text.getArgs().length == 2 && text.getArgs()[0].equals(Integer.toString(amount)), "original report key and exact queried amount: " + key);
            Object name = text.getArgs()[1];
            check(aspect != null ? aspect.equals(name) : name instanceof Component component
                    && component.getContents() instanceof TranslatableContents contents && contents.getKey().equals("tc.resonator3"), "null suction retains translatable tc.resonator3; typed reports retain original aspect name");
        }
        @Override public void close() {
            try {
                player.stopUsingItem();
                if (player.containerMenu != player.inventoryMenu) player.containerMenu.setCarried(ItemStack.EMPTY);
                player.closeContainer(); player.getInventory().clearContent();
                if (touched) {
                    // Detach owned BEs before removal, so cleanup never enqueues
                    // labels/inventory and never destroys/replaces original BEs.
                    for (BlockPos pos : states.keySet()) level.removeBlockEntity(pos);
                    states.forEach((pos, state) -> level.setBlock(pos, state, QUIET));
                    originals.values().forEach(tile -> { tile.clearRemoved(); level.setBlockEntity(tile); });
                    for (var drop : level.getEntitiesOfClass(ItemEntity.class, bounds)) if (!oldDrops.contains(drop.getUUID())) drop.discard();
                }
            } finally {
                try { if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player); }
                finally { player.discard(); channel.finishAndReleaseAll(); }
            }
        }
    }

    private static final class DetachedContext extends UseOnContext {
        DetachedContext(Level level, ServerPlayer player, ItemStack held, BlockHitResult hit) { super(level, player, InteractionHand.MAIN_HAND, held, hit); }
    }

    /** Adversarial IE-only query fixture; no transfer, buffer or marker fallback is legal. */
    private static final class Probe extends BlockEntity implements IEssentiaTransport, IAspectContainer {
        final List<Direction> faces = new ArrayList<>(); Aspect stored = Aspect.AIR; Runnable query = () -> {};
        Probe(BlockPos pos, BlockState state) { super(ThaumcraftContent.JAR_BLOCK_ENTITY.get(), pos, state); }
        @Override public Aspect getEssentiaType(Direction face) { faces.add(face); return stored; }
        @Override public int getEssentiaAmount(Direction face) { faces.add(face); if (stored == null) throw new AssertionError("untyped contents have no report"); return 20 + face.get3DDataValue(); }
        @Override public Aspect getSuctionType(Direction face) { faces.add(face); query.run(); return Aspect.WATER; }
        @Override public int getSuctionAmount(Direction face) { faces.add(face); return 13; }
        @Override public boolean isConnectable(Direction face) { return false; }
        @Override public boolean canInputFrom(Direction face) { return false; }
        @Override public boolean canOutputTo(Direction face) { return false; }
        @Override public int getMinimumSuction() { return 0; }
        @Override public boolean renderExtendedTube() { return false; }
        @Override public void setSuction(Aspect aspect, int amount) { throw new AssertionError("read-only inspector"); }
        @Override public int takeEssentia(Aspect aspect, int amount, Direction face) { throw new AssertionError("read-only inspector"); }
        @Override public int addEssentia(Aspect aspect, int amount, Direction face) { throw new AssertionError("read-only inspector"); }
        @Override public AspectList getAspects() { throw new AssertionError("IAspectContainer filter marker is not buffer contents"); }
        @Override public void setAspects(AspectList aspects) { throw new AssertionError("read-only inspector"); }
        @Override public boolean doesContainerAccept(Aspect aspect) { return false; }
        @Override public int addToContainer(Aspect aspect, int amount) { throw new AssertionError("read-only inspector"); }
        @Override public boolean takeFromContainer(Aspect aspect, int amount) { throw new AssertionError("read-only inspector"); }
        @Override public boolean takeFromContainer(AspectList aspects) { throw new AssertionError("read-only inspector"); }
        @Override public boolean doesContainerContainAmount(Aspect aspect, int amount) { return false; }
        @Override public boolean doesContainerContain(AspectList aspects) { return false; }
        @Override public int containerContains(Aspect aspect) { return -1; }
    }
}
