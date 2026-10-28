package thaumic.tinkerer.common.block.camo;

/** A device that acts once for every rising redstone edge (the Remote Placer and the Transvector Dislocator). */
public interface RedstonePulseReceiver {
    /** The pulse: called on the server, one game tick after the edge was seen (1.7.10 scheduled a tick at rate 1). */
    void receiveRedstonePulse();
}
