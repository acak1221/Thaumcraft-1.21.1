package thaumcraft.common.items.relics;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * The Sanity Checker (original ItemSanityChecker), a relic of uncommon rarity.
 *
 * <p>Oracle {@code thaumcraft/common/items/relics/ItemSanityChecker.java:27-55} (CFR 0.152
 * decompilation of {@code Thaumcraft-1.7.10-4.2.3.5.jar}): the item of the 4.2.3.5 carries its
 * stack limit of one, its uncommon rarity and <b>nothing else</b> - it has no use behaviour at
 * all, so right clicking it does nothing.</p>
 *
 * <p>Forced 1.21.1 change: none. The reading of the warp that the 1.12 reference of a later
 * Thaumcraft carries (the {@code src/main/java/thaumcraft/common/items/relics/ItemSanityChecker.java}
 * of the repository) is <b>not</b> part of the 4.2.3.5 behaviour and was removed from the native
 * item, so the relic of the native port is as pointless as the relic of the original.</p>
 */
public final class ItemSanityChecker extends Item {
    public ItemSanityChecker(Properties properties) {
        // Oracle ItemSanityChecker:31-36 - the stack limit of one and the uncommon rarity.
        super(properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    }
}
