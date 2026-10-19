package thaumcraft.fabric.clienttest;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Detached geometry only. No native provenance, permission, world read or write.
 * The caller still verifies actual ambient ownership and native removal frames. */
final class BoreClientEvidenceGeometry {
    private BoreClientEvidenceGeometry() {}
    /** Finite PLAYER proposals only, including the real top of the third
     * inherited alembic. The caller must read current support/collision/contact;
     * a proposal is never a platform, item movement or pickup acknowledgement. */
    static java.util.List<Vec3> contactCandidates(Vec3 actualItemPosition) {
        if(!Double.isFinite(actualItemPosition.x) || !Double.isFinite(actualItemPosition.y) || !Double.isFinite(actualItemPosition.z))throw new IllegalStateException("Nonfinite actual item contact point");
        var result=new java.util.ArrayList<Vec3>(125);
        for(int y=216;y<=220;y++)for(double dx:new double[]{0,.6,-.6,1,-1})for(double dz:new double[]{0,.6,-.6,1,-1})result.add(new Vec3(actualItemPosition.x+dx,y,actualItemPosition.z+dz));
        return java.util.List.copyOf(result);
    }
    static boolean outside(AABB body,AABB owner,Iterable<BlockPos> cells) {
        finite(body);finite(owner);
        if(body.intersects(owner.inflate(6)))return false;
        for(BlockPos pos:cells)if(body.intersects(new AABB(pos)))return false;
        return !body.intersects(new AABB(1613,216,382,1618,221,448));
    }
    private static void finite(AABB box) {
        if(!Double.isFinite(box.minX) || !Double.isFinite(box.minY) || !Double.isFinite(box.minZ)
                || !Double.isFinite(box.maxX) || !Double.isFinite(box.maxY) || !Double.isFinite(box.maxZ))
            throw new IllegalStateException("Nonfinite actual retirement body");
    }
}
