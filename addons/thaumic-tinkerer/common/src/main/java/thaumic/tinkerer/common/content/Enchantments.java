/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.content;

import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.client.EnchantmentsClient;
import thaumic.tinkerer.client.lib.LibResources;
import thaumic.tinkerer.common.block.BlockEnchanter;
import thaumic.tinkerer.common.block.tile.TileEnchanter;
import thaumic.tinkerer.common.block.tile.container.ContainerEnchanter;
import thaumic.tinkerer.common.enchantment.ModEnchantments;
import thaumic.tinkerer.common.enchantment.core.EnchantmentManager;
import thaumic.tinkerer.common.lib.LibBlockNames;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.network.packet.PacketEnchanterAddEnchant;
import thaumic.tinkerer.common.network.packet.PacketEnchanterStartWorking;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * Feature group: the fourteen enchantments and the Osmotic Enchanter that puts them on tools and armor.
 *
 * <p>The enchantments themselves are data ({@code data/thaumictinkerer/enchantment}); this group registers the Enchanter
 * (block, block entity, menu, the two packets of its screen), the event handler that gives the enchantments their effect,
 * the table of costs and rules of the Enchanter, and the fourteen research entries of the enchantments (the entry of the
 * Enchanter itself comes with its block).</p>
 */
public final class Enchantments implements ContentGroup {
    public static final Enchantments INSTANCE = new Enchantments();

    private static RegistrySupplier<BlockEnchanter> enchanter;
    private static RegistrySupplier<BlockEntityType<TileEnchanter>> enchanterType;
    private static RegistrySupplier<MenuType<ContainerEnchanter>> enchanterMenu;

    private Enchantments() {}

    @Override
    public void register() {
        // Material.rock: needs a pickaxe (any), stone step sound; hardness 5, resistance 2000 of 1.7.10, which is 1200 now
        // (the explosion resistance of 1.7.10 was a third of what the same number means today, divided by five).
        enchanter = TTRegistry.block(LibBlockNames.ENCHANTER, () -> new BlockEnchanter(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE).strength(5.0F, 1200.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()
                .noOcclusion()));
        enchanterType = TTRegistry.BLOCK_ENTITIES.register(LibBlockNames.ENCHANTER,
                () -> BlockEntityType.Builder.of(TileEnchanter::new, enchanter.get()).build(null));
        enchanterMenu = TTRegistry.MENUS.register(LibBlockNames.ENCHANTER,
                () -> MenuRegistry.ofExtended(ContainerEnchanter::fromNetwork));

        PacketEnchanterAddEnchant.register();
        PacketEnchanterStartWorking.register();

        ModEnchantments.initEnchantments();
    }

    /** The costs and rules of the Enchanter need the aspects of Thaumcraft, the research entries its research tab. */
    @Override
    public void ready() {
        ModEnchantments.initJump();
        EnchantmentManager.initEnchantmentData();
        registerResearch();
    }

    @Override
    public void registerClient() {
        EnchantmentsClient.register();
    }

    // ---- registered objects ---------------------------------------------------------------------------------------

    public static BlockEntityType<TileEnchanter> enchanterType() {
        return enchanterType.get();
    }

    public static MenuType<ContainerEnchanter> enchanterMenu() {
        return enchanterMenu.get();
    }

    public static BlockEnchanter enchanterBlock() {
        return enchanter.get();
    }

    // ---- research -------------------------------------------------------------------------------------------------

    /**
     * The fourteen entries of {@code ResearchHelper.initResearch} that belong to the enchantments: secondary entries of
     * complexity 2 below the entry of the Enchanter, one text page each. (The entry {@code PERIPHERALS} of the same method
     * is not an enchantment's.)
     */
    private void registerResearch() {
        research(LibResearch.KEY_ENCHANT_ASCENT_BOOST,
                new AspectList().add(Aspect.AIR, 1).add(Aspect.MOTION, 1).add(Aspect.MAGIC, 2), 6, 2,
                LibResources.ENCHANT_ASCENT_BOOST, LibResearch.KEY_ENCHANTER);
        research(LibResearch.KEY_ENCHANT_SLOW_FALL,
                new AspectList().add(Aspect.AIR, 1).add(Aspect.MOTION, 1).add(Aspect.MAGIC, 2), 7, 3,
                LibResources.ENCHANT_SLOW_FALL, LibResearch.KEY_ENCHANTER);
        research(LibResearch.KEY_ENCHANT_AUTO_SMELT,
                new AspectList().add(Aspect.FIRE, 1).add(Aspect.ENTROPY, 1).add(Aspect.MAGIC, 2), 7, 5,
                LibResources.ENCHANT_AUTO_SMELT, LibResearch.KEY_ENCHANTER);
        research(LibResearch.KEY_ENCHANT_DESINTEGRATE,
                new AspectList().add(Aspect.ENTROPY, 1).add(Aspect.VOID, 1).add(Aspect.MAGIC, 2), 6, 6,
                LibResources.ENCHANT_DESINTEGRATE, LibResearch.KEY_ENCHANTER);
        research(LibResearch.KEY_ENCHANT_QUICK_DRAW,
                new AspectList().add(Aspect.SENSES, 1).add(Aspect.WEAPON, 1).add(Aspect.MAGIC, 2), 4, 6,
                LibResources.ENCHANT_QUICK_DRAW, LibResearch.KEY_ENCHANTER);
        research(LibResearch.KEY_ENCHANT_VAMPIRISM,
                new AspectList().add(Aspect.HUNGER, 1).add(Aspect.WEAPON, 1).add(Aspect.MAGIC, 2), 3, 5,
                LibResources.ENCHANT_VAMPIRISM, LibResearch.KEY_ENCHANTER);
        research(LibResearch.KEY_ENCHANT_FOCUSED_STRIKE,
                new AspectList().add(Aspect.ORDER, 1).add(Aspect.WEAPON, 1).add(Aspect.MAGIC, 2), 2, 7,
                LibResources.ENCHANT_FOCUSED_STRIKE, LibResearch.KEY_ENCHANT_VAMPIRISM);
        research(LibResearch.KEY_ENCHANT_DISPERSED_STRIKE,
                new AspectList().add(Aspect.ENTROPY, 1).add(Aspect.WEAPON, 1).add(Aspect.MAGIC, 2), 1, 6,
                LibResources.ENCHANT_DISPERSED_STRIKE, LibResearch.KEY_ENCHANT_VAMPIRISM);
        research(LibResearch.KEY_ENCHANT_FINAL_STRIKE,
                new AspectList().add(Aspect.ENTROPY, 3).add(Aspect.ORDER, 3).add(Aspect.WEAPON, 3).add(Aspect.MAGIC, 2), 0, 8,
                LibResources.ENCHANT_FINAL_STRIKE, LibResearch.KEY_ENCHANT_FOCUSED_STRIKE,
                LibResearch.KEY_ENCHANT_DISPERSED_STRIKE);
        research(LibResearch.KEY_ENCHANT_POUNCE,
                new AspectList().add(Aspect.AIR, 3).add(Aspect.ORDER, 3).add(Aspect.ARMOR, 3).add(Aspect.MAGIC, 2), 7, 0,
                LibResources.ENCHANT_POUNCE, LibResearch.KEY_ENCHANT_ASCENT_BOOST);
        research(LibResearch.KEY_ENCHANT_SHATTER,
                new AspectList().add(Aspect.EARTH, 3).add(Aspect.ENTROPY, 3).add(Aspect.TOOL, 1).add(Aspect.MAGIC, 2), 5, 8,
                LibResources.ENCHANT_SHATTER, LibResearch.KEY_ENCHANT_DESINTEGRATE);
        research(LibResearch.KEY_ENCHANT_SHOCKWAVE,
                new AspectList().add(Aspect.AIR, 3).add(Aspect.ENTROPY, 3).add(Aspect.ARMOR, 1).add(Aspect.MAGIC, 2), 9, 2,
                LibResources.ENCHANT_SHOCKWAVE, LibResearch.KEY_ENCHANT_SLOW_FALL);
        research(LibResearch.KEY_ENCHANT_TUNNEL,
                new AspectList().add(Aspect.EARTH, 3).add(Aspect.ORDER, 3).add(Aspect.TOOL, 1).add(Aspect.MAGIC, 2), 9, 6,
                LibResources.ENCHANT_TUNNEL, LibResearch.KEY_ENCHANT_AUTO_SMELT);
        research(LibResearch.KEY_ENCHANT_VALIANCE,
                new AspectList().add(Aspect.WEAPON, 3).add(Aspect.HEAL, 3).add(Aspect.MAGIC, 2), 1, 4,
                LibResources.ENCHANT_VALIANCE, LibResearch.KEY_ENCHANT_VAMPIRISM);
    }

    private static void research(String key, AspectList aspects, int column, int row, String icon, String... parents) {
        TTResearchItem research = new TTResearchItem(key, aspects, column, row, 2, ResourceLocation.parse(icon));
        research.setParents(parents);
        research.setPages(new ResearchPage("0")).setSecondary();
        ResearchHelper.queue(research);
    }
}
