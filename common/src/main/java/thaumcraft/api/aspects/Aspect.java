package thaumcraft.api.aspects;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Objects;

/**
 * The aspect registry of TC4: the 48 original aspects plus every aspect an add-on registers.
 * Amounts and centivis are stored by their consumers.
 *
 * <h2>Add-on aspects</h2>
 *
 * <p>An add-on registers an aspect exactly as it did on 1.7.10, by constructing it once:</p>
 * <pre>{@code
 * public static final Aspect TEMPUS = new Aspect("tempus", 0xB68CFF, new Aspect[]{Aspect.VOID, Aspect.ORDER},
 *         ResourceLocation.fromNamespaceAndPath("myaddon", "textures/aspects/tempus.png"), 1);
 * ThaumcraftApi.registerObjectTag(new ItemStack(Items.CLOCK), new AspectList().add(TEMPUS, 4));
 * }</pre>
 * <p>The constructor is the whole registration: the aspect is then known to the thaumometer, the
 * research table, the research notes, essentia storage and transport, the crucible, the alchemical
 * furnace, the centrifuge, phials, crystals, wisps, the creative tab and the EMI/JEI/Jade
 * integrations, exactly like an original aspect. The contract:</p>
 * <ul>
 * <li><b>When.</b> During mod initialisation, on both physical sides (common code, never only the
 * client or only the server): a {@code static final} field of a class your initialiser loads (Fabric
 * {@code ModInitializer#onInitialize}; NeoForge mod constructor or
 * {@code FMLCommonSetupEvent#enqueueWork}). The order relative to Thaumcraft's own initialiser does not
 * matter: the 48 original aspects are registered by the static initialiser of this class, so they always
 * precede add-on aspects, and every consumer reads this registry when it needs it. Do not register
 * aspects after a world or server started (data pack recipes, open menus and saved data are validated
 * against the registry at that point) and never remove one: the registry has no unregistration.</li>
 * <li><b>Tag.</b> Unique, non-empty, lowercase, made of {@code [a-z0-9_.-]}; a duplicate throws
 * {@link IllegalArgumentException}. The tag is the identity of the aspect everywhere: saved block
 * entities, item components, player knowledge, research notes, recipes and every packet store the tag
 * string, and EMI/JEI use {@code thaumcraft:<tag>} as identifier (a tag that is not a valid resource
 * path is accepted, as in the original, but logged, because those identifiers then fail). Changing a
 * tag later orphans existing worlds.</li>
 * <li><b>Components.</b> A compound has exactly two registered parents; any other array length makes
 * the aspect primal ({@link #isPrimal()}, original rule). The research table and the hex notes link an
 * aspect to its two parents; {@link #getCombinationResult} answers the first registered aspect with that
 * pair, so a pair that an original aspect already uses never yields the add-on aspect. An aspect
 * without components ({@code null}) is a primal: like the six original primals it is given to a player
 * on the first join (original ResearchManager.loadPlayerData), but wand, sceptre and amulet vis, vis
 * relays, shards, crystal clusters and infused ores of this port exist for the six original primals
 * only.</li>
 * <li><b>Texture.</b> {@link #getImage()} is bound directly as a texture, not as an atlas sprite: ship
 * {@code assets/<namespace>/textures/aspects/<name>.png}, a square image (the originals are 32x32
 * RGBA) drawn white or grey on transparency, because Thaumcraft tints it with {@link #getColor()}
 * (RGB {@code 0xRRGGBB}, no alpha). Use the five-argument constructor with your own namespace; the
 * three- and four-argument forms derive {@code thaumcraft:textures/aspects/<lowercase tag>.png},
 * which only the original aspects ship.</li>
 * <li><b>Blend.</b> {@code 1} (additive, the default) or {@code 771} (alpha blended, the dark
 * aspects), used by nodes and aspect orbs; other values render as additive.</li>
 * <li><b>Chat colour.</b> One legacy formatting code character (for example {@code "5"}), used for
 * primal vis costs of foci; the compound constructors default it to {@code "f"}.</li>
 * <li><b>Language keys.</b> {@link #getName()} is the capitalised tag and is not translated, as in the
 * original. Add {@code tc.aspect.<tag>} (the meaning shown under the name, for example
 * {@code "tc.aspect.tempus": "Time"}) and {@code tc.aspect.help.<tag>} (a short lowercase noun used
 * by the thaumometer when the aspect is a missing parent: "To understand this you need to study
 * %s.") to your own {@code assets/<namespace>/lang/en_us.json}.</li>
 * <li><b>Networking.</b> Client and server must register the same aspects: a client that lacks one
 * cannot decode the phials, crystals, wisp essence, jar labels, recipes and research notes that name
 * it and is disconnected. The essentia tube vent effect and the deconstruction table menu send a
 * position in the registry order, and the thaumatorium menu a position in the tag-sorted registry, so
 * register the same aspects in a deterministic order on both sides.</li>
 * <li><b>Threads.</b> Registration synchronises on {@link #aspects}, so parallel mod construction
 * (NeoForge) cannot corrupt the map, and {@link #getPrimalAspects()}, {@link #getCompoundAspects()} and
 * {@link #getCombinationResult} read it under the same lock. Code that iterates {@link #aspects}
 * itself while other mods may still be constructing must synchronise on it too.</li>
 * </ul>
 */
public class Aspect {
    /**
     * The live registry, keyed by tag, in registration order: the 48 original aspects first, then the
     * add-on aspects. Writes synchronise on this map; see the class documentation.
     */
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

    private String tag;
    private final int color;
    private Aspect[] components;
    private final ResourceLocation image;
    private int blend;
    private String chatcolor = "f";

    /**
     * Original Aspect:74-84. The original accepts any component array: a compound aspect with
     * three components is legal and is then reported as primal by {@link #isPrimal()}.
     * Null/empty tags throw IllegalArgumentException; null images throw NPE and duplicate tags throw IAE before replacement.
     */
    public Aspect(String tag, int color, Aspect[] components, ResourceLocation image, int blend) {
        this.tag = requireTag(tag);
        this.color = color;
        this.components = components == null ? null : components.clone();
        this.image = Objects.requireNonNull(image, "image");
        this.blend = blend;
        synchronized (aspects) {
            if (aspects.putIfAbsent(tag, this) != null) {
                throw new IllegalArgumentException("Aspect already registered: " + tag);
            }
        }
        if (!ResourceLocation.isValidPath(tag)) {
            org.slf4j.LoggerFactory.getLogger("Thaumcraft").warn(
                    "Aspect tag '{}' is not a lowercase resource path; EMI/JEI identifiers of this aspect will fail", tag);
        }
    }

    /** Registers a new aspect with a derived texture; RGB color/blend are raw render values, components are copied (null allowed), null tag throws NPE and duplicate tags throw IAE. */
    public Aspect(String tag, int color, Aspect[] components, int blend) {
        this(tag, color, components,
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/aspects/" + requireTag(tag).toLowerCase(Locale.ROOT) + ".png"), blend);
    }

    /** Registers an aspect as {@link #Aspect(String, int, Aspect[], int)} with blend 1; component entries are not validated. */
    public Aspect(String tag, int color, Aspect[] components) {
        this(tag, color, components, 1);
    }

    /** Registers a primal aspect with raw RGB/blend values and nullable chat color; null tag throws NPE and a duplicate tag throws IAE. */
    public Aspect(String tag, int color, String chatcolor, int blend) {
        this(tag, color, (Aspect[]) null, blend);
        this.chatcolor = chatcolor;
    }

    /** Returns the mutable display tag, possibly null after setTag; does not change registry keys. */
    /** Rejects a null or empty tag with IllegalArgumentException; the tag is part of the identity. */
    private static String requireTag(String tag) {
        if (tag == null || tag.isEmpty()) throw new IllegalArgumentException("tag must not be null or empty");
        return tag;
    }

    public String getTag() { return this.tag; }
    /** Returns the stored RGB integer without changing state. */
    public int getColor() { return this.color; }
    /**
     * Original Aspect.getName: {@code WordUtils.capitalizeFully(tag)}, the tag in lower case with the
     * first letter of each whitespace-separated word in title case; a null or empty tag (after
     * {@link #setTag}) is returned unchanged. This is not a translation lookup. The lower casing uses
     * {@link Locale#ROOT}, not the default locale of the original, so a Turkish locale cannot change a
     * name.
     */
    public String getName() {
        if (this.tag == null || this.tag.isEmpty()) return this.tag;
        char[] name = this.tag.toLowerCase(Locale.ROOT).toCharArray();
        boolean wordStart = true;
        for (int index = 0; index < name.length; index++) {
            if (Character.isWhitespace(name[index])) {
                wordStart = true;
            } else if (wordStart) {
                name[index] = Character.toTitleCase(name[index]);
                wordStart = false;
            }
        }
        return new String(name);
    }
    /** Returns a new component array sharing Aspect objects, or null when no array is stored. */
    public Aspect[] getComponents() { return this.components == null ? null : this.components.clone(); }
    /** Returns the non-null texture identifier without changing state. */
    public ResourceLocation getImage() { return this.image; }
    /** Returns the raw render blend value without changing state. */
    public int getBlend() { return this.blend; }
    /** Returns the stored chat-color string, which constructors/setters may leave null. */
    public String getChatcolor() { return this.chatcolor; }

    /**
     * Original Aspect:108 - the localised description of the aspect, the translation of
     * {@code tc.aspect.<tag>}.
     */
    public String getLocalizedDescription() { return Component.translatable("tc.aspect." + this.tag).getString(); }

    /**
     * Original Aspect:145 - an aspect is primal when it has no components or when its component
     * array does not hold exactly two parents.
     */
    public boolean isPrimal() { return this.components == null || this.components.length != 2; }

    /** Accepts null/empty tags without validation or registry re-keying; subsequent {@link #getName()} calls fail for them. */
    public void setTag(String tag) { this.tag = tag; }

    /** Original Aspect:124 - the components of an aspect can be retuned by an add-on. */
    public void setComponents(Aspect[] components) { this.components = components == null ? null : components.clone(); }

    /** Original Aspect:140 - the blend of an aspect can be retuned by an add-on. */
    public void setBlend(int blend) { this.blend = blend; }

    /** Stores any chat colour, including null or malformed formatting strings, without validation. */
    public void setChatcolor(String chatcolor) { this.chatcolor = chatcolor; }

    /** Looks up the live registry by key; unknown/null keys return null unless the public map was manually populated at that key. */
    public static Aspect getAspect(String tag) { return aspects.get(tag); }

    /**
     * Returns a new registry-ordered list of primal aspects, read from the live registry (add-on
     * primals included, after the six original ones); elements remain shared mutable identities.
     */
    public static ArrayList<Aspect> getPrimalAspects() {
        ArrayList<Aspect> result = new ArrayList<>();
        synchronized (aspects) {
            for (Aspect aspect : aspects.values()) if (aspect.isPrimal()) result.add(aspect);
        }
        return result;
    }

    /**
     * Returns a new registry-ordered list of nonprimal aspects, read from the live registry (add-on
     * compounds included); elements remain shared mutable identities.
     */
    public static ArrayList<Aspect> getCompoundAspects() {
        ArrayList<Aspect> result = new ArrayList<>();
        synchronized (aspects) {
            for (Aspect aspect : aspects.values()) if (!aspect.isPrimal()) result.add(aspect);
        }
        return result;
    }

    /**
     * Original ResearchManager.getCombinationResult: the first registry aspect whose components are the
     * two identities in either order, add-on aspects included, or null for null inputs/no match.
     * Malformed short component arrays can throw.
     */
    public static Aspect getCombinationResult(Aspect first, Aspect second) {
        if (first == null || second == null) return null;
        synchronized (aspects) {
            for (Aspect aspect : aspects.values()) {
                Aspect[] parents = aspect.components;
                if (parents != null && ((parents[0] == first && parents[1] == second)
                        || (parents[1] == first && parents[0] == second))) return aspect;
            }
        }
        return null;
    }
}
