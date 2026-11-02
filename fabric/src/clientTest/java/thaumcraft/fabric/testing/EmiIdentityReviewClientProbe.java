package thaumcraft.fabric.testing;

import com.google.gson.GsonBuilder;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.serializer.EmiIngredientSerializer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftWandEnchantments;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.ArcaneShapelessRecipe;
import thaumcraft.common.items.CrystalEssenceData;
import thaumcraft.common.items.ItemCrystalEssence;
import thaumcraft.common.items.ItemEssence;
import thaumcraft.common.items.wands.WandData;

/** Actual optional EMI stack comparisons and serialization after its real world/plugin reload. */
public final class EmiIdentityReviewClientProbe implements ClientModInitializer {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("thaumcraft-emi-identity-review");
    private final String runId = "emi-identity-review-" + Instant.now().toString().replace(':', '-');
    private final List<Map<String, Object>> checks = new ArrayList<>();
    private final List<String> failures = new ArrayList<>();
    private Path directory;
    private CompletableFuture<Void> reload;
    private Object recipeManagerBeforeReload;
    private int ticks, phase, settle;
    private boolean created, stopped;

    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean("thaumcraft.emiIdentityReview")) return;
        directory = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
        if (!directory.getFileName().toString().equals("run-review-emi-identity"))
            throw new IllegalStateException("EMI identity review requires its isolated run directory");
        if (!FabricLoader.getInstance().isModLoaded("emi")) throw new IllegalStateException("Run with -PwithEmi");
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(Minecraft client) {
        if (stopped) return;
        try {
            client.options.pauseOnLostFocus = false;
            if (++ticks > 4000) throw new IllegalStateException("Timed out waiting for the actual EMI reload at phase " + phase);
            if (client.level == null || client.player == null || client.getSingleplayerServer() == null) {
                if (!created && client.screen instanceof AccessibilityOnboardingScreen first && client.getOverlay() == null) first.onClose();
                if (!created && client.screen instanceof TitleScreen && client.getOverlay() == null) {
                    created = true;
                    client.options.tutorialStep = TutorialSteps.NONE;
                    client.createWorldOpenFlows().createFreshLevel(runId,
                            new LevelSettings("EMI identity review supplied data", GameType.CREATIVE, false,
                                    Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                            new WorldOptions(73471007L, false, false),
                            access -> access.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions(), client.screen);
                }
                return;
            }
            if (client.getOverlay() != null || !emiReady()) { settle = 0; return; }
            if (++settle < 30) return;
            if (phase == 0) {
                compareActualStacks(client);
                if (!failures.isEmpty()) { finish(client); return; }
                recipeManagerBeforeReload = EmiApi.getRecipeManager();
                reload = client.reloadResourcePacks(); phase = 1; settle = 0;
            } else if (reload.isDone()) {
                reload.join(); compareActualStacks(client); finish(client);
            }
        } catch (Throwable failure) {
            failures.add(failure.toString()); LOG.error("EMI identity review failed", failure); finish(client);
        }
    }

    private boolean emiReady() {
        var manager = EmiApi.getRecipeManager();
        return manager != null && (phase == 0 || manager != recipeManagerBeforeReload)
                && manager.getRecipes().size() > 1000 && manager.getCategories().stream()
                .anyMatch(category -> category.getId().equals(ResourceLocation.parse("thaumcraft:arcane")))
                // Establish that the actual Thaumcraft comparison registration has completed;
                // otherwise an in-progress EMI reload could look like the subtype regression.
                && !EmiStack.of(wand("wood", "iron", 0)).isEqual(EmiStack.of(wand("greatwood", "iron", 0)));
    }

    private void compareActualStacks(Minecraft client) {
        var air = ItemCrystalEssence.create(Aspect.AIR);
        var fire = ItemCrystalEssence.create(Aspect.FIRE);
        var moreAir = air.copy();
        moreAir.set(ThaumcraftContent.CRYSTAL_ESSENCE_DATA.get(), CrystalEssenceData.fromAspects(new AspectList().add(Aspect.AIR, 2)));
        var absent = new ItemStack(ThaumcraftContent.CRYSTAL_ESSENCE.get());
        var empty = absent.copy(); empty.set(ThaumcraftContent.CRYSTAL_ESSENCE_DATA.get(), CrystalEssenceData.EMPTY);
        var firstAir = air.copy(); firstAir.set(ThaumcraftContent.CRYSTAL_ESSENCE_DATA.get(),
                CrystalEssenceData.fromAspects(new AspectList().add(Aspect.AIR, 1).add(Aspect.FIRE, 1)));
        var firstFire = air.copy(); firstFire.set(ThaumcraftContent.CRYSTAL_ESSENCE_DATA.get(),
                CrystalEssenceData.fromAspects(new AspectList().add(Aspect.FIRE, 1).add(Aspect.AIR, 1)));
        var airPhial = ItemEssence.filled("aer");
        var firePhial = ItemEssence.filled("ignis");

        // A real native typed arcane recipe accepts the exact component requirement,
        // accepts incidental name/count, and refuses different amounts or order.
        recipeIdentity(client, "crystal aspect", air, fire);
        recipeIdentity(client, "crystal amount per item", air, moreAir);
        recipeIdentity(client, "crystal complete ordered payload", firstAir, firstFire);
        recipeIdentity(client, "explicitly empty versus absent crystal", empty, absent);
        recipeIdentity(client, "filled phial aspect", firePhial, airPhial);

        equal("different crystal aspects", air, fire, false);
        equal("different crystal amounts per item", air, moreAir, false);
        equal("absent versus explicitly empty crystal", absent, empty, false);
        equal("complete crystal entry order", firstAir, firstFire, false);
        equal("crystal cosmetic name and count", air, cosmetic(air, 32), true);
        equal("different filled phial aspects", airPhial, firePhial, false);
        equal("filled phial cosmetic name and count", firePhial, cosmetic(firePhial, 16), true);

        var wood = wand("wood", "iron", 0);
        var sceptre = wood.copy(); sceptre.set(ThaumcraftWandEnchantments.WAND_SCEPTRE.get(), true);
        equal("wand rods", wood, wand("greatwood", "iron", 0), false);
        equal("wand caps", wood, wand("wood", "gold", 0), false);
        equal("wand versus sceptre", wood, sceptre, false);
        equal("wand charge, count and cosmetic name", wood, cosmetic(wand("wood", "iron", 1000), 2), true);

        // EMI's public serializer is the persistence boundary used by favourite stacks.
        // Round-trip the real component stacks; do not use a fake equality/serializer.
        for (var stack : List.of(air, moreAir, firstAir, firstFire, empty, firePhial, wood, sceptre)) {
            var serialized = EmiIngredientSerializer.getSerialized(EmiStack.of(stack));
            check("EMI favourite serialization " + stack.getItem(), serialized != null && !serialized.isJsonNull());
            if (serialized == null || serialized.isJsonNull()) continue;
            var restored = EmiIngredientSerializer.getDeserialized(serialized);
            check("EMI favourite component round-trip " + stack.getItem(), restored != null
                    && restored.getEmiStacks().size() == 1
                    && ItemStack.isSameItemSameComponents(stack, restored.getEmiStacks().getFirst().getItemStack()));
        }
    }

    private void recipeIdentity(Minecraft client, String name, ItemStack required, ItemStack rejected) {
        var recipe = new ArcaneShapelessRecipe("", List.of(Ingredient.of(required)), new ItemStack(Items.STICK), Map.of("aer", 1));
        check("native typed recipe accepts " + name, recipe.matches(input(required), client.level));
        check("native typed recipe ignores cosmetic count/name for " + name, recipe.matches(input(cosmetic(required, 7)), client.level));
        check("native typed recipe refuses different " + name, !recipe.matches(input(rejected), client.level));
    }

    private static ArcaneRecipeInput input(ItemStack stack) {
        var cells = new ArrayList<ItemStack>(); cells.add(stack);
        while (cells.size() < 9) cells.add(ItemStack.EMPTY);
        return new ArcaneRecipeInput(cells);
    }

    private static ItemStack cosmetic(ItemStack stack, int count) {
        var result = stack.copyWithCount(count); result.set(DataComponents.CUSTOM_NAME, Component.literal("cosmetic review name")); return result;
    }

    private static ItemStack wand(String rod, String cap, int vis) {
        var stack = new ItemStack(ThaumcraftContent.WAND.get());
        stack.set(ThaumcraftContent.WAND_DATA.get(), new WandData(rod, cap, Map.of("aer", vis))); return stack;
    }

    private void equal(String name, ItemStack first, ItemStack second, boolean expected) {
        boolean forward = EmiStack.of(first).isEqual(EmiStack.of(second));
        boolean reverse = EmiStack.of(second).isEqual(EmiStack.of(first));
        check("actual EMI identity " + name, forward == expected && reverse == expected);
    }

    private void check(String name, boolean passed) {
        checks.add(Map.of("phase", phase, "check", name, "passed", passed));
        if (!passed) failures.add("phase " + phase + ": " + name);
    }

    private void finish(Minecraft client) {
        if (stopped) return;
        stopped = true;
        try {
            var receipt = Map.of("run_id", runId, "status", failures.isEmpty() ? "PASS" : "FAIL",
                    "fixture", "fresh isolated 1.21.1 client world; real optional EMI registration, comparison and serializer; supplied local stacks",
                    "checks", checks, "failures", failures);
            Files.writeString(directory.resolve("emi-identity-review.json"), new GsonBuilder().setPrettyPrinting().create().toJson(receipt) + "\n");
            LOG.info("EMI identity review {}: {} checks, {} failures", failures.isEmpty() ? "PASS" : "FAIL", checks.size(), failures.size());
        } catch (Exception failure) { LOG.error("Cannot write EMI identity review receipt", failure); }
        client.stop();
    }
}
