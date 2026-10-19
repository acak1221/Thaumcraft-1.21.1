package thaumcraft.fabric.clienttest;

/** Detached deadline/state checks, NOT evidence of native rescue or stock. */
public final class BoreCustodyWindowChecks {
    private static int checks;
    private static void expect(boolean value) {checks++;if(!value)throw new AssertionError("custody state");}
    private static void denied(Runnable action) {checks++;try {action.run();}catch(IllegalStateException expected) {return;}throw new AssertionError("accepted forbidden custody transition");}
    private static BoreCustodyWindow departed() {
        var w=new BoreCustodyWindow();w.depart(100,1_000_000_000L,100,10,1_000_000_000L,"R");return w;
    }
    public static void main(String[] ignored) {
        var w=departed();expect(w.coverageGap());
        w.picked(160,5_000_000_000L,160,70,5_000_000_000L,"R");expect(w.coverageGap());
        w.recovered(300,15_000_000_000L,300,210,15_000_000_000L,"R");expect(!w.coverageGap());
        w.stowed(900,51_000_000_000L,900,810,51_000_000_000L,"R");expect(w.state()==BoreCustodyWindow.State.STOWED);
        denied(()->w.depart(901,52_000_000_000L,901,811,52_000_000_000L,"R"));
        denied(()->departed().picked(161,2_000_000_000L,161,71,2_000_000_000L,"R"));
        denied(()->departed().picked(101,5_000_000_001L,101,11,1_000_000_001L,"R"));
        denied(()->departed().recovered(101,2_000_000_000L,101,11,2_000_000_000L,"R"));
        denied(()->departed().check(101,2_000_000_000L,101,11,2_000_000_000L,"B"));
        denied(()->new BoreCustodyWindow().depart(1,1,1101,1,1,"R"));
        denied(()->new BoreCustodyWindow().depart(1,1,1,41,1,"R"));
        denied(()->new BoreCustodyWindow().depart(1,1,1,1,3_000_000_001L,"R"));
        for(boolean wall:new boolean[]{false,true}) {
            var a=departed();a.picked(159,4_999_999_999L,159,69,4_999_999_999L,"R");
            denied(()->a.recovered(wall?299:300,wall?15_000_000_000L:5_000_000_000L,300,210,15_000_000_000L,"R"));
        }
        var a=departed();a.picked(110,2_000_000_000L,110,20,2_000_000_000L,"R");a.recovered(120,3_000_000_000L,120,30,3_000_000_000L,"R");
        denied(()->a.stowed(1601,4_000_000_000L,1601,900,4_000_000_000L,"R"));
        denied(()->a.stowed(1500,4_000_000_000L,1500,1001,4_000_000_000L,"R"));
        denied(()->a.stowed(1500,62_000_000_000L,1500,900,60_000_000_001L,"R"));
        denied(()->a.picked(121,4_000_000_000L,121,31,4_000_000_000L,"R"));
        System.out.println("Detached Bore custody deadline/state assertions: "+checks);
    }
}
