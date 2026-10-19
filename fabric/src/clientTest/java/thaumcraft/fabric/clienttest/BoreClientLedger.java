package thaumcraft.fabric.clienttest;

import static thaumcraft.fabric.clienttest.BoreClientSequence.*;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
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
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.api.visnet.TileVisNode;
import thaumcraft.common.config.ThaumcraftArcaneBore;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftNaturals;
import thaumcraft.common.items.ItemJar;
import thaumcraft.common.items.ItemJarLabel;
import thaumcraft.common.items.ItemResearchNotes;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.foci.ItemFocusExcavation;
import thaumcraft.common.lib.bore.ArcaneBoreMath;
import thaumcraft.common.lib.bore.BoreNativeContext;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.utils.DeferredItemDrops;
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

/** Package-private passive evidence/guards for ONE BoreClientSequence. No
 * resource, world, entity, native-time, RNG or inventory mutation methods. */
final class BoreClientLedger {
    private final BoreClientSequence s;
    private final ServerPlayer p;
    private final ServerLevel level;
    private final BoreClientParentModel parent=new BoreClientParentModel();
    private final BoreClientEvidence evidence=new BoreClientEvidence();
    private final BoreClientEvidence.BorePosts borePosts=new BoreClientEvidence.BorePosts();
    private WeakReference<TileArcaneBore> boundTop;
    private final Map<BlockPos,Boolean> beCoverage=new LinkedHashMap<>();
    private final Map<UUID,Long> cowGaps=new LinkedHashMap<>();
    private final Set<UUID> seenParentCows=new LinkedHashSet<>();
    private final Map<UUID,Entity.RemovalReason> unloadReasons=new LinkedHashMap<>();
    private final Map<BlockEntity,CompoundTag> postTags=new java.util.IdentityHashMap<>();
    private final Map<BlockEntity,String> postTagStrings=new java.util.IdentityHashMap<>();
    private boolean collectingPost;
    private boolean normalStopTeardown;
    private record StackEvent(String log,Map<String,Object> facts,Map<String,ItemStack> detachedStacks) {}
    private final List<StackEvent> pendingNativeEvidence=new ArrayList<>();
    volatile boolean ready;
    volatile String expectedScan;
    final ItemStack savedFocus,savedPayer,savedPick,savedOffhand,savedLabel;
    private final boolean savedEntityInvulnerable,savedAbilityInvulnerable;
    final List<ItemStack> savedInventory=new ArrayList<>(),savedArmor=new ArrayList<>();
    final int[] consumed=new int[4];
    volatile int sourceTotal,harvests,rawGrants;
    final Map<Integer,UUID> harvestIds=new ConcurrentHashMap<>();
    private final Map<String,Integer> generated=new TreeMap<>(),infusionPaid=new TreeMap<>();
    private final Map<BlockPos,CompoundTag> oldBEs=new LinkedHashMap<>();
    private final Map<BlockPos,BlockState> oldBlocks=new LinkedHashMap<>();
    private final Map<BlockPos,BlockEntity> identities=new LinkedHashMap<>();
    private final Map<BlockPos,BlockState> protectedSupports=new LinkedHashMap<>();
    private final Map<UUID,Entity> allEntities=new LinkedHashMap<>();
    private final Map<UUID,Map<String,Object>> protectedLiving=new LinkedHashMap<>(),firstLiving=new LinkedHashMap<>();
    private final Map<UUID,Long> firstLivingTicks=new LinkedHashMap<>();
    final BoreAmbientItemWitness ambientWitness;
    private final List<Map<String,Object>> ambientItemEvents=BoreClientEvidence.log(),ambientItemLifecycle=BoreClientEvidence.log();
    private final Map<UUID,BoreAmbientItemMotion> ambientItems=new LinkedHashMap<>();
    private final Map<UUID,BoreItemLifecycle> ambientManagers=new LinkedHashMap<>();
    private final List<BoreItemLifecycle> retiredAmbientManagers=new ArrayList<>();
    private final Map<UUID,Mob> ambientLootSources=new LinkedHashMap<>();
    private final Map<UUID,UUID> ambientItemSources=new LinkedHashMap<>();
    private record AmbientActorPair(long tick,Map<String,Object> head,Map<String,Object> returned) {}
    private final Map<UUID,Map<String,Object>> ambientActorHeads=new LinkedHashMap<>();
    private final Map<UUID,AmbientActorPair> ambientActorPairs=new LinkedHashMap<>();
    private final Set<UUID> ambientLootDeaths=new LinkedHashSet<>();
    private volatile boolean ambientAllRetired=true;
    private final Map<UUID,BoreClientEvidenceMotion> protectedMobile=new LinkedHashMap<>();
    private final Set<UUID> ambient=new HashSet<>(),naturalRemovals=new HashSet<>();
    private final List<Map<String,Object>> lifecycle=BoreClientEvidence.log(),late=BoreClientEvidence.log(),
            boreTimeline=BoreClientEvidence.log(),epochs=BoreClientEvidence.log(),nodeTimeline=BoreClientEvidence.log();
    private final Map<BlockPos,NodeState> nodes=new LinkedHashMap<>();
    private record NodeFrame(long tick,NodeState before) {}
    private final Map<BlockPos,NodeFrame> nodeFrames=new LinkedHashMap<>();
    private final Map<BlockPos,Integer> authorizedModifiers=new LinkedHashMap<>();
    private final Map<UUID,Touch> touches=new LinkedHashMap<>();
    private final Map<BlockPos,ItemStack> previousPedestals=new LinkedHashMap<>();
    private final Map<String,Integer> ingredientsConsumed=new TreeMap<>();
    private final CompoundTag baselineKnowledge;
    private CompoundTag learnedKnowledge;
    private View previous;
    private long first=-1,last=-1,infusionStart=-1;
    private int checks,burns,lastBurn,lastCoal=3,ingredientCount,infusionDraws,scans,baseCrafts,pickCrafts;
    private boolean startedInfusion,finishedInfusion;
    volatile boolean infusionStartWitness;
    private Boolean infusionWasCrafting;
    private Map<String,Integer> lastDemand=Map.of();
    private BoreHead head;
    private int selectedCount,eligible,slowEligible,fastEligible;
    private BlockPos selected;
    private float priorCredit;
    private long sourceEpoch=-1;
    private Map<String,Integer> sourceAvailable=Map.of();
    private final Map<String,Long> pulseGenerated=new TreeMap<>(),pulseUnused=new TreeMap<>(),epochPaid=new TreeMap<>();
    private long previousRaw;
    private record AlchemyHead(TileAlchemyFurnace identity,long tick,ItemStack input,ItemStack fuel,int cook,int burn) {}
    private AlchemyHead alchemy;
    private long lastAlchemy=-1;
    private int installedLabel=-1,labelBindings,labelReturns;
    private int lastMatrixCounter=-1;
    static final UUID INHERITED_JAR=UUID.fromString("14d47d60-09eb-4949-a6fd-84dd78e8eea1");
    private final BoreCustodyWindow custody=new BoreCustodyWindow();
    private final List<Map<String,Object>> custodyEvents=BoreClientEvidence.log();
    private final List<Map<String,Object>> inheritedItemEvents=BoreClientEvidence.log();
    private final List<Map<String,Object>> custodyMenuEvents=BoreClientEvidence.log();
    private record CustodyMenuFrame(AbstractContainerMenu menu,int slot,int button,ClickType type,long tick,Map<String,Object> before,List<Map<String,Object>> frames) {}
    private CustodyMenuFrame custodyMenuFrame;
    private ItemEntity inheritedJar;
    private volatile ItemStack inheritedJarStack=ItemStack.EMPTY;
    private CompoundTag inheritedJarTag;
    private volatile boolean inheritedJarPicked;

    BoreClientLedger(BoreClientSequence sequence) {
        s=sequence;p=s.owner;level=p.serverLevel();
        savedEntityInvulnerable=p.isInvulnerable();savedAbilityInvulnerable=p.getAbilities().invulnerable;
        for(ItemStack item:p.getInventory().items)savedInventory.add(item.copy());
        savedPayer=savedInventory.get(10).copy();savedFocus=savedInventory.get(15).copy();savedPick=savedInventory.stream().filter(v->v.is(Items.WOODEN_PICKAXE)).findFirst().orElseThrow().copy();
        savedLabel=savedInventory.stream().filter(v->v.getItem() instanceof ItemJarLabel).findFirst().orElseThrow().copy();
        savedOffhand=p.getOffhandItem().copy();for(EquipmentSlot slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))savedArmor.add(p.getItemBySlot(slot).copy());
        baselineKnowledge=KnowledgeAccess.get(p).save().copy();
        s.report.put("native_global_entity_lifecycle",lifecycle);s.report.put("late_native_Mob_first_observations",late);
        ambientWitness=new BoreAmbientItemWitness(level,p,this::ambientReceipt,s::fail);
        s.report.put("ambient_item_native_events",ambientItemEvents);s.report.put("ambient_item_native_lifecycle",ambientItemLifecycle);
        s.report.put("ambient_item_trajectory_schema","bore_ambient_item_trajectory_v2");
        s.report.put("inherited_jar_custody_events",custodyEvents);
        s.report.put("inherited_item_native_events",inheritedItemEvents);
        s.report.put("inherited_jar_native_menu_events",custodyMenuEvents);
        BoreInheritedItemWitness.bind(level,p,event->{require(inheritedItemEvents.size()<65_536,"Bounded complete inherited item receipt log");inheritedItemEvents.add(event);},s::fail);
        inheritedCheckpoint("Ledger constructor before bootstrap readiness");
        s.report.put("inherited_jar_custody_policy",row("schema","bore_inherited_jar_custody_v1","UUID",INHERITED_JAR.toString(),
                "authorization","User approved ONE custody-only old-jar coverage gap, <=200native/14wall; normal all20-live guard otherwise unchanged",
                "old_loose_units",8,"separate_workshop_old_units",333,"NEW_generated_paid_retained",List.of(300,112,188),
                "pickup_native_wall",List.of(60,4),"recovery_native_wall",List.of(140,10),"chest_slot",7,"additional_raw",0));
        s.report.put("native_Bore_HEAD_RETURN_timeline",boreTimeline);s.report.put("native_source_epochs_and_raw_payments",epochs);s.report.put("ordinary_nodes_native_pruning",nodeTimeline);
        s.report.put("payload_versions",evidence.versions());
        s.report.put("evidence_encoding",row("schema",1,"refs","{type,version}; recursively resolve through payload_versions; observations are made EVERY_POST","live_coverage_is_not_stopped_audit",true));
        s.report.put("ordinary_native_wildlife",row("policy","Unnamed nonpersistent normal-AI mobs are ambient regardless of broad observation rectangle; native motion is not frozen",
                "protected","All named/NoAI/persistent identities, all11 parent cows and concrete stock/action cells remain protected; only witnessed allowed retirements pass"));
        s.report.put("protected_mobile_policy",row("scope","ONLY originally protected, unnamed normal-AI persistent vanilla nonpassenger Mobs outside ALL owned/old-block/BE footprints",
                "strict","Original type/health/name/NoAI/persistence/invulnerability remain exact; named/NoAI/parent11 stay position-exact; destruction never becomes ambient retirement",
                "motion_witness","Actual ServerLevel.tickNonPassenger(Entity) HEAD/RETURN, same actual owner/class/native tick; POST must match latest paired position/counter or unchanged initial/rebound binding",
                "gaps","Witnessed UNLOADED_TO_CHUNK -> genuine native reobservation; STOPPED_AUDIT_REQUIRED, no motion proof across a gap",
                "limits","Native body + outside endpoints, not a proof of each internal SELF move or third-party/adversarial in-body teleports"));
        s.report.put("trusted_parent_expectations",row("parent191_manifest_sha256",BoreClientParentModel.PARENT,"native_stopped_audit_sha256",BoreClientParentModel.AUDIT,
                "resource",BoreClientParentModel.RESOURCE,"decoded_metadata_sha256",BoreClientParentModel.DECODED,"all11_protected_cows",parent.cows.values().stream().map(BoreClientParentModel.Cow::report).toList(),
                "all113_BE_positions_types",parent.bes.values().stream().map(v->row("pos",v.pos().toShortString(),"type",v.type(),"expected_NBT",evidence.payloadVersion("parent_BE_tag",v.snbt()))).toList(),
                "mandatory_independent_after_STOP","ALL113 source stock/metadata and ALL11 identities/position/health6/NoAI/persistence/name; unseen intervals are never runtime PASS"));
    }
    /** Bounded bootstrap caller polls only raw, already-loaded cells. A configured
     * view radius is not evidence that asynchronous native chunk loading ended. */
    Map<String,Object> bootstrapReadiness() {
        inheritedCheckpoint("actual bootstrap readiness poll");
        require(p.server.isSameThread() && !ready,"Readiness is an actual pre-baseline server observation");
        int jars=0,units=0,candles=0,localCows=0;var missing=new ArrayList<String>();
        for(var expected:parent.bes.values())if(expected.type().equals("thaumcraft:jar")) {
            BlockEntity current=be(expected.pos());
            if(current instanceof TileJar jar) {jars++;units+=jar.getAmount();}
            else missing.add("jar:"+expected.pos().toShortString()+":"+(current==null?"NOT_CURRENTLY_LOADED":current.getClass().getName()));
        }
        for(var expected:parent.candles) {
            var chunk=loadedChunk(expected.pos().getX()>>4,expected.pos().getZ()>>4);
            if(chunk!=null && expected.state().matches(chunk.getBlockState(expected.pos())))candles++;
            else missing.add("candle:"+expected.pos().toShortString()+":NOT_CURRENTLY_MATCHED");
        }
        for(UUID id:BoreClientParentModel.LOCAL)if(level.getEntity(id) instanceof LivingEntity live && !live.isRemoved())localCows++;
        if(jars==20)require(units==333,"All20 actual jars loaded but native contents differ: "+units+" !=333");
        boolean available=jars==20 && units==333 && candles==72 && localCows==3;
        return row("ready",available,"actual_jars",jars,"actual_units",units,"actual_candles",candles,
                "actual_local_parent_cows",localCows,"missing_cells",missing,"chunk_load_requests",0);
    }
    /** Same original bootstrap deadline, but AFTER the one workshop baseline so
     * global native observations continue while distant loading finishes. */
    Map<String,Object> custodyBootstrapReadiness() {
        require(p.server.isSameThread() && ready && s.phase.equals("ENTRY") && custody.state()==BoreCustodyWindow.State.NEW
                && s.inputs.isEmpty() && s.noteRequests==0 && scans==0,"Passive contact readiness before R/preflight/raw/acquisition");
        inheritedCheckpoint("actual custody contact bootstrap poll");
        long elapsed=inheritedElapsed();require(elapsed<=1100,"Contact readiness cannot spend the original inherited departure lifetime cap");
        Entity current=level.getEntity(INHERITED_JAR);
        require(current==null || current instanceof ItemEntity,"Original inherited item UUID may not become another entity type");
        ItemEntity actual=BoreInheritedItemWitness.originalJarOwner(level);
        inheritedCheckpoint("after actual custody bootstrap owner observation");
        if(current!=null)require(current==actual && !current.isRemoved(),"Actual helper-bound same UUID owner, never a replacement or absent-owner waiver");
        boolean contactChunk=loadedChunk(81,22)!=null;
        return row("schema","bore_custody_bootstrap_readiness_v1","ready",actual!=null && contactChunk,
                "native_tick",p.server.getTickCount(),"native_game_time",level.getGameTime(),"accepted_elapsed",elapsed,
                "baseline_already_bound",true,"actual_inherited_item_owner",actual==null?"NOT_CURRENTLY_LOADED":row("UUID",actual.getUUID().toString(),
                        "actual_object",System.identityHashCode(actual),"Age",read(actual,"age"),"full_stack",BorePassiveStackSnapshot.nativeTag(rawPhysicalStack(actual),level).toString()),
                "actual_contact_chunk_loaded",contactChunk,"chunk_load_requests",0);
    }
    void baseline() {
        require(p.server.isSameThread() && PLAYER.equals(p.getUUID()),"Owner server thread/exact inherited profile");
        require(savedEntityInvulnerable && !savedAbilityInvulnerable,"Exact accepted parent has Entity.Invulnerable1 and abilities.invulnerable0; never set either flag");
        require(rawHealth(p)==20.0F,"Exact inherited actor Health20 is retained; not ordinary damage verification");
        require(p.experienceLevel==0 && p.totalExperience==123 && Float.floatToIntBits(p.experienceProgress)==Float.floatToIntBits(.17391325533390045F),"Actual accepted XP tuple0/123/4over23");
        require(count(p,Items.GOLD_INGOT)==108 && count(p,phial())==2 && count(p,Items.COBBLESTONE)==4 && savedPick.getDamageValue()==5,"Actual inherited raw counts/worn pick");
        require(ItemStack.matches(savedLabel,ItemJarLabel.blank()) && componentCount(savedInventory,savedLabel)==1,"Exactly one original plain blank label; no metadata may be lost by binding/blank return");
        require(p.getInventory().items.stream().filter(v->!v.isEmpty()).count()==22 && p.getInventory().getItem(21).getCount()==60 && p.getInventory().getItem(35).getCount()==48,"Actual22occupied/14free; gold60+48, no normalization");
        require(payer(savedPayer) && ItemWandCasting.data(savedPayer).vis().equals(Map.of("aer",4070,"aqua",1470,"ignis",70,"ordo",1270,"perditio",770,"terra",657)),"Exact saved finite gold/silverwood payer10");
        require(savedFocus.getItem() instanceof ItemFocusExcavation f && Arrays.equals(f.getAppliedUpgrades(savedFocus),new short[]{0,-1,-1,-1,-1}),"Same actual paid Potency1 loose focus15");
        PlayerKnowledge knowledge=KnowledgeAccess.get(p);
        require(!knowledge.isResearchComplete("ARCANEBORE") && knowledge.isResearchComplete("FOCUSEXCAVATION") && knowledge.isResearchComplete("INFUSION"),"Actual missing PRIMARY + real completed parents");
        require(knowledge.getScannedItems().isEmpty() && knowledge.getScannedEntities().isEmpty() && knowledge.getScannedPhenomena().isEmpty(),"All nine scans must really be first identities");
        require(be(BENCH) instanceof TileArcaneWorkbench bench && bench.isEmpty(),"Existing native bench genuinely empty (no charging wand)");
        TileAlchemyFurnace furnace=furnace();require(furnace.getItem(0).isEmpty() && furnace.getItem(1).is(Items.COAL) && furnace.getItem(1).getCount()==3
                && furnace.getVis()==0 && furnace.getBurnTime()==0 && furnace.getCookTime()==0 && furnace.getBellowsCount()==0,"Finite inherited furnace3coal / zero buffers / NO bellows");
        for(int i=1;i<=3;i++)require(be(FURNACE.above(i)) instanceof TileAlembic a && a.getAmount()==0 && a.getFilter()==null,"Exactly three real empty/unfiltered alembics");
        require(!(be(FURNACE.above(4)) instanceof TileAlembic),"No hidden fourth/fifth alembic fixture");
        TileInfusionMatrix matrix=matrix();require(matrix.isActive() && !matrix.isCrafting() && amounts(matrix.getDemand()).isEmpty(),"Same formed active idle matrix, cached old recipe metadata is NOT debt");
        require(pedestal(CENTER).isEmpty(),"Empty actual center");for(BlockPos pos:PEDESTALS)require(pedestal(pos).isEmpty(),"Eight genuinely empty peripheral pedestals");
        require(be(RELAY) instanceof TileVisRelay && be(SOURCE) instanceof TileNodeEnergized,"Actual inherited relay/source devices, not grants");
        require(be(NODE) instanceof TileNode node && node.getScanId().equals("1b1965a9-747f-4d43-82ff-91e1f0ab443b"),"Actual accepted node identity");
        for(AABB area:List.of(RESEARCH_AREA,BORE_AREA,ALTAR_AREA))require(level.getEntitiesOfClass(ItemEntity.class,area).isEmpty() && level.getEntitiesOfClass(ExperienceOrb.class,area).isEmpty(),"Refuse inherited loose stock/XP, never clear it");
        var supports=new ArrayList<>(List.of(FOUNDATION.below(),SCAFFOLD.below(),SLOW.below(),FAST.below(),TABLE.below(),TABLE.south().below(),CHEST.below(),
                p(1611,216,377),p(1611,217,377),p(1611,218,377),p(1603,215,379),p(1605,215,380)));
        for(int i=0;i<11;i++)supports.add(jarPos(i).below());for(BlockPos pos:supports) {require(block(pos).is(Blocks.STONE),"Exact real stone support, never manufacture AIR anchor: "+pos);protectedSupports.put(pos,block(pos));}
        int jars=0,units=0,candles=0;
        // Trusted parent table covers ALL113, not just a conveniently loaded subset.
        for(var expected:parent.bes.values())oldBEs.put(expected.pos(),expected.tag());
        // Snapshot only already-loaded world data. No forced loads/activation.
        for(int x=1461>>4;x<=1947>>4;x++)for(int z=358>>4;z<=449>>4;z++) {
            var chunk=loadedChunk(x,z);if(chunk==null)continue;
            for(var entry:chunk.getBlockEntities().entrySet()) {
                BlockPos pos=entry.getKey().immutable();BlockEntity tile=entry.getValue();require(parent.bes.containsKey(pos),"Unexpected baseline BE outside trusted113 table: "+pos);identities.put(pos,tile);
                if(tile instanceof TileJar jar) {jars++;units+=jar.getAmount();}
            }
            for(int bx=x*16;bx<x*16+16;bx++)for(int bz=z*16;bz<z*16+16;bz++)for(int y=214;y<=225;y++) {
                BlockPos pos=p(bx,y,bz);BlockState state=chunk.getBlockState(pos);
                if(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals("thaumcraft"))oldBlocks.put(pos,state);
                if(state.is(ThaumcraftInfusion.TALLOW_CANDLE.get()))candles++;
            }
        }
        require(jars==20 && units==333 && candles==72,"Actual workshop baseline must match20oldjars/333/72candles: jars="+jars+", units="+units+", candles="+candles+"; never manufacture missing cells");
        for(Entity entity:level.getAllEntities())if(entity!=p) {
            allEntities.put(entity.getUUID(),entity);if(entity instanceof Mob mob)observeMob(mob,false);
            else if(entity instanceof LivingEntity living)protect(living);
        }
        var localNoAi=new LinkedHashSet<UUID>();for(var entry:allEntities.entrySet())if(entry.getValue() instanceof Mob mob && mob.isNoAi())localNoAi.add(entry.getKey());
        require(localNoAi.containsAll(BoreClientParentModel.LOCAL) && parent.cows.keySet().containsAll(localNoAi),"Entry requires exact local UUIDs3; any additional loaded NoAI must also be a validated trusted parent11, never pretend unseen11 live");
        for(UUID id:BoreClientParentModel.LOCAL)require(level.getEntity(id)==allEntities.get(id) && allEntities.get(id) instanceof LivingEntity,"Exact local parent cow must really be loaded: "+id);
        for(UUID id:parent.cows.keySet())if(!seenParentCows.contains(id))cowGaps.put(id,(long)p.server.getTickCount());
        preserveBEs();
        require(pending().isEmpty(),"Accepted deferred output queue has no pending owner/quarantine");
        s.report.put("baseline",row("player",p.getUUID().toString(),"inventory_full_components",inventoryRows(),"armor",savedArmor.stream().map(v->snbt(v,p)).toList(),"offhand",snbt(savedOffhand,p),
                "inherited_player_protection",row("Entity.Invulnerable",savedEntityInvulnerable,"abilities.invulnerable",savedAbilityInvulnerable,
                        "unchanged_from_accepted_parent",true,"limitation","Existing actor protection retained; not evidence of ordinary survival damage vulnerability"),
                "knowledge",baselineKnowledge.toString(),"trusted_parent_expected_BE_tags_NOT_live_observations",oldBEs.entrySet().stream().map(e->row("pos",e.getKey().toShortString(),"NBT",evidence.payloadVersion("parent_BE_tag",parent.bes.get(e.getKey()).snbt()))).toList(),
                "actual_initial_BE_observations",protectedTagRows(),"initial_observation_server_tick",p.server.getTickCount(),"entry_required_local_UUIDs3",BoreClientParentModel.LOCAL.stream().map(UUID::toString).sorted().toList(),
                "protected_living",Map.copyOf(protectedLiving),"known11_liveSubset",parentCowRows(),"ALL_loaded_entity_UUIDs",allEntities.keySet().stream().map(UUID::toString).toList(),"old_jars_units_candles",List.of(jars,units,candles),"unloaded191file_audit","ROOT_REQUIRED"));
        previous=snapshot();lastBurn=furnace.getBurnTime();first=-1;last=-1;ready=true;
    }

    void post() {
        require(p.server.isSameThread(),"Pure ledger only on actual native server POST");long tick=p.server.getTickCount(),expected=last<0?tick:last+1;
        if(tick!=expected)s.fail(new IllegalStateException("Noncontiguous actual SERVER_POST: "+expected+" -> "+tick));
        if(first<0)first=tick;last=tick;checks++;collectingPost=true;postTags.clear();postTagStrings.clear();
        try {
            // A native tracked UUID may reappear before its separate ADD/tick
            // callback. Bind its ACTUAL owner before any preservation check,
            // never later as a side effect of formatting the coverage rows.
            guard(this::observeProtectedCurrentOwners);
            if(!normalStopTeardown)guard(this::custodyGuard);
            guard(()->inheritedCheckpoint("actual native SERVER_POST"));
            guard(this::ambientCheckpoint);
            if(!normalStopTeardown && !s.inert)guard(()->{int actual=matrix().getTickCount();require(lastMatrixCounter<0 || actual==lastMatrixCounter+1,"Actual required matrix counter advances once per native POST, never infer ticking from radius");lastMatrixCounter=actual;});
            flushNativeEvidence();
            View now=null;try {if(!normalStopTeardown || snapshotAvailable())now=snapshot();}catch(Throwable error) {s.fail(error);}
            final View observed=now;
            if(!s.inert && observed!=null && !normalStopTeardown) {
                guard(()->{require(s.combinedNativeAge()<=16000 && System.nanoTime()-s.wallStart<=960_000_000_000L,"Hard combined native16000/wall960 INCLUDING entry/bootstrap/capture and stop tail");
                    require(s.phaseNativeAge()<=(s.phase.equals("R")?4000:12000) && s.phaseWallAge()<=(s.phase.equals("R")?240_000_000_000L:720_000_000_000L),"Hard phaseR4000/240 or B12000/720, never extend for captures");});
                guard(()->observeFurnace(observed));guard(()->observeInfusion(observed));guard(()->preserve(observed));guard(this::observeEpoch);guard(this::observeOrdinaryNodes);
            }
            Object boreWitness=capture("Bore_POST_witness",()->{
                BlockEntity current=be(TOP);TileArcaneBore top=current instanceof TileArcaneBore t?t:null;
                if(top!=null)bindTop(top);
                return borePosts.post(tick,top!=null,top==null?0:1,top==null?null:top.lifetimeId().toString(),normalStopTeardown);
            });
            var out=new LinkedHashMap<String,Object>();
            out.putAll(row("POST",checks,"server_tick",tick,"expected_contiguous_tick",expected,"native_game_time",level.getGameTime(),"phase",s.phase,"operation",s.opName,
                    "normal_stop_teardown",normalStopTeardown,"menu",p.containerMenu.getClass().getName(),"selected",p.getInventory().selected,"XP_LEVEL_TOTAL_PROGRESS",List.of(p.experienceLevel,p.totalExperience,p.experienceProgress),
                    "native_source_consumed",List.of(consumed[0],consumed[1],consumed[2],consumed[3]),"ingredient_count",ingredientCount,"Bore_witness",boreWitness,"observer_world_resource_entity_writes",0));
            out.putAll(s.actualClockObservation(tick,System.nanoTime()));
            out.put("inherited_jar_native_menu_event_count",custodyMenuEvents.size());out.put("inherited_jar_custody_event_count",custodyEvents.size());
            out.put("ambient_item_native_event_count",ambientItemEvents.size());out.put("ambient_item_native_lifecycle_event_count",ambientItemLifecycle.size());
            out.put("raw_delivery_event_count",s.inputs.size());out.put("phase_start_event_count",s.phaseStarts.size());
            out.put("critical_context_owners",scalar(()->criticalContextRows(custody.coverageGap())));
            out.put("actual_inventory_stack_NBT",capture("inventory36",this::inventoryRows));
            out.put("inherited_jar_custody",capture("inherited_jar_custody",this::custodyRow));
            out.put("inherited_items_observer",capture("inherited_items_observer",BoreInheritedItemWitness::snapshot));
            out.put("ambient_item_actual_states",scalar(this::ambientItemRows));
            out.put("cursor",capture("cursor",()->snbt(p.containerMenu.getCarried(),p)));
            out.put("health_AI",capture("actor_health",()->row("health",rawHealth(p),"position",p.position().toString(),"using",p.isUsingItem(),"mode",p.gameMode.getGameModeForPlayer().getName(),
                    "Entity.Invulnerable",p.isInvulnerable(),"abilities.invulnerable",p.getAbilities().invulnerable)));
            out.put("actual_actor_knowledge",capture("knowledge",()->KnowledgeAccess.get(p).save().toString()));
            out.put("furnace",scalar(this::furnaceRow));
            out.put("source_generated",evidence.payloadVersion("source_generated",generated));
            out.put("native_infusion_paid",evidence.payloadVersion("infusion_paid",infusionPaid));
            out.put("actual_source_scalars",scalar(()->{var tile=be(SOURCE);if(tile==null && normalStopTeardown)return row("status","NORMAL_STOP_UNAVAILABLE_NO_SOURCE_OBSERVATION");var source=(TileNodeEnergized)tile;return row("epoch",source.getRefillEpoch(),"available",evidence.payloadVersion("source_vis",amounts(source.getAvailableVis())),"capacity",evidence.payloadVersion("source_capacity",amounts(source.getVisBase())),"raw_grants_observed",rawGrants);}));
            out.put("actual_Bore",scalar(()->{var tile=be(TOP);if(!(tile instanceof TileArcaneBore top))return row("present",false);
                return row("present",true,"observer_owner",boundTop!=null && boundTop.get()==top?1:"UNBOUND","lifetime",top.lifetimeId().toString(),"count",top.getCountdown(),"target",top.selectedTarget()==null?"NONE":top.selectedTarget().toShortString(),"credit",top.getAccelerationCredit(),"relay",be(RELAY) instanceof TileVisRelay,"harvests",harvests);}));
            out.put("ordinary_node_actual_scalars",scalar(()->{var rows=new ArrayList<Map<String,Object>>();for(BlockPos pos:oldBEs.keySet())if(be(pos) instanceof TileNode node) {
                NodeState value=nodeState(node);rows.add(row("pos",pos.toShortString(),"counter",value.count(),"recharge_wait",value.waitTicks(),"regeneration",value.regeneration(),"catchUp",value.catchUp(),
                        "stock_modifier_identity",evidence.payloadVersion("node_stock",row("current",value.current(),"BASE",value.base(),"modifier",value.modifier(),"scanId",value.id()))));}return rows;}));
            out.put("actual_pending_queue",capture("pending",()->pending().stream().map(v->snbt(v,p)).toList()));
            out.put("protected_BE_tags",capture("protected_BE_rows",this::protectedTagRows));
            out.put("new_earned_BE_states",capture("new_BE_rows",this::newTagRows));
            out.put("protected_living",capture("protected_living",this::protectedRows));
            out.put("protected_mobile_actual_positions",scalar(this::mobileRows));
            out.put("known11_liveSubset",capture("parent_cow_coverage",this::parentCowRows));
            out.put("living_native_tickCounts",scalar(()->{var clocks=new LinkedHashMap<String,Object>();for(UUID id:protectedLiving.keySet()) {var current=level.getEntity(id);if(current instanceof LivingEntity && !current.isRemoved())clocks.put(id.toString(),current.tickCount);}return clocks;}));
            out.put("protected_original_stone_supports",capture("support_cells",()->protectedSupports.keySet().stream().map(pos->row("pos",pos.toShortString(),"actual",observedBlock(pos))).toList()));
            if(observed!=null) {
                out.put("alembics",evidence.payloadVersion("alembics",observed.alembics().stream().map(v->row("type",v.tag(),"amount",v.amount(),"filter",v.filter())).toList()));
                out.put("actual_live_NEW_essentia",evidence.payloadVersion("NEW_essentia",observed.sourceStock()));out.put("native_infusion_demand",evidence.payloadVersion("infusion_demand",observed.demand()));
                var positions=new LinkedHashMap<String,Object>();observed.dropPositions().forEach((id,pos)->positions.put(id.toString(),pos.toString()));out.put("physical_UUIDs",evidence.payloadVersion("physical_positions",positions));
                s.view=observed;previous=observed;
            } else out.put("snapshot_unavailable",normalStopTeardown?"NORMAL_STOP_UNAVAILABLE; no prior snapshot substituted":"ACTUAL_READ_FAILED; no prior snapshot substituted");
            out.put("failed_but_tail_retained",s.inert);s.posts.add(Collections.unmodifiableMap(out));
        } finally {collectingPost=false;postTags.clear();postTagStrings.clear();}
    }
    private void guard(Runnable check) {try {check.run();}catch(Throwable error) {s.fail(error);}}
    private BoreClientEvidence.Ref capture(String type,java.util.function.Supplier<Object> observation) {
        try {return evidence.payloadVersion(type,observation.get());}
        catch(Throwable error) {s.fail(error);return evidence.payloadVersion(type,row("ACTUAL_READ_FAILED",error.toString(),"native_tick",p.server.getTickCount(),"normal_stop_teardown",normalStopTeardown));}
    }
    private Object scalar(java.util.function.Supplier<Object> observation) {
        try {return BoreClientEvidence.freeze(observation.get());}
        catch(Throwable error) {s.fail(error);return row("ACTUAL_READ_FAILED",error.toString(),"native_tick",p.server.getTickCount(),"normal_stop_teardown",normalStopTeardown);}
    }
    private void flushNativeEvidence() {
        for(StackEvent event:pendingNativeEvidence) {
            var encoded=new LinkedHashMap<String,Object>(event.facts());
            event.detachedStacks().forEach((key,value)->encoded.put(key,capture("stack",()->snbt(value,p))));
            (event.log().equals("bore")?boreTimeline:event.log().equals("physical")?s.physical:s.actions).add(Collections.unmodifiableMap(encoded));
        }
        pendingNativeEvidence.clear();
    }

    private View snapshot() {
        PlayerKnowledge knowledge=KnowledgeAccess.get(p);TileAlchemyFurnace f=furnace();TileInfusionMatrix m=matrix();
        var alembics=new ArrayList<Alembic>();var stock=new TreeMap<String,Integer>();add(stock,amounts(f.getAspects()));
        for(int i=1;i<=3;i++) {TileAlembic a=(TileAlembic)be(FURNACE.above(i));String type=a.getAspect()==null?"":a.getAspect().getTag(),filter=a.getFilter()==null?"":a.getFilter().getTag();
            alembics.add(new Alembic(type,a.getAmount(),filter));if(a.getAmount()>0)stock.merge(type,a.getAmount(),Integer::sum);}
        ItemStack originalCustodyOwner=custodyOwner();
        var jars=new TreeMap<String,Integer>();for(ItemStack stack:physicalStacks())if(stack!=originalCustodyOwner && ItemJar.isJar(stack) && ItemJar.getAmount(stack)>0)jars.merge(ItemJar.getAspect(stack).getTag(),ItemJar.getAmount(stack)*stack.getCount(),Integer::sum);
        for(int i=0;i<11;i++)if(be(jarPos(i)) instanceof TileJar jar && jar.getAmount()>0)jars.merge(jar.getAspect().getTag(),jar.getAmount(),Integer::sum);
        add(stock,jars);
        TileArcaneBore top=be(TOP) instanceof TileArcaneBore t?t:null;
        var positions=new LinkedHashMap<UUID,Vec3>();for(UUID id:s.drops.keySet())if(level.getEntity(id) instanceof ItemEntity item && !item.isRemoved())positions.put(id,item.position());
        var mainCounts=new LinkedHashMap<Item,Integer>();for(ItemStack stack:p.getInventory().items)if(!stack.isEmpty())mainCounts.merge(stack.getItem(),stack.getCount(),Integer::sum);
        return new View(p.server.getTickCount(),knowledge.getResearchComplete(),knowledge.getAspectsDiscovered(),knowledge.getScannedItems(),knowledge.getScannedPhenomena(),
                p.getInventory().selected,p.getMainHandItem().copy(),p.isShiftKeyDown(),p.containerMenu==p.inventoryMenu,Map.copyOf(mainCounts),f.getItem(0).copy(),f.getItem(1).copy(),f.getBurnTime(),f.getCookTime(),f.getVis(),Map.copyOf(amounts(f.getAspects())),List.copyOf(alembics),Map.copyOf(jars),Map.copyOf(stock),
                m.isCrafting(),m.getInstability(),amounts(m.getDemand()),pedestal(CENTER).getItem(0).copy(),top!=null,be(RELAY) instanceof TileVisRelay,top!=null && top.isPowered(),top==null?Direction.UP:top.miningDirection(),
                top==null?0:top.getAccelerationCredit(),top==null?0:top.getCountdown(),top==null?null:top.selectedTarget(),harvests,Map.copyOf(positions));
    }
    private boolean snapshotAvailable() {
        if(!(be(FURNACE) instanceof TileAlchemyFurnace) || !(be(MATRIX) instanceof TileInfusionMatrix) || !(be(CENTER) instanceof TilePedestal))return false;
        for(int i=1;i<=3;i++)if(!(be(FURNACE.above(i)) instanceof TileAlembic))return false;
        for(BlockPos pos:PEDESTALS)if(!(be(pos) instanceof TilePedestal))return false;return true;
    }
    private void observeFurnace(View now) {
        TileAlchemyFurnace f=furnace();require(f.getBellowsCount()==0 && !f.isSpeedBoost(),"No bellow/Alumentum fixture or speed rewrite");
        require(lastAlchemy==p.server.getTickCount() || checks==1 && lastAlchemy<0,"MISSING actual native alchemy HEAD/RETURN witness for this POST (only first POST may precede first observed native tick)");
        int coal=owned(Items.COAL);int allowance=s.boreDelivered?5:3;
        require(coal+burns==allowance && burns<=5,"All inherited3+new2 coal = unspent owners + actual native ignitions; no refill");lastCoal=coal;lastBurn=now.furnaceBurn();
    }
    void alchemyHead(TileAlchemyFurnace furnace) {
        require(alchemy==null && furnace==be(FURNACE),"Single current native furnace tick frame");
        alchemy=new AlchemyHead(furnace,p.server.getTickCount(),furnace.getItem(0).copy(),furnace.getItem(1).copy(),furnace.getCookTime(),furnace.getBurnTime());
    }
    void alchemyReturn(TileAlchemyFurnace furnace) {
        require(alchemy!=null && alchemy.identity()==furnace && furnace==be(FURNACE) && alchemy.tick()==p.server.getTickCount(),"Actual paired native furnace RETURN");
        ItemStack old=alchemy.input(),current=furnace.getItem(0);
        if(!old.isEmpty() && current.getCount()==old.getCount()-1 && (current.isEmpty() || ItemStack.isSameItemSameComponents(old,current))) {
            int batch=old.is(pickItem())?0:old.is(Items.PISTON)?1:old.is(Items.REDSTONE)?2:old.is(phial())?3:-1;
            require(batch>=0 && alchemy.cook()==new int[]{179,169,29,9}[batch] && furnace.getCookTime()==0,"Every source disappearance is actual native zero-bellows cook completion");
            require(++consumed[batch]<=new int[]{8,4,24,16}[batch],"No operating-pick/source overconsumption");
            Map<String,Integer> row=switch(batch) {
                case 0->Map.of("metallum",9,"praecantatio",4,"arbor",1,"perfodio",4);
                case 1->Map.of("arbor",2,"terra",3,"perditio",3,"metallum",3,"machina",2,"motus",4);
                case 2->Map.of("potentia",2,"machina",1);default->Map.of("vacuos",1);};
            add(generated,row);sourceTotal+=row.values().stream().mapToInt(Integer::intValue).sum();
            pendingNativeEvidence.add(new StackEvent("actions",row("event","actual native source cook completion","batch",batch,"complete_original_derived_tags",row,"total_units",sourceTotal,"native_tick",alchemy.tick()),Map.of("source",old.copyWithCount(1))));
        }
        int beforeBurn=alchemy.burn(),afterBurn=furnace.getBurnTime();
        if(afterBurn>beforeBurn) {require(beforeBurn<=1 && afterBurn==1600 && alchemy.fuel().is(Items.COAL) && furnace.getItem(1).getCount()==alchemy.fuel().getCount()-1,"Actual native one-coal1600 ignition");burns++;}
        else require(afterBurn==beforeBurn || afterBurn==Math.max(0,beforeBurn-1),"Natural fuel clock, never reset/accelerate");
        lastAlchemy=alchemy.tick();alchemy=null;
    }
    private void observeInfusion(View now) {
        TileInfusionMatrix matrix=matrix();
        if(!startedInfusion && now.crafting()) {
            require(infusionStartWitness,"MISSING native infusion start witness; no112-start reconstruction from a later demand snapshot");
            startedInfusion=true;
        }
        int delta=0;
        if(startedInfusion && !finishedInfusion) {
            require(now.tick()-infusionStart<=1610 && matrix.getInstability()==(now.crafting()?-3:0),"Finite actual clean1610 infusion, then native finish clears instability to0; no loss buffer/RNG reset");
            for(String tag:COST.keySet()) {
                int before=lastDemand.getOrDefault(tag,0),after=now.demand().getOrDefault(tag,0);
                int paid=before-after;require(paid>=0 && paid<=1,"Actual one unit native infusion demand draw");if(paid>0)infusionPaid.merge(tag,paid,Integer::sum);delta+=paid;
            }
            require(delta<=1,"At most ONE total unit paid per native craft cycle");infusionDraws+=delta;
            for(BlockPos pos:PEDESTALS) {
                ItemStack before=previousPedestals.get(pos),after=pedestal(pos).getItem(0);
                if(!before.isEmpty() && after.isEmpty()) {require(before.getCount()==1 && matrix.getRemainingIngredientCount()==8-(ingredientCount+1),"Native whole-count1 ingredient countdown completion");ingredientCount++;ingredientsConsumed.merge(BuiltInRegistries.ITEM.getKey(before.getItem()).toString(),1,Integer::sum);
                    s.actions.add(row("event","ACTUAL native count1 pedestal consumption","native_tick",now.tick(),"pos",pos.toShortString(),"full_stack_NBT",snbt(before,p),"remaining_ingredients",matrix.getRemainingIngredientCount(),"native_matrix_count",matrix.getTickCount()));}
                else require(ItemStack.matches(before,after),"No component replacement/loss/rewrite during native infusion");
                previousPedestals.put(pos,after.copy());
            }
            if(!now.crafting()) {require(now.center().is(topItem()) && now.center().getCount()==1 && infusionPaid.equals(COST) && infusionDraws==112 && ingredientCount==8,"Native finish ONLY after112+8 actual payments");finishedInfusion=true;s.earnedTop=now.center().copy();}
            lastDemand=now.demand();
        }
        var accounted=new TreeMap<>(now.sourceStock());add(accounted,infusionPaid);require(accounted.equals(generated),"EVERY unit: actual generated = real furnace+alembics+new carried/placed jars + native infusion paid, no clearing/residue loss");
    }
    void infusionHead(TileInfusionMatrix matrix) {
        require(matrix==be(MATRIX) && infusionWasCrafting==null,"Single actual native start frame/current formed matrix");infusionWasCrafting=matrix.isCrafting();
    }
    void infusionReturn(TileInfusionMatrix matrix) {
        require(infusionWasCrafting!=null && matrix==be(MATRIX),"Paired actual native matrix start RETURN");
        if(!infusionWasCrafting && matrix.isCrafting()) {
            require(!infusionStartWitness && s.boreDelivered && s.researchDone && nativeFrame(ItemWandCasting.class.getName(),"useOn"),"ONE actual wand-initiated start; no direct backend activation");
            require(amounts(matrix.getDemand()).equals(COST) && amounts(matrix.getRecipeCost()).equals(COST) && matrix.getSymmetry()==-7 && matrix.getInstability()==-3 && matrix.getRemainingIngredientCount()==8,"At actual start boundary: exact112/8/72candles/symmetry-7/instability-3");
            infusionStart=p.server.getTickCount();lastDemand=COST;infusionStartWitness=true;
            for(BlockPos pos:PEDESTALS)previousPedestals.put(pos,pedestal(pos).getItem(0).copy());
            s.actions.add(row("event","ACTUAL native wand start RETURN before first possible draw","tick",infusionStart,"demand",COST,"symmetry",matrix.getSymmetry(),"instability",matrix.getInstability(),"remaining_ingredients",8));
        }
        infusionWasCrafting=null;
    }

    private record BoreHead(TileArcaneBore top,UUID lifetime,long tick,BlockPos target,int count,float credit,ItemStack pick,
                            boolean eligible,Map<String,Integer> source,long epoch) {}
    void boreHead(TileArcaneBore top) {
        require(p.server.isSameThread() && top==be(TOP) && head==null,"Actual single native Bore frame/current identity");
        bindTop(top);
        TileNodeEnergized source=(TileNodeEnergized)be(SOURCE);
        boolean eligible=top.isPowered() && top.hasFocus() && top.hasPickaxe() && top.getItem(1).getDamageValue()+1<top.getItem(1).getMaxDamage()
                && ArcaneBoreMath.stepRotation(top.rotation()).aligned();
        head=new BoreHead(top,top.lifetimeId(),p.server.getTickCount(),top.selectedTarget(),top.getCountdown(),top.getAccelerationCredit(),top.getItem(1).copy(),eligible,amounts(source.getAvailableVis()),source.getRefillEpoch());
        require(!top.isQuarantined() && !top.needsLoadedReconciliation(),"Genuine placed owner never fenced/quarantined/reloaded mid-attempt");
        if(top.selectedTarget()!=null)require(top.selectedTarget().equals(harvests==0?SLOW:FAST),"Production selector chose an undeclared target; no target setter or silent substitution");
        // Bind output expectation only from a REAL eligible native completion
        // frame, before ADD can occur. This supplies no target or permission.
        if(head.target()!=null && head.eligible() && head.count()<=1) {
            require(harvests<2 && s.expectedDrop==null,"Exactly two scheduled physical outputs, no replay");
            s.expectedDrop=new Expectation(harvests==0?"Bore slow harvest":"Bore fast harvest",new ItemStack(Items.COBBLESTONE),BORE_AREA);s.lastDrop=null;
        }
    }
    void boreReturn(TileArcaneBore top) {
        require(head!=null && head.top()==top && top==be(TOP) && head.lifetime().equals(top.lifetimeId()) && head.tick()==p.server.getTickCount(),"Actual paired native Bore RETURN owner/lifetime");
        TileNodeEnergized source=(TileNodeEnergized)be(SOURCE);Map<String,Integer> afterSource=amounts(source.getAvailableVis());
        require(source.getRefillEpoch()==head.epoch(),"Source cannot be manually/refill-ticked INSIDE the Bore invocation");
        int raw=head.source().getOrDefault("perditio",0)-afterSource.getOrDefault("perditio",0);
        require(raw>=0 && raw<=100,"Actual raw Perditio native source debit, not credit inference");
        for(String tag:WandData.PRIMALS)if(!tag.equals("perditio"))require(head.source().getOrDefault(tag,0).equals(afterSource.getOrDefault(tag,0)),"Bore does not consume another raw primal");
        if(raw>0) {
            require(be(RELAY) instanceof TileVisRelay relay && rawRoot(relay)==source && RELAY.distSqr(SOURCE)==49 && RELAY.distSqr(TOP)==50 && SOURCE.distSqr(TOP)==113,"Actual native relay path; direct source range excluded");
            require(harvests>=1,"Relay absent BEFORE top creation and entire slow baseline; no hidden unconditional prefill");
        }
        rawGrants+=raw;
        boolean dug=head.target()!=null && block(head.target()).isAir();
        float credited=head.credit()+raw/5.0F;
        float expected=dug && credited>0?credited-1:credited;
        require(Math.abs(top.getAccelerationCredit()-expected)<.00001F,"Exact actual raw/5 fractional credit then real dig debit; NO base essentia/reset/refund");
        if(head.target()!=null && head.eligible()) {
            require(selected!=null && selected.equals(head.target()),"Native pending selection witness before countdown");eligible++;
            if(head.count()>1)require(top.getCountdown()==head.count()-1 && !dug,"One native eligible decrement, no time acceleration");
            else {
                require(dug && top.getItem(1).getDamageValue()==head.pick().getDamageValue()+1,"Real guarded removal + one native operating-pick wear");
                int targetCount=harvests==0?36:9;require(selectedCount==targetCount && eligible==targetCount,"Actual selection-to-completion eligible trajectory36/9, not wall/photo time");
                if(harvests==0) {require(rawGrants==0 && head.credit()==0 && !(be(RELAY) instanceof TileVisRelay),"Genuine relay-absent unassisted36");slowEligible=eligible;}
                else {require(rawGrants>0 && selectedCount==9,"Genuine routed-credit9 cached ONLY after prefill");fastEligible=eligible;}
                harvests++;selected=null;eligible=0;
            }
        } else if(head.target()!=null)require(top.getCountdown()==head.count() && !dug,"OFF/rotation/ineligible pause never consumes native countdown");
        if(top.selectedTarget()!=null && !top.selectedTarget().equals(head.target())) {
            require(top.selectedTarget().equals(harvests==0?SLOW:FAST),"Only actual declared ordinary target may be selected");
            selected=top.selectedTarget();selectedCount=top.getCountdown();eligible=0;
            require(selectedCount==(harvests==0?36:9),"Original hardness2/Potency1 cached slow36 or funded9 at actual selection");
            if(harvests==0)require(head.credit()==0,"No prefilled slow plan");else require(credited>=1,"Do not preplace second target at old credit");
        }
        require(top.getItem(1).isEmpty() || top.getItem(1).getDamageValue()==harvests,"Operating pick only0->1->2, no Repair/Mending/stock replacement");
        pendingNativeEvidence.add(new StackEvent("bore",row("native_server_tick",head.tick(),"actual_owner",1,"actual_lifetime",head.lifetime().toString(),"target_before",head.target()==null?"NONE":head.target().toShortString(),"target_after",top.selectedTarget()==null?"NONE":top.selectedTarget().toShortString(),
                "eligible_from_actual_power_items_and_next_rotation",head.eligible(),"count_before_after",List.of(head.count(),top.getCountdown()),"selected_delay",selectedCount,"eligible_since_selection",eligible,
                "credit_before_after",List.of(head.credit(),top.getAccelerationCredit()),"actual_raw_Perditio",raw,"source_epoch",head.epoch(),"source_before_after",List.of(head.source(),afterSource),
                "actual_rotations",top.rotation().toString(),"dug_native_target",dug,"harvests",harvests),Map.of("pick_before",head.pick().copy(),"pick_after",top.getItem(1).copy())));
        borePosts.returned(head.tick(),1,head.lifetime().toString());head=null;
    }
    private void bindTop(TileArcaneBore top) {
        if(boundTop==null)boundTop=new WeakReference<>(top);
        require(boundTop.get()==top,"Earned Bore actual owner may not be replaced/reloaded at the same cell");
        borePosts.bind(1,top.lifetimeId().toString());
    }
    private static TileVisNode rawRoot(TileVisNode node) {
        // PURE private-field observation, not getParent/rootFor (which may inspect
        // and repair routing in future implementations). Never writes weak links.
        var seen=new HashSet<TileVisNode>();
        while(node!=null && seen.add(node)) {
            if(node.isSource())return node;
            Object parent=read(node,"parent");node=parent instanceof WeakReference<?> ref && ref.get() instanceof TileVisNode next?next:null;
        }
        return null;
    }
    void entityAdded(Entity entity) {
        if(entity instanceof ItemEntity item)ambientWitness.added(item);
        if(entity instanceof Mob mob) {
            boolean firstObservation=!firstLiving.containsKey(mob.getUUID());observeMob(mob,false);
            AABB box=mob.getBoundingBox();
            if(firstObservation && ambient.contains(mob.getUUID()))lifecycle.add(row("event","actual registered native Mob ADD first observation","UUID",mob.getUUID().toString(),
                    "native_tick",p.server.getTickCount(),"observation_origin","ACTUAL_REGISTERED_ENTITY_ADD_CALLBACK",
                    "actual_body_bounds",List.of(box.minX,box.minY,box.minZ,box.maxX,box.maxY,box.maxZ),
                    "native",evidence.payloadVersion("living_state",living(mob)),"entity_mutations",0));
        }
        if(entity instanceof ExperienceOrb && ownedArea(entity.getBoundingBox()))throw new IllegalStateException("Unexpected native XP ADD; Bore/research/acquisition award ZERO");
        if(!(entity instanceof ItemEntity item) || !ownedArea(item.getBoundingBox()))return;
        Expectation expected=s.expectedDrop;
        require(expected!=null && expected.area().contains(item.position()) && ItemStack.matches(item.getItem(),expected.stack()),"Unclassified new owned-area physical item ADD: "+item.getUUID()+" "+item.getItem());
        require(item.getItem().getCount()==1 && !s.drops.containsKey(item.getUUID()),"One real physical count1 drop, never fabricated UUID or duplicate");
        s.drops.put(item.getUUID(),item.getItem().copy());s.lastDrop=item.getUUID();s.expectedDrop=null;
        if(expected.kind().startsWith("Bore ")) {
            int index=expected.kind().equals("Bore slow harvest")?0:1;require(harvestIds.putIfAbsent(index,item.getUUID())==null,"One actual native UUID per harvest");
            Vec3 actual=item.position(),motion=item.getDeltaMovement();
            require(actual.distanceTo(new Vec3(1615.5,217.4,380.84))<.00001 && motion.distanceTo(new Vec3(0,.025F,-.075F))<.00001,"Actual NORTH ejection geometry/velocity, not scatter/chest routing");
            require(((Number)read(item,"pickupDelay")).intValue()==0,"Original ejection native pickup delay0; observer never resets it");
        }
        pendingNativeEvidence.add(new StackEvent("physical",row("event","actual native ItemEntity ADD","kind",expected.kind(),"UUID",item.getUUID().toString(),"tick",p.server.getTickCount(),
                "position",item.position().toString(),"velocity",item.getDeltaMovement().toString(),"native_pickup_delay",read(item,"pickupDelay"),"entity_mutations",0),Map.of("full_stack",item.getItem().copy())));
    }
    @SuppressWarnings("unchecked") private void ambientReceipt(Map<String,Object> event) {
        require(p.server.isSameThread() && ambientItemEvents.size()<768,"Bounded actual native ambient receipt sink");
        Mob source=ambientWitness.pendingSource();
        require(source!=null && source.level()==level && level.getEntity(source.getUUID())==source && allEntities.get(source.getUUID())==source,
                "Ambient receipt source is the SAME originally registered current native owner, never chosen by proximity");
        UUID id=source.getUUID();Map<String,Object> original=firstLiving.get(id);
        require(original!=null && firstLivingTicks.containsKey(id) && ambient.contains(id) && !protectedLiving.containsKey(id) && !protectedMobile.containsKey(id) && !parent.cows.containsKey(id)
                && Boolean.FALSE.equals(original.get("named")) && Boolean.FALSE.equals(original.get("NoAI")) && Boolean.FALSE.equals(original.get("persistent"))
                && Boolean.FALSE.equals(original.get("invulnerable")) && original.get("type").equals(BuiltInRegistries.ENTITY_TYPE.getKey(source.getType()).toString()),
                "No protected/late-reclassified source may manufacture ambient loot admission");
        require(outsideOwned(source.getBoundingBox()) && outsideConcreteProtectedCells(source.getBoundingBox()),"Actual ambient source/body must be outside all owned/old-stock/actor cells");
        String kind=(String)event.get("event");
        require(Set.of("ambient_item_source_HEAD","ambient_item_ADD_observed","ambient_item_source_RETURN").contains(kind),"Exact native ambient event schema");
        Mob old=ambientLootSources.putIfAbsent(id,source);require(old==null || old==source,"Ambient source cannot rebind across native birth/death");ambientAllRetired=false;
        var detached=new LinkedHashMap<String,Object>(event);
        detached.put("root_original_cohort",row("UUID",id.toString(),"registration_native_tick",firstLivingTicks.get(id),"original",original,
                "original_unprotected",true,"actual_current_registered_object",System.identityHashCode(source),"protected_parent11",false));
        Object physical=event.get(kind.equals("ambient_item_ADD_observed")?"actual_item":"actual_nullable_return");
        if(physical instanceof Map<?,?> raw) {
            AABB body=receiptBounds(((Map<String,Object>)raw).get("body_bounds"));
            require(outsideOwned(body) && outsideConcreteProtectedCells(body),"Actual native ambient item birth/body must be outside every protected context/actor body");
        }
        if(kind.equals("ambient_item_source_RETURN")) {
            require("PAIRED_SOURCE_ADD_RETURN".equals(event.get("outcome")) && Boolean.TRUE.equals(event.get("loaded_same_UUID_owner_at_RETURN")) && physical instanceof Map<?,?>,
                    "Never admit ADD alone/null/denied/unowned native ambient returns");
            UUID itemId=UUID.fromString((String)((Map<?,?>)physical).get("UUID"));
            require(!s.drops.containsKey(itemId) && !itemId.equals(INHERITED_JAR) && !ambientItems.containsKey(itemId) && ambientItems.size()<256,
                    "Ambient identities are disjoint from inherited custody and exact13 acquisition outputs");
            require(level.getEntity(itemId) instanceof ItemEntity,"Completed ambient return must have an actual loaded native item owner");
            ItemEntity item=(ItemEntity)level.getEntity(itemId);ambientItemScope(item);
            var state=ambientItemState(item);require(state.fullStack().equals(((Map<?,?>)physical).get("fullstack_snbt")),"Root actual ambient physical owner full persistent stack agrees with native birth receipt");
            var motion=new BoreAmbientItemMotion(item,p.server.getTickCount(),state);ambientItems.put(itemId,motion);ambientItemSources.put(itemId,id);
            BoreItemLifecycle managerProof=new BoreItemLifecycle(level,item,1,"PROVEN_AMBIENT",id.toString());ambientManagers.put(itemId,managerProof);
            ambientLifecycle(row("event","actual paired ambient birth owner binding","UUID",itemId.toString(),"source_UUID",id.toString(),"owner_generation",1,
                    "manager_live_certificate",managerProof.certificate(),
                    "actual_object",System.identityHashCode(item),"actual",ambientStateRow(state),"actual_actor",ambientActor(),"binding_is_not_native_item_RETURN",true));
        }
        ambientItemEvents.add(Collections.unmodifiableMap(detached));
    }
    private static AABB receiptBounds(Object value) {
        require(value instanceof List<?> && ((List<?>)value).size()==6,"Actual six-scalar native body bounds");var raw=(List<?>)value;double[] n=new double[6];
        for(int i=0;i<6;i++) {require(raw.get(i) instanceof Number,"Numeric native body scalar");n[i]=((Number)raw.get(i)).doubleValue();require(Double.isFinite(n[i]),"Finite native body scalar");}
        for(int i=0;i<3;i++)require(n[i]<n[i+3],"Strict nonempty native body");return new AABB(n[0],n[1],n[2],n[3],n[4],n[5]);
    }
    private static List<Double> coordinates(Vec3 point) {return List.of(point.x,point.y,point.z);}
    private static List<Double> bounds(AABB body) {return List.of(body.minX,body.minY,body.minZ,body.maxX,body.maxY,body.maxZ);}
    private ItemStack rawPhysicalStack(ItemEntity item) {
        Object accessor=read(item,"DATA_ITEM"),data=read(item,"entityData");int index=((Number)read(accessor,"id")).intValue();Object[] slots=(Object[])read(data,"itemsById");
        require(index>=0 && index<slots.length && slots[index]!=null,"Raw physical synced stack slot");return (ItemStack)read(slots[index],"value");
    }
    private BoreAmbientItemMotion.State ambientItemState(ItemEntity item) {
        return new BoreAmbientItemMotion.State(item.tickCount,((Number)read(item,"age")).intValue(),((Number)read(item,"health")).intValue(),
                ((Number)read(item,"pickupDelay")).intValue(),(Boolean)read(item,"invulnerable"),BorePassiveStackSnapshot.ambientTag(rawPhysicalStack(item),level).toString(),
                coordinates((Vec3)read(item,"position")),coordinates((Vec3)read(item,"deltaMovement")),bounds((AABB)read(item,"bb")));
    }
    private static Map<String,Object> ambientStateRow(BoreAmbientItemMotion.State state) {return row("native_counter",state.counter(),"Age",state.age(),"Health",state.health(),
            "PickupDelay",state.pickupDelay(),"Invulnerable",state.invulnerable(),"full_stack",state.fullStack(),"position",state.position(),"velocity",state.velocity(),"body_bounds",state.bounds());}
    private Map<String,Object> ambientActor() {
        require(p.server.isSameThread() && p.level()==level && PLAYER.equals(p.getUUID()),"Actual same-world bound player body observation");
        return row("UUID",p.getUUID().toString(),"actual_object",System.identityHashCode(p),"class",p.getClass().getName(),"dimension",level.dimension().location().toString(),
                "position",coordinates((Vec3)read(p,"position")),"body_bounds",bounds((AABB)read(p,"bb")),"removed",p.isRemoved(),"loaded_same_UUID_owner",level.getEntity(p.getUUID())==p);
    }
    private void ambientItemScope(ItemEntity item) {
        require(p.server.isSameThread() && item.getClass()==ItemEntity.class && item.level()==level && !item.isRemoved() && level.getEntity(item.getUUID())==item,
                "Actual live same-UUID vanilla ambient item owner, never remote activation");
        ItemStack stack=rawPhysicalStack(item);require(stack.is(Items.BONE) || stack.is(Items.ARROW) || stack.is(Items.ROTTEN_FLESH),"Only proven native bone/arrow/plain-flesh physical slice");
        require(outsideOwned(item.getBoundingBox()) && outsideConcreteProtectedCells(item.getBoundingBox()),"Ambient physical item remains outside all stock/action/actor bodies");
    }
    void ambientItemHead(ItemEntity item) {
        BoreAmbientItemMotion motion=ambientItems.get(item.getUUID());if(motion==null)return;
        ambientItemRebind(item.getUUID(),motion);ambientManagers.get(item.getUUID()).liveBoundary();ambientItemScope(item);mobileNativeFrame();
        motion.head(item,p.server.getTickCount(),ambientItemState(item));
        require(ambientActorHeads.putIfAbsent(item.getUUID(),ambientActor())==null,"One actual actor-body observation per native ambient HEAD");
    }
    void ambientItemReturn(ItemEntity item) {
        BoreAmbientItemMotion motion=ambientItems.get(item.getUUID());if(motion==null)return;
        ambientManagers.get(item.getUUID()).liveBoundary();ambientItemScope(item);mobileNativeFrame();motion.returned(item,p.server.getTickCount(),ambientItemState(item));
        Map<String,Object> actorHead=ambientActorHeads.remove(item.getUUID());require(actorHead!=null,"Actual paired actor-body HEAD, never reconstructed from a POST");
        ambientActorPairs.put(item.getUUID(),new AmbientActorPair(p.server.getTickCount(),actorHead,ambientActor()));
    }
    private void ambientItemRebind(UUID id,BoreAmbientItemMotion motion) {
        Entity actual=level.getEntity(id);
        if(!motion.unloaded()) {require(actual==motion.owner() && actual instanceof ItemEntity && !actual.isRemoved(),"Ambient owner missing/replaced without actual native unload");return;}
        require(!motion.frameOpen(),"Missing ambient native body RETURN");
        BoreItemLifecycle managerProof=ambientManagers.get(id);require(managerProof!=null && managerProof.completed(),"Ambient rebind requires completed outer native manager unload RETURN");
        if(actual==null || actual.isRemoved())return;
        require(actual instanceof ItemEntity && motion.owner() instanceof ItemEntity old && old.isRemoved() && old.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK,
                "Ambient rebind follows actual completed native chunk unload");
        ItemEntity item=(ItemEntity)actual;ambientItemScope(item);managerProof.rebindPhysical(item);motion.rebind(item,p.server.getTickCount(),ambientItemState(item));
        retiredAmbientManagers.add(managerProof);managerProof=new BoreItemLifecycle(level,item,motion.generation(),"PROVEN_AMBIENT",ambientItemSources.get(id).toString());ambientManagers.put(id,managerProof);
        ambientLifecycle(row("event","actual same-UUID ambient item rebind","UUID",id.toString(),"source_UUID",ambientItemSources.get(id).toString(),"owner_generation",motion.generation(),
                "manager_live_certificate",managerProof.certificate(),
                "actual_object",System.identityHashCode(item),"actual",ambientStateRow(motion.latest()),"actual_actor",ambientActor(),"unseen_interval","STOPPED_AUDIT_REQUIRED; no gap motion invented"));
    }
    private void ambientItemRemoval(ItemEntity item,Entity.RemovalReason reason) {
        BoreAmbientItemMotion motion=ambientItems.get(item.getUUID());require(reason==Entity.RemovalReason.UNLOADED_TO_CHUNK,"No supported ambient merge/pickup/despawn/destruction waiver");
        BoreItemLifecycle proof=ambientManagers.get(item.getUUID());
        try {
            ambientLifecyclePhysical(item,motion);
            ambientManagerReceipt(item,motion,proof.innerReason(reason));
        }catch(Throwable error) {if(proof!=null)proof.fail();throw error;}
    }
    /** Already-proven ambient map only: never recognizes a new item by type,
     * radius, hidden lookup, or unload callback. Inherited dispatch is earlier. */
    void itemManagerLifecycle(Object manager,net.minecraft.world.level.entity.EntityAccess entity,boolean unload,boolean returned) {
        if(entity==null || !ambientItems.containsKey(entity.getUUID()))return;
        UUID id=entity.getUUID();BoreAmbientItemMotion motion=ambientItems.get(id);BoreItemLifecycle proof=ambientManagers.get(id);
        try {
            require(entity.getClass()==ItemEntity.class,"Known ambient UUID must remain exact ItemEntity, not silent wrong-object skip");
            ItemEntity item=(ItemEntity)entity;ambientLifecyclePhysical(item,motion);
            Map<String,Object> event=unload?proof.unload(manager,returned):proof.tracking(manager,returned);
            if(unload && returned) {require(proof.completed(),"Only outer native unload RETURN commits ambient unload");motion.unload(item,p.server.getTickCount(),ambientItemState(item));}
            ambientManagerReceipt(item,motion,event);
        }catch(Throwable error) {if(proof!=null)proof.fail();throw error;}
    }
    private void ambientLifecyclePhysical(ItemEntity item,BoreAmbientItemMotion motion) {
        try {
        require(p.server.isSameThread() && item.getClass()==ItemEntity.class && item.level()==level && motion!=null && motion.owner()==item
                && !motion.unloaded() && !motion.frameOpen() && ambientManagers.containsKey(item.getUUID()) && ambientActorHeads.isEmpty() && touches.isEmpty(),
                "Same original proven ambient object/generation, no overlapping native body/touch/unload");
        require(!s.drops.containsKey(item.getUUID()) && !parent.cows.containsKey(item.getUUID()) && !protectedLiving.containsKey(item.getUUID())
                && !protectedMobile.containsKey(item.getUUID()) && !INHERITED_JAR.equals(item.getUUID()),"Ambient lifecycle cannot bypass another physical protection");
        ambientWitness.checkpoint();require(ambientWitness.failure()==null,"No native source/loot/body frame at item lifecycle boundary");
        ambientLifecycleGeometry(item);
        require(ambientItemState(item).equals(motion.latest()),"All ambient physical scalars/typed stack/geometry/counter equal original binding or latest native pair");
        }catch(Throwable error) {BoreItemLifecycle proof=ambientManagers.get(item.getUUID());if(proof!=null)proof.fail();throw error;}
    }
    private void ambientLifecycleGeometry(ItemEntity item) {
        ItemStack stack=rawPhysicalStack(item);require(stack.is(Items.BONE) || stack.is(Items.ARROW) || stack.is(Items.ROTTEN_FLESH),"Only proven native bone/arrow/plain-flesh physical slice");
        AABB body=(AABB)read(item,"bb");require(outsideOwned(body) && outsideConcreteProtectedCells(body),"Ambient physical item remains outside all stock/action/actor bodies");
    }
    private void ambientManagerReceipt(ItemEntity item,BoreAmbientItemMotion motion,Map<String,Object> proof) {
        Map<String,Object> actor=ambientActor(),context=BoreClientSequence.itemLifecycleContext(level);
        require(BoreItemLifecycleCore.actorAllowed(new BoreItemLifecycleCore.ActorContext(p,s.owner,p.server,s.owner==null?null:s.owner.server,level,s.owner==null?null:s.owner.level(),
                p.isRemoved(),level.getEntity(p.getUUID())==p,Boolean.TRUE.equals(context.get("native_normal_stop_HEAD_observer_returned")),Boolean.TRUE.equals(context.get("actual_stopServer_frame"))),
                BoreItemLifecycleCore.ActorUse.TRACKING_UNLOAD_CONTEXT),"Disconnected actor is context ONLY during actual witnessed native normal-stop lifecycle spine");
        ambientLifecycle(row("event","actual paired ambient manager lifecycle","UUID",item.getUUID().toString(),"source_UUID",ambientItemSources.get(item.getUUID()).toString(),
                "owner_generation",motion.generation(),"actual_object",System.identityHashCode(item),"actual",ambientStateRow(ambientItemState(item)),
                "actual_actor",actor,"native_boundary_context",context,"manager_lifecycle",proof,"last_native_pair",ambientNativePair(item.getUUID(),motion),
                "unseen_interval","STOPPED_AUDIT_REQUIRED; no item body/acquisition/stock credit from lifecycle"));
    }
    private void ambientLifecycle(Map<String,Object> facts) {
        require(ambientItemLifecycle.size()<2048,"Bounded complete ambient physical lifecycle");var out=new LinkedHashMap<String,Object>(facts);
        out.put("schema","bore_ambient_item_lifecycle_v2");out.put("native_tick",p.server.getTickCount());out.put("world_game_time",level.getGameTime());out.put("observer_mutations",0);
        ambientItemLifecycle.add(Collections.unmodifiableMap(out));
    }
    private List<Map<String,Object>> ambientItemRows() {
        var rows=new ArrayList<Map<String,Object>>();long tick=p.server.getTickCount();
        for(var entry:ambientItems.entrySet()) {
            UUID id=entry.getKey();BoreAmbientItemMotion motion=entry.getValue();BoreItemLifecycle managerProof=ambientManagers.get(id);managerProof.closedBoundary();
            if(managerProof.hidden()) {
                ItemEntity old=(ItemEntity)motion.owner();ambientLifecyclePhysical(old,motion);
                rows.add(row("UUID",id.toString(),"source_UUID",ambientItemSources.get(id).toString(),"owner_generation",motion.generation(),
                        "status","MANAGER_CUSTODY_UNAVAILABLE_STOPPED_AUDIT_REQUIRED","current_public_live_POST_authority",false,
                        "manager_lifecycle",managerProof.checkpoint("actual ambient POST/checkpoint"),"actual_actor_context_only",ambientActor(),
                        "actual",ambientStateRow(ambientItemState(old))));continue;
            }
            ambientItemRebind(id,motion);
            if(motion.unloaded()) {ambientLifecycleGeometry((ItemEntity)motion.owner());rows.add(row("UUID",id.toString(),"source_UUID",ambientItemSources.get(id).toString(),"owner_generation",motion.generation(),"status","STOPPED_AUDIT_REQUIRED_NATIVE_UNLOAD",
                    "unload_tick",motion.unloadTick(),"manager_lifecycle",managerProof.checkpoint("completed ambient unload checkpoint"),"actual_actor",ambientActor(),"latest_observation_NOT_current",ambientStateRow(motion.latest())));continue;}
            ItemEntity item=(ItemEntity)level.getEntity(id);ambientManagers.get(id).liveBoundary();ambientItemScope(item);var actual=ambientItemState(item);String status=motion.post(item,tick,actual);
            var out=new LinkedHashMap<String,Object>(row("UUID",id.toString(),"source_UUID",ambientItemSources.get(id).toString(),"owner_generation",motion.generation(),"binding_tick",motion.bindingTick(),
                    "status",status,"actual_object",System.identityHashCode(item),"actual",ambientStateRow(actual)));
            out.put("actual_actor",ambientActor());
            var pair=motion.pair();if(pair!=null && pair.tick()==tick)out.put("actual_native_pair",ambientNativePair(id,motion));
            else out.put("actual_native_pair","NONE_THIS_POST; unchanged initial/rebound/latest paired state, no invented body");rows.add(Collections.unmodifiableMap(out));
        }return rows;
    }
    private Object ambientNativePair(UUID id,BoreAmbientItemMotion motion) {
        var pair=motion.pair();if(pair==null)return "NONE_OBSERVED; no invented native body";
        AmbientActorPair actors=ambientActorPairs.get(id);require(actors!=null && actors.tick()==pair.tick(),"Actual same-native-body item and actor observations are inseparable");
        return row("native_tick",pair.tick(),"HEAD",ambientStateRow(pair.before()),"RETURN",ambientStateRow(pair.after()),"HEAD_actor",actors.head(),"RETURN_actor",actors.returned());
    }
    private void ambientCheckpoint() {
        ambientWitness.checkpoint();require(ambientWitness.failure()==null && ambientActorHeads.isEmpty(),"Ambient native source/body witness failed or unclosed, regardless of sink");ambientItemRows();
    }
    boolean ambientBirthsRetired() {return ambientAllRetired;}
    void captureCheckpoint(String name) {inheritedCheckpoint("before capture "+name);ambientCheckpoint();custodyGuard();}
    private static List<Map<String,Object>> detachedNativeFrames() {
        var frames=StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk(stream->stream.limit(161).toList());require(frames.size()<=160,"Finite native descriptor stack");
        return frames.stream().map(frame->row("class",frame.getClassName(),"method",frame.getMethodName(),"descriptor",frame.getDescriptor(),"bci",frame.getByteCodeIndex(),"line",frame.getLineNumber())).toList();
    }
    private static Object actorReference(Object actor) {return actor instanceof Entity entity?row("UUID",entity.getUUID().toString(),"type",BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(),"class",entity.getClass().getName()):"NONE";}
    private Map<String,Object> damageAtRemoval(Mob mob) {
        Object raw=read(mob,"lastDamageSource");DamageSource source=raw instanceof DamageSource d?d:null;long stamp=((Number)read(mob,"lastDamageStamp")).longValue();
        return row("cache_type",source==null?"NONE":source.typeHolder().unwrapKey().orElseThrow().location().toString(),"cache_stamp",stamp,"cache_age",level.getGameTime()-stamp,
                "actual_damage_object",source==null?0:System.identityHashCode(source),"causing_entity",source==null?"NONE":actorReference(read(source,"causingEntity")),
                "direct_entity",source==null?"NONE":actorReference(read(source,"directEntity")),"lastHurtByMob",actorReference(read(mob,"lastHurtByMob")),
                "lastHurtByPlayer",actorReference(read(mob,"lastHurtByPlayer")),"lastHurtByPlayerTime",read(mob,"lastHurtByPlayerTime"));
    }
    private record Touch(ItemEntity identity,int before,ItemStack stack,Vec3 position) {}
    void touchHead(ItemEntity item) {
        if(INHERITED_JAR.equals(item.getUUID())) {
            custodyGuard();require(custody.state()==BoreCustodyWindow.State.OUTBOUND && item==inheritedJar && level.getEntity(INHERITED_JAR)==item
                    && custodyMatches(rawPhysicalStack(item)),"Only the actual original inherited jar may enter native custody");
            require(touches.put(INHERITED_JAR,new Touch(item,custodyCount(p.getInventory().items),inheritedJarStack.copy(),item.position()))==null,"Nonreentrant inherited native touch");return;
        }
        if(!s.drops.containsKey(item.getUUID())) {
            require(!ownedArea(item.getBoundingBox()),"Never touch inherited/unclassified owned-area stock");return;
        }
        require(ItemStack.matches(item.getItem(),s.drops.get(item.getUUID())),"Same physical UUID/full components before native contact");
        require(touches.put(item.getUUID(),new Touch(item,componentCount(p.getInventory().items,item.getItem()),item.getItem().copy(),item.position()))==null,"Non-reentrant actual playerTouch");
    }
    void touchReturn(ItemEntity item) {
        Touch before=touches.remove(item.getUUID());if(before==null)return;
        require(before.identity()==item,"Actual paired native same-identity playerTouch");
        boolean original=INHERITED_JAR.equals(item.getUUID());
        int delta=(original?custodyCount(p.getInventory().items):componentCount(p.getInventory().items,before.stack()))-before.before();
        if(delta==0) {require(!item.isRemoved() && (original?custodyMatches(rawPhysicalStack(item)):ItemStack.matches(item.getItem(),before.stack())),"Refused native pickup retains actual stock");return;}
        require(delta==1 && before.stack().getCount()==1 && item.isRemoved() && item.getRemovalReason()==Entity.RemovalReason.DISCARDED,"Actual native full same-UUID contact consumes item and inserts same full components");
        if(INHERITED_JAR.equals(item.getUUID())) {
            require(item==inheritedJar && !inheritedJarPicked && before.before()==0 && custodyCount(p.getInventory().items)==1
                    && BoreInheritedItemWitness.picked() && ItemStack.matches(BoreInheritedItemWitness.oldJarStack(),before.stack()),"Independent native startup/touch witness and root original jar inventory delta0->1 must agree");
            var clock=custodyClock();custody.picked(clock.tick(),clock.wall(),clock.elapsed(),clock.rTicks(),clock.rWall(),s.phase);
            inheritedJarPicked=true;naturalRemovals.add(INHERITED_JAR);
            custodyEvents.add(row("event","actual inherited native playerTouch RETURN pickup","native_tick",clock.tick(),"wall_nanos",clock.wall(),
                    "native_game_time",level.getGameTime(),"R_native_age",clock.rTicks(),"R_wall_nanos",clock.rWall(),
                    "UUID",INHERITED_JAR.toString(),"full_stack",snbt(before.stack(),p),"inventory_before",0,"inventory_after",1,
                    "actual_contact_position",before.position().toString(),"actual_player_position",p.position().toString(),"native_removal_reason",item.getRemovalReason().name(),
                    "acquisition_output_credit",0,"observer_pickup_calls",0));return;
        }
        require(s.picked.add(item.getUUID()),"One acknowledged actual pickup per real UUID");
        pendingNativeEvidence.add(new StackEvent("physical",row("event","ACTUAL native playerTouch RETURN pickup","UUID",item.getUUID().toString(),"tick",p.server.getTickCount(),
                "actual_contact_position",before.position().toString(),"actual_player_position",p.position().toString(),"inventory_delta",delta,"observer_pickup_calls",0),Map.of("stack",before.stack().copy())));
    }

    private void preserve(View now) {
        require(p.experienceLevel==0 && p.totalExperience==123 && Float.floatToIntBits(p.experienceProgress)==Float.floatToIntBits(.17391325533390045F),"No native XP grants/debits; exact0/123/0.173913255...");
        require(p.gameMode.getGameModeForPlayer()==GameType.SURVIVAL && !p.getAbilities().flying && !p.getAbilities().instabuild
                && p.isInvulnerable()==savedEntityInvulnerable && p.getAbilities().invulnerable==savedAbilityInvulnerable && rawHealth(p)==20.0F,
                "Actual Survival mode and exact inherited entity/ability protection; no mode/health/NoAI/invulnerability setters");
        require(ItemStack.matches(savedOffhand,p.getOffhandItem()),"All inherited offhand components retained");
        int armor=0;for(EquipmentSlot slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))require(ItemStack.matches(savedArmor.get(armor++),p.getItemBySlot(slot)),"All actual worn armor components unchanged");
        List<ItemStack> values=physicalStacks();labelCustody(now,values);var groups=new HashSet<String>();
        for(ItemStack old:savedInventory)if(!old.isEmpty()) {
            String key=snbt(old.copyWithCount(1),p);if(!groups.add(key))continue;
            if(old.is(Items.GOLD_INGOT) || old.is(Items.COBBLESTONE) || old.is(phial()) || old.is(Items.WOODEN_PICKAXE) || old.is(ThaumcraftFoci.EXCAVATION_FOCUS.get()) || old.getItem() instanceof ItemJarLabel || payer(old))continue;
            int expected=componentCount(savedInventory,old);require(componentCount(values,old)==expected,"Inherited component-sensitive item family retained through menu/drop/storage: "+key);
        }
        var payers=values.stream().filter(BoreClientSequence::payer).toList();require(payers.size()==1 && payers.getFirst().getCount()==1,"ONE actual inherited payer owner");
        ItemStack payer=payers.getFirst();require(sameExcept(payer,savedPayer,ThaumcraftContent.WAND_DATA.get()),"Payer entire non-economic components unchanged");
        baseCrafts=owned(baseItem())+(be(BASE) instanceof TileArcaneBoreBase?1:0);require(baseCrafts>=0 && baseCrafts<=1,"Only ONE earned native base");
        for(String tag:WandData.PRIMALS)require(ItemWandCasting.getVis(payer,tag)==ItemWandCasting.getVis(savedPayer,tag)-baseCrafts*((tag.equals("aer") || tag.equals("ordo"))?900:0),"Every actual payer CV change is the one native900+900 base payment");
        require(componentCount(values,savedFocus)==1,"Same old paid Potency1 focus, unchanged components, loose or installed exactly ONCE");
        var picks=values.stream().filter(v->v.is(Items.WOODEN_PICKAXE)).toList();require(picks.size()==1 && sameExcept(picks.getFirst(),savedPick,DataComponents.DAMAGE)
                && picks.getFirst().getDamageValue()>=5 && picks.getFirst().getDamageValue()<=6,"Old wood pick exact shell, only genuine relay wear5->6");
        int scanner=owned(ThaumcraftContent.THAUMOMETER.get());require(scanner<=1,"ONE genuinely crafted scanner, no completed-tool grant");
        int goldConsumed=ingredientsConsumed.getOrDefault("minecraft:gold_ingot",0);
        require(owned(Items.GOLD_INGOT)==108+(s.inputs.stream().anyMatch(r->"R".equals(r.get("phase")))?2:0)-2*scanner-goldConsumed,"Exact actual gold108+2-2scanner-2infusion, no slot normalization");
        int scribe=owned(ThaumcraftContent.INKWELL.get());require(scribe<=1 && owned(phial())==2-scribe+(s.boreDelivered?16:0)-consumed[3],"Old phials2->1scribe +16source -16real distill =1, no invented spare");
        int cobbleWorld=0;for(BlockPos cell:cobbleCells())if(block(cell).is(Blocks.COBBLESTONE))cobbleWorld++;
        int lever=owned(Items.LEVER)+(block(LEVER).is(Blocks.LEVER)?1:0);
        int cobbleExpected=4+(s.inputs.stream().anyMatch(r->"R".equals(r.get("phase")))?4:0)+(s.boreDelivered?1:0);
        require(owned(Items.COBBLESTONE)+cobbleWorld+lever==cobbleExpected,"All old/new cobble conserved across4support pads/foundation/scaffold/lever/two native target outputs");
        if(s.boreDelivered) {
            pickCrafts=owned(pickItem())+consumed[0];require(pickCrafts>=0 && pickCrafts<=9 && owned(thaumium())==27-3*pickCrafts,"All9 unstackable picks are real3ingot crafts, eight genuinely consumed");
            require(owned(Items.STICK)==19-2*pickCrafts-lever,"All18 pick sticks + one lever stick paid natively");
        }
        knowledge(now);preserveEntities();preserveBEs();
        require(pending().stream().allMatch(v->!v.isEmpty()),"Queue stock rows remain representable");
    }
    /** A native label attachment is not a missing item. Count its actual filter
     * owner, not a synthetic ItemStack; every other inherited family stays exact.
     * These are adjacent POST observations, NOT an invented interact RETURN. */
    private void labelCustody(View now,List<ItemStack> values) {
        int blanks=componentCount(values,savedLabel);
        require(values.stream().filter(v->v.getItem() instanceof ItemJarLabel).mapToInt(ItemStack::getCount).sum()==blanks,"No altered/bound/duplicate label stack may hide behind installed custody");
        int actual=BoreClientEvidence.metalLabelCustodyIndex(blanks,now.alembics().stream().map(Alembic::filter).toList());
        if(actual!=installedLabel) {
            require(s.researchDone && s.boreDelivered && previous!=null,"No label edge before real research/B delivery and adjacent native observation");
            String edge;
            if(actual>=0) {
                require(installedLabel==-1 && labelBindings==0 && labelReturns==0,"Exactly one original blank-label attachment, never transfer/rebind/refill");
                Alembic before=previous.alembics().get(actual),after=now.alembics().get(actual);
                require(before.filter().isEmpty() && before.amount()>0 && before.tag().equals("metallum") && after.amount()>0 && after.tag().equals("metallum")
                        && ItemStack.matches(previous.hand(),savedLabel) && !previous.sneaking(),"Binding edge requires actual positive metal buffer and same selected original blank label");
                labelBindings++;edge="physical original blank -> actual metallum filter";
            } else {
                require(labelBindings==1 && labelReturns==0 && previous.alembics().get(installedLabel).amount()==0 && now.alembics().get(installedLabel).amount()==0
                        && previous.sneaking() && ItemJar.isJar(previous.hand()) && ItemJar.getAspect(previous.hand())==Aspect.VOID && ItemJar.getAmount(previous.hand())==16
                        && sourceTotal==300,"One empty-buffer label removal via actual retained vacuos16 jar/Shift; all300 already generated");
                labelReturns++;edge="actual empty metallum filter -> physical/pending original blank";
            }
            s.actions.add(row("event","adjacent actual POST label custody edge","native_tick",now.tick(),"before_installed_alembic",installedLabel,"after_installed_alembic",actual,
                    "exact_physical_blank_owners",blanks,"edge",edge,"direct_interact_RETURN_claim",false,"observer_item_filter_writes",0));
            installedLabel=actual;
        }
        if(actual>=0)require(now.alembics().get(actual).amount()==0 || now.alembics().get(actual).tag().equals("metallum"),"Installed metal filter cannot disguise another positive source tag");
    }
    private void knowledge(View now) {
        PlayerKnowledge k=KnowledgeAccess.get(p);Set<String> base=baselineResearch();
        var allowed=new LinkedHashSet<>(base);if(k.isResearchComplete("ARCANEBORE")) {require(s.noteRequests==1 && s.editActions>0,"Learned key only after ONE real edited note");allowed.add("ARCANEBORE");}
        require(k.getResearchComplete().equals(allowed),"No unrelated research flag grants/removals");
        var old=baselineKnowledge.copy();var current=k.save();for(String key:List.of("researchComplete","discoveredAspects","scannedItems","scannedPhenomena")) {old.remove(key);current.remove(key);}
        require(old.equals(current),"All other knowledge/scannedEntities/warp state untouched");
        Set<String> itemDelta=new LinkedHashSet<>(now.scanned());itemDelta.removeAll(previous.scanned());Set<String> nodeDelta=new LinkedHashSet<>(now.phenomena());nodeDelta.removeAll(previous.phenomena());
        require(itemDelta.size()+nodeDelta.size()<=1,"At most ONE actual first scan per native POST");
        if(!itemDelta.isEmpty() || !nodeDelta.isEmpty()) {
            String signature=itemDelta.isEmpty()?nodeDelta.iterator().next():itemDelta.iterator().next();require(signature.equals(expectedScan),"Actual scan signature must match one announced real ray, no alternate target credit");
            scans++;require(scans<=9,"Nine distinct first scan identities only");
            Map<String,Integer> tags;
            if(!nodeDelta.isEmpty()) {var node=(TileNode)be(NODE);var out=new TreeMap<String,Integer>();for(Aspect a:node.getAspects().getAspects())out.put(a.getTag(),Math.max(4,node.getAspects().getAmount(a)/10));tags=out;}
            else if(signature.equals("@thaumcraft:resonator"))tags=Map.of("metallum",12,"aqua",2,"aer",2);
            else if(signature.equals("@thaumcraft:wand"))tags=Map.of("metallum",4,"praecantatio",7,"instrumentum",5);
            else {String primal=null;for(String tag:WandData.PRIMALS)if(signature.equals("@"+BuiltInRegistries.ITEM.getKey(shard(tag))))primal=tag;require(primal!=null,"Exactly six actual shard ITEM-IDs");tags=Map.of(primal,2,"praecantatio",1,"vitreus",1);}
            var expected=new TreeMap<>(previous.pools());
            for(var row:tags.entrySet()) {int before=expected.getOrDefault(row.getKey(),0),reward=row.getValue()+(expected.containsKey(row.getKey())?0:2);if(before>=100)reward=(int)Math.sqrt(reward);if(reward>1 && before>=125)reward=1;expected.put(row.getKey(),before+reward);}
            require(now.pools().equals(expected),"Every genuine first scan RP/discovery award from actual resolver row, no other pool normalization");
            s.actions.add(row("event","ACTUAL authoritative first scan/pool edge","signature",signature,"before",previous.pools(),"after",now.pools(),"native_tick",now.tick()));
        }
        require(now.scanned().containsAll(previous.scanned()) && now.phenomena().containsAll(previous.phenomena()),"No scan/reset/reroll ledger erasure");
        if(s.researchDone)require(k.save().equals(learnedKnowledge),"No research/RP mutations during paid Bore phase");
    }

    private void preserveBEs() {
        for(var support:protectedSupports.entrySet())require(block(support.getKey())==support.getValue(),"All protected original support/pillar/floor cells unchanged: "+support.getKey());
        for(var entry:oldBEs.entrySet()) {
            BlockPos pos=entry.getKey();var chunk=loadedChunk(pos.getX()>>4,pos.getZ()>>4);
            BlockEntity actual=chunk==null?null:be(pos);
            boolean pending=chunk!=null && ((BoreNativeContext.Owners)chunk).thaumcraft$boreHasPendingOwner(pos);
            boolean loaded=chunk!=null && !pending;Boolean prior=beCoverage.put(pos,loaded);
            if(prior==null || prior!=loaded)lifecycle.add(row("event","actual parent BE availability edge","pos",pos.toShortString(),"loaded",loaded,"availability",chunk==null?"CHUNK_UNAVAILABLE":pending?"NATIVE_OWNER_PENDING":"CURRENT_NATIVE_CELL", "native_tick",p.server.getTickCount(),"unseen_interval","STOPPED_AUDIT_REQUIRED; no observer load"));
            if(!loaded) {
                identities.remove(pos);NodeState state=nodes.get(pos);if(state!=null)nodes.put(pos,new NodeState(null,state.count(),state.current(),state.base(),state.id(),state.modifier(),state.waitTicks(),state.regeneration(),state.catchUp()));
                require(!requiredParentBE(pos),"Required action-context parent BE current owner unavailable: "+pos);continue;
            }
            if(pos.equals(RELAY))continue; // EXACT one native mine/same-UUID return, checked separately.
            var expected=parent.bes.get(pos);
            require(actual!=null && !actual.isRemoved() && expected.type().equals(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(actual.getType()).toString()),"Trusted loaded parent BE type/current owner: "+pos);
            BlockEntity priorOwner=identities.get(pos);
            require(priorOwner==null || priorOwner==actual,"Continuously loaded inherited BE identity changed: "+pos);identities.put(pos,actual);
            if(pos.equals(FURNACE))require(parent.bes.get(pos).state().name().equals(BuiltInRegistries.BLOCK.getKey(chunk.getBlockState(pos).getBlock()).toString()),"Actual inherited furnace block binding");
            else require(expected.state().matches(chunk.getBlockState(pos)),"Trusted parent BE block/state binding: "+pos);
            if(ownedBE(pos))continue;
            CompoundTag after=tag(actual),before=BoreClientParentModel.loadedOwnerExpected(entry.getValue(),after);
            if(actual instanceof TileNode node) {
                validateNodeSnapshot(node);
                for(String key:List.of("Aspects","AspectsBase","lastActive")) {before.remove(key);after.remove(key);}
                // ONLY the modifier actually authorized by a paired native prune.
                Integer modifier=authorizedModifiers.get(pos);if(modifier!=null)before.putByte("modifier",modifier.byteValue());
            }
            // Existing valve's original cached suction amount is a NATIVE dynamic,
            // not a stock waiver; its full contents/other metadata remain equal.
            if(pos.equals(p(1882,218,377))) {before.remove("samount");after.remove("samount");}
            if(!before.equals(after))s.report.put("unexpected_parent_BE_difference",row("pos",pos.toShortString(),
                    "compared_expected_NBT",evidence.payloadVersion("BE_difference",before.toString()),
                    "compared_actual_NBT",evidence.payloadVersion("BE_difference",after.toString())));
            require(before.equals(after),"All unrelated old BE stock/components/metadata unchanged: "+pos);
        }
        for(var entry:oldBlocks.entrySet()) {
            BlockPos pos=entry.getKey();var chunk=loadedChunk(pos.getX()>>4,pos.getZ()>>4);if(chunk==null || pos.equals(RELAY))continue;
            if(pos.equals(FURNACE))require(chunk.getBlockState(pos).getBlock()==entry.getValue().getBlock(),"Only genuine furnace LIT/FILLED display states may change");
            else require(chunk.getBlockState(pos)==entry.getValue(),"All other inherited TC blocks/candles remain exact: "+pos);
        }
        int oldJars=0,oldUnits=0;
        for(var expected:parent.bes.values())if(expected.type().equals("thaumcraft:jar")) {
            BlockEntity current=be(expected.pos());
            if(!(current instanceof TileJar)) {require(custody.coverageGap(),"ALL20 original jars must actually remain loaded outside the single approved custody excursion");continue;}
            oldJars++;oldUnits+=((TileJar)current).getAmount();
        }
        if(!custody.coverageGap())require(oldJars==20 && oldUnits==333,"Actual old20 jars/333 retained separately from NEW300");
        for(var candle:parent.candles)require(candle.state().matches(block(candle.pos())),"ALL72 actual altar candle cells remain loaded/equal");
        if(be(BASE) instanceof TileArcaneBoreBase base) {
            require(!base.isQuarantined(),"No invalid base data");
            for(Direction face:Direction.values()) {
                BlockEntity neighbour=be(BASE.relative(face));require(!(neighbour instanceof IEssentiaTransport),"No base essentia credit can mask real VisNet acceleration");
            }
        }
    }
    private static boolean ownedBE(BlockPos pos) {return pos.equals(BENCH) || pos.equals(MATRIX) || pos.equals(CENTER) || PEDESTALS.contains(pos) || pos.equals(FURNACE)
            || pos.equals(FURNACE.above()) || pos.equals(FURNACE.above(2)) || pos.equals(FURNACE.above(3));}
    private long inheritedElapsed() {long value=level.getGameTime()-133500;require(value>=0,"Accepted native game time may not be rewound");return value;}
    private record CustodyClock(long tick,long wall,long elapsed,long rTicks,long rWall) {}
    private CustodyClock custodyClock() {
        long tick=p.server.getTickCount(),wall=System.nanoTime();var observed=s.actualClockObservation(tick,wall);
        return new CustodyClock(tick,wall,inheritedElapsed(),((Number)observed.get("phase_native_age")).longValue(),((Number)observed.get("phase_wall_nanos")).longValue());
    }
    void custodyDeparture() {
        require(p.server.isSameThread() && ready && !s.researchDone && !s.boreDelivered,"Custody after R raw, before any R acquisition");
        require(p.containerMenu==p.inventoryMenu && p.containerMenu.getCarried().isEmpty() && !p.isUsingItem() && !p.isShiftKeyDown()
                && p.getInventory().items.stream().anyMatch(ItemStack::isEmpty),"Actual released native actor and one free main slot");
        require(scans==0 && s.noteRequests==0 && s.totalCombos==0 && owned(Items.CHEST)==0 && owned(ThaumcraftContent.THAUMOMETER.get())==0,"Custody precedes crafting/scanning/note issuance");
        inheritedCheckpoint("single custody departure");
        require(level.getEntity(INHERITED_JAR) instanceof ItemEntity,"Exact old jar must really be loaded before departure; never force load");
        ItemEntity actual=(ItemEntity)level.getEntity(INHERITED_JAR);require(!actual.isRemoved() && actual.getItem().getCount()==1,"Actual inherited count1 jar owner");
        require(BoreInheritedItemWitness.originalJarOwner(level)==actual,"Original inherited startup/tick owner must be the SAME actual departure owner");
        // Expected detached TAG only; never construct/install an inventory item.
        try {
            var expected=net.minecraft.nbt.TagParser.parseTag("{id:\"thaumcraft:jar\",count:1,components:{\"minecraft:max_stack_size\":1,\"thaumcraft:jar_label_data\":\"potentia\",\"minecraft:block_entity_data\":{Amount:8,facing:3b,id:\"thaumcraft:jar\",Aspect:\"potentia\"}}}");
            require(expected.equals(BorePassiveStackSnapshot.nativeTag(rawPhysicalStack(actual),level)),"Exact accepted old jar full typed component stack; no broad potentia exemption");
        } catch(com.mojang.brigadier.exceptions.CommandSyntaxException error) {throw new IllegalStateException("Invalid inherited expected metadata",error);}
        require(((Number)read(actual,"age")).intValue()>=4253 && ((Number)read(actual,"age")).intValue()<=4253+inheritedElapsed(),"Actual old Age and conservative native elapsed agree");
        inheritedJar=actual;inheritedJarTag=BorePassiveStackSnapshot.nativeTag(rawPhysicalStack(actual),level);
        // Observer-private comparison copy from the ACTUAL native raw patch,
        // never an expected-template stack, normalization or installed stock.
        inheritedJarStack=BorePassiveStackSnapshot.copyForComparison(rawPhysicalStack(actual),level);
        require(custodyCount(physicalStacks())==0,"No prior matching physical custody owner");
        var clock=custodyClock();custody.depart(clock.tick(),clock.wall(),clock.elapsed(),clock.rTicks(),clock.rWall(),s.phase);
        custodyEvents.add(row("event","single approved custody departure","native_tick",clock.tick(),"wall_nanos",clock.wall(),
                "native_game_time",level.getGameTime(),"R_native_age",clock.rTicks(),"R_wall_nanos",clock.rWall(),"UUID",INHERITED_JAR.toString(),
                "full_stack",snbt(inheritedJarStack,p),"actual_age",read(actual,"age"),"position",actual.position().toString(),"raw_delivery_count",s.inputs.size(),
                "actual_player_position",p.position().toString(),"actual_player_body",bounds(p.getBoundingBox())));
        custodyGuard();
    }
    boolean custodyPicked() {return inheritedJarPicked;}
    boolean matchesInheritedJar(ItemStack value) {return inheritedJarPicked && !inheritedJarStack.isEmpty() && ItemStack.matches(value,inheritedJarStack);}
    boolean custodyRecoveryReady() {
        custodyGuard();require(custody.state()==BoreCustodyWindow.State.RECOVERY,"Recovery poll only after genuine pickup");
        int jars=0,units=0,candles=0;
        for(var expected:parent.bes.values())if(expected.type().equals("thaumcraft:jar") && be(expected.pos()) instanceof TileJar jar) {jars++;units+=jar.getAmount();}
        for(var candle:parent.candles) {var chunk=loadedChunk(candle.pos().getX()>>4,candle.pos().getZ()>>4);if(chunk!=null && candle.state().matches(chunk.getBlockState(candle.pos())))candles++;}
        if(jars!=20 || units!=333 || candles!=72)return false;
        preserveBEs();observeOrdinaryNodes();
        require(lastAlchemy==p.server.getTickCount()-1 || lastAlchemy==p.server.getTickCount(),"Actual current furnace ticker, not geometric readiness");
        require(sourceEpoch>=0 && sourceEpoch==((TileNodeEnergized)be(SOURCE)).getRefillEpoch(),"Actual source native epoch at recovery boundary");
        var critical=criticalContextRows(true);var jarOwners=new ArrayList<Map<String,Object>>();var candleCells=new ArrayList<Map<String,Object>>();
        for(var expected:parent.bes.values())if(expected.type().equals("thaumcraft:jar")) {
            TileJar jar=(TileJar)be(expected.pos());var ticker=criticalOwnerRow(expected.pos(),true,true);
            jarOwners.add(row("pos",expected.pos().toShortString(),"actual_object",System.identityHashCode(jar),"actual_NBT",tagText(jar),
                    "actual_block",block(expected.pos()).toString(),"native_owner_ticker",ticker));
        }
        for(var candle:parent.candles)candleCells.add(row("pos",candle.pos().toShortString(),"actual_block",block(candle.pos()).toString()));
        var clock=custodyClock();custody.recovered(clock.tick(),clock.wall(),clock.elapsed(),clock.rTicks(),clock.rWall(),s.phase);
        preserveBEs();
        custodyEvents.add(row("event","actual restored20/333/72 readiness","native_tick",clock.tick(),"wall_nanos",clock.wall(),
                "native_game_time",level.getGameTime(),"R_native_age",clock.rTicks(),"R_wall_nanos",clock.rWall(),
                "actual_jars",jars,"actual_units",units,"actual_candles",candles,"actual_jar_owners",jarOwners,"actual_candle_cells",candleCells,
                "actual_critical_context_owners",critical,"actual_player_position",p.position().toString(),"actual_player_body",bounds(p.getBoundingBox()),
                "actual_furnace_last_native_tick",lastAlchemy,"actual_matrix_native_counter",matrix().getTickCount(),"actual_source_epoch",sourceEpoch,
                "coverage_gap_ended",true,"unseen_intervals","STOPPED_AUDIT_REQUIRED"));return true;
    }
    void custodyStowed() {
        require(p.containerMenu==p.inventoryMenu && p.containerMenu.getCarried().isEmpty(),"Native menu/cursor closed after actual slot7 transfer");
        require(be(CHEST) instanceof Container && custodyMatches(((Container)be(CHEST)).getItem(7)),"Actual crafted chest slot7 retains original full components");
        var clock=custodyClock();custody.stowed(clock.tick(),clock.wall(),clock.elapsed(),clock.rTicks(),clock.rWall(),s.phase);
        requireStowedCustody();custodyEvents.add(row("event","actual original jar stowed chest slot7","native_tick",clock.tick(),"wall_nanos",clock.wall(),
                "native_game_time",level.getGameTime(),"R_native_age",clock.rTicks(),"R_wall_nanos",clock.rWall(),"UUID",INHERITED_JAR.toString(),"full_stack",snbt(inheritedJarStack,p),
                "chest",CHEST.toShortString(),"slot",7,"native_altar_source_box",false,"old8_is_NEW_credit",false));
    }
    private void custodyGuard() {
        require(custodyMenuFrame==null,"Unclosed actual inherited custody menu HEAD/RETURN");
        custody.check(p.server.getTickCount(),System.nanoTime(),inheritedElapsed(),s.phaseNativeAge(),s.phaseWallAge(),s.phase);
        if(custody.coverageGap()) {
            require(!s.scanning && !s.mining && !p.isUsingItem() && !p.isShiftKeyDown() && p.containerMenu==p.inventoryMenu && p.containerMenu.getCarried().isEmpty(),"Custody-only excursion: no gameplay/menu/channel/Shift");
            require(furnace().getItem(0).isEmpty() && furnace().getVis()==0 && furnace().getBurnTime()==0 && !matrix().isCrafting()
                    && sourceTotal==0 && infusionDraws==0 && scans==0 && s.noteRequests==0 && s.totalCombos==0 && s.drops.isEmpty(),"No resource-consuming/research/Bore gameplay during coverage gap");
        }
        custodyOwner();
    }
    /** Exclusion uses this one proven current physical OWNER, not a jar-ID or
     * potentia-count subtraction. An equal NEW stack in another owner fails. */
    private ItemStack custodyOwner() {
        if(inheritedJarStack.isEmpty())return null;
        if(!inheritedJarPicked) {require(custodyCount(physicalStacks())==0,"Old jar cannot appear in custody before native pickup");return null;}
        require(inheritedJar!=null && inheritedJar.isRemoved() && inheritedJar.getRemovalReason()==Entity.RemovalReason.DISCARDED
                && level.getEntity(INHERITED_JAR)==null,"Picked inherited physical UUID has no replacement world owner");
        var owners=new ArrayList<ItemStack>();
        for(ItemStack stack:p.getInventory().items)if(custodyMatches(stack))owners.add(stack);
        if(custodyMatches(p.containerMenu.getCarried()))owners.add(p.containerMenu.getCarried());
        if(be(CHEST) instanceof Container chest && custodyMatches(chest.getItem(7)))owners.add(chest.getItem(7));
        require(owners.size()==1 && custodyCount(physicalStacks())==1,"Exactly one original component-sensitive player/cursor/chest7 owner, no merge/duplication/other slot");
        if(custody.state()==BoreCustodyWindow.State.STOWED)require(be(CHEST) instanceof Container chest && chest.getItem(7)==owners.getFirst(),"Original custody remains untouched in reserved chest7");
        return owners.getFirst();
    }
    private void requireStowedCustody() {require(custody.state()==BoreCustodyWindow.State.STOWED && custodyOwner()!=null,"Actual old Potentia8 permanently stowed separately from old333 and NEW300/112/188");}
    private boolean custodyMatches(ItemStack actual) {return inheritedJarTag!=null && !actual.isEmpty() && actual.is(jarItem()) && inheritedJarTag.equals(BorePassiveStackSnapshot.nativeTag(actual,level));}
    private int custodyCount(List<ItemStack> stacks) {int count=0;for(ItemStack stack:stacks)if(custodyMatches(stack))count+=stack.getCount();return count;}
    private String passiveStackText(ItemStack actual) {return actual.isEmpty()?"EMPTY":BorePassiveStackSnapshot.nativeTag(actual,level).toString();}
    void custodyMenuHead(AbstractContainerMenu menu,int slot,int button,ClickType type) {
        if(!inheritedJarPicked)return;
        if(custody.state()==BoreCustodyWindow.State.STOWED) {requireStowedCustody();return;}
        require(custodyMenuFrame==null && p.server.isSameThread() && menu==p.containerMenu,"Actual same-owner nonreentrant native server menu HEAD");
        List<Map<String,Object>> frames=custodyMenuFrames("thaumcraft$boreCustodyMenuHead");
        custodyMenuFrame=new CustodyMenuFrame(menu,slot,button,type,p.server.getTickCount(),custodyMenuState(menu),frames);
    }
    void custodyMenuReturn(AbstractContainerMenu menu,int slot,int button,ClickType type) {
        if(!inheritedJarPicked)return;
        if(custody.state()==BoreCustodyWindow.State.STOWED) {requireStowedCustody();return;}
        CustodyMenuFrame before=custodyMenuFrame;
        require(before!=null && before.menu()==menu && menu==p.containerMenu && before.slot()==slot && before.button()==button && before.type()==type
                && before.tick()==p.server.getTickCount(),"Actual paired inherited custody native menu RETURN/arguments");
        var frames=custodyMenuFrames("thaumcraft$boreCustodyMenuReturn");var after=custodyMenuState(menu);
        if(before.before().get("owner").equals("ACTUAL_CHEST_SLOT"))require(after.get("owner").equals("ACTUAL_CHEST_SLOT") && after.get("slot").equals(7),"Never extract/replay/drain original stowed jar");
        boolean moved=!before.before().get("owner").equals(after.get("owner")) || !before.before().get("slot").equals(after.get("slot"));
        if(moved)require(type==ClickType.PICKUP || type==ClickType.SWAP,"Original jar owner changes only through native ordinary PICKUP or hotbar SWAP");
        require(custodyMenuEvents.size()<512,"Bounded complete native custody menu graph; no truncation");
        custodyMenuEvents.add(row("schema","bore_inherited_jar_native_menu_v1","event","actual native clicked HEAD/RETURN","native_tick",before.tick(),
                "world_game_time",level.getGameTime(),"menu_class",menu.getClass().getName(),"menu_object",System.identityHashCode(menu),"container_id",menu.containerId,
                "slot_argument",slot,"button_argument",button,"click_type",type.name(),"HEAD",before.before(),"RETURN",after,
                "HEAD_frames",before.frames(),"RETURN_frames",frames,"original_owner_changed",moved,"observer_mutations",0));
        custodyMenuFrame=null;
    }
    private Map<String,Object> custodyMenuState(AbstractContainerMenu menu) {
        var state=new LinkedHashMap<String,Object>(custodyRow());
        state.put("actual_main36",p.getInventory().items.stream().map(this::passiveStackText).toList());state.put("actual_cursor",passiveStackText(menu.getCarried()));
        var slots=new ArrayList<Map<String,Object>>();for(var slot:menu.slots)slots.add(row("menu_slot",slot.index,"container_slot",slot.getContainerSlot(),
                "is_player_inventory",slot.container==p.getInventory(),"is_custody_chest",slot.container==be(CHEST),"full_stack",passiveStackText(slot.getItem())));
        state.put("actual_menu_slots",slots);return Collections.unmodifiableMap(state);
    }
    private List<Map<String,Object>> custodyMenuFrames(String handler) {
        var frames=StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk(stream->stream.limit(161).toList());
        require(frames.size()<=160,"Finite native menu stack");int clicked=-1,packet=-1;
        for(int i=0;i<frames.size();i++) {
            var frame=frames.get(i);
            if(frame.getDeclaringClass()==AbstractContainerMenu.class && frame.getMethodName().equals("clicked")
                    && frame.getDescriptor().equals("(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V"))clicked=i;
            if(frame.getClassName().equals("net.minecraft.server.network.ServerGamePacketListenerImpl") && frame.getMethodName().equals("handleContainerClick"))packet=i;
        }
        require(clicked>0 && packet>clicked && frames.get(clicked-1).getDeclaringClass()==AbstractContainerMenu.class && frames.get(clicked-1).getMethodName().contains(handler),"Actual transformed server clicked native packet spine, never synthetic custody callbacks");
        for(int i=clicked;i<=packet;i++) {
            var frame=frames.get(i);boolean exactBenchDelegate=frame.getDeclaringClass()==thaumcraft.common.container.ContainerArcaneWorkbench.class
                    && frame.getMethodName().equals("clicked") && frame.getDescriptor().equals("(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V");
            require(frame.getClassName().startsWith("net.minecraft.") || exactBenchDelegate,"Foreign/direct custody menu caller; only exact production bench super.clicked delegation is supported");
        }
        return frames.stream().map(frame->row("class",frame.getClassName(),"method",frame.getMethodName(),"descriptor",frame.getDescriptor(),"bci",frame.getByteCodeIndex(),"line",frame.getLineNumber())).toList();
    }
    private Map<String,Object> custodyRow() {
        if(normalStopTeardown && !(be(CHEST) instanceof Container))return row("schema","bore_inherited_jar_custody_v1","state",custody.state().name(),
                "coverage_gap_active",false,"owner","NORMAL_STOP_UNAVAILABLE","unseen_intervals","STOPPED_AUDIT_REQUIRED; no old stack substituted");
        ItemStack current=custodyOwner();String owner="LOOSE_OR_NOT_YET_OBSERVED";int slot=-1;
        if(current!=null) {
            if(current==p.containerMenu.getCarried())owner="ACTUAL_NATIVE_CURSOR";
            else if(be(CHEST) instanceof Container chest && current==chest.getItem(7)) {owner="ACTUAL_CHEST_SLOT";slot=7;}
            else {owner="ACTUAL_PLAYER_MAIN_SLOT";for(int i=0;i<36;i++)if(current==p.getInventory().getItem(i))slot=i;}
        }
        int jars=0,units=0;for(var expected:parent.bes.values())if(expected.type().equals("thaumcraft:jar") && be(expected.pos()) instanceof TileJar jar) {jars++;units+=jar.getAmount();}
        return row("schema","bore_inherited_jar_custody_v1","state",custody.state().name(),"coverage_gap_active",custody.coverageGap(),
                "actual_old_jars_loaded",jars,"actual_loaded_old_units",units,"unloaded_old_stock","STOPPED_AUDIT_REQUIRED; NOT observed stock",
                "original_loose_UUID",INHERITED_JAR.toString(),"picked",inheritedJarPicked,"owner",owner,"slot",slot,
                "full_stack",current==null?"NO_CURRENT_CUSTODY_STACK":passiveStackText(current),"native_elapsed_since_parent",inheritedElapsed(),
                "departure_tick",custody.departureTick(),"pickup_tick",custody.pickupTick(),"old_loose_units_are_NEW",false);
    }
    void requireFixtureLoaded(double x,double y,double z) {
        for(int cx=BlockPos.containing(x-1,y,z-1).getX()>>4;cx<=(BlockPos.containing(x+1,y,z+1).getX()>>4);cx++)
            for(int cz=BlockPos.containing(x-1,y,z-1).getZ()>>4;cz<=(BlockPos.containing(x+1,y,z+1).getZ()>>4);cz++)
                require(loadedChunk(cx,cz)!=null,"Supported fixture reads ONLY genuinely already-loaded native chunks");
    }
    private List<Map<String,Object>> criticalContextRows(boolean required) {
        var positions=new ArrayList<>(List.of(BENCH,MATRIX,CENTER,FURNACE,FURNACE.above(),FURNACE.above(2),FURNACE.above(3),NODE,SOURCE,RELAY));positions.addAll(PEDESTALS);
        var rows=new ArrayList<Map<String,Object>>();for(BlockPos pos:positions)rows.add(criticalOwnerRow(pos,
                pos.equals(FURNACE) || pos.equals(MATRIX) || pos.equals(NODE) || pos.equals(SOURCE) || pos.equals(RELAY),required));return rows;
    }
    @SuppressWarnings("unchecked") private Map<String,Object> criticalOwnerRow(BlockPos pos,boolean expectsTicker,boolean required) {
        var chunk=loadedChunk(pos.getX()>>4,pos.getZ()>>4);BlockEntity owner=be(pos);
        boolean pending=chunk!=null && ((BoreNativeContext.Owners)chunk).thaumcraft$boreHasPendingOwner(pos);
        Object wrapper=chunk==null?null:((Map<BlockPos,Object>)read(chunk,"tickersInLevel")).get(pos);
        Object bound=wrapper instanceof BoreNativeContext.WrapperAccess access?access.thaumcraft$boreWrappedTicker():null;
        boolean current=chunk!=null && !pending && owner!=null && !owner.isRemoved() && owner.getLevel()==level;
        boolean registered=current && wrapper!=null && ((BoreNativeContext.ChunkAccess)chunk).thaumcraft$boreRegisteredTicker(pos,wrapper)
                && bound instanceof BoreNativeContext.BoundAccess access && access.thaumcraft$boreBoundOwner()==owner && access.thaumcraft$boreBoundChunk()==chunk
                && access.thaumcraft$boreBoundTicker()!=null && wrapper.getClass().getEnclosingClass()==net.minecraft.world.level.chunk.LevelChunk.class
                && bound.getClass().getEnclosingClass()==net.minecraft.world.level.chunk.LevelChunk.class;
        if(required)require(current && (expectsTicker?registered:wrapper==null),"Actual critical owner/ticker availability, never radius/metadata readiness: "+pos);
        Object counter=owner instanceof TileInfusionMatrix matrix?matrix.getTickCount():owner instanceof TileNode node?node.getTickCount():
                owner instanceof TileNodeEnergized source?source.getRefillEpoch():owner instanceof TileAlchemyFurnace furnace?read(furnace,"count"):"NO_SERIALIZED_COUNTER";
        return row("pos",pos.toShortString(),"owner_status",current?"ACTUAL_CURRENT_NATIVE_OWNER":"NO_CURRENT_NATIVE_OWNER","actual_object",owner==null?0:System.identityHashCode(owner),
                "actual_type",owner==null?"NONE":BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(owner.getType()).toString(),"native_owner_pending",pending,
                "inherently_ticking",expectsTicker,"native_ticker_registered",registered,"ticker_status",expectsTicker?(registered?"ACTUAL_REGISTERED_CURRENT_BOUND_TICKER":"NO_CURRENT_BOUND_TICKER"):
                        (current && wrapper==null?"INHERENTLY_NONTICKING_BE":"NO_CURRENT_NONTICKING_OWNER"),
                "actual_wrapper_object",wrapper==null?0:System.identityHashCode(wrapper),"actual_bound_object",bound==null?0:System.identityHashCode(bound),
                "actual_native_counter",counter,"ticker_invocation_not_inferred_from_registration",true);
    }
    private boolean requiredParentBE(BlockPos pos) {return ownedBE(pos) || pos.equals(SOURCE) || pos.equals(RELAY) || pos.equals(NODE)
            || parent.bes.get(pos).type().equals("thaumcraft:jar") && !custody.coverageGap();}
    private record NodeState(TileNode identity,int count,Map<String,Integer> current,Map<String,Integer> base,String id,int modifier,int waitTicks,int regeneration,boolean catchUp) {}
    private NodeState nodeState(TileNode node) {return new NodeState(node,node.getTickCount(),includingZero(node.getAspects()),includingZero(node.getAspectsBase()),node.getScanId(),
            node.getNodeModifier()==null?-1:node.getNodeModifier().ordinal(),node.getRechargeWait(),((Number)read(node,"regeneration")).intValue(),(Boolean)read(node,"catchUp"));}
    private void validateNodeSnapshot(TileNode node) {
        BlockPos pos=node.getBlockPos();NodeState now=nodeState(node),before=nodes.get(pos);
        if(before==null || before.identity()!=node) {
            CompoundTag expected=oldBEs.get(pos);var base=new AspectList();base.readFromNBT(expected,"AspectsBase");
            require(now.base().equals(before==null?includingZero(base):before.base()) && now.modifier()==authorizedModifiers.getOrDefault(pos,(int)expected.getByte("modifier")),"First/reloaded native node cannot hide unexplained BASE/modifier changes");
            nodes.put(pos,now);
            nodeTimeline.add(row("event","actual loaded node phase binding","pos",pos.toShortString(),"native_tick",p.server.getTickCount(),"raw",nodeRow(now),"unseen_interval","STOPPED_AUDIT_REQUIRED"));
        } else require(now.count()==before.count() && now.base().equals(before.base()) && now.modifier()==before.modifier(),"MISSING paired native ordinary-node RETURN or unexplained BASE/modifier mutation");
    }
    void nodeHead(TileNode node) {
        BlockPos pos=node.getBlockPos();if(!oldBEs.containsKey(pos))return;
        require(node==be(pos) && !nodeFrames.containsKey(pos),"Actual current nonreentrant native ordinary-node HEAD");
        validateNodeSnapshot(node);nodeFrames.put(pos,new NodeFrame(p.server.getTickCount(),nodeState(node)));
    }
    void nodeReturn(TileNode node) {
        BlockPos pos=node.getBlockPos();if(!oldBEs.containsKey(pos))return;
        NodeFrame frame=nodeFrames.remove(pos);NodeState after=nodeState(node);
        require(frame!=null && frame.tick()==p.server.getTickCount(),"Paired same-native-tick ordinary-node RETURN");NodeState before=frame.before();
        require(before.identity()==node && node==be(pos) && before.id().equals(after.id()),"Paired same-owner ordinary-node native RETURN");
        require(after.count()==before.count() || after.count()==before.count()+1,"Native ordinary-node increments at most once");
        boolean changedBase=!after.base().equals(before.base()),changedModifier=after.modifier()!=before.modifier();
        var removed=new LinkedHashSet<>(before.current().keySet());removed.removeAll(after.current().keySet());
        if(changedBase || changedModifier) {
            require(after.count()==before.count()+1 && after.count()%1200==0 && changedBase,"Only actual1200 prune may authorize BASE/modifier edge");
            require(after.base().keySet().equals(before.base().keySet()) && removed.size()<=1,"Native prune retains BASE keys and removes at most one current tag");
            int losses=0;for(var entry:before.base().entrySet()) {
                int lost=entry.getValue()-after.base().get(entry.getKey());
                require(lost==0 || lost==1 && before.current().containsKey(entry.getKey()) && before.current().get(entry.getKey())==0,"Only zero-current native prune BASE loss1");losses+=lost;
            }
            require(losses>0,"Actual native prune must debit BASE");
            for(String tag:removed)require(before.current().get(tag)==0 && before.base().get(tag)-after.base().get(tag)==1,"Modifier witness requires the actually pruned zero-current tag");
            if(changedModifier) {
                // Native code can chain null->PALE->FADING in ONE pruning invocation.
                require(removed.size()==1 && (before.modifier()==-1 && (after.modifier()==1 || after.modifier()==2) || before.modifier()==1 && after.modifier()==2),"Only supported native null->PALE[/FADING] or PALE->FADING prune edges");
                authorizedModifiers.put(pos,after.modifier());
            }
            nodeTimeline.add(row("event","ACTUAL paired native1200 pruning","pos",pos.toShortString(),"native_tick",p.server.getTickCount(),"before",nodeRow(before),"after",nodeRow(after),"removed_current_tags",List.copyOf(removed),
                    "modifier_edges",before.modifier()==-1 && after.modifier()==2?List.of("NULL->PALE","PALE->FADING"):List.of(before.modifier()+"->"+after.modifier()),"node_writes",0));
        }
        nodes.put(pos,after);
    }
    private static Map<String,Object> nodeRow(NodeState v) {return row("counter",v.count(),"current",v.current(),"BASE",v.base(),"scanId",v.id(),"modifier",v.modifier(),"recharge_wait",v.waitTicks(),"regeneration",v.regeneration(),"catchUp",v.catchUp());}
    private void observeOrdinaryNodes() {
        require(nodeFrames.isEmpty(),"Missing ordinary-node RETURN frame");
        for(BlockPos pos:oldBEs.keySet())if(be(pos) instanceof TileNode node)validateNodeSnapshot(node);
    }
    private void observeEpoch() {
        require(be(SOURCE) instanceof TileNodeEnergized && be(SOURCE)==identities.get(SOURCE),"Keep actual inherited source identity/AURA/capacity, no node replacement/refill fixture");
        TileNodeEnergized source=(TileNodeEnergized)be(SOURCE);require(!source.isQuarantined(),"Native source not quarantined");
        Map<String,Integer> available=amounts(source.getAvailableVis()),capacity=amounts(source.getVisBase());long epoch=source.getRefillEpoch();
        long paid=rawGrants-previousRaw;
        if(sourceEpoch<0) {
            sourceEpoch=epoch;sourceAvailable=available;
            for(String tag:WandData.PRIMALS)pulseGenerated.put(tag,(long)available.getOrDefault(tag,0));
            require(paid==0,"First observed epoch cannot hide a pre-baseline Bore grant");previousRaw=rawGrants;return;
        }
        long change=epoch-sourceEpoch;require(change==1,"Required live source must commit exactly one REAL native refill epoch per contiguous POST after initial binding");
        for(String tag:WandData.PRIMALS) {
            int amount=tag.equals("perditio")?(int)paid:0,old=sourceAvailable.getOrDefault(tag,0),now=available.getOrDefault(tag,0),cap=capacity.getOrDefault(tag,0);
            if(change==0)require(now==old-amount && amount<=old,"Same actual epoch residual conservation "+tag);
            else {
                boolean afterRefill=now==cap-amount,priorEpoch=now==cap && amount<=old;
                require(afterRefill || priorEpoch,"Actual source pulse/grant ordering must reconcile "+tag);
                int oldPaid=afterRefill?0:amount;pulseUnused.merge(tag,(long)old-oldPaid,Long::sum);pulseGenerated.merge(tag,(long)cap,Long::sum);
            }
            epochPaid.merge(tag,(long)amount,Long::sum);
            require(pulseGenerated.getOrDefault(tag,0L)==epochPaid.getOrDefault(tag,0L)+pulseUnused.getOrDefault(tag,0L)+now,"Every observed capacity pulse = actual native grants + replaced unused + live residual "+tag);
        }
        epochs.add(row("native_tick",p.server.getTickCount(),"epoch_before_after",List.of(sourceEpoch,epoch),"before",evidence.payloadVersion("source_vis",sourceAvailable),"after",evidence.payloadVersion("source_vis",available),"raw_THIS_POST",paid,"all_actual_raw",rawGrants,
                "capacity",evidence.payloadVersion("source_capacity",capacity),"AURA_BASE",evidence.payloadVersion("source_AURA",amounts(source.getAuraBase())),"observed_initial_plus_generated",Map.copyOf(pulseGenerated),"unused_replaced",Map.copyOf(pulseUnused),"actual_paid",Map.copyOf(epochPaid)));
        sourceEpoch=epoch;sourceAvailable=available;previousRaw=rawGrants;
    }
    void observeMob(Mob mob,boolean nativeLate) {
        UUID id=mob.getUUID();var expected=parent.cows.get(id);
        if(expected!=null)expected.validate(mob,level,rawHealth(mob));
        Entity priorOwner=allEntities.get(id);
        if(firstLiving.containsKey(id)) {
            if(priorOwner!=mob) {
                require(priorOwner==null || priorOwner.isRemoved() && unloadReasons.get(id)==Entity.RemovalReason.UNLOADED_TO_CHUNK,"Known UUID replacement must follow witnessed native chunk unload");
                if(protectedMobile.containsKey(id)) {
                    require(unloadReasons.get(id)==Entity.RemovalReason.UNLOADED_TO_CHUNK,"Mobile rebind needs exact native unload reason");
                    mobileScope(mob);protectedMobile.get(id).rebind(mob,p.server.getTickCount(),mobileObservation(mob));
                } else if(protectedLiving.containsKey(id))require(living(mob).equals(protectedLiving.get(id)),"Reappearing protected UUID must match SAME trusted/first baseline");
                allEntities.put(id,mob);
                Long gap=cowGaps.remove(id);lifecycle.add(row("event","actual native known-UUID rebind","UUID",id.toString(),"native_tick",p.server.getTickCount(),"uncovered_since_tick",gap==null?"UNKNOWN":gap,
                        "reason",unloadReasons.get(id)==null?"NONE":unloadReasons.get(id).name(),"unseen_interval","STOPPED_AUDIT_REQUIRED","mobile_binding_is_not_RETURN",protectedMobile.containsKey(id),"native",evidence.payloadVersion("living_state",living(mob))));
                unloadReasons.remove(id);
            } else if(protectedMobile.containsKey(id)) {
                mobileScope(mob);protectedMobile.get(id).observe(mob,p.server.getTickCount(),mobileObservation(mob));
            }
            if(expected!=null)seenParentCows.add(id);
            return;
        }
        allEntities.put(mob.getUUID(),mob);Map<String,Object> before=living(mob);firstLiving.put(mob.getUUID(),before);firstLivingTicks.put(mob.getUUID(),(long)p.server.getTickCount());
        if(mob.hasCustomName() || mob.isNoAi() || mob.isPersistenceRequired())protectedLiving.put(mob.getUUID(),before);
        else ambient.add(mob.getUUID());
        registerMobile(mob,before);
        if(expected!=null)seenParentCows.add(id);
        if(expected!=null && cowGaps.containsKey(id))lifecycle.add(row("event","actual first native parent11 observation after initial uncovered interval","UUID",id.toString(),"native_tick",p.server.getTickCount(),
                "uncovered_since_tick",cowGaps.remove(id),"reason","NOT_INITIALLY_LOADED","unseen_interval","STOPPED_AUDIT_REQUIRED","native",evidence.payloadVersion("living_state",before)));
        if(nativeLate)late.add(row("event","actual Mob.checkDespawn HEAD first observation","UUID",mob.getUUID().toString(),"native_tick",p.server.getTickCount(),"native",evidence.payloadVersion("living_state",before),"accepted_parent_membership",expected==null?"ROOT stopped NBT audit, NOT inferred from late loading":"EXACT trusted parent11 UUID validated"));
    }
    private void protect(LivingEntity entity) {Map<String,Object> before=living(entity);firstLiving.put(entity.getUUID(),before);protectedLiving.put(entity.getUUID(),before);}
    private static Map<String,Object> living(LivingEntity entity) {
        Mob mob=entity instanceof Mob m?m:null;return row("type",BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(),"position",entity.position().toString(),"health",rawHealth(entity),
                "name",entity.getName().getString(),"named",entity.hasCustomName(),"NoAI",mob!=null && mob.isNoAi(),"persistent",mob!=null && mob.isPersistenceRequired(),"invulnerable",entity.isInvulnerable());
    }
    private void registerMobile(Mob mob,Map<String,Object> original) {
        UUID id=mob.getUUID();
        if(!protectedLiving.containsKey(id) || parent.cows.containsKey(id) || !Boolean.FALSE.equals(original.get("named")) || !Boolean.FALSE.equals(original.get("NoAI"))
                || !Boolean.TRUE.equals(original.get("persistent")) || mob.isPassenger() || !outsideOwned(mob.getBoundingBox()) || !nativeMobileClass(mob))return;
        require(!protectedMobile.containsKey(id),"Mobile cohort is registered only from the original protected observation");
        mobileScope(mob);protectedMobile.put(id,new BoreClientEvidenceMotion(mob,p.server.getTickCount(),mobileObservation(mob)));
        lifecycle.add(row("event","original protected outside mobile native binding (NOT a RETURN)","UUID",id.toString(),"native_tick",p.server.getTickCount(),
                "original_stable",evidence.payloadVersion("protected_mobile_stable",BoreClientEvidenceMotion.nonPosition(original)),"original_position",mob.position().toString(),"mandatory_gap_audit","STOPPED_AUDIT_REQUIRED"));
    }
    private static boolean nativeMobileClass(Mob mob) {return mob.getClass().getName().startsWith("net.minecraft.world.entity.");}
    private void mobileScope(Mob mob) {
        require(p.server.isSameThread() && mob.level()==level && !parent.cows.containsKey(mob.getUUID()) && nativeMobileClass(mob),"Mobile protection is only for actual same-level vanilla Mob owners, never parent11");
        require(!mob.hasCustomName() && !mob.isNoAi() && mob.isPersistenceRequired() && !mob.isPassenger() && outsideOwned(mob.getBoundingBox()),"Protected mobile must remain unnamed normal-AI persistent/nonpassenger OUTSIDE every owned/old-block/BE footprint");
    }
    private static BoreClientEvidenceMotion.Point motionPoint(Vec3 value) {return new BoreClientEvidenceMotion.Point(value.x,value.y,value.z);}
    private static BoreClientEvidenceMotion.Observation mobileObservation(Mob mob) {
        return new BoreClientEvidenceMotion.Observation(mob.tickCount,motionPoint(mob.position()),motionPoint(mob.getDeltaMovement()),living(mob));
    }
    /** Only the registered protected mobile cohort pays for this stack read. No
     * supplied native-eligibility flag, movement reason or alleged actor token. */
    private void mobileNativeFrame() {
        require(p.server.isSameThread(),"Native mobile witness on actual owner server thread");
        var frames=StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk(stream->stream.toList());int body=-1;
        for(int i=0;i<frames.size();i++)if(frames.get(i).getDeclaringClass()==ServerLevel.class && frames.get(i).getMethodName().equals("tickNonPassenger")
                && frames.get(i).getDescriptor().equals("(Lnet/minecraft/world/entity/Entity;)V")) {body=i;break;}
        require(body>=0,"MISSING actual ServerLevel.tickNonPassenger(Entity) body");boolean worldTick=false,serverTick=false;
        for(int i=body+1;i<frames.size();i++) {
            var frame=frames.get(i);if(frame.getDeclaringClass()==ServerLevel.class && frame.getMethodName().equals("tick"))worldTick=true;
            if(frame.getDeclaringClass()==net.minecraft.server.MinecraftServer.class && frame.getMethodName().equals("tickServer")) {serverTick=true;break;}
            require(frame.getClassName().startsWith("net.minecraft.") || frame.getClassName().startsWith("java."),"Foreign/direct mobile tick caller is not native world entity ticking");
        }
        require(worldTick && serverTick,"Mobile body must be reached from the actual native world/server tick spine");
    }
    void mobileHead(Mob mob) {
        BoreClientEvidenceMotion motion=protectedMobile.get(mob.getUUID());if(motion==null)return;
        mobileNativeFrame();mobileScope(mob);
        require(!mob.isRemoved() && level.getEntity(mob.getUUID())==mob && allEntities.get(mob.getUUID())==mob,"Actual native mobile HEAD current UUID owner");
        motion.head(mob,p.server.getTickCount(),mobileObservation(mob));
    }
    void mobileReturn(Mob mob) {
        BoreClientEvidenceMotion motion=protectedMobile.get(mob.getUUID());if(motion==null)return;
        mobileNativeFrame();
        if(motion.unloaded()) {
            require(unloadReasons.get(mob.getUUID())==Entity.RemovalReason.UNLOADED_TO_CHUNK,"Only genuine native unload may close a body without a current-owner observation");
            motion.unloadedReturn(mob,p.server.getTickCount());return; // NEVER read stale removed-owner health/position.
        }
        mobileScope(mob);
        require(!mob.isRemoved() && level.getEntity(mob.getUUID())==mob && allEntities.get(mob.getUUID())==mob,"Actual paired native mobile RETURN current UUID owner");
        motion.returned(mob,p.server.getTickCount(),mobileObservation(mob));
    }
    private void preserveEntities() {
        for(var entry:protectedLiving.entrySet()) {
            UUID id=entry.getKey();Entity stored=allEntities.get(id),entity=level.getEntity(id);
            if(entity==null || entity.isRemoved()) {
                require(stored!=null && stored.isRemoved() && unloadReasons.get(id)==Entity.RemovalReason.UNLOADED_TO_CHUNK,"Protected UUID absent without actual native chunk-unload witness: "+id);continue;
            }
            require(entity==stored && entity instanceof LivingEntity,"Loaded protected UUID must be bound by actual ADD/reobservation, never stale owner: "+id);
            if(protectedMobile.containsKey(id)) {
                require(entity instanceof Mob && id.equals(entity.getUUID()),"Protected mobile retains actual UUID/Mob type");Mob mob=(Mob)entity;mobileScope(mob);protectedMobile.get(id).verify(mob,mobileObservation(mob));
            } else require(living((LivingEntity)entity).equals(entry.getValue()),"All named/NoAI/protected living entities exact native health/position/AI flags: "+id);
            var expected=parent.cows.get(id);if(expected!=null)expected.validate((LivingEntity)entity,level,rawHealth((LivingEntity)entity));
        }
        for(Entity entity:allEntities.values())require(!entity.isRemoved() || entity.getRemovalReason()!=null && !entity.getRemovalReason().shouldDestroy() || naturalRemovals.contains(entity.getUUID()),"Unwitnessed global inherited/late-entity removal: "+entity.getUUID()+"/"+entity.getRemovalReason());
    }
    boolean outsideOwned(AABB body) {
        if(ownedArea(body))return false;
        for(BlockPos pos:oldBEs.keySet())if(body.intersects(new AABB(pos)))return false;
        for(BlockPos pos:oldBlocks.keySet())if(body.intersects(new AABB(pos)))return false;
        return true;
    }
    /** Concrete stock/action cells, not the broad observation rectangles. Used
     * ONLY for a witnessed vanilla checkDespawn of an already ambient Mob. It
     * does not relax mobile/NoAI ownership, environmental deaths or item custody. */
    private boolean outsideConcreteProtectedCells(AABB body) {
        var cells=new ArrayList<BlockPos>(oldBEs.keySet());cells.addAll(oldBlocks.keySet());cells.addAll(protectedSupports.keySet());
        cells.addAll(List.of(BENCH,TABLE,TABLE.south(),CHEST,FURNACE,FURNACE.above(),FURNACE.above(2),FURNACE.above(3),
                MATRIX,CENTER,RELAY,SOURCE,NODE,FOUNDATION,BASE,TOP,TOP.south(),SCAFFOLD,LEVER,SLOW,FAST));
        cells.addAll(PEDESTALS);for(int i=0;i<11;i++)cells.add(jarPos(i));
        cells.addAll(List.of(p(1602,215,379),p(1601,215,379),p(1601,215,378),p(1601,215,377)));
        return BoreClientEvidenceGeometry.outside(body,p.getBoundingBox(),cells);
    }
    void removal(Entity entity,Entity.RemovalReason reason) {
        if(entity==p)return;
        UUID id=entity.getUUID();
        if(ambientItems.containsKey(id)) {ambientItemRemoval((ItemEntity)entity,reason);return;}
        if(ambientLootSources.containsKey(id))require(ambientLootSources.get(id)==entity && reason.shouldDestroy(),"Ambient loot source cannot unload/rebind between birth and actual death");
        if(INHERITED_JAR.equals(id) && reason.shouldDestroy()) {
            require(entity==inheritedJar && custody.state()==BoreCustodyWindow.State.OUTBOUND && reason==Entity.RemovalReason.DISCARDED
                    && nativeFrame(ItemEntity.class.getName(),"playerTouch") && touches.containsKey(id),"Inherited jar may leave the world ONLY within its genuine same-owner native pickup");return;
        }
        if(parent.cows.containsKey(id) && reason.shouldDestroy()) {
            lifecycle.add(row("event","FORBIDDEN actual protected parent11 destruction","UUID",id.toString(),"reason",reason.name(),"native_tick",p.server.getTickCount()));
            s.fail(new IllegalStateException("Unexpected destruction of ANY trusted parent11 cow: "+id+"/"+reason));return;
        }
        if(!reason.shouldDestroy()) {
            if(parent.cows.containsKey(id) || protectedLiving.containsKey(id) || allEntities.containsKey(id)) {
                require(reason==Entity.RemovalReason.UNLOADED_TO_CHUNK,"Only native chunk unload is an allowed inherited availability gap: "+id+"/"+reason);
                if(entity instanceof Mob mob)observeMob(mob,false);
                if(protectedMobile.containsKey(id)) {
                    require(entity instanceof Mob,"Native mobile unload retains Mob identity");Mob mob=(Mob)entity;mobileScope(mob);
                    protectedMobile.get(id).unload(mob,p.server.getTickCount(),mobileObservation(mob));
                }
                unloadReasons.put(id,reason);cowGaps.putIfAbsent(id,(long)p.server.getTickCount());
                lifecycle.add(row("event","actual native Entity.setRemoved HEAD non-destroying unload","UUID",id.toString(),"reason",reason.name(),"native_tick",p.server.getTickCount(),"position",entity.position().toString(),
                        "status","STOPPED_AUDIT_REQUIRED; gap is not preservation proof","entity_mutations",0));
            }
            return;
        }
        if(s.drops.containsKey(entity.getUUID())) {
            require(reason==Entity.RemovalReason.DISCARDED && nativeFrame(ItemEntity.class.getName(),"playerTouch") && touches.containsKey(entity.getUUID()),"Owned physical item may retire ONLY in witnessed native contact, not merge/despawn/discard");return;
        }
        if(!allEntities.containsKey(entity.getUUID())) {
            if(entity instanceof Mob mob)observeMob(mob,true);else {require(outsideOwned(entity.getBoundingBox()),"Unclassified owned-area entity removal");return;}
        }
        List<String> frames=Arrays.stream(Thread.currentThread().getStackTrace()).map(f->f.getClassName()+"."+f.getMethodName()).toList();
        String cause="UNPROVEN",damage="NONE";boolean outside=outsideOwned(entity.getBoundingBox());
        boolean outsideConcrete=outsideConcreteProtectedCells(entity.getBoundingBox());
        boolean unprotectedAmbient=ambient.contains(entity.getUUID()) && !protectedLiving.containsKey(entity.getUUID()) && !parent.cows.containsKey(entity.getUUID());
        if(entity instanceof Mob mob && unprotectedAmbient && !mob.hasCustomName() && !mob.isNoAi() && !mob.isPersistenceRequired()) {
            int removed=frames.indexOf("net.minecraft.world.entity.Entity.setRemoved"),remove=frames.indexOf("net.minecraft.world.entity.Entity.remove");
            boolean nativeCaller=removed>=0 && remove>removed;
            for(int i=Math.max(0,removed);i<frames.size();i++)if(frames.get(i).startsWith("thaumcraft."))nativeCaller=false;
            long stamp=((Number)read(mob,"lastDamageStamp")).longValue();DamageSource cached=(DamageSource)read(mob,"lastDamageSource");
            DamageSource source=level.getGameTime()-stamp>40?null:cached;Object lastHurt=read(mob,"lastHurtByMob");
            boolean unowned=lastHurt!=p && (source==null || source.getEntity()!=p && source.getDirectEntity()!=p);
            if(nativeCaller && unowned) {
                int discard=frames.indexOf("net.minecraft.world.entity.Entity.discard"),despawn=frames.indexOf("net.minecraft.world.entity.Mob.checkDespawn");
                if(reason==Entity.RemovalReason.DISCARDED && discard>remove && despawn>discard && outsideConcrete)cause="VANILLA_CHECK_DESPAWN";
                if(outside && source!=null) {
                    damage=source.typeHolder().unwrapKey().orElseThrow().location().toString();
                    boolean environmental=source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.FALL) || source.is(DamageTypes.FELL_OUT_OF_WORLD)
                            || source.is(DamageTypes.DROWN) || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.LAVA) || source.is(DamageTypes.FREEZE);
                    if(reason==Entity.RemovalReason.KILLED && frames.indexOf("net.minecraft.world.entity.LivingEntity.tickDeath")>remove && environmental
                            && source.getEntity()==null && source.getDirectEntity()==null && rawHealth(mob)<=0)cause="VANILLA_ENVIRONMENTAL_TICK_DEATH";
                }
            }
        }
        AABB actualBody=entity.getBoundingBox(),actualPlayerBody=p.getBoundingBox();
        boolean proven=!cause.equals("UNPROVEN");lifecycle.add(row("schema","bore_native_mob_retirement_v2","event","actual native Entity.setRemoved HEAD","native_tick",p.server.getTickCount(),"world_game_time",level.getGameTime(),
                "UUID",entity.getUUID().toString(),"type",BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(),
                "reason",reason.name(),"position",entity.position().toString(),"outside_owned",outside,
                "actual_body_bounds",List.of(actualBody.minX,actualBody.minY,actualBody.minZ,actualBody.maxX,actualBody.maxY,actualBody.maxZ),
                "actual_owner_body_bounds",List.of(actualPlayerBody.minX,actualPlayerBody.minY,actualPlayerBody.minZ,actualPlayerBody.maxX,actualPlayerBody.maxY,actualPlayerBody.maxZ),
                "actual_flags",entity instanceof Mob mob?row("named",mob.hasCustomName(),"NoAI",mob.isNoAi(),"persistent",mob.isPersistenceRequired()):"NOT_MOB",
                "outside_concrete_protected_cells",outsideConcrete,"registered_unprotected_ambient",unprotectedAmbient,
                "scope","Only true vanilla checkDespawn may cross a broad observation buffer; concrete cells and all protected/item ownership remain strict",
                "cause",cause,"damage",damage,"native_frames",frames,"native_frame_descriptors",detachedNativeFrames(),
                "health_raw",entity instanceof LivingEntity live?rawHealth(live):"NOT_LIVING","damage_at_removal",entity instanceof Mob mob?damageAtRemoval(mob):"NOT_MOB",
                "proven_vanilla",proven,"entity_mutations",0));
        if(ambientLootSources.containsKey(id)) {require(proven && cause.equals("VANILLA_ENVIRONMENTAL_TICK_DEATH"),"Ambient source-linked birth requires its actual same-object later environmental tickDeath, not proximity/despawn");ambientLootDeaths.add(id);ambientAllRetired=ambientLootDeaths.equals(ambientLootSources.keySet());}
        if(proven)naturalRemovals.add(entity.getUUID());else s.fail(new IllegalStateException("Unproven inherited entity removal; never freeze AI or restore entities: "+entity.getUUID()+"/"+reason));
    }

    void researchFinal() {
        PlayerKnowledge k=KnowledgeAccess.get(p);require(k.isResearchComplete("ARCANEBORE") && s.noteRequests==1 && s.editActions>=1 && s.editActions<=29 && s.totalCombos>=13 && s.totalCombos<=19 && scans==9,"Actual ONE note learning/9 scans/finite combinations and ink");
        require(physicalStacks().stream().noneMatch(v->v.getItem() instanceof ItemResearchNotes) && count(p,Items.PAPER)==0,"Actual paper+completed note consumption, not local complete setter");
        require(be(TABLE) instanceof TileResearchTable table && table.getItem(0).is(ThaumcraftContent.INKWELL.get()) && table.getItem(0).getDamageValue()==1+s.editActions && table.getItem(1).isEmpty(),"Same actual partly used inkwell back at new table0, damage issuance1+edits");
        require(be(CHEST) instanceof Container,"Earned real storage chest");Container chest=(Container)be(CHEST);int occupied=0;
        for(int i=0;i<chest.getContainerSize();i++)if(!chest.getItem(i).isEmpty())occupied++;
        require(occupied==8 && countContainer(chest,ThaumcraftContent.THAUMOMETER.get())==1,"Actual scanner/six shards/original old jar in eight native chest slots");
        requireStowedCustody();
        for(String tag:WandData.PRIMALS)require(countContainer(chest,shard(tag))==1,"All six scanned shards retained unchanged before B retrieval");
        require(p.getInventory().items.stream().filter(v->!v.isEmpty()).count()==22 && count(p,Items.GOLD_INGOT)==108 && count(p,phial())==1,"Natural core22occupied/14free again; real gold108/phial1, no slot restoration");
        require(be(BENCH) instanceof TileArcaneWorkbench bench && bench.isEmpty() && p.containerMenu==p.inventoryMenu && p.containerMenu.getCarried().isEmpty(),"Research menus/bench/cursor genuinely closed/empty");
        learnedKnowledge=k.save().copy();s.report.put("genuine_research_final_receipt",row("actor_knowledge",learnedKnowledge.toString(),"same_note_hash",BoreResearchNotePlan.fingerprint(s.issued),"native_first_scans",scans,
                "native_combines",s.totalCombos,"additional_motus",s.bufferCombos,"actual_paid_edit_actions",s.editActions,"ink_damage",1+s.editActions,"main_occupied",22,"stock_restorations",0));
    }
    void infusionAdmission() {
        requireStowedCustody();
        require(s.researchDone && s.boreDelivered && sourceTotal==300 && consumed[0]==8 && consumed[1]==4 && consumed[2]==24 && consumed[3]==16,"All actual300 produced BEFORE activation, no assumed future source budget");
        require(snapshot().sourceStock().equals(PRODUCED) && furnace().getVis()==0 && furnace().getItem(0).isEmpty(),"All actual source units retained, no unknown buffers");
        for(int i=1;i<=3;i++) {var a=(TileAlembic)be(FURNACE.above(i));require(a.getAmount()==0 && a.getFilter()==null,"All alembics genuinely drained and original blank label returned, no clear setter");}
        require(owned(ItemJarLabel.blank().getItem())==componentCount(savedInventory,ItemJarLabel.blank()),"Original blank label physically returned via native same UUID");
        for(int i=0;i<11;i++)require(be(jarPos(i)) instanceof TileJar && block(jarPos(i).below()).is(Blocks.STONE),"All eleven actual new source jars on original real floor");
        int old=0;for(var entry:oldBEs.entrySet())if(be(entry.getKey()) instanceof TileJar jar) {CompoundTag actual=tag(jar);
            require(BoreClientParentModel.loadedOwnerExpected(entry.getValue(),actual).equals(actual),"Every old jar/filter/component unchanged BEFORE start (only proven false outer chunk header may be absent live): "+entry.getKey());old+=jar.getAmount();
            if(inSourceBox(entry.getKey()) && jar.getAmount()>0)require(!COST.containsKey(jar.getAspect().getTag()),"Original x/z/y source order may NOT debit an old matching-cost jar");}
        require(old==333 && matrix().isActive() && !matrix().isCrafting(),"Actual old333 separate / same active matrix");
        Item wood=ThaumcraftNaturals.GREATWOOD_PLANKS.get().asItem();Item[] want={wood,wood,Items.GOLD_INGOT,Items.GOLD_INGOT,Items.DIAMOND_PICKAXE,Items.DIAMOND_SHOVEL,shard("aer"),shard("terra")};
        for(int i=0;i<8;i++) {ItemStack actual=pedestal(PEDESTALS.get(i)).getItem(0);require(actual.is(want[i]) && actual.getCount()==1 && (!actual.isDamageableItem() || actual.getDamageValue()==0),"Literal actual count1/pristine original Bore ingredients");}
        require(pedestal(CENTER).getItem(0).is(Items.PISTON) && pedestal(CENTER).getItem(0).getCount()==1,"Actual piston central ownership");
        // Exact source enumeration order is x THEN z THEN y[-12,11], never nearest.
        var sources=new ArrayList<String>();for(int dx=-12;dx<=12;dx++)for(int dz=-12;dz<=12;dz++)for(int dy=-12;dy<12;dy++) {
            BlockPos pos=MATRIX.offset(dx,dy,dz);BlockEntity tile=be(pos);if(tile instanceof thaumcraft.api.aspects.IAspectSource)sources.add(pos.toShortString());}
        s.report.put("native_source_order_admission",sources);s.report.put("pre_infusion_actual_NEW300",snapshot().sourceStock());
    }
    private static boolean inSourceBox(BlockPos pos) {return Math.abs(pos.getX()-MATRIX.getX())<=12 && Math.abs(pos.getZ()-MATRIX.getZ())<=12 && pos.getY()-MATRIX.getY()>=-12 && pos.getY()-MATRIX.getY()<12;}
    void infusionFinal() {
        requireStowedCustody();
        require(finishedInfusion && infusionDraws==112 && ingredientCount==8 && infusionPaid.equals(COST) && snapshot().sourceStock().equals(SURPLUS),"Full native300/112/188, not a cost-only or collateral-losing success");
        require(owned(topItem())+(be(TOP) instanceof TileArcaneBore?1:0)==1 && !s.earnedTop.isEmpty() && owned(pickItem())==1,"Actual paid top in its ONE physical/pedestal/installed owner, and ninth real operating pick remain");
        int filled=0,empty=0,units=0;for(int i=0;i<11;i++) {TileJar jar=(TileJar)be(jarPos(i));if(jar.getAmount()==0)empty++;else filled++;units+=jar.getAmount();}
        require(filled==7 && empty==4 && units==188,"Seven filled +four genuinely emptied jars, all eleven vessels");
        s.report.put("native_infusion_final",row("actual_draws",infusionDraws,"actual_ingredients",ingredientsConsumed,"all_NEW_retained",snapshot().sourceStock(),"all11vessels",List.of(filled,empty),"actual_remaining_coal",lastCoal,"actual_burn",furnace().getBurnTime()));
    }
    void finish() {
        inheritedCheckpoint("actual final full snapshot");
        ambientCheckpoint();require(ambientLootDeaths.equals(ambientLootSources.keySet()),"All ambient source births have actual later same-source environmental deaths");
        View now=snapshot();preserve(now);infusionFinal();
        require(installedLabel==-1 && labelBindings==1 && labelReturns==1,"Original blank label genuinely attached once and physically returned once");
        require(harvests==2 && slowEligible==36 && fastEligible==9 && rawGrants>0 && harvestIds.size()==2,"Actual native36/9 / real relay acceleration / exactly two physical output UUIDs");
        for(UUID id:harvestIds.values())require(s.picked.contains(id) && level.getEntity(id)==null,"Both actual native ejections acknowledged by same-UUID playerTouch RETURN");
        require(be(RELAY) instanceof TileVisRelay && s.picked.stream().filter(id->s.drops.get(id).is(relayItem())).count()==1 && owned(relayItem())==0,"Same inherited relay mined, picked up and genuinely reinstalled, no spare/grant");
        require(pickCrafts==9 && consumed[0]==8 && owned(pickItem())==1 && be(TOP) instanceof TileArcaneBore,"Exactly9 actual crafts,8real sources,1operating owner");
        TileArcaneBore top=(TileArcaneBore)be(TOP);require(ItemStack.matches(top.getItem(0),savedFocus) && top.getItem(1).is(pickItem()) && top.getItem(1).getDamageValue()==2 && top.stats().speed()==1 && top.stats().area()==0 && top.stats().fortune()==0,"Same paid old focus unchanged and exact operating wear2/width5/speed1/fortune0");
        require(top.miningDirection()==Direction.SOUTH && top.attachment()==Direction.UP && !top.isPowered() && !top.isQuarantined() && ((TileArcaneBoreBase)be(BASE)).nozzle()==Direction.NORTH,"Actual installed states/nozzle/off, never forced flags");
        require(count(p,Items.GOLD_INGOT)==106 && count(p,phial())==1 && count(p,Items.COBBLESTONE)==2 && count(p,Items.WOODEN_PICKAXE)==1,"Final physical gold106/phial1/two native cobbles/same worn old pick");
        for(Item exhausted:List.of(Items.OAK_PLANKS,Items.OAK_SLAB,Items.GLASS,Items.FEATHER,Items.INK_SAC,Items.PAPER,Items.IRON_INGOT,Items.DISPENSER,Items.PISTON,
                Items.DIAMOND_PICKAXE,Items.DIAMOND_SHOVEL,thaumium(),Items.STICK,Items.REDSTONE,ThaumcraftNaturals.GREATWOOD_PLANKS.get().asItem(),shard("aer"),shard("terra"),baseItem(),topItem()))
            require(owned(exhausted)==0,"Every declared exhausted raw/output family is actually consumed or installed; no disappearing surplus: "+BuiltInRegistries.ITEM.getKey(exhausted));
        for(BlockPos cell:cobbleCells())require(block(cell).is(cell.equals(SLOW) || cell.equals(FAST)?Blocks.AIR:Blocks.COBBLESTONE),"All4 earned support pads +foundation/scaffold retained, two actual targets removed");
        require(block(LEVER).is(Blocks.LEVER),"Genuinely crafted/placed lever retained");
        Container chest=(Container)be(CHEST);require(countContainer(chest,ThaumcraftContent.THAUMOMETER.get())==1,"Actual earned scanner retained after B");
        requireStowedCustody();int occupiedChest=0;for(int i=0;i<chest.getContainerSize();i++)if(!chest.getItem(i).isEmpty())occupiedChest++;
        require(occupiedChest==6,"Final chest: scanner/four untouched shards/one original labelled jar, exactly six occupied slots");
        for(String tag:WandData.PRIMALS)require(countContainer(chest,shard(tag))==(tag.equals("aer") || tag.equals("terra")?0:1),"Only genuinely retrieved Air/Earth consumed by infusion; four other scanned IDs retained");
        require(physicalStacks().stream().filter(v->v.is(Items.WOODEN_PICKAXE)).findFirst().orElseThrow().getDamageValue()==6,"Old relay mine alone wood5->6");
        require(((TileArcaneWorkbench)be(BENCH)).isEmpty() && p.containerMenu==p.inventoryMenu && p.containerMenu.getCarried().isEmpty() && !p.isUsingItem() && !p.isShiftKeyDown(),"Normal cursor/menu/wand/Shift cleanup, no stock normalization");
        for(AABB area:List.of(RESEARCH_AREA,BORE_AREA,ALTAR_AREA))require(level.getEntitiesOfClass(ItemEntity.class,area).isEmpty() && level.getEntitiesOfClass(ExperienceOrb.class,area).isEmpty(),"No lost native physical outputs/XP");
        require(pending().isEmpty() && s.expectedDrop==null && s.drops.keySet().equals(s.picked),"All physical UUID custody reconciled, no pending/quarantine/lost target");
        s.report.put("final_full_native_receipt_UNINSPECTED",row("all300_generated",Map.copyOf(generated),"all112_paid",Map.copyOf(infusionPaid),"all188_retained",now.sourceStock(),
                "all8_ingredients",ingredientsConsumed,"actual9_pick_crafts",pickCrafts,"actual_36_9_eligible",List.of(slowEligible,fastEligible),"actual_raw_Perditio",rawGrants,"actual_harvest_UUIDs",Map.copyOf(harvestIds),
                "all_physical_UUIDs",s.picked.stream().map(UUID::toString).toList(),"focus_full_components",snbt(top.getItem(0),p),"operating_pick_full_components",snbt(top.getItem(1),p),
                "XP_LEVEL_TOTAL_PROGRESS",List.of(p.experienceLevel,p.totalExperience,p.experienceProgress),"gold",106,"empty_phials",1,"native_output_cobbles",2,"coal_items_unspent",lastCoal,"coal_actually_ignited",burns,"native_live_burn",furnace().getBurnTime(),"POSTs_so_far",checks,"tail_continues_through_disconnect",true));
    }
    void stopping() {
        s.report.put("native_stop_actual_clocks",s.actualClockObservation(p.server.getTickCount(),System.nanoTime()));
        inheritedCheckpoint("actual native stopped lifecycle callback");
        ambientCheckpoint();s.report.put("ambient_item_final_observer_states_NOT_stopped_audit",ambientItemRows());
        s.report.put("inherited_item_final_observer_state_NOT_stopped_audit",BoreInheritedItemWitness.snapshot());
        flushNativeEvidence();
        require(last==p.server.getTickCount() || last==p.server.getTickCount()-1,"No unobserved stopping tail gap");
        s.report.put("normal_stop_tail",row("first_POST_tick",first,"last_POST_tick",last,"POST_count",checks,"native_contiguous",checks==last-first+1,"story_complete",s.storyFinal,"failure",s.failure==null?"NONE":s.failure.toString()));
    }
    /** ROOT bridge ONLY from actual native MinecraftServer.stopServer HEAD.
     * Not a script flag or a general missing-ticker waiver. Never resumes checks. */
    void normalStopTeardown() {
        require(p.server.isSameThread() && nativeFrame("net.minecraft.server.MinecraftServer","stopServer") && (s.storyFinal || s.failure!=null),"Actual terminal native normal-stop frame after story acknowledgement/failure");
        inheritedCheckpoint("actual native normal-stop HEAD");
        ambientCheckpoint();
        if(normalStopTeardown)return;normalStopTeardown=true;
        lifecycle.add(row("event","ACTUAL native normal-stop teardown HEAD","native_tick",p.server.getTickCount(),"story_complete",s.storyFinal,"coverage_after_boundary","STOPPED_AUDIT_REQUIRED; missing native ticks/owners never fabricated"));
    }

    private List<ItemStack> physicalStacks() {
        var out=new ArrayList<ItemStack>();out.addAll(p.getInventory().items);out.addAll(p.getInventory().offhand);out.add(p.containerMenu.getCarried());
        if(be(BENCH) instanceof TileArcaneWorkbench bench)for(int i=0;i<11;i++)if(i!=9)out.add(bench.getItem(i));
        for(BlockPos pos:List.of(TABLE,CHEST,FURNACE,CENTER))if(be(pos) instanceof Container c)for(int i=0;i<c.getContainerSize();i++)out.add(c.getItem(i));
        for(BlockPos pos:PEDESTALS)out.add(pedestal(pos).getItem(0));
        if(be(TOP) instanceof TileArcaneBore top) {out.add(top.getItem(0));out.add(top.getItem(1));}
        var ids=new HashSet<UUID>();for(AABB area:List.of(RESEARCH_AREA,BORE_AREA,ALTAR_AREA))for(ItemEntity item:level.getEntitiesOfClass(ItemEntity.class,area))if(ids.add(item.getUUID()))out.add(item.getItem());
        out.addAll(pending());return out;
    }
    private int owned(Item item) {return physicalStacks().stream().filter(v->v.is(item)).mapToInt(ItemStack::getCount).sum();}
    private void inheritedCheckpoint(String boundary) {
        BoreInheritedItemWitness.checkpoint(level,boundary);Throwable failure=BoreInheritedItemWitness.failure();
        if(failure!=null)throw new IllegalStateException("Inherited physical witness failed; stop before losing stock",failure);
    }
    private static int componentCount(List<ItemStack> values,ItemStack pattern) {return values.stream().filter(v->ItemStack.isSameItemSameComponents(v,pattern)).mapToInt(ItemStack::getCount).sum();}
    private static int countContainer(Container c,Item item) {int n=0;for(int i=0;i<c.getContainerSize();i++)if(c.getItem(i).is(item))n+=c.getItem(i).getCount();return n;}
    private List<BoreClientEvidence.Ref> inventoryRows() {var rows=new ArrayList<BoreClientEvidence.Ref>();for(int i=0;i<36;i++)rows.add(evidence.payloadVersion("inventory_slot",row("slot",i,"stack_NBT",evidence.payloadVersion("stack",snbt(p.getInventory().getItem(i),p)))));return List.copyOf(rows);}
    private List<BoreClientEvidence.Ref> protectedRows() {
        var out=new ArrayList<BoreClientEvidence.Ref>();for(UUID id:protectedLiving.keySet()) {
            Entity current=level.getEntity(id),stored=allEntities.get(id);boolean live=current instanceof LivingEntity && !current.isRemoved() && current==stored;
            BoreClientEvidenceMotion motion=protectedMobile.get(id);Object observed="NO_CURRENT_OBSERVATION";
            if(live && motion!=null) {
                require(current instanceof Mob,"Actual protected mobile Mob binding");Mob mob=(Mob)current;mobileScope(mob);
                var actual=mobileObservation(mob);motion.verify(mob,actual);observed=evidence.payloadVersion("protected_mobile_stable",actual.stable());
            } else if(live)observed=evidence.payloadVersion("living_state",living((LivingEntity)current));
            out.add(evidence.payloadVersion("protected_living_row",row("UUID",id.toString(),"status",live?"ACTUAL_LOADED":"STOPPED_AUDIT_REQUIRED", "unload_reason",unloadReasons.get(id)==null?"NONE":unloadReasons.get(id).name(),
                    "protection_policy",motion==null?"STRICT_ORIGINAL_POSITION_AND_PROPERTIES":"NONPOSITION_EXACT_NATIVE_BODY_MOBILE",
                    "position",motion==null?"IN_NATIVE_SNAPSHOT":"SEE_THIS_POST_protected_mobile_actual_positions; original firstLiving position is not a fixed-position promise","native",observed)));
        }return out;
    }
    private static List<Double> motionVector(BoreClientEvidenceMotion.Point value) {return List.of(value.x(),value.y(),value.z());}
    /** Only the narrow registered cohort, not every ambient Mob. One compact
     * scalar row per POST; no growing per-entity native frame/trajectory array. */
    private List<Map<String,Object>> mobileRows() {
        var out=new ArrayList<Map<String,Object>>();long tick=p.server.getTickCount();
        for(var entry:protectedMobile.entrySet()) {
            UUID id=entry.getKey();BoreClientEvidenceMotion motion=entry.getValue();Entity actual=level.getEntity(id);
            if(motion.unloaded()) {
                require(!motion.frameOpen(),"MISSING native mobile RETURN after witnessed unload");
                require(actual==null || actual.isRemoved(),"Loaded mobile UUID must first be rebound by actual native ADD/reobservation");
                out.add(row("UUID",id.toString(),"observer_lifetime",motion.lifetime(),"status","STOPPED_AUDIT_REQUIRED_NO_CURRENT_POSITION",
                        "uncovered_since_tick",motion.unloadTick(),"reason","UNLOADED_TO_CHUNK"));continue;
            }
            require(actual instanceof Mob && id.equals(actual.getUUID()) && !actual.isRemoved() && actual==allEntities.get(id),"Protected mobile current UUID owner missing without native unload: "+id);
            Mob mob=(Mob)actual;var observed=mobileObservation(mob);String status,validationError="NONE";
            try {mobileScope(mob);status=motion.post(mob,tick,observed);}
            catch(Throwable error) {s.fail(error);status="FAILED_PROTECTED_MOTION_NOT_NATIVE_PROOF";validationError=error.toString();}
            var row=new LinkedHashMap<String,Object>();row.putAll(row("UUID",id.toString(),"observer_lifetime",motion.lifetime(),"binding_tick",motion.bindingTick(),
                    "status",status,"validation_error",validationError,"actual_native_counter",observed.counter(),"actual_position",motionVector(observed.position()),"actual_velocity",motionVector(observed.velocity()),
                    "actual_NONPOSITION",evidence.payloadVersion("protected_mobile_stable",observed.stable())));
            var receipt=motion.receipt();if(receipt!=null && receipt.tick()==tick)row.put("actual_paired_body",row("server_tick",receipt.tick(),"observer_lifetime",receipt.lifetime(),
                    "native_counter_before_after",List.of(receipt.before().counter(),receipt.after().counter()),"position_before_after",List.of(motionVector(receipt.before().position()),motionVector(receipt.after().position())),
                    "velocity_before_after",List.of(motionVector(receipt.before().velocity()),motionVector(receipt.after().velocity()))));
            else row.put("actual_paired_body","NONE_THIS_POST; unchanged initial/rebound/latest-paired position/counter, not invented native tick proof");
            out.add(Collections.unmodifiableMap(row));
        }return out;
    }
    private void observeProtectedCurrentOwners() {
        var candidates=new LinkedHashSet<UUID>(parent.cows.keySet());candidates.addAll(protectedLiving.keySet());
        for(UUID id:candidates) {
            Entity current=level.getEntity(id);
            if(current==null || current.isRemoved() || current==allEntities.get(id))continue;
            if(!(current instanceof Mob)) {require(!parent.cows.containsKey(id),"Trusted parent cow current owner must be an actual Mob");continue;}
            // Existing observeMob validates all original properties and requires
            // a witnessed UNLOADED_TO_CHUNK for any previously seen replacement.
            observeMob((Mob)current,false);
            lifecycle.add(row("event","actual SERVER_POST current UUID owner registration","UUID",id.toString(),
                    "native_tick",p.server.getTickCount(),"observation_origin","CURRENT_UUID_LOOKUP_NOT_ADD_OR_TICK",
                    "observed",evidence.payloadVersion("living_state",living((LivingEntity)current)),"entity_mutations",0));
        }
    }
    private List<BoreClientEvidence.Ref> parentCowRows() {
        var out=new ArrayList<BoreClientEvidence.Ref>();for(var expected:parent.cows.values()) {
            UUID id=expected.uuid();Entity current=level.getEntity(id);boolean live=current instanceof LivingEntity && !current.isRemoved();
            if(live) {
                require(current==allEntities.get(id),"Known parent cow loaded but not rebound by native ADD/reobservation: "+id);
                expected.validate((LivingEntity)current,level,rawHealth((LivingEntity)current));
            }
            out.add(evidence.payloadVersion("parent_cow_coverage_row",row("UUID",id.toString(),"known_parent",true,"seen_live",seenParentCows.contains(id),"status",live?"ACTUAL_LOADED_VALIDATED":"STOPPED_AUDIT_REQUIRED",
                    "uncovered_since_tick",cowGaps.getOrDefault(id,-1L),"unload_reason",unloadReasons.get(id)==null?"NOT_OBSERVED":unloadReasons.get(id).name(),
                    "actual",live?evidence.payloadVersion("living_state",living((LivingEntity)current)):"NO_CURRENT_OBSERVATION")));
        }return out;
    }
    private List<BoreClientEvidence.Ref> protectedTagRows() {
        var out=new ArrayList<BoreClientEvidence.Ref>();for(BlockPos pos:oldBEs.keySet()) {
            var chunk=loadedChunk(pos.getX()>>4,pos.getZ()>>4);BlockEntity tile=chunk==null?null:be(pos);
            out.add(evidence.payloadVersion("parent_BE_row",row("pos",pos.toShortString(),"status",tile==null?"STOPPED_AUDIT_REQUIRED":"ACTUAL_LOADED",
                    "actual_block",chunk==null?"UNAVAILABLE":chunk.getBlockState(pos).toString(),
                    "actual_NBT",tile==null?"NO_CURRENT_OBSERVATION":evidence.payloadVersion("BE_tag",tagText(tile)))));
        }return out;
    }
    private List<BoreClientEvidence.Ref> newTagRows() {var positions=new ArrayList<>(List.of(TABLE,TABLE.south(),CHEST,BASE,TOP));for(int i=0;i<11;i++)positions.add(jarPos(i));
        var out=new ArrayList<BoreClientEvidence.Ref>();for(BlockPos pos:positions) {BlockEntity tile=be(pos);out.add(evidence.payloadVersion("new_BE_row",row("pos",pos.toShortString(),"actual_block",observedBlock(pos),"actual_NBT",tile==null?"NO_LOADED_BE":evidence.payloadVersion("BE_tag",tagText(tile)))));}return out;}
    private Map<String,Object> furnaceRow() {if(normalStopTeardown && be(FURNACE)==null)return row("status","NORMAL_STOP_UNAVAILABLE_NO_FURNACE_OBSERVATION");var f=furnace();return row("full_NBT",evidence.payloadVersion("BE_tag",tagText(f)),"input",evidence.payloadVersion("stack",snbt(f.getItem(0),p)),"fuel",evidence.payloadVersion("stack",snbt(f.getItem(1),p)),"burn_cook_duration_vis",List.of(f.getBurnTime(),f.getCookTime(),f.getSmeltTime(),f.getVis()),"native_counter",read(f,"count"));}
    private Set<String> baselineResearch() {var list=baselineKnowledge.getList("researchComplete",net.minecraft.nbt.Tag.TAG_STRING);var out=new LinkedHashSet<String>();for(int i=0;i<list.size();i++)out.add(list.getString(i));return out;}
    static List<BlockPos> cobbleCells() {return List.of(p(1602,215,379),p(1601,215,379),p(1601,215,378),p(1601,215,377),FOUNDATION,SCAFFOLD,SLOW,FAST);}
    private static boolean ownedArea(AABB body) {return RESEARCH_AREA.intersects(body) || BORE_AREA.intersects(body) || ALTAR_AREA.intersects(body);}
    private static void add(Map<String,Integer> to,Map<String,Integer> from) {from.forEach((tag,n)->{if(n>0)to.merge(tag,n,Integer::sum);});}
    private static Map<String,Integer> includingZero(AspectList values) {var out=new TreeMap<String,Integer>();for(Aspect a:values.getAspects())out.put(a.getTag(),values.getAmount(a));return Map.copyOf(out);}
    private net.minecraft.world.level.chunk.LevelChunk loadedChunk(int x,int z) {
        require(level.getChunkSource() instanceof BoreNativeContext.LoadedChunks,"MISSING existing native raw loaded-chunk read adapter");
        return ((BoreNativeContext.LoadedChunks)level.getChunkSource()).thaumcraft$boreLoadedChunk(x,z);
    }
    private BlockEntity be(BlockPos pos) {
        var chunk=loadedChunk(pos.getX()>>4,pos.getZ()>>4);if(chunk==null)return null;
        require(chunk instanceof BoreNativeContext.Owners,"MISSING existing native current-owner read adapter");
        return ((BoreNativeContext.Owners)chunk).thaumcraft$boreExistingOwner(pos);
    }
    private BlockState block(BlockPos pos) {var chunk=loadedChunk(pos.getX()>>4,pos.getZ()>>4);require(chunk!=null,"Owned protected cell must be loaded, never force load: "+pos);return chunk.getBlockState(pos);}
    private String observedBlock(BlockPos pos) {var chunk=loadedChunk(pos.getX()>>4,pos.getZ()>>4);if(chunk==null && normalStopTeardown)return "NORMAL_STOP_UNAVAILABLE";return block(pos).toString();}
    private TileAlchemyFurnace furnace() {require(be(FURNACE) instanceof TileAlchemyFurnace,"Actual owned alchemy furnace identity");return (TileAlchemyFurnace)be(FURNACE);}
    private TileInfusionMatrix matrix() {require(be(MATRIX) instanceof TileInfusionMatrix,"Actual same formed native matrix");return (TileInfusionMatrix)be(MATRIX);}
    private TilePedestal pedestal(BlockPos pos) {require(be(pos) instanceof TilePedestal,"Actual loaded pedestal: "+pos);return (TilePedestal)be(pos);}
    private CompoundTag tag(BlockEntity tile) {
        if(!collectingPost)return tile.saveWithFullMetadata(tile.getLevel().registryAccess()).copy();
        return postTags.computeIfAbsent(tile,key->key.saveWithFullMetadata(key.getLevel().registryAccess()).copy()).copy();
    }
    private String tagText(BlockEntity tile) {return collectingPost?postTagStrings.computeIfAbsent(tile,key->tag(key).toString()):tag(tile).toString();}
    /** No virtual LivingEntity.getHealth or SynchedEntityData.get call-out. Read
     * the native accessor index and DataItem.value fields; never set data/dirty. */
    private static float rawHealth(LivingEntity entity) {
        Object accessor=read(entity,"DATA_HEALTH_ID"),data=read(entity,"entityData");
        int index=((Number)read(accessor,"id")).intValue();Object[] items=(Object[])read(data,"itemsById");
        require(index>=0 && index<items.length && items[index]!=null,"Native raw health data slot schema");
        Object value=read(items[index],"value");require(value instanceof Float,"Native raw health data is float");return (Float)value;
    }
    @SuppressWarnings("unchecked") private List<ItemStack> pending() {
        try {
            Field f=DeferredItemDrops.class.getDeclaredField("FACTORY");f.setAccessible(true);
            var factory=(SavedData.Factory<DeferredItemDrops>)f.get(null);DeferredItemDrops queue=level.getDataStorage().get(factory,"thaumcraft_pending_inventory_drops");if(queue==null)return List.of();
            require(queue.quarantinedCount()==0 && queue.boreRetirementCustodyCount()==0,"Never ignore opaque/quarantined custody as empty output");
            Set<UUID> settled=(Set<UUID>)read(queue,"settled");var rows=new ArrayList<Object>();for(Object entry:(Iterable<?>)read(queue,"pending"))rows.add(entry);
            Object flight=read(queue,"inFlight");if(flight!=null && !(Boolean)read(flight,"accepted"))rows.add(read(flight,"entry"));
            var ids=new HashSet<UUID>();var out=new ArrayList<ItemStack>();
            for(Object entry:rows) {
                UUID id=(UUID)read(entry,"id");if(settled.contains(id) || !ids.add(id))continue;BlockPos pos=(BlockPos)read(entry,"source");if(!ownedArea(new AABB(pos)))continue;
                ItemStack stack=(ItemStack)read(entry,"stack");if(level.getEntity(id) instanceof ItemEntity live && !live.isRemoved() && ItemStack.matches(live.getItem(),stack))continue;out.add(stack.copy());
            }
            return List.copyOf(out);
        }catch(ReflectiveOperationException error) {throw new IllegalStateException("Pure pending owner schema changed; NEVER call reconcile/save/tick as an observer",error);}
    }
}
