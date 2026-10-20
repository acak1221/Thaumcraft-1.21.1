package thaumcraft.common.testing;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import thaumcraft.common.blocks.BlockArcaneLamp;
import thaumcraft.common.config.ThaumcraftLamps;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.tiles.TileArcaneLamp;

/** Explicit dark room and finite ingredient inputs; actual lamp ticker, air-light blocks and support removal. */
public final class ArcaneLampRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private ArcaneLampRuntimeChecks() {}
    public static void run(GameTestHelper test) {
        Check check = new Check(test);
        try { check.prepare(); } catch (RuntimeException | Error failure) { check.finish(); throw failure; }
        awaitNativeLight(check, 0);
    }
    private static void awaitNativeLight(Check check, int attempts) {
        check.test.runAfterDelay(5, () -> {
            try {
                check.test.assertTrue(check.level.getBlockState(check.center).is(ThaumcraftLamps.ARCANE_LAMP.get())
                                && check.level.getBlockState(check.center).getLightEmission() == 15
                                && check.level.getBlockEntity(check.center) == check.tile,
                        "actual registered lamp remains a native emission15 source while threaded propagation completes");
                int blockLight = check.level.getBrightness(LightLayer.BLOCK, check.center.above());
                if (blockLight < 9) {
                    check.test.assertTrue(attempts < 25,
                            "native block-light propagation reaches lamp-adjacent cell within130 ordinary server ticks; actual=" + blockLight);
                    awaitNativeLight(check, attempts + 1);
                    return;
                }
                check.direct(); check.recipe();
                // direct() already replaced its sole injected light with cobblestone.
                // Retain lights created by ordinary native ticks during the initial propagation wait.
                // Purging them here queues asynchronous light decreases: fast GameTest ticks can
                // sample their stale bright cache for the whole next observation window.
                check.test.runAfterDelay(60, () -> {
                    try { check.naturalAndRemoval(); check.test.succeed(); } finally { check.finish(); }
                });
            } catch (RuntimeException | Error failure) { check.finish(); throw failure; }
        });
    }
    private static final class Check {
        final GameTestHelper test;
        final ServerLevel level;
        final BlockPos center;
        final Map<BlockPos, BlockState> original = new LinkedHashMap<>();
        final List<ChunkPos> forced = new ArrayList<>();
        final Set<UUID> existingItems = new HashSet<>();
        TileArcaneLamp tile;
        boolean finished;
        Check(GameTestHelper test) { this.test=test; level=test.getLevel(); center=test.absolutePos(new BlockPos(3,100,3)); }
        void set(BlockPos pos, BlockState state, int flags) { original.putIfAbsent(pos.immutable(), level.getBlockState(pos)); level.setBlock(pos,state,flags); }
        void prepare() {
            level.getEntitiesOfClass(ItemEntity.class,new AABB(center).inflate(20)).forEach(item->existingItems.add(item.getUUID()));
            forced.addAll(GolemChunks.hold(level, List.of(center.offset(-16,-16,-16),center.offset(16,16,16))));
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-16,-16,-16), center.offset(16,16,16))) {
                int x=Math.abs(pos.getX()-center.getX()), y=Math.abs(pos.getY()-center.getY()), z=Math.abs(pos.getZ()-center.getZ());
                if (x==16 || y==16 || z==16) set(pos,Blocks.STONE.defaultBlockState(),QUIET);
            }
            set(center.below(),Blocks.STONE.defaultBlockState(),QUIET);
            // Transparent temporary guard prevents native lamp ticks from placing a secondary
            // light in the exact bright-air probe before its own emission has propagated.
            set(center.above(),Blocks.GLASS.defaultBlockState(),QUIET);
            set(center,ThaumcraftLamps.ARCANE_LAMP.get().defaultBlockState(),QUIET);
            tile=(TileArcaneLamp) level.getBlockEntity(center);
            test.assertTrue(tile!=null,"a real Arcane Lamp is installed in an explicitly roofed room");
        }
        RandomSource draws(int dx,int dy,int dz) {
            int[] values={Math.max(dx,0),Math.max(-dx,0),Math.max(dy,0),Math.max(-dy,0),Math.max(dz,0),Math.max(-dz,0)};
            int[] cursor={0};
            return (RandomSource) Proxy.newProxyInstance(RandomSource.class.getClassLoader(),new Class<?>[]{RandomSource.class},(proxy,method,args)->{
                if (method.getName().equals("nextInt") && args!=null && args.length==1) {
                    test.assertTrue((int)args[0]==16 && cursor[0]<6,"exactly six original nextInt(16) calls in axis order");
                    return values[cursor[0]++];
                }
                throw new AssertionError("Unexpected lamp RNG method "+method.getName());
            });
        }
        boolean at(int dx,int dy,int dz) { return tile.spreadLight(draws(dx,dy,dz)); }
        void direct() {
            test.assertTrue(!at(0,-1,0) && level.getBlockState(center.below()).is(Blocks.STONE),"a solid support is not replaced by light");
            BlockPos bright=center.above();
            set(bright,Blocks.AIR.defaultBlockState(),QUIET);
            test.assertTrue(level.getRawBrightness(bright,0)>=9,"the lamp itself is an actually bright source");
            test.assertTrue(!at(0,1,0) && !level.getBlockState(bright).is(ThaumcraftLamps.ARCANE_LIGHT.get()),"already safe bright air is not filled with light blocks");
            BlockPos candidate=null;
            search: for (BlockPos p:BlockPos.betweenClosed(center.offset(-15,-15,-15),center.offset(15,15,15))) {
                var state=level.getBlockState(p);
                if (state.isAir() && !state.is(ThaumcraftLamps.ARCANE_LIGHT.get()) && level.getRawBrightness(p,0)<9) {candidate=p.immutable();break search;}
            }
            test.assertTrue(candidate!=null,"there is real unsafe darkness in the closed room");
            int dx=candidate.getX()-center.getX(),dy=candidate.getY()-center.getY(),dz=candidate.getZ()-center.getZ();
            test.assertTrue(at(dx,dy,dz),"the actual triangular candidate places real light in dark air");
            var light=level.getBlockState(candidate);
            test.assertTrue(light.is(ThaumcraftLamps.ARCANE_LIGHT.get()) && light.isAir() && light.getLightEmission()==15
                            && light.getCollisionShape(level,candidate,CollisionContext.empty()).isEmpty()
                            && light.getRenderShape()==RenderShape.INVISIBLE && level.getBlockEntity(candidate)==null
                            && light.getBlock().asItem()==Items.AIR,"the secondary light is invisible replaceable air with emission15, no collision, no tile and no item");
            test.assertTrue(!at(dx,dy,dz),"the same existing lamp light is never overwritten as a new light");
            level.setBlock(candidate,Blocks.COBBLESTONE.defaultBlockState(),QUIET);
            test.assertTrue(level.getBlockState(candidate).is(Blocks.COBBLESTONE),"a physical block replaces the intangible light without a phantom item");
            original.putIfAbsent(candidate,Blocks.AIR.defaultBlockState());
            var saved=tile.saveWithoutMetadata(level.registryAccess());
            test.assertTrue(saved.getInt("orientation")==Direction.DOWN.get3DDataValue(),"source orientation is saved");
            var restored=new TileArcaneLamp(center,tile.getBlockState());
            restored.loadWithComponents(saved,level.registryAccess());
            test.assertTrue(restored.getFacing()==tile.getFacing(),"fresh native state and tile NBT keep the same attachment");
        }
        void recipe() {
            var recipe=(ArcaneShapedRecipe)level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft","arcane/arcane_lamp")).orElseThrow().value();
            ItemStack iron=new ItemStack(Items.IRON_INGOT), amber=new ItemStack(ThaumcraftLamps.AMBER_BLOCK_ITEM.get());
            ItemStack nitor=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thaumcraft","nitor")));
            var in=new ArcaneRecipeInput(List.of(ItemStack.EMPTY,new ItemStack(Items.DAYLIGHT_DETECTOR),ItemStack.EMPTY,iron,amber,iron,ItemStack.EMPTY,nitor,ItemStack.EMPTY));
            test.assertTrue(recipe.matches(in,level) && recipe.research().equals("ARCANELAMP") && recipe.vis().equals(Map.of("ignis",8,"aer",8,"aqua",4,"perditio",4))
                    && recipe.assemble(in,level.registryAccess()).is(ThaumcraftLamps.ARCANE_LAMP_ITEM.get()),"original lamp recipe, actual result, exact research and vis");
            var block=ThaumcraftLamps.ARCANE_LAMP.get();
            test.assertTrue(block.defaultBlockState().getDestroySpeed(level,center)==3 && Math.abs(block.getExplosionResistance()-10.2F)<1e-6,"original metal-device hardness and converted resistance");
        }
        void naturalAndRemoval() {
            int lights=0,darkAir=0;
            for (BlockPos pos:BlockPos.betweenClosed(center.offset(-15,-15,-15),center.offset(15,15,15))) {
                var state=level.getBlockState(pos);
                if(state.is(ThaumcraftLamps.ARCANE_LIGHT.get()))++lights;
                else if(state.isAir()&&level.getRawBrightness(pos,0)<9)++darkAir;
            }
            test.assertTrue(lights>0,"ordinary server ticks make secondary lights in the real dark room, saw "+lights
                    +", darkAir="+darkAir+", currentTile="+(level.getBlockEntity(center)==tile)
                    +", removed="+tile.isRemoved()+", aboveBlockLight="+level.getBrightness(LightLayer.BLOCK,center.above()));
            BlockPos edge=center.offset(15,0,0),outside=center.offset(16,0,0);
            set(edge,ThaumcraftLamps.ARCANE_LIGHT.get().defaultBlockState(),QUIET);
            set(outside,ThaumcraftLamps.ARCANE_LIGHT.get().defaultBlockState(),QUIET);
            set(center.below(),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
            test.assertTrue(!level.getBlockState(center).is(ThaumcraftLamps.ARCANE_LAMP.get()),"loss of the actual support removes the lamp");
            for (BlockPos pos:BlockPos.betweenClosed(center.offset(-15,-15,-15),center.offset(15,15,15))) test.assertTrue(!level.getBlockState(pos).is(ThaumcraftLamps.ARCANE_LIGHT.get()),"every light in the original removal cube is removed");
            test.assertTrue(level.getBlockState(outside).is(ThaumcraftLamps.ARCANE_LIGHT.get()),"the original cube does not remove a light sixteen blocks away");
            int drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(center).inflate(3),item->!existingItems.contains(item.getUUID()) && item.getItem().is(ThaumcraftLamps.ARCANE_LAMP_ITEM.get())).stream().mapToInt(item->item.getItem().getCount()).sum();
            test.assertTrue(drops==1,"support loss drops exactly the original lamp item: "+drops);
        }
        void finish() {
            if(finished)return;finished=true;
            if(tile!=null)tile.removeLights();
            level.getEntitiesOfClass(ItemEntity.class,new AABB(center).inflate(20),item->!existingItems.contains(item.getUUID()) && item.getItem().is(ThaumcraftLamps.ARCANE_LAMP_ITEM.get())).forEach(ItemEntity::discard);
            original.forEach((pos,state)->level.setBlock(pos,state,QUIET));
            for(ChunkPos chunk:forced)level.setChunkForced(chunk.x,chunk.z,false);
        }
    }
}
