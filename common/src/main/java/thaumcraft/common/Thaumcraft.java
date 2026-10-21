package thaumcraft.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftOres;
import thaumcraft.common.config.ThaumcraftWorldgen;
import thaumcraft.common.config.ThaumcraftNodes;
import thaumcraft.common.config.ThaumcraftNodeDevices;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftAlchemy;
import thaumcraft.common.config.ThaumcraftFlux;
import thaumcraft.common.config.ThaumcraftResources;
import thaumcraft.common.config.ThaumcraftNaturals;
import thaumcraft.common.config.ThaumcraftMasonry;
import thaumcraft.common.config.ThaumcraftDistillation;
import thaumcraft.common.config.ThaumcraftAlembics;
import thaumcraft.common.config.ThaumcraftWands;
import thaumcraft.common.config.ThaumcraftBellows;
import thaumcraft.common.config.ThaumcraftLabels;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.config.ThaumcraftFocalManipulation;
import thaumcraft.common.config.ThaumcraftEmbers;
import thaumcraft.common.config.ThaumcraftFrost;
import thaumcraft.common.config.ThaumcraftPlants;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.config.ThaumcraftEssences;
import thaumcraft.common.config.ThaumcraftWearables;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.network.ResearchPurchaseNetworking;
import thaumcraft.common.lib.network.ResearchNoteRequestNetworking;
import thaumcraft.common.lib.network.ResearchTableEditNetworking;
import thaumcraft.common.lib.network.ResearchTableCombineNetworking;
import thaumcraft.common.lib.network.FocusSelectionNetworking;
import thaumcraft.common.lib.network.FocalManipulatorNetworking;
import thaumcraft.common.lib.network.InfusionFxNetworking;
import thaumcraft.common.lib.network.ShockFxNetworking;
import thaumcraft.common.lib.network.ExcavationFxNetworking;
import thaumcraft.common.lib.network.VisNetFxNetworking;
import thaumcraft.api.visnet.VisNetHandler;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.research.ObjectAspectCatalog;
import thaumcraft.common.lib.events.ThaumcraftCommands;
import thaumcraft.common.lib.world.NodeHarvest;
import thaumcraft.common.items.wands.foci.WandFocusActions;
import thaumcraft.common.items.wands.foci.ItemFocusExcavation;
import thaumcraft.common.items.wands.ItemWandCasting;

/** Native 1.21.1 bootstrap. Legacy src/ is reference material, not runtime code. */
public final class Thaumcraft {
    public static final String MODID = "thaumcraft";
    public static final Logger LOG = LoggerFactory.getLogger("Thaumcraft");
    private static boolean initialized;

    private Thaumcraft() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        thaumcraft.common.config.ThaumcraftSettings.load(dev.architectury.platform.Platform.getConfigFolder().resolve("thaumcraft.properties"));
        thaumcraft.common.lib.network.ResearchSettingsNetworking.initServer();
        thaumcraft.common.lib.network.WarpMessageNetworking.initServer();
        thaumcraft.common.lib.network.KnowledgeNotificationNetworking.initServer();
        ResearchCatalog.loadBundled();
        thaumcraft.api.research.ResearchCategories.loadNative();
        thaumcraft.common.lib.mods.ChampionHooks.init();
        thaumcraft.common.lib.mods.ChampionWiring.init();
        thaumcraft.common.lib.research.ResearchConditions.initializeNative();
        ObjectAspectCatalog.loadBundled();
        ThaumcraftLabels.register();
        ThaumcraftContent.register();
        // The eldritch block family owns the ids the worldgen of the Outer Lands and of the
        // villages reads, so it is registered before the legacy tile unit, whose wand pedestal
        // block entity type also claims the stone device of the eldritch family.
        thaumcraft.common.config.ThaumcraftEldritchBlocks.register();
        thaumcraft.common.config.ThaumcraftDeviceMenus.register();
        thaumcraft.common.config.ThaumcraftLegacyTiles.register();
        thaumcraft.common.config.ThaumcraftProjectiles.register();
        thaumcraft.common.lib.entities.FollowingItems.install();
        thaumcraft.common.config.ThaumcraftCrystalClusters.register();
        thaumcraft.common.config.ThaumcraftRunic.register();
        thaumcraft.common.config.ThaumcraftBaubles.register();
        thaumcraft.common.config.ThaumcraftRunicBaubles.register();
        thaumcraft.common.config.ThaumcraftTravelGear.register();
        thaumcraft.common.lib.network.HoverNetworking.init();
        thaumcraft.common.config.ThaumcraftLoot.register();
        thaumcraft.common.config.ThaumcraftFocusPouches.register();
        thaumcraft.common.config.ThaumcraftColoredCandles.register();
        thaumcraft.common.config.ThaumcraftManaBeans.register();
        thaumcraft.common.config.ThaumcraftPech.register();
        thaumcraft.common.config.ThaumcraftHellbatFoci.register();
        thaumcraft.common.config.ThaumcraftAspectOrbs.register();
        thaumcraft.common.config.ThaumcraftExplosiveOrbs.register();
        thaumcraft.common.config.ThaumcraftShockOrbs.register();
        thaumcraft.common.config.ThaumcraftUnknownKnowledge.register();
        thaumcraft.common.config.ThaumcraftGolemMaterials.register();
        thaumcraft.common.config.ThaumcraftTaint.register();
        thaumcraft.common.config.ThaumcraftTaintAnimals.register();
        thaumcraft.common.config.ThaumcraftTaintCreeper.register();
        thaumcraft.common.entities.monster.TaintDeathConversion.register();
        thaumcraft.common.config.ThaumcraftPrimalFoci.register();
        thaumcraft.common.config.ThaumcraftBiomes.register();
        ThaumcraftOres.register();
        ThaumcraftWorldgen.register();
        thaumcraft.common.config.ThaumcraftVillages.register();
        thaumcraft.common.config.ThaumcraftOuterLands.register();
        ThaumcraftNodes.register();
        ThaumcraftNodeDevices.register();
        ThaumcraftArcane.register();
        ThaumcraftFlux.register();
        ThaumcraftResources.register();
        ThaumcraftAlchemy.register();
        ThaumcraftNaturals.register();
        ThaumcraftPlants.register();
        ThaumcraftMasonry.register();
        thaumcraft.common.config.ThaumcraftGrate.register();
        thaumcraft.common.config.ThaumcraftCosmeticWood.register();
        ThaumcraftDistillation.register();
        thaumcraft.common.config.ThaumcraftAdvancedFurnace.register();
        ThaumcraftAlembics.register();
        ThaumcraftWands.register();
        thaumcraft.common.config.ThaumcraftWandEnchantments.register();
        ThaumcraftBellows.register();
        ThaumcraftFoci.register();
        ThaumcraftFocalManipulation.register();
        thaumcraft.common.config.ThaumcraftHoles.register();
        thaumcraft.common.config.ThaumcraftLevitator.register();
        thaumcraft.common.config.ThaumcraftArcaneEar.register();
        thaumcraft.common.config.ThaumcraftLamps.register();
        thaumcraft.common.config.ThaumcraftBiologicalLamps.register();
        thaumcraft.common.lib.network.GrowthLampFxNetworking.initServer();
        thaumcraft.common.config.ThaumcraftDeconstruction.register();
        thaumcraft.common.config.ThaumcraftVisCharging.register();
        thaumcraft.common.config.ThaumcraftThaumatorium.register();
        thaumcraft.common.config.ThaumcraftWards.register();
        thaumcraft.common.config.ThaumcraftArchitect.register();
        thaumcraft.common.config.ThaumcraftWarp.register();
        thaumcraft.common.config.ThaumcraftMonsters.register();
        thaumcraft.common.config.ThaumcraftEldritchMobs.register();
        thaumcraft.common.config.ThaumcraftCultists.register();
        thaumcraft.common.config.ThaumcraftEldritchBlocks.register();
        thaumcraft.common.config.ThaumcraftWarp.enableEvents();
        thaumcraft.common.config.ThaumcraftSpa.register();
        thaumcraft.common.config.ThaumcraftEldritchItems.register();
        thaumcraft.common.lib.world.TradeSwaps.init();
        thaumcraft.common.lib.network.WandArchitectNetworking.init();
        thaumcraft.common.config.ThaumcraftArcaneBore.register();
        thaumcraft.common.config.ThaumcraftHungryChest.register();
        thaumcraft.common.config.ThaumcraftGolems.register();
        ThaumcraftEmbers.register();
        ThaumcraftFrost.register();
        ThaumcraftTubes.register();
        ThaumcraftInfusion.register();
        ThaumcraftEquipment.register();
        thaumcraft.common.config.ThaumcraftLegacyItems.register();
        ThaumcraftEssences.register();
        ThaumcraftWearables.register();
        KnowledgeEvents.initServer();
        ResearchPurchaseNetworking.init();
        ResearchNoteRequestNetworking.init();
        ResearchTableEditNetworking.init();
        ResearchTableCombineNetworking.init();
        thaumcraft.common.lib.network.ResearchTableCopyNetworking.init();
        FocusSelectionNetworking.init();
        thaumcraft.common.lib.network.GolemBellNetworking.init();
        thaumcraft.common.lib.network.ShovelOrientationNetworking.init();
        FocalManipulatorNetworking.init();
        InfusionFxNetworking.initServer();
        ShockFxNetworking.initServer();
        ExcavationFxNetworking.initServer();
        VisNetFxNetworking.initServer();
        VisNetHandler.setEffectSink(VisNetFxNetworking::send);
        // Coordinate-only progress survives ordinary release, NOT a change of
        // player instance, dimension, actual death or server lifetime.
        PlayerEvent.PLAYER_QUIT.register(ItemFocusExcavation::clearPlayer);
        PlayerEvent.CRAFT_ITEM.register((player, output, matrix) -> {
            if (player instanceof net.minecraft.server.level.ServerPlayer server)
                thaumcraft.common.lib.WarpEvents.onCrafted(server, output);
        });
        PlayerEvent.CHANGE_DIMENSION.register((player, from, to) -> ItemFocusExcavation.clearPlayer(player));
        PlayerEvent.PLAYER_CLONE.register((oldPlayer, newPlayer, wonGame) -> {
            ItemFocusExcavation.clearPlayer(oldPlayer);
            ItemFocusExcavation.clearPlayer(newPlayer);
        });
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer serverPlayer && !serverPlayer.isAlive()) ItemFocusExcavation.clearPlayer(serverPlayer);
        });
        TickEvent.SERVER_LEVEL_POST.register(level -> {
            NodeHarvest.endTick(level);
            DeferredItemDrops.tick(level);
            thaumcraft.common.lib.GrowthLampTicks.tick(level);
            thaumcraft.common.lib.utils.AspectOrbQueue.tick(level);
        });
        LifecycleEvent.SERVER_LEVEL_UNLOAD.register(level -> {
            thaumcraft.common.lib.world.WardingCasts.clear(level);
            thaumcraft.common.lib.bore.BoreNativeContext.clearLevel(level);
            VisNetHandler.clearLevel(level);
            VisNetFxNetworking.clearLevel(level);
        });
        LifecycleEvent.SERVER_STOPPED.register(server -> {
            thaumcraft.common.lib.research.ObjectAspectRegistry.serverStopped(server);
            thaumcraft.common.lib.bore.BoreNativeContext.clearServer(server);
            VisNetHandler.clearServer(server);
            VisNetFxNetworking.clearServer(server);
            NodeHarvest.clear();
            ItemWandCasting.clearNodeUses();
            WandFocusActions.clearCooldowns();
            ItemFocusExcavation.clearAll();
            thaumcraft.api.ThaumcraftApi.installRecipeManager(null, null);
        });
        LifecycleEvent.SERVER_STARTED.register(server -> {
            ObjectAspectCatalog.validateAgainstRegistries();
            thaumcraft.common.lib.loot.LootTables.initialize();
            thaumcraft.common.lib.research.ObjectAspectRegistry.serverStarted(server);
            // The public recipe API answers from the recipe manager of the loaded world.
            thaumcraft.api.ThaumcraftApi.installRecipeManager(server.getRecipeManager(), server.registryAccess());
        });
        thaumcraft.common.lib.utils.OriginalOreTags.register();
        thaumcraft.api.ThaumcraftApi.refreshPortableHoleBlackList();
        thaumcraft.api.aspects.AspectRegistryEvent.post();
        ThaumcraftCommands.register();
        LOG.info("TC4 Minecraft 1.21.1 migration: {} research definitions loaded; gameplay coverage is tracked in MIGRATION_STATUS.md",
                ResearchCatalog.all().size());
    }
}
