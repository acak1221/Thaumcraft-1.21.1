package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockManaPod;
import thaumcraft.common.config.BiomeSettings;
import thaumcraft.common.config.ThaumcraftBiomeSettings;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.config.ThaumcraftManaBeans;
import thaumcraft.common.config.ThaumcraftNaturals;
import thaumcraft.common.entities.monster.EntityPech;
import thaumcraft.common.items.ItemManaBean;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.tiles.TileManaPod;

/** Fresh canonical native noise source -> actual forest features -> survival
 * harvest/replant and genuine native weighted natural-spawn callback. No biome
 * rebinding, fixture plant/tree insertion or forced Pech type/spawn predicate.
 * Uses the existing test-only normal-noise dimension, not new smoke machinery. */
public final class NaturalBiomeRuntimeChecks {
    private NaturalBiomeRuntimeChecks() {}
    public static void run(GameTestHelper test) {
        var key=ResourceKey.create(Registries.DIMENSION,ResourceLocation.fromNamespaceAndPath("thaumcraft_gametest","natural"));
        ServerLevel level=test.getLevel().getServer().getLevel(key);
        test.assertTrue(level!=null,"existing test-only natural noise dimension is present");
        test.assertTrue(ThaumcraftBiomeSettings.current().equals(BiomeSettings.DEFAULTS),"declared fresh-world fixture uses original startup biome options");
        var generator=level.getChunkSource().getGenerator();var sampler=level.getChunkSource().randomState().sampler();
        BlockPos centre=null;
        // Pure native source/base-height queries only: no chunk generation
        // until an actual inland forest core at ordinary pod-search height is found.
        search:for(int x=4096;x<=12288;x+=128) {
          candidate:for(int z=4096;z<=12288;z+=128) {
            if(level.getChunkSource().getChunkNow(x>>4,z>>4)!=null)continue;
            if(!generator.getBiomeSource().getNoiseBiome(x>>2,20,z>>2,sampler).is(ThaumcraftBiomes.MAGICAL_FOREST))continue;
            for(int dx:new int[]{-48,0,48})for(int dz:new int[]{-48,0,48})
                if(!generator.getBiomeSource().getNoiseBiome((x+dx)>>2,20,(z+dz)>>2,sampler).is(ThaumcraftBiomes.MAGICAL_FOREST))continue candidate;
            int height=generator.getBaseHeight(x,z,Heightmap.Types.WORLD_SURFACE_WG,level,level.getChunkSource().randomState());
            if(height<65||height>105)continue;
            centre=new BlockPos(x,height,z);break search;
          }
        }
        test.assertTrue(centre!=null,"bounded immutable native biome/terrain lookup locates a real fresh Magical Forest core");
        int minX=(centre.getX()>>4)-2,minZ=(centre.getZ()>>4)-2;
        for(int cx=minX;cx<minX+5;cx++)for(int cz=minZ;cz<minZ+5;cz++)
            test.assertTrue(level.getChunkSource().getChunkNow(cx,cz)==null,"all25 requested forest chunks are fresh before native generation");
        // Finish the whole declared region before observing it: a later
        // neighbor's genuine feature pass may write into an earlier chunk.
        for(int cx=minX;cx<minX+5;cx++)for(int cz=minZ;cz<minZ+5;cz++)level.getChunk(cx,cz);
        int oak=0,magicalLogs=0,vishrooms=0;List<BlockPos> pods=new ArrayList<>(),ground=new ArrayList<>();
        for(int cx=minX;cx<minX+5;cx++)for(int cz=minZ;cz<minZ+5;cz++) {
            var chunk=level.getChunk(cx,cz);
            for(int lx=0;lx<16;lx++)for(int lz=0;lz<16;lz++) {
                int x=cx*16+lx,z=cz*16+lz;
                for(int y=50;y<Math.min(160,level.getMaxBuildHeight());y++) {
                    var pos=new BlockPos(x,y,z);var state=chunk.getBlockState(pos);
                    if(state.is(Blocks.OAK_LOG))oak++;
                    if(state.is(ThaumcraftNaturals.GREATWOOD_LOG.get())||state.is(ThaumcraftNaturals.SILVERWOOD_LOG.get()))magicalLogs++;
                    if(state.is(ThaumcraftBiomes.VISHROOM.get()))vishrooms++;
                    if(state.is(ThaumcraftManaBeans.MANA_POD.get())) {
                        test.assertTrue(level.getBiome(pos).is(ThaumcraftManaBeans.MAGICAL_BIOMES)&&BlockManaPod.isSupport(level.getBlockState(pos.above())),"naturally generated pod has a real MAGICAL biome and original wood support");
                        if(pods.isEmpty()&&state.getValue(BlockManaPod.AGE)<3&&level.getBlockEntity(pos) instanceof TileManaPod stale) {
                            // The case harvests and replants the first pod of the region, and the
                            // original plants the age zero (oracle ItemManaBean:192-193). The GameTest
                            // world keeps that planted pod, so the next run of this case would read it
                            // instead of the generated one. It is grown back with the original growth
                            // rule (oracle TileManaPod.checkGrowth) before the case reads it.
                            while(level.getBlockState(pos).getValue(BlockManaPod.AGE)<3)stale.checkGrowth();
                            state=level.getBlockState(pos);
                        }
                        test.assertTrue(state.getValue(BlockManaPod.AGE)>=3&&state.getValue(BlockManaPod.AGE)<=7&&level.getBlockEntity(pos) instanceof TileManaPod pod&&pod.getAspect()!=null,"region generation admits actual typed BE with original generation-plus-growth age3..7");
                        pods.add(pos);
                    }
                    if(state.is(Blocks.GRASS_BLOCK)&&chunk.getBlockState(pos.above()).isAir()&&chunk.getBlockState(pos.above(2)).isAir()
                            &&level.getBiome(pos.above()).is(ThaumcraftBiomes.MAGICAL_FOREST))ground.add(pos.above());
                }
            }
        }
        Thaumcraft.LOG.info("TC4 fresh forest core {}: oakLogs={}, magicalLogs={}, vishrooms={}, typedPods={}",centre,oak,magicalLogs,vishrooms,pods.size());
        test.assertTrue(oak>0&&magicalLogs>0,"real fresh forest decoration produces actual BigMagic oak and original magical tree species");
        test.assertTrue(vishrooms>0&&!pods.isEmpty(),"real25-chunk forest decoration produces actual Vishrooms and typed ManaPods without fixture insertion");
        test.assertTrue(!ground.isEmpty(),"naturally generated forest contains real supported open ground for native population");
        try(var actor=new FocusUpgradeRuntimeChecks.Actor(test)) {
            try {
                harvestAndReplant(test,level,actor,pods.getFirst());
                naturalPech(test,level,actor,ground);
            } finally {
                actor.player.teleportTo(actor.level,actor.root.getX()+3.5,actor.root.getY()+.5-actor.player.getEyeHeight(),actor.root.getZ()+.5,90,0);
            }
        }
        test.succeed();
    }
    private static void harvestAndReplant(GameTestHelper test,ServerLevel level,FocusUpgradeRuntimeChecks.Actor actor,BlockPos podPos) {
        Aspect expected=((TileManaPod)level.getBlockEntity(podPos)).getAspect();
        int generatedAge=level.getBlockState(podPos).getValue(BlockManaPod.AGE);
        actor.player.teleportTo(level,podPos.getX()+.5,podPos.getY()-1.5,podPos.getZ()+.5,0,0);
        actor.player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        Set<UUID> prior=new HashSet<>();level.getAllEntities().forEach(entity->prior.add(entity.getUUID()));
        test.assertTrue(actor.player.gameMode.destroyBlock(podPos)&&level.isEmptyBlock(podPos),"actual survival block-break path harvests the naturally generated pod");
        List<ItemEntity> rewards=level.getEntitiesOfClass(ItemEntity.class,new AABB(podPos).inflate(3),item->!prior.contains(item.getUUID())&&item.getItem().is(ThaumcraftManaBeans.MANA_BEAN.get()));
        int count=rewards.stream().mapToInt(item->item.getItem().getCount()).sum();
        test.assertTrue(count>=1&&count<=2&&rewards.stream().allMatch(item->ItemManaBean.data(item.getItem())!=null&&ItemManaBean.data(item.getItem()).firstTag().equals(expected.getTag())),"native harvest emits actual one-or-two typed beans matching the generated pod");
        // Native item merging is real ownership transfer. Advance all live
        // outputs together, then collect surviving entities rather than trying
        // to pick a source already merged into another actual reward.
        for(int tick=0;tick<10;tick++)for(var item:rewards)if(!item.isRemoved())item.tick();
        for(var item:rewards) {
            if(item.isRemoved())continue;
            actor.player.teleportTo(item.getX(),item.getY(),item.getZ());
            test.assertTrue(item.getBoundingBox().inflate(.1).intersects(actor.player.getBoundingBox()),"connected survival actor physically overlaps the real delayed reward before native pickup callback");
            item.playerTouch(actor.player);test.assertTrue(item.isRemoved(),"actual native pickup transfers each real reward exactly once");
        }
        test.assertTrue(rewards.stream().allMatch(Entity::isRemoved),"every original reward entity relinquishes ownership through merging or actual pickup");
        int slot=-1,owned=0;
        for(int i=0;i<actor.player.getInventory().items.size();i++) {
            var stack=actor.player.getInventory().getItem(i);if(stack.is(ThaumcraftManaBeans.MANA_BEAN.get())){owned+=stack.getCount();if(slot<0)slot=i;}
        }
        test.assertTrue(owned==count&&slot>=0&&slot<9,"actual pickup owns the harvested beans in the connected player's initially empty hotbar");
        actor.player.getInventory().selected=slot;actor.player.teleportTo(podPos.getX()+.5,podPos.getY()-1.5,podPos.getZ()+.5);
        var held=actor.player.getMainHandItem();var hit=new BlockHitResult(Vec3.atBottomCenterOf(podPos.above()),Direction.DOWN,podPos.above(),false);
        test.assertTrue(actor.player.gameMode.useItemOn(actor.player,level,held,InteractionHand.MAIN_HAND,hit).consumesAction(),"actual native RMB replants a harvested bean under its real original support in the natural biome");
        test.assertTrue(level.getBlockState(podPos).is(ThaumcraftManaBeans.MANA_POD.get())&&level.getBlockState(podPos).getValue(BlockManaPod.AGE)==0&&level.getBlockEntity(podPos) instanceof TileManaPod planted&&planted.getAspect()==expected,"native survival planting preserves actual generated aspect into new age0 BE");
        int remaining=actor.player.getInventory().items.stream().filter(stack->stack.is(ThaumcraftManaBeans.MANA_BEAN.get())).mapToInt(ItemStack::getCount).sum();
        test.assertTrue(remaining==count-1,"ordinary survival planting spends exactly one actual harvested bean");
        // The GameTest world is saved between the runs of this case, and the pod that
        // the original plants carries the age zero (oracle ItemManaBean:192-193). The
        // next run of this case would read that planted pod instead of the generated
        // one, so the generated state of the cell is restored here.
        level.setBlock(podPos,ThaumcraftManaBeans.MANA_POD.get().defaultBlockState().setValue(BlockManaPod.AGE,generatedAge),3);
        test.assertTrue(level.getBlockState(podPos).getValue(BlockManaPod.AGE)==generatedAge
                        &&level.getBlockEntity(podPos) instanceof TileManaPod restored&&restored.getAspect()==expected,
                "the planted pod is restored to the generated age and aspect for the next run of this case");
    }
    private static void naturalPech(GameTestHelper test,ServerLevel level,FocusUpgradeRuntimeChecks.Actor actor,List<BlockPos> ground) {
        test.assertTrue(level.getDifficulty()!=Difficulty.PEACEFUL,"declared native natural population fixture is non-Peaceful");
        long oldTime=actor.level.getDayTime(),resume=level.random.nextLong();Set<UUID> prior=new HashSet<>();level.getAllEntities().forEach(entity->prior.add(entity.getUUID()));
        List<Entity> spawned=new ArrayList<>();boolean found=false;int attempts=0;
        try {
            actor.level.setDayTime(18000);actor.level.updateSkyBrightness();level.updateSkyBrightness();
            for(int seed=0;seed<512&&!found;seed++) {
                var pos=ground.get((seed*37)%ground.size());
                actor.player.teleportTo(level,pos.getX()+40.5,pos.getY()+1,pos.getZ()+.5,0,0);
                level.random.setSeed(seed);attempts++;
                // Unmodified native callback chooses weighted biome rows,
                // placement/light/population rules, construction, finalizeSpawn
                // and actual entity admission. No forced Pech type is passed.
                NaturalSpawner.spawnCategoryForPosition(MobCategory.MONSTER,level,pos);
                level.getAllEntities().forEach(entity->{if(entity instanceof Mob&&!prior.contains(entity.getUUID())&&!spawned.contains(entity))spawned.add(entity);});
                for(var entity:spawned)if(entity instanceof EntityPech pech&&!pech.isRemoved()) {
                    int type=pech.getPechType();var held=pech.getMainHandItem();
                    test.assertTrue(level.getBiome(pech.blockPosition()).is(ThaumcraftBiomes.MAGICAL_FOREST)&&type>=0&&type<=2
                        &&(type==1?held.getItem() instanceof ItemWandCasting:type==2?held.is(net.minecraft.world.item.Items.BOW):!(held.getItem() instanceof ItemWandCasting)&&!held.is(net.minecraft.world.item.Items.BOW)),"actual native weighted natural Pech has genuine forest placement and original randomized variant/equipment, including legal unarmed foragers");
                    test.assertTrue(pech.getAttribute(Attributes.FOLLOW_RANGE).getModifier(ResourceLocation.fromNamespaceAndPath("thaumcraft","pech_spawn_bonus_first"))!=null
                        &&pech.getAttribute(Attributes.FOLLOW_RANGE).getModifier(ResourceLocation.fromNamespaceAndPath("thaumcraft","pech_spawn_bonus_second"))!=null,"native natural admission called original Pech finalizeSpawn with both independent original bonuses");found=true;
                }
                spawned.forEach(Entity::discard);spawned.clear();
            }
            test.assertTrue(found,"bounded512 declared native WORLD seeds select/admit a real naturally populated Pech from actual generated forest ground; attempts="+attempts);
            Thaumcraft.LOG.info("TC4 actual fresh forest native weighted natural Pech admitted after {} callback attempts",attempts);
        } finally {
            spawned.forEach(Entity::discard);level.random.setSeed(resume);actor.level.setDayTime(oldTime);actor.level.updateSkyBrightness();level.updateSkyBrightness();
        }
    }
}
