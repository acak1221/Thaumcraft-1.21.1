package thaumcraft.api.internal;

import java.util.ArrayList;
import net.minecraft.world.item.ItemStack;

/** Original public, mutable loot entries and lists; native weighted selection lives in LootTables. */
public class WeightedRandomLoot {
    public ItemStack item;
    public int itemWeight;
    public static ArrayList<WeightedRandomLoot> lootBagCommon = new ArrayList<>();
    public static ArrayList<WeightedRandomLoot> lootBagUncommon = new ArrayList<>();
    public static ArrayList<WeightedRandomLoot> lootBagRare = new ArrayList<>();

    /** Creates an unregistered entry retaining the nullable stack and unvalidated dimensionless relative weight; does not append to any loot list. */
    public WeightedRandomLoot(ItemStack stack, int weight) { item = stack; itemWeight = weight; }
}
