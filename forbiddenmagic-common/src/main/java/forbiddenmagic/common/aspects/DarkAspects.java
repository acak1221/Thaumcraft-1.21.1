package forbiddenmagic.common.aspects;

import forbiddenmagic.common.ForbiddenMagic;
import forbiddenmagic.common.config.ForbiddenMagicConfig;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.aspects.Aspect;

/**
 * Original DarkAspects.initAspects() (fox.spiteful.forbidden.DarkAspects, decompiled from the
 * 0.575 oracle jar). Six new sin aspects plus a seventh (Luxuria) gated by the original's own
 * {@code Config.noLust} toggle (default false, i.e. enabled) &mdash; ported verbatim here as
 * {@link ForbiddenMagicConfig#noLust}.
 *
 * <p>The original's "Greed" and "Taint" shards deliberately reuse the base TC4 aspects
 * {@code Aspect.GREED} (lucrum) and {@code Aspect.TAINT} (vitium); the original registers no
 * seventh/eighth custom aspect for them, so none is added here either &mdash; see
 * {@link forbiddenmagic.common.items.ForbiddenMagicAspectTags}.</p>
 *
 * <p>Colors, components and blend mode are copied from the decompiled original field for field;
 * they are the original's raw render values, not reconstructed from the item's lang-file name.</p>
 */
public final class DarkAspects {
    public static Aspect NETHER;
    public static Aspect WRATH;
    public static Aspect ENVY;
    public static Aspect GLUTTONY;
    public static Aspect PRIDE;
    public static Aspect LUST;
    public static Aspect SLOTH;

    private DarkAspects() {}

    public static synchronized void initAspects() {
        if (NETHER != null) return; // Aspect's constructor throws on a re-registered tag; guard re-entry.

        if (!ForbiddenMagicConfig.noLust) {
            LUST = new Aspect("luxuria", 16761294, new Aspect[]{Aspect.FLESH, Aspect.HUNGER}, aspectTexture("luxuria"), 1);
        }

        NETHER = new Aspect("infernus", 16711680, new Aspect[]{Aspect.FIRE, Aspect.MAGIC}, aspectTexture("infernus"), 771);
        PRIDE = new Aspect("superbia", 9845247, new Aspect[]{Aspect.FLIGHT, Aspect.VOID}, aspectTexture("superbia"), 1);
        GLUTTONY = new Aspect("gula", 13999174, new Aspect[]{Aspect.HUNGER, Aspect.VOID}, aspectTexture("gula"), 1);
        ENVY = new Aspect("invidia", 47616, new Aspect[]{Aspect.SENSES, Aspect.HUNGER}, aspectTexture("invidia"), 1);
        SLOTH = new Aspect("desidia", 7237230, new Aspect[]{Aspect.TRAP, Aspect.SOUL}, aspectTexture("desidia"), 771);
        WRATH = new Aspect("ira", 8848388, new Aspect[]{Aspect.WEAPON, Aspect.FIRE}, aspectTexture("ira"), 771);
    }

    /** Original DarkAspects.addAspects() lines porting object-tag registrations that depend on
     * these aspects existing first; split into {@link forbiddenmagic.common.items.ForbiddenMagicAspectTags}
     * so item registration (which must also run between initAspects and addAspects) stays in items/. */
    public static void addAspects() {
        // Intentionally empty: the vanilla-item tag additions (glowstone, nether wart, diamond,
        // leather, etc. - original lines 44-131) need their 1.7.10 obfuscated field references
        // resolved against a verified SRG/MCP mapping table before porting; not done yet.
        // See docs/addons/forbidden-magic/STATUS.md "Deferred in checkpoint 1".
    }

    private static ResourceLocation aspectTexture(String tag) {
        return ResourceLocation.fromNamespaceAndPath(ForbiddenMagic.MODID, "textures/aspects/" + tag + ".png");
    }
}
