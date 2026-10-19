package thaumcraft.api.aspects;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Objects;

/** The 48 TC4 aspects; amounts and centivis are stored by their consumers. */
public class Aspect {
    public static final LinkedHashMap<String, Aspect> aspects = new LinkedHashMap<>();

    public static final Aspect AIR = new Aspect("aer", 0xffff7e, "e", 1);
    public static final Aspect EARTH = new Aspect("terra", 0x56c000, "2", 1);
    public static final Aspect FIRE = new Aspect("ignis", 0xff5a01, "c", 1);
    public static final Aspect WATER = new Aspect("aqua", 0x3cd4fc, "3", 1);
    public static final Aspect ORDER = new Aspect("ordo", 0xd5d4ec, "7", 1);
    public static final Aspect ENTROPY = new Aspect("perditio", 0x404040, "8", 771);
    public static final Aspect VOID = new Aspect("vacuos", 0x888888, new Aspect[]{AIR, ENTROPY}, 771);
    public static final Aspect LIGHT = new Aspect("lux", 0xfff663, new Aspect[]{AIR, FIRE});
    public static final Aspect WEATHER = new Aspect("tempestas", 0xffffff, new Aspect[]{AIR, WATER});
    public static final Aspect MOTION = new Aspect("motus", 0xcdccf4, new Aspect[]{AIR, ORDER});
    public static final Aspect COLD = new Aspect("gelum", 0xe1ffff, new Aspect[]{FIRE, ENTROPY});
    public static final Aspect CRYSTAL = new Aspect("vitreus", 0x80ffff, new Aspect[]{EARTH, ORDER});
    public static final Aspect LIFE = new Aspect("victus", 0xde0005, new Aspect[]{WATER, EARTH});
    public static final Aspect POISON = new Aspect("venenum", 0x89f000, new Aspect[]{WATER, ENTROPY});
    public static final Aspect ENERGY = new Aspect("potentia", 0xc0ffff, new Aspect[]{ORDER, FIRE});
    public static final Aspect EXCHANGE = new Aspect("permutatio", 0x578357, new Aspect[]{ENTROPY, ORDER});
    public static final Aspect METAL = new Aspect("metallum", 0xb5b5cd, new Aspect[]{EARTH, CRYSTAL});
    public static final Aspect DEATH = new Aspect("mortuus", 0x887788, new Aspect[]{LIFE, ENTROPY});
    public static final Aspect FLIGHT = new Aspect("volatus", 0xe7e7d7, new Aspect[]{AIR, MOTION});
    public static final Aspect DARKNESS = new Aspect("tenebrae", 0x222222, new Aspect[]{VOID, LIGHT});
    public static final Aspect SOUL = new Aspect("spiritus", 0xebebfb, new Aspect[]{LIFE, DEATH});
    public static final Aspect HEAL = new Aspect("sano", 0xff2f34, new Aspect[]{LIFE, ORDER});
    public static final Aspect TRAVEL = new Aspect("iter", 0xe0585b, new Aspect[]{MOTION, EARTH});
    public static final Aspect ELDRITCH = new Aspect("alienis", 0x805080, new Aspect[]{VOID, DARKNESS});
    public static final Aspect MAGIC = new Aspect("praecantatio", 0x9700c0, new Aspect[]{VOID, ENERGY});
    public static final Aspect AURA = new Aspect("auram", 0xffc0ff, new Aspect[]{MAGIC, AIR});
    public static final Aspect TAINT = new Aspect("vitium", 0x800080, new Aspect[]{MAGIC, ENTROPY});
    public static final Aspect SLIME = new Aspect("limus", 0x01f800, new Aspect[]{LIFE, WATER});
    public static final Aspect PLANT = new Aspect("herba", 0x01ac00, new Aspect[]{LIFE, EARTH});
    public static final Aspect TREE = new Aspect("arbor", 0x876531, new Aspect[]{AIR, PLANT});
    public static final Aspect BEAST = new Aspect("bestia", 0x9f6409, new Aspect[]{MOTION, LIFE});
    public static final Aspect FLESH = new Aspect("corpus", 0xee478d, new Aspect[]{DEATH, BEAST});
    public static final Aspect UNDEAD = new Aspect("exanimis", 0x3a4000, new Aspect[]{MOTION, DEATH});
    public static final Aspect MIND = new Aspect("cognitio", 0xffc2b3, new Aspect[]{FIRE, SOUL});
    public static final Aspect SENSES = new Aspect("sensus", 0x0fd9ff, new Aspect[]{AIR, SOUL});
    public static final Aspect MAN = new Aspect("humanus", 0xffd7c0, new Aspect[]{BEAST, MIND});
    public static final Aspect CROP = new Aspect("messis", 0xe1b371, new Aspect[]{PLANT, MAN});
    public static final Aspect MINE = new Aspect("perfodio", 0xdcd2d8, new Aspect[]{MAN, EARTH});
    public static final Aspect TOOL = new Aspect("instrumentum", 0x4040ee, new Aspect[]{MAN, ORDER});
    public static final Aspect HARVEST = new Aspect("meto", 0xeead82, new Aspect[]{CROP, TOOL});
    public static final Aspect WEAPON = new Aspect("telum", 0xc05050, new Aspect[]{TOOL, FIRE});
    public static final Aspect ARMOR = new Aspect("tutamen", 0x00c0c0, new Aspect[]{TOOL, EARTH});
    public static final Aspect HUNGER = new Aspect("fames", 0x9a0305, new Aspect[]{LIFE, VOID});
    public static final Aspect GREED = new Aspect("lucrum", 0xe6be44, new Aspect[]{MAN, HUNGER});
    public static final Aspect CRAFT = new Aspect("fabrico", 0x809d80, new Aspect[]{MAN, TOOL});
    public static final Aspect CLOTH = new Aspect("pannus", 0xeaeac2, new Aspect[]{TOOL, BEAST});
    public static final Aspect MECHANISM = new Aspect("machina", 0x8080a0, new Aspect[]{MOTION, TOOL});
    public static final Aspect TRAP = new Aspect("vinculum", 0x9a8080, new Aspect[]{MOTION, ENTROPY});

    private final String tag;
    private final int color;
    private final Aspect[] components;
    private final ResourceLocation image;
    private final int blend;
    private String chatcolor = "f";

    public Aspect(String tag, int color, Aspect[] components, ResourceLocation image, int blend) {
        this.tag = Objects.requireNonNull(tag, "tag");
        this.color = color;
        this.components = components == null ? null : components.clone();
        if (components != null && (components.length != 2 || components[0] == null || components[1] == null)) {
            throw new IllegalArgumentException("Compound aspects require two components");
        }
        this.image = Objects.requireNonNull(image, "image");
        this.blend = blend;
        if (aspects.putIfAbsent(tag, this) != null) {
            throw new IllegalArgumentException("Aspect already registered: " + tag);
        }
    }

    public Aspect(String tag, int color, Aspect[] components, int blend) {
        this(tag, color, components,
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/aspects/" + tag.toLowerCase(Locale.ROOT) + ".png"), blend);
    }

    public Aspect(String tag, int color, Aspect[] components) {
        this(tag, color, components, 1);
    }

    public Aspect(String tag, int color, String chatcolor, int blend) {
        this(tag, color, (Aspect[]) null, blend);
        this.chatcolor = chatcolor;
    }

    public String getTag() { return this.tag; }
    public int getColor() { return this.color; }
    public String getName() { return this.tag.substring(0, 1).toUpperCase(Locale.ROOT) + this.tag.substring(1); }
    public Aspect[] getComponents() { return this.components == null ? null : this.components.clone(); }
    public ResourceLocation getImage() { return this.image; }
    public int getBlend() { return this.blend; }
    public String getChatcolor() { return this.chatcolor; }
    public boolean isPrimal() { return this.components == null; }

    public static Aspect getAspect(String tag) { return aspects.get(tag); }

    public static ArrayList<Aspect> getPrimalAspects() {
        ArrayList<Aspect> result = new ArrayList<>();
        for (Aspect aspect : aspects.values()) if (aspect.isPrimal()) result.add(aspect);
        return result;
    }

    public static ArrayList<Aspect> getCompoundAspects() {
        ArrayList<Aspect> result = new ArrayList<>();
        for (Aspect aspect : aspects.values()) if (!aspect.isPrimal()) result.add(aspect);
        return result;
    }

    public static Aspect getCombinationResult(Aspect first, Aspect second) {
        if (first == null || second == null) return null;
        for (Aspect aspect : aspects.values()) {
            Aspect[] parents = aspect.components;
            if (parents != null && ((parents[0] == first && parents[1] == second)
                    || (parents[1] == first && parents[0] == second))) return aspect;
        }
        return null;
    }
}
