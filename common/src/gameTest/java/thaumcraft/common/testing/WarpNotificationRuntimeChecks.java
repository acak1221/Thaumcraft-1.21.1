package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ResearchSettings;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftSettings;
import thaumcraft.common.items.ItemResearchNotes;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.ResearchPurchaseNetworking;
import thaumcraft.common.lib.network.WarpMessagePayload;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.research.ResearchNoteGenerator;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;

/** Actual server paths/native S2C packets. Inputs and solved flag are declared fixtures, not acquisition/puzzle replay. */
public final class WarpNotificationRuntimeChecks {
    private WarpNotificationRuntimeChecks() {}
    public static void run(GameTestHelper test) {
        ResearchSettings original = ThaumcraftSettings.server();
        try (var f = new Fixture(test)) {
            f.open(); configure(false, false); codec(f);
            var knowledge = KnowledgeAccess.get(f.player);
            var dispatcher = f.level.getServer().getCommands().getDispatcher();
            var source = f.player.createCommandSourceStack().withPermission(2).withSuppressedOutput();
            messages(f);
            dispatcher.execute("tc warp @s set 2 TEMP", source);
            f.check(messages(f).isEmpty(), "original command SET syncs state but sends no delta popup packet");
            dispatcher.execute("tc warp @s ADD -100 TEMP", source);
            f.check(knowledge.getWarpTemp() == 0 && messages(f).equals(List.of(new WarpMessagePayload(-100, (byte)2))),
                    "actual command ADD packet retains REQUESTED negative100 even when native nonnegative channel clamps actual loss to two");
            dispatcher.execute("tc warp @s add 7 PERM ignored", source);
            f.check(messages(f).equals(List.of(new WarpMessagePayload(7, (byte)1))), "original extra-token fallback sends sticky kind, never stale PERM type");
            dispatcher.execute("tc warp @s add 0 PERM", source);
            f.check(messages(f).equals(List.of(new WarpMessagePayload(0, (byte)0))), "zero requested command ADD still has original packet; client deliberately displays nothing");

            prepare(f, "JARBRAIN");
            dispatcher.execute("tc research @s JARBRAIN", source);
            f.check(messages(f).equals(List.of(new WarpMessagePayload(2,(byte)0), new WarpMessagePayload(1,(byte)1)))
                            && knowledge.getWarpPerm()==2 && knowledge.getWarpSticky()==1,
                    "actual recursive admin research emits original warp3 PERM2 then sticky1 after single state award");
            dispatcher.execute("tc research @s JARBRAIN", source);
            f.check(messages(f).isEmpty() && knowledge.getWarpPerm()==2 && knowledge.getWarpSticky()==1, "known-key stop never replays warp or notifications");

            prepare(f, "COREBUTCHER");
            var bought=ResearchPurchaseNetworking.handle(f.player,"COREBUTCHER");
            f.check(bought.success() && messages(f).equals(List.of(new WarpMessagePayload(1,(byte)0)))
                            && knowledge.getWarpPerm()==1 && knowledge.getWarpSticky()==0,
                    "actual paid Secondary purchase emits only its newly completed original permanent1 message");
            f.check(!ResearchPurchaseNetworking.handle(f.player,"COREBUTCHER").success() && messages(f).isEmpty(), "refused purchase cannot issue free/replayed popup");

            prepare(f,"JARBRAIN");
            var generated=ResearchNoteGenerator.generate("JARBRAIN",new Random(42));
            var note=ItemResearchNotes.create(generated.withCells(generated.cells(),true),false); // Declared trusted solved-state fixture.
            f.player.setItemInHand(InteractionHand.MAIN_HAND,note);
            f.player.gameMode.useItem(f.player,f.level,note,InteractionHand.MAIN_HAND);
            f.check(f.player.getMainHandItem().isEmpty() && knowledge.isResearchComplete("JARBRAIN")
                            && messages(f).equals(List.of(new WarpMessagePayload(2,(byte)0),new WarpMessagePayload(1,(byte)1))),
                    "actual survival note use consumes physical note and emits only its committed warp3 split");

            prepare(f,"JARBRAIN"); configure(true,false);
            var book=new ItemStack(ThaumcraftContent.THAUMONOMICON_CHEAT.get());
            f.player.setItemInHand(InteractionHand.MAIN_HAND,book);
            f.player.gameMode.useItem(f.player,f.level,book,InteractionHand.MAIN_HAND);
            f.check(messages(f).equals(List.of(new WarpMessagePayload(2,(byte)0),new WarpMessagePayload(1,(byte)1)))
                            && f.player.getMainHandItem()==book && book.getCount()==1,
                    "real enabled cheat-sheet consumer emits only missing key's original split without consuming book or notifying known keys");
            f.player.gameMode.useItem(f.player,f.level,book,InteractionHand.MAIN_HAND);
            f.check(messages(f).isEmpty(), "same book action cannot duplicate completed research notifications");
            prepare(f,"JARBRAIN"); configure(true,true);
            f.player.gameMode.useItem(f.player,f.level,book,InteractionHand.MAIN_HAND);
            f.check(knowledge.isResearchComplete("JARBRAIN") && knowledge.getWarpPerm()==0 && knowledge.getWarpSticky()==0 && messages(f).isEmpty(),
                    "actual server Wuss gate admits research without warp award, popup or whispers packet");
        } catch(com.mojang.brigadier.exceptions.CommandSyntaxException failure) { throw new IllegalStateException("Native warp notification command failed",failure); }
        finally { ThaumcraftSettings.configureServer(original); }
        test.succeed();
    }
    private static void configure(boolean cheat,boolean wuss) { ThaumcraftSettings.configureServer(new ResearchSettings(cheat,true,true,true,wuss,0,100)); }
    private static void prepare(Fixture f,String absent) {
        var knowledge=KnowledgeAccess.get(f.player); knowledge.reset(); knowledge.setInitializedAspects(true);
        for(var research:ResearchCatalog.all()) if(research.isEnabled()&&!research.key().equals(absent)) knowledge.addResearch(research.key());
        for(Aspect aspect:Aspect.aspects.values()) knowledge.setAspectPool(aspect.getTag(),100);
        messages(f);
    }
    private static List<WarpMessagePayload> messages(Fixture f) {
        f.connection.flushChannel(); f.channel.runPendingTasks(); Object packet; var result=new ArrayList<WarpMessagePayload>();
        while((packet=f.channel.readOutbound())!=null) {
            try { if(packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof WarpMessagePayload warp) result.add(warp); }
            finally { ReferenceCountUtil.release(packet); }
        }
        return List.copyOf(result);
    }
    private static void codec(Fixture f) {
        var wire=new RegistryFriendlyByteBuf(Unpooled.buffer(),f.level.registryAccess());
        try {
            var original=new WarpMessagePayload(-100,(byte)1); WarpMessagePayload.STREAM_CODEC.encode(wire,original);
            f.check(wire.readableBytes()==5 && wire.getInt(0)==-100 && wire.getByte(4)==1, "actual original packet order is requested int delta then signed byte kind, exactly five bytes");
            f.check(WarpMessagePayload.STREAM_CODEC.decode(wire).equals(original)&&!wire.isReadable(), "native S2C codec roundtrip has no extra client state or unsafe registry lookup");
        } finally { wire.release(); }
    }
}
