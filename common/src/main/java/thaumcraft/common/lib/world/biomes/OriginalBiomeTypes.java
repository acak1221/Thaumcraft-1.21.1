package thaumcraft.common.lib.world.biomes;

import java.util.List;
import thaumcraft.api.aspects.Aspect;

/** Immutable TC4 Config.registerBiomes:782-812 values, in their original registration order.
 * The public BiomeHandler registry and both native tag adapters start from these same rows.
 * Runtime adapters retain this original snapshot; BiomeHandler.registerBiomeInfo remains a separate mutable API.
 */
public final class OriginalBiomeTypes {
    public record Type(String name, int aura, Aspect aspect, boolean greatwood, float greatwoodChance) {}

    private static final List<Type> TYPES = List.of(
            new Type("water",100,Aspect.WATER,false,0.0F),
            new Type("ocean",120,Aspect.WATER,false,0.0F),
            new Type("river",100,Aspect.WATER,false,0.0F),
            new Type("wet",80,Aspect.WATER,false,0.0F),
            new Type("hot",100,Aspect.FIRE,false,0.0F),
            new Type("desert",100,Aspect.FIRE,false,0.0F),
            new Type("nether",120,Aspect.FIRE,false,0.0F),
            new Type("mesa",80,Aspect.FIRE,false,0.0F),
            new Type("dense",100,Aspect.ORDER,false,0.0F),
            new Type("snowy",80,Aspect.ORDER,false,0.0F),
            new Type("cold",80,Aspect.ORDER,false,0.0F),
            new Type("frozen",100,Aspect.ORDER,false,0.0F),
            new Type("mushroom",140,Aspect.ORDER,false,0.0F),
            new Type("coniferous",100,Aspect.EARTH,true,0.2F),
            new Type("forest",120,Aspect.EARTH,true,1.0F),
            new Type("sandy",80,Aspect.EARTH,false,0.0F),
            new Type("beach",80,Aspect.EARTH,false,0.0F),
            new Type("savanna",80,Aspect.AIR,true,0.2F),
            new Type("mountain",100,Aspect.AIR,false,0.0F),
            new Type("hills",120,Aspect.AIR,false,0.0F),
            new Type("plains",80,Aspect.AIR,true,0.2F),
            new Type("dry",80,Aspect.ENTROPY,false,0.0F),
            new Type("sparse",80,Aspect.ENTROPY,false,0.0F),
            new Type("swamp",120,Aspect.ENTROPY,true,0.2F),
            new Type("wasteland",80,Aspect.ENTROPY,false,0.0F),
            new Type("jungle",100,Aspect.PLANT,false,0.0F),
            new Type("lush",100,Aspect.PLANT,true,0.5F),
            new Type("magical",100,null,true,1.0F),
            new Type("end",80,Aspect.VOID,false,0.0F),
            new Type("spooky",80,Aspect.SOUL,false,0.0F),
            new Type("dead",50,Aspect.DEATH,false,0.0F));

    private OriginalBiomeTypes() {}

    public static List<Type> all() { return TYPES; }
}
