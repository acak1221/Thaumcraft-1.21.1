package thaumcraft.fabric.clienttest;

import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

/** Read installed private class/resource bytes BEFORE opening the clone. No
 * source-path guess, game action, world read/write or observer installation. */
final class BoreNativeBinding {
    record Inputs(String fingerprint,Map<String,String> resourceSha256) {}
    static Inputs installed() throws Exception {
        var hashes=new TreeMap<String,String>();
        String manifest="/thaumcraft-client-smoke.mixins.json";
        byte[] bytes=resource(manifest);hashes.put(manifest,sha(bytes));
        var roots=new ArrayList<String>();
        for(String name:new String[]{"BookClientSmoke","BoreClientSequence","BoreClientLedger","BoreClientParentModel","BoreClientEvidence",
                "BoreClientEvidenceMotion","BoreClientEvidenceGeometry","BoreOwnedForeground","BoreResearchGuiActions","BoreResearchNotePlan",
                "FocalNativeShift","ClientTestServerLifecycle","BoreCustodyWindow","BoreInheritedItemWitness","BoreAmbientItemWitness","BoreAmbientItemMotion","BorePassiveStackSnapshot","BoreNativeBinding","BoreStopTail",
                "BoreItemLifecycle","BoreItemLifecycleCore"})
            roots.add("thaumcraft.fabric.clienttest."+name);
        var json=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();
        if(!json.get("required").getAsBoolean() || json.getAsJsonObject("injectors").get("defaultRequire").getAsInt()!=1
                || json.getAsJsonArray("mixins").size()+json.getAsJsonArray("client").size()!=22)
            throw new IllegalStateException("Exactly22 required private client-test observer classes");
        String pkg=json.get("package").getAsString();
        // Mixin classes are intentionally NOT loaded as ordinary runtime
        // classes. Read their installed bytes without Class.forName.
        for(String kind:new String[]{"mixins","client"})for(var entry:json.getAsJsonArray(kind)) {
            String path="/"+(pkg+"."+entry.getAsString()).replace('.','/')+".class";
            hashes.put(path,sha(resource(path)));
        }
        // Exact external wrapper/factory/setter-mixin and save-spine bytes, through
        // this installed loader. NEVER load/initialize a mixin as a class or
        // call the factory. Remapped installed bytes are not upstream hashes.
        for(String path:new String[]{"/dev/architectury/hooks/level/entity/fabric/EntityHooksImpl$1.class",
                "/dev/architectury/hooks/level/entity/fabric/EntityHooksImpl.class",
                "/dev/architectury/mixin/fabric/MixinEntity.class",
                "/it/unimi/dsi/fastutil/longs/LongCollection.class"})
            hashes.put(path,sha(resource(path)));
        for(String name:roots) {
            Class<?> root=Class.forName(name,false,BoreNativeBinding.class.getClassLoader());
            for(Class<?> member:root.getNestMembers()) {
                String path="/"+member.getName().replace('.','/')+".class";
                hashes.put(path,sha(resource(path)));
            }
        }
        String parent="/thaumcraft-clienttest/bore-parent-model.json";hashes.put(parent,sha(resource(parent)));
        var digest=MessageDigest.getInstance("SHA-256");
        hashes.forEach((name,hash)->digest.update((name+"\0"+hash+"\n").getBytes(StandardCharsets.UTF_8)));
        return new Inputs(HexFormat.of().formatHex(digest.digest()),Map.copyOf(hashes));
    }
    private static byte[] resource(String path) throws Exception {
        try(var input=BoreNativeBinding.class.getResourceAsStream(path)) {
            if(input==null)throw new IllegalStateException("Missing installed private native binding resource "+path);
            byte[] bytes=input.readNBytes(8_000_001);
            if(bytes.length==0 || bytes.length>8_000_000)throw new IllegalStateException("Unbounded native binding resource "+path);
            return bytes;
        }
    }
    private static String sha(byte[] bytes) throws Exception {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
}
