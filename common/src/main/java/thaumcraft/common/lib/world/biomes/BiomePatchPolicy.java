package thaumcraft.common.lib.world.biomes;

import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import thaumcraft.common.config.BiomeSettings;

/** Forced1.21 surface-climate adapter: original WARM/COOL weighted palettes,
 * LCG and six Zoom policies select TC4 islands while vanilla tickets retain
 * the existing native biome. This is not the removed1.7 terrain/river engine.
 * Distinct vanilla palette tokens survive mode voting to retain proper odds. */
public final class BiomePatchPolicy {
    public static final int MAGICAL_FOREST=1000,TAINT=1001;
    private static final long MULTIPLIER=6364136223846793005L,ADDEND=1442695040888963407L;
    private static final int[] ZOOM_SALTS={1000,1001,1000,1001,1002,1003};
    private static final int[] WARM={4,29,3,1,27,6},COOL={4,3,5,1};
    private static final Climate.Parameter[] TEMPERATURES=new OverworldBiomeBuilder().getTemperatureThresholds();
    private final Climate.Sampler sampler;
    private final long seed;
    private final BiomeSettings settings;
    private final long[] seeds=new long[7];
    private final Long2IntLinkedOpenHashMap[] caches=new Long2IntLinkedOpenHashMap[7];
    public BiomePatchPolicy(long seed,Climate.Sampler sampler,BiomeSettings settings) {
        this.sampler=sampler;this.seed=seed;this.settings=settings;seeds[0]=worldSeed(seed,200);
        for(int i=1;i<7;i++)seeds[i]=worldSeed(seed,ZOOM_SALTS[i-1]);
        for(int i=0;i<7;i++){caches[i]=new Long2IntLinkedOpenHashMap();caches[i].defaultReturnValue(Integer.MIN_VALUE);}
    }
    /** Each policy belongs to one thread and one native world sampler. */
    public boolean matches(long seed,Climate.Sampler sampler,BiomeSettings settings) {return this.seed==seed&&this.sampler==sampler&&this.settings.equals(settings);}
    public int sample(int quartX,int quartZ) { return sample(6,quartX,quartZ); }
    private int sample(int layer,int x,int z) {
        var cache=caches[layer];long key=(long)x<<32|(z&0xffffffffL);int cached=cache.getAndMoveToFirst(key);if(cached!=Integer.MIN_VALUE)return cached;
        int value;
        if(layer==0) {
            // Native quart cell64 is256blocks; sample its surface-climate center.
            int band=temperatureBand(sampler.sample((x<<6)+32,16,(z<<6)+32).temperature());
            var random=new LayerRandom(seeds[0],x,z);
            if(band==1)value=ticket(COOL,random.nextInt(settings.coolWeight()));
            else if(band==2||band==3)value=ticket(WARM,random.nextInt(settings.warmWeight()));
            else value=band==0?12:2;
        } else {
            int px=x>>1,pz=z>>1;
            int tl=sample(layer-1,px,pz),bl=sample(layer-1,px,pz+1),tr=sample(layer-1,px+1,pz),br=sample(layer-1,px+1,pz+1);
            var random=new LayerRandom(seeds[layer],px<<1,pz<<1);
            int bottomLeft=random.nextInt(2)==0?tl:bl;
            int topRight=random.nextInt(2)==0?tl:tr;
            int bottomRight=mode(random,tl,tr,bl,br);
            value=(x&1)==0?((z&1)==0?tl:bottomLeft):((z&1)==0?topRight:bottomRight);
        }
        cache.putAndMoveToFirst(key,value);if(cache.size()>8192)cache.removeLastInt();return value;
    }
    private int ticket(int[] vanilla,int ticket) {
        int base=vanilla.length*10;if(ticket<base)return vanilla[ticket/10];return ticket<base+settings.magicalForestWeight()?MAGICAL_FOREST:TAINT;
    }
    public static int temperatureBand(long temperature) {
        // Native shared-boundary ties use the lower band, preserving cold edges.
        for(int i=0;i<TEMPERATURES.length;i++)if(temperature<=TEMPERATURES[i].max())return i;
        return TEMPERATURES.length-1;
    }
    private static int mode(LayerRandom random,int a,int b,int c,int d) {
        if(b==c&&c==d)return b;
        if(a==b&&a==c||a==b&&a==d||a==c&&a==d)return a;
        if(a==b&&c!=d||a==c&&b!=d||a==d&&b!=c)return a;
        if(b==c&&a!=d||b==d&&a!=c)return b;
        if(c==d&&a!=b)return c;
        return switch(random.nextInt(4)){case 0->a;case 1->b;case 2->c;default->d;};
    }
    private static long mix(long state,long add) { return state*(state*MULTIPLIER+ADDEND)+add; }
    private static long worldSeed(long seed,long salt) {
        long base=salt;for(int i=0;i<3;i++)base=mix(base,salt);
        for(int i=0;i<3;i++)seed=mix(seed,base);return seed;
    }
    private static final class LayerRandom {
        private final long world;private long state;
        LayerRandom(long world,long x,long z) {this.world=world;state=world;state=mix(state,x);state=mix(state,z);state=mix(state,x);state=mix(state,z);}
        int nextInt(int bound) {int value=(int)((state>>24)%bound);if(value<0)value+=bound;state=mix(state,world);return value;}
    }
}
