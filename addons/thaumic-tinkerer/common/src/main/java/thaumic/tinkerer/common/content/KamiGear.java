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

import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.utils.Env;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandHelper;
import thaumcraft.api.wands.WandRod;
import thaumic.tinkerer.client.KamiGearClient;
import thaumic.tinkerer.common.ThaumicTinkerer;
import thaumic.tinkerer.common.block.tile.container.kami.ContainerIchorPouch;
import thaumic.tinkerer.common.core.handler.ConfigHandler;
import thaumic.tinkerer.common.core.handler.kami.KamiArmorHandler;
import thaumic.tinkerer.common.item.kami.ItemBlockTalisman;
import thaumic.tinkerer.common.item.kami.ItemCatAmulet;
import thaumic.tinkerer.common.item.kami.ItemIchorPouch;
import thaumic.tinkerer.common.item.kami.ItemKamiResource;
import thaumic.tinkerer.common.item.kami.ItemPlacementMirror;
import thaumic.tinkerer.common.item.kami.ItemProtoclay;
import thaumic.tinkerer.common.item.kami.ItemSkyPearl;
import thaumic.tinkerer.common.item.kami.KamiComponents;
import thaumic.tinkerer.common.item.kami.KamiResearch;
import thaumic.tinkerer.common.item.kami.armor.ItemGemBoots;
import thaumic.tinkerer.common.item.kami.armor.ItemGemChest;
import thaumic.tinkerer.common.item.kami.armor.ItemGemHelm;
import thaumic.tinkerer.common.item.kami.armor.ItemGemLegs;
import thaumic.tinkerer.common.item.kami.armor.ItemIchorclothArmor;
import thaumic.tinkerer.common.item.kami.armor.KamiArmorMaterials;
import thaumic.tinkerer.common.item.kami.foci.ItemFocusRecall;
import thaumic.tinkerer.common.item.kami.foci.ItemFocusShadowbeam;
import thaumic.tinkerer.common.item.kami.foci.ItemFocusXPDrain;
import thaumic.tinkerer.common.item.kami.tool.IchorTier;
import thaumic.tinkerer.common.item.kami.tool.ItemIchorAxe;
import thaumic.tinkerer.common.item.kami.tool.ItemIchorAxeAdv;
import thaumic.tinkerer.common.item.kami.tool.ItemIchorPick;
import thaumic.tinkerer.common.item.kami.tool.ItemIchorPickAdv;
import thaumic.tinkerer.common.item.kami.tool.ItemIchorShovel;
import thaumic.tinkerer.common.item.kami.tool.ItemIchorShovelAdv;
import thaumic.tinkerer.common.item.kami.tool.ItemIchorSword;
import thaumic.tinkerer.common.item.kami.tool.ItemIchorSwordAdv;
import thaumic.tinkerer.common.item.kami.tool.ToolHandler;
import thaumic.tinkerer.common.item.kami.wand.CapIchor;
import thaumic.tinkerer.common.item.kami.wand.RodIchorcloth;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.network.packet.kami.PacketArmorStatus;
import thaumic.tinkerer.common.network.packet.kami.PacketShadowBeam;
import thaumic.tinkerer.common.network.packet.kami.PacketToggleArmor;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * Feature group: the Kami gear: ichor resources, wand rod and cap, tools, armour, pouch, amulet, talisman, mirror,
 * protoclay, sky pearl and the three Kami foci.
 *
 * <p>Everything is gated by {@code ConfigHandler.enableKami} at registration time: with the option off nothing of the
 * group exists (items, menu, events, research, wand parts), as in 1.7.10 where each Kami class answered
 * {@code shouldRegister()} with the option.</p>
 *
 * <p>Registry paths are the 1.7.10 names ({@link LibItemNames}); the eight metadata values of the resource item are the
 * eight items of {@link LibItemNames#KAMI_RESOURCE_NAMES}.</p>
 */
public final class KamiGear implements ContentGroup {
    public static final KamiGear INSTANCE = new KamiGear();

    private static TTResearchItem ichorResearch;

    private KamiGear() {}

    /** The ICHOR research entry (1.7.10 {@code ResearchHelper.kamiResearch}), null until the ready hook ran or with Kami off. */
    public static TTResearchItem ichorResearch() {
        return ichorResearch;
    }

    @Override
    public void register() {
        if (!ConfigHandler.enableKami) return;

        KamiComponents.register();
        KamiArmorMaterials.register();

        // Resources: the order of the metadata values.
        for (int i = 0; i < LibItemNames.KAMI_RESOURCE_NAMES.length; i++) {
            final int meta = i;
            TTRegistry.item(LibItemNames.KAMI_RESOURCE_NAMES[i], () -> new ItemKamiResource(meta, new Item.Properties()));
        }

        // Armour.
        TTRegistry.item(LibItemNames.ICHOR_HELM, () -> new ItemIchorclothArmor(KamiArmorMaterials.ICHOR,
                ArmorItem.Type.HELMET, new Item.Properties()));
        TTRegistry.item(LibItemNames.ICHOR_CHEST, () -> new ItemIchorclothArmor(KamiArmorMaterials.ICHOR,
                ArmorItem.Type.CHESTPLATE, new Item.Properties()));
        TTRegistry.item(LibItemNames.ICHOR_LEGS, () -> new ItemIchorclothArmor(KamiArmorMaterials.ICHOR,
                ArmorItem.Type.LEGGINGS, new Item.Properties()));
        TTRegistry.item(LibItemNames.ICHOR_BOOTS, () -> new ItemIchorclothArmor(KamiArmorMaterials.ICHOR,
                ArmorItem.Type.BOOTS, new Item.Properties()));
        TTRegistry.item(LibItemNames.ICHOR_HELM_GEM, () -> new ItemGemHelm(new Item.Properties()));
        TTRegistry.item(LibItemNames.ICHOR_CHEST_GEM, () -> new ItemGemChest(new Item.Properties()));
        TTRegistry.item(LibItemNames.ICHOR_LEGS_GEM, () -> new ItemGemLegs(new Item.Properties()));
        TTRegistry.item(LibItemNames.ICHOR_BOOTS_GEM, () -> new ItemGemBoots(new Item.Properties()));

        // Amulet, tools, pouch, ring, mirror and the rest.
        TTRegistry.item(LibItemNames.CAT_AMULET, () -> new ItemCatAmulet(new Item.Properties()));
        TTRegistry.item(LibItemNames.ICHOR_PICK, () -> new ItemIchorPick(IchorTier.pickaxeProperties()));
        TTRegistry.item(LibItemNames.ICHOR_SHOVEL, () -> new ItemIchorShovel(IchorTier.shovelProperties()));
        TTRegistry.item(LibItemNames.ICHOR_AXE, () -> new ItemIchorAxe(IchorTier.axeProperties()));
        TTRegistry.item(LibItemNames.ICHOR_SWORD, () -> new ItemIchorSword(IchorTier.swordProperties()));
        TTRegistry.item(LibItemNames.ICHOR_PICK_GEM, () -> new ItemIchorPickAdv(IchorTier.pickaxeProperties()));
        TTRegistry.item(LibItemNames.ICHOR_SHOVEL_GEM, () -> new ItemIchorShovelAdv(IchorTier.shovelProperties()));
        TTRegistry.item(LibItemNames.ICHOR_AXE_GEM, () -> new ItemIchorAxeAdv(IchorTier.axeProperties()));
        TTRegistry.item(LibItemNames.ICHOR_SWORD_GEM, () -> new ItemIchorSwordAdv(IchorTier.swordProperties()));
        TTRegistry.item(LibItemNames.ICHOR_POUCH, () -> new ItemIchorPouch(new Item.Properties()));
        TTRegistry.item(LibItemNames.BLOCK_TALISMAN, () -> new ItemBlockTalisman(new Item.Properties()));
        TTRegistry.item(LibItemNames.PLACEMENT_MIRROR, () -> new ItemPlacementMirror(new Item.Properties()));
        TTRegistry.item(LibItemNames.PROTOCLAY, () -> new ItemProtoclay(new Item.Properties()));
        TTRegistry.item(LibItemNames.SKY_PEARL, () -> new ItemSkyPearl(new Item.Properties()));

        // The three Kami foci.
        TTRegistry.item(LibItemNames.FOCUS_SHADOWBEAM, () -> new ItemFocusShadowbeam(new Item.Properties()));
        TTRegistry.item(LibItemNames.FOCUS_XP_DRAIN, () -> new ItemFocusXPDrain(new Item.Properties()));
        TTRegistry.item(LibItemNames.FOCUS_RECALL, () -> new ItemFocusRecall(new Item.Properties()));

        // The menu of the pouch.
        ItemIchorPouch.MENU = TTRegistry.MENUS.register(LibItemNames.ICHOR_POUCH,
                () -> MenuRegistry.ofExtended(ContainerIchorPouch::new));

        // The Ichor wand of the creative tab (1.7.10 ModCreativeTab.addWand).
        TTRegistry.addTabStack(KamiGear::ichorWand);

        registerNetwork();
        registerEvents();
    }

    private static void registerNetwork() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, PacketToggleArmor.TYPE, PacketToggleArmor.CODEC,
                (packet, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer player) {
                        KamiArmorHandler.setArmorStatus(player, packet.armorStatus());
                    }
                }));
        // The payloads the server sends: Architectury registers the type of an S2C payload with its client receiver
        // (KamiGearClient), so only a dedicated server registers it here; both would make Fabric's registry throw
        // "Packet type ... is already registered!" on a client (the integrated server shares the client's registry).
        if (Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(PacketArmorStatus.TYPE, PacketArmorStatus.CODEC);
            NetworkManager.registerS2CPayloadType(PacketShadowBeam.TYPE, PacketShadowBeam.CODEC);
        }
    }

    private static void registerEvents() {
        KamiArmorHandler.register();
        BlockEvent.BREAK.register(ToolHandler::onBlockBreak);
        TickEvent.SERVER_POST.register(ToolHandler::onServerTickEnd);
        InteractionEvent.LEFT_CLICK_BLOCK.register(ItemIchorPickAdv::onLeftClickBlock);
        InteractionEvent.LEFT_CLICK_BLOCK.register(ItemProtoclay::onLeftClickBlock);
        PlayerEvent.ATTACK_ENTITY.register(ItemIchorSwordAdv::onAttackEntity);
    }

    @Override
    public void ready() {
        if (!ConfigHandler.enableKami) return;

        // The wand parts (1.7.10: TTCommonProxy.preInit): the rod and the cap register themselves with Thaumcraft.
        new CapIchor(ItemKamiResource.stack(ItemKamiResource.ICHOR_CAP));
        new RodIchorcloth(ItemKamiResource.stack(ItemKamiResource.ICHORCLOTH_ROD));

        // Research: queued here, registered by ResearchHelper.flush() after every other entry (see KamiResearchItem).
        for (TTResearchItem research : ItemKamiResource.research()) {
            if (research.key.equals(LibResearch.KEY_ICHOR)) ichorResearch = research;
            ResearchHelper.queue(research);
        }
        ResearchHelper.queue(ItemIchorPick.toolsResearch());
        ResearchHelper.queue(ItemIchorPickAdv.research());
        ResearchHelper.queue(ItemIchorShovelAdv.research());
        ResearchHelper.queue(ItemIchorAxeAdv.research());
        ResearchHelper.queue(ItemIchorSwordAdv.research());
        ResearchHelper.queue(ItemIchorclothArmor.research());
        ResearchHelper.queue(ItemGemHelm.research());
        ResearchHelper.queue(ItemGemChest.research());
        ResearchHelper.queue(ItemGemLegs.research());
        ResearchHelper.queue(ItemGemBoots.research());
        ResearchHelper.queue(ItemCatAmulet.research());
        ResearchHelper.queue(ItemIchorPouch.research());
        ResearchHelper.queue(ItemBlockTalisman.research());
        ResearchHelper.queue(ItemPlacementMirror.research());
        ResearchHelper.queue(ItemProtoclay.research());
        ResearchHelper.queue(ItemFocusShadowbeam.research());
        ResearchHelper.queue(ItemFocusXPDrain.research());
        // 1.7.10: ItemFocusRecall.getResearchItem() answered null while Thaumcraft's allowMirrors was off (the recipe stayed).
        if (ThaumcraftApi.isMirrorsAllowed()) ResearchHelper.queue(ItemFocusRecall.research());
    }

    @Override
    public void registerClient() {
        if (!ConfigHandler.enableKami) return;
        KamiGearClient.register();
    }

    /** The creative-tab wand with the Ichor rod and cap and every primal full (1.7.10: 100000 centivis of each). */
    private static ItemStack ichorWand() {
        try {
            ItemStack wand = KamiResearch.tc("itemWandCasting", 0);
            WandHelper.setRod(wand, WandRod.byTag(RodIchorcloth.TAG));
            WandHelper.setCap(wand, WandCap.byTag(CapIchor.TAG));
            for (Aspect primal : Aspect.getPrimalAspects()) WandHelper.addRealVis(wand, primal, 100000, true);
            return wand;
        } catch (RuntimeException e) {
            ThaumicTinkerer.LOG.warn("Kami: the Ichor wand for the creative tab could not be built: {}", e.toString());
            return ItemStack.EMPTY;
        }
    }
}
