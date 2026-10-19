package thaumcraft.common.lib.potions;

/** Per-instance equivalent of TC4 PotionEffect.getCurativeItems().clear(). */
public interface FluxEffectAccess {
    boolean thaumcraft$isUncurableFlux();
    void thaumcraft$setUncurableFlux();
}
