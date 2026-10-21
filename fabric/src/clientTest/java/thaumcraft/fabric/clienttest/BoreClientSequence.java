package thaumcraft.fabric.clienttest;

import com.google.gson.GsonBuilder;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.client.gui.GuiArcaneBore;
import thaumcraft.client.gui.GuiArcaneWorkbench;
import thaumcraft.client.gui.GuiResearchBrowser;
import thaumcraft.client.gui.GuiResearchTable;
import thaumcraft.common.blocks.BlockArcaneBore;
import thaumcraft.common.config.ThaumcraftArcaneBore;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftNaturals;
import thaumcraft.common.config.ThaumcraftNodeDevices;
import thaumcraft.common.container.ContainerAlchemyFurnace;
import thaumcraft.common.container.ContainerArcaneBore;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.container.ContainerResearchTable;
import thaumcraft.common.items.ItemJar;
import thaumcraft.common.items.ItemJarLabel;
import thaumcraft.common.items.ItemResearchNotes;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.research.ResearchNoteData;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileAlchemyFurnace;
import thaumcraft.common.tiles.TileArcaneBore;
import thaumcraft.common.tiles.TileArcaneBoreBase;
import thaumcraft.common.tiles.TileArcaneWorkbench;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.tiles.TileNodeEnergized;
import thaumcraft.common.tiles.TilePedestal;
import thaumcraft.common.tiles.TileResearchTable;
import thaumcraft.common.tiles.TileVisRelay;

/** CLIENT TEST ONLY. ONE issued note -> native paid base/top -> two native digs.
 * Descriptive finite input program, not a gameplay helper, server executor API,
 * research grant, item installer or substitute for root's stopped-world audit.
 * The only server writes here are two predeclared RAW deliveries and explicitly
 * logged supported PLAYER camera fixtures; all gameplay actions use native input.
 * No original/donor implementation is copied. Source-only / UNEXECUTED. */
public final class BoreClientSequence implements AutoCloseable {
    static final UUID PLAYER = UUID.fromString("13848433-7be7-350d-8b4a-c038819c2260");
    static final BlockPos BENCH = p(1601,216,374), TABLE = p(1603,216,381), CHEST = p(1604,216,382),
            NODE = p(1591,218,377), FURNACE = p(1465,216,377), MATRIX = p(1474,218,377), CENTER = MATRIX.below(2),
            RELAY = p(1616,218,374), SOURCE = p(1623,218,374), FOUNDATION = p(1615,216,381),
            BASE = FOUNDATION.above(), TOP = BASE.above(), SCAFFOLD = p(1614,216,383), LEVER = p(1614,217,381),
            SLOW = p(1615,216,384), FAST = p(1615,216,385);
    static final AABB RESEARCH_AREA = new AABB(1589,213,370,1614,224,386),
            BORE_AREA = new AABB(1613,214,370,1626,224,449), ALTAR_AREA = new AABB(1461,213,366,1488,225,386);
    static final List<BlockPos> PEDESTALS = List.of(p(1471,216,374),p(1474,216,374),p(1477,216,374),
            p(1471,216,377),p(1477,216,377),p(1471,216,380),p(1474,216,380),p(1477,216,380));
    static final List<String> PLAN_TAGS = List.of("aer","humanus","instrumentum","machina","motus","ordo","perfodio","terra");
    static final Map<String,Integer> PRODUCED = Map.of("arbor",16,"terra",12,"perditio",12,"metallum",84,"machina",32,
            "motus",16,"praecantatio",32,"perfodio",32,"potentia",48,"vacuos",16);
    static final Map<String,Integer> COST = Map.of("potentia",16,"perfodio",32,"machina",32,"vacuos",16,"motus",16);
    static final Map<String,Integer> SURPLUS = Map.of("arbor",16,"terra",12,"perditio",12,"metallum",84,"praecantatio",32,"potentia",32);
    private static final List<String> CAPTURES = List.of(
            "174-native-first-scans-funded-research-pools-en.png", "175-one-genuinely-issued-arcane-bore-note-en.png",
            "176-native-funded-hex-drags-ink-revisions-en.png", "177-genuinely-learned-arcane-bore-book-en.png",
            "178-native-earned-research-900-aer-900-ordo-base-preview-en.png", "179-genuine-300-distilled-units-eleven-jars-en.png",
            "180-native-112-essentia-eight-ingredients-earned-top-en.png", "181-earned-installed-bore-width5-speed1-model-ui-en.png",
            "182-relay-absent-native36-beam-harvest-uuid-ledger-en.png", "183-same-relay-restored-native9-beam-harvest-uuid-ledger-en.png");
    private static volatile BoreClientSequence active;
    private static volatile String binding;
    private static volatile Map<String,String> bindingInputs=Map.of();
    private static final int HAND = 3, MAX_OPS = 2000;
    public final Map<String,Object> report = new ConcurrentHashMap<>();
    final Minecraft client;
    final String runId;
    final long wallStart = System.nanoTime();
    final List<Map<String,Object>> actions = new CopyOnWriteArrayList<>(), inputs = new CopyOnWriteArrayList<>(),
            physical = new CopyOnWriteArrayList<>(), captures = new CopyOnWriteArrayList<>();
    // Lossless native POST append log, not an O(n) array copy for every world tick.
    final List<Map<String,Object>> posts = BoreClientEvidence.log();
    final Map<UUID,ItemStack> drops = new ConcurrentHashMap<>();
    final Set<UUID> picked = ConcurrentHashMap.newKeySet();
    private final Consumer<String> capture;
    private final Path tailFile;
    private final FocalNativeShift shift = new FocalNativeShift();
    private final ArrayDeque<Op> queue = new ArrayDeque<>();
    private final TickEvent.Server postHook = this::serverPost;
    private final EntityEvent.Add addHook = this::entityAdded;
    private final LifecycleEvent.ServerState stopHook = this::serverStopped;
    private CameraType camera;
    volatile ServerPlayer owner;
    volatile BoreClientLedger ledger;
    volatile View view;
    volatile Throwable failure;
    volatile String phase = "ENTRY", opName = "ENTRY";
    volatile boolean inert, storyFinal, stopped, researchDone, boreDelivered, scanning, mining;
    volatile long clientHeart, serverHeart;
    volatile Expectation expectedDrop;
    volatile UUID lastDrop;
    volatile Vec3 scanPoint;
    volatile ItemStack earnedBase = ItemStack.EMPTY, earnedTop = ItemStack.EMPTY;
    volatile ResearchNoteData issued;
    volatile int noteRequests, editActions, bufferCombos, totalCombos;
    private volatile boolean closed;
    private boolean started, rDelivered;
    private boolean nativePreflightVerified;
    private volatile boolean nativeStopHeadReturned;
    private long menuFocusLostAt=-1;
    private int menuFocusLostTick,menuFocusStableTick=-1;
    private int declaredOps, clientTicks, selectedAt = -100, shots;
    private long phaseTick = -1, phaseWall;
    private long nativeEntryTick=-1;
    final List<Map<String,Object>> phaseStarts=new CopyOnWriteArrayList<>();
    private CompletableFuture<Void> serverTask;
    private Op current;

    public BoreClientSequence(Minecraft client, Consumer<String> capture, Path evidenceDirectory, String runId) {
        require(client != null && capture != null && runId != null && runId.matches("client-smoke-[a-f0-9-]{36}"), "Root-owned client run required");
        this.client=client; this.capture=capture; this.runId=runId;
        tailFile=evidenceDirectory.toAbsolutePath().normalize().resolve("bore-tail-"+runId+".json");
        require(Files.isDirectory(tailFile.getParent()) && !Files.exists(tailFile), "Existing root evidence directory / NEW tail file");
        report.put("state", "AUTHORED_NOT_NATIVE_PASS"); report.put("run_id",runId); report.put("capture_names",CAPTURES);
        report.put("declared_PHASE_R_raw_ONCE",rawDeclaration(false)); report.put("declared_PHASE_B_raw_ONLY_AFTER_LEARN",rawDeclaration(true));
        report.put("limits",row("R_native",4000,"R_wall_seconds",240,"B_native",12000,"B_wall_seconds",720,
                "combined_native",16000,"combined_wall_seconds",960,"declarative_ops",MAX_OPS,"planner_states_each",250000,
                "planner_calls_max",7,"additional_motus_combines_max",6,"issued_notes",1,"ink_max",30,"autoextensions",false));
        report.put("source_phase_limits",row("picks",3600,"pick_input_window",3200,"pistons",2400,"redstone",2200,"phials",900,"infusion",1610));
        report.put("source_servicing_policy","Keep actual positive typed buffers for native parallel refill; drain full32, exhausted unfiltered type only to free a missing native tag, or final buffers after actual batch cooks and furnace empty. No pulse/time/fuel/type setters or predicted RNG order.");
        report.put("declared_travel_and_support",row("travel","Explicit supported PLAYER teleports, NOT walking; no travel bridge",
                "research_pad_cells",List.of("1602, 215, 379","1601, 215, 379","1601, 215, 378","1601, 215, 377"),
                "R_raw_cobblestone",4,"B_foundation_and_scaffold_retained",true,"all_live_support_collision_and_BLOCK_ray_checks_required",true));
        report.put("declared_B_supported_camera_points",row("foundation",List.of(1614.5,216.0,381.5),
                "scaffold_approach",List.of(1614.5,216.0,382.5),"GUI_lever_model",List.of(1614.5,216.0,380.5),
                "boundary","Existing saved stone starts at x1614 here; no floor/anchor grant, every live camera still collision/support/entity checked"));
        report.put("native_actions",actions); report.put("raw_delivery_receipts",inputs); report.put("EVERY_SERVER_POST",posts);
        report.put("phase_start_events",phaseStarts);
        report.put("native_physical_UUID_events",physical); report.put("capture_events",captures);
        report.put("boundary","No research/RP/XP/vis/essentia/finished-target grants, backend calls, target/count/credit/RNG/clock writes, arbitrary entity movement, save restore, retry or accepted-record writes. Supported named player camera fixtures are logged as fixtures, NOT walking evidence.");
        report.put("pending_scope","13 ordinary non-BE target kinds / empty vacuum / ejection only. No chest-routing, arbitrary loot, Repair, full FX/audio, multiplayer or NeoForge claim. Root must inspect PNGs and independently audit all191 inherited files/entities.");
        report.put("parent_binding_required_from_ROOT",row("manifest",".tmp/bore-accepted-parent-before-client.json","expected_manifest_SHA256","cbcb22db9beb7056d0a61d08852bfb5bd5df38d22c58e78baa4d7de24ba58fd3","source_world","focal-smoke-8fd11823-4346-48f9-97ed-e06dbf9ec4be","raw191_verification_by_this_sequence",false));
        report.put("native_binding_inputs_v2",bindingInputs);
    }

    public static void nativeHooksInstalled(int version,String fingerprint,Map<String,String> inputs) {
        require(version==2 && fingerprint!=null && fingerprint.matches("[a-f0-9]{64}") && active==null && binding==null && inputs!=null && !inputs.isEmpty(),"Exact installed-class/resource v2 private observer binding before entry");
        bindingInputs=Map.copyOf(inputs);binding=fingerprint;
        BoreInheritedItemWitness.arm(fingerprint);
        require(BoreInheritedItemWitness.failure()==null,"Inherited item witness must arm BEFORE any native clone tick");
    }
    public static List<String> captureNames() { return CAPTURES; }
    /** Small current progress only. The stopped tail owns the lossless timeline;
     * do not serialize/traverse that growing graph on every GUI/capture update. */
    public Map<String,Object> progressSnapshot() {
        View currentView=view;
        return row("run_id",runId,"state",report.get("state"),"phase",phase,"operation",opName,
                "last_observed_native_tick",currentView==null?-1:currentView.tick(),
                "native_client_heartbeats",clientHeart,"native_server_heartbeats",serverHeart,
                "story_complete",storyFinal,"stopped",stopped,"failed",failure!=null,
                "error",failure==null?"NONE":failure.toString(),"issued_note_requests",noteRequests,
                "acknowledged_edits",editActions,"acknowledged_combinations",totalCombos,
                "additional_motus_combinations",bufferCombos,"capture_names",CAPTURES,
                "limits",report.get("limits"),"full_evidence_tail",tailFile.toString(),
                "last_actual_bootstrap_readiness",report.get("last_actual_bootstrap_readiness"),
                "full_evidence_available_only_after_normal_stop",true,
                "visual_and_stopped_audit","PENDING; this progress report is not acceptance");
    }
    public void captureEvent(String event,String name) {
        if(!CAPTURES.contains(name))return;
        if(event.equals("after_render_before_native_PNG_grab") && (name.equals(CAPTURES.get(8)) || name.equals(CAPTURES.get(9)))) {
            require(client.level!=null && client.level.getBlockEntity(TOP) instanceof TileArcaneBore,"Actual earned client head, not placeholder scene");
            var top=(TileArcaneBore)client.level.getBlockEntity(TOP);
            require(top.clientTarget()!=null && top.getBeamLength()>0 && top.isPowered() && top.hasFocus() && top.hasPickaxe(),"Required real beam presentation at actual native grab boundary; never force visual state or freeze time");
        }
        captures.add(row("event",event,"name",name,"nanos",System.nanoTime(),"metadata_NOT_material_proof",true,"actual_phase",phase,"pickup_evidence","separate actual native playerTouch RETURN ledger"));
    }
    public void check() { if(failure!=null) throw new IllegalStateException("Bore sequence failed/retained at "+phase+"/"+opName,failure); }
    public boolean tick(Minecraft actual) throws ReflectiveOperationException {
        try {
            require(actual==client && client.player!=null && client.level!=null && client.gameMode!=null && client.getSingleplayerServer()!=null,"Same connected root client/server");
             check(); clientTicks++;
             require(System.nanoTime()-wallStart<=960_000_000_000L,"Combined960wall-second cap INCLUDING captures/tail");
             if(!started) { enter(); return false; }
             if(nativePreflightVerified && client.screen!=null && !menuForegroundReady())return false;
            if(current==null) current=queue.pollFirst();
            if(current==null) { require(storyFinal,"Program may end ONLY after actual full final acknowledgement"); return true; }
            opName=current.name;
            if(current.tick()) { actions.add(row("event","native operation acknowledged","op",current.name,"POST",view==null?-1:view.tick)); current=null; }
            check(); return storyFinal && current==null && queue.isEmpty();
        } catch(ReflectiveOperationException|RuntimeException|Error error) { fail(error); close(); throw error; }
    }
    private void enter() {
        require(binding!=null && active==null,"MISSING_NATIVE_BINDING: root must wire v2 installed-class/resource observers; no raw delivery");
        require(PLAYER.equals(client.player.getUUID()) && "Player653".equals(client.player.getGameProfile().getName()),"Preserve actual Player653 UUID/profile; never replace with random dev player");
        Path world=client.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        String name=world.getFileName().toString(); require(name.startsWith("bore-smoke-"),"Fresh private bore-smoke-UUID COPY, not accepted/user world");
        UUID.fromString(name.substring("bore-smoke-".length()));
        require(world.getParent().getFileName().toString().equals("saves") && world.getParent().getParent().getFileName().toString().equals("run-clienttest"),"Private root client-test world");
        require(client.getLanguageManager().getSelected().equals("en_us"),"English capture locale prepared by root");
        require(client.player.containerMenu==client.player.inventoryMenu && client.player.containerMenu.getCarried().isEmpty(),"Root enters with no unowned container/cursor stock");
        started=true; active=this; camera=client.options.getCameraType(); client.options.setCameraType(CameraType.FIRST_PERSON);
        nativeEntryTick=nativeTick();report.put("actual_clock_origins",row("schema","bore_actual_clock_origins_v1","combined_wall_start_nanos",wallStart,
                "combined_native_entry_tick",nativeEntryTick,"accepted_parent_game_time",BoreInheritedItemWitness.PARENT_GAME_TIME,"ENTRY_phase_has_no_R_or_B_origin",true));
        client.setScreen(null);releaseInputs(); report.put("actual_owned_world",world.toString()); report.put("native_binding_declaration",binding);
        // Registration is owner-scheduled, never modifies Architectury's live list in a callback.
        add(bootstrap());
        add(preflight());
        add(serverOp("R delivery after real native foreground Shift preflight",()->{ require(clientHeart>0 && serverHeart>0,"Missing ACTUAL native tick heartbeats before delivery"); deliver(false); beginPhase("R"); }));
        add(custodyRescue());
        researchProgram();
    }

    private abstract class Op {
        final String name; final int ttl; long firstNativeTick=-1; int polls;
        Op(String name,int ttl) { require(++declaredOps<=MAX_OPS,"Finite2000 total declarative operations INCLUDING dynamic children");this.name=name;this.ttl=ttl; }
        final boolean tick() throws ReflectiveOperationException {
            if(firstNativeTick<0) firstNativeTick=nativeTick();
            require(++polls<=ttl*8+200 && nativeTick()-firstNativeTick<=ttl,"Finite native/client bound: "+name);
            return poll();
        }
        abstract boolean poll() throws ReflectiveOperationException;
    }
    private Op bootstrap() {
        return new Op("bounded actual native chunk readiness, then ONE baseline",600) {
            long startedWall=System.nanoTime();boolean sent;volatile boolean complete;
            @Override boolean poll() {
                require(System.nanoTime()-startedWall<=60_000_000_000L,"Bootstrap60wall/600native cap; never extend the combined960wall cap");
                if(sent) {if(!serverDone())return false;sent=false;if(complete)return true;}
                submit(()->{
                    if(owner==null) {
                        owner=serverPlayer();require(owner.gameMode.getGameModeForPlayer()==GameType.SURVIVAL && !owner.getAbilities().flying && !owner.getAbilities().instabuild,"Actual Survival actor");
                        ledger=new BoreClientLedger(BoreClientSequence.this);
                        ClientTestServerLifecycle.register(owner.server,stopHook);
                        report.put("bootstrap_limits",row("native_ticks",600,"wall_seconds",60,"part_of_combined960wall",true,"observer_chunk_requests",0));
                    }
                    if(!ledger.ready) {
                        Map<String,Object> observed=ledger.bootstrapReadiness();
                        report.put("last_actual_bootstrap_readiness",row("native_tick",nativeTick(),"readiness",observed));
                        if(Boolean.TRUE.equals(observed.get("ready"))) {
                            ledger.baseline();TickEvent.SERVER_POST.register(postHook);EntityEvent.ADD.register(addHook);
                        }
                    }
                    if(ledger.ready) {
                        Map<String,Object> contact=ledger.custodyBootstrapReadiness();
                        report.put("last_actual_custody_bootstrap_readiness",contact);
                        complete=Boolean.TRUE.equals(contact.get("ready"));
                    }
                });sent=true;return false;
            }
        };
    }
    @FunctionalInterface private interface Poll { boolean poll() throws ReflectiveOperationException; }
    @FunctionalInterface private interface ServerReadOrFixture { void run() throws ReflectiveOperationException; }
    private Op op(String name,int ttl,Poll poll) { return new Op(name,ttl) { @Override boolean poll() throws ReflectiveOperationException { return poll.poll(); } }; }
    private Op once(String name,Runnable run) { return op(name,100,()->{run.run();return true;}); }
    private Op waitFor(String name,int ttl,BooleanSupplier predicate) { return op(name,ttl,()->predicate.getAsBoolean()); }
    private Op serverOp(String name,ServerReadOrFixture action) {
        return new Op(name,120) { boolean sent;
            @Override boolean poll() { if(!sent) { submit(action);sent=true;return false; } return serverDone(); }
        };
    }
    private void add(Op value) { queue.addLast(value); }
    private void front(List<Op> values) { for(int i=values.size()-1;i>=0;i--)queue.addFirst(values.get(i)); }
    private Op sequence(String name,List<Op> values) {
        return new Op(name,4000) { int index;
            @Override boolean poll() throws ReflectiveOperationException { if(index==values.size())return true; if(values.get(index).tick())index++;return false; }
        };
    }
    private Op preflight() {
        return new Op("actual owned foreground HWND / native Shift down+up",500) { int state,wait;
            @Override boolean poll() {
                if(state==0) { if(!shift.focusOwnedWindow(client))return false; holdShift(true); state=1;wait=clientTicks;return false; }
                if(state==1) { if(clientTicks-wait<3 || !Screen.hasShiftDown())return false; holdShift(false);state=2;wait=clientTicks;return false; }
                if(clientTicks-wait<3 || Screen.hasShiftDown())return false;
                report.put("actual_native_Shift_preflight",row("foreground",shift.describe(client),"down_up_seen",true,"before_any_input",true));nativePreflightVerified=true;return true;
            }
        };
    }
    /** Reacquire only BEFORE a new menu intent, with no step/index/ack replay.
     * Native scanning/mining/Shift channel loss remains a hard failure. World
     * ticks, source fuel and all existing phase/combined deadlines continue. */
    private boolean menuForegroundReady() {
        if(!BoreOwnedForeground.isOwned(client)) {
            require(!scanning && !mining && !Screen.hasShiftDown() && !client.options.keyShift.isDown(),
                    "Native channel/Shift foreground loss is not an automatic retry: "+shift.describe(client));
            if(menuFocusLostAt<0) {
                menuFocusLostAt=System.nanoTime();menuFocusLostTick=clientTicks;
                actions.add(row("event","actual menu foreground unavailable before next intent","native_facts",shift.describe(client),"phase",phase,"operation",opName,"packets_resent",0));
            }
            require(System.nanoTime()-menuFocusLostAt<=25_000_000_000L && clientTicks-menuFocusLostTick<=500,
                    "Owned menu foreground25wall/500client-tick cap; no phase or combined extension");
            menuFocusStableTick=-1;releaseMovement();shift.focusOwnedWindow(client);return false;
        }
        if(menuFocusLostAt>=0) {
            if(menuFocusStableTick<0)menuFocusStableTick=clientTicks;
            require(System.nanoTime()-menuFocusLostAt<=25_000_000_000L && clientTicks-menuFocusLostTick<=500,"Owned menu foreground cap includes native stabilization");
            if(clientTicks-menuFocusStableTick<3)return false;
            actions.add(row("event","actual owned Win32/GLFW menu foreground re-established BEFORE next intent","native_facts",shift.describe(client),
                    "client_ticks_waited",clientTicks-menuFocusLostTick,"packets_resent",0,"native_world_ticks_paused",false));
            menuFocusLostAt=-1;menuFocusStableTick=-1;
        }
        return true;
    }
    private void beginPhase(String value) {
        require(value.equals("R") && phase.equals("ENTRY") && phaseTick<0 || value.equals("B") && phase.equals("R") && researchDone && boreDelivered,"Exactly R then B native phase origins, no clock restart");
        phase=value;phaseTick=nativeTick();phaseWall=System.nanoTime();
        phaseStarts.add(row("phase",value,"native_tick",phaseTick,"wall_nanos",phaseWall,"native_game_time",owner.serverLevel().getGameTime()));
    }
    private Op custodyRescue() {
        return new Op("ONE authorized inherited-jar custody excursion;200native/14wall INSIDE R",200) {
            int state;boolean sent;volatile boolean restored;
            final Op depart=serverOp("single custody departure gate",()->ledger.custodyDeparture());
            final Op outbound=camera("custody-only original jar contact on existing stone; temporary old-jar coverage gap",1297.75,204,361.5);
            final Op home=camera("custody return to original supported research floor",1604.5,216,380.5);
            @Override boolean poll() throws ReflectiveOperationException {
                if(state==0) {releaseInputs();require(client.screen==null && client.player.containerMenu.getCarried().isEmpty(),"Custody releases native channels, never unresolved cursor");if(!depart.tick())return false;state=1;}
                if(state==1) {if(!outbound.tick())return false;state=2;}
                if(state==2) {if(!ledger.custodyPicked())return false;state=3;}
                if(state==3) {if(!home.tick())return false;state=4;}
                if(sent) {if(!serverDone())return false;sent=false;if(restored)return true;}
                submit(()->restored=ledger.custodyRecoveryReady());sent=true;return false;
            }
        };
    }
    private void researchProgram() {
        // User prefers teleport fixtures for smoke-test travel. Go directly to
        // the existing floor; build only four REAL support/pickup-pad cells.
        add(camera("direct teleport to existing research floor; no travel bridge",1604.5,216,380.5));add(holdSneak(true));
        BlockPos previous=p(1603,215,379);
        for(BlockPos at:List.of(p(1602,215,379),p(1601,215,379),p(1601,215,378),p(1601,215,377))) {
            Direction face=at.getX()<previous.getX()?Direction.WEST:Direction.NORTH;
            add(camera("supported pad overhang teleport",previous.getX()+.5+face.getStepX()*.69,216,previous.getZ()+.5+face.getStepZ()*.69));
            add(place(Items.COBBLESTONE,at,previous,face));add(camera("actual placed pad support teleport",at.getX()+.5,216,at.getZ()+.5));previous=at;
        }
        add(holdSneak(false));add(camera("earned approach to existing bench",1601.5,216,377.5));
        add(open(BENCH,ContainerArcaneWorkbench.class));
        add(craft("same planned storage chest FIRST for inherited custody",grid(Items.OAK_PLANKS,Items.OAK_PLANKS,Items.OAK_PLANKS,Items.OAK_PLANKS,null,Items.OAK_PLANKS,Items.OAK_PLANKS,Items.OAK_PLANKS,Items.OAK_PLANKS),Items.CHEST,1,false,-1));
        add(closeMenu());add(camera("early genuine chest placement and EAST open from existing clear stone",1605.5,216,382.5));
        add(place(Items.CHEST,CHEST,CHEST.below(),Direction.UP));add(open(CHEST,AbstractContainerMenu.class,Direction.EAST));
        add(transfer(-1,7,value->ledger.matchesInheritedJar(value),1));add(closeMenu());
        add(serverOp("actual original jar slot7 stow gate; no NEW units credited",()->ledger.custodyStowed()));
        add(camera("resume original pending scanner recipe at real bench",1601.5,216,377.5));add(open(BENCH,ContainerArcaneWorkbench.class));
        add(craft("scanner",grid(null,shard("aer"),null,Items.GOLD_INGOT,Items.GLASS,Items.GOLD_INGOT,null,shard("aer"),null),ThaumcraftContent.THAUMOMETER.get(),1,false,-1));
        add(craft("scribe",grid(phial(),Items.FEATHER,Items.INK_SAC,null,null,null,null,null,null),ThaumcraftContent.INKWELL.get(),1,false,-1));
        add(craft("six oak slabs",grid(Items.OAK_PLANKS,Items.OAK_PLANKS,Items.OAK_PLANKS,null,null,null,null,null,null),Items.OAK_SLAB,6,false,-1));
        for(int i=0;i<2;i++)add(craft("ordinary table "+i,grid(Items.OAK_SLAB,Items.OAK_SLAB,Items.OAK_SLAB,Items.OAK_PLANKS,null,Items.OAK_PLANKS,null,null,null),ThaumcraftContent.TABLE_ITEM.get(),1,false,-1));
        add(closeMenu());add(camera("research catch pad apparatus",1604.5,216,380.5));
        add(place(ThaumcraftContent.TABLE_ITEM.get(),TABLE,TABLE.below(),Direction.UP));
        add(camera("second table south approach clear of early custody chest",1603.5,216,383.5));
        add(place(ThaumcraftContent.TABLE_ITEM.get(),TABLE.south(),TABLE.south().below(),Direction.UP));
        add(camera("actual north table conversion approach",1604.5,216,380.5));
        add(select(s->s.is(ThaumcraftContent.INKWELL.get())));add(use(TABLE,Direction.UP));
        add(waitFor("actual converted pair retains same scribe at0",80,()->client.level.getBlockEntity(TABLE) instanceof TileResearchTable));
        add(camera("real ten-block node ray from actual support pad",1601.2,216,377.5));
        add(scanNode());
        add(camera("table approach on actual pad",1604.5,216,380.5));add(open(TABLE,ContainerResearchTable.class));
        for(String[] pair:new String[][]{{"aer","perditio","vacuos"},{"terra","ordo","vitreus"},{"ordo","ignis","potentia"}}) add(combine(pair[0],pair[1],pair[2],false));
        add(closeMenu());
        for(String tag:WandData.PRIMALS)add(scanDropped(s->s.is(shard(tag)),"first shard "+tag));
        add(camera("table compound-discovery approach",1604.5,216,380.5));add(open(TABLE,ContainerResearchTable.class));
        for(String[] pair:new String[][]{{"aer","ordo","motus"},{"aqua","terra","victus"},{"victus","perditio","mortuus"},
                {"victus","mortuus","spiritus"},{"ignis","spiritus","cognitio"},{"motus","victus","bestia"},
                {"bestia","cognitio","humanus"},{"humanus","terra","perfodio"},{"humanus","ordo","instrumentum"},{"motus","instrumentum","machina"}})
            add(combine(pair[0],pair[1],pair[2],false));
        add(closeMenu());add(scanDropped(s->s.is(ThaumcraftContent.RESONATOR.get()),"inherited resonator"));
        add(scanDropped(s->s.is(ThaumcraftContent.WAND.get()) && ItemWandCasting.data(s).cap().equals("thaumium") && ItemWandCasting.data(s).rod().equals("silverwood"),"strongest intact Frost wand first ITEM-ID scan"));
        add(camera("funded research table",1604.5,216,380.5));add(open(TABLE,ContainerResearchTable.class));add(shot(0));
        add(transfer(0,-1,null,1));add(closeMenu());add(book(false));
        add(once("ONE native primary click",()->{require(noteRequests++==0,"No reroll/re-request");clickResearchEntry();}));
        add(waitFor("real issued note + paper/ink debit",100,()->findOptional(s->s.is(ThaumcraftContent.RESEARCH_NOTE.get()))>=0 && count(client.player,Items.PAPER)==0));
        add(once("bind actual ONE RNG note",()->{
            issued=ItemResearchNotes.data(client.player.getInventory().getItem(find(s->s.is(ThaumcraftContent.RESEARCH_NOTE.get()))));
            require(issued!=null && !issued.complete() && issued.key().equals("ARCANEBORE"),"Actual issued Bore component");report.put("issued_note_hash",BoreResearchNotePlan.fingerprint(issued));report.put("issued_note",issued);
        }));
        add(closeMenu());add(open(TABLE,ContainerResearchTable.class));
        add(transfer(-1,0,s->s.is(ThaumcraftContent.INKWELL.get()),1));add(transfer(-1,1,s->s.is(ThaumcraftContent.RESEARCH_NOTE.get()),1));
        add(shot(1));add(solveActualNote());add(closeMenu());
        add(select(s->s.is(ThaumcraftContent.DISCOVERY.get()) && ItemResearchNotes.data(s)!=null && ItemResearchNotes.data(s).complete()));
        add(once("actual solved-note use",()->{require(client.gameMode.useItem(client.player,InteractionHand.MAIN_HAND).consumesAction(),"Native earned discovery use");}));
        add(waitFor("authoritative learned key + actual note consumption",100,()->KnowledgeAccess.get(client.player).isResearchComplete("ARCANEBORE")
                && findOptional(s->s.getItem() instanceof ItemResearchNotes)<0 && view!=null && view.research.contains("ARCANEBORE")));
        add(book(true));add(shot(3));add(closeMenu());
        add(open(CHEST,AbstractContainerMenu.class));
        add(transfer(-1,0,s->s.is(ThaumcraftContent.THAUMOMETER.get()),1));int slot=1;
        for(String tag:WandData.PRIMALS)add(transfer(-1,slot++,s->s.is(shard(tag)),1));add(closeMenu());
        add(serverOp("genuine research final gate BEFORE B delivery",()->{ledger.researchFinal();researchDone=true;report.put("research_learned_native",true);}));
        add(serverOp("B declared raw ONCE after actual learned gate",()->{deliver(true);beginPhase("B");}));
        add(once("append finite paid-acquisition program",this::boreProgram));
    }

    private void boreProgram() {
        add(open(CHEST,AbstractContainerMenu.class));
        // Read the actual retained slot identity; do not grant duplicate shards.
        add(takeChestShard("aer"));add(takeChestShard("terra"));add(closeMenu());
        add(camera("base bench approach",1601.5,216,377.5));add(open(BENCH,ContainerArcaneWorkbench.class));
        add(transfer(-1,1,BoreClientSequence::payer,1));
        Item wood=ThaumcraftNaturals.GREATWOOD_PLANKS.get().asItem();
        add(craft("earned Bore base",grid(wood,Items.IRON_INGOT,wood,Items.IRON_INGOT,Items.DISPENSER,Items.IRON_INGOT,wood,Items.IRON_INGOT,wood),baseItem(),1,true,4));
        add(transfer(1,-1,null,1));add(closeMenu());
        add(camera("foundation supported actual stone floor",1614.5,216,381.5));add(place(Items.COBBLESTONE,FOUNDATION,FOUNDATION.below(),Direction.UP));
        add(place(baseItem(),BASE,FOUNDATION,Direction.UP));
        add(camera("scaffold supported stone floor",1614.5,216,382.5));add(place(Items.COBBLESTONE,SCAFFOLD,SCAFFOLD.below(),Direction.UP));
        add(camera("altar center supported approach",1474.5,216,378.8));
        add(pedestal(CENTER,Items.PISTON));
        Item[] ingredients={wood,wood,Items.GOLD_INGOT,Items.GOLD_INGOT,Items.DIAMOND_PICKAXE,Items.DIAMOND_SHOVEL,shard("aer"),shard("terra")};
        for(int i=0;i<8;i++) { BlockPos pos=PEDESTALS.get(i); add(camera("component approach "+i,pos.getX()+.5,216,pos.getZ()+1.7));add(pedestal(pos,ingredients[i])); }
        add(camera("lever bench",1601.5,216,377.5));add(open(BENCH,ContainerArcaneWorkbench.class));
        add(craft("native lever",grid(Items.STICK,null,null,Items.COBBLESTONE,null,null,null,null,null),Items.LEVER,1,false,-1));add(closeMenu());
        add(camera("teleport onto actual earned scaffold for lever placement",1614.5,217,383.5));
        add(place(Items.LEVER,LEVER,BASE,Direction.WEST));
        add(camera("north nozzle actual supported approach",1615.5,216,379.5));add(select(BoreClientSequence::payer));add(use(BASE,Direction.NORTH));
        add(camera("finite inherited furnace fuel menu",1465.5,216,379.5));add(open(FURNACE,ContainerAlchemyFurnace.class));
        add(transfer(-1,1,s->s.is(Items.COAL),2));add(closeMenu());
        add(camera("all nine source/operating tools at real bench",1601.5,216,377.5));add(open(BENCH,ContainerArcaneWorkbench.class));
        for(int i=0;i<9;i++) add(craft("thaumium pick "+(i+1),grid(thaumium(),thaumium(),thaumium(),null,Items.STICK,null,null,Items.STICK,null),pickItem(),1,false,-1));
        add(closeMenu());add(camera("actual distillation service floor",1465.5,216,379.5));
        add(new Distillation());
        add(camera("wide diagonal eleven-jar view from existing clear eastern floor",1485.5,216,377.5));
        add(aimForCapture("actual source row, not an inherited placement yaw",new Vec3(1479.5,216.5,370.5)));add(shot(5));
        add(serverOp("live pre-infusion guard: all112 costs/8count1/72candles/old333",()->ledger.infusionAdmission()));
        add(camera("real wand-to-matrix start",1474.5,216,379.2));add(select(BoreClientSequence::payer));add(use(MATRIX,Direction.SOUTH));
        add(waitFor("actual clean infusion started",80,()->ledger.infusionStartWitness && view!=null && view.crafting && view.instability==-3));
        add(waitFor("112 one-unit native draws +8 real ingredients + earned top",1610,()->view!=null && !view.crafting && view.center.is(topItem())));
        add(serverOp("native infusion completion ledger",()->ledger.infusionFinal()));
        add(once("expect genuine earned pedestal ejection",()->expect("earned Bore top",new ItemStack(topItem()),ALTAR_AREA)));
        add(select(ItemStack::isEmpty));add(use(CENTER,Direction.SOUTH));add(pickupExpected());add(shot(6));
        add(camera("old relay genuine Survival mine from inherited supported pillar",1618.5,218,376.5));
        add(once("bind old relay physical drop",()->expect("inherited relay",new ItemStack(relayItem()),BORE_AREA)));
        add(mineRelay());add(pickupExpected());
        add(camera("teleport onto actual earned scaffold for top placement",1614.5,217,383.5));
        add(place(topItem(),TOP,BASE,Direction.UP));
        add(waitFor("initial actual SOUTH head / UP attachment / credit0",80,()->view!=null && view.topPresent && view.direction==Direction.SOUTH && view.credit==0 && !view.relay));
        add(camera("real Bore UI supported northwest approach",1614.5,216,380.5));add(open(TOP,ContainerArcaneBore.class));
        add(transfer(-1,0,s->ItemStack.matches(s,ledger.savedFocus),1));add(transfer(-1,1,s->s.is(pickItem()) && s.getDamageValue()==0,1));
        add(waitFor("actual width5/speed1/fortune0 menu",80,()->client.screen instanceof GuiArcaneBore && client.player.containerMenu instanceof ContainerArcaneBore menu
                && menu.stats().speed()==1 && menu.stats().area()==0 && menu.stats().fortune()==0));add(shot(7));add(closeMenu());
        add(camera("first target genuine floor UP",1614.5,216,385.5));add(place(Items.COBBLESTONE,SLOW,SLOW.below(),Direction.UP));
        add(camera("slow36 real lever control and model",1614.5,216,380.5));add(toggle(true));
        add(waitFor("native slow36 completion, wear1, actual output UUID",1600,()->view!=null && view.harvests==1));
        add(camera("whole installed Bore and actual slow beam from supported southeast floor",1618.5,216,383.5));
        add(aimForCapture("whole earned model and actual slow beam",new Vec3(1615.5,217.6,382.0)));add(shot(8));
        add(camera("return to actual lever control after slow photo",1614.5,216,380.5));add(toggle(false));add(pickupHarvest(0));
        add(camera("same relay reinstallation from inherited supported pillar",1618.5,218,376.5));add(place(relayItem(),RELAY,RELAY.below(),Direction.UP));
        add(waitFor("native relay path/epoch raw Perditio prefill >=1",300,()->view!=null && view.relay && view.credit>=1 && ledger.rawGrants>0));
        add(camera("second target only AFTER actual credit",1614.5,216,386.5));add(place(Items.COBBLESTONE,FAST,FAST.below(),Direction.UP));
        add(camera("fast9 real lever control/model",1614.5,216,380.5));add(toggle(true));
        add(waitFor("native fast9 completion, wear2, actual output UUID",1600,()->view!=null && view.harvests==2));
        add(camera("whole installed Bore and actual fast beam from supported southeast floor",1618.5,216,383.5));
        add(aimForCapture("whole earned model and actual fast beam",new Vec3(1615.5,217.6,382.0)));add(shot(9));
        add(camera("return to actual lever control after fast photo",1614.5,216,380.5));add(toggle(false));add(pickupHarvest(1));add(closeMenu());
        add(waitFor("actual source-linked ambient deaths, no invented same-tick retirement",40,()->ledger.ambientBirthsRetired()));
        add(serverOp("full native final stock/protection gate",()->{ledger.finish();storyFinal=true;report.put("state","SCRIPT_COMPLETE_UNINSPECTED_NOT_ACCEPTED");}));
        add(once("release normal inputs, retain live audit tail",this::releaseInputs));
    }

    private record Click(int slot,int button,ClickType type) { Click(int slot,int button) { this(slot,button,ClickType.PICKUP); } }
    private Op clicks(String name,List<Click> values) {
        return new Op(name,Math.max(80,values.size()*10)) { int index,last=-10;
            @Override boolean poll() {
                if(clientTicks-last<3)return false;
                if(index==values.size())return client.player.containerMenu.getCarried().isEmpty();
                Click value=values.get(index++);click(value.slot,value.button,value.type);last=clientTicks;return false;
            }
        };
    }
    private Op select(Predicate<ItemStack> predicate) {
        return new Op("native hand ownership/selected-slot acknowledgement",100) { int sent=-1;
            @Override boolean poll() throws ReflectiveOperationException {
                require(client.player.containerMenu==client.player.inventoryMenu,"Hand selection only outside menus");
                if(!predicate.test(client.player.getInventory().getItem(HAND))) {
                    click(playerSlot(find(predicate)),HAND,ClickType.SWAP);selectedAt=clientTicks;return false;
                }
                if(clientTicks-selectedAt<4)return false;
                if(sent<0) { client.player.getInventory().selected=HAND;
                    Method method=client.gameMode.getClass().getDeclaredMethod("ensureHasSentCarriedItem");method.setAccessible(true);method.invoke(client.gameMode);sent=clientTicks;return false; }
                return clientTicks-sent>=4 && view!=null && view.selected==HAND && predicate.test(view.hand);
            }
        };
    }
    private Op holdSneak(boolean held) {
        return new Op("actual native Shift "+held,100) { int at=-1;
            @Override boolean poll() { if(at<0) {holdShift(held);at=clientTicks;return false;} return clientTicks-at>=3 && Screen.hasShiftDown()==held && view!=null && view.sneaking==held; }
        };
    }
    private Op closeMenu() {
        return new Op("normal menu/cursor/channel release (no stock restoration)",100) { boolean sent;
            @Override boolean poll() {
                require(client.player.containerMenu.getCarried().isEmpty(),"Do not close with unresolved cursor stock");
                if(!sent) { releaseUse();client.player.closeContainer();client.setScreen(null);sent=true;return false; }
                return view!=null && view.inventoryMenu && client.player.containerMenu==client.player.inventoryMenu;
            }
        };
    }
    private Op camera(String label,double x,double y,double z) {
        Op fixture=serverOp("SUPPORTED PLAYER CAMERA FIXTURE (not walking): "+label,()->{
            require(failure==null && !inert && !closed && !stopped,"No own camera fixture after failure/close/stop");
            ServerPlayer p=serverPlayer();
            require(!scanning && !mining && p.containerMenu==p.inventoryMenu,"No camera fixture during a native channel/menu");
            ledger.requireFixtureLoaded(x,y,z);
            AABB body=new AABB(x-.3,y+.001,z-.3,x+.3,y+p.getBbHeight(),z+.3);
            AABB support=new AABB(x-.299,y-.035,z-.299,x+.299,y+.002,z+.299);
            require(p.level().hasChunksAt(BlockPos.containing(x-1,y-1,z-1),BlockPos.containing(x+1,y+2,z+1))
                    && p.level().noCollision(p,body) && !p.level().noCollision(p,support),"Actual supported/collision-clear PLAYER camera; never install AIR support: "+label);
            require(p.level().getEntitiesOfClass(LivingEntity.class,body.inflate(.1),e->e!=p).isEmpty(),"Camera must not move/push another entity");
            require(failure==null && !inert && !closed && !stopped,"Do not apply own camera after a concurrent failure");
            Vec3 before=p.position();p.teleportTo(x,y,z);
             actions.add(row("event","DECLARED supported PLAYER camera fixture","label",label,"before",before.toString(),"after",p.position().toString(),"walking_claim",false,"other_entity_mutations",0));
         });
        return new Op(fixture.name,120) {boolean applied;int settled=-1;
            @Override boolean poll() throws ReflectiveOperationException {
                if(!applied) {if(!fixture.tick())return false;applied=true;}
                require(client.player!=null && PLAYER.equals(client.player.getUUID()),"Actual same client actor acknowledges supported teleport");
                if(client.player.position().distanceToSqr(new Vec3(x,y,z))>.000001) {settled=-1;return false;}
                if(settled<0)settled=clientTicks;
                return clientTicks-settled>=2; // Real packets/render state, no tick/clock write or deadline extension.
            }
        };
    }
    private Op aimForCapture(String label,Vec3 point) {
        return new Op("actual settled client photo aim: "+label,80) {int began=-1;
            @Override boolean poll() {
                require(client.player!=null && client.screen==null,"World camera, not a manufactured frame");rotate(point);
                if(began<0)began=clientTicks;
                return clientTicks-began>=3;
            }
        };
    }
    /** A genuine OUTLINE clip decides the packet's BLOCK hit/face/location. The
     * point is only an aim proposal; no synthetic AIR hit is ever transmitted. */
    private Op use(BlockPos pos,Direction face) {
        return new Op("real BLOCK use "+pos.toShortString()+"/"+face,100) { int aim=-1;
            @Override boolean poll() {
                require(client.screen==null && client.isWindowActive() && client.player.canInteractWithBlock(pos,0),"Actual Survival reach/foreground");
                var shape=client.level.getBlockState(pos).getShape(client.level,pos);require(!shape.isEmpty(),"Actual native interactable outline");
                AABB bounds=shape.bounds();Vec3 local=bounds.getCenter();
                if(face!=null)local=switch(face) {
                    case WEST->new Vec3(bounds.minX+.001,local.y,local.z);case EAST->new Vec3(bounds.maxX-.001,local.y,local.z);
                    case DOWN->new Vec3(local.x,bounds.minY+.001,local.z);case UP->new Vec3(local.x,bounds.maxY-.001,local.z);
                    case NORTH->new Vec3(local.x,local.y,bounds.minZ+.001);case SOUTH->new Vec3(local.x,local.y,bounds.maxZ-.001);};
                Vec3 point=Vec3.atLowerCornerOf(pos).add(local);rotate(point);
                if(aim<0) {aim=clientTicks;return false;} if(clientTicks-aim<5)return false;
                BlockHitResult hit=client.level.clip(new ClipContext(client.player.getEyePosition(),point.add(point.subtract(client.player.getEyePosition()).normalize().scale(.02)),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,client.player));
                require(hit.getType()==HitResult.Type.BLOCK && hit.getBlockPos().equals(pos) && (face==null || hit.getDirection()==face),"Real first BLOCK ray/face differs; never fabricate requested hit: "+pos+" expected "+face+" actual "+hit);
                require(hit.getLocation().distanceTo(client.player.getEyePosition())<=client.player.blockInteractionRange(),"Real native hit within Survival reach");
                require(client.gameMode.useItemOn(client.player,InteractionHand.MAIN_HAND,hit).consumesAction(),"Native use refused: "+pos);
                actions.add(row("event","actual client BLOCK-use intent","pos",pos.toShortString(),"face",hit.getDirection().name(),"location",hit.getLocation().toString(),"hand",client.player.getMainHandItem().toString()));return true;
            }
        };
    }
    private Op place(Item item,BlockPos at,BlockPos anchor,Direction face) {
        Op choose=select(s->s.is(item)), action=use(anchor,face);
        return new Op("native item placement "+BuiltInRegistries.ITEM.getKey(item)+" at "+at.toShortString(),200) { int state,before;
            @Override boolean poll() throws ReflectiveOperationException {
                if(state==0) { if(!choose.tick())return false;
                    require(anchor.relative(face).equals(at) && client.level.isEmptyBlock(at) && client.level.getBlockEntity(at)==null,"Live exact AIR-only destination; no clearing/replacing");
                    require(client.level.getEntitiesOfClass(Entity.class,new AABB(at),e->e!=client.player).isEmpty(),"No placement through another entity");
                    if(at.equals(FAST))require(view!=null && view.credit>=1 && view.harvests==1 && !view.powered,"Second target ONLY after real prefill/first pickup/powerOFF");
                    if(at.equals(SLOW))require(view!=null && view.credit==0 && !view.relay && !view.powered && client.level.isEmptyBlock(FAST),"Unassisted first target; second absent");
                    if(at.equals(TOP))require(view!=null && !view.relay && view.credit==0 && picked.stream().anyMatch(id->drops.get(id).is(relayItem())),"Relay same-UUID pickup BEFORE top exists");
                    before=count(client.player,item);state=1;
                }
                if(state==1) {if(!action.tick())return false;state=2;return false;}
                if(count(client.player,item)!=before-1 || client.level.isEmptyBlock(at))return false;
                require(client.level.getBlockState(at).getBlock().asItem()==item,"Actual placement identity differs");return true;
            }
        };
    }
    private Op open(BlockPos at,Class<? extends AbstractContainerMenu> type) {
        return open(at,type,null);
    }
    private Op open(BlockPos at,Class<? extends AbstractContainerMenu> type,Direction face) {
        Op choose=select(s->s.isEmpty() || s.is(Items.COBBLESTONE) || s.is(Items.STICK)), action=use(at,face);
        return new Op("real open "+at.toShortString()+" "+type.getSimpleName(),200) { int state;
            @Override boolean poll() throws ReflectiveOperationException {
                if(state==0) {require(!Screen.hasShiftDown(),"No native Shift bypass when opening menu");if(!choose.tick())return false;state=1;}
                if(state==1) {if(!action.tick())return false;state=2;return false;}
                return client.screen!=null && client.player.containerMenu!=client.player.inventoryMenu && type.isInstance(client.player.containerMenu) && view!=null && !view.inventoryMenu;
            }
        };
    }
    /** Source/destination -1 denotes the REAL player inventory, not a supplied
     * stack. Split by native right-clicks; return every cursor remainder. */
    private Op transfer(int source,int destination,Predicate<ItemStack> wanted,int amount) {
        return new Op("native slot transfer "+source+" -> "+destination+" x"+amount,300) { Op input; ItemStack expected;int actualDestination,beforeMain;long beforeTick;
            @Override boolean poll() throws ReflectiveOperationException {
                AbstractContainerMenu menu=client.player.containerMenu;
                if(input==null) {
                    require(menu!=client.player.inventoryMenu && menu.getCarried().isEmpty(),"Actual open menu/empty transfer cursor");
                    int src=source<0?playerSlot(find(wanted)):source;
                    ItemStack stack=menu.getSlot(src).getItem();require(!stack.isEmpty() && stack.getCount()>=amount,"Actual source quantity, never reconstruct stock");expected=stack.copyWithCount(amount);
                    beforeMain=count(client.player,stack.getItem());beforeTick=nativeTick();
                    actualDestination=destination<0?playerSlot(find(ItemStack::isEmpty)):destination;
                    ItemStack target=menu.getSlot(actualDestination).getItem();require(target.isEmpty() || ItemStack.isSameItemSameComponents(target,stack) && target.getCount()+amount<=target.getMaxStackSize(),"Exact native transfer room");
                    var list=new ArrayList<Click>();list.add(new Click(src,0));for(int i=0;i<amount;i++)list.add(new Click(actualDestination,1));list.add(new Click(src,0));input=clicks("actual funded split clicks",list);
                }
                if(!input.tick())return false;
                int expectedMain=beforeMain+(source<0?-amount:destination<0?amount:0);
                if(view==null || view.tick<=beforeTick || view.mainCounts.getOrDefault(expected.getItem(),0)!=expectedMain)return false;
                // Real native distillation may consume some right-click deposits
                // WHILE the remaining deposits are still being sent. Actor debit
                // is the acknowledgement; HEAD/RETURN separately witnesses every
                // cook. Never wait for a fictional unconsumed16/24 result count.
                if(menu instanceof ContainerAlchemyFurnace && destination==0)return true;
                ItemStack target=menu.getSlot(actualDestination).getItem();return ItemStack.isSameItemSameComponents(target,expected) && target.getCount()>=amount;
            }
        };
    }
    private Op takeChestShard(String tag) {
        return new Op("retrieve actual scanned "+tag+" shard (no new grant)",250) { Op move;
            @Override boolean poll() throws ReflectiveOperationException {
                if(move==null) {int source=-1;for(int i=0;i<27;i++)if(client.player.containerMenu.getSlot(i).getItem().is(shard(tag)))source=i;
                    require(source>=0,"Actual scanned retained chest shard");move=transfer(source,-1,null,1);}
                return move.tick();
            }
        };
    }
    private Op craft(String label,Item[] ingredients,Item output,int amount,boolean paid,int captureIndex) {
        return new Op("native real3x3 craft "+label,600) { int state,last;Op fill,photo;ItemStack beforePayer;int outSlot;
            @Override boolean poll() throws ReflectiveOperationException {
                require(client.player.containerMenu instanceof ContainerArcaneWorkbench && client.screen instanceof GuiArcaneWorkbench,"Native actual bench menu");
                var menu=(ContainerArcaneWorkbench)client.player.containerMenu;
                if(state==0) {
                    require(menu.getCarried().isEmpty(),"Craft begins empty cursor");
                    for(int i=2;i<11;i++)require(menu.getSlot(i).getItem().isEmpty(),"Never overwrite an occupied craft grid");
                    require(paid?!menu.getSlot(1).getItem().isEmpty():menu.getSlot(1).getItem().isEmpty(),"Ordinary crafts have NO wand; base uses only saved payer");
                    beforePayer=menu.getSlot(1).getItem().copy();fill=clicks("real3x3 exact ingredients "+label,recipeClicks(ingredients));state=1;
                }
                if(state==1) {if(!fill.tick())return false;state=2;last=clientTicks;return false;}
                if(state==2) {
                    if(clientTicks-last<5 || !menu.craftable() || !menu.getSlot(0).getItem().is(output))return false;
                    require(menu.getSlot(0).getItem().getCount()==amount,"Literal genuine output count");
                    for(int i=0;i<9;i++) {ItemStack actual=menu.getSlot(i+2).getItem();require(ingredients[i]==null?actual.isEmpty():actual.is(ingredients[i]) && actual.getCount()==1,"Exact registered recipe grid");}
                    for(String tag:WandData.PRIMALS)require(menu.requiredVis(tag)==(paid && (tag.equals("aer") || tag.equals("ordo"))?900:0),"Actual worn native900Aer/900Ordo ONLY for base");
                    if(captureIndex>=0)photo=shot(captureIndex);state=3;
                }
                if(state==3) {if(photo!=null && !photo.tick())return false;click(0,0,ClickType.PICKUP);state=4;last=clientTicks;return false;}
                if(state==4) {
                    if(clientTicks-last<3 || !menu.getCarried().is(output))return false;
                    require(menu.getCarried().getCount()==amount,"Real output ownership after payment");
                    if(paid) {verifyPayerDebit(beforePayer,menu.getSlot(1).getItem());earnedBase=menu.getCarried().copy();}
                    // Crafting freed real source slots; no temporary item deletion to make space.
                    outSlot=playerSlot(findRoom(menu.getCarried()));click(outSlot,0,ClickType.PICKUP);state=5;last=clientTicks;return false;
                }
                if(clientTicks-last<4 || !menu.getCarried().isEmpty())return false;
                for(int i=2;i<11;i++)require(menu.getSlot(i).getItem().isEmpty(),"All declared source components actually consumed");return true;
            }
        };
    }
    private List<Click> recipeClicks(Item[] ingredients) {
        var list=new ArrayList<Click>();var available=new int[36];for(int i=0;i<36;i++)available[i]=client.player.getInventory().getItem(i).getCount();
        for(int cell=0;cell<9;cell++)if(ingredients[cell]!=null) {
            int source=-1;for(int slot=0;slot<36;slot++)if(available[slot]>0 && client.player.getInventory().getItem(slot).is(ingredients[cell])) {source=slot;break;}
            require(source>=0,"Missing REAL craft ingredient "+ingredients[cell]);available[source]--;
            list.add(new Click(playerSlot(source),0));list.add(new Click(cell+2,1));list.add(new Click(playerSlot(source),0));
        }
        return List.copyOf(list);
    }
    private Op pedestal(BlockPos pos,Item item) {
        return sequence("real count1 pedestal ingredient",List.of(once("guard live empty owned pedestal",()->require(client.level.getBlockEntity(pos) instanceof TilePedestal tile && tile.isEmpty(),"Never overwrite/eject an occupied ingredient")),select(s->s.is(item)),use(pos,Direction.SOUTH),waitFor("actual count1 pedestal acknowledgement",80,
                ()->client.level.getBlockEntity(pos) instanceof TilePedestal tile && tile.getItem(0).is(item) && tile.getItem(0).getCount()==1)));
    }
    private Op toggle(boolean on) {
        return sequence("real lever "+on,List.of(select(s->s.isEmpty() || s.is(Items.COBBLESTONE)),once("guard actual lever edge",()->require(client.level.getBlockState(LEVER).is(Blocks.LEVER)
                && client.level.getBlockState(LEVER).getValue(LeverBlock.POWERED)!=on,"No duplicate lever toggle/reset")),use(LEVER,null),
                waitFor("actual native power acknowledgement",80,()->view!=null && view.powered==on)));
    }

    private Op book(boolean learned) {
        return new Op("actual inherited book / native ARTIFICE pan"+(learned?" learned":" request"),250) { Op choose=select(s->s.is(ThaumcraftContent.THAUMONOMICON.get()));int state,wait;
            @Override boolean poll() throws ReflectiveOperationException {
                if(state==0) {if(!choose.tick())return false;require(client.gameMode.useItem(client.player,InteractionHand.MAIN_HAND).consumesAction(),"Native book use");state=1;wait=clientTicks;return false;}
                if(!(client.screen instanceof GuiResearchBrowser browser) || clientTicks-wait<4)return false;
                if(!"ARTIFICE".equals(read(browser,"category"))) {
                    BookClientSmoke.clickCategoryTab(client,"ARTIFICE");
                    wait=clientTicks;return false;
                }
                int[] point=researchPoint(browser);int left=(Integer)read(browser,"graphLeft"),top=(Integer)read(browser,"graphTop"),width=(Integer)read(browser,"graphWidth"),height=(Integer)read(browser,"graphHeight");
                int dx=left+width/2-point[0],dy=top+height/2-point[1];
                if(Math.abs(dx)>6 || Math.abs(dy)>6) {require(browser.mouseDragged(left+width/2.0,top+height/2.0,0,dx,dy),"Actual native book graph drag");wait=clientTicks;return false;}
                require(KnowledgeAccess.get(client.player).isResearchComplete("ARCANEBORE")==learned,"Actual authoritative book learned state");
                return clientTicks-wait>=4;
            }
        };
    }
    private int[] researchPoint(GuiResearchBrowser browser) {
        var research=ResearchCatalog.get("ARCANEBORE");
        return new int[]{(Integer)read(browser,"graphLeft")+(Integer)read(browser,"graphWidth")/2+(int)(double)(Double)read(browser,"panX")+research.column()*24+11,
                (Integer)read(browser,"graphTop")+(Integer)read(browser,"graphHeight")/2+(int)(double)(Double)read(browser,"panY")+research.row()*24+11};
    }
    private void clickResearchEntry() {
        require(client.screen instanceof GuiResearchBrowser,"Real inherited book request screen");var browser=(GuiResearchBrowser)client.screen;int[] point=researchPoint(browser);
        require(browser.mouseClicked(point[0],point[1],0) && client.screen==browser,"ONE original primary node click");browser.mouseReleased(point[0],point[1],0);
    }
    private Op combine(String first,String second,String result,boolean buffer) {
        return new Op("actual table combine "+first+"+"+second+"="+result,180) { BoreResearchGuiActions.Sent sent;Map<String,Integer> before;int resultBefore;boolean known;
            @Override boolean poll() {
                var menu=(ContainerResearchTable)client.player.containerMenu;
                if(sent==null) {
                    if(!menu.editReady())return false;
                    if(System.nanoTime()<(Long)read(client.screen,"combinePressedUntil"))return false;
                    if(!showPair(first,second))return false;
                    // Both must be in the SAME real viewport, not oscillating pages forever.
                    var knowledge=KnowledgeAccess.get(client.player);before=knowledge.getAspectsDiscovered();resultBefore=knowledge.getAspectPoolFor(result);known=knowledge.hasDiscoveredAspect(result);
                    require(before.getOrDefault(first,0)>0 && before.getOrDefault(second,0)>0,"Spend actual positive actor pools only; no table-bonus assumption");
                    if(buffer)require(bufferCombos<6,"No more than six predeclared real motus buffer combinations");
                    sent=BoreResearchGuiActions.combineVisible(client,first,second);totalCombos++;if(buffer)bufferCombos++;require(totalCombos<=19,"13+at most6 combinations");return false;
                }
                if(!menu.editReady())return false;
                require(menu.sessionToken()==sent.session() && menu.tableRevision()==sent.revision()+1,"ONE accepted native combination revision; no silent retry");
                var now=KnowledgeAccess.get(client.player).getAspectsDiscovered();require(now.getOrDefault(result,0)==resultBefore+(known?1:3),"Actual first-known output1 / new discovery3");
                for(String tag:before.keySet())require(now.getOrDefault(tag,0)==before.get(tag)-(tag.equals(first)?1:0)-(tag.equals(second)?1:0)+(tag.equals(result)?(known?1:3):0),"Exact actor-RP combine ledger; untouched pools preserved: "+tag);
                require(ItemStack.matches(menu.getSlot(0).getItem(),sent.inkBefore()) && ItemStack.matches(menu.getSlot(1).getItem(),sent.noteBefore()),"Native combinations consume no ink/note");
                actions.add(row("event","actual acknowledged funded combination","container",sent.container(),"session",sent.session(),"revision_before",sent.revision(),"revision_after",menu.tableRevision(),"before",before,"after",now,"result",result));return true;
            }
        };
    }
    private boolean showPair(String first,String second) {
        require(client.screen instanceof GuiResearchTable,"Native palette only");
        List<String> tags=KnowledgeAccess.get(client.player).getAspectsDiscovered().keySet().stream().sorted().toList();
        int a=tags.indexOf(first),b=tags.indexOf(second);require(a>=0 && b>=0,"Both canonical parents really discovered");
        int low=Math.max(0,(Math.max(a,b)-24+4)/5),high=Math.min(Math.min(a,b)/5,Math.max(0,(tags.size()-20)/5));
        require(low<=high,"The two real parents cannot coexist in a native25-icon viewport; no private palette setter");
        int page=(Integer)read(client.screen,"page");if(page>=low && page<=high)return true;
        int x=(client.screen.width-255)/2+(page<low?63:39),y=(client.screen.height-255)/2+125;
        require(client.screen.mouseClicked(x,y,0),"Real palette paging input");client.screen.mouseReleased(x,y,0);return false;
    }
    private Op solveActualNote() {
        return new Op("same issued note / actual-funded bounded planner / acknowledged native hex drags",1600) {
            BoreResearchNotePlan.Plan plan;Op fund,photo;int index;BoreResearchGuiActions.Sent sent;Map<String,Integer> before;Map<String,Integer> bonusBefore;
            @Override boolean poll() throws ReflectiveOperationException {
                require(client.screen instanceof GuiResearchTable && client.player.containerMenu instanceof ContainerResearchTable,"Actual note table only");
                var menu=(ContainerResearchTable)client.player.containerMenu;
                if(fund!=null) {if(!fund.tick())return false;fund=null;plan=null;return false;}
                if(photo!=null) {if(!photo.tick())return false;photo=null;}
                if(!menu.editReady())return false;
                ResearchNoteData note=ItemResearchNotes.data(menu.getSlot(1).getItem());require(note!=null,"Actual table note component");
                if(sent!=null) {
                    require(menu.sessionToken()==sent.session() && menu.tableRevision()==sent.revision()+1,"ONE accepted placement revision; never re-send stale request");
                    require(menu.getSlot(0).getItem().getDamageValue()==sent.inkBefore().getDamageValue()+1,"One literal native ink per acknowledged edit");
                    var cell=note.cellAt(sent.q(),sent.r());require(cell!=null && cell.type()==2 && sent.first().equals(cell.aspect()),"Actual native requested hex placed");
                    verifyAcknowledgedCells(ItemResearchNotes.data(sent.noteBefore()),note,sent);
                    Map<String,Integer> now=KnowledgeAccess.get(client.player).getAspectsDiscovered();int debit=before.get(sent.first())-now.getOrDefault(sent.first(),0);
                    require(debit==0 || debit==1,"Observe genuine R2-free result or one RP, NEVER predict/reset RNG");
                    for(String tag:before.keySet())if(!tag.equals(sent.first()))require(before.get(tag).equals(now.get(tag)),"Unrelated actual pools unchanged by PLACE");
                    require(menu.bonusPools().equals(bonusBefore),"Positive actor-funded placement must not silently consume table bonus");
                    actions.add(row("event","actual acknowledged native hex edit","container",sent.container(),"session",sent.session(),"revision",menu.tableRevision(),"q",sent.q(),"r",sent.r(),"aspect",sent.first(),"actual_actor_RP_debit",debit,"ink_damage",menu.getSlot(0).getItem().getDamageValue(),"native_complete",note.complete()));
                    sent=null;index++;
                    if(index==1)photo=shot(2);
                }
                if(note.complete()) {
                    require(menu.getSlot(1).getItem().is(ThaumcraftContent.DISCOVERY.get()) && index>0,"Native final edit produced real discovery item");
                    require(menu.getSlot(0).getItem().getDamageValue()<=30,"Declared ink<=30");
                    if(photo!=null)return false;
                    front(List.of(transfer(1,-1,null,1)));return true;
                }
                if(photo!=null)return false;
                if(plan==null) {
                    require(index==0 && issued.equals(note),"Readonly buffer replan SAME actual cells; no paid edits until validated READY");
                    require(fixedGeometryConnected(note),"Actual one-note hole geometry disconnects fixed endpoints: retain note/resources; no buffer or reroll");
                    Map<String,Integer> all=KnowledgeAccess.get(client.player).getAspectsDiscovered();var budget=new TreeMap<String,Integer>();
                    for(String tag:PLAN_TAGS)if(all.containsKey(tag)) {int raw=all.get(tag);require(raw>=0 && raw<=Short.MAX_VALUE,"Normal actual signed-short prerequisite");budget.put(tag,raw);}
                    long start=System.nanoTime();plan=BoreResearchNotePlan.plan(note,all.keySet(),budget);long nanos=System.nanoTime()-start;
                    require(nanos<=30_000_000_000L,"Planner30wall-second cap; no native clock rewrites");
                    actions.add(row("event","actual READONLY note plan","same_note_hash",BoreResearchNotePlan.fingerprint(note),"actual_funded_subset",budget,"status",plan.status().name(),"reason",plan.reason(),"stats",plan.stats(),"placements",plan.placements(),"costs",plan.rpCosts(),"CPU_nanos",nanos));
                    if(!plan.ready()) {
                        require((plan.status()==BoreResearchNotePlan.Status.PROVEN_UNREACHABLE || plan.status()==BoreResearchNotePlan.Status.SEARCH_EXHAUSTED) && bufferCombos<6,"Actual note unsolved/cap failure after bounded funding: retain note/ink/resources; STOP BEFORE B");
                        require(all.getOrDefault("aer",0)>0 && all.getOrDefault("ordo",0)>0,"No invented future funding");
                        fund=combine("aer","ordo","motus",true);return false;
                    }
                    require(plan.validateAgainst(note,all.keySet(),budget).valid(),"Actual input-bound detached certificate BEFORE first paid edit");report.put("actual_plan",row("placements",plan.placements(),"costs",plan.rpCosts(),"stats",plan.stats()));
                }
                require(index<plan.placements().size(),"Planner exhausted without native completion; never invent complete flag");
                var placement=plan.placements().get(index);if(!BoreResearchGuiActions.makeVisible(client,placement.aspect()))return false;
                before=KnowledgeAccess.get(client.player).getAspectsDiscovered();bonusBefore=menu.bonusPools();
                require(before.getOrDefault(placement.aspect(),0)>0 && ++editActions<=29,"Actual funded blank-cell action; finite29 edits");
                sent=BoreResearchGuiActions.placeVisible(client,placement.aspect(),placement.q(),placement.r());return false;
            }
        };
    }

    private Op shot(int index) {
        return new Op("await ACTUAL native PNG completion "+CAPTURES.get(index),300) { boolean sent,checked;
            final Op boundary=serverOp("actual passive witness boundary before PNG "+index,()->ledger.captureCheckpoint(CAPTURES.get(index)));
            @Override boolean poll() throws ReflectiveOperationException {
                if(!checked) {if(!boundary.tick())return false;checked=true;}
                if(!sent) {require(index==shots++,"Exactly ten contiguous174..183 captures, no placeholders");captureEvent("requested",CAPTURES.get(index));capture.accept(CAPTURES.get(index));sent=true;return false;}
                return captures.stream().anyMatch(row->"after_render_before_native_PNG_grab".equals(row.get("event")) && CAPTURES.get(index).equals(row.get("name")))
                        && captures.stream().anyMatch(row->"native_PNG_written".equals(row.get("event")) && CAPTURES.get(index).equals(row.get("name")));
            }
        };
    }

    private Op scanNode() {
        Op choose=select(s->s.is(ThaumcraftContent.THAUMOMETER.get()));
        return new Op("actual node20native-tick/10block first scan",150) { int state;long began;
            @Override boolean poll() throws ReflectiveOperationException {
                if(state==0) {if(!choose.tick())return false;
                    scanPoint=new Vec3(1591.5,218.5,377.5);rotate(scanPoint);
                    require(client.player.getEyePosition().distanceTo(new Vec3(1591.7,218.5,377.5))<10,"Actual small node outline ray within10, not a full-cube hit");
                    require(view!=null && !view.phenomena.contains("@NODE1b1965a9-747f-4d43-82ff-91e1f0ab443b"),"Genuine first accepted node identity");
                    ledger.expectedScan="@NODE1b1965a9-747f-4d43-82ff-91e1f0ab443b";began=nativeTick();state=1;return false;
                }
                if(state==1) {if(nativeTick()-began<6)return false;startScanner();state=2;began=nativeTick();return false;}
                if(!view.phenomena.contains(ledger.expectedScan) || !KnowledgeAccess.get(client.player).hasScannedPhenomena(ledger.expectedScan))return false;
                require(nativeTick()-began>=20,"No instantaneous scan award");releaseUse();return true;
            }
        };
    }
    private Op scanDropped(Predicate<ItemStack> wanted,String label) {
        return new Op("real drop -> stable20tick scan -> SAME UUID contact: "+label,300) {
            Op child;int state;UUID id;String signature;long began;ItemStack original;
            @Override boolean poll() throws ReflectiveOperationException {
                if(child!=null) {if(!child.tick())return false;child=null;state++;return false;}
                if(state==0) {child=camera("declared research catch pad scan camera",1604.5,216,379.5);return false;}
                if(state==1) {child=select(wanted);return false;}
                if(state==2) {
                    original=client.player.getMainHandItem().copyWithCount(1);require(!original.isEmpty(),"Real item to scan");
                    signature="@"+BuiltInRegistries.ITEM.getKey(original.getItem());require(!view.scanned.contains(signature),"Distinct first ITEM-ID only; no repeated shard/wand farm");
                    rotate(new Vec3(1604.5,216.4,382));expect(label,original,RESEARCH_AREA);
                    click(playerSlot(HAND),0,ClickType.THROW);state=3;return false;
                }
                if(state==3) {if(lastDrop==null)return false;id=lastDrop;require(ItemStack.matches(drops.get(id),original),"Actual dropped full components/UUID");child=camera("supported scan offset, never move drop/pickup delay",1604.5,216,379.0);return false;}
                if(state==4) {child=select(s->s.is(ThaumcraftContent.THAUMOMETER.get()));return false;}
                if(state==5) {
                    require(view.dropPositions.containsKey(id),"Same actual physical target still exists");scanPoint=view.dropPositions.get(id).add(0,.12,0);rotate(scanPoint);
                    ledger.expectedScan=signature;began=nativeTick();state=6;return false;
                }
                if(state==6) {if(nativeTick()-began<6)return false;startScanner();began=nativeTick();state=7;return false;}
                if(state==7) {
                    if(!view.scanned.contains(signature) || !KnowledgeAccess.get(client.player).hasScannedItem(signature)) {
                        require(view.dropPositions.containsKey(id) && !picked.contains(id),"Do not lose/contact the target before its real first scan");
                        scanPoint=view.dropPositions.get(id).add(0,.12,0);return false;
                    }
                    require(nativeTick()-began>=20,"Actual20-tick scan, not a reward backend");releaseUse();child=pickup(id);return false;
                }
                require(picked.contains(id),"Same UUID native playerTouch RETURN receipt required");return true;
            }
        };
    }
    private void startScanner() {
        require(client.isWindowActive() && client.screen==null && client.player.getMainHandItem().is(ThaumcraftContent.THAUMOMETER.get()),"Owned real scanner channel");
        scanning=true;client.options.keyUse.setDown(true);
        require(client.gameMode.useItem(client.player,InteractionHand.MAIN_HAND).consumesAction(),"Native scanner item-use intent");
    }
    record Expectation(String kind,ItemStack stack,AABB area) { Expectation {stack=stack.copy();} }
    private void expect(String kind,ItemStack stack,AABB area) {
        require(expectedDrop==null,"No overlapping physical drop expectation");expectedDrop=new Expectation(kind,stack,area);lastDrop=null;
    }
    private Op pickupExpected() {
        return new Op("actual expected UUID pickup acknowledgement",240) { Op child;
            @Override boolean poll() throws ReflectiveOperationException {if(lastDrop==null)return false;if(child==null)child=pickup(lastDrop);return child.tick();}
        };
    }
    private Op pickupHarvest(int index) {
        return new Op("actual ejection UUID"+index+" contact pickup",240) {Op child;
            @Override boolean poll() throws ReflectiveOperationException {
                UUID id=ledger.harvestIds.get(index);if(id==null)return false;if(child==null)child=pickup(id);return child.tick();
            }
        };
    }
    private Op pickup(UUID id) {
        return new Op("same-UUID native contact "+id,220) {boolean moved,sent;volatile boolean contactMoved,noSupportObserved;long lastProbe=-1;
             @Override boolean poll() {
                if(sent) {if(!serverDone())return false;sent=false;moved=contactMoved;}
                if(picked.contains(id))return true;
                require(drops.containsKey(id),"Keep actual physical UUID; never spawn/move a replacement");
                if(!moved && nativeTick()!=lastProbe && serverDone()) {
                     submit(()->{
                        if(picked.contains(id))return; // Actual native contact may finish BEFORE this queued read.
                         Entity entity=owner.serverLevel().getEntity(id);require(entity instanceof ItemEntity item && ItemStack.matches(item.getItem(),drops.get(id)),"Unchanged actual UUID/full contents until contact");
                         // Use the ACTUAL current server point, not an older POST
                         // captured before this task. Airborne drops may have no
                         // supported contact camera yet; native physics continues
                         // inside the SAME original220-tick bound, no item writes.
                         for(Vec3 candidate:BoreClientEvidenceGeometry.contactCandidates(entity.position())) {
                             double x=candidate.x,y=candidate.y,z=candidate.z;AABB body=new AABB(x-.3,y+.001,z-.3,x+.3,y+owner.getBbHeight(),z+.3);
                             if(!owner.level().hasChunksAt(BlockPos.containing(x-1,y-1,z-1),BlockPos.containing(x+1,y+2,z+1)))continue;
                             if(!body.inflate(1,.5,1).intersects(entity.getBoundingBox()) || !owner.level().noCollision(owner,body))continue;
                             AABB floor=new AABB(x-.299,y-.035,z-.299,x+.299,y+.002,z+.299);
                             if(owner.level().noCollision(owner,floor) || !owner.level().getEntitiesOfClass(LivingEntity.class,body.inflate(.1),e->e!=owner).isEmpty())continue;
                              require(failure==null && !inert && !closed && !stopped,"Do not apply own contact camera after a concurrent failure");
                              Vec3 before=owner.position();owner.teleportTo(x,y,z);contactMoved=true;
                             actions.add(row("event","supported PLAYER contact camera fixture ONLY","UUID",id.toString(),"native_tick",owner.server.getTickCount(),"native_game_time",owner.serverLevel().getGameTime(),"player_before",before.toString(),"player_after",owner.position().toString(),"item_position_UNCHANGED",entity.position().toString(),"item_writes",0));return;
                         }
                         if(!noSupportObserved) {noSupportObserved=true;actions.add(row("event","actual owned item temporarily has no supported contact camera; wait for native physics within original220ticks",
                                 "UUID",id.toString(),"native_tick",nativeTick(),"actual_item_position",entity.position().toString(),"actual_item_velocity",entity.getDeltaMovement().toString(),"candidate_count",125,"item_writes",0,"time_extensions",0));}
                     });lastProbe=nativeTick();sent=true;
                 }
                 return false; // Only actual native playerTouch RETURN may acknowledge pickup.
            }
        };
    }
    private Op mineRelay() {
        Op choose=select(s->s.is(Items.WOODEN_PICKAXE));
        return new Op("native mine old relay / wood-pick5->6",300) {int state,aim;
            @Override boolean poll() throws ReflectiveOperationException {
                if(state==0) {if(!choose.tick())return false;require(client.player.getMainHandItem().getDamageValue()==5,"Same saved worn wood pick5");state=1;aim=clientTicks;}
                if(state==1) {
                    rotate(Vec3.atCenterOf(RELAY));if(clientTicks-aim<6)return false;
                    HitResult ray=client.player.pick(client.player.blockInteractionRange(),1,false);
                    require(ray instanceof BlockHitResult hit && hit.getType()==HitResult.Type.BLOCK && hit.getBlockPos().equals(RELAY),"Actual relay mining ray");
                    require(client.player.getMainHandItem().isCorrectToolForDrops(client.level.getBlockState(RELAY)),"Real correct saved pick");
                    mining=true;client.options.keyAttack.setDown(true);require(client.gameMode.startDestroyBlock(RELAY,((BlockHitResult)ray).getDirection()),"Native Survival destroy begins");state=2;return false;
                }
                rotate(Vec3.atCenterOf(RELAY));if(!client.level.isEmptyBlock(RELAY))return false;
                mining=false;client.options.keyAttack.setDown(false);client.gameMode.stopDestroyBlock();
                return view!=null && !view.relay && client.player.getMainHandItem().getDamageValue()==6;
            }
        };
    }

    private static final List<String> JAR_TAGS=List.of("metallum","metallum","praecantatio","perfodio","arbor","terra","perditio","motus","machina","potentia","vacuos");
    private static final int[] JAR_AMOUNTS={64,20,32,32,16,12,12,16,32,48,16};
    static BlockPos jarPos(int index) {return p(1465+2*index,216,370);}
    private final class Distillation extends Op {
        final int[] quantities={8,4,24,16},deadlines={3600,2400,2200,900};
        int batch,admitted;long start=-1,pickStart=-1;Op child;boolean labelBound,removed;int metal=-1;
        final Set<Integer> placed=new LinkedHashSet<>();
        Distillation() {super("genuine finite300 source servicing / no tag-order or pulse assumptions",9200);}
        @Override boolean poll() throws ReflectiveOperationException {
            if(child!=null) {if(!child.tick())return false;child=null;return false;}
            require(view!=null,"Actual native furnace view");
            if(batch==4) {
                if(labelBound && !removed) {
                    if(placed.size()!=10) {if(placeReadyJar())return false;throw new IllegalStateException("Ten final jars plus the actual retained vacuos jar are required before label removal");}
                    require(view.alembics.get(metal).amount==0 && "metallum".equals(view.alembics.get(metal).filter),"Remove actual metal label ONLY at amount0");
                    expect("return actual blank label",ItemJarLabel.blank(),ALTAR_AREA);
                    Predicate<ItemStack> retainedJar=s->ItemJar.isJar(s) && ItemJar.getAspect(s)!=null
                            && ItemJar.getAspect(s).getTag().equals("vacuos") && ItemJar.getAmount(s)==16;
                    require(!placed.contains(10) && findOptional(retainedJar)>=0,"Retain ONE actual eleven-vessel jar; never change the inherited offhand");
                    child=sequence("native held-jar Shift-use returns blank label / same UUID contact",List.of(camera("actual empty filtered alembic",1465.5,216,379.5),select(retainedJar),holdSneak(true),
                             use(FURNACE.above(metal+1),Direction.SOUTH),holdSneak(false),pickupExpected()));removed=true;return false;
                }
                if(placed.size()!=11) {if(placeReadyJar())return false;throw new IllegalStateException("All sources drained but final physical eleven-jar ledger is incomplete");}
                require(ledger.sourceTotal==300 && view.sourceStock.equals(PRODUCED),"All300 retained in actual jars before activation");return true;
            }
            if(start<0) {start=nativeTick();if(batch==0)pickStart=start;}
            require(nativeTick()-start<=deadlines[batch],"Finite source phase ceiling; no fuel extras or clock resets: "+batch);
            if(batch==0 && admitted<8)require(nativeTick()-pickStart<=3200,"All eight real source picks must enter within3200 native ticks");
            // Bind the EXISTING blank label to the ACTUAL first live metal buffer.
            if(!labelBound)for(int i=0;i<view.alembics.size();i++) {
                Alembic a=view.alembics.get(i);
                if(a.amount>0 && a.tag.equals("metallum")) {
                    metal=i;labelBound=true;
                    child=sequence("native metal-label binding, no type/RNG setter",List.of(camera("real metal alembic label approach",1465.5,216,379.5),select(s->s.getItem() instanceof ItemJarLabel && ItemJarLabel.aspect(s)==null),use(FURNACE.above(i+1),Direction.SOUTH),
                            waitFor("actual metallum filter bound by consumed label",80,()->"metallum".equals(view.alembics.get(metal).filter))));return false;
                }
            }
            // Feed an unstackable source promptly. The ninth genuine pick is reserved.
            if(view.furnaceInput.isEmpty() && admitted<quantities[batch]) {
                Item input=batch==0?pickItem():batch==1?Items.PISTON:batch==2?Items.REDSTONE:phial();int n=batch==0?1:quantities[batch];admitted+=n;
                child=sequence("actual source input menu, batch"+batch,List.of(camera("supported source input approach",1465.5,216,379.5),open(FURNACE,ContainerAlchemyFurnace.class),transfer(-1,0,s->s.is(input) && (input!=pickItem() || s.getDamageValue()==0),n),closeMenu()));return false;
            }
            if(placeReadyJar())return false;
            // Keep typed positive buffers alive for the native first-pass
            // parallel refill. Greedy one-unit extraction starves that pass.
            boolean batchCooked=admitted==quantities[batch] && view.furnaceInput.isEmpty() && ledger.consumed[batch]==quantities[batch];
            var order=BoreClientEvidence.sourceBuffersToDrain(view.furnaceAspects,
                    view.alembics.stream().map(a->new BoreClientEvidence.SourceBuffer(a.tag,a.amount,a.filter)).toList(),batchCooked);
            for(int i:order) {
                Alembic a=view.alembics.get(i);if(a.amount<=0)continue;
                Predicate<ItemStack> compatible=s->ItemJar.isJar(s) && !ItemJar.isVoid(s) && ItemJar.getAspect(s)!=null && ItemJar.getAspect(s).getTag().equals(a.tag) && ItemJar.getAmount(s)<64;
                int existing=findOptional(compatible);boolean newJar=existing<0;
                if(newJar && findOptional(ItemStack::isEmpty)<0)continue; // Never mint a slot or discard collateral.
                if(newJar)require(findOptional(ItemJar::isEmptyIngredient)>=0,"Actual eleven-vessel stock exhausted; no extra jars");
                Predicate<ItemStack> chosen=newJar?ItemJar::isEmptyIngredient:compatible;
                int beforeJar=view.jarStock.getOrDefault(a.tag,0);long revisionTick=view.tick;
                child=sequence("native whole-jar extraction of actual positive "+a.tag,List.of(camera("actual jar/alembic approach",1465.5,216,379.5),select(chosen),
                        once("guard actual positive alembic before jar use",()->require(!Screen.hasShiftDown() && view.alembics.get(i).amount>0 && view.alembics.get(i).tag.equals(a.tag),"Never sneak-clear or withdraw stale tag")),use(FURNACE.above(i+1),Direction.SOUTH),
                        waitFor("actual native jar transfer receipt",80,()->view.tick>revisionTick && view.jarStock.getOrDefault(a.tag,0)>beforeJar)));
                return false;
            }
            if(admitted==quantities[batch] && view.furnaceInput.isEmpty() && view.furnaceVis==0 && view.alembics.stream().allMatch(a->a.amount==0)) {
                require(ledger.consumed[batch]==quantities[batch],"Native actual source consumption, not admitted count");batch++;admitted=0;start=-1;return false;
            }
            require(view.furnaceBurn>0 || view.furnaceFuel.getCount()>0 || view.furnaceInput.isEmpty(),"Finite five-coal stock spent with unfinished cook; retain stock and fail");return false;
        }
        private boolean placeReadyJar() {
            for(int i=0;i<11;i++)if(!placed.contains(i)) {
                // A real held jar dispatches label removal despite the preserved
                // occupied offhand; empty MAIN + Shift cannot reach that branch.
                if(i==10 && labelBound && !removed)continue;
                int index=i;String tag=JAR_TAGS.get(i);int amount=JAR_AMOUNTS[i];
                Predicate<ItemStack> exact=s->ItemJar.isJar(s) && ItemJar.getAspect(s)!=null && ItemJar.getAspect(s).getTag().equals(tag) && ItemJar.getAmount(s)==amount;
                if(findOptional(exact)<0)continue;
                // Small metal8/arbor8/machina8 are CARRIED until genuinely final.
                placed.add(i);child=sequence("place actual final source jar "+index,List.of(camera("new jar row supported floor "+index,jarPos(i).getX()+.5,216,372.5),
                        select(exact),place(jarItem(),jarPos(i),jarPos(i).below(),Direction.UP)));return true;
            }
            return false;
        }
    }

    record Alembic(String tag,int amount,String filter) {}
    record View(long tick,Set<String> research,Map<String,Integer> pools,Set<String> scanned,Set<String> phenomena,
                int selected,ItemStack hand,boolean sneaking,boolean inventoryMenu,Map<Item,Integer> mainCounts,ItemStack furnaceInput,ItemStack furnaceFuel,
                int furnaceBurn,int furnaceCook,int furnaceVis,Map<String,Integer> furnaceAspects,List<Alembic> alembics,Map<String,Integer> jarStock,Map<String,Integer> sourceStock,
                boolean crafting,int instability,Map<String,Integer> demand,ItemStack center,
                boolean topPresent,boolean relay,boolean powered,Direction direction,float credit,int countdown,BlockPos target,
                int harvests,Map<UUID,Vec3> dropPositions) {}
    private static boolean fixedGeometryConnected(ResearchNoteData note) {
        var remaining=new ArrayList<>(note.cells());var reached=new HashSet<ResearchNoteData.Cell>();var pending=new ArrayDeque<ResearchNoteData.Cell>();
        ResearchNoteData.Cell first=remaining.stream().filter(c->c.type()==1).findFirst().orElseThrow();reached.add(first);pending.add(first);
        int[][] neighbours={{1,0},{1,-1},{0,-1},{-1,0},{-1,1},{0,1}};
        while(!pending.isEmpty()) {var at=pending.removeFirst();for(int[] delta:neighbours) {var next=note.cellAt(at.q()+delta[0],at.r()+delta[1]);if(next!=null && reached.add(next))pending.addLast(next);}}
        return remaining.stream().filter(c->c.type()==1).allMatch(reached::contains);
    }
    private static void verifyAcknowledgedCells(ResearchNoteData before,ResearchNoteData after,BoreResearchGuiActions.Sent sent) {
        require(before!=null && before.key().equals(after.key()) && before.color()==after.color() && before.copies()==after.copies(),"All observed note metadata preserved across native PLACE");
        var expected=new ArrayList<ResearchNoteData.Cell>();
        for(var cell:before.cells())expected.add(cell.q()==sent.q() && cell.r()==sent.r()?new ResearchNoteData.Cell(cell.q(),cell.r(),2,sent.first()):cell);
        if(!after.complete())require(after.cells().equals(expected),"Only ONE observed blank changes; native order/holes/all other labels immutable");
        else {
            int last=-1;for(var cell:after.cells()) {int index=expected.indexOf(cell);require(index>last && cell.type()>0,"Native solved copy only prunes occupied root connectivity in original order, never adds/changes labels");last=index;}
            for(var cell:expected)if(cell.type()==1)require(after.cells().contains(cell),"Every actual fixed endpoint retained at native completion");
        }
    }

    private void deliver(boolean b) {
        require(failure==null && !inert && !closed && !stopped,"No own raw delivery after failure/close/stop");
        require(owner!=null && owner.server.isSameThread() && ledger!=null && clientHeart>0 && serverHeart>0,"Real native preflight/thread/snapshot before raw delivery");
        if(b)require(rDelivered && researchDone && !boreDelivered && KnowledgeAccess.get(owner).isResearchComplete("ARCANEBORE"),"No B inputs before actual native learning; no retries/extras");
        else require(!rDelivered && !KnowledgeAccess.get(owner).isResearchComplete("ARCANEBORE"),"One actual fresh research prelude only");
        require(owner.containerMenu==owner.inventoryMenu && !owner.isUsingItem(),"Normal empty-cursor delivery context");
        for(ItemStack declared:rawInputs(b)) {
            require(failure==null && !inert && !closed && !stopped,"Stop remaining own raw insertions after a recorded failure");
            ItemStack actual=declared.copy();require(owner.getInventory().add(actual) && actual.isEmpty(),"Predeclared finite raw stock must genuinely fit; never delete to make room");
            inputs.add(row("phase",b?"B":"R","event","ONCE predeclared RAW delivery","server_tick",owner.server.getTickCount(),"actual_stack",snbt(declared,owner)));
        }
        if(b)boreDelivered=true;else rDelivered=true;
        owner.getInventory().setChanged();owner.containerMenu.broadcastChanges();
    }
    private static List<ItemStack> rawInputs(boolean b) {
        if(!b)return List.of(new ItemStack(Items.OAK_PLANKS,15),new ItemStack(Items.GOLD_INGOT,2),new ItemStack(Items.GLASS),
                new ItemStack(shard("aer"),3),new ItemStack(shard("ignis")),new ItemStack(shard("aqua")),new ItemStack(shard("terra")),
                new ItemStack(shard("ordo")),new ItemStack(shard("perditio")),new ItemStack(Items.FEATHER),new ItemStack(Items.INK_SAC),new ItemStack(Items.PAPER),new ItemStack(Items.COBBLESTONE,4));
        return List.of(new ItemStack(ThaumcraftNaturals.GREATWOOD_PLANKS.get(),6),new ItemStack(Items.IRON_INGOT,4),new ItemStack(Items.DISPENSER),
                new ItemStack(Items.PISTON,5),new ItemStack(Items.DIAMOND_PICKAXE),new ItemStack(Items.DIAMOND_SHOVEL),new ItemStack(thaumium(),27),
                new ItemStack(Items.STICK,19),new ItemStack(phial(),16),new ItemStack(Items.REDSTONE,24),new ItemStack(jarItem(),11),new ItemStack(Items.COAL,2),new ItemStack(Items.COBBLESTONE));
    }
    private static Map<String,Integer> rawDeclaration(boolean b) {
        var result=new LinkedHashMap<String,Integer>();for(ItemStack value:rawInputs(b))result.put(BuiltInRegistries.ITEM.getKey(value.getItem()).toString(),value.getCount());return Map.copyOf(result);
    }

    private void serverPost(MinecraftServer server) {
        if(owner==null || server!=owner.server || stopped || ledger==null)return;
        try {ledger.post();}catch(Throwable error) {fail(error);}
    }
    private EventResult entityAdded(Entity entity,Level level) {
        if(owner==null || level!=owner.level() || stopped || ledger==null)return EventResult.pass();
        try {ledger.entityAdded(entity);}catch(Throwable error) {fail(error);}
        return EventResult.pass(); // This observer NEVER vetoes a native addition.
    }
    public static void nativeClientTick(Minecraft client) {
        BoreClientSequence self=active;if(self==null || self.client!=client || self.stopped)return;
        try {
            require(client.isSameThread() && nativeFrame("net.minecraft.client.Minecraft","tick"),"Actual native client tick witness");self.clientHeart++;
            if(self.inert || self.closed) {self.releaseUse();self.releaseMovement();return;}
            if(self.scanning || self.mining) {
                require(client.isWindowActive() && client.screen==null,"Native foreground/channel loss is a failure, not an automatic retry");
                for(var key:client.options.keyHotbarSlots)while(key.consumeClick())self.actions.add(row("event","suppressed unsolicited owned hotbar click","phase",self.phase));
            }
            if(self.scanning && self.view!=null && self.ledger.expectedScan!=null && (self.view.scanned.contains(self.ledger.expectedScan) || self.view.phenomena.contains(self.ledger.expectedScan)))self.releaseUse();
            if(self.scanning) {self.rotate(self.scanPoint);client.options.keyUse.setDown(true);}
            else {client.options.keyUse.setDown(false);while(client.options.keyUse.consumeClick()){} }
            if(self.mining) {self.rotate(Vec3.atCenterOf(RELAY));client.options.keyAttack.setDown(true);}
            else {client.options.keyAttack.setDown(false);while(client.options.keyAttack.consumeClick()){} }
        }catch(Throwable error) {self.fail(error);}
    }
    public static void nativeServerTick(MinecraftServer server) {
        if(binding!=null && server.overworld()!=null)BoreInheritedItemWitness.checkpoint(server.overworld(),"actual native tickServer HEAD including startup");
        BoreClientSequence self=active;if(self==null || server!=self.client.getSingleplayerServer() || self.stopped)return;
        try {Throwable inheritedFailure=BoreInheritedItemWitness.failure();if(inheritedFailure!=null)throw new IllegalStateException("Inherited item witness startup/tick failure",inheritedFailure);require(server.isSameThread() && nativeFrame("net.minecraft.server.MinecraftServer","tickServer"),"Actual native integrated-server heartbeat");self.serverHeart++;}
        catch(Throwable error) {self.fail(error);}
    }
    public static void observeBoreTickHead(Level level,BlockPos pos,BlockState state,TileArcaneBore top) {
        BoreClientSequence self=observer(level);if(self==null || level.isClientSide || !TOP.equals(pos))return;
        try {require(nativeFrame(TileArcaneBore.class.getName(),"tick"),"Native Bore ticker HEAD, never a supplied sample");self.ledger.boreHead(top);}
        catch(Throwable error) {self.fail(error);}
    }
    public static void observeBoreTickReturn(Level level,BlockPos pos,BlockState state,TileArcaneBore top) {
        BoreClientSequence self=observer(level);if(self==null || level.isClientSide || !TOP.equals(pos))return;
        try {require(nativeFrame(TileArcaneBore.class.getName(),"tick"),"Native Bore ticker RETURN");self.ledger.boreReturn(top);}
        catch(Throwable error) {self.fail(error);}
    }
    /** Read-only exact native start boundary is needed because the matrix may
     * legitimately draw its first unit BEFORE this tick's SERVER_POST. */
    public static void observeInfusionStartHead(TileInfusionMatrix matrix,ServerPlayer player) {
        BoreClientSequence self=observer(matrix.getLevel());if(self==null || player!=self.owner || !MATRIX.equals(matrix.getBlockPos()))return;
        try {require(nativeFrame(TileInfusionMatrix.class.getName(),"startCrafting"),"Actual native infusion start HEAD");self.ledger.infusionHead(matrix);}
        catch(Throwable error) {self.fail(error);}
    }
    public static void observeInfusionStartReturn(TileInfusionMatrix matrix,ServerPlayer player) {
        BoreClientSequence self=observer(matrix.getLevel());if(self==null || player!=self.owner || !MATRIX.equals(matrix.getBlockPos()))return;
        try {require(nativeFrame(TileInfusionMatrix.class.getName(),"startCrafting"),"Actual native infusion start RETURN");self.ledger.infusionReturn(matrix);}
        catch(Throwable error) {self.fail(error);}
    }
    public static void observeAlchemyTickHead(Level level,BlockPos pos,BlockState state,TileAlchemyFurnace furnace) {
        BoreClientSequence self=observer(level);if(self==null || level.isClientSide || !FURNACE.equals(pos))return;
        try {require(nativeFrame(TileAlchemyFurnace.class.getName(),"tick"),"Actual native alchemy ticker HEAD");self.ledger.alchemyHead(furnace);}
        catch(Throwable error) {self.fail(error);}
    }
    public static void observeAlchemyTickReturn(Level level,BlockPos pos,BlockState state,TileAlchemyFurnace furnace) {
        BoreClientSequence self=observer(level);if(self==null || level.isClientSide || !FURNACE.equals(pos))return;
        try {require(nativeFrame(TileAlchemyFurnace.class.getName(),"tick"),"Actual native alchemy ticker RETURN");self.ledger.alchemyReturn(furnace);}
        catch(Throwable error) {self.fail(error);}
    }
    public static void observeOrdinaryNodeTickHead(Level level,BlockPos pos,BlockState state,TileNode node) {
        BoreClientSequence self=observer(level);if(self==null || level.isClientSide)return;
        try {
            require(node.getLevel()==level && node.getBlockPos().equals(pos) && nativeFrame(TileNode.class.getName(),"serverTick"),
                    "Actual native ordinary-node HEAD and arguments");
            self.ledger.nodeHead(node);
        } catch(Throwable error) {self.fail(error);}
    }
    public static void observeOrdinaryNodeTickReturn(Level level,BlockPos pos,BlockState state,TileNode node) {
        BoreClientSequence self=observer(level);if(self==null || level.isClientSide)return;
        try {
            require(node.getLevel()==level && node.getBlockPos().equals(pos) && nativeFrame(TileNode.class.getName(),"serverTick"),
                    "Actual native ordinary-node RETURN and arguments");
            self.ledger.nodeReturn(node);
        } catch(Throwable error) {self.fail(error);}
    }
    public static void observeNormalServerStopHead(MinecraftServer server) {
        BoreClientSequence self=active;
        if(self==null || self.owner==null || self.ledger==null || !self.ledger.ready || self.stopped || server!=self.owner.server)return;
        self.report.put("native_stop_HEAD_observer",row("entered",true,"returned",false,"server_object",System.identityHashCode(server),"native_tick",server.getTickCount()));
        try {
            self.ledger.normalStopTeardown();self.nativeStopHeadReturned=true;
            self.report.put("native_stop_HEAD_observer",row("entered",true,"returned",true,"server_object",System.identityHashCode(server),"native_tick",server.getTickCount()));
        }catch(Throwable error) {self.stopCallbackFailure("native_stop_HEAD_observer",error);}
    }
    public static void observeItemTouchHead(ItemEntity item,Player player) {
        BoreInheritedItemWitness.touchHead(item,player);
        BoreClientSequence self=observer(item.level());if(self==null || player!=self.owner)return;
        try {require(nativeFrame(ItemEntity.class.getName(),"playerTouch"),"Actual ItemEntity.playerTouch HEAD");self.ledger.touchHead(item);}
        catch(Throwable error) {self.fail(error);}
    }
    public static void observeItemTouchReturn(ItemEntity item,Player player) {
        BoreInheritedItemWitness.touchReturn(item,player);
        BoreClientSequence self=observer(item.level());if(self==null || player!=self.owner)return;
        try {require(nativeFrame(ItemEntity.class.getName(),"playerTouch"),"Actual ItemEntity.playerTouch RETURN");self.ledger.touchReturn(item);}
        catch(Throwable error) {self.fail(error);}
    }
    public static void observeCustodyMenuHead(AbstractContainerMenu menu,int slot,int button,ClickType type,Player player) {
        BoreClientSequence self=active;
        try {if(self==null || self.stopped || player!=self.owner || self.ledger==null || !self.ledger.ready)return;
            self.ledger.custodyMenuHead(menu,slot,button,type);
        }catch(Throwable error) {if(self!=null)self.fail(error);}
    }
    public static void observeCustodyMenuReturn(AbstractContainerMenu menu,int slot,int button,ClickType type,Player player) {
        BoreClientSequence self=active;
        try {if(self==null || self.stopped || player!=self.owner || self.ledger==null || !self.ledger.ready)return;
            self.ledger.custodyMenuReturn(menu,slot,button,type);
        }catch(Throwable error) {if(self!=null)self.fail(error);}
    }
    public static void observeXpTouch(ExperienceOrb orb,Player player) {
        BoreClientSequence self=observer(orb.level());if(self==null || player!=self.owner)return;
        self.fail(new IllegalStateException("Unexpected native XP contact "+orb.getUUID()+"; this route may award ZERO XP"));
    }
    public static void observeLaterMob(Mob mob) {
        BoreClientSequence self=observer(mob.level());if(self==null)return;
        try {require(nativeFrame(Mob.class.getName(),"checkDespawn"),"Actual native late-Mob HEAD");self.ledger.observeMob(mob,true);}
        catch(Throwable error) {self.fail(error);}
    }
    public static void observeMobileMobTickHead(ServerLevel world,Entity entity) {
        BoreClientSequence self=active;
        try {
            self=observer(world);if(self==null)return;
            require(entity.level()==world && nativeFrame(ServerLevel.class.getName(),"tickNonPassenger"),"Actual native world nonpassenger HEAD and arguments");
            if(entity instanceof Mob mob) {self.ledger.ambientWitness.bodyHead(world,entity);self.ledger.mobileHead(mob);}
            else if(entity instanceof ItemEntity item)self.ledger.ambientItemHead(item);
        } catch(Throwable error) {if(self!=null)self.fail(error);}
    }
    public static void observeMobileMobTickReturn(ServerLevel world,Entity entity) {
        BoreClientSequence self=active;
        // Do not filter removed owners or mutable flags: the ledger must close
        // a genuine in-body unload without inventing a motion receipt.
        try {
            self=observer(world);if(self==null)return;
            require(entity.level()==world && nativeFrame(ServerLevel.class.getName(),"tickNonPassenger"),"Actual native world nonpassenger RETURN and arguments");
            if(entity instanceof Mob mob) {self.ledger.ambientWitness.bodyReturned(world,entity);self.ledger.mobileReturn(mob);}
            else if(entity instanceof ItemEntity item)self.ledger.ambientItemReturn(item);
        } catch(Throwable error) {if(self!=null)self.fail(error);}
    }
    public static void observeAmbientSpawnHead(Entity source,ItemStack offered,float offset) {
        BoreClientSequence self=active;
        try {self=observer(source.level());if(self!=null)self.ledger.ambientWitness.head(source,offered,offset);}
        catch(Throwable error) {if(self!=null)self.fail(error);}
    }
    public static void observeAmbientSpawnReturn(Entity source,ItemStack offered,float offset,ItemEntity returned) {
        BoreClientSequence self=active;
        try {self=observer(source.level());if(self!=null)self.ledger.ambientWitness.returned(source,offered,offset,returned);}
        catch(Throwable error) {if(self!=null)self.fail(error);}
    }
    public static void observeAmbientDeathHead(LivingEntity source,ServerLevel world,DamageSource damage) {
        BoreClientSequence self=active;
        try {self=observer(world);if(self!=null) {require(source.level()==world,"Same actual native death-loot world argument");self.ledger.ambientWitness.deathHead(source,world,damage);}}
        catch(Throwable error) {if(self!=null)self.fail(error);}
    }
    public static void observeAmbientDeathReturn(LivingEntity source,ServerLevel world,DamageSource damage) {
        BoreClientSequence self=active;
        try {self=observer(world);if(self!=null) {require(source.level()==world,"Same actual native death-loot world argument");self.ledger.ambientWitness.deathReturned(source,world,damage);}}
        catch(Throwable error) {if(self!=null)self.fail(error);}
    }
    public static void observeItemManagerLifecycle(Object manager,net.minecraft.world.level.entity.EntityAccess entity,boolean unload,boolean returned) {
        BoreInheritedItemWitness.managerLifecycle(manager,entity,unload,returned);
        BoreClientSequence self=active;
        if(self==null || self.ledger==null)return;
        try {self.ledger.itemManagerLifecycle(manager,entity,unload,returned);}
        catch(Throwable error) {self.fail(error);}
    }
    /** Actual references and normal-stop observer completion, context ONLY.
     * No supplied stop flag, actor flag rewriting, or gameplay authority. */
    static Map<String,Object> itemLifecycleContext(ServerLevel level) {
        BoreClientSequence self=active;
        boolean bound=self!=null && self.owner!=null && self.owner.server==level.getServer() && self.owner.level()==level && self.ledger!=null;
        boolean stopFrame=StackWalker.getInstance().walk(frames->frames.anyMatch(f->f.getClassName().equals(MinecraftServer.class.getName()) && f.getMethodName().equals("stopServer") && f.getDescriptor().equals("()V")));
        return row("root_owner_bound",bound,"owner_server_object",bound?System.identityHashCode(self.owner.server):"UNBOUND",
                "owner_level_object",bound?System.identityHashCode(self.owner.level()):"UNBOUND","owner_object",bound?System.identityHashCode(self.owner):"UNBOUND",
                "native_normal_stop_HEAD_observer_returned",bound && self.nativeStopHeadReturned,"actual_stopServer_frame",stopFrame,
                "native_stop_context",bound && self.nativeStopHeadReturned && stopFrame && level.getServer().isSameThread(),
                "scope","LIFECYCLE_CONTEXT_ONLY_NOT_BIRTH_SOURCE_BODY_OR_CURRENT_LIVE_POST");
    }
    public static void observeInheritedRemoval(Entity entity,Entity.RemovalReason reason) {
        if(entity instanceof ItemEntity item)BoreInheritedItemWitness.removal(item,reason);
        BoreClientSequence self=observer(entity.level());if(self==null)return;
        try {require(nativeFrame(Entity.class.getName(),"setRemoved"),"Actual native removal HEAD");self.ledger.removal(entity,reason);}
        catch(Throwable error) {self.fail(error);}
    }
    public static boolean suppressHotbarScroll(Minecraft client,double horizontal,double vertical) {
        BoreClientSequence self=active;
        if(self==null || self.client!=client || self.inert || self.stopped || !(self.scanning || self.mining))return false;
        self.actions.add(row("event","suppressed unsolicited owned-channel hotbar scroll","horizontal",horizontal,"vertical",vertical,"server_mutation",false));return true;
    }
    private static BoreClientSequence observer(Level level) {
        BoreClientSequence self=active;
        return self==null || self.owner==null || self.ledger==null || !self.ledger.ready || self.stopped || level!=self.owner.level() ? null:self;
    }
    static boolean nativeFrame(String type,String method) {
        return StackWalker.getInstance().walk(frames->frames.anyMatch(frame->frame.getClassName().equals(type) && frame.getMethodName().equals(method)));
    }

    private void submit(ServerReadOrFixture action) {
        require(serverDone(),"No overlapping server observation/approved fixture tasks");
        serverTask=CompletableFuture.runAsync(()->{
            require(failure==null && !inert && !closed && !stopped,"Queued own observation/fixture cannot run after failure/close/stop");
            try {action.run();}catch(ReflectiveOperationException error) {throw new IllegalStateException(error);}
        },client.getSingleplayerServer());
    }
    private boolean serverDone() {if(serverTask==null)return true;if(!serverTask.isDone())return false;serverTask.join();return true;}
    private ServerPlayer serverPlayer() {ServerPlayer p=client.getSingleplayerServer().getPlayerList().getPlayer(PLAYER);require(p!=null && p.getUUID().equals(client.player.getUUID()),"Same actual owned actor");return p;}
    long nativeTick() {return BoreClientEvidence.actualServerOwner(client.getSingleplayerServer(),owner==null?null:owner.server).getTickCount();}
    long phaseNativeAge() {return phaseTick<0?0:nativeTick()-phaseTick;}
    long phaseWallAge() {return phaseTick<0?0:System.nanoTime()-phaseWall;}
    long combinedNativeAge() {require(nativeEntryTick>=0,"Native combined entry origin exists");return nativeTick()-nativeEntryTick;}
    Map<String,Object> actualClockObservation(long tick,long wall) {
        require(nativeEntryTick>=0 && tick>=nativeEntryTick && wall>=wallStart,"Actual established monotone combined clock origins");
        return row("wall_nanos",wall,"phase_native_age",phaseTick<0?0:tick-phaseTick,"phase_wall_nanos",phaseTick<0?0:wall-phaseWall,
                "combined_native_age",tick-nativeEntryTick,"combined_wall_nanos",wall-wallStart,"phase_origin_native_tick",phaseTick,"phase_origin_wall_nanos",phaseWall);
    }
    void fail(Throwable error) {
        if(failure==null) {failure=error;report.put("FAIL_RETAIN_WORLD",row("phase",phase,"op",opName,"error",error.toString(),"auto_retry",false,"restore_stock_save_entities",false));}
        inert=true;
    }
    private void rotate(Vec3 point) {
        require(point!=null,"Actual finite aim point");Vec3 delta=point.subtract(client.player.getEyePosition());
        client.player.setYRot((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));client.player.yRotO=client.player.getYRot();
        client.player.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z))));client.player.xRotO=client.player.getXRot();
        client.player.connection.send(new ServerboundMovePlayerPacket.Rot(client.player.getYRot(),client.player.getXRot(),client.player.onGround()));
    }
    private void click(int slot,int button,ClickType type) {require(BoreOwnedForeground.isOwned(client),"Owned native inventory input: "+shift.describe(client));client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId,slot,button,type,client.player);}
    private int playerSlot(int index) {return client.player.containerMenu.slots.stream().filter(s->s.container==client.player.getInventory() && s.getContainerSlot()==index).findFirst().orElseThrow().index;}
    private int find(Predicate<ItemStack> wanted) {int result=findOptional(wanted);require(result>=0,"Missing physical input/working slot; never manufacture room");return result;}
    private int findOptional(Predicate<ItemStack> wanted) {for(int i=0;i<36;i++)if(wanted.test(client.player.getInventory().getItem(i)))return i;return -1;}
    private int findRoom(ItemStack stack) {
        int empty=findOptional(ItemStack::isEmpty);if(empty>=0)return empty;
        return find(s->ItemStack.isSameItemSameComponents(s,stack) && s.getCount()+stack.getCount()<=s.getMaxStackSize());
    }
    private void holdShift(boolean held) {client.options.keyShift.setDown(held);shift.setHeld(client,held);}
    private void releaseUse() {scanning=false;client.options.keyUse.setDown(false);if(client.player!=null && client.gameMode!=null && client.player.isUsingItem())client.gameMode.releaseUsingItem(client.player);}
    private void releaseMovement() {client.options.keyUp.setDown(false);client.options.keyDown.setDown(false);client.options.keyLeft.setDown(false);client.options.keyRight.setDown(false);client.options.keyJump.setDown(false);}
    private void releaseInputs() {
        releaseUse();releaseMovement();mining=false;client.options.keyAttack.setDown(false);client.options.keyShift.setDown(false);
        shift.setHeld(client,false);if(client.gameMode!=null)client.gameMode.stopDestroyBlock();
        while(client.options.keyUse.consumeClick()){}while(client.options.keyAttack.consumeClick()){}
    }
    private void serverStopped(MinecraftServer server) {
        if(owner==null || server!=owner.server || stopped)return;stopped=true;
        report.put("native_STOPPED_handler",row("entered",true,"server_object",System.identityHashCode(server),
                "native_tick",server.getTickCount(),"actual_server_thread",server.isSameThread()));
        try {
            boolean baselineReady=ledger!=null && ledger.ready;
            if(!baselineReady)report.put("normal_stop_tail",row("status","BASELINE_NOT_ESTABLISHED_NO_POST_WITNESS","raw_deliveries_started",rDelivered,"issued_note_requests",noteRequests));
            var result=BoreStopTail.complete(new BoreStopTail.Context(runId,tailFile,storyFinal,baselineReady,nativeStopHeadReturned),report,new BoreStopTail.Ops() {
                public void finalizeLedger() {
                    require(server.isSameThread() && server==owner.server,"Actual bound server STOPPED finalization thread");
                    ledger.stopping();
                }
                public void unregisterServerPost() {TickEvent.SERVER_POST.unregister(postHook);}
                public void unregisterEntityAdd() {EntityEvent.ADD.unregister(addHook);}
                public void unregisterPrivateStopHook() {ClientTestServerLifecycle.unregister(stopHook);}
                public void clearActiveReference() {if(active==BoreClientSequence.this)active=null;}
                public Throwable rootFailure() {return failure;}
                public void latchFirstFailure(Throwable error) {fail(error);}
                public String encodeFullEnvelope(Map<String,Object> envelope) {return new GsonBuilder().create().toJson(envelope);}
            });
            if(result.status().equals("STOPPED_UNINSPECTED")) {
                long completedWall=System.nanoTime(),completedTick=server.getTickCount();
                try {require(nativeEntryTick>=0 && phase.equals("B") && phaseTick>=0 && completedTick>=nativeEntryTick && completedTick>=phaseTick
                        && completedTick-nativeEntryTick<=16000 && completedTick-phaseTick<=12000
                        && completedWall-wallStart<=960_000_000_000L && completedWall-phaseWall<=720_000_000_000L,
                        "Original combined/B deadlines include actual completed tail/proof IO");}
                catch(Throwable error) {stopCallbackFailure("post_write_deadline",error);}
            }
            // Post-return console facts bind BOTH close outcomes. They are not
            // preclaimed inside the primary file or reconstructed from its bytes.
            System.out.println("BORE_STOP_TAIL_RESULT "+new GsonBuilder().create().toJson(row("run_id",runId,"status",result.status(),
                    "tail_status",result.tailStatus(),"full_evidence",result.fullEvidence(),"primary_close_completed",result.primaryCloseCompleted(),
                    "proof_close_completed",result.proofCloseCompleted(),"UTF8bytes",result.utf8Bytes(),"sha256",result.sha256())));
            if(!result.status().equals("STOPPED_UNINSPECTED"))stopCallbackFailure("stop_tail_result_failed",new IllegalStateException("Stop evidence remains failed: "+result.status()+"; "+result.completion()));
        }catch(Throwable error) {stopCallbackFailure("stop_driver_or_console_result",error);}
        try {
            client.execute(()->{try {releaseInputs();if(camera!=null)client.options.setCameraType(camera);}
                catch(Throwable error) {stopCallbackFailure("client_cleanup_body",error);}});
        }catch(Throwable error) {stopCallbackFailure("client_cleanup_submission",error);}
    }
    private void stopCallbackFailure(String stage,Throwable error) {
        fail(error);
        try {System.err.println("[BoreStopTailCallback] run="+runId+" stage="+stage+" tail="+tailFile);error.printStackTrace(System.err);System.err.flush();}
        catch(Throwable loggingFailure) {fail(loggingFailure);}
    }
    @Override public void close() {
        if(closed)return;closed=true;releaseInputs();
        if(client.player!=null)client.player.closeContainer();client.setScreen(null);
        if(camera!=null)client.options.setCameraType(camera);
        report.put("cleanup","Only native input/menu/channel cleanup; NO inventory/knowledge/entity/world restoration. Independent SERVER_POST and lifecycle tail remains through normal stop, including failure.");
        // No live-list listener removal here. Root stops normally and retains the
        // failed private copy; no save overwrite, automatic retry or rollback.
    }

    static BlockPos p(int x,int y,int z) {return new BlockPos(x,y,z);}
    static Item shard(String tag) {return ThaumcraftContent.SHARDS.get(tag).get();}
    static Item phial() {return ThaumcraftContent.EMPTY_PHIAL.get();}
    static Item thaumium() {return ThaumcraftContent.MATERIALS.get("thaumium_ingot").get();}
    static Item jarItem() {return ThaumcraftContent.JAR.get().asItem();}
    static Item pickItem() {return ThaumcraftEquipment.THAUMIUM_PICKAXE.get();}
    static Item baseItem() {return ThaumcraftArcaneBore.ARCANE_BORE_BASE_ITEM.get();}
    static Item topItem() {return ThaumcraftArcaneBore.ARCANE_BORE_ITEM.get();}
    static Item relayItem() {return ThaumcraftNodeDevices.VIS_RELAY_ITEM.get();}
    private static Item[] grid(Item... items) {require(items.length==9,"Literal3x3");return items;}
    static boolean payer(ItemStack value) {return value.is(ThaumcraftContent.WAND.get()) && ItemWandCasting.data(value).cap().equals("gold") && ItemWandCasting.data(value).rod().equals("silverwood");}
    private static void verifyPayerDebit(ItemStack before,ItemStack after) {
        require(sameExcept(before,after,ThaumcraftContent.WAND_DATA.get()),"Same exact saved payer shell/components");
        for(String tag:WandData.PRIMALS)require(ItemWandCasting.getVis(before,tag)-ItemWandCasting.getVis(after,tag)==(tag.equals("aer") || tag.equals("ordo")?900:0),"Native exact base900+900, no wand refill");
    }
    static boolean sameExcept(ItemStack a,ItemStack b,net.minecraft.core.component.DataComponentType<?>... omitted) {
        ItemStack x=a.copy(),y=b.copy();for(var key:omitted) {x.remove(key);y.remove(key);}return ItemStack.matches(x,y);
    }
    static int count(Player p,Item item) {return p.getInventory().items.stream().filter(s->s.is(item)).mapToInt(ItemStack::getCount).sum();}
    static String snbt(ItemStack stack,ServerPlayer p) {return stack.isEmpty()?"EMPTY":stack.save(p.registryAccess()).toString();}
    static Map<String,Integer> amounts(AspectList list) {var result=new TreeMap<String,Integer>();for(Aspect aspect:list.getAspects())if(list.getAmount(aspect)>0)result.put(aspect.getTag(),list.getAmount(aspect));return Map.copyOf(result);}
    static Object read(Object object,String name) {
        for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try {Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(NoSuchFieldException absent) {continue;}catch(ReflectiveOperationException error) {throw new IllegalStateException("Pure native field "+name,error);}
        throw new IllegalStateException("Read-only observation schema changed: "+object.getClass()+"/"+name);
    }
    static void require(boolean condition,String message) {if(!condition)throw new IllegalStateException(message);}
    static Map<String,Object> row(Object... pairs) {var out=new LinkedHashMap<String,Object>();for(int i=0;i<pairs.length;i+=2)out.put((String)pairs[i],pairs[i+1]==null?"NONE":pairs[i+1]);return Collections.unmodifiableMap(out);}
}
