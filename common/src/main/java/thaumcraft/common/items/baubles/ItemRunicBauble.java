package thaumcraft.common.items.baubles;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.IRunicArmor;
import thaumcraft.api.baubles.BaubleType;
import thaumcraft.api.baubles.IBauble;
import thaumcraft.common.lib.runic.RunicShield;

/** Original runic rings, amulets and girdles, with their exact per-variant base charge. */
public final class ItemRunicBauble extends Item implements IBauble, IRunicArmor {
    public enum Upgrade { NONE, CHARGED, HEALING, KINETIC, EMERGENCY }
    private final BaubleType type;
    private final int charge;
    private final Upgrade upgrade;
    public ItemRunicBauble(Properties properties, BaubleType type, int charge, Upgrade upgrade) {
        super(properties); this.type = type; this.charge = charge; this.upgrade = upgrade;
    }
    @Override public BaubleType getBaubleType(ItemStack stack) { return type; }
    @Override public int getRunicCharge(ItemStack stack) { return charge; }
    public Upgrade upgrade() { return upgrade; }
    @Override public void onEquipped(ItemStack stack, LivingEntity wearer) { RunicShield.markDirty(wearer); }
    @Override public void onUnequipped(ItemStack stack, LivingEntity wearer) { RunicShield.markDirty(wearer); }
}
