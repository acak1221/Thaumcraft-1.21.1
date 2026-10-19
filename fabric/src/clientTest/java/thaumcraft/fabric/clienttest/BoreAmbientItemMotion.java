package thaumcraft.fabric.clienttest;

import java.util.List;

/** Private detached bookkeeping for a separately proven ambient birth. This
 * class neither grants birth eligibility nor invokes any native entity method.
 * Native callbacks, actual ownership/scope, and final disk reconciliation belong
 * to the root Ledger/independent reader. No merge/pickup/despawn is supported. */
final class BoreAmbientItemMotion {
    record State(int counter,int age,int health,int pickupDelay,boolean invulnerable,String fullStack,
                 List<Double> position,List<Double> velocity,List<Double> bounds) {
        State {
            position=List.copyOf(position);velocity=List.copyOf(velocity);bounds=List.copyOf(bounds);
            require(counter>=0 && age>=0 && age<6000 && health==5 && pickupDelay>=0 && pickupDelay<=10 && !invulnerable,"Supported native ambient physical state");
            require(fullStack!=null && !fullStack.isBlank() && fullStack.length()<=65536,"Bounded complete ambient stack");
            require(position.size()==3 && velocity.size()==3 && bounds.size()==6,"Actual ambient native vectors/body");
            for(var vector:List.of(position,velocity,bounds))for(Double n:vector)require(n!=null && Double.isFinite(n),"Finite native ambient vector");
            for(int i=0;i<3;i++)require(bounds.get(i)<bounds.get(i+3),"Nonempty actual native ambient body");
        }
    }
    record Pair(long tick,State before,State after) {}
    private Object owner;
    private State latest,head;
    private long headTick=-1,bindingTick,lastPairTick=-1,unloadTick=-1;
    private int generation=1;
    private boolean unloaded;
    private Pair pair;

    BoreAmbientItemMotion(Object actualOwner,long tick,State actualBirth) {
        require(actualOwner!=null && tick>=0 && actualBirth.counter()==0 && actualBirth.age()==0 && actualBirth.pickupDelay()==10,"Actual native spawn-at-location new owner, not a late item baseline");
        owner=actualOwner;latest=actualBirth;bindingTick=tick;
    }
    Object owner() {return owner;}
    State latest() {return latest;}
    int generation() {return generation;}
    boolean unloaded() {return unloaded;}
    boolean frameOpen() {return head!=null;}
    long bindingTick() {return bindingTick;}
    long unloadTick() {return unloadTick;}
    Pair pair() {return pair;}
    void head(Object actualOwner,long tick,State actual) {
        require(!unloaded && actualOwner==owner && head==null && tick>=bindingTick && tick>lastPairTick,"One current actual ambient native item body");
        require(actual.equals(latest),"Ambient item changed outside its paired native body");head=actual;headTick=tick;
    }
    void returned(Object actualOwner,long tick,State actual) {
        require(!unloaded && actualOwner==owner && head!=null && tick==headTick,"Paired same-owner/tick ambient native RETURN");
        require(actual.counter()==head.counter()+1 && actual.age()==head.age()+1 && actual.pickupDelay()==Math.max(0,head.pickupDelay()-1),"Native ambient counter/Age/delay advance exactly once");
        require(actual.health()==head.health() && actual.invulnerable()==head.invulnerable() && actual.fullStack().equals(head.fullStack()),"No ambient item damage/merge/component loss or rewrite");
        pair=new Pair(tick,head,actual);latest=actual;lastPairTick=tick;head=null;
    }
    void unload(Object actualOwner,long tick,State actual) {
        require(!unloaded && head==null && owner==actualOwner && actual.equals(latest),"Native ambient unload preserves exact latest owner/state");
        unloaded=true;unloadTick=tick;
    }
    void rebind(Object actualOwner,long tick,State actual) {
        require(unloaded && head==null && actualOwner!=null && actualOwner!=owner && tick>=unloadTick && actual.counter()==0,"Ambient owner rebind only after witnessed unload");
        require(actual.age()==latest.age() && actual.health()==latest.health() && actual.pickupDelay()==latest.pickupDelay()
                && actual.invulnerable()==latest.invulnerable() && actual.fullStack().equals(latest.fullStack()) && actual.position().equals(latest.position())
                && actual.velocity().equals(latest.velocity()) && actual.bounds().equals(latest.bounds()),"No invented ambient item motion/stock across uncovered unload gap");
        owner=actualOwner;latest=actual;head=null;pair=null;bindingTick=tick;generation++;unloaded=false;
    }
    String post(Object actualOwner,long tick,State actual) {
        require(!unloaded && head==null && owner==actualOwner && actual.equals(latest),"Actual POST equals unchanged birth/rebind or latest paired native item body");
        return pair!=null && pair.tick()==tick?"ACTUAL_PAIRED_NATIVE_ITEM_BODY_THIS_POST":"UNCHANGED_LATEST_BINDING_OR_NATIVE_RETURN";
    }
    private static void require(boolean condition,String message) {if(!condition)throw new IllegalStateException(message);}
}
