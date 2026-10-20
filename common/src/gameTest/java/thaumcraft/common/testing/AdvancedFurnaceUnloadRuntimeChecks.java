package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.common.config.ThaumcraftAdvancedFurnace;
import thaumcraft.common.lib.AdvancedFurnaceFormation;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileAlchemyFurnaceAdvanced;
import thaumcraft.common.tiles.TileAlchemyFurnaceAdvancedPart;

/** ACTUAL ticket-driven neighboring chunk unload/save/reload, not reflected visibility absence or loadStatic replacement.
 * Suggested timeout1000: vanilla player-ticket removal/unload can take hundreds of ordinary server ticks. */
public final class AdvancedFurnaceUnloadRuntimeChecks {
    private static final TicketType<BlockPos> OWNED=TicketType.create("thaumcraft_furnace_custody_test",Comparator.comparingLong(BlockPos::asLong));
    private AdvancedFurnaceUnloadRuntimeChecks() {}
    public static void run(GameTestHelper test) {
        var check=new Check(test);
        try { check.prepare(); } catch(RuntimeException|Error failure) { check.close(); throw failure; }
    }
    private static final class Check {
        final GameTestHelper test; final Fixture f; final BlockPos center,shell; final ChunkPos coreChunk,shellChunk;
        final List<ChunkPos> forced=new ArrayList<>();
        TileAlchemyFurnaceAdvanced core; TileAlchemyFurnaceAdvancedPart oldShell;
        UUID machine; int polls; boolean coreTicket,shellTicket,closed;
        Check(GameTestHelper test) {
            this.test=test; f=new Fixture(test);
            int remoteX=((f.base.getX()+512)>>4)*16+15, remoteZ=((f.base.getZ()+512)>>4)*16+8;
            center=new BlockPos(remoteX,f.base.getY()+100,remoteZ); shell=center.offset(1,0,1);
            coreChunk=new ChunkPos(center); shellChunk=new ChunkPos(shell);
        }
        void prepare() {
            f.open(); forced.addAll(GolemChunks.hold(f.level,List.of(center)));
            f.check(!coreChunk.equals(shellChunk),"actual paid machine intentionally straddles native chunk boundary");
            AdvancedFurnaceRuntimeChecks.rememberBox(f,center.offset(-2,-2,-2),center.offset(2,3,2));
            core=AdvancedFurnaceRuntimeChecks.form(f,center,false);
            oldShell=(TileAlchemyFurnaceAdvancedPart)f.level.getBlockEntity(shell); machine=core.machineId();
            f.check(oldShell.belongsTo(core),"real paid formation gives the remote shell exact persistent core UUID");
            // Radius0 means native FULL level33 only for this chunk; its neighbor becomes unavailable at level34.
            f.level.getChunkSource().addRegionTicket(OWNED,coreChunk,0,center); coreTicket=true;
            f.level.getServer().getPlayerList().remove(f.player); // Real owned actor logs out; no player view ticket can pin the remote shell.
            for(var chunk:forced) f.level.setChunkForced(chunk.x,chunk.z,false); forced.clear();
            f.level.getChunkSource().save(true);
            pollUnload();
        }
        void pollUnload() {
            test.runAfterDelay(5,()-> {
                try {
                    polls++;
                    var visible=f.level.getChunkSource().getChunkNow(shellChunk.x,shellChunk.z);
                    if(visible!=null || !oldShell.isRemoved()) {
                        f.check(polls<140,"native neighboring shell chunk ACTUALLY unloads and retires its old BE after ticket/player removal"); pollUnload(); return;
                    }
                    f.check(f.level.getChunkSource().getChunkNow(coreChunk.x,coreChunk.z)!=null && core.isCurrent(),"core remains the same genuinely loaded native BE while neighboring shell is unloaded");
                    int count=core.getTickCount(),heat=core.getHeat();
                    f.check(AdvancedFurnaceFormation.structure(core)==AdvancedFurnaceFormation.Status.UNLOADED,"real native structure observation distinguishes unavailable from missing");
                    AdvancedFurnaceRuntimeChecks.ticks(f,core,5);
                    f.check(core.getTickCount()==count && core.getHeat()==heat && f.level.getChunkSource().getChunkNow(shellChunk.x,shellChunk.z)==null,
                            "actual loaded core pauses through unknown17-part terrain without raw CV debit, heat decay, count advance or force-load");
                    f.level.setBlock(center,Blocks.AIR.defaultBlockState(),TubeVariantsRuntimeChecks.QUIET);
                    f.check(core.isRemoved() && f.level.getChunkSource().getChunkNow(shellChunk.x,shellChunk.z)==null,"actual core removal never force-loads the saved remote shell chunk");
                    f.level.getChunkSource().addRegionTicket(OWNED,shellChunk,0,shell); shellTicket=true; polls=0; pollReload();
                } catch(RuntimeException|Error failure) { close(); throw failure; }
            });
        }
        void pollReload() {
            test.runAfterDelay(5,()-> {
                try {
                    polls++;
                    var chunk=f.level.getChunkSource().getChunkNow(shellChunk.x,shellChunk.z);
                    if(chunk==null) { f.check(polls<40,"native radius0 ticket asynchronously reloads actual saved shell terrain"); pollReload(); return; }
                    var loaded=chunk.getBlockEntity(shell);
                    f.check(loaded instanceof TileAlchemyFurnaceAdvancedPart part && part!=oldShell && machine.equals(part.machineId()) && center.equals(part.ownerPosition()),
                            "actual disk-backed chunk reload creates a NEW native BE retaining exact paid owner UUID/position");
                    var part=(TileAlchemyFurnaceAdvancedPart)loaded;
                    f.check(f.level.getChunkSource().getChunkNow(coreChunk.x,coreChunk.z)!=null && f.level.getBlockEntity(center)==null,
                            "owner absence is now genuinely known and cannot be confused with unknown terrain");
                    part.observeOwner();
                    f.check(f.level.getBlockState(shell).is(ThaumcraftAdvancedFurnace.ADVANCED_CONSTRUCT.get()) && part.isRemoved()
                                    && f.level.getChunkSource().getChunkNow(coreChunk.x,coreChunk.z)!=null,
                            "saved paid orphan shell reverts to its original physical advanced construct after real unload/core break/reload, with no resurrected pool/core or detached debit");
                    close(); test.succeed();
                } catch(RuntimeException|Error failure) { close(); throw failure; }
            });
        }
        void close() {
            if(closed) return; closed=true;
            try {
                // Cleanup explicitly loads owned fixture columns so Fixture can restore its exact recorded originals.
                for(var pos:f.states.keySet()) f.level.getChunk(pos.getX()>>4,pos.getZ()>>4);
                f.close();
            } finally {
                if(coreTicket) f.level.getChunkSource().removeRegionTicket(OWNED,coreChunk,0,center);
                if(shellTicket) f.level.getChunkSource().removeRegionTicket(OWNED,shellChunk,0,shell);
                for(var chunk:forced) f.level.setChunkForced(chunk.x,chunk.z,false);
            }
        }
    }
}
