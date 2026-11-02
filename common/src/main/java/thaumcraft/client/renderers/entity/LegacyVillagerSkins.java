package thaumcraft.client.renderers.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.VillagerProfession;
import thaumcraft.common.config.ThaumcraftVillageProfessions;

/** Full villager skins registered by the original ClientProxy, rather than modern profession overlays. */
public final class LegacyVillagerSkins {
    private static final ResourceLocation BANKER = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/moneychanger.png");
    private static final ResourceLocation WIZARD = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wizard.png");

    private LegacyVillagerSkins() {}

    /** Returns null for professions that use the native biome and profession clothing layers. */
    public static ResourceLocation texture(VillagerProfession profession) {
        if (profession == ThaumcraftVillageProfessions.BANKER.get()) return BANKER;
        if (profession == ThaumcraftVillageProfessions.WIZARD.get()) return WIZARD;
        return null;
    }
}
