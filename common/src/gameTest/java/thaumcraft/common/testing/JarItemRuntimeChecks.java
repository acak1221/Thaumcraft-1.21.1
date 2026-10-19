package thaumcraft.common.testing;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftLabels;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.items.ItemJar;
import thaumcraft.common.items.JarLabelData;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.ObjectAspectResolver;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.tiles.TileArcaneWorkbench;
import thaumcraft.common.tiles.TileJar;

/** One bounded native item/API/recipe/save-place case. Root owns its wrapper and execution. */
public final class JarItemRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private JarItemRuntimeChecks() {}

    public static void run(GameTestHelper test, ServerPlayer player) {
        var level = test.getLevel();
        BlockPos pos = test.absolutePos(new BlockPos(3, 2, 3)), bench = pos.east(2);
        test.assertTrue(player.level() == level && player.containerMenu == player.inventoryMenu, "same-level native player has no existing menu to discard");
        var states = new LinkedHashMap<BlockPos, BlockState>();
        var originals = new LinkedHashMap<BlockPos, BlockEntity>();
        for (BlockPos saved : List.of(pos, pos.below(), bench)) {
            states.put(saved, level.getBlockState(saved));
            if (level.getBlockEntity(saved) != null) originals.put(saved, level.getBlockEntity(saved));
        }
        var inventory = new ArrayList<ItemStack>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) inventory.add(player.getInventory().getItem(slot).copy());
        var knowledge = KnowledgeAccess.get(player).copy();
        var abilities = new CompoundTag(); player.getAbilities().addSaveData(abilities);
        var food = new CompoundTag(); player.getFoodData().addAdditionalSaveData(food);
        var position = player.position(); var motion = player.getDeltaMovement();
        var mode = player.gameMode.getGameModeForPlayer();
        int selected = player.getInventory().selected;
        float yaw = player.getYRot(), pitch = player.getXRot();
        boolean sneaking = player.isShiftKeyDown(), tileDrops = level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS);
        var mined = new LinkedHashMap<Block, Integer>();
        var pickedUp = new LinkedHashMap<Block, Integer>();
        for (Block block : List.of(ThaumcraftContent.JAR.get(), ThaumcraftContent.VOID_JAR.get())) {
            mined.put(block, player.getStats().getValue(Stats.BLOCK_MINED.get(block)));
            pickedUp.put(block, player.getStats().getValue(Stats.ITEM_PICKED_UP.get(block.asItem())));
        }
        int crafted = player.getStats().getValue(Stats.ITEM_CRAFTED.get(ThaumcraftContent.VOID_JAR.get().asItem()));
        AABB bounds = new AABB(pos).inflate(4);
        Set<UUID> existing = level.getEntitiesOfClass(ItemEntity.class, bounds).stream().map(ItemEntity::getUUID).collect(Collectors.toSet());
        try {
            originals.keySet().forEach(level::removeBlockEntity);
            states.keySet().forEach(saved -> level.setBlock(saved, Blocks.AIR.defaultBlockState(), QUIET));
            player.getInventory().clearContent(); player.getInventory().selected = 0;
            player.setGameMode(GameType.SURVIVAL); player.setShiftKeyDown(false);
            player.setDeltaMovement(Vec3.ZERO);
            level.getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS).set(true, level.getServer());
            components(test, player);
            recipes(test, player, bench);
            for (Block block : List.of(ThaumcraftContent.JAR.get(), ThaumcraftContent.VOID_JAR.get())) dropPlace(test, player, pos, block);
        } finally {
            player.closeContainer();
            if (level.getBlockEntity(bench) instanceof TileArcaneWorkbench table) table.clearContent();
            states.keySet().forEach(level::removeBlockEntity);
            states.forEach((saved, state) -> level.setBlock(saved, state, QUIET));
            originals.values().forEach(original -> { original.clearRemoved(); level.setBlockEntity(original); });
            DeferredItemDrops.tick(level);
            for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, bounds)) if (!existing.contains(entity.getUUID())) entity.discard();
            player.getInventory().clearContent();
            for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().setItem(slot, inventory.get(slot));
            player.getInventory().selected = selected; player.setGameMode(mode); player.setShiftKeyDown(sneaking);
            player.getAbilities().loadSaveData(abilities); player.onUpdateAbilities();
            player.getFoodData().readAdditionalSaveData(food);
            player.setPos(position); player.setDeltaMovement(motion); player.setYRot(yaw); player.setXRot(pitch);
            KnowledgeAccess.get(player).copyFrom(knowledge);
            mined.forEach((block, value) -> player.getStats().setValue(player, Stats.BLOCK_MINED.get(block), value));
            pickedUp.forEach((block, value) -> player.getStats().setValue(player, Stats.ITEM_PICKED_UP.get(block.asItem()), value));
            player.getStats().setValue(player, Stats.ITEM_CRAFTED.get(ThaumcraftContent.VOID_JAR.get().asItem()), crafted);
            level.getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS).set(tileDrops, level.getServer());
        }
        test.succeed();
    }

    private static void components(GameTestHelper test, ServerPlayer player) {
        var level = test.getLevel();
        for (Block block : List.of(ThaumcraftContent.JAR.get(), ThaumcraftContent.VOID_JAR.get())) {
            ItemStack empty = new ItemStack(block, 3);
            test.assertTrue(empty.getItem() instanceof ItemJar && empty.getItem() instanceof IEssentiaContainerItem
                    && ItemJar.isJar(empty) && ItemJar.isVoid(empty) == (block == ThaumcraftContent.VOID_JAR.get())
                    && empty.getMaxStackSize() == 64 && ItemJar.isEmptyIngredient(empty), "both real registered variants expose the native container API; empty jars stack64");
            ItemJar item = (ItemJar) empty.getItem();
            empty.set(DataComponents.CUSTOM_NAME, Component.literal("jar-helper-components"));
            var addon = new CompoundTag(); addon.putString("addon", "untouched");
            empty.set(DataComponents.CUSTOM_DATA, CustomData.of(addon));
            test.assertTrue(ItemJar.isEmptyIngredient(empty), "harmless custom names and unrelated item components are not filled ingredients");
            ItemStack before = empty.copy();
            ItemStack filled = ItemJar.withContents(empty, Aspect.WATER, 40);
            test.assertTrue(ItemStack.matches(empty, before) && filled.getCount() == 1 && filled.getMaxStackSize() == 1
                    && ItemJar.getAmount(filled) == 40 && ItemJar.getAspect(filled) == Aspect.WATER
                    && filled.get(DataComponents.CUSTOM_NAME).equals(empty.get(DataComponents.CUSTOM_NAME))
                    && filled.get(DataComponents.CUSTOM_DATA).equals(empty.get(DataComponents.CUSTOM_DATA)), "copy helper splits exactly one output without changing the three empty inputs or unrelated components");
            var foreign = filled.get(DataComponents.BLOCK_ENTITY_DATA).copyTag(); foreign.putString("addon_be", "preserved");
            foreign.putString("AspectFilter", Aspect.WATER.getTag());
            filled.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(foreign));
            filled.set(ThaumcraftLabels.JAR_LABEL_DATA.get(), new JarLabelData(Aspect.WATER.getTag()));
            ItemStack updated = ItemJar.withContents(filled, Aspect.WATER, 64);
            test.assertTrue(ItemJar.getAmount(filled) == 40 && ItemJar.getAmount(updated) == 64 && ItemJar.getFilter(updated) == Aspect.WATER
                    && updated.get(ThaumcraftLabels.JAR_LABEL_DATA.get()).equals(filled.get(ThaumcraftLabels.JAR_LABEL_DATA.get()))
                    && updated.get(DataComponents.BLOCK_ENTITY_DATA).copyTag().getString("addon_be").equals("preserved"), "update copy preserves the physical label, BE extension fields and original stack");
            AspectList view = item.getAspects(updated);
            test.assertTrue(view.size() == 1 && view.getAmount(Aspect.WATER) == 64, "API contains only the actual64 units, never an extra filter unit");
            view.aspects.clear();
            test.assertTrue(item.getAspects(updated).getAmount(Aspect.WATER) == 64, "mutable API lists cannot mutate component storage");
            roundTrip(test, updated);
            for (AspectList invalid : List.of(new AspectList().add(Aspect.AIR, 1).add(Aspect.WATER, 1),
                    new AspectList().add(Aspect.WATER, -1), new AspectList().add(Aspect.WATER, 0), new AspectList().add(Aspect.WATER, 65))) {
                ItemStack snapshot = updated.copy();
                rejected(test, () -> item.setAspects(updated, invalid));
                test.assertTrue(ItemStack.matches(snapshot, updated), "invalid API input is rejected atomically, not clamped or partially erased");
            }
            rejected(test, () -> item.setAspects(empty, new AspectList().add(Aspect.WATER, 8)));
            rejected(test, () -> ItemJar.withContents(empty, null, 1));
            rejected(test, () -> ItemJar.withContents(empty, Aspect.WATER, -1));
            rejected(test, () -> ItemJar.withContents(empty, Aspect.WATER, 65));
            item.setAspects(updated, new AspectList().add(Aspect.WATER, 8));
            test.assertTrue(item.getAspects(updated).getAmount(Aspect.WATER) == 8 && ItemJar.getFilter(updated) == Aspect.WATER
                    && updated.getMaxStackSize() == 1 && updated.get(DataComponents.CUSTOM_NAME).equals(empty.get(DataComponents.CUSTOM_NAME))
                    && updated.get(DataComponents.BLOCK_ENTITY_DATA).copyTag().getString("addon_be").equals("preserved"), "valid API setter updates only contents/stackability, preserving name, filter and BE extension fields");
            item.setAspects(updated, null);
            test.assertTrue(item.getAspects(updated) == null && ItemJar.getAmount(updated) == 0 && ItemJar.getFilter(updated) == Aspect.WATER
                    && updated.getMaxStackSize() == 1 && !ItemJar.isEmptyIngredient(updated)
                    && updated.get(DataComponents.BLOCK_ENTITY_DATA).copyTag().getString("addon_be").equals("preserved"), "API clear keeps label/extension data and maxstack1 without treating the filter as essentia");
            ItemStack plain = ItemJar.withContents(new ItemStack(block), Aspect.WATER, 8);
            item.setAspects(plain, new AspectList());
            test.assertTrue(plain.getMaxStackSize() == 64 && !plain.has(DataComponents.BLOCK_ENTITY_DATA) && ItemJar.isEmptyIngredient(plain), "clearing an unlabelled item restores the real native default64 component, not an absent/max1 component");

            ItemStack bonus = ItemJar.withContents(new ItemStack(block), Aspect.WATER, 24);
            AspectList expected = ObjectAspectResolver.get(new ItemStack(block), level).copy().add(Aspect.WATER, 24);
            test.assertTrue(ObjectAspectResolver.get(bonus, level).aspects.equals(expected.aspects), "real aspect resolver adds the actual contained24 exactly once on this variant");
            bonus.set(ThaumcraftLabels.JAR_LABEL_DATA.get(), new JarLabelData(Aspect.WATER.getTag()));
            test.assertTrue(ObjectAspectResolver.get(bonus, level).aspects.equals(expected.aspects), "label metadata contributes no extra contained-aspect bonus");
            item.setAspects(bonus, new AspectList());
            test.assertTrue(ObjectAspectResolver.get(bonus, level).aspects.equals(ObjectAspectResolver.get(new ItemStack(block), level).aspects), "empty labelled jar resolves only its real base item tags");
        }
        test.assertTrue(!ItemJar.isJar(null) && !ItemJar.isJar(ItemStack.EMPTY) && !ItemJar.isEmptyIngredient(new ItemStack(Items.GLASS_BOTTLE))
                && ItemJar.getAspect(null) == null && ItemJar.getAmount(null) == 0 && ItemJar.getFilter(null) == null, "safe probes do not mistake foreign or absent stacks for jars");
        malformedIngredients(test);
        KnowledgeAccess.get(player).copyFrom(new PlayerKnowledge());
        ItemStack secret = ItemJar.withContents(new ItemStack(ThaumcraftContent.JAR.get()), Aspect.AURA, 8);
        secret.set(ThaumcraftLabels.JAR_LABEL_DATA.get(), new JarLabelData(Aspect.AURA.getTag()));
        ItemJar item = (ItemJar) secret.getItem();
        var tooltip = new ArrayList<Component>(); item.appendAspectTooltip(secret, player, tooltip);
        var hidden = List.of(Component.translatable("tc.aspect.unknown"), Component.translatable("tc.aspect.unknown").withStyle(ChatFormatting.DARK_PURPLE));
        test.assertTrue(tooltip.equals(hidden), "undiscovered contents AND filter stay knowledge-gated, with the original purple filter line");
        KnowledgeAccess.get(player).addDiscoveredAspect(Aspect.AURA.getTag()); tooltip.clear(); item.appendAspectTooltip(secret, player, tooltip);
        test.assertTrue(tooltip.size() == 2 && tooltip.getFirst().getString().equals(Aspect.AURA.getName() + " x 8")
                && tooltip.getLast().getString().equals(Aspect.AURA.getName()), "discovered tooltip shows contained amount, not base aspect tags or a label amount");
        tooltip.clear(); item.appendAspectTooltip(secret, null, tooltip);
        test.assertTrue(tooltip.equals(hidden), "a missing client viewer never leaks discovered names from another player");
        tooltip.clear(); item.appendHoverText(secret, Item.TooltipContext.of(level), tooltip, TooltipFlag.NORMAL);
        test.assertTrue(tooltip.size() == 2, "native item tooltip owns exactly one contents and one filter line; no duplicate legacy block tooltip delegation");
    }

    private static void malformedIngredients(GameTestHelper test) {
        ItemStack defaults = new ItemStack(ThaumcraftContent.JAR.get());
        var tag = new CompoundTag(); tag.putString("Aspect", ""); tag.putInt("Amount", 0); tag.putString("AspectFilter", ""); tag.putByte("facing", (byte) 5);
        BlockItem.setBlockEntityData(defaults, ThaumcraftContent.JAR_BLOCK_ENTITY.get(), tag);
        test.assertTrue(ItemJar.isEmptyIngredient(defaults), "explicit empty native scalar defaults and harmless facing metadata are genuinely empty");
        String[] keys = {"Amount", "Amount", "Amount", "Amount", "Aspect", "AspectFilter", "AspectFilter", "Aspects", "facing", "addon_payload", "id"};
        Tag[] values = {StringTag.valueOf("0"), IntTag.valueOf(-1), IntTag.valueOf(65), LongTag.valueOf(4_294_967_296L),
                StringTag.valueOf("not_a_registered_jar_aspect"), StringTag.valueOf("not_a_registered_jar_filter"), IntTag.valueOf(0), new ListTag(),
                StringTag.valueOf("east"), StringTag.valueOf("must_not_be_erased"), StringTag.valueOf("minecraft:chest")};
        for (int index = 0; index < keys.length; index++) {
            ItemStack malformed = defaults.copy();
            CompoundTag payload = malformed.get(DataComponents.BLOCK_ENTITY_DATA).copyTag(); payload.put(keys[index], values[index]);
            malformed.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(payload));
            test.assertTrue(!ItemJar.isEmptyIngredient(malformed), "malformed/unknown meaningful jar ingredient is refused intact: " + keys[index] + "=" + values[index]);
        }
        var invalid = new CompoundTag(); invalid.putString("Aspect", "not_a_registered_jar_aspect"); invalid.putInt("Amount", 8);
        ItemStack unknown = new ItemStack(ThaumcraftContent.JAR.get());
        BlockItem.setBlockEntityData(unknown, ThaumcraftContent.JAR_BLOCK_ENTITY.get(), invalid);
        test.assertTrue(ItemJar.getAspect(unknown) == null && ItemJar.getAmount(unknown) == 0 && ((ItemJar) unknown.getItem()).getAspects(unknown) == null
                && !ItemJar.isEmptyIngredient(unknown), "unknown raw aspect cannot become bonus essentia or silently become an empty recipe ingredient");
    }

    private static void recipes(GameTestHelper test, ServerPlayer player, BlockPos pos) {
        var level = test.getLevel();
        var type = ThaumcraftArcane.ARCANE_RECIPE_TYPE.get();
        var holder = level.getRecipeManager().getAllRecipesFor(type).stream()
                .filter(recipe -> recipe.value().getResultItem(level.registryAccess()).is(ThaumcraftContent.VOID_JAR.get().asItem())).findFirst().orElseThrow();
        test.assertTrue(holder.value() instanceof ArcaneShapedRecipe, "void jar is a real loaded shaped arcane recipe, not a synthetic test recipe");
        var recipe = (ArcaneShapedRecipe) holder.value();
        test.assertTrue(recipe.research().equals("JARVOID") && recipe.pattern().equals(List.of("O", "J", "P"))
                && recipe.vis().equals(java.util.Map.of("aqua", 5, "perditio", 15)), "original vertical obsidian/jar/BLAZE POWDER upgrade and raw5aqua/15perditio cost");
        ItemStack empty = new ItemStack(ThaumcraftContent.JAR.get());
        empty.set(DataComponents.CUSTOM_NAME, Component.literal("harmless-empty-ingredient-name"));
        ItemStack filled = ItemJar.withContents(empty, Aspect.WATER, 8);
        ItemStack labelled = ItemJar.withContents(empty, null, 0);
        labelled.set(ThaumcraftLabels.JAR_LABEL_DATA.get(), new JarLabelData(Aspect.WATER.getTag()));
        ItemStack scalarLabel = empty.copy();
        var labelTag = new CompoundTag(); labelTag.putString("AspectFilter", Aspect.WATER.getTag());
        BlockItem.setBlockEntityData(scalarLabel, ThaumcraftContent.JAR_BLOCK_ENTITY.get(), labelTag);
        ItemStack malformed = empty.copy();
        var bad = new CompoundTag(); bad.putString("Amount", "8");
        BlockItem.setBlockEntityData(malformed, ThaumcraftContent.JAR_BLOCK_ENTITY.get(), bad);
        for (int column = 0; column < 3; column++) {
            var input = column(column, empty);
            test.assertTrue(recipe.matches(input, level) && level.getRecipeManager().getRecipeFor(type, input, level).orElseThrow().id().equals(holder.id()), "real recipe matches column " + column + " and accepts a harmless custom name");
            for (ItemStack refused : List.of(filled, labelled, scalarLabel, malformed, new ItemStack(ThaumcraftContent.VOID_JAR.get()))) {
                test.assertTrue(!recipe.matches(column(column, refused), level), "filled, either label encoding, malformed, or wrong-variant ingredient cannot be erased by upgrade in column " + column);
            }
        }
        var reversed = new ArrayList<>(column(1, empty).items());
        ItemStack top = reversed.get(1); reversed.set(1, reversed.get(7)); reversed.set(7, top);
        var rotated = blankMatrix(); rotated.set(3, new ItemStack(Items.OBSIDIAN)); rotated.set(4, empty); rotated.set(5, new ItemStack(Items.BLAZE_POWDER));
        var powder = new ArrayList<>(column(1, empty).items()); powder.set(7, new ItemStack(Items.BLAZE_ROD));
        test.assertTrue(!recipe.matches(new ArcaneRecipeInput(reversed), level) && !recipe.matches(new ArcaneRecipeInput(rotated), level)
                && !recipe.matches(new ArcaneRecipeInput(powder), level), "upgrade is not reversed/rotatable and never substitutes a blaze rod for powder");

        level.setBlock(pos, ThaumcraftArcane.ARCANE_WORKBENCH.get().defaultBlockState(), QUIET);
        player.setPos(Vec3.atBottomCenterOf(pos.south(2))); player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        test.assertTrue(use(player, pos).consumesAction() && player.containerMenu instanceof ContainerArcaneWorkbench, "real block dispatch opens the native arcane menu");
        var menu = (ContainerArcaneWorkbench) player.containerMenu;
        var table = (TileArcaneWorkbench) level.getBlockEntity(pos);
        KnowledgeAccess.get(player).addResearch("JARVOID");
        for (ItemStack refused : List.of(filled, labelled, scalarLabel, malformed)) {
            matrix(table, column(1, refused)); table.setItem(TileArcaneWorkbench.WAND_SLOT, wand()); menu.broadcastChanges();
            CompoundTag before = table.saveWithoutMetadata(level.registryAccess());
            menu.clicked(0, 0, ClickType.PICKUP, player);
            test.assertTrue(menu.getCarried().isEmpty() && menu.getSlot(0).getItem().isEmpty() && before.equals(table.saveWithoutMetadata(level.registryAccess())), "actual result click refuses the content-bearing ingredient without loss of jar/ingredients/vis");
        }
        for (int column = 0; column < 3; column++) {
            matrix(table, column(column, empty)); table.setItem(TileArcaneWorkbench.WAND_SLOT, wand()); menu.broadcastChanges();
            test.assertTrue(menu.craftable() && menu.requiredVis("aqua") == 550 && menu.requiredVis("perditio") == 1650, "real iron-cap payer prices original5/15 at1.1 in column " + column);
            menu.clicked(0, 0, ClickType.PICKUP, player);
            test.assertTrue(menu.getCarried().is(ThaumcraftContent.VOID_JAR.get().asItem()) && menu.getCarried().getCount() == 1
                    && ItemJar.isVoid(menu.getCarried()) && ItemJar.isEmptyIngredient(menu.getCarried()), "native result extraction produces exactly one empty actual void jar in column " + column);
            for (int slot = 0; slot < 9; slot++) test.assertTrue(table.getItem(slot).isEmpty(), "each real craft consumes exactly the three raw ingredients");
            for (String tag : WandData.PRIMALS) test.assertTrue(ItemWandCasting.getVis(table.getItem(TileArcaneWorkbench.WAND_SLOT), tag)
                    == (tag.equals("aqua") ? 1750 : tag.equals("perditio") ? 650 : 2300), "one native atomic centivis debit, no charge to other primals: " + tag);
            menu.setCarried(ItemStack.EMPTY);
        }
        // The client story uses the original ink sac, not only modern black
        // dye. Exercise its actual ordinary fallback/result click on THIS menu.
        var labels = blankMatrix(); labels.set(0, new ItemStack(Items.INK_SAC)); labels.set(1, new ItemStack(Items.SLIME_BALL));
        for (int slot = 2; slot < 6; slot++) labels.set(slot, new ItemStack(Items.PAPER));
        matrix(table, new ArcaneRecipeInput(labels)); menu.broadcastChanges();
        ItemStack payerBeforeLabels = table.getItem(TileArcaneWorkbench.WAND_SLOT).copy();
        test.assertTrue(menu.craftable() && menu.getSlot(0).getItem().is(ThaumcraftContent.MATERIALS.get("jar_label").get())
                && menu.getSlot(0).getItem().getCount() == 4, "original ink sac makes the real four-label preview on the arcane table's vanilla fallback");
        menu.clicked(0, 0, ClickType.PICKUP, player);
        test.assertTrue(menu.getCarried().is(ThaumcraftContent.MATERIALS.get("jar_label").get()) && menu.getCarried().getCount() == 4
                && ItemStack.matches(payerBeforeLabels, table.getItem(TileArcaneWorkbench.WAND_SLOT)),
                "one native label-result click consumes original ink/slime/paper with zero vis and four physical blanks");
        for (int slot = 0; slot < 9; slot++) test.assertTrue(table.getItem(slot).isEmpty(), "ordinary label fallback consumes all six actual ingredients once");
        menu.setCarried(ItemStack.EMPTY);
        table.clearContent(); player.closeContainer();
    }

    private static void dropPlace(GameTestHelper test, ServerPlayer player, BlockPos pos, Block block) {
        var level = test.getLevel();
        player.getInventory().clearContent(); player.setPos(Vec3.atBottomCenterOf(pos.south(2))); player.setYRot(90);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), QUIET); level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), QUIET);
        ItemStack filled = ItemJar.withContents(new ItemStack(block), Aspect.AURA, 24);
        Component name = Component.literal("jar-item-save-drop-place-" + ItemJar.isVoid(filled));
        filled.set(DataComponents.CUSTOM_NAME, name);
        var extension = new CompoundTag(); extension.putString("jar_owner", "component-preservation");
        filled.set(DataComponents.CUSTOM_DATA, CustomData.of(extension));
        filled.set(ThaumcraftLabels.JAR_LABEL_DATA.get(), new JarLabelData(Aspect.AURA.getTag()));
        CustomData.update(DataComponents.BLOCK_ENTITY_DATA, filled, tag -> { tag.putString("AspectFilter", Aspect.AURA.getTag()); tag.putByte("facing", (byte) Direction.WEST.get3DDataValue()); });
        player.setItemInHand(InteractionHand.MAIN_HAND, roundTrip(test, filled));
        test.assertTrue(use(player, pos.below()).consumesAction() && player.getMainHandItem().isEmpty(), "normal BlockItem placement pays exactly the component-bearing jar after the alembic helper passes");
        var jar = (TileJar) level.getBlockEntity(pos);
        test.assertTrue(jar != null && jar.getBlockState().is(block) && jar.isVoid() == ItemJar.isVoid(filled) && jar.getAspect() == Aspect.AURA
                && jar.getAmount() == 24 && jar.getFilter() == Aspect.AURA && jar.getFacing() == player.getDirection().getOpposite(), "placed variant/contents/filter are real, while facing is fresh rather than copied from item data");
        var saved = jar.saveWithFullMetadata(level.registryAccess());
        var state = level.getBlockState(pos);
        level.removeBlockEntity(pos);
        TileJar loaded = ThaumcraftContent.JAR_BLOCK_ENTITY.get().create(pos, state);
        loaded.loadWithComponents(saved, level.registryAccess()); level.setBlockEntity(loaded);
        test.assertTrue(loaded.getAmount() == 24 && loaded.getFilter() == Aspect.AURA && loaded.isVoid() == jar.isVoid(), "native registered BE save/reload retains item-placed data and variant");
        AABB box = new AABB(pos).inflate(2);
        Set<UUID> before = level.getEntitiesOfClass(ItemEntity.class, box).stream().map(ItemEntity::getUUID).collect(Collectors.toSet());
        test.assertTrue(player.gameMode.destroyBlock(pos), "real survival player break removes the placed jar");
        DeferredItemDrops.tick(level);
        var drops = level.getEntitiesOfClass(ItemEntity.class, box, entity -> !before.contains(entity.getUUID()));
        test.assertTrue(drops.size() == 1, "one native jar drop, no duplicate loose label or default loot-table jar");
        ItemEntity drop = drops.getFirst(); ItemStack dropped = drop.getItem();
        test.assertTrue(dropped.is(block.asItem()) && dropped.getCount() == 1 && dropped.getMaxStackSize() == 1 && ItemJar.getAmount(dropped) == 24
                && ItemJar.getAspect(dropped) == Aspect.AURA && ItemJar.getFilter(dropped) == Aspect.AURA
                && name.equals(dropped.get(DataComponents.CUSTOM_NAME)) && filled.get(DataComponents.CUSTOM_DATA).equals(dropped.get(DataComponents.CUSTOM_DATA)), "break preserves native variant, all actual contents/filter, custom name and unrelated item component");
        drop.setNoPickUpDelay(); drop.playerTouch(player);
        test.assertTrue(drop.isRemoved() && player.getInventory().countItem(block.asItem()) == 1, "actual native item pickup receives the one earned jar");
        ItemStack picked = ItemStack.EMPTY;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) if (player.getInventory().getItem(slot).is(block.asItem())) picked = player.getInventory().removeItemNoUpdate(slot);
        player.setYRot(-90); player.setItemInHand(InteractionHand.MAIN_HAND, roundTrip(test, picked));
        test.assertTrue(use(player, pos.below()).consumesAction(), "physically dropped/picked-up native item is placeable again");
        jar = (TileJar) level.getBlockEntity(pos);
        ItemStack finalDrop = Block.getDrops(level.getBlockState(pos), level, pos, jar).getFirst();
        test.assertTrue(jar.getAmount() == 24 && jar.getAspect() == Aspect.AURA && jar.getFilter() == Aspect.AURA
                && jar.getFacing() == player.getDirection().getOpposite() && finalDrop.is(block.asItem())
                && name.equals(finalDrop.get(DataComponents.CUSTOM_NAME)) && filled.get(DataComponents.CUSTOM_DATA).equals(finalDrop.get(DataComponents.CUSTOM_DATA)), "second native placement has fresh facing and retains the same custom-name/component/content-bearing drop");
    }

    private static ItemStack roundTrip(GameTestHelper test, ItemStack stack) {
        var registries = test.getLevel().registryAccess();
        ItemStack saved = ItemStack.parse(registries, stack.save(registries)).orElseThrow();
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        try {
            ItemStack.STREAM_CODEC.encode(buffer, saved);
            ItemStack packet = ItemStack.STREAM_CODEC.decode(buffer);
            test.assertTrue(ItemStack.matches(stack, saved) && ItemStack.matches(saved, packet) && !buffer.isReadable(), "native persistent and item-packet codecs preserve the exact jar components/count");
            return packet;
        } finally { buffer.release(); }
    }
    private static void rejected(GameTestHelper test, Runnable operation) {
        boolean rejected = false;
        try { operation.run(); } catch (IllegalArgumentException expected) { rejected = true; }
        test.assertTrue(rejected, "invalid jar API contents cannot create or silently truncate units");
    }
    private static ArrayList<ItemStack> blankMatrix() {
        var matrix = new ArrayList<ItemStack>(); for (int slot = 0; slot < 9; slot++) matrix.add(ItemStack.EMPTY); return matrix;
    }
    private static ArcaneRecipeInput column(int column, ItemStack jar) {
        var matrix = blankMatrix(); matrix.set(column, new ItemStack(Items.OBSIDIAN)); matrix.set(column + 3, jar); matrix.set(column + 6, new ItemStack(Items.BLAZE_POWDER));
        return new ArcaneRecipeInput(matrix);
    }
    private static void matrix(TileArcaneWorkbench table, ArcaneRecipeInput input) { for (int slot = 0; slot < 9; slot++) table.setItem(slot, input.getItem(slot).copy()); }
    private static ItemStack wand() {
        ItemStack stack = new ItemStack(ThaumcraftContent.WAND.get()); WandData data = WandData.EMPTY;
        for (String tag : WandData.PRIMALS) data = data.withAmount(tag, 2300);
        stack.set(ThaumcraftContent.WAND_DATA.get(), data); return stack;
    }
    private static InteractionResult use(ServerPlayer player, BlockPos pos) {
        return player.gameMode.useItemOn(player, player.serverLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos).relative(Direction.UP, .5), Direction.UP, pos, false));
    }
}
