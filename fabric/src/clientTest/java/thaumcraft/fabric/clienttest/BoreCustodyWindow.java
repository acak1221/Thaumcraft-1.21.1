package thaumcraft.fabric.clienttest;

/** Observer-private deadlines only. Never pauses or writes a native clock.
 * The sole authorized coverage gap is OUTBOUND -> RECOVERY -> READY, once. */
final class BoreCustodyWindow {
    enum State { NEW, OUTBOUND, RECOVERY, READY, STOWED }
    private State state=State.NEW;
    private long departureTick=-1,departureWall=-1,pickupTick=-1,pickupWall=-1;

    State state() { return state; }
    boolean coverageGap() { return state==State.OUTBOUND || state==State.RECOVERY; }
    long departureTick() { return departureTick; }
    long pickupTick() { return pickupTick; }

    void depart(long tick,long wall,long elapsed,long rTicks,long rWall,String phase) {
        require(state==State.NEW && phase.equals("R"),"Exactly ONE custody-only departure in R");
        require(elapsed>=0 && elapsed<=1100 && rTicks>=0 && rTicks<=40 && rWall>=0 && rWall<=3_000_000_000L,
                "Custody starts within original inherited lifetime and R40/3 gate");
        departureTick=tick;departureWall=wall;state=State.OUTBOUND;
    }
    void picked(long tick,long wall,long elapsed,long rTicks,long rWall,String phase) {
        check(tick,wall,elapsed,rTicks,rWall,phase);
        require(state==State.OUTBOUND,"One actual same-UUID pickup, never replay");
        pickupTick=tick;pickupWall=wall;state=State.RECOVERY;
    }
    void recovered(long tick,long wall,long elapsed,long rTicks,long rWall,String phase) {
        check(tick,wall,elapsed,rTicks,rWall,phase);
        require(state==State.RECOVERY,"Readiness only after actual pickup");
        state=State.READY;
    }
    void stowed(long tick,long wall,long elapsed,long rTicks,long rWall,String phase) {
        check(tick,wall,elapsed,rTicks,rWall,phase);
        require(state==State.READY,"One genuine slot7 stow after restored readiness");
        state=State.STOWED;
    }
    void check(long tick,long wall,long elapsed,long rTicks,long rWall,String phase) {
        require(tick>=0 && wall>=0 && elapsed>=0 && rTicks>=0 && rWall>=0,"Actual monotone native/wall observations");
        if(state==State.NEW || state==State.STOWED)return;
        require(phase.equals("R"),"No custody coverage exception in B or another phase");
        if(coverageGap()) {
            require(tick>=departureTick && wall>=departureWall && tick-departureTick<=200 && wall-departureWall<=14_000_000_000L,
                    "Custody total200native/14wall cap; never extend");
            if(state==State.OUTBOUND)require(tick-departureTick<=60 && wall-departureWall<=4_000_000_000L,
                    "Actual pickup60native/4wall cap; never retry");
            else require(tick>=pickupTick && wall>=pickupWall && tick-pickupTick<=140 && wall-pickupWall<=10_000_000_000L,
                    "Restored readiness140native/10wall after pickup");
        }
        require(elapsed<=1600 && rTicks<=1000 && rWall<=60_000_000_000L,"Original jar stow elapsed1600/R1000/60 cap");
    }
    private static void require(boolean condition,String message) {if(!condition)throw new IllegalStateException(message);}
}
