package thaumcraft.client.fx.bolt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import net.minecraft.world.phys.Vec3;

/** Original FXLightningBoltCommon fractal, float vector arithmetic, split parent links and joined segment ends. */
public final class WispLightningShape {
    public final List<Segment> segments=new ArrayList<>();
    public final float length;
    public final int maxAge;
    public int mainSegments=1;
    private final Random random;
    private int splits;
    public WispLightningShape(Vec3 from,Vec3 to,long seed) {
        random=new Random(seed);V start=V.of(from),end=V.of(to);length=end.copy().sub(start).length();maxAge=3+random.nextInt(3)-1;
        segments.add(new Segment(new Point(start,new V(0,0,0)),new Point(end,new V(0,0,0)),1,0,0));
        int[] divisors={8,12,17,23,30,34,40};float[] chance={.7F,.5F,.5F,.5F,0,0,0};float[] angles={45,50,55,60,0,0,0};
        for(int i=0;i<divisors.length;i++) fractal(length*.4F/divisors[i],chance[i],i<4?.1F:0,angles[i]);
        segments.sort(Comparator.comparingInt((Segment s)->s.split).thenComparingInt(s->s.number));
        for(var segment:segments) segment.join();
        segments.sort(Comparator.comparingDouble((Segment s)->-s.light));
    }
    private void fractal(float amount,float chance,float splitLength,float angle) {
        var old=new ArrayList<>(segments);segments.clear();
        for(var segment:old) {
            Segment previous=segment.previous;V sub=segment.diff.copy().scale(.5F);Point[] points={segment.start,null,segment.end};
            V axis=segment.diff;V offset=(axis.z==0?new V(-axis.y,axis.x,0):new V(0,axis.z,-axis.y)).rotate(random.nextFloat()*360,axis).scale((random.nextFloat()-.5F)*amount);
            points[1]=new Point(segment.start.point.copy().add(sub),offset);
            for(int i=0;i<2;i++) {
                Segment next=new Segment(points[i],points[i+1],segment.light,segment.number*2+i,segment.split);next.previous=previous;if(previous!=null)previous.next=next;
                if(i!=0 && random.nextFloat()<chance) {
                    V splitAxis=new V(0,next.diff.z,-next.diff.y).rotate(random.nextFloat()*360,next.diff);
                    V delta=next.diff.copy().rotate((random.nextFloat()*.66F+.33F)*angle,splitAxis).scale(splitLength);
                    Segment fork=new Segment(points[i],new Point(points[i+1].base,points[i+1].offset.copy().add(delta)),segment.light/2,next.number,++splits);fork.previous=previous;segments.add(fork);
                }
                previous=next;segments.add(next);
            }
            if(segment.next!=null)segment.next.previous=previous;
        }
        mainSegments*=2;
    }
    private record Point(V base,V offset,V point) { Point(V base,V offset) { this(base,offset,base.copy().add(offset)); } }
    public static final class Segment {
        private final Point start,end;
        public final float light;
        public final int number,split;
        private final V diff;
        private Segment previous,next;
        private V previousDiff,nextDiff;
        private float sinPrevious,sinNext;
        Segment(Point start,Point end,float light,int number,int split) { this.start=start;this.end=end;this.light=light;this.number=number;this.split=split;diff=end.point.copy().sub(start.point); }
        private void join() {
            if(previous!=null) {
                V before=previous.diff.copy().normalize(),current=diff.copy().normalize();previousDiff=current.add(before).normalize();
                sinPrevious=jointSine(current.dot(before.scale(-1)));
            } else { previousDiff=diff.copy().normalize();sinPrevious=1; }
            if(next!=null) {
                V after=next.diff.copy().normalize(),current=diff.copy().normalize();nextDiff=current.add(after).normalize();
                sinNext=jointSine(current.dot(after.scale(-1)));
            } else { nextDiff=diff.copy().normalize();sinNext=1; }
        }
        public Vec3 start() { return start.point.nativeVector(); }
        public Vec3 end() { return end.point.nativeVector(); }
        public Vec3 direction() { return diff.nativeVector(); }
        public Vec3 previousDirection() { return previousDiff.nativeVector(); }
        public Vec3 nextDirection() { return nextDiff.nativeVector(); }
        public float sinPrevious() { return sinPrevious; }
        public float sinNext() { return sinNext; }
        public boolean first() { return previous==null; }
        public boolean last() { return next==null; }
        private static float jointSine(float dot) {
            // Original normalized float dot can exceed1 by one ulp (seed2, fork12, segment110), yielding acos NaN.
            float sine=(float)Math.sin((float)Math.acos(Math.max(-1,Math.min(1,dot)))/2F);
            return Float.isFinite(sine) && sine>0 ? sine : 1;
        }
    }
    /** Keep the original mutable float operations; double Vec3 normalization changes the seeded branch geometry. */
    private static final class V {
        float x,y,z;
        V(float x,float y,float z) { this.x=x;this.y=y;this.z=z; }
        static V of(Vec3 v) { return new V((float)v.x,(float)v.y,(float)v.z); }
        V copy() { return new V(x,y,z); }
        V add(V v) {x+=v.x;y+=v.y;z+=v.z;return this;}
        V sub(V v) {x-=v.x;y-=v.y;z-=v.z;return this;}
        V scale(float f) {x*=f;y*=f;z*=f;return this;}
        float length() { return (float)Math.sqrt(x*x+y*y+z*z); }
        V normalize() { float length=length();x/=length;y/=length;z/=length;return this; }
        float dot(V v) { return x*v.x+y*v.y+z*v.z; }
        V rotate(float angle,V axis) {
            V normal=axis.copy().normalize();float a=normal.x,b=normal.y,c=normal.z;
            double radians=angle*.0174532925;float cos=(float)Math.cos(radians),sin=(float)Math.sin(radians),opposite=1-cos;
            float newX=x*(a*a*opposite+cos)+y*(b*a*opposite+c*sin)+z*(a*c*opposite-b*sin);
            float newY=x*(a*b*opposite-c*sin)+y*(b*b*opposite+cos)+z*(b*c*opposite+a*sin);
            float newZ=x*(a*c*opposite+b*sin)+y*(b*c*opposite-a*sin)+z*(c*c*opposite+cos);
            x=newX;y=newY;z=newZ;return this;
        }
        Vec3 nativeVector() { return new Vec3(x,y,z); }
    }
}
