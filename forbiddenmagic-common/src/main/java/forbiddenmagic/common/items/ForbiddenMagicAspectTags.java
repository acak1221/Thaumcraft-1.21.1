package forbiddenmagic.common.items;

import forbiddenmagic.common.aspects.DarkAspects;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * Original DarkAspects.addAspects() lines 185-203 (the FM-item self-tags only; the vanilla-item
 * lines are deferred, see {@link DarkAspects#addAspects()}). Amounts/components are copied from
 * the decompiled original verbatim, remapped onto the flattened item ids of
 * {@link ForbiddenMagicItems}.
 *
 * <p>The {@code *_TAGS} constants are deliberately plain data (no {@code Item}/registry
 * dependency): building the real {@code Item} each one belongs to requires a live Fabric/NeoForge
 * Architectury platform (see {@link ForbiddenMagicItems} and
 * {@link forbiddenmagic.common.ForbiddenMagic} class docs), which a common-module JUnit run does
 * not have; the data itself does not, and is tested directly.</p>
 */
public final class ForbiddenMagicAspectTags {
    static {
        // The *_TAGS constants below reference DarkAspects fields at class-init time; guarantee
        // they exist regardless of caller discipline (DarkAspects.initAspects() is itself
        // idempotent/re-entry-guarded, see its own doc).
        DarkAspects.initAspects();
    }

    public static final AspectList WRATH_SHARD_TAGS = new AspectList().add(DarkAspects.NETHER, 1).add(DarkAspects.WRATH, 2).add(Aspect.CRYSTAL, 1);
    public static final AspectList ENVY_SHARD_TAGS = new AspectList().add(DarkAspects.NETHER, 1).add(DarkAspects.ENVY, 2).add(Aspect.CRYSTAL, 1);
    // Original line 187: no NETHER component here, unlike every other slot.
    public static final AspectList TAINT_SHARD_TAGS = new AspectList().add(Aspect.TAINT, 3).add(Aspect.CRYSTAL, 1);
    public static final AspectList PRIDE_SHARD_TAGS = new AspectList().add(DarkAspects.NETHER, 1).add(DarkAspects.PRIDE, 2).add(Aspect.CRYSTAL, 1);
    public static final AspectList LUST_SHARD_TAGS = new AspectList().add(DarkAspects.NETHER, 1).add(DarkAspects.LUST, 2).add(Aspect.CRYSTAL, 1);
    // Original line 192 (the !noLust-else branch): this is what the original tags the SAME slot
    // with when Config.noLust is true. Both items always carry their tag here; see ForbiddenMagicItems.
    public static final AspectList DESPAIR_SHARD_TAGS = new AspectList().add(Aspect.VOID, 1);
    public static final AspectList SLOTH_SHARD_TAGS = new AspectList().add(DarkAspects.NETHER, 1).add(DarkAspects.SLOTH, 2).add(Aspect.CRYSTAL, 1);
    public static final AspectList GREED_SHARD_TAGS = new AspectList().add(DarkAspects.NETHER, 1).add(Aspect.GREED, 2).add(Aspect.CRYSTAL, 1);
    public static final AspectList GLUTTONY_SHARD_TAGS = new AspectList().add(DarkAspects.NETHER, 1).add(DarkAspects.GLUTTONY, 2).add(Aspect.CRYSTAL, 1);
    public static final AspectList EMERALD_FRAGMENT_TAGS = new AspectList().add(Aspect.CRYSTAL, 1);
    public static final AspectList SHADOW_INK_TAGS = new AspectList().add(Aspect.DARKNESS, 1);

    private static boolean registered;

    private ForbiddenMagicAspectTags() {}

    /** Attaches the tags above to the real registered items. Needs {@link ForbiddenMagicItems#register()}
     * to have already run on a live platform; not callable from a plain common-module JUnit test. */
    public static void register() {
        if (registered) return;
        registered = true;

        ThaumcraftApi.registerObjectTag(new ItemStack(ForbiddenMagicItems.WRATH_SHARD.get()), WRATH_SHARD_TAGS);
        ThaumcraftApi.registerObjectTag(new ItemStack(ForbiddenMagicItems.ENVY_SHARD.get()), ENVY_SHARD_TAGS);
        ThaumcraftApi.registerObjectTag(new ItemStack(ForbiddenMagicItems.TAINT_SHARD.get()), TAINT_SHARD_TAGS);
        ThaumcraftApi.registerObjectTag(new ItemStack(ForbiddenMagicItems.PRIDE_SHARD.get()), PRIDE_SHARD_TAGS);
        if (DarkAspects.LUST != null) {
            ThaumcraftApi.registerObjectTag(new ItemStack(ForbiddenMagicItems.LUST_SHARD.get()), LUST_SHARD_TAGS);
        }
        ThaumcraftApi.registerObjectTag(new ItemStack(ForbiddenMagicItems.DESPAIR_SHARD.get()), DESPAIR_SHARD_TAGS);
        ThaumcraftApi.registerObjectTag(new ItemStack(ForbiddenMagicItems.SLOTH_SHARD.get()), SLOTH_SHARD_TAGS);
        ThaumcraftApi.registerObjectTag(new ItemStack(ForbiddenMagicItems.GREED_SHARD.get()), GREED_SHARD_TAGS);
        ThaumcraftApi.registerObjectTag(new ItemStack(ForbiddenMagicItems.GLUTTONY_SHARD.get()), GLUTTONY_SHARD_TAGS);
        ThaumcraftApi.registerObjectTag(new ItemStack(ForbiddenMagicItems.EMERALD_FRAGMENT.get()), EMERALD_FRAGMENT_TAGS);
        ThaumcraftApi.registerObjectTag(new ItemStack(ForbiddenMagicItems.SHADOW_INK.get()), SHADOW_INK_TAGS);
    }
}
