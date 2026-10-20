package thaumcraft.common.testing;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftNodeDevices;
import thaumcraft.common.entities.EntityAspectOrb;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.utils.AspectOrbQueue;
import thaumcraft.common.lib.world.ThaumcraftNormalNodeFeature;
import thaumcraft.common.tiles.TileNode;

/** Actual natural roll/scan, one paid source unit, canceled spawn and saved custody, wand pickup and native expiry. */
public final class UnstableNodeRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private UnstableNodeRuntimeChecks() {}
    public static void run(GameTestHelper test) {
        var check = new Check(test);
        try { check.prepare(); } catch (RuntimeException | Error failure) { check.finish(); throw failure; }
        test.runAfterDelay(5, () -> { try { check.naturalScan(); } catch (RuntimeException | Error failure) { check.finish(); throw failure; } });
    }
    private static final class Check {
        final GameTestHelper test; final ServerLevel level; final ServerPlayer player; final BlockPos base, controlled;
        final Map<BlockPos, BlockState> original = new LinkedHashMap<>(); final List<ChunkPos> forced = new ArrayList<>();
        final List<EntityAspectOrb> fixtures = new ArrayList<>();
        TileNode natural, node; ItemStack scanner; UUID paidId; CompoundTag paidRow;
        EntityEvent.Add observer; boolean trajectorySeen, finished; int attempts;
        Check(GameTestHelper test) { this.test = test; level = test.getLevel(); player = test.makeMockServerPlayerInLevel(); base = test.absolutePos(new BlockPos(3, 100, 3)); controlled = base.above(8); }
        void set(BlockPos pos, BlockState state) { original.putIfAbsent(pos, level.getBlockState(pos)); level.setBlock(pos, state, QUIET); }
        void prepare() {
            forced.addAll(GolemChunks.hold(level, List.of(base.offset(-3,-3,-3), base.offset(3,3,3))));
            for (BlockPos pos : BlockPos.betweenClosed(base.offset(-3,-3,-3), base.offset(3,3,3))) set(pos.immutable(), Blocks.AIR.defaultBlockState());
            set(controlled, Blocks.AIR.defaultBlockState()); set(controlled.below(), Blocks.AIR.defaultBlockState());
            player.getInventory().clearContent(); player.setNoGravity(true);
        }
        void naturalScan() {
            // ONE original roll, no retries. For the native plains adapter this
            // literal seed rolls UNSTABLE and a single AIR tag, not creative25x6.
            test.assertTrue(ThaumcraftNormalNodeFeature.createRandomNodeAt(level, base, RandomSource.create(88)), "a genuinely rolled UNSTABLE node now reaches the native world instead of being skipped");
            natural = (TileNode) level.getBlockEntity(base);
            test.assertTrue(natural.isNatural() && natural.getNodeType() == TileNode.NodeType.UNSTABLE && natural.getAspects().getAmount(Aspect.ENTROPY) == 0
                            && natural.getAspects().aspects.equals(natural.getAspectsBase().aspects), "actual generated current/base/type replace the creative fixture; Entropy scan bonus is not already in stock");
            for (Aspect aspect : Aspect.aspects.values()) { KnowledgeAccess.get(player).addDiscoveredAspect(aspect.getTag()); KnowledgeAccess.get(player).setAspectPool(aspect.getTag(), 0); }
            player.setPos(base.getX()+.5,base.getY(),base.getZ()-1.5); player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(base));
            scanner = new ItemStack(ThaumcraftContent.THAUMOMETER.get()); player.setItemInHand(InteractionHand.MAIN_HAND, scanner);
            var before = natural.getAspects().copy(); scanner.getItem().use(level, player, InteractionHand.MAIN_HAND);
            test.assertTrue(scanner.has(ThaumcraftContent.SCAN_SESSION.get()), "a real thaumometer use observes the actual natural unstable target");
            test.runAfterDelay(21, () -> {
                try {
                    scanner.getItem().onUseTick(level, player, scanner, 4);
                    test.assertTrue(KnowledgeAccess.get(player).hasScannedPhenomena("@NODE"+natural.getScanId()) && KnowledgeAccess.get(player).getAspectPoolFor("perditio") == 4
                                    && natural.getAspects().aspects.equals(before.aspects), "actual timed scan adds original EntropyMAX4 without draining any natural aura");
                    player.stopUsingItem(); player.getInventory().clearContent(); emissionAndCancellation();
                } catch (RuntimeException | Error failure) { finish(); throw failure; }
            });
        }
        void nodeTicks(int count) { for(int tick=0;tick<count;tick++) TileNode.serverTick(level,controlled,level.getBlockState(controlled),node); }
        void boundary() { while(node.getTickCount()%100!=99) nodeTicks(1); }
        long emissionSeed(int choice) {
            for(long seed=0;seed<100000;seed++) { var random=RandomSource.create(seed); if(random.nextBoolean() && random.nextInt(2)==choice) return seed; }
            throw new AssertionError("Missing explicit emission branch seed");
        }
        void emissionAndCancellation() {
            set(controlled,ThaumcraftContent.AURA_NODE.get().defaultBlockState()); node=(TileNode)level.getBlockEntity(controlled);
            node.initializeNatural(new AspectList().add(Aspect.AIR,3).add(Aspect.FIRE,0).add(Aspect.METAL,7),TileNode.NodeType.UNSTABLE,TileNode.NodeModifier.FADING);
            boundary(); var before=node.getAspects().copy(); level.random.setSeed(emissionSeed(1)); nodeTicks(1);
            test.assertTrue(node.getAspects().aspects.equals(before.aspects) && node.getTickCount()==100, "selecting a retained zero primal spends nothing; compounds do not replace that failed draw");
            boundary(); level.random.setSeed(emissionSeed(0)); nodeTicks(1);
            test.assertTrue(node.getAspects().getAmount(Aspect.AIR)==2 && node.getNodeVisBase(Aspect.AIR)==3 && node.getAspects().getAmount(Aspect.FIRE)==0
                            && node.getAspects().getAmount(Aspect.METAL)==7, "one native unstable update debits exactly ONE current primal, retains zero keys and leaves base/compounds unchanged");
            var external=AspectOrbQueue.prepare(level,controlled,Aspect.AIR);
            test.assertTrue(external!=null && !external.commit(node), "a public prepared queue entry has no unpaid node-debit authority");
            var saved=node.saveWithoutMetadata(level.registryAccess()); var detached=new TileNode(ThaumcraftContent.NODE_BLOCK_ENTITY.get(),controlled,node.getBlockState()); detached.loadWithComponents(saved,level.registryAccess());
            test.assertTrue(detached.getNodeType()==TileNode.NodeType.UNSTABLE && detached.getAspects().getAmount(Aspect.AIR)==2 && detached.getNodeVisBase(Aspect.AIR)==3, "actual unstable type, current debit and separate capacity survive NBT");
            CompoundTag snapshot=AspectOrbQueue.snapshot(level);
            for(Tag value:snapshot.getList("Pending",Tag.TAG_COMPOUND)) {
                CompoundTag row=(CompoundTag)value;
                if(row.getDouble("X")==controlled.getX()+.5 && row.getDouble("Y")==controlled.getY()+.5 && row.getDouble("Z")==controlled.getZ()+.5) {
                    test.assertTrue(paidRow==null,"exactly one paid pending emission exists at the source"); paidRow=row.copy(); paidId=row.getUUID("Id");
                }
            }
            test.assertTrue(paidRow!=null && paidRow.getString("Aspect").equals("aer") && paidRow.getInt("Value")==1,"the durable owner holds one actual AIR unit");
            var duplicate = snapshot.copy(); var duplicatedRows = new ListTag(); duplicatedRows.add(paidRow.copy()); duplicatedRows.add(paidRow.copy());
            duplicate.put("Pending", duplicatedRows); duplicate.put("Settled", new ListTag()); duplicate.put("Rejected", new ListTag());
            var rejectedDuplicate = AspectOrbQueue.load(duplicate, level.registryAccess()).save(new CompoundTag(), level.registryAccess());
            test.assertTrue(rejectedDuplicate.getList("Pending",Tag.TAG_COMPOUND).isEmpty() && rejectedDuplicate.getList("Rejected",Tag.TAG_COMPOUND).size()==2,
                    "ambiguous duplicate paid UUID rows preserve BOTH raw payloads and cannot issue even the first orb");
            var malformed = duplicate.copy(); var badRows = new ListTag(); var bad = paidRow.copy(); bad.putDouble("MotionX",Double.NaN); badRows.add(bad); malformed.put("Pending",badRows);
            var rejectedMalformed = AspectOrbQueue.load(malformed,level.registryAccess()).save(new CompoundTag(),level.registryAccess());
            test.assertTrue(rejectedMalformed.getList("Pending",Tag.TAG_COMPOUND).isEmpty() && rejectedMalformed.getList("Rejected",Tag.TAG_COMPOUND).getCompound(0).equals(bad),
                    "non-finite persisted trajectory is preserved verbatim and never becomes a physical orb");
            EntityEvent.Add cancel=(entity,world)->{
                if(world!=level || !entity.getUUID().equals(paidId)) return EventResult.pass();
                attempts++; AspectOrbQueue.tick(level); return EventResult.interruptFalse();
            };
            EntityEvent.ADD.register(cancel);
            try { AspectOrbQueue.tick(level); } finally { EntityEvent.ADD.unregister(cancel); }
            test.assertTrue(attempts==1 && level.getEntity(paidId)==null && node.getAspects().getAmount(Aspect.AIR)==2 && hasPending(paidId), "actual native ADD cancellation and recursive flush retain the one paid pending owner without another debit");
            var savedQueue=AspectOrbQueue.snapshot(level); level.getDataStorage().set(AspectOrbQueue.ID,AspectOrbQueue.load(savedQueue,level.registryAccess()));
            test.assertTrue(hasPending(paidId),"the canceled actual emission survives a real SavedData encode/decode/storage replacement");
            set(controlled,Blocks.AIR.defaultBlockState());
            observer=(entity,world)->{
                if(world==level && entity.getUUID().equals(paidId) && entity instanceof EntityAspectOrb orb) {
                    trajectorySeen=orb.position().equals(new Vec3(paidRow.getDouble("X"),paidRow.getDouble("Y"),paidRow.getDouble("Z")))
                            && orb.getDeltaMovement().equals(new Vec3(paidRow.getDouble("MotionX"),paidRow.getDouble("MotionY"),paidRow.getDouble("MotionZ"))) && orb.getYRot()==paidRow.getFloat("Yaw");
                    AspectOrbQueue.tick(level);
                }
                return EventResult.pass();
            };
            EntityEvent.ADD.register(observer);
            test.runAfterDelay(21,()->{
                try { deliveryAndPickup(); stabilization(); lifetime(); }
                catch(RuntimeException|Error failure) { finish(); throw failure; }
            });
        }
        boolean hasPending(UUID id) { for(Tag value:AspectOrbQueue.snapshot(level).getList("Pending",Tag.TAG_COMPOUND)) if(((CompoundTag)value).getUUID("Id").equals(id)) return true; return false; }
        ItemStack wand(int air) { var wand=new ItemStack(ThaumcraftContent.WAND.get()); wand.set(ThaumcraftContent.WAND_DATA.get(),WandData.EMPTY.withAmount("aer",air)); return wand; }
        void deliveryAndPickup() {
            EntityEvent.ADD.unregister(observer); observer=null;
            test.assertTrue(trajectorySeen && !hasPending(paidId) && level.getEntity(paidId) instanceof EntityAspectOrb,"ordinary POST delivers the saved exact original position/motion after the source block is gone");
            var orb=(EntityAspectOrb)level.getEntity(paidId); fixtures.add(orb); player.setPos(orb.position()); player.takeXpDelay=0;
            orb.playerTouch(player); test.assertTrue(!orb.isConsumed() && orb.getAspectValue()==1,"without an eligible wand the actual orb grants no vis or RP");
            player.getInventory().setItem(9,wand(0)); orb.playerTouch(player);
            test.assertTrue(!orb.isConsumed(),"a wand outside the hotbar cannot collect an orb");
            player.getInventory().setItem(0,wand(2500)); var first=wand(0); first.set(DataComponents.CUSTOM_NAME,Component.literal("paid-orb-first-wand")); player.getInventory().setItem(1,first); player.getInventory().setItem(2,wand(0));
            var knowledge=KnowledgeAccess.get(player).save(); orb.playerTouch(player);
            test.assertTrue(orb.isConsumed() && orb.isRemoved() && player.getInventory().getItem(1)==first && ItemWandCasting.getVis(first,"aer")==100
                            && ItemWandCasting.getVis(player.getInventory().getItem(0),"aer")==2500 && ItemWandCasting.getVis(player.getInventory().getItem(2),"aer")==0
                            && first.getHoverName().getString().equals("paid-orb-first-wand") && KnowledgeAccess.get(player).save().equals(knowledge) && player.takeXpDelay==2,
                    "the first eligible actual hotbar wand receives100centivis once; its identity/components and all RP are preserved");
            orb.playerTouch(player); test.assertTrue(ItemWandCasting.getVis(first,"aer")==100,"replaying pickup cannot credit the paid orb again");
            var partial=fixtureOrb(player.position(),Aspect.AIR); first.set(ThaumcraftContent.WAND_DATA.get(),WandData.EMPTY.withAmount("aer",2499)); partial.playerTouch(player);
            test.assertTrue(!partial.isConsumed() && ItemWandCasting.getVis(first,"aer")==2499,"the original shared player pickup delay protects the next actual orb");
            player.takeXpDelay=0; partial.playerTouch(player);
            test.assertTrue(partial.isConsumed() && ItemWandCasting.getVis(first,"aer")==2500,"one centivis of room accepts the original whole orb and clamps the remaining99, preserving TC4 rounding");
            // Replay ONLY this old pending row, preserving every other live row.
            var replay=AspectOrbQueue.snapshot(level); var pending=replay.getList("Pending",Tag.TAG_COMPOUND); pending.add(paidRow.copy()); replay.put("Pending",pending);
            var receipts=replay.getList("Settled",Tag.TAG_COMPOUND); receipts.removeIf(value->((CompoundTag)value).getUUID("Id").equals(paidId)); replay.put("Settled",receipts);
            level.getDataStorage().set(AspectOrbQueue.ID,AspectOrbQueue.load(replay,level.registryAccess())); AspectOrbQueue.tick(level);
            test.assertTrue(!hasPending(paidId) && level.getEntity(paidId)==null && ItemWandCasting.getVis(first,"aer")==2500,"a stale saved pending row cannot reissue an already consumed UUID in the same native server lifetime");
            player.getInventory().clearContent(); player.setPos(Vec3.atCenterOf(base).add(20,0,0));
        }
        EntityAspectOrb fixtureOrb(Vec3 where,Aspect aspect) { var orb=EntityAspectOrb.unspawned(level,where,Vec3.ZERO,0,aspect,1); test.assertTrue(level.addFreshEntity(orb),"declared one-unit physical orb fixture is admitted natively"); fixtures.add(orb); return orb; }
        void stabilization() {
            set(controlled.below(),ThaumcraftNodeDevices.NODE_STABILIZER.get().defaultBlockState()); set(controlled,ThaumcraftContent.AURA_NODE.get().defaultBlockState()); node=(TileNode)level.getBlockEntity(controlled);
            node.initializeNatural(new AspectList().add(Aspect.AIR,3),TileNode.NodeType.UNSTABLE,TileNode.NodeModifier.FADING); boundary();
            long seed=0; boolean found=false;
            for(;seed<1000000;seed++) { var random=RandomSource.create(seed); if(random.nextBoolean() && random.nextInt(10000)==42 && random.nextInt(12500)!=69) { found=true; break; } }
            test.assertTrue(found && node.getLock()==1,"a real basic stabilizer supplies the original lock1"); level.random.setSeed(seed); nodeTicks(1);
            test.assertTrue(node.getNodeType()==TileNode.NodeType.NORMAL && node.getAspects().getAmount(Aspect.AIR)==3 && node.getNodeVisBase(Aspect.AIR)==3
                            && node.getNodeModifier()==TileNode.NodeModifier.FADING && node.getRegenerationTicks()==0,"the actual locked nextInt10000==42 changes UNSTABLE→NORMAL without an orb, free vis or regeneration-cache reset");
        }
        void lifetime() {
            // Native entity selection + force, not only the shared vector helper.
            Vec3 attractionPoint=Vec3.atCenterOf(controlled.above(4)); player.getInventory().setItem(0,wand(0));
            player.setPos(attractionPoint.x+4,attractionPoint.y-player.getEyeHeight(),attractionPoint.z);
            var attracted=fixtureOrb(attractionPoint,Aspect.AIR); attracted.tick();
            test.assertTrue(Math.abs(attracted.getX()-(attractionPoint.x+.025))<1e-8 && Math.abs(attracted.getDeltaMovement().x-.025*.98F)<1e-8,
                    "the actual native orb finds a hotbar wand at four blocks and applies the original squared8-block eye-distance pull"); attracted.discard();
            player.setPos(attractionPoint.x+8.01,attractionPoint.y-player.getEyeHeight(),attractionPoint.z);
            var beyond=fixtureOrb(attractionPoint,Aspect.AIR); beyond.tick();
            test.assertTrue(beyond.getX()==attractionPoint.x && beyond.getDeltaMovement().x==0,
                    "a real player just beyond8 eye-blocks supplies no attraction even if its bounding box intersects the search cube"); beyond.discard();
            player.getInventory().clearContent(); player.setPos(Vec3.atCenterOf(base).add(20,0,0));
            var air=fixtureOrb(Vec3.atCenterOf(controlled.above(4)),Aspect.FIRE); double y=air.getY(); air.tick();
            test.assertTrue(Math.abs(air.getY()-(y-(double).03F))<1e-8 && Math.abs(air.getDeltaMovement().y-(-(double).03F*.98F))<1e-8 && air.getOrbAge()==1,"a real native orb applies the exact original gravity/move/friction order"); air.discard();
            CompoundTag physical=new CompoundTag(); air.saveWithoutId(physical);
            var restored=EntityAspectOrb.unspawned(level,Vec3.atCenterOf(controlled.above(4)),Vec3.ZERO,0,Aspect.FIRE,1); restored.load(physical);
            test.assertTrue(!restored.isQuarantined() && restored.getOrbAge()==1 && restored.getAspect()==Aspect.FIRE && restored.getAspectValue()==1,
                    "actual native entity NBT preserves age, positive value and aspect independently of the pending queue");
            var unknown=physical.copy(); unknown.putString("Aspect","unknown_aspect_orb_payload"); restored.load(unknown); var quarantined=new CompoundTag(); restored.saveWithoutId(quarantined);
            test.assertTrue(restored.isQuarantined() && restored.getAspectValue()==0 && quarantined.getCompound("RejectedAspectOrb").equals(unknown),
                    "unknown physical-orb payload retains its original native NBT and cannot grant wand vis"); restored.discard();
            var hurt=fixtureOrb(Vec3.atCenterOf(controlled.above(4)),Aspect.FIRE);
            test.assertTrue(!hurt.hurt(level.damageSources().generic(),4F) && !hurt.isRemoved() && !hurt.hurt(level.damageSources().generic(),1F) && hurt.isRemoved(),"original health5 and false damage return survive the real native entity path");
            BlockPos floor=controlled.above(5); set(floor,Blocks.STONE.defaultBlockState()); var expires=fixtureOrb(new Vec3(floor.getX()+.5,floor.getY()+1,floor.getZ()+.5),Aspect.FIRE);
            for(int tick=0;tick<149;tick++) expires.tick();
            test.assertTrue(expires.getOrbAge()==149 && !expires.isRemoved() && !expires.isConsumed(),"149 real native orb ticks preserve the unclaimed original unit");
            int before=natural.getTickCount(); test.runAfterDelay(2,()->{
                try { test.assertTrue(expires.isRemoved() && !expires.isConsumed() && natural.getTickCount()>before,"ordinary registered entity ticking expires at150 and the natural UNSTABLE block ticker advances without manual calls"); test.succeed(); }
                finally { finish(); }
            });
        }
        void finish() {
            if(finished)return; finished=true; if(observer!=null)EntityEvent.ADD.unregister(observer); player.stopUsingItem(); player.getInventory().clearContent();
            for(var orb:fixtures)if(!orb.isRemoved())orb.discard();
            if(paidId!=null && level.getEntity(paidId) instanceof EntityAspectOrb orb)orb.discard();
            original.forEach((pos,state)->level.setBlock(pos,state,QUIET)); for(ChunkPos chunk:forced)level.setChunkForced(chunk.x,chunk.z,false);
        }
    }
}
