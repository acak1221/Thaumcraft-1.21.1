package thaumcraft.common.testing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.IVisDiscountGear;
import thaumcraft.api.IWarpingGear;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.baubles.BaubleType;
import thaumcraft.api.baubles.IBauble;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.IWandRodOnUpdate;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.api.wands.StaffRod;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandHelper;
import thaumcraft.api.wands.WandRod;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.config.ThaumcraftWandEnchantments;
import thaumcraft.common.config.ThaumcraftWands;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneCrafting;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.crafting.ArcaneWandRecipe;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.WandMaterials;
import thaumcraft.common.items.wands.WandVisDiscount;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.WandFocusActions;
import thaumcraft.common.lib.WarpGear;
import thaumcraft.common.lib.baubles.PlayerAccessories;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.crafting.ArcaneSceptreRecipe;
import thaumcraft.common.tiles.TileArcaneWorkbench;

/**
 * Wand parts and a focus registered by an add-on through the public API, in real gameplay.
 *
 * <p>The add-on here is the test harness itself: four test-only items (a rod, a staff core, a cap and an
 * {@link ItemFocusBasic} focus) enter the native item registry from the GameTest mod's initialisation, exactly where an
 * add-on registers its items; the API parts ({@link WandRod}, {@link StaffRod}, {@link WandCap}) are constructed only
 * when a scenario starts, long after Thaumcraft's own initialisation, which proves that nothing is frozen there. The main
 * mod never sees any of this. The player is a real connected survival player
 * ({@link TubeVariantsRuntimeChecks.Fixture}); research, materials and the charged paying wand are declared inputs.</p>
 */
public final class WandApiRuntimeChecks {
    static final String ROD = "gt_ember", STAFF = "gt_ember_staff", CAP = "gt_tidal";
    private static Item rodItem, staffItem, capItem;
    private static AddonFocus focusItem;
    private static AddonRing ringItem;
    private static int onUpdateCalls;
    private static WandRod rod;
    private static StaffRod staff;
    private static WandCap cap;
    private WandApiRuntimeChecks() {}

    /** Test-only items, registered while the native registry is still open (Fabric initialiser, NeoForge RegisterEvent). */
    public static synchronized void registerTestItems() {
        if (rodItem != null) return;
        rodItem = Registry.register(BuiltInRegistries.ITEM, id("addon_wand_rod"), new Item(new Item.Properties()));
        staffItem = Registry.register(BuiltInRegistries.ITEM, id("addon_staff_rod"), new Item(new Item.Properties()));
        capItem = Registry.register(BuiltInRegistries.ITEM, id("addon_wand_cap"), new Item(new Item.Properties()));
        focusItem = Registry.register(BuiltInRegistries.ITEM, id("addon_focus"), new AddonFocus(new Item.Properties()));
        ringItem = Registry.register(BuiltInRegistries.ITEM, id("addon_ring"), new AddonRing(new Item.Properties()));
        // The add-on bauble and focus pouch items of the baubles API scenarios (equipsOnUse, IFocusPouch).
        BaublesApiRuntimeChecks.registerTestItems();
        AddonPouchRuntimeChecks.registerTestItems();
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft_gametest", path); }

    /** The add-on rod's hook (original WandRod onUpdate): one whole vis of fire per carried tick, up to the capacity. */
    private static final IWandRodOnUpdate EMBER_REFILL = (stack, player) -> {
        onUpdateCalls++;
        if (WandHelper.getVis(stack, Aspect.FIRE) < WandHelper.getMaxVis(stack)) WandHelper.addVis(stack, Aspect.FIRE, 1, true);
    };

    /** The add-on's own registration, the 1.7.10 constructors unchanged (capacities in whole vis). */
    private static synchronized void registerParts() {
        if (rodItem == null) throw new IllegalStateException("The GameTest bootstrap must register the add-on test items");
        if (rod != null && WandRod.rods.get(ROD) == rod && WandRod.rods.get(STAFF) == staff && WandCap.caps.get(CAP) == cap) return;
        rod = new WandRod(ROD, 150, new ItemStack(rodItem), 12, EMBER_REFILL, id("textures/models/wand_rod_gt_ember.png"));
        staff = new StaffRod(ROD, 200, new ItemStack(staffItem), 15, (IWandRodOnUpdate) null, id("textures/models/wand_rod_gt_ember.png"));
        staff.setRunes(true);
        cap = new WandCap(CAP, 0.9F, List.of(Aspect.WATER), 0.8F, new ItemStack(capItem), 7);
        cap.setTexture(id("textures/models/wand_cap_gt_tidal.png"));
    }

    /** An add-on focus: its own colour, ornament, cost and cast, dispatched through the ItemFocusBasic hooks only. */
    static final class AddonFocus extends ItemFocusBasic {
        int casts;
        AddonFocus(Properties properties) { super(properties); }
        @Override public int getFocusColor(ItemStack focus) { return 0x33CCAA; }
        @Override public ResourceLocation getOrnament(ItemStack focus) {
            return ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/items/focus_warding_orn.png");
        }
        @Override public AspectList getVisCost(ItemStack focus) { return new AspectList().add(Aspect.WATER, 500).add(Aspect.AIR, 100); }
        @Override public FocusUpgradeType[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
            return rank == 1 ? new FocusUpgradeType[]{FocusUpgradeType.frugal} : new FocusUpgradeType[0];
        }
        @Override public ItemStack onFocusRightClick(ItemStack wand, Level level, Player player, InteractionHand hand) {
            if (!WandHelper.consumeAllVis(wand, player, getVisCost(WandHelper.getFocusItem(wand)), true, false)) return null;
            casts++;
            return wand;
        }
    }

    /** An add-on ring: a bauble that carries warp and a fire discount, and counts its equip, unequip and worn-tick callbacks. */
    static final class AddonRing extends Item implements IBauble, IWarpingGear, IVisDiscountGear {
        int equipped, unequipped, wornTicks;
        AddonRing(Properties properties) { super(properties.stacksTo(1)); }
        @Override public BaubleType getBaubleType(ItemStack stack) { return BaubleType.RING; }
        @Override public void onEquipped(ItemStack stack, LivingEntity wearer) { equipped++; }
        @Override public void onUnequipped(ItemStack stack, LivingEntity wearer) { unequipped++; }
        @Override public void onWornTick(ItemStack stack, LivingEntity wearer) { wornTicks++; }
        @Override public int getWarp(ItemStack stack, Player player) { return 2; }
        @Override public int getVisDiscount(ItemStack stack, Player player, Aspect aspect) { return aspect == Aspect.FIRE ? 3 : 0; }
    }

    private static ItemStack wand(String rod, String cap, int each) {
        ItemStack stack = new ItemStack(ThaumcraftContent.WAND.get());
        Map<String, Integer> vis = new LinkedHashMap<>();
        if (each > 0) for (String primal : WandData.PRIMALS) vis.put(primal, each);
        ItemWandCasting.withData(stack, new WandData(rod, cap, vis));
        return stack;
    }

    private static String key(net.minecraft.network.chat.Component component, int argument) {
        var contents = (TranslatableContents) component.getContents();
        return ((TranslatableContents) ((net.minecraft.network.chat.Component) contents.getArgs()[argument]).getContents()).getKey();
    }

    /** Arcane workbench wand/staff/sceptre assembly from add-on parts, its research gate, cost, capacity and discount. */
    public static void addonPartsAssemble(GameTestHelper test) {
        registerParts();
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            var level = f.level;
            var registries = level.registryAccess();
            var known = KnowledgeAccess.get(f.player);
            // The native catalogue is untouched by the add-on registration.
            f.check(WandMaterials.rod("greatwood") == WandMaterials.GREATWOOD && WandMaterials.cap("gold") == WandMaterials.GOLD
                    && WandRod.rods.get("greatwood").nativeRod() == WandMaterials.GREATWOOD && WandRod.rods.get("greatwood").getCapacity() == 50,
                    "native rods stay the native catalogue; their API views are present with the original whole-vis capacity");
            f.check(WandMaterials.supportsRod(ROD) && WandMaterials.supportsRod(STAFF) && WandMaterials.supportsCap(CAP)
                    && WandMaterials.rod(ROD).capacity() == 15000 && WandMaterials.rod(STAFF).staff() && WandMaterials.rod(STAFF).hasRunes()
                    && WandMaterials.rod(STAFF).baseTag().equals(ROD) && !WandMaterials.rod(ROD).staff(),
                    "add-on parts registered after Thaumcraft's start are resolved by the catalogue");

            f.put(f.base, ThaumcraftArcane.ARCANE_WORKBENCH.get());
            f.aim(Vec3.atCenterOf(f.base));
            f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            f.check(f.use(f.base).consumesAction() && f.player.containerMenu instanceof ContainerArcaneWorkbench,
                    "a real block interaction opens the arcane workbench");
            var table = (TileArcaneWorkbench) level.getBlockEntity(f.base);
            var menu = (ContainerArcaneWorkbench) f.player.containerMenu;
            known.removeResearch(rod.getResearch());
            known.removeResearch(cap.getResearch());
            table.setItem(10, wand("silverwood", "gold", 10000));
            table.setItem(6, new ItemStack(capItem));
            table.setItem(2, new ItemStack(capItem));
            table.setItem(4, new ItemStack(rodItem));
            menu.broadcastChanges();
            f.check(menu.getSlot(0).getItem().isEmpty(), "original ROD_/CAP_ research gates the add-on assembly");
            known.addResearch("CAP_" + CAP);
            menu.broadcastChanges();
            f.check(menu.getSlot(0).getItem().isEmpty(), "the cap research alone is not enough");
            known.addResearch("ROD_" + ROD);
            menu.broadcastChanges();
            var match = ArcaneCrafting.find(f.player, table);
            f.check(match != null && match.arcane() && match.recipe().id().equals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "arcane/wand_assembly")),
                    "the registered dynamic wand recipe matches the add-on items");
            f.check(menu.craftable() && WandData.PRIMALS.stream().allMatch(primal -> menu.requiredVis(primal) == 8400),
                    "rod 12 x cap 7 = 84 vis of every primal, priced by the gold paying wand at 8400 centivis");
            menu.clicked(0, 0, ClickType.PICKUP, f.player);
            ItemStack made = menu.getCarried().copy();
            menu.setCarried(ItemStack.EMPTY);
            f.check(made.is(ThaumcraftContent.WAND.get()) && ItemWandCasting.data(made).equals(new WandData(ROD, CAP, Map.of()))
                    && made.getOrDefault(ThaumcraftWands.WAND_CRAFT_COST.get(), 0) == 84,
                    "the assembled wand carries the add-on rod and cap and the original craft cost metadata");
            f.check(WandData.PRIMALS.stream().allMatch(primal -> ItemWandCasting.getVis(table.getItem(10), primal) == 1600)
                    && table.getItem(4).isEmpty() && table.getItem(2).isEmpty() && table.getItem(6).isEmpty(),
                    "one atomic payment and one material per cell");
            f.check(WandHelper.getRod(made) == rod && WandHelper.getCap(made) == cap && WandHelper.getMaxVis(made) == 15000
                    && !WandHelper.isStaff(made) && !WandHelper.isSceptre(made), "the API answers the add-on parts and capacity");
            var name = made.getHoverName();
            f.check(key(name, 0).equals("item.Wand." + CAP + ".cap") && key(name, 1).equals("item.Wand." + ROD + ".rod")
                    && key(name, 2).equals("item.Wand.wand.obj"), "the original name keys of the parts: " + name);
            f.check(WandHelper.addRealVis(made, Aspect.FIRE, 20000, true) == 5000 && WandHelper.getVis(made, Aspect.FIRE) == 15000,
                    "the wand holds the add-on rod's 150 whole vis, more than the dearest native wand of that price");
            f.check(ItemStack.matches(made, ItemStack.parse(registries, made.save(registries)).orElseThrow()),
                    "the item codec keeps the add-on parts and the vis above the native maximum per rod");
            var prices = ArcaneVis.price(Map.of("aqua", 1, "aer", 1), made, f.player);
            f.check(prices.get("aqua") == 80 && prices.get("aer") == 90
                    && WandHelper.getConsumptionModifier(made, f.player, Aspect.WATER, true) == 0.8F
                    && WandHelper.getConsumptionModifier(made, f.player, Aspect.FIRE, false) == 0.9F,
                    "the cap discounts water by its special 0.8 and everything else by 0.9: " + prices);

            ArcaneWandRecipe recipe = (ArcaneWandRecipe) level.getRecipeManager()
                    .byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "arcane/wand_assembly")).orElseThrow().value();
            var staffInput = ArcaneWandRecipe.displayInput(STAFF, CAP);
            ItemStack staffWand = recipe.assemble(staffInput, registries);
            known.removeResearch("ROD_" + STAFF);
            f.check(recipe.matches(staffInput, level) && !recipe.hasResearch(staffInput, f.player), "the staff rod has its own research");
            known.addResearch("ROD_" + STAFF);
            f.check(recipe.hasResearch(staffInput, f.player) && recipe.vis(staffInput).values().stream().allMatch(cost -> cost == 105)
                    && WandHelper.isStaff(staffWand) && WandHelper.hasRunes(staffWand) && WandHelper.getMaxVis(staffWand) == 20000
                    && ItemWandCasting.STAFF_MODIFIERS.equals(staffWand.get(DataComponents.ATTRIBUTE_MODIFIERS))
                    && !ArcaneVis.isWorkbenchWand(staffWand) && ArcaneVis.isCraftingWand(staffWand),
                    "a StaffRod makes a staff: 15 x 7, 200 vis, the staff's weapon damage, runes, and no workbench slot");
            f.check(key(staffWand.getHoverName(), 1).equals("item.Wand." + ROD + ".rod") && key(staffWand.getHoverName(), 2).equals("item.Wand.staff.obj"),
                    "a staff is named after its plain rod");
            ItemStack fire = new ItemStack(ThaumcraftFoci.FIRE_FOCUS.get());
            f.check(WandHelper.setFocus(staffWand, fire) && WandHelper.getFocusPotency(staffWand) == 1 && ItemWandCasting.focusPotency(staffWand) == 1,
                    "the add-on staff's runes add one focus potency");

            ArcaneSceptreRecipe sceptre = (ArcaneSceptreRecipe) level.getRecipeManager()
                    .byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "arcane/wand_sceptre")).orElseThrow().value();
            var sceptreInput = ArcaneSceptreRecipe.displayInput(ROD, CAP);
            known.removeResearch(ArcaneSceptreRecipe.RESEARCH);
            f.check(sceptre.matches(sceptreInput, level) && !sceptre.hasResearch(sceptreInput, f.player), "the sceptre needs SCEPTRE");
            known.addResearch(ArcaneSceptreRecipe.RESEARCH);
            ItemStack sceptreWand = sceptre.assemble(sceptreInput, registries);
            f.check(sceptre.hasResearch(sceptreInput, f.player) && sceptre.vis(sceptreInput).values().stream().allMatch(cost -> cost == 126)
                    && Boolean.TRUE.equals(sceptreWand.get(ThaumcraftWandEnchantments.WAND_SCEPTRE.get()))
                    && sceptreWand.getOrDefault(ThaumcraftWands.WAND_CRAFT_COST.get(), 0) == 126 && WandHelper.getMaxVis(sceptreWand) == 22500
                    && key(sceptreWand.getHoverName(), 2).equals("item.Wand.sceptre.obj"),
                    "the add-on sceptre costs (int) (84 * 1.5F) and holds 150 x 150 centivis");

            // Native assembly is unchanged next to the add-on parts.
            var nativeInput = ArcaneWandRecipe.displayInput("greatwood", "gold");
            f.check(recipe.vis(nativeInput).values().stream().allMatch(cost -> cost == 9)
                    && ItemWandCasting.data(recipe.assemble(nativeInput, registries)).equals(new WandData("greatwood", "gold", Map.of())),
                    "a native greatwood/gold wand still costs 3 x 3");
            // A registration under a native tag does not replace the native rod (documented deviation).
            WandRod nativeView = WandRod.rods.get("greatwood");
            try {
                new WandRod("greatwood", 999, new ItemStack(Items.STICK), 99);
                f.check(WandMaterials.rod("greatwood") == WandMaterials.GREATWOOD && WandHelper.getRod(wand("greatwood", "iron", 0)) == nativeView,
                        "a native tag always resolves to the native rod");
            } finally {
                WandRod.rods.put("greatwood", nativeView);
            }
            f.player.closeContainer();
        }
        test.succeed();
    }

    /** The add-on rod's IWandRodOnUpdate runs on the carried wand's server tick; native rods refill once, unchanged. */
    public static void addonRodOnUpdateRuns(GameTestHelper test) {
        registerParts();
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            var inventory = f.player.getInventory();
            ItemStack ember = wand(ROD, CAP, 0);
            ItemStack obsidian = wand("obsidian", "iron", 0);
            inventory.setItem(3, ember);
            inventory.setItem(4, obsidian);
            f.player.tickCount = 400; // a multiple of the native passive period of two hundred ticks
            int before = onUpdateCalls;
            inventory.tick();
            f.check(onUpdateCalls == before + 1 && WandHelper.getVis(inventory.getItem(3), Aspect.FIRE) == 100,
                    "the add-on hook ran once on the carrier's tick and added one whole vis");
            f.check(WandHelper.getVis(inventory.getItem(4), Aspect.EARTH) == 100,
                    "the native obsidian rod refilled once: its API view's hook is never called on top of the native passive");
            f.player.tickCount = 401;
            inventory.tick();
            f.check(onUpdateCalls == before + 2 && WandHelper.getVis(inventory.getItem(3), Aspect.FIRE) == 200
                    && WandHelper.getVis(inventory.getItem(4), Aspect.EARTH) == 100,
                    "the add-on hook runs every tick, the native passive only on its period");
            ItemStack full = inventory.getItem(3);
            WandHelper.addRealVis(full, Aspect.FIRE, 20000, true);
            inventory.tick();
            f.check(WandHelper.getVis(inventory.getItem(3), Aspect.FIRE) == 15000, "the hook stops at the add-on capacity");
            inventory.clearContent();
        }
        test.succeed();
    }

    /**
     * An add-on bauble counts only while it is worn: its slot type, its equip/unequip/worn-tick callbacks, and its warp
     * (original WarpEvents.getWarpFromGear reads the four Baubles slots) and vis discount (original WandManager) from an
     * accessory slot, never from the pack.
     */
    public static void addonBaubleCountsWhenWorn(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            var accessories = PlayerAccessories.get(f.player);
            for (int slot = 0; slot < 4; slot++) accessories.removeItem(slot, 1);
            int equipped = ringItem.equipped, unequipped = ringItem.unequipped, worn = ringItem.wornTicks;
            ItemStack ring = new ItemStack(ringItem);
            f.player.getInventory().setItem(9, ring.copy());
            f.check(WarpGear.fromGear(f.player) == 0 && WandVisDiscount.percentagePoints(f.player, Aspect.FIRE) == 0,
                    "a ring in the pack counts for nothing");
            f.check(!accessories.canPlaceItem(0, ring) && accessories.canPlaceItem(1, ring) && accessories.canPlaceItem(2, ring)
                    && !accessories.canPlaceItem(3, ring) && PlayerAccessories.freeSlotFor(f.player, ring) == 1,
                    "a RING bauble goes into the two ring slots only, and a right click puts it into the first free one");
            accessories.setItem(1, ring.copy());
            f.check(ringItem.equipped == equipped + 1, "onEquipped ran once");
            f.check(WarpGear.fromGear(f.player) == 2, "worn, its warp counts: " + WarpGear.fromGear(f.player));
            f.check(WandVisDiscount.percentagePoints(f.player, Aspect.FIRE) == 3 && WandVisDiscount.percentagePoints(f.player, Aspect.WATER) == 0,
                    "worn, its fire discount counts and only for fire");
            PlayerAccessories.tick(f.player);
            f.check(ringItem.wornTicks == worn + 1, "onWornTick runs once per accessory tick");
            accessories.removeItem(1, 1);
            f.check(ringItem.unequipped == unequipped + 1 && WarpGear.fromGear(f.player) == 0
                    && WandVisDiscount.percentagePoints(f.player, Aspect.FIRE) == 0, "taken off: onUnequipped ran, warp and discount are gone");
            f.player.getInventory().clearContent();
        }
        test.succeed();
    }

    /** The WandHelper facade on real wand stacks: vis units, payment and discounts, part and focus accessors, an add-on focus. */
    public static void wandHelperPaysAndMounts(GameTestHelper test) {
        registerParts();
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            ItemStack stack = wand("greatwood", "thaumium", 4000);
            f.check(WandHelper.isWand(stack) && !WandHelper.isWand(new ItemStack(Items.STICK)) && WandHelper.getRod(new ItemStack(Items.STICK)) == null
                    && WandHelper.getRod(stack) == WandRod.rods.get("greatwood") && WandHelper.getRod(stack).nativeRod() == WandMaterials.GREATWOOD
                    && WandHelper.getCap(stack).getTag().equals("thaumium") && WandHelper.getMaxVis(stack) == 5000,
                    "native parts are answered as their API views; greatwood holds 5000 centivis");
            AspectList all = WandHelper.getAllVis(stack);
            f.check(all.size() == 6 && Aspect.getPrimalAspects().stream().allMatch(primal -> all.getAmount(primal) == 4000)
                    && WandHelper.getAspectsWithRoom(stack).size() == 6, "getAllVis answers centivis of the six primals");
            f.check(WandHelper.addVis(stack, Aspect.FIRE, 5, true) == 0 && WandHelper.getVis(stack, Aspect.FIRE) == 4500,
                    "addVis takes whole vis");
            f.check(WandHelper.addVis(stack, Aspect.FIRE, 7, false) == 2 && WandHelper.getVis(stack, Aspect.FIRE) == 4500,
                    "a simulated addVis answers the whole-vis leftover and changes nothing");
            f.check(WandHelper.addVis(stack, Aspect.FIRE, 7, true) == 2 && WandHelper.getVis(stack, Aspect.FIRE) == 5000
                    && WandHelper.getAspectsWithRoom(stack).getAmount(Aspect.FIRE) == 0 && WandHelper.getAspectsWithRoom(stack).size() == 5,
                    "addVis fills to the capacity and answers the leftover");
            f.check(WandHelper.addVis(stack, Aspect.MAGIC, 5, true) == 0 && WandHelper.addRealVis(stack, Aspect.AIR, 1500, true) == 500
                    && WandHelper.getVis(stack, Aspect.AIR) == 5000, "addRealVis takes centivis; a compound aspect is never stored");
            f.check(WandHelper.getConsumptionModifier(stack, f.player, Aspect.ORDER, true) == 0.9F
                    && WandHelper.consumeVis(stack, f.player, Aspect.ORDER, 1000, false) && WandHelper.getVis(stack, Aspect.ORDER) == 3100,
                    "consumeVis pays (int) (1000 * 0.9) centivis");
            f.check(!WandHelper.consumeAllVis(stack, f.player, new AspectList().add(Aspect.WATER, 1000).add(Aspect.EARTH, 5000), true, false)
                    && WandHelper.getVis(stack, Aspect.WATER) == 4000 && WandHelper.getVis(stack, Aspect.EARTH) == 4000,
                    "consumeAllVis pays all or nothing");
            f.check(WandHelper.consumeAllVis(stack, f.player, new AspectList().add(Aspect.WATER, 1000).add(Aspect.EARTH, 2000), false, false)
                    && WandHelper.getVis(stack, Aspect.WATER) == 4000, "doit=false only checks");
            f.check(WandHelper.consumeAllVis(stack, f.player, new AspectList().add(Aspect.WATER, 1000).add(Aspect.EARTH, 2000), true, false)
                    && WandHelper.getVis(stack, Aspect.WATER) == 3100 && WandHelper.getVis(stack, Aspect.EARTH) == 2200,
                    "doit=true pays every discounted aspect");
            f.check(WandHelper.consumeAllVisCrafting(stack, f.player, new AspectList().add(Aspect.ENTROPY, 10), true)
                    && WandHelper.getVis(stack, Aspect.ENTROPY) == 3100, "consumeAllVisCrafting takes whole vis");
            f.check(!WandHelper.consumeAllVis(stack, f.player, new AspectList().add(Aspect.MAGIC, 100), true, false)
                    && !WandHelper.consumeAllVis(stack, f.player, new AspectList(), true, false)
                    && !WandHelper.consumeVis(new ItemStack(Items.STICK), f.player, Aspect.FIRE, 1, false),
                    "a compound aspect, an empty list or a non-wand never pays");

            ItemStack focus = new ItemStack(focusItem);
            f.check(FocusData.isAddonFocus(focusItem) && focusItem.applyUpgrade(focus, FocusUpgradeType.frugal, 1) && FocusData.isSupportedFocus(focus)
                    && WandHelper.setFocus(stack, focus) && WandHelper.getFocus(stack) == focusItem
                    && ItemStack.matches(WandHelper.getFocusItem(stack), focus) && WandHelper.getFocusFrugal(stack) == 1
                    && WandHelper.getFocusPotency(stack) == 0 && WandHelper.getFocusTreasure(stack) == 0,
                    "an add-on ItemFocusBasic mounts on the wand with its own upgrades");
            f.check(WandHelper.getConsumptionModifier(stack, f.player, Aspect.WATER, false) == Math.max(0.9F - 0F - 1 / 10F, .1F)
                    && WandHelper.getConsumptionModifier(stack, f.player, Aspect.WATER, true) == 0.9F,
                    "the frugal upgrade discounts casting, never crafting");
            f.player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            int casts = focusItem.casts;
            WandFocusActions.use(f.player, InteractionHand.MAIN_HAND);
            ItemStack held = f.player.getMainHandItem();
            int waterPrice = (int) (500 * Math.max(0.9F - 0F - 1 / 10F, .1F)), airPrice = (int) (100 * Math.max(0.9F - 0F - 1 / 10F, .1F));
            f.check(focusItem.casts == casts + 1 && WandHelper.getVis(held, Aspect.WATER) == 3100 - waterPrice
                    && WandHelper.getVis(held, Aspect.AIR) == 5000 - airPrice,
                    "the add-on focus casts through its own hook and pays its discounted cost through WandHelper");
            f.check(!WandHelper.setFocus(held, new ItemStack(Items.STICK)) && WandHelper.getFocus(held) == focusItem
                    && WandHelper.setFocus(held, ItemStack.EMPTY) && WandHelper.getFocus(held) == null && WandHelper.getFocusItem(held).isEmpty(),
                    "a non-focus is refused; an empty stack removes the focus");

            WandHelper.setRod(held, rod);
            f.check(ItemWandCasting.data(held).rod().equals(ROD) && WandHelper.getMaxVis(held) == 15000 && WandHelper.getVis(held, Aspect.AIR) == 5000 - airPrice,
                    "setRod moves the wand to the add-on rod and keeps its vis");
            WandHelper.addRealVis(held, Aspect.AIR, 20000, true);
            WandHelper.setRod(held, WandRod.rods.get("greatwood"));
            WandHelper.setCap(held, cap);
            f.check(WandHelper.getVis(held, Aspect.AIR) == 5000 && WandHelper.getCap(held) == cap && ItemWandCasting.data(held).rod().equals("greatwood"),
                    "setRod cuts the vis to the new capacity; setCap installs the add-on cap");
            f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        test.succeed();
    }
}
