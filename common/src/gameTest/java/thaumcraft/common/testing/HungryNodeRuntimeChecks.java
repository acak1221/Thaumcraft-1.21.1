package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import thaumcraft.api.ThaumcraftApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ResearchSettings;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftEssences;
import thaumcraft.common.config.ThaumcraftNodeDevices;
import thaumcraft.common.config.ThaumcraftSettings;
import thaumcraft.common.lib.research.DeconstructionRules;
import thaumcraft.common.lib.research.ObjectAspectResolver;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.lib.world.HungryNodeTrace;
import thaumcraft.common.lib.world.ThaumcraftNormalNodeFeature;
import thaumcraft.common.testing.NodeTransductionRuntimeChecks.InputRandom;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileNode;

/** Native actors/items/blocks; branch inputs declare RNG choices, never inject a node reward. */
public final class HungryNodeRuntimeChecks {
    private HungryNodeRuntimeChecks() {}
    public static void run(GameTestHelper test) {
        var check=new Check(test);
        try { check.open(); } catch(RuntimeException|Error failure) { check.close(); throw failure; }
        // Real ServerPlayer starts with60 spawn-protected ticks. Its private
        // ServerPlayer gate is separate from LivingEntity.invulnerableTime;
        // let ordinary native level ticks expire it rather than changing either receipt or damage.
        test.runAfterDelay(65,()->{
            try { check.f.check(check.f.player.tickCount>=60,"actual native player has completed the initial sixty spawn-protected ticks; count="+check.f.player.tickCount);check.meals(); check.pullAndFlags(); check.blocks(); check.pearlAndHarvest(); check.naturalAndOrdinaryTicks(); }
            catch(RuntimeException|Error failure) { check.close(); throw failure; }
        });
    }
    private static final class Check implements AutoCloseable {
        final GameTestHelper test;final Fixture f;final ServerLevel world;final BlockPos pos;
        final ResearchSettings settings=ThaumcraftSettings.server();
        final List<ChunkPos> forced=new ArrayList<>();final List<Entity> entities=new ArrayList<>();final Set<UUID> oldEntities=new HashSet<>();
        TileNode node;boolean closed;
        Check(GameTestHelper test) { this.test=test;f=new Fixture(test);world=f.level;pos=f.base.above(100); }
        void open() {
            f.open();world.getAllEntities().forEach(entity->oldEntities.add(entity.getUUID()));forced.addAll(GolemChunks.hold(world,List.of(pos.offset(-18,-5,-18),pos.offset(18,30,18))));
            for(BlockPos mutable:BlockPos.betweenClosed(pos.offset(-4,-4,-4),pos.offset(4,4,4))) put(mutable.immutable(),Blocks.AIR);
            f.player.setInvulnerable(true);f.mode(GameType.CREATIVE);f.aim(Vec3.atCenterOf(pos));hard(true);
        }
        void hard(boolean hard) { ThaumcraftSettings.configureServer(new ResearchSettings(settings.cheatSheet(),settings.wardedStone(),settings.mirrors(),hard,settings.wuss(),settings.difficulty(),settings.aspectCap())); }
        void put(BlockPos where,net.minecraft.world.level.block.Block block) {
            if(!f.states.containsKey(where)) {
                f.states.put(where,world.getBlockState(where));
                if(world.getBlockEntity(where)!=null)f.originals.put(where,world.getBlockEntity(where));
            }
            f.put(where,block);
        }
        AspectList stock(int amount) { var result=new AspectList();for(Aspect aspect:Aspect.getPrimalAspects())result.add(aspect,amount);return result; }
        TileNode fresh(AspectList stock) {
            put(pos,ThaumcraftContent.AURA_NODE.get());node=(TileNode)world.getBlockEntity(pos);
            node.initializeNatural(stock,TileNode.NodeType.HUNGRY,TileNode.NodeModifier.FADING);return node;
        }
        void tick() { TileNode.serverTick(world,pos,world.getBlockState(pos),node); }
        void ticks(int count) { for(int n=0;n<count;n++)tick(); }
        void phase49() { for(int n=0;n<50 && node.getTickCount()%50!=49;n++)tick();f.check(node.getTickCount()%50==49,"bounded actual node phase49"); }
        <T extends Entity>T admit(T entity) { f.check(world.addFreshEntity(entity),"declared native physical input is admitted");entities.add(entity);return entity; }
        ItemEntity item(ItemStack actual,Vec3 at,int existingDamage) {
            var entity=new ItemEntity(world,at.x,at.y,at.z,actual,0,0,0);entity.setNoGravity(true);entity.setNeverPickUp();admit(entity);
            if(existingDamage>0)f.check(entity.hurt(world.damageSources().drown(),existingDamage),"declared existing native item health damage");return entity;
        }
        ItemStack named(net.minecraft.world.item.Item item,int amount,String name) { var stack=new ItemStack(item,amount);stack.set(DataComponents.CUSTOM_NAME,Component.literal(name));return stack; }
        void meals() {
            fresh(stock(3));var iron=named(Items.IRON_INGOT,13,"hungry-whole-stack-input");
            AspectList primals=DeconstructionRules.primals(ObjectAspectResolver.get(iron.copyWithCount(1),world));
            f.check(primals.size()>0,"actual one-iron scan has edible primal aspects");Aspect chosen=primals.getAspects()[0];node.takeVis(chosen.getTag(),1);
            put(pos.east(),ThaumcraftContent.AURA_NODE.get());var peer=(TileNode)world.getBlockEntity(pos.east());peer.initializeNatural(stock(3),TileNode.NodeType.HUNGRY,TileNode.NodeModifier.FADING);peer.takeVis(chosen.getTag(),1);
            var witness=new WitnessItem(world,Vec3.atCenterOf(pos),iron,node,peer);admit(witness);
            ticks(4);f.check(!witness.isRemoved() && witness.getItem()==iron && iron.getCount()==13 && node.getAspects().getAmount(chosen)==2,"four native drowning hits preserve every one of the thirteen named physical inputs and grant no aura");
            try(var rng=new InputRandom(f,bound->0,true)) { tick();f.check(rng.bounds.equals(List.of(primals.size())),"depleted current uses only one unweighted distinct-primal draw, with no BASE chance"); }
            f.check(witness.isRemoved() && node.getAspects().getAmount(chosen)==3 && node.getNodeVisBase(chosen)==3
                            && witness.seenCounts.equals(List.of(0,1,2,3,4)) && witness.calls==5 && witness.unpaidPearlRefused
                            && peer.getAspects().getAmount(chosen)==2 && peer.getTickCount()==5,
                    "actual WHOLE-stack native death buys ONE current unit; damage runs BEFORE increment and neither a second actual hungry node, a nested tick nor unpaid pearl can debit/credit that in-flight source again");
            var paid=node.getAspects();ticks(5);f.check(node.getAspects().aspects.equals(paid.aspects) && node.getNodeVisBase(chosen)==3,"dead source is never credited again");put(pos.east(),Blocks.AIR);
            fresh(stock(3));var dirt=named(Items.DIRT,64,"hungry-one-item-chance-not-stack");
            f.check(!dirt.has(DataComponents.CONTAINER),"ordinary named dirt starts without a synthetic container component");
            f.check(ObjectAspectResolver.get(dirt.copyWithCount(1),world).getAmount(Aspect.EARTH)==2,"real dirt carries original Earth2 per ONE item");
            var dirtPrimals=DeconstructionRules.primals(ObjectAspectResolver.scanTags(dirt.copyWithCount(1),world));
            f.check(dirtPrimals.size()==1 && dirtPrimals.getAmount(Aspect.EARTH)==2,"actual production scan/SUM path has exactly one Earth2 primal before consuming dirt; actual="+dirtPrimals.aspects);
            var failed=item(dirt,Vec3.atCenterOf(pos),4);
            f.check(failed.saveWithoutId(new net.minecraft.nbt.CompoundTag()).getShort("Health")==1 && !failed.isRemoved()
                            && world.getBlockEntity(pos)==node && !node.isRemoved() && node.getTickCount()==0
                            && node.getAspects().getAmount(Aspect.EARTH)==3 && node.getNodeVisBase(Aspect.EARTH)==3,
                    "full-BASE fixture starts with a current zero-tick node and an actual one-health item; nodeRemoved="+node.isRemoved()+", count="+node.getTickCount()+", current="+node.getAspects().aspects+", base="+node.getAspectsBase().aspects);
            try(var rng=new InputRandom(f,bound->bound==7?2:0,true)) { tick();f.check(rng.bounds.equals(List.of(1,7)),"full BASE3 chance uses original nextInt(1+3*2), not stackcount weighting; actual bounds="+rng.bounds+", effects="+rng.effects+", itemRemoved="+failed.isRemoved()+", current="+node.getAspects().aspects+", base="+node.getAspectsBase().aspects); }
            f.check(failed.isRemoved() && failed.getItem()==dirt && dirt.getCount()==64
                            && ItemContainerContents.EMPTY.equals(dirt.get(DataComponents.CONTAINER))
                            && node.getNodeVisBase(Aspect.EARTH)==3 && node.getAspects().getAmount(Aspect.EARTH)==3,
                    "actual64-item stack is consumed through native BlockItem.onDestroyed adding EMPTY container; draw2 is NOT below one-itemEarth2 and grows no aura");
            var won=item(named(Items.DIRT,9,"hungry-base-growth-input"),Vec3.atCenterOf(pos),4);
            try(var rng=new InputRandom(f,bound->bound==7?1:0,true)) { tick(); }
            f.check(won.isRemoved() && node.getNodeVisBase(Aspect.EARTH)==4 && node.getAspects().getAmount(Aspect.EARTH)==3,"winning draw1 raises only BASE byONE; nine physical items do not refill current or multiply the reward");
            var saved=node.saveWithoutMetadata(world.registryAccess());var detached=new TileNode(ThaumcraftContent.NODE_BLOCK_ENTITY.get(),pos,node.getBlockState());detached.loadWithComponents(saved,world.registryAccess());
            f.check(detached.getNodeType()==TileNode.NodeType.HUNGRY && detached.getNodeVisBase(Aspect.EARTH)==4 && detached.getAspects().getAmount(Aspect.EARTH)==3,"actual current/base/type ownership survives native NBT independently");
            nativeBlockItemDeath();
            var oldTags=ThaumcraftApi.objectTags;var oldGroups=ThaumcraftApi.groupedObjectTags;
            try {
                ThaumcraftApi.objectTags=new ConcurrentHashMap<>(oldTags);ThaumcraftApi.groupedObjectTags=new ConcurrentHashMap<>(oldGroups);
                ThaumcraftApi.registerObjectTag(new ItemStack(Items.DIRT),new int[]{7,0},new AspectList().add(Aspect.FIRE,2));
                fresh(stock(3));node.takeVis("ignis",1);var grouped=item(named(Items.DIRT,4,"hungry-original-grouped-scan-input"),Vec3.atCenterOf(pos),4);
                try(var rng=new InputRandom(f,bound->0,true)) { tick(); }
                f.check(grouped.isRemoved() && node.getAspects().getAmount(Aspect.FIRE)==3 && node.getAspects().getAmount(Aspect.EARTH)==3
                                && ObjectAspectResolver.get(new ItemStack(Items.DIRT),world).getAmount(Aspect.EARTH)==2,
                        "Hungry original ScanManager path honors grouped canonical FIRE2 while ordinary real dirt dissolution stays EARTH2");
            } finally { ThaumcraftApi.objectTags=oldTags;ThaumcraftApi.groupedObjectTags=oldGroups; }
            var refused=new RefusingItem(world,Vec3.atCenterOf(pos),named(Items.DIRT,6,"native-damage-refusal-retains-stock"));admit(refused);
            var beforeRefusal=node.getAspects();var baseBeforeRefusal=node.getAspectsBase();tick();
            f.check(refused.isAlive() && !refused.isRemoved() && refused.getItem().getCount()==6 && node.getAspects().aspects.equals(beforeRefusal.aspects)
                            && node.getAspectsBase().aspects.equals(baseBeforeRefusal.aspects),"an actual native damage refusal keeps all six physical items and creates no current/BASE aura");refused.discard();
            var chicken=EntityType.CHICKEN.create(world);f.check(chicken!=null,"actual native chicken input factory");chicken.setNoAi(true);chicken.setNoGravity(true);chicken.setPos(Vec3.atCenterOf(pos));chicken.setHealth(1F);admit(chicken);
            var entityTags=thaumcraft.common.lib.research.EntityAspectBindings.describe(chicken);
            f.check(entityTags!=null,"native chicken uses its original public entity-tag row");var entityPrimals=DeconstructionRules.primals(entityTags.aspects());Aspect fed=entityPrimals.getAspects()[0];
            fresh(stock(3));node.takeVis(fed.getTag(),1);
            try(var rng=new InputRandom(f,bound->0,true)) { tick(); }
            f.check(!chicken.isAlive() && node.getAspects().getAmount(fed)==3 && node.getNodeVisBase(fed)==3,
                    "a real native living-entity death uses original entity scan tags/SUM and refills ONE current primal");chicken.discard();clearDrops();

        }
        void nativeBlockItemDeath() {
            fresh(stock(3));node.takeVis("terra",1);
            var contents=named(Items.IRON_INGOT,7,"hungry-native-container-seven-physical-iron");
            var expectedContents=contents.copy();
            var packed=named(Items.DIRT,2,"hungry-native-container-parent-block");
            packed.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(contents)));
            var source=item(packed,Vec3.atCenterOf(pos),4);
            try(var rng=new InputRandom(f,bound->0,true)) {
                tick();
                f.check(rng.bounds.equals(List.of(1)),"one paid parent block death draws only its original single Earth primal; native child contents get no extra same-tick meal");
            }
            var children=nearDrops().stream().filter(drop->ItemStack.isSameItemSameComponents(drop.getItem(),expectedContents)).toList();
            f.check(source.isRemoved() && source.getItem()==packed && packed.getCount()==2
                            && ItemContainerContents.EMPTY.equals(packed.get(DataComponents.CONTAINER))
                            && children.size()==1 && ItemStack.matches(children.getFirst().getItem(),expectedContents)
                            && node.getAspects().getAmount(Aspect.EARTH)==3 && node.getNodeVisBase(Aspect.EARTH)==3
                            && node.getAspects().getAmount(Aspect.ORDER)==3 && node.getNodeVisBase(Aspect.ORDER)==3,
                    "native nonempty CONTAINER clears once, both parent blocks pay one Earth refill, and exactly seven named physical iron ingots survive separately without an extra aura reward");
            children.forEach(ItemEntity::discard);
            for(int mutation=0;mutation<3;mutation++) {
                fresh(stock(3));node.takeVis("terra",1);
                var changed=new ChangingDeathItem(world,Vec3.atCenterOf(pos),named(Items.DIRT,2,"hungry-death-custody-"+mutation),mutation);
                admit(changed);f.check(changed.hurt(world.damageSources().drown(),4F),"adversarial native source starts at one real item health");changed.armed=true;
                var before=node.getAspects();var base=node.getAspectsBase();
                try(var rng=new InputRandom(f,bound->0,true)) {
                    tick();
                    f.check(changed.isRemoved() && changed.changed && rng.bounds.isEmpty()
                                    && node.getAspects().aspects.equals(before.aspects) && node.getAspectsBase().aspects.equals(base.aspects),
                            "a native paid death callback changing another component, count or stack identity refuses all aura and every meal RNG draw; mutation="+mutation);
                }
            }
        }
        Pig pig(Vec3 at,boolean invulnerable) { var pig=EntityType.PIG.create(world);f.check(pig!=null,"native pig fixture factory");pig.setNoAi(true);pig.setNoGravity(true);pig.setPos(at);pig.setInvulnerable(invulnerable);return admit(pig); }
        void pullAndFlags() {
            fresh(stock(3));Vec3 centre=Vec3.atCenterOf(pos);var side=pig(centre.add(7.5,0,0),false);var vertical=pig(centre.add(0,7.5,0),false);var beyond=pig(centre.add(15.01,0,0),false);
            tick();f.check(Math.abs(side.getDeltaMovement().x+.0375D)<1e-10 && Math.abs(vertical.getDeltaMovement().y+.0625D)<1e-10
                            && beyond.getDeltaMovement().equals(Vec3.ZERO),"actual native entities use15-block spherical falloff with .15horizontal/.25vertical, without line-of-sight gating");
            side.discard();vertical.discard();beyond.discard();
            var immune=pig(centre.add(1,0,0),true);float hp=immune.getHealth();tick();
            f.check(immune.getHealth()==hp && immune.getDeltaMovement().x<0,"an invulnerable non-player refuses damage but still feels original pull");immune.discard();
            f.mode(GameType.CREATIVE);f.player.setPos(centre.add(1,0,0));f.player.setDeltaMovement(Vec3.ZERO);hp=f.player.getHealth();tick();
            f.check(f.player.getHealth()==hp && f.player.getDeltaMovement().equals(Vec3.ZERO),"actual creative abilities skip BOTH damage and pull");
            f.mode(GameType.SURVIVAL);f.player.setInvulnerable(false);f.player.invulnerableTime=0;f.player.setDeltaMovement(Vec3.ZERO);tick();
            f.check(f.player.getHealth()==hp-1F && f.player.getDeltaMovement().x<0,"actual survival actor takes native drowning1 and is pulled inside squared-distance2; health="+f.player.getHealth()+", before="+hp+", motion="+f.player.getDeltaMovement()+", abilitiesInvulnerable="+f.player.getAbilities().invulnerable+", nativeTicks="+f.player.tickCount);f.player.setInvulnerable(true);f.mode(GameType.CREATIVE);f.aim(centre);
            var disabled=item(named(Items.DIRT,3,"hard-disabled-surviving-input"),centre,4);hard(false);tick();
            f.check(!disabled.isRemoved() && disabled.getDeltaMovement().equals(Vec3.ZERO),"live hardNodes=false disables BOTH physical input damage and attraction, granting no reward");disabled.discard();
            hard(true);put(pos.below(),ThaumcraftNodeDevices.NODE_STABILIZER.get());fresh(stock(3));var locked=pig(centre.add(7.5,0,0),false);tick();locked.setDeltaMovement(Vec3.ZERO);tick();
            f.check(node.getLock()==1 && Math.abs(locked.getDeltaMovement().x+.0375D)<1e-10,"actual basic lock1 does not suppress Hungry First on later ticks");locked.discard();hard(false);
        }
        void ray(int dx,int dy,int dz) { ray(dx,dy,dz,false); }
        void ray(int dx,int dy,int dz,boolean fadingRepair) {
            phase49();int[] draws={Math.max(0,dx),Math.max(0,-dx),Math.max(0,dy),Math.max(0,-dy),Math.max(0,dz),Math.max(0,-dz)};int[] cursor={0};
            try(var rng=new InputRandom(f,bound->{if(fadingRepair && bound==12500 && cursor[0]==0)return 0;f.check(bound==16 && cursor[0]<6,"Hungry Second reads exactly its six original bound16 offsets");return draws[cursor[0]++];},true)) {
                tick();var expected=new ArrayList<Integer>();if(fadingRepair)expected.add(12500);for(int n=0;n<6;n++)expected.add(16);
                f.check(rng.bounds.equals(expected),"original stability precedes the one Second ray, outside hardNodes/lock gates");
            }
        }
        List<ItemEntity> nearDrops() { var result=world.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(18),entity->!oldEntities.contains(entity.getUUID()));for(var drop:result)if(!entities.contains(drop))entities.add(drop);return result; }
        void clearDrops() { nearDrops().forEach(ItemEntity::discard); }
        void blocks() {
            hard(false);put(pos.below(),Blocks.AIR);fresh(stock(3));f.check(node.getLock()==0,"fresh fixture has no cached lock before ordinary polling");
            put(pos.east(2),Blocks.STONE);ray(2,0,0);
            f.check(world.getBlockState(pos.east(2)).isAir() && nearDrops().stream().filter(e->e.getItem().is(Items.COBBLESTONE)).mapToInt(e->e.getItem().getCount()).sum()==1,"native50-tick Second destroys its first real stone hit and emits exactly one real loot item while hardNodes=false");clearDrops();
            put(pos.east(2),Blocks.IRON_BLOCK);f.check(world.getBlockState(pos.east(2)).getDestroySpeed(world,pos.east(2))==5,"actual vanilla boundary block hardness5");ray(2,0,0);
            f.check(world.getBlockState(pos.east(2)).is(Blocks.IRON_BLOCK),"exact hardness5 is refused");put(pos.east(2),Blocks.BEDROCK);ray(2,0,0);f.check(world.getBlockState(pos.east(2)).is(Blocks.BEDROCK),"negative hardness is refused");
            put(pos.east(2),Blocks.OAK_SAPLING);ray(2,0,0);f.check(world.getBlockState(pos.east(2)).isAir(),"zero hardness and a non-collision outline remain valid original ray targets");clearDrops();
            put(pos.east(3),Blocks.STONE);put(pos.east(),Blocks.WATER);ray(3,0,0);
            f.check(world.getBlockState(pos.east(3)).is(Blocks.STONE),"original stopOnLiquid source water shields the block behind it");
            world.setBlock(pos.east(),Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL,1),TubeVariantsRuntimeChecks.QUIET);ray(3,0,0);
            f.check(world.getBlockState(pos.east(3)).isAir(),"non-source flowing water is skipped by the same native ray rather than shielding every fluid");put(pos.east(),Blocks.AIR);clearDrops();
            put(pos.offset(12,0,12),Blocks.STONE);ray(12,0,12);f.check(world.getBlockState(pos.offset(12,0,12)).is(Blocks.STONE),"first hit outside original strict16-block destruction sphere survives even though offset components are each valid");put(pos.offset(12,0,12),Blocks.AIR);
            put(pos.below(),ThaumcraftNodeDevices.NODE_STABILIZER.get());ray(0,-1,0,true);
            f.check(node.getLock()==1 && world.getBlockState(pos.below()).isAir(),"Second can eat the actual unpowered stabilizer AFTER this tick's lock1 sample, outside both gates");clearDrops();
            put(pos.east(2),Blocks.CHEST);var chest=(ChestBlockEntity)world.getBlockEntity(pos.east(2));var contents=named(Items.IRON_INGOT,7,"hungry-destroyed-chest-stock");var expectedContents=contents.copy();chest.setItem(0,contents);ray(2,0,0);
            f.check(world.getBlockState(pos.east(2)).isAir() && chest.isRemoved() && nearDrops().stream().filter(e->ItemStack.isSameItemSameComponents(e.getItem(),expectedContents)).mapToInt(e->e.getItem().getCount()).sum()==7
                            && nearDrops().stream().filter(e->e.getItem().is(Items.CHEST)).mapToInt(e->e.getItem().getCount()).sum()==1,"actual native destruction preserves all seven named container inputs and emits one chest, rather than synthesizing only the block loot");clearDrops();
            f.check(HungryNodeTrace.hit(world,pos,pos)==null,"source-ignoring native ray does not hit or temporarily remove its actual node block");
        }
        void pearlAndHarvest() {
            for(BlockPos mutable:BlockPos.betweenClosed(pos.offset(-4,-4,-4),pos.offset(4,4,4))) {
                BlockPos at=mutable.immutable();boolean shell=Math.abs(at.getX()-pos.getX())==4 || Math.abs(at.getY()-pos.getY())==4 || Math.abs(at.getZ()-pos.getZ())==4;
                put(at,shell?Blocks.BEDROCK:Blocks.AIR);
            }
            put(pos.below(),ThaumcraftNodeDevices.NODE_STABILIZER.get());fresh(stock(3));hard(false);tick();
            int count=node.getTickCount(),regeneration=node.getRegenerationTicks();byte lock=node.getLock();String identity=node.getScanId();
            f.mode(GameType.CREATIVE);f.aim(Vec3.atCenterOf(pos));var pearl=named(ThaumcraftContent.PRIMORDIAL_PEARL.get(),1,"hungry-actual-paid-pearl");f.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,pearl);
            try(var rng=new InputRandom(f,bound->0,true)) { f.check(f.use(pos).consumesAction(),"actual native pearl item accepts a HUNGRY ordinary node"); }
            f.check(pearl.isEmpty() && f.player.getMainHandItem().isEmpty() && world.getBlockEntity(pos)==node && node.getNodeType()==TileNode.NodeType.HUNGRY
                            && node.getScanId().equals(identity) && node.getTickCount()==count && node.getLock()==lock && node.getRegenerationTicks()==regeneration,
                    "one physical Pearl changes staged capacities while preserving HUNGRY type, scan identity, transient phase, cached lock and regeneration through the real blast");clearDrops();
            for(TileNode.NodeType type:List.of(TileNode.NodeType.UNSTABLE,TileNode.NodeType.HUNGRY)) {
                put(pos,ThaumcraftContent.AURA_NODE.get());node=(TileNode)world.getBlockEntity(pos);node.initializeNatural(new AspectList().add(Aspect.AIR,25),type,TileNode.NodeModifier.FADING);
                f.aim(Vec3.atCenterOf(pos));f.check(f.player.gameMode.destroyBlock(pos),"actual native creative block break succeeds for supported "+type);DeferredItemDrops.tick(world);
                var physical=nearDrops();int countEssence=physical.stream().filter(e->e.getItem().is(ThaumcraftEssences.WISP_ESSENCE.get())).mapToInt(e->e.getItem().getCount()).sum();
                f.check(world.getBlockState(pos).isAir() && countEssence==3 && physical.stream().filter(e->e.getItem().is(ThaumcraftEssences.WISP_ESSENCE.get()))
                                .allMatch(e->ThaumcraftEssences.WISP_ESSENCE.get().getAspects(e.getItem()).getAmount(Aspect.AIR)==2),
                        "supported "+type+" player harvest converts actual25 CURRENT into exactly original three AIR2 essences, even in creative");clearDrops();
            }
        }
        void naturalAndOrdinaryTicks() {
            // A single literal original roll, never search/retry/reroll the real world.
            BlockPos natural=pos.above(22);put(natural,Blocks.AIR);
            f.check(ThaumcraftNormalNodeFeature.createRandomNodeAt(world,natural,RandomSource.create(466)),"one real original HUNGRY roll is admitted only now that its ecology exists");
            TileNode actual=(TileNode)world.getBlockEntity(natural);
            f.check(actual.isNatural() && actual.getNodeType()==TileNode.NodeType.HUNGRY && actual.getAspects().getAmount(Aspect.HUNGER)>=2
                            && actual.getAspects().aspects.equals(actual.getAspectsBase().aspects),"natural HUNGER2/minimum tag and independently initialized actual current/BASE are present");
            put(pos.below(),Blocks.AIR);fresh(stock(3));node.takeVis("terra",1);hard(true);f.mode(GameType.CREATIVE);f.aim(Vec3.atCenterOf(pos));
            var paid=item(named(Items.DIRT,5,"ordinary-hungry-native-input"),Vec3.atCenterOf(pos),0);
            test.runAfterDelay(6,()->{
                try { f.check(paid.isRemoved() && node.getTickCount()>=6 && node.getAspects().getAmount(Aspect.EARTH)==3 && node.getNodeVisBase(Aspect.EARTH)==3
                                    && actual.getTickCount()>0,"ordinary registered native ticking destroys the real five-item source through health5 and refills ONE current unit without manual ticks");test.succeed(); }
                finally { close(); }
            });
        }
        @Override public void close() {
            if(closed)return;closed=true;ThaumcraftSettings.configureServer(settings);for(Entity entity:entities)if(!entity.isRemoved())entity.discard();
            try { f.close(); } finally { for(ChunkPos chunk:forced)world.setChunkForced(chunk.x,chunk.z,false); }
        }
    }
    private static final class RefusingItem extends ItemEntity {
        RefusingItem(ServerLevel world,Vec3 at,ItemStack actual) { super(world,at.x,at.y,at.z,actual,0,0,0);setNoGravity(true);setNeverPickUp(); }
        @Override public boolean hurt(DamageSource source,float amount) { return false; }
    }
    private static final class ChangingDeathItem extends ItemEntity {
        final int mutation;boolean armed,changed;
        ChangingDeathItem(ServerLevel world,Vec3 at,ItemStack actual,int mutation) { super(world,at.x,at.y,at.z,actual,0,0,0);this.mutation=mutation;setNoGravity(true);setNeverPickUp(); }
        @Override public boolean hurt(DamageSource source,float amount) {
            boolean result=super.hurt(source,amount);
            if(armed && isRemoved()) {
                changed=true;
                switch(mutation) {
                    case 0 -> getItem().set(DataComponents.CUSTOM_NAME,Component.literal("changed-after-native-container-clear"));
                    case 1 -> getItem().shrink(1);
                    case 2 -> setItem(getItem().copy());
                    default -> throw new AssertionError(mutation);
                }
            }
            return result;
        }
    }
    private static final class WitnessItem extends ItemEntity {
        final TileNode node,peer;final List<Integer> seenCounts=new ArrayList<>();int calls;boolean unpaidPearlRefused=true;
        WitnessItem(ServerLevel world,Vec3 at,ItemStack stack,TileNode node,TileNode peer) { super(world,at.x,at.y,at.z,stack,0,0,0);this.node=node;this.peer=peer;setNoGravity(true);setNeverPickUp(); }
        @Override public boolean hurt(DamageSource source,float amount) {
            calls++;seenCounts.add(node.getTickCount());int before=node.getTickCount();
            unpaidPearlRefused &= node.preparePrimalPearl(RandomSource.create(0),false)==null;
            TileNode.serverTick(level(),node.getBlockPos(),node.getBlockState(),node);
            TileNode.serverTick(level(),peer.getBlockPos(),peer.getBlockState(),peer);
            if(node.getTickCount()!=before)throw new AssertionError("A native damage callback reentered node tick");
            return super.hurt(source,amount);
        }
    }
}
