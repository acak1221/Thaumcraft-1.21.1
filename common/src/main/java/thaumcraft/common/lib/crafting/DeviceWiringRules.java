package thaumcraft.common.lib.crafting;

/**
 * The pure decisions of the device glue: the magic box menu, the hand mirror
 * link and the item filter block.
 *
 * <p>The menu classes themselves need a server, so every branch they take
 * before they touch the world is pulled into this class and unit tested.
 * Sources are the original 4.2.3.5 classes: {@code BlockMagicBox},
 * {@code ContainerMagicBox}, {@code ItemHandMirror}, {@code ContainerHandMirror}
 * and {@code EntityItemGrate}.</p>
 */
public final class DeviceWiringRules {
    private DeviceWiringRules() {}

    // ------------------------------------------------------------------ magic box

    /**
     * Oracle ContainerMagicBox:28 - the menu of a magic box carries the size
     * of the whole box chain, because the quick move boundary uses it.
     */
    public static int magicBoxMenuSize(int chainSize) { return Math.max(0, chainSize); }

    /**
     * Oracle ContainerMagicBox:38-44 - the menu binds three rows of the chain
     * whatever the chain size is, so a linked box shows the first twenty seven
     * slots only.
     */
    public static int magicBoxBoundSlots() { return DeviceContainerRules.MAGIC_BOX_SLOTS; }

    // --------------------------------------------------------------- hand mirror

    /** What the hand mirror found when it looked for the mirror it is linked to. */
    public enum HandMirrorTarget {
        /** Oracle ItemHandMirror:158 - the hand mirror carries no link at all. */
        NO_LINK,
        /** Oracle ItemHandMirror:162-165 - the dimension of the link does not exist. */
        NO_WORLD,
        /** Oracle ItemHandMirror:167-173 - the mirror is gone, the link dies with it. */
        NO_MIRROR,
        /** Oracle ItemHandMirror:174-185 - the mirror is there and the item can travel. */
        MIRROR
    }

    /**
     * The decision the hand mirror takes when it looks for its mirror. The
     * world of the link is asked first, the mirror inside it second, exactly as
     * the original does.
     */
    public static HandMirrorTarget handMirrorTarget(boolean linked, boolean worldFound, boolean mirrorFound) {
        if (!linked) return HandMirrorTarget.NO_LINK;
        if (!worldFound) return HandMirrorTarget.NO_WORLD;
        if (!mirrorFound) return HandMirrorTarget.NO_MIRROR;
        return HandMirrorTarget.MIRROR;
    }

    /**
     * Oracle ItemHandMirror:123-143 - the screen of the hand mirror only opens
     * for a mirror that is still there.
     */
    public static boolean handMirrorOpensScreen(HandMirrorTarget target) { return target == HandMirrorTarget.MIRROR; }

    /**
     * Oracle ItemHandMirror:167-173 - a mirror that is gone clears the link of
     * the hand mirror, so the hand mirror never keeps a dead link.
     */
    public static boolean handMirrorClearsLink(HandMirrorTarget target) { return target == HandMirrorTarget.NO_MIRROR; }

    /**
     * Oracle ContainerHandMirror:64-65 and ItemHandMirror:157-188 - only a
     * mirror that is really there carries the item away.
     */
    public static boolean handMirrorTransports(HandMirrorTarget target) { return target == HandMirrorTarget.MIRROR; }

    // -------------------------------------------------------------- item filter

    /** Original BlockMetalDevice metadata five: the open item filter. */
    public static final int ITEM_FILTER_OPEN = 5;
    /** Original BlockMetalDevice metadata six: the closed item filter. */
    public static final int ITEM_FILTER_CLOSED = 6;

    /**
     * Oracle EntityItemGrate:28-36 - the item filter is the metal device of
     * the fifth and the sixth metadata. The native tree carries that filter
     * twice: as the dedicated item grate block and as the two metal device
     * variants, so both are recognised.
     */
    public static boolean itemFilterBlock(boolean dedicatedGrate, boolean metalDevice, int variant) {
        if (dedicatedGrate) return true;
        return metalDevice && (variant == ITEM_FILTER_OPEN || variant == ITEM_FILTER_CLOSED);
    }

    // -------------------------------------------------------------- mirror link

    /** What a right click with a mirror item does. */
    public enum MirrorLinkAction {
        /** Oracle BlockMirrorItem:95-110 - the item is bound to the mirror it was used on. */
        BIND_ITEM,
        /** Oracle BlockMirrorItem:112 - the mirror already has a valid partner. */
        ALREADY_LINKED,
        /** The block is no mirror the item can be bound to. */
        UNSUPPORTED
    }

    /**
     * Oracle BlockMirrorItem:86-139 - a mirror item that is used on a mirror
     * without a valid partner binds itself to that mirror; a mirror with a
     * valid partner refuses a second one.
     */
    public static MirrorLinkAction mirrorItemLink(boolean mirrorBlock, boolean itemMirror, boolean partnerLinked) {
        if (!mirrorBlock || !itemMirror) return MirrorLinkAction.UNSUPPORTED;
        return partnerLinked ? MirrorLinkAction.ALREADY_LINKED : MirrorLinkAction.BIND_ITEM;
    }
}
