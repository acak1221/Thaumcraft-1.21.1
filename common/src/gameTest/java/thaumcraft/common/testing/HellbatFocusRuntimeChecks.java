package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.common.config.ThaumcraftFocalManipulation;
import thaumcraft.common.config.ThaumcraftHellbatFoci;
import thaumcraft.common.config.ThaumcraftMonsters;
import thaumcraft.common.container.ContainerFocalManipulator;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.entities.monster.EntityFireBat;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.ItemFocusHellbat;
import thaumcraft.common.items.wands.foci.WandFocusActions;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.tiles.TileFocalManipulator;
import thaumcraft.common.tiles.TileNode;

/** Native input fixtures, not natural acquisition/client acceptance. Root owns
 * wrapper/Gradle; actual actor/RMB, target ray, ADD and paid focal flow are used. */
public final class HellbatFocusRuntimeChecks {
    private HellbatFocusRuntimeChecks() {}
    public static void run(GameTestHelper test) {
        try(var a=new FocusUpgradeRuntimeChecks.Actor(test,new BlockPos(4,220,4))) {
            a.clearLane();recipeAndUpgradeApi(a);castAndTargeting(a);
        }
        focalResearchGate(test);test.succeed();
    }
    private static ItemStack upgraded(FocusUpgradeRuntimeChecks.Actor a,boolean devil) {
        return a.focus(ThaumcraftHellbatFoci.HELLBAT_FOCUS.get(),FocusUpgradeType.frugal,1,FocusUpgradeType.potency,2,
                devil?FocusUpgradeType.devilbats:FocusUpgradeType.batbombs,3,FocusUpgradeType.potency,4,FocusUpgradeType.vampirebats,5);
    }
    private static void recipeAndUpgradeApi(FocusUpgradeRuntimeChecks.Actor a) {
        var item=ThaumcraftHellbatFoci.HELLBAT_FOCUS.get();
        var recipe=(InfusionStackRecipe)a.level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft","infusion/focus_hellbat")).orElseThrow().value();
        a.require(recipe.research().equals("FOCUSHELLBAT")&&recipe.instability()==3&&recipe.input().test(new ItemStack(Items.MAGMA_CREAM))
                        &&recipe.aspects().equals(Map.of("ignis",25,"aer",15,"bestia",15,"perditio",25))
                        &&recipe.result().is(item)&&recipe.result().getCount()==1,"real Hellbat infusion retains original magma cream/research/aspects/instability/output");
        List<ItemStack> components=List.of(new ItemStack(Items.QUARTZ),new ItemStack(thaumcraft.common.config.ThaumcraftContent.SHARDS.get("ignis").get()),
                new ItemStack(Items.QUARTZ),new ItemStack(thaumcraft.common.config.ThaumcraftContent.SHARDS.get("aer").get()),
                new ItemStack(Items.QUARTZ),new ItemStack(thaumcraft.common.config.ThaumcraftContent.SHARDS.get("perditio").get()));
        a.require(recipe.components().size()==6,"Hellbat has six original physical infusion components");
        for(int i=0;i<components.size();i++)a.require(recipe.components().get(i).test(components.get(i)),"original ordered Hellbat component "+i);
        ItemStack focus=new ItemStack(item);KnowledgeAccess.get(a.player).removeResearch("VAMPBAT");
        a.require(!item.canApplyUpgrade(focus,a.player,FocusUpgradeType.vampirebats,5),"actual unresearched actor cannot buy Vampire rank5");
        boolean threw=false;try{item.canApplyUpgrade(focus,null,FocusUpgradeType.vampirebats,5);}catch(NullPointerException expected){threw=true;}
        a.require(threw,"original explicit Vampire canApplyUpgrade query requires a nonnull actor");
        a.require(item.applyUpgrade(focus,FocusUpgradeType.vampirebats,5)&&FocusData.isSupportedFocus(focus),"original actor-free applyUpgrade remains a structural API and native codec accepts its actual rank5");
        focus=new ItemStack(item);KnowledgeAccess.get(a.player).addResearch("VAMPBAT");
        a.require(item.canApplyUpgrade(focus,a.player,FocusUpgradeType.vampirebats,5),"actual researched actor passes Vampire prerequisite");
        ItemStack bomb=upgraded(a,false),devil=upgraded(a,true);
        a.require(item.getVisCost(bomb).getAmount(Aspect.FIRE)==100&&item.getVisCost(bomb).getAmount(Aspect.ENTROPY)==200
                        &&item.getVisCost(devil).getAmount(Aspect.FIRE)==100&&item.getVisCost(devil).getAmount(Aspect.EARTH)==100,
                "actual upgraded tooltip costs differ between Bomb and Devil");
        a.equip(bomb,"gold",1000);
        a.require(item.getVisCost(a.wand).getAmount(Aspect.FIRE)==200&&item.getVisCost(a.wand).getAmount(Aspect.ENTROPY)==100
                        &&item.getVisCost(a.wand).size()==3&&ItemFocusHellbat.activationCost(a.wand,a.player).equals(Map.of("ignis",180,"perditio",90,"aer",90)),
                "outer-wand cost quirk uses base200/100/100 with actual Frugal1 float truncation");
    }
    private static void paid(FocusUpgradeRuntimeChecks.Actor a) {
        for(String tag:WandData.PRIMALS)a.require(a.vis(tag)==1000-switch(tag){case "ignis"->180;case "perditio","aer"->90;default->0;},"actual Hellbat debit "+tag);
    }
    private static List<EntityFireBat> summoned(FocusUpgradeRuntimeChecks.Actor a) {
        var out=new ArrayList<EntityFireBat>();a.level.getAllEntities().forEach(e->{if(e instanceof EntityFireBat bat&&bat.getIsSummoned()&&bat.owner==a.player)out.add(bat);});return out;
    }
    private static Cow cow(FocusUpgradeRuntimeChecks.Actor a,Vec3 pos,List<Entity> owned) {
        a.level.getChunk(BlockPos.containing(pos));var cow=EntityType.COW.create(a.level);a.require(cow!=null,"registered target cow exists");
        cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(pos);owned.add(cow);a.require(a.level.addFreshEntity(cow),"native target enters actual world index");return cow;
    }
    private static void clear(List<Entity> owned) { owned.forEach(Entity::discard);owned.clear(); }
    private static void castAndTargeting(FocusUpgradeRuntimeChecks.Actor a) {
        List<Entity> owned=new ArrayList<>();ItemStack bomb=upgraded(a,false),devil=upgraded(a,true);
        try {
            KnowledgeAccess.get(a.player).removeResearch("FOCUSHELLBAT");KnowledgeAccess.get(a.player).removeResearch("VAMPBAT");
            Cow target=cow(a,Vec3.atBottomCenterOf(a.root),owned);a.equip(bomb,"gold",1000);a.aim();
            Vec3 look=a.player.getLookAngle();float yaw=a.player.getYRot()/180F*3.141593F;
            Vec3 expected=new Vec3(a.player.getX()-(double)(Mth.cos(yaw)*.16F)+look.x*.5,
                    a.player.getBoundingBox().minY+a.player.getBbHeight()/2F+.25-.05000000014901161+look.y*.5+.9F,
                    a.player.getZ()-(double)(Mth.sin(yaw)*.16F)+look.z*.5);
            a.begin();var bats=summoned(a);owned.addAll(bats);a.require(bats.size()==1,"actual unresearched found Hellbat focus summons exactly one bat");
            var bat=bats.getFirst();
            a.require(bat.getTarget()==target&&bat.getIsSummoned()&&!bat.getIsBatHanging()&&bat.getIsExplosive()&&!bat.getIsDevil()&&bat.getIsVampire()
                            &&bat.owner==a.player&&bat.damBonus==2&&bat.getAttributeValue(Attributes.ATTACK_DAMAGE)==4&&bat.position().distanceToSqr(expected)<1e-10,
                    "native summon keeps original target, exact muzzle, Potency2, explosion/Vampire flags and real owner");paid(a);
            a.require(!a.player.isUsingItem()&&WandFocusActions.cooldownMillis(a.player)>0,"Hellbat is one WAVE activation with a real1000ms deadline");
            a.player.gameMode.useItem(a.player,a.level,a.wand,InteractionHand.MAIN_HAND);
            a.require(summoned(a).size()==1&&a.vis("ignis")==820,"immediate native cooldown replay creates no bat/payment");
            clear(owned);target=cow(a,Vec3.atBottomCenterOf(a.root),owned);a.equip(devil,"gold",1000);a.begin();bats=summoned(a);owned.addAll(bats);
            a.require(bats.size()==1&&bats.getFirst().getIsDevil()&&!bats.getFirst().getIsExplosive()&&bats.getFirst().getAttributeValue(Attributes.ATTACK_DAMAGE)==5,
                    "actual Devil specialization upgrades summoned native attack damage to3+P without Bomb flag");paid(a);
            clear(owned);
            for(GameType mode:List.of(GameType.SURVIVAL,GameType.CREATIVE)) {
                a.player.setGameMode(mode);target=cow(a,Vec3.atBottomCenterOf(a.root),owned);a.equip(bomb,"gold",1000);
                a.wand.set(thaumcraft.common.config.ThaumcraftContent.WAND_DATA.get(),thaumcraft.common.items.wands.ItemWandCasting.data(a.wand).withAmount("aer",89));
                ItemStack before=a.wand.copy();a.begin();a.require(summoned(a).isEmpty()&&ItemStack.matches(before,a.wand),"one-short "+mode+" Hellbat refuses all three primal debits atomically");clear(owned);
            }
            a.player.setGameMode(GameType.CREATIVE);target=cow(a,Vec3.atBottomCenterOf(a.root),owned);a.equip(bomb,"gold",1000);a.begin();bats=summoned(a);owned.addAll(bats);
            a.require(bats.size()==1,"funded real creative actor actually summons");paid(a);clear(owned);a.player.setGameMode(GameType.SURVIVAL);

            // A physical nonliving target occludes a living one, while Firebat
            // exclusion is part of the original targeting helper itself.
            target=cow(a,Vec3.atBottomCenterOf(a.root),owned);var boat=EntityType.BOAT.create(a.level);a.require(boat!=null,"real nonliving boat exists");
            boat.setPos(Vec3.atBottomCenterOf(a.root.east()));owned.add(boat);a.require(a.level.addFreshEntity(boat),"actual boat enters ray");
            a.equip(bomb,"gold",1000);ItemStack before=a.wand.copy();a.begin();a.require(summoned(a).isEmpty()&&ItemStack.matches(before,a.wand),"nearer nonliving boat blocks summoning and spends nothing");clear(owned);
            target=cow(a,Vec3.atBottomCenterOf(a.root),owned);EntityFireBat excluded=ThaumcraftMonsters.FIREBAT.get().create(a.level);a.require(excluded!=null,"original excluded Firebat exists");
            excluded.setNoAi(true);excluded.setIsBatHanging(false);excluded.setPos(Vec3.atBottomCenterOf(a.root.east()));owned.add(excluded);a.require(a.level.addFreshEntity(excluded),"excluded Firebat enters nearer ray");
            a.equip(bomb,"gold",1000);a.begin();bats=summoned(a);owned.addAll(bats);a.require(bats.size()==1&&bats.getFirst().getTarget()==target,"actual32-block helper excludes existing Firebats rather than letting them occlude the intended target");paid(a);clear(owned);
            target=cow(a,Vec3.atBottomCenterOf(a.root.west(28)),owned);a.equip(bomb,"gold",1000);a.begin();bats=summoned(a);owned.addAll(bats);
            a.require(bats.size()==1&&bats.getFirst().getTarget()==target,"actual Hellbat ray reaches living target31blocks away");paid(a);clear(owned);
            target=cow(a,Vec3.atBottomCenterOf(a.root.west(36)),owned);a.equip(bomb,"gold",1000);before=a.wand.copy();a.begin();
            a.require(summoned(a).isEmpty()&&ItemStack.matches(before,a.wand),"a target beyond32-block ray and pick padding cannot consume or summon");clear(owned);
            target=cow(a,Vec3.atBottomCenterOf(a.root),owned);a.put(a.root.east(2),Blocks.STONE.defaultBlockState());a.equip(bomb,"gold",1000);before=a.wand.copy();a.begin();
            a.require(summoned(a).isEmpty()&&ItemStack.matches(before,a.wand),"original per-candidate eye line of sight refuses through stone for free");clear(owned);a.clearLane();

            target=cow(a,Vec3.atBottomCenterOf(a.root),owned);a.equip(bomb,"gold",1000);int[] attempts={0};
            EntityEvent.Add cancel=(entity,world)->{
                if(world!=a.level||!(entity instanceof EntityFireBat candidate)||candidate.owner!=a.player)return EventResult.pass();
                attempts[0]++;owned.add(candidate);paid(a);
                a.require(!a.player.gameMode.useItem(a.player,a.level,a.wand,InteractionHand.MAIN_HAND).consumesAction(),"native ADD reentrant RMB cannot spend/summon another bat");
                return EventResult.interruptFalse();
            };
            EntityEvent.ADD.register(cancel);try{a.begin();}finally{EntityEvent.ADD.unregister(cancel);}
            a.require(attempts[0]==1&&summoned(a).isEmpty(),"cancelled actual ADD stays one paid attempt without retry/refund");paid(a);clear(owned);
            pvp(a,bomb,owned);
        }finally{clear(owned);a.player.setGameMode(GameType.SURVIVAL);}
    }
    private static void pvp(FocusUpgradeRuntimeChecks.Actor a,ItemStack focus,List<Entity> owned) {
        boolean old=a.level.getServer().isPvpAllowed();
        var connection=new Connection(PacketFlow.SERVERBOUND);var channel=new EmbeddedChannel(connection);
        var profile=new GameProfile(UUID.randomUUID(),"hellbat-"+UUID.randomUUID().toString().substring(0,7));
        var victim=new ServerPlayer(a.level.getServer(),a.level,profile,ClientInformation.createDefault());
        try {
            a.level.getServer().getPlayerList().placeNewPlayer(connection,victim,CommonListenerCookie.createInitial(profile,false));
            victim.setGameMode(GameType.SURVIVAL);victim.setNoGravity(true);victim.setPos(Vec3.atBottomCenterOf(a.root));
            a.level.getServer().setPvpAllowed(false);a.equip(focus,"gold",1000);ItemStack before=a.wand.copy();a.begin();
            a.require(summoned(a).isEmpty()&&ItemStack.matches(before,a.wand),"actual connected player target with server PvP disabled is free and cannot summon");
            a.level.getServer().setPvpAllowed(true);a.equip(focus,"gold",1000);a.begin();var bats=summoned(a);owned.addAll(bats);
            a.require(bats.size()==1&&bats.getFirst().getTarget()==victim,"same actual player target becomes a paid native summon when server PvP allows");paid(a);
        }finally{a.level.getServer().setPvpAllowed(old);a.level.getServer().getPlayerList().remove(victim);victim.discard();channel.finishAndReleaseAll();clear(owned);}
    }
    private static void focalResearchGate(GameTestHelper test) {
        try(var f=NodeTransductionRuntimeChecks.fixture(test,false)) {
            f.open();var item=ThaumcraftHellbatFoci.HELLBAT_FOCUS.get();var focus=new ItemStack(item);
            for(int rank=1;rank<=4;rank++)f.check(item.applyUpgrade(focus,FocusUpgradeType.frugal,rank),"declared structural four-rank input "+rank);
            f.put(f.base,ThaumcraftFocalManipulation.FOCAL_MANIPULATOR.get());var tile=(TileFocalManipulator)f.level.getBlockEntity(f.base);tile.setItem(0,focus);
            KnowledgeAccess.get(f.player).addResearch("FOCALMANIPULATION");KnowledgeAccess.get(f.player).removeResearch("VAMPBAT");
            f.player.experienceLevel=40;f.aim(Vec3.atCenterOf(f.base));tile.openMenu(f.player);
            f.check(f.player.containerMenu instanceof ContainerFocalManipulator,"actual native focal menu/session admits Hellbat input");var menu=(ContainerFocalManipulator)f.player.containerMenu;
            f.check(!menu.requestStart(f.player,menu.sessionId(),menu.selectionRevision(),FocusUpgradeType.vampirebats.id)
                            &&f.player.experienceLevel==40&&tile.getSize()==0&&ItemStack.matches(tile.getItem(0),focus),"actual focal admission refuses missing VAMPBAT before XP/vis/item mutation");
            var base=new AspectList();for(Aspect primal:List.of(Aspect.AIR,Aspect.EARTH,Aspect.FIRE,Aspect.WATER,Aspect.ORDER,Aspect.ENTROPY))base.add(primal,10000);
            var source=NodeTransductionRuntimeChecks.energize(f,f.base.west(4),base,null,TileNode.NodeType.NORMAL);
            KnowledgeAccess.get(f.player).addResearch("VAMPBAT");
            f.check(menu.requestStart(f.player,menu.sessionId(),menu.selectionRevision(),FocusUpgradeType.vampirebats.id)
                            &&f.player.experienceLevel==0&&tile.getRank()==5&&tile.getSize()>0,"actual researched rank5 start pays exactly40 native levels and records real primal debt");
            KnowledgeAccess.get(f.player).removeResearch("VAMPBAT"); // Admission owns the research check; completion's original API is actor-free.
            for(int epoch=0;epoch<256&&tile.getSize()>0;epoch++) {
                NodeTransductionRuntimeChecks.sourceTick(f,source);
                var ticker=tile.getBlockState().getTicker(f.level,ThaumcraftFocalManipulation.FOCAL_MANIPULATOR_BLOCK_ENTITY.get());f.check(ticker!=null,"real focal ticker exists");
                for(int tick=0;tick<5;tick++)ticker.tick(f.level,tile.getBlockPos(),tile.getBlockState(),tile);
            }
            f.check(tile.getSize()==0&&item.getUpgradeLevel(tile.getItem(0),FocusUpgradeType.vampirebats)==1&&f.player.experienceLevel==0,
                    "actual converted source pays the full rank5 debt; actor-free completion applies Vampire once without null-player exception or a second XP charge");
            tile.clearContent();
        }
    }
}
