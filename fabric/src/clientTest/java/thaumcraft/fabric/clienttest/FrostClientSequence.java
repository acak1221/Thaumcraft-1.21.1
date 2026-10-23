package thaumcraft.fabric.clienttest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
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
import thaumcraft.common.config.ThaumcraftFrost;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftWands;
import thaumcraft.common.config.ThaumcraftWearables;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneCrafting;
import thaumcraft.common.entities.projectile.EntityFrostShard;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.WandVisDiscount;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.WandFocusActions;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.tiles.TileArcaneWorkbench;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.api.nodes.NodeType;

/** Five bounded scenes in root's NEW private copy; no old acquisition is replayed. */
public final class FrostClientSequence {
    private enum Step {
        HANDOFF, SETUP, BOOK, BOOK_CAPTURE, CHARGE_TRAVEL, CHARGE_AIM, CHARGE_READY, CHARGE_START, CHARGE_USE, CHARGE_STOP, CHARGE_ACK,
        CRAFT_TRAVEL, CRAFT_OPEN, CRAFT_FUND, CRAFT_GRID, CRAFT_READY, CRAFT_PREVIEW, CRAFT_TAKE, CRAFT_OUTPUT, CRAFT_STORED,
        WAND_PICK, WAND_RETURN, CRAFT_FINISH, CRAFT_ACK, FOCUS_ARM, FOCUS_PRESS, RADIAL, RADIAL_HOVER, RADIAL_SELECT, FOCUS_EQUIPPED,
        SHOT_TRAVEL, SHOT_AIM, SHOT_READY, SHOT_START, SHOT_LIVE, SHOT_WAIT, SHOT_FINISH, SHOT_ACK,
        FINAL_CHECK, FINAL_ACK, FINAL_SCENE, FINAL_CAPTURE, DONE
    }
    private record Frame(int paid, int id, boolean live, int impacts, int hurts, float health, int stopped) {}
    private static final List<String> CAPTURES = List.of(
            "95-frost-original-water-quartz-saved-earned-diamond-native-book-recipe-en.png",
            "96-earned-frost-native-four-focus-F-selector-retained-fire-shock-excavation-en.png",
            "97-native-paid-frost-shard-visible-flight-and-observed-wall-ricochet-en.png",
            "98-native-frost-shard-thrown-three-damage-target-hit-no-freeze-en.png",
            "99-earned-held-frost-final-finite-vis-retained-saved-inputs-en.png");
    private static final Map<String, Integer> SAVED_VIS = Map.of("aer", 951, "aqua", 2941, "ignis", 2248, "ordo", 1132, "perditio", 332, "terra", 164);
    private static final Map<String, Integer> RECIPE_PRICE = Map.of("aqua", 799, "ordo", 799, "perditio", 799);
    private static final Map<String, Integer> SHOT_PRICE = Map.of("aqua", 3, "ignis", 1, "perditio", 1);
    private static final List<EquipmentSlot> ARMOR = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
    public final Map<String, Object> report = new LinkedHashMap<>();
    private final BlockPos workbench, center, chargingNode, wall, cowPosition;
    private final Map<BlockPos, BlockState> oldBlocks = new LinkedHashMap<>(), fixtureBlocks = new LinkedHashMap<>();
    private final Map<BlockPos, CompoundTag> oldBEs = new LinkedHashMap<>();
    private final Map<BlockPos, String> oldNodes = new LinkedHashMap<>();
    private final List<ItemStack> savedStacks = new ArrayList<>(), savedArmor = new ArrayList<>();
    private final List<Map<String, Object>> inputs = new ArrayList<>(), shots = new ArrayList<>(), clientFrames = new ArrayList<>();
    private final ClientTickEvent.Client preGuard = this::guardInput, postGuard = this::observeClient;
    private Step step = Step.HANDOFF;
    private Minecraft owningClient;
    private CameraType previousCamera;
    private CompletableFuture<Void> serverAction;
    private TickEvent.Server nativeTicks;
    private LifecycleEvent.ServerState serverStopped;
    private EntityEvent.Add nativeAdds;
    private ItemStack expectedWand, chargedWand, craftWand, savedExcavation, savedFire, savedShock, savedDiamond, apple, earnedFocus;
    private CompoundTag knowledgeBefore;
    private volatile ChargeProbe charge;
    private volatile ShotProbe shot;
    private volatile Throwable observationFailure;
    private Cow ownedCow;
    private List<int[]> gridClicks;
    private String focusBinding, swapBinding, particlesBefore;
    private boolean savedInvulnerability, diamondConsumed, focusChanged, aimCharge, chargingHeld, guardingShot, releaseSent;
    private int ticks, totalTicks, turns, gridIndex, destination, shotIndex, targetId = -1, initialXp, guardTicks;
    private int handChangedAt = Integer.MIN_VALUE / 2;

    public FrostClientSequence(BlockPos workbench, BlockPos existingCenter) {
        this.workbench = workbench.immutable(); center = existingCenter.immutable(); chargingNode = center.offset(149, 2, -3);
        wall = workbench.offset(11, 0, -8); cowPosition = workbench.offset(1, 0, -9);
        report.put("scope", "frost: NEW root-owned frost-smoke-UUID copy of accepted STOPPED excavation-smoke-dc8878ce-5af1-4825-b4d8-c2869791f6e5; never open/modify accepted source");
        report.put("progression_not_replayed", List.of("research and all earlier acquisition", "three wands/Fire/Shock/Excavation", "robes/goggles/173 jar units/72 candles", "ordinary earned chests/container recovery/diamond mining/four earned ore XP"));
        report.put("boundary", "ACTUAL saved stacks/components/armor/knowledge are INPUTS. Consume the ONE actual saved earned diamond, never supply another. RAW NEW INPUTS ONLY:4water shards+4quartz. FOCUSFROST+parents are research fixtures, not earned. New empty workbench, separate bounded stone-floor/air patch, wide stone-brick wall, one ordinary no-AI cow with declared native scale1.5, daylight/weather and collision-checked camera are fixtures. Charge by NEW real held RMB from actual saved NORMAL node1623,218,374, not a new node or vis top-up. Saved Entity Invulnerable1 remains unchanged; owner ricochet is not survival damage proof.");
        report.put("native_action_boundary", "Native result/inventory clicks craft799aqua/ordo/perditio; real held F installs Frost and returns exact Excavation. TWO single native useItem activations pay3aqua/1ignis/1perditio each, one wall/one cow. Independent CLIENT_PRE releases on first native debit/ADD even while capture I/O suspends story; shot RMB is never held for autorepeat. Camera held in PRE and POST before native charge packets. Passive SERVER_POST/ADD and ordinary tracked client entities only: no backend craft/charge/focus tick, projectile construction/reposition/velocity/damage writes, RNG seed, clock freeze, synthetic impact/FX or iframe reset.");
        report.put("coordinates", Map.of("workbench", workbench.toShortString(), "existing_center", center.toShortString(), "ACTUAL_saved_node", chargingNode.toShortString(), "wall", wall.toShortString(), "cow", cowPosition.toShortString()));
        report.put("fresh_patch", Map.of("min", patchMin().toShortString(), "max", patchMax().toShortString(), "old_scan_max_X_from_center", 202, "reject_old_BE_TC_block_item_XP_living_or_shard", true));
        report.put("supplied_inputs", inputs); report.put("native_paid_shots", shots); report.put("native_client_tracking", clientFrames);
        report.put("capture_names", CAPTURES); report.put("capture_count", CAPTURES.size());
        report.put("visual_inspection", "Pending root inspection of exact95-99 PNGs.97 is requested on FIRST eligible live tracked flight frame; subsequent native ricochet is observed, not necessarily pictured in97.98 waits for actual client hurt presentation. Never re-render transparent selector in afterRender.");
        report.put("cleanup_boundary", "After observed flight, discard only identity-recorded new shards and this new cow. New floor/wall/empty workbench remain declared fixtures in owned copy; all scanned old blocks/BEs and old entities are never cleanup targets. The old native cactus at1623,216,382 may naturally change AGE/grow to at most three blocks; its base/type and any already-existing cactus blocks must survive. No random-tick/RNG changes or restoration writes.");
        report.put("not_covered", List.of("full survival/material/research/XP acquisition", "cooldown/mode/PvP/refused damage/ADD cancellation/launch Gaussian ordering/physics/save matrix: separate server cases, NOT replayed here", "upgrades/slowness/freezing/block transformation", "full legacy mesh/FX/audio parity or independently heard sound", "NeoForge"));
    }

    public boolean tick(Minecraft client, Consumer<String> capture) throws ReflectiveOperationException {
        try { return advance(client, capture); }
        catch (ReflectiveOperationException | RuntimeException | Error error) {
            releaseInputs(client); if (client.getSingleplayerServer() != null) client.getSingleplayerServer().execute(this::stopObservation);
            if (error instanceof RuntimeException) throw new IllegalStateException("Frost sequence failed at " + step + "; shot=" + shotIndex, error);
            throw error;
        }
    }

    private boolean advance(Minecraft client, Consumer<String> capture) throws ReflectiveOperationException {
        ++ticks; ++totalTicks; require(ticks <= 1800 && totalTicks <= 4200, "bounded Frost sequence timeout:" + step);
        if (observationFailure != null) throw new IllegalStateException("Passive Frost observer failed", observationFailure);
        orient(client);
        if (client.screen instanceof GuiFocusSelect && (step == Step.RADIAL || step == Step.RADIAL_HOVER || step == Step.RADIAL_SELECT)) ThaumcraftFocusClient.CHANGE_FOCUS.setDown(true);
        switch (step) {
            case HANDOFF -> {
                owningClient = client; var world = client.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
                String name = world.getFileName().toString(); require(name.startsWith("frost-smoke-"), "only root's NEW owned frost world, never accepted source"); UUID.fromString(name.substring("frost-smoke-".length()));
                require(world.getParent().getFileName().toString().equals("saves") && world.getParent().getParent().getFileName().toString().equals("run-clienttest"), "isolated client-test save only"); report.put("actual_owned_world", world.toString());
                previousCamera = client.options.getCameraType(); client.options.setCameraType(CameraType.FIRST_PERSON); client.setScreen(null);
                client.options.keyUse.setDown(false); client.options.keyAttack.setDown(false); client.options.keyShift.setDown(false); releaseFocusKey(client);
                focusBinding = ThaumcraftFocusClient.CHANGE_FOCUS.saveString(); swapBinding = client.options.keySwapOffhand.saveString();
                require(client.player.containerMenu == client.player.inventoryMenu && client.player.containerMenu.getCarried().isEmpty() && payer(client.player.getInventory().getItem(8)), "actual accepted payer8 and empty native cursor");
                select(client); onServer(client, () -> prepare(serverPlayer(client))); next(Step.SETUP);
            }
            case SETUP -> { if (!serverDone() || ticks < 12 || !KnowledgeAccess.get(client.player).isResearchComplete("FOCUSFROST")) return false; client.setScreen(new GuiResearchRecipe(new GuiResearchBrowser(), ResearchCatalog.get("FOCUSFROST"))); next(Step.BOOK); }
            case BOOK -> { if (ticks < 8 || !showRecipe(client)) return false; next(Step.BOOK_CAPTURE); }
            case BOOK_CAPTURE -> { report.put("native_book", Map.of("research", "FOCUSFROST", "synced_recipe", id("arcane/focus_frost").toString(), "center", "ACTUAL saved earned diamond", "base_whole_vis", Map.of("aqua", 10, "ordo", 10, "perditio", 10), "screen_research_fixtures", true)); next(Step.CHARGE_TRAVEL); capture.accept(CAPTURES.get(0)); }
            case CHARGE_TRAVEL -> { client.setScreen(null); aimCharge = true; onServer(client, () -> stand(serverPlayer(client), chargingNode.getX() + .5, workbench.getY(), chargingNode.getZ() + 3.5)); next(Step.CHARGE_AIM); }
            case CHARGE_AIM -> { if (!serverDone() || !hand(client) || ticks < 16) return false; onServer(client, () -> charge = new ChargeProbe(serverPlayer(client))); next(Step.CHARGE_READY); }
            case CHARGE_READY -> { if (!serverDone()) return false; next(Step.CHARGE_START); }
            case CHARGE_START -> { if (!client.isWindowActive()) return false; orient(client); releaseSent = false; chargingHeld = true; guardTicks = 0; client.options.keyUse.setDown(true); require(client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND).consumesAction(), "NEW real RMB starts actual saved-node use despite installed Excavation"); next(Step.CHARGE_USE); }
            case CHARGE_USE -> { if (!charge.funded) return false; releaseUse(client); next(Step.CHARGE_STOP); }
            case CHARGE_STOP -> { releaseUse(client); if (ticks < 6 || client.player.isUsingItem() || charge.stopped < 2) return false; onServer(client, () -> { report.put("native_finite_recharge", charge.finish()); expectedWand = chargedWand = serverPlayer(client).getMainHandItem().copy(); retained(serverPlayer(client)); }); next(Step.CHARGE_ACK); }
            case CHARGE_ACK -> { if (!serverDone()) return false; aimCharge = false; client.setScreen(null); onServer(client, () -> stand(serverPlayer(client), workbench.getX() + .5, workbench.getY(), workbench.getZ() + 2.6)); next(Step.CRAFT_TRAVEL); }
            case CRAFT_TRAVEL -> { if (!serverDone() || ticks < 10 || !hand(client)) return false; Vec3 point = Vec3.atCenterOf(workbench).add(0, 0, .5); lookAt(client, point); require(client.player.canInteractWithBlock(workbench, 0) && client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, new BlockHitResult(point, Direction.SOUTH, workbench, false)).consumesAction(), "native workbench open"); next(Step.CRAFT_OPEN); }
            case CRAFT_OPEN -> { if (ticks < 8 || !(client.screen instanceof GuiArcaneWorkbench)) return false; for (int i = 0; i <= 10; i++) require(arcane(client).getSlot(i).getItem().isEmpty(), "genuinely empty new table/preview"); craftWand = expectedWand.copy(); click(client, playerSlot(client, 8), 0, ClickType.QUICK_MOVE); next(Step.CRAFT_FUND); }
            case CRAFT_FUND -> { if (ticks < 6 || !ItemStack.matches(arcane(client).getSlot(1).getItem(), craftWand)) return false; require(client.player.getInventory().getItem(8).isEmpty(), "only actual saved payer moved to table"); gridClicks = recipeClicks(client); gridIndex = 0; next(Step.CRAFT_GRID); }
            case CRAFT_GRID -> { if (ticks < 2) return false; if (gridIndex == gridClicks.size()) { next(Step.CRAFT_READY); return false; } int[] at = gridClicks.get(gridIndex++); click(client, at[0], at[1], ClickType.PICKUP); ticks = 0; }
            case CRAFT_READY -> {
                if (ticks < 8 || !arcane(client).craftable() || !arcane(client).getSlot(0).getItem().is(frost())) return false;
                verifyGrid(arcane(client), 1); require(arcane(client).getCarried().isEmpty() && arcane(client).getSlot(0).getItem().getCount() == 1, "one actual native result preview");
                onServer(client, () -> { var player = serverPlayer(client); var table = (TileArcaneWorkbench) player.level().getBlockEntity(workbench); var match = ArcaneCrafting.find(player, table);
                    require(match != null && match.arcane() && match.recipe().id().equals(id("arcane/focus_frost")) && match.prices().equals(RECIPE_PRICE) && match.output().is(frost()), "actual actor-priced native799/799/799 recipe");
                    require(ItemStack.matches(table.getItem(4), savedDiamond) && count(player, Items.DIAMOND) == 0 && ItemStack.matches(table.getItem(10), craftWand), "ONLY saved diamond is in real recipe center; exact payer/Excavation retained");
                    report.put("native_craft_preview", Map.of("recipe", match.recipe().id().toString(), "centivis", match.prices(), "actual_saved_diamond_snbt", snbt(table.getItem(4), player), "payer_before", snbt(craftWand, player), "preview", snbt(match.output(), player))); }); next(Step.CRAFT_PREVIEW);
            }
            case CRAFT_PREVIEW -> { if (!serverDone()) return false; for (String tag : WandData.PRIMALS) require(arcane(client).requiredVis(tag) == RECIPE_PRICE.getOrDefault(tag, 0), "native client/server exact float-truncated recipe price:" + tag); next(Step.CRAFT_TAKE); }
            case CRAFT_TAKE -> { click(client, 0, 0, ClickType.PICKUP); next(Step.CRAFT_OUTPUT); }
            case CRAFT_OUTPUT -> { if (ticks < 6 || !arcane(client).getCarried().is(frost()) || arcane(client).getCarried().getCount() != 1) return false; verifyGrid(arcane(client), 0); destination = emptyMain(client.player); click(client, playerSlot(client, destination), 0, ClickType.PICKUP); next(Step.CRAFT_STORED); }
            case CRAFT_STORED -> { if (ticks < 6 || !arcane(client).getCarried().isEmpty() || count(client.player, frost()) != 1) return false; ItemStack actual = arcane(client).getSlot(1).getItem(); verifyDebit(craftWand, actual, RECIPE_PRICE); expectedWand = actual.copy(); click(client, 1, 0, ClickType.PICKUP); next(Step.WAND_PICK); }
            case WAND_PICK -> { if (ticks < 6 || !ItemStack.matches(arcane(client).getCarried(), expectedWand)) return false; destination = emptyMain(client.player); click(client, playerSlot(client, destination), 0, ClickType.PICKUP); next(Step.WAND_RETURN); }
            case WAND_RETURN -> { if (ticks < 6 || !ItemStack.matches(client.player.getInventory().getItem(destination), expectedWand) || !arcane(client).getCarried().isEmpty()) return false; next(Step.CRAFT_FINISH); }
            case CRAFT_FINISH -> { client.player.closeContainer(); onServer(client, () -> { var player = serverPlayer(client); require(((TileArcaneWorkbench) player.level().getBlockEntity(workbench)).isEmpty() && player.containerMenu.getCarried().isEmpty(), "actual paid result retrieval leaves table/cursor empty"); require(count(player, Items.DIAMOND) == 0 && count(player, shard()) == 0 && count(player, Items.QUARTZ) == 0 && count(player, frost()) == 1, "actual saved diamond plus ONLY4water/4quartz consumed"); diamondConsumed = true; earnedFocus = stack(player, frost()).copy(); retained(player); report.put("native_craft_result", Map.of("native_result_PICKUP_clicks", 1, "actual_focus_snbt", snbt(earnedFocus, player), "payer_after", snbt(expectedWand, player), "saved_diamonds_consumed", 1, "supplied_diamonds_or_focus_outputs", 0)); }); next(Step.CRAFT_ACK); }
            case CRAFT_ACK -> { if (!serverDone() || ticks < 8) return false; next(Step.FOCUS_ARM); }
            case FOCUS_ARM -> { if (!hand(client) || ticks < 10) return false; next(Step.FOCUS_PRESS); }
            case FOCUS_PRESS -> { if (!client.isWindowActive()) return false; require(ThaumcraftFocusClient.CHANGE_FOCUS.matches(GLFW.GLFW_KEY_F, 0), "actual F binding, never rebound"); KeyMapping.click(InputConstants.getKey(focusBinding)); ThaumcraftFocusClient.CHANGE_FOCUS.setDown(true); if (ThaumcraftFocusClient.CHANGE_FOCUS.same(client.options.keySwapOffhand)) client.options.keySwapOffhand.setDown(true); next(Step.RADIAL); }
            case RADIAL -> { if (!client.isWindowActive()) { releaseFocusKey(client); next(Step.FOCUS_PRESS); return false; } if (ticks < 8 || !(client.screen instanceof GuiFocusSelect)) return false; require(ItemStack.matches(FocusData.get(client.player.getMainHandItem()), savedExcavation) && ItemStack.matches(stack(client.player, frost()), earnedFocus), "actual installed Excavation plus new loose Frost"); moveCursor(client, focusPoint(client.screen)); next(Step.RADIAL_HOVER); }
            case RADIAL_HOVER -> {
                if (!client.isWindowActive()) { releaseFocusKey(client); next(Step.FOCUS_PRESS); return false; } if (ticks < 3 || !(client.screen instanceof GuiFocusSelect)) return false; requireCursor(client, focusPoint(client.screen));
                report.put("native_F_selector", Map.of("bindings_changed", 0, "key", focusBinding, "all_four_sort_order", List.of("Fire AF", "Excavation BE", "Frost BF", "Shock BL"), "actual_loose_order", List.of("Fire AF", "Frost BF", "Shock BL"), "installed", "actual saved Excavation BE", "capture", "ONE ordinary transparent frame, active owned-window cursor over second20x20 native choice; no afterRender re-render")); next(Step.RADIAL_SELECT); capture.accept(CAPTURES.get(1));
            }
            case RADIAL_SELECT -> { if (!client.isWindowActive()) { releaseFocusKey(client); next(Step.FOCUS_PRESS); return false; } require(client.screen instanceof GuiFocusSelect, "same actual selector"); var clickedScreen = client.screen; int[] point = focusPoint(clickedScreen); clickedScreen.mouseMoved(point[0], point[1]); require(clickedScreen.mouseClicked(point[0], point[1], 0), "native selector accepts earned Frost"); clickedScreen.mouseReleased(point[0], point[1], 0); releaseFocusKey(client); next(Step.FOCUS_EQUIPPED); }
            case FOCUS_EQUIPPED -> {
                if (ticks < 10 || !ItemStack.matches(FocusData.get(client.player.getMainHandItem()), earnedFocus) || count(client.player, frost()) != 0 || count(client.player, excavation()) != 1) return false;
                onServer(client, () -> { var player = serverPlayer(client); ItemStack actual = player.getMainHandItem(); require(sameExcept(actual, expectedWand, ThaumcraftFoci.WAND_FOCUS.get()) && ItemStack.matches(FocusData.get(actual), earnedFocus) && ItemStack.matches(stack(player, excavation()), savedExcavation), "native F changed ONLY focus and returned exact saved Excavation"); expectedWand = actual.copy(); focusChanged = true; inspectFocus(player); retained(player); report.put("native_focus_switch", Map.of("earned_focus", snbt(earnedFocus, player), "exact_returned_Excavation", snbt(savedExcavation, player), "old_Fire_Shock_exact", true, "vis_unchanged", true, "direct_focus_writes", 0)); }); next(Step.SHOT_TRAVEL);
            }
            case SHOT_TRAVEL -> { if (!serverDone()) return false; onServer(client, () -> stand(serverPlayer(client), shotIndex == 0 ? wall.getX() + .5 : cowPosition.getX() + .5, workbench.getY(), shotIndex == 0 ? wall.getZ() + 9.5 : cowPosition.getZ() + 5.5)); next(Step.SHOT_AIM); }
            case SHOT_AIM -> { if (!serverDone() || !hand(client) || ticks < 16 || shotIndex > 0 && !(client.level.getEntity(targetId) instanceof Cow)) return false; lookAt(client, launchAim(client)); particlesBefore = client.particleEngine.countParticles(); onServer(client, () -> shot = new ShotProbe(serverPlayer(client), shotIndex == 0 ? null : ownedCow)); next(Step.SHOT_READY); }
            case SHOT_READY -> { if (!serverDone()) return false; next(Step.SHOT_START); }
            case SHOT_START -> { if (!client.isWindowActive()) return false; lookAt(client, launchAim(client)); guardingShot = true; releaseSent = false; guardTicks = 0; client.options.keyUse.setDown(false); require(client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND).consumesAction(), "ONE ordinary native Frost useItem activation, not held CHARGE/autorepeat"); next(Step.SHOT_LIVE); }
            case SHOT_LIVE -> {
                require(ticks <= 100, "bounded native payment/tracking/hit observation, no retry or synthetic fallback");
                if (shot.frame.paid() == 0) return false;
                if (shotIndex == 0) {
                    EntityFrostShard tracked = tracked(client); if (tracked == null || tracked.getAge() < 1 || tracked.isRemoved() || client.player.getEyePosition().distanceToSqr(tracked.position()) < 4) { require(ticks < 90 && shot.frame.live(), "must capture actual tracked shard WHILE flying, never after it disappeared"); return false; }
                    require(shot.frame.live() && tracked.getOwner() == client.player && tracked.getDamage() == 3F, "actual server shard still ongoing and native client owner/damage tracking resolved");
                    lookAt(client, tracked.position()); report.put("native_visible_flight_capture", clientEvidence(client, tracked)); next(Step.SHOT_WAIT); capture.accept(CAPTURES.get(2));
                } else {
                    if (!(client.level.getEntity(targetId) instanceof Cow cow) || shot.frame.hurts() == 0 || cow.getHealth() >= shot.healthBefore || cow.hurtTime <= 0) { require(ticks < 100, "bounded natural-spread shot must yield actual native target hurt presentation"); return false; }
                    lookAt(client, cow.getBoundingBox().getCenter()); report.put("native_target_hit_capture", Map.of("server_health", shot.frame.health(), "client_health", cow.getHealth(), "native_hurt_ticks", cow.hurtTime, "observed_damage_events", shot.frame.hurts(), "first_damage", 3, "later_health_not_forced_to_seven", true, "global_native_particle_count_before", particlesBefore, "global_native_particle_count_now", client.particleEngine.countParticles(), "synthetic_FX_calls", 0)); next(Step.SHOT_WAIT); capture.accept(CAPTURES.get(3));
                }
            }
            case SHOT_WAIT -> { releaseUse(client); require(ticks <= 180, "bounded observation, NOT a custom projectile air TTL"); if (shot.frame.live() || shot.frame.stopped() < 2 || ticks < 4) return false; guardingShot = false; next(Step.SHOT_FINISH); }
            case SHOT_FINISH -> { onServer(client, () -> { var player = serverPlayer(client); shots.add(shot.finish()); expectedWand = player.getMainHandItem().copy(); retained(player); if (shotIndex == 0) createCow(player); }); next(Step.SHOT_ACK); }
            case SHOT_ACK -> { if (!serverDone() || ticks < 8) return false; if (++shotIndex < 2) next(Step.SHOT_TRAVEL); else next(Step.FINAL_CHECK); }
            case FINAL_CHECK -> { onServer(client, () -> { finalState(serverPlayer(client)); stopObservation(); }); next(Step.FINAL_ACK); }
            case FINAL_ACK -> { if (!serverDone() || ClientTickEvent.CLIENT_PRE.isRegistered(preGuard) || ClientTickEvent.CLIENT_POST.isRegistered(postGuard)) return false; require(nativeTicks == null && nativeAdds == null && serverStopped == null && focusBinding.equals(ThaumcraftFocusClient.CHANGE_FOCUS.saveString()) && swapBinding.equals(client.options.keySwapOffhand.saveString()), "temporary hooks removed and bindings unchanged"); require(hand(client), "final earned payer actually held"); report.put("native_held_model", heldModel(client)); report.put("native_client_ticks", totalTicks); client.setScreen(new InventoryScreen(client.player)); next(Step.FINAL_SCENE); }
            case FINAL_SCENE -> { if (ticks < 8 || !client.isWindowActive()) return false; moveCursor(client, inventoryPoint(client, itemSlot(client.player, FrostClientSequence::payer))); next(Step.FINAL_CAPTURE); }
            case FINAL_CAPTURE -> { if (ticks < 3) return false; requireCursor(client, inventoryPoint(client, itemSlot(client.player, FrostClientSequence::payer))); next(Step.DONE); capture.accept(CAPTURES.get(4)); }
            case DONE -> { client.setScreen(null); releaseInputs(client); return true; }
        }
        return false;
    }

    private void prepare(ServerPlayer player) {
        require(center.equals(new BlockPos(1474, 216, 377)) && workbench.equals(center.offset(208, 0, 0)), "root passes1682,216,377 / accepted center1474,216,377");
        var level = player.serverLevel(); expectedWand = player.getInventory().getItem(8).copy(); savedExcavation = FocusData.get(expectedWand).copy(); savedFire = stack(player, ThaumcraftFoci.FIRE_FOCUS.get()).copy(); savedShock = stack(player, ThaumcraftFoci.SHOCK_FOCUS.get()).copy(); savedDiamond = stack(player, Items.DIAMOND).copy(); apple = player.getOffhandItem().copy(); initialXp = player.totalExperience; savedInvulnerability = player.isInvulnerable();
        require(player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL && savedInvulnerability && player.isAlive(), "actual saved Survival/Entity Invulnerable1 remains unchanged");
        require(payer(expectedWand) && ItemWandCasting.data(expectedWand).vis().equals(SAVED_VIS) && expectedWand.getOrDefault(ThaumcraftWands.WAND_CRAFT_COST.get(), 0) == 54 && savedExcavation.is(excavation()), "EXACT actual accepted saved payer/installed Excavation, no reconstruction/top-up");
        require(count(player, ThaumcraftContent.WAND.get()) == 3 && count(player, ThaumcraftContent.EMPTY_PHIAL.get()) == 4 && count(player, ThaumcraftContent.FILLED_PHIAL.get()) == 0 && count(player, ThaumcraftContent.JAR.get().asItem()) == 1 && apple.is(Items.APPLE) && apple.getCount() == 1, "actual three wands/four phials/earned jar/apple");
        require(count(player, Items.DIAMOND) == 1 && savedDiamond.getCount() == 1 && count(player, Items.CHEST) == 1 && count(player, Items.COBBLESTONE) == 3 && initialXp == 11, "actual recovered chest/cobble3/ONE earned diamond and saved11 total XP including4 native ore XP, not an XP grant");
        require(count(player, ThaumcraftFoci.FIRE_FOCUS.get()) == 1 && count(player, ThaumcraftFoci.SHOCK_FOCUS.get()) == 1 && count(player, excavation()) == 0 && count(player, frost()) == 0 && count(player, shard()) == 0 && count(player, Items.QUARTZ) == 0, "actual old Fire+Shock loose/Excavation installed, no hidden new recipe inputs/output");
        List<Item> armor = List.of(ThaumcraftWearables.GOGGLES.get(), ThaumcraftWearables.ROBE_CHESTPLATE.get(), ThaumcraftWearables.ROBE_LEGGINGS.get(), ThaumcraftWearables.ROBE_BOOTS.get());
        for (int i = 0; i < ARMOR.size(); i++) { ItemStack actual = player.getItemBySlot(ARMOR.get(i)); require(actual.is(armor.get(i)) && actual.getDamageValue() == (i >= 2 ? 1 : 0), "actual worn accepted armor0/0/1/1"); savedArmor.add(actual.copy()); }
        require(savedArmor.get(1).getOrDefault(DataComponents.REPAIR_COST, 0) == 1 && WandVisDiscount.percentagePoints(player, Aspect.WATER) == 10, "same repaired chestCost1 and full worn10 discount points");
        for (ItemStack actual : player.getInventory().items) if (!actual.isEmpty() && !payer(actual)) savedStacks.add(actual.copy());
        for (int x = (center.getX() - 12) >> 4; x <= (center.getX() + 202) >> 4; x++) for (int z = (center.getZ() - 12) >> 4; z <= (center.getZ() + 12) >> 4; z++) level.getChunk(x, z);
        int tcBlocks = 0, jars = 0, units = 0, candles = 0;
        for (BlockPos at : BlockPos.betweenClosed(center.offset(-10, 0, -10), center.offset(202, 6, 11))) {
            BlockState state = level.getBlockState(at); BlockEntity tile = level.getBlockEntity(at); oldBlocks.put(at.immutable(), state);
            if (BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals("thaumcraft")) ++tcBlocks;
            if (state.is(ThaumcraftInfusion.TALLOW_CANDLE.get())) ++candles;
            if (tile instanceof TileNode node) oldNodes.put(at.immutable(), node.getScanId()); else if (tile != null) oldBEs.put(at.immutable(), beTag(tile));
            if (tile instanceof TileJar jar) { ++jars; units += jar.getAmount(); }
        }
        require(tcBlocks >= 114 && oldBEs.size() >= 34 && jars == 12 && units == 173 && candles == 72, "all accepted114TC/34BE minimum plus extended previous Excavation bench/protected chest, twelve jars173units/72candles");
        require(level.getBlockEntity(center.offset(176, 0, 0)) instanceof TileArcaneWorkbench oldBench && oldBench.isEmpty() && level.getBlockEntity(center.offset(186, 0, -3)) instanceof ChestBlockEntity oldChest && oldChest.isEmpty(), "previous Excavation workbench1650/protected earned chest1660 included and untouched");
        TileNode node = node(player); require(node.getNodeType() == NodeType.NORMAL && aspects(node.getAspectsBase()).equals(Map.of("aer", 80, "aqua", 80, "ignis", 80, "ordo", 80, "perditio", 80, "terra", 80)), "ACTUAL saved clothing NORMAL node with80/primal base, current is NOT reset");
        var saved = new LinkedHashMap<String, Object>(); saved.put("main_stacks_snbt", player.getInventory().items.stream().filter(value -> !value.isEmpty()).map(value -> snbt(value, player)).toList()); saved.put("armor_snbt", savedArmor.stream().map(value -> snbt(value, player)).toList()); saved.put("payer_snbt", snbt(expectedWand, player)); saved.put("offhand_snbt", snbt(apple, player)); saved.put("actual_earned_diamond_snbt", snbt(savedDiamond, player)); saved.put("total_saved_XP", initialXp); saved.put("prior_native_ore_XP_included", 4); saved.put("old_TC_blocks_found", tcBlocks); saved.put("old_non_node_BEs_found", oldBEs.size()); saved.put("actual_visited_saved_node_snbt", beTag(node).toString()); report.put("actual_saved_inputs", saved);
        patch(level); level.setBlockAndUpdate(workbench, ThaumcraftArcane.ARCANE_WORKBENCH.get().defaultBlockState());
        for (int x = -5; x <= 5; x++) for (int y = 0; y <= 5; y++) { BlockPos at = wall.offset(x, y, 0); require(level.isEmptyBlock(at), "wide wall only in new empty fixture cells"); level.setBlockAndUpdate(at, Blocks.STONE_BRICKS.defaultBlockState()); }
        for (BlockPos at : BlockPos.betweenClosed(patchMin(), patchMax())) fixtureBlocks.put(at.immutable(), level.getBlockState(at));
        long oldTime = level.getDayTime(); level.setDayTime(6000); level.setWeatherParameters(0, 12000, false, false); inputs.add(Map.of("fixture", "fresh floor/air/empty workbench/11x6 wall/daylight/clear weather/camera", "old_day_time", oldTime, "new_day_time", 6000));
        var knowledge = KnowledgeAccess.get(player); CompoundTag original = knowledge.save().copy(); var keys = new LinkedHashSet<String>(); keys.addAll(ResearchCatalog.get("FOCUSFROST").parents()); keys.addAll(ResearchCatalog.get("FOCUSFROST").parentsHidden()); keys.add("FOCUSFROST"); var already = new LinkedHashMap<String, Boolean>();
        for (String key : keys) { already.put(key, knowledge.isResearchComplete(key)); knowledge.addResearch(key); } knowledgeBefore = knowledge.save().copy(); KnowledgeEvents.sync(player);
        report.put("research_fixture", Map.of("keys", List.copyOf(keys), "already_complete_before", already, "earned_here", false, "original_knowledge_snbt", original.toString(), "after_declared_fixture_snbt", knowledgeBefore.toString()));
        supply(player, shard(), 4); supply(player, Items.QUARTZ, 4); player.inventoryMenu.broadcastChanges(); retained(player); startObservation(player);
    }

    /** Reads only native node/current-wand transitions; never calls a charging backend. */
    private final class ChargeProbe {
        final ServerPlayer player; final TileNode source; final ItemStack wand, original; final WandData before;
        final Map<String, Integer> sourceBefore, base, transferred = new LinkedHashMap<>(), regenerated = new LinkedHashMap<>();
        final List<Map<String, Object>> trace = new ArrayList<>(); final long started;
        WandData previous; Map<String, Integer> previousSource; boolean active = true, used; volatile boolean funded; volatile int stopped;
        ChargeProbe(ServerPlayer player) {
            this.player = player; source = node(player); wand = player.getMainHandItem(); original = wand.copy(); before = previous = ItemWandCasting.data(wand); sourceBefore = previousSource = aspects(source.getAspects()); base = aspects(source.getAspectsBase()); started = player.serverLevel().getGameTime();
            require(ItemStack.matches(wand, expectedWand) && !player.isUsingItem() && !ItemWandCasting.isUsingNode(player) && before.amount("perditio") == 332 && sourceBefore.getOrDefault("perditio", 0) >= 10, "actual insufficient payer and finite saved source budget before NEW use");
        }
        void observe() {
            require(player.getMainHandItem() == wand && sameExcept(wand, original, ThaumcraftContent.WAND_DATA.get()) && player.level().getBlockEntity(chargingNode) == source && aspects(source.getAspectsBase()).equals(base), "same exact native wand/focus and saved node/base");
            require(player.serverLevel().getGameTime() - started <= 1800, "bounded saved-node charge observation"); var now = ItemWandCasting.data(wand); var current = aspects(source.getAspects()); int regenTotal = 0, moved = 0;
            for (String tag : WandData.PRIMALS) {
                int delta = now.amount(tag) - previous.amount(tag); require(delta >= 0 && delta % 100 == 0, "ONLY native whole100 transfers, zero focus cost while charging:" + tag + "/" + delta);
                int regen = current.getOrDefault(tag, 0) - previousSource.getOrDefault(tag, 0) + delta / 100; require(regen >= 0 && regen <= 1, "actual node budget plus native per-tick regen:" + tag);
                if (delta > 0) transferred.merge(tag, delta / 100, Integer::sum); if (regen > 0) regenerated.merge(tag, regen, Integer::sum); moved += delta / 100; regenTotal += regen;
            }
            require(regenTotal <= 1, "at most one native normal-node regeneration per tick");
            String actualSessionNode = "released";
            if (player.isUsingItem()) { require(player.getUseItem() == wand && player.getUsedItemHand() == InteractionHand.MAIN_HAND && ItemWandCasting.isUsingNode(player), "actual server-side node session/exact use stack survives client release transit"); BlockPos sessionNode = serverSessionNode(player, wand); require(sessionNode.equals(chargingNode), "actual SERVER-owned session targets THIS saved node, not merely some node"); actualSessionNode = sessionNode.toShortString(); used = true; }
            if (moved > 0 || regenTotal > 0) trace.add(Map.of("server_tick", player.serverLevel().getGameTime(), "wand_before", previous.vis(), "wand_after", now.vis(), "source_before", previousSource, "source_after", current, "native_node_session", ItemWandCasting.isUsingNode(player), "actual_server_session_node", actualSessionNode, "using", player.isUsingItem()));
            stopped = used && !player.isUsingItem() ? stopped + 1 : 0; funded = now.amount("perditio") >= 1200 && now.amount("aqua") >= 799 && now.amount("ordo") >= 799; previous = now; previousSource = current;
        }
        Map<String, Object> finish() {
            require(used && funded && stopped >= 2 && !ItemWandCasting.isUsingNode(player) && !transferred.isEmpty(), "real finite native recharge and own-side release completed");
            for (String tag : WandData.PRIMALS) require(source.getAspects().getAmount(Aspect.getAspect(tag)) == sourceBefore.getOrDefault(tag, 0) - transferred.getOrDefault(tag, 0) + regenerated.getOrDefault(tag, 0), "whole source budget exactly reconciled:" + tag);
            active = false; return Map.of("source", chargingNode.toShortString(), "actual_before", sourceBefore, "actual_after", aspects(source.getAspects()), "base_unchanged", base, "whole_vis_transferred_by_aspect", Map.copyOf(transferred), "native_regeneration_by_aspect", Map.copyOf(regenerated), "wand_before", before.vis(), "wand_after", ItemWandCasting.data(wand).vis(), "native_transition_trace", List.copyOf(trace), "direct_vis_or_node_writes", 0);
        }
    }

    /** Passive ADD + SERVER_POST snapshots of exactly one real paid native activation. */
    private final class ShotProbe {
        final ServerPlayer player; final Cow target; final ItemStack wand, original; final WandData before; final float healthBefore; final long started;
        final List<Map<String, Object>> trace = new ArrayList<>(), damage = new ArrayList<>(), impacts = new ArrayList<>();
        volatile Frame frame; volatile int adds; EntityFrostShard projectile; Map<String, Object> birth; boolean active = true, wallRicochet; int paid, stopped, hurts, previousImpacts; float previousHealth; Vec3 previousMotion, previousPosition;
        ShotProbe(ServerPlayer player, Cow target) {
            this.player = player; this.target = target; wand = player.getMainHandItem(); original = wand.copy(); before = ItemWandCasting.data(wand); healthBefore = previousHealth = target == null ? 0 : target.getHealth(); started = player.serverLevel().getGameTime(); frame = new Frame(0, -1, false, 0, 0, healthBefore, 0);
            require(ItemStack.matches(wand, expectedWand) && FocusData.get(wand).is(frost()) && !player.isUsingItem() && !ItemWandCasting.isUsingNode(player), "genuine earned Frost/finite payer, no fabricated held session"); inspectFocus(player);
            if (target != null) require(target == ownedCow && target.isAlive() && target.getHealth() == target.getMaxHealth() && player.distanceTo(target) >= 4.5 && player.distanceTo(target) <= 6, "wide unharmed ordinary cow fixture at about five blocks");
        }
        void added(EntityFrostShard entity) {
            require(++adds == 1 && entity.getType() == ThaumcraftFrost.FROST_SHARD.get() && entity.getOwner() == player && entity.getDamage() == 3F && entity.getBounceLimit() == 3 && entity.getImpactCount() == 0 && entity.getAge() == 0, "ONE actual native basic shard/owner/damage3/initial three remaining bounces");
            require(Math.abs(entity.getBbWidth() - .6F) < .00001 && finite(entity.position()) && finite(entity.getDeltaMovement()) && entity.getDeltaMovement().lengthSqr() > 0, "native natural launch/size/motion finite, not a test-constructed projectile");
            verifyDebit(original, wand, SHOT_PRICE); projectile = entity; previousMotion = entity.getDeltaMovement(); previousPosition = entity.position(); birth = shardEvidence(entity); birth.put("all_primal_vis_before", before.vis()); birth.put("all_primal_vis_at_ADD_after_payment", ItemWandCasting.data(wand).vis());
        }
        void observe() {
            require(player.getMainHandItem() == wand && sameExcept(wand, original, ThaumcraftContent.WAND_DATA.get()) && player.serverLevel().getGameTime() - started <= 240, "same focused payer, bounded real projectile observation");
            WandData now = ItemWandCasting.data(wand); int aqua = before.amount("aqua") - now.amount("aqua"); require(aqua == 0 || aqua == 3, "one paid spawn per activation, no accidental repeat burst"); int actualPaid = aqua / 3;
            for (String tag : WandData.PRIMALS) require(before.amount(tag) - now.amount(tag) == actualPaid * SHOT_PRICE.getOrDefault(tag, 0), "actual exact full-gear3/1/1 debit, other primals unchanged:" + tag);
            if (actualPaid > paid) trace.add(Map.of("payment_tick", player.serverLevel().getGameTime(), "all_primal_before", before.vis(), "all_primal_after", now.vis(), "native_cooldown_remaining_ms_observed_POST", WandFocusActions.cooldownMillis(player), "ordinary_using_state", player.isUsingItem())); paid = actualPaid;
            require(adds == paid && adds <= 1, "real ADD follows the single server-first debit; no synthetic spawn");
            if (projectile != null) {
                require(projectile.getOwner() == player && projectile.getDamage() == 3F && projectile.getImpactCount() >= previousImpacts && projectile.getImpactCount() <= 4 && finite(projectile.position()) && finite(projectile.getDeltaMovement()), "actual tracked owner/basic finite motion/at most fourth impact terminal");
                if (projectile.getImpactCount() > previousImpacts) {
                    Vec3 motion = projectile.getDeltaMovement();
                    // Original flight moves from the OLD position using POST-
                    // bounce velocity, not from the ray's contact. A radius2
                    // post-position test wrongly rejected this real reflection.
                    // Verify the passively observed INCOMING segment against
                    // the actual south wall face and the real Z sign reversal.
                    if (target == null && previousImpacts == 0 && previousMotion.z < 0 && motion.z > 0) {
                        double fraction = (wall.getZ() + 1D - previousPosition.z) / previousMotion.z;
                        Vec3 contact = previousPosition.add(previousMotion.scale(fraction));
                        wallRicochet = fraction >= 0 && fraction <= 1 && contact.x >= wall.getX() - 5 && contact.x < wall.getX() + 6
                                && contact.y >= wall.getY() && contact.y < wall.getY() + 6
                                && player.level().getBlockState(BlockPos.containing(contact.x, contact.y, wall.getZ() + .5)).is(Blocks.STONE_BRICKS);
                    }
                    impacts.add(Map.of("server_tick", player.serverLevel().getGameTime(), "native_impacts_before", previousImpacts, "native_impacts_after", projectile.getImpactCount(), "motion_before", previousMotion.toString(), "motion_after", motion.toString(), "position", projectile.position().toString(), "target_health", target == null ? 0F : target.getHealth(), "target_native_iframes", target == null ? 0 : target.invulnerableTime, "previous_position", previousPosition.toString(), "actual_incoming_wall_face_verified", wallRicochet));
                }
                previousImpacts = projectile.getImpactCount(); previousMotion = projectile.getDeltaMovement(); previousPosition = projectile.position();
            }
            if (target != null) {
                require(!target.isOnFire() && target.getTicksFrozen() == 0 && target.getActiveEffects().isEmpty(), "basic Frost adds NO freezing, slowness, fire or other effects"); float health = target.getHealth();
                if (health < previousHealth) { var source = target.getLastDamageSource(); require(source != null && source.getDirectEntity() == projectile && source.getEntity() == player && source.getMsgId().equals("thrown"), "actual owner-attributed native thrown hit"); require(hurts > 0 || previousHealth - health == 3F, "FIRST native collision really inflicts3, no direct damage call"); ++hurts; damage.add(Map.of("server_tick", player.serverLevel().getGameTime(), "health_before", previousHealth, "health_after", health, "native_hurt_ticks", target.hurtTime, "native_invulnerability_ticks", target.invulnerableTime, "impact_count", projectile.getImpactCount(), "source", source.getMsgId(), "owner", player.getUUID().toString())); }
                previousHealth = health;
            }
            stopped = paid == 1 && !player.isUsingItem() ? stopped + 1 : 0;
            var snapshot = new LinkedHashMap<String, Object>(); snapshot.put("server_tick", player.serverLevel().getGameTime()); snapshot.put("all_primal_vis", now.vis()); snapshot.put("native_shard", projectile == null ? Map.of() : shardEvidence(projectile)); snapshot.put("target_health", previousHealth); snapshot.put("target_hurt_ticks", target == null ? 0 : target.hurtTime); trace.add(snapshot);
            frame = new Frame(paid, projectile == null ? -1 : projectile.getId(), projectile != null && !projectile.isRemoved(), projectile == null ? 0 : projectile.getImpactCount(), hurts, previousHealth, stopped);
        }
        Map<String, Object> finish() {
            require(paid == 1 && adds == 1 && stopped >= 2 && projectile != null && projectile.isRemoved() && projectile.getImpactCount() >= 1 && (target == null ? wallRicochet : hurts >= 1),
                    "one paid real flight/wall reflection and actual target damage where intended; ordinary lifecycle completed; paid=" + paid + "; adds=" + adds
                            + "; stopped=" + stopped + "; wall=" + wallRicochet + "; hurts=" + hurts + "; impacts=" + impacts); active = false;
            var result = new LinkedHashMap<String, Object>(Map.of("case", target == null ? "wide wall / live flight capture / native ricochet" : "ordinary wide cow / first thrown3 / later collisions and iframes preserved", "all_primal_before", before.vis(), "all_primal_after", ItemWandCasting.data(wand).vis(), "native_ADD_birth", birth, "native_POST_trace", List.copyOf(trace), "actual_damage_transitions", List.copyOf(damage), "final_native_shard", shardEvidence(projectile), "paid_spawns", 1, "health_after_not_forced", previousHealth, "projectile_or_damage_setters", 0));
            result.put("native_impact_transitions", List.copyOf(impacts)); result.put("actual_wide_wall_reflection_observed", wallRicochet); result.put("global_native_particle_count_before_activation", particlesBefore); return result;
        }
    }

    private void retained(ServerPlayer player) {
        for (ItemStack saved : savedStacks) { int before = savedStacks.stream().filter(value -> ItemStack.isSameItemSameComponents(value, saved)).mapToInt(ItemStack::getCount).sum(); int after = player.getInventory().items.stream().filter(value -> ItemStack.isSameItemSameComponents(value, saved)).mapToInt(ItemStack::getCount).sum(); require(after == before - (diamondConsumed && saved.is(Items.DIAMOND) ? 1 : 0), "all old main counts/components exact EXCEPT declared saved diamond consumption:" + saved); }
        for (int i = 0; i < ARMOR.size(); i++) require(ItemStack.matches(player.getItemBySlot(ARMOR.get(i)), savedArmor.get(i)), "same saved robes/goggles stay worn/exact");
        require(count(player, ThaumcraftContent.WAND.get()) == 3 && ItemStack.matches(stack(player, FrostClientSequence::payer), expectedWand) && ItemStack.matches(player.getOffhandItem(), apple) && player.isInvulnerable() == savedInvulnerability && player.totalExperience == initialXp && player.isAlive(), "all wands/apple/saved Entity flag/actual saved XP preserved");
        require(ItemStack.matches(FocusData.get(expectedWand), focusChanged ? earnedFocus : savedExcavation) && ItemStack.matches(stack(player, ThaumcraftFoci.FIRE_FOCUS.get()), savedFire) && ItemStack.matches(stack(player, ThaumcraftFoci.SHOCK_FOCUS.get()), savedShock), "old Fire/Shock exact and actual installed focus accounted");
        if (focusChanged) require(ItemStack.matches(stack(player, excavation()), savedExcavation), "exact old Excavation returned, not recreated");
        for (var entry : oldBlocks.entrySet()) {
            BlockPos pos = entry.getKey(); BlockState before = entry.getValue(), after = player.level().getBlockState(pos);
            // This saved cactus is a native random-ticking INPUT, not static
            // scenery. Preserve it without pretending AGE/natural growth stop
            // while the actual saved node in the same loaded chunk is used.
            BlockPos cactus = center.offset(149, 0, 5);
            boolean nativeCactusColumn = pos.getX() == cactus.getX() && pos.getZ() == cactus.getZ()
                    && pos.getY() >= cactus.getY() && pos.getY() <= cactus.getY() + 2;
            boolean nativeGrowth = nativeCactusColumn && (before.is(Blocks.CACTUS) && after.is(Blocks.CACTUS)
                    || before.isAir() && after.is(Blocks.CACTUS) && player.level().getBlockState(pos.below()).is(Blocks.CACTUS));
            require(after == before || nativeGrowth, "all old scanned blocks retained through+202X; only declared native cactus AGE/bounded growth may evolve:" + pos);
        }
        for (var entry : oldBEs.entrySet()) require(player.level().getBlockEntity(entry.getKey()) != null && beTag(player.level().getBlockEntity(entry.getKey())).equals(entry.getValue()), "all old non-node BE/container/jar data exact:" + entry.getKey());
        for (var entry : oldNodes.entrySet()) require(player.level().getBlockEntity(entry.getKey()) instanceof TileNode node && node.getScanId().equals(entry.getValue()), "same old node identity; declared recharge/current native regeneration allowed, NEVER exact-old-current-NBT assertion");
        require(knowledgeBefore.equals(KnowledgeAccess.get(player).save()), "all knowledge/pools/discoveries exact beyond declared Frost research fixture");
    }

    private void finalState(ServerPlayer player) {
        retained(player); require(shots.size() == 2 && shotIndex == 2 && diamondConsumed && focusChanged && !player.isUsingItem() && player.containerMenu == player.inventoryMenu && player.containerMenu.getCarried().isEmpty(), "two bounded single native activations complete; inputs released/cursor empty");
        for (String tag : WandData.PRIMALS) require(ItemWandCasting.getVis(expectedWand, tag) == ItemWandCasting.getVis(chargedWand, tag) - RECIPE_PRICE.getOrDefault(tag, 0) - 2 * SHOT_PRICE.getOrDefault(tag, 0), "finite saved vis+actual node charge minus799craft and two3/1/1 shots:" + tag);
        require(count(player, shard()) == 0 && count(player, Items.QUARTZ) == 0 && count(player, Items.DIAMOND) == 0 && count(player, frost()) == 0 && ((TileArcaneWorkbench) player.level().getBlockEntity(workbench)).isEmpty(), "all declared inputs consumed, earned focus installed/table empty");
        for (var entry : fixtureBlocks.entrySet()) require(player.level().getBlockState(entry.getKey()) == entry.getValue(), "native Frost changed NO fresh floor/wall/block:" + entry.getKey());
        var cleanup = new ArrayList<String>(); for (var entity : player.serverLevel().getEntitiesOfClass(EntityFrostShard.class, patchBounds().inflate(32))) if (shots.stream().anyMatch(result -> ((Map<?, ?>) result.get("native_ADD_birth")).get("uuid").equals(entity.getUUID().toString()))) { cleanup.add(entity.getUUID().toString()); entity.discard(); }
        if (ownedCow != null && !ownedCow.isRemoved()) { require(player.serverLevel().getEntity(ownedCow.getUUID()) == ownedCow, "cleanup only exact new cow identity"); cleanup.add(ownedCow.getUUID().toString()); ownedCow.discard(); }
        require(player.serverLevel().getEntitiesOfClass(ItemEntity.class, patchBounds()).isEmpty() && player.serverLevel().getEntitiesOfClass(ExperienceOrb.class, patchBounds()).isEmpty(), "no native block drops/unclaimed fixture XP or supplied output entities");
        report.put("native_net_outputs", Map.of("actual_saved_diamond_consumed", 1, "earned_Frost_installed", 1, "exact_saved_Excavation_returned", true, "native_paid_shards", 2, "native_cast_centivis_total", Map.of("aqua", 6, "ignis", 2, "perditio", 2), "final_actual_payer_snbt", snbt(expectedWand, player), "saved_total_XP_unchanged", player.totalExperience, "cleanup_only_new_entity_UUIDs", cleanup, "new_vis_XP_or_output_grants", 0));
        report.put("retained_prior_outputs", Map.of("all_old_main_counts_components_exact_except_saved_diamond", true, "two_old_gold_wands_exact", true, "all_saved_armor_worn_exact", true, "Fire_Shock_Excavation_exact", true, "twelve_jars_173units_72candles", true, "old_non_node_BEs_exact", oldBEs.size(), "old_node_identity_retained_current_may_regenerate", true, "prior_acquisition_replayed", false));
        report.put("actual_saved_charge_node_after_story_snbt", beTag(node(player)).toString());
        BlockPos cactus = center.offset(149, 0, 5);
        report.put("native_saved_cactus_observation", java.util.stream.IntStream.range(0, 3).mapToObj(height -> {
            BlockPos pos = cactus.above(height);
            return Map.of("position", pos.toShortString(), "before", oldBlocks.get(pos).toString(), "after", player.level().getBlockState(pos).toString(), "state_writes", 0);
        }).toList());
    }

    private void startObservation(ServerPlayer player) {
        ServerLevel ownedLevel = player.serverLevel(); long started = ownedLevel.getGameTime();
        nativeTicks = server -> { if (server != player.server) return; try { require(player.isAlive() && !player.isRemoved() && player.serverLevel() == ownedLevel && server.getPlayerList().getPlayer(player.getUUID()) == player && ownedLevel.getGameTime() - started <= 4200 && player.isInvulnerable() == savedInvulnerability, "bounded live native server/player/level/saved Entity flag"); if (charge != null && charge.active) charge.observe(); if (shot != null && shot.active) shot.observe(); } catch (RuntimeException | Error error) { observationFailure = error; stopObservation(); } };
        nativeAdds = (entity, level) -> { if (level == player.level() && shot != null && shot.active && entity instanceof EntityFrostShard shard && shard.getOwner() == player) try { shot.added(shard); } catch (RuntimeException | Error error) { observationFailure = error; } return EventResult.pass(); };
        serverStopped = server -> { if (server == player.server) stopObservation(); };
        TickEvent.SERVER_POST.register(nativeTicks); EntityEvent.ADD.register(nativeAdds); LifecycleEvent.SERVER_STOPPED.register(serverStopped); ClientTickEvent.CLIENT_PRE.register(preGuard); ClientTickEvent.CLIENT_POST.register(postGuard);
    }
    private void stopObservation() {
        if (nativeTicks != null) { TickEvent.SERVER_POST.unregister(nativeTicks); nativeTicks = null; } if (nativeAdds != null) { EntityEvent.ADD.unregister(nativeAdds); nativeAdds = null; } if (serverStopped != null) { LifecycleEvent.SERVER_STOPPED.unregister(serverStopped); serverStopped = null; }
        if (charge != null) charge.active = false; if (shot != null) shot.active = false;
        // Keep PRE installed until CLIENT-thread cleanup really releases input, even during capture I/O.
        if (owningClient != null) owningClient.execute(() -> releaseInputs(owningClient));
    }
    private void guardInput(Minecraft client) {
        try {
            orient(client); if (client.player == null || client.gameMode == null) return;
            if (chargingHeld && charge != null && charge.active && !releaseSent && (charge.funded || observationFailure != null || !client.isWindowActive() || client.screen != null || ++guardTicks >= 1800)) releaseUse(client);
            if (guardingShot && !releaseSent && (observationFailure != null || !client.isWindowActive() || client.screen != null || ++guardTicks >= 40 || shot != null && (shot.adds > 0 || shot.frame.paid() > 0 || ItemWandCasting.getVis(client.player.getMainHandItem(), "aqua") < shot.before.amount("aqua")))) releaseUse(client);
        } catch (RuntimeException | Error error) { observationFailure = error; releaseUse(client); }
    }
    private void observeClient(Minecraft client) {
        try { orient(client); EntityFrostShard entity = tracked(client); if (client.player != null && entity != null && shot != null && shot.active && clientFrames.size() < 240) clientFrames.add(clientEvidence(client, entity)); }
        catch (RuntimeException | Error error) { observationFailure = error; releaseUse(client); }
    }
    private void orient(Minecraft client) {
        if (client.player == null || client.level == null || client.screen != null) return;
        if (aimCharge) lookAt(client, Vec3.atCenterOf(chargingNode));
        else if (step == Step.SHOT_AIM || step == Step.SHOT_READY || step == Step.SHOT_START) lookAt(client, launchAim(client));
        else if (guardingShot && shotIndex == 0) { EntityFrostShard entity = tracked(client); if (entity != null && !entity.isRemoved()) lookAt(client, entity.position()); }
        else if (guardingShot && client.level.getEntity(targetId) instanceof Cow cow) lookAt(client, cow.getBoundingBox().getCenter());
    }
    private void releaseUse(Minecraft client) { chargingHeld = false; client.options.keyUse.setDown(false); if (!releaseSent && client.player != null && client.gameMode != null) { client.gameMode.releaseUsingItem(client.player); releaseSent = true; } }
    private void releaseInputs(Minecraft client) { releaseUse(client); aimCharge = guardingShot = false; ClientTickEvent.CLIENT_PRE.unregister(preGuard); ClientTickEvent.CLIENT_POST.unregister(postGuard); releaseFocusKey(client); client.options.keyAttack.setDown(false); client.options.keyShift.setDown(false); if (previousCamera != null) client.options.setCameraType(previousCamera); }
    private static void releaseFocusKey(Minecraft client) { ThaumcraftFocusClient.CHANGE_FOCUS.setDown(false); client.options.keySwapOffhand.setDown(false); }

    private void createCow(ServerPlayer player) {
        AABB site = new AABB(cowPosition).inflate(2);
        require(player.serverLevel().getEntitiesOfClass(LivingEntity.class, site).isEmpty() && player.serverLevel().getEntitiesOfClass(ItemEntity.class, site).isEmpty() && player.serverLevel().getEntitiesOfClass(ExperienceOrb.class, site).isEmpty(), "new cow never overwrites an old living fixture or physical output that arrived later");
        ownedCow = EntityType.COW.create(player.serverLevel()); require(ownedCow != null, "ordinary cow fixture"); ownedCow.setNoAi(true); ownedCow.setPersistenceRequired(); ownedCow.getAttribute(Attributes.SCALE).setBaseValue(1.5D); ownedCow.setCustomName(Component.literal("Native Frost: thrown3 / ordinary iframes")); ownedCow.setCustomNameVisible(true); ownedCow.moveTo(cowPosition.getX() + .5, cowPosition.getY(), cowPosition.getZ() + .5, 180, 0);
        require(player.level().noCollision(ownedCow) && player.serverLevel().addFreshEntity(ownedCow) && ownedCow.getHealth() == 10F, "fresh wide cow INPUT, never pre-damaged/frozen"); targetId = ownedCow.getId(); inputs.add(Map.of("fixture", "ordinary no-AI cow, native scale1.5 for natural Gaussian spread reliability", "uuid", ownedCow.getUUID().toString(), "position", ownedCow.position().toString(), "initial_health", ownedCow.getHealth()));
    }
    private void inspectFocus(ServerPlayer player) {
        var focus = (ItemFocusBasic) earnedFocus.getItem(); AspectList base = focus.getVisCost(earnedFocus); require(aspects(base).equals(Map.of("aqua", 5, "ignis", 2, "perditio", 2)) && focus.getActivationCooldown(earnedFocus) == 200 && !focus.isVisCostPerTick(earnedFocus) && focus.getAnimation(earnedFocus) == ItemFocusBasic.WandFocusAnimation.WAVE && focus.getFocusColor(earnedFocus) == 5204428 && focus.getSortingHelper(earnedFocus).startsWith("BF"), "original Frost basic native cost/cooldown/WAVE/color/sorting");
        for (Aspect aspect : base.getAspects()) require(WandVisDiscount.percentagePoints(player, aspect) == 10 && (int) (base.getAmount(aspect) * WandVisDiscount.modifier(expectedWand, player, aspect)) == SHOT_PRICE.get(aspect.getTag()), "original FLOAT full-gear native3/1/1, not4/2/2");
    }
    private EntityFrostShard tracked(Minecraft client) { ShotProbe actual = shot; return client.level != null && actual != null && client.level.getEntity(actual.frame.id()) instanceof EntityFrostShard entity ? entity : null; }
    private Map<String, Object> clientEvidence(Minecraft client, EntityFrostShard entity) {
        var result = shardEvidence(entity); result.put("client_tick", client.player.tickCount); result.put("native_renderer_class", client.getEntityRenderDispatcher().getRenderer(entity).getClass().getName()); result.put("actual_camera", client.options.getCameraType().name()); result.put("camera_distance", client.player.getEyePosition().distanceTo(entity.position())); result.put("global_native_particle_count", client.particleEngine.countParticles()); result.put("FX_boundary", "global native particle count is not isolated impact-particle/audio proof; no synthetic FX calls"); return result;
    }
    private static Map<String, Object> shardEvidence(EntityFrostShard entity) {
        var result = new LinkedHashMap<String, Object>(); result.put("uuid", entity.getUUID().toString()); result.put("id", entity.getId()); result.put("owner_uuid", entity.getOwner() == null ? "unresolved" : entity.getOwner().getUUID().toString()); result.put("position", entity.position().toString()); result.put("motion", entity.getDeltaMovement().toString()); result.put("age", entity.getAge()); result.put("damage", entity.getDamage()); result.put("remaining_bounce_counter", entity.getBounceLimit()); result.put("impact_count", entity.getImpactCount()); result.put("removed", entity.isRemoved()); return result;
    }
    private Vec3 launchAim(Minecraft client) { return shotIndex == 0 ? Vec3.atCenterOf(wall).add(0, 2.6, 0) : client.level.getEntity(targetId) instanceof Cow cow ? cow.getBoundingBox().getCenter().add(0, .15, 0) : Vec3.atCenterOf(cowPosition).add(0, .7, 0); }
    private List<int[]> recipeClicks(Minecraft client) {
        var clicks = new ArrayList<int[]>(); for (Item item : List.of(shard(), Items.QUARTZ, Items.DIAMOND)) { int source = playerSlot(client, itemSlot(client.player, value -> value.is(item))); int amount = 0; clicks.add(new int[] {source, 0}); for (int cell = 0; cell < 9; cell++) if (ingredient(cell) == item) { clicks.add(new int[] {cell + 2, 1}); ++amount; } require(client.player.containerMenu.getSlot(source).getItem().getCount() == amount, "exact finite raw stack, including ONLY actual saved diamond"); } return clicks;
    }
    private static Item ingredient(int cell) { return cell == 4 ? Items.DIAMOND : cell % 2 == 0 ? shard() : Items.QUARTZ; }
    private static void verifyGrid(ContainerArcaneWorkbench menu, int amount) { for (int i = 0; i < 9; i++) { ItemStack actual = menu.getSlot(i + 2).getItem(); require(amount == 0 ? actual.isEmpty() : actual.is(ingredient(i)) && actual.getCount() == amount, "real CQC/Q#Q/CQC grid/consumption:" + i); } }
    private static void verifyDebit(ItemStack before, ItemStack after, Map<String, Integer> price) { require(sameExcept(before, after, ThaumcraftContent.WAND_DATA.get()), "all non-vis wand/focus/craft-cost components exact"); WandData first = ItemWandCasting.data(before), last = ItemWandCasting.data(after); require(first.rod().equals(last.rod()) && first.cap().equals(last.cap()), "same saved wand materials"); for (String tag : WandData.PRIMALS) require(first.amount(tag) - last.amount(tag) == price.getOrDefault(tag, 0), "exact native debit:" + tag); }
    private BlockPos patchMin() { return workbench.offset(-4, -2, -16); }
    private BlockPos patchMax() { return workbench.offset(18, 8, 12); }
    private AABB patchBounds() { return new AABB(Vec3.atLowerCornerOf(patchMin()), Vec3.atLowerCornerOf(patchMax().offset(1, 1, 1))); }
    private void patch(ServerLevel level) {
        for (int x = patchMin().getX() >> 4; x <= patchMax().getX() >> 4; x++) for (int z = patchMin().getZ() >> 4; z <= patchMax().getZ() >> 4; z++) level.getChunk(x, z);
        require(level.getEntitiesOfClass(ItemEntity.class, patchBounds()).isEmpty() && level.getEntitiesOfClass(ExperienceOrb.class, patchBounds()).isEmpty() && level.getEntitiesOfClass(LivingEntity.class, patchBounds()).isEmpty() && level.getEntitiesOfClass(EntityFrostShard.class, patchBounds()).isEmpty(), "refuse old physical items/XP/living fixtures/shards in new patch");
        for (BlockPos at : BlockPos.betweenClosed(patchMin(), patchMax())) require(level.getBlockEntity(at) == null && !BuiltInRegistries.BLOCK.getKey(level.getBlockState(at).getBlock()).getNamespace().equals("thaumcraft"), "refuse ANY old BE/TC overwrite:" + at);
        for (BlockPos at : BlockPos.betweenClosed(patchMin(), patchMax())) level.setBlockAndUpdate(at, at.getY() < workbench.getY() ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
    }
    private void supply(ServerPlayer player, Item item, int amount) { require((item == shard() || item == Items.QUARTZ) && amount == 4, "ONLY declared new raw4water/4quartz, NEVER another diamond"); int slot = emptyMain(player); player.getInventory().setItem(slot, new ItemStack(item, amount)); inputs.add(Map.of("item", BuiltInRegistries.ITEM.getKey(item).toString(), "count", amount, "genuinely_empty_main_slot", slot, "purpose", "RAW INPUT, not earned output")); }
    private boolean showRecipe(Minecraft client) throws ReflectiveOperationException {
        int page = (int) BookClientSmoke.field(client.screen, "page"); var pages = (List<?>) BookClientSmoke.field(client.screen, "pages"); for (int i = page; i < Math.min(page + 2, pages.size()); i++) if (BookClientSmoke.field(pages.get(i), "recipe") instanceof ThaumcraftRecipeDisplay display && display.recipe().id().equals(id("arcane/focus_frost"))) { require(display.recipe() == client.level.getRecipeManager().byKey(id("arcane/focus_frost")).orElseThrow(), "book uses actual synced native recipe holder"); return true; }
        require(++turns <= 16, "bounded native book navigation"); var button = client.screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(value -> value.active && value.getMessage().getString().equals("→")).findFirst().orElseThrow(); var clickedScreen = client.screen; clickedScreen.mouseClicked(button.getX() + 5, button.getY() + 5, 0); clickedScreen.mouseReleased(button.getX() + 5, button.getY() + 5, 0); ticks = 0; return false;
    }
    private static int[] focusPoint(Screen screen) { require(screen instanceof GuiFocusSelect, "actual native selector"); var buttons = screen.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast).filter(value -> value.getWidth() == 20 && value.getHeight() == 20).toList(); require(buttons.size() == 3, "three actual loose choices FireAF/FrostBF/ShockBL; installed ExcavationBE is fourth focus"); var button = buttons.get(1); require(button.getMessage().getString().contains(frost().getDefaultInstance().getHoverName().getString()), "actual second native widget really names Frost"); return new int[] {button.getX() + 10, button.getY() + 10}; }
    private static void moveCursor(Minecraft client, int[] point) { client.screen.mouseMoved(point[0], point[1]); GLFW.glfwSetCursorPos(client.getWindow().getWindow(), point[0] * (double) client.getWindow().getScreenWidth() / client.screen.width, point[1] * (double) client.getWindow().getScreenHeight() / client.screen.height); }
    private static void requireCursor(Minecraft client, int[] point) { double x = client.mouseHandler.xpos() * client.screen.width / client.getWindow().getScreenWidth(), y = client.mouseHandler.ypos() * client.screen.height / client.getWindow().getScreenHeight(); require(Math.abs(x - point[0]) <= 1 && Math.abs(y - point[1]) <= 1, "real cursor hovers native widget/slot inside owned active window"); }
    private static int[] inventoryPoint(Minecraft client, int inventory) throws ReflectiveOperationException { var slot = client.player.containerMenu.getSlot(playerSlot(client, inventory)); return new int[] {(int) BookClientSmoke.field(client.screen, "leftPos") + slot.x + 8, (int) BookClientSmoke.field(client.screen, "topPos") + slot.y + 8}; }
    private static Map<String, Object> heldModel(Minecraft client) throws ReflectiveOperationException {
        ItemStack actual = client.player.getMainHandItem(); require(payer(actual) && FocusData.get(actual).is(frost()), "final actual earned held Frost");
        var model = client.getItemRenderer().getModel(actual, client.level, client.player, 0); require(model != client.getModelManager().getMissingModel(), "native held model resolves");
        // The wand is a builtin/entity item drawn by ThaumcraftGearModelsClient.renderWand, whose
        // BakedModel is the empty custom-renderer shell. The material sheets and the focus cube are
        // not quads; assert the selection the renderer actually reads.
        var data = ItemWandCasting.data(actual);
        var rodTexture = thaumcraft.client.renderers.models.gear.ModelWand.rodTexture(
                thaumcraft.common.items.wands.WandMaterials.rod(data.rod()).baseTag());
        var capTexture = thaumcraft.client.renderers.models.gear.ModelWand.capTexture(
                thaumcraft.common.items.wands.WandMaterials.cap(data.cap()).tag());
        var focusStack = FocusData.get(actual);
        int color = ((ItemFocusBasic) focusStack.getItem()).getFocusColor(focusStack);
        require(rodTexture.getPath().equals("textures/models/wand_rod_silverwood.png")
                        && capTexture.getPath().equals("textures/models/wand_cap_thaumium.png")
                        && color == (0xFF000000 | 5204428),
                "held wand selects the original Silverwood/Thaumium sheets and the native Frost focus RGB "
                        + Integer.toHexString(0xFF000000 | 5204428) + ", NOT the flat focus_frost loose icon; rod="
                        + rodTexture + "; cap=" + capTexture + "; argb=" + Integer.toHexString(color));
        for (var sheet : List.of(rodTexture, capTexture, thaumcraft.client.renderers.models.gear.ModelWand.FOCUS_TEXTURE)) {
            require(FrostClientSequence.class.getClassLoader()
                            .getResource("assets/" + sheet.getNamespace() + "/" + sheet.getPath()) != null,
                    "a sheet the held renderer binds exists in the shipped assets: " + sheet);
        }
        require(client.getResourceManager().getResource(id("textures/blocks/frostshard.png")).isPresent(), "original native projectile texture resolves");
        return Map.of("rod_texture", rodTexture.toString(), "cap_texture", capTexture.toString(),
                "focus_texture", thaumcraft.client.renderers.models.gear.ModelWand.FOCUS_TEXTURE.toString(),
                "native_focus_RGB", Integer.toHexString(color & 0xFFFFFF), "native_focus_ARGB", Integer.toHexString(color), "seen_pixels", "pending root exact97/99 inspection");
    }
    private boolean hand(Minecraft client) throws ReflectiveOperationException { require(client.player.containerMenu == client.player.inventoryMenu, "native hand SWAP outside menus"); if (!payer(client.player.getInventory().getItem(8))) { click(client, playerSlot(client, itemSlot(client.player, FrostClientSequence::payer)), 8, ClickType.SWAP); handChangedAt = client.player.tickCount; return false; } if (client.player.tickCount - handChangedAt < 6) return false; select(client); return true; }
    private static void stand(ServerPlayer player, double x, double y, double z) { var body = new AABB(x - .3, y + .001, z - .3, x + .3, y + 1.8, z + .3); BlockPos floor = BlockPos.containing(x, y - .01, z); require(player.level().noCollision(player, body) && !player.level().getBlockState(floor).getCollisionShape(player.level(), floor).isEmpty(), "declared collision-checked camera above existing solid floor"); player.teleportTo(x, y, z); }
    private static void lookAt(Minecraft client, Vec3 point) { Vec3 d = point.subtract(client.player.getEyePosition()); client.player.setYRot((float) Math.toDegrees(Math.atan2(-d.x, d.z))); client.player.yRotO = client.player.getYRot(); client.player.setXRot((float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)))); client.player.xRotO = client.player.getXRot(); }
    private static void select(Minecraft client) throws ReflectiveOperationException { client.player.getInventory().selected = 8; var ensure = client.gameMode.getClass().getDeclaredMethod("ensureHasSentCarriedItem"); ensure.setAccessible(true); ensure.invoke(client.gameMode); }
    private static int playerSlot(Minecraft client, int inventory) { return client.player.containerMenu.slots.stream().filter(slot -> slot.container == client.player.getInventory() && slot.getContainerSlot() == inventory).findFirst().orElseThrow().index; }
    private static void click(Minecraft client, int slot, int button, ClickType type) { client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId, slot, button, type, client.player); }
    /** Detached assertion copies only; never installed in any inventory or world. */
    private static boolean sameExcept(ItemStack first, ItemStack second, DataComponentType<?>... excluded) { ItemStack a = first.copy(), b = second.copy(); for (var type : excluded) { a.remove(type); b.remove(type); } return ItemStack.matches(a, b); }
    private static boolean finite(Vec3 value) { return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z); }
    private static Map<String, Integer> aspects(AspectList list) { var result = new LinkedHashMap<String, Integer>(); for (Aspect aspect : list.getAspects()) if (list.getAmount(aspect) > 0) result.put(aspect.getTag(), list.getAmount(aspect)); return result; }
    /** Read-only view of the accepted root-owned side-isolated session, never a session setter. */
    private static BlockPos serverSessionNode(ServerPlayer player, ItemStack wand) {
        try {
            var field = ItemWandCasting.class.getDeclaredField("SERVER_NODE_USES"); field.setAccessible(true); Object session = ((Map<?, ?>) field.get(null)).get(player);
            require(session != null && BookClientSmoke.field(session, "hand") == InteractionHand.MAIN_HAND && BookClientSmoke.field(session, "stack") == wand, "actual SERVER session owns exact main-hand native wand identity");
            return ((BlockPos) BookClientSmoke.field(session, "pos")).immutable();
        } catch (ReflectiveOperationException error) { throw new IllegalStateException("Read-only saved-node session observation needs root API reconciliation", error); }
    }
    private static CompoundTag beTag(BlockEntity tile) { return tile.saveWithFullMetadata(tile.getLevel().registryAccess()).copy(); }
    private static boolean payer(ItemStack value) { return value.is(ThaumcraftContent.WAND.get()) && ItemWandCasting.data(value).rod().equals("silverwood") && ItemWandCasting.data(value).cap().equals("thaumium"); }
    private static int emptyMain(Player player) { for (int i = 9; i < 36; i++) if (player.getInventory().getItem(i).isEmpty()) return i; throw new IllegalStateException("No genuinely empty main slot"); }
    private static int itemSlot(Player player, Predicate<ItemStack> wanted) { for (int i = 0; i < 36; i++) if (wanted.test(player.getInventory().getItem(i))) return i; throw new IllegalStateException("Missing actual finite Frost input/output"); }
    private static ItemStack stack(Player player, Item item) { return stack(player, value -> value.is(item)); }
    private static ItemStack stack(Player player, Predicate<ItemStack> wanted) { return player.getInventory().getItem(itemSlot(player, wanted)); }
    private static int count(Player player, Item item) { return player.getInventory().items.stream().filter(value -> value.is(item)).mapToInt(ItemStack::getCount).sum(); }
    private static Item frost() { return ThaumcraftFoci.FROST_FOCUS.get(); }
    private static Item excavation() { return ThaumcraftFoci.EXCAVATION_FOCUS.get(); }
    private static Item shard() { return ThaumcraftContent.SHARDS.get("aqua").get(); }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }
    private static String snbt(ItemStack value, ServerPlayer player) { return value.save(player.registryAccess()).toString(); }
    private TileNode node(ServerPlayer player) { require(player.level().getBlockEntity(chargingNode) instanceof TileNode, "actual saved node still exists"); return (TileNode) player.level().getBlockEntity(chargingNode); }
    private static ContainerArcaneWorkbench arcane(Minecraft client) { return (ContainerArcaneWorkbench) client.player.containerMenu; }
    private static ServerPlayer serverPlayer(Minecraft client) { return client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID()); }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private void onServer(Minecraft client, Runnable action) { require(serverAction == null || serverAction.isDone(), "no overlapping fixture/read-only server actions"); serverAction = CompletableFuture.runAsync(action, client.getSingleplayerServer()); }
    private boolean serverDone() { if (serverAction == null) return true; if (!serverAction.isDone()) return false; serverAction.join(); return true; }
    private void next(Step next) { step = next; ticks = 0; }
}
