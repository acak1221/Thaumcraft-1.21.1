package thaumcraft.client.lib;

import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import thaumcraft.common.lib.network.ResearchNoticePayload;
import thaumcraft.common.lib.research.ResearchCatalog;

/** Original path-specific research toast/clue/highlight consumer. Native full snapshot alone owns knowledge. */
public final class ResearchNotifications {
    private static final ResearchToastQueue POPUPS=new ResearchToastQueue();
    private static final Set<String> HIGHLIGHTED=new LinkedHashSet<>();
    private static final SoundEvent LEARN=SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft","learn"));
    private static int column=-5,row=-6;
    private static boolean hasQueuedResearch;
    private static String selectedCategory="BASICS";
    private ResearchNotifications() {}
    public static long now() { return System.nanoTime()/1_000_000L; }
    public static void reset() { POPUPS.clear(); HIGHLIGHTED.clear(); column=-5;row=-6;hasQueuedResearch=false;selectedCategory="BASICS"; }
    public static void clearHighlights() { HIGHLIGHTED.clear(); }
    public static String selectedCategory() { return selectedCategory; }
    public static void selectCategory(String key) { selectedCategory=key; }
    public static boolean highlighted(String key) { return HIGHLIGHTED.contains(key); }
    public static void highlightAspects() { HIGHLIGHTED.add("ASPECTS");HIGHLIGHTED.add("BASICS"); }
    public static int lastColumn() { return column; }
    public static int lastRow() { return row; }
    public static boolean hasQueuedResearch() { return hasQueuedResearch; }
    public static void rememberCenter(int newColumn,int newRow) { column=newColumn;row=newRow;hasQueuedResearch=true; }
    public static ResearchToastQueue.Frame frame() { return POPUPS.frame(now()); }
    static void receive(ResearchNoticePayload payload) {
        if(!PlayerNotifications.current())return;
        String key=payload.key(); boolean clue=key.startsWith("@");
        var definition=ResearchCatalog.get(clue?key.substring(1):key);
        if(definition==null)return; // Unknown addon/stale-server keys never crash native client rendering.
        var client=Minecraft.getInstance();
        if(clue) {
            PlayerNotifications.addNotification("§a"+Component.translatable("tc.addclue").getString());
            client.player.playSound(LEARN,.2F,1F+client.level.random.nextFloat()*.1F);
        } else if(!definition.isVirtual()) {
            POPUPS.add(key,now());column=definition.column();row=definition.row();hasQueuedResearch=true;
            HIGHLIGHTED.add(key);HIGHLIGHTED.add(definition.category());
        }
    }
}
