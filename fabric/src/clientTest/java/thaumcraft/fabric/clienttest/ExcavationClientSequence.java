package thaumcraft.fabric.clienttest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.client.ThaumcraftFocusClient;
import thaumcraft.client.gui.GuiArcaneWorkbench;
import thaumcraft.client.gui.GuiFocusSelect;
import thaumcraft.client.gui.GuiResearchBrowser;
import thaumcraft.client.gui.GuiResearchRecipe;
import thaumcraft.client.gui.ThaumcraftRecipeDisplay;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftWands;
import thaumcraft.common.config.ThaumcraftWearables;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneCrafting;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.WandVisDiscount;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.ItemFocusExcavation;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.tiles.TileArcaneWorkbench;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TileNode;

/** Six focused scenes; only the root-owned copy of the accepted stopped clothing save is opened. */
public final class ExcavationClientSequence {
    private enum Step {
        HANDOFF, SETUP, BOOK, BOOK_CAPTURE, CHESTS, FOCUS,
        CRAFT_TRAVEL, CRAFT_OPEN, CRAFT_FUND, CRAFT_GRID, CRAFT_READY, CRAFT_INSPECTED,
        CRAFT_TAKE, CRAFT_OUTPUT, CRAFT_STORE, CRAFT_STORED, WAND_PICK, WAND_RETURN, CRAFT_FINISH, CRAFT_ACK,
        FOCUS_ARM, FOCUS_PRESS, RADIAL, RADIAL_HOVER, RADIAL_SELECT, FOCUS_EQUIPPED,
        PLACE_TRAVEL, PLACE_AIM, PLACE_WAIT, OPEN_TRAVEL, OPEN_CONTAINER, CONTENT_PICK, CONTENT_PLACE, CONTENT_WAIT, CONTENT_VERIFY,
        MINE_TRAVEL, MINE_AIM, MINE_READY, MINE_START, MINE_LIVE, MINE_STOP, MINE_VERIFY, MINE_ACK, PAUSE_WAIT,
        PICKUP_TRAVEL, PICKUP_WAIT, PICKUP_ACK, LOOT_SCENE, LOOT_CAPTURE, PROTECTED_SCENE, PROTECTED_CAPTURE,
        FINAL_CHECK, FINAL_ACK, FINAL_SCENE, FINAL_CAPTURE, DONE
    }
    private enum Case { PARTIAL, CONTAINER, ORE, PROTECTED }
    private record Craft(ResourceLocation recipe, Item output, Item[] grid, int repetitions, Map<String, Integer> prices) {}
    private record Progress(BlockPos target, float progress) {}
    private record Frame(Progress progress, int paid, int stopped, boolean using) {}
    private static final List<String> CAPTURES = List.of(
            "89-excavation-original-earth-quartz-emerald-native-book-recipe-en.png",
            "90-earned-excavation-native-three-focus-F-selector-retained-fire-shock-en.png",
            "91-native-continuous-green-excavation-beam-cracks-earned-filled-chest-en.png",
            "92-native-excavated-chest-contents-diamond-and-earned-ore-XP-inventory-en.png",
            "93-native-protection-canceled-excavation-paid-eleven-unchanged-earned-chest-en.png",
            "94-earned-held-excavation-final-finite-vis-saved-robes-and-foci-en.png");
    private static final Map<String, Integer> SAVED_VIS = Map.of("aer", 951, "aqua", 2941, "ignis", 2248, "ordo", 1531, "perditio", 731, "terra", 1796);
    private static final Map<String, Integer> RECIPE_PRICE = Map.of("terra", 1599, "ordo", 399, "perditio", 399);
    private static final List<EquipmentSlot> ARMOR = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    public final Map<String, Object> report = new LinkedHashMap<>();
    private final BlockPos workbench, center, filledChest, protectedChest, ore;
    private final Map<BlockPos, BlockState> protectedBlocks = new LinkedHashMap<>();
    private final Map<BlockPos, CompoundTag> protectedBEs = new LinkedHashMap<>();
    private final Map<BlockPos, Map<String, Object>> savedJars = new LinkedHashMap<>();
    private final Map<BlockPos, String> savedNodes = new LinkedHashMap<>();
    private final List<ItemStack> savedStacks = new ArrayList<>(), savedArmor = new ArrayList<>();
    private final List<Map<String, Object>> inputs = new ArrayList<>(), crafts = new ArrayList<>(), attempts = new ArrayList<>();
    private final ClientTickEvent.Client useGuard = this::guardUse;
    private Step step = Step.HANDOFF, afterCraft;
    private Case mining = Case.PARTIAL;
    private Craft craft;
    private CompletableFuture<Void> serverAction;
    private Minecraft owningClient;
    private TickEvent.Server nativeTicks;
    private LifecycleEvent.ServerState serverStopped;
    private CameraType previousCamera;
    private ItemStack expectedWand, savedShock, savedFire, apple, earnedFocus, craftWand;
    private CompoundTag knowledgeBefore;
    private List<int[]> gridClicks;
    private Map<String, Object> craftPreview;
    private volatile AttemptProbe probe;
    private ProtectionPolicy protection;
    private volatile Throwable observationFailure;
    private Progress pausedProgress;
    private boolean savedInvulnerability, focusChanged, guardingUse, releaseSent;
    private int ticks, totalTicks, turns, gridIndex, craftTaken, outputBefore, destination, placed, castTicks, paidAttempts, oreExperience, initialXp;
    private int handChangedAt = Integer.MIN_VALUE / 2;
    private String focusBinding, swapBinding;

    public ExcavationClientSequence(BlockPos workbench, BlockPos existingCenter) {
        this.workbench = workbench.immutable(); center = existingCenter.immutable();
        filledChest = workbench.offset(5, 0, -3); protectedChest = workbench.offset(10, 0, -3); ore = workbench.offset(5, 0, 5);
        report.put("scope", "excavation: NEW root-owned excavation-smoke-UUID copy of accepted STOPPED clothing-smoke-792ae806-a552-4027-a65e-fb17312d794e; source unchanged");
        report.put("progression_not_replayed", List.of("research acquisition", "all previous metallurgy/alchemy/distillation/tube/infusion/wand acquisition", "Fire/goggles/three robes/dye/wash/anvil/Shock acquisition", "finite node charging"));
        report.put("boundary", "All actual saved inventory/armor/components/knowledge are INPUTS: three wands, installed Shock, loose Fire, worn earned robes and goggles, four empty phials, earned empty Warded Jar, twelve jars/173 essentia,72 candles, book/apple and old machines. RAW INPUTS only:4earth shards,4quartz,1emerald,16oak planks,3cobblestone. FOCUSEXCAVATION+parents are declared research fixtures, not earned. Bounded fresh stone floor/air/camera/daylight/clear weather, one empty new arcane workbench and one diamond-ore TERRAIN block are fixtures. Both ordinary chests are actually crafted and placed; contents enter via real inventory clicks. No supplied chest/focus/diamond/XP/drop, new node, vis top-up, XP grant, incoming damage, Entity invulnerability change, production focus tick/harvest/craft/debit call or FX injection.");
        report.put("native_action_boundary", "Native result PICKUP+inventory clicks perform two ordinary minecraft:chest transactions with paying wand ABSENT, then one paid arcane focus transaction. Actual held F installs genuine Excavation and returns exact saved Shock; Fire unchanged. Real held RMB at six blocks: slow filled chest partial/free release, same-coordinate preserved-progress resume and paid native container harvest; native tier-bypassing diamond ore/drop/XP; one paid protection-canceled earned empty chest. Independent CLIENT_PRE releases at bounded partial progress or first payment observed by passive SERVER_POST, including while capture I/O suspends this sequence. Collision-checked positioning only for native item/XP pickup, never inventory output writes.");
        report.put("protection_fixture", "Temporary REAL Fabric PlayerBlockBreakEvents.BEFORE policy returns false ONLY for this owned player's UUID, level and second actually earned chest position. Never manually dispatch an event. Pinned Fabric has no public unregister: identity-only listener removal under its own event lock rebuilds the current listener array, preserving all other listeners; no production gameplay state is reflected/written.");
        report.put("coordinates", Map.of("workbench", workbench.toShortString(), "existing_center", center.toShortString(), "earned_filled_chest", filledChest.toShortString(), "earned_protected_chest", protectedChest.toShortString(), "ore_terrain_input", ore.toShortString()));
        report.put("fresh_patch", Map.of("min", patchMin().toShortString(), "max", patchMax().toShortString(), "old_loaded_scan_max_X_from_center", 170, "refuse_existing_BE_TC_block_item_XP_or_living_fixture", true));
        report.put("supplied_inputs", inputs); report.put("native_crafts", crafts); report.put("native_attempts", attempts);
        report.put("capture_names", CAPTURES); report.put("capture_count", CAPTURES.size());
        report.put("visual_inspection", "Pending root inspection of exact89-94 PNGs, especially91 WHILE channeling. Active production beam/crack/model probes are not a claim of seen green pixels or independently heard audio.");
        report.put("not_covered", List.of("full survival/research/raw-material acquisition or natural ore frequency", "server timing/negative-hardness/miss/depletion/coordinate-inheritance/lifecycle matrix", "Architectury cancellation, adventure/creative/reentrancy/replacement/modern-block matrix covered separately by server cases, not replayed here", "upgrades/fortune/silk/treasure/full modern block compatibility", "full FX/audio parity", "NeoForge"));
    }

    public boolean tick(Minecraft client, Consumer<String> capture) throws ReflectiveOperationException {
        try { return advance(client, capture); }
        catch (ReflectiveOperationException | RuntimeException | Error error) {
            releaseInputs(client);
            if (client.getSingleplayerServer() != null) client.getSingleplayerServer().execute(this::stopObservation);
            if (error instanceof RuntimeException) throw new IllegalStateException("Excavation sequence failed at " + step + "; case=" + mining + "; recipe=" + (craft == null ? "none" : craft.recipe()), error);
            throw error;
        }
    }

    private boolean advance(Minecraft client, Consumer<String> capture) throws ReflectiveOperationException {
        ++ticks; ++totalTicks;
        require(ticks <= 900 && totalTicks <= 6000, "bounded focused excavation timeout: " + step);
        if (observationFailure != null) throw new IllegalStateException("Passive excavation observer failed", observationFailure);
        if (client.screen instanceof GuiFocusSelect && (step == Step.RADIAL || step == Step.RADIAL_HOVER || step == Step.RADIAL_SELECT)) ThaumcraftFocusClient.CHANGE_FOCUS.setDown(true);
        switch (step) {
            case HANDOFF -> {
                owningClient = client;
                var world = client.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
                require(world.getFileName().toString().startsWith("excavation-smoke-"), "only root's NEW owned excavation world may be prepared, never the accepted source save");
                report.put("actual_owned_world", world.toString());
                previousCamera = client.options.getCameraType(); client.options.setCameraType(CameraType.FIRST_PERSON); client.setScreen(null);
                client.options.keyUse.setDown(false); client.options.keyAttack.setDown(false); client.options.keyShift.setDown(false); releaseFocusKey(client);
                focusBinding = ThaumcraftFocusClient.CHANGE_FOCUS.saveString(); swapBinding = client.options.keySwapOffhand.saveString();
                require(client.player.containerMenu == client.player.inventoryMenu && client.player.containerMenu.getCarried().isEmpty(), "actual accepted inventory and empty cursor");
                require(reward(client.player.getInventory().getItem(8)), "actual saved Thaumium/Silverwood wand in selected hotbar8");
                select(client, 8); onServer(client, () -> prepare(serverPlayer(client))); next(Step.SETUP);
            }
            case SETUP -> {
                if (!serverDone() || ticks < 12 || !KnowledgeAccess.get(client.player).isResearchComplete("FOCUSEXCAVATION")) return false;
                client.setScreen(new GuiResearchRecipe(new GuiResearchBrowser(), ResearchCatalog.get("FOCUSEXCAVATION"))); next(Step.BOOK);
            }
            case BOOK -> { if (ticks < 8 || !showRecipe(client)) return false; next(Step.BOOK_CAPTURE); }
            case BOOK_CAPTURE -> {
                report.put("native_book", Map.of("research", "FOCUSEXCAVATION", "actual_synced_recipe", id("arcane/focus_excavation").toString(), "base_whole_vis", Map.of("terra", 20, "ordo", 5, "perditio", 5), "research_and_screen_are_fixtures", true));
                next(Step.CHESTS); capture.accept(CAPTURES.get(0));
            }
            case CHESTS -> beginCraft(client, new Craft(ResourceLocation.withDefaultNamespace("chest"), Items.CHEST,
                    new Item[] {Items.OAK_PLANKS, Items.OAK_PLANKS, Items.OAK_PLANKS, Items.OAK_PLANKS, null, Items.OAK_PLANKS, Items.OAK_PLANKS, Items.OAK_PLANKS, Items.OAK_PLANKS}, 2, Map.of()), Step.FOCUS);
            case FOCUS -> beginCraft(client, new Craft(id("arcane/focus_excavation"), excavation(),
                    new Item[] {shard(), Items.QUARTZ, shard(), Items.QUARTZ, Items.EMERALD, Items.QUARTZ, shard(), Items.QUARTZ, shard()}, 1, RECIPE_PRICE), Step.FOCUS_ARM);
            case CRAFT_TRAVEL -> {
                if (!serverDone() || ticks < 10 || !(craft.prices().isEmpty() ? emptyHand(client) : hand(client, ExcavationClientSequence::reward))) return false;
                use(client, workbench, Direction.SOUTH, Vec3.atCenterOf(workbench).add(0, 0, .5)); next(Step.CRAFT_OPEN);
            }
            case CRAFT_OPEN -> {
                if (ticks < 8 || !(client.screen instanceof GuiArcaneWorkbench)) return false;
                for (int i = 0; i <= 10; i++) require(arcane(client).getSlot(i).getItem().isEmpty(), "new workbench inputs/wand/preview genuinely empty");
                require(arcane(client).getCarried().isEmpty(), "native empty craft cursor"); craftWand = expectedWand.copy();
                if (!craft.prices().isEmpty()) click(client, playerSlot(client, 8), 0, ClickType.QUICK_MOVE); next(Step.CRAFT_FUND);
            }
            case CRAFT_FUND -> {
                if (ticks < 6 || !craft.prices().isEmpty() && !ItemStack.matches(arcane(client).getSlot(1).getItem(), expectedWand)) return false;
                require(craft.prices().isEmpty() ? arcane(client).getSlot(1).getItem().isEmpty() : client.player.getInventory().getItem(8).isEmpty(), "ordinary chest has NO payer; focus has ONLY actual saved payer");
                gridClicks = recipeClicks(client); gridIndex = 0; craftTaken = 0; next(Step.CRAFT_GRID);
            }
            case CRAFT_GRID -> {
                if (ticks < 2) return false;
                if (gridIndex == gridClicks.size()) { next(Step.CRAFT_READY); return false; }
                int[] at = gridClicks.get(gridIndex++); click(client, at[0], at[1], ClickType.PICKUP); ticks = 0;
            }
            case CRAFT_READY -> {
                if (ticks < 8 || !arcane(client).craftable() || !arcane(client).getSlot(0).getItem().is(craft.output())) return false;
                require(arcane(client).getSlot(0).getItem().getCount() == 1 && arcane(client).getCarried().isEmpty(), "one real native preview, no output injection");
                verifyGrid(arcane(client), craft.repetitions()); onServer(client, () -> inspectCraft(serverPlayer(client))); next(Step.CRAFT_INSPECTED);
            }
            case CRAFT_INSPECTED -> {
                if (!serverDone()) return false;
                for (String tag : WandData.PRIMALS) require(arcane(client).requiredVis(tag) == craft.prices().getOrDefault(tag, 0), "actual native server/client price: " + tag);
                next(Step.CRAFT_TAKE);
            }
            case CRAFT_TAKE -> { outputBefore = count(client.player, craft.output()); click(client, 0, 0, ClickType.PICKUP); next(Step.CRAFT_OUTPUT); }
            case CRAFT_OUTPUT -> {
                if (ticks < 6 || !arcane(client).getCarried().is(craft.output()) || arcane(client).getCarried().getCount() != 1) return false;
                verifyGrid(arcane(client), craft.repetitions() - craftTaken - 1); destination = emptyMain(client.player);
                click(client, playerSlot(client, destination), 0, ClickType.PICKUP); next(Step.CRAFT_STORE);
            }
            case CRAFT_STORE -> {
                if (ticks < 6 || !arcane(client).getCarried().isEmpty() || count(client.player, craft.output()) != outputBefore + 1) return false;
                if (++craftTaken < craft.repetitions()) next(Step.CRAFT_TAKE); else next(Step.CRAFT_STORED);
            }
            case CRAFT_STORED -> {
                if (craft.prices().isEmpty()) next(Step.CRAFT_FINISH);
                else {
                    ItemStack actual = arcane(client).getSlot(1).getItem(); verifyDebit(craftWand, actual, craft.prices()); expectedWand = actual.copy();
                    require(ItemWandCasting.getVis(actual, "terra") == 197, "existing payer alone funded recipe, leaving197terra; NO new charge");
                    click(client, 1, 0, ClickType.PICKUP); next(Step.WAND_PICK);
                }
            }
            case WAND_PICK -> {
                if (ticks < 6 || !ItemStack.matches(arcane(client).getCarried(), expectedWand)) return false;
                destination = emptyMain(client.player); click(client, playerSlot(client, destination), 0, ClickType.PICKUP); next(Step.WAND_RETURN);
            }
            case WAND_RETURN -> {
                if (ticks < 6 || !ItemStack.matches(client.player.getInventory().getItem(destination), expectedWand) || !arcane(client).getCarried().isEmpty()) return false;
                next(Step.CRAFT_FINISH);
            }
            case CRAFT_FINISH -> { onServer(client, () -> finishCraft(serverPlayer(client))); client.player.closeContainer(); next(Step.CRAFT_ACK); }
            case CRAFT_ACK -> { if (!serverDone() || ticks < 8) return false; next(afterCraft); }
            case FOCUS_ARM -> { if (ticks < 10 || !hand(client, ExcavationClientSequence::reward)) return false; next(Step.FOCUS_PRESS); }
            case FOCUS_PRESS -> {
                if (!client.isWindowActive()) return false;
                require(ThaumcraftFocusClient.CHANGE_FOCUS.matches(GLFW.GLFW_KEY_F, 0), "actual native F mapping, no rebinding");
                KeyMapping.click(InputConstants.getKey(focusBinding)); ThaumcraftFocusClient.CHANGE_FOCUS.setDown(true);
                if (ThaumcraftFocusClient.CHANGE_FOCUS.same(client.options.keySwapOffhand)) client.options.keySwapOffhand.setDown(true);
                next(Step.RADIAL);
            }
            case RADIAL -> {
                if (!client.isWindowActive()) { releaseFocusKey(client); next(Step.FOCUS_PRESS); return false; }
                if (ticks < 8 || !(client.screen instanceof GuiFocusSelect)) return false;
                require(ItemStack.matches(FocusData.get(client.player.getMainHandItem()), savedShock) && ItemStack.matches(stack(client.player, excavation()), earnedFocus) && ItemStack.matches(stack(client.player, ThaumcraftFoci.FIRE_FOCUS.get()), savedFire), "three genuine foci: saved Shock installed, saved Fire and newly crafted Excavation loose");
                moveCursor(client, focusPoint(client.screen)); next(Step.RADIAL_HOVER);
            }
            case RADIAL_HOVER -> {
                if (!client.isWindowActive()) { releaseFocusKey(client); next(Step.FOCUS_PRESS); return false; }
                if (ticks < 3 || !(client.screen instanceof GuiFocusSelect)) return false;
                requireCursor(client, focusPoint(client.screen));
                report.put("native_F_selector", Map.of("held_key", focusBinding, "bindings_changed", 0, "loose_order", List.of("Fire AF", "Excavation BE"), "installed_saved_focus", "Shock BL", "capture", "ONE ordinary transparent-screen frame, owned active-window cursor positioned over actual second20x20 choice; root must suppress afterRender re-render"));
                next(Step.RADIAL_SELECT); capture.accept(CAPTURES.get(1));
            }
            case RADIAL_SELECT -> {
                if (!client.isWindowActive()) { releaseFocusKey(client); next(Step.FOCUS_PRESS); return false; }
                require(client.screen instanceof GuiFocusSelect, "actual F selector still open");
                var clickedScreen = client.screen; int[] point = focusPoint(clickedScreen);
                clickedScreen.mouseMoved(point[0], point[1]); require(clickedScreen.mouseClicked(point[0], point[1], 0), "native selector accepts earned Excavation");
                clickedScreen.mouseReleased(point[0], point[1], 0); // onClick closes client.screen; release the SAME screen, not null.
                releaseFocusKey(client); next(Step.FOCUS_EQUIPPED);
            }
            case FOCUS_EQUIPPED -> {
                if (ticks < 10 || !ItemStack.matches(FocusData.get(client.player.getMainHandItem()), earnedFocus) || count(client.player, excavation()) != 0 || count(client.player, ThaumcraftFoci.SHOCK_FOCUS.get()) != 1) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client); ItemStack actual = player.getMainHandItem();
                    require(sameExcept(actual, expectedWand, ThaumcraftFoci.WAND_FOCUS.get()) && ItemStack.matches(FocusData.get(actual), earnedFocus) && ItemStack.matches(stack(player, ThaumcraftFoci.SHOCK_FOCUS.get()), savedShock), "native F changed ONLY focus and returned exact old Shock components");
                    expectedWand = actual.copy(); focusChanged = true; var focus = (ItemFocusBasic) earnedFocus.getItem();
                    require(focus.getFocusColor(earnedFocus) == 409606 && focus.getSortingHelper(earnedFocus).startsWith("BE") && focus.getActivationCooldown(earnedFocus) == 0 && focus.isVisCostPerTick(earnedFocus) && focus.getVisCost(earnedFocus).size() == 1 && focus.getVisCost(earnedFocus).getAmount(Aspect.EARTH) == 15 && focus.getAnimation(earnedFocus) == ItemFocusBasic.WandFocusAnimation.CHARGE, "original basic Excavation metadata, including per-tick tooltip despite completed-attempt payments");
                    report.put("native_focus_switch", Map.of("server_verified", true, "earned_focus_snbt", snbt(earnedFocus, player), "exact_returned_shock_snbt", snbt(savedShock, player), "saved_fire_unchanged", true, "vis_unchanged", true, "direct_focus_writes", 0)); retained(player);
                }); next(Step.PLACE_TRAVEL);
            }
            case PLACE_TRAVEL -> {
                if (!serverDone()) return false;
                onServer(client, () -> stand(serverPlayer(client), placement().getX() + .5, workbench.getY(), placement().getZ() + 2.6)); next(Step.PLACE_AIM);
            }
            case PLACE_AIM -> {
                if (!serverDone() || ticks < 12 || !hand(client, value -> value.is(Items.CHEST))) return false;
                require(client.level.isEmptyBlock(placement()), "earned chest only placed in fresh empty cell");
                use(client, placement().below(), Direction.UP, Vec3.atCenterOf(placement().below()).add(0, .5, 0)); next(Step.PLACE_WAIT);
            }
            case PLACE_WAIT -> {
                if (ticks < 8 || !client.level.getBlockState(placement()).is(Blocks.CHEST)) return false;
                if (++placed < 2) next(Step.PLACE_TRAVEL); else next(Step.OPEN_TRAVEL);
            }
            case OPEN_TRAVEL -> {
                onServer(client, () -> {
                    var player = serverPlayer(client); require(count(player, Items.CHEST) == 0 && chest(player.level().getBlockEntity(filledChest)).isEmpty() && chest(player.level().getBlockEntity(protectedChest)).isEmpty(), "both ordinary earned chests placed, neither supplied or pre-filled");
                    stand(player, filledChest.getX() + .5, workbench.getY(), filledChest.getZ() + 2.6);
                }); next(Step.OPEN_CONTAINER);
            }
            case OPEN_CONTAINER -> {
                if (!serverDone() || ticks < 10 || !emptyHand(client)) return false;
                use(client, filledChest, Direction.SOUTH, Vec3.atCenterOf(filledChest).add(0, 0, .5)); next(Step.CONTENT_PICK);
            }
            case CONTENT_PICK -> {
                if (ticks < 8 || !(client.screen instanceof ContainerScreen) || !(client.player.containerMenu instanceof ChestMenu menu)) return false;
                require(menu.getRowCount() == 3 && menu.getCarried().isEmpty(), "real ordinary single-chest menu");
                for (int i = 0; i < 27; i++) require(menu.getSlot(i).getItem().isEmpty(), "actually empty earned chest, no contents setter");
                click(client, playerSlot(client, itemSlot(client.player, value -> value.is(Items.COBBLESTONE))), 0, ClickType.PICKUP); next(Step.CONTENT_PLACE);
            }
            case CONTENT_PLACE -> {
                if (ticks < 6 || !client.player.containerMenu.getCarried().is(Items.COBBLESTONE) || client.player.containerMenu.getCarried().getCount() != 3) return false;
                click(client, 0, 0, ClickType.PICKUP); next(Step.CONTENT_WAIT);
            }
            case CONTENT_WAIT -> {
                if (ticks < 6 || !client.player.containerMenu.getCarried().isEmpty() || count(client.player, Items.COBBLESTONE) != 0) return false;
                client.player.closeContainer(); onServer(client, () -> {
                    var player = serverPlayer(client); var container = chest(player.level().getBlockEntity(filledChest));
                    require(container.getItem(0).is(Items.COBBLESTONE) && container.getItem(0).getCount() == 3 && count(player, Items.COBBLESTONE) == 0, "real open+inventory clicks put raw3cobble into earned chest");
                    for (int i = 1; i < container.getContainerSize(); i++) require(container.getItem(i).isEmpty(), "no other container contents");
                    report.put("native_earned_containers", Map.of("ordinary_crafted", 2, "real_placed", 2, "filled_position", filledChest.toShortString(), "native_inserted_cobblestone", 3, "actual_filled_BE", beTag(container).toString(), "protected_empty_BE", beTag(chest(player.level().getBlockEntity(protectedChest))).toString(), "container_output_writes", 0));
                }); next(Step.CONTENT_VERIFY);
            }
            case CONTENT_VERIFY -> { if (!serverDone() || ticks < 8) return false; next(Step.MINE_TRAVEL); }
            case MINE_TRAVEL -> {
                if (!serverDone()) return false;
                onServer(client, () -> stand(serverPlayer(client), target().getX() + .5, workbench.getY(), target().getZ() + 6.5)); next(Step.MINE_AIM);
            }
            case MINE_AIM -> {
                if (!serverDone() || !hand(client, ExcavationClientSequence::reward)) return false;
                lookAt(client, Vec3.atCenterOf(target())); if (ticks < 16) return false;
                onServer(client, () -> {
                    try {
                        var player = serverPlayer(client); probe = new AttemptProbe(player, mining, target());
                        if (mining == Case.PROTECTED) { protection = new ProtectionPolicy(player, protectedChest, probe); protection.install(); }
                    } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
                }); next(Step.MINE_READY);
            }
            case MINE_READY -> { if (!serverDone()) return false; next(Step.MINE_START); }
            case MINE_START -> {
                if (!client.isWindowActive()) return false;
                lookAt(client, Vec3.atCenterOf(target())); guardingUse = true; releaseSent = false; castTicks = 0; client.options.keyUse.setDown(true);
                require(client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND).consumesAction(), "actual native RMB starts Excavation CHARGE beyond normal block reach"); next(Step.MINE_LIVE);
            }
            case MINE_LIVE -> {
                if (mining == Case.PARTIAL) {
                    if (probe.frame.progress().progress() < .6F) return false;
                    require(!releaseSent && client.player.isUsingItem() && probe.frame.using() && probe.frame.paid() == 0 && probe.frame.progress().progress() < 1.4F, "capture WHILE genuine unpaid mid-progress channel is live, not after release/completion");
                    var fx = beamEvidence(client);
                    if ((Integer) fx.get("active_production_beams") == 0 || ((List<?>) fx.get("native_cracks")).isEmpty()) return false;
                    report.put("native_continuous_beam_capture", fx); next(Step.MINE_STOP); capture.accept(CAPTURES.get(2));
                } else { if (probe.frame.paid() == 0) return false; next(Step.MINE_STOP); }
            }
            case MINE_STOP -> {
                releaseUse(client); if (client.player.isUsingItem() || probe.frame.stopped() < 3) return false;
                guardingUse = false; next(Step.MINE_VERIFY);
            }
            case MINE_VERIFY -> { onServer(client, () -> finishAttempt(serverPlayer(client))); next(Step.MINE_ACK); }
            case MINE_ACK -> {
                if (!serverDone() || ticks < 8) return false;
                if (mining == Case.PARTIAL) next(Step.PAUSE_WAIT);
                else if (mining == Case.PROTECTED) next(Step.PROTECTED_SCENE);
                else next(Step.PICKUP_TRAVEL);
            }
            case PAUSE_WAIT -> {
                if (ticks < 12) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client); Progress actual = progress(player);
                    require(actual.equals(pausedProgress) && ItemStack.matches(player.getMainHandItem(), expectedWand) && !player.isUsingItem(), "original release-callback mismatch preserves same-coordinate progress and ZERO debit throughout pause");
                    report.put("native_pause", Map.of("server_verified", true, "target", actual.target().toShortString(), "preserved_progress", actual.progress(), "vis_paid", 0, "release_or_progress_setters", 0));
                }); mining = Case.CONTAINER; next(Step.MINE_TRAVEL);
            }
            case PICKUP_TRAVEL -> {
                onServer(client, () -> {
                    var player = serverPlayer(client); var drops = player.serverLevel().getEntitiesOfClass(ItemEntity.class, lootBounds(target()));
                    require(!drops.isEmpty(), "real native physical output exists before collision pickup");
                    var drop = drops.getFirst(); stand(player, drop.getX(), workbench.getY(), drop.getZ());
                }); next(Step.PICKUP_WAIT);
            }
            case PICKUP_WAIT -> {
                if (!serverDone() || ticks < 16) return false;
                require(ticks < 120, "bounded native collision pickup, no playerTouch/inventory/XP setters");
                if (mining == Case.CONTAINER ? count(client.player, Items.CHEST) != 1 || count(client.player, Items.COBBLESTONE) != 3 : count(client.player, Items.DIAMOND) != 1) return false;
                if (mining == Case.ORE && client.player.totalExperience != initialXp + oreExperience) return false;
                onServer(client, () -> verifyPickup(serverPlayer(client))); next(Step.PICKUP_ACK);
            }
            case PICKUP_ACK -> {
                if (!serverDone() || ticks < 8) return false;
                if (mining == Case.CONTAINER) { mining = Case.ORE; next(Step.MINE_TRAVEL); } else next(Step.LOOT_SCENE);
            }
            case LOOT_SCENE -> {
                client.setScreen(new InventoryScreen(client.player)); next(Step.LOOT_CAPTURE);
            }
            case LOOT_CAPTURE -> {
                if (ticks < 10) return false;
                require(count(client.player, Items.CHEST) == 1 && count(client.player, Items.COBBLESTONE) == 3 && count(client.player, Items.DIAMOND) == 1, "actual native chest/contents/ore loot in inventory capture");
                next(Step.PROTECTED_SCENE); capture.accept(CAPTURES.get(3));
                mining = Case.PROTECTED;
            }
            case PROTECTED_SCENE -> {
                client.setScreen(null);
                if (paidAttempts < 3) { next(Step.MINE_TRAVEL); return false; }
                lookAt(client, Vec3.atCenterOf(protectedChest)); next(Step.PROTECTED_CAPTURE);
            }
            case PROTECTED_CAPTURE -> {
                if (ticks < 8) return false;
                require(client.level.getBlockState(protectedChest).is(Blocks.CHEST) && !client.player.isUsingItem() && !client.options.keyUse.isDown(), "actual protected earned chest still visible after one paid/refused native completion and release");
                next(Step.FINAL_CHECK); capture.accept(CAPTURES.get(4));
            }
            case FINAL_CHECK -> { onServer(client, () -> { finalState(serverPlayer(client)); stopObservation(); }); next(Step.FINAL_ACK); }
            case FINAL_ACK -> {
                if (!serverDone() || ClientTickEvent.CLIENT_PRE.isRegistered(useGuard)) return false;
                require(nativeTicks == null && serverStopped == null && protection == null && !ClientTickEvent.CLIENT_PRE.isRegistered(useGuard), "all temporary listeners/observers/guards actually unregistered");
                require(focusBinding.equals(ThaumcraftFocusClient.CHANGE_FOCUS.saveString()) && swapBinding.equals(client.options.keySwapOffhand.saveString()), "user's focus and vanilla swap bindings unchanged");
                report.put("native_held_model", heldModel(client)); report.put("native_client_ticks", totalTicks);
                client.setScreen(new InventoryScreen(client.player)); next(Step.FINAL_SCENE);
            }
            case FINAL_SCENE -> {
                if (ticks < 8 || !client.isWindowActive()) return false;
                moveCursor(client, inventoryPoint(client, itemSlot(client.player, ExcavationClientSequence::reward))); next(Step.FINAL_CAPTURE);
            }
            case FINAL_CAPTURE -> {
                if (ticks < 3) return false;
                requireCursor(client, inventoryPoint(client, itemSlot(client.player, ExcavationClientSequence::reward)));
                next(Step.DONE); capture.accept(CAPTURES.get(5));
            }
            case DONE -> { client.setScreen(null); releaseInputs(client); return true; }
        }
        return false;
    }

    private void prepare(ServerPlayer player) {
        require(workbench.equals(center.offset(176, 0, 0)), "root passes clothing bench+32X:1650,216,377 / saved center1474,216,377");
        var level = player.serverLevel(); expectedWand = player.getInventory().getItem(8).copy(); savedShock = FocusData.get(expectedWand).copy();
        savedFire = stack(player, ThaumcraftFoci.FIRE_FOCUS.get()).copy(); apple = player.getOffhandItem().copy(); savedInvulnerability = player.isInvulnerable(); initialXp = player.totalExperience;
        require(player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL && savedInvulnerability, "actual Survival and saved Entity Invulnerable1 fixture unchanged, no incoming damage needed");
        require(reward(expectedWand) && ItemWandCasting.data(expectedWand).vis().equals(SAVED_VIS) && expectedWand.getOrDefault(ThaumcraftWands.WAND_CRAFT_COST.get(), 0) == 54 && savedShock.is(ThaumcraftFoci.SHOCK_FOCUS.get()), "EXACT accepted clothing payer INPUT, no reconstruction or top-up");
        require(count(player, ThaumcraftContent.WAND.get()) == 3 && count(player, ThaumcraftFoci.FIRE_FOCUS.get()) == 1 && count(player, ThaumcraftFoci.SHOCK_FOCUS.get()) == 0 && count(player, ThaumcraftContent.EMPTY_PHIAL.get()) == 4 && count(player, ThaumcraftContent.FILLED_PHIAL.get()) == 0 && count(player, ThaumcraftContent.JAR.get().asItem()) == 1 && apple.is(Items.APPLE) && apple.getCount() == 1, "actual old three wands/Fire/four phials/earned empty jar/apple");
        List<Item> armorItems = List.of(ThaumcraftWearables.GOGGLES.get(), ThaumcraftWearables.ROBE_CHESTPLATE.get(), ThaumcraftWearables.ROBE_LEGGINGS.get(), ThaumcraftWearables.ROBE_BOOTS.get());
        for (int i = 0; i < ARMOR.size(); i++) {
            ItemStack actual = player.getItemBySlot(ARMOR.get(i)); require(actual.is(armorItems.get(i)) && actual.getDamageValue() == (i >= 2 ? 1 : 0), "actual earned worn goggles/robes with accepted damage0/0/1/1"); savedArmor.add(actual.copy());
        }
        require(savedArmor.get(1).getOrDefault(DataComponents.REPAIR_COST, 0) == 1 && WandVisDiscount.percentagePoints(player, Aspect.EARTH) == 10, "accepted repaired chestCost1/full worn10points");
        for (ItemStack actual : player.getInventory().items) if (!actual.isEmpty() && !reward(actual)) savedStacks.add(actual.copy());
        for (Item item : List.of(shard(), Items.QUARTZ, Items.EMERALD, Items.OAK_PLANKS, Items.COBBLESTONE, Items.CHEST, Items.DIAMOND, excavation())) require(count(player, item) == 0, "no saved raw/output stack hidden in new acquisition: " + itemId(item));
        for (int x = (center.getX() - 12) >> 4; x <= (center.getX() + 170) >> 4; x++) for (int z = (center.getZ() - 12) >> 4; z <= (center.getZ() + 12) >> 4; z++) level.getChunk(x, z);
        for (BlockPos at : BlockPos.betweenClosed(center.offset(-10, 0, -10), center.offset(170, 6, 11))) {
            BlockState state = level.getBlockState(at); BlockEntity tile = level.getBlockEntity(at);
            if (BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals("thaumcraft")) protectedBlocks.put(at.immutable(), state);
            if (tile instanceof TileNode node) savedNodes.put(at.immutable(), node.getScanId()); // Ordinary saved-node regeneration remains native; never charge from it.
            else if (tile != null) protectedBEs.put(at.immutable(), beTag(tile));
            if (tile instanceof TileJar jar) savedJars.put(at.immutable(), jarState(jar));
        }
        require(savedJars.size() == 12 && savedJars.values().stream().mapToInt(value -> (Integer) value.get("amount")).sum() == 173, "all actual saved twelve jars/173essentia");
        require(protectedBlocks.values().stream().filter(value -> value.is(ThaumcraftInfusion.TALLOW_CANDLE.get())).count() == 72, "all actual saved72candles");
        require(level.getBlockEntity(center.offset(144, 0, 0)) instanceof TileArcaneWorkbench oldBench && oldBench.isEmpty() && level.getBlockEntity(center.offset(149, 2, -3)) instanceof TileNode, "extended scan includes old clothing workbench/node beyond old+130 bound");
        require(level.getBlockEntity(center.above(2)) instanceof TileInfusionMatrix matrix && matrix.isActive() && !matrix.isCrafting(), "accepted old formed idle altar");
        report.put("actual_saved_inputs", Map.of("main_stacks_snbt", player.getInventory().items.stream().filter(value -> !value.isEmpty()).map(value -> snbt(value, player)).toList(), "armor_snbt", savedArmor.stream().map(value -> snbt(value, player)).toList(), "payer_snbt", snbt(expectedWand, player), "offhand_snbt", snbt(apple, player), "saved_total_xp", initialXp, "entity_invulnerable", savedInvulnerability, "protected_TC_blocks", protectedBlocks.size(), "protected_BE_data", protectedBEs.size(), "saved_node_regeneration", "native allowed; never charged/topped up", "acquisition_replayed", false));
        patch(player); level.setBlockAndUpdate(workbench, ThaumcraftArcane.ARCANE_WORKBENCH.get().defaultBlockState()); level.setBlockAndUpdate(ore, Blocks.DIAMOND_ORE.defaultBlockState());
        require(((TileArcaneWorkbench) level.getBlockEntity(workbench)).isEmpty(), "fixture new workbench genuinely empty");
        long oldTime = level.getDayTime(); level.setDayTime(6000); level.setWeatherParameters(0, 12000, false, false);
        inputs.add(Map.of("fixture", "bounded fresh floor/air/new empty workbench/daylight/clear weather/collision-checked camera", "old_day_time", oldTime, "new_day_time", 6000));
        inputs.add(Map.of("fixture", "ONE diamond_ore TERRAIN INPUT, not diamond loot or naturally found ore", "position", ore.toShortString()));
        var knowledge = KnowledgeAccess.get(player); CompoundTag original = knowledge.save().copy(); var keys = new LinkedHashSet<String>();
        keys.addAll(ResearchCatalog.get("FOCUSEXCAVATION").parents()); keys.addAll(ResearchCatalog.get("FOCUSEXCAVATION").parentsHidden()); keys.add("FOCUSEXCAVATION");
        var already = new LinkedHashMap<String, Boolean>(); for (String key : keys) { already.put(key, knowledge.isResearchComplete(key)); knowledge.addResearch(key); }
        knowledgeBefore = knowledge.save().copy(); KnowledgeEvents.sync(player);
        report.put("research_fixture", Map.of("keys", List.copyOf(keys), "already_complete_before", already, "earned_here", false, "original_knowledge_snbt", original.toString(), "after_declared_fixture_snbt", knowledgeBefore.toString()));
        supply(player, shard(), 4); supply(player, Items.QUARTZ, 4); supply(player, Items.EMERALD, 1); supply(player, Items.OAK_PLANKS, 16); supply(player, Items.COBBLESTONE, 3);
        player.inventoryMenu.broadcastChanges(); retained(player); startObservation(player);
    }

    private void inspectCraft(ServerPlayer player) {
        var table = (TileArcaneWorkbench) player.level().getBlockEntity(workbench); var match = ArcaneCrafting.find(player, table);
        require(match != null && match.recipe().id().equals(craft.recipe()) && match.output().is(craft.output()) && match.output().getCount() == 1 && match.arcane() == !craft.prices().isEmpty() && match.prices().equals(craft.prices()), "actual native recipe and exact actor-priced preview");
        require(WandVisDiscount.percentagePoints(player, Aspect.EARTH) == 10, "all saved earned armor remains worn throughout acquisition");
        if (craft.prices().isEmpty()) require(table.getItem(10).isEmpty() && ItemStack.matches(stack(player, ExcavationClientSequence::reward), expectedWand), "minecraft:chest native fallback with paying wand ABSENT, no vis debit");
        else require(ItemStack.matches(table.getItem(10), craftWand) && ItemStack.matches(FocusData.get(table.getItem(10)), savedShock) && craft.prices().equals(RECIPE_PRICE), "actual old Shock retained through1599terra/399ordo/399perditio arcane craft");
        craftPreview = Map.of("recipe", match.recipe().id().toString(), "transactions", craft.repetitions(), "native_centivis_each", match.prices(), "wand_absent", table.getItem(10).isEmpty(), "actual_preview_snbt", snbt(match.output(), player), "payer_before_snbt", snbt(craftWand, player));
    }

    private void finishCraft(ServerPlayer player) {
        require(((TileArcaneWorkbench) player.level().getBlockEntity(workbench)).isEmpty() && player.containerMenu.getCarried().isEmpty(), "native result retrieval leaves actual table/cursor empty");
        if (craft.output() == Items.CHEST) require(count(player, Items.CHEST) == 2 && count(player, Items.OAK_PLANKS) == 0 && ItemStack.matches(stack(player, ExcavationClientSequence::reward), expectedWand), "16raw planks became two ordinary earned chests via two native extraction transactions, zero vis");
        else {
            earnedFocus = stack(player, excavation()).copy(); require(count(player, excavation()) == 1 && count(player, shard()) == 0 && count(player, Items.QUARTZ) == 0 && count(player, Items.EMERALD) == 0, "4earth+4quartz+1emerald consumed by actual paid focus extraction");
            require(ItemStack.matches(stack(player, ExcavationClientSequence::reward), expectedWand), "actual paid wand returned natively");
        }
        var entry = new LinkedHashMap<String, Object>(craftPreview); entry.put("server_verified", true); entry.put("native_result_PICKUP_clicks", craftTaken); entry.put("payer_after_snbt", snbt(expectedWand, player)); entry.put("output_grants", 0); crafts.add(entry); retained(player);
    }

    private void finishAttempt(ServerPlayer player) {
        try {
            Map<String, Object> result = probe.finish();
            if (mining == Case.PARTIAL) { pausedProgress = probe.frame.progress(); report.put("native_partial_free_release", result); }
            else { attempts.add(result); ++paidAttempts; }
            if (mining == Case.ORE) oreExperience = probe.spawnedExperience;
            if (mining == Case.PROTECTED) {
                require(protection != null && protection.calls == 1 && protection.paidBeforeCallback, "ONE real Fabric protection callback after actual11terra payment");
                protection.close(); require(!protection.registered, "actual protection listener removed, not just disabled"); protection = null;
                report.put("native_protected_paid_refusal", result);
            }
            expectedWand = player.getMainHandItem().copy(); retained(player);
        } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }

    /** A passive server POST probe. It never calls the focus, a harvest backend, drops or XP code. */
    private final class AttemptProbe {
        final ServerPlayer player; final Case kind; final BlockPos at; final ItemStack wand, original; final WandData before;
        final BlockState state; final BlockEntity tile; final CompoundTag tileData; final float hardness, increment, exhaustion;
        final int xpBefore, minedBefore; final long started; final double range;
        final List<Map<String, Object>> trace = new ArrayList<>();
        volatile Frame frame; Progress previous; boolean active = true, used, resumedIncrement, sawNewTargetZero; int paid, stopped, spawnedExperience;
        List<Map<String, Object>> committedDrops = List.of();

        AttemptProbe(ServerPlayer player, Case kind, BlockPos at) throws ReflectiveOperationException {
            this.player = player; this.kind = kind; this.at = at.immutable(); wand = player.getMainHandItem(); original = wand.copy(); before = ItemWandCasting.data(wand);
            state = player.level().getBlockState(at); tile = player.level().getBlockEntity(at); tileData = tile == null ? null : beTag(tile);
            hardness = state.getDestroySpeed(player.level(), at); increment = kind == Case.ORE ? .25F : .05F;
            exhaustion = player.getFoodData().getExhaustionLevel(); xpBefore = player.totalExperience; minedBefore = player.getStats().getValue(Stats.BLOCK_MINED.get(state.getBlock())); started = player.serverLevel().getGameTime();
            previous = progress(player); frame = new Frame(previous, 0, 0, false); range = player.getEyePosition().distanceTo(Vec3.atCenterOf(at));
            require(ItemStack.matches(wand, expectedWand) && FocusData.get(wand).is(excavation()) && !player.isUsingItem() && before.amount("terra") >= 11, "genuine saved finite payer with actual earned focus, not a fabricated use session");
            require(range > 4.5 && range < 10 && !player.canInteractWithBlock(at, 0), "real spell target beyond ordinary4.5 block reach but inside10: " + range);
            var hit = player.level().clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(player.getLookAngle().scale(10)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            require(hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(at), "real10block collider ray hits intended earned chest/ore");
            require(kind == Case.ORE ? state.is(Blocks.DIAMOND_ORE) && hardness == 3F && tile == null : state.is(Blocks.CHEST) && hardness == 2.5F && tile instanceof ChestBlockEntity, "original slow wood versus fast rock fixture");
            require(player.serverLevel().getEntitiesOfClass(ItemEntity.class, lootBounds(at)).isEmpty() && xpOnGround(player.serverLevel(), at) == 0, "isolated genuine pre-harvest physical-output baseline");
            if (kind == Case.CONTAINER) require(previous.equals(pausedProgress) && previous.target().equals(at) && previous.progress() > 0, "resume begins with original paused progress, not reset/new target");
        }

        void observe() throws ReflectiveOperationException {
            require(player.getMainHandItem() == wand && sameExcept(wand, original, ThaumcraftContent.WAND_DATA.get()) && player.serverLevel().getGameTime() - started <= 240, "same actual focused wand/components, bounded native use");
            WandData now = ItemWandCasting.data(wand); int expense = before.amount("terra") - now.amount("terra");
            require(expense == 0 || expense == 11, "NO interim debit; only one completed attempt pays11terra, NOT12");
            for (String tag : WandData.PRIMALS) if (!tag.equals("terra")) require(now.amount(tag) == before.amount(tag), "Excavation leaves every other primal unchanged");
            Progress actual = progress(player); boolean using = player.isUsingItem();
            if (using) { require(player.getUseItem() == wand && player.getUsedItemHand() == InteractionHand.MAIN_HAND, "real native main-hand CHARGE state"); used = true; }
            if (expense == 0) {
                require(player.level().getBlockState(at) == state && player.level().getBlockEntity(at) == tile && (tile == null || beTag(tile).equals(tileData)), "partial progress never changes block/container/data");
                require(player.serverLevel().getEntitiesOfClass(ItemEntity.class, lootBounds(at)).isEmpty() && xpOnGround(player.serverLevel(), at) == 0 && player.totalExperience == xpBefore, "no free interim item/XP outputs");
                if (at.equals(actual.target())) {
                    if (!at.equals(previous.target())) { require(actual.progress() == 0F, "new coordinates start at0 WITHOUT increment"); sawNewTargetZero = true; }
                    else if (actual.progress() != previous.progress()) {
                        require(Math.abs(actual.progress() - previous.progress() - increment) < .00001F, "actual native original material increment: " + increment);
                        if (kind == Case.CONTAINER) resumedIncrement = true;
                    }
                }
            } else if (paid == 0) {
                require(kind != Case.PARTIAL && used && at.equals(previous.target()) && previous.progress() >= hardness, "completion checked BEFORE increment; exactly11 paid only after old progress reaches hardness");
                paid = 1;
                if (kind == Case.PROTECTED) require(player.level().getBlockState(at) == state && player.level().getBlockEntity(at) == tile && beTag(tile).equals(tileData) && chest(tile).isEmpty(), "paid cancellation preserves exact earned chest state/BE/data");
                else require(player.level().isEmptyBlock(at) && player.level().getBlockEntity(at) == null, "native committed removal really happened, not merely a true backend return");
                committedDrops = dropEvidence(player.serverLevel(), at);
                if (kind == Case.CONTAINER) require(dropCount(player.serverLevel(), at, Items.CHEST) == 1 && dropCount(player.serverLevel(), at, Items.COBBLESTONE) == 3 && player.totalExperience == xpBefore && xpOnGround(player.serverLevel(), at) == 0 && resumedIncrement, "native one chest+three actual contents, no XP; same-coordinate resume really incremented preserved progress");
                if (kind == Case.ORE) {
                    require(dropCount(player.serverLevel(), at, Items.DIAMOND) == 1 && dropCount(player.serverLevel(), at, Items.DIAMOND_ORE) == 0, "native fortune0/no-silk diamond loot bypasses ordinary wand tool tier");
                    spawnedExperience = xpOnGround(player.serverLevel(), at) + player.totalExperience - xpBefore;
                    require(spawnedExperience >= 3 && spawnedExperience <= 7, "native diamond ore produces3..7 actual XP, never an XP grant");
                }
            }
            if (kind == Case.PROTECTED) require(player.totalExperience == xpBefore && player.serverLevel().getEntitiesOfClass(ItemEntity.class, lootBounds(at)).isEmpty() && xpOnGround(player.serverLevel(), at) == 0, "protected paid attempt produces ZERO items/XP");
            require(player.getStats().getValue(Stats.BLOCK_MINED.get(state.getBlock())) == minedBefore && player.getFoodData().getExhaustionLevel() == exhaustion, "native spell harvest adds no mining stats/exhaustion");
            stopped = used && !using ? stopped + 1 : 0;
            if (used && trace.size() < 160) trace.add(Map.of("server_tick", player.serverLevel().getGameTime(), "using", using, "target", actual.target() == null ? "none" : actual.target().toShortString(), "progress", actual.progress(), "terra", now.amount("terra"), "paid", paid, "block_present", player.level().getBlockState(at) == state));
            previous = actual; frame = new Frame(actual, paid, stopped, using);
        }

        Map<String, Object> finish() throws ReflectiveOperationException {
            require(used && stopped >= 3 && !player.isUsingItem(), "actual native use/release reached server and stopped");
            if (kind == Case.PARTIAL) require(paid == 0 && ItemStack.matches(wand, original) && at.equals(frame.progress().target()) && frame.progress().progress() >= .6F && frame.progress().progress() < hardness && sawNewTargetZero, "bounded partial release preserves genuine progress with zero payment and no harvest");
            else require(paid == 1 && before.amount("terra") - ItemWandCasting.getVis(wand, "terra") == 11, "one actual completed attempt paid exactly11 before harvest/refusal");
            active = false;
            var result = new LinkedHashMap<String, Object>(); result.put("server_verified", true); result.put("case", kind.name()); result.put("target", at.toShortString()); result.put("range_from_native_eye", range);
            result.put("hardness", hardness); result.put("original_increment", increment); result.put("native_POST_trace", List.copyOf(trace)); result.put("paid_attempts", paid); result.put("terra_paid", paid * 11);
            result.put("wand_before", before.vis()); result.put("wand_after", ItemWandCasting.data(wand).vis()); result.put("actual_committed_physical_drops", committedDrops); result.put("native_ore_XP", spawnedExperience);
            result.put("saved_container_BE_before", tileData == null ? "none" : tileData.toString()); result.put("native_mining_stat_delta", player.getStats().getValue(Stats.BLOCK_MINED.get(state.getBlock())) - minedBefore); result.put("native_exhaustion_delta", player.getFoodData().getExhaustionLevel() - exhaustion);
            if (kind == Case.PROTECTED) { result.put("real_Fabric_BEFORE_calls", protection.calls); result.put("paid_before_callback", protection.paidBeforeCallback); result.put("unchanged_BE_after", beTag(tile).toString()); }
            return result;
        }
    }

    private void verifyPickup(ServerPlayer player) {
        try {
            require(player.serverLevel().getEntitiesOfClass(ItemEntity.class, lootBounds(target())).isEmpty(), "all real nearby native item drops picked up by collision");
            if (mining == Case.CONTAINER) require(count(player, Items.CHEST) == 1 && count(player, Items.COBBLESTONE) == 3 && player.totalExperience == initialXp, "actual pickup returned earned chest and raw container contents without XP");
            else require(count(player, Items.DIAMOND) == 1 && player.totalExperience == initialXp + oreExperience && xpOnGround(player.serverLevel(), ore) == 0, "actual collision/orb pickup earned exactly native3..7oreXP");
            report.put(mining == Case.CONTAINER ? "native_container_pickup" : "native_ore_pickup", Map.of("server_verified", true, "chests", count(player, Items.CHEST), "cobblestone", count(player, Items.COBBLESTONE), "diamonds", count(player, Items.DIAMOND), "total_XP_before_story", initialXp, "total_XP_now", player.totalExperience, "artificial_item_or_XP_outputs", 0)); retained(player);
        } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }

    private void retained(ServerPlayer player) {
        for (ItemStack saved : savedStacks) {
            int before = savedStacks.stream().filter(value -> ItemStack.isSameItemSameComponents(value, saved)).mapToInt(ItemStack::getCount).sum();
            int after = player.getInventory().items.stream().filter(value -> ItemStack.isSameItemSameComponents(value, saved)).mapToInt(ItemStack::getCount).sum();
            require(before == after, "old main stack count/components unchanged: " + saved);
        }
        for (int i = 0; i < ARMOR.size(); i++) require(ItemStack.matches(player.getItemBySlot(ARMOR.get(i)), savedArmor.get(i)), "all saved earned robes/goggles stay WORN and exact, including old damage/repairCost/color");
        require(count(player, ThaumcraftContent.WAND.get()) == 3 && ItemStack.matches(stack(player, ExcavationClientSequence::reward), expectedWand), "three real wands retained; only explicitly recorded native payer transactions changed vis/focus");
        require(ItemStack.matches(player.getOffhandItem(), apple) && player.isInvulnerable() == savedInvulnerability, "actual old apple and Entity Invulnerable fixture unchanged");
        require(ItemStack.matches(FocusData.get(expectedWand), focusChanged ? earnedFocus : savedShock), "actual installed focus exact");
        if (focusChanged) require(ItemStack.matches(stack(player, ThaumcraftFoci.SHOCK_FOCUS.get()), savedShock), "complete old Shock returned exactly");
        require(ItemStack.matches(stack(player, ThaumcraftFoci.FIRE_FOCUS.get()), savedFire), "old loose Fire exact");
        for (var entry : protectedBlocks.entrySet()) require(player.level().getBlockState(entry.getKey()) == entry.getValue(), "all old TC blocks/72candles/nodes/machines preserved through+170X: " + entry.getKey());
        for (var entry : protectedBEs.entrySet()) require(player.level().getBlockEntity(entry.getKey()) != null && beTag(player.level().getBlockEntity(entry.getKey())).equals(entry.getValue()), "old container/machine BE data exact: " + entry.getKey());
        for (var entry : savedJars.entrySet()) require(player.level().getBlockEntity(entry.getKey()) instanceof TileJar jar && jarState(jar).equals(entry.getValue()), "all12saved jars/types/filters/173units unchanged");
        for (var entry : savedNodes.entrySet()) require(player.level().getBlockEntity(entry.getKey()) instanceof TileNode node && node.getScanId().equals(entry.getValue()), "same old saved node identity, native regeneration allowed but no charge");
        require(knowledgeBefore.equals(KnowledgeAccess.get(player).save()), "all prior knowledge/discovery/pools exact beyond declared Excavation research fixture");
    }

    private void finalState(ServerPlayer player) {
        retained(player);
        require(paidAttempts == 3 && attempts.size() == 3 && !player.isUsingItem() && player.containerMenu == player.inventoryMenu && player.containerMenu.getCarried().isEmpty(), "partial-free/resumed chest/ore/ONE protected completion finished and native controls/menu stopped");
        require(count(player, Items.CHEST) == 1 && count(player, Items.COBBLESTONE) == 3 && count(player, Items.DIAMOND) == 1 && chest(player.level().getBlockEntity(protectedChest)).isEmpty() && player.level().isEmptyBlock(filledChest) && player.level().isEmptyBlock(ore), "two real earned chests accounted:one recovered,one protected; actual contents/diamond recovered");
        require(probe.kind == Case.PROTECTED && player.level().getBlockState(protectedChest) == probe.state && player.level().getBlockEntity(protectedChest) == probe.tile && beTag(probe.tile).equals(probe.tileData), "paid protected chest retains exact final state/BE identity/inventory data after policy removal");
        for (Item item : List.of(shard(), Items.QUARTZ, Items.EMERALD, Items.OAK_PLANKS, excavation())) require(count(player, item) == 0, "finite raw inputs consumed; earned focus installed, no loose duplicate");
        require(ItemWandCasting.data(expectedWand).vis().equals(Map.of("aer", 951, "aqua", 2941, "ignis", 2248, "ordo", 1132, "perditio", 332, "terra", 164)), "exact saved finite vis minus1599/399/399recipe and3x11completed attempts");
        require(player.totalExperience == initialXp + oreExperience && oreExperience >= 3 && oreExperience <= 7 && ((TileArcaneWorkbench) player.level().getBlockEntity(workbench)).isEmpty(), "only actual ore XP earned; actual new workbench empty");
        for (BlockPos at : BlockPos.betweenClosed(patchMin(), new BlockPos(patchMax().getX(), workbench.getY() - 1, patchMax().getZ()))) require(player.level().getBlockState(at).is(Blocks.STONE), "release guards prevented mining ANY fixture floor after completion");
        report.put("native_net_outputs", Map.of("ordinary_chests_crafted", 2, "recovered_earned_chest", 1, "protected_earned_chest", 1, "actual_container_cobblestone_recovered", 3, "native_diamonds", 1, "native_ore_XP", oreExperience, "paid_completed_attempts", paidAttempts, "native_attempt_terra_debit", 33, "final_actual_wand_snbt", snbt(expectedWand, player), "new_charge_or_XP_grants", 0));
        report.put("retained_prior_outputs", Map.of("server_verified", true, "old_main_counts_components_exact", true, "two_old_gold_wands_unchanged", true, "three_wands_total", 3, "all_saved_robes_and_goggles_worn_exact", true, "Fire_and_Shock_exact", true, "all12jars_173units_and72candles_preserved", true, "protected_old_TC_blocks", protectedBlocks.size(), "old_BE_data_exact", protectedBEs.size(), "prior_acquisition_replayed", false));
    }

    private void startObservation(ServerPlayer player) {
        nativeTicks = server -> {
            if (server != player.server) return;
            try { if (probe != null && probe.active) probe.observe(); }
            catch (ReflectiveOperationException | RuntimeException | Error error) { observationFailure = error; stopObservation(); }
        };
        serverStopped = server -> { if (server == player.server) stopObservation(); };
        TickEvent.SERVER_POST.register(nativeTicks); LifecycleEvent.SERVER_STOPPED.register(serverStopped); ClientTickEvent.CLIENT_PRE.register(useGuard);
    }
    private void stopObservation() {
        try { if (protection != null) { protection.close(); protection = null; } }
        catch (ReflectiveOperationException | RuntimeException error) { if (observationFailure == null) observationFailure = error; else observationFailure.addSuppressed(error); }
        finally {
            if (nativeTicks != null) { TickEvent.SERVER_POST.unregister(nativeTicks); nativeTicks = null; }
            if (serverStopped != null) { LifecycleEvent.SERVER_STOPPED.unregister(serverStopped); serverStopped = null; }
            if (probe != null) probe.active = false;
            // An observer failure can occur while screenshot I/O has paused
            // advance(). Keep the PRE guard until this CLIENT-thread cleanup
            // actually releases RMB; never unregister it and leave a held key.
            if (owningClient != null) owningClient.execute(() -> releaseInputs(owningClient));
            else ClientTickEvent.CLIENT_PRE.unregister(useGuard);
        }
    }

    /** This is policy installation, NOT a fake event, harvest adapter or production test switch. */
    private final class ProtectionPolicy implements AutoCloseable {
        final ServerPlayer owner; final BlockPos at; final AttemptProbe attempt; final PlayerBlockBreakEvents.Before listener;
        Field phaseListeners; Method rebuild; Object lock; boolean registered, enabled = true, paidBeforeCallback; int calls;
        ProtectionPolicy(ServerPlayer owner, BlockPos at, AttemptProbe attempt) {
            this.owner = owner; this.at = at.immutable(); this.attempt = attempt;
            listener = (world, actor, pos, state, tile) -> {
                if (!enabled || world != owner.level() || !actor.getUUID().equals(owner.getUUID()) || !pos.equals(this.at)) return true;
                ++calls; paidBeforeCallback = ItemWandCasting.getVis(actor.getMainHandItem(), "terra") == attempt.before.amount("terra") - 11;
                if (calls != 1 || !paidBeforeCallback || state != attempt.state || tile != attempt.tile) observationFailure = new IllegalStateException("Native protection must receive the exact earned chest/BE ONCE, after payment");
                return false;
            };
        }
        void install() throws ReflectiveOperationException {
            var event = PlayerBlockBreakEvents.BEFORE;
            require(event.getClass().getName().equals("net.fabricmc.fabric.impl.base.event.ArrayBackedEvent"), "pinned Fabric event shape required for safe temporary listener cleanup");
            lock = BookClientSmoke.field(event, "lock"); rebuild = event.getClass().getDeclaredMethod("rebuildInvoker", int.class); rebuild.setAccessible(true);
            phaseListeners = Class.forName("net.fabricmc.fabric.impl.base.event.EventPhaseData").getDeclaredField("listeners"); phaseListeners.setAccessible(true);
            synchronized (lock) { event.register(listener); registered = true; }
        }
        @Override public void close() throws ReflectiveOperationException {
            enabled = false;
            if (!registered) return;
            var event = PlayerBlockBreakEvents.BEFORE;
            synchronized (lock) {
                var phases = (Map<?, ?>) BookClientSmoke.field(event, "phases"); int removed = 0;
                for (Object phase : phases.values()) {
                    Object old = phaseListeners.get(phase); int length = Array.getLength(old), index = -1;
                    for (int i = 0; i < length; i++) if (Array.get(old, i) == listener) { require(index == -1, "owned listener registered only once"); index = i; }
                    if (index < 0) continue;
                    Object kept = Array.newInstance(old.getClass().getComponentType(), length - 1); int next = 0;
                    for (int i = 0; i < length; i++) if (i != index) Array.set(kept, next++, Array.get(old, i));
                    phaseListeners.set(phase, kept); ++removed;
                }
                require(removed == 1, "remove ONLY this policy's actual listener identity, preserving all other current listeners/phases");
                rebuild.invoke(event, Array.getLength(BookClientSmoke.field(event, "handlers")) - removed);
                for (Object handler : (Object[]) BookClientSmoke.field(event, "handlers")) require(handler != listener, "temporary real Fabric policy no longer registered");
                registered = false;
            }
        }
    }

    /** Read-only frozen production API; observation never initializes progress. */
    private static Progress progress(ServerPlayer player) {
        var view = ItemFocusExcavation.progress(player);
        if (view == null) return new Progress(null, 0F);
        return new Progress(view.pos().immutable(), view.amount());
    }
    private Map<String, Object> beamEvidence(Minecraft client) throws ReflectiveOperationException {
        int count = ((Number) Class.forName("thaumcraft.client.ThaumcraftExcavationClient").getMethod("activeBeamCount").invoke(null)).intValue();
        var cracks = new ArrayList<Map<String, Object>>();
        for (Object value : ((Map<?, ?>) BookClientSmoke.field(client.levelRenderer, "destroyingBlocks")).values()) {
            if (value instanceof BlockDestructionProgress crack && crack.getPos().equals(filledChest) && crack.getProgress() > 0 && crack.getProgress() <= 8) cracks.add(Map.of("position", crack.getPos().toShortString(), "actual_native_stage", crack.getProgress()));
        }
        return Map.of("active_production_beams", count, "native_cracks", cracks, "actual_server_progress", probe.frame.progress().progress(), "actual_server_payer_terra", ItemWandCasting.getVis(client.player.getMainHandItem(), "terra"), "paid_attempts", probe.frame.paid(), "native_using", client.player.isUsingItem(), "production_color_contract", "00FF66; root must inspect actual91 pixels", "synthetic_FX_or_crack_calls", 0);
    }

    private void guardUse(Minecraft client) {
        // Runs independently of the root's screenshot-pending sequence pause.
        AttemptProbe actual = probe;
        if (actual != null && actual.active && client.player != null && client.screen == null) lookAt(client, Vec3.atCenterOf(actual.at));
        if (guardingUse && !releaseSent && client.player != null && (observationFailure != null || !client.isWindowActive() || client.screen != null || ++castTicks >= 180 || actual != null && (actual.frame.paid() > 0 || actual.kind == Case.PARTIAL && actual.frame.progress().progress() >= 1.4F))) releaseUse(client);
    }
    private void releaseUse(Minecraft client) {
        client.options.keyUse.setDown(false);
        if (!releaseSent && client.gameMode != null && client.player != null) { client.gameMode.releaseUsingItem(client.player); releaseSent = true; }
    }
    private void releaseInputs(Minecraft client) {
        guardingUse = false; ClientTickEvent.CLIENT_PRE.unregister(useGuard); releaseFocusKey(client);
        client.options.keyUse.setDown(false); client.options.keyAttack.setDown(false); client.options.keyShift.setDown(false);
        if (client.gameMode != null && client.player != null && client.player.isUsingItem()) client.gameMode.releaseUsingItem(client.player);
        if (previousCamera != null) client.options.setCameraType(previousCamera);
    }
    private static void releaseFocusKey(Minecraft client) { ThaumcraftFocusClient.CHANGE_FOCUS.setDown(false); client.options.keySwapOffhand.setDown(false); }

    private void beginCraft(Minecraft client, Craft plan, Step following) {
        require(serverDone(), "no overlapping native craft/server action"); client.setScreen(null); craft = plan; afterCraft = following;
        onServer(client, () -> stand(serverPlayer(client), workbench.getX() + .5, workbench.getY(), workbench.getZ() + 2.6)); next(Step.CRAFT_TRAVEL);
    }
    private List<int[]> recipeClicks(Minecraft client) {
        var clicks = new ArrayList<int[]>(); var groups = new LinkedHashMap<Item, List<Integer>>();
        for (int cell = 0; cell < 9; cell++) if (craft.grid()[cell] != null) for (int n = 0; n < craft.repetitions(); n++) groups.computeIfAbsent(craft.grid()[cell], ignored -> new ArrayList<>()).add(cell + 2);
        for (var group : groups.entrySet()) {
            int placed = 0;
            for (int inventory = 0; inventory < 36 && placed < group.getValue().size(); inventory++) {
                ItemStack actual = client.player.getInventory().getItem(inventory); if (!actual.is(group.getKey())) continue;
                int amount = Math.min(actual.getCount(), group.getValue().size() - placed), source = playerSlot(client, inventory); clicks.add(new int[] {source, 0});
                for (int n = 0; n < amount; n++) clicks.add(new int[] {group.getValue().get(placed++), 1});
                if (actual.getCount() > amount) clicks.add(new int[] {source, 0});
            }
            require(placed == group.getValue().size(), "actual finite raw inputs supply native grid: " + itemId(group.getKey()));
        }
        return clicks;
    }
    private void verifyGrid(ContainerArcaneWorkbench menu, int amount) {
        for (int i = 0; i < 9; i++) { ItemStack actual = menu.getSlot(i + 2).getItem(); Item expected = craft.grid()[i]; require(expected == null || amount == 0 ? actual.isEmpty() : actual.is(expected) && actual.getCount() == amount, "native grid/raw consumption exactly matches recipe cell" + i); }
    }
    private static void verifyDebit(ItemStack before, ItemStack after, Map<String, Integer> price) {
        require(sameExcept(before, after, ThaumcraftContent.WAND_DATA.get()), "actual native craft preserves all non-vis wand components/focus");
        WandData first = ItemWandCasting.data(before), last = ItemWandCasting.data(after); require(first.rod().equals(last.rod()) && first.cap().equals(last.cap()), "same actual saved wand materials");
        for (String tag : WandData.PRIMALS) require(first.amount(tag) - last.amount(tag) == price.getOrDefault(tag, 0), "native exact recipe debit: " + tag);
    }

    private BlockPos patchMin() { return workbench.offset(-4, -2, -10); }
    private BlockPos patchMax() { return workbench.offset(14, 5, 12); }
    private void patch(ServerPlayer player) {
        var level = player.serverLevel(); BlockPos min = patchMin(), max = patchMax();
        for (int x = min.getX() >> 4; x <= max.getX() >> 4; x++) for (int z = min.getZ() >> 4; z <= max.getZ() >> 4; z++) level.getChunk(x, z);
        var bounds = new AABB(Vec3.atLowerCornerOf(min), Vec3.atLowerCornerOf(max.offset(1, 1, 1)));
        require(level.getEntitiesOfClass(ItemEntity.class, bounds).isEmpty() && level.getEntitiesOfClass(ExperienceOrb.class, bounds).isEmpty() && level.getEntitiesOfClass(LivingEntity.class, bounds).isEmpty(), "fresh patch must not erase old physical outputs/XP/living fixtures");
        for (BlockPos at : BlockPos.betweenClosed(min, max)) require(level.getBlockEntity(at) == null && !BuiltInRegistries.BLOCK.getKey(level.getBlockState(at).getBlock()).getNamespace().equals("thaumcraft"), "reject ANY old BE/node/machine/TC block overwrite: " + at);
        for (BlockPos at : BlockPos.betweenClosed(min, max)) level.setBlockAndUpdate(at, at.getY() < workbench.getY() ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
    }
    private void supply(ServerPlayer player, Item item, int amount) {
        int slot = emptyMain(player); require(amount > 0 && amount <= item.getDefaultInstance().getMaxStackSize(), "normal bounded RAW INPUT stack");
        player.getInventory().setItem(slot, new ItemStack(item, amount)); inputs.add(Map.of("item", itemId(item), "count", amount, "genuinely_empty_main_slot", slot, "purpose", "RAW INPUT, not crafted/harvested output"));
    }
    private boolean showRecipe(Minecraft client) throws ReflectiveOperationException {
        int page = (int) BookClientSmoke.field(client.screen, "page"); var pages = (List<?>) BookClientSmoke.field(client.screen, "pages");
        for (int i = page; i < Math.min(page + 2, pages.size()); i++) if (BookClientSmoke.field(pages.get(i), "recipe") instanceof ThaumcraftRecipeDisplay display && display.recipe().id().equals(id("arcane/focus_excavation"))) {
            require(display.recipe() == client.level.getRecipeManager().byKey(id("arcane/focus_excavation")).orElseThrow(), "book renders actual synced native recipe holder"); return true;
        }
        require(++turns <= 16, "bounded native book navigation"); var button = client.screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(value -> value.active && value.getMessage().getString().equals("→")).findFirst().orElseThrow();
        var clickedScreen = client.screen; clickedScreen.mouseClicked(button.getX() + 5, button.getY() + 5, 0); clickedScreen.mouseReleased(button.getX() + 5, button.getY() + 5, 0); ticks = 0; return false;
    }
    private static int[] focusPoint(Screen screen) {
        require(screen instanceof GuiFocusSelect, "actual radial required");
        var buttons = screen.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast).filter(value -> value.getWidth() == 20 && value.getHeight() == 20).toList();
        require(buttons.size() == 2, "exactly two real loose-focus choices:FireAF then ExcavationBE; ShockBL remains installed");
        var earned = buttons.get(1); return new int[] {earned.getX() + 10, earned.getY() + 10};
    }
    private static void moveCursor(Minecraft client, int[] point) {
        client.screen.mouseMoved(point[0], point[1]);
        GLFW.glfwSetCursorPos(client.getWindow().getWindow(), point[0] * (double) client.getWindow().getScreenWidth() / client.screen.width, point[1] * (double) client.getWindow().getScreenHeight() / client.screen.height);
    }
    private static void requireCursor(Minecraft client, int[] point) {
        double x = client.mouseHandler.xpos() * client.screen.width / client.getWindow().getScreenWidth(), y = client.mouseHandler.ypos() * client.screen.height / client.getWindow().getScreenHeight();
        require(Math.abs(x - point[0]) <= 1 && Math.abs(y - point[1]) <= 1, "real owned active-window cursor actually hovers intended native widget/slot");
    }
    private static int[] inventoryPoint(Minecraft client, int inventory) throws ReflectiveOperationException {
        var slot = client.player.containerMenu.getSlot(playerSlot(client, inventory));
        return new int[] {(int) BookClientSmoke.field(client.screen, "leftPos") + slot.x + 8, (int) BookClientSmoke.field(client.screen, "topPos") + slot.y + 8};
    }
    private static Map<String, Object> heldModel(Minecraft client) {
        ItemStack actual = client.player.getMainHandItem(); require(reward(actual) && FocusData.get(actual).is(excavation()), "final genuinely held earned Excavation");
        var model = client.getItemRenderer().getModel(actual, client.level, client.player, 0); require(model != client.getModelManager().getMissingModel(), "native focused wand model resolves");
        var sprites = new LinkedHashSet<String>(); int focusFaces = 0;
        for (var quad : model.getQuads(null, null, RandomSource.create(0))) {
            String sprite = quad.getSprite().contents().name().toString(); sprites.add(sprite);
            if (sprite.equals("thaumcraft:models/wand") && quad.getTintIndex() == 0) ++focusFaces;
        }
        int color = ((thaumcraft.common.mixin.client.MinecraftItemColorsAccess) client).thaumcraft$itemColors().getColor(actual, 0);
        // Held foci are the original shared models/wand cube + focus RGB, NOT
        // the flat items/focus_excavation icon used for a loose focus/selector.
        require(focusFaces == 6 && color == (0xFF000000 | 409606)
                && sprites.contains("thaumcraft:models/wand_rod_silverwood") && sprites.contains("thaumcraft:models/wand_cap_thaumium")
                && sprites.stream().noneMatch(value -> value.contains("missingno")),
                "actual earned held model has original six-face focus cube/Excavation RGB and unchanged Silverwood/Thaumium materials; sprites=" + sprites + "; argb=" + Integer.toHexString(color));
        return Map.of("actual_sprites", List.copyOf(sprites), "focus", "thaumcraft:focus_excavation", "native_focus_tint_argb", Integer.toHexString(color),
                "tinted_focus_faces", focusFaces, "seen_pixels", "pending root94 inspection");
    }

    private boolean hand(Minecraft client, Predicate<ItemStack> wanted) throws ReflectiveOperationException {
        require(client.player.containerMenu == client.player.inventoryMenu, "native hand SWAP outside menus only");
        if (!wanted.test(client.player.getInventory().getItem(8))) { click(client, playerSlot(client, itemSlot(client.player, wanted)), 8, ClickType.SWAP); handChangedAt = client.player.tickCount; return false; }
        if (client.player.tickCount - handChangedAt < 6) return false; select(client, 8); return true;
    }
    private boolean emptyHand(Minecraft client) throws ReflectiveOperationException {
        require(client.player.containerMenu == client.player.inventoryMenu, "empty hand via actual inventory SWAP");
        if (!client.player.getInventory().getItem(8).isEmpty()) { click(client, playerSlot(client, emptyMain(client.player)), 8, ClickType.SWAP); handChangedAt = client.player.tickCount; return false; }
        if (client.player.tickCount - handChangedAt < 6) return false; select(client, 8); return true;
    }
    private static int playerSlot(Minecraft client, int inventory) {
        return client.player.containerMenu.slots.stream().filter(slot -> slot.container == client.player.getInventory() && slot.getContainerSlot() == inventory).findFirst().orElseThrow().index;
    }
    private static void stand(ServerPlayer player, double x, double y, double z) {
        var body = new AABB(x - .3, y + .001, z - .3, x + .3, y + 1.8, z + .3); BlockPos floor = BlockPos.containing(x, y - .01, z);
        require(player.level().noCollision(player, body) && !player.level().getBlockState(floor).getCollisionShape(player.level(), floor).isEmpty(), "declared camera/pickup fixture collision-free above solid floor"); player.teleportTo(x, y, z);
    }
    private static void use(Minecraft client, BlockPos at, Direction face, Vec3 point) {
        require(!client.gameMode.hasInfiniteItems() && client.player.canInteractWithBlock(at, 0), "real native Survival placement/container reach");
        lookAt(client, point); require(client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, new BlockHitResult(point, face, at, false)).consumesAction(), "actual native block use: " + at);
    }
    private static void lookAt(Minecraft client, Vec3 point) { Vec3 d = point.subtract(client.player.getEyePosition()); client.player.setYRot((float) Math.toDegrees(Math.atan2(-d.x, d.z))); client.player.yRotO = client.player.getYRot(); client.player.setXRot((float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)))); client.player.xRotO = client.player.getXRot(); }
    private static void select(Minecraft client, int slot) throws ReflectiveOperationException { client.player.getInventory().selected = slot; var ensure = client.gameMode.getClass().getDeclaredMethod("ensureHasSentCarriedItem"); ensure.setAccessible(true); ensure.invoke(client.gameMode); }
    private static void click(Minecraft client, int slot, int button, ClickType type) { client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId, slot, button, type, client.player); }
    /** Detached assertion copies only: neither copy is ever installed in an inventory. */
    private static boolean sameExcept(ItemStack first, ItemStack second, DataComponentType<?>... excluded) { ItemStack a = first.copy(), b = second.copy(); for (var type : excluded) { a.remove(type); b.remove(type); } return ItemStack.matches(a, b); }
    private static AABB lootBounds(BlockPos at) { return new AABB(at).inflate(3); }
    private static int dropCount(ServerLevel level, BlockPos at, Item item) { return level.getEntitiesOfClass(ItemEntity.class, lootBounds(at)).stream().map(ItemEntity::getItem).filter(value -> value.is(item)).mapToInt(ItemStack::getCount).sum(); }
    private static List<Map<String, Object>> dropEvidence(ServerLevel level, BlockPos at) {
        return level.getEntitiesOfClass(ItemEntity.class, lootBounds(at)).stream().<Map<String, Object>>map(entity -> Map.of("uuid", entity.getUUID().toString(), "actual_stack_snbt", entity.getItem().save(level.registryAccess()).toString(), "position", entity.position().toString())).toList();
    }
    private static int xpOnGround(ServerLevel level, BlockPos at) throws ReflectiveOperationException {
        int total = 0; for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, lootBounds(at))) total += orb.getValue() * (int) BookClientSmoke.field(orb, "count"); return total;
    }
    private static CompoundTag beTag(BlockEntity tile) { return tile.saveWithFullMetadata(tile.getLevel().registryAccess()).copy(); }
    private static ChestBlockEntity chest(BlockEntity tile) { require(tile instanceof ChestBlockEntity, "actual earned ordinary chest BE"); return (ChestBlockEntity) tile; }
    private static Map<String, Object> jarState(TileJar jar) { return Map.of("aspect", jar.getAspect() == null ? "" : jar.getAspect().getTag(), "amount", jar.getAmount(), "filter", jar.getFilter() == null ? "" : jar.getFilter().getTag()); }
    private BlockPos placement() { return placed == 0 ? filledChest : protectedChest; }
    private BlockPos target() { return mining == Case.ORE ? ore : mining == Case.PROTECTED ? protectedChest : filledChest; }
    private static boolean reward(ItemStack value) { return value.is(ThaumcraftContent.WAND.get()) && ItemWandCasting.data(value).rod().equals("silverwood") && ItemWandCasting.data(value).cap().equals("thaumium"); }
    private static int emptyMain(Player player) { for (int i = 9; i < 36; i++) if (player.getInventory().getItem(i).isEmpty()) return i; throw new IllegalStateException("No genuinely empty main slot"); }
    private static int itemSlot(Player player, Predicate<ItemStack> wanted) { for (int i = 0; i < 36; i++) if (wanted.test(player.getInventory().getItem(i))) return i; throw new IllegalStateException("Missing actual finite Excavation input/output"); }
    private static ItemStack stack(Player player, Item item) { return stack(player, value -> value.is(item)); }
    private static ItemStack stack(Player player, Predicate<ItemStack> wanted) { return player.getInventory().getItem(itemSlot(player, wanted)); }
    private static int count(Player player, Item item) { return player.getInventory().items.stream().filter(value -> value.is(item)).mapToInt(ItemStack::getCount).sum(); }
    private static Item excavation() { return ThaumcraftFoci.EXCAVATION_FOCUS.get(); }
    private static Item shard() { return ThaumcraftContent.SHARDS.get("terra").get(); }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }
    private static String itemId(Item item) { return BuiltInRegistries.ITEM.getKey(item).toString(); }
    private static String snbt(ItemStack value, ServerPlayer player) { return value.save(player.registryAccess()).toString(); }
    private static ContainerArcaneWorkbench arcane(Minecraft client) { return (ContainerArcaneWorkbench) client.player.containerMenu; }
    private static ServerPlayer serverPlayer(Minecraft client) { return client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID()); }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private void onServer(Minecraft client, Runnable action) { require(serverAction == null || serverAction.isDone(), "no overlapping fixture/read-only server actions"); serverAction = CompletableFuture.runAsync(action, client.getSingleplayerServer()); }
    private boolean serverDone() { if (serverAction == null) return true; if (!serverAction.isDone()) return false; serverAction.join(); return true; }
    private void next(Step next) { step = next; ticks = 0; }
}
