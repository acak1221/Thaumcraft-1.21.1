package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.entities.EntityAspectOrb;
import thaumcraft.common.items.ItemPrimalCharm;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.utils.AspectOrbQueue;

/** Original inventory-only free rolls, exact RNG, authoritative clue and durable native-orb delivery. */
public final class PrimalCharmRuntimeChecks {
    private PrimalCharmRuntimeChecks() {}
    public static void run(GameTestHelper test) {
        Check check=new Check(test);
        try {check.prepare();}catch(RuntimeException|Error failure){check.finish();throw failure;}
        test.runAfterDelay(5,()->{try {check.inventoryRolls();check.clue();check.cancelAndReload();}catch(RuntimeException|Error failure){check.finish();throw failure;}});
    }
    private static final class Check {
        final GameTestHelper test;final ServerLevel level;final ServerPlayer player;final BlockPos base,chestPos;
        final Connection connection=new Connection(PacketFlow.SERVERBOUND);final EmbeddedChannel channel=new EmbeddedChannel(connection);
        final List<ChunkPos> forced=new ArrayList<>();final Set<UUID> oldEntities=new HashSet<>(),delivered=new HashSet<>();
        final Map<UUID,CompoundTag> owned=new HashMap<>();EntityEvent.Add observer;ItemPrimalCharm item;ItemStack charm;ItemEntity dropped;ChestBlockEntity chest;BlockState oldChest;
        boolean finished;int attempts;
        Check(GameTestHelper test) {
            this.test=test;level=test.getLevel();base=test.absolutePos(new BlockPos(3,100,3));chestPos=base.above(5);
            player=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"charm-"+UUID.randomUUID().toString().substring(0,8)),ClientInformation.createDefault());
        }
        void prepare() {
            level.getServer().getPlayerList().placeNewPlayer(connection,player,CommonListenerCookie.createInitial(player.getGameProfile(),false));player.setGameMode(GameType.SURVIVAL);player.setNoGravity(true);player.getInventory().clearContent();player.setPos(Vec3.atCenterOf(base));
            forced.addAll(GolemChunks.hold(level,List.of(base.offset(-3,-3,-3),base.offset(3,8,3))));level.getAllEntities().forEach(e->oldEntities.add(e.getUUID()));
            test.assertTrue(ThaumcraftContent.MATERIALS.get("primal_charm").get() instanceof ItemPrimalCharm,"the actual existing crafted material now registers its native passive item class");item=(ItemPrimalCharm)ThaumcraftContent.MATERIALS.get("primal_charm").get();charm=new ItemStack(item);charm.set(DataComponents.CUSTOM_NAME,Component.literal("real-kept-primal-charm"));
        }
        Map<UUID,CompoundTag> rows() {
            Map<UUID,CompoundTag> result=new HashMap<>();for(Tag raw:AspectOrbQueue.snapshot(level).getList("Pending",Tag.TAG_COMPOUND)){var row=(CompoundTag)raw;result.put(row.getUUID("Id"),row.copy());}return result;
        }
        void legacyBlurb(ItemStack stack) {var data=new CompoundTag();data.putString("blurb","obsolete");data.putInt("keep",73);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));}
        void inventoryRolls() {
            player.getInventory().setItem(9,charm);legacyBlurb(charm);var before=rows();level.random.setSeed(0);player.getInventory().tick();
            test.assertTrue(level.random.nextInt(1000)==948&&rows().keySet().equals(before.keySet()),"an ordinary idle roll1360 consumes exactly nextInt20000 and no primal draw");
            test.assertTrue(!charm.get(DataComponents.CUSTOM_DATA).contains("blurb")&&charm.get(DataComponents.CUSTOM_DATA).copyTag().getInt("keep")==73,"server tick clears only obsolete blurb even when no orb/clue was rolled");
            long[] seeds={11908,5781,1221,1695,3270,3736};int[] tails={402,369,369,323,244,846};Aspect[] aspects={Aspect.AIR,Aspect.EARTH,Aspect.FIRE,Aspect.WATER,Aspect.ORDER,Aspect.ENTROPY};
            for(int index=0;index<6;index++) {
                before=rows();level.random.setSeed(seeds[index]);player.getInventory().tick();var after=rows();after.keySet().removeAll(before.keySet());
                test.assertTrue(after.size()==1&&level.random.nextInt(1000)==tails[index],"literal original seed rolls one uniform primal and preserves world-RNG tail "+index);
                var row=after.values().iterator().next();owned.put(row.getUUID("Id"),row);
                test.assertTrue(row.getString("Aspect").equals(aspects[index].getTag())&&row.getInt("Value")==1&&new Vec3(row.getDouble("X"),row.getDouble("Y"),row.getDouble("Z")).equals(player.position()),"one original free primal unit starts at exact holder FEET "+index);
                test.assertTrue(player.getInventory().getItem(9)==charm&&charm.getCount()==1&&charm.getHoverName().getString().equals("real-kept-primal-charm"),"the original passive lottery never consumes/replaces its actual named charm");
            }
            test.assertTrue(!AspectOrbQueue.issuePrimalCharm(level,player,charm,9,Aspect.AIR),"public queue endpoint cannot forge a native inventory-roll authorization");
            // Modern compartments use local indexes; only original MAIN36 is eligible.
            player.getInventory().setItem(9,ItemStack.EMPTY);player.setItemInHand(InteractionHand.OFF_HAND,charm);level.random.setSeed(11908);player.getInventory().tick();
            test.assertTrue(level.random.nextInt(20000)==15,"modern offhand does not invent an original main-inventory chance");player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            player.getInventory().armor.set(0,charm);level.random.setSeed(11908);player.getInventory().tick();test.assertTrue(level.random.nextInt(20000)==15,"original armor onArmorTick never ran ItemResource.onUpdate");player.getInventory().armor.set(0,ItemStack.EMPTY);player.getInventory().setItem(35,charm);
        }
        void clue() {
            var knowledge=KnowledgeAccess.get(player);knowledge.removeResearch("FOCUSPRIMAL");knowledge.removeResearch("@FOCUSPRIMAL");var before=knowledge.copy();
            level.random.setSeed(10342);player.getInventory().tick();test.assertTrue(level.random.nextInt(1000)==877&&knowledge.isResearchComplete("@FOCUSPRIMAL")&&!knowledge.isResearchComplete("FOCUSPRIMAL"),"exact roll42 grants only original clue marker, with no primal RNG draw");
            knowledge.removeResearch("@FOCUSPRIMAL");test.assertTrue(knowledge.save().equals(before.save()),"clue changes no RP, discoveries, scans, warp or full research");knowledge.addResearch("@FOCUSPRIMAL");before=knowledge.copy();level.random.setSeed(10342);player.getInventory().tick();test.assertTrue(knowledge.save().equals(before.save())&&level.random.nextInt(1000)==877,"an existing clue suppresses duplicate reward but still consumes one ordinary world roll");
            knowledge.removeResearch("@FOCUSPRIMAL");knowledge.addResearch("FOCUSPRIMAL");before=knowledge.copy();level.random.setSeed(10342);player.getInventory().tick();test.assertTrue(knowledge.save().equals(before.save())&&!knowledge.isResearchComplete("@FOCUSPRIMAL"),"completed main focus suppresses its clue independently");
            player.setGameMode(GameType.CREATIVE);knowledge.removeResearch("FOCUSPRIMAL");level.random.setSeed(10342);player.getInventory().tick();test.assertTrue(knowledge.isResearchComplete("@FOCUSPRIMAL")&&charm.getCount()==1,"original passive clue has no invented survival-only condition");player.setGameMode(GameType.SURVIVAL);player.getInventory().clearContent();
        }
        void cancelAndReload() {
            observer=(entity,world)->{if(world!=level||!owned.containsKey(entity.getUUID()))return EventResult.pass();attempts++;AspectOrbQueue.tick(level);return EventResult.interruptFalse();};EntityEvent.ADD.register(observer);try {AspectOrbQueue.tick(level);}finally{EntityEvent.ADD.unregister(observer);observer=null;}
            test.assertTrue(attempts==6&&rows().keySet().containsAll(owned.keySet())&&owned.keySet().stream().allMatch(id->level.getEntity(id)==null),"six actual canceled ADD attempts retain six issued free units, without another charm roll");
            var saved=AspectOrbQueue.snapshot(level);level.getDataStorage().set(AspectOrbQueue.ID,AspectOrbQueue.load(saved,level.registryAccess()));
            test.assertTrue(rows().keySet().containsAll(owned.keySet()),"actual saved queue encode/decode/storage replacement retains all six original free emissions");
            var physical=charm.copy();legacyBlurb(physical);dropped=new ItemEntity(level,base.getX()+2.5,base.getY()+.5,base.getZ()+.5,physical,0,0,0);dropped.setNoGravity(true);dropped.setPickUpDelay(32767);test.assertTrue(level.addFreshEntity(dropped),"declared actual dropped charm fixture is admitted");
            oldChest=level.getBlockState(chestPos);level.setBlock(chestPos,Blocks.CHEST.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);chest=(ChestBlockEntity)level.getBlockEntity(chestPos);var stored=charm.copy();legacyBlurb(stored);chest.setItem(0,stored);player.setPos(Vec3.atCenterOf(base).add(20,0,0));
            level.random.setSeed(11908);dropped.tick();test.assertTrue(level.random.nextInt(20000)==15&&physical.get(DataComponents.CUSTOM_DATA).contains("blurb"),"actual dropped native tick does not call ItemResource inventory passive or consume its world draw");
            observer=(entity,world)->{
                var row=owned.get(entity.getUUID());if(world!=level||row==null)return EventResult.pass();
                test.assertTrue(entity instanceof EntityAspectOrb orb&&orb.getAspectValue()==1&&orb.getAspect().getTag().equals(row.getString("Aspect"))&&entity.position().equals(new Vec3(row.getDouble("X"),row.getDouble("Y"),row.getDouble("Z")))&&entity.getDeltaMovement().equals(new Vec3(row.getDouble("MotionX"),row.getDouble("MotionY"),row.getDouble("MotionZ")))&&entity.getYRot()==row.getFloat("Yaw")&&delivered.add(entity.getUUID()),"actual retried orb preserves exact saved primal/value/feet/scatter/yaw once");AspectOrbQueue.tick(level);return EventResult.pass();
            };EntityEvent.ADD.register(observer);
            test.runAfterDelay(22,()->{try {
                test.assertTrue(delivered.size()==6&&owned.keySet().stream().noneMatch(rows()::containsKey),"ordinary POST admits six exact saved orbs after original charm left inventory");
                test.assertTrue(dropped.getItem().get(DataComponents.CUSTOM_DATA).contains("blurb")&&chest.getItem(0).get(DataComponents.CUSTOM_DATA).contains("blurb"),"ordinary world ticks leave actual dropped/stored charm passive state untouched");
                legacyBlurb(charm);player.getInventory().setItem(17,charm);
                test.runAfterDelay(2,()->{try {test.assertTrue(!charm.get(DataComponents.CUSTOM_DATA).contains("blurb")&&player.getInventory().getItem(17)==charm&&charm.getCount()==1,"ordinary registered world/player/inventory ticks reach an unselected main36 charm without manual item ticking");finish();test.succeed();}catch(RuntimeException|Error failure){finish();throw failure;}});
            }catch(RuntimeException|Error failure){finish();throw failure;}});
        }
        void finish() {
            if(finished)return;finished=true;if(observer!=null)EntityEvent.ADD.unregister(observer);player.getInventory().clearContent();
            if(dropped!=null)dropped.discard();if(chest!=null){chest.clearContent();level.setBlock(chestPos,oldChest,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);}
            for(var orb:level.getEntitiesOfClass(EntityAspectOrb.class,new AABB(base).inflate(30)))if(!oldEntities.contains(orb.getUUID()))orb.discard();
            var saved=AspectOrbQueue.snapshot(level);var pending=saved.getList("Pending",Tag.TAG_COMPOUND);
            pending.removeIf(raw->{var row=(CompoundTag)raw;var pos=new Vec3(row.getDouble("X"),row.getDouble("Y"),row.getDouble("Z"));return owned.containsKey(row.getUUID("Id"))||pos.equals(Vec3.atCenterOf(base))||pos.equals(player.position());});saved.put("Pending",pending);
            if(saved.contains("Version"))level.getDataStorage().set(AspectOrbQueue.ID,AspectOrbQueue.load(saved,level.registryAccess()));
            for(ChunkPos chunk:forced)level.setChunkForced(chunk.x,chunk.z,false);
            try{if(level.getServer().getPlayerList().getPlayer(player.getUUID())==player)level.getServer().getPlayerList().remove(player);}finally{player.discard();channel.finishAndReleaseAll();}
        }
    }
}
