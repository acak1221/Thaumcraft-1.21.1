package thaumcraft.warp;

import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import thaumcraft.client.fx.bolt.WispLightningShape;
import static org.junit.jupiter.api.Assertions.*;

class WispLightningShapeTest {
    @Test void sevenFractalsRetainEndpointsBranchesAndFiniteJoinedWidths() {
        Vec3 from=new Vec3(1,2,3),to=new Vec3(7,5,8);
        for(int seed=0;seed<64;seed++) {
            var shape=new WispLightningShape(from,to,seed);
            assertEquals(128,shape.mainSegments);
            var main=shape.segments.stream().filter(segment->segment.split==0).toList();
            assertEquals(128,main.size());assertTrue(shape.segments.size()>128);
            assertEquals(from,main.stream().filter(segment->segment.number==0).findFirst().orElseThrow().start());
            assertEquals(to,main.stream().filter(segment->segment.number==127).findFirst().orElseThrow().end());
            for(var segment:shape.segments) {
                for(Vec3 vector:List.of(segment.start(),segment.end(),segment.previousDirection(),segment.nextDirection()))
                    assertTrue(Double.isFinite(vector.x)&&Double.isFinite(vector.y)&&Double.isFinite(vector.z));
                assertTrue(segment.sinPrevious()>0 && Float.isFinite(segment.sinPrevious()));
                assertTrue(segment.sinNext()>0 && Float.isFinite(segment.sinNext()));
                assertTrue(segment.number>=0 && segment.number<128 && segment.light>0 && segment.light<=1);
            }
            assertTrue(shape.maxAge>=2 && shape.maxAge<=4);
        }
    }
    @Test void seededFractalIsStableAndDifferentSeedsChangeItsBranches() {
        Vec3 from=new Vec3(0,0,0),to=new Vec3(3,4,5);
        var first=new WispLightningShape(from,to,42);var repeated=new WispLightningShape(from,to,42);var different=new WispLightningShape(from,to,43);
        var firstGeometry=first.segments.stream().map(segment->List.of(segment.start(),segment.end())).toList();
        assertEquals(firstGeometry,repeated.segments.stream().map(segment->List.of(segment.start(),segment.end())).toList());
        assertNotEquals(firstGeometry,different.segments.stream().map(segment->List.of(segment.start(),segment.end())).toList());
    }
}
