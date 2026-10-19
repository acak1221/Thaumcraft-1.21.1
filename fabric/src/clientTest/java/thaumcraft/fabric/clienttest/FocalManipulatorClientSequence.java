package thaumcraft.fabric.clienttest;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.visnet.TileVisNode;
import thaumcraft.api.visnet.VisNetHandler;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.client.ThaumcraftFocusClient;
import thaumcraft.client.gui.GuiArcaneWorkbench;
import thaumcraft.client.gui.GuiFocalManipulator;
import thaumcraft.client.gui.GuiFocusSelect;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.config.ThaumcraftFocalManipulation;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftMasonry;
import thaumcraft.common.config.ThaumcraftNodeDevices;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.container.ContainerFocalManipulator;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.ItemFocusExcavation;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.FocalManipulatorAckPayload;
import thaumcraft.common.lib.network.FocalManipulatorNetworking;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.tiles.TileAlchemyFurnace;
import thaumcraft.common.tiles.TileArcaneWorkbench;
import thaumcraft.common.tiles.TileFocalManipulator;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.tiles.TileNodeConverter;
import thaumcraft.common.tiles.TileNodeEnergized;
import thaumcraft.common.tiles.TileNodeStabilizer;
import thaumcraft.common.tiles.TileTube;
import thaumcraft.common.tiles.TileVisRelay;

/** STAGED ONLY: root promotes after review. Native input and native server ticks
 * produce every gold ingot/XP/device/upgrade/drop. Initial RAW/empty-block/camera
 * fixtures are separately declared. No production backend, resource/counter/RNG
 * setter, manually ticked entity, moved orb/item, or accepted-save write. */
public final class FocalManipulatorClientSequence implements AutoCloseable {
    private static volatile FocalManipulatorClientSequence activeInputOwner;
    private static volatile FocalManipulatorClientSequence activeServerOwner;
    private enum Step {
        HANDOFF,SCENE_READY,INPUT_CHECK,INPUT_PRESSED,INPUT_RELEASED,SETUP,FURNACE_MOVE,FURNACE_OPEN,FURNACE_FILL,FURNACE_FILLED,
        CHARGE_MOVE,CHARGE_HAND,CHARGE_READY,CHARGE_LIVE,CHARGE_RELEASE,
        MINE_MOVE,MINE_HAND,MINE_AIM,MINING,MINE_PICKUP,MINE_PICKED,
        LOWER_MOVE,LOWER_HAND,LOWER_PLACE,LOWER_ACK,UPPER_MOVE,UPPER_HAND,UPPER_PLACE,UPPER_ACK,
        LEVER_HAND,LEVER_PLACE,LEVER_ACK,POWER_HAND,POWER_USE,POWER_ACK,
        SMELT_WAIT,TAKE_MOVE,TAKE_OPEN,TAKE_RESULT,TAKE_ACK,XP_CONTACT,XP_READY,SOURCE_WAIT,
        CRAFT_MOVE,CRAFT_HAND,CRAFT_OPEN,CRAFT_FUND,CRAFT_FUNDED,CRAFT_GRID,CRAFT_PREVIEW,CRAFT_CAPTURE,
        CRAFT_TAKE,CRAFT_STORE,CRAFT_RETURN,CRAFT_DONE,
        MACHINE_MOVE,MACHINE_HAND,MACHINE_PLACE,MACHINE_ACK,MACHINE_VIEW,
        FOCAL_OPEN,FOCAL_INSERT,FOCAL_INSERTED,LOCAL_SELECT,LOCAL_DESELECT,LOCAL_RESELECT,LOCAL_VIEW,
        START,START_ACK,STALL,RELAY_MOVE,RELAY_HAND,RELAY_PLACE,RELAY_ACK,NETWORK_VIEW,
        ACTIVE_MOVE,ACTIVE_OPEN,ACTIVE_VIEW,COMPLETE,COMPLETED_VIEW,FOCUS_REMOVE,FOCUS_REMOVED,
        EQUIP_MOVE,EQUIP_HAND,EQUIP_PRESS,EQUIP_RADIAL,EQUIP_ACK,
        CAST_MOVE,CAST_READY,CAST_START,CAST_LIVE,CAST_RELEASE,CAST_PICKUP,CAST_PICKED,CAST_VIEW,
        UNEQUIP,UNEQUIP_ACK,RANK2_MOVE,RANK2_OPEN,RANK2_INSERT,RANK2_VIEW,RANK2_REFUSE,
        RANK2_CAPTURE,RETURN_FOCUS,RESTORE,FINAL_CHECK,FINAL_ACK,DONE
    }
    private record Click(int slot,int button,ClickType type) {
        Click(int slot,int button) { this(slot,button,ClickType.PICKUP); }
    }
    private record Device(BlockPos from,BlockPos to,Item item,Direction face,BlockPos support) {}
    private static final BlockPos OLD_NODE=new BlockPos(1551,218,377),SOURCE=new BlockPos(1623,218,374),
            BENCH=new BlockPos(1601,216,374),MACHINE=new BlockPos(1609,219,374),RELAY=new BlockPos(1616,218,374),
            SUPPORT=new BlockPos(1623,220,374),LEVER=new BlockPos(1623,220,375),STONE=new BlockPos(1605,216,381);
    private static final List<BlockPos> FURNACES=List.of(new BlockPos(1632,216,371),new BlockPos(1634,216,371),
            new BlockPos(1636,216,371),new BlockPos(1638,216,371),new BlockPos(1632,216,375),
            new BlockPos(1634,216,375),new BlockPos(1636,216,375),new BlockPos(1638,216,375));
    private static final List<String> CAPTURES=List.of(
            "164-native-primal-charm-produced-gold-paid-arcane-preview-en.png",
            "165-native-focal-manipulator-same-earned-charm-paid-preview-en.png",
            "166-earned-focal-manipulator-empty-original-table-model-en.png",
            "167-native-potency-select-deselect-local-preview-tooltip-en.png",
            "168-native-eight-level-potency-payment-no-relay-debt-stall-en.png",
            "169-native-earned-relay-real-source-powered-focal-table-en.png",
            "170-native-six-primal-remaining-debt-focal-progress-en.png",
            "171-native-one-rank-potency-focus-completed-history-en.png",
            "172-native-paid-potency-stone-harvest-same-UUID-pickup-en.png",
            "173-native-rank-two-insufficient-levels-final-retained-stock-en.png");
    private static final Map<String,Integer> INITIAL_VIS=Map.of("aer",4600,"aqua",1900,"ignis",2400,"ordo",1200,"perditio",0,"terra",1200),
            POTENCY=Map.of("aer",200,"terra",600,"ignis",400,"aqua",600,"ordo",400,"perditio",200),
            BASE=Map.of("aer",80,"aqua",80,"ignis",80,"ordo",80,"perditio",80,"terra",80);
    private static final List<EquipmentSlot> ARMOR=List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET);
    private static final AABB SCENE=new AABB(1597,213,365,1643,226,390),OLD_AREA=new AABB(1547,214,373,1557,224,383);
    private static final int HAND=3;
    public final Map<String,Object> report=new ConcurrentHashMap<>();
    private final Minecraft client;
    private final Consumer<String> capture;
    private final Path tailFile;
    private final String runId;
    private final long wallStart=System.nanoTime();
    private final List<Map<String,Object>> posts=new CopyOnWriteArrayList<>(),inputs=new CopyOnWriteArrayList<>(),
            actions=new CopyOnWriteArrayList<>(),physical=new CopyOnWriteArrayList<>(),acks=new CopyOnWriteArrayList<>(),
            epochs=new CopyOnWriteArrayList<>(),captures=new CopyOnWriteArrayList<>(),oldNodeRows=new CopyOnWriteArrayList<>();
    private final Map<BlockPos,BlockState> fixtureBlocks=new LinkedHashMap<>(),oldBlocks=new LinkedHashMap<>();
    private final Map<BlockPos,CompoundTag> oldBEs=new LinkedHashMap<>();
    private final Map<Integer,ItemStack> savedInventory=new LinkedHashMap<>();
    private final List<ItemStack> savedArmor=new ArrayList<>();
    private final Map<Item,Integer> declared=new LinkedHashMap<>();
    private final Map<UUID,Map<String,Object>> savedLiving=new LinkedHashMap<>();
    private final Map<UUID,Entity> inheritedEntities=new LinkedHashMap<>();
    private final Set<UUID> ambientEligible=ConcurrentHashMap.newKeySet(),provenAmbientRemovals=ConcurrentHashMap.newKeySet();
    private final List<Map<String,Object>> ambientRemovalRows=new CopyOnWriteArrayList<>();
    private final List<Map<String,Object>> laterLivingRows=new CopyOnWriteArrayList<>();
    private final Map<UUID,ItemStack> drops=new ConcurrentHashMap<>();
    private final Set<UUID> contacted=ConcurrentHashMap.newKeySet();
    private final Map<Integer,UUID> deviceDrops=new ConcurrentHashMap<>();
    private final TickEvent.Server postHook=this::serverPost;
    private final EntityEvent.Add addHook=this::entityAdded;
    private final LifecycleEvent.ServerState stopHook=this::serverStopped;
    private final ClientTickEvent.Client preHook=this::clientPre;
    private final BlockEvent.Break breakHook=(world,pos,state,player,experience)->beforeBreak(world,pos,state,player);
    private final List<NativeEventTap> protectionTaps=new ArrayList<>();
    private CompletableFuture<Void> task;
    private volatile Step step=Step.HANDOFF;
    private volatile Throwable failure;
    private volatile View view;
    private volatile Charge charge;
    private volatile boolean inert,stopped,charging,casting,releaseSent,mining,done,cleanupRequested;
    private volatile boolean sceneReady;
    private volatile String clientInputSnapshot="UNSEEN";
    private volatile int miningDevice=-1;
    private volatile UUID cobbleId;
    private volatile long firstPost=-1,lastPost=-1;
    private volatile int breakCalls,beforeCalls,afterCalls;
    private volatile int castPayments;
    private volatile boolean sawZero,sawHalf;
    private volatile long paidStartTick=-1;
    private ServerPlayer owner;
    private Ledger ledger;
    private FocalXpPickupObserver xp;
    private CameraType camera;
    private ItemStack savedPayer,savedFocus,savedPick,savedOffhand,chargedPayer,recipeBefore,earnedCharm,earnedMachine,upgradedFocus;
    private CompoundTag knowledge;
    private Consumer<FocalManipulatorAckPayload> previousAck,ackTap;
    private List<Click> clicks=List.of();
    private int clickIndex,age,furnaceIndex,takeIndex,deviceIndex,craftIndex,destination,handChanged=-100,
            captureCount,restoreIndex;
    private volatile int actualStarts;
    private int beforeLevel,beforeTotal,requestedContainer=-1;
    private float beforeProgress;
    private Object expectedPending;
    private long requestedRevision=-1,cosmeticRevision=-1;
    private UUID requestedSession;
    private int useAimTick;
    private BlockPos useAim;
    private String focusBinding,swapBinding;
    private String healthyId;
    private boolean hooks,ackInstalled,storyFinal;
    private boolean savedInvulnerable;
    private final FocalNativeShift nativeShift=new FocalNativeShift();

    public FocalManipulatorClientSequence(Minecraft client,Consumer<String> capture,Path evidenceDirectory,String runId) {
        require(client!=null && capture!=null && runId.matches("client-smoke-[a-f0-9-]{36}"),"Root-owned client run required");
        this.client=client; this.capture=capture; this.runId=runId;
        tailFile=evidenceDirectory.toAbsolutePath().normalize().resolve("focal-tail-"+runId+".json");
        require(Files.isDirectory(tailFile.getParent()) && !Files.exists(tailFile),"Existing root evidence directory / new tail filename");
        report.put("scope","Fresh root-owned focal-smoke-UUID COPY of accepted stopped M09b e0a10f37; partial M08 only; no accepted save opened by this harness author");
        report.put("limits",row("SERVER_POST_ticks",9000,"wall_seconds",450,"captures",10,"timeout_autoextension",false));
        report.put("corrected_XP_input",row("XpLevel",0,"XpTotal",11,"XpP",0.5714285969734192,"total_is_not_levels",true));
        report.put("capture_names",CAPTURES); report.put("declared_initial_fixtures",inputs); report.put("native_actions",actions);
        report.put("EVERY_SERVER_POST",posts); report.put("native_physical_UUID_events",physical); report.put("native_ACKs",acks);
        report.put("actual_source_epochs",epochs); report.put("old1551_actual_native_pruning",oldNodeRows); report.put("capture_events",captures);
        report.put("native_inherited_ambient_lifecycle",ambientRemovalRows);
        report.put("later_native_Mob_checkDespawn_first_observations",laterLivingRows);
        report.put("inherited_lifecycle_policy","Never clear/move/damage/reset an inherited entity. Named/NoAI/persistent entities and owned action areas remain protected. Only captured vanilla checkDespawn or environmental tickDeath of pre-existing unprotected ambient mobs OUTSIDE owned areas may retire naturally; all other removal fails closed.");
        report.put("boundary","Only approved initial RAW slots/empty furnaces/empty bench/exact AIR-only mundane supports/stone terrain/FOCALMANIPULATION and collision-checked player camera are fixtures. All smelting, XP pickup, device mining/pickup/placement, node taps/conversion, recipes, START and Potency harvest are native actions/ticks. No finished target grant, XP/vis/essentia/counter/RNG/clock setters or manual ticking/pickup/backend calls.");
        report.put("no_item_UUID_fabrication","ItemStack has no native entity UUID: unique payer components are conserved through menu ownership; physical drops are tracked by their real Entity UUIDs. No UUID component is written.");
        report.put("coverage_limits","No all-rank/all-focus client claim, no new research acquisition except declared primary key, no charm passive parity, no full survival/remote/audio/NeoForge claim. Root must inspect exact PNGs and independently audit stopped NBT/all inherited unloaded entities/devices.");
    }
    public static List<String> captureNames() { return CAPTURES; }
    public void captureEvent(String event,String name) {
        if (CAPTURES.contains(name)) captures.add(row("event",event,"name",name,"monotonic_nanos",System.nanoTime(),"client_metadata_NOT_server_stock",true));
    }
    public void check() { if (failure!=null) throw new IllegalStateException("Passive focal observation failed at "+step,failure); }
    public boolean tick(Minecraft actual) throws ReflectiveOperationException {
        try { return advance(actual); }
        catch (ReflectiveOperationException|RuntimeException|Error error) { fail(error); close(); throw error; }
    }
    private boolean advance(Minecraft actual) throws ReflectiveOperationException {
        require(actual==client && client.player!=null && client.getSingleplayerServer()!=null,"Same root client/integrated server");
        check(); age++;
        require(System.nanoTime()-wallStart<=450_000_000_000L,"Hard450wall-second budget; never extended for captures");
        require(age<=((step==Step.SMELT_WAIT || step==Step.CHARGE_LIVE || step==Step.XP_CONTACT)?3600:step==Step.SOURCE_WAIT || step==Step.COMPLETE || step==Step.POWER_USE?1400:500),"Finite phase "+step);
        switch(step) {
            case HANDOFF -> {
                Path world=client.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
                String name=world.getFileName().toString(); require(name.startsWith("focal-smoke-"),"Never open accepted/failed/foreign save");
                UUID.fromString(name.substring("focal-smoke-".length()));
                require(client.getLanguageManager().getSelected().equals("en_us"),"Root prepares the English focused client locale before entry");
                require(world.getParent().getFileName().toString().equals("saves") && world.getParent().getParent().getFileName().toString().equals("run-clienttest"),"Root's isolated fresh-copy directory");
                report.put("actual_owned_world",world.toString()); camera=client.options.getCameraType(); client.options.setCameraType(CameraType.FIRST_PERSON);
                client.setScreen(null); releaseKeys(); focusBinding=ThaumcraftFocusClient.CHANGE_FOCUS.saveString(); swapBinding=client.options.keySwapOffhand.saveString();
                installAckTap(); server(()->sceneReady=sceneLoaded(serverPlayer())); next(Step.SCENE_READY);
            }
            case SCENE_READY -> {
                if(!serverDone())return false;
                if(!sceneReady) { if(age%5==0)server(()->sceneReady=sceneLoaded(serverPlayer())); return false; }
                next(Step.INPUT_CHECK);
            }
            case INPUT_CHECK -> {
                require(age<500,"Native input preflight cannot acquire owned foreground window within user-approved25seconds: "+nativeShift.describe(client));
                if(!nativeShift.focusOwnedWindow(client))return false;
                holdShift(true); next(Step.INPUT_PRESSED);
            }
            case INPUT_PRESSED -> {
                require(age<100,"OS Shift did not reach native GLFW/Screen.hasShiftDown");
                if(age<3 || !Screen.hasShiftDown())return false;
                report.put("native_Shift_preflight",row("actual_native_pressed",true,"foreground",nativeShift.describe(client),"before_any_fixtures_or_payment",true));
                holdShift(false); next(Step.INPUT_RELEASED);
            }
            case INPUT_RELEASED -> {
                require(age<100,"Native Shift key-up did not reach GLFW");
                if(age<3 || Screen.hasShiftDown())return false;
                report.put("native_Shift_preflight_released",true); server(()->prepare(serverPlayer())); next(Step.SETUP);
            }
            case SETUP -> { if(!serverDone() || age<12)return false; next(Step.FURNACE_MOVE); }
            case FURNACE_MOVE -> { server(()->stand(serverPlayer(),FURNACES.get(furnaceIndex).getX()+.5,216,FURNACES.get(furnaceIndex).getZ()+2.6)); next(Step.FURNACE_OPEN); }
            case FURNACE_OPEN -> {
                if(!serverDone() || age<10)return false;
                // Initial15 raw stacks occupy every approved empty slot. Holding a
                // normal ingredient opens a vanilla furnace; no empty-slot grant.
                if(!use(FURNACES.get(furnaceIndex),Direction.SOUTH,Vec3.atCenterOf(FURNACES.get(furnaceIndex)).add(0,0,.5)))return false;
                next(Step.FURNACE_FILL);
            }
            case FURNACE_FILL -> {
                if(!(client.screen instanceof FurnaceScreen) || age<6)return false;
                if(clicks.isEmpty()) { require(furnaceMenu().getSlot(0).getItem().isEmpty() && furnaceMenu().getSlot(1).getItem().isEmpty() && furnaceMenu().getSlot(2).getItem().isEmpty(),"New native furnace empty");
                    clicks=ingredientClicks(List.of(new Ingredient(Items.RAW_GOLD,14,0),new Ingredient(Items.COAL,2,1))); clickIndex=0; }
                if(!runClicks())return false; next(Step.FURNACE_FILLED);
            }
            case FURNACE_FILLED -> {
                if(age<6)return false; require(furnaceMenu().getCarried().isEmpty(),"Native furnace cursor clear");
                client.player.closeContainer(); clicks=List.of(); if(++furnaceIndex<8)next(Step.FURNACE_MOVE); else next(Step.CHARGE_MOVE);
            }
            case CHARGE_MOVE -> { server(()->stand(serverPlayer(),1623.5,216,376.6)); next(Step.CHARGE_HAND); }
            case CHARGE_HAND -> { if(!serverDone() || age<12 || !hand(FocalManipulatorClientSequence::payer))return false; next(Step.CHARGE_READY); }
            case CHARGE_READY -> {
                if(!client.isWindowActive() || age<6)return false;
                sendRotation(Vec3.atCenterOf(SOURCE));
                if(charge==null) { require(age<80,"Actual native selected-hand C2S ACK before ChargeProbe");
                    if(serverDone())server(()->{ServerPlayer p=serverPlayer(); if(p.getInventory().selected==HAND && p.getMainHandItem()==p.getInventory().getItem(HAND) && payer(p.getMainHandItem()))charge=new Charge(p);}); return false; }
                charging=true; releaseSent=false; client.options.keyUse.setDown(true);
                require(client.gameMode.useItem(client.player,InteractionHand.MAIN_HAND).consumesAction(),"Native nonsneak node charge starts"); next(Step.CHARGE_LIVE);
            }
            case CHARGE_LIVE -> { if(!charge.funded)return false; releaseUse(); next(Step.CHARGE_RELEASE); }
            case CHARGE_RELEASE -> {
                releaseUse(); if(age<8 || charge.stopped<2 || client.player.isUsingItem())return false;
                if(!serverDone())return false; server(()->{charge.finish();chargedPayer=serverPlayer().getMainHandItem().copy();}); next(Step.MINE_MOVE);
            }
            case MINE_MOVE -> { if(!serverDone())return false; miningDevice=deviceIndex; server(()->{ServerPlayer p=serverPlayer(); if(deviceIndex==0)stand(p,1551.5,219,380.5); else stand(p,1553.5,216,379.5);}); next(Step.MINE_HAND); }
            case MINE_HAND -> { if(!serverDone() || age<12 || !hand(s->s.is(Items.WOODEN_PICKAXE)))return false; next(Step.MINE_AIM); }
            case MINE_AIM -> {
                if(!client.mouseHandler.isMouseGrabbed()){client.mouseHandler.grabMouse();useAim=null;return false;}
                BlockPos at=device().from(); if(!aim(at,Direction.SOUTH,Vec3.atCenterOf(at)))return false;
                require(client.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(at),"Actual crosshair targets only earned device");
                require(client.player.getMainHandItem().isCorrectToolForDrops(client.level.getBlockState(at)),"Same saved wooden pick is a real correct tool");
                mining=true; client.options.keyAttack.setDown(true);
                require(client.gameMode.startDestroyBlock(at,((BlockHitResult)client.hitResult).getDirection()),"Native Survival mining"); next(Step.MINING);
            }
            case MINING -> { sendRotation(Vec3.atCenterOf(device().from())); if(!client.level.isEmptyBlock(device().from()))return false;
                mining=false; client.options.keyAttack.setDown(false); client.gameMode.stopDestroyBlock(); next(Step.MINE_PICKUP); }
            case MINE_PICKUP -> {
                UUID id=deviceDrops.get(deviceIndex); if(id==null)return false;
                if(!serverDone())return false;
                if(count(client.player,device().item())<1) { if(age%8==0)server(()->approach(serverPlayer(),id)); return false; }
                server(()->verifyContact(serverPlayer(),id,device().item())); next(Step.MINE_PICKED);
            }
            case MINE_PICKED -> { if(!serverDone() || age<8)return false; miningDevice=-1; if(++deviceIndex<2)next(Step.MINE_MOVE); else next(Step.LOWER_MOVE); }
            case LOWER_MOVE -> { server(()->stand(serverPlayer(),1625.5,216,377.5)); next(Step.LOWER_HAND); }
            case LOWER_HAND -> { if(!serverDone() || age<12 || !hand(s->s.is(basicItem())))return false; next(Step.LOWER_PLACE); }
            case LOWER_PLACE -> { if(!use(SOURCE.below(2),Direction.UP,Vec3.atCenterOf(SOURCE.below(2)).add(0,.5,0)))return false; next(Step.LOWER_ACK); }
            case LOWER_ACK -> { if(!ack(client.level.getBlockEntity(SOURCE.below()) instanceof TileNodeStabilizer,"same recovered lower stabilizer"))return false; server(()->stand(serverPlayer(),1625.5,220,376.5)); next(Step.UPPER_MOVE); }
            case UPPER_MOVE -> { if(!serverDone() || age<12)return false; next(Step.UPPER_HAND); }
            case UPPER_HAND -> { if(!hand(s->s.is(converterItem())))return false; next(Step.UPPER_PLACE); }
            case UPPER_PLACE -> { if(!use(SOURCE,Direction.UP,Vec3.atCenterOf(SOURCE).add(0,.2,0)))return false; next(Step.UPPER_ACK); }
            case UPPER_ACK -> { if(!ack(client.level.getBlockEntity(SOURCE.above()) instanceof TileNodeConverter,"same recovered upper converter"))return false; next(Step.LEVER_HAND); }
            case LEVER_HAND -> { if(!hand(s->s.is(Items.LEVER)))return false; next(Step.LEVER_PLACE); }
            case LEVER_PLACE -> { if(!use(SUPPORT,Direction.SOUTH,Vec3.atCenterOf(SUPPORT).add(0,0,.5)))return false; next(Step.LEVER_ACK); }
            case LEVER_ACK -> { if(!ack(client.level.getBlockState(LEVER).is(Blocks.LEVER),"genuine lever item placement"))return false;
                require(!client.level.getBlockState(LEVER).getValue(LeverBlock.POWERED),"Placed lever begins OFF, no state setter"); next(Step.POWER_HAND); }
            case POWER_HAND -> { if(!hand(ItemStack::isEmpty))return false; next(Step.POWER_USE); }
            case POWER_USE -> {
                // Observe, NEVER reset, the native ordinary phase: warmup drains
                // current to zero. Begin early enough that its next1200 prune
                // boundary is AFTER the genuine1001-update conversion.
                if(view.normalCount()<0 || view.normalCount()%1200>100)return false;
                if(!use(LEVER,Direction.SOUTH,Vec3.atCenterOf(LEVER)))return false; next(Step.POWER_ACK);
            }
            case POWER_ACK -> { if(!ack(view!=null && view.powered(),"Actual native lever powers upper only"))return false; next(Step.SMELT_WAIT); }
            case SMELT_WAIT -> { if(view==null || view.smelted()!=112 || view.furnaceOutputs()!=112)return false; next(Step.TAKE_MOVE); }
            case TAKE_MOVE -> { server(()->stand(serverPlayer(),FURNACES.get(takeIndex).getX()+.5,216,FURNACES.get(takeIndex).getZ()+2.6)); next(Step.TAKE_OPEN); }
            case TAKE_OPEN -> { if(!serverDone() || age<10 || !use(FURNACES.get(takeIndex),Direction.SOUTH,Vec3.atCenterOf(FURNACES.get(takeIndex)).add(0,0,.5)))return false; next(Step.TAKE_RESULT); }
            case TAKE_RESULT -> { if(!(client.screen instanceof FurnaceScreen) || age<6)return false;
                require(furnaceMenu().getSlot(2).getItem().is(Items.GOLD_INGOT) && furnaceMenu().getSlot(2).getItem().getCount()==14,"Exactly14 genuinely smelted gold per actual result slot");
                click(2,0,ClickType.QUICK_MOVE); next(Step.TAKE_ACK); }
            case TAKE_ACK -> { if(!ack(furnaceMenu().getSlot(2).getItem().isEmpty(),"Native output extraction/XP award"))return false;
                client.player.closeContainer(); next(Step.XP_CONTACT); }
            case XP_CONTACT -> { if(!serverDone())return false;
                if(view.xpCredit()<(takeIndex+1)*14L) { if(age%8==0)server(()->approachXp(serverPlayer())); return false; }
                require(view.xpCredit()==(takeIndex+1)*14L && view.xpMass()==0,"Only this result's actual native orb pickups; merges counted");
                if(++takeIndex<8)next(Step.TAKE_MOVE); else next(Step.XP_READY); }
            case XP_READY -> { require(view.xpCredit()==112 && view.level()>=8 && view.level()<9,"112 real XP points earn LEVEL8, never XpTotal-as-level"); next(Step.SOURCE_WAIT); }
            case SOURCE_WAIT -> { if(view.source()==null || view.converterCount()!=1000 || view.converterStatus()!=2)return false; next(Step.CRAFT_MOVE); }
            case CRAFT_MOVE -> { server(()->stand(serverPlayer(),1601.5,216,376.6)); next(Step.CRAFT_HAND); }
            case CRAFT_HAND -> { if(!serverDone() || age<10 || !hand(FocalManipulatorClientSequence::payer))return false; next(Step.CRAFT_OPEN); }
            case CRAFT_OPEN -> { if(!use(BENCH,Direction.SOUTH,Vec3.atCenterOf(BENCH).add(0,0,.5)))return false; next(Step.CRAFT_FUND); }
            case CRAFT_FUND -> { if(!(client.screen instanceof GuiArcaneWorkbench) || age<6)return false;
                require(arcane().getSlot(1).getItem().isEmpty() && arcane().getCarried().isEmpty(),"New native bench empty payer/cursor"); recipeBefore=client.player.getMainHandItem().copy(); click(playerSlot(HAND),0,ClickType.QUICK_MOVE); next(Step.CRAFT_FUNDED); }
            case CRAFT_FUNDED -> { if(!ack(ItemStack.matches(arcane().getSlot(1).getItem(),recipeBefore),"Actual unique saved payer inserted"))return false;
                clicks=recipeClicks(); clickIndex=0; next(Step.CRAFT_GRID); }
            case CRAFT_GRID -> { if(!runClicks())return false; next(Step.CRAFT_PREVIEW); }
            case CRAFT_PREVIEW -> { if(!ack(arcane().craftable() && arcane().getSlot(0).getItem().is(craftOutput()),"Actual researched recipe output"))return false;
                for(String tag:WandData.PRIMALS)require(arcane().requiredVis(tag)==(craftIndex==0?2250:2880),"Literal all6 worn10% price");
                for(int cell=0;cell<9;cell++)require(arcane().getSlot(cell+2).getItem().is(recipeItem(cell)) && arcane().getSlot(cell+2).getItem().getCount()==1,"Literal genuine3x3 recipe matrix");
                if(craftIndex==1)require(ItemStack.matches(arcane().getSlot(6).getItem(),earnedCharm),"Exactly THAT earlier native-paid full charm is the machine's central ingredient");
                moveMouse((client.screen.width-190)/2+160,(client.screen.height-234)/2+64); next(Step.CRAFT_CAPTURE); }
            case CRAFT_CAPTURE -> { if(age<4)return false; shot(craftIndex); next(Step.CRAFT_TAKE); }
            case CRAFT_TAKE -> { click(0,0,ClickType.PICKUP); next(Step.CRAFT_STORE); }
            case CRAFT_STORE -> { if(!ack(client.player.containerMenu.getCarried().is(craftOutput()),"Genuine paid target cursor"))return false;
                ItemStack earned=client.player.containerMenu.getCarried().copy(); if(craftIndex==0)earnedCharm=earned;else earnedMachine=earned;
                destination=emptySlot(client.player); click(playerSlot(destination),0,ClickType.PICKUP); next(Step.CRAFT_RETURN); }
            case CRAFT_RETURN -> { if(age<6)return false;
                if(clicks.isEmpty()) { if(!client.player.containerMenu.getCarried().isEmpty())return false;
                    verifyDebit(recipeBefore,arcane().getSlot(1).getItem(),craftIndex==0?2250:2880);
                    int slot=emptySlot(client.player); clicks=List.of(new Click(1,0),new Click(playerSlot(slot),0)); clickIndex=0; }
                if(!runClicks())return false; next(Step.CRAFT_DONE); }
            case CRAFT_DONE -> { if(age<6)return false; require(arcane().getCarried().isEmpty(),"Paid payer returned through native inventory");
                for(int slot=0;slot<11;slot++)require(arcane().getSlot(slot).getItem().isEmpty(),"Native table empty after real transaction");
                client.player.closeContainer(); clicks=List.of(); if(++craftIndex<2)next(Step.CRAFT_MOVE); else next(Step.MACHINE_MOVE); }
            case MACHINE_MOVE -> { server(()->stand(serverPlayer(),1611.5,219,377.5)); next(Step.MACHINE_HAND); }
            case MACHINE_HAND -> { if(!serverDone() || age<12 || !hand(s->s.is(machineItem())))return false; require(ItemStack.matches(client.player.getMainHandItem(),earnedMachine),"Exactly the earned machine, not a new grant"); next(Step.MACHINE_PLACE); }
            case MACHINE_PLACE -> { if(!use(MACHINE.below(),Direction.UP,Vec3.atCenterOf(MACHINE.below()).add(0,.5,0)))return false; next(Step.MACHINE_ACK); }
            case MACHINE_ACK -> { if(!ack(client.level.getBlockEntity(MACHINE) instanceof TileFocalManipulator,"Earned machine native placement"))return false; next(Step.MACHINE_VIEW); }
            case MACHINE_VIEW -> { if(age<8 || !hand(ItemStack::isEmpty))return false; sendRotation(Vec3.atCenterOf(MACHINE)); shot(2); next(Step.FOCAL_OPEN); }
            case FOCAL_OPEN -> { if(!use(MACHINE,Direction.SOUTH,Vec3.atCenterOf(MACHINE).add(0,0,.5)))return false; next(Step.FOCAL_INSERT); }
            case FOCAL_INSERT -> { if(!(client.screen instanceof GuiFocalManipulator) || age<6)return false; require(focal().getSlot(0).getItem().isEmpty(),"No fake upgraded preview in physical slot");
                click(playerSlot(find(client.player,s->ItemStack.matches(s,savedFocus))),0,ClickType.QUICK_MOVE); next(Step.FOCAL_INSERTED); }
            case FOCAL_INSERTED -> { if(!ack(ItemStack.matches(focal().getSlot(0).getItem(),savedFocus),"Same saved unupgraded Excavation physical slot"))return false;
                cosmeticRevision=focal().selectionRevision(); beforeLevel=view.level(); beforeTotal=view.totalXp(); beforeProgress=view.xpProgress(); next(Step.LOCAL_SELECT); }
            case LOCAL_SELECT -> { if(age<8)return false; guiClick(64,111); require(((Number)BookClientSmoke.field(client.screen,"selected")).intValue()==0,"Local Potency ID0 selected in filtered original order"); next(Step.LOCAL_DESELECT); }
            case LOCAL_DESELECT -> { if(age<6)return false; assertCosmetic(); guiClick(64,111); require(((Number)BookClientSmoke.field(client.screen,"selected")).intValue()==-1,"Same icon deselects without payment"); next(Step.LOCAL_RESELECT); }
            case LOCAL_RESELECT -> { if(age<6)return false; assertCosmetic(); guiClick(64,111); next(Step.LOCAL_VIEW); }
            case LOCAL_VIEW -> { if(age<6)return false; assertCosmetic(); moveMouse((client.screen.width-192)/2+64,(client.screen.height-233)/2+111); shot(3); next(Step.START); }
            case START -> { requestedRevision=focal().selectionRevision(); requestedSession=focal().sessionId();requestedContainer=focal().containerId;
                beforeLevel=view.level(); beforeTotal=view.totalXp(); beforeProgress=view.xpProgress(); require(beforeLevel>=8 && beforeLevel<9,"Real furnace-earned levels before ONE START");
                require(actualStarts==0,"Publish exactly one thread-visible intent before outbound C2S"); actualStarts=1;
                guiClick(90,92); expectedPending=BookClientSmoke.field(client.screen,"pending"); require(expectedPending!=null,"Actual GUI captured pending session/revision intent"); next(Step.START_ACK); }
            case START_ACK -> { if(view==null || view.jobSize()!=2400 || acks.isEmpty() || BookClientSmoke.field(client.screen,"pending")!=null)return false;
                require(view.level()==beforeLevel-8 && view.upgrade()==0 && view.rank()==1,"Native8LEVEL debit and exact actual Potency job"); next(Step.STALL); }
            case STALL -> { if(view.tick()-paidStartTick<20)return false; require(view.debt().equals(POTENCY) && ledger.totalGrants()==0 && !view.relay(),"Actual fifth-tick debt stall with no relay/grants");
                next(Step.RELAY_MOVE);shot(4); }
            case RELAY_MOVE -> { client.player.closeContainer();server(()->stand(serverPlayer(),1618.5,218,376.5)); next(Step.RELAY_HAND); }
            case RELAY_HAND -> { if(!serverDone() || age<12 || !hand(s->s.is(relayItem())))return false; next(Step.RELAY_PLACE); }
            case RELAY_PLACE -> { if(!use(RELAY.below(),Direction.UP,Vec3.atCenterOf(RELAY.below()).add(0,.5,0)))return false; next(Step.RELAY_ACK); }
            case RELAY_ACK -> { if(!ack(client.level.getBlockEntity(RELAY) instanceof TileVisRelay && view.relay(),"Saved spare relay native placement"))return false;
                require(RELAY.distSqr(SOURCE)==49 && MACHINE.distSqr(RELAY)==50 && MACHINE.distSqr(SOURCE)==197,"Real routed geometry outside direct8 range");server(()->stand(serverPlayer(),1616.5,216,386.5));next(Step.NETWORK_VIEW); }
            case NETWORK_VIEW -> { if(!serverDone() || age<12)return false; sendRotation(new Vec3(1616.5,219,374.5)); shot(5); next(Step.ACTIVE_MOVE); }
            case ACTIVE_MOVE -> { server(()->stand(serverPlayer(),1611.5,219,377.5)); next(Step.ACTIVE_OPEN); }
            case ACTIVE_OPEN -> { if(!serverDone() || age<12 || !hand(ItemStack::isEmpty) || !use(MACHINE,Direction.SOUTH,Vec3.atCenterOf(MACHINE).add(0,0,.5)))return false; next(Step.ACTIVE_VIEW); }
            case ACTIVE_VIEW -> { if(!(client.screen instanceof GuiFocalManipulator) || age<8 || ledger.totalGrants()==0)return false;
                require(view.jobSize()==2400 && sum(view.debt())>0 && sum(view.debt())<2400,"Actual partially paid original remaining-vis bar"); shot(6); next(Step.COMPLETE); }
            case COMPLETE -> { if(view.jobSize()!=0 || view.focus().isEmpty() || !potencyOne(view.focus()))return false; upgradedFocus=view.focus().copy(); next(Step.COMPLETED_VIEW); }
            case COMPLETED_VIEW -> { if(age<8 || !potencyOne(focal().getSlot(0).getItem()))return false; require(ledger.totalGrants()==2400,"All6 genuine source grants total2400"); shot(7); next(Step.FOCUS_REMOVE); }
            case FOCUS_REMOVE -> { click(0,0,ClickType.QUICK_MOVE); next(Step.FOCUS_REMOVED); }
            case FOCUS_REMOVED -> { if(!ack(focal().getSlot(0).getItem().isEmpty(),"Same full upgraded focus returned natively"))return false; client.player.closeContainer(); next(Step.EQUIP_MOVE); }
            case EQUIP_MOVE -> { server(()->stand(serverPlayer(),1605.5,216,387.5)); next(Step.EQUIP_HAND); }
            case EQUIP_HAND -> { if(!serverDone() || age<12 || !hand(FocalManipulatorClientSequence::payer))return false; next(Step.EQUIP_PRESS); }
            case EQUIP_PRESS -> { if(!client.isWindowActive())return false; pressFocus(); next(Step.EQUIP_RADIAL); }
            case EQUIP_RADIAL -> { holdFocus(); if(!(client.screen instanceof GuiFocusSelect) || age<8)return false;
                int[] point=selectorPoint(); var screen=client.screen; screen.mouseMoved(point[0],point[1]); require(screen.mouseClicked(point[0],point[1],0),"Native selector chooses actual upgraded focus"); screen.mouseReleased(point[0],point[1],0); releaseFocus(); next(Step.EQUIP_ACK); }
            case EQUIP_ACK -> { if(!ack(ItemStack.matches(FocusData.get(client.player.getMainHandItem()),upgradedFocus),"Real focus-selector C2S install"))return false;
                server(()->prepareCast(serverPlayer())); next(Step.CAST_MOVE); }
            case CAST_MOVE -> { if(!serverDone() || age<10)return false; next(Step.CAST_READY); }
            case CAST_READY -> { if(!client.isWindowActive())return false; sendRotation(Vec3.atCenterOf(STONE)); if(age<8)return false; next(Step.CAST_START); }
            case CAST_START -> { casting=true; releaseSent=false; client.options.keyUse.setDown(true); require(client.gameMode.useItem(client.player,InteractionHand.MAIN_HAND).consumesAction(),"ONE genuine held Potency cast"); next(Step.CAST_LIVE); }
            case CAST_LIVE -> { if(castPayments!=1)return false; releaseUse(); next(Step.CAST_RELEASE); }
            case CAST_RELEASE -> { releaseUse(); if(age<8 || client.player.isUsingItem() || view.using())return false; require(cobbleId!=null && breakCalls==1 && beforeCalls==1 && afterCalls==1 && sawZero && sawHalf,"Native protection callbacks/first-coordinate/preincrement/one paid removal witnessed"); next(Step.CAST_PICKUP); }
            case CAST_PICKUP -> { if(!serverDone())return false; if(count(client.player,Items.COBBLESTONE)!=4) { if(age%8==0)server(()->approach(serverPlayer(),cobbleId)); return false; }
                server(()->verifyContact(serverPlayer(),cobbleId,Items.COBBLESTONE)); next(Step.CAST_PICKED); }
            case CAST_PICKED -> { if(!serverDone() || age<8 || !nativeShift.focusOwnedWindow(client))return false; client.setScreen(new InventoryScreen(client.player)); holdShift(true); next(Step.CAST_VIEW); }
            case CAST_VIEW -> { if(age<8 || !Screen.hasShiftDown())return false; moveInventoryMouse(find(client.player,FocalManipulatorClientSequence::payer));next(Step.UNEQUIP);shot(8); }
            case UNEQUIP -> { if(client.screen!=null)client.setScreen(null);holdShift(true); if(age<8 || !client.player.isShiftKeyDown())return false; pressFocus(); next(Step.UNEQUIP_ACK); }
            case UNEQUIP_ACK -> { holdFocus(); if(!FocusData.get(client.player.getMainHandItem()).isEmpty() || count(client.player,ThaumcraftFoci.EXCAVATION_FOCUS.get())!=1)return false;
                releaseFocus(); holdShift(false); require(ItemStack.matches(stack(client.player,s->s.is(ThaumcraftFoci.EXCAVATION_FOCUS.get())),upgradedFocus),"Native unequip returns same full focus"); server(()->stand(serverPlayer(),1611.5,219,377.5)); next(Step.RANK2_MOVE); }
            case RANK2_MOVE -> { if(!serverDone() || age<12 || !hand(ItemStack::isEmpty))return false; next(Step.RANK2_OPEN); }
            case RANK2_OPEN -> { if(!use(MACHINE,Direction.SOUTH,Vec3.atCenterOf(MACHINE).add(0,0,.5)))return false; next(Step.RANK2_INSERT); }
            case RANK2_INSERT -> { if(!(client.screen instanceof GuiFocalManipulator) || age<6)return false; click(playerSlot(find(client.player,s->ItemStack.matches(s,upgradedFocus))),0,ClickType.QUICK_MOVE); next(Step.RANK2_VIEW); }
            case RANK2_VIEW -> { if(!ack(ItemStack.matches(focal().getSlot(0).getItem(),upgradedFocus),"Actual rank1 focus reinserted"))return false; guiClick(64,111); beforeLevel=view.level(); beforeTotal=view.totalXp(); beforeProgress=view.xpProgress(); next(Step.RANK2_REFUSE); }
            case RANK2_REFUSE -> { if(age<8)return false; require(beforeLevel<16,"Actual insufficient rank2 levels"); guiClick(90,92); require(BookClientSmoke.field(client.screen,"pending")==null,"Disabled native rank2 bar cannot issue another START"); next(Step.RANK2_CAPTURE); }
            case RANK2_CAPTURE -> { if(age<10)return false; require(view.level()==beforeLevel && view.totalXp()==beforeTotal && view.xpProgress()==beforeProgress && view.jobSize()==0 && actualStarts==1 && acks.size()==1,"Rank2 refusal has no economic mutation or second START/ACK"); shot(9); next(Step.RETURN_FOCUS); }
            case RETURN_FOCUS -> { if(age==1)click(0,0,ClickType.QUICK_MOVE); if(age<8)return false; require(focal().getSlot(0).getItem().isEmpty(),"Final machine physical slot empty"); client.player.closeContainer(); clicks=List.of(); next(Step.RESTORE); }
            case RESTORE -> { if(!restoreSavedSlots())return false; server(()->finish(serverPlayer())); next(Step.FINAL_CHECK); }
            case FINAL_CHECK -> { if(!serverDone() || age<8)return false; require(storyFinal && captureCount==10,"Ten exact requested captures and finite native final ledger"); releaseInputs(); restoreAckTap(); next(Step.FINAL_ACK); }
            case FINAL_ACK -> { if(age<6)return false; done=true; report.put("story_status","COMPLETED_UNINSPECTED_tail_still_running"); next(Step.DONE); }
            case DONE -> { check(); return true; }
        }
        return false;
    }

    /** Read-only natural startup readiness. Never request/force-load a chunk,
     * create a missing entity, or count an unloaded cow as preserved. */
    private boolean sceneLoaded(ServerPlayer p) {
        ServerLevel level=p.serverLevel();
        for(BlockPos pos:List.of(OLD_NODE,SOURCE,BENCH,MACHINE,RELAY,FURNACES.getFirst(),FURNACES.getLast()))
            if(level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)==null)return false;
        for(Vec3 position:List.of(new Vec3(1561.5,216,386.5),new Vec3(1596.5,216,381.5),new Vec3(1628.5,216,369.5)))
            if(level.getEntitiesOfClass(LivingEntity.class,new AABB(position,position.add(.01,2,.01)).inflate(.4),
                    entity->entity.getType()==net.minecraft.world.entity.EntityType.COW && entity.position().equals(position)).isEmpty())return false;
        return true;
    }
    private void prepare(ServerPlayer p) {
        require(p.server.isSameThread() && p.isAlive() && p.gameMode.getGameModeForPlayer()==GameType.SURVIVAL,"Actual saved Survival actor"); owner=p;
        ClientTestServerLifecycle.register(p.server,stopHook); // Snapshot-dispatched failure tail even if initial guards reject the clone.
        require(p.experienceLevel==0 && p.totalExperience==11 && Float.floatToIntBits(p.experienceProgress)==Float.floatToIntBits(.5714285969734192F),"Exact corrected saved XP triplet, never normalized");
        require(p.containerMenu==p.inventoryMenu && p.containerMenu.getCarried().isEmpty() && !p.isUsingItem(),"Saved menu/cursor/channel clear");
        savedPayer=p.getInventory().getItem(10).copy(); savedFocus=p.getInventory().getItem(15).copy(); savedPick=stack(p,s->s.is(Items.WOODEN_PICKAXE)).copy(); savedOffhand=p.getOffhandItem().copy(); savedInvulnerable=p.isInvulnerable();
        require(payer(savedPayer) && WandData.PRIMALS.stream().allMatch(type->ItemWandCasting.data(savedPayer).amount(type)==INITIAL_VIS.get(type)) && FocusData.get(savedPayer).isEmpty(),"Exact UNFOCUSED silverwood/gold10 finite payer; missing zero keys are not normalized");
        require(savedFocus.is(ThaumcraftFoci.EXCAVATION_FOCUS.get()) && Arrays.equals(((ItemFocusExcavation)savedFocus.getItem()).getAppliedUpgrades(savedFocus),new short[]{-1,-1,-1,-1,-1}),"Same saved basic Excavation15");
        require(savedPick.getDamageValue()==3 && count(p,Items.WOODEN_PICKAXE)==1,"Saved one native wooden pick, damage3");
        require(count(p,ThaumcraftContent.WAND.get())==3,"Saved three actual wands");
        require(count(p,relayItem())==1,"Saved ONE spare relay from M09b's genuine batch2 minus installed1; actual="+count(p,relayItem()));
        require(count(p,Items.COAL)==0 && count(p,Items.COBBLESTONE)==3,"Exact accepted coal0/oldcobble3; old stock is never spent or relabeled as the new output");
        for(int i=0;i<36;i++)if(!p.getInventory().getItem(i).isEmpty())savedInventory.put(i,p.getInventory().getItem(i).copy());
        for(EquipmentSlot slot:ARMOR)savedArmor.add(p.getItemBySlot(slot).copy());
        var k=KnowledgeAccess.get(p); require(k.isResearchComplete("NODEPRESERVE") && k.isResearchComplete("BASICARTIFACE"),"Accepted NODEPRESERVE/BASICARTIFACE already known");
        CompoundTag before=k.save().copy(); boolean added=!k.isResearchComplete("FOCALMANIPULATION"); if(added)k.addResearch("FOCALMANIPULATION"); knowledge=k.save().copy(); KnowledgeEvents.sync(p);
        inputs.add(row("ONLY_primary_knowledge_fixture","FOCALMANIPULATION","already_known",!added,"before",before.toString(),"after",knowledge.toString()));
        ServerLevel level=p.serverLevel();
        require(level.getBlockEntity(SOURCE) instanceof TileNode node && amounts(node.getAspectsBase()).equals(BASE),"Healthy actual saved1623 source/base");
        healthyId=((TileNode)level.getBlockEntity(SOURCE)).getScanId();UUID.fromString(healthyId);
        require(level.getBlockEntity(OLD_NODE.above()) instanceof TileNodeConverter converter && !converter.isPowered() && level.getBlockEntity(OLD_NODE.below()) instanceof TileNodeStabilizer,"Two actual earned OFF devices, never fixtures");
        snapshotOld(level); activeServerOwner=this; fixtures(level); supply(p);
        report.put("actual_initial_inventory",savedInventory.entrySet().stream().map(e->row("slot",e.getKey(),"stack",snbt(e.getValue(),p))).toList());
        report.put("actual_initial_healthy_node",tag(level.getBlockEntity(SOURCE)).toString());
        xp=new FocalXpPickupObserver(p,SCENE,this::fail); report.put("native_XP_orb_HEAD_RETURN_POST_rows",xp.rows());
        ledger=new Ledger(p); TickEvent.SERVER_POST.register(postHook); EntityEvent.ADD.register(addHook); BlockEvent.BREAK.register(breakHook);
        hooks=true;
        client.execute(()->{ activeInputOwner=this; ClientTickEvent.CLIENT_PRE.register(preHook); }); p.inventoryMenu.broadcastChanges();
    }

    private void fixtures(ServerLevel level) {
        for(BlockPos pos:FURNACES)fixtureBlocks.put(pos,Blocks.FURNACE.defaultBlockState()); fixtureBlocks.put(BENCH,ThaumcraftArcane.ARCANE_WORKBENCH.get().defaultBlockState());
        fixtureBlocks.put(SUPPORT,Blocks.STONE.defaultBlockState()); fixtureBlocks.put(STONE,Blocks.STONE.defaultBlockState());
        column(new BlockPos(1609,216,374),218); column(new BlockPos(1616,216,374),217);
        column(new BlockPos(1611,216,377),218); column(new BlockPos(1618,216,376),217); column(new BlockPos(1625,216,376),219);
        // Stopped-NBT preflight proved these precise floor cells were AIR: the
        // accepted workshop has separate pads, not one broad continuous floor.
        // Declare only these supports; never replace/clear an inherited block.
        for(int x:new int[]{1634,1636,1638})for(int z:new int[]{371,373,375,377})
            fixtureBlocks.put(new BlockPos(x,215,z),Blocks.STONE.defaultBlockState());
        for(BlockPos pos:List.of(new BlockPos(1601,215,374),new BlockPos(1601,215,376),
                new BlockPos(1605,215,381),new BlockPos(1605,215,387),new BlockPos(1600,215,372),
                new BlockPos(1610,215,379),new BlockPos(1640,215,373)))fixtureBlocks.put(pos,Blocks.STONE.defaultBlockState());
        // Real native output has random position/velocity. Preserve its exact
        // UUID and motion; provide a declared bounded5x5 catch/contact floor
        // rather than teleporting/replacing a drop that leaves a one-cell pad.
        for(int x=1603;x<=1607;x++)for(int z=379;z<=383;z++)
            fixtureBlocks.put(new BlockPos(x,215,z),Blocks.STONE.defaultBlockState());
        // Lower stabilizer sits at217; this one mundane base is not a power item.
        fixtureBlocks.put(SOURCE.below(2),Blocks.STONE.defaultBlockState());
        for(BlockPos pos:List.of(new BlockPos(1600,216,372),new BlockPos(1610,216,379),new BlockPos(1630,216,373),new BlockPos(1640,216,373)))fixtureBlocks.put(pos,Blocks.TORCH.defaultBlockState());
        var earnedCells=List.of(MACHINE,RELAY,SOURCE.above(),SOURCE.below(),LEVER);
        for(BlockPos pos:fixtureBlocks.keySet())guardAirCell(level,pos);
        for(BlockPos pos:earnedCells)guardAirCell(level,pos);
        for(BlockPos pos:FURNACES)require(fixtureBlocks.getOrDefault(pos.below(),level.getBlockState(pos.below())).is(Blocks.STONE),"Furnace uses exact preserved or explicitly declared stone support");
        for(var entry:fixtureBlocks.entrySet()) { level.setBlockAndUpdate(entry.getKey(),entry.getValue()); inputs.add(row("fixture","initial exact AIR-only empty/mundane block","pos",entry.getKey().toShortString(),"state",entry.getValue().toString())); }
        for(BlockPos pos:FURNACES) { FurnaceBlockEntity furnace=furnace(level,pos); require(furnace.isEmpty() && recipesUsed(furnace)==0 && furnace.saveWithoutMetadata(level.registryAccess()).getShort("BurnTime")==0,"New furnace empty/B0/recipeUsage0, no setters"); }
        require(((TileArcaneWorkbench)level.getBlockEntity(BENCH)).isEmpty(),"Empty new arcane bench fixture only");
    }
    private void column(BlockPos bottom,int top) { for(int y=bottom.getY();y<=top;y++)fixtureBlocks.put(new BlockPos(bottom.getX(),y,bottom.getZ()),Blocks.STONE.defaultBlockState()); }
    private void guardAirCell(ServerLevel level,BlockPos pos) {
        require(level.hasChunkAt(pos) && level.isEmptyBlock(pos) && level.getBlockEntity(pos)==null,"Only loaded exact previously AIR/no-BE fixture/earned cell "+pos);
        require(level.getEntitiesOfClass(LivingEntity.class,new AABB(pos)).isEmpty(),"No living-entity (including player) overlap/clearing at "+pos);
    }
    private void supply(ServerPlayer p) {
        List<Integer> slots=List.of(20,21,22,24,25,26,27,28,29,30,31,32,33,34,35);
        var raw=new ArrayList<ItemStack>(); raw.add(new ItemStack(Items.RAW_GOLD,64)); raw.add(new ItemStack(Items.RAW_GOLD,48)); raw.add(new ItemStack(Items.COAL,16));
        for(String primal:WandData.PRIMALS)raw.add(new ItemStack(ThaumcraftContent.SHARDS.get(primal).get()));
        raw.add(new ItemStack(ThaumcraftContent.SHARDS.get("balanced").get())); raw.add(new ItemStack(ThaumcraftMasonry.ARCANE_STONE.get(),2));
        raw.add(new ItemStack(ThaumcraftMasonry.SLAB.get())); raw.add(new ItemStack(ThaumcraftContent.TABLE_ITEM.get())); raw.add(new ItemStack(Items.IRON_INGOT,2)); raw.add(new ItemStack(Items.LEVER));
        require(raw.size()==15 && count(p,Items.GOLD_INGOT)==0 && count(p,Items.RAW_GOLD)==0 && count(p,charmItem())==0 && count(p,machineItem())==0,"Exactly15 approved RAW stacks, no finished/produced target hidden");
        for(int i=0;i<slots.size();i++)require(p.getInventory().getItem(slots.get(i)).isEmpty(),"Exact saved empty fixture slot "+slots.get(i));
        for(int i=0;i<slots.size();i++) { ItemStack value=raw.get(i); p.getInventory().setItem(slots.get(i),value); declared.merge(value.getItem(),value.getCount(),Integer::sum); inputs.add(row("fixture","initial RAW player stack ONLY","slot",slots.get(i),"actual_stack",snbt(value,p))); }
    }

    private record View(long tick,int level,int totalXp,float xpProgress,long xpCredit,long xpMass,int smelted,int furnaceOutputs,
                        int charmCrafts,int machineCrafts,Map<String,Integer> payerVis,int jobSize,int rank,int upgrade,
                        Map<String,Integer> debt,ItemStack focus,long revision,TileNodeEnergized source,long epoch,
                        Map<String,Integer> available,boolean relay,boolean powered,int converterCount,int converterStatus,
                        int normalCount,boolean using) {}
    private final class Ledger {
        final ServerPlayer player;
        View previous;
        int checks,smelts,charms,machines,xpDebits,burnedCoal,warmupIncrements,warmupUpdates;
        int lastCoal=16;
        List<NativeFurnace> previousFurnaces;
        long xpCreditsBefore;
        final Map<String,Integer> gains=new LinkedHashMap<>(),grants=new LinkedHashMap<>();
        final Map<String,Long> generated=new LinkedHashMap<>(),unused=new LinkedHashMap<>();
        private Map<String,Integer> lastEpochPaid=new LinkedHashMap<>();
        private String oldNodeSignature="";
        private NodeState previousOld;
        private TileNode oldIdentity;
        private boolean sawOldPrune;
        private boolean completed;
        Ledger(ServerPlayer player) { this.player=player; previous=snapshot();previousFurnaces=nativeFurnaces(player.serverLevel());view=previous; }
        int totalGrants() { return sum(grants); }

        void observe() throws ReflectiveOperationException {
            require(player.server.isSameThread(),"Pure ledger on exact server thread");
            if(firstPost<0)firstPost=player.server.getTickCount(); lastPost=player.server.getTickCount();
            require(lastPost-firstPost<=9000 && System.nanoTime()-wallStart<=450_000_000_000L,"Hard native9000tick/450wall-second bound INCLUDING PNG pauses and tail");
            View now=snapshot();
            int nativeSmelts=observeFurnaces();observePhysicalContacts(player);
            require(now.smelted()>=smelts && now.smelted()-smelts<=8 && now.smelted()<=112,"At most one real smelt per furnace/native POST");
            require(now.smelted()-smelts==nativeSmelts,"Every produced ingot corresponds to actual199->0 native cook completion at200ticks");
            smelts=now.smelted();
            int dc=now.charmCrafts()-charms,dm=now.machineCrafts()-machines;
            require(dc>=0 && dc<=1 && dm>=0 && dm<=1 && dc+dm<=1 && now.charmCrafts()<=1 && now.machineCrafts()<=1,"Only two real paid recipe edges");
            if(dm>0)require(charms==1,"Machine consumes the previously paid dependency");
            if(dc+dm>0)actions.add(row("event","actual native recipe ownership/payment edge","tick",now.tick(),"charm",dc,"machine",dm,"payer_before",previous.payerVis(),"payer_after",now.payerVis()));
            charms=now.charmCrafts(); machines=now.machineCrafts();
            Map<String,Integer> received=charge!=null && charge.active?charge.lastGains:Map.of();
            int cast=0;
            for(String type:WandData.PRIMALS) {
                int gain=received.getOrDefault(type,0); if(gain>0)gains.merge(type,gain,Integer::sum);
                int unexplained=previous.payerVis().getOrDefault(type,0)+gain-dc*2250-dm*2880-now.payerVis().getOrDefault(type,0);
                if(type.equals("terra") && unexplained==13)cast=1;else require(unexplained==0,"EVERY payer CV change is native tap/actual recipe/exact13terra cast: "+type);
                require(now.payerVis().getOrDefault(type,0)==INITIAL_VIS.get(type)+gains.getOrDefault(type,0)-charms*2250-machines*2880-(castPayments+cast)*(type.equals("terra")?13:0),"Full finite payer conservation "+type);
            }
            if(cast>0) { require(casting && castPayments==0 && ledger.completed && castProgressSamples==4 && lastCastProgress==1.5F,"Only one funded cast after native0,.5,1,1.5 / original NEXT tick preincrement completion"); castPayments++;
                actions.add(row("event","actual13terra completion debit","tick",now.tick(),"before",previous.payerVis(),"after",now.payerVis(),"not_a_backend_call",true)); }
            Map<String,Integer> paid=Map.of();
            if(paidStartTick<0 && now.jobSize()>0) {
                require(actualStarts==1 && now.jobSize()==2400 && now.rank()==1 && now.upgrade()==0 && now.debt().equals(POTENCY),"ONE actual bound rank1 Potency START/full SUM debt");
                require(previous.level()>=8 && previous.level()<9 && now.level()==previous.level()-8,"Original native8LEVEL upfront debit");
                require(now.totalXp()==previous.totalXp() && Float.floatToIntBits(now.xpProgress())==Float.floatToIntBits(previous.xpProgress()),"Native nonnegative giveExperienceLevels debit preserves total/progress, never fabricated XP points");
                require(now.xpCredit()==previous.xpCredit(),"No overlapping XP pickup obscures native level debit");
                paidStartTick=now.tick(); xpDebits++;
                actions.add(row("event","native START material edge","tick",now.tick(),"XpLevel_Total_Progress_before",List.of(previous.level(),previous.totalXp(),previous.xpProgress()),"XpLevel_Total_Progress_after",List.of(now.level(),now.totalXp(),now.xpProgress()),"actual_debt",now.debt(),"actual_revision",now.revision(),"XP_field_writes",0));
            } else if(paidStartTick>=0 && !completed) {
                var delta=new LinkedHashMap<String,Integer>();
                for(String type:WandData.PRIMALS) {
                    int grant=previous.debt().getOrDefault(type,0)-now.debt().getOrDefault(type,0);
                    require(grant>=0 && grant<=8,"Real per-primal grant within actual source8 pulse "+type);
                    if(grant>0) { require(now.source()!=null && now.relay() && actualMachine(player.serverLevel()).getTickCount()%5==0,"Only native fifth-tick routed processing reduces debt"); delta.put(type,grant); grants.merge(type,grant,Integer::sum); }
                }
                paid=Map.copyOf(delta);
                if(now.jobSize()==0) { require(grants.equals(POTENCY) && now.debt().isEmpty() && potencyOne(now.focus()),"Only exact full2400 grants apply rank1; no free completion"); completed=true; }
            } else require(now.jobSize()==0,"After completion no extra paid job may exist");
            observeEpoch(previous,now,paid);
            if(paidStartTick<0 || now.tick()!=paidStartTick) {
                if(now.xpCredit()==previous.xpCredit())require(now.level()==previous.level() && now.totalXp()==previous.totalXp() && now.xpProgress()==previous.xpProgress(),"No XP change without actual mixin pickup or ONE native level debit");
                else require(now.xpCredit()>previous.xpCredit() && now.totalXp()-previous.totalXp()==now.xpCredit()-previous.xpCredit(),"Actual native XP touch points reconcile player total, not assumed level arithmetic");
            }
            if(casting && castPayments==0)observeCastProgress(player);
            observeTower(previous,now);observeOldNode(player.serverLevel(),now.tick()); preserve(player,false);
            posts.add(row("POST",++checks,"server_tick",player.server.getTickCount(),"game_tick",now.tick(),"phase",step.name(),
                    "XpLevel_Total_Progress",List.of(now.level(),now.totalXp(),now.xpProgress()),"actual_XP_credit_mass",List.of(now.xpCredit(),now.xpMass()),
                    "actual_smelts_recipe_edges",List.of(now.smelted(),charms,machines),"actual_payer_CV",now.payerVis(),
                    "actual_job",row("size",now.jobSize(),"rank",now.rank(),"upgrade",now.upgrade(),"remaining",now.debt(),"revision",now.revision(),"native_table_ticks",actualMachine(player.serverLevel())==null?-1:actualMachine(player.serverLevel()).getTickCount()),
                    "actual_source",row("epoch",now.epoch(),"available",now.available(),"THIS_POST_grants",paid,"all_grants",Map.copyOf(grants)),
                    "actual_converter",List.of(now.converterCount(),now.converterStatus(),now.powered()),"healthy_ordinary_count",now.normalCount(),
                    "native_furnaces",furnaceRows(player.serverLevel()),"physical_gold",countOwned(player,Items.GOLD_INGOT),"actual_coal_live_burned",List.of(lastCoal,burnedCoal),
                    "observer_resource_tick_clock_writes",0));
            previous=now; view=now;
        }

        private void observeTower(View before,View after) {
            if(after.source()==null && after.powered()) {
                require(after.converterStatus()==1 && after.converterCount()>=0,"Only native ordinary-node powered warmup");
                if(before.powered())require(after.converterCount()==before.converterCount() || after.converterCount()==before.converterCount()+1,"No forced converter warmup ticks/count writes");
                int increment=before.converterCount()<0?after.converterCount():after.converterCount()-before.converterCount();
                require(increment>=0 && increment<=1,"Actual at-most-one converter increment per native POST");
                warmupIncrements+=increment;if(increment==1)warmupUpdates++;
            } else if(before.source()==null && after.source()!=null) {
                warmupUpdates++;
                require(warmupIncrements==1000 && warmupUpdates==1001 && before.converterCount()==1000 && after.converterCount()==1000 && after.converterStatus()==2 && after.powered(),"1000 native increments then original preincrement1001st conversion update");
                actions.add(row("event","genuine1001update conversion boundary","tick",after.tick(),"actual_increments",warmupIncrements,"actual_updates",warmupUpdates,"healthy_BASE",amounts(after.source().getAuraBase())));
            }
        }

        private int observeFurnaces() {
            List<NativeFurnace> now=nativeFurnaces(player.serverLevel());int ignited=0,finished=0;
            for(int i=0;i<8;i++) {
                NativeFurnace old=previousFurnaces.get(i),current=now.get(i);int before=old.burn(),after=current.burn();
                if(after>before) { require(before<=1 && after==1600,"Only real vanilla coal ignition replenishes BurnTime");ignited++; }
                else require(after==before || after==Math.max(0,before-1),"Native furnace BurnTime never accelerates or resets");
                // A pristine native furnace has total0 until its FIRST actual
                // input insertion initializes the recipe's200-tick duration.
                // This observer never initializes or repairs a clock itself.
                boolean pristine=current.total()==0 && old.total()==0 && before==0 && after==0
                        && old.cook()==0 && current.cook()==0 && old.raw()==0 && current.raw()==0;
                boolean initialized=current.total()==200 && (old.total()==200 || old.total()==0 && old.cook()==0 && old.raw()==0 && current.raw()>0);
                require((pristine || initialized) && current.cook()>=0 && current.cook()<200
                        && (current.cook()==old.cook() || current.cook()==old.cook()+1 || old.cook()==199 && current.cook()==0),"Pristine native0 then one-way real input initialization/200tick cookclock; never observer clock writes");
                if(old.cook()==199 && current.cook()==0) { require(current.raw()==old.raw()-1,"Native cook completion consumes one raw gold");finished++; }
            }
            int coal=countOwned(player,Items.COAL);require(lastCoal-coal==ignited && coal>=0 && coal<=16,"EVERY coal disappearance corresponds to an actual native1600tick ignition");
            burnedCoal+=ignited;lastCoal=coal;previousFurnaces=now;return finished;
        }

        private View snapshot() {
            ServerLevel level=player.serverLevel();
            List<ItemStack> values=physicalStacks(player);
            List<ItemStack> payers=values.stream().filter(FocalManipulatorClientSequence::payer).toList();
            require(payers.size()==1 && payers.getFirst().getCount()==1 && samePayerShell(payers.getFirst(),savedPayer),"ONE component-sensitive saved payer owner, no fabricated UUID/duplicate");
            ItemStack actualPayer=payers.getFirst();
            int raw=count(values,Items.RAW_GOLD),made=112-raw;
            int machineOwners=count(values,machineItem())+(loadedBlock(level,MACHINE).is(ThaumcraftFocalManipulation.FOCAL_MANIPULATOR.get())?1:0);
            int charmCrafts=count(values,charmItem())+machineOwners;
            int gold=count(values,Items.GOLD_INGOT);
            require(raw>=0 && raw<=112 && machineOwners>=0 && machineOwners<=1 && charmCrafts>=0 && charmCrafts<=1,"Finite raw/target ownership");
            require(gold==made-2*charmCrafts-2*machineOwners,"Every gold ingot is genuinely smelted; exactly4 spent in real recipes");
            int outputs=0,pendingXp=0;
            for(BlockPos pos:FURNACES) { FurnaceBlockEntity f=furnace(level,pos); require(f.getItem(0).isEmpty() || f.getItem(0).is(Items.RAW_GOLD),"No other furnace input");
                require(f.getItem(1).isEmpty() || f.getItem(1).is(Items.COAL),"No other fuel"); require(f.getItem(2).isEmpty() || f.getItem(2).is(Items.GOLD_INGOT),"No synthetic/non-gold furnace result");
                outputs+=f.getItem(2).getCount(); pendingXp+=recipesUsed(f); }
            long mass=xp.post(made-pendingXp); require(mass>=0,"XP observer remains live");
            checkRaw(values,charmCrafts,machineOwners);
            int lower=count(values,basicItem())+blockCount(level,OLD_NODE.below(),ThaumcraftNodeDevices.NODE_STABILIZER.get())+blockCount(level,SOURCE.below(),ThaumcraftNodeDevices.NODE_STABILIZER.get());
            int upper=count(values,converterItem())+blockCount(level,OLD_NODE.above(),ThaumcraftNodeDevices.NODE_TRANSDUCER.get())+blockCount(level,SOURCE.above(),ThaumcraftNodeDevices.NODE_TRANSDUCER.get());
            int relay=count(values,relayItem())+blockCount(level,RELAY,ThaumcraftNodeDevices.VIS_RELAY.get());
            require(lower==1 && upper==1 && relay==1,"Every earned power-device owner conserved through item/world/UUID pickup, never granted");
            TileFocalManipulator machine=loadedBE(level,MACHINE) instanceof TileFocalManipulator f?f:null;
            require(machine==null || !machine.isQuarantined(),"Machine never quarantined");
            require(count(values,ThaumcraftFoci.EXCAVATION_FOCUS.get())+values.stream().filter(FocalManipulatorClientSequence::payer).map(FocusData::get).filter(s->s.is(ThaumcraftFoci.EXCAVATION_FOCUS.get())).mapToInt(ItemStack::getCount).sum()==1,"ONE saved Excavation owner, loose/table/cursor/equipped, never a cloned copy");
            ItemStack focus=machine==null?ItemStack.EMPTY:machine.getItem(0).copy();
            if(focus.isEmpty())for(ItemStack value:values) {
                if(value.is(ThaumcraftFoci.EXCAVATION_FOCUS.get()))focus=value.copy();
                if(payer(value) && !FocusData.get(value).isEmpty())focus=FocusData.get(value);
            }
            require(!focus.isEmpty() && sameFocusShell(focus,savedFocus),"Same actual saved focus components except earned upgrade");
            TileNodeConverter converter=loadedBE(level,SOURCE.above()) instanceof TileNodeConverter c?c:null;
            TileNodeEnergized source=loadedBE(level,SOURCE) instanceof TileNodeEnergized s?s:null;
            TileNode ordinary=loadedBE(level,SOURCE) instanceof TileNode n?n:null;
            require(source!=null || ordinary!=null,"Healthy1623 remains an actual ordinary or energized node");
            if(ordinary!=null)require(amounts(ordinary.getAspectsBase()).equals(BASE) && ordinary.getScanId().equals(healthyId),"Healthy node all6 BASE80/actual initial UUID retained until native conversion");
            if(source!=null)require(!source.isQuarantined() && amounts(source.getAuraBase()).equals(BASE) && amounts(source.getVisBase()).values().stream().allMatch(n->n==8) && amounts(source.getVisBase()).size()==6,"Genuine converted BASE80/source8 all6");
            require(!level.hasNeighborSignal(SOURCE.below()),"Lower stabilizer NEVER powered");
            boolean relayThere=loadedBE(level,RELAY) instanceof TileVisRelay;
            // Revision field read is deliberately PURE: server getSelectionRevision
            // observes external edits and can cancel jobs; a ledger must not do that.
            long revision=machine==null?-1:((Number)field(machine,"revision")).longValue();
            return new View(level.getGameTime(),player.experienceLevel,player.totalExperience,player.experienceProgress,xp.credits(),mass,made,outputs,charmCrafts,machineOwners,
                    ItemWandCasting.data(actualPayer).vis(),machine==null?0:machine.getSize(),machine==null?-1:machine.getRank(),machine==null?-1:machine.getUpgrade(),
                    machine==null?Map.of():amountsIncludingZero(machine.getAspects()),focus,revision,source,source==null?-1:source.getRefillEpoch(),source==null?Map.of():amounts(source.getAvailableVis()),
                    relayThere,converter!=null && converter.isPowered(),converter==null?-1:converter.getCount(),converter==null?-1:converter.getStatus(),ordinary==null?-1:ordinary.getTickCount(),player.isUsingItem());
        }

        private void observeEpoch(View before,View after,Map<String,Integer> paid) {
            if(after.source()==null) { require(paid.isEmpty(),"No grants without actual source"); return; }
            Map<String,Integer> capacity=amounts(after.source().getVisBase());
            if(before.source()!=after.source()) {
                require(before.source()==null && after.epoch()==0 && after.available().isEmpty() && paid.isEmpty(),"Native source starts empty, never fabricated/refilled by observer");
                actions.add(row("event","native ordinary-to-energized conversion","tick",after.tick(),"actual_converter",List.of(after.converterCount(),after.converterStatus()),"actual_BASE",amounts(after.source().getAuraBase()),"actual_first_epoch",after.epoch()));
                return;
            }
            if(!paid.isEmpty()) {
                var sources=VisNetHandler.getSources(player.serverLevel()).stream().map(java.lang.ref.WeakReference::get).filter(java.util.Objects::nonNull).toList();
                require(sources.size()==1 && sources.getFirst()==after.source(),"Exactly this real source, no other provider may mask its grants");
                var relay=(TileVisRelay)loadedBE(player.serverLevel(),RELAY);var parent=relay.getParent();var root=relay.getRootSource();
                require(parent!=null && parent.get()==after.source() && root!=null && root.get()==after.source(),"Actual native reciprocal relay path roots at actual1623 source");
            }
            long change=after.epoch()-before.epoch(); require(change>=0 && change<=1,"Only one native source refill per POST");
            var order=new LinkedHashMap<String,String>();
            for(String primal:WandData.PRIMALS) {
                int grant=paid.getOrDefault(primal,0),old=before.available().getOrDefault(primal,0),now=after.available().getOrDefault(primal,0),cap=capacity.get(primal);
                if(change==0) { require(now==old-grant && grant<=old,"Same real source epoch residual conservation "+primal); lastEpochPaid.merge(primal,grant,Integer::sum); order.put(primal,"SAME_EPOCH"); }
                else {
                    boolean afterRefill=now==cap-grant,priorEpoch=now==cap && grant<=old;
                    require(afterRefill || priorEpoch,"Actual available pulse identifies native grant/refill ordering "+primal);
                    int oldGrant=afterRefill?0:grant;
                    require(lastEpochPaid.getOrDefault(primal,0)+old== (before.epoch()==0?0:cap),"Old epoch prior grants+available equals capacity");
                    unused.merge(primal,(long)old-oldGrant,Long::sum); generated.merge(primal,(long)cap,Long::sum);
                    lastEpochPaid.put(primal,afterRefill?grant:0); order.put(primal,grant==0?"ZERO_GRANT_ORDER_UNOBSERVABLE":afterRefill?"REFILL_BEFORE_GRANT":"GRANT_BEFORE_REFILL");
                }
                require(generated.getOrDefault(primal,0L)==grants.getOrDefault(primal,0)+unused.getOrDefault(primal,0L)+now,"ALL native generated pulse = actual job grants + unused replaced + live residual "+primal);
            }
            epochs.add(row("tick",after.tick(),"actual_epoch_before_after",List.of(before.epoch(),after.epoch()),"available_before",before.available(),"available_after",after.available(),"actual_THIS_POST_grants",paid,"actual_order",order,"cumulative_grants",Map.copyOf(grants),"cumulative_generated",Map.copyOf(generated),"cumulative_unused_replaced",Map.copyOf(unused)));
        }
        private void observeOldNode(ServerLevel level,long tick) {
            BlockEntity be=loadedBE(level,OLD_NODE);
            var row=new LinkedHashMap<String,Object>(); row.put("tick",tick);
            if(be instanceof TileNode node) {
                NodeState now=new NodeState(node.getTickCount(),amountsIncludingZero(node.getAspects()),amountsIncludingZero(node.getAspectsBase()),node.getScanId());
                if(oldIdentity!=node) { oldNodeRows.add(row("event","actual loaded ordinary identity; transient count observed, never set","tick",tick,"actual_count",now.count(),"actual_BASE",now.base(),"actual_current",now.current()));previousOld=null;oldIdentity=node; }
                if(previousOld!=null) {
                    int delta=now.count()-previousOld.count();require(delta==0 || delta==1,"Observe real transient node phase, never pretend1091continued or reset it");
                    if(!now.base().equals(previousOld.base())) {
                        require(delta==1 && now.count()%1200==0 && now.id().equals(previousOld.id()),"Old1551 BASE change must be actual native1200 prune boundary");sawOldPrune=true;
                        for(String key:previousOld.base().keySet()) {
                            int lost=previousOld.base().get(key)-now.base().getOrDefault(key,0);
                            require(lost==0 || lost==1 && previousOld.current().getOrDefault(key,0)==0,"Native old-node prune only decreases an actually ZERO current key by1");
                        }
                        oldNodeRows.add(row("event","ACTUAL native1200 boundary; explicitly tracked, NOT a late BASE waiver","tick",tick,"count",now.count(),"before",previousOld.toString(),"after",now.toString()));
                    }
                }
                previousOld=now;
                row.put("kind","ORDINARY"); row.put("count",node.getTickCount()); row.put("current_INCLUDING_ZERO",amountsIncludingZero(node.getAspects())); row.put("BASE",amountsIncludingZero(node.getAspectsBase())); row.put("UUID",node.getScanId());
                row.put("actual_type",node.getNodeType().name());row.put("actual_modifier",node.getNodeModifier()==null?"NONE":node.getNodeModifier().name());
                row.put("actual_cached_lock",node.getLock());row.put("actual_cached_regeneration",node.getRegenerationTicks());row.put("actual_recharge_wait",node.getRechargeWait());
            } else { row.put("kind",be==null?"NO_LOADED_BE":be.getClass().getName());previousOld=null;oldIdentity=null; }
            String signature=row.entrySet().stream().filter(e->!e.getKey().equals("tick") && !e.getKey().equals("count")).toList().toString();
            if(!signature.equals(oldNodeSignature) || checks%100==0) { oldNodeRows.add(Map.copyOf(row)); oldNodeSignature=signature; }
        }
    }
    private record NodeState(int count,Map<String,Integer> current,Map<String,Integer> base,String id) {}

    private final class Charge {
        final ServerPlayer player;
        final ItemStack wand,original;
        final TileNode source;
        WandData previous;
        Map<String,Integer> previousSource;
        final Map<String,Integer> gained=new LinkedHashMap<>(),units=new LinkedHashMap<>(),regained=new LinkedHashMap<>();
        volatile Map<String,Integer> lastGains=Map.of();
        volatile boolean active=true,funded,used;
        volatile int stopped;
        Charge(ServerPlayer player) {
            this.player=player; wand=player.getMainHandItem(); original=wand.copy(); previous=ItemWandCasting.data(wand);
            require(player.getInventory().selected==HAND && payer(wand) && ItemStack.matches(wand,savedPayer) && !player.isUsingItem() && !player.isShiftKeyDown(),"Actual selected payer hand ACK before ChargeProbe");
            source=(TileNode)player.level().getBlockEntity(SOURCE); previousSource=amounts(source.getAspects());
            require(KnowledgeAccess.get(player).isResearchComplete("NODEPRESERVE"),"Non-sneak native preserve research");
            for(String type:WandData.PRIMALS)require(previousSource.getOrDefault(type,0)-1>=(5143-previous.amount(type)+99)/100,"Actual finite preserve budget leaves>=1 "+type);
        }
        void observe() {
            boolean sameHand=player.getMainHandItem()==wand,sameShell=samePayerShell(wand,original),emptyFocus=FocusData.get(wand).isEmpty(),sameSource=player.level().getBlockEntity(SOURCE)==source;
            if(!(sameHand && sameShell && emptyFocus && sameSource)) {
                actions.add(row("event","STRICT charging binding failure witness","server_tick",player.server.getTickCount(),"selected_slot",player.getInventory().selected,
                        "same_hand_object",sameHand,"same_original_shell",sameShell,"empty_focus",emptyFocus,"same_source_object",sameSource,
                        "using",player.isUsingItem(),"use_item_same_wand",player.getUseItem()==wand,"actual_hand",snbt(player.getMainHandItem(),player),"bound_wand",snbt(wand,player)));
                throw new IllegalStateException("Bound actual charging wand/source/hand: slot="+player.getInventory().selected+", sameHand="+sameHand+", sameShell="+sameShell+", emptyFocus="+emptyFocus+", sameSource="+sameSource);
            }
            WandData now=ItemWandCasting.data(wand); Map<String,Integer> current=amounts(source.getAspects()); var delta=new LinkedHashMap<String,Integer>(); int taps=0,regen=0;
            for(String type:WandData.PRIMALS) {
                int amount=now.amount(type)-previous.amount(type); require(amount>=0 && amount<=100 && (amount==0 || amount==100 || now.amount(type)==now.capacity()),"Native whole-unit or capped partial tap");
                int spent=(amount+99)/100,natural=current.getOrDefault(type,0)-previousSource.getOrDefault(type,0)+spent;
                require(natural>=0 && natural<=1 && current.getOrDefault(type,0)>=1,"Only native regeneration and actual preserve tap; never zero-key prune source while charging");
                if(amount>0) { gained.merge(type,amount,Integer::sum); delta.put(type,amount); } if(spent>0)units.merge(type,spent,Integer::sum); if(natural>0)regained.merge(type,natural,Integer::sum);
                taps+=spent; regen+=natural;
            }
            require(taps<=1 && regen<=1,"One genuine tap/regain maximum per actual POST");
            lastGains=Map.copyOf(delta); funded=WandData.PRIMALS.stream().allMatch(type->now.amount(type)>=5143);
            if(player.isUsingItem()) { require(ItemWandCasting.isUsingNode(player) && player.getUseItem()==wand && sessionNode(player,wand).equals(SOURCE),"Native server node-session acknowledgement, not direct charge calls"); used=true; }
            else if(used && !funded) {
                Object session;
                try { session=((Map<?,?>)fieldStatic(ItemWandCasting.class,"SERVER_NODE_USES")).get(player); }
                catch(ReflectiveOperationException error) { session="READ_ONLY_SCHEMA_ERROR: "+error; }
                var ray=player.pick(player.blockInteractionRange(),1.0F,true);
                actions.add(row("event","STRICT unexpected unfunded native charge-session loss","game_tick",player.level().getGameTime(),
                        "server_tick",player.server.getTickCount(),"selected_slot",player.getInventory().selected,"same_hand",player.getMainHandItem()==wand,
                        "using",player.isUsingItem(),"remaining_use_ticks",player.getUseItemRemainingTicks(),"use_item_same_wand",player.getUseItem()==wand,
                        "actual_server_session",session==null?"NONE":session.toString(),"actual_native_pick",ray.toString(),
                        "position",player.position().toString(),"rotation",List.of(player.getYRot(),player.getXRot()),"client_published_input_snapshot",clientInputSnapshot));
                throw new IllegalStateException("Native charge session ended before funds were earned; never auto-reselect/restart/refill");
            }
            stopped=used && !player.isUsingItem()?stopped+1:0;
            if(!delta.isEmpty() || regen>0)actions.add(row("event","actual native wand/source tap","tick",player.level().getGameTime(),"received",lastGains,"source_before",previousSource,"source_after",current,"whole_units_so_far",Map.copyOf(units),"regeneration_so_far",Map.copyOf(regained)));
            previous=now; previousSource=current;
        }
        void finish() {
            require(used && funded && stopped>=2 && !player.isUsingItem() && !ItemWandCasting.isUsingNode(player),"Genuine charged budget and released session"); active=false;
            var rounding=new LinkedHashMap<String,Integer>(); for(String type:WandData.PRIMALS)rounding.put(type,units.getOrDefault(type,0)*100-gained.getOrDefault(type,0));
            actions.add(row("event","native charge completed","actual_gains",Map.copyOf(gained),"actual_whole_source_units",Map.copyOf(units),"actual_regeneration",Map.copyOf(regained),"actual_cap_rounding",rounding,"overshoot_refunds",0,"payer_after",snbt(wand,player)));
        }
    }

    private void serverPost(MinecraftServer server) {
        if(owner==null || server!=owner.server || inert || stopped)return;
        try { if(charge!=null && charge.active)charge.observe(); ledger.observe(); }
        catch(Throwable error) { fail(error); } // NEVER self-unregister from Architectury's live list.
    }
    private EventResult entityAdded(Entity entity,Level level) {
        if(owner==null || level!=owner.level() || inert || stopped)return EventResult.pass();
        try {
            if(entity instanceof ExperienceOrb orb && SCENE.intersects(orb.getBoundingBox()))xp.added(orb);
            if(entity instanceof ItemEntity item && (SCENE.contains(item.position()) || OLD_AREA.contains(item.position()))) {
                ItemStack stack=item.getItem();
                if(miningDevice>=0 && miningDevice<2 && OLD_AREA.contains(item.position()) && stack.is(devices().get(miningDevice).item())) {
                    require(stack.getCount()==1 && !deviceDrops.containsKey(miningDevice),"ONE genuine earned-device native ADD"); deviceDrops.put(miningDevice,item.getUUID()); drops.put(item.getUUID(),stack.copy());
                } else if(casting && stack.is(Items.COBBLESTONE) && item.distanceToSqr(Vec3.atCenterOf(STONE))<9) {
                    require(stack.getCount()==1 && cobbleId==null,"ONE genuine Potency stone output UUID"); cobbleId=item.getUUID(); drops.put(item.getUUID(),stack.copy());
                } else throw new IllegalStateException("Unclassified new physical scene output: "+stack);
                physical.add(row("event","actual native item ADD","tick",level.getGameTime(),"UUID",item.getUUID().toString(),"stack",snbt(stack,owner),"position",item.position().toString(),"velocity",item.getDeltaMovement().toString(),"item_mutations",0));
            }
        } catch(Throwable error) { fail(error); }
        return EventResult.pass();
    }
    private EventResult beforeBreak(Level level,BlockPos pos,BlockState state,Player player) {
        if(owner==null || player!=owner || level!=owner.level() || !pos.equals(STONE) || inert)return EventResult.pass();
        try { breakCalls++; require(casting && breakCalls==1 && ledger!=null && currentCastDebit()==13,"Actual Architectury protection sees paid attempt ONCE"); }
        catch(Throwable error) { fail(error); }
        return EventResult.pass(); // Observe; do not override another protection listener.
    }
    private int castTerraBefore;
    private float lastCastProgress=-1;
    private int castProgressSamples;
    private void prepareCast(ServerPlayer p) throws ReflectiveOperationException {
        require(p.getInventory().selected==HAND && payer(p.getMainHandItem()) && ItemStack.matches(FocusData.get(p.getMainHandItem()),upgradedFocus),"Actual native focus-selection C2S ACK");
        require(p.getEyePosition().distanceTo(Vec3.atCenterOf(STONE))>4.5 && p.getEyePosition().distanceTo(Vec3.atCenterOf(STONE))<10,"Real spell reach, not ordinary mining");
        castTerraBefore=ItemWandCasting.getVis(p.getMainHandItem(),"terra");
        require(castTerraBefore>=13 && p.level().getBlockState(STONE).is(Blocks.STONE) && p.level().getBlockState(STONE).getDestroySpeed(p.level(),STONE)==1.5F,"Declared real stone hardness / finite cast funds");
        protectionTaps.add(new NativeEventTap(PlayerBlockBreakEvents.BEFORE,"net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents$Before",false));
        protectionTaps.add(new NativeEventTap(PlayerBlockBreakEvents.AFTER,"net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents$After",true));
    }
    private int currentCastDebit() {
        ItemStack actual=owner.getMainHandItem(); return castTerraBefore-ItemWandCasting.getVis(actual,"terra");
    }
    private void observeCastProgress(ServerPlayer p) {
        var progress=ItemFocusExcavation.progress(p); if(progress==null || !progress.pos().equals(STONE))return;
        require(progress.amount()==(castProgressSamples==0?0:lastCastProgress+.5F),"Actual one-per-native-tick .5 Potency trajectory, no reset/jump/manual tick");
        lastCastProgress=progress.amount();castProgressSamples++;
        if(progress.amount()==0)sawZero=true;
        if(progress.amount()==.5F) { require(sawZero,"First-coordinate tick remains zero before Potency .5 increment"); sawHalf=true; }
        require(progress.amount()==0 || progress.amount()==.5F || progress.amount()==1 || progress.amount()==1.5F,"Only original preincrement Potency1 .5 progress");
        actions.add(row("event","actual native Potency coordinate progress","tick",p.level().getGameTime(),"target",progress.pos().toShortString(),"actual_amount",progress.amount(),"sample",castProgressSamples,"terra_debit_so_far",currentCastDebit()));
    }
    private final class NativeEventTap implements AutoCloseable {
        final Object event,tap,lock;final Field handlers,phaseListeners;final Method rebuild;
        NativeEventTap(Object event,String interfaceName,boolean after) throws ReflectiveOperationException {
            this.event=event;Class<?> implementation=Class.forName("net.fabricmc.fabric.impl.base.event.ArrayBackedEvent");
            require(implementation.isInstance(event),"Pinned Fabric array-backed native event");
            Field lockField=implementation.getDeclaredField("lock");lockField.setAccessible(true);lock=lockField.get(event);
            handlers=implementation.getDeclaredField("handlers");handlers.setAccessible(true);
            phaseListeners=Class.forName("net.fabricmc.fabric.impl.base.event.EventPhaseData").getDeclaredField("listeners");phaseListeners.setAccessible(true);
            rebuild=implementation.getDeclaredMethod("rebuildInvoker",int.class);rebuild.setAccessible(true);
            Class<?> callback=Class.forName(interfaceName);
            tap=Proxy.newProxyInstance(callback.getClassLoader(),new Class<?>[]{callback},(proxy,method,args)->{
                if(method.getDeclaringClass()==Object.class)return switch(method.getName()) {
                    case "equals"->proxy==args[0];case "hashCode"->System.identityHashCode(proxy);default->"Focal read-only native callback";};
                if(args!=null && args.length==5 && args[0]==owner.level() && args[1]==owner && STONE.equals(args[2]) && !inert) {
                    try { if(after)afterCalls++;else beforeCalls++; require(casting && currentCastDebit()==13 && (after?afterCalls:beforeCalls)==1,"Native Fabric protection callback exact paid stone attempt"); }
                    catch(Throwable error) { fail(error); }
                }
                return after?null:true; // Normal real registered listener: do not bypass others' cancellation.
            });
            synchronized(lock) { Class.forName("net.fabricmc.fabric.api.event.Event").getMethod("register",Object.class).invoke(event,tap); }
        }
        @Override public void close() throws ReflectiveOperationException {
            synchronized(lock) {
                Field phases=event.getClass().getDeclaredField("phases");phases.setAccessible(true);int removed=0;
                for(Object phase:((Map<?,?>)phases.get(event)).values()) {
                    Object old=phaseListeners.get(phase);int length=java.lang.reflect.Array.getLength(old),index=-1;
                    for(int i=0;i<length;i++)if(java.lang.reflect.Array.get(old,i)==tap) { require(index==-1,"Owned native callback registered once");index=i; }
                    if(index<0)continue;Object kept=java.lang.reflect.Array.newInstance(old.getClass().getComponentType(),length-1);int next=0;
                    for(int i=0;i<length;i++)if(i!=index)java.lang.reflect.Array.set(kept,next++,java.lang.reflect.Array.get(old,i));
                    phaseListeners.set(phase,kept);removed++;
                }
                require(removed==1,"Identity-only removal of this native listener");
                rebuild.invoke(event,java.lang.reflect.Array.getLength(handlers.get(event))-removed);
                for(Object item:(Object[])handlers.get(event))require(item!=tap,"Owned native callback actually retired");
            }
        }
    }

    private void snapshotOld(ServerLevel level) {
        // Read ONLY already loaded chunks; never activate an old workshop merely
        // to manufacture a live preservation claim. Root audits ALL stopped NBT.
        int jars=0,essentia=0,candles=0;
        for(int x=1462>>4;x<=1946>>4;x++)for(int z=359>>4;z<=391>>4;z++) {
            var chunk=level.getChunkSource().getChunkNow(x,z); if(chunk==null)continue;
            for(var entry:chunk.getBlockEntities().entrySet()) {
                BlockPos pos=entry.getKey(); BlockEntity be=entry.getValue(); oldBEs.put(pos.immutable(),tag(be));
                if(be instanceof TileJar jar) { jars++;essentia+=jar.getAmount(); }
            }
            for(int bx=x*16;bx<x*16+16;bx++)for(int bz=z*16;bz<z*16+16;bz++)for(int y=214;y<=225;y++) {
                BlockPos pos=new BlockPos(bx,y,bz); BlockState state=chunk.getBlockState(pos);
                if(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals("thaumcraft"))oldBlocks.put(pos,state);
                if(state.is(ThaumcraftInfusion.TALLOW_CANDLE.get()))candles++;
            }
        }
        for(Entity entity:level.getAllEntities())if(entity!=owner) {
            inheritedEntities.put(entity.getUUID(),entity);if(entity instanceof LivingEntity living)savedLiving.put(entity.getUUID(),livingView(living));
            if(entity instanceof net.minecraft.world.entity.Mob mob && !mob.hasCustomName() && !mob.isNoAi() && !mob.isPersistenceRequired()
                    && outsideOwnedCells(entity.getBoundingBox()))ambientEligible.add(entity.getUUID());
        }
        for(Vec3 position:List.of(new Vec3(1561.5,216,386.5),new Vec3(1596.5,216,381.5),new Vec3(1628.5,216,369.5))) {
            var matches=level.getEntitiesOfClass(LivingEntity.class,new AABB(position,position.add(.01,2,.01)).inflate(.4),entity->entity.hasCustomName() && entity.getType()==net.minecraft.world.entity.EntityType.COW && entity.position().equals(position) && entity.getHealth()==6);
            require(matches.size()==1,"Every actual old named cow health6/position must remain a non-target input: "+position);
        }
        require(level.getEntitiesOfClass(ItemEntity.class,SCENE).isEmpty() && level.getEntitiesOfClass(ExperienceOrb.class,SCENE).isEmpty()
                && level.getEntitiesOfClass(ItemEntity.class,OLD_AREA).isEmpty() && level.getEntitiesOfClass(ExperienceOrb.class,OLD_AREA).isEmpty(),"Refuse inherited loose XP/items in owned action areas; never clear them");
        require(pendingRows(level).isEmpty(),"Accepted queue empty; observation does not call save()/pendingCount()/reconcile");
        report.put("initial_loaded_preservation_subset",row("old_non_node_and_node_BEs",oldBEs.size(),"loaded_jars_units",List.of(jars,essentia),"loaded_candles",candles,"expected_ALL_stopped_jars_units_candles",List.of(20,333,72),"forced_chunk_loads",0,"unloaded_ALL187file_manifest_audit","ROOT_REQUIRED"));
        report.put("all_initial_loaded_living_LEFT_UNTOUCHED",savedLiving.entrySet().stream().map(e->row("UUID",e.getKey().toString(),"native",e.getValue())).toList());
        report.put("ALL_initial_loaded_entity_UUIDs_LEFT_UNTOUCHED",inheritedEntities.keySet().stream().map(UUID::toString).toList());
    }
    private static Map<String,Object> livingView(LivingEntity entity) {
        boolean mob=entity instanceof net.minecraft.world.entity.Mob;
        AABB body=entity.getBoundingBox();
        return row("type",BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(),"position",entity.position().toString(),
                "health",entity.getHealth(),"name",entity.getName().getString(),"named",entity.hasCustomName(),"Mob",mob,
                "NoAI",mob && ((net.minecraft.world.entity.Mob)entity).isNoAi(),
                "PersistenceRequired",mob && ((net.minecraft.world.entity.Mob)entity).isPersistenceRequired(),"Invulnerable",entity.isInvulnerable(),
                "bounding_box",List.of(body.minX,body.minY,body.minZ,body.maxX,body.maxY,body.maxZ));
    }
    private boolean outsideOwnedCells(AABB body) {
        if(SCENE.intersects(body) || OLD_AREA.intersects(body))return false;
        for(BlockPos pos:oldBEs.keySet())if(body.intersects(new AABB(pos)))return false;
        for(BlockPos pos:oldBlocks.keySet())if(body.intersects(new AABB(pos)))return false;
        for(BlockPos pos:fixtureBlocks.keySet())if(body.intersects(new AABB(pos)))return false;
        for(BlockPos pos:List.of(SOURCE,MACHINE,RELAY,LEVER,OLD_NODE,OLD_NODE.above(),OLD_NODE.below()))
            if(body.intersects(new AABB(pos)))return false;
        return true;
    }
    private void preserve(ServerPlayer p,boolean terminal) {
        List<ItemStack> values=physicalStacks(p);
        for(var entry:savedInventory.entrySet()) {
            ItemStack saved=entry.getValue();
            if(entry.getKey()==10 || saved.is(Items.WOODEN_PICKAXE) || saved.is(ThaumcraftFoci.EXCAVATION_FOCUS.get()) || saved.is(relayItem()))continue;
            int expected=savedInventory.values().stream().filter(s->ItemStack.isSameItemSameComponents(s,saved)).mapToInt(ItemStack::getCount).sum();
            if(saved.is(Items.COBBLESTONE))expected+=castPayments; // Preserve old3 PLUS the one actual paid physical output.
            require(values.stream().filter(s->ItemStack.isSameItemSameComponents(s,saved)).mapToInt(ItemStack::getCount).sum()==expected,"EVERY old stack component/count retained, not replaced by new fixtures: "+saved);
        }
        for(int i=0;i<ARMOR.size();i++)require(ItemStack.matches(p.getItemBySlot(ARMOR.get(i)),savedArmor.get(i)),"All saved worn gear full components unchanged");
        require(ItemStack.matches(p.getOffhandItem(),savedOffhand) && p.getHealth()>0 && (p.isAlive() || storyFinal && p.hasDisconnected()) && p.isInvulnerable()==savedInvulnerable && p.gameMode.getGameModeForPlayer()==GameType.SURVIVAL,"Old offhand/health-mode/entity flag preserved; only AFTER completion may native normal disconnect retire the player");
        require(knowledge.equals(KnowledgeAccess.get(p).save()),"All knowledge/pools/scans/warp exact except only declared primary fixture");
        ItemStack pick=values.stream().filter(s->s.is(Items.WOODEN_PICKAXE)).findFirst().orElseThrow();
        int damage=pick.getDamageValue(); require(damage>=3 && damage<=5 && sameExcept(pick,savedPick,DataComponents.DAMAGE),"Only two native device mines may alter old pick durability3->5");
        for(var entry:savedLiving.entrySet()) {
            Entity entity=p.serverLevel().getEntity(entry.getKey());
            if(entity instanceof LivingEntity living && (Boolean)entry.getValue().get("named") && entry.getValue().get("type").equals("minecraft:cow"))
                require(livingView(living).equals(entry.getValue()),"Old named cow exact health/position/AI/entity flag, never a target");
        }
        for(Entity entity:inheritedEntities.values())require(!entity.isRemoved() || entity.getRemovalReason()!=null && !entity.getRemovalReason().shouldDestroy()
                || provenAmbientRemovals.contains(entity.getUUID()),"Unproven inherited removal: UUID="+entity.getUUID()+", type="+entity.getType()+", reason="+entity.getRemovalReason()+", position="+entity.position()+"; never restore/clear an entity");
        for(var entry:oldBEs.entrySet()) {
            BlockPos pos=entry.getKey(); if(pos.equals(OLD_NODE.above()) || pos.equals(OLD_NODE.below()) || pos.equals(OLD_NODE) || pos.equals(SOURCE))continue;
            var chunk=p.serverLevel().getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4); if(chunk==null)continue;
            BlockEntity be=chunk.getBlockEntities().get(pos); require(be!=null,"All other loaded old BE positions preserved");
            CompoundTag before=entry.getValue().copy(),after=tag(be);
            if(be instanceof TileNode) { before.remove("Aspects");after.remove("Aspects");before.remove("lastActive");after.remove("lastActive"); }
            if(pos.equals(new BlockPos(1882,218,377))) { before.remove("samount");after.remove("samount"); }
            require(before.equals(after),"No old BE stock/metadata/furnace clock mutation (only documented native node/valve dynamics): "+pos);
            if(be instanceof TileAlchemyFurnace furnace)require(furnace.getBurnTime()==0,"All old alchemy furnaces remain B0");
        }
        for(var entry:oldBlocks.entrySet()) {
            BlockPos pos=entry.getKey(); if(pos.equals(OLD_NODE.above()) || pos.equals(OLD_NODE.below()) || pos.equals(OLD_NODE) || pos.equals(SOURCE))continue;
            var chunk=p.serverLevel().getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4); if(chunk!=null)require(chunk.getBlockState(pos)==entry.getValue(),"All other loaded old TC blocks/candles untouched: "+pos);
        }
        for(var entry:fixtureBlocks.entrySet()) {
            if(entry.getKey().equals(STONE) && castPayments==1)require(loadedBlock(p.serverLevel(),STONE).isAir(),"Only ONE paid stone fixture may be removed");
            else {
                BlockState actual=loadedBlock(p.serverLevel(),entry.getKey());
                if(entry.getValue().is(Blocks.FURNACE))require(actual.is(Blocks.FURNACE),"Native furnace fixture stays same block (native LIT transitions allowed)");
                else require(actual==entry.getValue(),"All other declared empty/mundane fixture cells remain exact, never later rewritten: "+entry.getKey());
            }
        }
        if(terminal)require(damage==5,"Exactly two real device mines, original pick3->5");
    }

    private void checkRaw(List<ItemStack> values,int charmCrafts,int machines) {
        for(var entry:declared.entrySet()) {
            Item item=entry.getKey(); int owned=count(values,item),spent=0;
            if(item==Items.RAW_GOLD || item==Items.COAL)continue;
            if(item==Items.LEVER) { owned+=loadedBlock(owner.serverLevel(),LEVER).is(Blocks.LEVER)?1:0; }
            else if(item==ThaumcraftMasonry.ARCANE_STONE.get().asItem())spent=2*machines;
            else if(item==ThaumcraftMasonry.SLAB.get().asItem() || item==ThaumcraftContent.TABLE_ITEM.get())spent=machines;
            else if(item==Items.IRON_INGOT)spent=2*machines;
            else spent=charmCrafts; // Exactly six primal + one already-crafted balanced shard.
            int baseline=savedInventory.values().stream().filter(s->s.is(item)).mapToInt(ItemStack::getCount).sum();
            require(owned==baseline+entry.getValue()-spent,"Exact declared native raw ingredient ownership/consumption: "+BuiltInRegistries.ITEM.getKey(item));
        }
    }
    private List<ItemStack> physicalStacks(ServerPlayer p) {
        var result=new ArrayList<ItemStack>(); result.addAll(p.getInventory().items); result.addAll(p.getInventory().offhand); result.add(p.containerMenu.getCarried());
        if(loadedBE(p.serverLevel(),BENCH) instanceof TileArcaneWorkbench bench)for(int i=0;i<11;i++)if(i!=9)result.add(bench.getItem(i));
        if(loadedBE(p.serverLevel(),MACHINE) instanceof TileFocalManipulator machine)result.add(machine.getItem(0));
        for(BlockPos pos:FURNACES) { FurnaceBlockEntity f=furnace(p.serverLevel(),pos); for(int i=0;i<3;i++)result.add(f.getItem(i)); }
        var worldIds=new HashSet<UUID>(); for(AABB area:List.of(SCENE,OLD_AREA))for(ItemEntity item:p.serverLevel().getEntitiesOfClass(ItemEntity.class,area))if(worldIds.add(item.getUUID()))result.add(item.getItem());
        result.addAll(pendingRows(p.serverLevel())); return result;
    }
    private int countOwned(ServerPlayer p,Item item) { return count(physicalStacks(p),item); }
    @SuppressWarnings("unchecked") private List<ItemStack> pendingRows(ServerLevel level) {
        try {
            Field factoryField=DeferredItemDrops.class.getDeclaredField("FACTORY"); factoryField.setAccessible(true);
            var factory=(SavedData.Factory<DeferredItemDrops>)factoryField.get(null);
            DeferredItemDrops queue=level.getDataStorage().get(factory,"thaumcraft_pending_inventory_drops"); if(queue==null)return List.of();
            require(queue.quarantinedCount()==0,"No quarantined pending output");
            Set<UUID> settled=(Set<UUID>)field(queue,"settled"); var rows=new ArrayList<Object>(); for(Object entry:(Iterable<?>)field(queue,"pending"))rows.add(entry);
            Object flight=field(queue,"inFlight"); if(flight!=null && !(Boolean)field(flight,"accepted"))rows.add(field(flight,"entry"));
            var ids=new HashSet<UUID>(); var stacks=new ArrayList<ItemStack>();
            for(Object entry:rows) {
                UUID id=(UUID)field(entry,"id"); if(settled.contains(id) || !ids.add(id))continue;
                BlockPos pos=(BlockPos)field(entry,"source"); if(!SCENE.contains(Vec3.atCenterOf(pos)) && !OLD_AREA.contains(Vec3.atCenterOf(pos)))continue;
                ItemStack stack=(ItemStack)field(entry,"stack");
                if(level.getEntity(id) instanceof ItemEntity live && !live.isRemoved() && ItemStack.matches(live.getItem(),stack))continue;
                stacks.add(stack.copy());
            }
            return List.copyOf(stacks);
        } catch(ReflectiveOperationException error) { throw new IllegalStateException("Pure queue schema changed; never call reconcile/save as an observer",error); }
    }

    private record Ingredient(Item item,int amount,int destination) {}
    private List<Click> ingredientClicks(List<Ingredient> wanted) {
        var list=new ArrayList<Click>(); var available=new LinkedHashMap<Integer,Integer>();
        for(int slot=0;slot<36;slot++)available.put(slot,client.player.getInventory().getItem(slot).getCount());
        for(Ingredient ingredient:wanted) {
            int left=ingredient.amount();
            for(int slot=0;slot<36 && left>0;slot++) {
                ItemStack stack=client.player.getInventory().getItem(slot); int n=Math.min(left,available.get(slot)); if(n<=0 || !stack.is(ingredient.item()))continue;
                list.add(new Click(playerSlot(slot),0)); for(int i=0;i<n;i++)list.add(new Click(ingredient.destination(),1)); list.add(new Click(playerSlot(slot),0));
                available.put(slot,available.get(slot)-n); left-=n;
            }
            require(left==0,"Actual native inventory covers exact ingredient quantity");
        }
        return List.copyOf(list);
    }
    private List<Click> recipeClicks() {
        var list=new ArrayList<Click>(); var groups=new LinkedHashMap<Item,List<Integer>>();
        for(int cell=0;cell<9;cell++)groups.computeIfAbsent(recipeItem(cell),ignored->new ArrayList<>()).add(cell+2);
        for(var group:groups.entrySet()) {
            int used=0;
            for(int slot=0;slot<36 && used<group.getValue().size();slot++) {
                ItemStack stack=client.player.getInventory().getItem(slot); if(!stack.is(group.getKey()))continue;
                int n=Math.min(stack.getCount(),group.getValue().size()-used); list.add(new Click(playerSlot(slot),0));
                for(int i=0;i<n;i++)list.add(new Click(group.getValue().get(used++),1)); list.add(new Click(playerSlot(slot),0));
            }
            require(used==group.getValue().size(),"No fabricated recipe inputs");
        }
        return List.copyOf(list);
    }
    private Item recipeItem(int cell) {
        if(craftIndex==0)return switch(cell) {
            case 0->ThaumcraftContent.SHARDS.get("aer").get();case 1->ThaumcraftContent.SHARDS.get("ignis").get();case 2->ThaumcraftContent.SHARDS.get("aqua").get();
            case 3,5->Items.GOLD_INGOT;case 4->ThaumcraftContent.SHARDS.get("balanced").get();case 6->ThaumcraftContent.SHARDS.get("terra").get();
            case 7->ThaumcraftContent.SHARDS.get("ordo").get();case 8->ThaumcraftContent.SHARDS.get("perditio").get();default->throw new IllegalArgumentException();};
        return switch(cell) { case 0,2->Items.IRON_INGOT;case 1->ThaumcraftMasonry.SLAB.get().asItem();case 3,5->ThaumcraftMasonry.ARCANE_STONE.get().asItem();
            case 4->charmItem();case 6,8->Items.GOLD_INGOT;case 7->ThaumcraftContent.TABLE_ITEM.get();default->throw new IllegalArgumentException();};
    }
    private Item craftOutput() { return craftIndex==0?charmItem():machineItem(); }
    private boolean runClicks() {
        if(age<3)return false;
        if(clickIndex==clicks.size()) { clicks=List.of(); clickIndex=0; return true; }
        Click click=clicks.get(clickIndex++); click(click.slot(),click.button(),click.type()); age=0; return false;
    }
    private void assertCosmetic() {
        require(focal().selectionRevision()==cosmeticRevision && view.jobSize()==0 && view.level()==beforeLevel && view.totalXp()==beforeTotal
                && view.xpProgress()==beforeProgress && ItemStack.matches(focal().getSlot(0).getItem(),savedFocus) && actualStarts==0,"Select/deselect only presentation; no XP/focus/job/payment mutation");
    }
    private boolean restoreSavedSlots() {
        if(!clicks.isEmpty())return runClicks() && false;
        if(age<5)return false;
        for(var entry:savedInventory.entrySet()) {
            int destination=entry.getKey(); ItemStack desired=entry.getValue();
            Predicate<ItemStack> matches=destination==10?FocalManipulatorClientSequence::payer:
                    desired.is(ThaumcraftFoci.EXCAVATION_FOCUS.get())?s->ItemStack.matches(s,upgradedFocus):
                    desired.is(Items.WOODEN_PICKAXE)?s->s.is(Items.WOODEN_PICKAXE) && s.getDamageValue()==5 && sameExcept(s,savedPick,DataComponents.DAMAGE):
                    desired.is(relayItem())?ItemStack::isEmpty:
                    desired.is(Items.COBBLESTONE)?s->ItemStack.isSameItemSameComponents(s,desired) && s.getCount()==desired.getCount()+1:s->ItemStack.matches(s,desired);
            if(matches.test(client.player.getInventory().getItem(destination)))continue;
            int source=find(client.player,matches),temporary=0;while(temporary==source || temporary==destination)temporary++;
            // Three genuine number-key swaps exchange two main slots without
            // PICKUP merging component-identical stacks of different counts.
            clicks=List.of(new Click(playerSlot(source),temporary,ClickType.SWAP),new Click(playerSlot(destination),temporary,ClickType.SWAP),new Click(playerSlot(source),temporary,ClickType.SWAP));clickIndex=0;return false;
        }
        require(client.player.containerMenu.getCarried().isEmpty(),"Final native cursor empty"); return true;
    }

    private void finish(ServerPlayer p) {
        // Client slot prediction is not the server's inventory ACK. Prove the
        // actual restored saved slots before serializing any final-state claim.
        ItemStack expectedPayer=chargedPayer.copy(); WandData expectedData=ItemWandCasting.data(expectedPayer);
        for(String tag:WandData.PRIMALS)expectedData=expectedData.withAmount(tag,expectedData.amount(tag)-5130-(tag.equals("terra")?13:0));
        expectedPayer.set(ThaumcraftContent.WAND_DATA.get(),expectedData);
        require(payer(p.getInventory().getItem(10)) && ItemStack.matches(p.getInventory().getItem(10),expectedPayer),"SERVER ACK: exact saved payer10 shell/components plus all6 actual charges/5130prices/13terra");
        for(var entry:savedInventory.entrySet()) {
            ItemStack expected=entry.getValue().copy();
            if(entry.getKey()==10)expected=expectedPayer;
            else if(expected.is(ThaumcraftFoci.EXCAVATION_FOCUS.get()))expected=upgradedFocus;
            else if(expected.is(Items.WOODEN_PICKAXE))expected.setDamageValue(5);
            else if(expected.is(relayItem()))expected.setCount(expected.getCount()-1);
            else if(expected.is(Items.COBBLESTONE))expected.setCount(expected.getCount()+1);
            require(ItemStack.matches(p.getInventory().getItem(entry.getKey()),expected),"SERVER ACK: restored full saved slot "+entry.getKey());
        }
        preserve(p,true); require(ledger.charms==1 && ledger.machines==1 && ledger.smelts==112 && ledger.burnedCoal==16 && ledger.lastCoal==0 && ledger.xpDebits==1 && actualStarts==1 && acks.size()==1,"Two genuine recipes /112actual smelts/16native coal burns/one paid rank1 only");
        require(xp.credits()==112 && xp.post(112)==0 && count(p,Items.GOLD_INGOT)==108 && count(p,Items.COBBLESTONE)==4,"112 native gold/XP;4gold spent;108gold and OLD3plusNEW1cobble retained");
        require(ledger.grants.equals(POTENCY) && castPayments==1 && breakCalls==1 && beforeCalls==1 && afterCalls==1,"All2400 actual six-primal grants and one native protected13terra Potency completion");
        require(ItemStack.matches(p.getInventory().getItem(15),upgradedFocus) && potencyOne(p.getInventory().getItem(15)) && FocusData.get(p.getInventory().getItem(10)).isEmpty(),"Same full upgraded focus returned to15; actual payer10 unequipped");
        require(count(p,basicItem())==0 && count(p,converterItem())==0 && count(p,relayItem())==0 && loadedBE(p.serverLevel(),SOURCE.below()) instanceof TileNodeStabilizer
                && loadedBE(p.serverLevel(),SOURCE.above()) instanceof TileNodeConverter && loadedBE(p.serverLevel(),SOURCE) instanceof TileNodeEnergized,"Only actual relocated earned tower + one saved spare relay installed");
        require(actualMachine(p.serverLevel()).isEmpty() && actualMachine(p.serverLevel()).getSize()==0 && ((TileArcaneWorkbench)loadedBE(p.serverLevel(),BENCH)).isEmpty(),"Final machines/bench empty/no pending job");
        for(BlockPos pos:FURNACES) { FurnaceBlockEntity furnace=furnace(p.serverLevel(),pos); require(furnace.isEmpty() && recipesUsed(furnace)==0,"Every native furnace empty/no pending recipe XP"); }
        require(pendingRows(p.serverLevel()).isEmpty() && p.serverLevel().getEntitiesOfClass(ItemEntity.class,SCENE).isEmpty() && p.serverLevel().getEntitiesOfClass(ExperienceOrb.class,SCENE).isEmpty()
                && p.serverLevel().getEntitiesOfClass(ItemEntity.class,OLD_AREA).isEmpty(),"No lost/unpicked physical stock/XP/queue owners");
        require(p.containerMenu==p.inventoryMenu && !p.isUsingItem() && !p.isShiftKeyDown() && !ItemWandCasting.isUsingNode(p),"All native menus/channels/Shift released");
        report.put("final_native_finite_state",row("actual_payer10",snbt(p.getInventory().getItem(10),p),"actual_upgraded_focus15",snbt(upgradedFocus,p),
                "actual_XpLevel_Total_Progress",List.of(p.experienceLevel,p.totalExperience,p.experienceProgress),"actual_XP_credited",xp.credits(),"native_orb_UUID_credits",xp.creditedUuids(),
                "gold_retained",108,"actual_OLD_plus_NEW_cobble",count(p,Items.COBBLESTONE),"actual_NEW_cobble",castPayments,"cobble_UUID",cobbleId.toString(),"same_device_UUIDs",deviceDrops.toString(),"actual_all6_CV_grants",Map.copyOf(ledger.grants),
                "native_pick_damage",List.of(3,5),"actual_SERVER_POSTs",ledger.checks,"tail_continues_to_normal_stop",true)); storyFinal=true;
        // Protection listeners belong to this completed cast, not normal-stop
        // resource tail observation. This owner task is outside their callbacks.
        retireProtection();
    }

    private void installAckTap() throws ReflectiveOperationException {
        Field field=FocalManipulatorNetworking.class.getDeclaredField("clientAckReceiver");field.setAccessible(true);
        @SuppressWarnings("unchecked") Consumer<FocalManipulatorAckPayload> existing=(Consumer<FocalManipulatorAckPayload>)field.get(null); previousAck=existing;
        ackTap=ack->{
            try { previousAck.accept(ack);
                if(requestedSession!=null && ack.containerId()==requestedContainer && ack.sessionId().equals(requestedSession) && ack.expectedRevision()==requestedRevision && ack.upgradeId()==0) {
                    require(ack.accepted() && acks.isEmpty(),"Exactly ONE actual accepted root C2S ACK, no fabricated callback");
                    acks.add(row("event","native queued S2C ACK after normal GUI receiver","container",ack.containerId(),"session",ack.sessionId().toString(),"expected_revision",ack.expectedRevision(),"upgrade",ack.upgradeId(),"accepted",ack.accepted(),"server_revision",ack.serverRevision()));
                }
            } catch(Throwable error) { fail(error); }
        }; FocalManipulatorNetworking.setClientAckReceiver(ackTap);ackInstalled=true;
    }
    private void restoreAckTap() {
        if(!ackInstalled)return;
        try { require(fieldStatic(FocalManipulatorNetworking.class,"clientAckReceiver")==ackTap,"Do not overwrite another ACK consumer"); FocalManipulatorNetworking.setClientAckReceiver(previousAck);ackInstalled=false; }
        catch(Throwable error) { fail(error); }
    }
    private void clientPre(Minecraft actual) {
        if(actual!=client || client.player==null || client.level==null)return;
        try {
            clientInputSnapshot=row("client_tick",client.player.tickCount,"phase",step.name(),"selected",client.player.getInventory().selected,
                    "window_active",client.isWindowActive(),"screen",client.screen==null?"NONE":client.screen.getClass().getName(),
                    "use_key_down",client.options.keyUse.isDown(),"charging",charging,"casting",casting,"mining",mining).toString();
            if(charging || casting || mining) {
                int hotbarClicks=0; for(KeyMapping key:client.options.keyHotbarSlots)while(key.consumeClick())hotbarClicks++;
                if(hotbarClicks>0)actions.add(row("event","suppressed unsolicited hotbar key clicks during owned channel/mining","client_tick",client.player.tickCount,"phase",step.name(),"count",hotbarClicks,"selected_slot",client.player.getInventory().selected));
            }
            if(charging && client.screen==null)sendRotation(Vec3.atCenterOf(SOURCE));
            if(casting && client.screen==null)sendRotation(Vec3.atCenterOf(STONE));
            boolean paidRelease=charging && charge!=null && charge.funded || casting && castPayments>0;
            if((paidRelease || inert || !client.isWindowActive() || client.screen!=null) && (charging || casting)) {
                if(!paidRelease && !inert)fail(new IllegalStateException("Unrequested native channel release: windowActive="+client.isWindowActive()+", screen="+(client.screen==null?"NONE":client.screen.getClass().getName())+", selected="+client.player.getInventory().selected));
                actions.add(row("event","native client release input reason","client_tick",client.player.tickCount,"phase",step.name(),"funded_or_paid",paidRelease,"window_active",client.isWindowActive(),"screen",client.screen==null?"NONE":client.screen.getClass().getName(),"selected_slot",client.player.getInventory().selected));
                releaseUse();
            }
            if(!(charging || casting)) { while(client.options.keyUse.consumeClick()){} client.options.keyUse.setDown(false); }
            if(!mining || miningDevice<0 || loadedClientDeviceGone()) { while(client.options.keyAttack.consumeClick()){}client.options.keyAttack.setDown(false); if(mining) { mining=false;client.gameMode.stopDestroyBlock(); } }
            if(step==Step.EQUIP_RADIAL || step==Step.UNEQUIP_ACK)holdFocus();
        } catch(Throwable error) { fail(error); client.options.keyUse.setDown(false);client.options.keyAttack.setDown(false); }
    }
    private boolean loadedClientDeviceGone() { return miningDevice>=0 && client.level.isEmptyBlock(devices().get(miningDevice).from()); }
    private void releaseUse() {
        charging=false;casting=false;client.options.keyUse.setDown(false);
        if(!releaseSent && client.player!=null && client.gameMode!=null) { client.gameMode.releaseUsingItem(client.player);releaseSent=true; }
    }
    private void releaseKeys() {
        client.options.keyUse.setDown(false);client.options.keyAttack.setDown(false);client.options.keyShift.setDown(false);
        client.options.keyUp.setDown(false);client.options.keyDown.setDown(false);client.options.keyLeft.setDown(false);client.options.keyRight.setDown(false);client.options.keyJump.setDown(false);
        while(client.options.keyUse.consumeClick()){}while(client.options.keyAttack.consumeClick()){}
    }
    private void releaseInputs() {
        if(activeInputOwner==this)activeInputOwner=null;
        releaseUse();releaseKeys();releaseFocus();holdShift(false);mining=false;
        if(client.gameMode!=null)client.gameMode.stopDestroyBlock(); if(camera!=null)client.options.setCameraType(camera);
        // Called from owner tick/close, never from this live CLIENT_PRE callback.
        ClientTickEvent.CLIENT_PRE.unregister(preHook);
    }
    /** TEST-ONLY owned UI isolation, not a gameplay/selection/vis override.
     * Scripted actions never scroll the hotbar. Keep native server identity
     * fences strict and log this input instead of allowing an accidental switch. */
    public static boolean suppressHotbarScroll(Minecraft client,double horizontal,double vertical) {
        FocalManipulatorClientSequence owner=activeInputOwner;
        if(owner==null || owner.client!=client || owner.inert || owner.stopped || !(owner.charging || owner.casting || owner.mining))return false;
        owner.actions.add(row("event","suppressed unsolicited hotbar scroll during owned channel/mining","client_tick",client.player==null?-1:client.player.tickCount,
                "phase",owner.step.name(),"horizontal",horizontal,"vertical",vertical,"window_active",client.isWindowActive(),"server_gameplay_mutation",false));
        return true;
    }
    private void pressFocus() {
        require(client.isWindowActive() && ThaumcraftFocusClient.CHANGE_FOCUS.matches(GLFW.GLFW_KEY_F,0),"Owned active native F mapping, no rebind");
        KeyMapping.click(InputConstants.getKey(focusBinding));holdFocus();
    }
    private void holdFocus() { ThaumcraftFocusClient.CHANGE_FOCUS.setDown(true); if(ThaumcraftFocusClient.CHANGE_FOCUS.same(client.options.keySwapOffhand))client.options.keySwapOffhand.setDown(true); }
    private void releaseFocus() {
        ThaumcraftFocusClient.CHANGE_FOCUS.setDown(false); if(ThaumcraftFocusClient.CHANGE_FOCUS.same(client.options.keySwapOffhand))client.options.keySwapOffhand.setDown(false);
        while(ThaumcraftFocusClient.CHANGE_FOCUS.consumeClick()){} while(client.options.keySwapOffhand.consumeClick()){}
    }
    private void holdShift(boolean held) {
        client.options.keyShift.setDown(held);
        nativeShift.setHeld(client,held);
        report.put("native_Shift_backend","Windows SendInput via Minecraft's existing JNA, exact owned foreground HWND; native GLFW/Screen.hasShiftDown, no tooltip/flag override");
    }
    private int[] selectorPoint() {
        require(client.screen instanceof GuiFocusSelect,"Actual native focus radial");
        List<ItemStack> choices=client.player.getInventory().items.stream().filter(FocusData::isSupportedFocus).sorted(java.util.Comparator.comparing(s->((thaumcraft.api.wands.ItemFocusBasic)s.getItem()).getSortingHelper(s))).toList();
        int index=-1;for(int i=0;i<choices.size();i++)if(ItemStack.matches(choices.get(i),upgradedFocus))index=i;
        require(index>=0 && choices.size()<=12,"Actual exact upgraded loose focus in unpaged native radial");
        double angle=-Math.PI/2+Math.PI*2*index/choices.size(),radius=16+choices.size()*2.5;
        return new int[]{client.screen.width/2+(int)Math.round(Math.cos(angle)*radius),client.screen.height/2+(int)Math.round(Math.sin(angle)*radius)};
    }
    private void guiClick(int x,int y) {
        require(client.screen instanceof GuiFocalManipulator,"Actual focal screen only");
        int px=(client.screen.width-192)/2+x,py=(client.screen.height-233)/2+y;
        client.screen.mouseMoved(px,py);require(client.screen.mouseClicked(px,py,0),"Actual native GUI hitbox");client.screen.mouseReleased(px,py,0);
    }
    private void moveMouse(int x,int y) {
        require(client.isWindowActive() && client.screen!=null,"Owned GUI cursor positioning");
        GLFW.glfwSetCursorPos(client.getWindow().getWindow(),x*client.getWindow().getScreenWidth()/(double)client.screen.width,y*client.getWindow().getScreenHeight()/(double)client.screen.height);
        client.screen.mouseMoved(x,y);
    }
    private void moveInventoryMouse(int slot) {
        int x=(client.screen.width-176)/2+(slot<9?8+slot*18:8+(slot%9)*18)+8;
        int y=(client.screen.height-166)/2+(slot<9?142:84+(slot/9-1)*18)+8;moveMouse(x,y);
    }
    private boolean hand(Predicate<ItemStack> wanted) throws ReflectiveOperationException {
        require(client.player.containerMenu==client.player.inventoryMenu,"Native hand selection outside menus");
        if(!wanted.test(client.player.getInventory().getItem(HAND))) {
            click(playerSlot(find(client.player,wanted)),HAND,ClickType.SWAP);handChanged=client.player.tickCount;return false;
        }
        if(client.player.tickCount-handChanged<6)return false;
        client.player.getInventory().selected=HAND;Method sync=client.gameMode.getClass().getDeclaredMethod("ensureHasSentCarriedItem");sync.setAccessible(true);sync.invoke(client.gameMode);return true;
    }
    private int playerSlot(int inventorySlot) {
        return client.player.containerMenu.slots.stream().filter(s->s.container==client.player.getInventory() && s.getContainerSlot()==inventorySlot).findFirst().orElseThrow().index;
    }
    private void click(int slot,int button,ClickType type) { client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId,slot,button,type,client.player); }
    private boolean aim(BlockPos pos,Direction face,Vec3 point) {
        require(client.isWindowActive() && client.player.canInteractWithBlock(pos,0),"Native Survival block reach "+pos);sendRotation(point);
        if(!pos.equals(useAim)) { useAim=pos;useAimTick=client.player.tickCount;return false; }
        return client.player.tickCount-useAimTick>=6;
    }
    private boolean use(BlockPos pos,Direction face,Vec3 point) {
        if(!aim(pos,face,point))return false;
        require(client.gameMode.useItemOn(client.player,InteractionHand.MAIN_HAND,new BlockHitResult(point,face,pos,false)).consumesAction(),"Actual native C2S useItemOn "+pos);
        useAim=null;return true;
    }
    private void sendRotation(Vec3 point) {
        Vec3 delta=point.subtract(client.player.getEyePosition());client.player.setYRot((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));client.player.yRotO=client.player.getYRot();
        client.player.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z))));client.player.xRotO=client.player.getXRot();
        client.player.connection.send(new ServerboundMovePlayerPacket.Rot(client.player.getYRot(),client.player.getXRot(),client.player.onGround()));
    }
    private static void stand(ServerPlayer p,double x,double y,double z) {
        AABB body=new AABB(x-.3,y+.001,z-.3,x+.3,y+1.8,z+.3);BlockPos floor=BlockPos.containing(x,y-.01,z);
        require(p.level().noCollision(p,body) && !p.level().getBlockState(floor).getCollisionShape(p.level(),floor).isEmpty(),"Supported collision-checked camera, no floor/air rewrite");
        require(p.level().getEntitiesOfClass(LivingEntity.class,body.inflate(.1),e->e!=p).isEmpty(),"Never move/push an inherited living entity for a camera");p.teleportTo(x,y,z);
    }
    private void approach(ServerPlayer p,UUID id) {
        Entity entity=p.serverLevel().getEntity(id);require(entity instanceof ItemEntity item && drops.containsKey(id) && ItemStack.matches(item.getItem(),drops.get(id)),"Same untouched native item UUID/components until contact");
        contactPosition(p,entity);contacted.add(id);physical.add(row("event","supported player contact approach ONLY","tick",p.level().getGameTime(),"UUID",id.toString(),"item_position_UNCHANGED",entity.position().toString(),"player",p.position().toString()));
    }
    private void observePhysicalContacts(ServerPlayer p) {
        for(UUID id:drops.keySet())if(p.serverLevel().getEntity(id) instanceof ItemEntity item
                && p.getBoundingBox().inflate(1,.5,1).intersects(item.getBoundingBox()) && contacted.add(id))
            physical.add(row("event","actual native same-UUID contact AABB observed","tick",p.level().getGameTime(),"UUID",id.toString(),"entity_position",item.position().toString(),"player_position",p.position().toString(),"actual_item",snbt(item.getItem(),p)));
    }
    private void approachXp(ServerPlayer p) {
        var orbs=p.serverLevel().getEntitiesOfClass(ExperienceOrb.class,SCENE);require(!orbs.isEmpty(),"Actual native XP still exists, never spawn a replacement");contactPosition(p,orbs.getFirst());
    }
    private void contactPosition(ServerPlayer p,Entity item) {
        for(int y=215;y<=221;y++)for(double dx:new double[]{0,.6,-.6,1,-1})for(double dz:new double[]{0,.6,-.6,1,-1}) {
            Vec3 at=new Vec3(item.getX()+dx,y,item.getZ()+dz);AABB body=p.getBoundingBox().move(at.subtract(p.position()));BlockPos floor=BlockPos.containing(at.x,at.y-.01,at.z);
            if(!p.level().noCollision(p,body) || p.level().getBlockState(floor).getCollisionShape(p.level(),floor).isEmpty() || !body.inflate(1,.5,1).intersects(item.getBoundingBox()))continue;
            if(!p.level().getEntitiesOfClass(LivingEntity.class,body.inflate(.1),entity->entity!=p).isEmpty())continue;
            stand(p,at.x,at.y,at.z);return;
        }
        throw new IllegalStateException("No supported same-UUID native contact path; never teleport the entity");
    }
    private void verifyContact(ServerPlayer p,UUID id,Item item) {
        require(contacted.contains(id) && p.serverLevel().getEntity(id)==null && p.getInventory().items.stream().anyMatch(value->value.is(item) && ItemStack.isSameItemSameComponents(value,drops.get(id))),"Same observed UUID/full components after actual player contact, inventory not reconstructed");
        physical.add(row("event","actual same-UUID native item pickup completed","tick",p.level().getGameTime(),"UUID",id.toString(),"stack",snbt(drops.get(id),p),"item_position_or_pickup_calls",0));
    }

    private void shot(int index) { require(index==captureCount,"Exact contiguous164–173 order");captureCount++;captureEvent("requested",CAPTURES.get(index));capture.accept(CAPTURES.get(index)); }
    private boolean ack(boolean condition,String action) { require(age<80 || condition,"Finite native ACK: "+action);return age>=6 && condition; }
    private static ItemStack stack(Player p,Predicate<ItemStack> predicate) { return p.getInventory().getItem(find(p,predicate)); }
    private static int find(Player p,Predicate<ItemStack> predicate) { for(int i=0;i<36;i++)if(predicate.test(p.getInventory().getItem(i)))return i;throw new IllegalStateException("Missing genuine physical input/empty slot"); }
    private static int emptySlot(Player p) { return find(p,ItemStack::isEmpty); }
    private static int count(Player p,Item item) { return count(p.getInventory().items,item); }
    private static int count(List<ItemStack> values,Item item) { return values.stream().filter(s->s.is(item)).mapToInt(ItemStack::getCount).sum(); }
    private static int sum(Map<String,Integer> values) { return values.values().stream().mapToInt(Integer::intValue).sum(); }
    private static Item basicItem() { return ThaumcraftNodeDevices.NODE_STABILIZER_ITEM.get(); }
    private static Item converterItem() { return ThaumcraftNodeDevices.NODE_TRANSDUCER_ITEM.get(); }
    private static Item relayItem() { return ThaumcraftNodeDevices.VIS_RELAY_ITEM.get(); }
    private static Item machineItem() { return ThaumcraftFocalManipulation.FOCAL_MANIPULATOR_ITEM.get(); }
    private static Item charmItem() { return ThaumcraftContent.MATERIALS.get("primal_charm").get(); }
    private static boolean payer(ItemStack stack) { return stack.is(ThaumcraftContent.WAND.get()) && ItemWandCasting.data(stack).rod().equals("silverwood") && ItemWandCasting.data(stack).cap().equals("gold"); }
    private List<Device> devices() { return List.of(new Device(OLD_NODE.above(),SOURCE.above(),converterItem(),Direction.UP,SOURCE),new Device(OLD_NODE.below(),SOURCE.below(),basicItem(),Direction.UP,SOURCE.below(2))); }
    private Device device() { return devices().get(deviceIndex); }
    private FurnaceMenu furnaceMenu() { return (FurnaceMenu)client.player.containerMenu; }
    private ContainerArcaneWorkbench arcane() { return (ContainerArcaneWorkbench)client.player.containerMenu; }
    private ContainerFocalManipulator focal() { return (ContainerFocalManipulator)client.player.containerMenu; }
    private static FurnaceBlockEntity furnace(ServerLevel level,BlockPos pos) { require(loadedBE(level,pos) instanceof FurnaceBlockEntity,"Actual loaded native vanilla furnace "+pos);return (FurnaceBlockEntity)loadedBE(level,pos); }
    private static TileFocalManipulator actualMachine(ServerLevel level) { return (TileFocalManipulator)loadedBE(level,MACHINE); }
    private static BlockEntity loadedBE(ServerLevel level,BlockPos pos) { var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);return chunk==null?null:chunk.getBlockEntities().get(pos); }
    private static BlockState loadedBlock(ServerLevel level,BlockPos pos) { var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);require(chunk!=null,"Required owned native location remains loaded: "+pos);return chunk.getBlockState(pos); }
    private static int blockCount(ServerLevel level,BlockPos pos,net.minecraft.world.level.block.Block block) { return loadedBlock(level,pos).is(block)?1:0; }
    private static CompoundTag tag(BlockEntity be) { return be.saveWithFullMetadata(be.getLevel().registryAccess()).copy(); }
    private static int recipesUsed(FurnaceBlockEntity furnace) {
        CompoundTag tag=furnace.saveWithoutMetadata(furnace.getLevel().registryAccess()).getCompound("RecipesUsed");int total=0;
        for(String key:tag.getAllKeys()) { require(key.equals("minecraft:gold_ingot_from_smelting_raw_gold"),"Only actual raw-gold smelting recipe usage");total+=tag.getInt(key); }return total;
    }
    private record NativeFurnace(int burn,int cook,int total,int raw) {}
    private static List<NativeFurnace> nativeFurnaces(ServerLevel level) {
        return FURNACES.stream().map(pos->{var f=furnace(level,pos);var n=f.saveWithoutMetadata(level.registryAccess());return new NativeFurnace(n.getShort("BurnTime"),n.getShort("CookTime"),n.getShort("CookTimeTotal"),f.getItem(0).getCount());}).toList();
    }
    private static List<Map<String,Object>> furnaceRows(ServerLevel level) {
        var rows=new ArrayList<Map<String,Object>>();for(BlockPos pos:FURNACES) { FurnaceBlockEntity f=furnace(level,pos);CompoundTag tag=f.saveWithoutMetadata(level.registryAccess());
            rows.add(row("pos",pos.toShortString(),"input_fuel_output",List.of(f.getItem(0).getCount(),f.getItem(1).getCount(),f.getItem(2).getCount()),"native_Burn_Cook_Total",List.of(tag.getShort("BurnTime"),tag.getShort("CookTime"),tag.getShort("CookTimeTotal")),"actual_recipeUsage",recipesUsed(f))); }return List.copyOf(rows);
    }
    private static Map<String,Integer> amounts(AspectList list) { var result=new LinkedHashMap<String,Integer>();for(Aspect aspect:list.getAspects()) { int n=list.getAmount(aspect);require(n>=0,"Nonnegative native aspect amount");if(n>0)result.put(aspect.getTag(),n); }return Map.copyOf(result); }
    private static Map<String,Integer> amountsIncludingZero(AspectList list) { var result=new LinkedHashMap<String,Integer>();for(Aspect aspect:list.getAspects())result.put(aspect.getTag(),list.getAmount(aspect));return Map.copyOf(result); }
    private static boolean samePayerShell(ItemStack a,ItemStack b) { return ItemWandCasting.data(a).rod().equals(ItemWandCasting.data(b).rod()) && ItemWandCasting.data(a).cap().equals(ItemWandCasting.data(b).cap()) && sameExcept(a,b,ThaumcraftContent.WAND_DATA.get(),ThaumcraftFoci.WAND_FOCUS.get()); }
    private static boolean sameFocusShell(ItemStack a,ItemStack b) { return sameExcept(a,b,ThaumcraftFoci.FOCUS_UPGRADES.get()); }
    private static boolean sameExcept(ItemStack a,ItemStack b,net.minecraft.core.component.DataComponentType<?>... components) { ItemStack x=a.copy(),y=b.copy();for(var component:components) { x.remove(component);y.remove(component); }return ItemStack.matches(x,y); }
    private static boolean potencyOne(ItemStack focus) { return focus.getItem() instanceof ItemFocusExcavation item && Arrays.equals(item.getAppliedUpgrades(focus),new short[]{0,-1,-1,-1,-1}); }
    private static void verifyDebit(ItemStack before,ItemStack after,int price) { require(samePayerShell(before,after),"Actual payer non-economic shell retained");for(String type:WandData.PRIMALS)require(ItemWandCasting.getVis(before,type)-ItemWandCasting.getVis(after,type)==price,"Exact actual all6 native recipe debit"); }
    private static String snbt(ItemStack stack,ServerPlayer p) { return stack.isEmpty()?"EMPTY":stack.save(p.registryAccess()).toString(); }
    private static Object field(Object target,String name) { try { return BookClientSmoke.field(target,name); }catch(ReflectiveOperationException error) { throw new IllegalStateException("Read-only schema field "+name,error); } }
    private static Object fieldStatic(Class<?> type,String name) throws ReflectiveOperationException { Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(null); }
    private static BlockPos sessionNode(ServerPlayer p,ItemStack wand) {
        try { var sessions=(Map<?,?>)fieldStatic(ItemWandCasting.class,"SERVER_NODE_USES");Object actual=sessions.get(p);
            require(actual!=null && field(actual,"stack")==wand && field(actual,"hand")==InteractionHand.MAIN_HAND,"Actual bound server node-session stack/hand");return (BlockPos)field(actual,"pos");
        } catch(ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }
    private ServerPlayer serverPlayer() { ServerPlayer p=client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID());require(p!=null,"Current connected owned player");return p; }
    @FunctionalInterface private interface ServerAction { void run() throws ReflectiveOperationException; }
    private void server(ServerAction action) { require(task==null || task.isDone(),"No overlapping fixture/read-only server actions");if(task!=null)task.join();task=CompletableFuture.runAsync(()->{try{action.run();}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}},client.getSingleplayerServer()); }
    private boolean serverDone() { if(task==null)return true;if(!task.isDone())return false;task.join();return true; }
    private void next(Step value) { step=value;age=0;useAim=null;thaumcraft.common.Thaumcraft.LOG.info("Staged focal client step={} furnace={} device={} craft={}",value,furnaceIndex,deviceIndex,craftIndex); }
    private void fail(Throwable error) {
        if(failure==null) { failure=error;report.put("INERT_observation_failure",row("phase",step.name(),"last_native_SERVER_POST",lastPost,"error",error.toString(),"callbacks_unregister_nothing",true)); }
        inert=true;
    }
    /** TEST-ONLY read-only HEAD witness; never cancel/throw from Entity.setRemoved. */
    public static void observeLaterMob(net.minecraft.world.entity.Mob mob) {
        FocalManipulatorClientSequence self=activeServerOwner;
        if(self==null || self.owner==null || self.inert || self.stopped || mob.level()!=self.owner.level()
                || self.inheritedEntities.containsKey(mob.getUUID()))return;
        try {
            require(self.owner.server.isSameThread(),"Late native Mob observation must stay on owner server thread");
            Map<String,Object> view=livingView(mob);
            self.inheritedEntities.put(mob.getUUID(),mob);self.savedLiving.put(mob.getUUID(),view);
            self.laterLivingRows.add(row("event","actual native Mob.checkDespawn HEAD first observation","UUID",mob.getUUID().toString(),
                    "tick",mob.level().getGameTime(),"native",view,"observer_entity_mutations",0,
                    "accepted_parent_identity","ROOT independently checks original stopped NBT; not inferred from late loading"));
            if(!mob.hasCustomName() && !mob.isNoAi() && !mob.isPersistenceRequired() && self.outsideOwnedCells(mob.getBoundingBox()))
                self.ambientEligible.add(mob.getUUID());
        } catch(Throwable error) { self.fail(error); }
    }
    /** TEST-ONLY read-only HEAD witness; never cancel/throw from Entity.setRemoved. */
    public static void observeInheritedRemoval(Entity entity,Entity.RemovalReason reason) {
        FocalManipulatorClientSequence self=activeServerOwner;
        if(self==null || self.owner==null || self.inert || self.stopped || entity.level()!=self.owner.level()
                || !self.inheritedEntities.containsKey(entity.getUUID()) || !reason.shouldDestroy())return;
        try {
            require(self.owner.server.isSameThread(),"Inherited removal observer must stay on owner server thread");
            List<String> frames=Arrays.stream(Thread.currentThread().getStackTrace()).map(frame->frame.getClassName()+"."+frame.getMethodName()).toList();
            boolean outside=self.outsideOwnedCells(entity.getBoundingBox());
            String cause="UNPROVEN",damage="NONE",damageType="NONE",damageEntity="NONE",damageDirect="NONE",lastHurt="NONE";
            long lastDamageStamp=0;
            Map<String,Object> current=entity instanceof LivingEntity living?livingView(living):row("nonliving",true);
            if(entity instanceof LivingEntity living) {
                // getLastDamageSource() is NOT a pure getter: native code may
                // clear its expired cache. Observe fields and apply the same
                // expiry predicate locally without any write to the entity.
                lastDamageStamp=((Number)field(living,"lastDamageStamp")).longValue();
                var cached=(net.minecraft.world.damagesource.DamageSource)field(living,"lastDamageSource");
                var source=entity.level().getGameTime()-lastDamageStamp>40?null:cached;
                Entity last=(Entity)field(living,"lastHurtByMob");if(last!=null)lastHurt=last.getUUID().toString();
                if(source!=null) {
                    damage=source.getMsgId();
                    damageType=source.typeHolder().unwrapKey().orElseThrow().location().toString();
                    if(source.getEntity()!=null)damageEntity=source.getEntity().getUUID().toString();
                    if(source.getDirectEntity()!=null)damageDirect=source.getDirectEntity().getUUID().toString();
                }
                int removed=frames.indexOf("net.minecraft.world.entity.Entity.setRemoved"),remove=frames.indexOf("net.minecraft.world.entity.Entity.remove");
                boolean nativeCaller=removed>=0 && remove>removed;
                for(int i=Math.max(0,removed);i<frames.size();i++)if(frames.get(i).startsWith("thaumcraft."))nativeCaller=false;
                boolean currentUnprotected=living instanceof net.minecraft.world.entity.Mob mob && !mob.hasCustomName() && !mob.isNoAi()
                        && !mob.isPersistenceRequired() && living.isInvulnerable()==(Boolean)self.savedLiving.get(entity.getUUID()).get("Invulnerable");
                boolean unowned=last!=self.owner && (source==null || source.getEntity()!=self.owner && source.getDirectEntity()!=self.owner);
                if(self.ambientEligible.contains(entity.getUUID()) && outside && currentUnprotected && nativeCaller && unowned) {
                    int discard=frames.indexOf("net.minecraft.world.entity.Entity.discard"),despawn=frames.indexOf("net.minecraft.world.entity.Mob.checkDespawn");
                    if(reason==Entity.RemovalReason.DISCARDED && discard>remove && despawn>discard)cause="VANILLA_MOB_CHECK_DESPAWN";
                    if(source!=null) {
                    boolean environmental=source.is(net.minecraft.world.damagesource.DamageTypes.ON_FIRE) || source.is(net.minecraft.world.damagesource.DamageTypes.IN_FIRE)
                            || source.is(net.minecraft.world.damagesource.DamageTypes.FALL) || source.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)
                            || source.is(net.minecraft.world.damagesource.DamageTypes.DROWN) || source.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)
                            || source.is(net.minecraft.world.damagesource.DamageTypes.LAVA) || source.is(net.minecraft.world.damagesource.DamageTypes.FREEZE);
                    if(reason==Entity.RemovalReason.KILLED && frames.indexOf("net.minecraft.world.entity.LivingEntity.tickDeath")>remove
                            && environmental && source.getEntity()==null && source.getDirectEntity()==null && living.getHealth()<=0)cause="VANILLA_ENVIRONMENTAL_TICK_DEATH";
                    }
                }
            }
            boolean proven=!cause.equals("UNPROVEN");
            self.ambientRemovalRows.add(row("event","actual native inherited Entity.setRemoved HEAD","UUID",entity.getUUID().toString(),
                    "tick",entity.level().getGameTime(),"type",BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(),
                    "reason",reason.name(),"position",entity.position().toString(),"initially_unprotected_ambient",self.ambientEligible.contains(entity.getUUID()),
                    "outside_owned_areas",outside,"cause",cause,"damage",damage,"damage_type",damageType,
                    "damage_entity_UUID",damageEntity,"damage_direct_entity_UUID",damageDirect,"last_hurt_by_mob_UUID",lastHurt,
                    "last_damage_game_tick",lastDamageStamp,"current_native",current,"native_frames",frames,"proven_vanilla",proven,"observer_entity_mutations",0));
            if(proven)self.provenAmbientRemovals.add(entity.getUUID());
            else self.fail(new IllegalStateException("Unexpected inherited entity removal, no native ambient witness: "+entity.getUUID()+"/"+reason));
        } catch(Throwable error) { self.fail(error); }
    }
    private static void require(boolean condition,String message) { if(!condition)throw new IllegalStateException(message); }
    private static Map<String,Object> row(Object... pairs) { require(pairs.length%2==0,"Evidence key/value pairs");var result=new LinkedHashMap<String,Object>();for(int i=0;i<pairs.length;i+=2)result.put((String)pairs[i],pairs[i+1]==null?"NONE":pairs[i+1]);return Map.copyOf(result); }

    private void retireProtection() {
        for(NativeEventTap tap:protectionTaps)try { tap.close(); }catch(Throwable error) { fail(error); }
        protectionTaps.clear();if(hooks)BlockEvent.BREAK.unregister(breakHook);
    }
    private void serverStopped(MinecraftServer server) {
        if(owner==null || server!=owner.server || stopped)return;
        // Invoked by the existing private SNAPSHOT dispatcher, never by removing
        // our callback from Architectury's live SERVER_STOPPED ArrayList.
        stopped=true;
        if(activeServerOwner==this)activeServerOwner=null;
        try {
            TickEvent.SERVER_POST.unregister(postHook);EntityEvent.ADD.unregister(addHook);retireProtection();ClientTestServerLifecycle.unregister(stopHook);hooks=false;
            if(xp!=null)xp.close();
            var tail=row("status",failure==null && storyFinal?"STOPPED_UNINSPECTED":"FAILED_OR_INCOMPLETE","run_id",runId,
                    "last_native_SERVER_POST",lastPost,"first_native_SERVER_POST",firstPost,"story_complete",storyFinal,
                    "error",failure==null?"NONE":failure.toString(),"report",report,"stopped_NBT_audit","ROOT_REQUIRED; never invoke save from observer");
            Files.writeString(tailFile,new GsonBuilder().setPrettyPrinting().create().toJson(tail)+"\n",StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
        } catch(Throwable error) { fail(error); }
        client.execute(()->{releaseInputs();restoreAckTap();});
    }
    @Override public void close() {
        if(cleanupRequested)return;cleanupRequested=true;
        releaseInputs();restoreAckTap();
        if(storyFinal && failure==null && !stopped) {
            report.put("normal_close","Client inputs retired; independent read-only SERVER_POST tail retained until normal-stop snapshot dispatcher");return;
        }
        if(client.getSingleplayerServer()!=null)client.getSingleplayerServer().execute(()->{
            if(activeServerOwner==this)activeServerOwner=null;
            // Owner-scheduled AFTER any live callback returns. A failed scene is
            // inert; normal-stop snapshot dispatcher still writes its failure tail.
            retireProtection();if(hooks) { TickEvent.SERVER_POST.unregister(postHook);EntityEvent.ADD.unregister(addHook); }
            if(xp!=null)xp.close();
        });
    }
}
