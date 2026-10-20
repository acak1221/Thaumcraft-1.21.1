package thaumcraft.common.lib.research;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.items.ItemResearchNotes;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;

/** Original primary-note issuance at normal difficulty (0); no research grant. */
public final class ResearchNoteIssuance {
    private static final int MAIN_SIZE = 36;

    public enum Failure {
        NONE, UNKNOWN_RESEARCH, UNAVAILABLE, ALREADY_COMPLETE, MISSING_PREREQUISITES,
        MISSING_CLUE, SECONDARY_REQUIRES_PURCHASE, MISSING_PAPER, MISSING_INK,
        DELIVERY_FAILED, INVALID_GENERATION
    }

    public record Result(Failure failure, String key, boolean reused, boolean dropped, boolean discarded) {
        public Result {
            Objects.requireNonNull(failure, "failure");
            Objects.requireNonNull(key, "key");
        }
        public Result(Failure failure, String key, boolean reused, boolean dropped) {
            this(failure, key, reused, dropped, false);
        }
        public boolean success() { return failure == Failure.NONE; }
    }

    private record Plan(Failure failure, String key, boolean reused, int paper, int ink) {
        Result result() { return new Result(failure, key, reused, false); }
    }

    private ResearchNoteIssuance() {}

    /** Current admission/material quote; no inventory mutation, generation or RNG. */
    public static Result preview(Player player, String key) {
        return plan(Objects.requireNonNull(player, "player"), key).result();
    }

    public static Result issue(ServerPlayer player, String key) {
        Objects.requireNonNull(player, "player");
        // Do not seed another generator with world.nextLong: the original uses
        // precisely these bounded calls on World.rand, including discarded draws.
        return issue(player, key, new Random(0L) {
            @Override public int nextInt(int bound) { return player.serverLevel().random.nextInt(bound); }
        });
    }

    /** Explicit generator RNG permits original vectors without replacing native delivery/ink. */
    public static Result issue(ServerPlayer player, String key, Random generationRandom) {
        Objects.requireNonNull(player, "player");
        if (!player.server.isSameThread()) throw new IllegalStateException("Research notes require the server thread");
        Plan plan = plan(player, key);
        if (plan.failure != Failure.NONE || plan.reused) return plan.result();

        List<ItemStack> before = snapshot(player);
        ItemStack output;
        try {
            ResearchNoteData data = ResearchNoteGenerator.generate(plan.key, generationRandom);
            if (!validGeneration(plan.key, data)) return rejected(Failure.INVALID_GENERATION, plan.key);
            output = ItemResearchNotes.create(data, false);
            if (output.isEmpty() || output.getCount() != 1) return rejected(Failure.INVALID_GENERATION, plan.key);
        } catch (RuntimeException exception) {
            // Authority/crash correction to ResearchManager:120-134: prepare a
            // valid note before spending paper or ink. No player RNG was used.
            return rejected(Failure.INVALID_GENERATION, plan.key);
        }

        List<ItemStack> staged = new ArrayList<>(MAIN_SIZE);
        for (ItemStack stack : before) staged.add(stack.copy());
        // Original consumeInventoryItem also decrements paper in creative.
        staged.get(plan.paper).shrink(1);
        if (!ScribingInk.consumeFromPlayer(player, staged.get(plan.ink), true)) {
            return rejected(Failure.MISSING_INK, plan.key);
        }

        int empty = firstEmpty(staged);
        if (empty >= 0) {
            output.setPopTime(5);
            staged.set(empty, output);
            if (!unchanged(player, before)) return rejected(Failure.DELIVERY_FAILED, plan.key);
            commit(player, before, staged);
            return new Result(Failure.NONE, plan.key, false, false);
        }
        if (player.getAbilities().instabuild) {
            // Quirk kept: vanilla 1.7.10 InventoryPlayer.func_70441_a (yx.a,
            // bytecode 56-83) reports success and clears an undeliverable stack
            // in creative. TC4 consequently does not drop this generated note.
            if (!unchanged(player, before)) return rejected(Failure.DELIVERY_FAILED, plan.key);
            commit(player, before, staged);
            return new Result(Failure.NONE, plan.key, false, false, true);
        }

        ItemEntity dropped = player.drop(output.copy(), false);
        // Native ServerPlayer.drop ignores addFreshEntity's boolean; a returned
        // entity can still be unspawned. NeoForge's two-argument path also runs
        // ItemTossEvent, whose cancellation returns null.
        if (dropped == null || dropped.isRemoved() || dropped.level() != player.serverLevel()
                || player.serverLevel().getEntity(dropped.getId()) != dropped
                || !ItemStack.matches(output, dropped.getItem()) || !unchanged(player, before)) {
            if (dropped != null) dropped.discard();
            return rejected(Failure.DELIVERY_FAILED, plan.key);
        }
        commit(player, before, staged);
        return new Result(Failure.NONE, plan.key, false, true);
    }

    /** Pure admission shared by preview/issue and focused catalog regression tests. */
    static Failure validate(PlayerKnowledge knowledge, String key) {
        Objects.requireNonNull(knowledge, "knowledge");
        ResearchDefinition research = resolve(key);
        if (research == null) return Failure.UNKNOWN_RESEARCH;
        if (research.isVirtual() || research.isAutoUnlock() || !research.isEnabled()
                || research.aspectCost().isEmpty()
                || ("ELDRITCH".equals(research.category()) && !knowledge.isResearchComplete("ELDRITCHMINOR"))) {
            return Failure.UNAVAILABLE;
        }
        if (knowledge.isResearchComplete(key)) return Failure.ALREADY_COMPLETE;
        if (thaumcraft.common.config.ThaumcraftSettings.current().purchases(research.isSecondary())) return Failure.SECONDARY_REQUIRES_PURCHASE;
        if (!research.hasPrerequisites(knowledge.getResearchComplete())) return Failure.MISSING_PREREQUISITES;
        if ((research.isHidden() || research.isLost()) && !knowledge.isResearchComplete("@" + key)) {
            return Failure.MISSING_CLUE;
        }
        return Failure.NONE;
    }

    static boolean validGeneration(String key, ResearchNoteData data) {
        return data != null && key.equals(data.key()) && !data.complete() && data.copies() == 0
                && data.cells().stream().noneMatch(cell -> cell.type() == 2);
    }

    private static ResearchDefinition resolve(String key) {
        if (key == null || key.isBlank() || key.length() > ResearchNoteData.MAX_KEY_LENGTH || key.startsWith("@")) return null;
        return ResearchCatalog.get(key);
    }

    private static Plan plan(Player player, String key) {
        Failure failure = validate(KnowledgeAccess.get(player), key);
        String canonical = failure == Failure.UNKNOWN_RESEARCH ? "" : key;
        if (failure != Failure.NONE) return new Plan(failure, canonical, false, -1, -1);
        var items = player.getInventory().items;
        // ResearchManager.getResearchSlot:187-197 searches the main inventory,
        // including either flattened normal/discovery identity, before costs.
        for (int slot = 0; slot < MAIN_SIZE; slot++) {
            ItemStack stack = items.get(slot);
            if (!(stack.getItem() instanceof ItemResearchNotes)) continue;
            ResearchNoteData data = ItemResearchNotes.data(stack);
            if (data != null && key.equals(data.key())) return new Plan(Failure.NONE, key, true, -1, -1);
        }
        int paper = -1;
        for (int slot = 0; slot < MAIN_SIZE; slot++) {
            if (items.get(slot).is(Items.PAPER)) { paper = slot; break; }
        }
        if (paper < 0) return new Plan(Failure.MISSING_PAPER, key, false, -1, -1);
        int ink = -1;
        for (int slot = 0; slot < MAIN_SIZE; slot++) {
            if (ScribingInk.consumeFromPlayer(player, items.get(slot), false)) { ink = slot; break; }
        }
        return new Plan(ink < 0 ? Failure.MISSING_INK : Failure.NONE, key, false, paper, ink);
    }

    private static List<ItemStack> snapshot(Player player) {
        List<ItemStack> result = new ArrayList<>(MAIN_SIZE);
        for (int slot = 0; slot < MAIN_SIZE; slot++) result.add(player.getInventory().items.get(slot).copy());
        return result;
    }

    private static int firstEmpty(List<ItemStack> items) {
        for (int slot = 0; slot < MAIN_SIZE; slot++) if (items.get(slot).isEmpty()) return slot;
        return -1;
    }

    private static boolean unchanged(Player player, List<ItemStack> before) {
        for (int slot = 0; slot < MAIN_SIZE; slot++) {
            if (!ItemStack.matches(before.get(slot), player.getInventory().items.get(slot))) return false;
        }
        return true;
    }

    private static void commit(ServerPlayer player, List<ItemStack> before, List<ItemStack> staged) {
        for (int slot = 0; slot < MAIN_SIZE; slot++) {
            // Preserve unrelated stack instances as well as all their components.
            if (!ItemStack.matches(before.get(slot), staged.get(slot))) player.getInventory().items.set(slot, staged.get(slot));
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }

    private static Result rejected(Failure failure, String key) { return new Result(failure, key, false, false); }
}
