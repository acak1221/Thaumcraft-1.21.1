package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftPrimalFoci;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.EntityAspectBindings;
import thaumcraft.common.lib.research.ResearchClues;
import thaumcraft.common.lib.research.ResearchTriggerBindings;
import thaumcraft.common.lib.research.ScanManager;

/** Real native variants, the production scan receiver and a real20-world-tick entity target. Packet dispatch/client rendering remain unclaimed. */
public final class ResearchClueRuntimeChecks {
    private ResearchClueRuntimeChecks() {}

    public static void itemCluesUseOriginalMetadataDamageAndNbt(GameTestHelper test, ServerPlayer player) {
        var knowledge = KnowledgeAccess.get(player); var old = knowledge.copy();
        try {
            knowledge.reset(); for (Aspect aspect : Aspect.aspects.values()) knowledge.addDiscoveredAspect(aspect.getTag());
            ItemStack silver = new ItemStack(ThaumcraftContent.MATERIALS.get("quick_silver").get());
            silver.set(DataComponents.CUSTOM_NAME, Component.literal("ore shortcut preserves original named quicksilver behavior"));
            test.assertTrue(ResearchTriggerBindings.matches("JARBRAIN", ResearchClues.Context.item(silver)), "JARBRAIN itemResource3 is quicksilver; original ore shortcut accepts its NBT");
            test.assertTrue(!ResearchTriggerBindings.matches("JARBRAIN", ResearchClues.Context.item(new ItemStack(ThaumcraftContent.MATERIALS.get("zombie_brain").get()))),
                    "a zombie brain is not the original itemResource3 trigger");
            var bow = new ItemStack(Items.BOW); bow.setDamageValue(40);
            test.assertTrue(ResearchTriggerBindings.matches("BONEBOW", ResearchClues.Context.item(bow)), "bow trigger wildcard/ignoreDamage accepts worn native bow");
            bow.set(DataComponents.CUSTOM_NAME, Component.literal("custom NBT is compared"));
            test.assertTrue(!ResearchTriggerBindings.matches("BONEBOW", ResearchClues.Context.item(bow)), "ignoreDamage does not ignore arbitrary bow NBT");
            for (var wool : List.of(Items.WHITE_WOOL, Items.RED_WOOL, Items.BLACK_WOOL))
                test.assertTrue(ResearchTriggerBindings.matches("TINYFEZ", ResearchClues.Context.item(new ItemStack(wool))), "old wool metadata wildcard maps to native dye identities");
            var namedWool = new ItemStack(Items.RED_WOOL); namedWool.set(DataComponents.CUSTOM_NAME, Component.literal("named wool"));
            test.assertTrue(!ResearchTriggerBindings.matches("TINYFEZ", ResearchClues.Context.item(namedWool)), "default original wool has no ore shortcut and retains NBT equality");
            // Original ConfigResearch:414 binds the primal focus to the primal staff rod.
            test.assertTrue(ResearchTriggerBindings.matches("ROD_primal_staff", ResearchClues.Context.item(new ItemStack(ThaumcraftPrimalFoci.FOCUS_PRIMAL.get()))),
                    "the primal focus is the original item trigger of the primal staff rod research");
            test.assertTrue(!ResearchTriggerBindings.matches("ROD_primal_staff", ResearchClues.Context.item(new ItemStack(Items.NETHER_STAR))),
                    "an unrelated item never triggers the primal staff rod research");

            test.assertTrue(ScanManager.completeScan(player, "@thaumcraft:quick_silver", new AspectList().add(Aspect.AIR, 1), ScanManager.ScanKind.ITEM, ResearchClues.Context.item(silver)),
                    "actual production scan receiver awards a natural direct item clue");
            test.assertTrue(knowledge.isResearchComplete("@JARBRAIN") && !knowledge.isResearchComplete("JARBRAIN") && !knowledge.isResearchComplete("INFUSION")
                            && knowledge.getAspectPoolFor("aer") == 1 && knowledge.getWarpPerm() == 0 && silver.getCount() == 1,
                    "clue may precede its parents and changes no completion, warp or physical item count");
            var after = knowledge.save();
            test.assertTrue(!ScanManager.completeScan(player, "@thaumcraft:quick_silver", new AspectList().add(Aspect.AIR, 1), ScanManager.ScanKind.ITEM, ResearchClues.Context.item(silver))
                            && knowledge.save().equals(after), "native duplicate scan cannot repay RP or reroll a clue");
            test.assertTrue(ScanManager.completeScan(player, "@NODEr12_no_witness", new AspectList().add(Aspect.LIGHT, 1), ScanManager.ScanKind.PHENOMENA),
                    "the unchanged four-argument node scan path still awards its aspects");
            test.assertTrue(!knowledge.isResearchComplete("@LAMPGROWTH") && !knowledge.isResearchComplete("@LAMPFERTILITY"),
                    "node aspects do not invent a nonnull clue witness absent from original completeScan");
            var actualPlayer = EntityAspectBindings.describe(player);
            test.assertTrue(actualPlayer != null && actualPlayer.clue() == null, "a real native player has generated entity aspects but no original EntityList clue witness");
            test.assertTrue(ScanManager.completeScan(player, actualPlayer.signature(), new AspectList().add(Aspect.FLIGHT, 1), ScanManager.ScanKind.ENTITY, actualPlayer.clue())
                            && !knowledge.isResearchComplete("@HOVERGIRDLE"), "positive player-scan rewards cannot invent aspect-based research clues");
            var saved = new CompoundTag(); player.addAdditionalSaveData(saved);
            var restored = test.makeMockPlayer(GameType.SURVIVAL); restored.readAdditionalSaveData(saved);
            test.assertTrue(KnowledgeAccess.get(restored).save().equals(knowledge.save()), "real player NBT preserves the clue marker independently of research completion");
            test.succeed();
        } finally { knowledge.copyFrom(old); }
    }

    public static void entitiesUseNativeVariantsAndRealThaumometerTiming(GameTestHelper test, ServerPlayer player) {
        var level = test.getLevel(); var knowledge = KnowledgeAccess.get(player); var oldKnowledge = knowledge.copy();
        var oldPosition = player.position(); float oldYaw = player.getYRot(), oldPitch = player.getXRot();
        var oldGameType = player.gameMode.getGameModeForPlayer();
        List<ItemStack> inventory = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) inventory.add(player.getInventory().getItem(slot).copy());
        var origin = test.absolutePos(new BlockPos(3, 3, 3)); var chunk = new ChunkPos(origin);
        boolean forced = !level.getForcedChunks().contains(chunk.toLong()); boolean[] closed = {false};
        var target = EntityType.ENDERMAN.create(level);
        test.assertTrue(target != null, "native Enderman fixture exists");
        Runnable cleanup = () -> {
            if (closed[0]) return; closed[0] = true;
            player.stopUsingItem(); target.discard(); player.getInventory().clearContent();
            for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().setItem(slot, inventory.get(slot));
            player.setGameMode(oldGameType); player.setPos(oldPosition); player.setYRot(oldYaw); player.setXRot(oldPitch); knowledge.copyFrom(oldKnowledge);
            if (forced) level.setChunkForced(chunk.x, chunk.z, false);
        };
        try {
            if (forced) level.setChunkForced(chunk.x, chunk.z, true);
            var skeleton = EntityType.SKELETON.create(level); var wither = EntityType.WITHER_SKELETON.create(level);
            var ordinary = EntityAspectBindings.describe(skeleton); var nether = EntityAspectBindings.describe(wither);
            test.assertTrue(ordinary.aspects().getAmount(Aspect.UNDEAD) == 3 && nether.aspects().getAmount(Aspect.UNDEAD) == 4
                            && nether.aspects().getAmount(Aspect.FIRE) == 2 && !ordinary.signature().equals(nether.signature()),
                    "native split skeleton types retain original variant reward replacement and duplicate identity");
            var creeper = EntityType.CREEPER.create(level); var normalCreeper = EntityAspectBindings.describe(creeper);
            var chargedNbt = creeper.saveWithoutId(new CompoundTag()); chargedNbt.putBoolean("powered", true); creeper.load(chargedNbt);
            var charged = EntityAspectBindings.describe(creeper);
            test.assertTrue(charged.aspects().getAmount(Aspect.ENERGY) == 3 && charged.aspects().getAmount(Aspect.PLANT) == 3
                            && !normalCreeper.signature().equals(charged.signature()), "actual native powered NBT changes Creeper tags and scan identity");
            var child = EntityType.ZOMBIE_VILLAGER.create(level); child.setBaby(true);
            test.assertTrue(EntityAspectBindings.describe(child).signature().endsWith("CHILDVILLAGER"), "native child villager-zombie split preserves original signature modifiers");

            player.setGameMode(GameType.SURVIVAL); player.getInventory().clearContent(); player.setPos(Vec3.atBottomCenterOf(origin));
            knowledge.reset(); for (Aspect aspect : Aspect.aspects.values()) knowledge.addDiscoveredAspect(aspect.getTag());
            target.setNoAi(true); target.setNoGravity(true); target.setSilent(true); target.setPos(Vec3.atBottomCenterOf(origin.south(4)));
            test.assertTrue(level.addFreshEntity(target), "actual entity scan target enters the native world index");
            aim(player, target.getEyePosition());
            var tool = new ItemStack(ThaumcraftContent.THAUMOMETER.get()); player.setItemInHand(InteractionHand.MAIN_HAND, tool);
            test.assertTrue(player.gameMode.useItem(player, level, tool, InteractionHand.MAIN_HAND).consumesAction()
                            && tool.has(ThaumcraftContent.SCAN_SESSION.get()), "real native RMB resolves the observed Enderman and starts its stable20-tick session");
            long began = level.getGameTime();
            test.onEachTick(() -> {
                if (closed[0]) return;
                try {
                    if (player.isUsingItem()) tool.getItem().onUseTick(level, player, tool, player.getUseItemRemainingTicks());
                    if (level.getGameTime() - began < 20) {
                        test.assertTrue(!knowledge.hasScannedEntity("@entity:Enderman") && !knowledge.isResearchComplete("@MIRROR"), "no early entity reward or clue before twenty real world ticks");
                        return;
                    }
                    if (!knowledge.hasScannedEntity("@entity:Enderman")) return;
                    test.assertTrue(knowledge.getAspectPoolFor("alienis") == 4 && knowledge.getAspectPoolFor("iter") == 2 && knowledge.getAspectPoolFor("aer") == 2,
                            "actual Enderman scan pays original4alienis/2iter/2aer once");
                    test.assertTrue(knowledge.isResearchComplete("@MIRROR") && !knowledge.isResearchComplete("MIRROR") && !knowledge.isResearchComplete("INFUSION")
                                    && !tool.has(ThaumcraftContent.SCAN_SESSION.get()) && !player.isUsingItem(),
                            "actual entity trigger grants only the mirror clue before its parent and ends its use session");
                    var before = knowledge.save();
                    test.assertTrue(!ScanManager.completeScan(player, "@entity:Enderman", EntityAspectBindings.describe(target).aspects(), ScanManager.ScanKind.ENTITY, ResearchClues.Context.entity("Enderman"))
                                    && knowledge.save().equals(before), "native entity variant signature blocks repeat rewards");
                    cleanup.run(); test.succeed();
                } catch (RuntimeException | Error error) { cleanup.run(); throw error; }
            });
            test.runAfterDelay(80, () -> { if (!closed[0]) { cleanup.run(); test.fail("native entity scan did not complete its stable20-tick session within80 world ticks"); } });
        } catch (RuntimeException | Error error) { cleanup.run(); throw error; }
    }

    private static void aim(ServerPlayer player, Vec3 target) {
        Vec3 delta = target.subtract(player.getEyePosition()); double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        player.setYRot((float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90)); player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, horizontal)));
    }
}
