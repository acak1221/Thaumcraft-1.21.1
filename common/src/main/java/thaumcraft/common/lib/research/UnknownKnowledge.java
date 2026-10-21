package thaumcraft.common.lib.research;

import java.util.Random;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.items.ItemUnknownResearchNotes;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.utils.DeferredItemDrops;

/** Original unknown-note use: a privately WORLD-time-seeded hidden choice, then actual world-RNG note generation or seven to nine fragments. */
public final class UnknownKnowledge {
    private UnknownKnowledge() {}
    public static ItemStack reveal(ServerPlayer player, InteractionHand hand) {
        if (!player.server.isSameThread()) throw new IllegalStateException("Unknown knowledge requires the server thread");
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemUnknownResearchNotes)) return stack;
        var level = player.serverLevel();
        // Original ResearchManager.findHiddenResearch:148 seeds the private RNG with
        // the world time counter (1.7.10 World.getWorldTime(), SRG func_72820_D),
        // not the time of day; the same world state must reveal the same hidden
        // research. The 1.21.1 equivalent of getWorldTime() is Level.getGameTime().
        String key = ResearchHiddenSelection.choose(KnowledgeAccess.get(player), level.getGameTime());
        if (key == null) {
            ItemStack fragments = new ItemStack(ThaumcraftContent.MATERIALS.get("knowledge_fragment").get(), 7 + level.random.nextInt(3));
            // Original EntityItem(xk) samples Math.random for hover, yaw and float-rounded X/Z motion.
            // The modern convenience constructor instead consumes two extra WORLD draws; never instantiate that template.
            Math.random(); // Original hoverStart draw, unused by the persisted modern output.
            Math.random(); // Original rotationYaw draw.
            Vec3 position = new Vec3(player.getX(), player.getY() + (double) (player.getEyeHeight() / 2.0F), player.getZ());
            Vec3 motion = new Vec3((float) (Math.random() * .2F - .1F), (double) .2F, (float) (Math.random() * .2F - .1F));
            DeferredItemDrops.enqueueDirected(level, player.blockPosition(), position, motion, fragments);
            stack.shrink(1); // Original explicitly consumes even in creative.
            sound(player, "erase");
        } else {
            ResearchNoteData note = ResearchNoteGenerator.generate(key, new Random(0L) {
                @Override public int nextInt(int bound) { return level.random.nextInt(bound); }
            });
            ItemStack result = stack.transmuteCopy(ThaumcraftContent.RESEARCH_NOTE.get(), stack.getCount());
            result.set(ThaumcraftContent.RESEARCH_NOTE_DATA.get(), note);
            player.setItemInHand(hand, result);
            stack = result;
            sound(player, "write");
        }
        player.getInventory().setChanged();
        return stack;
    }
    private static void sound(ServerPlayer player, String name) {
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", name)),
                SoundSource.PLAYERS, .75F, 1.0F);
    }
}
