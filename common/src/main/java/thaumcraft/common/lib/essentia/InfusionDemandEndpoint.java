package thaumcraft.common.lib.essentia;

import thaumcraft.api.aspects.Aspect;

/** Internal matrix demand half of one paid wireless unit, not an addon transaction API. */
public interface InfusionDemandEndpoint {
    int getPendingEssentia(Aspect aspect);
    /** Field-only reduction; requires EssentiaTransfers' active raw mutation scope. */
    void consumeInfusionEssentiaRaw(Aspect aspect);
    /** Called only after the source AND demand have been reduced. */
    void notifyInfusionEssentiaChanged();
}
