package thaumcraft.client.lib;

import java.util.ArrayList;
import java.util.List;

/** Original1500ms quadratic research-point flight, kept independent of Minecraft rendering. */
public final class AspectFlightQueue {
    public record Flight(String aspect, int color, float startX, float startY, long created, long expire) {}
    public record Frame(double x, double y, float alpha) {}
    private final List<Flight> flights = new ArrayList<>();
    public void add(String aspect, int color, float startX, float startY, long created) {
        flights.add(new Flight(aspect,color,startX,startY,created,created+1500));
    }
    public List<Flight> update(long now) { flights.removeIf(value -> value.expire < now); return List.copyOf(flights); }
    public void clear() { flights.clear(); }
    public static Frame frame(Flight flight,long now,double width,double height) {
        if (flight.created > now || flight.expire < now) return null;
        int screenWidth=(int)width,screenHeight=(int)height;
        int startX=(int)(width*(double)flight.startX),startY=(int)(height*(double)flight.startY);
        int controlX=(int)((float)screenWidth*(.25F+flight.startX)),controlY=(int)((float)screenHeight*flight.startY);
        double t=(double)(now-flight.created)/(flight.expire-flight.created);
        double x=(1-t)*(1-t)*startX+2*(1-t)*t*controlX+t*t*screenWidth;
        double y=(1-t)*(1-t)*startY+2*(1-t)*t*controlY+t*t*-8;
        float alpha=1F;
        if(t<(double).3F) alpha=(float)(t/(double).3F);
        else if(t>(double).66F) alpha=(float)(1D-(t-(double).66F)/(double).34F);
        return new Frame(x,y,alpha);
    }
}
