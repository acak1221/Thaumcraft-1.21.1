package thaumcraft.fabric.clienttest;

import java.util.List;

/** Detached actual Java state machine checks, NOT evidence of a native birth. */
public final class BoreAmbientItemMotionChecks {
    private static int checks;
    private static BoreAmbientItemMotion.State state(int counter,int age,int delay,double x,String stack) {
        return new BoreAmbientItemMotion.State(counter,age,5,delay,false,stack,List.of(x,216.,378.),List.of(.1,0.,0.),List.of(x-.125,216.,377.875,x+.125,216.25,378.125));
    }
    private static void check(boolean value) {checks++;if(!value)throw new AssertionError("ambient item state");}
    private static void denied(Runnable action) {checks++;try {action.run();}catch(IllegalStateException expected) {return;}throw new AssertionError("Forbidden ambient state accepted");}
    public static void main(String[] ignored) {
        Object owner=new Object(),replacement=new Object();var birth=state(0,0,10,1422.,"bone2/full-components");
        var m=new BoreAmbientItemMotion(owner,100,birth);
        check(m.post(owner,100,birth).equals("UNCHANGED_LATEST_BINDING_OR_NATIVE_RETURN"));
        denied(()->m.head(replacement,101,birth));m.head(owner,101,birth);
        denied(()->m.post(owner,101,birth));denied(()->m.returned(owner,102,state(1,1,9,1422.1,"bone2/full-components")));
        denied(()->m.returned(owner,101,state(1,1,9,1422.1,"bone3/full-components")));
        var after=state(1,1,9,1422.1,"bone2/full-components");m.returned(owner,101,after);
        check(m.post(owner,101,after).equals("ACTUAL_PAIRED_NATIVE_ITEM_BODY_THIS_POST"));
        denied(()->m.head(owner,101,after));denied(()->m.head(owner,102,state(1,1,9,1423.,"bone2/full-components")));
        denied(()->m.rebind(replacement,102,state(0,1,9,1422.1,"bone2/full-components")));
        m.unload(owner,102,after);check(m.unloaded());
        denied(()->m.head(owner,103,after));denied(()->m.rebind(replacement,103,state(0,1,9,1423.,"bone2/full-components")));
        var rebound=state(0,1,9,1422.1,"bone2/full-components");m.rebind(replacement,103,rebound);check(m.generation()==2 && !m.unloaded());
        check(m.post(replacement,103,rebound).equals("UNCHANGED_LATEST_BINDING_OR_NATIVE_RETURN"));
        m.head(replacement,104,rebound);denied(()->m.returned(replacement,104,state(1,1,8,1422.2,"bone2/full-components")));
        m.returned(replacement,104,state(1,2,8,1422.2,"bone2/full-components"));
        check(m.latest().age()==2);
        Object next=new Object();m.unload(replacement,104,m.latest());m.rebind(next,104,state(0,2,8,1422.2,"bone2/full-components"));
        check(m.pair()==null);
        check(m.post(next,104,m.latest()).equals("UNCHANGED_LATEST_BINDING_OR_NATIVE_RETURN"));
        denied(()->new BoreAmbientItemMotion(new Object(),0,state(1,1,10,0.,"bone")));
        System.out.println("Detached Bore ambient item state assertions: "+checks);
    }
}
