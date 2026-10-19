package thaumcraft.fabric.clienttest;

import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

/** Real Minecraft AABB arithmetic, detached fixtures only; NOT a native despawn,
 * world/owned-cell observation, eligibility grant or candidate audit. */
public final class BoreClientEvidenceGeometryChecks {
    private static int assertions;
    private BoreClientEvidenceGeometryChecks() {}
    private static void check(boolean value,String message) {assertions++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args) throws Exception {
        if(args.length!=1)throw new IllegalArgumentException("One NEW result path required");
        var owner=new AABB(1471.2,216,375.4,1471.8,217.8,376.0);
        var spider=new AABB(1598.7647843128432,216,371.2804718695344,1600.1647843128432,216.9,372.6804718695344);
        var cells=List.of(new BlockPos(1601,216,374),new BlockPos(1603,216,381),new BlockPos(1604,216,382));
        check(BoreClientEvidenceGeometry.outside(spider,owner,cells),"Outside concrete cells despite being within the broad observation rectangle");
        for(BlockPos cell:cells)check(!BoreClientEvidenceGeometry.outside(new AABB(cell).inflate(.01),owner,cells),"Each stock/action cell independently forbids despawn scope");
        check(!BoreClientEvidenceGeometry.outside(owner,owner,List.of()),"Actual owner body protected");
        check(!BoreClientEvidenceGeometry.outside(owner.move(5,0,0),owner,List.of()),"Six-block action buffer protected");
        check(BoreClientEvidenceGeometry.outside(owner.move(8,0,0),owner,List.of()),"No infinite broad rectangle substituted for concrete footprint");
        check(!BoreClientEvidenceGeometry.outside(new AABB(1614,218,400,1615,219,401),owner,List.of()),"Reserved Bore lane cannot be waived as AIR");
        check(!BoreClientEvidenceGeometry.outside(spider,owner,List.of(BlockPos.containing(1599,216,371))),"A concrete protected cell at the same native point blocks the classification");
        check(BoreClientEvidenceGeometry.outside(spider,owner,List.of(BlockPos.containing(1599,215,371))),"Merely touching a floor face is not an intersected body, matching native AABB semantics");
        try {BoreClientEvidenceGeometry.outside(new AABB(Double.NaN,0,0,1,1,1),owner,cells);throw new AssertionError("Nonfinite body accepted");}
        catch(IllegalStateException expected) {assertions++;}
        try {BoreClientEvidenceGeometry.outside(spider,new AABB(0,0,0,Double.POSITIVE_INFINITY,1,1),cells);throw new AssertionError("Nonfinite owner accepted");}
        catch(IllegalStateException expected) {assertions++;}
        var pads=List.of(new BlockPos(1602,215,379),new BlockPos(1601,215,379),new BlockPos(1601,215,378),new BlockPos(1601,215,377));
        var cobbles=BoreClientLedger.cobbleCells();
        check(cobbles.size()==8 && cobbles.stream().distinct().count()==8 && cobbles.containsAll(pads),"Only four support pads +foundation/scaffold/two targets are tracked; no travel bridge");
        for(int x=1605;x<=1610;x++)check(!cobbles.contains(new BlockPos(x,217,377)),"Removed travel bridge must not become a final retained-block expectation");
        for(BlockPos removed:List.of(new BlockPos(1605,217,378),new BlockPos(1605,217,379),new BlockPos(1605,216,380)))check(!cobbles.contains(removed),"Removed southern travel cells are not expected outputs");
        check(BoreClientEvidence.metalLabelCustodyIndex(1,List.of("","",""))==-1,"Original physical blank has one owner");
        for(int i=0;i<3;i++) {var filters=new java.util.ArrayList<>(List.of("","",""));filters.set(i,"metallum");check(BoreClientEvidence.metalLabelCustodyIndex(0,filters)==i,"Actual installed filter is the single temporary owner");}
        for(var invalid:List.of(List.of("metallum","metallum",""),List.of("arbor","",""),List.of("metallum","")))rejectLabel(0,invalid);
        rejectLabel(0,List.of("","",""));rejectLabel(2,List.of("","",""));rejectLabel(1,List.of("metallum","",""));rejectLabel(-1,List.of("metallum","",""));
        sourceServicingChecks();
        oldJarEnvelopeChecks();
        contactProposalChecks();
        nativeOwnerIdentityChecks();
        try(var out=Files.newBufferedWriter(Path.of(args[0]),StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)) {
            new Gson().toJson(Map.of("status","DETACHED_GEOMETRY_CHECKS_PASSED_NOT_DESPAWN_AUTHORITY","assertions",assertions,
                    "limitations","No actual mob, caller frame, world cell, game or candidate audit was created; Ledger must observe these natively"),out);out.write('\n');
        }
    }
    private static void rejectLabel(int owners,List<String> filters) {
        try {BoreClientEvidence.metalLabelCustodyIndex(owners,filters);throw new AssertionError("Invalid label ownership accepted");}
        catch(IllegalStateException expected) {assertions++;}
    }
    private static BoreClientEvidence.SourceBuffer buffer(String type,int amount,String filter) {return new BoreClientEvidence.SourceBuffer(type,amount,filter);}
    private static void sourceServicingChecks() {
        var active=List.of(buffer("metallum",7,"metallum"),buffer("praecantatio",4,""),buffer("perfodio",4,""));
        check(BoreClientEvidence.sourceBuffersToDrain(Map.of("metallum",9,"praecantatio",4,"perfodio",4),active,false).isEmpty(),"Do NOT greedily drain one-unit positive channels that still have native work");
        check(BoreClientEvidence.sourceBuffersToDrain(Map.of("metallum",9,"praecantatio",4,"perfodio",4,"arbor",1),active,false).isEmpty(),"Wait for a typed channel to finish before rotation; a missing tag does not waive parallel servicing");
        check(BoreClientEvidence.sourceBuffersToDrain(Map.of("metallum",9,"perfodio",4,"arbor",1),active,false).equals(List.of(1)),"Free only the actually exhausted unfiltered channel for a missing live tag");
        var free=List.of(buffer("metallum",7,"metallum"),buffer("praecantatio",4,""),buffer("",0,""));
        check(BoreClientEvidence.sourceBuffersToDrain(Map.of("metallum",9,"perfodio",4,"arbor",1),free,false).isEmpty(),"An actual empty unfiltered channel already permits native initialization");
        var pinnedEmpty=List.of(buffer("",0,"metallum"),buffer("praecantatio",4,""),buffer("perfodio",4,""));
        check(BoreClientEvidence.sourceBuffersToDrain(Map.of("perfodio",4,"arbor",1),pinnedEmpty,false).equals(List.of(1)),"An empty metal-only filter is NOT room for a different waiting type");
        check(BoreClientEvidence.sourceBuffersToDrain(Map.of("metallum",9,"perfodio",4,"arbor",1),pinnedEmpty,false).isEmpty(),"Let the actual native metal filter refill before reconsidering rotation");
        var full=List.of(buffer("metallum",32,"metallum"),buffer("praecantatio",4,""),buffer("perfodio",4,""));
        check(BoreClientEvidence.sourceBuffersToDrain(Map.of("metallum",9,"praecantatio",4,"perfodio",4),full,false).equals(List.of(0)),"Real full32 buffer must be drained even when its native type still has stock");
        check(BoreClientEvidence.sourceBuffersToDrain(Map.of(),active,false).isEmpty(),"Do not erase typed channels between genuine source inputs");
        check(BoreClientEvidence.sourceBuffersToDrain(Map.of(),active,true).equals(List.of(0,1,2)),"Drain final partials only after real batch cooks and furnace empty");
        check(BoreClientEvidence.sourceBuffersToDrain(Map.of("metallum",9),active,true).isEmpty(),"Last admitted/cooked input is NOT an empty furnace or final-drain permission");
        var empty=List.of(buffer("",0,""),buffer("",0,"metallum"),buffer("",0,""));
        check(BoreClientEvidence.sourceBuffersToDrain(Map.of(),empty,true).isEmpty(),"No extraction from an empty buffer");
        rejectService(Map.of("metallum",-1),active);rejectService(Map.of("metallum",51),active);rejectService(Map.of("metallum",30,"perfodio",21),active);
        rejectService(Map.of(),List.of(buffer("metallum",1,"")));rejectService(Map.of(),List.of(buffer("",1,""),buffer("",0,""),buffer("",0,"")));
        rejectService(Map.of(),List.of(buffer("metallum",33,""),buffer("",0,""),buffer("",0,"")));
    }
    private static void rejectService(Map<String,Integer> furnace,List<BoreClientEvidence.SourceBuffer> buffers) {
        try {BoreClientEvidence.sourceBuffersToDrain(furnace,buffers,false);throw new AssertionError("Invalid source observation accepted");}
        catch(IllegalStateException expected) {assertions++;}
    }
    private static void oldJarEnvelopeChecks() {
        var model=new BoreClientParentModel();int jars=0;
        for(var expected:model.bes.values())if(expected.type().equals("thaumcraft:jar")) {
            var saved=expected.tag();var original=saved.copy();var live=saved.copy();live.remove("keepPacked");jars++;
            check(BoreClientParentModel.loadedOwnerExpected(saved,live).equals(live) && saved.equals(original),"Actual bound old-jar payload survives narrow outer chunk-header comparison on a copy");
            var stock=live.copy();stock.putInt("Amount",live.getInt("Amount")+1);
            check(!BoreClientParentModel.loadedOwnerExpected(saved,stock).equals(stock),"A jar stock change must NOT be waived as a chunk header");
            var filter=live.copy();filter.putString("AspectFilter",live.getString("AspectFilter")+"_changed");
            check(!BoreClientParentModel.loadedOwnerExpected(saved,filter).equals(filter),"Actual old jar filter metadata remains exact");
            var nested=live.copy();var child=new net.minecraft.nbt.CompoundTag();child.putBoolean("keepPacked",false);nested.put("nested",child);
            check(!BoreClientParentModel.loadedOwnerExpected(saved,nested).equals(nested),"No nested metadata/header stripping");
        }
        check(jars==20,"All20 bound old-jar metadata fixtures checked, not a world/native preservation claim");
    }
    private static void contactProposalChecks() {
        var actualPoint=new net.minecraft.world.phys.Vec3(1465.5,220,377.5);
        var candidates=BoreClientEvidenceGeometry.contactCandidates(actualPoint);
        check(candidates.size()==125 && candidates.stream().distinct().count()==125,"Finite five heights/five offsets per axis, not unbounded player-position search");
        check(candidates.contains(new net.minecraft.world.phys.Vec3(1465.5,220,377.5)),"Existing third-alembic top is an eligible proposal, never an installed platform");
        check(candidates.stream().allMatch(v->v.y>=216 && v.y<=220 && Math.abs(v.x-actualPoint.x)<=1 && Math.abs(v.z-actualPoint.z)<=1),"All candidates remain within the declared local physical contact envelope");
        var topBody=new AABB(1465.2,220.001,377.2,1465.8,221.8,377.8);var cube=new AABB(1465,219,377,1466,220,378);
        check(!topBody.intersects(cube) && new AABB(1465.201,219.965,377.201,1465.799,220.002,377.799).intersects(cube),"Detached full-cube arithmetic: actual top has support and body clearance");
        check(topBody.inflate(1,.5,1).intersects(new AABB(1465.375,220,377.375,1465.625,220.25,377.625)),"Top proposal overlaps the vanilla item-contact buffer; NOT an actual pickup witness");
        try {BoreClientEvidenceGeometry.contactCandidates(new net.minecraft.world.phys.Vec3(Double.NaN,218,377));throw new AssertionError("Nonfinite contact point accepted");}
        catch(IllegalStateException expected) {assertions++;}
    }
    private static void nativeOwnerIdentityChecks() {
        Object actual=new Object(),other=new Object();
        check(BoreClientEvidence.actualServerOwner(actual,null)==actual,"Before binding use the actual current native server reference");
        check(BoreClientEvidence.actualServerOwner(actual,actual)==actual,"Bound/current identity must agree");
        check(BoreClientEvidence.actualServerOwner(null,actual)==actual,"Client disconnect clears its pointer, not the actual bound server or its native stop clock");
        try {BoreClientEvidence.actualServerOwner(null,null);throw new AssertionError("Missing native owner accepted");}
        catch(IllegalStateException expected) {assertions++;}
        try {BoreClientEvidence.actualServerOwner(other,actual);throw new AssertionError("Replacement native server accepted");}
        catch(IllegalStateException expected) {assertions++;}
    }
}
