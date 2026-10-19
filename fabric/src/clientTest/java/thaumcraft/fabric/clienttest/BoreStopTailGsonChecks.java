package thaumcraft.fabric.clienttest;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Actual helper + real target Gson, with declared synthetic callbacks/data only.
 * No Minecraft objects/stubs, native callbacks, save reads, or acceptance proof. */
public final class BoreStopTailGsonChecks {
    private static int checks;
    private record FixtureRef(String type,String version) {}
    private static void check(boolean value) {checks++;if(!value)throw new AssertionError("Gson stop integration assertion "+checks);}

    public static void main(String[] args) throws Exception {
        if(args.length!=1)throw new IllegalArgumentException("One NEW ignored root sandbox required");
        Path home=Path.of(args[0]).toAbsolutePath().normalize();
        check(home.getFileName().toString().equals("run-clienttest") && home.toString().contains(".tmp") && !Files.exists(home));
        Files.createDirectories(home);
        for(int scenario=0;scenario<3;scenario++) {
            final int mode=scenario;
            String run="client-smoke-"+UUID.nameUUIDFromBytes(("root-real-gson-"+mode).getBytes(StandardCharsets.UTF_8));
            Path tail=home.resolve("bore-tail-"+run+".json");
            var report=new LinkedHashMap<String,Object>();
            report.put("ref",new FixtureRef("declared_fixture","v1"));
            report.put("array",List.of(1,true,"quotes\" and newline\n"));
            if(mode==2)report.put("deliberate_nonfinite_fixture",Double.NaN);
            Throwable[] root={null};int[] calls=new int[6];
            var result=BoreStopTail.complete(new BoreStopTail.Context(run,tail,true,true,true),report,new BoreStopTail.Ops() {
                public void finalizeLedger() {calls[0]++;if(mode==1)throw new IllegalStateException("declared finalizer fault",new IllegalArgumentException("cause"));}
                public void unregisterServerPost() {calls[1]++;}
                public void unregisterEntityAdd() {calls[2]++;}
                public void unregisterPrivateStopHook() {calls[3]++;}
                public void clearActiveReference() {calls[4]++;}
                public Throwable rootFailure() {return root[0];}
                public void latchFirstFailure(Throwable error) {if(root[0]==null)root[0]=error;}
                public String encodeFullEnvelope(Map<String,Object> envelope) {
                    calls[5]++;check(envelope.get("report")==report);
                    return new GsonBuilder().setPrettyPrinting().create().toJson(envelope);
                }
            });
            for(int count:calls)check(count==1);
            check(result.primaryCloseCompleted() && result.proofCloseCompleted());
            byte[] bytes=Files.readAllBytes(tail);
            JsonObject actual=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject proof=JsonParser.parseString(Files.readString(tail.resolveSibling(tail.getFileName()+".write-proof.json"))).getAsJsonObject();
            check(actual.get("run_id").getAsString().equals(run));
            check(actual.get("status").getAsString().equals(result.tailStatus()));
            check(proof.get("sha256").getAsString().equals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))));
            check(proof.get("UTF8bytes").getAsLong()==bytes.length && result.utf8Bytes()==bytes.length);
            check(proof.get("primary_close_completed").getAsBoolean());
            check(!actual.getAsJsonObject("stop_completion").has("primary_close_completed"));
            if(mode==0) {
                check(result.status().equals("STOPPED_UNINSPECTED") && result.fullEvidence() && root[0]==null);
                check(actual.getAsJsonObject("report").getAsJsonObject("ref").get("version").getAsString().equals("v1"));
                check(actual.getAsJsonObject("report").getAsJsonArray("array").size()==3);
            } else if(mode==1) {
                check(result.status().equals("FAILED_OR_INCOMPLETE") && result.fullEvidence());
                check(root[0] instanceof IllegalStateException && root[0].getCause() instanceof IllegalArgumentException);
                check(actual.getAsJsonObject("stop_completion").getAsJsonArray("stop_errors").size()>0);
            } else {
                check(result.status().equals("FAILED_DIAGNOSTIC_ONLY") && !result.fullEvidence());
                check(!actual.has("report") && !proof.get("full_evidence").getAsBoolean() && root[0]!=null);
                check(actual.get("error").getAsString().contains("NaN"));
            }
        }
        System.out.println("ACTUAL Fabric-runtime Gson stop integration assertions: "+checks+"; scenarios: 3; no game/native evidence");
    }
}
