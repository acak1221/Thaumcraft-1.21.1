package thaumcraft.common.config;

import com.google.common.collect.ImmutableSet;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.npc.VillagerProfession;
import thaumcraft.common.Thaumcraft;

/**
 * The two villager professions of the TC4 villages.
 *
 * <p>Oracle {@code thaumcraft/common/lib/world/VillageBankerManager.java:41-56} and
 * {@code thaumcraft/common/lib/world/VillageWizardManager.java:35-53}: the two managers of
 * the original were Forge village creation handlers. The banker home of the original spawns
 * a villager whose profession is {@code ConfigEntities.entBankerId} (191) and the wizard
 * tower one whose profession is {@code ConfigEntities.entWizardId} (190), and the managers
 * add their trade table to every villager of that profession.
 *
 * <p>1.21.1 has no village piece system and no villager trade registry, but it does have
 * the villager profession registry. The two professions of the original are registered here
 * and the trade tables of the original are attached to them by
 * {@code VillagerVillageTradesMixin}.
 *
 * <p>The professions carry no job site: the original had none either, the piece of the
 * village placed the villager with its profession directly. A job site that no point of
 * interest grants keeps the profession of the villager, which is what the original did.
 */
public final class ThaumcraftVillageProfessions {
    private static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(Thaumcraft.MODID, Registries.VILLAGER_PROFESSION);

    /** The profession of the banker of the original, oracle ConfigEntities:83. */
    public static final RegistrySupplier<VillagerProfession> BANKER =
            PROFESSIONS.register("banker", () -> profession("banker"));
    /** The profession of the wizard of the original, oracle ConfigEntities:82. */
    public static final RegistrySupplier<VillagerProfession> WIZARD =
            PROFESSIONS.register("wizard", () -> profession("wizard"));

    private static boolean registered;

    private ThaumcraftVillageProfessions() {}

    /**
     * A profession without a job site.
     *
     * <p>The two predicates of the original profession never accept a point of interest,
     * so no villager acquires the profession on its own and no villager loses it: the
     * pieces of the original placed the villager with the profession themselves.
     */
    private static VillagerProfession profession(String name) {
        return new VillagerProfession(name, holder -> false, holder -> false,
                ImmutableSet.of(), ImmutableSet.of(), null);
    }

    public static void register() {
        if (registered) return;
        registered = true;
        PROFESSIONS.register();
    }
}
