package thaumcraft.fabric.clienttest;

import java.lang.reflect.RecordComponent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import static thaumcraft.fabric.clienttest.BoreItemLifecycleCore.*;

/** Actual detached production-core tests. ALL calls/data below are synthetic
 * callback MODELS, never native receipts or Minecraft stubs. Run JDK21 only. */
public final class BoreItemLifecycleCoreChecks {
    private static int checks;
    private static boolean wrappedSuite;
    private static final class Fixture {
        final Object manager = new Object(), item = new Object(), world = new Object(), thread = new Object();
        final Object callback = new Object(), storage = new Object(), sections = new Object(), section = new Object();
        final Object instances = new Object(), known = new Object(), nil = new Object();
        final UUID uuid = UUID.fromString("38b3dcb4-151e-40b8-8b5f-b77461ce49c8");
        final Physical physical = new Physical("{id:\"minecraft:egg\",count:1,components:{x:3b}}", 3241, 0, 5, 0, false,
                List.of(1.0, 2.0, 3.0), List.of(0.0, 0.0, 0.0), List.of(0.0, 1.0, 2.0, 1.0, 2.0, 3.0));
        final Object delegate = wrappedSuite ? new Object() : callback;
        final Object wrapperEntity = wrappedSuite ? item : null;
        final Sample live = new Sample(manager, manager, item, world, thread, uuid, 1, callback, item, manager,
                storage, sections, 42, section, section, instances, known, true, true, item, nil, false, "NONE", physical, 10, 133510,
                delegate, wrapperEntity, wrappedSuite ? Representation.EXACT_ARCHITECTURY13_WRAPPER : Representation.DIRECT_NATIVE_CALLBACK);
        final BoreItemLifecycleCore core = new BoreItemLifecycleCore(live);
        Sample hidden() { return edit(live, "publicOwner", null); }
        Sample after() { return edit(hidden(), "removed", true, "reason", "UNLOADED_TO_CHUNK", "callback", nil,
                "callbackEntity", null, "callbackManager", null, "member", false, "known", false, "indexedSection", null,
                "canonicalCallback", null, "wrapperEntity", null, "representation", Representation.NATIVE_NULL); }
        Sample nested(boolean visible) { return edit(live, "removed", true, "reason", "UNLOADED_TO_CHUNK", "member", false, "publicOwner", visible ? item : null); }
        void retire() { core.trackingHead(live, Origin.VISIBILITY); core.trackingReturn(hidden(), Origin.VISIBILITY); }
        void pendingHidden() { retire(); core.unloadHead(hidden(), Origin.UNLOAD); }
        void reasonHidden() { pendingHidden(); core.innerReason(hidden(), Origin.INNER_REASON, "UNLOADED_TO_CHUNK"); }
        void completeHidden() { reasonHidden(); core.unloadReturn(after(), Origin.UNLOAD); }
    }
    public static void main(String[] args) {
        for (boolean wrapped : List.of(false, true)) {
            wrappedSuite = wrapped; int before = checks; lifecycleChecks();
            ok(checks - before == 394, "All original394 checks preserved for " + (wrapped ? "WRAPPED" : "DIRECT"));
        }
        representationChecks();
        fastutilSaveChecks();
        System.out.println("PASS " + checks + " detached production-core checks; synthetic callback/metadata models, NOT native verification");
    }
    private static void lifecycleChecks() {
        Fixture f = new Fixture();
        f.retire(); ok(f.core.hidden() && !f.core.completed(), "Tracking is not unload");
        Sample deferred = edit(f.hidden(), "tick", 700L, "time", 134200L);
        f.core.checkpoint(deferred); f.core.checkpoint(deferred); ok(f.core.hidden(), "Hidden custody spans POSTs/deferred storage without invented body");
        f.core.unloadHead(deferred, Origin.UNLOAD); f.core.innerReason(deferred, Origin.INNER_REASON, "UNLOADED_TO_CHUNK");
        f.core.unloadReturn(edit(f.after(), "tick", 700L, "time", 134200L), Origin.UNLOAD);
        ok(f.core.completed() && f.core.completedUnload().serial() == 2 && f.core.retirement().serial() == 1, "Completed hidden pair correlates exact operation serials");
        f.core.rebind(f.physical); ok(f.core.completed(), "Rebind comparison grants no new live owner or baseline");

        Fixture visible = new Fixture();
        visible.core.unloadHead(visible.live, Origin.UNLOAD); visible.core.innerReason(visible.live, Origin.INNER_REASON, "UNLOADED_TO_CHUNK");
        visible.core.trackingHead(visible.nested(true), Origin.NESTED_ON_REMOVE);
        visible.core.trackingReturn(visible.nested(false), Origin.NESTED_ON_REMOVE);
        visible.core.unloadReturn(visible.after(), Origin.UNLOAD);
        ok(visible.core.completed() && visible.core.completedUnload().serial() == 1 && visible.core.trackingSerial() == 2, "Visible unload handles true inner removed onRemove tracking without relabeling live body");

        // Every reference relation is checked with ==, not equal UUID/class or supplied canonical flags.
        for (String field : List.of("manager", "canonicalManager", "entity", "world", "thread", "callback", "callbackEntity", "callbackManager",
                "sectionStorage", "sectionMap", "indexedSection", "knownSet", "nullCallback")) {
            bad("Tracking HEAD wrong " + field, x -> x.core.trackingHead(edit(x.live, field, new Object()), Origin.VISIBILITY));
        }
        for (String field : List.of("uuid", "generation", "member", "known", "publicOwner", "removed", "reason")) {
            Object value = switch (field) { case "uuid" -> new UUID(0,1); case "generation" -> 2; case "member", "known" -> false;
                case "removed" -> true; case "reason" -> "DISCARDED"; default -> null; };
            bad("Tracking HEAD wrong " + field, x -> x.core.trackingHead(edit(x.live, field, value), Origin.VISIBILITY));
        }
        for (String field : List.of("manager", "entity", "world", "thread", "callback", "callbackEntity", "callbackManager", "sectionStorage", "sectionMap",
                "section", "indexedSection", "allInstances", "knownSet", "publicOwner")) {
            bad("Tracking RETURN wrong " + field, x -> { x.core.trackingHead(x.live, Origin.VISIBILITY); x.core.trackingReturn(edit(x.hidden(), field, new Object()), Origin.VISIBILITY); });
        }
        for (String field : List.of("known", "member", "removed")) {
            bad("Tracking RETURN wrong " + field, x -> { x.core.trackingHead(x.live, Origin.VISIBILITY); x.core.trackingReturn(edit(x.hidden(), field, field.equals("removed")), Origin.VISIBILITY); });
        }
        bad("Tracking wrong section key", x -> { x.core.trackingHead(x.live, Origin.VISIBILITY); x.core.trackingReturn(edit(x.hidden(), "sectionKey", 43L), Origin.VISIBILITY); });
        for (String clock : List.of("tick", "time")) {
            bad("Tracking mismatched actual clock " + clock, x -> { x.core.trackingHead(x.live, Origin.VISIBILITY); x.core.trackingReturn(edit(x.hidden(), clock, clock.equals("tick") ? 11L : 133511L), Origin.VISIBILITY); });
            bad("Unload mismatched actual clock " + clock, x -> { x.reasonHidden(); x.core.unloadReturn(edit(x.after(), clock, clock.equals("tick") ? 11L : 133511L), Origin.UNLOAD); });
        }
        for (Object[] drift : List.of(new Object[]{"age", 3242}, new Object[]{"counter", 1}, new Object[]{"delay", 1},
                new Object[]{"fullStack", "{id:\"minecraft:egg\",count:2,components:{x:3b}}"}, new Object[]{"fullStack", "{id:\"minecraft:egg\",count:1,components:{x:3}}"},
                new Object[]{"position", List.of(1.1, 2.0, 3.0)}, new Object[]{"velocity", List.of(0.1, 0.0, 0.0)},
                new Object[]{"bounds", List.of(0.1, 1.0, 2.0, 1.1, 2.0, 3.0)})) {
            String field = (String) drift[0]; Object val = drift[1];
            bad("Tracking physical drift " + field, x -> { x.core.trackingHead(x.live, Origin.VISIBILITY); x.core.trackingReturn(edit(x.hidden(), "physical", edit(x.physical, field, val)), Origin.VISIBILITY); });
            bad("Hidden checkpoint physical drift " + field, x -> { x.retire(); x.core.checkpoint(edit(x.hidden(), "physical", edit(x.physical, field, val))); });
            bad("Inner reason physical drift " + field, x -> { x.pendingHidden(); x.core.innerReason(edit(x.hidden(), "physical", edit(x.physical, field, val)), Origin.INNER_REASON, "UNLOADED_TO_CHUNK"); });
            bad("Unload RETURN physical drift " + field, x -> { x.reasonHidden(); x.core.unloadReturn(edit(x.after(), "physical", edit(x.physical, field, val)), Origin.UNLOAD); });
        }
        for (String field : List.of("manager", "entity", "callback", "callbackManager", "callbackEntity", "indexedSection", "section", "allInstances", "knownSet")) {
            bad("Hidden unload HEAD wrong " + field, x -> { x.retire(); x.core.unloadHead(edit(x.hidden(), field, new Object()), Origin.UNLOAD); });
        }
        bad("Hidden unload HEAD absent knownUUID", x -> { x.retire(); x.core.unloadHead(edit(x.hidden(), "known", false), Origin.UNLOAD); });
        bad("No prior hidden tracking certificate", x -> x.core.unloadHead(x.hidden(), Origin.UNLOAD));
        rejected("Initially unseen hidden/late hidden baseline", () -> new BoreItemLifecycleCore(new Fixture().hidden()));
        for (String field : List.of("health", "invulnerable", "age", "delay")) rejected("Unsupported physical " + field, () -> {
            Physical p = new Fixture().physical; edit(p, field, switch (field) { case "health" -> 4; case "invulnerable" -> true; case "age" -> 6000; default -> -1; });
        });
        bad("Already removed foreign retirement", x -> x.core.trackingHead(x.nested(true), Origin.VISIBILITY));
        bad("Nested onRemove without pending unload", x -> x.core.trackingHead(x.nested(true), Origin.NESTED_ON_REMOVE));
        bad("Nested onRemove before actual inner reason", x -> { x.core.unloadHead(x.live, Origin.UNLOAD); x.core.trackingHead(x.nested(true), Origin.NESTED_ON_REMOVE); });
        bad("Nested onRemove must have removed=true", x -> { x.core.unloadHead(x.live, Origin.UNLOAD); x.core.innerReason(x.live, Origin.INNER_REASON, "UNLOADED_TO_CHUNK"); x.core.trackingHead(edit(x.live, "member", false), Origin.NESTED_ON_REMOVE); });
        bad("Nested onRemove known UUID not yet removed", x -> { x.core.unloadHead(x.live, Origin.UNLOAD); x.core.innerReason(x.live, Origin.INNER_REASON, "UNLOADED_TO_CHUNK"); x.core.trackingHead(edit(x.nested(true), "known", false), Origin.NESTED_ON_REMOVE); });
        bad("Nested onRemove section membership already removed", x -> { x.core.unloadHead(x.live, Origin.UNLOAD); x.core.innerReason(x.live, Origin.INNER_REASON, "UNLOADED_TO_CHUNK"); x.core.trackingHead(edit(x.nested(true), "member", true), Origin.NESTED_ON_REMOVE); });
        bad("Visible unload missing nested retirement", x -> { x.core.unloadHead(x.live, Origin.UNLOAD); x.core.innerReason(x.live, Origin.INNER_REASON, "UNLOADED_TO_CHUNK"); x.core.unloadReturn(x.after(), Origin.UNLOAD); });
        bad("Wrong tracking origin", x -> x.core.trackingHead(x.live, Origin.UNLOAD));
        bad("Wrong unload origin", x -> x.core.unloadHead(x.live, Origin.VISIBILITY));
        bad("Double tracking HEAD", x -> { x.core.trackingHead(x.live, Origin.VISIBILITY); x.core.trackingHead(x.live, Origin.VISIBILITY); });
        bad("Tracking RETURN without HEAD", x -> x.core.trackingReturn(x.hidden(), Origin.VISIBILITY));
        bad("Double tracking RETURN", x -> { x.retire(); x.core.trackingReturn(x.hidden(), Origin.VISIBILITY); });
        bad("Tracking pending POST/missing RETURN/throw", x -> { x.core.trackingHead(x.live, Origin.VISIBILITY); x.core.checkpoint(x.live); });
        bad("Unload pending POST/missing RETURN/throw", x -> { x.pendingHidden(); x.core.checkpoint(x.hidden()); });
        bad("Unload HEAD twice", x -> { x.pendingHidden(); x.core.unloadHead(x.hidden(), Origin.UNLOAD); });
        bad("Unload RETURN without HEAD", x -> x.core.unloadReturn(x.after(), Origin.UNLOAD));
        bad("Missing actual inner reason", x -> { x.pendingHidden(); x.core.unloadReturn(x.after(), Origin.UNLOAD); });
        bad("Wrong inner reason", x -> { x.pendingHidden(); x.core.innerReason(x.hidden(), Origin.INNER_REASON, "DISCARDED"); });
        bad("Double actual inner reason", x -> { x.reasonHidden(); x.core.innerReason(x.hidden(), Origin.INNER_REASON, "UNLOADED_TO_CHUNK"); });
        bad("Canceled setter is not unload", x -> { x.reasonHidden(); x.core.unloadReturn(x.hidden(), Origin.UNLOAD); });
        bad("Hidden public retracking", x -> { x.retire(); x.core.checkpoint(x.live); });
        bad("Hidden cannot supply native body/current POST", x -> { x.retire(); x.core.liveBoundary(x.live); });
        bad("Hidden cannot reopen tracking", x -> { x.retire(); x.core.trackingHead(x.live, Origin.VISIBILITY); });
        bad("Hidden cannot rebind before completed unload", x -> { x.retire(); x.core.rebind(x.physical); });
        for (Object[] wrong : List.of(new Object[]{"known", true}, new Object[]{"member", true}, new Object[]{"callback", new Object()},
                new Object[]{"publicOwner", new Object()}, new Object[]{"removed", false}, new Object[]{"reason", "DISCARDED"},
                new Object[]{"generation", 2}, new Object[]{"uuid", new UUID(0,1)}, new Object[]{"indexedSection", new Object()})) {
            bad("Unload RETURN incomplete/wrong " + wrong[0], x -> { x.reasonHidden(); x.core.unloadReturn(edit(x.after(), (String) wrong[0], wrong[1]), Origin.UNLOAD); });
        }
        bad("Double unload RETURN", x -> { x.completeHidden(); x.core.unloadReturn(x.after(), Origin.UNLOAD); });
        bad("Double unload after completed", x -> { x.completeHidden(); x.core.unloadHead(x.live, Origin.UNLOAD); });
        bad("Completed old owner physical drift", x -> { x.completeHidden(); x.core.checkpoint(edit(x.after(), "physical", edit(x.physical, "age", 3242))); });
        bad("Replacement after completion", x -> { x.completeHidden(); x.core.checkpoint(edit(x.after(), "publicOwner", new Object())); });
        bad("Reload cannot reset Age", x -> { x.completeHidden(); x.core.rebind(edit(x.physical, "age", 0)); });
        bad("Reload requires native counter0", x -> { x.completeHidden(); x.core.rebind(edit(x.physical, "counter", 1)); });
        Object actor=new Object(),server=new Object(),world=new Object();
        ActorContext stoppedActor=new ActorContext(actor,actor,server,server,world,world,true,false,true,true);
        ok(actorAllowed(stoppedActor,ActorUse.TRACKING_UNLOAD_CONTEXT),"Actual removed/untracked actor is lifecycle-only native-stop context");
        ok(!actorAllowed(stoppedActor,ActorUse.BIRTH_SOURCE_BODY_CURRENT_POST),"Stop cannot authorize birth/source/body/current POST");
        for(String field:List.of("actor","rootActor","server","rootServer","world","rootWorld"))
            ok(!actorAllowed(edit(stoppedActor,field,new Object()),ActorUse.TRACKING_UNLOAD_CONTEXT),"Stop actor wrong actual reference "+field);
        for(String field:List.of("stopHeadReturned","actualNativeStopFrame"))
            ok(!actorAllowed(edit(stoppedActor,field,false),ActorUse.TRACKING_UNLOAD_CONTEXT),"No stop flag/spine spoof "+field);
        ok(!actorAllowed(edit(stoppedActor,"stopHeadReturned",false,"actualNativeStopFrame",false),ActorUse.TRACKING_UNLOAD_CONTEXT),"Running removed actor unsupported");
        ok(actorAllowed(edit(stoppedActor,"removed",false,"publicSameOwner",true,"stopHeadReturned",false,"actualNativeStopFrame",false),ActorUse.TRACKING_UNLOAD_CONTEXT),"Running live actor flags remain ordinary actual context");
    }
    private static void representationChecks() {
        Object nativeLoader = new Object(), foreignLoader = new Object();
        List<CallbackField> fields = List.of(new CallbackField("val$entity", "Lnet/minecraft/world/entity/Entity;", 0x1010),
                new CallbackField("val$callback", "Lnet/minecraft/world/level/entity/EntityInLevelCallback;", 0x1010), new CallbackField("lastSectionKey", "J", 0x0002));
        ok(representation(NATIVE_CALLBACK_CLASS, nativeLoader, nativeLoader, List.of()) == Representation.DIRECT_NATIVE_CALLBACK, "Symbolic direct metadata shape");
        ok(representation(ARCHITECTURY13_WRAPPER_CLASS, nativeLoader, nativeLoader, fields) == Representation.EXACT_ARCHITECTURY13_WRAPPER, "Symbolic exact wrapper metadata shape");
        ok(representation(ARCHITECTURY13_WRAPPER_CLASS, nativeLoader, nativeLoader, List.of(fields.get(2),fields.get(0),fields.get(1))) == Representation.EXACT_ARCHITECTURY13_WRAPPER, "Declared-field order does not manufacture authority");
        for(String name : List.of("dev.architectury.hooks.level.entity.fabric.EntityHooksImpl$2", "dev.architectury.other.Callback", "foreign.Callback", "net.minecraft.world.level.entity.EntityInLevelCallback"))
            rejected("No namespace/interface/unknown shape acceptance "+name, () -> representation(name,nativeLoader,nativeLoader,fields));
        rejected("No Java-null class", () -> representation(null,nativeLoader,nativeLoader,fields));
        rejected("No null field schema", () -> representation(ARCHITECTURY13_WRAPPER_CLASS,nativeLoader,nativeLoader,null));
        rejected("No bootstrap/null native loader fallback", () -> representation(NATIVE_CALLBACK_CLASS,null,null,List.of()));
        for(String name:List.of(NATIVE_CALLBACK_CLASS,ARCHITECTURY13_WRAPPER_CLASS))
            rejected("Foreign raw loader "+name, () -> representation(name,foreignLoader,nativeLoader,name.equals(NATIVE_CALLBACK_CLASS)?List.of():fields));
        rejected("Wrong delegate class", () -> nativeDelegateClass("foreign.Callback",nativeLoader,nativeLoader));
        rejected("Wrapper-of-wrapper delegate", () -> nativeDelegateClass(ARCHITECTURY13_WRAPPER_CLASS,nativeLoader,nativeLoader));
        rejected("Native NULL is not a native delegate class", () -> nativeDelegateClass("net.minecraft.world.level.entity.EntityInLevelCallback$1",nativeLoader,nativeLoader));
        rejected("Java-null delegate metadata", () -> nativeDelegateClass(null,nativeLoader,nativeLoader));
        rejected("Foreign delegate loader", () -> nativeDelegateClass(NATIVE_CALLBACK_CLASS,foreignLoader,nativeLoader));
        nativeDelegateClass(NATIVE_CALLBACK_CLASS,nativeLoader,nativeLoader);checks++;
        for(int i=0;i<3;i++) {
            final int at=i;var missing=new java.util.ArrayList<>(fields);missing.remove(i);
            rejected("Missing declared field "+i, () -> representation(ARCHITECTURY13_WRAPPER_CLASS,nativeLoader,nativeLoader,missing));
            for(CallbackField wrong:List.of(new CallbackField(fields.get(i).name(),"Ljava/lang/Object;",fields.get(i).modifiers()),
                    new CallbackField(fields.get(i).name(),fields.get(i).descriptor(),0),new CallbackField(fields.get(i).name(),fields.get(i).descriptor(),0x0010),
                    new CallbackField(fields.get(i).name(),fields.get(i).descriptor(),0x1018),new CallbackField("unknown",fields.get(i).descriptor(),fields.get(i).modifiers()))) {
                var altered=new java.util.ArrayList<>(fields);altered.set(at,wrong);
                rejected("Wrong exact field descriptor/modifiers/name "+i, () -> representation(ARCHITECTURY13_WRAPPER_CLASS,nativeLoader,nativeLoader,altered));
            }
        }
        rejected("Extra declared wrapper field", () -> representation(ARCHITECTURY13_WRAPPER_CLASS,nativeLoader,nativeLoader,List.of(fields.get(0),fields.get(1),fields.get(2),fields.get(0))));
        rejected("Duplicate field cannot hide missing field", () -> representation(ARCHITECTURY13_WRAPPER_CLASS,nativeLoader,nativeLoader,List.of(fields.get(0),fields.get(1),fields.get(1))));
        rejected("Direct shape cannot be relabeled wrapper", () -> representation(NATIVE_CALLBACK_CLASS,nativeLoader,nativeLoader,fields));
        // Actual Java-reference pinning in the production core, not classloader
        // proof. The fixture Objects are synthetic, not Minecraft stand-ins.
        for(boolean wrapped:List.of(false,true)) {
            wrappedSuite=wrapped;
            bad("Changed raw with same delegate", x -> x.core.liveBoundary(edit(x.live,"callback",new Object())));
            bad("Changed delegate with same raw", x -> x.core.liveBoundary(edit(x.live,"canonicalCallback",new Object())));
            bad("Replacement raw at tracking RETURN", x -> {x.core.trackingHead(x.live,Origin.VISIBILITY);x.core.trackingReturn(edit(x.hidden(),"callback",new Object()),Origin.VISIBILITY);});
            bad("Replacement delegate at tracking RETURN", x -> {x.core.trackingHead(x.live,Origin.VISIBILITY);x.core.trackingReturn(edit(x.hidden(),"canonicalCallback",new Object()),Origin.VISIBILITY);});
            bad("Replacement raw at hidden unload HEAD", x -> {x.retire();x.core.unloadHead(edit(x.hidden(),"callback",new Object()),Origin.UNLOAD);});
            bad("Replacement delegate at inner reason", x -> {x.pendingHidden();x.core.innerReason(edit(x.hidden(),"canonicalCallback",new Object()),Origin.INNER_REASON,"UNLOADED_TO_CHUNK");});
            bad("Wrong captured entity", x -> x.core.trackingHead(edit(x.live,"wrapperEntity",new Object()),Origin.VISIBILITY));
            bad("Missing canonical delegate", x -> x.core.trackingHead(edit(x.live,"canonicalCallback",null),Origin.VISIBILITY));
            bad("Native NULL cannot be canonical delegate", x -> x.core.trackingHead(edit(x.live,"canonicalCallback",x.nil),Origin.VISIBILITY));
            bad("Missing representation", x -> x.core.trackingHead(edit(x.live,"representation",null),Origin.VISIBILITY));
            bad("Wrong representation", x -> x.core.trackingHead(edit(x.live,"representation",wrapped?Representation.DIRECT_NATIVE_CALLBACK:Representation.EXACT_ARCHITECTURY13_WRAPPER),Origin.VISIBILITY));
            bad("Wrapped NULL not completed NULL", x -> {x.reasonHidden();x.core.unloadReturn(edit(x.after(),"callback",x.callback,"canonicalCallback",x.nil,"wrapperEntity",x.item,"representation",Representation.EXACT_ARCHITECTURY13_WRAPPER),Origin.UNLOAD);});
            bad("Stale delegate on raw native NULL", x -> {x.reasonHidden();x.core.unloadReturn(edit(x.after(),"canonicalCallback",x.delegate),Origin.UNLOAD);});
            bad("Stale captured entity on raw native NULL", x -> {x.reasonHidden();x.core.unloadReturn(edit(x.after(),"wrapperEntity",x.item),Origin.UNLOAD);});
            bad("Stale native metadata on raw NULL", x -> {x.reasonHidden();x.core.unloadReturn(edit(x.after(),"callbackEntity",x.item),Origin.UNLOAD);});
            bad("Representation replacement across hidden checkpoint", x -> {x.retire();x.core.checkpoint(edit(x.hidden(),"representation",Representation.NATIVE_NULL));});
        }
        String reason="(Lnet/minecraft/world/entity/Entity$RemovalReason;)V";
        ok(wrapperDelegation("onMove","()V","onMove","()V","setPosRaw","(DDD)V"),"Symbolic exact onMove adjacent delegate/caller metadata");
        ok(wrapperDelegation("onRemove",reason,"onRemove",reason,"setRemoved",reason),"Symbolic exact onRemove adjacent delegate/caller metadata");
        for(String[] wrong:List.of(new String[]{"factory","()V","onMove","()V","setPosRaw","(DDD)V"},
                new String[]{"onMove","(I)V","onMove","(I)V","setPosRaw","(DDD)V"},new String[]{"onMove","()V","onRemove","()V","setPosRaw","(DDD)V"},
                new String[]{"onMove","()V","onMove","(I)V","setPosRaw","(DDD)V"},new String[]{"onMove","()V","onMove","()V","setPos","(DDD)V"},
                new String[]{"onMove","()V","onMove","()V","setPosRaw","()V"},new String[]{"onRemove",reason,"onRemove",reason,"remove",reason},
                new String[]{"onRemove","()V","onRemove","()V","setRemoved",reason},new String[]{"onRemove",reason,"onRemove",reason,"setRemoved","()V"}))
            ok(!wrapperDelegation(wrong[0],wrong[1],wrong[2],wrong[3],wrong[4],wrong[5]),"Wrong/foreign symbolic adjacent frame metadata rejected");
    }
    private static void fastutilSaveChecks() {
        ok(checks == 936, "All previous936 checks run unchanged before the save-spine metadata cases");
        String[] exact = {"removeIf", "(Ljava/util/function/LongPredicate;)Z", "method_31842", "(J)Z", "saveAll", "()V"};
        ok(fastutilSaveDelegation(exact[0],exact[1],exact[2],exact[3],exact[4],exact[5]), "Synthetic exact adjacent save-spine method metadata, not native Class/loader proof");
        for (int field=0;field<exact.length;field++) {
            for(String wrong:List.of("foreign", "", "()V".equals(exact[field]) ? "()Z" : "()V")) {
                String[] changed=exact.clone();changed[field]=wrong;
                ok(!fastutilSaveDelegation(changed[0],changed[1],changed[2],changed[3],changed[4],changed[5]), "Wrong adjacent save-spine method/descriptor field"+field);
            }
            String[] missing=exact.clone();missing[field]=null;
            ok(!fastutilSaveDelegation(missing[0],missing[1],missing[2],missing[3],missing[4],missing[5]), "Missing save-spine method metadata field"+field);
        }
        for(String overload:List.of("(Ljava/util/function/Predicate;)Z", "(Lit/unimi/dsi/fastutil/longs/LongPredicate;)Z"))
            ok(!fastutilSaveDelegation("removeIf",overload,"method_31842","(J)Z","saveAll","()V"), "Unsupported removeIf overload remains rejected");
        ok(!fastutilSaveDelegation("removeIf",exact[1],"processChunkUnload","(J)Z","saveAll","()V"), "No other native child/adjacency substitution");
        ok(!fastutilSaveDelegation("removeIf",exact[1],"method_31842","(J)Z","processUnloads","()V"), "No general native/library caller waiver");
    }
    private static void bad(String name, Consumer<Fixture> action) {
        Fixture f = new Fixture(); rejected(name, () -> action.accept(f));
        ok(f.core.status() == Status.FAILED, name + " permanently latched FAILED");
        rejected(name + " no retry/reset", () -> f.core.liveBoundary(f.live));
    }
    private static void rejected(String name, Runnable action) {
        try { action.run(); throw new AssertionError("Accepted negative: " + name); }
        catch (IllegalStateException expected) { checks++; }
    }
    private static void ok(boolean condition, String name) { if (!condition) throw new AssertionError(name); checks++; }
    @SuppressWarnings("unchecked") private static <T> T edit(T record, Object... changes) {
        Map<String, Object> replacements = new LinkedHashMap<>(); for (int i = 0; i < changes.length; i += 2) replacements.put((String) changes[i], changes[i + 1]);
        try {
            RecordComponent[] fields = record.getClass().getRecordComponents(); Class<?>[] types = new Class<?>[fields.length]; Object[] values = new Object[fields.length];
            for (int i = 0; i < fields.length; i++) { types[i] = fields[i].getType(); values[i] = replacements.containsKey(fields[i].getName()) ? replacements.get(fields[i].getName()) : fields[i].getAccessor().invoke(record); }
            var constructor = record.getClass().getDeclaredConstructor(types); constructor.setAccessible(true); return (T) constructor.newInstance(values);
        } catch (java.lang.reflect.InvocationTargetException error) {
            if (error.getCause() instanceof RuntimeException runtime) throw runtime;
            throw new AssertionError(error.getCause());
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }
}
