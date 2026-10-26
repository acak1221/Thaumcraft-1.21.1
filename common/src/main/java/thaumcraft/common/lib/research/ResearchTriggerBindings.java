package thaumcraft.common.lib.research;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Closed bindings for naturally available original item triggers. Missing late content remains explicitly unbound. */
public final class ResearchTriggerBindings {
    /**
     * The native identity of the original item trigger of a research entry.
     *
     * <p>The order is the order of the original {@code setItemTriggers} call, so that the public
     * {@code getItemTriggers()} accessor of the original returns the same sequence.</p>
     *
     * <p>Every row is the native identity of one original expression of the corpus
     * ({@code common/src/main/resources/data/thaumcraft/tc4/research.json}, the eleven entries
     * that carry {@code item_trigger_expressions}):</p>
     * <ul>
     *   <li>{@code BANNERS} - oracle {@code ConfigResearch:199}
     *   {@code new ItemStack(ConfigBlocks.blockWoodenDevice, 1, 8)}: the original banner is the
     *   wooden device metadata eight, which the native port owns as {@code thaumcraft:banner}
     *   ({@code ThaumcraftEldritchBlocks:375}).</li>
     *   <li>{@code BONEBOW} - oracle {@code ConfigResearch:221}: {@code Items.field_151031_f} is
     *   the 1.7.10 bow and {@code Items.field_151103_aS} is the 1.7.10 bone.</li>
     *   <li>{@code JARBRAIN} - oracle {@code ConfigResearch:235}: {@code itemResource} metadata
     *   three is the quick silver, which the native port owns as {@code thaumcraft:quick_silver}.</li>
     *   <li>{@code MIRROR} - oracle {@code ConfigResearch:213}: the ender pearl and the three
     *   1.7.10 portal blocks ({@code Blocks.field_150427_aO} the nether portal,
     *   {@code Blocks.field_150384_bq} the end portal and {@code Blocks.field_150378_br} the end
     *   portal frame, resolved with the 1.7.10 MCP field names).</li>
     *   <li>{@code OUTERREV} - oracle {@code ConfigResearch:399}: the eldritch block metadata five
     *   (the glyphed eldritch stone) and ten (the runed eldritch trap), which the native port
     *   owns as {@code thaumcraft:eldritch_crust} and {@code thaumcraft:eldritch_spawner}
     *   ({@code EldritchBlockRules:35,40}).</li>
     *   <li>{@code PRIMPEARL} - oracle {@code ConfigResearch:400}: the eldritch object metadata
     *   three, the primordial pearl.</li>
     *   <li>{@code ROD_primal_staff} - oracle {@code ConfigResearch:414}: the primal focus.</li>
     * </ul>
     */
    private static final Map<String, List<String>> ITEMS = Map.of(
            "BANNERS", List.of("thaumcraft:banner"),
            "BONEBOW", List.of("minecraft:bow", "minecraft:bone"),
            "JARBRAIN", List.of("thaumcraft:quick_silver"),
            "MIRROR", List.of("minecraft:ender_pearl", "minecraft:nether_portal", "minecraft:end_portal",
                    "minecraft:end_portal_frame"),
            "OUTERREV", List.of("thaumcraft:eldritch_crust", "thaumcraft:eldritch_spawner"),
            "PRIMPEARL", List.of("thaumcraft:primordial_pearl"),
            // Original ConfigResearch:414 binds the primal staff rod research to the
            // primal focus, which the native port owns as thaumcraft:focus_primal.
            "ROD_primal_staff", List.of("thaumcraft:focus_primal"));
    /**
     * The original tiny clothing entries were bound to one wool block with an unbreakable
     * damage value. 1.21.1 flattened the sixteen wool colours of 1.7.10 into sixteen items,
     * so the native binding accepts every wool of the namespace.
     */
    private static final Set<String> CLOTHING = Set.of("TINYBOWTIE", "TINYFEZ", "TINYGLASSES", "TINYHAT");
    private static final List<String> WOOLS = List.of(
            "minecraft:white_wool", "minecraft:orange_wool", "minecraft:magenta_wool", "minecraft:light_blue_wool",
            "minecraft:yellow_wool", "minecraft:lime_wool", "minecraft:pink_wool", "minecraft:gray_wool",
            "minecraft:light_gray_wool", "minecraft:cyan_wool", "minecraft:purple_wool", "minecraft:blue_wool",
            "minecraft:brown_wool", "minecraft:green_wool", "minecraft:red_wool", "minecraft:black_wool");
    private ResearchTriggerBindings() {}

    /** The native identity of the original item trigger of a research entry. */
    public static Set<String> boundItems(String researchKey) {
        List<String> ids = ITEMS.get(researchKey);
        if (ids != null) return Set.copyOf(ids);
        if (CLOTHING.contains(researchKey)) return Set.copyOf(WOOLS);
        return Set.of();
    }

    /**
     * The executable item stacks of the original item trigger of a research entry.
     *
     * <p>The original registered the triggers with {@code ResearchItem.setItemTriggers}; the native
     * corpus keeps the provenance of the original expressions and the native identity of the bound
     * items here, so the public accessor of the entry can answer them.</p>
     */
    public static ItemStack[] boundStacks(String researchKey) {
        List<String> ids = ITEMS.get(researchKey);
        List<String> wanted = ids != null ? ids : (CLOTHING.contains(researchKey) ? WOOLS : List.of());
        List<ItemStack> stacks = new ArrayList<>();
        for (String id : wanted) {
            ResourceLocation key = ResourceLocation.tryParse(id);
            if (key == null) continue;
            Item item = BuiltInRegistries.ITEM.get(key);
            if (item == null || item == Items.AIR) continue;
            stacks.add(new ItemStack(item));
        }
        return stacks.toArray(new ItemStack[0]);
    }

    public static boolean matches(String researchKey, ResearchClues.Context context) {
        var research = ResearchCatalog.get(researchKey);
        if (research == null) return false;
        if (!ResearchCatalog.isBundled(researchKey)) return AddonResearch.matchesTrigger(researchKey, context);
        if (context.entityName() != null) return research.entityTriggers().contains(context.entityName());
        ItemStack stack = context.item();
        if (stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        boolean identity = ITEMS.getOrDefault(researchKey, List.of()).contains(id.toString())
                || CLOTHING.contains(researchKey) && "minecraft".equals(id.getNamespace()) && id.getPath().endsWith("_wool");
        if (!identity) return false;
        // ConfigItems registers itemResource:3 as nuggetQuicksilver. Its original OreDictionary shortcut
        // accepts the native quicksilver identity before NBT comparison; zombie brains are not this trigger.
        if ("JARBRAIN".equals(researchKey) && id.equals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "quick_silver"))) return true;
        // Original ignoreDamage=true still compares NBT unless an OreDictionary shortcut succeeds.
        // Damage moved from metadata to a native component; remove that component's varying value only for durable items.
        ItemStack comparable = stack.copyWithCount(1);
        if (comparable.isDamageableItem()) comparable.setDamageValue(0);
        return ItemStack.isSameItemSameComponents(new ItemStack(stack.getItem()), comparable);
    }
}
