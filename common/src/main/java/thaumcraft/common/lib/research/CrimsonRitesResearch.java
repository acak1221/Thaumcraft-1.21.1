package thaumcraft.common.lib.research;

/** Direct original completeResearch: no prerequisite, sibling, aspect or consumption pass. */
public final class CrimsonRitesResearch {
    public static final String KEY="CRIMSON";
    private CrimsonRitesResearch(){}
    public static boolean study(PlayerKnowledge knowledge,boolean wuss){
        var definition=ResearchCatalog.get(KEY);
        if(definition==null)throw new IllegalStateException("Original CRIMSON research must be loaded before studying Rites");
        if(!knowledge.addResearch(KEY))return false;
        int warp=definition.currentWarp();
        if(warp>0&&!wuss){knowledge.addWarpPerm(warp-warp/2);knowledge.addWarpSticky(warp/2);knowledge.setWarpCounter(knowledge.getTotalWarp());}
        return true;
    }
}
