import java.nio.file.Files;
import java.util.TreeMap;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordingFile;
import thaumcraft.client.lib.NotificationQueue;

/** Compile with the real NotificationQueue source; run with -XX:-UseTLAB -XX:-DoEscapeAnalysis.
 * Disabling TLAB makes OutsideTLAB an exact allocation-event counter, not sampling.
 * Counts Java allocation sites, not optimised production heap pressure. */
public final class NotificationAllocationProbe {
    public static void main(String[] args) throws Exception {
        var queue = new NotificationQueue(2000);
        int rows = args.length == 0 ? 0 : Integer.parseInt(args[0]);
        for (int i = 0; i < rows; i++) queue.add("row", null, 0, 0);
        for (int i = 0; i < 2000; i++) queue.update(i);
        var output = Files.createTempFile("notification-allocations", ".jfr");
        try (var recording = new Recording()) {
            recording.enable("jdk.ObjectAllocationOutsideTLAB").withStackTrace();
            recording.start();
            for (int i = 2000; i < 3000; i++) queue.update(i);
            recording.stop();
            recording.dump(output);
        }
        var counts = new TreeMap<String, Integer>();
        try (var events = new RecordingFile(output)) {
            while (events.hasMoreEvents()) {
                var event = events.readEvent();
                if (event.getStackTrace() == null) continue;
                for (var frame : event.getStackTrace().getFrames()) {
                    if (frame.getMethod().getType().getName().equals(NotificationQueue.class.getName())
                            && frame.getMethod().getName().equals("update")) {
                        counts.merge("NotificationQueue.java:" + frame.getLineNumber() + " "
                                + event.getClass("objectClass").getName(), 1, Integer::sum);
                        break;
                    }
                }
            }
        }
        Files.delete(output);
        System.out.println("MEASURED rows=" + rows + " update calls=1000 allocationEvents=" + counts.values().stream().mapToInt(Integer::intValue).sum());
        counts.forEach((site, count) -> System.out.println(site + " events=" + count));
    }
}
