package thaumcraft.fabric.clienttest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
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
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.client.ThaumcraftFocusClient;
import thaumcraft.client.ThaumcraftShockClient;
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
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.items.armor.ItemRobeArmor;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.WandVisDiscount;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.ItemFocusFire;
import thaumcraft.common.items.wands.foci.ItemFocusShock;
import thaumcraft.common.items.wands.foci.WandFocusActions;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.tiles.TileAlchemyFurnace;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileArcaneWorkbench;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.tiles.TilePedestal;

/** Eight focused scenes in a NEW owned copy of the accepted stopped goggles world. */
public final class RobeShockClientSequence {
    private enum Step {
        HANDOFF, SETUP, BOOK, BOOK_PAGE, CHARGE_TRAVEL, CHARGE_AIM, CHARGE_START, CHARGE_USE, CHARGE_STOP,
        FABRIC, CHEST, LEGS, BOOTS, CRAFT_TRAVEL, CRAFT_OPEN, CRAFT_FUND, CRAFT_GRID, CRAFT_READY,
        CRAFT_PREVIEW, CRAFT_TAKE, CRAFT_OUTPUT, WAND_PICK, WAND_RETURN, CRAFT_FINISH, CRAFT_VERIFIED,
        HEAD_OFF, HEAD_STORED, EQUIP_ROBES, EQUIP_WAIT, WEAR_TRAVEL, WEAR, WEAR_STOPPED, CHEST_OFF, CHEST_STORED,
        DYE, WASH_TRAVEL, WASH_HAND, WASH_COLORED, WASH_DEFAULT, WASH_VERIFY, WASH_SCENE,
        ANVIL_TRAVEL, ANVIL_OPEN, ANVIL_CHEST, ANVIL_FABRIC, ANVIL_READY, ANVIL_PREVIEW,
        ANVIL_TAKE, ANVIL_OUTPUT, ANVIL_STORE, ANVIL_STORED, ANVIL_VERIFIED,
        CHEST_ON, CHEST_ON_WAIT, HEAD_ON, HEAD_ON_WAIT, WORN_VERIFY, WORN_SCENE,
        SHOCK_BOOK, SHOCK_PAGE, SHOCK_CRAFT, FOCUS_ARM, FOCUS_PRESS, RADIAL, RADIAL_HOVER, RADIAL_SELECT, FOCUS_EQUIPPED,
        SHOT_TRAVEL, SHOT_AIM, SHOT_START, SHOT_LIVE, SHOT_CAPTURED, SHOT_STOP, SHOT_VERIFIED,
        FINAL_CHECK, FINAL_ACK, DONE
    }
    private enum ShotKind { RANGED, MISS, BLOCKED }
    private record Craft(String recipe, Item output, Item[] grid, int repetitions, Map<String, Integer> vis, String capture) {}
    private static final List<String> CAPTURES = List.of(
            "81-native-enchanted-fabric-twenty-finite-inputs-arcane-menu-en.png",
            "82-earned-cactus-worn-robe-original-purple-red-dye-native-menu-en.png",
            "83-native-colored-and-default-robe-two-cauldron-washes-world-en.png",
            "84-cactus-worn-robe-earned-twentieth-fabric-native-anvil-repair-en.png",
            "85-earned-three-robes-two-native-armor-layers-and-saved-goggles-en.png",
            "86-earned-shock-focus-native-held-F-selector-retained-fire-en.png",
            "87-native-paid-nineteen-centivis-long-range-shock-bolt-world-en.png",
            "88-native-paid-wall-blocked-shock-after-paid-miss-world-en.png");
    private static final Map<String, Integer> FABRIC_VIS = Map.of("aer", 1, "terra", 1, "ignis", 1, "aqua", 1, "ordo", 1, "perditio", 1);
    private static final List<EquipmentSlot> ROBE_SLOTS = List.of(EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    public final Map<String, Object> report = new LinkedHashMap<>();
    private final BlockPos workbench, center, chargingNode, cauldron, anvil, cactus, arena, wall;
    private final List<ItemStack> savedStacks = new ArrayList<>(), robes = new ArrayList<>();
    private final Map<BlockPos, BlockState> protectedBlocks = new LinkedHashMap<>();
    private final Map<BlockPos, Map<String, Object>> savedJars = new LinkedHashMap<>();
    private final List<Map<String, Object>> inputs = new ArrayList<>(), crafts = new ArrayList<>(), shots = new ArrayList<>();
    private final ClientTickEvent.Client useGuard = this::guardUse;
    private Step step = Step.HANDOFF, afterCraft;
    private Craft craft;
    private CompletableFuture<Void> serverAction;
    private CameraType previousCamera;
    private ItemStack expectedWand, savedFire, savedGoggles, apple, craftWand, wornChest, dyedChest, washedChest, repairedChest, earnedShock;
    private CompoundTag knowledgeBefore;
    private List<int[]> gridClicks;
    private Map<String, Integer> prices;
    private Map<String, Object> preview;
    private ChargeProbe charge;
    private ShockProbe shot;
    private TickEvent.Server nativeTicks;
    private LifecycleEvent.ServerState serverStopped;
    private InteractionEvent.RightClickItem nativeItemUses;
    private final List<Map<String, Object>> chargeUseEvents = new CopyOnWriteArrayList<>();
    private ServerPlayer wearSubject;
    private boolean savedEntityInvulnerability, wearVulnerable;
    private volatile Throwable observationFailure;
    private int ticks, totalTicks, turns, gridIndex, outputBefore, destination, equipIndex, targetId, shotIndex, paidTotal;
    private int handChangedAt = Integer.MIN_VALUE / 2, levelsBefore, xpInput, castTicks;
    private long lastNativeDispatch;
    private float wearHealthBefore;
    private volatile float observedWearHealth;
    private volatile boolean observedCactusSource;
    private boolean focusChanged, guardingUse, releaseSent;
    private String focusBinding, swapBinding;

    /** Root owns the private world copy and passes accepted goggles bench+32X, not the saved camera. */
    public RobeShockClientSequence(BlockPos workbench, BlockPos existingCenter) {
        this.workbench = workbench.immutable(); center = existingCenter.immutable();
        chargingNode = workbench.offset(5, 2, -3); cauldron = workbench.west(2); anvil = workbench.east(2);
        cactus = workbench.offset(5, 0, 5); arena = workbench.offset(10, 0, -8); wall = workbench.offset(10, 0, 3);
        report.put("scope", "clothing: NEW root-owned clothing-smoke-UUID copy of accepted STOPPED goggles-smoke-056d3340-a4f2-44ed-b006-54b6984c5e7f; source never opened/modified");
        report.put("progression_not_replayed", List.of("research acquisition", "metallurgy/equipment", "distillation/tubes/173 jar essentia", "altar/72 candles/infusion", "Salis/charged caps/three wand assemblies", "Fire Focus and goggles acquisition"));
        report.put("boundary", "Three actual saved wands, installed Fire, worn goggles, four phials,173 jar units,72 candles, empty earned Warded Jar, book/apple and all old stacks/machines are retained INPUTS. RAW INPUTS:80string in normal64+16 stacks,20purple wool,1red dye,4air shards,4nether quartz,1potato. ENCHFABRIC/FOCUSSHOCK and their parents are declared research fixtures, NOT earned. Empty arcane workbench/anvil, water cauldron3, isolated finite NORMAL node80 of each primal, bounded fresh floor, sand/cactus, later opaque wall, ordinary no-AI cow, daylight/clear weather and collision-checked positioning are fixtures. The saved test player's separate Entity Invulnerable flag is temporarily disabled ONLY for genuine cactus contact, then restored; this is declared fixture preparation, not armor damage injection. If saved XP is below one level, add only the bounded missing1..7 XP points, explicitly recorded. No fabric/robe/focus output grants, color/wear/repair setters, wand top-ups, backend crafting/charging/debits/damage/focus tick or FX packet injection.");
        report.put("native_action_boundary", "Real inventory PICKUP/QUICK_MOVE/SWAP packets; one native shift-result click performs20 paid fabric transactions, then three real robe crafts BEFORE equipping. Real finite node held-use retains Fire priority. Goggles are natively stored before cactus contact wears ONLY newly earned robes. Real ordinary workbench dye preserves actual wear/data; colored AND default cauldron washes spend3->2->1. Real anvil extraction spends the20th earned fabric/one level. Native head equip and held-F select the genuine new Shock, returning the exact old Fire. Four native paid Shock pulses:two ranged to observe real cooldown (only FIRST damage must equal4; ordinary iframes respected),ONE sky miss,ONE opaque-wall-blocked pulse. Native RMB/250ms dispatch and passive server POST observations only; no direct gameplay callbacks or key rebinding.");
        report.put("coordinates", Map.of("workbench", workbench.toShortString(), "existing_center", center.toShortString(), "finite_node", chargingNode.toShortString(), "cauldron", cauldron.toShortString(), "anvil", anvil.toShortString(), "sand_cactus", cactus.toShortString(), "cow", arena.toShortString(), "later_wall", wall.toShortString()));
        report.put("fresh_patch", Map.of("min", workbench.offset(-4, -2, -12).toShortString(), "max", workbench.offset(14, 5, 9).toShortString(), "refuse_existing_BE_TC_block_or_physical_drop", true));
        report.put("supplied_inputs", inputs); report.put("native_crafts", crafts); report.put("native_paid_shock", shots);
        report.put("capture_names", CAPTURES); report.put("capture_count", CAPTURES.size());
        report.put("visual_inspection", "Pending root inspection of exact81-88 PNGs. Model/resource/FX observations do not themselves prove seen textures, independent audio listening or full FX parity.");
        report.put("not_covered", List.of("full survival/research/XP acquisition", "creative/default wash and rejected/PvP/nonliving/unaffordable/depletion cases in this client story (server fixture scope)", "large25percent repair/enchanting/Repair enchantment/runic shielding", "other foci/upgrades/chains/lightning entities", "full armor combat or full FX/audio parity", "NeoForge"));
    }

    public boolean tick(Minecraft client, Consumer<String> capture) throws ReflectiveOperationException {
        try { return advance(client, capture); }
        catch (ReflectiveOperationException | RuntimeException | Error error) {
            releaseInputs(client);
            if (client.getSingleplayerServer() != null) client.getSingleplayerServer().execute(this::stopObservation);
            if (error instanceof RuntimeException) throw new IllegalStateException("Robe/Shock sequence failed at " + step + "; recipe=" + (craft == null ? "none" : craft.recipe()) + "; shot=" + shotIndex, error);
            throw error;
        }
    }

    private boolean advance(Minecraft client, Consumer<String> capture) throws ReflectiveOperationException {
        ++ticks; ++totalTicks;
        require(ticks <= 2000 && totalTicks <= 9000, "bounded focused clothing sequence timeout: " + step);
        if (observationFailure != null) throw new IllegalStateException("Native clothing observer failed", observationFailure);
        if (client.screen instanceof GuiFocusSelect && (step == Step.RADIAL || step == Step.RADIAL_HOVER || step == Step.RADIAL_SELECT)) ThaumcraftFocusClient.CHANGE_FOCUS.setDown(true);
        switch (step) {
            case HANDOFF -> {
                previousCamera = client.options.getCameraType(); client.options.setCameraType(CameraType.FIRST_PERSON); client.setScreen(null);
                client.options.keyUse.setDown(false); client.options.keyShift.setDown(false); client.options.keyAttack.setDown(false); releaseFocusKey(client);
                focusBinding = ThaumcraftFocusClient.CHANGE_FOCUS.saveString(); swapBinding = client.options.keySwapOffhand.saveString();
                require(client.player.containerMenu == client.player.inventoryMenu && client.player.containerMenu.getCarried().isEmpty(), "actual saved inventory/cursor intact");
                require(reward(client.player.getInventory().getItem(8)) && client.player.getItemBySlot(EquipmentSlot.HEAD).is(goggles()), "actual accepted wand8 and worn goggles INPUTS");
                select(client, 8); onServer(client, () -> prepare(serverPlayer(client))); next(Step.SETUP);
            }
            case SETUP -> {
                if (!serverDone() || ticks < 12 || !KnowledgeAccess.get(client.player).isResearchComplete("FOCUSSHOCK")) return false;
                client.setScreen(new GuiResearchRecipe(new GuiResearchBrowser(), ResearchCatalog.get("ENCHFABRIC"))); next(Step.BOOK);
            }
            case BOOK -> { if (ticks < 8 || !showRecipe(client, "arcane/robe_chestplate")) return false; next(Step.BOOK_PAGE); }
            case BOOK_PAGE -> {
                report.put("native_robe_book", Map.of("research", "ENCHFABRIC", "actual_synced_recipe", id("arcane/robe_chestplate").toString(), "screen_fixture_not_research_acquisition", true));
                client.setScreen(null); onServer(client, () -> stand(serverPlayer(client), chargingNode.getX() + .5, workbench.getY(), chargingNode.getZ() + 3.5)); next(Step.CHARGE_TRAVEL);
            }
            case CHARGE_TRAVEL -> {
                if (!serverDone() || ticks < 12 || !hand(client, RobeShockClientSequence::reward)) return false;
                lookAt(client, Vec3.atCenterOf(chargingNode)); next(Step.CHARGE_AIM);
            }
            case CHARGE_AIM -> {
                lookAt(client, Vec3.atCenterOf(chargingNode)); if (ticks < 12) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    require((Object) player != client.player && player.equals(client.player) && player.getId() == client.player.getId(),
                            "actual integrated client/server actors are DISTINCT objects but native-equal numeric-ID keys");
                    report.put("native_node_session_side_regression", Map.of("same_native_numeric_id", player.getId(),
                            "distinct_client_server_objects_compare_equal", true,
                            "boundary", "Only native charge/release; server POST must retain its own node session until its own release, with zero Fire expense"));
                    charge = new ChargeProbe(player, chargingNode);
                }); next(Step.CHARGE_START);
            }
            case CHARGE_START -> {
                if (!serverDone()) return false; lookAt(client, Vec3.atCenterOf(chargingNode)); client.options.keyUse.setDown(true);
                require(client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND).consumesAction(), "native held-use starts finite node charge despite retained Fire"); next(Step.CHARGE_USE);
            }
            case CHARGE_USE -> {
                lookAt(client, Vec3.atCenterOf(chargingNode));
                if (!WandData.PRIMALS.stream().allMatch(tag -> ItemWandCasting.getVis(client.player.getMainHandItem(), tag) >= 3200)) return false;
                client.options.keyUse.setDown(false); client.gameMode.releaseUsingItem(client.player); next(Step.CHARGE_STOP);
            }
            case CHARGE_STOP -> {
                if (ticks < 10 || client.player.isUsingItem() || charge.stopped < 2) return false;
                onServer(client, () -> { report.put("native_finite_charge", charge.finish()); expectedWand = serverPlayer(client).getMainHandItem().copy(); }); next(Step.FABRIC);
            }
            case FABRIC -> {
                if (!serverDone()) return false;
                beginCraft(client, new Craft("arcane/enchanted_fabric", fabric(), new Item[] {null, Items.STRING, null, Items.STRING, Items.PURPLE_WOOL, Items.STRING, null, Items.STRING, null}, 20, FABRIC_VIS, CAPTURES.get(0)), Step.CHEST);
            }
            case CHEST -> beginCraft(client, new Craft("arcane/robe_chestplate", chest(), new Item[] {fabric(), null, fabric(), fabric(), fabric(), fabric(), fabric(), fabric(), fabric()}, 1, Map.of("aer", 5), null), Step.LEGS);
            case LEGS -> beginCraft(client, new Craft("arcane/robe_leggings", legs(), new Item[] {fabric(), fabric(), fabric(), fabric(), null, fabric(), fabric(), null, fabric()}, 1, Map.of("aqua", 5), null), Step.BOOTS);
            case BOOTS -> beginCraft(client, new Craft("arcane/robe_boots", boots(), new Item[] {fabric(), null, fabric(), fabric(), null, fabric(), null, null, null}, 1, Map.of("terra", 3), null), Step.HEAD_OFF);
            case CRAFT_TRAVEL -> {
                if (!serverDone() || ticks < 10 || !(craft.vis().isEmpty() ? emptyHand(client) : hand(client, RobeShockClientSequence::reward))) return false;
                use(client, workbench, Direction.SOUTH, Vec3.atCenterOf(workbench).add(0, 0, .5)); next(Step.CRAFT_OPEN);
            }
            case CRAFT_OPEN -> {
                if (ticks < 8 || !(client.screen instanceof GuiArcaneWorkbench)) return false;
                for (int slot = 0; slot <= 10; slot++) require(arcane(client).getSlot(slot).getItem().isEmpty(), "genuinely empty new workbench/preview");
                require(arcane(client).getCarried().isEmpty(), "empty native cursor"); craftWand = expectedWand.copy();
                if (!craft.vis().isEmpty()) click(client, arcaneSlot(8), 0, ClickType.QUICK_MOVE); next(Step.CRAFT_FUND);
            }
            case CRAFT_FUND -> {
                if (ticks < 6 || !craft.vis().isEmpty() && !ItemStack.matches(arcane(client).getSlot(1).getItem(), expectedWand)) return false;
                require(craft.vis().isEmpty() ? arcane(client).getSlot(1).getItem().isEmpty() : client.player.getInventory().getItem(8).isEmpty(), "ordinary dye has NO wand; arcane uses ONLY real saved payer");
                gridClicks = recipeClicks(client); gridIndex = 0; next(Step.CRAFT_GRID);
            }
            case CRAFT_GRID -> {
                if (ticks < 2) return false;
                if (gridIndex == gridClicks.size()) { next(Step.CRAFT_READY); return false; }
                int[] at = gridClicks.get(gridIndex++); click(client, at[0], at[1], ClickType.PICKUP); ticks = 0;
            }
            case CRAFT_READY -> {
                if (ticks < 8 || !arcane(client).craftable() || !arcane(client).getSlot(0).getItem().is(craft.output())) return false;
                require(arcane(client).getCarried().isEmpty() && arcane(client).getSlot(0).getItem().getCount() == 1, "one native result preview, not a supplied output");
                verifyGrid(arcane(client), craft.repetitions()); onServer(client, () -> inspectCraft(serverPlayer(client))); next(Step.CRAFT_PREVIEW);
            }
            case CRAFT_PREVIEW -> {
                if (!serverDone()) return false;
                for (String tag : WandData.PRIMALS) require(arcane(client).requiredVis(tag) == prices.getOrDefault(tag, 0), "native server/client price agreement: " + tag);
                next(Step.CRAFT_TAKE); if (craft.capture() != null) capture.accept(craft.capture());
            }
            case CRAFT_TAKE -> {
                outputBefore = count(client.player, craft.output()); click(client, 0, 0, ClickType.QUICK_MOVE); next(Step.CRAFT_OUTPUT);
            }
            case CRAFT_OUTPUT -> {
                if (ticks < 8 || count(client.player, craft.output()) != outputBefore + craft.repetitions()) return false;
                verifyGrid(arcane(client), 0); require(arcane(client).getCarried().isEmpty(), "native shift-result stores all outputs without harness inventory writes");
                if (!craft.vis().isEmpty()) {
                    ItemStack actual = arcane(client).getSlot(1).getItem(); verifyDebit(craftWand, actual, prices, craft.repetitions()); expectedWand = actual.copy();
                    click(client, 1, 0, ClickType.PICKUP); next(Step.WAND_PICK);
                } else next(Step.CRAFT_FINISH);
            }
            case WAND_PICK -> {
                if (ticks < 6 || !ItemStack.matches(arcane(client).getCarried(), expectedWand)) return false;
                destination = emptyMain(client.player); click(client, arcaneSlot(destination), 0, ClickType.PICKUP); next(Step.WAND_RETURN);
            }
            case WAND_RETURN -> {
                if (ticks < 6 || !ItemStack.matches(client.player.getInventory().getItem(destination), expectedWand) || !arcane(client).getCarried().isEmpty()) return false;
                next(Step.CRAFT_FINISH);
            }
            case CRAFT_FINISH -> {
                onServer(client, () -> finishCraft(serverPlayer(client))); client.player.closeContainer(); next(Step.CRAFT_VERIFIED);
            }
            case CRAFT_VERIFIED -> { if (!serverDone() || ticks < 8) return false; next(afterCraft); }
            case HEAD_OFF -> {
                require(count(client.player, fabric()) == 1 && ItemStack.matches(client.player.getItemBySlot(EquipmentSlot.HEAD), savedGoggles), "all19 robe fabric spent before equip; twentieth reserved; accepted goggles untouched");
                click(client, 5, 0, ClickType.QUICK_MOVE); next(Step.HEAD_STORED);
            }
            case HEAD_STORED -> {
                if (ticks < 8 || !client.player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) return false;
                require(ItemStack.matches(stack(client.player, goggles()), savedGoggles), "real inventory click stored exact goggles before ANY incoming damage"); equipIndex = 0; next(Step.EQUIP_ROBES);
            }
            case EQUIP_ROBES -> {
                require(client.player.getItemBySlot(ROBE_SLOTS.get(equipIndex)).isEmpty(), "never overwrite pre-existing armor");
                click(client, inventorySlot(itemSlot(client.player, value -> value.is(robe(equipIndex)))), 0, ClickType.QUICK_MOVE); next(Step.EQUIP_WAIT);
            }
            case EQUIP_WAIT -> {
                if (ticks < 8 || !ItemStack.matches(client.player.getItemBySlot(ROBE_SLOTS.get(equipIndex)), robes.get(equipIndex))) return false;
                if (++equipIndex < 3) next(Step.EQUIP_ROBES); else next(Step.WEAR_TRAVEL);
            }
            case WEAR_TRAVEL -> {
                onServer(client, () -> {
                    var player = serverPlayer(client); require(player.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && ItemStack.matches(stack(player, goggles()), savedGoggles), "saved head safely unequipped before cactus contact");
                    require(player.getHealth() >= 6F && player.getAttributeValue(Attributes.ARMOR) == 6 && WandVisDiscount.percentagePoints(player, Aspect.AIR) == 5, "three genuine robes provide3+2+1 armor and2+2+1 points");
                    // Accepted fixture saves retain Entity's NBT Invulnerable
                    // independently of abilities.invulnerable / Survival mode.
                    wearSubject = player; wearHealthBefore = observedWearHealth = player.getHealth(); observedCactusSource = false;
                    wearVulnerable = true; player.setInvulnerable(false);
                    require(!player.isInvulnerableTo(player.damageSources().cactus()), "declared wearer fixture genuinely accepts native cactus damage");
                    stand(player, cactus.getX() + 1.25, cactus.getY(), cactus.getZ() + .5);
                }); next(Step.WEAR);
            }
            case WEAR -> {
                if (!serverDone()) return false;
                // A teleport beside the inset cactus collision box is not a
                // native movement/contact action. Walk into it using the real
                // client controls; never call entityInside/hurt or write wear.
                lookAt(client, Vec3.atCenterOf(cactus));
                if (ROBE_SLOTS.stream().anyMatch(slot -> client.player.getItemBySlot(slot).getDamageValue() < 1)) {
                    if (ticks < 10) return false;
                    require(ticks < 100, "native cactus walk must produce bounded contact wear; position=" + client.player.position()
                            + "; health=" + client.player.getHealth() + "; cactus=" + client.level.getBlockState(cactus));
                    client.options.keyUp.setDown(true);
                    return false;
                }
                client.options.keyUp.setDown(false);
                onServer(client, () -> {
                    var player = serverPlayer(client); Vec3 contact = player.position(); stand(player, cactus.getX() + 2.5, cactus.getY(), cactus.getZ() + .5);
                    require(observedCactusSource && observedWearHealth < wearHealthBefore && player.getHealth() > 0,
                            "passive native POST observed actual cactus damage before ordinary saturation regeneration; no hurt/heal/setDamage call");
                    for (int i = 0; i < 3; i++) {
                        ItemStack actual = player.getItemBySlot(ROBE_SLOTS.get(i)); require(actual.getDamageValue() >= 1 && actual.getDamageValue() <= 4 && sameExcept(actual, robes.get(i), DataComponents.DAMAGE), "only bounded native cactus wear changed each newly earned robe"); robes.set(i, actual.copy());
                    }
                    wornChest = robes.getFirst().copy(); require(ItemStack.matches(stack(player, goggles()), savedGoggles), "old goggles received ZERO contact wear");
                    player.setInvulnerable(savedEntityInvulnerability); wearVulnerable = false;
                    report.put("native_cactus_wear", Map.of("server_verified", true, "health", Map.of("before", wearHealthBefore, "minimum_observed_POST", observedWearHealth, "after", player.getHealth(), "native_last_source_cactus", observedCactusSource), "new_robes_snbt", robes.stream().map(value -> snbt(value, player)).toList(), "saved_goggles_unequipped_unchanged", true, "contact_action", "native cactus entityInside during real movement/contact at " + contact, "saved_entity_invulnerable_temporarily_disabled", savedEntityInvulnerability, "saved_entity_invulnerable_restored", player.isInvulnerable() == savedEntityInvulnerability, "artificial_damage_or_wear_writes", 0));
                }); next(Step.WEAR_STOPPED);
            }
            case WEAR_STOPPED -> { if (!serverDone() || ticks < 8) return false; next(Step.CHEST_OFF); }
            case CHEST_OFF -> { click(client, 6, 0, ClickType.QUICK_MOVE); next(Step.CHEST_STORED); }
            case CHEST_STORED -> {
                if (ticks < 8 || !client.player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) return false;
                require(ItemStack.matches(stack(client.player, chest()), wornChest), "native armor click retrieved same cactus-worn chest"); next(Step.DYE);
            }
            case DYE -> beginCraft(client, new Craft("robe_dye", chest(), new Item[] {chest(), Items.RED_DYE, null, null, null, null, null, null, null}, 1, Map.of(), CAPTURES.get(1)), Step.WASH_TRAVEL);
            case WASH_TRAVEL -> {
                onServer(client, () -> stand(serverPlayer(client), cauldron.getX() + .5, cauldron.getY(), cauldron.getZ() + 2.6)); next(Step.WASH_HAND);
            }
            case WASH_HAND -> {
                if (!serverDone() || ticks < 10 || !hand(client, value -> value.is(chest()))) return false;
                require(ItemStack.matches(client.player.getMainHandItem(), dyedChest) && water(client.level) == 3, "same real dyed/worn robe and supplied full cauldron");
                use(client, cauldron, Direction.SOUTH, Vec3.atCenterOf(cauldron).add(0, 0, .5)); next(Step.WASH_COLORED);
            }
            case WASH_COLORED -> {
                if (ticks < 8 || water(client.level) != 2 || ItemRobeArmor.color(client.player.getMainHandItem()) != ItemRobeArmor.DEFAULT_COLOR) return false;
                require(sameExcept(client.player.getMainHandItem(), dyedChest, DataComponents.DYED_COLOR), "colored wash preserves actual cactus wear and every non-color component");
                washedChest = client.player.getMainHandItem().copy(); use(client, cauldron, Direction.SOUTH, Vec3.atCenterOf(cauldron).add(0, 0, .5)); next(Step.WASH_DEFAULT);
            }
            case WASH_DEFAULT -> {
                if (ticks < 8 || water(client.level) != 1 || !ItemStack.matches(client.player.getMainHandItem(), washedChest)) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client); require(water(player.level()) == 1 && ItemStack.matches(player.getMainHandItem(), washedChest) && ItemRobeArmor.color(washedChest) == 0x6A3880, "server verifies BOTH genuine3->2colored/2->1default washes");
                    require(washedChest.get(DataComponents.DYED_COLOR).equals(chest().getDefaultInstance().get(DataComponents.DYED_COLOR)), "native worn-color prototype restored, not leather-brown component absence");
                    report.put("native_cauldron_washes", Map.of("server_verified", true, "levels", List.of(3, 2, 1), "colored_wash", true, "default_wash_also_paid", true, "default_rgb", "6A3880", "actual_worn_stack_snbt", snbt(washedChest, player), "native_survival_item_use", true, "color_or_water_output_setters", 0));
                    retained(player);
                }); next(Step.WASH_VERIFY);
            }
            case WASH_VERIFY -> { if (!serverDone()) return false; next(Step.WASH_SCENE); }
            case WASH_SCENE -> { lookAt(client, Vec3.atCenterOf(cauldron)); next(Step.ANVIL_TRAVEL); capture.accept(CAPTURES.get(2)); }
            case ANVIL_TRAVEL -> {
                onServer(client, () -> {
                    var player = serverPlayer(client); require(count(player, fabric()) == 1, "repair uses only twentieth genuinely crafted fabric");
                    int levelBefore = player.experienceLevel, totalBefore = player.totalExperience;
                    if (levelBefore < 1) {
                        xpInput = Math.max(1, player.getXpNeededForNextLevel() - (int) Math.floor(player.experienceProgress * player.getXpNeededForNextLevel()));
                        require(xpInput <= 7, "bounded missing XP INPUT, never30levels or a free anvil"); player.giveExperiencePoints(xpInput);
                        inputs.add(Map.of("fixture", "explicit XP INPUT only because actual saved XP is below one level", "points_added", xpInput, "saved_levels", levelBefore, "saved_total_points", totalBefore));
                    }
                    levelsBefore = player.experienceLevel; require(levelsBefore >= 1, "real native one-level repair funding");
                    stand(player, anvil.getX() + .5, anvil.getY(), anvil.getZ() + 2.6);
                }); next(Step.ANVIL_OPEN);
            }
            case ANVIL_OPEN -> {
                if (!serverDone() || ticks < 10 || !emptyHand(client)) return false;
                use(client, anvil, Direction.UP, Vec3.atCenterOf(anvil).add(0, .5, 0)); next(Step.ANVIL_CHEST);
            }
            case ANVIL_CHEST -> {
                if (ticks < 8 || !(client.screen instanceof AnvilScreen)) return false;
                for (int i = 0; i <= 2; i++) require(repair(client).getSlot(i).getItem().isEmpty(), "genuinely empty native anvil");
                click(client, anvilSlot(itemSlot(client.player, value -> value.is(chest()))), 0, ClickType.QUICK_MOVE); next(Step.ANVIL_FABRIC);
            }
            case ANVIL_FABRIC -> {
                if (ticks < 8 || !ItemStack.matches(repair(client).getSlot(0).getItem(), washedChest)) return false;
                click(client, anvilSlot(itemSlot(client.player, value -> value.is(fabric()))), 0, ClickType.QUICK_MOVE); next(Step.ANVIL_READY);
            }
            case ANVIL_READY -> {
                if (ticks < 8 || !repair(client).getSlot(2).getItem().is(chest()) || repair(client).getSlot(2).getItem().getDamageValue() != 0) return false;
                require(repair(client).getCost() == 1, "one actual material repairs bounded cactus wear for one XP level");
                onServer(client, () -> {
                    var player = serverPlayer(client); var menu = (AnvilMenu) player.containerMenu; ItemStack result = menu.getSlot(2).getItem();
                    require(ItemStack.matches(menu.getSlot(0).getItem(), washedChest) && menu.getSlot(1).getItem().is(fabric()) && menu.getSlot(1).getItem().getCount() == 1 && menu.getCost() == 1 && player.experienceLevel == levelsBefore, "native server anvil owns genuine worn robe/last earned fabric, XP not yet paid");
                    require(result.getDamageValue() == 0 && result.getOrDefault(DataComponents.REPAIR_COST, 0) == 1 && sameExcept(result, washedChest, DataComponents.DAMAGE, DataComponents.REPAIR_COST), "native anvil preserves washed color/other earned data");
                    repairedChest = result.copy(); report.put("native_anvil_preview", Map.of("server_verified", true, "actual_worn_input", snbt(washedChest, player), "earned_fabric_input", 1, "damage_before", washedChest.getDamageValue(), "actual_preview", snbt(result, player), "levels_cost", 1, "levels_before", levelsBefore, "xp_points_fixture", xpInput));
                }); next(Step.ANVIL_PREVIEW);
            }
            case ANVIL_PREVIEW -> { if (!serverDone()) return false; next(Step.ANVIL_TAKE); capture.accept(CAPTURES.get(3)); }
            case ANVIL_TAKE -> { click(client, 2, 0, ClickType.PICKUP); next(Step.ANVIL_OUTPUT); }
            case ANVIL_OUTPUT -> {
                if (ticks < 8 || !ItemStack.matches(repair(client).getCarried(), repairedChest)) return false;
                destination = emptyMain(client.player); click(client, anvilSlot(destination), 0, ClickType.PICKUP); next(Step.ANVIL_STORE);
            }
            case ANVIL_STORE -> { if (ticks < 8 || !repair(client).getCarried().isEmpty()) return false; next(Step.ANVIL_STORED); }
            case ANVIL_STORED -> {
                onServer(client, () -> {
                    var player = serverPlayer(client); require(ItemStack.matches(stack(player, chest()), repairedChest) && count(player, fabric()) == 0 && player.experienceLevel == levelsBefore - 1 && player.containerMenu.getCarried().isEmpty(), "real anvil extraction paid earned fabric and native XP");
                    for (int i = 0; i <= 2; i++) require(player.containerMenu.getSlot(i).getItem().isEmpty(), "native anvil fully empty after extraction");
                    robes.set(0, repairedChest.copy()); report.put("native_anvil_repair", Map.of("server_verified", true, "damage_from_actual_cactus", washedChest.getDamageValue(), "damage_after", repairedChest.getDamageValue(), "earned_fabric_spent", 1, "levels_paid", 1, "levels_after", player.experienceLevel, "actual_output", snbt(repairedChest, player)));
                }); client.player.closeContainer(); next(Step.ANVIL_VERIFIED);
            }
            case ANVIL_VERIFIED -> { if (!serverDone() || ticks < 8) return false; next(Step.CHEST_ON); }
            case CHEST_ON -> { click(client, inventorySlot(itemSlot(client.player, value -> value.is(chest()))), 0, ClickType.QUICK_MOVE); next(Step.CHEST_ON_WAIT); }
            case CHEST_ON_WAIT -> { if (ticks < 8 || !ItemStack.matches(client.player.getItemBySlot(EquipmentSlot.CHEST), repairedChest)) return false; next(Step.HEAD_ON); }
            case HEAD_ON -> { click(client, inventorySlot(itemSlot(client.player, value -> ItemStack.matches(value, savedGoggles))), 0, ClickType.QUICK_MOVE); next(Step.HEAD_ON_WAIT); }
            case HEAD_ON_WAIT -> {
                if (ticks < 8 || !ItemStack.matches(client.player.getItemBySlot(EquipmentSlot.HEAD), savedGoggles)) return false;
                onServer(client, () -> { inspectWearer(serverPlayer(client)); retained(serverPlayer(client)); stand(serverPlayer(client), workbench.getX() + 3.5, workbench.getY(), workbench.getZ() + 3.5); }); next(Step.WORN_VERIFY);
            }
            case WORN_VERIFY -> {
                if (!serverDone() || ticks < 10) return false;
                report.put("native_worn_presentation", presentation(client)); client.setScreen(new InventoryScreen(client.player)); next(Step.WORN_SCENE);
            }
            case WORN_SCENE -> { if (ticks < 10) return false; next(Step.SHOCK_BOOK); capture.accept(CAPTURES.get(4)); }
            case SHOCK_BOOK -> { client.setScreen(new GuiResearchRecipe(new GuiResearchBrowser(), ResearchCatalog.get("FOCUSSHOCK"))); turns = 0; next(Step.SHOCK_PAGE); }
            case SHOCK_PAGE -> {
                if (ticks < 8 || !showRecipe(client, "arcane/focus_shock")) return false;
                report.put("native_shock_book", Map.of("research", "FOCUSSHOCK", "actual_synced_recipe", id("arcane/focus_shock").toString(), "center_is_potato", true, "research_screen_fixture", true)); next(Step.SHOCK_CRAFT);
            }
            case SHOCK_CRAFT -> beginCraft(client, new Craft("arcane/focus_shock", shock(), new Item[] {shard(), Items.QUARTZ, shard(), Items.QUARTZ, Items.POTATO, Items.QUARTZ, shard(), Items.QUARTZ, shard()}, 1, Map.of("aer", 10, "ordo", 10, "perditio", 10), null), Step.FOCUS_ARM);
            case FOCUS_ARM -> { if (!hand(client, RobeShockClientSequence::reward) || ticks < 10) return false; next(Step.FOCUS_PRESS); }
            case FOCUS_PRESS -> {
                if (!client.isWindowActive()) return false;
                require(ThaumcraftFocusClient.CHANGE_FOCUS.matches(GLFW.GLFW_KEY_F, 0), "actual native F binding, never permanently rebound");
                KeyMapping.click(InputConstants.getKey(focusBinding)); ThaumcraftFocusClient.CHANGE_FOCUS.setDown(true);
                if (ThaumcraftFocusClient.CHANGE_FOCUS.same(client.options.keySwapOffhand)) client.options.keySwapOffhand.setDown(true);
                next(Step.RADIAL);
            }
            case RADIAL -> {
                if (!client.isWindowActive()) { releaseFocusKey(client); next(Step.FOCUS_PRESS); return false; }
                if (ticks < 8 || !(client.screen instanceof GuiFocusSelect)) return false;
                require(ItemStack.matches(FocusData.get(client.player.getMainHandItem()), savedFire) && ItemStack.matches(stack(client.player, shock()), earnedShock), "real selector displays new loose Shock with old Fire still installed");
                int[] point = FocusClientSequence.hoverPoint(client.screen); client.screen.mouseMoved(point[0], point[1]);
                // The radial has a TRANSPARENT background: drawing it again in
                // afterRender stacks two tooltips/footers on the world. Move
                // the cursor inside this owned active test window instead and
                // capture its ONE ordinary native frame.
                GLFW.glfwSetCursorPos(client.getWindow().getWindow(),
                        point[0] * (double) client.getWindow().getScreenWidth() / client.screen.width,
                        point[1] * (double) client.getWindow().getScreenHeight() / client.screen.height);
                next(Step.RADIAL_HOVER);
            }
            case RADIAL_HOVER -> {
                if (!client.isWindowActive()) { releaseFocusKey(client); next(Step.FOCUS_PRESS); return false; }
                if (ticks < 3 || !(client.screen instanceof GuiFocusSelect)) return false;
                int[] point = FocusClientSequence.hoverPoint(client.screen);
                double mouseX = client.mouseHandler.xpos() * client.screen.width / client.getWindow().getScreenWidth();
                double mouseY = client.mouseHandler.ypos() * client.screen.height / client.getWindow().getScreenHeight();
                require(Math.abs(mouseX - point[0]) <= 1 && Math.abs(mouseY - point[1]) <= 1, "native window cursor actually hovers the earned Shock choice");
                report.put("native_F_selector", Map.of("held_native_key", focusBinding, "binding_changes", 0, "actual_loose_shock", true, "old_fire_still_installed_at_preview", true, "pauses_world", client.screen.isPauseScreen(), "capture", "single native frame; declared cursor positioning inside the owned active test window, no second overlay render"));
                next(Step.RADIAL_SELECT); capture.accept(CAPTURES.get(5));
            }
            case RADIAL_SELECT -> {
                if (!client.isWindowActive()) { releaseFocusKey(client); next(Step.FOCUS_PRESS); return false; }
                require(client.screen instanceof GuiFocusSelect, "same actual native radial"); var radial = client.screen; int[] point = FocusClientSequence.hoverPoint(radial);
                radial.mouseMoved(point[0], point[1]); require(radial.mouseClicked(point[0], point[1], 0), "native radial mouse selection accepts actual crafted Shock"); radial.mouseReleased(point[0], point[1], 0); releaseFocusKey(client); next(Step.FOCUS_EQUIPPED);
            }
            case FOCUS_EQUIPPED -> {
                if (ticks < 10 || !ItemStack.matches(FocusData.get(client.player.getMainHandItem()), earnedShock) || count(client.player, shock()) != 0 || count(client.player, ThaumcraftFoci.FIRE_FOCUS.get()) != 1) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client); ItemStack actual = player.getMainHandItem();
                    require(sameExcept(actual, expectedWand, ThaumcraftFoci.WAND_FOCUS.get()) && ItemStack.matches(FocusData.get(actual), earnedShock) && ItemStack.matches(stack(player, ThaumcraftFoci.FIRE_FOCUS.get()), savedFire), "native F changed ONLY focus component and returned exact original Fire stack");
                    expectedWand = actual.copy(); focusChanged = true; var focus = (ItemFocusShock) earnedShock.getItem();
                    require(focus.getFocusColor(earnedShock) == 10466239 && focus.getSortingHelper(earnedShock).startsWith("BL") && focus.getActivationCooldown(earnedShock) == 250 && !focus.isVisCostPerTick(earnedShock) && focus.getAnimation(earnedShock) == ItemFocusBasic.WandFocusAnimation.CHARGE, "actual basic Shock color/sorting/250ms per-activation/CHARGE contract");
                    var cow = EntityType.COW.create(player.serverLevel()); require(cow != null, "ordinary living target INPUT");
                    cow.setNoAi(true); cow.setPersistenceRequired(); cow.setCustomName(Component.literal("Native Shock: fourteen blocks / paid19")); cow.setCustomNameVisible(true);
                    cow.moveTo(arena.getX() + .5, arena.getY(), arena.getZ() + .5, 180, 0);
                    require(player.serverLevel().addFreshEntity(cow) && cow.getHealth() == cow.getMaxHealth() && !cow.isOnFire(), "fresh unharmed/no-AI cow, no damage/fire/bolt injection"); targetId = cow.getId();
                    inputs.add(Map.of("fixture", "ordinary no-AI long-range cow INPUT", "uuid", cow.getUUID().toString(), "position", cow.position().toString()));
                    report.put("native_focus_switch", Map.of("server_verified", true, "new_focus", snbt(earnedShock, player), "old_fire_returned_exact", snbt(savedFire, player), "wand_vis_unchanged", true, "direct_focus_writes", 0)); retained(player);
                }); next(Step.SHOT_TRAVEL);
            }
            case SHOT_TRAVEL -> {
                if (!serverDone()) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client); if (kind() == ShotKind.BLOCKED) addWall(player.serverLevel());
                    stand(player, arena.getX() + .5, arena.getY(), arena.getZ() + 14.5);
                }); next(Step.SHOT_AIM);
            }
            case SHOT_AIM -> {
                if (!serverDone() || !hand(client, RobeShockClientSequence::reward) || !(client.level.getEntity(targetId) instanceof Cow)) return false;
                aimShot(client); if (ticks < 16) return false;
                onServer(client, () -> shot = new ShockProbe(serverPlayer(client), (Cow) serverPlayer(client).serverLevel().getEntity(targetId), kind(), lastNativeDispatch)); next(Step.SHOT_START);
            }
            case SHOT_START -> {
                if (!serverDone() || !client.isWindowActive()) return false; aimShot(client);
                guardingUse = true; releaseSent = false; castTicks = 0; client.options.keyUse.setDown(true);
                require(client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND).consumesAction(), "one native RMB starts real Shock use"); next(Step.SHOT_LIVE);
            }
            case SHOT_LIVE -> {
                aimShot(client); if (shot.frame.paid() < 1) return false;
                var fx = shockEffects(client);
                if (fx.isEmpty()) { require(ticks < 30, "real paid Shock must deliver a live client bolt, never a synthetic test effect"); return false; }
                if (kind() == ShotKind.MISS) {
                    require(fx.stream().allMatch(bolt -> bolt.impactGlints() == 0), "actual paid sky-miss S2C has NO target/block impact, not just an unharmed cow");
                    report.put("native_paid_miss_client_FX", fx.stream().map(BoltView::evidence).toList()); releaseUse(client); next(Step.SHOT_STOP); return false;
                }
                require(fx.stream().anyMatch(bolt -> bolt.impactGlints() == 5), "real paid hit/blocked endpoint supplies production impact glints");
                if (kind() == ShotKind.RANGED && fx.stream().noneMatch(BoltView::reached)) { require(ticks < 30, "capture waits for real long-range bolt/impact, not its first short partial frame"); return false; }
                if (kind() == ShotKind.RANGED && ((Cow) client.level.getEntity(targetId)).getHealth() >= shot.healthBefore) return false;
                if (kind() == ShotKind.BLOCKED) require(fx.stream().anyMatch(bolt -> client.level.getBlockState(BlockPos.containing(bolt.target().add(client.player.getLookAngle().scale(.01)))).is(Blocks.STONE_BRICKS)), "actual S2C bolt ends at opaque wall rather than the living target behind it");
                report.put(kind() == ShotKind.RANGED ? "native_ranged_bolt_capture" : "native_blocked_bolt_capture", Map.of("paid_pulses_at_observation", shot.frame.paid(), "actual_air_debit_at_observation", shot.frame.paid() * 19, "server_health", shot.frame.health(), "client_health", ((Cow) client.level.getEntity(targetId)).getHealth(), "production_active_bolts", fx.stream().map(BoltView::evidence).toList(), "synthetic_FX_calls", 0));
                next(Step.SHOT_CAPTURED); capture.accept(CAPTURES.get(kind() == ShotKind.RANGED ? 6 : 7));
            }
            case SHOT_CAPTURED -> {
                if (kind() == ShotKind.RANGED && shot.frame.paid() < 2 && !releaseSent) return false;
                releaseUse(client); next(Step.SHOT_STOP);
            }
            case SHOT_STOP -> {
                releaseUse(client); if (client.player.isUsingItem() || shot.frame.stopped() < 2) return false; guardingUse = false;
                onServer(client, () -> { var result = shot.finish(); paidTotal += shot.frame.paid(); require(paidTotal <= 6, "bounded finite client story, NOT a100vis depletion treadmill"); lastNativeDispatch = shot.previousDispatch; shots.add(result); expectedWand = serverPlayer(client).getMainHandItem().copy(); }); next(Step.SHOT_VERIFIED);
            }
            case SHOT_VERIFIED -> { if (!serverDone() || ticks < 8) return false; if (++shotIndex < 3) next(Step.SHOT_TRAVEL); else next(Step.FINAL_CHECK); }
            case FINAL_CHECK -> { onServer(client, () -> { finalState(serverPlayer(client)); stopObservation(); }); next(Step.FINAL_ACK); }
            case FINAL_ACK -> {
                if (!serverDone()) return false;
                require(nativeTicks == null && serverStopped == null && !ClientTickEvent.CLIENT_PRE.isRegistered(useGuard), "all temporary native observers/guards removed");
                require(focusBinding.equals(ThaumcraftFocusClient.CHANGE_FOCUS.saveString()) && swapBinding.equals(client.options.keySwapOffhand.saveString()), "original user controls unchanged");
                report.put("native_client_ticks", totalTicks); next(Step.DONE);
            }
            case DONE -> { client.setScreen(null); releaseInputs(client); return true; }
        }
        return false;
    }

    private void prepare(ServerPlayer player) {
        require(workbench.equals(center.offset(144, 0, 0)), "root must pass accepted goggles bench+32X (1618,216,377), center1474,216,377");
        var level = player.serverLevel(); savedEntityInvulnerability = player.isInvulnerable(); expectedWand = player.getInventory().getItem(8).copy(); savedFire = FocusData.get(expectedWand).copy();
        savedGoggles = player.getItemBySlot(EquipmentSlot.HEAD).copy(); apple = player.getOffhandItem().copy();
        require(reward(expectedWand) && ItemWandCasting.getMaxVis(expectedWand) == 10000 && ItemWandCasting.data(expectedWand).vis().equals(Map.of("aer", 650, "aqua", 665, "ignis", 448, "ordo", 530, "perditio", 530, "terra", 550)) && expectedWand.getOrDefault(ThaumcraftWands.WAND_CRAFT_COST.get(), 0) == 54 && savedFire.is(ThaumcraftFoci.FIRE_FOCUS.get()), "exact real accepted goggles-client2 wand INPUT, not reconstructed or topped up");
        require(savedGoggles.is(goggles()) && savedGoggles.getDamageValue() == 0 && apple.is(Items.APPLE) && apple.getCount() == 1 && count(player, ThaumcraftContent.WAND.get()) == 3 && count(player, ThaumcraftContent.EMPTY_PHIAL.get()) == 4 && count(player, ThaumcraftContent.FILLED_PHIAL.get()) == 0 && count(player, ThaumcraftContent.JAR.get().asItem()) == 1, "actual worn goggles/three wands/four phials/apple/earned empty jar retained INPUTS");
        for (EquipmentSlot slot : ROBE_SLOTS) require(player.getItemBySlot(slot).isEmpty(), "accepted goggles world's other armor slots genuinely empty");
        for (ItemStack value : player.getInventory().items) if (!value.isEmpty() && !reward(value)) savedStacks.add(value.copy());
        for (Item item : List.of(Items.STRING, Items.PURPLE_WOOL, Items.RED_DYE, shard(), Items.QUARTZ, Items.POTATO, fabric(), chest(), legs(), boots(), shock())) require(count(player, item) == 0, "new raw inputs/outputs cannot hide an old saved stack: " + itemId(item));
        for (int x = (center.getX() - 12) >> 4; x <= (center.getX() + 130) >> 4; x++) for (int z = (center.getZ() - 10) >> 4; z <= (center.getZ() + 11) >> 4; z++) level.getChunk(x, z);
        for (BlockPos at : BlockPos.betweenClosed(center.offset(-10, 0, -9), center.offset(130, 5, 10))) {
            if (BuiltInRegistries.BLOCK.getKey(level.getBlockState(at).getBlock()).getNamespace().equals("thaumcraft")) protectedBlocks.put(at.immutable(), level.getBlockState(at));
            if (level.getBlockEntity(at) instanceof TileJar jar) savedJars.put(at.immutable(), jarState(jar));
        }
        require(savedJars.size() == 12 && savedJars.values().stream().mapToInt(value -> (Integer) value.get("amount")).sum() == 173, "actual saved twelve jars/173units, not a supplied essentia source");
        require(level.getBlockEntity(center.above(2)) instanceof TileInfusionMatrix matrix && matrix.isActive() && !matrix.isCrafting(), "actual saved formed idle altar");
        require(protectedBlocks.values().stream().filter(value -> value.is(ThaumcraftInfusion.TALLOW_CANDLE.get())).count() == 72, "all72 actual saved candles");
        report.put("actual_saved_inputs", Map.of("main_stacks_snbt", player.getInventory().items.stream().filter(value -> !value.isEmpty()).map(value -> snbt(value, player)).toList(), "focused_wand_snbt", snbt(expectedWand, player), "head_snbt", snbt(savedGoggles, player), "offhand_snbt", snbt(apple, player), "saved_total_xp", player.totalExperience, "saved_levels", player.experienceLevel, "jar_units", 173, "candles", 72, "acquisition_replayed", false, "entity_invulnerable", savedEntityInvulnerability));
        patch(level); level.setBlockAndUpdate(workbench, ThaumcraftArcane.ARCANE_WORKBENCH.get().defaultBlockState()); level.setBlockAndUpdate(anvil, Blocks.ANVIL.defaultBlockState());
        level.setBlockAndUpdate(cauldron, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        level.setBlockAndUpdate(cactus.below(), Blocks.SAND.defaultBlockState()); level.setBlockAndUpdate(cactus, Blocks.CACTUS.defaultBlockState());
        var values = new AspectList(); for (String tag : WandData.PRIMALS) values.add(Aspect.getAspect(tag), 80);
        for (BlockPos at : BlockPos.betweenClosed(chargingNode.offset(-4, -4, -4), chargingNode.offset(4, 4, 4))) require(!level.hasChunkAt(at) || !(level.getBlockEntity(at) instanceof TileNode), "finite INPUT isolated from old/native nodes, no bullying or deletion");
        level.setBlockAndUpdate(chargingNode, ThaumcraftContent.AURA_NODE.get().defaultBlockState()); node(level, chargingNode).setAspects(values); // Declared finite NORMAL NODE INPUT only.
        require(node(level, chargingNode).getNodeType() == TileNode.NodeType.NORMAL, "ordinary NORMAL fixture");
        inputs.add(Map.of("fixture", "finite NORMAL node current+base INPUT", "position", chargingNode.toShortString(), "primals_each", 80, "naturally_found_claimed", false));
        long beforeTime = level.getDayTime(); level.setDayTime(6000); level.setWeatherParameters(0, 12000, false, false);
        inputs.add(Map.of("fixture", "daylight/clear weather and bounded floor/camera/workbench/anvil/full cauldron/sand/cactus", "old_day_time", beforeTime, "new_day_time", 6000, "cauldron_initial_level", 3, "normal_ticks_retained", true));
        var knowledge = KnowledgeAccess.get(player); var original = knowledge.save().copy(); var research = new LinkedHashSet<String>();
        for (String key : List.of("ENCHFABRIC", "FOCUSSHOCK")) { research.addAll(ResearchCatalog.get(key).parents()); research.addAll(ResearchCatalog.get(key).parentsHidden()); research.add(key); }
        var beforeResearch = new LinkedHashMap<String, Boolean>(); for (String key : research) { beforeResearch.put(key, knowledge.isResearchComplete(key)); knowledge.addResearch(key); }
        require(knowledge.isResearchComplete("NODEPRESERVE") && knowledge.isResearchComplete("GOGGLES"), "old earned/declared accepted research retained");
        knowledgeBefore = knowledge.save().copy(); KnowledgeEvents.sync(player);
        report.put("research_fixture", Map.of("keys", List.copyOf(research), "already_complete_before", beforeResearch, "earned_here", false, "original_saved_knowledge_snbt", original.toString(), "after_declared_fixture_snbt", knowledgeBefore.toString()));
        supply(player, Items.STRING, 64); supply(player, Items.STRING, 16); supply(player, Items.PURPLE_WOOL, 20); supply(player, Items.RED_DYE, 1); supply(player, shard(), 4); supply(player, Items.QUARTZ, 4); supply(player, Items.POTATO, 1);
        player.setGameMode(GameType.SURVIVAL); player.setShiftKeyDown(false); player.inventoryMenu.broadcastChanges(); retained(player); startObservation(player);
    }

    private void inspectCraft(ServerPlayer player) {
        var table = (TileArcaneWorkbench) player.level().getBlockEntity(workbench); var match = ArcaneCrafting.find(player, table);
        prices = craft.vis().isEmpty() ? Map.of() : ArcaneVis.price(craft.vis(), table.getItem(10), player);
        require(match != null && match.recipe().id().equals(id(craft.recipe())) && match.output().is(craft.output()) && match.output().getCount() == 1 && match.arcane() == !craft.vis().isEmpty() && match.prices().equals(prices), "actual synced recipe/native actor-priced match");
        if (craft.output() == fabric()) {
            require(WandVisDiscount.percentagePoints(player, Aspect.AIR) == 5 && prices.values().stream().allMatch(value -> value == 85) && prices.size() == 6, "native one-vis goggles/Thaumium FLOAT price85, NOT84");
        } else if (!craft.vis().isEmpty() && craft.output() != shock()) {
            require(ROBE_SLOTS.stream().allMatch(slot -> player.getItemBySlot(slot).isEmpty()) && WandVisDiscount.percentagePoints(player, Aspect.AIR) == 5, "all THREE robes crafted before any earned robe can discount itself");
            require(prices.equals(Map.of(craft.output() == chest() ? "aer" : craft.output() == legs() ? "aqua" : "terra", craft.output() == boots() ? 254 : 424)), "native5vis424/3vis254 goggles-only prices");
        } else if (craft.output() == shock()) require(WandVisDiscount.percentagePoints(player, Aspect.AIR) == 10 && ItemStack.matches(FocusData.get(table.getItem(10)), savedFire), "native Shock craft uses actual ten worn points and retains old installed Fire");
        else {
            require(table.getItem(10).isEmpty() && ItemStack.matches(table.getItem(0), wornChest) && table.getItem(1).is(Items.RED_DYE) && match.recipe().value().getSerializer() == ThaumcraftWearables.ROBE_DYE_RECIPE_SERIALIZER.get(), "one real cactus-worn robe/one RAW red dye uses registered original dynamic recipe, no inserted wand");
            require(ItemRobeArmor.color(match.output()) == 0x8C3960 && sameExcept(match.output(), wornChest, DataComponents.DYED_COLOR), "default purple participates in original RED mix; actual wear and ALL other components retained");
        }
        var materials = new LinkedHashMap<String, Integer>(); for (Item item : craft.grid()) if (item != null) materials.merge(itemId(item), 1, Integer::sum);
        preview = Map.of("recipe", match.recipe().id().toString(), "materials_each", materials, "repetitions", craft.repetitions(), "base_whole_vis", craft.vis(), "native_centivis_each", prices, "native_discount_points", WandVisDiscount.percentagePoints(player, Aspect.AIR), "real_preview_snbt", snbt(match.output(), player), "payer_before", snbt(craftWand, player));
    }

    private void finishCraft(ServerPlayer player) {
        require(((TileArcaneWorkbench) player.level().getBlockEntity(workbench)).isEmpty() && player.containerMenu.getCarried().isEmpty(), "actual table/cursor empty after native result and payer retrieval");
        if (craft.output() == fabric()) require(count(player, fabric()) == 20 && count(player, Items.STRING) == 0 && count(player, Items.PURPLE_WOOL) == 0, "80raw string/20colored wool became20 real paid fabric, no leftovers or grants");
        if (craft.recipe().startsWith("arcane/robe_")) {
            ItemStack actual = stack(player, craft.output()); require(actual.getDamageValue() == 0 && ItemRobeArmor.color(actual) == 0x6A3880, "new actual robe initially undamaged/default purple"); robes.add(actual.copy());
        }
        if (craft.recipe().equals("robe_dye")) {
            dyedChest = stack(player, chest()).copy(); require(ItemRobeArmor.color(dyedChest) == 0x8C3960 && sameExcept(dyedChest, wornChest, DataComponents.DYED_COLOR) && count(player, Items.RED_DYE) == 0, "native ordinary result extraction consumed only raw dye and preserved actual earned robe wear/data");
            report.put("native_original_dye", Map.of("server_verified", true, "input_worn_robe", snbt(wornChest, player), "raw_red_dyes_spent", 1, "original_default_rgb", "6A3880", "actual_mixed_rgb", "8C3960", "actual_result_snbt", snbt(dyedChest, player), "wear_and_other_data_preserved", true));
        }
        if (craft.output() == shock()) {
            earnedShock = stack(player, shock()).copy(); require(count(player, shock()) == 1 && count(player, shard()) == 0 && count(player, Items.QUARTZ) == 0 && count(player, Items.POTATO) == 0, "four air/four quartz/one potato became exactly one real Shock focus");
        }
        var entry = new LinkedHashMap<String, Object>(preview); entry.put("native_shift_result_clicks", 1); entry.put("native_paid_transactions", craft.repetitions()); entry.put("payer_after", snbt(expectedWand, player)); entry.put("server_verified", true); entry.put("output_grants", 0); crafts.add(entry); retained(player);
    }

    private void inspectWearer(ServerPlayer player) {
        require(ItemStack.matches(player.getItemBySlot(EquipmentSlot.HEAD), savedGoggles), "same accepted goggles back in actual head slot, unchanged");
        var stats = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < 3; i++) {
            ItemStack actual = player.getItemBySlot(ROBE_SLOTS.get(i)); require(ItemStack.matches(actual, robes.get(i)) && actual.getItem() instanceof ItemRobeArmor, "actual earned robe equipped via inventory click");
            var armor = (ItemRobeArmor) actual.getItem(); int durability = i == 0 ? 400 : i == 1 ? 375 : 325;
            require(actual.getMaxDamage() == durability && armor.getDefense() == 3 - i && armor.getEnchantmentValue() == 25 && actual.getRarity() == Rarity.UNCOMMON && armor.getVisDiscount(actual, player, Aspect.AIR) == (i == 2 ? 1 : 2) && armor.getRunicCharge(actual) == 0, "original native durability/armor/enchantability/rarity/discount, no invented shield");
            require(armor.isValidRepairItem(actual, new ItemStack(fabric())) && !armor.isValidRepairItem(actual, new ItemStack(Items.GOLD_INGOT)) && !armor.isValidRepairItem(actual, new ItemStack(Items.LEATHER)) && !armor.isValidRepairItem(actual, new ItemStack(Items.PURPLE_WOOL)), "ONLY actual enchanted fabric repairs robes");
            stats.add(Map.of("slot", ROBE_SLOTS.get(i).getName(), "actual_stack", snbt(actual, player), "durability", durability, "armor", armor.getDefense(), "discount_points", armor.getVisDiscount(actual, player, Aspect.AIR)));
        }
        require(player.getAttributeValue(Attributes.ARMOR) == 7 && player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 0 && WandVisDiscount.percentagePoints(player, Aspect.AIR) == 10 && ItemFocusFire.tickCost(expectedWand, player) == 7 && shockCost(expectedWand, player) == 19, "actual goggles5+robes5=10points; native FLOAT Fire7/Shock19, NOT8/20");
        report.put("actual_equipped_robes", Map.of("server_verified", true, "native_equip", "inventory QUICK_MOVE only", "robes", stats, "saved_goggles_points", 5, "robe_points", 5, "total_points", 10, "actual_armor", 7, "native_fire_centivis", 7, "native_shock_centivis", 19, "all_armor_combat_or_Repair_enchantment_claimed", false));
    }

    private void retained(ServerPlayer player) {
        for (ItemStack saved : savedStacks) {
            int before = savedStacks.stream().filter(value -> ItemStack.isSameItemSameComponents(value, saved)).mapToInt(ItemStack::getCount).sum();
            int after = player.getInventory().items.stream().filter(value -> ItemStack.isSameItemSameComponents(value, saved)).mapToInt(ItemStack::getCount).sum();
            require(before == after, "old unrelated main stack count/components unchanged: " + saved);
        }
        require(count(player, ThaumcraftContent.WAND.get()) == 3 && ItemStack.matches(stack(player, RobeShockClientSequence::reward), expectedWand), "all three actual wands preserved; only reported native charge/craft/use changed payer");
        require(ItemStack.matches(player.getOffhandItem(), apple), "same saved offhand apple never swapped/used");
        require(wearVulnerable || player.isInvulnerable() == savedEntityInvulnerability, "the saved test-only Entity invulnerability flag is restored outside actual cactus contact");
        require(ItemStack.matches(player.getItemBySlot(EquipmentSlot.HEAD), savedGoggles) || player.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && ItemStack.matches(stack(player, goggles()), savedGoggles), "same old goggles preserved worn or genuinely stored, no damage");
        if (focusChanged) require(ItemStack.matches(FocusData.get(expectedWand), earnedShock) && ItemStack.matches(stack(player, ThaumcraftFoci.FIRE_FOCUS.get()), savedFire), "both complete actual focus stacks retained after native replacement");
        else require(ItemStack.matches(FocusData.get(expectedWand), savedFire), "saved Fire retained throughout charge/robes/repair/crafting");
        for (var entry : savedJars.entrySet()) require(entry.getValue().equals(jarState((TileJar) player.level().getBlockEntity(entry.getKey()))), "ALL old jar types/filters/contents remain exact: " + entry.getKey());
        for (var entry : protectedBlocks.entrySet()) require(player.level().getBlockState(entry.getKey()) == entry.getValue(), "old TC blocks/altar/candles/machines/nodes never replaced: " + entry.getKey());
        require(player.level().getBlockEntity(center.west(9)) instanceof TileAlchemyFurnace oven && oven.getItem(0).isEmpty() && oven.getVis() == 0 && oven.getBurnTime() == 0 && oven.getItem(1).is(Items.COAL) && oven.getItem(1).getCount() == 3, "old empty oven/three saved fuel coal untouched");
        for (int dy = 1; dy <= 3; dy++) require(player.level().getBlockEntity(center.west(9).above(dy)) instanceof TileAlembic tile && tile.getAmount() == 0, "old empty alembics untouched");
        for (int x : new int[] {-3, 0, 3}) for (int z : new int[] {-3, 0, 3}) require(player.level().getBlockEntity(center.offset(x, 0, z)) instanceof TilePedestal tile && tile.isEmpty(), "nine old pedestals remain empty");
        require(knowledgeBefore.equals(KnowledgeAccess.get(player).save()), "knowledge/discovery/pools unchanged beyond explicitly declared new research fixture");
    }

    private void finalState(ServerPlayer player) {
        retained(player); inspectWearer(player); require(!player.isUsingItem() && paidTotal == 4 && shots.size() == 3 && water(player.level()) == 1, "exactly two ranged/one miss/one blocked native paid pulse and both real washes complete");
        for (Item item : List.of(Items.STRING, Items.PURPLE_WOOL, Items.RED_DYE, shard(), Items.QUARTZ, Items.POTATO, fabric())) require(count(player, item) == 0, "every finite RAW/earned fabric input accounted: " + itemId(item));
        for (int i = 0; i < 3; i++) require(ItemStack.matches(player.getItemBySlot(ROBE_SLOTS.get(i)), robes.get(i)) && count(player, robe(i)) == 0, "all three same earned robes worn, no duplicates");
        require(((TileArcaneWorkbench) player.level().getBlockEntity(workbench)).isEmpty() && player.containerMenu == player.inventoryMenu && player.containerMenu.getCarried().isEmpty(), "table/menu/cursor genuinely returned empty");
        report.put("native_net_outputs", Map.of("native_fabric_crafted", 20, "robe_fabric_spent", 19, "earned_anvil_fabric_spent", 1, "earned_robes_equipped", 3, "shock_focus_equipped", 1, "old_fire_returned_exact", true, "paid_shock_pulses", paidTotal, "native_shock_air_debit", paidTotal * 19, "final_actual_wand_snbt", snbt(expectedWand, player), "xp_input_points", xpInput));
        report.put("retained_prior_outputs", Map.of("server_verified", true, "old_main_stacks_counts_components_exact", true, "two_old_gold_wands_unchanged", true, "three_wands_total", 3, "accepted_goggles_unchanged_and_worn", true, "all12jars_173units_untouched", true, "four_phials_retained", true, "protected_old_TC_blocks", protectedBlocks.size(), "saved_knowledge_preserved_after_declared_research", true, "old_acquisition_replayed", false));
    }

    /** Passive integrated-server POST observations; no test-owned gameplay ticks. */
    private void startObservation(ServerPlayer player) {
        long started = player.serverLevel().getGameTime();
        nativeTicks = server -> {
            if (server != player.server) return;
            try {
                require(player.isAlive() && !player.isRemoved() && player.serverLevel().getGameTime() - started <= 9000, "bounded live native observer");
                if (wearVulnerable) {
                    observedWearHealth = Math.min(observedWearHealth, player.getHealth());
                    var damage = player.getLastDamageSource();
                    if (damage != null && damage.is(DamageTypes.CACTUS)) observedCactusSource = true;
                }
                if (charge != null && charge.active) charge.observe(); if (shot != null && shot.active) shot.observe();
            } catch (RuntimeException | Error error) { observationFailure = error; stopObservation(); }
        };
        serverStopped = server -> { if (server == player.server) stopObservation(); };
        nativeItemUses = (actor, hand) -> {
            if (actor.getUUID().equals(player.getUUID()) && charge != null && charge.active && chargeUseEvents.size() < 64) {
                HitResult ray = actor.pick(actor.blockInteractionRange(), 1F, true);
                chargeUseEvents.add(Map.of("side", actor.level().isClientSide ? "client" : "server", "tick", actor.level().getGameTime(),
                        "hand", hand.name(), "using_before", actor.isUsingItem(), "node_session_before", ItemWandCasting.isUsingNode(actor),
                        "ray", ray.getType() + (ray instanceof BlockHitResult hit ? ":" + hit.getBlockPos().toShortString() : ""),
                        "look", actor.getLookAngle().toString()));
            }
            return CompoundEventResult.pass();
        };
        report.put("native_charge_item_use_events", chargeUseEvents);
        InteractionEvent.RIGHT_CLICK_ITEM.register(nativeItemUses);
        TickEvent.SERVER_POST.register(nativeTicks); LifecycleEvent.SERVER_STOPPED.register(serverStopped); ClientTickEvent.CLIENT_PRE.register(useGuard);
    }
    private void stopObservation() {
        if (wearVulnerable && wearSubject != null) wearSubject.setInvulnerable(savedEntityInvulnerability);
        wearVulnerable = false; wearSubject = null;
        if (nativeTicks != null) { TickEvent.SERVER_POST.unregister(nativeTicks); nativeTicks = null; }
        if (serverStopped != null) { LifecycleEvent.SERVER_STOPPED.unregister(serverStopped); serverStopped = null; }
        if (nativeItemUses != null) { InteractionEvent.RIGHT_CLICK_ITEM.unregister(nativeItemUses); nativeItemUses = null; }
        ClientTickEvent.CLIENT_PRE.unregister(useGuard); if (charge != null) charge.active = false; if (shot != null) shot.active = false;
    }

    private static final class ChargeProbe {
        final ServerPlayer player; final BlockPos at; final ItemStack wand, original; final WandData before;
        final Map<String, Integer> sourceBefore, regenerated = new LinkedHashMap<>(); final long started;
        WandData previous; Map<String, Integer> previousSource; volatile int stopped; boolean active = true, used; int transfers;
        ChargeProbe(ServerPlayer player, BlockPos at) {
            this.player = player; this.at = at; wand = player.getMainHandItem(); original = wand.copy(); before = ItemWandCasting.data(wand); previous = before;
            sourceBefore = aspects(node(player.level(), at).getAspects()); previousSource = sourceBefore; started = player.serverLevel().getGameTime();
            require(reward(wand) && !player.isUsingItem() && sourceBefore.size() == 6 && sourceBefore.values().stream().allMatch(value -> value == 80), "actual finite six-primal80 baseline");
        }
        void observe() {
            require(player.getMainHandItem() == wand && player.serverLevel().getGameTime() - started <= 2000 && sameExcept(wand, original, ThaumcraftContent.WAND_DATA.get()), "same real wand/components throughout finite native node use");
            var now = ItemWandCasting.data(wand); var source = aspects(node(player.level(), at).getAspects()); int regenTotal = 0;
            for (String tag : WandData.PRIMALS) {
                int delta = now.amount(tag) - previous.amount(tag); require(delta >= 0 && delta % 100 == 0,
                        "only native whole-vis charging, no focused Fire expense/top-up; tag=" + tag + "; delta=" + delta);
                int regen = source.getOrDefault(tag, 0) - previousSource.getOrDefault(tag, 0) + delta / 100;
                require(regen >= 0 && regen <= 1, "finite source decrement accounted with ordinary regeneration only"); if (regen > 0) regenerated.merge(tag, regen, Integer::sum); regenTotal += regen; transfers += delta / 100;
            }
            require(regenTotal <= 1, "at most one normal node regenerated unit per tick");
            if (player.isUsingItem()) { require(player.getUseItem() == wand && ItemWandCasting.isUsingNode(player), "native held-use owns exact server-side node session, including while client release is in transit"); used = true; }
            stopped = used && !player.isUsingItem() ? stopped + 1 : 0; previous = now; previousSource = source;
        }
        Map<String, Object> finish() {
            require(used && stopped >= 2 && transfers > 0 && WandData.PRIMALS.stream().allMatch(tag -> ItemWandCasting.getVis(wand, tag) >= 3200), "real finite charge/release funded batch with bounded surplus; no direct vis writes");
            require(aspects(node(player.level(), at).getAspectsBase()).equals(sourceBefore) && !aspects(node(player.level(), at).getAspects()).equals(sourceBefore), "normal base retained/current genuinely depleted"); active = false;
            return Map.of("server_verified", true, "source", at.toShortString(), "source_before", sourceBefore, "source_after", aspects(node(player.level(), at).getAspects()), "native_regeneration", Map.copyOf(regenerated), "actual_vis_units_transferred", transfers, "wand_before", before.vis(), "wand_after", ItemWandCasting.data(wand).vis(), "native_ticks", player.serverLevel().getGameTime() - started, "direct_topups", 0);
        }
    }

    private record ShockFrame(int paid, int stopped, float health) {}
    private static final class ShockProbe {
        final ServerPlayer player; final Cow cow; final ShotKind kind; final ItemStack wand, original; final WandData before; final float healthBefore;
        final List<Map<String, Object>> payments = new ArrayList<>(); final double rangeBefore; final long started;
        volatile ShockFrame frame; boolean active = true, used; int paid, stopped, previous; long previousPayment, previousDispatch;
        ShockProbe(ServerPlayer player, Cow cow, ShotKind kind, long lastDispatch) {
            this.player = player; this.cow = cow; this.kind = kind; wand = player.getMainHandItem(); original = wand.copy(); before = ItemWandCasting.data(wand); previous = before.amount("aer"); healthBefore = cow.getHealth(); started = player.serverLevel().getGameTime();
            previousDispatch = lastDispatch;
            rangeBefore = player.getEyePosition().distanceTo(cow.getBoundingBox().getCenter()); frame = new ShockFrame(0, 0, healthBefore);
            require(reward(wand) && FocusData.get(wand).is(shock()) && !player.isUsingItem() && shockCost(wand, player) == 19 && WandVisDiscount.percentagePoints(player, Aspect.AIR) == 10 && cow.isAlive() && !cow.isOnFire(), "genuine Shock+full earned gear finite19 AIR baseline");
            require(rangeBefore >= 12 && rangeBefore <= 16, "actual long-range living target12..16blocks, NOT old10block visual fallback");
            var hit = player.level().clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(player.getLookAngle().scale(20)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            require(kind == ShotKind.BLOCKED ? hit.getType() == HitResult.Type.BLOCK && player.level().getBlockState(hit.getBlockPos()).is(Blocks.STONE_BRICKS) : hit.getType() == HitResult.Type.MISS, "real collider LOS matches intended ranged/miss/blocked fixture");
        }
        void observe() {
            require(player.getMainHandItem() == wand && sameExcept(wand, original, ThaumcraftContent.WAND_DATA.get()) && player.serverLevel().getGameTime() - started <= 160, "same actual focused stack, bounded native use");
            var now = ItemWandCasting.data(wand); int delta = previous - now.amount("aer"); require(delta == 0 || delta == 19, "each native server tick pays zero or exactly19AIR");
            for (String tag : WandData.PRIMALS) if (!tag.equals("aer")) require(now.amount(tag) == before.amount(tag), "Shock leaves all five other primals unchanged");
            if (player.isUsingItem()) { require(player.getUseItem() == wand && player.getUsedItemHand() == InteractionHand.MAIN_HAND, "actual native main-hand use state"); used = true; }
            if (delta > 0) {
                require(player.isUsingItem() && shockCost(wand, player) == 19, "payment observed ONLY within real native RMB session");
                long clock = System.currentTimeMillis(), interval = previousPayment == 0 ? -1 : clock - previousPayment;
                long dispatch = nativeDispatchMillis(player), dispatchInterval = previousDispatch == 0 ? -1 : dispatch - previousDispatch;
                require(dispatchInterval < 0 || dispatchInterval >= 250, "ALL real native paid dispatches, including between sessions, spaced at least250ms");
                long cooldown = WandFocusActions.cooldownMillis(player);
                require(cooldown > 0 && cooldown <= 250, "actual native dispatcher installed its real250ms cooldown before paid pulse");
                if (kind == ShotKind.RANGED && paid == 0) require(healthBefore - cow.getHealth() == 4F, "FIRST long-range pulse applies exactly four ORDINARY damage");
                if (kind != ShotKind.RANGED) require(cow.getHealth() == healthBefore, "paid miss/opaque wall must not damage distant target");
                ++paid; require(paid <= (kind == ShotKind.RANGED ? 2 : 1), "two ranged pulses/ONE paid miss/ONE paid wall block, never depletion treadmill");
                payments.add(Map.of("server_tick", player.serverLevel().getGameTime(), "observed_POST_wall_clock_ms", clock, "observed_POST_interval_ms", interval, "native_dispatch_ms", dispatch, "native_dispatch_interval_ms", dispatchInterval, "native_dispatch_cooldown_remaining_ms", cooldown, "air_before", previous, "air_after", now.amount("aer"), "native_remaining_use_ticks", player.getUseItemRemainingTicks(), "target_health", cow.getHealth())); previousPayment = clock; previousDispatch = dispatch;
            }
            require(cow.isAlive() && !cow.isOnFire() && (kind == ShotKind.RANGED || cow.getHealth() == healthBefore), "basic Shock has no fire/extra target damage; ordinary repeated-hit iframes are not overridden");
            previous = now.amount("aer"); stopped = used && paid > 0 && !player.isUsingItem() ? stopped + 1 : 0; frame = new ShockFrame(paid, stopped, cow.getHealth());
        }
        Map<String, Object> finish() {
            require(used && stopped >= 2 && !player.isUsingItem() && paid == (kind == ShotKind.RANGED ? 2 : 1) && before.amount("aer") - ItemWandCasting.getVis(wand, "aer") == paid * 19, "native finite payment/release verified, no forced debit/refund"); active = false;
            return Map.of("server_verified", true, "case", kind.name(), "range_to_living_target", rangeBefore, "per_pulse_air", 19, "paid_pulses", paid, "actual_payments", List.copyOf(payments), "wand_before", before.vis(), "wand_after", ItemWandCasting.data(wand).vis(), "health_before", healthBefore, "health_after", cow.getHealth());
        }
    }

    /** Recover the REAL dispatch timestamp through B's read-only cooldown API.
     * Same-millisecond bracketing avoids confusing POST sampling skew with a
     * sub250ms pulse. No clock setter, sleep, cooldown write or focus call. */
    private static long nativeDispatchMillis(Player player) {
        for (int i = 0; i < 8; i++) {
            long before = System.currentTimeMillis(), remaining = WandFocusActions.cooldownMillis(player), after = System.currentTimeMillis();
            if (before == after) { require(remaining > 0 && remaining <= 250, "real paid pulse has a live native250ms deadline"); return before + remaining - 250; }
        }
        throw new IllegalStateException("Could not obtain bounded same-millisecond native cooldown observation");
    }
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
                int amount = Math.min(actual.getCount(), group.getValue().size() - placed), source = arcaneSlot(inventory); clicks.add(new int[] {source, 0});
                for (int n = 0; n < amount; n++) clicks.add(new int[] {group.getValue().get(placed++), 1});
                if (actual.getCount() > amount) clicks.add(new int[] {source, 0});
            }
            require(placed == group.getValue().size(), "finite normal raw stacks supply exact native grid: " + itemId(group.getKey()));
        }
        return clicks;
    }
    private void verifyGrid(ContainerArcaneWorkbench menu, int amount) {
        for (int i = 0; i < 9; i++) { ItemStack actual = menu.getSlot(i + 2).getItem(); Item expected = craft.grid()[i]; require(expected == null || amount == 0 ? actual.isEmpty() : actual.is(expected) && actual.getCount() == amount, "native grid exactly matches finite recipe/consumption: " + craft.recipe() + "/" + i); }
    }
    private static void verifyDebit(ItemStack before, ItemStack after, Map<String, Integer> price, int repetitions) {
        require(sameExcept(before, after, ThaumcraftContent.WAND_DATA.get()), "native crafting preserves every non-vis wand component/focus");
        var first = ItemWandCasting.data(before); var last = ItemWandCasting.data(after); require(first.rod().equals(last.rod()) && first.cap().equals(last.cap()), "same actual wand materials");
        for (String tag : WandData.PRIMALS) require(first.amount(tag) - last.amount(tag) == price.getOrDefault(tag, 0) * repetitions, "native extraction debits exact actor-priced repeated cost: " + tag);
    }

    private void patch(ServerLevel level) {
        BlockPos min = workbench.offset(-4, -2, -12), max = workbench.offset(14, 5, 9);
        for (int x = min.getX() >> 4; x <= max.getX() >> 4; x++) for (int z = min.getZ() >> 4; z <= max.getZ() >> 4; z++) level.getChunk(x, z);
        var bounds = new AABB(Vec3.atLowerCornerOf(min), Vec3.atLowerCornerOf(max.offset(1, 1, 1)));
        require(level.getEntitiesOfClass(ItemEntity.class, bounds).isEmpty(), "new fixture must not erase old physical outputs");
        require(level.getEntitiesOfClass(Cow.class, bounds).isEmpty(), "new target patch cannot overwrite an old living fixture");
        for (BlockPos at : BlockPos.betweenClosed(min, max)) require(level.getBlockEntity(at) == null && !BuiltInRegistries.BLOCK.getKey(level.getBlockState(at).getBlock()).getNamespace().equals("thaumcraft"), "refuse any existing BE/node/machine/TC block: " + at);
        for (BlockPos at : BlockPos.betweenClosed(min, max)) level.setBlockAndUpdate(at, at.getY() < workbench.getY() ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
    }
    private void addWall(ServerLevel level) {
        for (int dx = -1; dx <= 1; dx++) for (int dy = 0; dy <= 2; dy++) require(level.isEmptyBlock(wall.offset(dx, dy, 0)) && level.getBlockEntity(wall.offset(dx, dy, 0)) == null, "later nine-block wall only occupies fresh empty fixture cells");
        for (int dx = -1; dx <= 1; dx++) for (int dy = 0; dy <= 2; dy++) level.setBlockAndUpdate(wall.offset(dx, dy, 0), Blocks.STONE_BRICKS.defaultBlockState());
        inputs.add(Map.of("fixture", "nine-block opaque wall INPUT only AFTER unblocked ranged hit and paid miss", "base", wall.toShortString()));
    }
    private void supply(ServerPlayer player, Item item, int count) {
        int slot = emptyMain(player); require(count > 0 && count <= item.getDefaultInstance().getMaxStackSize(), "normal finite raw INPUT stack, no oversized80-string stack");
        player.getInventory().setItem(slot, new ItemStack(item, count)); inputs.add(Map.of("item", itemId(item), "count", count, "genuinely_empty_main_slot", slot, "purpose", "RAW INPUT, never crafted output"));
    }
    private boolean showRecipe(Minecraft client, String recipe) throws ReflectiveOperationException {
        int page = (int) BookClientSmoke.field(client.screen, "page"); var pages = (List<?>) BookClientSmoke.field(client.screen, "pages");
        for (int i = page; i < Math.min(page + 2, pages.size()); i++) if (BookClientSmoke.field(pages.get(i), "recipe") instanceof ThaumcraftRecipeDisplay display && display.recipe().id().equals(id(recipe))) {
            require(display.recipe() == client.level.getRecipeManager().byKey(id(recipe)).orElseThrow(), "book uses actual native synced recipe holder"); return true;
        }
        require(++turns <= 16, "bounded native recipe book navigation"); var button = client.screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(value -> value.active && value.getMessage().getString().equals("→")).findFirst().orElseThrow();
        client.screen.mouseClicked(button.getX() + 5, button.getY() + 5, 0); client.screen.mouseReleased(button.getX() + 5, button.getY() + 5, 0); ticks = 0; return false;
    }
    private Map<String, Object> presentation(Minecraft client) {
        var result = new LinkedHashMap<String, Object>();
        for (EquipmentSlot slot : ROBE_SLOTS) {
            ItemStack actual = client.player.getItemBySlot(slot); var armor = (ArmorItem) actual.getItem(); var textures = armor.getMaterial().value().layers().stream().map(layer -> layer.texture(slot == EquipmentSlot.LEGS)).toList();
            require(textures.size() == 2 && textures.stream().allMatch(texture -> client.getResourceManager().getResource(texture).isPresent()), "both real tinted/untinted worn layers resolve for " + slot);
            var model = client.getItemRenderer().getModel(actual, client.level, client.player, 0); require(model != client.getModelManager().getMissingModel(), "actual earned robe item model resolves");
            var sprites = new LinkedHashSet<String>(); for (var quad : model.getQuads(null, null, RandomSource.create(0))) sprites.add(quad.getSprite().contents().name().toString());
            require(sprites.size() >= 2 && sprites.stream().noneMatch(value -> value.contains("missingno")), "native robe item has base/overlay sprites, no missing model");
            result.put(slot.getName(), Map.of("actual_worn_textures", textures.stream().map(ResourceLocation::toString).toList(), "actual_item_sprites", List.copyOf(sprites), "actual_rgb", Integer.toHexString(ItemRobeArmor.color(actual)), "tooltip", actual.getTooltipLines(Item.TooltipContext.of(client.level), client.player, TooltipFlag.NORMAL).stream().map(Component::getString).toList()));
        }
        result.put("visual_inspection", "pending exact85; native InventoryScreen presents SAME earned wearer/robes/goggles, resource probes are not seen-texture evidence"); return result;
    }

    private record BoltView(Vec3 origin, Vec3 target, int age, int travel, int impactGlints, boolean reached) {
        Map<String, Object> evidence() { return Map.of("origin", origin.toString(), "target", target.toString(), "age", age, "native_travel", travel, "actual_impact_glints", impactGlints, "native_beam_reached_endpoint", reached); }
    }
    /** Narrow read-only view of B's actual paid-S2C render objects, like book-page inspection.
     * No receiver, render/tick callback, particle constructor or payload is invoked.
     * B currently exports count only; root may reconcile this with its final public view. */
    private static List<BoltView> shockEffects(Minecraft client) throws ReflectiveOperationException {
        var field = ThaumcraftShockClient.class.getDeclaredField("BOLTS"); field.setAccessible(true); var actual = (List<?>) field.get(null);
        require(actual.size() == ThaumcraftShockClient.activeBoltCount(), "same actual production bolt collection/count");
        var result = new ArrayList<BoltView>(); float partial = Math.max(0F, Math.min(1F, client.getTimer().getGameTimeDeltaPartialTick(false)));
        for (Object bolt : actual) {
            Vec3 origin = (Vec3) BookClientSmoke.field(bolt, "origin"), end = (Vec3) BookClientSmoke.field(bolt, "end");
            if (origin.distanceToSqr(client.player.getEyePosition()) > 4) continue;
            int age = (int) BookClientSmoke.field(bolt, "age"), travel = (int) BookClientSmoke.field(bolt, "travel");
            int glints = ((List<?>) BookClientSmoke.field(bolt, "glints")).size();
            result.add(new BoltView(origin, origin.add(end), age, travel, glints, (age + partial) * 8 >= travel));
        }
        return List.copyOf(result);
    }
    private static int shockCost(ItemStack wand, Player actor) { return ItemFocusShock.pulseCost(wand, actor); }
    private void aimShot(Minecraft client) {
        Vec3 point = kind() == ShotKind.MISS ? client.player.getEyePosition().add(10, 8, 0) : client.level.getEntity(targetId).getBoundingBox().getCenter(); lookAt(client, point);
    }
    private ShotKind kind() { return ShotKind.values()[shotIndex]; }
    private boolean hand(Minecraft client, Predicate<ItemStack> wanted) throws ReflectiveOperationException {
        require(client.player.containerMenu == client.player.inventoryMenu, "native hand SWAP only outside menus");
        if (!wanted.test(client.player.getInventory().getItem(8))) { click(client, inventorySlot(itemSlot(client.player, wanted)), 8, ClickType.SWAP); handChangedAt = client.player.tickCount; return false; }
        if (client.player.tickCount - handChangedAt < 6) return false; select(client, 8); return true;
    }
    private boolean emptyHand(Minecraft client) throws ReflectiveOperationException {
        require(client.player.containerMenu == client.player.inventoryMenu, "empty hand only through real inventory SWAP");
        if (!client.player.getInventory().getItem(8).isEmpty()) { click(client, inventorySlot(emptyMain(client.player)), 8, ClickType.SWAP); handChangedAt = client.player.tickCount; return false; }
        if (client.player.tickCount - handChangedAt < 6) return false; select(client, 8); return true;
    }
    private void guardUse(Minecraft client) {
        // Capture I/O suspends the sequence, NOT this safety input observer.
        // Hold the declared camera BEFORE vanilla sends rotation/use packets,
        // not only in the sequence's POST callback. Losing the node normally
        // stops use, and a still-held RMB may then legitimately start Fire.
        if (charge != null && charge.active && client.player != null && client.screen == null) lookAt(client, Vec3.atCenterOf(chargingNode));
        if (guardingUse && !releaseSent && client.player != null && (++castTicks >= 30 || shot.frame.paid() >= (shot.kind == ShotKind.RANGED ? 2 : 1))) releaseUse(client);
    }
    private void releaseUse(Minecraft client) { if (!releaseSent) { client.options.keyUse.setDown(false); client.gameMode.releaseUsingItem(client.player); releaseSent = true; } }
    private static void releaseFocusKey(Minecraft client) { ThaumcraftFocusClient.CHANGE_FOCUS.setDown(false); client.options.keySwapOffhand.setDown(false); }
    private void releaseInputs(Minecraft client) {
        guardingUse = false; ClientTickEvent.CLIENT_PRE.unregister(useGuard); releaseFocusKey(client); client.options.keyUse.setDown(false); client.options.keyShift.setDown(false); client.options.keyAttack.setDown(false); client.options.keyUp.setDown(false);
        if (client.gameMode != null && client.player != null && client.player.isUsingItem()) client.gameMode.releaseUsingItem(client.player);
        if (previousCamera != null) client.options.setCameraType(previousCamera);
    }
    /** Detached assertion copies ONLY; neither copy is ever installed anywhere. */
    private static boolean sameExcept(ItemStack first, ItemStack second, DataComponentType<?>... excluded) {
        ItemStack a = first.copy(), b = second.copy(); for (DataComponentType<?> type : excluded) { a.remove(type); b.remove(type); } return ItemStack.matches(a, b);
    }
    private static void stand(ServerPlayer player, double x, double y, double z) {
        var body = new AABB(x - .3, y + .001, z - .3, x + .3, y + 1.8, z + .3); BlockPos floor = BlockPos.containing(x, y - .01, z);
        require(player.level().noCollision(player, body) && !player.level().getBlockState(floor).getCollisionShape(player.level(), floor).isEmpty(), "bounded camera/contact fixture collision-free above solid floor"); player.teleportTo(x, y, z);
    }
    private int water(Level level) { var state = level.getBlockState(cauldron); return state.is(Blocks.WATER_CAULDRON) ? state.getValue(LayeredCauldronBlock.LEVEL) : 0; }
    private static boolean reward(ItemStack value) { return value.is(ThaumcraftContent.WAND.get()) && ItemWandCasting.data(value).rod().equals("silverwood") && ItemWandCasting.data(value).cap().equals("thaumium"); }
    private static Map<String, Integer> aspects(AspectList list) { var result = new LinkedHashMap<String, Integer>(); for (Aspect aspect : list.getAspects()) if (list.getAmount(aspect) > 0) result.put(aspect.getTag(), list.getAmount(aspect)); return result; }
    private static Map<String, Object> jarState(TileJar jar) { return Map.of("aspect", jar.getAspect() == null ? "" : jar.getAspect().getTag(), "amount", jar.getAmount(), "filter", jar.getFilter() == null ? "" : jar.getFilter().getTag()); }
    private static int emptyMain(Player player) { for (int i = 9; i < 36; i++) if (player.getInventory().getItem(i).isEmpty()) return i; throw new IllegalStateException("No genuinely empty main slot"); }
    private static int itemSlot(Player player, Predicate<ItemStack> wanted) { for (int i = 0; i < 36; i++) if (wanted.test(player.getInventory().getItem(i))) return i; throw new IllegalStateException("Missing actual finite robe/Shock input"); }
    private static ItemStack stack(Player player, Item item) { return stack(player, value -> value.is(item)); }
    private static ItemStack stack(Player player, Predicate<ItemStack> wanted) { return player.getInventory().getItem(itemSlot(player, wanted)); }
    private static int count(Player player, Item item) { return player.getInventory().items.stream().filter(value -> value.is(item)).mapToInt(ItemStack::getCount).sum(); }
    private static int inventorySlot(int slot) { return slot < 9 ? slot + 36 : slot; }
    private static int arcaneSlot(int slot) { return slot < 9 ? slot + 38 : slot + 2; }
    private static int anvilSlot(int slot) { return slot < 9 ? slot + 30 : slot - 6; }
    private static Item robe(int index) { return index == 0 ? chest() : index == 1 ? legs() : boots(); }
    private static Item chest() { return ThaumcraftWearables.ROBE_CHESTPLATE.get(); }
    private static Item legs() { return ThaumcraftWearables.ROBE_LEGGINGS.get(); }
    private static Item boots() { return ThaumcraftWearables.ROBE_BOOTS.get(); }
    private static Item goggles() { return ThaumcraftWearables.GOGGLES.get(); }
    private static Item fabric() { return ThaumcraftContent.MATERIALS.get("enchanted_fabric").get(); }
    private static Item shock() { return ThaumcraftFoci.SHOCK_FOCUS.get(); }
    private static Item shard() { return ThaumcraftContent.SHARDS.get("aer").get(); }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }
    private static String itemId(Item item) { return BuiltInRegistries.ITEM.getKey(item).toString(); }
    private static String snbt(ItemStack value, ServerPlayer player) { return value.save(player.registryAccess()).toString(); }
    private static TileNode node(Level level, BlockPos at) { return (TileNode) level.getBlockEntity(at); }
    private static ContainerArcaneWorkbench arcane(Minecraft client) { return (ContainerArcaneWorkbench) client.player.containerMenu; }
    private static AnvilMenu repair(Minecraft client) { return (AnvilMenu) client.player.containerMenu; }
    private static void click(Minecraft client, int slot, int button, ClickType type) { client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId, slot, button, type, client.player); }
    private static void select(Minecraft client, int slot) throws ReflectiveOperationException { client.player.getInventory().selected = slot; var ensure = client.gameMode.getClass().getDeclaredMethod("ensureHasSentCarriedItem"); ensure.setAccessible(true); ensure.invoke(client.gameMode); }
    private static void lookAt(Minecraft client, Vec3 point) { Vec3 d = point.subtract(client.player.getEyePosition()); client.player.setYRot((float) Math.toDegrees(Math.atan2(-d.x, d.z))); client.player.yRotO = client.player.getYRot(); client.player.setXRot((float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)))); client.player.xRotO = client.player.getXRot(); }
    private static void use(Minecraft client, BlockPos at, Direction face, Vec3 point) { require(!client.gameMode.hasInfiniteItems() && client.player.canInteractWithBlock(at, 0), "native survival block reach"); lookAt(client, point); require(client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, new BlockHitResult(point, face, at, false)).consumesAction(), "actual native block use accepts " + at); }
    private static ServerPlayer serverPlayer(Minecraft client) { return client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID()); }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private void onServer(Minecraft client, Runnable action) { require(serverAction == null || serverAction.isDone(), "fixture/read-only server actions cannot overlap"); serverAction = CompletableFuture.runAsync(action, client.getSingleplayerServer()); }
    private boolean serverDone() { if (serverAction == null) return true; if (!serverAction.isDone()) return false; serverAction.join(); return true; }
    private void next(Step next) { step = next; ticks = 0; }
}
