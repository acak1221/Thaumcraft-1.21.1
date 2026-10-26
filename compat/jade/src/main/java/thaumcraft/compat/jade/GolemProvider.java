package thaumcraft.compat.jade;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemUpgrade;

/**
 * A golem's master, core, upgrades and load. Every one of them is synchronized entity data, so
 * the client reads them directly; the server sends nothing.
 */
enum GolemProvider implements IEntityComponentProvider {
    INSTANCE;

    static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("thaumcraft", "golem");
    /** The core numbers of GolemData, in order, as the names of their core items. */
    private static final String[] CORES = {"fill", "empty", "gather", "harvest", "guard", "liquid", "essentia", "lumber", "use",
            "butcher", "sorting", "fishing"};

    @Override public ResourceLocation getUid() { return UID; }

    @Override public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        if (!(accessor.getEntity() instanceof EntityGolemBase golem)) return;
        if (!golem.getOwnerName().isEmpty()) tooltip.add(gray("tc.port.jade.owner", golem.getOwnerName()));
        int core = golem.getGolemCore();
        Component coreName = core >= 0 && core < CORES.length ? Component.translatable("item.thaumcraft.golem_core_" + CORES[core])
                : Component.translatable("tc.golem.core.none");
        tooltip.add(gray("tc.port.jade.core", coreName));
        List<Component> upgrades = new ArrayList<>();
        for (int slot = 0; slot < golem.getUpgradeSlotCount(); slot++) {
            int id = golem.getUpgrade(slot);
            if (id == GolemUpgrade.EMPTY) continue;
            GolemUpgrade upgrade = GolemUpgrade.byId(id);
            if (upgrade != null) upgrades.add(Component.translatable("item.thaumcraft.golem_upgrade_" + upgrade.key));
        }
        if (!upgrades.isEmpty()) {
            MutableComponent list = Component.empty();
            for (int i = 0; i < upgrades.size(); i++) {
                if (i > 0) list.append(", ");
                list.append(upgrades.get(i));
            }
            tooltip.add(gray("tc.port.jade.upgrades", list));
        }
        var carried = golem.getCarriedItem();
        if (!carried.isEmpty()) tooltip.add(gray("tc.port.jade.carrying", carried.getCount(), carried.getHoverName()));
        if (core == GolemData.NO_CORE) return;
        if (golem.getEssentiaAmount() > 0 && golem.getEssentiaAspect() != null) {
            tooltip.add(gray("tc.port.jade.carrying", golem.getEssentiaAmount(), golem.getEssentiaAspect().getName()));
        }
    }

    private static Component gray(String key, Object... arguments) {
        return Component.translatable(key, arguments).withStyle(ChatFormatting.GRAY);
    }
}
