package thaumcraft.client.lib;

import java.util.ArrayList;
import java.util.List;

/** Original GuiResearchPopup: one item for3000ms, quartic36px top-left slide, blank transition frame. */
public final class ResearchToastQueue {
    public record Frame(String key,int y) {}
    private final List<String> research = new ArrayList<>();
    private long started;
    public void add(String key,long now) { if(research.isEmpty()) started=now; research.add(key); }
    public void clear() { research.clear(); started=0; }
    public Frame frame(long now) {
        if(research.isEmpty()) return null;
        double time=(double)(now-started)/3000D;
        if(time<0D || time>1D) { research.removeFirst(); started=research.isEmpty()?0:now; return null; }
        double slide=time*2D; if(slide>1D) slide=2D-slide;
        slide=1D-slide*4D; if(slide<0D) slide=0D; slide*=slide; slide*=slide;
        return new Frame(research.getFirst(),-(int)(slide*36D));
    }
}
