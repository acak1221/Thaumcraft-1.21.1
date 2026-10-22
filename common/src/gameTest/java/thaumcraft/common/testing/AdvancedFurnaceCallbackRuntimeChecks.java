package thaumcraft.common.testing;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.visnet.TileVisNode;
import thaumcraft.common.blocks.BlockVisRelay;
import thaumcraft.common.config.ThaumcraftNodeDevices;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileAlchemyFurnaceAdvanced;
import thaumcraft.common.tiles.TileAlchemyFurnaceAdvancedNozzle;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.api.nodes.NodeType;

/** Actual native comparator notification reentry and actual relay pulse destruction; no final production class is subclassed. */
public final class AdvancedFurnaceCallbackRuntimeChecks {
    private AdvancedFurnaceCallbackRuntimeChecks() {}
    public static void run(GameTestHelper test) {
        var f=new Fixture(test);
        var forced=GolemChunks.hold(test.getLevel(),List.of(f.base.west(12),f.base.east(12)));
        try {
            f.open(); AdvancedFurnaceRuntimeChecks.rememberBox(f,f.base.offset(-12,-3,-3),f.base.offset(4,3,3));
            var core=AdvancedFurnaceRuntimeChecks.form(f,f.base,false);
            var source=NodeTransductionRuntimeChecks.energize(f,f.base.west(6),NodeTransductionRuntimeChecks.primals(2500),null,NodeType.NORMAL);
            AdvancedFurnaceRuntimeChecks.warm(f,core,source,50);
            var input=AdvancedFurnaceRuntimeChecks.physical(f,f.base,new ItemStack(Items.COAL)); f.level.getBlockState(f.base).entityInside(f.level,f.base,input);
            f.check(input.isRemoved() && core.getVis()>1,"callback case owns actual paid native slurry");
            BlockPos comparator=f.base.east(2); f.put(comparator.below(),Blocks.STONE);
            f.level.setBlock(comparator,Blocks.COMPARATOR.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.WEST),TubeVariantsRuntimeChecks.QUIET);
            f.level.updateNeighborsAt(f.base.east(),f.level.getBlockState(f.base.east()).getBlock());
            test.runAfterDelay(5,()-> {
                try {
                    var east=(TileAlchemyFurnaceAdvancedNozzle)f.level.getBlockEntity(f.base.east());
                    var north=(TileAlchemyFurnaceAdvancedNozzle)f.level.getBlockEntity(f.base.north());
                    var nativeComparator=(ComparatorBlockEntity)f.level.getBlockEntity(comparator);
                    f.check(nativeComparator.getOutputSignal()==1,"real native comparator settles to original Boolean1 output for nonempty nozzle");
                    var probe=new ComparatorProbe(comparator,f.level.getBlockState(comparator)); probe.setOutputSignal(1);
                    f.level.removeBlockEntity(comparator); f.level.setBlockEntity(probe);
                    int total=core.getVis(),count=core.getTickCount(); Aspect aspect=east.getEssentiaType(null);
                    probe.action=()-> {
                        f.check(EssentiaTransfers.isLocked(east) && EssentiaTransfers.isLocked(core),"FIRST real comparator callback sees both physical source nozzle AND its shared CORE locked");
                        f.check(core.getVis()==total-1 && !north.takeFromContainer(aspect,1),"paid source unit is already gone; second nozzle cannot alias-debit shared pool during first source notification");
                        AdvancedFurnaceRuntimeChecks.ticks(f,core,5);
                        f.check(core.getTickCount()==count,"reentrant actual core ticker cannot advance draw/heat phase while a nozzle owns shared core custody");
                        boolean denied=false; try { core.loadWithComponents(core.saveWithoutMetadata(f.level.registryAccess()),f.level.registryAccess()); } catch(IllegalStateException expected) { denied=true; }
                        f.check(denied,"core reload cannot overwrite the pool through a different nozzle's locked native notification");
                    };
                    f.check(east.takeEssentia(aspect,1,Direction.EAST)==1 && probe.calls==1 && core.getVis()==total-1,"one actual native extraction and one real comparator callback conserve the shared slurry exactly");
                    f.reset(); pulseRemoval(f); f.close(); for(var chunk:forced) test.getLevel().setChunkForced(chunk.x,chunk.z,false); test.succeed();
                } catch(RuntimeException|Error failure) { f.close(); for(var chunk:forced) test.getLevel().setChunkForced(chunk.x,chunk.z,false); throw failure; }
            });
        } catch(RuntimeException|Error failure) { f.close(); for(var chunk:forced) test.getLevel().setChunkForced(chunk.x,chunk.z,false); throw failure; }
    }
    private static void pulseRemoval(Fixture f) {
        var core=AdvancedFurnaceRuntimeChecks.form(f,f.base,false);
        var source=NodeTransductionRuntimeChecks.energize(f,f.base.west(9),NodeTransductionRuntimeChecks.primals(2500),null,NodeType.NORMAL);
        NodeTransductionRuntimeChecks.sourceTick(f,source);
        BlockPos relayPos=f.base.west(3); f.put(relayPos.below(),Blocks.STONE);
        f.level.setBlock(relayPos,ThaumcraftNodeDevices.VIS_RELAY.get().defaultBlockState().setValue(BlockVisRelay.FACING,Direction.UP),TubeVariantsRuntimeChecks.QUIET);
        f.level.removeBlockEntity(relayPos); var relay=new PulseRelay(relayPos,f.level.getBlockState(relayPos)); f.level.setBlockEntity(relay);
        TileVisNode.tick(f.level,relayPos,relay.getBlockState(),relay);
        f.check(relay.getParent()!=null && relay.getParent().get()==source,"actual consumer routes via intermediary; source nine blocks away is outside direct consumer range");
        int[] calls={0};
        relay.action=()-> {
            calls[0]++;
            f.check(source.getAvailableVis().getAmount(Aspect.FIRE)==0 && core.getHeat()==49,"FIRST source-debit pulse sees actual source50→0 AND native core heat−1→49 BEFORE any removal callback");
            f.level.destroyBlock(core.getBlockPos(),true);
        };
        AdvancedFurnaceRuntimeChecks.ticks(f,core,5);
        f.check(calls[0]==1 && core.isRemoved() && source.getAvailableVis().getAmount(Aspect.FIRE)==0
                        && source.getAvailableVis().getAmount(Aspect.ENTROPY)==50 && source.getAvailableVis().getAmount(Aspect.WATER)==50,
                "actual pulse removal retires paid core without a detached late credit, source refund, duplicate50 grant or following-aspect debit");
        f.drops();
    }
    private static final class ComparatorProbe extends ComparatorBlockEntity {
        Runnable action; int calls;
        ComparatorProbe(BlockPos pos,BlockState state) { super(pos,state); }
        @Override public int getOutputSignal() { if(action!=null) { var once=action; action=null; calls++; once.run(); } return super.getOutputSignal(); }
    }
    private static final class PulseRelay extends TileVisNode {
        Runnable action;
        PulseRelay(BlockPos pos,BlockState state) { super(ThaumcraftNodeDevices.VIS_RELAY_BLOCK_ENTITY.get(),pos,state); }
        @Override public int getRange() { return 8; }
        @Override public boolean isSource() { return false; }
        @Override public void triggerConsumeEffect(Aspect aspect) { if(action!=null) { var once=action; action=null; once.run(); } }
    }
}
