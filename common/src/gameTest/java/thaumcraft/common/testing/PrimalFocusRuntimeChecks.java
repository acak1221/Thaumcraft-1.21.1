package thaumcraft.common.testing;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.config.ThaumcraftPrimalFoci;
import thaumcraft.common.config.ThaumcraftTaint;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.entities.projectile.EntityPrimalOrb;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.foci.ItemFocusPrimal;
import thaumcraft.common.items.wands.foci.WandFocusActions;
import thaumcraft.common.lib.world.BiomeColumns;
import thaumcraft.common.lib.world.NodeGeneration;
import thaumcraft.common.tiles.TileNode;

/** Declared focus/vis/rare RNG inputs; actual wand dispatch, native entity physics/collision, explosion and terrain outcomes. */
public final class PrimalFocusRuntimeChecks {
    private PrimalFocusRuntimeChecks(){}
    public static void actualFocusPaymentAndSeeker(GameTestHelper test){
        try(var a=new FocusUpgradeRuntimeChecks.Actor(test,new BlockPos(8,220,8))){
            a.clearLane();var item=ThaumcraftPrimalFoci.FOCUS_PRIMAL.get();
            var recipe=(ArcaneShapedRecipe)a.level.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft","arcane/focus_primal")).orElseThrow().value();
            a.require(recipe.research().equals("FOCUSPRIMAL")&&recipe.pattern().equals(List.of("CQC","Q#Q","CQC"))&&recipe.key().get("C").test(new ItemStack(Items.DIAMOND))
                    &&recipe.key().get("Q").test(new ItemStack(Items.QUARTZ))&&recipe.key().get("#").test(new ItemStack(thaumcraft.common.config.ThaumcraftContent.MATERIALS.get("primal_charm").get()))
                    &&recipe.result().is(item)&&recipe.vis().size()==6&&recipe.vis().values().stream().allMatch(v->v==25),
                    "real original four-diamond/quartz/PrimalCharm arcane recipe and six25 vis costs are registered");
            ItemStack focus=a.focus(item,FocusUpgradeType.frugal,1,FocusUpgradeType.frugal,2,FocusUpgradeType.seeker,3,FocusUpgradeType.frugal,4,FocusUpgradeType.frugal,5);
            a.equip(focus,"gold",1000);long before=System.currentTimeMillis();a.begin();long after=System.currentTimeMillis();
            var orbs=a.projectiles(EntityPrimalOrb.class);a.require(orbs.size()==1,"actual native wand RMB creates exactly one distinct Primal Orb");var orb=orbs.getFirst();
            a.require(orb.getOwner()==a.player&&orb.isSeeker()&&orb.originalCasterId()==a.player.getId()&&orb.getAge()==0
                    &&Math.abs(orb.getDeltaMovement().length()-.5)<.03&&!a.player.isUsingItem()&&WandFocusActions.cooldownMillis(a.player)>0,
                    "genuine cast retains owner, spawn-only Seeker/original ID, one .5 launch and500ms WAVE dispatcher");
            boolean matched=false;
            for(long bucket=before/200;bucket<=after/200;bucket++){
                var cost=ItemFocusPrimal.activationCost(FocusUpgradeRuntimeChecks.Actor.charged(focus,"gold",1000),a.player,bucket*200);
                boolean same=true;for(String tag:WandData.PRIMALS)if(a.vis(tag)!=1000-cost.get(tag))same=false;matched|=same;
            }
            a.require(matched,"all six actual debits belong to one original200ms quotation with actual Frugal4 FLOAT prices");
            var paid=a.wand.copy();a.player.gameMode.useItem(a.player,a.level,a.wand,net.minecraft.world.InteractionHand.MAIN_HAND);
            a.require(a.projectiles(EntityPrimalOrb.class).size()==1&&ItemStack.matches(paid,a.wand),"actual native cooldown replay creates no second orb or debit");
            orb.discard();a.equip(focus,"gold",0);var empty=a.wand.copy();a.begin();a.require(a.projectiles(EntityPrimalOrb.class).isEmpty()&&ItemStack.matches(empty,a.wand),"unfunded six-aspect cast is atomic and creates no projectile");
            seeker(a);
        }
        test.succeed();
    }
    private static void seeker(FocusUpgradeRuntimeChecks.Actor a){
        var cow=EntityType.COW.create(a.level);a.require(cow!=null,"native homing target exists");
        var orb=new EntityPrimalOrb(a.level,a.player,true);orb.setPos(Vec3.atCenterOf(a.root));orb.setDeltaMovement(Vec3.ZERO);
        cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(Vec3.atCenterOf(a.root.east(4)));a.put(a.root.east(2),Blocks.STONE.defaultBlockState());a.put(a.root.east(2).above(),Blocks.STONE.defaultBlockState());
        a.require(a.level.addFreshEntity(cow)&&a.level.addFreshEntity(orb),"real target and orb enter the native world index");
        try{
            for(int i=0;i<20;i++)orb.tick();a.require(!orb.isRemoved()&&orb.getAge()==20&&orb.getDeltaMovement().x==0,"first20 actual projectile updates retain no Seeker steering");
            a.require(a.level.clip(new net.minecraft.world.level.ClipContext(orb.position(),cow.getEyePosition(),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,orb)).getType()==net.minecraft.world.phys.HitResult.Type.BLOCK,
                    "actual native voxel line of sight is obstructed before the genuine homing update");
            Vec3 old=orb.getDeltaMovement();double distance=orb.distanceToSqr(cow);
            double x=(double)net.minecraft.util.Mth.clamp((float)(old.x+(cow.getX()-orb.getX())/distance*.2D),-.2F,.2F);
            double y=(double)net.minecraft.util.Mth.clamp((float)(old.y+(cow.getBoundingBox().minY+cow.getBbHeight()*.9D-orb.getY())/distance*.2D),-.2F,.2F);
            orb.tick();a.require(!orb.isRemoved()&&orb.getAge()==21&&orb.getDeltaMovement().x==x*(double).99F&&orb.getDeltaMovement().y==y*(double).99F-(double).001F,
                    "actual21st tick steers through a solid LOS wall using nearest living target, squared distance, FLOAT clamp, drag and .001F gravity");
        }finally{orb.discard();cow.discard();}
    }

    /** Separate isolated batch. Biome palettes and owned terrain are restored through native section APIs. */
    public static void actualRareImpactNodeAndTaint(GameTestHelper test){
        try(var a=new FocusUpgradeRuntimeChecks.Actor(test,new BlockPos(8,220,8));var terrain=new Terrain(a)){
            for(boolean taint:new boolean[]{false,true}){
                terrain.prepare();var cow=EntityType.COW.create(a.level);a.require(cow!=null,"native explosion victim exists");cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(Vec3.atCenterOf(a.root.south()));a.level.addFreshEntity(cow);
                var orb=new EntityPrimalOrb(a.level);orb.setPos(Vec3.atCenterOf(a.root));orb.setDeltaMovement(1,0,0);long seed=specialSeed(taint);orb.getRandom().setSeed(seed);
                BlockPos impact=a.root.east();a.put(impact,Blocks.GLASS.defaultBlockState());a.require(a.level.addFreshEntity(orb),"declared rare-branch orb enters actual collision world");
                try{
                    orb.tick();a.require(orb.isRemoved()&&cow.getHealth()<cow.getMaxHealth(),"genuine block collision performs its null-source destructive radius2 explosion and terminal removal");
                    if(!taint){
                        a.require(a.level.getBlockEntity(impact) instanceof TileNode,"actual rare NODE branch places a live aura node at the real block-hit coordinates");
                        var node=(TileNode)a.level.getBlockEntity(impact);var expectedRandom=RandomSource.create(seed);a.require(expectedRandom.nextInt(100)<=1&&!expectedRandom.nextBoolean(),"declared seed genuinely selects original non-Seeker NODE outcome");
                        var expected=NodeGeneration.plan(BiomeColumns.surfaceHolder(a.level,impact.getX(),impact.getZ()),impact,expectedRandom,false,false,true,p->a.level.getBlockState(p));
                        a.require(node.getNodeType()==expected.type()&&node.getNodeModifier()==expected.modifier()&&node.getAspects().aspects.equals(expected.aspects().aspects),
                                "actual small-node creation uses the remaining original RNG stream, all-type initializer and quarter-aura plan");
                    }else{
                        int fibres=0;for(BlockPos pos:BlockPos.betweenClosed(a.root.offset(-6,-4,-6),a.root.offset(1,1,1)))if(a.level.getBlockState(pos).is(ThaumcraftTaint.FIBRES.get())){
                            fibres++;a.require(BiomeColumns.surfaceHolder(a.level,pos.getX(),pos.getZ()).is(ThaumcraftBiomes.TAINT),"actual planted fibres belong to the genuinely converted taint biome column");
                        }
                        a.require(fibres>0,"actual rare TAINT branch replaces native biome columns and plants real supported metadata0 fibres");
                    }
                }finally{orb.discard();cow.discard();}
            }
        }
        test.succeed();
    }
    private static long specialSeed(boolean taint){
        for(long seed=0;seed<100000;seed++){
            var rng=RandomSource.create(seed);if(rng.nextInt(100)>1||rng.nextBoolean()!=taint)continue;
            if(!taint)return seed;
            for(int attempt=0;attempt<10;attempt++){rng.nextFloat();rng.nextFloat();rng.nextFloat();rng.nextFloat();if(rng.nextBoolean())return seed;}
        }
        throw new AssertionError("No declared original rare branch seed");
    }
    private static final class Terrain implements AutoCloseable {
        final FocusUpgradeRuntimeChecks.Actor a;final Map<BlockPos,BlockState> states=new LinkedHashMap<>();final Map<BlockPos,net.minecraft.nbt.CompoundTag> tiles=new LinkedHashMap<>();final Map<LevelChunk,List<byte[]>> biomes=new LinkedHashMap<>();
        Terrain(FocusUpgradeRuntimeChecks.Actor a){
            this.a=a;
            for(BlockPos mutable:BlockPos.betweenClosed(a.root.offset(-8,-5,-8),a.root.offset(8,5,8))){
                var pos=mutable.immutable();states.put(pos,a.level.getBlockState(pos));var chunk=a.level.getChunkAt(pos);
                if(a.level.getBlockEntity(pos)!=null)tiles.put(pos,a.level.getBlockEntity(pos).saveWithFullMetadata(a.level.registryAccess()));
                if(!biomes.containsKey(chunk)){
                    var sections=new ArrayList<byte[]>();for(var section:chunk.getSections()){
                        var buf=new FriendlyByteBuf(Unpooled.buffer());try{section.getBiomes().write(buf);byte[] data=new byte[buf.readableBytes()];buf.readBytes(data);sections.add(data);}finally{buf.release();}
                    }
                    biomes.put(chunk,sections);
                }
            }
        }
        void prepare(){
            for(var pos:states.keySet()){a.level.removeBlockEntity(pos);a.put(pos,pos.getY()<a.root.getY()&&pos.getY()>=a.root.getY()-3?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());}
            for(int x=-6;x<=1;x++)for(int z=-6;z<=1;z++)BiomeColumns.set(a.level,a.root.getX()+x,a.root.getZ()+z,Biomes.PLAINS);
        }
        @Override public void close(){
            for(var entry:biomes.entrySet()){
                var chunk=entry.getKey();for(int i=0;i<chunk.getSections().length;i++){
                    var buf=new FriendlyByteBuf(Unpooled.wrappedBuffer(entry.getValue().get(i)));try{chunk.getSections()[i].readBiomes(buf);}finally{buf.release();}
                }
                chunk.setUnsaved(true);
            }
            a.level.getChunkSource().chunkMap.resendBiomesForChunks(new ArrayList<>(biomes.keySet()));
            for(var pos:states.keySet())a.level.removeBlockEntity(pos);states.forEach(a::put);
            tiles.forEach((pos,tag)->{if(a.level.getBlockEntity(pos)!=null)a.level.getBlockEntity(pos).loadWithComponents(tag,a.level.registryAccess());});
        }
    }
}
